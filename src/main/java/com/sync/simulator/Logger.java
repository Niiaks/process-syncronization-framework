package com.sync.simulator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public static void log(String message) {
        String threadName = Thread.currentThread().getName();
        String timestamp = LocalDateTime.now().format(formatter);
        System.out.printf("[%s] [%s] : %s%n", threadName, timestamp, message);
    }
}
