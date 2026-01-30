package com.sync.simulator;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class CigaretteSmokers implements SyncProblem, BenchmarkCapable {
    // Ingredients: 0=Tobacco, 1=Paper, 2=Matches
    // Smokers: 0=Has Tobacco (needs P+M), 1=Has Paper (needs T+M), 2=Has Matches
    // (needs T+P)

    // Semaphores for Agent to signal presence of ingredients on table
    private final Semaphore tobacco = new Semaphore(0);
    private final Semaphore paper = new Semaphore(0);
    private final Semaphore matches = new Semaphore(0);

    // Semaphores for Smokers to wait on their specific combination
    private final Semaphore[] smokerSemaphores = new Semaphore[] {
            new Semaphore(0), new Semaphore(0), new Semaphore(0)
    };

    private final Semaphore agentSemaphore = new Semaphore(1); // Agent waits for smoker to finish
    private final Semaphore mutex = new Semaphore(1); // Protects booleans

    // Booleans to track what's on the table (for Pushers)
    private boolean isTobacco = false;
    private boolean isPaper = false;
    private boolean isMatches = false;

    private final List<Thread> activeThreads = new ArrayList<>();
    private PerformanceMetrics metrics = null;

    @Override
    public void setMetrics(PerformanceMetrics metrics) {
        this.metrics = metrics;
    }

    @Override
    public void runBroken() {
        Logger.log("Starting Cigarette Smokers (Broken - Deadlock)...");
        // Broken: Naive implementation where smokers grab one item then wait for the
        // other. Agent blindly puts items.

        Thread agent = new Thread(() -> {
            try {
                // In Broken mode, Agent doesn't wait for "Table Clear". This causes buildup and
                // confusion
                while (!Thread.currentThread().isInterrupted()) {
                    int rand = ThreadLocalRandom.current().nextInt(3);
                    Logger.log("Agent puts ingredients on table.");
                    if (rand == 0) { // Tobacco + Paper -> Needs Match-Smoker
                        tobacco.release();
                        paper.release();
                    } else if (rand == 1) { // Paper + Matches -> Needs Tobacco-Smoker
                        paper.release();
                        matches.release();
                    } else { // Tobacco + Matches -> Needs Paper-Smoker
                        tobacco.release();
                        matches.release();
                    }
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Agent");
        agent.start();
        activeThreads.add(agent);

        // Naive Smoker with Tobacco (Needs Paper + Matches)
        Thread t0 = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    paper.acquire();
                    Logger.log("Smoker w/ Tobacco picked up Paper. Waiting for Matches...");
                    Thread.sleep(100); // Deadlock window
                    matches.acquire(); // This might never happen if another smoker took matches
                    Logger.log("Smoker w/ Tobacco smoking...");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Smoker-Tobacco");
        t0.start();
        activeThreads.add(t0);

        // Naive Smoker with Paper (Needs Tobacco + Matches)
        Thread t1 = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    tobacco.acquire();
                    Logger.log("Smoker w/ Paper picked up Tobacco. Waiting for Matches...");
                    Thread.sleep(100);
                    matches.acquire();
                    Logger.log("Smoker w/ Paper smoking...");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Smoker-Paper");
        t1.start();
        activeThreads.add(t1);

        // Naive Smoker with Matches (Needs Tobacco + Paper)
        Thread t2 = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    tobacco.acquire();
                    Logger.log("Smoker w/ Matches picked up Tobacco. Waiting for Paper...");
                    Thread.sleep(100);
                    paper.acquire();
                    Logger.log("Smoker w/ Matches smoking...");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Smoker-Matches");
        t2.start();
        activeThreads.add(t2);
    }

    @Override
    public void runFixed() {
        Logger.log("Starting Cigarette Smokers (Fixed - Pushers)...");

        // Agent Thread
        Thread agent = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    agentSemaphore.acquire(); // Wait for table to be empty
                    int rand = ThreadLocalRandom.current().nextInt(3);
                    if (rand == 0) { // Agent offering Tobacco + Paper (Needs Match Smoker 2)
                        Logger.log("Agent put Tobacco and Paper.");
                        tobacco.release();
                        paper.release();
                    } else if (rand == 1) { // Agent offering Paper + Matches (Needs Tobacco Smoker 0)
                        Logger.log("Agent put Paper and Matches.");
                        paper.release();
                        matches.release();
                    } else { // Agent offering Tobacco + Matches (Needs Paper Smoker 1)
                        Logger.log("Agent put Tobacco and Matches.");
                        tobacco.release();
                        matches.release();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Agent");
        agent.start();
        activeThreads.add(agent);

        // Pushers for Tobacco, Paper, Matches
        createPusher(tobacco, "Tobacco", 0, 1, 2);
        createPusher(paper, "Paper", 1, 0, 2);
        createPusher(matches, "Matches", 2, 0, 1);

        // Smokers
        createSmoker(0, "has Tobacco");
        createSmoker(1, "has Paper");
        createSmoker(2, "has Matches");
    }

    private void createPusher(Semaphore ingredient, String name, int type, int other1, int other2) {
        Thread t = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    ingredient.acquire();
                    mutex.acquire();
                    if (isPresent(other1)) {
                        setPresent(other1, false);
                        selectSmokerToWake(type, other1);
                    } else if (isPresent(other2)) {
                        setPresent(other2, false);
                        selectSmokerToWake(type, other2);
                    } else {
                        setPresent(type, true);
                    }
                    mutex.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Pusher-" + name);
        t.start();
        activeThreads.add(t);
    }

    private void selectSmokerToWake(int item1, int item2) {
        int sum = item1 + item2;
        if (sum == 1) // 0+1 (T+P) -> M(2)
            smokerSemaphores[2].release();
        else if (sum == 3) // 1+2 (P+M) -> T(0)
            smokerSemaphores[0].release();
        else if (sum == 2) // 0+2 (T+M) -> P(1)
            smokerSemaphores[1].release();
    }

    private boolean isPresent(int type) {
        if (type == 0)
            return isTobacco;
        if (type == 1)
            return isPaper;
        return isMatches;
    }

    private void setPresent(int type, boolean val) {
        if (type == 0)
            isTobacco = val;
        else if (type == 1)
            isPaper = val;
        else
            isMatches = val;
    }

    private void createSmoker(int id, String desc) {
        Thread t = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    long waitStart = System.currentTimeMillis();
                    smokerSemaphores[id].acquire();
                    if (metrics != null)
                        metrics.recordWaitTime(System.currentTimeMillis() - waitStart);

                    Logger.log("Smoker (" + desc + ") makes cigarette and smokes.");
                    long workStart = System.currentTimeMillis();
                    Thread.sleep(1000);
                    if (metrics != null) {
                        metrics.recordActiveTime(System.currentTimeMillis() - workStart);
                        metrics.recordOperation();
                    }
                    Logger.log("Smoker (" + desc + ") done smoking. Signaling agent.");
                    agentSemaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Smoker-" + id);
        t.start();
        activeThreads.add(t);
    }

    @Override
    public void stop() {
        Logger.log("Stopping Cigarette Smokers...");
        for (Thread t : activeThreads) {
            if (t != null && t.isAlive()) {
                t.interrupt();
            }
        }
        activeThreads.clear();
    }
}
