package lb2.opencl;

import lb2.GpuFrameProcessor;
import org.jocl.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.jocl.CL.*;

public class OpenClFrameProcessor2d2d implements GpuFrameProcessor {

    private final cl_context context;
    private final cl_command_queue commandQueue;
    private final cl_program program;
    private final cl_kernel kernel;
    private final cl_device_id device;

    public OpenClFrameProcessor2d2d(String kernelSource) throws IOException {

        String src = new String(
                Files.readAllBytes(Paths.get(kernelSource))
        );

        CL.setExceptionsEnabled(true);

        // Инициализация платформы и устройства
        int[] numPlatformsArray = new int[1];
        clGetPlatformIDs(0, null, numPlatformsArray);
        cl_platform_id[] platforms = new cl_platform_id[numPlatformsArray[0]];
        clGetPlatformIDs(platforms.length, platforms, null);

        cl_platform_id platform = platforms[0];

        int[] numDevicesArray = new int[1];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, 0, null, numDevicesArray);
        cl_device_id[] devices = new cl_device_id[numDevicesArray[0]];
        clGetDeviceIDs(platform, CL_DEVICE_TYPE_GPU, devices.length, devices, null);

        device = devices[0];

        // Контекст
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL_CONTEXT_PLATFORM, platform);

        context = clCreateContext(
                contextProperties,
                1,
                new cl_device_id[]{device},
                null,
                null,
                null
        );

        // Очередь команд
        commandQueue = clCreateCommandQueue(context, device, 0, null);

        // Программа
        program = clCreateProgramWithSource(
                context,
                1,
                new String[]{src},
                null,
                null
        );

        clBuildProgram(program, 0, null, null, null, null);

        // Ядро
        kernel = clCreateKernel(program, "embossDownscale2x_2d2d", null);
    }

    @Override
    public synchronized byte[] process(byte[] input, int width, int height) {
        if (input.length != width * height * 3) {
            throw new IllegalArgumentException("input length does not match width*height*3");
        }

        int outWidth = width / 2;
        int outHeight = height / 2;

        byte[] output = new byte[outWidth * outHeight * 3];

        cl_mem inputMem = null;
        cl_mem outputMem = null;

        try {
            inputMem = clCreateBuffer(
                    context,
                    CL_MEM_READ_ONLY | CL_MEM_COPY_HOST_PTR,
                    input.length * Sizeof.cl_uchar,
                    Pointer.to(input),
                    null
            );

            outputMem = clCreateBuffer(
                    context,
                    CL_MEM_WRITE_ONLY,
                    output.length * Sizeof.cl_uchar,
                    null,
                    null
            );

            clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(inputMem));
            clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(outputMem));
            clSetKernelArg(kernel, 2, Sizeof.cl_int, Pointer.to(new int[]{width}));
            clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{height}));
            clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{outWidth}));
            clSetKernelArg(kernel, 5, Sizeof.cl_int, Pointer.to(new int[]{outHeight}));

            int localX = 16;
            int localY = 16;

            long globalX = ((outWidth + localX - 1) / localX) * localX;
            long globalY = ((outHeight + localY - 1) / localY) * localY;

            clEnqueueNDRangeKernel(
                    commandQueue,
                    kernel,
                    2,
                    null,
                    new long[]{globalX, globalY},
                    new long[]{localX, localY},
                    0,
                    null,
                    null
            );

            clFinish(commandQueue);

            clEnqueueReadBuffer(
                    commandQueue,
                    outputMem,
                    CL_TRUE,
                    0,
                    output.length * Sizeof.cl_uchar,
                    Pointer.to(output),
                    0,
                    null,
                    null
            );

            return output;

        } finally {
            if (inputMem != null) clReleaseMemObject(inputMem);
            if (outputMem != null) clReleaseMemObject(outputMem);
        }
    }

    public void close() {
        clReleaseKernel(kernel);
        clReleaseProgram(program);
        clReleaseCommandQueue(commandQueue);
        clReleaseContext(context);
    }

}
