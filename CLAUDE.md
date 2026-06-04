## Session Mode

Use Superpowers in a minimal-token, YAML-first mode.
- Prefer short YAML specs and 1–2 line plan steps.
- Do not recap the full plan unless asked.
- If output gets verbose, return only the next step.
- Prefer updating existing task and memory files.
- Never dispatch more than 4 subagents at once.
- Squash commits and keep the commit description under 30 words.

Use Superpowers in this order:
1. /superpowers:brainstorm
2. /superpowers:write-plan
3. /superpowers:execute-plan
4. /superpowers:finish

## Task Tracking

After spec approval:
1. Create or update `.claude/memory/todo.md`.
2. Use `.claude/memory/todo.md` as the source of truth.
3. Use only: `PENDING:`, `DOING:`, `DONE:`, `BLOCKED:`.
4. Group tasks under `## Setup`, `## Implementation`, `## Tests`, `## Review`.
5. Keep tasks short and concrete.
6. After each completed task:
  - mark it `DONE:`
  - promote one `PENDING:` item to `DOING:`
  - add new work as `PENDING:`
  - mark blockers `BLOCKED:`
  - output only the next step
7. Do not copy the full spec into `todo.md`.
8. Prefer `.claude/memory/todo.md` over user-level auto-memory.
9. Use `.claude/memory/MEMORY.md` as the index if it exists.
10. Spec approval is incomplete until `todo.md` has at least one `PENDING:` item for each major area.

## Memory Safety

- Do not save personal info, secrets, credentials, tokens, keys, emails, phone numbers, local usernames, private paths, or machine-specific details.
- Do not create personalized files, user-home notes, or machine-local memory files.
- Save only repo-relevant, shareable project memory under `.claude/memory/`.

---

## Project Memory

Extended session context is in `.claude/memory/` (git-tracked, portable across machines).
Read the relevant files when deeper context is needed:

| File | Contents |
|---|---|
| `.claude/memory/project_overview.md` | Architecture, modules, tech stack, file map, key design decisions |
| `.claude/memory/pending_work.md` | Known issues and potential future improvements |
| `.claude/memory/todo.md` | Active task tracking (PENDING/DOING/DONE/BLOCKED) |

---

## Project Context

**Stack**: Java 21, JavaFX 21.0.9, JPMS, Maven → fat-jar `take-break-app.jar`
**Module**: `brane.fx.takebreak`
**Config file** (runtime): `~/.config/take-break/config.properties`

### Build & test commands
```
# Fast headless test (always use this — ApplicationTest hangs)
mvn -q test -Dtest="BreakConfigTest"

# Compile check
mvn -q compile

# Full fat-jar build
mvn -q package

# Run (macOS — sets HiDPI flags)
bash scripts/run.sh
```

### Key entry points
| File | Purpose |
|---|---|
| `BreakApplication.java` | `main()`, `start()`, tray setup, `reschedule()` |
| `controllers/ConfigController.java` | Settings dialog, FXML-bound sliders, reschedule callback |
| `controllers/BreakController.java` | Countdown overlay, `AnimationTimer`, hide callback |
| `domain/BreakConfig.java` | Immutable record; `fromFile()` / `save()` |
| `inject/Injector.java` | Hand-rolled DI; `initDefault()` seeds `breakConfig` + `jsonMapper` |
| `tasks/BreakSchedule.java` | Tick `Runnable`; re-resolves `breakConfig` from Injector each tick |

### Pitfalls
- **Never run `mvn test` bare** — `ApplicationTest` calls `Application.launch()` and hangs.
- `ApplicationTest.java` is a known broken test; skip or delete it before fixing.
- `jackson-databind` is registered in `Injector` but never resolved — candidate for removal.
- Spacing/break durations are all in **seconds** throughout the codebase.
