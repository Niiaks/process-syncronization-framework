package com.sync.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadersWriters implements SyncProblem, BenchmarkCapable {
    private static int sharedData = 0;
    private int readCount = 0;

    // Semaphores for Fixed solution
    private final Semaphore mutex = new Semaphore(1);
    private final Semaphore wrt = new Semaphore(1);

    private final List<Thread> activeThreads = new ArrayList<>();

    // Monitors for detection
    private final AtomicInteger activeReaders = new AtomicInteger(0);
    private final AtomicInteger activeWriters = new AtomicInteger(0);

    private PerformanceMetrics metrics = null;

    @Override
    public void setMetrics(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void runBroken() {
        Logger.log("Starting Readers-Writers (Broken - Race Condition)...");
        // Start 5 readers and 2 writers
        for (int i = 0; i < 5; i++)
            startBrokenReader(i);
        for (int i = 0; i < 2; i++)
            startBrokenWriter(i);
    }

    private void startBrokenReader(int id) {
        Thread t = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // Broken: No protection
                    activeReaders.incrementAndGet();
                    if (activeWriters.get() > 0) {
                        Logger.log("!!! VIOLATION !!! Reader " + id + " is reading while Writer is writing!");
                    }

                    Logger.log("Reader " + id + " is reading data: " + sharedData);
                    Thread.sleep(500);

                    activeReaders.decrementAndGet();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Reader-" + id);
        t.start();
        activeThreads.add(t);
    }

    private void startBrokenWriter(int id) {
        Thread t = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    // Broken: No protection
                    activeWriters.incrementAndGet();
                    if (activeReaders.get() > 0 || activeWriters.get() > 1) {
                        Logger.log("!!! VIOLATION !!! Writer " + id + " is writing while others are active!");
                    }

                    int temp = sharedData;
                    temp++;
                    Thread.sleep(200); // Simulate processing to widen race window
                    sharedData = temp;
                    Logger.log("Writer " + id + " updated data to: " + sharedData);

                    activeWriters.decrementAndGet();
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Writer-" + id);
        t.start();
        activeThreads.add(t);
    }

    @Override
    public void runFixed() {
        Logger.log("Starting Readers-Writers (Fixed - Reader Priority)...");
        for (int i = 0; i < 5; i++)
            startFixedReader(i);
        for (int i = 0; i < 2; i++)
            startFixedWriter(i);
    }

    private void startFixedReader(int id) {
        Thread t = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    long waitStart = System.currentTimeMillis();
                    mutex.acquire();
                    readCount++;
                    if (readCount == 1)
                        wrt.acquire(); // First reader locks writer
                    mutex.release();
                    long waitTime = System.currentTimeMillis() - waitStart;
                    if (metrics != null)
                        metrics.recordWaitTime(waitTime);

                    // Reading Section
                    long readStart = System.currentTimeMillis();
                    activeReaders.incrementAndGet();
                    if (activeWriters.get() > 0) {
                        Logger.log("!!! FAILURE IN FIXED MODE !!! Reader accessing while writer active!");
                    }
                    Logger.log("Reader " + id + " is reading data: " + sharedData);
                    Thread.sleep(500);
                    activeReaders.decrementAndGet();
                    if (metrics != null) {
                        metrics.recordActiveTime(System.currentTimeMillis() - readStart);
                        metrics.recordOperation();
                    }

                    mutex.acquire();
                    readCount--;
                    if (readCount == 0)
                        wrt.release(); // Last reader releases writer
                    mutex.release();

                    long idleStart = System.currentTimeMillis();
                    Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1000));
                    if (metrics != null)
                        metrics.recordIdleTime(System.currentTimeMillis() - idleStart);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Reader-" + id);
        t.start();
        activeThreads.add(t);
    }

    private void startFixedWriter(int id) {
        Thread t = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    long waitStart = System.currentTimeMillis();
                    wrt.acquire();
                    long waitTime = System.currentTimeMillis() - waitStart;
                    if (metrics != null) {
                        metrics.recordWaitTime(waitTime);
                        if (waitTime > 100)
                            metrics.recordContention(waitTime);
                    }

                    // Critical Section
                    long writeStart = System.currentTimeMillis();
                    activeWriters.incrementAndGet();
                    if (activeReaders.get() > 0 || activeWriters.get() > 1) {
                        Logger.log("!!! FAILURE IN FIXED MODE !!! Writer accessing while others active!");
                    }

                    Logger.log("Writer " + id + " entering critical section.");
                    int temp = sharedData;
                    temp++;
                    sharedData = temp;
                    Logger.log("Writer " + id + " updated data to: " + sharedData);

                    activeWriters.decrementAndGet();
                    if (metrics != null) {
                        metrics.recordActiveTime(System.currentTimeMillis() - writeStart);
                        metrics.recordOperation();
                    }
                    wrt.release();

                    long idleStart = System.currentTimeMillis();
                    Thread.sleep(ThreadLocalRandom.current().nextInt(1000, 2000));
                    if (metrics != null)
                        metrics.recordIdleTime(System.currentTimeMillis() - idleStart);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Writer-" + id);
        t.start();
        activeThreads.add(t);
    }

    @Override
    public void stop() {
        Logger.log("Stopping Readers-Writers...");
        for (Thread t : activeThreads) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        activeThreads.clear();
    }
}
