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
    if (gid >= total) {
        return;
    }

    int x = gid % outWidth;
    int y = gid / outWidth;

    int srcX = x * 2;
    int srcY = y * 2;

    // Индексы соседних пикселей для emboss
    int x1 = srcX;
    int y1 = srcY;
    int x2 = min(srcX + 1, width - 1);
    int y2 = min(srcY + 1, height - 1);

    int idx1 = (y1 * width + x1) * 3;
    int idx2 = (y2 * width + x2) * 3;

    int outIdx = (y * outWidth + x) * 3;

    for (int c = 0; c < 3; c++) {
        int v = (int)input[idx2 + c] - (int)input[idx1 + c] + 128;

        if (v < 0) v = 0;
        if (v > 255) v = 255;

        output[outIdx + c] = (uchar)v;
    }
}