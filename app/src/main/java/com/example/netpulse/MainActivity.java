package com.example.netpulse;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.*;

public final class MainActivity extends Activity {
    private TextView status;
    private boolean pendingStart;
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
                "NetPulse 0.1 · Android 8+",
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
        status.setText(MeterService.running ? "● Ölçüm açık" :
                (Settings.canDrawOverlays(this) ?
                        "● Başlatmaya hazır" : "○ Önce pencere iznini aç"));
    }
    @Override protected void onResume() {
        super.onResume();
        handler.removeCallbacks(refresh);
        handler.post(refresh);
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
        super.onSaveInstanceState(state);
    }
}
