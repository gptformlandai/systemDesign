import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * L5-grade bounded blocking queue. Two-Condition design → no wasted wakeups.
 * Safe for N producers x M consumers.
 */
public final class BoundedBlockingQueue<T> {

    private final Object[] items;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notFull  = lock.newCondition();
    private final Condition notEmpty = lock.newCondition();
    private int putIdx, takeIdx, count;

    public BoundedBlockingQueue(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("capacity must be > 0");
        this.items = new Object[capacity];
    }

    public void put(T item) throws InterruptedException {
        Objects.requireNonNull(item, "null items disallowed");
        lock.lockInterruptibly();
        try {
            while (count == items.length) notFull.await();
            items[putIdx] = item;
            if (++putIdx == items.length) putIdx = 0;
            count++;
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T take() throws InterruptedException {
        lock.lockInterruptibly();
        try {
            while (count == 0) notEmpty.await();
            T item = (T) items[takeIdx];
            items[takeIdx] = null;
            if (++takeIdx == items.length) takeIdx = 0;
            count--;
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public boolean offer(T item) {
        Objects.requireNonNull(item);
        lock.lock();
        try {
            if (count == items.length) return false;
            items[putIdx] = item;
            if (++putIdx == items.length) putIdx = 0;
            count++;
            notEmpty.signal();
            return true;
        } finally {
            lock.unlock();
        }
    }

    @SuppressWarnings("unchecked")
    public T poll() {
        lock.lock();
        try {
            if (count == 0) return null;
            T item = (T) items[takeIdx];
            items[takeIdx] = null;
            if (++takeIdx == items.length) takeIdx = 0;
            count--;
            notFull.signal();
            return item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try { return count; }
        finally { lock.unlock(); }
    }

    public int capacity() { return items.length; }
}
