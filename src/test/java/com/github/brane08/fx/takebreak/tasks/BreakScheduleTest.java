package com.github.brane08.fx.takebreak.tasks;

import com.github.brane08.fx.takebreak.FxHelper;
import com.github.brane08.fx.takebreak.call.CallDetector;
import com.github.brane08.fx.takebreak.idle.IdleDetector;
import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testfx.util.WaitForAsyncUtils;

import java.awt.MenuItem;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BreakScheduleTest {

    @BeforeAll
    static void startFx() throws InterruptedException {
        // Surefire runs with -Djava.awt.headless=true for Monocle; MenuItem's constructor
        // rejects that, so flip it back before touching AWT (no real display init needed here).
        System.setProperty("java.awt.headless", "false");
        Injector.initDefault();
        FxHelper.startOnce();
    }

    private Stage stage;
    private AtomicReference<MenuItem> skipItemRef;
    private AtomicInteger counter;
    private AtomicInteger startTimerCalls;

    @BeforeEach
    void setUp() throws Exception {
        stage = FxHelper.onFxThread(Stage::new);
        skipItemRef = new AtomicReference<>(new MenuItem());
        counter = new AtomicInteger(0);
        startTimerCalls = new AtomicInteger(0);
    }

    private BreakSchedule newSchedule(AtomicLong epoch, long myEpoch) {
        IdleDetector idle = () -> 0L;
        CallDetector call = () -> false;
        return new BreakSchedule(counter, stage, skipItemRef,
                displayTime -> { startTimerCalls.incrementAndGet(); return 0; },
                idle, call, epoch, myEpoch);
    }

    @Test
    void counterAdvancesWhenBreakActuallyShows() {
        AtomicLong epoch = new AtomicLong(0);
        newSchedule(epoch, 0).run();
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(1, counter.get());
        assertEquals(1, startTimerCalls.get());
    }

    @Test
    void droppedTickWhileOverlayShowingDoesNotAdvanceCounter() throws Exception {
        FxHelper.onFxThread(() -> { stage.show(); return null; });

        AtomicLong epoch = new AtomicLong(0);
        newSchedule(epoch, 0).run();
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(0, counter.get());
        assertEquals(0, startTimerCalls.get());
    }

    @Test
    void staleEpochInsideRunLaterDoesNotAdvanceCounter() {
        AtomicLong epoch = new AtomicLong(0);
        BreakSchedule schedule = newSchedule(epoch, 0);

        schedule.run(); // queues Platform.runLater while epoch still matches
        epoch.set(1);   // a reschedule lands before the queued runnable executes
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(0, counter.get());
        assertEquals(0, startTimerCalls.get());
    }
}
