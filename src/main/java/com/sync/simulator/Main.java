package com.sync.simulator;

import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        SyncProblem currentProblem = null;

        while (true) {
            System.out.println("\n=== Process Synchronization Simulator ===");
            System.out.println("1. Dining Philosophers");
            System.out.println("2. Readers-Writers");
            System.out.println("3. Sleeping Barber");
            System.out.println("4. Cigarette Smokers");
            System.out.println("5. Run Performance Benchmarks");
            System.out.println("6. Exit");
            System.out.print("Select a problem to simulate: ");

            int choice = -1;
            try {
                choice = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }

            if (choice == 6) {
                if (currentProblem != null) {
                    currentProblem.stop();
                }
                System.out.println("Exiting...");
                break;
            }

            if (choice == 5) {
                // Run benchmarks
                if (currentProblem != null) {
                    currentProblem.stop();
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                    }
                    currentProblem = null;
                }
                BenchmarkRunner.runAllBenchmarks();
                continue;
            }

            if (currentProblem != null) {
                currentProblem.stop();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                }
                currentProblem = null;
            }

            SyncProblem problem = null;
            switch (choice) {
                case 1:
                    problem = new DiningPhilosophers();
                    break;
                case 2:
                    problem = new ReadersWriters();
                    break;
                case 3:
                    problem = new SleepingBarber();
                    break;
                case 4:
                    problem = new CigaretteSmokers();
                    break;
                default:
                    System.out.println("Invalid number. Try again.");
                    continue;
            }
            currentProblem = problem;

            System.out.println("\nSelect Simulation Mode:");
            System.out.println("1. Simulate Deadlock/Race Condition (Broken)");
            System.out.println("2. Simulate Solution (Fixed)");
            System.out.print("Enter choice (1 or 2): ");

            int mode = -1;
            try {
                mode = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Defaulting to Broken mode.");
                mode = 1;
            }

            System.out.println(
                    "\n--- Starting Simulation (Press Enter to stop and return to menu) ---");

            if (mode == 2) {
                problem.runFixed();
            } else {
                problem.runBroken();
            }

            scanner.nextLine();
            problem.stop();
            System.out.println("--- Simulation Stopped ---");
        }
        scanner.close();
    }
}
