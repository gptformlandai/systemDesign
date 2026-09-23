import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

public final class LRUCacheDemo {
    public static void main(String[] args) throws Exception {
        LRUCache<Integer, Integer> cache = new LRUCache<>(1000);
        AtomicLong hits = new AtomicLong();
        AtomicLong misses = new AtomicLong();

        Thread[] threads = new Thread[8];
        int opsPerThread = 100_000;

        for (int i = 0; i < threads.length; i++) {
            threads[i] = Thread.ofPlatform().start(() -> {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                for (int j = 0; j < opsPerThread; j++) {
                    int key = r.nextInt(2000);
                    if (r.nextInt(3) == 0) {
                        cache.put(key, key * 2);
                    } else {
                        if (cache.get(key) != null) hits.incrementAndGet();
                        else                        misses.incrementAndGet();
                    }
                }
            });
        }
        for (Thread t : threads) t.join();

        System.out.printf("hits=%d misses=%d hit-rate=%.2f size=%d%n",
                hits.get(), misses.get(),
                hits.get() * 1.0 / (hits.get() + misses.get()),
                cache.size());
    }
}
