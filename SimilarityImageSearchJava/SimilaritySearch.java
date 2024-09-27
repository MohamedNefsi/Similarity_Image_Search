import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.PriorityQueue;

public class SimilaritySearch {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java SimilaritySearch <query_image_file> <image_database_directory>");
            return;
        }

        String queryImageFile = "queryImages/"+args[0];
        String imageDatabaseDirectory = args[1];

        SimilaritySearch similaritySearch = new SimilaritySearch();
        similaritySearch.searchSimilarImages(queryImageFile, imageDatabaseDirectory, 5);
    }

    public void searchSimilarImages(String queryImageFile, String imageDatabaseDirectory, int k) {

     
        ColorImage queryImage = new ColorImage(queryImageFile);
        queryImage.reduceColor(3);
        ColorHistogram queryHistogram = new ColorHistogram(queryImage.getDepth());//depth=3
        queryHistogram.setImage(queryImage);
      


        File databaseDir = new File(imageDatabaseDirectory);
        File[] imageFiles = databaseDir.listFiles();

        if (imageFiles == null) {
            System.out.println("la base de donnes ne contient rien");
            return;
        }

        
        PriorityQueue<ImageDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(ImageDistance::getSimilarity));

        for (File imageFile : imageFiles) {
            if (imageFile.isFile() && imageFile.getName().endsWith(".txt") && !imageFile.getName().equals(queryImageFile)) {
                ColorHistogram databaseHistogram = new ColorHistogram(imageFile.getName());

                double similarity = queryHistogram.compare(databaseHistogram);

                pq.offer(new ImageDistance(imageFile.getName(), similarity));

                if (pq.size() > k) {
                    pq.poll(); 
                }
            }
        }

        // Print the top k similar images
        System.out.println("les meilleurs" + k + " similair a sont " + queryImageFile + ":");
        while (!pq.isEmpty()) {
            ImageDistance imageDistance = pq.poll();
            System.out.println(imageDistance.getFilename() + " - Similarite: " + imageDistance.getSimilarity());
        }
    }
}