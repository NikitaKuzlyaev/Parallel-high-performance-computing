package lb1;

import java.awt.image.BufferedImage;

public class CalculateColorChannelsHistogramTask implements FrameTask {

    private final int sizeOfVector;

    public CalculateColorChannelsHistogramTask(int sizeOfVector) {
        this.sizeOfVector = sizeOfVector;
    }

    @Override
    public FrameResult process(FrameCapsule frame) {
        /*
        Этот метод запускается воркерами, он определяет какая задача будет выполняться
        с каждым кадром - у нас это построение гистограмм распределения цветовых оттенков
         */

        // Запускаем подсчет гистограмм по всем цветовым каналам
        int colorChannels = 3;
        double[] hist = computeAllHistograms(frame.getImage(), colorChannels, sizeOfVector);

        return new FrameResult(frame.getFrameNumber(), frame.getImage(), hist);
    }

    private double[] computeAllHistograms(BufferedImage image, int colorChannels, int sizeOfVector) {
        // Для каждого цветового канала по отдельности вызываем метод построения гистограммы
        // и сохраняем все результаты в матрицу
        double[][] histograms = new double[colorChannels][sizeOfVector];
        for (int c = 0; c < colorChannels; c++) {
            histograms[c] = computeHistogram(image, sizeOfVector, c);
        }

        // делаем матрицу flat - просто записываем все вектора подряд в один вектор (массив)
        double[] merged = new double[colorChannels * sizeOfVector];
        for (int c = 0; c < colorChannels; c++) {
            System.arraycopy(histograms[c], 0, merged, c * sizeOfVector, sizeOfVector);
        }
        return merged;
    }

    private double[] computeHistogram(BufferedImage image, int sizeOfVector, int channel) {
        double[] histogram = new double[sizeOfVector];
        int width = image.getWidth();
        int height = image.getHeight();

        // Размера одного кванта
        int quantSize = 256 / sizeOfVector;

        // Перебираем каждый отдельный пиксель кадра
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {

                int rgb = image.getRGB(x, y); // получаем пиксель
                // !! цвет пикселя возвращается как одно 32-битное число.
                // внутри этого числа лежат сразу все каналы.

                // AAAAAAAA RRRRRRRR GGGGGGGG BBBBBBBB
                // 31....24 23....16 15.....8 7......0

                int value;
                switch (channel) {
                    // 0xFF - это 11111111
                    // делаем битовый сдвиг - нужные нам биты - первые 8 (считая справа)
                    // но слева может быть мусор - биты альфа канала (они не нужны)
                    // делаем битовое И чтобы оставить только первые 8 бит
                    case 0 -> value = (rgb >> 16) & 0xFF; // Red
                    case 1 -> value = (rgb >> 8) & 0xFF;  // Green
                    case 2 -> value = rgb & 0xFF;         // Blue
                    default -> throw new IllegalArgumentException();
                }
                // Определяем в какой бин отправить отенок пикселя
                int bin = Math.min(value / quantSize, sizeOfVector - 1);
                histogram[bin]++;
            }
        }

        // Нормализуем к [0,1]
        double totalPixels = width * height;
        for (int i = 0; i < sizeOfVector; i++) {
            histogram[i] /= totalPixels;
        }

        return histogram;
    }

}
