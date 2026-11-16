package com.example.netpulse;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.Button;
import android.widget.TextView;

final class Ui {
    static final int BG = Color.rgb(12, 17, 25);
    static final int CARD = Color.rgb(21, 30, 42);
    static final int GREEN = Color.rgb(67, 229, 170);
    static final int MUTED = Color.rgb(163, 180, 199);

    static int dp(Context c, int value) {
        return Math.round(value * c.getResources().getDisplayMetrics().density);
    }
    static TextView text(Context c, String value, int size, int color) {
        TextView view = new TextView(c);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setIncludeFontPadding(false);
        return view;
    }
    static GradientDrawable background(Context c, int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(c, radius));
        return drawable;
    }
    static Button button(Context c, String value) {
        Button button = new Button(c);
        button.setText(value);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return button;
    }
}
