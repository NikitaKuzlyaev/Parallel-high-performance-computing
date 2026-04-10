package lb1;


import common.FrameResult;
import common.FrameTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


class Main {

    public static void main(String[] args) throws Exception {

        String inputVideoPath;

        // Раскоментировать один из блоков ниже
        // (в зависимости от того, обработку какого видео нужно протестировать)

        {
            // Видео 1
            inputVideoPath = "src/main/java/statics/videos/input/video_1.avi";
        }

        {
            // Видео 2
            // inputVideoPath = "src/main/java/statics/videos/input/video_2.mp4";
        }

        {
            // Видео 3
            // inputVideoPath = "src/main/java/statics/videos/input/video_3.mp4";
        }

        //int numberOfWorkers = 2;
        int[] numberOfWorkersGrid = new int[]{
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12,
                16, 20, 24, 28, 32, 48, 64, 72, 96, 128, 256,
                512, 1024, 2048, 4096, 8192
        };


        int sizeOfVector = 10; // Чем больше - тем лучше!!!!!! :)
        double threshold = 0.32d; // Методом тыка это оказалось самым адекватным значением
        int timesToRepeat = 0;

        HashMap<Integer, List<Long>> results = new HashMap<>();
        List<FrameResult> frameResults;

        // Болид должен начинать гонку разогретым
        int warmup_repeats = 1;


        for (int gridIdx = 0; gridIdx < numberOfWorkersGrid.length; gridIdx++) {

            int numberOfWorkers = numberOfWorkersGrid[gridIdx];
            System.out.println("\n\nStart benchmark for n_workers = " + numberOfWorkers);

            for (int iteration = 0; iteration < timesToRepeat + warmup_repeats; iteration++) {
                // Определение объекта пайплайна
                // Он создает воркеров, блокирующие очереди для задач и все остальное
                VideoProcessingPipeline processingPipeline = new VideoProcessingPipeline(
                        numberOfWorkers, 500, 500);

                processingPipeline.preprocess(inputVideoPath);
                // Немного временени форы, чтобы препроцессинг положил кадры для обработки в очередь задач
                Thread.sleep(5000);

                // Определяю, что я хочу делать с каждыйм кадром - это абстракция задачи
                FrameTask videoFrameTask = new CalculateColorChannelsHistogramTask(sizeOfVector);


                long enterTime = System.nanoTime(); // Время входа

                // Тут происходит запуск всего. В объекте пайплайна, что был определен выше
                frameResults = processingPipeline.process(videoFrameTask);

                long exitTime = System.nanoTime(); // Время выхода
                long duration = (exitTime - enterTime) / 1_000_000; // выводим за сколько выполнилась обработка
                System.out.println("Execution time = " + duration + "ms");

                // Для разогревочных запусков результат не сохраняем
                if (iteration >= warmup_repeats) {
                    results.computeIfAbsent(numberOfWorkers, k -> new ArrayList<>()).add(duration);
                }
            }

        }

        System.out.println(results);

        // Код ниже использовался для компилияции видео, на котором будут подписи сцен и прочего
        String outputVideoPath = "output.mp4";
        compileVideo(inputVideoPath, sizeOfVector, threshold, outputVideoPath);
    }

    static private void compileVideo(String inputVideoPath, int sizeOfVector, double threshold, String outputVideoPath) throws Exception {
        VideoProcessingPipeline processingPipeline = new VideoProcessingPipeline(
                12, 500, 500);
        processingPipeline.preprocess(inputVideoPath);
        FrameTask videoFrameTask = new CalculateColorChannelsHistogramTask(sizeOfVector);
        List<FrameResult> frameResults = processingPipeline.process(videoFrameTask);
        processingPipeline.compile(frameResults, threshold, outputVideoPath);
    }

}
