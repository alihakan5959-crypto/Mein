package com.example.netpulse;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrafficEngineTest {
    @Test public void usesActualElapsedTimeIncludingDelayedTicks() {
        TrafficEngine e = new TrafficEngine();
        assertFalse(e.sample(100, new long[]{1000, 50, 10, 2}));
        assertTrue(e.sample(2100, new long[]{5000, 1050, 14, 4}));
        assertEquals(2000, e.rxRate, .001);
        assertEquals(500, e.txRate, .001);
        assertEquals(2, e.rxPacketRate, .001);
        assertEquals(4000, e.rxTotal);
        assertEquals(4, e.rxPackets);
    }
    @Test public void unsupportedCounterIsNotConvertedIntoZeroTraffic() {
        TrafficEngine e = new TrafficEngine();
        e.sample(0, new long[]{100, 100, 1, 1});
        assertFalse(e.sample(1000, new long[]{-1, 200, 2, 2}));
        assertFalse(e.sample(2000, new long[]{2000, 2000, 20, 20}));
        assertEquals(0, e.rxTotal);
        assertTrue(e.sample(3000, new long[]{3000, 2500, 25, 25}));
        assertEquals(1000, e.rxTotal);
    }
    @Test public void counterDecreaseRebasesWithoutNegativeOrHugeTotals() {
        TrafficEngine e = new TrafficEngine();
        e.sample(0, new long[]{1000, 1000, 10, 10});
        e.sample(1000, new long[]{2000, 2000, 20, 20});
        assertFalse(e.sample(2000, new long[]{10, 10, 1, 1}));
        assertTrue(e.sample(3000, new long[]{510, 510, 3, 3}));
        assertEquals(1500, e.rxTotal);
        assertEquals(12, e.rxPackets);
    }
    @Test public void resetStartsANewSessionWithoutCountingBootTotals() {
        TrafficEngine e = new TrafficEngine();
        e.sample(0, new long[]{100, 100, 1, 1});
        e.sample(1000, new long[]{200, 200, 2, 2});
        e.reset();
        assertEquals(0, e.rxTotal);
        assertFalse(e.sample(2000, new long[]{1000000, 1000000, 900, 900}));
        assertTrue(e.sample(3000, new long[]{1000200, 1000100, 902, 901}));
        assertEquals(200, e.rxTotal);
        assertEquals(2, e.rxPackets);
    }
    @Test public void invalidTimeAndInvalidArraysDoNotAddTraffic() {
        TrafficEngine e = new TrafficEngine();
        assertFalse(e.sample(0, null));
        assertFalse(e.sample(0, new long[]{1}));
        e.sample(1000, new long[]{100, 100, 1, 1});
        assertFalse(e.sample(1000, new long[]{200, 200, 2, 2}));
        assertEquals(0, e.rxTotal);
        assertTrue(e.sample(2000, new long[]{300, 300, 3, 3}));
        assertEquals(100, e.rxTotal);
    }
    @Test public void countersAreCopiedAndUnitsAreDecimal() {
        TrafficEngine e = new TrafficEngine();
        long[] counters = {100, 100, 1, 1};
        e.sample(0, counters);
        counters[0] = 1100;
        assertTrue(e.sample(1000, counters));
        assertEquals(1000, e.rxTotal);
        assertEquals("1,00 KB", TrafficEngine.bytes(1000));
        assertEquals("1,50 MB", TrafficEngine.bytes(1500000));
    }
}
