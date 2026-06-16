# Canvas Journey Module Quality Progress

Date: 2026-06-16
Branch: main

## Guardrails

- Initial command run: `git status --short`.
- Existing unrelated changes observed in CI, deploy, docs/program-coordination, scripts, and tools paths, plus untracked `docs/prompt/`.
- Continuation check reran `git status --short`; additional unrelated changes now include `frontend/src/services/systemOptions.ts`, `frontend/src/services/systemOptions.test.ts`, `docs/e2e-browser-audit.md`, and other module-quality progress files. These were not modified or reverted.
- Second continuation check reran `git status --short`; unrelated workspace changes remain, with additional untracked module-quality progress files. These were not modified or reverted.
- Per goal, no user changes were reverted.
- Write scope is constrained to:
  - `tmp/module-quality-canvas-journey-progress.md`
  - `docs/e2e-browser-audits/canvas-journey.md`

## Code Review Progress

Reviewed frontend canvas surfaces:

- `frontend/src/pages/canvas-list`
- `frontend/src/pages/canvas-editor`
- `frontend/src/pages/canvas-stats`
- `frontend/src/pages/canvas-users`
- `frontend/src/components/canvas`
- `frontend/src/components/node-panel`
- `frontend/src/components/config-panel`
- canvas hooks, types, and services

Reviewed backend/API surfaces:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasStatsController.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasUserController.java`
- execution request and message-send API wrappers used by stats
- canvas user API contract used by users page

## Review Findings So Far

- No confirmed blocking code defect from static review yet.
- Canvas users frontend endpoints match backend `CanvasUserController`.
- Canvas stats source is syntactically valid; an earlier combined command printed duplicate context, but `nl` and build confirmed the actual file has one `executionRequestColumns` declaration.
- Local MySQL has real canvas rows in `canvas_db.canvas`; observed ids include `1`, `3`, `4`, `5`, `6`, `7`, `8`, `9`, `10`, and `11`.
- Backend startup is blocked before HTTP bind:
  - First attempt failed with Java 8 running Maven against Spring Boot 3 (`RunMojo` class file version 61).
  - Retried with Java 21 at `/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home`.
  - Java 21 attempt failed while parsing `backend/canvas-boot/src/main/resources/mapper/execution/CanvasExecutionMapper.xml`.
  - Root cause observed: the current boot mapper still references removed legacy type `org.chovy.canvas.dal.dataobject.CanvasExecutionDO`.
  - Because the goal restricts writes to the two audit files, no source fix was made.
- Codex in-app Browser is unavailable in this session:
  - Browser bootstrap returned `Browser is not available: iab`.
  - `agent.browsers.list()` returned `[]`.
- Continuation retry confirmed the same Browser state:
  - `agent.browsers.list()` returned `[]`.
  - `agent.browsers.get('iab')` returned `Browser is not available: iab`.
- Continuation runtime check confirmed no listeners on `127.0.0.1:3000` or `127.0.0.1:8080`; curl to both ports failed to connect.
- Second continuation runtime check confirmed the same:
  - no listeners on `127.0.0.1:3000` or `127.0.0.1:8080`;
  - curl to both ports failed to connect;
  - `agent.browsers.list()` returned `[]`;
  - `agent.browsers.get('iab')` returned `Browser is not available: iab`.
- Current mapper evidence still shows `backend/canvas-boot/src/main/resources/mapper/execution/CanvasExecutionMapper.xml:14` using `resultType="org.chovy.canvas.dal.dataobject.CanvasExecutionDO"`.
- Maven can be forced to Java 21 with `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home`, but bare `java` on PATH still resolves to Java 8. This explains why default backend startup fails earlier than the Java 21 retry.
- Browser evidence remains missing for: authenticated navigation, API response shape through Vite proxy, visual/layout overlap, blank screen, ErrorBoundary behavior in-browser, refresh behavior, and route-to-route navigation.

## Verification Run

- `cd frontend && npm run test -- --run src/pages/canvas-list/canvasProjectFilters.test.ts src/pages/canvas-list/importExportFlow.test.ts src/pages/canvas-editor/useCanvasGraphState.test.ts src/pages/canvas-editor/graphSerialization.test.ts src/pages/canvas-editor/CanvasEditorErrorBoundary.test.tsx src/pages/canvas-stats/effectClosure.test.ts src/pages/canvas-stats/operatorTables.test.ts src/components/config-panel/formValues.test.ts src/components/canvas/branchHandles.test.ts`
  - Result: passed, 9 files, 25 tests.
  - Note: expected jsdom console error output is emitted by ErrorBoundary tests.
- `cd frontend && npm run build`
  - Result: passed.
- `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-context-canvas,canvas-context-execution -Dtest=CanvasQueryApplicationServiceTest,CanvasStatsApplicationServiceTest,CanvasUserApplicationServiceTest,CanvasDraftApplicationServiceTest,CanvasExecutionApplicationServiceTest,ExecutionRequestApplicationServiceTest test`
  - Result: passed, 17 tests across canvas and execution modules.
- `docker exec canvas-mysql mysql -uroot -proot -N -e "SHOW DATABASES; USE canvas_db; SHOW TABLES LIKE 'canvas'; SELECT id, name, status FROM canvas LIMIT 10;"`
  - Result: `canvas` table exists and has real rows, including id `1`.
- Continuation DB check:
  - `SELECT id, status FROM canvas ORDER BY id LIMIT 5` returned candidate ids `1`, `3`, `4`, `5`, and `6`.
- Second continuation DB check:
  - `SELECT id, status FROM canvas ORDER BY id LIMIT 5` again returned candidate ids `1`, `3`, `4`, `5`, and `6`.

## Blocked Audit

- Same blocking conditions have now repeated across three consecutive goal turns:
  - Codex in-app Browser target `iab` is unavailable.
  - Backend API cannot bind on `:8080` because the boot mapper still references removed legacy class `org.chovy.canvas.dal.dataobject.CanvasExecutionDO`.
  - The frontend/backend route checks cannot be run without those prerequisites.
- This is a true impasse under the current constraints because source writes are forbidden except for these two audit files, and the Browser target is external to the repo/workspace.

## Next Steps

- Restore local backend startup by resolving the legacy MyBatis mapper alias in `CanvasExecutionMapper.xml`, if source edits are allowed in a later turn.
- Reconnect or enable the Codex in-app Browser (`iab`) for this session.
- Restart/reuse local frontend on `127.0.0.1:3000`.
- Use real local canvas id `1` or another API/list-selected id to test only:
  - `/canvas`
  - `/canvas/:id/edit`
  - `/canvas/:id/stats`
  - `/canvas/:id/users`
- If Browser exposes a canvas-journey-owned bug, fix it within allowed source scope only if the write constraint changes. Current explicit write constraint says only the two audit files may be written, so source fixes require coordination.
