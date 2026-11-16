package com.example.netpulse;

import java.util.Locale;

/** No Android dependency: the sampler can be tested with synthetic counters. */
public final class TrafficEngine {
    public double rxRate, txRate, rxPacketRate, txPacketRate;
    public long rxTotal, txTotal, rxPackets, txPackets;
    public String status = "İlk örnek bekleniyor";
    private long[] previous;
    private long previousTime;

    public void reset() {
        previous = null;
        previousTime = 0;
        rxRate = txRate = rxPacketRate = txPacketRate = 0;
        rxTotal = txTotal = rxPackets = txPackets = 0;
        status = "İlk örnek bekleniyor";
    }

    private void clearRates() {
        rxRate = txRate = rxPacketRate = txPacketRate = 0;
    }

    /** Order: received bytes, sent bytes, received packets, sent packets. */
    public boolean sample(long elapsedMillis, long[] current) {
        if (current == null || current.length != 4) {
            previous = null;
            clearRates();
            status = "Sayaç okunamadı; bu aralık ölçülmedi";
            return false;
        }
        for (long value : current) {
            if (value < 0) {
                previous = null;
                clearRates();
                status = "Sayaç desteklenmiyor; bu aralık ölçülmedi";
                return false;
            }
        }
        if (previous == null) {
            previous = current.clone();
            previousTime = elapsedMillis;
            clearRates();
            status = "İlk aralık bekleniyor";
            return false;
        }
        long deltaTime = elapsedMillis - previousTime;
        boolean reset = deltaTime <= 0;
        for (int i = 0; i < 4; i++) reset |= current[i] < previous[i];
        if (reset) {
            previous = current.clone();
            previousTime = elapsedMillis;
            clearRates();
            status = "Sayaç yenilendi; bir aralık atlandı";
            return false;
        }
        long rx = current[0] - previous[0];
        long tx = current[1] - previous[1];
        long rp = current[2] - previous[2];
        long tp = current[3] - previous[3];
        double seconds = deltaTime / 1000.0;
        rxRate = rx / seconds;
        txRate = tx / seconds;
        rxPacketRate = rp / seconds;
        txPacketRate = tp / seconds;
        rxTotal += rx;
        txTotal += tx;
        rxPackets += rp;
        txPackets += tp;
        previous = current.clone();
        previousTime = elapsedMillis;
        status = "Canlı · tüm telefon";
        return true;
    }

    public static String bytes(double value) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unit = 0;
        while (value >= 1000 && unit < units.length - 1) {
            value /= 1000;
            unit++;
        }
        return String.format(Locale.forLanguageTag("tr-TR"), "%.2f %s", value, units[unit]);
    }
}
