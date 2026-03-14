package lb1;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.BlockingQueue;

public class FrameProducer implements Runnable {
    /*
        Класс, который определяет процесс подготовки кадров видео для дальнейшей обработки
     */

    private final FFmpegFrameGrabber grabber;
    private final BlockingQueue<FrameCapsule> queue;
    private final Runnable callback;

    public FrameProducer(
            String videoPath,
            BlockingQueue<FrameCapsule> queue,
            Runnable callback
    ) throws Exception {
        this.grabber = new FFmpegFrameGrabber(videoPath);
        this.queue = queue;
        this.callback = callback;
    }

    @Override
    public void run() {
        try {
            grabber.start();

            // для преобразования Frame в BufferedImage
            Java2DFrameConverter converter = new Java2DFrameConverter();

            Frame frame; // для хранения текущего кадра
            int frameNumber = 0; // для хранения номера текущего кадра

            // Последовательно берем кадры из видео
            while ((frame = grabber.grabFrame()) != null) {
                // берем кадр (Frame), преобразовываетм в BufferedImage
                BufferedImage img = converter.getBufferedImage(frame);

                // клонируем - были проблемы, это их решило
                // все, что ниже - часть процесса клонирования
                BufferedImage imgClone = new BufferedImage(img.getWidth(), img.getHeight(), img.getType());
                Graphics2D g = imgClone.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();
                // тут завершено клонирование

                // пакуем картинку и номер кадра в FrameCapsule и кладем в очередь задач
                queue.put(new FrameCapsule(imgClone, frameNumber++));
            }

            callback.run(); // запуск действия по завершении основной работы (убить воркеров...)
            grabber.stop(); // останавливаем граббер (корректное закрытие ресурса)

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
