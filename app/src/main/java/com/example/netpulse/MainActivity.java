package com.example.netpulse;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.media.projection.MediaProjectionManager;
import android.media.projection.MediaProjectionConfig;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.*;

public final class MainActivity extends Activity {
    private TextView status;
    private boolean pendingStart, pendingScreen;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            updateStatus();
            handler.postDelayed(this, 1000);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        pendingStart = saved != null && saved.getBoolean("pendingStart");
        pendingScreen = saved != null && saved.getBoolean("pendingScreen");

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BG);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        int pad = Ui.dp(this, 22);
        page.setPadding(pad, pad, pad, pad);
        scroll.addView(page);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            page.setPadding(pad + insets.getSystemWindowInsetLeft(),
                    pad + insets.getSystemWindowInsetTop(),
                    pad + insets.getSystemWindowInsetRight(),
                    pad + insets.getSystemWindowInsetBottom());
            return insets;
        });

        TextView brand = Ui.text(this, "NETPULSE", 14, Ui.GREEN);
        brand.setLetterSpacing(.18f);
        page.addView(brand);
        TextView title = Ui.text(this, "Trafik gözünün\nönünde.", 34, Color.WHITE);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 14));
        page.addView(title);
        page.addView(Ui.text(this,
                "Oyun açıkken gelen ve giden veriyi\ntaşınabilir pencerede izle.",
                16, Ui.MUTED));

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(pad, pad, pad, pad);
        card.setBackground(Ui.background(this, Ui.CARD, 20));
        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(-1, -2);
        cardParams.topMargin = Ui.dp(this, 28);
        cardParams.bottomMargin = Ui.dp(this, 20);
        page.addView(card, cardParams);
        status = Ui.text(this, "", 15, Ui.GREEN);
        card.addView(status);
        TextView features = Ui.text(this,
                "\n↓ Gelen / ↑ giden KB–MB/sn\n" +
                "Oturum toplamı ve IP paketleri\n" +
                "Son 60 örneğin trafik grafiği\n" +
                "Seçilen alanda görüntü değişimi %\n" +
                "Sürükle, küçült, sıfırla ve kapat",
                15, Color.WHITE);
        features.setLineSpacing(Ui.dp(this, 5), 1);
        card.addView(features);

        Button start = Ui.button(this, "Ölçümü başlat");
        start.setTextColor(Ui.BG);
        start.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(Ui.GREEN));
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                pendingStart = true;
                openOverlaySettings();
            } else startMeter();
        });
        page.addView(start, new LinearLayout.LayoutParams(-1, Ui.dp(this, 56)));

        Button scan = Ui.button(this, "Ekran + veri analizi başlat");
        scan.setOnClickListener(v -> beginScreenScan());
        page.addView(scan, new LinearLayout.LayoutParams(-1, -2));
        Button stopScan = Ui.button(this, "Yalnız ekran taramasını durdur");
        stopScan.setOnClickListener(v -> {
            if (MeterService.running) startService(
                    new Intent(this, MeterService.class).setAction("STOP_SCREEN"));
        });
        page.addView(stopScan, new LinearLayout.LayoutParams(-1, -2));
        TextView scanInfo = Ui.text(this,
                "Ekran analizinde Android izin penceresini onayla. " +
                "Oyununa dönüp kayan penceredeki Bölge seç ile alanı çiz. " +
                "Görüntüler cihazda, saniyede en çok 4 kez örneklenir; " +
                "kaydedilmez ve gönderilmez. Paylaşım izni her oturumda istenir.",
                12, Ui.MUTED);
        scanInfo.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 14));
        page.addView(scanInfo);

        Button permissions = Ui.button(this, "Pencere izni");
        permissions.setOnClickListener(v -> openOverlaySettings());
        page.addView(permissions, new LinearLayout.LayoutParams(-1, -2));
        Button notifications = Ui.button(this, "Bildirim izni");
        notifications.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 &&
                    checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                            != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{
                        Manifest.permission.POST_NOTIFICATIONS}, 10);
            } else Toast.makeText(this, "Bildirim izni hazır.",
                    Toast.LENGTH_SHORT).show();
        });
        page.addView(notifications, new LinearLayout.LayoutParams(-1, -2));
        Button stop = Ui.button(this, "Ölçümü durdur");
        stop.setOnClickListener(v -> {
            stopService(new Intent(this, MeterService.class));
            handler.postDelayed(this::updateStatus, 200);
        });
        page.addView(stop, new LinearLayout.LayoutParams(-1, -2));

        TextView note = Ui.text(this,
                "\nKapsam: tüm telefonun trafiği. Arka plandaki " +
                "uygulamalar ve sistem de dahildir.\n\n" +
                "Paket sayısı animasyon parçası sayısı değildir. " +
                "Animasyon adedi, boyutu ve süresi bu sayaçlardan " +
                "ayırt edilemez.\n\n" +
                "VPN/sanal arayüzler üst üste sayıma yol açabilir. " +
                "Fatura ölçümü değildir. 1 KB = 1000 B.\n\n" +
                "İçerik kaydetmez, internete veri göndermez. " +
                "NetPulse Ekran 0.2.1 · Android 8+",
                13, Ui.MUTED);
        note.setLineSpacing(Ui.dp(this, 3), 1);
        page.addView(note);
        setContentView(scroll);
    }

    private void openOverlaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
        } catch (RuntimeException e) {
            pendingStart = false;
            Toast.makeText(this,
                    "Uygulama ayarlarından üstte gösterme iznini aç.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void beginScreenScan() {
        if (MeterService.capturing) {
            Toast.makeText(this, "Tarama açık. Oyununa dönüp Bölge seç düğmesini kullan.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            pendingScreen = true;
            openOverlaySettings();
            return;
        }
        try {
            MediaProjectionManager manager = getSystemService(MediaProjectionManager.class);
            Intent request;
            if (Build.VERSION.SDK_INT >= 34)
                request = manager.createScreenCaptureIntent(
                        MediaProjectionConfig.createConfigForDefaultDisplay());
            else request = manager.createScreenCaptureIntent();
            startActivityForResult(request, 40);
        } catch (RuntimeException e) {
            Toast.makeText(this, "Ekran paylaşımı açılamadı.", Toast.LENGTH_LONG).show();
        }
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 40) return;
        if (result == RESULT_OK && data != null) {
            try {
                startForegroundService(new Intent(this, MeterService.class)
                        .setAction("CAPTURE").putExtra("projection", data)
                        .putExtra("resultCode", result));
                Toast.makeText(this, "Oyununa dön. Pencereden Bölge seç ile alanı çiz.",
                        Toast.LENGTH_LONG).show();
            } catch (RuntimeException e) {
                Toast.makeText(this, "Tarama başlatılamadı.", Toast.LENGTH_LONG).show();
            }
        } else Toast.makeText(this, "Ekran izni verilmedi; görüntü taranmıyor.",
                Toast.LENGTH_SHORT).show();
    }

    private void startMeter() {
        try {
            startForegroundService(new Intent(this, MeterService.class));
            Toast.makeText(this, "Pencere açılıyor; oyununa dönebilirsin.",
                    Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            Toast.makeText(this, "Başlatılamadı: " +
                    e.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
        updateStatus();
    }

    private void updateStatus() {
        if (status == null) return;
        status.setText(MeterService.running ?
                (MeterService.capturing ? "● Ekran + veri analizi açık" : "● Veri ölçümü açık") :
                (Settings.canDrawOverlays(this) ?
                        "● Başlatmaya hazır" : "○ Önce pencere iznini aç"));
    }
    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
        if (getIntent().getBooleanExtra("requestCapture", false)) {
            getIntent().removeExtra("requestCapture");
            beginScreenScan();
        }
        if (pendingScreen) {
            pendingScreen = false;
            if (Settings.canDrawOverlays(this)) beginScreenScan();
        }
        if (pendingStart) {
            pendingStart = false;
            if (Settings.canDrawOverlays(this)) startMeter();
        }
    }
    @Override protected void onPause() {
        handler.removeCallbacksAndMessages(null);
        super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putBoolean("pendingStart", pendingStart);
        state.putBoolean("pendingScreen", pendingScreen);
        super.onSaveInstanceState(state);
    }
}
