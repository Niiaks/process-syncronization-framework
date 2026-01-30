package com.syncsim;

public class philosopher extends Thread {
    private String name;
    private String leftStick;
    private String rightStick;

    public philosopher(String name, String lestStick, String rightStick) {
        this.name = name;
        this.leftStick = lestStick;
        this.rightStick = rightStick;
    }

    @Override
    public void run() {
        try {
            while (true) {
                System.out.println(name + " is thinking...");
                Thread.sleep(1000); // Wait 1 second

                System.out.println(name + " is hungry and reaching for sticks.");
                
                // This is where the "Wait" happens
                leftStick.acquire(); 
                System.out.println(name + " picked up the left stick.");
                
                rightStick.acquire();
                System.out.println(name + " picked up the right stick and is EATING.");
                
                Thread.sleep(1000); // Eating...

                // Putting them back (Signal)
                leftStick.release();
                rightStick.release();
                System.out.println(name + " put down both sticks.");
            }
        } catch (InterruptedException e) {
            System.out.println(name + " was interrupted.");
        }
    }
    
}
