package lb2;

public interface GpuFrameProcessor {
    byte[] process(byte[] input, int width, int height);
}
