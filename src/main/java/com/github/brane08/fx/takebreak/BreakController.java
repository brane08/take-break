package com.github.brane08.fx.takebreak;

import eu.hansolo.tilesfx.Tile;
import javafx.animation.AnimationTimer;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
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
    private AnimationTimer currentTimer;
    private Runnable hideCallback;

    @FXML
    VBox rootPane;
    @FXML
    Tile minutes;
    @FXML
    Tile seconds;
    @FXML
    Button btnSkip;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        rootPane.setBackground(new Background(new BackgroundFill(Tile.BACKGROUND.brighter(), CornerRadii.EMPTY, Insets.EMPTY)));
        minutes.descriptionProperty().bindBidirectional(minuteProperty);
        seconds.descriptionProperty().bindBidirectional(secondProperty);
    }

    public void stopTimer() {
        currentTimer.stop();
    }

    public Integer startTimer(int timerFor) {
        LOG.info("Starting animation timer");
        updateTiles(timerFor);
        currentTimer = getAnimationTimer((double) timerFor);
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

    private AnimationTimer getAnimationTimer(double duration) {
        return new AnimationTimer() {

            Duration timer = Duration.seconds(duration);
            long lastTimerCall = System.nanoTime();

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

            @Override
            public void stop() {
                hideCallback.run();
                LOG.info("stopped animation timer");
                super.stop();
            }
        };
    }

    private void updateTiles(int seconds) {
        updateTiles(seconds / secondsPerMin, seconds % secondsPerMin);
    }

    private void updateTiles(int m, int s) {
        minuteProperty.setValue(String.format("%02d", m));
        secondProperty.setValue(String.format("%02d", s));
    }
}
