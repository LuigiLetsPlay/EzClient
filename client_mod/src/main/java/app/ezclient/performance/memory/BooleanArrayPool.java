package app.ezclient.performance.memory;

import java.util.concurrent.ConcurrentHashMap;

/** Content-based canonicalizer for the small immutable boolean arrays used by render caches. */
public final class BooleanArrayPool {
    private static final int DIRECT_BITS = 16;
    private static final boolean[][][] DIRECT = new boolean[DIRECT_BITS + 1][][];
    private static final ConcurrentHashMap<ArrayKey, boolean[]> LARGE = new ConcurrentHashMap<>();

    private BooleanArrayPool() {
    }

    public static boolean[] intern(boolean[] value) {
        if (value.length <= DIRECT_BITS) {
            int key = 0;
            for (int index = 0; index < value.length; index++) {
                if (value[index]) key |= 1 << index;
            }
            synchronized (DIRECT) {
                boolean[][] valuesByBits = DIRECT[value.length];
                if (valuesByBits == null) {
                    valuesByBits = new boolean[1 << value.length][];
                    DIRECT[value.length] = valuesByBits;
                }
                boolean[] existing = valuesByBits[key];
                if (existing != null) return existing;
                valuesByBits[key] = value;
                return value;
            }
        }
        return LARGE.computeIfAbsent(new ArrayKey(value), ignored -> value);
    }

    private static boolean equals(boolean[] left, boolean[] right) {
        if (left.length != right.length) return false;
        for (int index = 0; index < left.length; index++) {
            if (left[index] != right[index]) return false;
        }
        return true;
    }

    private record ArrayKey(boolean[] values) {
        private ArrayKey(boolean[] values) {
            this.values = values.clone();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ArrayKey key && BooleanArrayPool.equals(values, key.values);
        }

        @Override
        public int hashCode() {
            int result = 1;
            for (boolean value : values) result = 31 * result + (value ? 1231 : 1237);
            return result;
        }
    }
}
