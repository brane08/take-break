# pending work

## Known issues

None open. Resolved: `BreakController.startTimer()` now silently cancels a running timer
(no hide callback), `BreakConfig.normalized()` keeps spacing above the longest break, and
`BreakSchedule` drops a break while the overlay is still showing. The epoch guard only
covers `reschedule()`, not overlapping ticks, so those three are what prevent overlap.

## Potential improvements

- Packaging: consider macOS code signing / notarization for Gatekeeper (blocked — needs
  a real Apple Developer ID certificate + notarization credentials, not available yet).
- Packaging: consider Linux `.deb`/`.rpm` or Windows `.msi` for easier installation.
- Tests: `BreakController` now has 3 TestFX robot tests (`BreakControllerUiTest`) —
  immediate skip, skip mid-countdown, and restart-overwrites-timer.
- Tests: `WarningController` now has TestFX coverage (`WarningControllerUiTest`) — default
  message label + auto-close-after-5s via `setStage()`.
