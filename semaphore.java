import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Example demonstrating the use of a Semaphore for controlling concurrency.
 * Allows up to 2 threads to enter the critical section at the same time.
 */
public class SemaphoreExample {
    // Shared counter incremented by all threads
    static int counter = 0;
    // Semaphore initialized to 2 permits (2 threads allowed in critical section)
    static Semaphore semaphore = new Semaphore(2); // allows 2 threads at once

    /**
     * Worker method run by each thread.
     * Acquires a permit, enters the critical section, increments the counter, and releases the permit.
     * @param id Thread identifier for logging
     * @param latch Used to signal when the thread is finished
     */
    static void worker(int id, CountDownLatch latch) {
        try {
            System.out.printf("[T%d] waiting for semaphore...%n", id);
            semaphore.acquire(); // Acquire a permit before entering critical section
            System.out.printf("[T%d] entering critical section...%n", id);
            counter++;
            Thread.sleep(1000); // Simulate work in critical section
            System.out.printf("[T%d] counter = %d%n", id, counter);
            System.out.printf("[T%d] leaving critical section...%n", id);
            semaphore.release(); // Release the permit
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            latch.countDown(); // Signal that this thread is done
        }
    }

    /**
     * Runs the Semaphore example with 5 worker threads.
     * At most 2 threads can be in the critical section at the same time.
     */
    public static void run() throws InterruptedException {
        int numWorkers = 5;
        CountDownLatch latch = new CountDownLatch(numWorkers);
        for (int i = 0; i < numWorkers; i++) {
            int id = i;
            new Thread(() -> worker(id, latch)).start();
        }
        latch.await(); // Wait for all threads to finish
        System.out.println("All workers finished.");
    }
}
