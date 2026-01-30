package com.sync.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class ProducerConsumer implements SyncProblem, BenchmarkCapable {
    private static final int BUFFER_SIZE = 5;
    private static final int NUM_PRODUCERS = 2;
    private static final int NUM_CONSUMERS = 3;

    private int[] buffer = new int[BUFFER_SIZE];
    private int count = 0; // Number of items in buffer
    private int in = 0; // Index where producer will insert
    private int out = 0; // Index where consumer will remove

    // Semaphores for Fixed solution
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore empty = new Semaphore(BUFFER_SIZE); // Count of empty slots
    private final Semaphore full = new Semaphore(0); // Count of full slots

    private final List<Thread> activeThreads = new ArrayList<>();
    private PerformanceMetrics metrics = null;

    @Override
    public void setMetrics(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void runBroken() {
        Logger.log("Starting Producer-Consumer (Broken - Race Condition)...");
        count = 0;
        in = 0;
        out = 0;

        // Broken Producers - No synchronization
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    int item = 0;
                    while (!Thread.currentThread().isInterrupted()) {
                        // Produce item
                        item++;
                        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));

                        // BROKEN: Check-then-act race condition
                        if (count < BUFFER_SIZE) {
                            Thread.sleep(50); // Widen race window
                            buffer[in] = item;
                            in = (in + 1) % BUFFER_SIZE;
                            count++;
                            Logger.log("Producer " + id + " produced item " + item + " (count=" + count + ")");

                            if (count > BUFFER_SIZE) {
                                Logger.log("!!! VIOLATION !!! Buffer overflow! count=" + count);
                            }
                        } else {
                            Logger.log("Producer " + id + " waiting (buffer full)");
                            Thread.sleep(100);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Producer-" + id);
            t.start();
            activeThreads.add(t);
        }

        // Broken Consumers - No synchronization
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        // BROKEN: Check-then-act race condition
                        if (count > 0) {
                            Thread.sleep(50); // Widen race window
                            int item = buffer[out];
                            out = (out + 1) % BUFFER_SIZE;
                            count--;
                            Logger.log("Consumer " + id + " consumed item " + item + " (count=" + count + ")");

                            if (count < 0) {
                                Logger.log("!!! VIOLATION !!! Buffer underflow! count=" + count);
                            }
                        } else {
                            Logger.log("Consumer " + id + " waiting (buffer empty)");
                            Thread.sleep(100);
                        }

                        // Consume item
                        Thread.sleep(ThreadLocalRandom.current().nextInt(150, 400));
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + id);
            t.start();
            activeThreads.add(t);
        }
    }

    @Override
    public void runFixed() {
        Logger.log("Starting Producer-Consumer (Fixed - Semaphores)...");
        count = 0;
        in = 0;
        out = 0;

        // Fixed Producers - Using semaphores
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    int item = 0;
                    while (!Thread.currentThread().isInterrupted()) {
                        // Produce item
                        item++;
                        long idleStart = System.currentTimeMillis();
                        Thread.sleep(ThreadLocalRandom.current().nextInt(100, 300));
                        if (metrics != null)
                            metrics.recordIdleTime(System.currentTimeMillis() - idleStart);

                        // Wait for empty slot
                        long waitStart = System.currentTimeMillis();
                        empty.acquire();
                        if (metrics != null)
                            metrics.recordWaitTime(System.currentTimeMillis() - waitStart);

                        // Critical section
                        mutex.acquire();
                        long workStart = System.currentTimeMillis();

                        buffer[in] = item;
                        in = (in + 1) % BUFFER_SIZE;
                        count++;
                        Logger.log("Producer " + id + " produced item " + item + " (count=" + count + ")");

                        if (metrics != null) {
                            metrics.recordActiveTime(System.currentTimeMillis() - workStart);
                            metrics.recordOperation();
                            metrics.updateQueueLength(count);
                        }

                        mutex.release();
                        full.release(); // Signal that there's a new item
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Producer-" + id);
            t.start();
            activeThreads.add(t);
        }

        // Fixed Consumers - Using semaphores
        for (int i = 0; i < NUM_CONSUMERS; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        // Wait for full slot
                        long waitStart = System.currentTimeMillis();
                        full.acquire();
                        if (metrics != null)
                            metrics.recordWaitTime(System.currentTimeMillis() - waitStart);

                        // Critical section
                        mutex.acquire();
                        long workStart = System.currentTimeMillis();

                        int item = buffer[out];
                        out = (out + 1) % BUFFER_SIZE;
                        count--;
                        Logger.log("Consumer " + id + " consumed item " + item + " (count=" + count + ")");

                        if (metrics != null) {
                            metrics.recordActiveTime(System.currentTimeMillis() - workStart);
                            metrics.recordOperation();
                            metrics.updateQueueLength(count);
                        }

                        mutex.release();
                        empty.release(); // Signal that there's an empty slot

                        // Consume item
                        long idleStart = System.currentTimeMillis();
                        Thread.sleep(ThreadLocalRandom.current().nextInt(150, 400));
                        if (metrics != null)
                            metrics.recordIdleTime(System.currentTimeMillis() - idleStart);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Consumer-" + id);
            t.start();
            activeThreads.add(t);
        }
    }

    @Override
    public void stop() {
        Logger.log("Stopping Producer-Consumer...");
        for (Thread t : activeThreads) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        activeThreads.clear();
    }
}
