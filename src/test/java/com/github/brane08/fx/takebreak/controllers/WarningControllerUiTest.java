package com.github.brane08.fx.takebreak.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class WarningControllerUiTest {

    private WarningController controller;
    private Stage stage;

    @Start
    void start(Stage stage) throws Exception {
        this.stage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/warning.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        stage.setScene(new Scene(root));
        stage.show();
    }

    @Test
    void displaysDefaultMessage(FxRobot robot) {
        Label label = robot.lookup("#message").query();
        assertEquals("Break coming up", label.getText());
    }

    @Test
    void setStageAutoClosesAfterDelay(FxRobot robot) throws TimeoutException {
        assertTrue(stage.isShowing());

        WaitForAsyncUtils.asyncFx(() -> controller.setStage(stage));
        WaitForAsyncUtils.waitForFxEvents();

        WaitForAsyncUtils.waitFor(6, TimeUnit.SECONDS, () -> !stage.isShowing());
        assertFalse(stage.isShowing());
    }
}
