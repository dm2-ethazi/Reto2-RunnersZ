package com.runnersz.reto2.desktop.controller;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;

public final class FxTestSupport {
    public static final AtomicInteger dialogs = new AtomicInteger();
    public static volatile boolean retryDialogs;
    private static boolean initialized;

    private FxTestSupport() {}

    public static synchronized void initialize() throws Exception {
        if (initialized) return;
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(() -> { Platform.setImplicitExit(false); ready.countDown(); });
        if (!ready.await(10, TimeUnit.SECONDS)) throw new AssertionError("JavaFX did not start");
        initialized = true;
        Thread dialogWatcher = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                Platform.runLater(() -> {
                    for (Window window : new ArrayList<>(Window.getWindows())) {
                        if (window.isShowing() && window.getScene().getRoot() instanceof DialogPane pane) {
                            int index = retryDialogs ? 0 : pane.getButtonTypes().size() - 1;
                            if (index >= 0) {
                                dialogs.incrementAndGet();
                                ((Button) pane.lookupButton(pane.getButtonTypes().get(index))).fire();
                            }
                        }
                    }
                });
                try { Thread.sleep(30); } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
        }, "test-dialog-watcher");
        dialogWatcher.setDaemon(true);
        dialogWatcher.start();
    }

    public static <Result> Result fx(Callable<Result> action) throws Exception {
        FutureTask<Result> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    public static void await(BooleanSupplier condition) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(12).toNanos();
        while (!fx(condition::getAsBoolean)) {
            if (System.nanoTime() > deadline) throw new AssertionError("UI condition timed out");
            Thread.sleep(20);
        }
    }

    public static <Value> Value field(Object target, String name, Class<Value> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    public static Object invoke(Object target, String name, Class<?>[] types, Object... arguments)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, arguments);
    }
}
