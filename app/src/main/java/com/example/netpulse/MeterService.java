package com.example.netpulse;

import android.app.*;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.TrafficStats;
import android.os.*;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import java.util.Locale;

public final class MeterService extends Service {
    public static volatile boolean running, capturing;
    private static final String CHANNEL = "netpulse_meter";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final TrafficEngine engine = new TrafficEngine();
    private WindowManager manager;
    private WindowManager.LayoutParams position;
    private LinearLayout panel, details;
    private ScrollView detailScroll;
    private TextView rxText, txText, totalsText, packetsText, graphText,
            stateText, compactText;
    private Button toggle;
    private TrafficGraph graph;
    private boolean compact, added, hasRate;
    private MotionCapture capture;
    private RegionPicker picker;
    private MotionMath.Region area = MotionMath.DEFAULT;
    private final MotionWindow motionWindow = new MotionWindow();
    private TextView motionText;
    private Button scanButton;
    private String screenState = "Ekran analizi kapalı";
    private double motionPercent = Double.NaN;
    private int motionSamples, captureGeneration, captureOrientation;
    private long lastTickAt;

    private long[] readCounters() {
        return new long[]{
            TrafficStats.getTotalRxBytes(), TrafficStats.getTotalTxBytes(),
            TrafficStats.getTotalRxPackets(), TrafficStats.getTotalTxPackets()
        };
    }

    private final Runnable sample = new Runnable() {
        @Override public void run() {
            if (!added) return;
            long now = SystemClock.elapsedRealtime();
            hasRate = engine.sample(now, readCounters());
            MotionWindow.Summary motion = motionWindow.take(lastTickAt, now);
            motionPercent = capturing ? motion.percent : Double.NaN;
            motionSamples = motion.samples;
            lastTickAt = now;
            graph.add(now, hasRate ? engine.rxRate : Double.NaN, motionPercent);
            updateExclusion();
            render();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP".equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (intent != null && "STOP_SCREEN".equals(intent.getAction())) {
            stopCapture("Ekran analizi durduruldu", true);
            if (!added) stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            Intent consent = intent == null ? null : intent.getParcelableExtra("projection");
            boolean startScreen = intent != null && "CAPTURE".equals(intent.getAction()) &&
                    intent.getIntExtra("resultCode", 0) == Activity.RESULT_OK && consent != null;
            showNotification(capturing || startScreen);
            if (panel == null) {
                createPanel();
                reset();
                running = true;
                handler.postDelayed(sample, 1000);
            }
            if (startScreen) {
                try { startCapture(Activity.RESULT_OK, consent); }
                catch (RuntimeException error) {
                    stopCapture("Ekran analizi açılamadı; yeniden başlat.", true);
                    Toast.makeText(this, screenState, Toast.LENGTH_LONG).show();
                }
            }
        } catch (RuntimeException e) {
            Toast.makeText(this, "Pencere başlatılamadı: " +
                    e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void showNotification(boolean screen) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL, "Canlı trafik ölçümü", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("NetPulse kayan penceresi açıkken gösterilir.");
        nm.createNotificationChannel(channel);
        PendingIntent stop = PendingIntent.getService(this, 1,
                new Intent(this, MeterService.class).setAction("STOP"),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent open = PendingIntent.getActivity(this, 2,
                new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(this, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat)
                .setContentTitle("NetPulse ölçümü açık")
                .setContentText(screen ? "Ekran + veri analizi açık · görüntüler kaydedilmez" :
                        "Tüm telefonun trafiği · pencere açık")
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Durdur", stop).build())
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            int type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
            if (screen) type |= ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
            startForeground(1, n, type);
        } else if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, screen ?
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION : 0);
        } else startForeground(1, n);
    }

    private void createPanel() {
        manager = getSystemService(WindowManager.class);
        SharedPreferences preferences = getSharedPreferences("window", MODE_PRIVATE);
        compact = preferences.getBoolean("compact", false);

        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(12), dp(7), dp(12), dp(10));
        panel.setBackground(Ui.background(this, Ui.CARD, 16));
        panel.setElevation(dp(8));
        panel.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.text(this, "NETPULSE  ⠿", 14, Ui.GREEN);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setContentDescription("NetPulse pencere taşıma alanı");
        title.setPadding(0, dp(10), 0, dp(10));
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        toggle = Ui.button(this, "−");
        toggle.setContentDescription("Pencereyi küçült veya büyüt");
        toggle.setMinWidth(0);
        toggle.setMinimumWidth(0);
        toggle.setPadding(0, 0, 0, 0);
        toggle.setOnClickListener(v -> setCompact(!compact));
        header.addView(toggle, new LinearLayout.LayoutParams(dp(44), dp(44)));

        Button close = Ui.button(this, "×");
        close.setContentDescription("Ölçümü kapat");
        close.setMinWidth(0);
        close.setMinimumWidth(0);
        close.setPadding(0, 0, 0, 0);
        close.setOnClickListener(v -> stopSelf());
        header.addView(close, new LinearLayout.LayoutParams(dp(44), dp(44)));
        panel.addView(header);

        compactText = Ui.text(this, "", 14, Color.WHITE);
        panel.addView(compactText);
        details = new LinearLayout(this);
        details.setOrientation(LinearLayout.VERTICAL);
        detailScroll = new ScrollView(this);
        detailScroll.setFillViewport(false);
        detailScroll.addView(details);
        panel.addView(detailScroll, new LinearLayout.LayoutParams(-1, dp(420)));

        stateText = Ui.text(this, "", 11, Ui.MUTED);
        details.addView(stateText);
        rxText = Ui.text(this, "", 25, Ui.GREEN);
        rxText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        rxText.setPadding(0, dp(10), 0, dp(3));
        details.addView(rxText);
        txText = Ui.text(this, "", 15, Color.WHITE);
        details.addView(txText);
        totalsText = Ui.text(this, "", 13, Color.WHITE);
        totalsText.setPadding(0, dp(12), 0, dp(8));
        details.addView(totalsText);
        packetsText = Ui.text(this, "", 12, Ui.MUTED);
        details.addView(packetsText);

        motionText = Ui.text(this, "", 12, Color.rgb(95, 170, 255));
        motionText.setPadding(0, dp(10), 0, 0);
        details.addView(motionText);
        graph = new TrafficGraph(this);
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, dp(58));
        gp.topMargin = dp(10);
        details.addView(graph, gp);
        graphText = Ui.text(this, "", 10, Ui.MUTED);
        details.addView(graphText);

        TextView note = Ui.text(this,
                "Tüm telefon · paket ≠ animasyon\n" +
                "Animasyon adedi/boyutu: ayırt edilemiyor",
                10, Ui.MUTED);
        note.setPadding(0, dp(10), 0, dp(5));
        details.addView(note);
        LinearLayout screenActions = new LinearLayout(this);
        scanButton = Ui.button(this, "Taramayı aç");
        scanButton.setOnClickListener(v -> {
            if (capturing) stopCapture("Ekran analizi durduruldu", true);
            else startActivity(new Intent(this, MainActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra("requestCapture", true));
        });
        screenActions.addView(scanButton, new LinearLayout.LayoutParams(0, dp(46), 1));
        Button regionButton = Ui.button(this, "Bölge seç");
        regionButton.setOnClickListener(v -> chooseRegion());
        screenActions.addView(regionButton, new LinearLayout.LayoutParams(0, dp(46), 1));
        details.addView(screenActions);
        Button reset = Ui.button(this, "Sayaçları sıfırla");
        reset.setOnClickListener(v -> reset());
        details.addView(reset, new LinearLayout.LayoutParams(-1, dp(46)));

        position = new WindowManager.LayoutParams(
                dp(292), WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        position.gravity = Gravity.TOP | Gravity.LEFT;
        position.setTitle("NetPulse network meter");
        position.x = preferences.getInt("x", dp(8));
        position.y = preferences.getInt("y", dp(80));
        title.setOnTouchListener(new View.OnTouchListener() {
            private float touchX, touchY;
            private int initialX, initialY;
            @Override public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        initialX = position.x;
                        initialY = position.y;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        position.x = initialX + Math.round(event.getRawX() - touchX);
                        position.y = initialY + Math.round(event.getRawY() - touchY);
                        clampPosition();
                        if (added) manager.updateViewLayout(panel, position);
                        updateExclusion();
                        return true;
                    case MotionEvent.ACTION_UP:
                        savePosition();
                        view.performClick();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        savePosition();
                        return true;
                    default:
                        return false;
                }
            }
        });
        applyCompact();
        clampPosition();
        manager.addView(panel, position);
        added = true;
        panel.post(this::reposition);
    }

    private int dp(int n) { return Ui.dp(this, n); }
    private void applyCompact() {
        detailScroll.setVisibility(compact ? View.GONE : View.VISIBLE);
        compactText.setVisibility(compact ? View.VISIBLE : View.GONE);
        toggle.setText(compact ? "+" : "−");
    }
    private void setCompact(boolean value) {
        compact = value;
        applyCompact();
        savePosition();
        panel.post(this::reposition);
    }
    private void clampPosition() {
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(metrics);
        position.width = Math.min(dp(312), Math.max(dp(120), metrics.widthPixels - dp(16)));
        detailScroll.getLayoutParams().height = Math.min(dp(450),
                Math.max(dp(120), metrics.heightPixels - dp(180)));
        int height = panel == null ? dp(100) : Math.max(dp(80), panel.getHeight());
        position.x = Math.max(0, Math.min(position.x, metrics.widthPixels - position.width));
        position.y = Math.max(0, Math.min(position.y,
                Math.max(0, metrics.heightPixels - height - dp(24))));
    }
    private void reposition() {
        if (!added) return;
        clampPosition();
        manager.updateViewLayout(panel, position);
        panel.post(this::updateExclusion);
    }
    private void savePosition() {
        if (position == null) return;
        getSharedPreferences("window", MODE_PRIVATE).edit()
                .putInt("x", position.x).putInt("y", position.y)
                .putBoolean("compact", compact).apply();
    }
    private void reset() {
        engine.reset();
        hasRate = false;
        graph.clear();
        lastTickAt = SystemClock.elapsedRealtime();
        engine.sample(lastTickAt, readCounters());
        motionWindow.clear();
        motionPercent = Double.NaN;
        motionSamples = 0;
        if (capture != null) capture.resetBaseline();
        render();
    }
    private void render() {
        String rx = hasRate ? TrafficEngine.bytes(engine.rxRate) : "—";
        String tx = hasRate ? TrafficEngine.bytes(engine.txRate) : "—";
        stateText.setText(engine.status);
        rxText.setText("↓ " + rx + "/sn");
        txText.setText("↑ " + tx + "/sn");
        compactText.setText("↓ " + rx + "/sn  ↑ " + tx + "/sn");
        totalsText.setText("Toplam ↓ " + TrafficEngine.bytes(engine.rxTotal) +
                "\nToplam ↑ " + TrafficEngine.bytes(engine.txTotal));
        String packetRates = hasRate ? String.format(
                Locale.forLanguageTag("tr-TR"), "↓ %.1f   ↑ %.1f paket/sn",
                engine.rxPacketRate, engine.txPacketRate) : "Paket/sn: —";
        packetsText.setText(packetRates + "\nOturum paketleri ↓ " +
                engine.rxPackets + "  ↑ " + engine.txPackets);
        String change = Double.isFinite(motionPercent) ?
                String.format(Locale.forLanguageTag("tr-TR"), "%%%.1f", motionPercent) : "—";
        motionText.setText("Görüntü değişimi: " + change + "\n" +
                (capturing ? motionSamples + " örnek / son aralık · hedef en çok 4/sn" :
                        screenState) + "\n" +
                (capturing && Double.isFinite(motionPercent) && hasRate ?
                        ((engine.rxRate > 0 ? "Veri geldi" : "Gelen veri yok") + " · " +
                         (motionPercent >= 1 ? "görüntü hareketli" : "az değişim")) :
                        (capturing ? "Görüntü aralığı bekleniyor" : "Tarama için izin ver")));
        compactText.setText("↓ " + rx + "/sn · hareket " + change);
        scanButton.setText(capturing ? "Taramayı durdur" : "Taramayı aç");
        graphText.setText("Son 60 aralık · yeşil: veri · mavi: değişim\n" +
                "Veri üst sınırı " + TrafficEngine.bytes(graph.peak()) +
                "/sn · değişim üst sınırı %100");
        panel.post(this::updateExclusion);
    }
    @Override public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);
        if (capturing && config.orientation != captureOrientation)
            stopCapture("Ekran döndü. Taramayı yeniden başlat.", true);
        if (panel != null) panel.post(this::reposition);
    }
    @Override public void onDestroy() {
        running = false;
        stopCapture("Ekran analizi kapalı", false);
        handler.removeCallbacksAndMessages(null);
        savePosition();
        if (added && manager != null) {
            try { manager.removeView(panel); }
            catch (IllegalArgumentException ignored) { }
        }
        added = false;
        panel = null;
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    private void startCapture(int code, Intent consent) {
        stopCapture("Yeni ekran oturumu", false);
        final int generation = ++captureGeneration;
        area = MotionMath.DEFAULT;
        captureOrientation = getResources().getConfiguration().orientation;
        screenState = "İlk görüntü aralığı bekleniyor";
        capture = new MotionCapture(this, handler, new MotionCapture.Listener() {
            @Override public void onMotion(long time, double percent) {
                if (generation != captureGeneration || !capturing) return;
                motionWindow.add(time, percent);
            }
            @Override public void onEnded(String reason) {
                if (generation == captureGeneration) stopCapture(reason, true);
            }
        });
        capture.setArea(area);
        updateExclusion();
        capture.start(code, consent);
        capturing = true;
        reset();
    }
    private void stopCapture(String reason, boolean updateNotification) {
        ++captureGeneration;
        MotionCapture old = capture;
        capture = null;
        capturing = false;
        removePicker();
        if (old != null) old.stop();
        motionWindow.clear();
        motionPercent = Double.NaN;
        motionSamples = 0;
        screenState = reason;
        if (added) render();
        if (updateNotification && running) showNotification(false);
    }
    private void updateExclusion() {
        if (capture == null || panel == null || !added || picker != null) return;
        DisplayMetrics metrics = new DisplayMetrics();
        manager.getDefaultDisplay().getRealMetrics(metrics);
        int[] origin = new int[2];
        panel.getLocationOnScreen(origin);
        double left = Math.max(0, origin[0] - dp(12)) / (double) metrics.widthPixels;
        double top = Math.max(0, origin[1] - dp(12)) / (double) metrics.heightPixels;
        double right = Math.min(metrics.widthPixels, origin[0] + panel.getWidth() + dp(12)) /
                (double) metrics.widthPixels;
        double bottom = Math.min(metrics.heightPixels, origin[1] + panel.getHeight() + dp(12)) /
                (double) metrics.heightPixels;
        if (right > left && bottom > top)
            capture.setExcluded(new MotionMath.Region(left, top, right, bottom));
    }
    private void chooseRegion() {
        if (capture == null || !capturing) {
            Toast.makeText(this, "Önce Taramayı aç ile ekran iznini ver.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (picker != null) return;
        capture.pause(true);
        motionWindow.clear();
        motionPercent = Double.NaN;
        panel.setVisibility(View.INVISIBLE);
        picker = new RegionPicker(this, new RegionPicker.Listener() {
            @Override public void onSelected(RectF pixels) {
                DisplayMetrics metrics = new DisplayMetrics();
                manager.getDefaultDisplay().getRealMetrics(metrics);
                area = new MotionMath.Region(pixels.left / metrics.widthPixels,
                        pixels.top / metrics.heightPixels, pixels.right / metrics.widthPixels,
                        pixels.bottom / metrics.heightPixels);
                if (capture != null) capture.setArea(area);
                removePicker();
                reset();
            }
            @Override public void onCancel() { removePicker(); reset(); }
        });
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                -1, -1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.LEFT;
        try { manager.addView(picker, params); }
        catch (RuntimeException e) {
            picker = null;
            panel.setVisibility(View.VISIBLE);
            capture.pause(false);
            Toast.makeText(this, "Alan seçimi açılamadı.", Toast.LENGTH_LONG).show();
        }
    }
    private void removePicker() {
        if (picker != null && manager != null) {
            try { manager.removeView(picker); }
            catch (IllegalArgumentException ignored) { }
            picker = null;
        }
        if (panel != null) panel.setVisibility(View.VISIBLE);
        if (capture != null) capture.pause(false);
        updateExclusion();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
