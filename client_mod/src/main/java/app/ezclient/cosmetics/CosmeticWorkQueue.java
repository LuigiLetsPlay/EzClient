package app.ezclient.cosmetics;

import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Function;

/** Bounded queue whose deduplication includes work currently being processed. */
final class CosmeticWorkQueue<K, T extends Comparable<T>> {
    private final PriorityQueue<T> queue = new PriorityQueue<>();
    private final Set<K> pending = new HashSet<>();
    private final Function<T, K> key;
    private final int capacity;

    CosmeticWorkQueue(int capacity, Function<T, K> key) {
        this.capacity = capacity;
        this.key = key;
    }

    synchronized boolean offer(T value) {
        if (pending.size() >= capacity || !pending.add(key.apply(value))) return false;
        queue.offer(value);
        notifyAll();
        return true;
    }

    synchronized T take() throws InterruptedException {
        while (queue.isEmpty()) wait();
        return queue.remove();
    }

    synchronized void complete(T value) { pending.remove(key.apply(value)); }
    synchronized boolean isPending(K id) { return pending.contains(id); }

    synchronized void clearQueued() {
        for (T value : queue) pending.remove(key.apply(value));
        queue.clear();
    }
}
