package com.sync.simulator;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class DiningPhilosophers implements SyncProblem, BenchmarkCapable {
    private static final int NUM_PHILOSOPHERS = 5;
    private final Semaphore[] forks = new Semaphore[NUM_PHILOSOPHERS];
    private final Thread[] threads = new Thread[NUM_PHILOSOPHERS];
    private PerformanceMetrics metrics = null;

    public DiningPhilosophers() {
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            forks[i] = new Semaphore(1);
        }
    }

    @Override
    public void setMetrics(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void runBroken() {
        Logger.log("Starting Dining Philosophers (Broken - Deadlock Prone)...");
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        long thinkStart = System.currentTimeMillis();
                        think(id);
                        if (metrics != null)
                            metrics.recordIdleTime(System.currentTimeMillis() - thinkStart);

                        Logger.log("Philosopher " + id + " trying to pick up LEFT fork");
                        long waitStart = System.currentTimeMillis();
                        forks[id].acquire();
                        long waitTime = System.currentTimeMillis() - waitStart;
                        if (metrics != null)
                            metrics.recordWaitTime(waitTime);
                        Logger.log("Philosopher " + id + " picked up LEFT fork");

                        Thread.sleep(1000);

                        Logger.log("Philosopher " + id + " trying to pick up RIGHT fork");
                        waitStart = System.currentTimeMillis();
                        forks[(id + 1) % NUM_PHILOSOPHERS].acquire();
                        waitTime = System.currentTimeMillis() - waitStart;
                        if (metrics != null) {
                            metrics.recordWaitTime(waitTime);
                            if (waitTime > 100)
                                metrics.recordContention(waitTime);
                        }
                        Logger.log("Philosopher " + id + " picked up RIGHT fork");

                        long eatStart = System.currentTimeMillis();
                        eat(id);
                        if (metrics != null) {
                            metrics.recordActiveTime(System.currentTimeMillis() - eatStart);
                            metrics.recordOperation();
                        }

                        forks[id].release();
                        forks[(id + 1) % NUM_PHILOSOPHERS].release();
                        Logger.log("Philosopher " + id + " put down forks");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Philosopher-" + i);
            threads[i].start();
        }
    }

    @Override
    public void runFixed() {
        Logger.log("Starting Dining Philosophers (Fixed - Asymmetric)...");
        for (int i = 0; i < NUM_PHILOSOPHERS; i++) {
            final int id = i;
            threads[i] = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        long thinkStart = System.currentTimeMillis();
                        think(id);
                        if (metrics != null)
                            metrics.recordIdleTime(System.currentTimeMillis() - thinkStart);

                        // Fixed Strategy: Asymmetric
                        long waitStart = System.currentTimeMillis();
                        if (id % 2 == 0) { // Even philosophers pick LEFT then RIGHT
                            forks[id].acquire();
                            Logger.log("Philosopher " + id + " picked up LEFT fork");
                            forks[(id + 1) % NUM_PHILOSOPHERS].acquire();
                            Logger.log("Philosopher " + id + " picked up RIGHT fork");
                        } else { // Odd philosophers pick RIGHT then LEFT
                            forks[(id + 1) % NUM_PHILOSOPHERS].acquire();
                            Logger.log("Philosopher " + id + " picked up RIGHT fork");
                            forks[id].acquire();
                            Logger.log("Philosopher " + id + " picked up LEFT fork");
                        }
                        long waitTime = System.currentTimeMillis() - waitStart;
                        if (metrics != null) {
                            metrics.recordWaitTime(waitTime);
                            if (waitTime > 100)
                                metrics.recordContention(waitTime);
                        }

                        long eatStart = System.currentTimeMillis();
                        eat(id);
                        if (metrics != null) {
                            metrics.recordActiveTime(System.currentTimeMillis() - eatStart);
                            metrics.recordOperation();
                        }

                        forks[id].release();
                        forks[(id + 1) % NUM_PHILOSOPHERS].release();
                        Logger.log("Philosopher " + id + " put down forks");
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }, "Philosopher-" + i);
            threads[i].start();
        }
    }

    @Override
    public void stop() {
        Logger.log("Stopping Dining Philosophers...");
        for (Thread t : threads) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
    }

    private void think(int id) throws InterruptedException {
        Logger.log("Philosopher " + id + " is thinking...");
        Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1000));
    }

    private void eat(int id) throws InterruptedException {
        Logger.log("Philosopher " + id + " is EATING");
        Thread.sleep(ThreadLocalRandom.current().nextInt(500, 1000));
    }
}
