# pending work

## Known issues

(none currently — `ApplicationTest.java`, Jackson/`jsonMapper`, and the stray
`TakeBreak.iconset/` dir have all been resolved; see project_overview.md for current state)

## Known issues

- **`BreakController.startTimer()` doesn't stop a prior running timer**: calling
  `startTimer()` again while a previous `AnimationTimer` is still active overwrites the
  `currentTimer` field without stopping the old one — the orphaned timer keeps ticking in
  the background and will eventually call `hideCallback` again on its own when it reaches
  zero. In practice `BreakSchedule`'s epoch guard prevents overlapping calls, so this
  hasn't caused an observed bug, but it's a latent double-hideCallback risk if that guard
  is ever bypassed. Documented (not fixed) by
  `BreakControllerUiTest.restartingTimerReplacesCurrentTimerReference`.

## Potential improvements

- Packaging: consider macOS code signing / notarization for Gatekeeper (blocked — needs
  a real Apple Developer ID certificate + notarization credentials, not available yet).
- Packaging: consider Linux `.deb`/`.rpm` or Windows `.msi` for easier installation.
- Tests: `BreakController` now has 3 TestFX robot tests (`BreakControllerUiTest`) —
  immediate skip, skip mid-countdown, and restart-overwrites-timer.
- Tests: `WarningController` now has TestFX coverage (`WarningControllerUiTest`) — default
  message label + auto-close-after-5s via `setStage()`.
