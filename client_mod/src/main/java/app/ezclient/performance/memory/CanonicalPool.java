package app.ezclient.performance.memory;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

/** Thread-safe weak canonicalizer suitable for immutable model/quad-like values. */
public final class CanonicalPool<T> {
    private final WeakHashMap<T, WeakReference<T>> entries = new WeakHashMap<>();

    public synchronized T intern(T value) {
        WeakReference<T> current = entries.get(value);
        if (current != null) {
            T canonical = current.get();
            if (canonical != null) {
                return canonical;
            }
        }
        entries.put(value, new WeakReference<>(value));
        return value;
    }

    public synchronized int approximateSize() {
        return entries.size();
    }

    public synchronized void clear() {
        entries.clear();
    }
}
