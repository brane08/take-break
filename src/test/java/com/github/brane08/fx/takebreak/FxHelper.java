package com.github.brane08.fx.takebreak;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class FxHelper {

    private static final CountDownLatch READY = new CountDownLatch(1);
    private static final AtomicBoolean started = new AtomicBoolean(false);

    private FxHelper() {}

    public static void startOnce() throws InterruptedException {
        if (started.compareAndSet(false, true)) {
            Platform.startup(READY::countDown);
        }
        READY.await();
        Platform.setImplicitExit(false);
    }

    public static <T> T onFxThread(Callable<T> task) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(task.call());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (error.get() != null) throw new RuntimeException(error.get());
        return result.get();
    }
}
