package com.github.brane08.fx.takebreak.tasks;

import com.github.brane08.fx.takebreak.Constants;
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
import java.util.function.Function;

public final class BreakSchedule implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_NAME);

    private final AtomicInteger counter;
    private final Stage currentStage;
    private final MenuItem skipItem;
    private final Function<Integer, Integer> startTimer;
    private final IdleDetector idleDetector;
    private final AtomicLong currentEpoch;
    private final long myEpoch;

    public BreakSchedule(AtomicInteger counter, Stage currentStage, MenuItem skipItem,
                         Function<Integer, Integer> startTimer, IdleDetector idleDetector,
                         AtomicLong currentEpoch, long myEpoch) {
        this.counter = counter;
        this.currentStage = currentStage;
        this.skipItem = skipItem;
        this.startTimer = startTimer;
        this.idleDetector = idleDetector;
        this.currentEpoch = currentEpoch;
        this.myEpoch = myEpoch;
    }

    @Override
    public void run() {
        try {
            LOG.info("{}", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            BreakConfig breakConfig = Injector.resolveNamed(Constants.DI_BREAK_CONFIG);
            long idleSecs = idleDetector.getIdleSeconds();
            if (idleSecs >= breakConfig.idleThreshold()) {
                LOG.info("Skipping break — idle {}s >= threshold {}s",
                        idleSecs, breakConfig.idleThreshold());
                return;
            }
            int displayTime = breakConfig.getBreakTime(counter.addAndGet(1));
            Platform.runLater(() -> {
                if (currentEpoch.get() != myEpoch) {
                    return; // superseded by a reschedule — drop this stale break
                }
                skipItem.setEnabled(true);
                currentStage.show();
                startTimer.apply(displayTime);
            });
        } catch (Exception e) {
            LOG.error("Unhandled exception, check!!!", e);
        }
    }
}
