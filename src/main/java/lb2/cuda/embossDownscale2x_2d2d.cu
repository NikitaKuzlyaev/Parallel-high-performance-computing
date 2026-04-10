__device__ unsigned char embossAt(
    const unsigned char* input,
    int width,
    int height,
    int x,
    int y,
    int c
) {
    if (x == 0 || y == 0 || x == width - 1 || y == height - 1) {
        int idx = (y * width + x) * 3 + c;
        return input[idx];
    }

    const int k[3][3] = {
        {-2, -1, 0},
        {-1,  1, 1},
        { 0,  1, 2}
    };

    int sum = 0;

    for (int ky = -1; ky <= 1; ky++) {
        for (int kx = -1; kx <= 1; kx++) {
            int nx = x + kx;
            int ny = y + ky;
            int nidx = (ny * width + nx) * 3 + c;
            sum += k[ky + 1][kx + 1] * (int)input[nidx];
        }
    }

    sum += 128;

    if (sum < 0) sum = 0;
    if (sum > 255) sum = 255;

    return (unsigned char)sum;
}

extern "C"
__global__ void embossDownscale2x(
    const unsigned char* input,
    unsigned char* output,
    int width,
    int height,
    int outWidth,
    int outHeight
) {
    int ox = blockIdx.x * blockDim.x + threadIdx.x;
    int oy = blockIdx.y * blockDim.y + threadIdx.y;

    if (ox >= outWidth || oy >= outHeight) return;

    int x0 = ox * 2;
    int y0 = oy * 2;
    int outIdx = (oy * outWidth + ox) * 3;

    for (int c = 0; c < 3; c++) {
        int p00 = embossAt(input, width, height, x0,     y0,     c);
        int p10 = embossAt(input, width, height, x0 + 1, y0,     c);
        int p01 = embossAt(input, width, height, x0,     y0 + 1, c);
        int p11 = embossAt(input, width, height, x0 + 1, y0 + 1, c);

        output[outIdx + c] = (unsigned char)((p00 + p10 + p01 + p11) / 4);
    }
}