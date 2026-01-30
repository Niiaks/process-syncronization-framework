import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Example demonstrating the use of Condition Variables with ReentrantLock.
 * Threads wait until a condition is met (counter >= 3), then proceed to increment the counter.
 */
public class ConditionVariableExample {
    // Shared counter used for the condition
    static int counter = 0;
    // Lock and condition for thread coordination
    static ReentrantLock lock = new ReentrantLock();
    static Condition condition = lock.newCondition();

    /**
     * Worker method run by each thread.
     * Waits until counter >= 3, then enters critical section and increments counter.
     * @param id Thread identifier for logging
     * @param latch Used to signal when the thread is finished
     */
    static void worker(int id, CountDownLatch latch) {
        lock.lock(); // Acquire the lock before checking the condition
        try {
            while (counter < 3) {
                System.out.printf("[T%d] waiting (counter=%d)%n", id, counter);
                condition.await(); // Wait until signaled and condition is true
            }
            System.out.printf("[T%d] ENTER critical section%n", id);
            counter++;
            System.out.printf("[T%d] counter = %d%n", id, counter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock(); // Always release the lock
            latch.countDown(); // Signal that this thread is done
        }
    }

    /**
     * Increments the counter and signals all waiting threads.
     * Used to trigger the condition for waiting threads.
     */
    static void incrementer() {
        lock.lock();
        try {
            counter++;
            System.out.printf("[INC] counter incremented to %d%n", counter);
            condition.signalAll(); // Wake up all waiting threads
        } finally {
            lock.unlock();
        }
    }

    /**
     * Runs the Condition Variable example with 7 worker threads.
     * Three calls to incrementer() are made to allow workers to proceed.
     */
    public static void run() throws InterruptedException {
        int numWorkers = 7;
        CountDownLatch latch = new CountDownLatch(numWorkers);
        for (int i = 1; i <= numWorkers; i++) {
            int id = i;
            new Thread(() -> worker(id, latch)).start();
        }
        incrementer();
        incrementer();
        incrementer();
        latch.await();
        System.out.println("All workers finished.");
    }
}
