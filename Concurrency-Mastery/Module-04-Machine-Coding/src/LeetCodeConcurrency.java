import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BooleanSupplier;
import java.util.function.IntConsumer;

/**
 * The 5 canonical LeetCode concurrency problems, in one file.
 * Each class is self-contained; the main() drives quick smoke tests.
 */
public final class LeetCodeConcurrency {

    // ---------- 1114. Print in Order ----------
    public static final class Foo {
        private final Semaphore s2 = new Semaphore(0);
        private final Semaphore s3 = new Semaphore(0);

        public void first(Runnable print)  { print.run(); s2.release(); }
        public void second(Runnable print) throws InterruptedException {
            s2.acquire(); print.run(); s3.release();
        }
        public void third(Runnable print) throws InterruptedException {
            s3.acquire(); print.run();
        }
    }

    // ---------- 1195. Fizz Buzz Multithreaded ----------
    public static final class FizzBuzz {
        private final int n;
        private int i = 1;
        private final ReentrantLock lock = new ReentrantLock();
        private final Condition turn = lock.newCondition();

        public FizzBuzz(int n) { this.n = n; }

        public void fizz(Runnable printFizz) throws InterruptedException {
            run(() -> i % 3 == 0 && i % 5 != 0, printFizz);
        }
        public void buzz(Runnable printBuzz) throws InterruptedException {
            run(() -> i % 5 == 0 && i % 3 != 0, printBuzz);
        }
        public void fizzbuzz(Runnable printFB) throws InterruptedException {
            run(() -> i % 15 == 0, printFB);
        }
        public void number(IntConsumer printNumber) throws InterruptedException {
            lock.lock();
            try {
                while (i <= n) {
                    if (i % 3 != 0 && i % 5 != 0) {
                        printNumber.accept(i);
                        i++;
                        turn.signalAll();
                    } else {
                        turn.await();
                    }
                }
                turn.signalAll();
            } finally { lock.unlock(); }
        }

        private void run(BooleanSupplier isMine, Runnable action) throws InterruptedException {
            lock.lock();
            try {
                while (i <= n) {
                    if (isMine.getAsBoolean()) {
                        action.run();
                        i++;
                        turn.signalAll();
                    } else {
                        turn.await();
                    }
                }
            } finally { lock.unlock(); }
        }
    }

    // ---------- 1117. Building H2O ----------
    public static final class H2O {
        private final Semaphore h = new Semaphore(2);
        private final CyclicBarrier barrier;

        public H2O() {
            this.barrier = new CyclicBarrier(3, () -> h.release(2));
        }

        public void hydrogen(Runnable releaseH) throws InterruptedException {
            h.acquire();
            releaseH.run();
            try { barrier.await(); } catch (BrokenBarrierException e) { throw new InterruptedException(); }
        }

        public void oxygen(Runnable releaseO) throws InterruptedException {
            releaseO.run();
            try { barrier.await(); } catch (BrokenBarrierException e) { throw new InterruptedException(); }
        }
    }

    // ---------- 1226. Dining Philosophers (lock ordering) ----------
    public static final class DiningPhilosophers {
        private final ReentrantLock[] forks = new ReentrantLock[5];

        public DiningPhilosophers() {
            for (int i = 0; i < 5; i++) forks[i] = new ReentrantLock();
        }

        public void wantsToEat(int philosopher,
                               Runnable pickLeft, Runnable pickRight,
                               Runnable eat,
                               Runnable putLeft, Runnable putRight) throws InterruptedException {
            int left  = philosopher;
            int right = (philosopher + 1) % 5;
            int first = Math.min(left, right);
            int second = Math.max(left, right);

            forks[first].lockInterruptibly();
            try {
                forks[second].lockInterruptibly();
                try {
                    pickLeft.run();
                    pickRight.run();
                    eat.run();
                    putLeft.run();
                    putRight.run();
                } finally { forks[second].unlock(); }
            } finally { forks[first].unlock(); }
        }
    }

    // ---------- 1279. Traffic Light Controlled Intersection ----------
    public static final class TrafficLight {
        private final ReentrantLock lock = new ReentrantLock();
        private int greenRoad = 1;

        public void carArrived(int carId, int roadId, int direction,
                               Runnable turnGreen, Runnable crossCar) {
            lock.lock();
            try {
                if (roadId != greenRoad) {
                    turnGreen.run();
                    greenRoad = roadId;
                }
                crossCar.run();
            } finally { lock.unlock(); }
        }
    }

    // ---------- smoke tests ----------
    public static void main(String[] args) throws Exception {
        smokePrintInOrder();
        smokeFizzBuzz();
        smokeH2O();
        smokeTrafficLight();
        System.out.println("all smoke tests passed");
    }

    private static void smokePrintInOrder() throws Exception {
        Foo foo = new Foo();
        StringBuilder out = new StringBuilder();
        Thread t3 = Thread.ofPlatform().start(() -> {
            try { foo.third(() -> out.append("3")); } catch (InterruptedException ignored) {}
        });
        Thread t2 = Thread.ofPlatform().start(() -> {
            try { foo.second(() -> out.append("2")); } catch (InterruptedException ignored) {}
        });
        Thread t1 = Thread.ofPlatform().start(() -> foo.first(() -> out.append("1")));
        t1.join(); t2.join(); t3.join();
        assert out.toString().equals("123") : "print-in-order broken: " + out;
    }

    private static void smokeFizzBuzz() throws Exception {
        FizzBuzz fb = new FizzBuzz(15);
        StringBuilder out = new StringBuilder();
        Thread tn = Thread.ofPlatform().start(() -> {
            try { fb.number(i -> out.append(i).append(",")); } catch (InterruptedException ignored) {}
        });
        Thread tf = Thread.ofPlatform().start(() -> {
            try { fb.fizz(() -> out.append("F,")); } catch (InterruptedException ignored) {}
        });
        Thread tb = Thread.ofPlatform().start(() -> {
            try { fb.buzz(() -> out.append("B,")); } catch (InterruptedException ignored) {}
        });
        Thread tfb = Thread.ofPlatform().start(() -> {
            try { fb.fizzbuzz(() -> out.append("FB,")); } catch (InterruptedException ignored) {}
        });
        tn.join(); tf.join(); tb.join(); tfb.join();
        String expected = "1,2,F,4,B,F,7,8,F,B,11,F,13,14,FB,";
        assert out.toString().equals(expected) : "fizzbuzz broken: " + out;
    }

    private static void smokeH2O() throws Exception {
        H2O h2o = new H2O();
        StringBuilder out = new StringBuilder();
        int molecules = 5;
        Runnable appendH = () -> { synchronized (out) { out.append('H'); } };
        Runnable appendO = () -> { synchronized (out) { out.append('O'); } };
        Thread[] hs = new Thread[molecules * 2];
        Thread[] os = new Thread[molecules];
        for (int i = 0; i < hs.length; i++) hs[i] = Thread.ofPlatform().start(() -> {
            try { h2o.hydrogen(appendH); } catch (InterruptedException ignored) {}
        });
        for (int i = 0; i < os.length; i++) os[i] = Thread.ofPlatform().start(() -> {
            try { h2o.oxygen(appendO); } catch (InterruptedException ignored) {}
        });
        for (Thread t : hs) t.join();
        for (Thread t : os) t.join();
        long h = out.chars().filter(c -> c == 'H').count();
        long o = out.chars().filter(c -> c == 'O').count();
        assert h == molecules * 2 && o == molecules : "h2o counts wrong: " + out;
    }

    private static void smokeTrafficLight() {
        TrafficLight tl = new TrafficLight();
        StringBuilder trace = new StringBuilder();
        tl.carArrived(1, 1, 1, () -> trace.append("G1,"), () -> trace.append("C1,"));
        tl.carArrived(2, 2, 1, () -> trace.append("G2,"), () -> trace.append("C2,"));
        tl.carArrived(3, 1, 1, () -> trace.append("G1,"), () -> trace.append("C3,"));
        // road-1 was already green initially, so no G1 for car 1
        assert trace.toString().equals("C1,G2,C2,G1,C3,") : "traffic light broken: " + trace;
    }
}
