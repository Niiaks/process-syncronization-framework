import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Example demonstrating the use of ReentrantLock for mutual exclusion.
 * Multiple threads increment a shared counter, but only one can enter the critical section at a time.
 */
public class ReentrantLockExample {
    // Shared counter incremented by all threads
    static int counter = 0;
    // ReentrantLock provides explicit locking/unlocking
    static ReentrantLock lock = new ReentrantLock();

    /**
     * Worker method run by each thread.
     * Acquires the lock, enters the critical section, increments the counter, and releases the lock.
     * @param id Thread identifier for logging
     * @param latch Used to signal when the thread is finished
     */
    static void worker(int id, CountDownLatch latch) {
        System.out.printf("[T%d] waiting for lock%n", id);
        lock.lock(); // Acquire the lock before entering critical section
        try {
            System.out.printf("[T%d] ENTER critical section%n", id);
            counter++; // Only one thread at a time can increment
            System.out.printf("[T%d] counter = %d%n", id, counter);
            System.out.printf("[T%d] LEAVE critical section%n", id);
        } finally {
            lock.unlock();
            latch.countDown(); // Signal that this thread is done
        }
    }

    /**
     * Runs the ReentrantLock example with 3 worker threads.
     * Each thread will increment the shared counter in a thread-safe manner.
     */
    public static void run() throws InterruptedException {
        int numWorkers = 3;
        CountDownLatch latch = new CountDownLatch(numWorkers); // Waits for all threads to finish
        for (int i = 1; i <= numWorkers; i++) {
            int id = i;
            // Start a new thread running the worker method
            new Thread(() -> worker(id, latch)).start();
        }
        latch.await(); // Wait for all threads to finish
        System.out.println("All workers finished.");
    }
}