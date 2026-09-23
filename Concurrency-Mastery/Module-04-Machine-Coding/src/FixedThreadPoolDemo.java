import java.util.concurrent.atomic.AtomicInteger;

public final class FixedThreadPoolDemo {
    public static void main(String[] args) throws Exception {
        FixedThreadPool pool = new FixedThreadPool(4, 16);
        AtomicInteger done = new AtomicInteger();

        for (int i = 0; i < 40; i++) {
            final int id = i;
            pool.execute(() -> {
                try { Thread.sleep(20); } catch (InterruptedException ignored) {}
                if (id == 7) throw new RuntimeException("task 7 blows up");   // pool must survive
                done.incrementAndGet();
            });
        }
        pool.shutdown();
        System.out.printf("completed=%d (expected 39; task 7 threw)%n", done.get());
    }
}
