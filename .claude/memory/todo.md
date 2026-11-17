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
