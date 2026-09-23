import java.util.concurrent.atomic.AtomicInteger;

public final class BoundedBlockingQueueDemo {

    public static void main(String[] args) throws Exception {
        BoundedBlockingQueue<Integer> q = new BoundedBlockingQueue<>(8);
        int producers = 4, consumers = 4, itemsPerProducer = 1000;

        AtomicInteger produced = new AtomicInteger();
        AtomicInteger consumed = new AtomicInteger();

        Thread[] ps = new Thread[producers];
        for (int i = 0; i < producers; i++) {
            final int id = i;
            ps[i] = Thread.ofPlatform().name("p-" + id).start(() -> {
                try {
                    for (int j = 0; j < itemsPerProducer; j++) {
                        q.put(id * itemsPerProducer + j);
                        produced.incrementAndGet();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        Thread[] cs = new Thread[consumers];
        int target = producers * itemsPerProducer;
        for (int i = 0; i < consumers; i++) {
            cs[i] = Thread.ofPlatform().name("c-" + i).start(() -> {
                try {
                    while (consumed.get() < target) {
                        Integer v = q.take();
                        if (v != null) consumed.incrementAndGet();
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
        }

        for (Thread p : ps) p.join();
        // Give consumers a moment then interrupt any stragglers.
        while (consumed.get() < target) Thread.sleep(5);
        for (Thread c : cs) c.interrupt();
        for (Thread c : cs) c.join();

        System.out.printf("produced=%d consumed=%d remaining=%d%n",
                produced.get(), consumed.get(), q.size());
    }
}
