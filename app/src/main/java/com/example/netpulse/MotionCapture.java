package com.example.netpulse;

import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import java.util.concurrent.atomic.AtomicBoolean;

/** Consented full-display capture; only a small RGB grid survives each Image.close(). */
final class MotionCapture {
    interface Listener {
        void onMotion(long time, double percent);
        void onEnded(String reason);
    }
    private final Context context;
    private final Handler main;
    private final Listener listener;
    private final AtomicBoolean resetRequested = new AtomicBoolean(true);
    private volatile MotionMath.Region area = MotionMath.DEFAULT, excluded;
    private volatile boolean active, paused;
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private HandlerThread thread;
    private Handler worker;
    private int[] previous;
    private long lastSample;
    private int screenWidth, screenHeight;

    MotionCapture(Context context, Handler main, Listener listener) {
        this.context = context;
        this.main = main;
        this.listener = listener;
    }
    private final MediaProjection.Callback callback = new MediaProjection.Callback() {
        @Override public void onStop() {
            if (active) listener.onEnded("Ekran paylaşımı durdu. Yeniden başlatabilirsin.");
        }
        @Override public void onCapturedContentResize(int width, int height) {
            if (active && (width != screenWidth || height != screenHeight))
                listener.onEnded("Ekran boyutu değişti. Taramayı yeniden başlat.");
        }
    };

    void start(int resultCode, Intent consent) {
        DisplayMetrics metrics = new DisplayMetrics();
        context.getSystemService(WindowManager.class).getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        int width = Math.min(360, screenWidth);
        int height = Math.max(1, Math.round(width * screenHeight / (float) screenWidth));
        MediaProjectionManager m = context.getSystemService(MediaProjectionManager.class);
        projection = m.getMediaProjection(resultCode, consent);
        if (projection == null) throw new IllegalStateException("Ekran izni alınamadı");
        projection.registerCallback(callback, main);
        thread = new HandlerThread("NetPulse-motion");
        thread.start();
        worker = new Handler(thread.getLooper());
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        reader.setOnImageAvailableListener(this::onImage, worker);
        active = true;
        display = projection.createVirtualDisplay("NetPulse local analysis",
                width, height, context.getResources().getConfiguration().densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.getSurface(), null, worker);
        if (display == null) throw new IllegalStateException("Ekran açılamadı");
    }

    void setArea(MotionMath.Region area) {
        this.area = area;
        resetRequested.set(true);
    }
    void setExcluded(MotionMath.Region excluded) { this.excluded = excluded; }
    void resetBaseline() { resetRequested.set(true); }
    void pause(boolean value) {
        paused = value;
        resetRequested.set(true);
    }
    private void onImage(ImageReader source) {
        Image image = null;
        try {
            image = source.acquireLatestImage();
            if (image == null || !active) return;
            if (paused) { previous = null; return; }
            long now = SystemClock.elapsedRealtime();
            if (now - lastSample < 250) return;
            boolean reset = resetRequested.getAndSet(false);
            if (reset || now - lastSample > 1500) previous = null;
            lastSample = now;
            Image.Plane p = image.getPlanes()[0];
            int[] current = MotionMath.grid(p.getBuffer(), image.getWidth(), image.getHeight(),
                    p.getRowStride(), p.getPixelStride(), area, excluded);
            double percent = MotionMath.percent(previous, current);
            previous = current;
            main.post(() -> { if (active && !paused) listener.onMotion(now, percent); });
        } catch (RuntimeException e) {
            if (active) {
                active = false;
                main.post(() -> listener.onEnded("Ekran örneği okunamadı; yeniden başlat."));
            }
        } finally {
            if (image != null) image.close();
        }
    }

    void stop() {
        active = false;
        MediaProjection oldProjection = projection;
        projection = null;
        if (oldProjection != null) {
            oldProjection.unregisterCallback(callback);
            oldProjection.stop();
        }
        if (worker != null) {
            VirtualDisplay oldDisplay = display;
            ImageReader oldReader = reader;
            HandlerThread oldThread = thread;
            worker.post(() -> {
                if (oldDisplay != null) oldDisplay.release();
                if (oldReader != null) oldReader.close();
                previous = null;
                oldThread.quitSafely();
            });
        }
        display = null;
        reader = null;
    }
}
