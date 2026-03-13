package lb1;

import java.awt.image.BufferedImage;

public class CalculateColorChannelsHistogramTask implements FrameTask {

    private final int numberOfKvants;

    public CalculateColorChannelsHistogramTask(int numberOfKvants) {
        this.numberOfKvants = numberOfKvants;
    }

    @Override
    public FrameResult process(FrameCapsule frame) {

        double[][] hist = computeAllHistograms(frame.image(), numberOfKvants);
        double[] merged = new double[3 * numberOfKvants];

        for (int c = 0; c < 3; c++) {
            System.arraycopy(hist[c], 0, merged, c * numberOfKvants, numberOfKvants);
        }

        return new FrameResult(frame.frameNumber(), frame.image(), merged);
    }

    private double[][] computeAllHistograms(BufferedImage image, int n) {
        double[][] histograms = new double[3][n];
        for (int c = 0; c < 3; c++) {
            histograms[c] = computeHistogram(image, n, c);
        }
        return histograms;
    }

    private double[] computeHistogram(BufferedImage image, int n, int channel) {
        double[] histogram = new double[n];
        int width = image.getWidth();
        int height = image.getHeight();
        int quantSize = 256 / n;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int value;
                switch (channel) {
                    case 0 -> value = (rgb >> 16) & 0xFF; // Red
                    case 1 -> value = (rgb >> 8) & 0xFF;  // Green
                    case 2 -> value = rgb & 0xFF;         // Blue
                    default -> throw new IllegalArgumentException();
                }
                int bin = Math.min(value / quantSize, n - 1);
                histogram[bin]++;
            }
        }

        // Нормализуем к [0,1]
        double totalPixels = width * height;
        for (int i = 0; i < n; i++) {
            histogram[i] /= totalPixels;
        }

        return histogram;
    }

}
