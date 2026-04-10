package common;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

public class BufferedImage2Bytes {

    public static BufferedImage ensure3ByteBGR(BufferedImage src) {
        if (src.getType() == BufferedImage.TYPE_3BYTE_BGR) return src;

        BufferedImage img = new BufferedImage(
                src.getWidth(),
                src.getHeight(),
                BufferedImage.TYPE_3BYTE_BGR
        );
        img.getGraphics().drawImage(src, 0, 0, null);
        return img;
    }

    public static byte[] bgrToRgb(byte[] bgr) {
        byte[] rgb = new byte[bgr.length];
        for (int i = 0; i < bgr.length; i += 3) {
            rgb[i] = bgr[i + 2];
            rgb[i + 1] = bgr[i + 1];
            rgb[i + 2] = bgr[i];
        }
        return rgb;
    }

    public static byte[] rgbToBgr(byte[] rgb) {
        byte[] bgr = new byte[rgb.length];
        for (int i = 0; i < rgb.length; i += 3) {
            bgr[i] = rgb[i + 2];
            bgr[i + 1] = rgb[i + 1];
            bgr[i + 2] = rgb[i];
        }
        return bgr;
    }

    public static BufferedImage buildImage(byte[] bgr, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        byte[] target = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
        System.arraycopy(bgr, 0, target, 0, bgr.length);
        return img;
    }

}
