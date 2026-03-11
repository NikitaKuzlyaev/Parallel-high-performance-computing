package lb1;

import lombok.SneakyThrows;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

public class VideoUtils {

    // Подсчет числа кадров видео.
    // Метаданные могут быть ошибочными. Этот метод - нет
    @SneakyThrows
    public static int getFrames(String videoPath) {

        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath)) {
            grabber.start();
            int frameCount = 0;
            Frame frame;
            while ((frame = grabber.grabFrame(false, true, false, false)) != null) {
                frameCount++;
            }
            return frameCount;
        }
    }

}
