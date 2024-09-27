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

public class ImageDistance implements Comparable<ImageDistance>{
    private String filename;
    private double similarity;

    public ImageDistance(String filename, double similarity) {
        this.filename = filename;
        this.similarity = similarity;
    }

    public String getFilename() {
        return filename;
    }

    public double getSimilarity() {
        return similarity;
    }

    @Override
    public int compareTo(ImageDistance other) {
        // Compare ImageDistances based on similarity
        return Double.compare(this.similarity, other.similarity);
    }
}