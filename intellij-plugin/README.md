# Take Break (IntelliJ)

Break reminders ported from the desktop app: countdown dialog, advance notice, idle skip.

- Settings: **Settings → Tools → Take Break** (all durations in seconds).
- Actions: **Tools → Take Break → Start Break Now / Pause / Resume Reminders**.
- Every third break is a long break; breaks are skipped after `Idle Threshold` seconds of no IDE input.

## Develop
```
gradle runIde        # sandbox IDE
gradle buildPlugin   # distributable zip in build/distributions
```

Status: scaffold, not yet built or run. Limitation: the dialog lives inside the IDE; no call detection.
