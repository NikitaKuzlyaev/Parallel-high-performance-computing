package lb2.cuda;

import jcuda.driver.*;
import lb2.GpuFrameProcessor;
import jcuda.Pointer;

import static jcuda.driver.JCudaDriver.*;

public class CudaFrameProcessor2d2d implements GpuFrameProcessor {

    private final CUcontext context;
    private final CUmodule module;
    private final CUfunction function;

    public CudaFrameProcessor2d2d(String ptxPath) {
        JCudaDriver.setExceptionsEnabled(true);

        cuInit(0);

        CUdevice device = new CUdevice();
        cuDeviceGet(device, 0);

        context = new CUcontext();
        cuCtxCreate(context, 0, device);

        module = new CUmodule();
        cuModuleLoad(module, ptxPath);

        function = new CUfunction();
        cuModuleGetFunction(function, module, "embossDownscale2x");
    }

    @Override
    public synchronized byte[] process(byte[] input, int width, int height) {
        if (input.length != width * height * 3) {
            throw new IllegalArgumentException("input length does not match width*height*3");
        }

        int outWidth = width / 2;
        int outHeight = height / 2;

        byte[] output = new byte[outWidth * outHeight * 3];

        CUdeviceptr dInput = new CUdeviceptr();
        CUdeviceptr dOutput = new CUdeviceptr();

        cuCtxPushCurrent(context);
        try {
            cuMemAlloc(dInput, input.length);
            cuMemAlloc(dOutput, output.length);

            cuMemcpyHtoD(dInput, Pointer.to(input), input.length);

            Pointer kernelParams = Pointer.to(
                    Pointer.to(dInput),
                    Pointer.to(dOutput),
                    Pointer.to(new int[]{width}),
                    Pointer.to(new int[]{height}),
                    Pointer.to(new int[]{outWidth}),
                    Pointer.to(new int[]{outHeight})
            );

            int blockX = 16;
            int blockY = 16;

            int gridX = (outWidth + blockX - 1) / blockX;
            int gridY = (outHeight + blockY - 1) / blockY;

            cuLaunchKernel(
                    function,
                    gridX, gridY, 1,
                    blockX, blockY, 1,
                    0, null,
                    kernelParams, null
            );

            cuCtxSynchronize();
            cuMemcpyDtoH(Pointer.to(output), dOutput, output.length);

            return output;

        } finally {
            if (dInput != null) cuMemFree(dInput);
            if (dOutput != null) cuMemFree(dOutput);

            CUcontext prev = new CUcontext();
            cuCtxPopCurrent(prev);
        }
    }

    public void close() {
        cuModuleUnload(module);
        cuCtxDestroy(context);
    }

}

