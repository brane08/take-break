package com.github.brane08.fx.takebreak.tasks;

import javafx.application.Platform;

import java.util.function.BooleanSupplier;

public final class WarningSchedule implements Runnable {

    private final Runnable showToast;
    private final BooleanSupplier suppress;

    public WarningSchedule(Runnable showToast) {
        this(showToast, () -> false);
    }

    /** @param suppress when true the toast is skipped (e.g. a call is active, so no break will follow) */
    public WarningSchedule(Runnable showToast, BooleanSupplier suppress) {
        this.showToast = showToast;
        this.suppress = suppress;
    }

    @Override
    public void run() {
        boolean skip;
        try {
            skip = suppress.getAsBoolean();
        } catch (RuntimeException | LinkageError e) {
            skip = false; // detection is best-effort — never lose the warning over it
        }
        if (skip || Thread.currentThread().isInterrupted()) {
            return;
        }
        Platform.runLater(showToast);
    }
}
