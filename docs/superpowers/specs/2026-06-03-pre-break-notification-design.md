# Pre-Break Notification — Design Spec
_2026-06-03_

## Summary

Show a small, non-interactive JavaFX toast overlay in the bottom-right corner a configurable number of seconds before the break overlay appears. The toast auto-dismisses after 5 seconds. Warning time is set via a new slider in the Settings dialog; 0 disables the feature.

---

## Architecture

Two schedulers run in parallel on the same `ScheduledExecutorService`:

| Task | Initial delay | Period |
|---|---|---|
| `BreakSchedule` (existing) | `spacing` | `spacing` |
| `WarningSchedule` (new) | `max(0, spacing − warningTime)` | `spacing` |

Both futures are stored as `volatile Future<?>` fields in `BreakApplication` and cancelled together in `reschedule(BreakConfig)`. If `warningTime == 0`, `WarningSchedule` is not submitted and its future stays null.

---

## New Files

### `tasks/WarningSchedule.java`
`Runnable` submitted to the existing single-thread `ScheduledExecutorService`.  
`run()` calls `Platform.runLater(() -> showWarningToast())` where `showWarningToast` is a `Runnable` injected at construction time (keeps the task testable and decoupled from the stage).

### `controllers/WarningController.java`
FXML controller for the toast stage.  
`initialize()` starts a `PauseTransition(Duration.seconds(5))`; on finish, calls `stage.close()`.  
Receives the stage reference via a setter called by `BreakApplication` after `FXMLLoader.load()`.

### `views/warning.fxml`
Single `StackPane` root with a `Label` (`fx:id="message"`) showing `"Break coming up"`.  
Styled via inline CSS: dark semi-transparent background (`rgba(30,30,30,0.85)`), white text, `12px` corner radius, padding `12 20`.

---

## Modified Files

### `domain/BreakConfig.java`
Add `int warningTime` as the fourth record component.

```java
public record BreakConfig(int smallBreak, int longBreak, int spacing, int warningTime) {
    public static final int DEFAULT_WARNING = 30;
}
```

- Persisted as key `warning` in `config.properties`.
- `fromFile()` reads with `DEFAULT_WARNING` fallback.
- `save()` writes `warning` alongside existing keys.

### `views/config.fxml`
New row (rowIndex=3) between Spacing and the button HBox (button HBox moves to rowIndex=4):

```xml
<Label text="Warning (seconds):" GridPane.columnIndex="0" GridPane.rowIndex="3"/>
<Slider fx:id="warningSlider" min="0" max="120" blockIncrement="10"
        majorTickUnit="30" showTickMarks="true" showTickLabels="true"
        snapToTicks="true" GridPane.columnIndex="1" GridPane.rowIndex="3"/>
```

Add a `<RowConstraints/>` entry for the new row.

### `controllers/ConfigController.java`
- Inject `@FXML Slider warningSlider`
- `initialize()`: `warningSlider.setValue(config.warningTime())`
- `resetDefaults()`: `warningSlider.setValue(BreakConfig.DEFAULT_WARNING)`
- `saveConfig()`: include `(int) warningSlider.getValue()` as the fourth constructor arg

### `BreakApplication.java`
- Add `volatile Future<?> warningFuture`
- Extract helper `scheduleWarning(BreakConfig)`:
  ```java
  private void scheduleWarning(BreakConfig config) {
      if (config.warningTime() <= 0) return;
      long delay = Math.max(0, config.spacing() - config.warningTime());
      warningFuture = scheduler.scheduleAtFixedRate(
          new WarningSchedule(this::showWarningToast),
          delay, config.spacing(), TimeUnit.SECONDS);
  }
  ```
- `start()` calls `scheduleWarning(breakConfig)` after scheduling the break task
- `reschedule(BreakConfig)` cancels `warningFuture` (null-check) then calls `scheduleWarning(config)`
- `showWarningToast()` runs on JavaFX thread: loads `warning.fxml`, sets stage position and style, calls `controller.setStage(stage)`, shows the stage

---

## Toast Appearance

- **Size**: 300 × 70 px
- **Position**: bottom-right — `screenX + screenWidth − 320`, `screenY + screenHeight − 90`
- **Stage style**: `StageStyle.TRANSPARENT`, always-on-top, not resizable
- **Label text**: `"Break coming up"`
- **Auto-dismiss**: 5 seconds via `PauseTransition` in `WarningController`
- No user interaction; no close button

---

## Edge Cases

| Scenario | Behaviour |
|---|---|
| `warningTime == 0` | `warningFuture` never submitted; `reschedule()` skips cancel |
| `warningTime >= spacing` | `delay = max(0, spacing − warningTime) = 0`; toast fires immediately on start, then every `spacing` seconds — acceptable |
| Toast visible when break appears | Toast auto-dismisses in 5s; only overlaps if `warningTime < 5s`, no special handling needed |
| Rapid config saves | `cancel(false)` on both futures before rescheduling; same pattern as existing `reschedule()` |

---

## Testing

- `BreakConfigTest`: add `DEFAULT_WARNING` constant check and `warningTime` load/save round-trip
- `WarningSchedule`: no unit test needed — single `Platform.runLater` call, trivially correct
- Manual smoke test: set warning=10s, spacing=60s; confirm toast appears ~50s after start and dismisses in 5s
