package com.example.netpulse;

import org.junit.Test;
import java.nio.ByteBuffer;
import java.util.Arrays;
import static org.junit.Assert.*;

public class MotionMathTest {
    @Test public void stillAndHalfChangedFramesAreDistinct() {
        int[] a = new int[100], b = new int[100];
        assertEquals(0, MotionMath.percent(a, b), .001);
        Arrays.fill(b, 0, 50, 0xff0000);
        assertEquals(50, MotionMath.percent(a, b), .001);
    }
    @Test public void colorChangesAndSmallNoiseAreHandled() {
        int[] a = new int[100], b = new int[100];
        Arrays.fill(b, 0x000017);
        assertEquals(0, MotionMath.percent(a, b), .001);
        Arrays.fill(b, 0x000018);
        assertEquals(100, MotionMath.percent(a, b), .001);
    }
    @Test public void overlayPixelsAreExcludedInBothFrames() {
        int[] a = new int[100], b = new int[100];
        Arrays.fill(a, 0, 40, -1);
        Arrays.fill(b, 30, 50, -1);
        Arrays.fill(b, 0, 30, 0xffffff);
        assertEquals(0, MotionMath.percent(a, b), .001);
        Arrays.fill(b, 50, 75, 0xffffff);
        assertEquals(50, MotionMath.percent(a, b), .001);
    }
    @Test public void missingPixelsAreUnknownNotZero() {
        int[] a = new int[100];
        Arrays.fill(a, -1);
        assertTrue(Double.isNaN(MotionMath.percent(a, a)));
        assertTrue(Double.isNaN(MotionMath.percent(null, a)));
    }
    @Test public void gridHonorsCropPaddingPixelStrideAndBufferPosition() {
        ByteBuffer b = ByteBuffer.allocate(4 + 24 * 4);
        b.position(4);
        for (int y = 0; y < 4; y++) for (int x = 0; x < 4; x++) {
            int p = 4 + y * 24 + x * 4;
            b.put(p, (byte) (x < 2 ? 255 : 0));
            b.put(p + 1, (byte) (x < 2 ? 0 : 255));
        }
        int[] left = MotionMath.grid(b, 4, 4, 24, 4,
                new MotionMath.Region(0, 0, .5, 1), null);
        for (int color : left) assertEquals(0xff0000, color);
        int[] right = MotionMath.grid(b, 4, 4, 24, 4,
                new MotionMath.Region(.5, 0, 1, 1), null);
        for (int color : right) assertEquals(0x00ff00, color);
        int[] covered = MotionMath.grid(b, 4, 4, 24, 4,
                new MotionMath.Region(0, 0, .5, 1),
                new MotionMath.Region(0, 0, .5, 1));
        for (int color : covered) assertEquals(-1, color);
    }
    @Test(expected=IllegalArgumentException.class)
    public void rejectsTruncatedPlane() {
        MotionMath.grid(ByteBuffer.allocate(5), 4, 4, 16, 4, MotionMath.DEFAULT, null);
    }
    @Test public void intervalsHaveNoDuplicatesAndMissingDataStaysUnknown() {
        MotionWindow w = new MotionWindow();
        w.add(100, 10); w.add(500, 20); w.add(1000, 30); w.add(1200, 40);
        assertEquals(25, w.take(100, 1000).percent, .001);
        MotionWindow.Summary next = w.take(1000, 1500);
        assertEquals(40, next.percent, .001);
        assertEquals(1, next.samples);
        assertTrue(Double.isNaN(w.take(1500, 2000).percent));
        w.add(2100, 50); w.clear();
        assertEquals(0, w.take(2000, 3000).samples);
    }
}
