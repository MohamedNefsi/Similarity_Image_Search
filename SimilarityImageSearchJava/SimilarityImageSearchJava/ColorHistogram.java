import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

class ColorHistogram {
    private int d; // Number of bits for color representation
    private double[] histogram; // Normalized histogram values

    public ColorHistogram(int d) {
        this.d = d;
        int numColors = (int) Math.pow(2, 3 * d);
        histogram = new double[numColors];
    }

    public ColorHistogram(String filename) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader("imageDataset2_15_20/"+filename));

            // Lecture de la première ligne pour obtenir le nombre de couleurs
            String line = reader.readLine();
            int numColors = Integer.parseInt(line.trim());

            histogram = new double[numColors];

            // Lecture de la deuxième ligne pour obtenir les nombres de pixels par couleur
            line = reader.readLine();
            String[] values = line.split(" ");

            for (int i = 0; i < numColors; i++) {
                histogram[i] = Double.parseDouble(values[i]);
            }

            reader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setImage(ColorImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                int[] pixel = image.getPixel(i, j);
                int index = calculateIndex(pixel[0], pixel[1], pixel[2]);
                
                histogram[index]++;
            }
        }

    }

    public double[] getHistogram() {
        double totalPixels = 0.0;

        // Calculer la somme des éléments dans l'histogramme
        for (double count : histogram) {
            totalPixels += count;
        }
        
        // Normaliser l'histogramme
        for (int i = 0; i < histogram.length; i++) {
            histogram[i] /= totalPixels;
        }

        return histogram;
    }

    public double compare(ColorHistogram hist) {
        double intersection = 0.0;

        for (int i = 0; i < histogram.length; i++) {
            intersection += Math.min(this.getHistogram()[i], hist.getHistogram()[i]);
        }

        return intersection;
    }

    public void save(String filename) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filename));

            // Écrire le nombre de couleurs sur la première ligne
            writer.write(histogram.length + "\n");

            // Écrire les nombres de pixels par couleur 
            for (double value : histogram) {
                writer.write((int) value + " ");
            }

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int calculateIndex(int red, int green, int blue) {
        return (red << (2 * d)) + (green << d) + blue;
    }
    private void normalizeHistogram(int totalPixels) {
        for (int i = 0; i < histogram.length; i++) {
            histogram[i] /= totalPixels;
        }
    }

}
