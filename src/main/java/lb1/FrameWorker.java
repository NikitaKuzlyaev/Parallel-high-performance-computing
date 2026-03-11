package lb1;

import org.bytedeco.javacv.Frame;

import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;

public class FrameWorker implements Runnable {

    private final BlockingQueue<FrameCapsule> queue;
    private final FrameTask task;
    private final BlockingQueue<FrameResult> results;

    public FrameWorker(
            BlockingQueue<FrameCapsule> queue,
            BlockingQueue<FrameResult> results,
            FrameTask task
    ) {
        this.queue = queue;
        this.results = results;
        this.task = task;
    }

    @Override
    public void run() {
        try {
            while (true) {
                FrameCapsule capsule = queue.take();

                if (capsule == FrameCapsule.POISON_PILL) {
                    results.put(new FrameResult(-1, new BufferedImage(1,1,1), null));
                    break;
                }

                FrameResult result = task.process(capsule);
                results.put(result);
            }
        } catch (InterruptedException ignored) {}
    }
}
