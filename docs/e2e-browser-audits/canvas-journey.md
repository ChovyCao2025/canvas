# Canvas Journey E2E Browser Audit

Date: 2026-06-16
Branch: main

## Scope

Routes to test with the Codex in-app Browser:

- `/canvas`
- `/canvas/:id/edit`
- `/canvas/:id/stats`
- `/canvas/:id/users`

Primary review scope:

- Canvas list/project filters
- Editor graph state
- Autosave/import/export
- Node config validation
- Stats/user detail data loading
- Execution APIs
- Tests around graph helpers and route data

## Pre-Browser Evidence

- Initial `git status --short` was run before other work.
- Existing unrelated workspace changes were observed and not reverted.
- Current branch: `main`.
- Focused frontend tests passed: 9 files, 25 tests.
- Frontend production build passed.

## Browser Results

Blocked in the current session.

- Codex in-app Browser bootstrap returned: `Browser is not available: iab`.
- Browser target discovery returned an empty list: `agent.browsers.list()` -> `[]`.
- Continuation retry returned the same result: no Browser targets and `iab` unavailable.
- Second continuation retry returned the same result: no Browser targets and `iab` unavailable.
- Backend `:8080` is not running because `canvas-boot` fails before binding HTTP.
- Frontend `:3000` was initially observed listening, but was no longer reachable when API/data checks were run later.
- Continuation port check found no listeners on `127.0.0.1:3000` or `127.0.0.1:8080`.
- Second continuation port check also found no listeners on `127.0.0.1:3000` or `127.0.0.1:8080`.

## Findings

### Code Review Findings

- No confirmed canvas-journey source defect was fixed in this pass.
- Static/API contract checks found that `frontend/src/services/cdpApi.ts` canvas-user endpoints match `CanvasUserController`.
- `frontend/src/pages/canvas-stats/index.tsx` is syntactically valid and builds; no duplicate declaration exists in the actual file.

### Runtime Blockers

- `canvas-boot` startup with the default Java failed because Maven was using Java 8.
- Retrying with Java 21 reached application startup, then failed in MyBatis mapper parsing:
  - File: `backend/canvas-boot/src/main/resources/mapper/execution/CanvasExecutionMapper.xml`
  - Failing alias: `org.chovy.canvas.dal.dataobject.CanvasExecutionDO`
  - Error: `ClassNotFoundException: Cannot find class: org.chovy.canvas.dal.dataobject.CanvasExecutionDO`
- Current-state file check confirms the alias is still present at `CanvasExecutionMapper.xml:14`.
- Maven can be run under Java 21 by setting `JAVA_HOME`, but the default `java` on PATH is still Java 8.
- This prevents API-backed Browser checks for list loading, editor loading, stats rendering, users rendering, refresh behavior, and navigation.

## Blockers

- In-app Browser unavailable: `iab` target missing.
- Backend unavailable: `canvas-boot` fails before HTTP bind due legacy MyBatis mapper alias.
- A real local canvas id is available in MySQL (`canvas_db.canvas.id = 1`; continuation check also saw ids `3`, `4`, `5`, and `6`), but it cannot be verified through the API until backend startup is fixed.
- Blocked-audit threshold met: the same Browser and backend blockers repeated across three consecutive goal turns.

## Verification

- Frontend focused tests: passed, 9 files / 25 tests.
- Frontend build: passed.
- Backend focused tests: passed, 17 tests across `canvas-context-canvas` and `canvas-context-execution`.
- Local DB data check: `canvas_db.canvas` exists and contains real rows.

## Routes

- `/canvas`: blocked, Browser unavailable and backend down.
- `/canvas/:id/edit`: blocked, Browser unavailable and backend down. Candidate id: `1`.
- `/canvas/:id/stats`: blocked, Browser unavailable and backend down. Candidate id: `1`.
- `/canvas/:id/users`: blocked, Browser unavailable and backend down. Candidate id: `1`.

## Remaining Risks

- Browser-only UI risks remain unverified: blank screen, ErrorBoundary rendering, console/network errors, layout overlap, refresh behavior, node/config panel basics, and navigation back to list.
- Source fixes were not made because the current goal allows writing only the two audit files.

## Needs Coordination

- Enable/expose the Codex in-app Browser target `iab`, or provide an equivalent Browser target for this session.
- Allow source edits if the backend startup blocker should be fixed here; the current mapper alias at `backend/canvas-boot/src/main/resources/mapper/execution/CanvasExecutionMapper.xml:14` points to a removed legacy class.
