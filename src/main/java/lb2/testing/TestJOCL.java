package lb2.testing;

import org.jocl.CL;

public class TestJOCL {

    public static void main(String[] args) {

        CL.setExceptionsEnabled(true);

        int[] numPlatforms = {1};
        CL.clGetPlatformIDs(0, null, numPlatforms);

        System.out.println("Platforms: " + numPlatforms[0]);
        // <<< Platforms: 1
    }

}
