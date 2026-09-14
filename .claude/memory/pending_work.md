# pending work

## Known issues

(none currently — `ApplicationTest.java`, Jackson/`jsonMapper`, and the stray
`TakeBreak.iconset/` dir have all been resolved; see project_overview.md for current state)

## Potential improvements

- Packaging: consider macOS code signing / notarization for Gatekeeper (blocked — needs
  a real Apple Developer ID certificate + notarization credentials, not available yet).
- Packaging: consider Linux `.deb`/`.rpm` or Windows `.msi` for easier installation.
- Tests: `BreakController` now has TestFX robot coverage (`BreakControllerUiTest`); could add
  more scenarios (e.g. skip mid-countdown, multiple starts).
- Tests: `WarningController` has no test coverage.
