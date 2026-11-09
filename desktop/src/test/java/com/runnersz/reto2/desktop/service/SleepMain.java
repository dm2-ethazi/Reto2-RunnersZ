package com.runnersz.reto2.desktop.service;

public final class SleepMain {
    private SleepMain() {}

    public static void main(String[] args) throws Exception {
        System.out.println("sleep-main listo");
        System.out.flush();
        Thread.sleep(Long.parseLong(args[0]));
    }
}
