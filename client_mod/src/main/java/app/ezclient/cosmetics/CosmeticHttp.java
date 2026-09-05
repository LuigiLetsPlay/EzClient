package app.ezclient.cosmetics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.Flow;

/** Cancels oversized responses while receiving them, before allocating an unbounded body. */
final class CosmeticHttp {
    private CosmeticHttp() {}
    static HttpResponse.BodyHandler<byte[]> bytes(int maxBytes) { return info -> new LimitedBody(maxBytes); }
    static HttpResponse.BodyHandler<String> text() {
        return info -> HttpResponse.BodySubscribers.mapping(new LimitedBody(256 * 1024), bytes -> new String(bytes, StandardCharsets.UTF_8));
    }
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final int limit;
        private final ByteArrayOutputStream data = new ByteArrayOutputStream();
        private final CompletableFuture<byte[]> result = new CompletableFuture<>();
        private Flow.Subscription subscription;
        LimitedBody(int limit) { this.limit = limit; }
        public CompletionStage<byte[]> getBody() { return result; }
        public void onSubscribe(Flow.Subscription subscription) { this.subscription = subscription; subscription.request(1); }
        public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) {
                if (buffer.remaining() > limit - data.size()) {
                    subscription.cancel(); result.completeExceptionally(new IOException("Cosmetic response exceeds byte budget")); return;
                }
                byte[] chunk = new byte[buffer.remaining()]; buffer.get(chunk); data.writeBytes(chunk);
            }
            subscription.request(1);
        }
        public void onError(Throwable error) { result.completeExceptionally(error); }
        public void onComplete() { result.complete(data.toByteArray()); }
    }
}
