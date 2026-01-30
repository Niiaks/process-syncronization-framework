import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose example to run:");
        System.out.println("1. ReentrantLock Example");
        System.out.println("2. Condition Variable Example");
        System.out.println("3. Monitor (Semaphore) Example");
        System.out.println("4. Semaphore Example");
        System.out.print("Enter choice (1-4): ");
        int choice = scanner.nextInt();

        switch (choice) {
            case 1:
                ReentrantLockExample.run();
                break;
            case 2:
                ConditionVariableExample.run();
                break;
            case 3:
                MonitorExample.run();
                break;
            case 4:
                SemaphoreExample.run();
                break;
            default:
                System.out.println("Invalid choice.");
        }
        scanner.close();
    }
}
