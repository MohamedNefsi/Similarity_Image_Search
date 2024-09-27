import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

class ColorImage {
    private int width;
    private int height;
    private int depth; // Number of bits per pixel
    private int[][][] pixels; // Representation of pixel values

    public ColorImage(String filename) {
        try {
            File file = new File(filename);
            BufferedImage image = ImageIO.read(file);

            width = image.getWidth();
            height = image.getHeight();
            depth = 8; // Assuming 8 bits per channel

            pixels = new int[width][height][3];
         

            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    int rgb = image.getRGB(i, j);
                    pixels[i][j][0] = (rgb >> 16) & 0xFF; // Red channel
                    pixels[i][j][1] = (rgb >> 8) & 0xFF;  // Green channel
                    pixels[i][j][2] = rgb & 0xFF;         // Blue channel
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getDepth() {
        return depth;
    }

    public int[] getPixel(int i, int j) {
        return pixels[i][j];
    }

    public void reduceColor(int d) {
        int maxColorValue = (1 << d) - 1;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                pixels[i][j][0] = (pixels[i][j][0] >> (8 - d)) & maxColorValue;
                pixels[i][j][1] = (pixels[i][j][1] >> (8 - d)) & maxColorValue;
                pixels[i][j][2] = (pixels[i][j][2] >> (8 - d)) & maxColorValue;
            }
        }

        depth = d;
    }
}
