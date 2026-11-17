# TestFX Headless UI Tests — Design Spec

## Goal

Add headless-safe JavaFX controller tests that run on both developer machines (macOS) and CI (no display), using Monocle as the headless glass backend and `Platform.startup()` for toolkit lifecycle management — no TestFX library required.

## Scope

**In scope:** `ConfigController` — slider initialization, reset-to-defaults, save behavior.

**Out of scope:** `WarningController` — its only testable behavior is the 5-second auto-dismiss, which requires `Thread.sleep(5000)`. Excluded unless the duration is made injectable (separate refactor).

---

## Architecture

### Headless toolkit

`openjfx-monocle:jdk-21+26` provides the Monocle glass backend. It is activated at test time via surefire JVM flags — no code changes needed. Monocle renders entirely in software with no display requirement.

### `FxHelper` (test utility)

`src/test/java/com/github/brane08/fx/takebreak/FxHelper.java`

- `startOnce()` — calls `Platform.startup()` exactly once per JVM using `AtomicBoolean` + `CountDownLatch`. Safe to call from multiple `@BeforeAll` methods.
- `onFxThread(Callable<T>)` — runs a callable on the JavaFX Application Thread, blocks until complete, rethrows any exception as `RuntimeException`. Used for FXML loading and controller method calls (both require the FX thread).

### `ConfigControllerTest`

`src/test/java/com/github/brane08/fx/takebreak/controllers/ConfigControllerTest.java`

`@BeforeAll` calls `Injector.initDefault()` and `FxHelper.startOnce()`.

Each test loads `config.fxml` fresh via `FXMLLoader` on the FX thread and gets the controller. Tests do not share controller instances.

| Test | Assertion |
|------|-----------|
| `slidersInitializeFromConfig` | Each slider value matches the corresponding field in the `BreakConfig` resolved from the Injector |
| `resetDefaultsRestoresConstants` | After `resetDefaults()`, each slider equals its `BreakConfig.DEFAULT_*` constant |
| `saveConfigWritesCorrectRecord` | After setting slider values and calling `saveConfig()`, the `BreakConfig` re-resolved from the Injector has matching fields |

---

## Changes Required

### `pom.xml`

1. Add Monocle test dependency:
   ```xml
   <dependency>
       <groupId>org.testfx</groupId>
       <artifactId>openjfx-monocle</artifactId>
       <version>jdk-21+26</version>
       <scope>test</scope>
   </dependency>
   ```

2. Add surefire plugin with Monocle JVM flags:
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

### `ConfigController.java`

Slider fields must be package-private (drop `private`) so tests in the same module can read values directly. The `@FXML` annotation already requires at least package-private access for injection.

```java
@FXML Slider smallBreakSlider;
@FXML Slider longBreakSlider;
@FXML Slider spacingSlider;
@FXML Slider warningSlider;
```

### `ConfigController.java` — null guard in `saveConfig()`

`saveConfig()` ends with `smallBreakSlider.getScene().getWindow().hide()`. In tests there is no Stage or Scene, so this NPEs. Add a null guard:

```java
if (smallBreakSlider.getScene() != null) {
    smallBreakSlider.getScene().getWindow().hide();
}
```

### `module-info.java`

No changes needed. `opens com.github.brane08.fx.takebreak.controllers` already allows FXML reflection. Test code runs inside the same named module (Maven patches test sources into the module), so it has direct access to package-private members.

---

## Test Commands

```bash
# Run only controller tests (fast, headless)
mvn test -Dtest="ConfigControllerTest"

# Run all tests
mvn test
```

---

## CI Notes

- `-Djava.awt.headless=true` suppresses AWT display requirement
- `-Dglass.platform=Monocle` forces Monocle regardless of environment
- `-Dprism.order=sw` forces software rendering (no GPU needed)
- No `xvfb` or virtual framebuffer required
