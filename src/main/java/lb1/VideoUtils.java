package lb1;

import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.FrameGrabber;

public class VideoUtils {

    // Подсчет числа кадров видео.
    // Метаданные могут быть ошибочными. Этот метод - нет
    public static int getFrames(String videoPath) throws FrameGrabber.Exception {

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
