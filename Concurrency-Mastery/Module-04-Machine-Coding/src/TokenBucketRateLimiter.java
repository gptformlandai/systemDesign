import java.util.concurrent.atomic.AtomicReference;

/**
 * Lock-free token bucket. Fixed-point token accounting to avoid FP drift.
 */
public final class TokenBucketRateLimiter {

    private static final long SCALE = 1_000_000L;

    private static final class State {
        final long lastNanos;
        final long tokensScaled;
        State(long t, long s) { this.lastNanos = t; this.tokensScaled = s; }
    }

    private final long capacityScaled;
    private final double refillScaledPerNano;
    private final AtomicReference<State> state;

    public TokenBucketRateLimiter(long capacity, double refillPerSecond) {
        if (capacity <= 0 || refillPerSecond <= 0) throw new IllegalArgumentException();
        this.capacityScaled = capacity * SCALE;
        this.refillScaledPerNano = (refillPerSecond * SCALE) / 1_000_000_000d;
        this.state = new AtomicReference<>(new State(System.nanoTime(), capacityScaled));
    }

    public boolean tryAcquire() { return tryAcquire(1); }

    public boolean tryAcquire(long permits) {
        if (permits <= 0) throw new IllegalArgumentException();
        long cost = permits * SCALE;
        while (true) {
            State cur = state.get();
            long now = System.nanoTime();
            long elapsed = Math.max(0, now - cur.lastNanos);
            long refilled = (long) (elapsed * refillScaledPerNano);
            long available = Math.min(capacityScaled, cur.tokensScaled + refilled);
            if (available < cost) return false;
            State next = new State(now, available - cost);
            if (state.compareAndSet(cur, next)) return true;
        }
    }
}
