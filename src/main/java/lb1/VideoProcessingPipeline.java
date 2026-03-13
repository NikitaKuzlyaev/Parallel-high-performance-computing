package lb1;

import lombok.SneakyThrows;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;
import utils.Benchmark;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

public class VideoProcessingPipeline {

    private final int workers;
    private final BlockingQueue<FrameCapsule> frameQueue;
    private final BlockingQueue<FrameResult> resultQueue;
    private final ExecutorService executor;

    public VideoProcessingPipeline(
            int workers,
            int frameQueueCapacity,
            int resultQueueCapacity
    ) {
        this.workers = workers;
        frameQueue = new ArrayBlockingQueue<>(400);
        resultQueue = new ArrayBlockingQueue<>(400);
        executor = Executors.newFixedThreadPool(workers + 1);
    }

    public void preprocess(String videoPath) throws Exception {
        executor.submit(new FrameProducer(videoPath, frameQueue, workers));
    }

    // @Benchmark
    public List<FrameResult> process(FrameTask task) throws Exception {

        //executor.submit(new FrameProducer(videoPath, frameQueue, workers));

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

        //System.out.println("END");
        return results;
    }

    public void compile(List<FrameResult> frames, double threshold, String outputPath)
            throws FFmpegFrameRecorder.Exception
    {

        int width = frames.get(0).image().getWidth();
        int height = frames.get(0).image().getHeight();

        // Я просто смотрел fps видео в свойствах файла и ставил такое же
        int fps = 24;

        // Вначале я сделал видео с шириной 480, и на нем шрифт 12 размера был норм
        // но потом запустил на видео с большим разрешением и шрифт стал слишком мелким
        // решил его скейлить пропорционально ширине
        double base_height = 480;
        double base_font_size = 12;
        double big_font_cf = 1.25;
        double font_size = base_font_size * (height / base_height);
        double big_font_size = font_size * big_font_cf;

        double base_cur_y = 50; // координата по высоте (считается сверху экрана) откуда начинается текст
        double base_offset = 10; // отступ между соседними строками
        double offset = base_offset * (height / base_height);

        Java2DFrameConverter converter = new Java2DFrameConverter();
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputPath, width, height);

        // без этих настроек сильно шакалит выходное видео
        recorder.setFormat("mp4");
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        recorder.setFrameRate(fps);
        recorder.setVideoBitrate(8_000_000);    // 8 Mbps
        recorder.setPixelFormat(0);             // YUV420P
        recorder.setGopSize(fps);               // ключевой кадр раз в ~1 сек
        recorder.start();

        // Сортируем FrameResult объекты по их индексам (чтобы идти в цикле и склеивать их в правильном порядке)
        frames.sort(Comparator.comparingInt(FrameResult::index));

        double[] prev_vector = new double[3 * frames.get(0).result().length]; // вектор "гистограмм" предыдущего кадра
        int current_scene = 0;

        for (FrameResult frameResult : frames) {

            BufferedImage frame = frameResult.image();
            BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

            Graphics2D g = img.createGraphics();
            g.drawImage(frame, 0, 0, null);

            g.setFont(new Font("Arial", Font.BOLD, (int) big_font_size));
            g.setColor(Color.RED);
            g.drawString(String.valueOf(frameResult.index()), 10, 30);

            g.setFont(new Font("Arial", Font.PLAIN, (int) font_size));

            Color[] colors = new Color[]{Color.RED, Color.GREEN, Color.BLUE};
            int n = frameResult.result().length / 3;
            int it = -1;
            double cur_y = base_cur_y;

            for (int i = 0; i < frameResult.result().length; i++) {

                if (i % n == 0) {
                    cur_y += offset;
                    it += 1;
                }
                g.setColor(colors[it]);

                g.drawString(String.valueOf(frameResult.result()[i]), 10, (int) cur_y);
                cur_y += offset;
            }

            double distance = 0d; // переменная для расстояния между ветором текущего кадра и предыдущего

            for (int j = 0; j < frameResult.result().length; j++) {
                distance += Math.pow(frameResult.result()[j] - prev_vector[j], 2);
            }
            distance = Math.sqrt(distance);
            if (distance > threshold) {
                current_scene++;
            }

            prev_vector = frameResult.result();

            cur_y += offset * 4;
            g.setColor(Color.RED);
            g.drawString("Dist = " + String.valueOf(distance), 10, (int) cur_y);

            cur_y += offset * 4;
            g.drawString("Scene = " + String.valueOf(current_scene), 10, (int) cur_y);
            g.dispose();

            recorder.record(converter.convert(img));
        }

        recorder.stop();
        recorder.release();


    }

}
