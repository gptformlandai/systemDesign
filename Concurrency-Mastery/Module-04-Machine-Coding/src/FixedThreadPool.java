import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Fixed-size thread pool with a bounded queue.
 * shutdown() drains, shutdownNow() interrupts.
 */
public final class FixedThreadPool implements Executor {

    private static final Runnable POISON = () -> {};

    private final BoundedBlockingQueue<Runnable> queue;
    private final Thread[] workers;
    private volatile boolean running = true;

    public FixedThreadPool(int nThreads, int queueCapacity) {
        if (nThreads <= 0) throw new IllegalArgumentException();
        this.queue = new BoundedBlockingQueue<>(queueCapacity);
        this.workers = new Thread[nThreads];
        for (int i = 0; i < nThreads; i++) {
            workers[i] = new Thread(this::workerLoop, "pool-worker-" + i);
            workers[i].start();
        }
    }

    @Override
    public void execute(Runnable task) {
        Objects.requireNonNull(task);
        if (!running) throw new RejectedExecutionException("pool is shut down");
        try {
            queue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RejectedExecutionException(e);
        }
    }

    private void workerLoop() {
        while (true) {
            try {
                Runnable task = queue.take();
                if (task == POISON) return;
                try {
                    task.run();
                } catch (Throwable t) {
                    // Never let a task kill a worker; log and continue.
                    System.err.println("[" + Thread.currentThread().getName() + "] task failed: " + t);
                }
            } catch (InterruptedException e) {
                if (!running) return;
                // else keep running; a rogue interrupt should not kill us
            }
        }
    }

    public void shutdown() throws InterruptedException {
        running = false;
        for (int i = 0; i < workers.length; i++) queue.put(POISON);
        for (Thread w : workers) w.join();
    }

    public void shutdownNow() {
        running = false;
        for (Thread w : workers) w.interrupt();
    }

    public int poolSize()          { return workers.length; }
    public int queuedTasks()       { return queue.size(); }
}
