package com.github.brane08.fx.takebreak.tasks;

import javafx.application.Platform;

public final class WarningSchedule implements Runnable {

    private final Runnable showToast;

    public WarningSchedule(Runnable showToast) {
        this.showToast = showToast;
    }

    @Override
    public void run() {
        Platform.runLater(showToast);
    }
}
