package com.sync.simulator;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks performance metrics for synchronization primitives
 */
public class PerformanceMetrics {
    private final String problemName;
    private final boolean isFixed;

    // Timing metrics
    private final AtomicLong totalWaitTime = new AtomicLong(0);
    private final AtomicInteger waitCount = new AtomicInteger(0);

    // Throughput metrics
    private final AtomicInteger completedOperations = new AtomicInteger(0);
    private long startTime;
    private long endTime;

    // Contention metrics
    private final AtomicInteger contentionCount = new AtomicInteger(0);
    private final AtomicLong totalContentionTime = new AtomicLong(0);

    // CPU usage approximation (thread active time)
    private final AtomicLong totalActiveTime = new AtomicLong(0);
    private final AtomicLong totalIdleTime = new AtomicLong(0);

    // Fairness metrics
    private final AtomicInteger maxQueueLength = new AtomicInteger(0);
    private final AtomicInteger currentQueueLength = new AtomicInteger(0);

    public PerformanceMetrics(String problemName, boolean isFixed) {
        this.problemName = problemName;
        this.isFixed = isFixed;
        this.startTime = System.currentTimeMillis();
    }

    public void recordWaitTime(long waitTimeMs) {
        totalWaitTime.addAndGet(waitTimeMs);
        waitCount.incrementAndGet();
    }

    public void recordOperation() {
        completedOperations.incrementAndGet();
    }

    public void recordContention(long contentionTimeMs) {
        contentionCount.incrementAndGet();
        totalContentionTime.addAndGet(contentionTimeMs);
    }

    public void recordActiveTime(long activeTimeMs) {
        totalActiveTime.addAndGet(activeTimeMs);
    }

    public void recordIdleTime(long idleTimeMs) {
        totalIdleTime.addAndGet(idleTimeMs);
    }

    public void updateQueueLength(int length) {
        currentQueueLength.set(length);
        int max = maxQueueLength.get();
        if (length > max) {
            maxQueueLength.compareAndSet(max, length);
        }
    }

    public void finish() {
        this.endTime = System.currentTimeMillis();
    }

    // Calculated metrics
    public double getAverageWaitTime() {
        int count = waitCount.get();
        return count > 0 ? (double) totalWaitTime.get() / count : 0.0;
    }

    public double getThroughput() {
        long duration = endTime - startTime;
        return duration > 0 ? (double) completedOperations.get() / (duration / 1000.0) : 0.0;
    }

    public double getContentionRate() {
        int total = completedOperations.get();
        return total > 0 ? (double) contentionCount.get() / total : 0.0;
    }

    public double getAverageContentionTime() {
        int count = contentionCount.get();
        return count > 0 ? (double) totalContentionTime.get() / count : 0.0;
    }

    public double getCpuUtilization() {
        long total = totalActiveTime.get() + totalIdleTime.get();
        return total > 0 ? (double) totalActiveTime.get() / total * 100.0 : 0.0;
    }

    public long getDuration() {
        return endTime - startTime;
    }

    // Getters
    public String getProblemName() {
        return problemName;
    }

    public boolean isFixed() {
        return isFixed;
    }

    public int getCompletedOperations() {
        return completedOperations.get();
    }

    public int getMaxQueueLength() {
        return maxQueueLength.get();
    }

    public long getTotalWaitTime() {
        return totalWaitTime.get();
    }

    public int getContentionCount() {
        return contentionCount.get();
    }

    @Override
    public String toString() {
        return String.format(
                "%s (%s):\n" +
                        "  Duration: %d ms\n" +
                        "  Completed Operations: %d\n" +
                        "  Throughput: %.2f ops/sec\n" +
                        "  Avg Wait Time: %.2f ms\n" +
                        "  Contention Rate: %.2f%%\n" +
                        "  Avg Contention Time: %.2f ms\n" +
                        "  CPU Utilization: %.2f%%\n" +
                        "  Max Queue Length: %d",
                problemName,
                isFixed ? "Fixed" : "Broken",
                getDuration(),
                completedOperations.get(),
                getThroughput(),
                getAverageWaitTime(),
                getContentionRate() * 100,
                getAverageContentionTime(),
                getCpuUtilization(),
                maxQueueLength.get());
    }
}
