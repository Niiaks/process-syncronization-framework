package com.syncsim;

import java.util.Scanner;

public class Main {
    

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        System.out.println("=== OS Process Synchronization Simulator ===");
        System.out.println("1. Dining Philosophers problem");
        System.out.println("2. Exit");
        System.out.println("Choose a problem");

        int choice = input.nextInt();

        if (choice == 1) {
            System.out.println("you chose Dining Philosopher problem");
        }

        input.close();



        System.out.println("Hello world!");
    }
}