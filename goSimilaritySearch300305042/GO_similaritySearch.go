//Réalisé par Mohamed Nefsi 300305042

package main

import (
	"fmt"
	"image"
	_ "image/jpeg"
	"io/ioutil"
	"log"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"sync"
	"time"
)

type Histo struct {
	Name string
	H    []int
}

type normalizedHisto struct {
	Name string
	H    []float64
}

var mutex sync.Mutex

func computeHistogram(imagePath string, depth int) (Histo, error) {

	// Ouvrir le fichier JPEG
	file, err := os.Open(imagePath)
	if err != nil {
		return Histo{"", nil}, err // Retourner l'erreur
	}
	defer file.Close()

	img, _, err := image.Decode(file)
	if err != nil {
		return Histo{"", nil}, err
	}

	// Obtenir les dimensions de l'image
	bounds := img.Bounds()
	width, height := bounds.Max.X, bounds.Max.Y

	// Initialiser l'histogramme avec le nombre de bits donné par profondeur
	histogram := make([]int, 1<<(uint(depth)*3))

	// Calculer l'histogramme de l'image
	for y := 0; y < height; y++ {
		for x := 0; x < width; x++ {
			// Convertir le pixel en RGBA
			red, green, blue, _ := img.At(x, y).RGBA()
			// Effectuer un décalage vers la droite de 8 bits pour obtenir la plage correcte (0 à 255)
			red >>= 8
			green >>= 8
			blue >>= 8

			// Réduire la valeur de chaque canal de couleur au nombre de bits spécifié
			red >>= uint(8 - depth)
			green >>= uint(8 - depth)
			blue >>= uint(8 - depth)

			// Calculer l'index de l'histogramme en combinant les valeurs des canaux RGB
			index := (red << uint(2*depth)) + (green << uint(depth)) + blue
			// Incrémenter le compteur de l'histogramme à cet index
			histogram[index]++
		}
	}

	histo := Histo{imagePath, histogram}

	return histo, nil
}

func computeHistograms(imagePaths []string, depth int, hChan chan<- Histo, wg *sync.WaitGroup) {
	defer wg.Done() // Marquer la goroutine comme terminée lorsqu'elle se termine
	for _, path := range imagePaths {
		histo, err := computeHistogram(path, depth)
		if err != nil {
			fmt.Printf("Error computing histogram for %s: %v\n", path, err)
			continue
		}
		hChan <- histo
	}
}

func normalizeHistogram(hist Histo) normalizedHisto {
	var sum int
	for _, val := range hist.H {
		sum += val
	}
	normalized := make([]float64, len(hist.H))
	for i, val := range hist.H {
		normalized[i] = float64(val) / float64(sum)
	}
	return normalizedHisto{Name: hist.Name, H: normalized}
}

func computeIntersection(hist1, hist2 []float64) float64 {
	intersection := 0.0
	for i := 0; i < len(hist1); i++ {
		if hist1[i] < hist2[i] {
			intersection += hist1[i]
		} else {
			intersection += hist2[i]
		}
	}
	return intersection
}

func main() {
	args := os.Args
	if len(args) != 3 {
		fmt.Println("Usage: go run similaritySearch queryImageFilename imageDatasetDirectory")
		return
	}

	// Lire le répertoire d'images
	files, err := ioutil.ReadDir(args[2])
	if err != nil {
		log.Fatal(err)
	}

	// Déclarer un chrono pour mesurer le temps d'exécution
	start := time.Now()

	// Créer une liste pour stocker les noms de fichiers d'images
	var imagePaths []string
	// Filtrer les fichiers avec l'extension .jpg
	for _, file := range files {
		if strings.HasSuffix(file.Name(), ".jpg") {
			imagePath := filepath.Join(args[2], file.Name())
			imagePaths = append(imagePaths, imagePath)
		}
	}

	k := 1048

	// Declaration des channel et du waitgrp
	hChan := make(chan Histo)

	var wg sync.WaitGroup

	for i := 0; i < len(imagePaths); i += k {
		end := i + k
		if end > len(imagePaths) {
			end = len(imagePaths)
		}
		wg.Add(1) // passer une go routine au waitgrp
		go computeHistograms(imagePaths[i:end], 3, hChan, &wg)
	}

	// Créer le canal pour l'histogramme de la requête
	qChan := make(chan Histo)

	// Dans un fil concurrent séparé, ouvrir l’image de requête et calculer son histogramme
	go func() {
		queryImagePath := args[1]
		queryHist, err := computeHistogram(queryImagePath, 3)
		if err != nil {
			fmt.Printf("Error computing histogram for query image: %v\n", err)
			close(qChan)
			return
		}
		qChan <- queryHist
	}()

	// Attendre que toutes les goroutines terminent pour fermer hChan
	go func() {
		wg.Wait()
		close(hChan)
	}()

	// Créer une liste pour stocker les histogrammes reçus
	var histograms []Histo

	// Lancer une goroutine pour recevoir les histogrammes
	go func() {
		for histo := range hChan {
			mutex.Lock()
			histograms = append(histograms, histo)
			mutex.Unlock()
		}
	}()

	queryHist := <-qChan

	wg.Wait() // Attendre que la lecture des histogrammes soit terminée.

	// Normaliser les histogrammes
	var normalizedHistograms []normalizedHisto
	mutex.Lock()
	for _, hist := range histograms {
		normalized := normalizeHistogram(hist)
		normalizedHistograms = append(normalizedHistograms, normalized)
	}
	mutex.Unlock()

	normalizedQueryHist := normalizeHistogram(queryHist)

	// Traiter les histogrammes et maintenir la liste des 5 images les plus similaires
	type SimilarImage struct {
		Path       string
		Similarity float64
	}

	var topSimilarImages []SimilarImage
	// Calculer les similarités avec l'histogramme de la requête
	for _, hist := range normalizedHistograms {
		similarity := computeIntersection(normalizedQueryHist.H, hist.H)
		topSimilarImages = append(topSimilarImages, SimilarImage{Path: hist.Name, Similarity: similarity})
	}

	// Trier par similarité décroissante
	sort.Slice(topSimilarImages, func(i, j int) bool {
		return topSimilarImages[i].Similarity > topSimilarImages[j].Similarity
	})

	elapsed := time.Since(start)
	fmt.Printf("Temps d'exécution: %s\n", elapsed)

	fmt.Println("les 5 meilleurs similarite : ")
	for i, img := range topSimilarImages[:5] {
		fmt.Printf("%d: %s - Similarite de : %f\n", i+1, img.Path, img.Similarity)
	}
}
