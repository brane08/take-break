package com.github.brane08.fx.takebreak.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class BreakControllerUiTest {

    private BreakController controller;
    private final AtomicBoolean hideCalled = new AtomicBoolean(false);

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        controller.setHideCallback(() -> hideCalled.set(true));
        stage.setScene(new Scene(root));
        stage.show();
    }

    @BeforeEach
    void resetFlag() {
        hideCalled.set(false);
    }

    @Test
    void clickingSkipStopsTimerAndInvokesHideCallback(FxRobot robot) {
        WaitForAsyncUtils.asyncFx(() -> controller.startTimer(60));
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#btnSkip");
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(hideCalled.get());
    }
}
