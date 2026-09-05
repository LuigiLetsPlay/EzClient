package app.ezclient.shared;

/** Allocation-free rolling one-second event counter shared by CPS adapters. */
public final class ClickRateTracker {
    private final long[] timestamps;
    private int writeIndex;
    private int count;

    public ClickRateTracker(int capacity) {
        timestamps = new long[Math.max(1, capacity)];
    }

    public synchronized void record(long timestampMillis) {
        timestamps[writeIndex] = timestampMillis;
        writeIndex = (writeIndex + 1) % timestamps.length;
        count = Math.min(timestamps.length, count + 1);
    }

    public synchronized int count(long nowMillis) {
        while (count > 0) {
            int oldest = (writeIndex - count + timestamps.length) % timestamps.length;
            if (nowMillis - timestamps[oldest] <= 1000L) break;
            count--;
        }
        return count;
    }
}
