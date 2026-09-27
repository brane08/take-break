package com.github.brane08.fx.takebreak.tasks;

import com.github.brane08.fx.takebreak.Constants;
import com.github.brane08.fx.takebreak.call.CallDetector;
import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.idle.IdleDetector;
import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class BreakSchedule implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_NAME);

    private final AtomicInteger counter;
    private final Stage currentStage;
    private final AtomicReference<MenuItem> skipItemRef;
    private final Function<Integer, Integer> startTimer;
    private final IdleDetector idleDetector;
    private final CallDetector callDetector;
    private final AtomicLong currentEpoch;
    private final long myEpoch;

    public BreakSchedule(AtomicInteger counter, Stage currentStage, AtomicReference<MenuItem> skipItemRef,
                         Function<Integer, Integer> startTimer, IdleDetector idleDetector, CallDetector callDetector,
                         AtomicLong currentEpoch, long myEpoch) {
        this.counter = counter;
        this.currentStage = currentStage;
        this.skipItemRef = skipItemRef;
        this.startTimer = startTimer;
        this.idleDetector = idleDetector;
        this.callDetector = callDetector;
        this.currentEpoch = currentEpoch;
        this.myEpoch = myEpoch;
    }

    @Override
    public void run() {
        try {
            if (currentEpoch.get() != myEpoch) {
                return; // superseded by a reschedule — don't consume a break slot
            }
            LOG.info("{}", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            BreakConfig breakConfig = Injector.resolveNamed(Constants.DI_BREAK_CONFIG);
            if (callActive()) {
                LOG.info("Skipping break — call in progress");
                return;
            }
            long idleSecs = idleSeconds();
            if (idleSecs >= breakConfig.idleThreshold()) {
                LOG.info("Skipping break — idle {}s >= threshold {}s",
                        idleSecs, breakConfig.idleThreshold());
                return;
            }
            // Detection can block for seconds; a reschedule during that window interrupts this
            // thread and bumps the epoch. Re-check so a superseded tick never consumes a break slot.
            if (Thread.currentThread().isInterrupted() || currentEpoch.get() != myEpoch) {
                return;
            }
            Platform.runLater(() -> {
                if (currentEpoch.get() != myEpoch) {
                    return; // superseded by a reschedule — drop this stale break
                }
                if (currentStage.isShowing()) {
                    LOG.warn("Dropping break — previous break still showing");
                    return;
                }
                // Only advance the counter once we've committed to actually showing a break —
                // a dropped tick above must not consume a slot in the short/long cadence.
                int displayTime = breakConfig.getBreakTime(counter.addAndGet(1));
                skipItemRef.get().setEnabled(true);
                currentStage.show();
                startTimer.apply(displayTime);
            });
        } catch (Exception e) {
            LOG.error("Unhandled exception, check!!!", e);
        } catch (Error e) {
            LOG.error("Fatal error in break schedule", e);
            throw e;
        }
    }

    // Detectors are best-effort: a failure (including a missing native library) must never
    // stop breaks from firing, so treat it as "no call" / "not idle".
    private boolean callActive() {
        try {
            return callDetector.isCallActive();
        } catch (Exception | LinkageError e) {
            LOG.warn("Call detection failed, assuming no call", e);
            return false;
        }
    }

    private long idleSeconds() {
        try {
            return idleDetector.getIdleSeconds();
        } catch (Exception | LinkageError e) {
            LOG.warn("Idle detection failed, assuming user is active", e);
            return 0;
        }
    }
}
