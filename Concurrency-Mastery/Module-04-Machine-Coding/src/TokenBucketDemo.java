import java.util.concurrent.atomic.AtomicLong;

public final class TokenBucketDemo {
    public static void main(String[] args) throws Exception {
        TokenBucketRateLimiter rl = new TokenBucketRateLimiter(100, 500);  // burst 100, 500/s
        AtomicLong admitted = new AtomicLong();
        AtomicLong rejected = new AtomicLong();

        long endAt = System.nanoTime() + 2_000_000_000L;                    // 2 seconds
        Thread[] threads = new Thread[8];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = Thread.ofPlatform().start(() -> {
                while (System.nanoTime() < endAt) {
                    if (rl.tryAcquire()) admitted.incrementAndGet();
                    else                  rejected.incrementAndGet();
                }
            });
        }
        for (Thread t : threads) t.join();

        System.out.printf("admitted=%d rejected=%d (expected ~1000 admitted over 2s)%n",
                admitted.get(), rejected.get());
    }
}
