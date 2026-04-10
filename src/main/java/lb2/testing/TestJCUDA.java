package lb2.testing;

import jcuda.driver.JCudaDriver;

public class TestJCUDA {

    public static void main(String[] args) {

        JCudaDriver.setExceptionsEnabled(true);
        JCudaDriver.cuInit(0); // обычная инициализация
        
        int[] count = {0};
        JCudaDriver.cuDeviceGetCount(count);

        System.out.println("CUDA Devices:" + count[0]);
        // <<< CUDA Devices:1
        
    }

}
