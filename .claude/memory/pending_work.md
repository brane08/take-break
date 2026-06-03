# pending work

## Resolved (as of last session)
- Live reschedule on config save — done via `reschedule(BreakConfig)` callback
- Reset-to-defaults button in config.fxml — done
- HiDPI args in `run.cmd` — done
- `BreakConfig` load/save unit tests — done (`BreakConfigTest.java`)
- Spacing label "minutes" mismatch — label now reads "Spacing (seconds)"

## Known issues

- **`DEFAULT_SPACING=10` below slider min=60**: `spacingSlider` in config.fxml has `min="60.0"`,
  but `BreakConfig.DEFAULT_SPACING = 10`. On first launch the slider snaps to 60 even though
  the scheduler starts with 10s spacing. Fix: either raise the default to 60 or lower the slider min.

- **`SettingsController` is a dead scaffold**: `controllers/SettingsController.java` has slider
  listeners but no `save()`, no `load()`, no FXML controller binding. It was added as a future
  replacement for `ConfigController` but is currently unused. Decision needed: implement it or delete it.

- **TilesFX unused**: `eu.hansolo.tilesfx` is declared in pom.xml and module-info but nothing
  in source uses it (old `Tile` import was removed from `BreakController`). Either use it or drop
  the dep + module-requires to reduce fat-jar size.

## Potential improvements

- Wire `SettingsController` to a new settings FXML if keeping it; ensure `DI_BREAK_CONFIG` key is used.
- Consider moving `ConfigController` to `controllers` package for consistency.
