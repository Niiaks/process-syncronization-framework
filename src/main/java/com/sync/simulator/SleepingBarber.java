package com.sync.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class SleepingBarber implements SyncProblem, BenchmarkCapable {
    private static final int CHAIRS = 3;
    private int waiting = 0;

    // Fixed Semaphores
    private final Semaphore customers = new Semaphore(0);
    private final Semaphore barber = new Semaphore(0);
    private final Semaphore mutex = new Semaphore(1);

    private final List<Thread> activeThreads = new ArrayList<>();
    private PerformanceMetrics metrics = null;

    @Override
    public void setMetrics(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void runBroken() {
        Logger.log("Starting Sleeping Barber (Broken - Lost Wakeups/Race)...");
        // Broken: No mutex on 'waiting' count, direct notify without semaphores
        waiting = 0;

        Thread barberThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    if (waiting == 0) {
                        Logger.log("Barber is sleeping (busy wait checking).");
                        Thread.sleep(100);
                    } else {
                        waiting--;
                        Logger.log("Barber is cutting hair. Waiting: " + waiting);
                        Thread.sleep(1000); // Cut hair
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Barber");
        barberThread.start();
        activeThreads.add(barberThread);

        for (int i = 0; i < 10; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(ThreadLocalRandom.current().nextInt(100, 2000));

                    // BROKEN LOGIC: Check then modify without atomicity
                    if (waiting < CHAIRS) {
                        int temp = waiting;
                        Thread.sleep(50); // Force race
                        waiting = temp + 1;
                        Logger.log("Customer " + id + " sat down. Waiting: " + waiting);
                        if (waiting > CHAIRS) {
                            Logger.log("!!! VIOLATION !!! waiting count (" + waiting + ") exceeds chairs (" + CHAIRS
                                    + ")");
                        }
                    } else {
                        Logger.log("Customer " + id + " left (shop full).");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Customer-" + i);
            t.start();
            activeThreads.add(t);
        }
    }

    @Override
    public void runFixed() {
        Logger.log("Starting Sleeping Barber (Fixed - Semaphores)...");
        waiting = 0;

        Thread barberThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    Logger.log("Barber is waiting for customer.");
                    long waitStart = System.currentTimeMillis();
                    customers.acquire(); // Sleep if no customers
                    if (metrics != null)
                        metrics.recordWaitTime(System.currentTimeMillis() - waitStart);

                    mutex.acquire();
                    waiting--;
                    if (metrics != null)
                        metrics.updateQueueLength(waiting);
                    barber.release(); // Ready to cut
                    mutex.release();

                    Logger.log("Barber is cutting hair.");
                    long workStart = System.currentTimeMillis();
                    Thread.sleep(1000);
                    if (metrics != null) {
                        metrics.recordActiveTime(System.currentTimeMillis() - workStart);
                        metrics.recordOperation();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Barber");
        barberThread.start();
        activeThreads.add(barberThread);

        for (int i = 0; i < 10; i++) {
            final int id = i;
            Thread t = new Thread(() -> {
                try {
                    long idleStart = System.currentTimeMillis();
                    Thread.sleep(ThreadLocalRandom.current().nextInt(500, 3000));
                    if (metrics != null)
                        metrics.recordIdleTime(System.currentTimeMillis() - idleStart);

                    Logger.log("Customer " + id + " arrived.");

                    mutex.acquire();
                    if (waiting < CHAIRS) {
                        waiting++;
                        if (metrics != null)
                            metrics.updateQueueLength(waiting);
                        customers.release(); // Wake up barber
                        mutex.release();

                        long waitStart = System.currentTimeMillis();
                        barber.acquire(); // Wait for barber to be ready
                        if (metrics != null)
                            metrics.recordWaitTime(System.currentTimeMillis() - waitStart);
                        Logger.log("Customer " + id + " is getting a haircut.");
                    } else {
                        mutex.release();
                        Logger.log("Customer " + id + " left (Wait room full).");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Customer-" + id);
            t.start();
            activeThreads.add(t);
        }
    }

    @Override
    public void stop() {
        Logger.log("Stopping Sleeping Barber...");
        for (Thread t : activeThreads) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        activeThreads.clear();
    }
}
