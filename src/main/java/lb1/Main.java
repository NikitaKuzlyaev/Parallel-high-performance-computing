package lb1;


import java.util.ArrayList;
import java.util.List;


public class Main {

    static void main() throws Exception {

        String inputVideoPath;
        String outputVideoPath = "output.mp4";

        // Расскоментировать один из блоков ниже
        // (в зависимости от того, обработку какого видео нужно протестировать)

        {
            // Видео 1
            //inputVideoPath = "src/main/java/lb1/statics/Rick Astley - Never Gonna Give You Up Official Video 4K Remaster_1.avi";
        }

        {
            // Видео 2
            inputVideoPath = "src/main/java/lb1/statics/Dead Sara - Heroes [Official Music Video].mp4";
        }

        {
            // todo
            // Видео 3
            // inputVideoPath = "";
        }

        int numberOfWorkers = 12;

        int sizeOfVector = 10; // Чем болльше - тем лучше!!!!!! :)
        double threshold = 0.32d; // Методом тыка это оказалось самым адекватным значением

        // Определяю, что я хочу делать с каждыйм кадром - это абстракция задачи
        FrameTask videoFrameTask = new CalculateColorChannelsHistogramTask(sizeOfVector);

        int timesToRepeat = 3;
        List<Long> results = new ArrayList<>();

        // Подождем немного для чистоты эксперемента
        // Болид должен начинать гонку разогретым
        Thread.sleep(1000);

        for (int i = 0; i < timesToRepeat; i++) {
            // Определение объекта пайплайна
            // Он создает воркеров, блокирующие очереди для задач и все остальное
            VideoProcessingPipeline processingPipeline = new VideoProcessingPipeline(numberOfWorkers);

            Thread.sleep(1000);

            // Время входа (начало обработки видео)
            long enterTime = System.nanoTime();

            // Тут происходит запуск всего. В объекте пайплайна, что был определен выше
            List<FrameResult> future = processingPipeline.process(inputVideoPath, videoFrameTask);

            // Время выхода (завершение обработки видео)
            long exitTime = System.nanoTime();

            long duration = (exitTime - enterTime) / 1_000_000; // выводим за сколько выполнилась обработка
            System.out.println("All frames has been processed!");
            System.out.println("Execution time = " + duration + "ms");

            results.add(duration);
            System.gc();
        }

        System.out.println(results);
        //processingPipeline.compile(future, threshold, outputVideoPath);

    }
}
