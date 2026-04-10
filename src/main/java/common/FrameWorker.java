package common;


import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;

public class FrameWorker implements Runnable {
    /*
        Класс, определяющий:
            * какую работу нужно выполнять с каждым поступившим объектом FrameCapsule
            * откуда эту работу брать (очередь задач)
            * куда отправлять результаты выполненной работы (очередь результатов)
    */

    private final BlockingQueue<FrameCapsule> queue;
    private final FrameTask task;
    private final BlockingQueue<FrameResult> results;

    public FrameWorker(BlockingQueue<FrameCapsule> queue, BlockingQueue<FrameResult> results, FrameTask task) {
        this.queue = queue;
        this.results = results;
        this.task = task;
    }

    @Override
    public void run() {
        try {
            // ждем работу бесконечно или пока нас не прервут
            while (true) {
                System.out.println("Worker in FrameWorker.run()");
                // берем задачу
                FrameCapsule capsule = queue.take();

                // если в ней яд -> gg
                if (capsule == FrameCapsule.POISON_PILL) {
                    results.put(new FrameResult(-1, new BufferedImage(1, 1, 1), null));
                    break;
                }

                FrameResult result = task.process(capsule); // иначе запускаем обработку
                results.put(result); // результат кладем в очередь
                System.out.println("Worker done!");

            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
