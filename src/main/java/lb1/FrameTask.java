package lb1;

import org.bytedeco.javacv.Frame;

@FunctionalInterface
public interface FrameTask {

    FrameResult process(FrameCapsule frame);

}
