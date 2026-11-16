package com.example.netpulse;

import java.nio.ByteBuffer;

/** Small in-memory RGB grid. No images, network payloads, or files are retained. */
public final class MotionMath {
    public static final int COLS = 48, ROWS = 32;
    public static final int CHANNEL_THRESHOLD = 24;

    public static final class Region {
        public final double left, top, right, bottom;
        public Region(double left, double top, double right, double bottom) {
            if (!Double.isFinite(left) || !Double.isFinite(top) ||
                    !Double.isFinite(right) || !Double.isFinite(bottom))
                throw new IllegalArgumentException("Non-finite region");
            this.left = Math.max(0, Math.min(1, left));
            this.top = Math.max(0, Math.min(1, top));
            this.right = Math.max(0, Math.min(1, right));
            this.bottom = Math.max(0, Math.min(1, bottom));
            if (this.right <= this.left || this.bottom <= this.top)
                throw new IllegalArgumentException("Empty region");
        }
        public boolean contains(double x, double y) {
            return x >= left && x <= right && y >= top && y <= bottom;
        }
    }
    public static final Region DEFAULT = new Region(.1, .25, .9, .65);

    public static int[] grid(ByteBuffer rgba, int width, int height,
            int rowStride, int pixelStride, Region area, Region excluded) {
        long required = (long) (height - 1) * rowStride +
                (long) (width - 1) * pixelStride + 3;
        if (width <= 0 || height <= 0 || pixelStride < 3 ||
                rowStride < (long) width * pixelStride ||
                required > rgba.remaining())
            throw new IllegalArgumentException("Invalid RGBA plane");
        int base = rgba.position();
        int[] grid = new int[COLS * ROWS];
        for (int y = 0; y < ROWS; y++) {
            double ny = area.top + (y + .5) / ROWS * (area.bottom - area.top);
            for (int x = 0; x < COLS; x++) {
                double nx = area.left + (x + .5) / COLS * (area.right - area.left);
                int index = y * COLS + x;
                if (excluded != null && excluded.contains(nx, ny)) {
                    grid[index] = -1;
                    continue;
                }
                int px = Math.min(width - 1, (int) (nx * width));
                int py = Math.min(height - 1, (int) (ny * height));
                int offset = base + py * rowStride + px * pixelStride;
                grid[index] = ((rgba.get(offset) & 255) << 16) |
                        ((rgba.get(offset + 1) & 255) << 8) |
                        (rgba.get(offset + 2) & 255);
            }
        }
        return grid;
    }
    public static double percent(int[] previous, int[] current) {
        if (previous == null || current == null ||
                previous.length != current.length) return Double.NaN;
        int valid = 0, changed = 0;
        for (int i = 0; i < current.length; i++) {
            int a = previous[i], b = current[i];
            if (a == -1 || b == -1) continue;
            valid++;
            int r = Math.abs(((a >> 16) & 255) - ((b >> 16) & 255));
            int g = Math.abs(((a >> 8) & 255) - ((b >> 8) & 255));
            int bl = Math.abs((a & 255) - (b & 255));
            if (Math.max(r, Math.max(g, bl)) >= CHANNEL_THRESHOLD) changed++;
        }
        return valid < 16 ? Double.NaN : 100.0 * changed / valid;
    }
}
