# pending work

## Known issues

None open. Resolved: `BreakController.startTimer()` now silently cancels a running timer
(no hide callback), `BreakConfig.normalized()` keeps spacing above the longest break, and
`BreakSchedule` drops a break while the overlay is still showing. The epoch guard only
covers `reschedule()`, not overlapping ticks, so those three are what prevent overlap.

Fixed 2026-09-27: `BreakSchedule.run()` incremented the break `counter` before the
`Platform.runLater` drop checks (stale epoch / overlay already showing), so a dropped
tick still skewed the short/long break cadence. Counter increment now happens inside
`runLater`, right before the break is actually shown.

Closed 2026-09-27: added headless-safe test coverage for `Injector`, `ApplicationLock`,
and `BreakApplication.start()/stop()` wiring. `BreakApplication` needed a small seam
(`take-break.test` system property, set in pom.xml surefire argLine) so `start()` skips
`systemTray()` in tests — otherwise flipping AWT headless off to construct `MenuItem`
would have created a real system tray icon on a real desktop during `mvn test`.

## Potential improvements

- Packaging: consider macOS code signing / notarization for Gatekeeper (blocked — needs
  a real Apple Developer ID certificate + notarization credentials, not available yet).
- Packaging: consider Linux `.deb`/`.rpm` or Windows `.msi` for easier installation.
- Tests: `BreakController` now has 3 TestFX robot tests (`BreakControllerUiTest`) —
  immediate skip, skip mid-countdown, and restart-overwrites-timer.
- Tests: `WarningController` now has TestFX coverage (`WarningControllerUiTest`) — default
  message label + auto-close-after-5s via `setStage()`.
