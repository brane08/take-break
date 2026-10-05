# Take Break (VS Code)

Break reminders ported from the desktop app: a countdown panel, advance notice, and idle skip.

- Every third break is a long break; all durations are in seconds (`takeBreak.*` settings).
- Breaks are skipped when there has been no editor activity for `takeBreak.idleThresholdSeconds`.
- Commands: `Take Break: Skip Break`, `Start Break Now`, `Pause / Resume Reminders`.

## Develop
```
npm install
npm run compile   # then F5 in VS Code to launch an Extension Development Host
npm run package   # builds a .vsix
```

Limitation: the panel is an editor tab, not a full-screen overlay; call detection is not ported.
