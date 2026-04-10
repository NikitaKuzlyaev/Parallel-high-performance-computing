package lb2;

import common.FrameResult;
import common.FrameTask;
import lb2.opencl.OpenClFrameProcessor1d1d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainOpenCL {
    // Раскоментировать необходимый путь для используемоего GpuFrameProcessor

    // OpenClFrameProcessor1d1d
    public static final String clPath = "src/main/java/lb2/opencl/embossDownscale2x_1d1d.cl";
    static Class<? extends GpuFrameProcessor> gpuFrameProcessorClass = OpenClFrameProcessor1d1d.class;

    // OpenClFrameProcessor2d2d
    //public static final String clPath = "src/main/java/lb2/opencl/embossDownscale2x_2d2d.cl";
    //static Class<? extends GpuFrameProcessor> gpuFrameProcessorClass = OpenClFrameProcessor2d2d.class;


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
            //inputVideoPath = "src/main/java/statics/videos/input/video_2.mp4";
        }

        {
            // Видео 3
            //inputVideoPath = "src/main/java/statics/videos/input/video_3.mp4";
        }

        HashMap<Integer, List<Long>> results = new HashMap<>();
        List<FrameResult> frameResults;

        // Болид должен начинать гонку разогретым
        int warmup_repeats = 0;
        int numberOfWorkers = 1;
        int timesToRepeat = 1;

        for (int iteration = 0; iteration < timesToRepeat + warmup_repeats; iteration++) {
            // Определение объекта пайплайна
            // Он создает воркеров, блокирующие очереди для задач и все остальное
            VideoProcessingPipeline processingPipeline = new VideoProcessingPipeline(
                    numberOfWorkers, 500, 500);

            processingPipeline.preprocess(inputVideoPath);
            // Немного временени форы, чтобы препроцессинг положил кадры для обработки в очередь задач
            Thread.sleep(5000);

            // Определяю, что я хочу делать с каждыйм кадром - это абстракция задачи
            GpuFrameProcessor gpuFrameProcessor = gpuFrameProcessorClass
                    .getDeclaredConstructor(String.class)
                    .newInstance(clPath);

            FrameTask videoFrameTask = new ApplyConvolutionalMatrixTask(gpuFrameProcessor);

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


        System.out.println(results);

        // Код ниже использовался для компилияции видео, на котором будут подписи сцен и прочего
        String outputVideoPath = "output.mp4";

        compileVideo(inputVideoPath, outputVideoPath);
    }

    static private void compileVideo(String inputVideoPath, String outputVideoPath) throws Exception {
        VideoProcessingPipeline processingPipeline = new VideoProcessingPipeline(
                1, 500, 500);

        processingPipeline.preprocess(inputVideoPath);

        GpuFrameProcessor gpuFrameProcessor = gpuFrameProcessorClass
                .getDeclaredConstructor(String.class)
                .newInstance(clPath);

        FrameTask videoFrameTask = new ApplyConvolutionalMatrixTask(gpuFrameProcessor);
        List<FrameResult> frameResults = processingPipeline.process(videoFrameTask);

        processingPipeline.compile(frameResults, outputVideoPath);
    }


}
