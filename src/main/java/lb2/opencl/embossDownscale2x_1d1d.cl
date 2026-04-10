uchar embossAt(
    __global const uchar* input,
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

    return (uchar)sum;
}

__kernel void embossDownscale2x_1d1d(
    __global const uchar* input,
    __global uchar* output,
    const int width,
    const int height,
    const int outWidth,
    const int outHeight)
{
    int localId = get_local_id(0);
    int groupId = get_group_id(0);
    int localSize = get_local_size(0);

    int gid = groupId * localSize + localId;

    int total = outWidth * outHeight;
    if (gid >= total) return;

    int ox = gid % outWidth;
    int oy = gid / outWidth;

    int x0 = ox * 2;
    int y0 = oy * 2;

    int outIdx = (oy * outWidth + ox) * 3;

    for (int c = 0; c < 3; c++) {
        int p00 = (int)embossAt(input, width, height, x0,     y0,     c);
        int p10 = (int)embossAt(input, width, height, x0 + 1, y0,     c);
        int p01 = (int)embossAt(input, width, height, x0,     y0 + 1, c);
        int p11 = (int)embossAt(input, width, height, x0 + 1, y0 + 1, c);

        output[outIdx + c] = (uchar)((p00 + p10 + p01 + p11) / 4);
    }
}