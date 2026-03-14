package lb1;

import java.util.concurrent.BlockingQueue;


public class FrameProducerCallback implements Runnable{
    /*
    Это класс описывает дейцствие, которое должен сделать FrameProducer после того, как выполнит всю свою работу
    А должен он сказать всем воркерам, которые обрабатывают кадры, что дальше кадров нет - можно идти домой
    -> он кладет POISON_PILL в очередь, а когда воркер встречает эту POISON_PILL, то он  R.I.P.
     */

    private final BlockingQueue<FrameCapsule> queue;
    private final int numberOfWorkers;

    public FrameProducerCallback(
            BlockingQueue<FrameCapsule> queue,
            int numberOfWorkers
    ){
        this.queue = queue;
        this.numberOfWorkers = numberOfWorkers;
    }

    @Override
    public void run() {
        // посылаем КАЖДОМУ воркеру POISON_PILL
        for (int i = 0; i < numberOfWorkers; i++) {
            try {
                queue.put(FrameCapsule.POISON_PILL);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
