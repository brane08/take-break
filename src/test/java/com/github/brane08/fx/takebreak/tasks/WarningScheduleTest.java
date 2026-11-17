package com.github.brane08.fx.takebreak.tasks;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;

class WarningScheduleTest {

    @Test
    void toastIsSuppressedWhileCallIsActive() {
        AtomicBoolean shown = new AtomicBoolean(false);
        new WarningSchedule(() -> shown.set(true), () -> true).run();
        assertFalse(shown.get());
    }

    @Test
    void interruptedTickDoesNotShowToast() {
        AtomicBoolean shown = new AtomicBoolean(false);
        Thread.currentThread().interrupt();
        try {
            new WarningSchedule(() -> shown.set(true), () -> false).run();
        } finally {
            Thread.interrupted(); // clear the flag for other tests
        }
        assertFalse(shown.get());
    }
}
