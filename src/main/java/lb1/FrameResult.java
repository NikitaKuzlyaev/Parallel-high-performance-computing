package lb1;

import java.awt.image.BufferedImage;

public record FrameResult(
        int index,
        BufferedImage image,
        double[] result
) { }