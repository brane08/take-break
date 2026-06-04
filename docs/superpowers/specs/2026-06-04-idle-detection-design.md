# Idle Detection — Design Spec

## Goal

Skip a scheduled break if the user has been idle (no keyboard or mouse input) for at least a configurable threshold. Idle threshold is configurable via the Settings UI and stored in `config.properties`.

## Scope

**In scope:** Skip break at fire time if idle ≥ threshold. Configurable threshold via Settings slider. Cross-platform: macOS, Windows, Linux.

**Out of scope:** Resetting the spacing timer during idle, showing a "skipped" notification, or tracking cumulative idle time.

---

## Architecture

A new `idle` package holds an `IdleDetector` interface and three platform implementations selected at startup by `IdleDetectorFactory`. The detector is created once in `BreakApplication` (not per-tick) and injected into `BreakSchedule`. At each break tick, `BreakSchedule` queries `idleDetector.getIdleSeconds()` and compares to `config.idleThreshold()`. If idle ≥ threshold, the break is silently skipped.

The idle threshold is stored as the 5th component of `BreakConfig` (`idleThreshold`), persisted under the key `"idle"` in `config.properties`, and exposed via a slider in the Settings dialog.

---

## File Map

| Action | File |
|--------|------|
| Create | `src/main/java/com/github/brane08/fx/takebreak/idle/IdleDetector.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/idle/IdleDetectorFactory.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/idle/MacIdleDetector.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/idle/WindowsIdleDetector.java` |
| Create | `src/main/java/com/github/brane08/fx/takebreak/idle/LinuxIdleDetector.java` |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/domain/BreakConfig.java` |
| Modify | `src/main/resources/views/config.fxml` |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/controllers/ConfigController.java` |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/tasks/BreakSchedule.java` |
| Modify | `src/main/java/com/github/brane08/fx/takebreak/BreakApplication.java` |
| Modify | `src/main/java/module-info.java` |
| Modify | `pom.xml` |

---

## Components

### `IdleDetector` interface

```java
package com.github.brane08.fx.takebreak.idle;

public interface IdleDetector {
    long getIdleSeconds();
}
```

### `IdleDetectorFactory`

```java
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

### `MacIdleDetector`

Uses CoreGraphics `CGEventSourceSecondsSinceLastEventType` via JNA. Catches both keyboard and mouse events.

```java
public final class MacIdleDetector implements IdleDetector {

    interface CoreGraphics extends Library {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);
        double CGEventSourceSecondsSinceLastEventType(int stateId, int eventType);
    }

    // kCGEventSourceStateHIDSystemState=1, kCGAnyInputEventType=0xFFFFFFFF (int -1)
    @Override
    public long getIdleSeconds() {
        return (long) CoreGraphics.INSTANCE
                .CGEventSourceSecondsSinceLastEventType(1, 0xFFFFFFFF);
    }
}
```

### `WindowsIdleDetector`

Uses `User32.GetLastInputInfo` and `Kernel32.GetTickCount` from jna-platform. The tick count wraps at ~49 days; the bitmask handles the wrap correctly.

```java
public final class WindowsIdleDetector implements IdleDetector {

    @Override
    public long getIdleSeconds() {
        WinUser.LASTINPUTINFO info = new WinUser.LASTINPUTINFO();
        User32.INSTANCE.GetLastInputInfo(info);
        long idleMs = (Kernel32.INSTANCE.GetTickCount() - info.dwTime) & 0xFFFFFFFFL;
        return idleMs / 1000;
    }
}
```

### `LinuxIdleDetector`

Spawns `xprintidle` (X11, available on most Ubuntu/Debian/Fedora desktops). Returns 0 if the tool is not installed or fails — meaning breaks are never skipped on unsupported setups (safe default).

```java
public final class LinuxIdleDetector implements IdleDetector {

    @Override
    public long getIdleSeconds() {
        try {
            Process p = new ProcessBuilder("xprintidle").start();
            String out = new String(p.getInputStream().readAllBytes()).trim();
            return Long.parseLong(out) / 1000;
        } catch (Exception e) {
            return 0;
        }
    }
}
```

---

## BreakConfig Changes

Add `idleThreshold` as the 5th record component:

```java
public record BreakConfig(int smallBreak, int longBreak, int spacing,
                          int warningTime, int idleThreshold) {
    public static final int DEFAULT_IDLE = 300;

    // save(): props.setProperty("idle", String.valueOf(idleThreshold))
    // fromFile(): Integer.parseInt(props.getProperty("idle", String.valueOf(DEFAULT_IDLE)))
}
```

All existing callers that construct `BreakConfig` (in `Injector`, `ConfigController`, `BreakConfigTest`) must be updated to pass the 5th argument.

---

## Settings UI Changes

Add idle slider at rowIndex=4, shift HBox to rowIndex=5, add 6th `RowConstraints`:

```xml
<Label text="Idle Timeout (seconds):" GridPane.columnIndex="0" GridPane.rowIndex="4"/>
<Slider fx:id="idleSlider" blockIncrement="60" majorTickUnit="60"
        max="600" min="60" showTickMarks="true" showTickLabels="true"
        snapToTicks="true" GridPane.columnIndex="1" GridPane.rowIndex="4"/>
<HBox ... GridPane.rowIndex="5" GridPane.columnSpan="2">...</HBox>
```

`ConfigController`: add `@FXML Slider idleSlider`; wire in `initialize()`, `resetDefaults()`, `saveConfig()`.

---

## BreakSchedule Changes

Add `IdleDetector` parameter; skip break if idle ≥ threshold:

```java
public BreakSchedule(AtomicInteger counter, Stage stage, MenuItem skipItem,
                     Runnable startTimer, IdleDetector idleDetector) { ... }

@Override
public void run() {
    BreakConfig config = Injector.resolveNamed("breakConfig");
    long idle = idleDetector.getIdleSeconds();
    if (idle >= config.idleThreshold()) {
        LOG.info("Skipping break — idle {}s >= threshold {}s", idle, config.idleThreshold());
        return;
    }
    // existing break logic unchanged
}
```

---

## BreakApplication Changes

Create detector once at field level; pass to `BreakSchedule` in both `start()` and `reschedule()`:

```java
private final IdleDetector idleDetector = IdleDetectorFactory.create();

// In start() and reschedule():
new BreakSchedule(counter, stage, skipItem, controller::startTimer, idleDetector)
```

---

## module-info.java Changes

```java
requires com.sun.jna;
requires com.sun.jna.platform;
opens com.github.brane08.fx.takebreak.idle;
```

---

## pom.xml Changes

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

---

## Test Notes

- `BreakConfigTest` — update all 4-arg `BreakConfig` constructors to 5-arg; add assertion for `DEFAULT_IDLE=300`; update `saveAndLoadRoundTrip` to check `idleThreshold`
- `ConfigControllerTest` — add `idleSlider` assertions to `slidersInitializeFromConfig`, `resetDefaultsRestoresConstants`, and `saveConfigWritesCorrectRecord`
- `IdleDetector` implementations are not unit-tested directly (they wrap native APIs); `IdleDetectorFactory` is tested by asserting it returns a non-null detector on the current platform

---

## CI / Cross-platform Notes

- `MacIdleDetector` only compiles and runs on macOS; on Linux/Windows it is never instantiated
- `WindowsIdleDetector` only instantiated on Windows
- `LinuxIdleDetector` is safe on any platform (subprocess, graceful fallback)
- JNA loads native libraries at runtime — no compile-time platform dependency
