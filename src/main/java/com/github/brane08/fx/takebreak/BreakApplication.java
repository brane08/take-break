package com.github.brane08.fx.takebreak;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.github.brane08.fx.takebreak.controllers.BreakController;
import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.inject.Injector;
import com.github.brane08.fx.takebreak.tasks.BreakSchedule;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BreakApplication extends Application {

    private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_NAME);
    private static final int SCALE_FACTOR = 70;
    public static double WIDTH = 0.0;
    public static double HEIGHT = 0.0;
    public static double X = 0.0;
    public static double Y = 0.0;

    static {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        WIDTH = (screenBounds.getWidth() * SCALE_FACTOR) / 100;
        HEIGHT = (screenBounds.getHeight() * SCALE_FACTOR) / 100;
        X = (screenBounds.getWidth() - WIDTH) / 2;
        Y = (screenBounds.getHeight() - HEIGHT) / 2;
    }

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService monitorPool = Executors.newSingleThreadExecutor();
    private final MenuItem skipItem = new MenuItem("Skip Break");
    private final AtomicInteger counter = new AtomicInteger(0);
    private Stage defaultStage;
    private BreakController breakController;
    private volatile Future<?> schedulerFuture;
    private final Runnable cleanup = () -> {
        scheduler.shutdownNow();
        monitorPool.shutdownNow();
    };

    private final Runnable hideCallback = () -> {
        skipItem.setEnabled(false);
        defaultStage.hide();
    };

    @Override
    public void start(Stage rootStage) throws Exception {
        this.defaultStage = rootStage;
        LOG.info("starting app");
        final var loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        final Parent parent = loader.load();
        final BreakController controller = loader.getController();
        this.breakController = controller;
        controller.setHideCallback(hideCallback);
        initStage(rootStage, parent);
        systemTray(controller);
        final BreakConfig breakConfig = Injector.resolveNamed("breakConfig");
        LOG.info("Using configs: {}", breakConfig.toString());
        schedulerFuture = scheduler.scheduleAtFixedRate(
                new BreakSchedule(counter, rootStage, skipItem, controller::startTimer),
                breakConfig.spacing(), breakConfig.spacing(), TimeUnit.SECONDS);
        monitorPool.submit(() -> {
            try {
                schedulerFuture.get();
            } catch (InterruptedException e) {
                LOG.info("Timer stopped");
            } catch (CancellationException e) {
                LOG.info("Scheduler cancelled");
            } catch (Exception e) {
                LOG.error("", e);
            }
        });
    }

    @Override
    public void stop() throws Exception {
        cleanup.run();
        super.stop();
    }

    private void initStage(Stage rootStage, Parent parent) {
        Platform.setImplicitExit(false);
        rootStage.setScene(new Scene(parent, WIDTH, HEIGHT));
        rootStage.initStyle(StageStyle.UNDECORATED);
        rootStage.setResizable(false);
        rootStage.setAlwaysOnTop(true);
        rootStage.setX(X);
        rootStage.setY(Y);
    }

    private void reschedule(BreakConfig config) {
        if (schedulerFuture != null) {
            schedulerFuture.cancel(false);
        }
        schedulerFuture = scheduler.scheduleAtFixedRate(
                new BreakSchedule(counter, defaultStage, skipItem, breakController::startTimer),
                config.spacing(), config.spacing(), TimeUnit.SECONDS);
        LOG.info("Rescheduled with spacing={}s", config.spacing());
    }

    private void settingStage(Parent parent) {
        Stage settingsStage = new Stage();
        settingsStage.setScene(new Scene(parent));
        settingsStage.setResizable(false);
        settingsStage.setAlwaysOnTop(true);
        settingsStage.setX(X);
        settingsStage.setY(Y);
        settingsStage.showAndWait();
    }

    private void systemTray(BreakController controller) throws IOException {
        var image = new Image(getClass().getResourceAsStream("/coffee.png"));
        final var trayIcon = new FXTrayIcon.Builder(defaultStage, image)
                .menuItem("Skip Break", e -> controller.stopTimer())
                .menuItem("Settings", e -> {
                    final var loader = new FXMLLoader(getClass().getResource("/views/config.fxml"));
                    try {
                        final Parent parent = loader.load();
                        ConfigController cc = loader.getController();
                        cc.setRescheduleCallback(this::reschedule);
                        settingStage(parent);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .menuItem("Exit", e -> {
                    cleanup.run();
                    Platform.exit();
                    System.exit(0);
                })
                .show()
                .build();
    }

    public static void main(String[] args) {
        // Allow auto-detection of HiDPI scale when not set explicitly via -Dglass.gtk.uiScale
        if (System.getProperty("glass.gtk.uiScale") == null) {
            System.setProperty("glass.gtk.uiScale", "auto");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(ApplicationLock::releaseLock));
        ApplicationLock.tryToGetLock();
        Injector.initDefault();
        launch(BreakApplication.class);
    }
}
