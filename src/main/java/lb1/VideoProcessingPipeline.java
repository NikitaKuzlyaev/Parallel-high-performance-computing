package lb1;

import lombok.SneakyThrows;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

public class VideoProcessingPipeline {

    private final int workers;

    public VideoProcessingPipeline(int workers) {
        this.workers = workers;
    }

    public List<FrameResult> process(String videoPath, FrameTask task) throws Exception {

        BlockingQueue<FrameCapsule> frameQueue = new ArrayBlockingQueue<>(100);
        BlockingQueue<FrameResult> resultQueue = new ArrayBlockingQueue<>(100);

        ExecutorService executor = Executors.newFixedThreadPool(workers + 1);

        executor.submit(new FrameProducer(videoPath, frameQueue, workers));

        for (int i = 0; i < workers; i++) {
            executor.submit(new FrameWorker(frameQueue, resultQueue, task));
        }

        List<FrameResult> results = new ArrayList<>();
        int finishedWorkers = 0;

        while (finishedWorkers < workers) {
            FrameResult r = resultQueue.take();

            if (r.index() == -1) {
                finishedWorkers++;
            } else {
                results.add(r);
            }
        }

        executor.shutdown();
        executor.awaitTermination(3, TimeUnit.SECONDS);

        System.out.println("END");
        return results;
    }

    @SneakyThrows
    public void compile(List<FrameResult> frames, double threshold) {
        String outputPath = "output.mp4";
        int width = frames.get(0).image().getWidth() * 2;
        int height = frames.get(0).image().getHeight() * 2;
        int fps = 30;

        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputPath, width, height);
        recorder.setFrameRate(fps);
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFormat("mp4");
        recorder.start();

        Java2DFrameConverter converter = new Java2DFrameConverter();

        frames.sort(Comparator.comparingInt(FrameResult::index));

        double[] prev_vector = new double[3*frames.get(0).result().length];
        double distance = 0d;
        int current_scene = 0;

        for (FrameResult frameResult : frames) {
            BufferedImage frame = frameResult.image();

            BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), frame.getType());
            Graphics2D g = img.createGraphics();
            g.drawImage(frame, 0, 0, null);

            g.setFont(new Font("Arial", Font.BOLD, 16));
            g.setColor(Color.RED);
            g.drawString(String.valueOf(frameResult.index()), 10, 30);

            g.setFont(new Font("Arial", Font.PLAIN, 12));
            int cur_y = 50;
            int offset = 10;
            Color[] colors = new Color[]{Color.RED, Color.GREEN, Color.BLUE};
            int n = frameResult.result().length / 3;
            int it = -1;

            for (int i = 0; i < frameResult.result().length; i++) {

                if (i % n == 0){
                    cur_y += 5;
                    it += 1;
                }
                g.setColor(colors[it]);

                g.drawString(String.valueOf(frameResult.result()[i]), 10, cur_y);
                cur_y += offset;
            }

            distance = 0d;
            for (int j = 0; j < frameResult.result().length; j++) {
                distance += Math.pow(frameResult.result()[j] - prev_vector[j], 2);
            }
            distance = Math.sqrt(distance);
            if (distance > threshold){
                current_scene++;
            }

            prev_vector = frameResult.result();

            cur_y += 20;
            g.setColor(Color.RED);
            g.drawString("Dist = "+String.valueOf(distance) ,10, cur_y);
            cur_y += 20;
            g.drawString("Scene = "+String.valueOf(current_scene) ,10, cur_y);
            g.dispose();

            recorder.record(converter.convert(img));
        }

        recorder.stop();
        recorder.release();


    }

}
