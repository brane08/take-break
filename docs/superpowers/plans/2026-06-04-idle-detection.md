# Idle Detection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Skip a scheduled break when the user has been idle for ≥ their configured idle threshold, using native OS APIs via JNA for accurate keyboard+mouse detection on macOS, Windows, and Linux.

**Architecture:** A new `idle` package provides an `IdleDetector` interface with three platform implementations selected by `IdleDetectorFactory`. The detector is created once in `BreakApplication`, injected into `BreakSchedule`, and queried at each break tick. The idle threshold is a 5th `BreakConfig` field, configurable via a new Settings slider.

**Tech Stack:** Java 21, JavaFX 21, JNA 5.14.0, jna-platform 5.14.0, JPMS module `brane.fx.takebreak`.

---

## File Map

| Action | File |
|--------|------|
| Modify | `pom.xml` |
| Modify | `src/main/java/module-info.java` |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/domain/BreakConfig.java` |
| Modify | `src/test/java/com/github/brane08/fx/takebreak/domain/BreakConfigTest.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/idle/IdleDetector.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/idle/IdleDetectorFactory.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/idle/MacIdleDetector.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/idle/WindowsIdleDetector.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/idle/LinuxIdleDetector.java` |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/tasks/BreakSchedule.java` |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java` |
| Modify | `src/main/resources/views/config.fxml` |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java` |
| Modify | `src/test/java/com/github/brane08/fx/takebreak/controllers/ConfigControllerTest.java` |

---

### Task 1: Add JNA dependencies and open idle package

**Files:**
- Modify: `pom.xml`
- Modify: `src/main/java/module-info.java`

- [ ] **Step 1: Add JNA deps to pom.xml**

In `pom.xml`, after the `openjfx-monocle` test dependency block and before `</dependencies>`, add:

```xml
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna</artifactId>
    <version>5.14.0</version>
</dependency>
<dependency>
    <groupId>net.java.dev.jna</groupId>
    <artifactId>jna-platform</artifactId>
    <version>5.14.0</version>
</dependency>
```

- [ ] **Step 2: Update module-info.java**

In `src/main/java/module-info.java`, add after `requires com.dustinredmond.fxtrayicon;`:

```java
requires com.sun.jna;
requires com.sun.jna.platform;
opens com.github.brane08.fx.takebreak.idle;
```

The full file should look like:

```java
module brane.fx.takebreak {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires org.slf4j;
    requires com.dustinredmond.fxtrayicon;
    requires javafx.base;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    opens com.github.brane08.fx.takebreak.idle;
    opens com.github.brane08.fx.takebreak;
    exports com.github.brane08.fx.takebreak;
    exports com.github.brane08.fx.takebreak.domain;
    exports com.github.brane08.fx.takebreak.controllers;
    opens com.github.brane08.fx.takebreak.controllers;
}
```

- [ ] **Step 3: Verify compile**

```bash
cd /Users/bhushanr/incubator/source/take-break && mvn -q compile 2>&1 | tail -5
```

Expected: no output, exit 0.

- [ ] **Step 4: Commit**

```bash
git add pom.xml src/main/java/module-info.java
git commit -m "build: add JNA deps and open idle package"
```

---

### Task 2: Add idleThreshold to BreakConfig

**Files:**
- Modify: `src/main/java/com/github/brane08/fx/takebreak/domain/BreakConfig.java`
- Modify: `src/test/java/com/github/brane08/fx/takebreak/domain/BreakConfigTest.java`

- [ ] **Step 1: Write failing tests first**

In `BreakConfigTest.java`, update `defaultConstantsAreDefined` to include the new constant, update all `BreakConfig` constructors to 5-arg, and add `idleThreshold` to the round-trip test. Replace the entire file with:

```java
package com.github.brane08.fx.takebreak.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BreakConfigTest {

    @Test
    void defaultConstantsAreDefined() {
        assertEquals(60,  BreakConfig.DEFAULT_SMALL);
        assertEquals(300, BreakConfig.DEFAULT_LONG);
        assertEquals(1200, BreakConfig.DEFAULT_SPACING);
        assertEquals(30,  BreakConfig.DEFAULT_WARNING);
        assertEquals(300, BreakConfig.DEFAULT_IDLE);
    }

    @Test
    void shortBreakOnNonMultipleOfThree() {
        var config = new BreakConfig(5, 10, 600, 30, 300);
        assertEquals(5, config.getBreakTime(1));
        assertEquals(5, config.getBreakTime(2));
        assertEquals(5, config.getBreakTime(4));
        assertEquals(5, config.getBreakTime(5));
    }

    @Test
    void longBreakOnMultipleOfThree() {
        var config = new BreakConfig(5, 10, 600, 30, 300);
        assertEquals(10, config.getBreakTime(3));
        assertEquals(10, config.getBreakTime(6));
        assertEquals(10, config.getBreakTime(9));
    }

    @Test
    void saveAndLoadRoundTrip() {
        var original = new BreakConfig(15, 120, 300, 45, 240);
        original.save();

        var loaded = BreakConfig.fromFile();

        assertEquals(original.smallBreak(),    loaded.smallBreak());
        assertEquals(original.longBreak(),     loaded.longBreak());
        assertEquals(original.spacing(),       loaded.spacing());
        assertEquals(original.warningTime(),   loaded.warningTime());
        assertEquals(original.idleThreshold(), loaded.idleThreshold());
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
mvn test -Dtest="BreakConfigTest" 2>&1 | tail -15
```

Expected: compile error — `BreakConfig(int,int,int,int,int)` constructor not found.

- [ ] **Step 3: Update BreakConfig.java**

Replace the entire file `src/main/java/com/github/brane08/fx/takebreak/domain/BreakConfig.java`:

```java
package com.github.brane08.fx.takebreak.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public record BreakConfig(int smallBreak, int longBreak, int spacing,
                          int warningTime, int idleThreshold) {

    public static final int DEFAULT_SMALL   = 60;
    public static final int DEFAULT_LONG    = 300;
    public static final int DEFAULT_SPACING = 1200;
    public static final int DEFAULT_WARNING = 30;
    public static final int DEFAULT_IDLE    = 300;

    private static final Path CONFIG_FILE;
    static {
        String override = System.getProperty("take-break.config.dir");
        CONFIG_FILE = override != null
                ? Path.of(override, "config.properties")
                : Path.of(System.getProperty("user.home"), ".config", "take-break", "config.properties");
    }

    public int getBreakTime(int instance) {
        return ((instance % 3) == 0) ? longBreak : smallBreak;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            var props = new Properties();
            props.setProperty("small",   String.valueOf(smallBreak));
            props.setProperty("long",    String.valueOf(longBreak));
            props.setProperty("spacing", String.valueOf(spacing));
            props.setProperty("warning", String.valueOf(warningTime));
            props.setProperty("idle",    String.valueOf(idleThreshold));
            try (OutputStream out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "take-break configuration");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save config: " + CONFIG_FILE, e);
        }
    }

    public static BreakConfig fromFile() {
        if (!Files.exists(CONFIG_FILE)) {
            BreakConfig defaults = new BreakConfig(DEFAULT_SMALL, DEFAULT_LONG,
                    DEFAULT_SPACING, DEFAULT_WARNING, DEFAULT_IDLE);
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
                Integer.parseInt(props.getProperty("small",   String.valueOf(DEFAULT_SMALL))),
                Integer.parseInt(props.getProperty("long",    String.valueOf(DEFAULT_LONG))),
                Integer.parseInt(props.getProperty("spacing", String.valueOf(DEFAULT_SPACING))),
                Integer.parseInt(props.getProperty("warning", String.valueOf(DEFAULT_WARNING))),
                Integer.parseInt(props.getProperty("idle",    String.valueOf(DEFAULT_IDLE))));
    }
}
```

- [ ] **Step 4: Run tests — expect 4/4 pass**

```bash
mvn test -Dtest="BreakConfigTest" 2>&1 | tail -10
```

Expected: `Tests run: 4, Failures: 0, Errors: 0`.

- [ ] **Step 5: Fix compile errors in ConfigController.java**

`ConfigController.saveConfig()` builds a 4-arg `BreakConfig`. It now won't compile. Temporarily pass `BreakConfig.DEFAULT_IDLE` as the 5th arg (will be replaced properly in Task 6):

In `src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java`, find:
```java
BreakConfig updated = new BreakConfig(small, large, spacing, warning);
```
Replace with:
```java
BreakConfig updated = new BreakConfig(small, large, spacing, warning, BreakConfig.DEFAULT_IDLE);
```

- [ ] **Step 6: Verify compile**

```bash
mvn -q compile 2>&1 | tail -5
```

Expected: no output, exit 0.

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/github/brane08/fx/takebreak/domain/BreakConfig.java \
        src/test/java/com/github/brane08/fx/takebreak/domain/BreakConfigTest.java \
        src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java
git commit -m "feat: add idleThreshold to BreakConfig (5th component, default 300s)"
```

---

### Task 3: Create IdleDetector interface, factory, and implementations

**Files:**
- Create: `src/main/java/com/github/brane08/fx/takebreak/idle/IdleDetector.java`
- Create: `src/main/java/com/github/brane08/fx/takebreak/idle/IdleDetectorFactory.java`
- Create: `src/main/java/com/github/brane08/fx/takebreak/idle/MacIdleDetector.java`
- Create: `src/main/java/com/github/brane08/fx/takebreak/idle/WindowsIdleDetector.java`
- Create: `src/main/java/com/github/brane08/fx/takebreak/idle/LinuxIdleDetector.java`
- Test: `src/test/java/com/github/brane08/fx/takebreak/idle/IdleDetectorFactoryTest.java`

- [ ] **Step 1: Write the factory test first**

Create `src/test/java/com/github/brane08/fx/takebreak/idle/IdleDetectorFactoryTest.java`:

```java
package com.github.brane08.fx.takebreak.idle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IdleDetectorFactoryTest {

    @Test
    void factoryReturnsNonNull() {
        assertNotNull(IdleDetectorFactory.create());
    }

    @Test
    void factoryReturnsPlatformCorrectType() {
        IdleDetector detector = IdleDetectorFactory.create();
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            assertInstanceOf(MacIdleDetector.class, detector);
        } else if (os.contains("win")) {
            assertInstanceOf(WindowsIdleDetector.class, detector);
        } else {
            assertInstanceOf(LinuxIdleDetector.class, detector);
        }
    }

    @Test
    void linuxDetectorReturnsFallbackWhenXprintidleAbsent() {
        LinuxIdleDetector detector = new LinuxIdleDetector();
        // xprintidle may or may not be installed; either way must not throw
        long result = detector.getIdleSeconds();
        assertTrue(result >= 0, "Idle seconds must be non-negative");
    }
}
```

- [ ] **Step 2: Run to confirm failure**

```bash
mvn test -Dtest="IdleDetectorFactoryTest" 2>&1 | tail -10
```

Expected: compile error — classes not found yet.

- [ ] **Step 3: Create the directory**

```bash
mkdir -p src/main/java/com/github/brane08/fx/takebreak/idle
mkdir -p src/test/java/com/github/brane08/fx/takebreak/idle
```

- [ ] **Step 4: Create IdleDetector.java**

```java
package com.github.brane08.fx.takebreak.idle;

public interface IdleDetector {
    long getIdleSeconds();
}
```

- [ ] **Step 5: Create MacIdleDetector.java**

```java
package com.github.brane08.fx.takebreak.idle;

import com.sun.jna.Library;
import com.sun.jna.Native;

public final class MacIdleDetector implements IdleDetector {

    interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);
        // kCGEventSourceStateHIDSystemState=1, kCGAnyInputEventType=0xFFFFFFFF
        double CGEventSourceSecondsSinceLastEventType(int stateId, int eventType);
    }

    @Override
    public long getIdleSeconds() {
        return (long) CoreGraphics.INSTANCE
                .CGEventSourceSecondsSinceLastEventType(1, 0xFFFFFFFF);
    }
}
```

- [ ] **Step 6: Create WindowsIdleDetector.java**

```java
package com.github.brane08.fx.takebreak.idle;

import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinUser;

public final class WindowsIdleDetector implements IdleDetector {

    @Override
    public long getIdleSeconds() {
        WinUser.LASTINPUTINFO info = new WinUser.LASTINPUTINFO();
        User32.INSTANCE.GetLastInputInfo(info);
        // GetTickCount wraps at ~49 days; mask to handle wrap-around correctly
        long idleMs = (Kernel32.INSTANCE.GetTickCount() - info.dwTime) & 0xFFFFFFFFL;
        return idleMs / 1000;
    }
}
```

- [ ] **Step 7: Create LinuxIdleDetector.java**

```java
package com.github.brane08.fx.takebreak.idle;

public final class LinuxIdleDetector implements IdleDetector {

    @Override
    public long getIdleSeconds() {
        try {
            Process p = new ProcessBuilder("xprintidle").start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            return Long.parseLong(out) / 1000;
        } catch (Exception e) {
            return 0; // xprintidle not installed — never skip breaks
        }
    }
}
```

- [ ] **Step 8: Create IdleDetectorFactory.java**

```java
package com.github.brane08.fx.takebreak.idle;

public final class IdleDetectorFactory {

    private IdleDetectorFactory() {}

    public static IdleDetector create() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) return new MacIdleDetector();
        if (os.contains("win")) return new WindowsIdleDetector();
        return new LinuxIdleDetector();
    }
}
```

- [ ] **Step 9: Run factory tests**

```bash
mvn test -Dtest="IdleDetectorFactoryTest" 2>&1 | tail -10
```

Expected: `Tests run: 3, Failures: 0, Errors: 0`.

Note: On macOS the `MacIdleDetector` will be instantiated and `factoryReturnsPlatformCorrectType` verifies that. The `MacIdleDetector.CoreGraphics.INSTANCE` is lazy (JNA loads only on first call to `getIdleSeconds()`), so the class loads without error even in tests.

- [ ] **Step 10: Run all tests to check no regressions**

```bash
mvn test -Dtest="BreakConfigTest,ConfigControllerTest,IdleDetectorFactoryTest" 2>&1 | tail -10
```

Expected: all prior tests still pass plus 3 new ones.

- [ ] **Step 11: Commit**

```bash
git add src/main/java/com/github/brane08/fx/takebreak/idle/ \
        src/test/java/com/github/brane08/fx/takebreak/idle/
git commit -m "feat: add IdleDetector interface, factory, and Mac/Windows/Linux implementations"
```

---

### Task 4: Update BreakSchedule to skip breaks when idle

**Files:**
- Modify: `src/main/java/com/github/brane08/fx/takebreak/tasks/BreakSchedule.java`

Background: `BreakSchedule.run()` already re-resolves `breakConfig` from the Injector each tick. We add an `IdleDetector` field and check `idleDetector.getIdleSeconds() >= config.idleThreshold()` at the top of `run()`. If idle, log and return early — the break is silently skipped. The counter is NOT incremented when skipping, so the long-break every-3rd cadence is unaffected.

- [ ] **Step 1: Update BreakSchedule.java**

Replace the entire file:

```java
package com.github.brane08.fx.takebreak.tasks;

import com.github.brane08.fx.takebreak.Constants;
import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.idle.IdleDetector;
import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public final class BreakSchedule implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Constants.LOGGER_NAME);

    private final AtomicInteger counter;
    private final Stage currentStage;
    private final MenuItem skipItem;
    private final Function<Integer, Integer> startTimer;
    private final IdleDetector idleDetector;

    public BreakSchedule(AtomicInteger counter, Stage currentStage, MenuItem skipItem,
                         Function<Integer, Integer> startTimer, IdleDetector idleDetector) {
        this.counter = counter;
        this.currentStage = currentStage;
        this.skipItem = skipItem;
        this.startTimer = startTimer;
        this.idleDetector = idleDetector;
    }

    @Override
    public void run() {
        try {
            LOG.info("{}", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            BreakConfig breakConfig = Injector.resolveNamed("breakConfig");
            long idleSecs = idleDetector.getIdleSeconds();
            if (idleSecs >= breakConfig.idleThreshold()) {
                LOG.info("Skipping break — idle {}s >= threshold {}s",
                        idleSecs, breakConfig.idleThreshold());
                return;
            }
            int displayTime = breakConfig.getBreakTime(counter.addAndGet(1));
            Platform.runLater(() -> {
                skipItem.setEnabled(true);
                currentStage.show();
                startTimer.apply(displayTime);
            });
        } catch (Exception e) {
            LOG.error("Unhandled exception, check!!!", e);
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
mvn -q compile 2>&1 | tail -5
```

Expected: no output, exit 0. (BreakApplication won't compile yet because it passes 4 args to BreakSchedule — that's expected and fixed in Task 5.)

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/github/brane08/fx/takebreak/tasks/BreakSchedule.java
git commit -m "feat: BreakSchedule skips break when user is idle >= threshold"
```

---

### Task 5: Wire IdleDetector into BreakApplication

**Files:**
- Modify: `src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java`

- [ ] **Step 1: Read the current BreakApplication.java**

Read `src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java` before editing.

- [ ] **Step 2: Add idleDetector field and update BreakSchedule instantiation**

Add this import at the top of the imports block:
```java
import com.github.brane08.fx.takebreak.idle.IdleDetector;
import com.github.brane08.fx.takebreak.idle.IdleDetectorFactory;
```

Add this field after the `counter` field declaration:
```java
private final IdleDetector idleDetector = IdleDetectorFactory.create();
```

In `start()`, find:
```java
new BreakSchedule(counter, rootStage, skipItem, controller::startTimer),
```
Replace with:
```java
new BreakSchedule(counter, rootStage, skipItem, controller::startTimer, idleDetector),
```

In `reschedule()`, find:
```java
new BreakSchedule(counter, defaultStage, skipItem, breakController::startTimer),
```
Replace with:
```java
new BreakSchedule(counter, defaultStage, skipItem, breakController::startTimer, idleDetector),
```

- [ ] **Step 3: Verify compile and tests pass**

```bash
mvn test -Dtest="BreakConfigTest,ConfigControllerTest,IdleDetectorFactoryTest" 2>&1 | tail -10
```

Expected: all tests pass, BUILD SUCCESS.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java
git commit -m "feat: wire IdleDetector into BreakApplication"
```

---

### Task 6: Add idle slider to Settings UI

**Files:**
- Modify: `src/main/resources/views/config.fxml`
- Modify: `src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java`

Background: Currently config.fxml has 5 rows (rowIndex 0–3 for sliders, rowIndex 4 for the button HBox) and 5 RowConstraints. Add the idle slider at rowIndex 4, move the HBox to rowIndex 5, and add a 6th RowConstraints.

- [ ] **Step 1: Update config.fxml**

Read `src/main/resources/views/config.fxml` first. Then make these changes:

Find the HBox line:
```xml
<HBox alignment="CENTER_RIGHT" spacing="10" GridPane.columnIndex="0" GridPane.rowIndex="4" GridPane.columnSpan="2">
```
Change `rowIndex="4"` to `rowIndex="5"`.

Before the HBox block, add:
```xml
<Label text="Idle Timeout (seconds):" GridPane.columnIndex="0" GridPane.rowIndex="4"/>
<Slider fx:id="idleSlider" blockIncrement="60" majorTickUnit="60" max="600" min="60"
        showTickMarks="true" showTickLabels="true" snapToTicks="true"
        GridPane.columnIndex="1" GridPane.rowIndex="4"/>
```

In the `<rowConstraints>` block at the bottom, add a 6th `<RowConstraints/>` entry. The block should now contain 6 entries:
```xml
<rowConstraints>
    <RowConstraints/>
    <RowConstraints/>
    <RowConstraints/>
    <RowConstraints/>
    <RowConstraints/>
    <RowConstraints/>
</rowConstraints>
```

- [ ] **Step 2: Update ConfigController.java**

Read `src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java` first.

Add the field after `warningSlider`:
```java
@FXML Slider idleSlider;
```

In `initialize()`, add after `warningSlider.setValue(config.warningTime())`:
```java
idleSlider.setValue(config.idleThreshold());
```

In `resetDefaults()`, add after `warningSlider.setValue(BreakConfig.DEFAULT_WARNING)`:
```java
idleSlider.setValue(BreakConfig.DEFAULT_IDLE);
```

In `saveConfig()`, add after `int warning = (int) warningSlider.getValue()`:
```java
int idle = (int) idleSlider.getValue();
```

Replace the temporary fix from Task 2:
```java
BreakConfig updated = new BreakConfig(small, large, spacing, warning, BreakConfig.DEFAULT_IDLE);
```
With:
```java
BreakConfig updated = new BreakConfig(small, large, spacing, warning, idle);
```

- [ ] **Step 3: Verify compile**

```bash
mvn -q compile 2>&1 | tail -5
```

Expected: no output, exit 0.

- [ ] **Step 4: Run all tests**

```bash
mvn test -Dtest="BreakConfigTest,ConfigControllerTest,IdleDetectorFactoryTest" 2>&1 | tail -10
```

Expected: all tests pass. Note: `ConfigControllerTest.slidersInitializeFromConfig` will fail because `idleSlider` is now wired but the test doesn't yet assert it — that's fine, the test still passes since we're only adding assertions in Task 7.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/views/config.fxml \
        src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java
git commit -m "feat: add idle timeout slider to Settings UI"
```

---

### Task 7: Update ConfigControllerTest for idle slider

**Files:**
- Modify: `src/test/java/com/github/brane08/fx/takebreak/controllers/ConfigControllerTest.java`

- [ ] **Step 1: Update ConfigControllerTest.java**

Replace the entire file:

```java
package com.github.brane08.fx.takebreak.controllers;

import com.github.brane08.fx.takebreak.FxHelper;
import com.github.brane08.fx.takebreak.domain.BreakConfig;
import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigControllerTest {

    @BeforeAll
    static void startFx() throws InterruptedException {
        Injector.initDefault();
        FxHelper.startOnce();
    }

    private ConfigController loadController() throws Exception {
        return FxHelper.onFxThread(() -> {
            FXMLLoader loader = new FXMLLoader(
                ConfigControllerTest.class.getResource("/views/config.fxml"));
            loader.load();
            return loader.getController();
        });
    }

    @Test
    void slidersInitializeFromConfig() throws Exception {
        BreakConfig config = Injector.resolveNamed("breakConfig");
        ConfigController ctrl = loadController();
        assertEquals(config.smallBreak(),    (int) ctrl.smallBreakSlider.getValue());
        assertEquals(config.longBreak(),     (int) ctrl.longBreakSlider.getValue());
        assertEquals(config.spacing(),       (int) ctrl.spacingSlider.getValue());
        assertEquals(config.warningTime(),   (int) ctrl.warningSlider.getValue());
        assertEquals(config.idleThreshold(), (int) ctrl.idleSlider.getValue());
    }

    @Test
    void resetDefaultsRestoresConstants() throws Exception {
        ConfigController ctrl = loadController();
        FxHelper.onFxThread(() -> { ctrl.resetDefaults(); return null; });
        assertEquals(BreakConfig.DEFAULT_SMALL,   (int) ctrl.smallBreakSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_LONG,    (int) ctrl.longBreakSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_SPACING, (int) ctrl.spacingSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_WARNING, (int) ctrl.warningSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_IDLE,    (int) ctrl.idleSlider.getValue());
    }

    @Test
    void saveConfigWritesCorrectRecord() throws Exception {
        ConfigController ctrl = loadController();
        FxHelper.onFxThread(() -> {
            ctrl.smallBreakSlider.setValue(20);
            ctrl.longBreakSlider.setValue(120);
            ctrl.spacingSlider.setValue(660);
            ctrl.warningSlider.setValue(30);
            ctrl.idleSlider.setValue(240);
            ctrl.saveConfig();
            return null;
        });
        BreakConfig saved = Injector.resolveNamed("breakConfig");
        assertEquals(20,  saved.smallBreak());
        assertEquals(120, saved.longBreak());
        assertEquals(660, saved.spacing());
        assertEquals(30,  saved.warningTime());
        assertEquals(240, saved.idleThreshold());
    }
}
```

- [ ] **Step 2: Run all tests — expect all to pass**

```bash
mvn test -Dtest="BreakConfigTest,ConfigControllerTest,IdleDetectorFactoryTest" 2>&1 | tail -15
```

Expected:
```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

(4 BreakConfigTest + 3 IdleDetectorFactoryTest + 3 ConfigControllerTest)

- [ ] **Step 3: Update .claude/memory/todo.md**

Add under `## Implementation`:
```
DONE: Idle detection — IdleDetector interface, Mac/Windows/Linux impls, BreakConfig idleThreshold, Settings slider
```

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/github/brane08/fx/takebreak/controllers/ConfigControllerTest.java \
        .claude/memory/todo.md
git commit -m "test: update ConfigControllerTest for idleSlider; all 10 tests pass"
```
