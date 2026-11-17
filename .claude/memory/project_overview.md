# take-break — project overview

## Stack
- Java 21, JavaFX 21.0.9, JPMS (module `brane.fx.takebreak`)
- Maven, fat-jar via `maven-assembly-plugin` → `take-break-app.jar`
- SLF4J 2.0.17 + Logback 1.5.32 (`src/main/resources/logback.xml`)
- Jackson Databind 2.20.1 — registered in Injector as `Constants.DI_JSON_MAPPER`
- FXTrayIcon 4.2.3 — system tray via JavaFX (replaces AWT TrayIcon)
- TilesFX 21.0.9 — declared as dep + required in module-info; currently unused in code
- JUnit Jupiter 5.11.4 (test scope)

## What it does
System-tray desktop app that forces eye-break pauses.
- Fires a full-screen (70 % of screen) overlay on a recurring schedule.
- Every 3rd break is a "long break"; others are "short break".
- Tray menu: Skip Break | Settings | Exit.

## Source files

| File | Role |
|---|---|
| `BreakApplication.java` | JavaFX `Application`; wires scheduler, FXTrayIcon, stage |
| `ConfigController.java` | Settings dialog: sliders → `BreakConfig.save()` + reschedule callback |
| `Constants.java` | `LOGGER_NAME`, `DI_JSON_MAPPER = "jsonMapper"`, `DI_BREAK_CONFIG = "breakConfig"` |
| `ApplicationLock.java` | Singleton guard via `ServerSocket` on port 14425 |
| `controllers/BreakController.java` | Countdown UI: `Label minutes/seconds`, `AnimationTimer` |
| `controllers/SettingsController.java` | **Scaffold only** — slider listeners, no save/load logic yet |
| `domain/BreakConfig.java` | `record`; reads/writes `~/.config/take-break/config.properties` |
| `inject/Injector.java` | Hand-rolled DI: `ConcurrentHashMap`, `registerNamed`/`resolveNamed` |
| `tasks/BreakSchedule.java` | `Runnable` fired by `scheduleAtFixedRate`; resolves config from Injector |

## Resources

| File | Role |
|---|---|
| `views/main.fxml` | Break overlay; `fx:controller=controllers.BreakController`; digit labels `6em` |
| `views/config.fxml` | Settings GridPane; `fx:controller=ConfigController`; Reset + Save buttons |
| `logback.xml` | Console + rolling-file appender (10 MB, gzip) |
| `coffee.png` | Tray icon image |
| `scripts/run.sh` | Launch with `-Dglass.gtk.uiScale=auto -Dprism.allowhidpi=true` |
| `scripts/run.cmd` | Windows launch with HiDPI args |

## Key design decisions

- `BreakConfig` is a Java record; immutable. Persisted as plain `.properties` in `~/.config/take-break/`.
- Config defaults: `DEFAULT_SMALL=5s`, `DEFAULT_LONG=10s`, `DEFAULT_SPACING=10s`.
  **Note:** `spacingSlider` in config.fxml has `min=60`, so default 10 is below slider range — mismatch.
- `getBreakTime(instance)` returns `longBreak` when `instance % 3 == 0`, else `smallBreak`.
- Scheduler uses `TimeUnit.SECONDS`; `spacing` field is in **seconds**.
- Stage is 70 % of screen, always-on-top, undecorated.
- HiDPI: `glass.gtk.uiScale=auto` set in `main()` and `run.sh`/`run.cmd`.
- Config screen: `settingStage()` creates a fresh `Stage` each open (fresh controller state).
- Live reschedule: `BreakApplication.reschedule(BreakConfig)` cancels current `Future` and re-schedules; wired to `ConfigController.setRescheduleCallback(this::reschedule)` in FXTrayIcon Settings handler.
- System tray uses `FXTrayIcon.Builder`; menu item lambdas run on JavaFX thread automatically.

## Injector registration (initDefault)
```java
registerNamed(Constants.DI_JSON_MAPPER, new ObjectMapper());
registerNamed(Constants.DI_BREAK_CONFIG, BreakConfig.fromFile());
```
`ConfigController.initialize()` calls `Injector.resolveNamed("breakConfig")` (= `DI_BREAK_CONFIG`).

## module-info.java
```
module brane.fx.takebreak {
    requires java.desktop;
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires eu.hansolo.tilesfx;
    requires org.slf4j;
    requires com.fasterxml.jackson.databind;
    requires com.dustinredmond.fxtrayicon;
    opens   com.github.brane08.fx.takebreak;
    exports com.github.brane08.fx.takebreak;
    exports com.github.brane08.fx.takebreak.domain;
    exports com.github.brane08.fx.takebreak.controllers;
    opens   com.github.brane08.fx.takebreak.controllers;
}
```

## Tests
- `src/test/java/.../domain/BreakConfigTest.java` — load/save round-trip for `BreakConfig`.
