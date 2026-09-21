package com.github.brane08.fx.takebreak.controllers;

import com.github.brane08.fx.takebreak.Constants;
import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ResourceBundle;

public class BreakController implements Initializable {

    private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_NAME);

    private final int secondsPerMin = 60;
    private final SimpleStringProperty minuteProperty = new SimpleStringProperty("00");
    private final SimpleStringProperty secondProperty = new SimpleStringProperty("00");
    private CountdownTimer currentTimer;
    private Runnable hideCallback;

    @FXML
    VBox rootPane;
    @FXML
    Label minutes;
    @FXML
    Label seconds;
    @FXML
    Button btnSkip;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        minutes.textProperty().bindBidirectional(minuteProperty);
        seconds.textProperty().bindBidirectional(secondProperty);
    }

    public void stopTimer() {
        if (currentTimer != null) {
            currentTimer.stop();
        }
    }

    public Integer startTimer(int timerFor) {
        LOG.info("Starting animation timer");
        if (currentTimer != null) {
            currentTimer.cancelSilently(); // replaced, not finished: keep the overlay up, no hide
        }
        updateTiles(timerFor);
        currentTimer = new CountdownTimer(timerFor);
        currentTimer.start();
        LOG.info("Started animation timer");
        return 0;
    }

    public Runnable getHideCallback() {
        return hideCallback;
    }

    public void setHideCallback(Runnable hideCallback) {
        this.hideCallback = hideCallback;
    }

    private final class CountdownTimer extends AnimationTimer {

        private Duration timer;
        private long lastTimerCall = System.nanoTime();

        CountdownTimer(double duration) {
            this.timer = Duration.seconds(duration);
        }

        @Override
        public void handle(long now) {
            if (now > lastTimerCall + 1000000000L) {
                timer = timer.subtract(Duration.seconds(1.0));
                int remainingSeconds = (int) timer.toSeconds();
                int m = remainingSeconds / secondsPerMin;
                int s = remainingSeconds % secondsPerMin;
                if (m == 0 && s == 0) {
                    this.stop();
                }
                updateTiles(m, s);
                lastTimerCall = now;
            }
        }

        /** Finishes or skips the break: hides the overlay. */
        @Override
        public void stop() {
            hideCallback.run();
            LOG.info("stopped animation timer");
            super.stop();
        }

        void cancelSilently() {
            super.stop();
        }
    }

    private void updateTiles(int seconds) {
        updateTiles(seconds / secondsPerMin, seconds % secondsPerMin);
    }

    private void updateTiles(int m, int s) {
        minuteProperty.setValue(String.format("%02d", m));
        secondProperty.setValue(String.format("%02d", s));
    }
}
