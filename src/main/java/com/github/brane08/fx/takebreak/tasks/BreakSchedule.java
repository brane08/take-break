package com.github.brane08.fx.takebreak.tasks;

import com.github.brane08.fx.takebreak.Constants;
import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class BreakSchedule implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_NAME);

    private final AtomicInteger counter;
    private final Stage currentStage;
    private final MenuItem skipItem;
    private final Function<Integer, Integer> startTimer;

    public BreakSchedule(AtomicInteger counter, Stage currentStage, MenuItem skipItem,
                         Function<Integer, Integer> startTimer) {
        this.counter = counter;
        this.currentStage = currentStage;
        this.skipItem = skipItem;
        this.startTimer = startTimer;
    }

    @Override
    public void run() {
        try {
            LOG.info("{}", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            BreakConfig breakConfig = Injector.resolveNamed("breakConfig");
            int displayTime = breakConfig.getBreakTime(counter.addAndGet(1));
            Platform.runLater(() -> {
                skipItem.setEnabled(true);
                currentStage.show();
                startTimer.apply(displayTime);
            });
        } catch (Exception e) {
            LOG.error("Unhandled exception, check!!!", e);
        }

    }
}
