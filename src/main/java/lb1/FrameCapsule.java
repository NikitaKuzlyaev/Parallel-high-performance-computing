package lb1;

import java.awt.image.BufferedImage;

public class FrameCapsule {
    public static final FrameCapsule POISON_PILL = new FrameCapsule(null, -1);

    private final BufferedImage image;
    private final int frameNumber;

    public FrameCapsule(BufferedImage image, int frameNumber) {
        this.image = image;
        this.frameNumber = frameNumber;
    }

    public BufferedImage image() {
        return image;
    }

    public int frameNumber() {
        return frameNumber;
    }
}
