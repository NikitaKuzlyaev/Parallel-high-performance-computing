package lb1;

import common.*;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

class VideoProcessingPipeline {

    private final int workers; // число воркеров (потоков) для выполнения задачи
    private final BlockingQueue<FrameCapsule> frameQueue; // очередь с кадрами на обработку
    private final BlockingQueue<FrameResult> resultQueue; // очередь с результатами обработки кадров
    private final ExecutorService executor; // сервис, управляющий потоками

    public VideoProcessingPipeline(
            int workers,
            int frameQueueCapacity,
            int resultQueueCapacity
    ) {
        this.workers = workers;
        frameQueue = new ArrayBlockingQueue<>(frameQueueCapacity);
        resultQueue = new ArrayBlockingQueue<>(resultQueueCapacity);
        // на один воркер больше - отдельный воркер, который готовит кадры для обработки
        // его задачи сильно легче - поэтому ему хватит одного потока и мы потом дадим ему фору по времени
        executor = Executors.newFixedThreadPool(workers + 1);
    }

    public void preprocess(String videoPath) throws Exception {
        /*
        Метод, запускающий поток, который будет готовить кадры и помещать их в очередь на обработку
         */
        Runnable callback = new FrameProducerCallback(frameQueue, workers);
        executor.submit(new FrameProducer(videoPath, frameQueue, callback));
    }

    public List<FrameResult> process(FrameTask task) throws Exception {
        /*
        Метод, который запускает все воркеры для обработки кадров
         */

        for (int i = 0; i < workers; i++) {
            // создаем воркера (каждый создается в отдельном потоке!)
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

        executor.shutdown(); // вежливо просим остановиться, так как работа выполнена
        executor.awaitTermination(3, TimeUnit.SECONDS); // ждем по-хорошему, иначе выдергиваем вилку питания

        return results;
    }

    public void compile(List<FrameResult> frames, double threshold, String outputPath)
            throws FFmpegFrameRecorder.Exception {
        /*
        Этот метод занимается соединением обработанных кадров и склеивает их в один видеоряд
        Также он:
            * считает дистанцию между соседними кадрами, определяет, когда началась другая сцена
            * наносит маркировку на видеокадры (с номером кадра, вектором, номером сцены)

            (да, он "немного" перегружен)
         */

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
        Color[] colors = new Color[]{Color.RED, Color.GREEN, Color.BLUE};

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
        int current_scene = 0; // номер текущей сцены

        // для каждого кадра выполняем...
        for (FrameResult frameResult : frames) {

            // берем этот кадр, на всякий случай переводим в 3 канала (были проблемки с этим связанные)
            BufferedImage frame = frameResult.image();
            BufferedImage img = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

            Graphics2D g = img.createGraphics(); // класс, позволяющий рисовать
            g.drawImage(frame, 0, 0, null);

            g.setFont(new Font("Arial", Font.BOLD, (int) big_font_size));
            g.setColor(Color.RED);
            // Подписываем в уголке номер кадра жирным красным шрифтом
            g.drawString(String.valueOf(frameResult.index()), 10, 30);

            // все остальное шрифтом обычной толщины
            g.setFont(new Font("Arial", Font.PLAIN, (int) font_size));

            // далее подписываем на картинке значение вектора - сверху вниз по одной компоненте
            // также каждый канал красим в соответсвующий цвет
            int n = frameResult.result().length / 3;
            int it = -1;
            double cur_y = base_cur_y;

            for (int i = 0; i < frameResult.result().length; i++) {

                if (i % n == 0) {
                    cur_y += offset;
                    it++;
                }
                g.setColor(colors[it]);

                g.drawString(String.valueOf(frameResult.result()[i]), 10, (int) cur_y);
                cur_y += offset;
            }


            double distance = 0d; // переменная для расстояния между ветором текущего кадра и предыдущего

            // определяем является ли предыдущий кадр и текущий частью одной сцены
            for (int j = 0; j < frameResult.result().length; j++) {
                distance += Math.pow(frameResult.result()[j] - prev_vector[j], 2);
            }
            distance = Math.sqrt(distance);

            // если порог выше - значит другая сцена
            if (distance > threshold) {
                current_scene++;
            }

            prev_vector = frameResult.result(); // "текущий" вектор станет "предыдущим" для следующего кадра

            // подписываем значение расстояния между векторами d
            cur_y += offset * 4;
            g.setColor(Color.RED);
            g.drawString("Dist = " + String.valueOf(distance), 10, (int) cur_y);

            // подписываем номер сцены
            cur_y += offset * 4;
            g.drawString("Scene = " + String.valueOf(current_scene), 10, (int) cur_y);
            g.dispose();

            // записываем кадр в видео
            recorder.record(converter.convert(img));
        }

        // сохраняем видео и освобождаем ресурсы
        recorder.stop();
        recorder.release();

    }

}
