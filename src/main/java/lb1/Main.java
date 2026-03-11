package lb1;

import lombok.SneakyThrows;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    @SneakyThrows
    static void main() {

        System.out.println("All right :)");

        String videoPath = "src/main/java/lb1/statics/Rick Astley - Never Gonna Give You Up Official Video 4K Remaster_1.avi";

        int numberOfKvants = 10;
        double threshold = 0.32d;

        FrameTask videoFrameTask = new CalculateColorChannelsHistogramTask(numberOfKvants);

        VideoProcessingPipeline processingPipeline = new VideoProcessingPipeline(4);

        List<FrameResult> future = processingPipeline.process(videoPath, videoFrameTask);
        System.out.println("GET RESULTS");

        processingPipeline.compile(future, threshold);


        System.out.println(VideoUtils.getFrames(videoPath));

    }

}
