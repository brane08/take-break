# pending work

## Known issues

- **`ApplicationTest.java` hangs**: calls `Application.launch()` inside a `@Test`; never exits.
  Not a real test. Fix: delete it, or replace with a headless FXML-load assertion.

- **Jackson registered but unused**: `Injector.initDefault()` registers `new ObjectMapper()` as
  `"jsonMapper"` but nothing ever resolves it. The dep and registration can be removed.
  Note: `com.fasterxml.jackson.databind` was already removed from module-info.java but the
  Jackson dep may still be in pom.xml — verify before removing.

- **TakeBreak.iconset/ temp dir**: `src/main/resources/TakeBreak.iconset/` left by icns generation,
  untracked. Safe to `rm -rf src/main/resources/TakeBreak.iconset`.

## Potential improvements

- Remove Jackson dep + `"jsonMapper"` registration in `Injector.initDefault()`.
- Fix or delete `ApplicationTest.java`.
- `ApplicationLock` uses hardcoded port 14425 — make configurable or document.
- Packaging: consider macOS code signing / notarization for Gatekeeper.
- Packaging: consider Linux `.deb`/`.rpm` or Windows `.msi` for easier installation.
- Tests: `BreakController` has no headless test coverage yet.
- Tests: `WarningController` has no test coverage.
