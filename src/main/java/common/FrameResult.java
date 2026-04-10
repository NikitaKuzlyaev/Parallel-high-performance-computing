package common;

import java.awt.image.BufferedImage;

public record FrameResult(
        int index, // номер кадра (нужен для дальнейшей склейки)
        BufferedImage image, // изображение кадра
        double[] result // гистограмма, посчитанная для изображения этого кадра
) { }