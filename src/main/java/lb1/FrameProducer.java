package lb1;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;

public class FrameProducer implements Runnable {

    private final FFmpegFrameGrabber grabber;
    private final BlockingQueue<FrameCapsule> queue;
    private final int numWorkers;


    public FrameProducer(
            String videoPath,
            BlockingQueue<FrameCapsule> queue,
            int numWorkers
    ) throws Exception {
        this.grabber = new FFmpegFrameGrabber(videoPath);
        this.queue = queue;
        this.numWorkers = numWorkers;
        grabber.start();
    }

    @Override
    public void run() {
        try {
            Java2DFrameConverter converter = new Java2DFrameConverter();
            Frame frame;
            int frameNumber = 0;

            while ((frame = grabber.grabFrame()) != null) {
                BufferedImage img = converter.getBufferedImage(frame);

                BufferedImage imgClone = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
                Graphics2D g = imgClone.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();

                queue.put(new FrameCapsule(imgClone, frameNumber++));
            }

            // посылаем каждому воркеру POISON_PILL
            for (int i = 0; i < numWorkers; i++) {
                queue.put(FrameCapsule.POISON_PILL);
            }

            grabber.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
