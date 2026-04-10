package lb2;

import common.FrameTask;
import common.FrameCapsule;
import common.FrameResult;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import static common.BufferedImage2Bytes.*;

public class ApplyConvolutionalMatrixTask implements FrameTask {

    private final GpuFrameProcessor gpuFrameProcessor;

    ApplyConvolutionalMatrixTask(GpuFrameProcessor gpuFrameProcessor){
        this.gpuFrameProcessor = gpuFrameProcessor;
    }

    @Override
    public FrameResult process(FrameCapsule frame) {
        BufferedImage input = frame.getImage();
        BufferedImage image = ensure3ByteBGR(input);

        int width = image.getWidth();
        int height = image.getHeight();

        byte[] bgr = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        // BGR → RGB
        byte[] rgb = bgrToRgb(bgr);
        // GPU обработка
        byte[] resultRgb = gpuFrameProcessor.process(rgb, width, height);
        // RGB → BGR

        byte[] resultBgr = rgbToBgr(resultRgb);

        BufferedImage resultImage = buildImage(resultBgr, width / 2, height / 2);
        return new FrameResult(frame.getFrameNumber(), resultImage, null);
    }
}
