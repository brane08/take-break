package com.github.brane08.fx.takebreak;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.github.brane08.fx.takebreak.controllers.BreakController;
import com.github.brane08.fx.takebreak.controllers.ConfigController;
import com.github.brane08.fx.takebreak.controllers.WarningController;
import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.inject.Injector;
import com.github.brane08.fx.takebreak.idle.IdleDetector;
import com.github.brane08.fx.takebreak.idle.IdleDetectorFactory;
import com.github.brane08.fx.takebreak.tasks.BreakSchedule;
import com.github.brane08.fx.takebreak.tasks.WarningSchedule;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ExecutorService monitorPool = Executors.newSingleThreadExecutor();
    private final MenuItem skipItem = new MenuItem("Skip Break");
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicLong epoch = new AtomicLong(0);
    private final IdleDetector idleDetector = IdleDetectorFactory.create();
    private Stage defaultStage;
    private BreakController breakController;
    private volatile Future<?> schedulerFuture;
    private volatile Future<?> warningFuture;
    private volatile Stage activeToast;
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
        final BreakConfig breakConfig = Injector.resolveNamed(Constants.DI_BREAK_CONFIG);
        LOG.info("Using configs: {}", breakConfig.toString());
        schedulerFuture = scheduler.scheduleAtFixedRate(
                new BreakSchedule(counter, rootStage, skipItem, controller::startTimer, idleDetector, epoch, epoch.get()),
                breakConfig.spacing(), breakConfig.spacing(), TimeUnit.SECONDS);
        scheduleWarning(breakConfig);
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
        if (schedulerFuture != null) schedulerFuture.cancel(true);
        if (warningFuture != null) warningFuture.cancel(true);
        warningFuture = null;
        long myEpoch = epoch.incrementAndGet();
        schedulerFuture = scheduler.scheduleAtFixedRate(
                new BreakSchedule(counter, defaultStage, skipItem, breakController::startTimer, idleDetector, epoch, myEpoch),
                config.spacing(), config.spacing(), TimeUnit.SECONDS);
        scheduleWarning(config);
        LOG.info("Rescheduled with spacing={}s warningTime={}s", config.spacing(), config.warningTime());
    }

    private void scheduleWarning(BreakConfig config) {
        if (config.warningTime() <= 0) return;
        long delay = Math.max(0, config.spacing() - config.warningTime());
        warningFuture = scheduler.scheduleAtFixedRate(
                new WarningSchedule(this::showWarningToast),
                delay, config.spacing(), TimeUnit.SECONDS);
    }

    private void showWarningToast() {
        if (activeToast != null && activeToast.isShowing()) {
            activeToast.close();
        }
        try {
            var loader = new FXMLLoader(getClass().getResource("/views/warning.fxml"));
            Parent root = loader.load();
            WarningController controller = loader.getController();
            Stage toastStage = new Stage();
            toastStage.initStyle(StageStyle.TRANSPARENT);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            toastStage.setScene(scene);
            toastStage.setAlwaysOnTop(true);
            toastStage.setResizable(false);
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            toastStage.setX(bounds.getMinX() + bounds.getWidth() - 320);
            toastStage.setY(bounds.getMinY() + bounds.getHeight() - 90);
            controller.setStage(toastStage);
            toastStage.show();
            activeToast = toastStage;
        } catch (IOException e) {
            LOG.error("Failed to show warning toast", e);
        }
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

    private void systemTray(BreakController controller) {
        if (!SystemTray.isSupported()) {
            LOG.warn("System tray unavailable — running without a tray icon");
            return;
        }
        try {
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
        } catch (Exception e) {
            LOG.error("Failed to initialize system tray icon — running without a tray icon", e);
        }
    }

    public static void main(String[] args) {
        if (System.getProperty("glass.gtk.uiScale") == null) {
            System.setProperty("glass.gtk.uiScale", "auto");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(ApplicationLock::releaseLock));
        ApplicationLock.tryToGetLock();
        Injector.initDefault();
        launch(BreakApplication.class);
    }
}
