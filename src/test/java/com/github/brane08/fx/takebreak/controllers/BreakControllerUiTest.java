package com.github.brane08.fx.takebreak.controllers;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.util.WaitForAsyncUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
class BreakControllerUiTest {

    private BreakController controller;
    private final AtomicBoolean hideCalled = new AtomicBoolean(false);
    private final AtomicInteger hideCallCount = new AtomicInteger(0);

    @Start
    void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        Parent root = loader.load();
        controller = loader.getController();
        controller.setHideCallback(() -> {
            hideCalled.set(true);
            hideCallCount.incrementAndGet();
        });
        stage.setScene(new Scene(root));
        stage.show();
    }

    @BeforeEach
    void resetFlag() {
        hideCalled.set(false);
        hideCallCount.set(0);
    }

    @Test
    void clickingSkipStopsTimerAndInvokesHideCallback(FxRobot robot) {
        WaitForAsyncUtils.asyncFx(() -> controller.startTimer(60));
        WaitForAsyncUtils.waitForFxEvents();

        robot.clickOn("#btnSkip");
        WaitForAsyncUtils.waitForFxEvents();

        assertTrue(hideCalled.get());
    }

    @Test
    void skippingMidCountdownStopsTimerAfterTicking(FxRobot robot) throws TimeoutException {
        WaitForAsyncUtils.asyncFx(() -> controller.startTimer(5));
        WaitForAsyncUtils.waitForFxEvents();

        Label seconds = robot.lookup("#seconds").queryAs(Label.class);
        WaitForAsyncUtils.waitFor(3, TimeUnit.SECONDS, () -> !"05".equals(seconds.getText()));

        robot.clickOn("#btnSkip");
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(1, hideCallCount.get());
    }

    @Test
    void restartingTimerReplacesCurrentTimerReference(FxRobot robot) {
        WaitForAsyncUtils.asyncFx(() -> controller.startTimer(60));
        WaitForAsyncUtils.waitForFxEvents();

        WaitForAsyncUtils.asyncFx(() -> controller.startTimer(3));
        WaitForAsyncUtils.waitForFxEvents();

        Label minutes = robot.lookup("#minutes").queryAs(Label.class);
        Label seconds = robot.lookup("#seconds").queryAs(Label.class);
        assertEquals("00", minutes.getText());
        assertEquals("03", seconds.getText());

        robot.clickOn("#btnSkip");
        WaitForAsyncUtils.waitForFxEvents();

        assertEquals(1, hideCallCount.get());
    }
}
