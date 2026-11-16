package com.example.netpulse;

import android.content.Context;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;

/** A visible, user-controlled selection overlay; it never injects touches into other apps. */
final class RegionPicker extends View {
    interface Listener {
        void onSelected(RectF screenPixels);
        void onCancel();
    }
    private final Listener listener;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF selection = new RectF(), cancel = new RectF(), accept = new RectF();
    private float startX, startY;
    private int gesture;
    RegionPicker(Context context, Listener listener) {
        super(context);
        this.listener = listener;
        setContentDescription("Oyun alanını sürükleyerek çiz. Üstten Kullan veya İptal seç.");
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        selection.set(w * .1f, h * .3f, w * .9f, h * .65f);
        int inset = getRootWindowInsets() == null ? Ui.dp(getContext(), 28) :
                getRootWindowInsets().getSystemWindowInsetTop() + Ui.dp(getContext(), 10);
        cancel.set(Ui.dp(getContext(), 12), inset, w * .42f, inset + Ui.dp(getContext(), 48));
        accept.set(w * .58f, inset, w - Ui.dp(getContext(), 12), cancel.bottom);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(0x66000000);
        canvas.drawRect(0, 0, getWidth(), selection.top, paint);
        canvas.drawRect(0, selection.bottom, getWidth(), getHeight(), paint);
        canvas.drawRect(0, selection.top, selection.left, selection.bottom, paint);
        canvas.drawRect(selection.right, selection.top, getWidth(), selection.bottom, paint);
        paint.setColor(Ui.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Ui.dp(getContext(), 3));
        canvas.drawRect(selection, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Ui.CARD);
        canvas.drawRoundRect(cancel, 12, 12, paint);
        paint.setColor(Ui.GREEN);
        canvas.drawRoundRect(accept, 12, 12, paint);
        paint.setTextSize(Ui.dp(getContext(), 16));
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.WHITE);
        canvas.drawText("İptal", cancel.centerX(), cancel.centerY() + Ui.dp(getContext(), 6), paint);
        paint.setColor(Ui.BG);
        canvas.drawText("Kullan", accept.centerX(), accept.centerY() + Ui.dp(getContext(), 6), paint);
        paint.setTextSize(Ui.dp(getContext(), 13));
        paint.setColor(Color.WHITE);
        canvas.drawText("İzlenecek alanı parmağınla çiz",
                getWidth() / 2f, cancel.bottom + Ui.dp(getContext(), 27), paint);
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        float x = Math.max(0, Math.min(getWidth(), event.getX()));
        float y = Math.max(0, Math.min(getHeight(), event.getY()));
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                gesture = cancel.contains(x, y) ? 1 : accept.contains(x, y) ? 2 : 0;
                startX = x; startY = y;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (gesture == 0) {
                    selection.set(Math.min(startX, x), Math.min(startY, y),
                            Math.max(startX, x), Math.max(startY, y));
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
                performClick();
                if (gesture == 1 && cancel.contains(x, y)) listener.onCancel();
                if (gesture == 2 && accept.contains(x, y) &&
                        selection.width() >= Ui.dp(getContext(), 24) &&
                        selection.height() >= Ui.dp(getContext(), 24)) {
                    int[] origin = new int[2];
                    getLocationOnScreen(origin);
                    RectF screen = new RectF(selection);
                    screen.offset(origin[0], origin[1]);
                    listener.onSelected(screen);
                }
                return true;
            case MotionEvent.ACTION_CANCEL: return true;
            default: return super.onTouchEvent(event);
        }
    }
    @Override public boolean performClick() {
        super.performClick();
        return true;
    }
}
