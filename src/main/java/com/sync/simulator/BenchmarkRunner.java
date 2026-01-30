package com.sync.simulator;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs benchmarks for synchronization problems and aggregates results
 */
public class BenchmarkRunner {
    private static final int BENCHMARK_DURATION_MS = 10000; // 10 seconds per benchmark

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    public static void runAllBenchmarks() {
        System.out.println("\n" + repeat("=", 70));
        System.out.println("PERFORMANCE BENCHMARK MODE");
        System.out.println(repeat("=", 70));
        System.out.println("Running each problem for " + (BENCHMARK_DURATION_MS / 1000) + " seconds...\n");

        List<PerformanceMetrics> allMetrics = new ArrayList<>();

        // Benchmark Dining Philosophers
        System.out.println("--- Benchmarking Dining Philosophers ---");
        allMetrics.add(benchmarkProblem(new DiningPhilosophers(), "Dining Philosophers", false));
        allMetrics.add(benchmarkProblem(new DiningPhilosophers(), "Dining Philosophers", true));

        // Benchmark Readers-Writers
        System.out.println("\n--- Benchmarking Readers-Writers ---");
        allMetrics.add(benchmarkProblem(new ReadersWriters(), "Readers-Writers", false));
        allMetrics.add(benchmarkProblem(new ReadersWriters(), "Readers-Writers", true));

        // Benchmark Sleeping Barber
        System.out.println("\n--- Benchmarking Sleeping Barber ---");
        allMetrics.add(benchmarkProblem(new SleepingBarber(), "Sleeping Barber", false));
        allMetrics.add(benchmarkProblem(new SleepingBarber(), "Sleeping Barber", true));

        // Benchmark Cigarette Smokers
        System.out.println("\n--- Benchmarking Cigarette Smokers ---");
        allMetrics.add(benchmarkProblem(new CigaretteSmokers(), "Cigarette Smokers", false));
        allMetrics.add(benchmarkProblem(new CigaretteSmokers(), "Cigarette Smokers", true));

        // Benchmark Producer-Consumer
        System.out.println("\n--- Benchmarking Producer-Consumer ---");
        allMetrics.add(benchmarkProblem(new ProducerConsumer(), "Producer-Consumer", false));
        allMetrics.add(benchmarkProblem(new ProducerConsumer(), "Producer-Consumer", true));

        // Print summary
        printBenchmarkSummary(allMetrics);
    }

    private static PerformanceMetrics benchmarkProblem(SyncProblem problem, String name, boolean runFixed) {
        PerformanceMetrics metrics = new PerformanceMetrics(name, runFixed);

        // Set metrics in problem if it supports benchmarking
        if (problem instanceof BenchmarkCapable) {
            ((BenchmarkCapable) problem).setMetrics(metrics);
        }

        System.out.println("  Running " + name + " (" + (runFixed ? "Fixed" : "Broken") + ")...");

        // Start the problem
        if (runFixed) {
            problem.runFixed();
        } else {
            problem.runBroken();
        }

        // Let it run for the benchmark duration
        try {
            Thread.sleep(BENCHMARK_DURATION_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Stop the problem
        problem.stop();
        metrics.finish();

        // Wait for threads to clean up
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("  Completed: " + metrics.getCompletedOperations() + " operations");

        return metrics;
    }

    private static void printBenchmarkSummary(List<PerformanceMetrics> allMetrics) {
        System.out.println("\n" + repeat("=", 70));
        System.out.println("BENCHMARK RESULTS SUMMARY");
        System.out.println(repeat("=", 70));

        for (PerformanceMetrics metrics : allMetrics) {
            System.out.println("\n" + metrics.toString());
        }

        // Comparative analysis
        System.out.println("\n" + repeat("=", 70));
        System.out.println("COMPARATIVE ANALYSIS");
        System.out.println(repeat("=", 70));

        // Group by problem type
        for (int i = 0; i < allMetrics.size(); i += 2) {
            PerformanceMetrics broken = allMetrics.get(i);
            PerformanceMetrics fixed = allMetrics.get(i + 1);

            System.out.println("\n" + broken.getProblemName() + ":");

            double throughputImprovement = ((fixed.getThroughput() - broken.getThroughput()) / broken.getThroughput())
                    * 100;
            System.out.printf("  Throughput Improvement: %.2f%%\n", throughputImprovement);

            double waitTimeReduction = ((broken.getAverageWaitTime() - fixed.getAverageWaitTime())
                    / broken.getAverageWaitTime()) * 100;
            System.out.printf("  Wait Time Reduction: %.2f%%\n", waitTimeReduction);

            double contentionReduction = ((broken.getContentionRate() - fixed.getContentionRate())
                    / broken.getContentionRate()) * 100;
            System.out.printf("  Contention Reduction: %.2f%%\n", contentionReduction);

            System.out.printf("  CPU Utilization (Broken): %.2f%% -> (Fixed): %.2f%%\n",
                    broken.getCpuUtilization(), fixed.getCpuUtilization());
        }

        // Overall comparison table
        System.out.println("\n" + repeat("=", 70));
        System.out.println("PERFORMANCE COMPARISON TABLE");
        System.out.println(repeat("=", 70));
        System.out.printf("%-25s %-10s %10s %12s %12s %10s\n",
                "Problem", "Mode", "Ops", "Throughput", "Avg Wait", "CPU %");
        System.out.println(repeat("-", 70));

        for (PerformanceMetrics m : allMetrics) {
            System.out.printf("%-25s %-10s %10d %12.2f %12.2f %10.2f\n",
                    m.getProblemName(),
                    m.isFixed() ? "Fixed" : "Broken",
                    m.getCompletedOperations(),
                    m.getThroughput(),
                    m.getAverageWaitTime(),
                    m.getCpuUtilization());
        }

        System.out.println(repeat("=", 70));
    }
}
