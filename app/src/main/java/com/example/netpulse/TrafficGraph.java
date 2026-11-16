package com.example.netpulse;

import android.content.Context;
import android.graphics.*;
import android.view.View;
import java.util.ArrayList;

final class TrafficGraph extends View {
    private final ArrayList<Float> values = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    TrafficGraph(Context context) {
        super(context);
        setContentDescription("Son 60 ölçümde gelen trafik grafiği");
    }
    void add(double rate) {
        values.add((float) rate);
        if (values.size() > 60) values.remove(0);
        invalidate();
    }
    void clear() { values.clear(); invalidate(); }
    float peak() {
        float result = 1000;
        for (float value : values) result = Math.max(result, value);
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
        path.reset();
        float maximum = peak();
        for (int i = 0; i < values.size(); i++) {
            float x = left + i * width / 59;
            float y = bottom - values.get(i) / maximum * (bottom - top);
            if (i == 0) path.moveTo(x, y); else path.lineTo(x, y);
        }
        paint.setColor(Ui.GREEN);
        paint.setStrokeWidth(Ui.dp(getContext(), 2));
        if (values.size() == 1) {
            canvas.drawCircle(left, bottom - values.get(0) / maximum *
                    (bottom - top), Ui.dp(getContext(), 2), paint);
        } else canvas.drawPath(path, paint);
    }
}
