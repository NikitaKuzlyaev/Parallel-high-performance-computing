package common;

import java.awt.image.BufferedImage;

public class FrameCapsule {
    /*
    Этот класс инкапсулирует в себе изображение кадра и его номер,
    чтобы они были вместе и не потерялись :)
     */

    // специальный объект класса, на который можно ссылаться
    // например, у нас применяется так:
    //      * когда воркер, готовящий кадры понимает, что кадры закончились,
    //        то кладет этот объект в очередь, далее если воркер, обрабатывающий кадры встречает этот объект,
    //        то он понимает, что "настоящие" кадры закончились и останавливается
    public static final FrameCapsule POISON_PILL = new FrameCapsule(null, -1);

    private final BufferedImage image;
    private final int frameNumber;

    public FrameCapsule(BufferedImage image, int frameNumber) {
        this.image = image;
        this.frameNumber = frameNumber;
    }

    // обычный геттер для image
    public BufferedImage getImage() {
        return image;
    }

    // обычный геттер для frameNumber
    public int getFrameNumber() {
        return frameNumber;
    }
}
