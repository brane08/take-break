# Headless JavaFX Controller Tests — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add three headless-safe `ConfigControllerTest` tests that run on both macOS dev machines and CI using Monocle as the JavaFX glass backend.

**Architecture:** `Platform.startup()` initialises the toolkit once via `FxHelper`; each test loads `config.fxml` fresh via `FXMLLoader` on the FX thread and asserts controller state. Monocle replaces the native glass backend via a surefire JVM flag — no display required.

**Tech Stack:** Java 21, JavaFX 21.0.9, JUnit 5.11.4, `openjfx-monocle:jdk-21+26`, Maven Surefire 3.5.3, JPMS module `brane.fx.takebreak`.

---

## File Map

| Action | File |
|--------|------|
| Modify | `pom.xml` — add Monocle dep + surefire plugin |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java` — package-private sliders, null guard |
| Create | `src/test/java/com/github/brane08/fx/takebreak/FxHelper.java` |
| Create | `src/test/java/com/github/brane08/fx/takebreak/controllers/ConfigControllerTest.java` |

---

### Task 1: Add Monocle dependency and configure Surefire

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: Add Monocle test dependency**

In `pom.xml`, add after the `junit-jupiter-engine` dependency (before `</dependencies>`):

```xml
<dependency>
    <groupId>org.testfx</groupId>
    <artifactId>openjfx-monocle</artifactId>
    <version>jdk-21+26</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Add Surefire plugin with Monocle JVM flags**

In `pom.xml`, add after the `maven-dependency-plugin` plugin block (before `</plugins>`):

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.3</version>
    <configuration>
        <argLine>
            -Djava.awt.headless=true
            -Dglass.platform=Monocle
            -Dprism.order=sw
            -Dprism.text=t2k
            --add-opens=javafx.graphics/com.sun.javafx.application=brane.fx.takebreak
            --add-opens=javafx.graphics/com.sun.glass.ui=brane.fx.takebreak
        </argLine>
    </configuration>
</plugin>
```

- [ ] **Step 3: Verify dependency resolves**

```bash
mvn dependency:resolve -q
```

Expected: `BUILD SUCCESS` with no errors. If `openjfx-monocle:jdk-21+26` is not found, verify the artifact exists on Maven Central at `org.testfx:openjfx-monocle:jdk-21+26`.

- [ ] **Step 4: Verify compile still passes**

```bash
mvn -q compile test-compile
```

Expected: `BUILD SUCCESS`, no output.

- [ ] **Step 5: Commit**

```bash
git add pom.xml
git commit -m "build: add Monocle dep and Surefire config for headless FX tests"
```

---

### Task 2: Make ConfigController testable

**Files:**
- Modify: `src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java`

Background: Test code runs inside the `brane.fx.takebreak` named module (Maven patches test sources into the module at compile and runtime). Package-private fields are therefore directly accessible from tests in the same package. The `@FXML` annotation already requires at least package-private access — this is not a visibility loosening in practice.

The `saveConfig()` method ends with `smallBreakSlider.getScene().getWindow().hide()`. In tests there is no Stage or Scene attached, so this NPEs. The null guard fixes it without affecting the real app (the scene is always present when the user clicks Save).

- [ ] **Step 1: Write the failing test first (verify NPE exists)**

Create the file `src/test/java/com/github/brane08/fx/takebreak/controllers/ConfigControllerTest.java` with this minimal stub to confirm the problem:

```java
package com.github.brane08.fx.takebreak.controllers;

import com.github.brane08.fx.takebreak.inject.Injector;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ConfigControllerTest {

    @BeforeAll
    static void startFx() throws Exception {
        Injector.initDefault();
    }

    @Test
    void smokeLoad() throws Exception {
        FXMLLoader loader = new FXMLLoader(
            ConfigControllerTest.class.getResource("/views/config.fxml"));
        loader.load();
        assertNotNull(loader.getController());
    }
}
```

- [ ] **Step 2: Run to confirm it fails (compilation error on private fields)**

```bash
mvn test -Dtest="ConfigControllerTest" 2>&1 | tail -20
```

Expected: compile error — `smallBreakSlider has private access in ConfigController` — confirming the fields need to be package-private. (If it fails with a different error, note it and proceed to Step 3 anyway.)

- [ ] **Step 3: Make slider fields package-private**

In `ConfigController.java`, change lines 15-18 from:

```java
@FXML private Slider smallBreakSlider;
@FXML private Slider longBreakSlider;
@FXML private Slider spacingSlider;
@FXML private Slider warningSlider;
```

To:

```java
@FXML Slider smallBreakSlider;
@FXML Slider longBreakSlider;
@FXML Slider spacingSlider;
@FXML Slider warningSlider;
```

- [ ] **Step 4: Add null guard in `saveConfig()`**

In `ConfigController.java`, change line 59 from:

```java
        smallBreakSlider.getScene().getWindow().hide();
```

To:

```java
        if (smallBreakSlider.getScene() != null) {
            smallBreakSlider.getScene().getWindow().hide();
        }
```

- [ ] **Step 5: Verify compile**

```bash
mvn -q compile
```

Expected: `BUILD SUCCESS`, no output.

- [ ] **Step 6: Delete the stub test file** — it will be replaced in full in Task 4

```bash
rm src/test/java/com/github/brane08/fx/takebreak/controllers/ConfigControllerTest.java
```

- [ ] **Step 7: Commit**

```bash
git add src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java
git commit -m "refactor: package-private slider fields, null-guard scene hide for testability"
```

---

### Task 3: Create FxHelper test utility

**Files:**
- Create: `src/test/java/com/github/brane08/fx/takebreak/FxHelper.java`

`FxHelper` is a pure test utility. `startOnce()` is safe to call from multiple `@BeforeAll` methods across test classes — the `AtomicBoolean` ensures `Platform.startup()` is called exactly once per JVM. `onFxThread` blocks the calling thread until the FX thread completes the callable, and rethrows any exception so test failures are visible.

- [ ] **Step 1: Create the directory if needed**

```bash
mkdir -p src/test/java/com/github/brane08/fx/takebreak
```

- [ ] **Step 2: Create `FxHelper.java`**

```java
package com.github.brane08.fx.takebreak;

import javafx.application.Platform;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class FxHelper {

    private static final AtomicBoolean started = new AtomicBoolean(false);

    private FxHelper() {}

    public static void startOnce() throws InterruptedException {
        if (started.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.startup(latch::countDown);
            latch.await();
        }
    }

    public static <T> T onFxThread(Callable<T> task) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> error = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                result.set(task.call());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });
        latch.await();
        if (error.get() != null) throw new RuntimeException(error.get());
        return result.get();
    }
}
```

- [ ] **Step 3: Verify test-compile**

```bash
mvn -q test-compile
```

Expected: `BUILD SUCCESS`, no output.

- [ ] **Step 4: Commit**

```bash
git add src/test/java/com/github/brane08/fx/takebreak/FxHelper.java
git commit -m "test: add FxHelper for Platform lifecycle and FX-thread execution"
```

---

### Task 4: Add ConfigControllerTest with 3 tests

**Files:**
- Create: `src/test/java/com/github/brane08/fx/takebreak/controllers/ConfigControllerTest.java`

Notes on what each test does:
- `slidersInitializeFromConfig` — reads the `BreakConfig` resolved from the Injector (which came from `~/.config/take-break/config.properties` or defaults) and asserts each slider value matches. Does NOT assume DEFAULT_* values because the user may have saved custom values.
- `resetDefaultsRestoresConstants` — calls `resetDefaults()` and asserts each slider is the constant value (not the user's saved values).
- `saveConfigWritesCorrectRecord` — sets sliders to specific values (20/120/660/30), calls `saveConfig()`, re-reads from the Injector, and asserts the record fields match. Values chosen to be within slider bounds (min/max from `config.fxml`): smallBreak min=10 max=120, longBreak min=60 max=600, spacing min=60 max=3600, warning min=0 max=120.

- [ ] **Step 1: Create the directory if needed**

```bash
mkdir -p src/test/java/com/github/brane08/fx/takebreak/controllers
```

- [ ] **Step 2: Create `ConfigControllerTest.java`**

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
        assertEquals(config.smallBreak(),  (int) ctrl.smallBreakSlider.getValue());
        assertEquals(config.longBreak(),   (int) ctrl.longBreakSlider.getValue());
        assertEquals(config.spacing(),     (int) ctrl.spacingSlider.getValue());
        assertEquals(config.warningTime(), (int) ctrl.warningSlider.getValue());
    }

    @Test
    void resetDefaultsRestoresConstants() throws Exception {
        ConfigController ctrl = loadController();
        FxHelper.onFxThread(() -> { ctrl.resetDefaults(); return null; });
        assertEquals(BreakConfig.DEFAULT_SMALL,   (int) ctrl.smallBreakSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_LONG,    (int) ctrl.longBreakSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_SPACING, (int) ctrl.spacingSlider.getValue());
        assertEquals(BreakConfig.DEFAULT_WARNING, (int) ctrl.warningSlider.getValue());
    }

    @Test
    void saveConfigWritesCorrectRecord() throws Exception {
        ConfigController ctrl = loadController();
        FxHelper.onFxThread(() -> {
            ctrl.smallBreakSlider.setValue(20);
            ctrl.longBreakSlider.setValue(120);
            ctrl.spacingSlider.setValue(660);
            ctrl.warningSlider.setValue(30);
            ctrl.saveConfig();
            return null;
        });
        BreakConfig saved = Injector.resolveNamed("breakConfig");
        assertEquals(20,  saved.smallBreak());
        assertEquals(120, saved.longBreak());
        assertEquals(660, saved.spacing());
        assertEquals(30,  saved.warningTime());
    }
}
```

- [ ] **Step 3: Run the tests — expect all 3 to pass**

```bash
mvn test -Dtest="ConfigControllerTest" 2>&1 | tail -30
```

Expected:
```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

**If you see `HeadlessException` or `UnsatisfiedLinkError`:** Monocle is not being activated. Check that `-Dglass.platform=Monocle` is in the surefire argLine and the `openjfx-monocle` JAR is on the test classpath (`mvn dependency:resolve -q` should list it).

**If you see `InaccessibleObjectException` for a JavaFX internal package:** Add the missing `--add-opens` to the surefire argLine targeting `brane.fx.takebreak`. The error message will name the exact package.

**If you see `Platform.startup() called more than once`:** `FxHelper.startOnce()` should prevent this. If it still appears, check that `Injector.initDefault()` does not start the FX toolkit.

- [ ] **Step 4: Run the full suite to check no regressions**

```bash
mvn test -Dtest="BreakConfigTest,ConfigControllerTest,IdleDetectorFactoryTest" 2>&1 | tail -10
```

Expected:
```
Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

(4 `BreakConfigTest` + 3 `ConfigControllerTest`)

- [ ] **Step 5: Update `.claude/memory/todo.md`**

Add under a new `## Tests` section:

```
DONE: Add ConfigControllerTest — 3 headless tests via Monocle + Platform.startup()
```

- [ ] **Step 6: Commit**

```bash
git add src/test/java/com/github/brane08/fx/takebreak/controllers/ConfigControllerTest.java \
        .claude/memory/todo.md
git commit -m "test: headless ConfigControllerTest via Monocle — 3 passing tests"
```
