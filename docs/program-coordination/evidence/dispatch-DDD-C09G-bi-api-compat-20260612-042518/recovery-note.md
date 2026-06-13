# DDD-C09G BI API Compatibility Reservation Recovery Note

Date: 2026-06-12T04:25:18+08:00
Coordinator: main session
Dispatch id: dispatch-DDD-C09G-bi-api-compat-20260612-042518
Task id: DDD-C09G
Branch: main
Worktree: /Users/photonpay/project/canvas
Base SHA: 01aac65697d524f4cf2e92d954db088895631004
Mode: code-writing
Integration target: DDD_FINAL_MODULE

## Decision

Reserve the next exact-scope DDD-C09 canvas-web compatibility seed for
`backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`.

The BI catalog seed was selected from local evidence after a read-only sidecar
explorer (`Dirac`, `019eb855-e001-70b1-b255-a3df175a4577`) timed out once and
was closed/shutdown without a completed recommendation. The remaining required
compatibility test targets are CDP and BI. BI is the narrower next seed because
the final `BiCatalogFacade` exposes a compact catalog surface, while CDP still
spans multiple final facades and broader legacy route groups.

## Fresh Pre-Dispatch Checks

- `node tools/program-coordination/check-dispatch-state.mjs .` exited 0 with
  `{ "ok": true }`.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  exited 0 with `{ "ok": true }` and `Program coordination checks passed.`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with `presentCount=5`, `missingCount=2`, and missing
  `CdpApiCompatibilityTest` plus `BiApiCompatibilityTest`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json`
  exited 1 as expected because CDP/BI compatibility tests and production
  controller/endpoint counts are still blockers.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` exited 0
  with the known `RiskRuleValidator` advisory only.
- `git status --short -- backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java`
  produced no output; both target paths have no pre-existing changes.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-bi -Dtest=BiCatalogApplicationServiceTest`
  exited 0 with `Tests run: 4, Failures: 0, Errors: 0, Skipped: 0`.

## Scope

Allowed write scope:

- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`

Forbidden write scope includes coordinator docs/state, production
`backend/canvas-web/src/main/java/**`, existing compatibility tests,
`backend/canvas-context-bi/**`, old `backend/canvas-engine/**`,
`backend/canvas-boot/**`, `backend/pom.xml`, frontend, and docs.

## Notes

- Active dispatch registry is empty before this reservation.
- The broader worktree remains dirty from prior completed DDD/OSG work; the
  reserved BI target path is clean.
- `ExecutionApiCompatibilityTest.java` is untracked from completed DDD-C09F and
  is outside this worker's write scope.
- The new active dispatch must remain `RESERVED` with `pending-spawn` until
  `multi_agent_v1.spawn_agent` returns a real worker id and nickname.

## Worker Timeout Fallback

- `node tools/program-coordination/generate-worker-prompt.mjs DDD-C09G .`
  exited 0 after the reservation was recorded.
- `multi_agent_v1.spawn_agent` returned worker `Bacon`
  (`019eb865-3b5d-7573-bdd7-d3f6689b948c`), and the active row was moved from
  `RESERVED` to `RUNNING` only after that id/nickname existed.
- One `multi_agent_v1.wait_agent` call for Bacon timed out after 180 seconds.
- Timeout audit found no
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java`
  file and no worker-return evidence; only this recovery note existed in the
  DDD-C09G evidence directory.
- `multi_agent_v1.close_agent` for Bacon returned `previous_status: "running"`,
  followed by a `shutdown` notification.
- The active dispatch now runs as `main-agent-inline` with the explicit fallback
  reason above, under the same exact single-file reservation.
