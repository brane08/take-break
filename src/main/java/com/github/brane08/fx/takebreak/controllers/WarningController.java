package com.github.brane08.fx.takebreak.controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

public class WarningController {

    @FXML
    private Label message;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(e -> stage.close());
        delay.play();
    }
}
