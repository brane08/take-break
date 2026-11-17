# Pre-Break Notification Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show a configurable JavaFX toast overlay N seconds before each break; toast auto-dismisses after 5s; warning time is 0–120s settable via a new Settings slider.

**Architecture:** A second `WarningSchedule` Runnable is submitted to the existing `ScheduledExecutorService` with `initialDelay = max(0, spacing − warningTime)` and the same period as the break task. Both futures are stored in `BreakApplication` and cancelled/rescheduled together. `warningTime == 0` skips submission entirely.

**Tech Stack:** Java 21, JavaFX 21 (`PauseTransition`, `StageStyle.TRANSPARENT`), FXML, Maven.

---

## File Map

| Action | File |
|---|---|
| Modify | `src/main/java/com/github/brane08/fx/takebreak/domain/BreakConfig.java` |
| Modify | `src/test/java/com/github/brane08/fx/takebreak/domain/BreakConfigTest.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/tasks/WarningSchedule.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/controllers/WarningController.java` |
| Create | `src/main/resources/views/warning.fxml` |
| Modify | `src/main/resources/views/config.fxml` |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java` |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java` |

---

## Task 1: Extend BreakConfig with warningTime (TDD)

**Files:**
- Modify: `src/main/java/com/github/brane08/fx/takebreak/domain/BreakConfig.java`
- Modify: `src/test/java/com/github/brane08/fx/takebreak/domain/BreakConfigTest.java`

- [ ] **Step 1: Update BreakConfigTest with failing assertions**

Replace the full content of `BreakConfigTest.java`:

```java
package com.github.brane08.fx.takebreak.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BreakConfigTest {

    @Test
    void defaultConstantsAreDefined() {
        assertEquals(60, BreakConfig.DEFAULT_SMALL);
        assertEquals(300, BreakConfig.DEFAULT_LONG);
        assertEquals(1200, BreakConfig.DEFAULT_SPACING);
        assertEquals(30, BreakConfig.DEFAULT_WARNING);
    }

    @Test
    void shortBreakOnNonMultipleOfThree() {
        var config = new BreakConfig(5, 10, 600, 30);
        assertEquals(5, config.getBreakTime(1));
        assertEquals(5, config.getBreakTime(2));
        assertEquals(5, config.getBreakTime(4));
        assertEquals(5, config.getBreakTime(5));
    }

    @Test
    void longBreakOnMultipleOfThree() {
        var config = new BreakConfig(5, 10, 600, 30);
        assertEquals(10, config.getBreakTime(3));
        assertEquals(10, config.getBreakTime(6));
        assertEquals(10, config.getBreakTime(9));
    }

    @Test
    void saveAndLoadRoundTrip() {
        var original = new BreakConfig(15, 120, 300, 45);
        original.save();

        var loaded = BreakConfig.fromFile();

        assertEquals(original.smallBreak(), loaded.smallBreak());
        assertEquals(original.longBreak(), loaded.longBreak());
        assertEquals(original.spacing(), loaded.spacing());
        assertEquals(original.warningTime(), loaded.warningTime());
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

```bash
mvn -q test -Dtest="BreakConfigTest" 2>&1 | head -20
```

Expected: compilation error — `BreakConfig(int,int,int,int)` does not exist.

- [ ] **Step 3: Update BreakConfig record**

Replace the full content of `BreakConfig.java`:

```java
package com.github.brane08.fx.takebreak.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record BreakConfig(int smallBreak, int longBreak, int spacing, int warningTime) {

    public static final int DEFAULT_SMALL = 60;
    public static final int DEFAULT_LONG = 300;
    public static final int DEFAULT_SPACING = 1200;
    public static final int DEFAULT_WARNING = 30;

    private static final Path CONFIG_FILE = Path.of(
            System.getProperty("user.home"), ".config", "take-break", "config.properties");

    public int getBreakTime(int instance) {
        return ((instance % 3) == 0) ? longBreak : smallBreak;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            var props = new Properties();
            props.setProperty("small", String.valueOf(smallBreak));
            props.setProperty("long", String.valueOf(longBreak));
            props.setProperty("spacing", String.valueOf(spacing));
            props.setProperty("warning", String.valueOf(warningTime));
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "take-break configuration");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + CONFIG_FILE, e);
        }
    }

    public static BreakConfig fromFile() {
        if (!Files.exists(CONFIG_FILE)) {
            BreakConfig defaults = new BreakConfig(DEFAULT_SMALL, DEFAULT_LONG, DEFAULT_SPACING, DEFAULT_WARNING);
            defaults.save();
            return defaults;
        }
        var props = new Properties();
        try (InputStream in = Files.newInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config: " + CONFIG_FILE, e);
        }
        return new BreakConfig(
                Integer.parseInt(props.getProperty("small", String.valueOf(DEFAULT_SMALL))),
                Integer.parseInt(props.getProperty("long", String.valueOf(DEFAULT_LONG))),
                Integer.parseInt(props.getProperty("spacing", String.valueOf(DEFAULT_SPACING))),
                Integer.parseInt(props.getProperty("warning", String.valueOf(DEFAULT_WARNING))));
    }
}
```

- [ ] **Step 4: Run tests — expect pass**

```bash
mvn -q test -Dtest="BreakConfigTest" 2>&1; echo "EXIT:$?"
```

Expected: `EXIT:0`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/github/brane08/fx/takebreak/domain/BreakConfig.java \
        src/test/java/com/github/brane08/fx/takebreak/domain/BreakConfigTest.java
git commit -m "Add warningTime to BreakConfig; DEFAULT_WARNING=30s"
```

---

## Task 2: Create WarningSchedule

**Files:**
- Create: `src/main/java/com/github/brane08/fx/takebreak/tasks/WarningSchedule.java`

- [ ] **Step 1: Create WarningSchedule.java**

```java
package com.github.brane08.fx.takebreak.tasks;

import javafx.application.Platform;

public final class WarningSchedule implements Runnable {

    private final Runnable showToast;

    public WarningSchedule(Runnable showToast) {
        this.showToast = showToast;
    }

    @Override
    public void run() {
        Platform.runLater(showToast);
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
mvn -q compile 2>&1; echo "EXIT:$?"
```

Expected: `EXIT:0`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/github/brane08/fx/takebreak/tasks/WarningSchedule.java
git commit -m "Add WarningSchedule runnable"
```

---

## Task 3: Create warning.fxml and WarningController

**Files:**
- Create: `src/main/resources/views/warning.fxml`
- Create: `src/main/java/com/github/brane08/fx/takebreak/controllers/WarningController.java`

- [ ] **Step 1: Create warning.fxml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.layout.StackPane?>
<StackPane xmlns="http://javafx.com/javafx"
           xmlns:fx="http://javafx.com/fxml"
           fx:controller="com.github.brane08.fx.takebreak.controllers.WarningController"
           style="-fx-background-color: rgba(30,30,30,0.85); -fx-background-radius: 12; -fx-padding: 12 20 12 20;"
           prefWidth="300" prefHeight="70">
    <Label fx:id="message" text="Break coming up"
           style="-fx-text-fill: white; -fx-font-size: 14;"/>
</StackPane>
```

- [ ] **Step 2: Create WarningController.java**

```java
package com.github.brane08.fx.takebreak.controllers;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.util.Duration;

public class WarningController {

    @FXML
    Label message;

    private Stage stage;

    public void setStage(Stage stage) {
        this.stage = stage;
        PauseTransition delay = new PauseTransition(Duration.seconds(5));
        delay.setOnFinished(e -> stage.close());
        delay.play();
    }
}
```

- [ ] **Step 3: Verify it compiles**

```bash
mvn -q compile 2>&1; echo "EXIT:$?"
```

Expected: `EXIT:0`

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/views/warning.fxml \
        src/main/java/com/github/brane08/fx/takebreak/controllers/WarningController.java
git commit -m "Add warning toast FXML and WarningController"
```

---

## Task 4: Wire WarningSchedule into BreakApplication

**Files:**
- Modify: `src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java`

- [ ] **Step 1: Update BreakApplication.java**

Replace the full content:

```java
package com.github.brane08.fx.takebreak;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import com.github.brane08.fx.takebreak.controllers.BreakController;
import com.github.brane08.fx.takebreak.controllers.ConfigController;
import com.github.brane08.fx.takebreak.controllers.WarningController;
import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.inject.Injector;
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
    private volatile Future<?> warningFuture;
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
        if (schedulerFuture != null) schedulerFuture.cancel(false);
        if (warningFuture != null) warningFuture.cancel(false);
        schedulerFuture = scheduler.scheduleAtFixedRate(
                new BreakSchedule(counter, defaultStage, skipItem, breakController::startTimer),
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
        if (System.getProperty("glass.gtk.uiScale") == null) {
            System.setProperty("glass.gtk.uiScale", "auto");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(ApplicationLock::releaseLock));
        ApplicationLock.tryToGetLock();
        Injector.initDefault();
        launch(BreakApplication.class);
    }
}
```

- [ ] **Step 2: Verify it compiles**

```bash
mvn -q compile 2>&1; echo "EXIT:$?"
```

Expected: `EXIT:0`

- [ ] **Step 3: Run tests**

```bash
mvn -q test -Dtest="BreakConfigTest" 2>&1; echo "EXIT:$?"
```

Expected: `EXIT:0`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java
git commit -m "Wire WarningSchedule into BreakApplication; add showWarningToast"
```

---

## Task 5: Add warning slider to Settings UI

**Files:**
- Modify: `src/main/resources/views/config.fxml`
- Modify: `src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java`

- [ ] **Step 1: Update config.fxml**

Replace the full content:

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Button?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.Slider?>
<?import javafx.scene.layout.ColumnConstraints?>
<?import javafx.scene.layout.GridPane?>
<?import javafx.scene.layout.HBox?>
<?import javafx.scene.layout.RowConstraints?>
<GridPane hgap="20" vgap="15" xmlns="http://javafx.com/javafx"
		  xmlns:fx="http://javafx.com/fxml"
		  fx:controller="com.github.brane08.fx.takebreak.controllers.ConfigController">
	<padding>
		<Insets top="20" right="20" bottom="20" left="20"/>
	</padding>
	<Label text="Short Break (seconds):" GridPane.columnIndex="0" GridPane.rowIndex="0"/>
	<Slider fx:id="smallBreakSlider" blockIncrement="10.0" majorTickUnit="20.0" max="120.0" min="10.0"
			minorTickCount="1" showTickMarks="true" showTickLabels="true" snapToTicks="true"
			GridPane.columnIndex="1" GridPane.rowIndex="0"/>
	<Label text="Long Break (seconds):" GridPane.columnIndex="0" GridPane.rowIndex="1"/>
	<Slider fx:id="longBreakSlider" blockIncrement="60.0" majorTickUnit="60.0" max="600.0" min="60.0"
			showTickMarks="true" showTickLabels="true" snapToTicks="true"
			GridPane.columnIndex="1" GridPane.rowIndex="1"/>
	<Label text="Spacing (seconds):" GridPane.columnIndex="0" GridPane.rowIndex="2"/>
	<Slider fx:id="spacingSlider" blockIncrement="60.0" majorTickUnit="600.0" max="3600.0" min="60.0"
			minorTickCount="4" showTickMarks="true" showTickLabels="true" snapToTicks="true"
			GridPane.columnIndex="1" GridPane.rowIndex="2"/>
	<Label text="Warning (seconds):" GridPane.columnIndex="0" GridPane.rowIndex="3"/>
	<Slider fx:id="warningSlider" blockIncrement="10.0" majorTickUnit="30.0" max="120.0" min="0.0"
			showTickMarks="true" showTickLabels="true" snapToTicks="true"
			GridPane.columnIndex="1" GridPane.rowIndex="3"/>
	<HBox alignment="CENTER_RIGHT" spacing="10" GridPane.columnIndex="0" GridPane.rowIndex="4" GridPane.columnSpan="2">
		<padding>
			<Insets top="10"/>
		</padding>
		<Button text="Reset" onAction="#resetDefaults"/>
		<Button text="Save" onAction="#saveConfig"/>
	</HBox>
	<columnConstraints>
		<ColumnConstraints/>
		<ColumnConstraints hgrow="ALWAYS" minWidth="250"/>
	</columnConstraints>
	<rowConstraints>
		<RowConstraints/>
		<RowConstraints/>
		<RowConstraints/>
		<RowConstraints/>
		<RowConstraints/>
	</rowConstraints>
</GridPane>
```

- [ ] **Step 2: Update ConfigController.java**

Replace the full content:

```java
package com.github.brane08.fx.takebreak.controllers;

import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Slider;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class ConfigController implements Initializable {

    @FXML private Slider smallBreakSlider;
    @FXML private Slider longBreakSlider;
    @FXML private Slider spacingSlider;
    @FXML private Slider warningSlider;

    private Consumer<BreakConfig> rescheduleCallback;

    public void setRescheduleCallback(Consumer<BreakConfig> callback) {
        this.rescheduleCallback = callback;
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        BreakConfig config = Injector.resolveNamed("breakConfig");
        smallBreakSlider.setValue(config.smallBreak());
        longBreakSlider.setValue(config.longBreak());
        spacingSlider.setValue(config.spacing());
        warningSlider.setValue(config.warningTime());
    }

    @FXML
    public void resetDefaults() {
        smallBreakSlider.setValue(BreakConfig.DEFAULT_SMALL);
        longBreakSlider.setValue(BreakConfig.DEFAULT_LONG);
        spacingSlider.setValue(BreakConfig.DEFAULT_SPACING);
        warningSlider.setValue(BreakConfig.DEFAULT_WARNING);
    }

    @FXML
    public void saveConfig() {
        int small = (int) smallBreakSlider.getValue();
        int large = (int) longBreakSlider.getValue();
        int spacing = (int) spacingSlider.getValue();
        int warning = (int) warningSlider.getValue();
        BreakConfig updated = new BreakConfig(small, large, spacing, warning);
        updated.save();
        Injector.registerNamed("breakConfig", updated);
        if (rescheduleCallback != null) {
            rescheduleCallback.accept(updated);
        }
        smallBreakSlider.getScene().getWindow().hide();
    }
}
```

- [ ] **Step 3: Compile and run tests**

```bash
mvn -q compile 2>&1; echo "COMPILE:$?"
mvn -q test -Dtest="BreakConfigTest" 2>&1; echo "TEST:$?"
```

Expected: `COMPILE:0` then `TEST:0`

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/views/config.fxml \
        src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java
git commit -m "Add warning slider to Settings UI"
```

---

## Task 6: Manual smoke test + update memory

**Files:**
- Modify: `.claude/memory/todo.md`
- Modify: `.claude/memory/pending_work.md`
- Modify: `.claude/memory/project_overview.md`

- [ ] **Step 1: Build fat-jar**

```bash
mvn -q package 2>&1; echo "EXIT:$?"
```

Expected: `EXIT:0`, file `target/take-break-app.jar` created.

- [ ] **Step 2: Manual smoke test**

Open Settings → set Warning to 10s, Spacing to 60s → Save.
Confirm: toast appears ~50s after save, reads "Break coming up", dismisses after 5s.
Confirm: break overlay appears at 60s.
Confirm: Reset button restores Warning to 30s.
Confirm: Warning=0 produces no toast.

- [ ] **Step 3: Update project_overview.md**

In the "Config defaults" line, add:
```
- Config defaults: `DEFAULT_SMALL=60s`, `DEFAULT_LONG=300s`, `DEFAULT_SPACING=1200s`, `DEFAULT_WARNING=30s`
```

Update the Source files table to add WarningController and WarningSchedule rows.
Update the module-info.java snippet to reflect the current state (no jackson, no tilesfx).

- [ ] **Step 4: Update todo.md — mark feature DONE**

Add under a new `## Features` section:
```
DONE: Pre-break toast notification (warningTime field, WarningSchedule, WarningController, warning.fxml)
```

- [ ] **Step 5: Final commit**

```bash
git add .claude/memory/
git commit -m "Pre-break notification complete; update memory"
```
