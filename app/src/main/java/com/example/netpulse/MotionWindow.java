package com.example.netpulse;

import java.util.ArrayDeque;

/** Main-thread samples, aligned with each actual network-counter interval. */
public final class MotionWindow {
    private static final class Point {
        final long time;
        final double percent;
        Point(long time, double percent) { this.time = time; this.percent = percent; }
    }
    public static final class Summary {
        public final double percent;
        public final int samples;
        Summary(double percent, int samples) { this.percent = percent; this.samples = samples; }
    }
    private final ArrayDeque<Point> points = new ArrayDeque<>();
    public void add(long time, double percent) {
        if (!Double.isFinite(percent)) return;
        points.addLast(new Point(time, Math.max(0, Math.min(100, percent))));
        while (points.size() > 64) points.removeFirst();
    }
    public Summary take(long startExclusive, long endInclusive) {
        double sum = 0;
        int count = 0;
        while (!points.isEmpty() && points.peekFirst().time <= endInclusive) {
            Point p = points.removeFirst();
            if (p.time > startExclusive) { sum += p.percent; count++; }
        }
        return new Summary(count == 0 ? Double.NaN : sum / count, count);
    }
    public void clear() { points.clear(); }
}
