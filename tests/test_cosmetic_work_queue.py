"""Exercise the actual Java scheduler without starting Minecraft or contacting cape APIs."""
import pathlib
import shutil
import subprocess
import tempfile
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]


class CosmeticQueueTests(unittest.TestCase):
    @unittest.skipUnless(shutil.which("javac") and shutil.which("java"), "JDK required")
    def test_bounded_deduplication_inflight_and_disconnect(self):
        source = ROOT / "client_mod/src/main/java/app/ezclient/cosmetics/CosmeticWorkQueue.java"
        harness = '''package app.ezclient.cosmetics;
public class QueueRegression {
  record Job(int id) implements Comparable<Job> {
    public int compareTo(Job other) { return Integer.compare(id, other.id); }
  }
  public static void main(String[] args) throws Exception {
    var q = new CosmeticWorkQueue<Integer, Job>(64, Job::id);
    if (!q.offer(new Job(0))) throw new AssertionError("initial enqueue");
    for (int i = 0; i < 100000; i++) if (q.offer(new Job(0))) throw new AssertionError("duplicate queued");
    Job active = q.take();
    for (int i = 0; i < 100000; i++) if (q.offer(new Job(0))) throw new AssertionError("duplicate inflight");
    for (int i = 1; i < 64; i++) if (!q.offer(new Job(i))) throw new AssertionError("capacity");
    if (q.offer(new Job(64))) throw new AssertionError("unbounded queue");
    q.clearQueued();
    if (q.offer(new Job(0))) throw new AssertionError("disconnect lost inflight dedup");
    if (!q.offer(new Job(1))) throw new AssertionError("disconnect retained queued work");
    q.complete(active);
    if (!q.offer(new Job(0))) throw new AssertionError("completion failed");
    if (q.take().id() != 0) throw new AssertionError("priority ordering");
    q.complete(new Job(0)); q.complete(q.take());
    Thread producer = new Thread(() -> q.offer(new Job(9)));
    producer.start();
    if (q.take().id() != 9) throw new AssertionError("wake consumer");
    producer.join();
    class Subscription implements java.util.concurrent.Flow.Subscription {
      boolean cancelled;
      public void request(long count) {}
      public void cancel() { cancelled = true; }
    }
    var response = CosmeticHttp.bytes(4).apply(null);
    var subscription = new Subscription();
    response.onSubscribe(subscription);
    response.onNext(java.util.List.of(java.nio.ByteBuffer.wrap(new byte[]{1, 2})));
    response.onNext(java.util.List.of(java.nio.ByteBuffer.wrap(new byte[]{3, 4})));
    response.onComplete();
    if (subscription.cancelled || response.getBody().toCompletableFuture().join().length != 4)
      throw new AssertionError("valid response rejected");
    var oversized = CosmeticHttp.bytes(4).apply(null);
    var cancelled = new Subscription();
    oversized.onSubscribe(cancelled);
    oversized.onNext(java.util.List.of(java.nio.ByteBuffer.wrap(new byte[]{1, 2, 3})));
    oversized.onNext(java.util.List.of(java.nio.ByteBuffer.wrap(new byte[]{4, 5})));
    if (!cancelled.cancelled || !oversized.getBody().toCompletableFuture().isCompletedExceptionally())
      throw new AssertionError("oversized streamed response not cancelled");
  }
}'''
        with tempfile.TemporaryDirectory() as directory:
            test = pathlib.Path(directory) / "QueueRegression.java"
            test.write_text(harness, encoding="utf-8")
            subprocess.run(["javac", "-d", directory, str(source), str(source.with_name("CosmeticHttp.java")), str(test)], check=True, capture_output=True, text=True)
            subprocess.run(["java", "-cp", directory, "app.ezclient.cosmetics.QueueRegression"], check=True, capture_output=True, text=True, timeout=20)


if __name__ == "__main__":
    unittest.main()
