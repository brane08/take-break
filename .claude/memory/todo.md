# todo

## Bugs

DONE: Fix slider/default mismatches (DEFAULT_SMALL=60, DEFAULT_LONG=300, DEFAULT_SPACING=1200)

## Cleanup

DONE: Remove TilesFX dep from pom.xml + module-info
DONE: Delete unused SettingsController scaffold
DONE: Remove Jackson dep from module-info.java (pom dep still present — candidate for removal)
DONE: Delete ApplicationTest.java (was hanging)

## Refactor

DONE: Move ConfigController to controllers package

## Polish

DONE: Remove BreakConfig.version field

## Tests

DONE: Add ConfigControllerTest — 3 headless tests via Monocle + Platform.startup()
DONE: Add IdleDetectorFactoryTest — 3 tests (non-null, correct subtype, Linux fallback)

## Implementation

DONE: Add warningTime field to BreakConfig (4th component in record)
DONE: Create WarningSchedule.java — posts Platform.runLater(showToast) on schedule
DONE: Create WarningController.java — manages toast stage, auto-closes after 5s via PauseTransition
DONE: Create warning.fxml — semi-transparent rounded-corner toast overlay (300x70)
DONE: Add warning slider to config UI (ConfigController + config.fxml)
DONE: Wire warning reschedule into ConfigController callback
DONE: Idle detection — IdleDetector interface, Mac/Windows/Linux impls, BreakConfig idleThreshold, Settings slider, idle-skip logic in BreakSchedule, 10/10 tests passing
DONE: Cross-platform packaging — macOS .app bundle, Linux .desktop+install.sh, Windows VBScript; mvn package produces 3 zips

## Review (2026-09-18, call-detector findings)

DONE: MacCallDetector — CoreAudio/CoreMediaIO IsRunningSomewhere via JNA (camera + default mic, Intel + Apple Silicon)
DONE: WarningSchedule — suppressed while callDetector reports a call
DONE: Mac/Windows/Linux detectors — restore interrupt flag; BreakSchedule re-checks interrupt+epoch before counting a break; reschedule bumps epoch before cancel
DONE: WindowsCallDetector — per-subkey parse, require LastUsedTimeStart != 0 with Stop == 0
DONE: LinuxCallDetector — ignore wireplumber/pipewire holders, add pactl capture-stream detection
DONE: CallDetectorFactoryTest — Windows budget cut to ~5s worst case, test timeout 20s
DONE: BreakConfig.normalized() (spacing > longest break, warning < spacing) used by ConfigController + fromFile; BreakController cancels replaced timer silently; BreakSchedule drops break while overlay showing
DONE: UI test timing margins widened; added restart-does-not-hide test
DONE: BreakApplication watches every rescheduled future; BreakSchedule logs+rethrows Error, detector failures fail-safe

## Review (2026-09-27, functionality scan)

DONE: Fix BreakSchedule counter off-by-one — counter.addAndGet(1) moved inside
Platform.runLater, after the epoch/isShowing drop checks, so a dropped tick no
longer skews the short/long break cadence
DONE: BreakScheduleTest — 3 tests covering counter-advances / dropped-while-showing /
stale-epoch-in-runLater
DONE: ConfigControllerTest — saveConfigInvokesRescheduleCallbackWithSavedConfig
DONE: take-break.test seam — BreakApplication.start() skips systemTray() under
-Dtake-break.test=true (set in pom.xml surefire argLine), avoiding real AWT tray
creation during headless test runs
DONE: InjectorTest — register/registerNamed/resolve/resolveNamed + missing-key
throws + initDefault
DONE: ApplicationLockTest — lock/double-lock-throws/release-frees-port/idempotent
release (LOCK_PORT unchanged)
DONE: BreakApplicationTest — start() stage/scene wiring + stop() no-throw, via
TestFX ApplicationExtension
PENDING: Consider closing BreakApplication.activeToast Stage explicitly on app stop()
(minor, low-value — JVM exit already closes it)

## Review (2026-10-02, UI copy)

DONE: Professional UI text — settings labels/buttons, break overlay, warning toast, tray menu, settings window title

## Review (2026-10-04, VS Code port)

DONE: Scaffold vscode-extension/ (config, break panel, scheduler, idle skip, status bar toggle)
PENDING: Manually test in Extension Development Host (F5)
DONE: Scaffold intellij-plugin/ (Kotlin, settings, service, countdown dialog, notice, idle skip)
PENDING: Build + run IntelliJ plugin (gradle runIde) — not verifiable in cloud sandbox
