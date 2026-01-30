package com.syncsim;

public class log {
    public static void msg(String action) {
        System.out.printf("[%s] : %s%n", Thread.currentThread().getName(), action);
    }
}
