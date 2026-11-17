package com.github.brane08.fx.takebreak;

import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.inject.Injector;
import com.github.brane08.fx.takebreak.tasks.BreakSchedule;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
        controller.setHideCallback(hideCallback);
        initStage(rootStage, parent);
        systemTray(controller);
        final BreakConfig breakConfig = Injector.resolveNamed("breakConfig");
        final Future<?> future = scheduler.schedule(
                new BreakSchedule(counter, rootStage, skipItem, controller::startTimer),
                breakConfig.spacing(), TimeUnit.MINUTES);
        monitorPool.submit(() -> {
            try {
                while (true) {
                    if (scheduler.isShutdown()) {
                        break;
                    }
                    future.get();
                }
            } catch (InterruptedException e) {
                LOG.info("Timer stopped");
            } catch (Exception e) {
                LOG.error("", e);
            }
        });
    }

    @Override
    public void stop() throws Exception {
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

    private void systemTray(BreakController controller) throws IOException {
        if (SystemTray.isSupported()) {
            var tray = SystemTray.getSystemTray();
            var image = ImageIO.read(getClass().getResource("/coffee.png"));
            var popup = new PopupMenu();
            var trayIcon = new TrayIcon(image, "Take Break", popup);

            skipItem.setEnabled(false);
            skipItem.addActionListener((e) -> {
                Platform.runLater(() -> {
                    controller.stopTimer();
                });
            });
            popup.add(skipItem);

            var exitItem = new MenuItem("Exit");
            exitItem.addActionListener((e) -> {
                cleanup.run();
                Platform.exit();
                tray.remove(trayIcon);
                System.exit(0);
            });
            popup.add(exitItem);

            trayIcon.setImageAutoSize(true);
            try {
                tray.add(trayIcon);
                Taskbar.getTaskbar().setIconImage(image);
            } catch (AWTException e) {
                LOG.error("Can't add to tray");
            }
        } else {
            LOG.error("Tray unavailable");
        }
    }

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(ApplicationLock::releaseLock));
        ApplicationLock.tryToGetLock();
        Injector.initDefault();
        launch(BreakApplication.class);
    }
}
