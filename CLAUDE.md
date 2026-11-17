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
