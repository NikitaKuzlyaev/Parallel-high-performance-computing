package lb1;

@FunctionalInterface
public interface FrameTask {
    FrameResult process(FrameCapsule frame);
}
