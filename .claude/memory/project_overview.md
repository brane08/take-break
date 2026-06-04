# take-break — project overview

## Stack
- Java 21, JavaFX 21.0.9, JPMS (module `brane.fx.takebreak`)
- Maven, fat-jar via `maven-assembly-plugin` → `take-break-app.jar`
- SLF4J 2.0.17 + Logback 1.5.32 (`src/main/resources/logback.xml`)
- FXTrayIcon 4.2.3 — system tray via JavaFX (replaces AWT TrayIcon)
- JNA 5.14.0 + jna-platform 5.14.0 — native idle detection (macOS/Windows/Linux)
- JUnit Jupiter 5.11.4 + openjfx-monocle jdk-12.0.1+2 (test scope)
- Jackson Databind 2.20.1 — registered in Injector as `DI_JSON_MAPPER`; **never resolved** (candidate for removal)

## What it does
System-tray desktop app that forces eye-break pauses.
- Fires a full-screen (70% of screen) overlay on a recurring schedule.
- Every 3rd break is a "long break"; others are "short break".
- Skips the break if user has been idle ≥ `idleThreshold` seconds.
- Shows a warning toast `warningTime` seconds before the overlay.
- Tray menu: Skip Break | Settings | Exit.

## Package layout

```
com.github.brane08.fx.takebreak
├── BreakApplication.java       — JavaFX Application entry; wires scheduler + FXTrayIcon
├── ApplicationLock.java        — singleton guard via ServerSocket on port 14425
├── Constants.java              — LOGGER_NAME, DI_BREAK_CONFIG
├── controllers/
│   ├── BreakController.java    — countdown overlay UI (AnimationTimer, Labels)
│   ├── ConfigController.java   — settings dialog: 5 sliders → BreakConfig.save() + reschedule callback
│   └── WarningController.java  — manages toast stage, auto-closes after 5s via PauseTransition
├── domain/
│   └── BreakConfig.java        — immutable 5-component record; load/save ~/.config/take-break/config.properties
├── idle/
│   ├── IdleDetector.java       — interface: long getIdleSeconds()
│   ├── IdleDetectorFactory.java — selects impl by os.name: mac→Mac, win→Windows, else→Linux
│   ├── MacIdleDetector.java    — CoreGraphics.CGEventSourceSecondsSinceLastEventType(1, 0xFFFFFFFF)
│   ├── WindowsIdleDetector.java — User32.GetLastInputInfo + Kernel32.GetTickCount
│   └── LinuxIdleDetector.java  — xprintidle subprocess; returns 0 on failure (safe: never skips breaks)
├── inject/
│   └── Injector.java           — hand-rolled DI: ConcurrentHashMap, registerNamed/resolveNamed
└── tasks/
    ├── BreakSchedule.java      — Runnable; re-resolves breakConfig from Injector; idle-skip logic
    └── WarningSchedule.java    — posts Platform.runLater(showToast) on schedule
```

## BreakConfig record (5 fields, all in seconds)

```java
public record BreakConfig(int smallBreak, int longBreak, int spacing,
                          int warningTime, int idleThreshold)
```

| Field | Default | Property key | Slider range |
|---|---|---|---|
| `smallBreak` | 60 | `small` | 10–120, step 10 |
| `longBreak` | 300 | `long` | 60–600, step 60 |
| `spacing` | 1200 | `spacing` | 60–3600, step 60 |
| `warningTime` | 30 | `warning` | 0–60, step 5 |
| `idleThreshold` | 300 | `idle` | 60–600, step 60 |

- Config file: `~/.config/take-break/config.properties`
- Override for tests: `-Dtake-break.config.dir=<tmpdir>`
- `getBreakTime(instance)` → `longBreak` if `instance % 3 == 0`, else `smallBreak`
- `save()` creates parent dirs, writes all 5 props
- `fromFile()` creates+saves defaults if file missing

## Key runtime wiring

```
main() → ApplicationLock.tryToGetLock() → Injector.initDefault() → launch()
start() → FXMLLoader(main.fxml) → BreakController
       → FXTrayIcon (tray menu)
       → IdleDetectorFactory.create()       ← one instance, field in BreakApplication
       → BreakSchedule(counter, stage, skipItem, controller::startTimer, idleDetector)
       → scheduleWarning(config)            ← WarningSchedule → showWarningToast()
       → monitorPool watches schedulerFuture (logs cancellation/errors)
```

**Settings flow:**
1. User clicks Settings in tray
2. `FXMLLoader(config.fxml)` → fresh `ConfigController`
3. `cc.setRescheduleCallback(this::reschedule)`
4. `settingStage(parent)` → `showAndWait()`
5. Save: `ConfigController.saveConfig()` → `updated.save()` → `Injector.registerNamed("breakConfig", updated)` → `rescheduleCallback.accept(updated)`
6. `reschedule(config)` cancels both futures, re-schedules `BreakSchedule` + `WarningSchedule`

**Idle-skip in BreakSchedule.run():**
```java
long idleSecs = idleDetector.getIdleSeconds();
if (idleSecs >= breakConfig.idleThreshold()) { LOG.info(...); return; }
```

**Warning toast timing:**
```java
long delay = Math.max(0, config.spacing() - config.warningTime());
```
Toast appears at `spacing - warningTime` seconds. Guard: if `warningTime >= spacing`, saveConfig() clamps warning to `spacing - 10`.

**BreakSchedule.run() key detail:** resolves `"breakConfig"` from Injector on every tick — picks up live reschedule without restarting the executor.

## Idle detection — native implementations

**macOS (CoreGraphics, JNA):**
```java
CoreGraphics.INSTANCE.CGEventSourceSecondsSinceLastEventType(1, 0xFFFFFFFF)
// stateId=1 = kCGEventSourceStateHIDSystemState, eventType=0xFFFFFFFF = any input
```

**Windows (jna-platform):**
```java
WinUser.LASTINPUTINFO info = new WinUser.LASTINPUTINFO();
User32.INSTANCE.GetLastInputInfo(info);
long idleMs = (Kernel32.INSTANCE.GetTickCount() - info.dwTime) & 0xFFFFFFFFL;
```
`& 0xFFFFFFFFL` handles 32-bit DWORD wrap-around.

**Linux (subprocess):**
```java
Process p = new ProcessBuilder("xprintidle").start();
return Long.parseLong(out) / 1000;  // xprintidle returns milliseconds
// Exception → return 0 (never skip breaks)
```

**JPMS required for JNA:**
```
requires com.sun.jna;
requires com.sun.jna.platform;
opens com.github.brane08.fx.takebreak.idle;  // JNA reflects on this package
```

## module-info.java (current)
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
Note: `com.fasterxml.jackson.databind` is **not** in module-info (Jackson was removed).

## Resources

| File | Role |
|---|---|
| `views/main.fxml` | Break overlay; `fx:controller=controllers.BreakController`; digit labels 6em |
| `views/config.fxml` | Settings GridPane (5 sliders + Reset/Save); `fx:controller=controllers.ConfigController` |
| `views/warning.fxml` | Pre-break toast; `fx:controller=controllers.WarningController`; semi-transparent rounded-corner 300×70 |
| `logback.xml` | Console + rolling-file appender (10 MB, gzip) |
| `coffee.png` | Tray icon image |
| `scripts/run.sh` | Linux/macOS launch with Java version check + HiDPI flags |
| `scripts/run.cmd` | Windows launch with `@setlocal`, version check, `javaw` (no console) |
| `scripts/take-break.vbs` | Windows silent launcher: `shell.Run(..., 0, True)` → no console window |
| `scripts/take-break.desktop` | Linux XDG desktop entry; Exec points to `run.sh` |
| `scripts/install.sh` | Linux installer: copies to `~/.local/share/take-break/`, rewrites .desktop path via sed |
| `assembly/macos/Contents/Info.plist` | macOS .app metadata: LSUIElement=true, NSHighResolutionCapable=true |
| `assembly/macos/Contents/MacOS/TakeBreak` | macOS launcher: /usr/libexec/java_home → PATH fallback, version check |
| `assembly/macos/Contents/Resources/coffee.icns` | Generated from coffee.png via sips+iconutil (committed) |

## Cross-platform packaging — mvn package output

Three platform zips in `target/`:
- `take-break-macos.zip` → `TakeBreak.app/Contents/{Info.plist, MacOS/TakeBreak, Resources/coffee.icns, Java/take-break-app.jar}`
- `take-break-linux.zip` → `take-break-linux/{take-break-app.jar, run.sh, install.sh, take-break.desktop, icon.png}`
- `take-break-windows.zip` → `take-break-windows/{take-break-app.jar, run.cmd, take-break.vbs}`

Assembly descriptors: `assembly/{macos-dist.xml, linux-dist.xml, windows-dist.xml}`.
`TakeBreak.app/Contents/MacOS/TakeBreak` has `fileMode=0755` set in macos-dist.xml (preserved in zip).

macOS launcher PATH resolution order:
1. `$JAVA_HOME/bin/java` if set and executable
2. `/usr/libexec/java_home -v 21+` (works for Finder/Dock launched apps)
3. `java` from PATH (fallback)
4. `osascript` alert if none found or version < 21

## Tests

| File | What it tests |
|---|---|
| `BreakConfigTest` | Default constants, break-time routing, load/save round-trip (headless, fast) |
| `ConfigControllerTest` | 3 headless tests via Monocle: slider init, reset defaults, saveConfig persists; uses `FxHelper` + config dir isolation |
| `IdleDetectorFactoryTest` | Factory non-null, correct subtype for current OS, Linux fallback returns ≥ 0 |
| `FxHelper` | Test utility: `CountDownLatch READY` + `AtomicBoolean` to start Monocle Platform once per JVM |

**Run tests (always use explicit -Dtest to avoid ApplicationTest hang):**
```bash
mvn -q test -Dtest="BreakConfigTest,ConfigControllerTest,IdleDetectorFactoryTest"
```

**Full build:**
```bash
mvn -q package
```

## Injector registration (initDefault)
```java
registerNamed("jsonMapper", new ObjectMapper()); // registered but never resolved — candidate for removal
registerNamed("breakConfig", BreakConfig.fromFile());
```

## BreakApplication — key fields
```java
private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
private final ExecutorService monitorPool = Executors.newSingleThreadExecutor();
private final MenuItem skipItem = new MenuItem("Skip Break");
private final AtomicInteger counter = new AtomicInteger(0);
private final IdleDetector idleDetector = IdleDetectorFactory.create(); // created once at startup
private volatile Future<?> schedulerFuture;
private volatile Future<?> warningFuture;
private volatile Stage activeToast;
```

Stage: 70% of primary screen, always-on-top, undecorated, centered.
`Platform.setImplicitExit(false)` — keeps JVM alive when overlay is hidden.

## Slider ranges (config.fxml)
| fx:id | min | max | blockIncrement | DEFAULT |
|---|---|---|---|---|
| `smallBreakSlider` | 10 | 120 | 10 | 60 |
| `longBreakSlider` | 60 | 600 | 60 | 300 |
| `spacingSlider` | 60 | 3600 | 60 | 1200 |
| `warningSlider` | 0 | 60 | 5 | 30 |
| `idleSlider` | 60 | 600 | 60 | 300 |
