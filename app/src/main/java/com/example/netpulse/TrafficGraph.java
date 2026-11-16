package com.example.netpulse;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.ArrayList;

final class TrafficGraph extends View {
    private static final class Point {
        final long time;
        final double rate, motion;
        Point(long time, double rate, double motion) {
            this.time = time; this.rate = rate; this.motion = motion;
        }
    }
    private final ArrayList<Point> values = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    TrafficGraph(Context context) {
        super(context);
        setContentDescription("Aynı zaman çizelgesinde gelen trafik ve görüntü değişimi");
    }
    void add(long time, double rate, double motion) {
        values.add(new Point(time, rate, motion));
        if (values.size() > 60) values.remove(0);
        invalidate();
    }
    void clear() { values.clear(); invalidate(); }
    double peak() {
        double result = 1000;
        for (Point p : values) if (Double.isFinite(p.rate)) result = Math.max(result, p.rate);
        return result;
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float inset = Ui.dp(getContext(), 5);
        float top = inset, bottom = getHeight() - inset;
        float left = inset, width = getWidth() - 2 * inset;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(getContext(), 1));
        paint.setColor(Color.rgb(48, 65, 83));
        for (int i = 0; i < 3; i++) {
            float y = top + (bottom - top) * i / 2;
            canvas.drawLine(left, y, left + width, y, paint);
        }
        if (values.isEmpty()) return;
        long first = values.get(0).time;
        long span = Math.max(1000, values.get(values.size() - 1).time - first);
        for (int series = 0; series < 2; series++) {
            path.reset();
            boolean connected = false;
            double max = series == 0 ? peak() : 100;
            paint.setColor(series == 0 ? Ui.GREEN : Color.rgb(95, 170, 255));
            paint.setStrokeWidth(Ui.dp(getContext(), 2));
            for (Point p : values) {
                double v = series == 0 ? p.rate : p.motion;
                if (!Double.isFinite(v)) { connected = false; continue; }
                float x = left + (p.time - first) / (float) span * width;
                float y = bottom - (float) Math.min(1, v / max) * (bottom - top);
                if (connected) path.lineTo(x, y); else path.moveTo(x, y);
                connected = true;
                canvas.drawCircle(x, y, Ui.dp(getContext(), 1), paint);
            }
            canvas.drawPath(path, paint);
        }
    }
}
