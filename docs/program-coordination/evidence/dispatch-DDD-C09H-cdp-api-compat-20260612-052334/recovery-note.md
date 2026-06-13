# DDD-C09H CDP API Compatibility Reservation Recovery Note

Date: 2026-06-12T05:23:34+08:00
Coordinator: main session
Dispatch id: dispatch-DDD-C09H-cdp-api-compat-20260612-052334
Task id: DDD-C09H
Branch: main
Worktree: /Users/photonpay/project/canvas
Base SHA: 01aac65697d524f4cf2e92d954db088895631004
Mode: code-writing
Integration target: DDD_FINAL_MODULE

## Decision

Reserve the final missing DDD-C09 canvas-web compatibility seed for
`backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java`.

CDP is the only remaining required compatibility test target after DDD-C09G.
The seed is intentionally narrow: event ingestion, manual user tags, audience
snapshot helper behavior, and warehouse readiness through final
`canvas-context-cdp` APIs/application services. Broader CDP and warehouse route
families remain DDD-C09 cutover blockers.

## Fresh Pre-Dispatch Checks

- `node tools/program-coordination/check-dispatch-state.mjs .` exited 0 with
  `{ "ok": true }`.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  exited 0 with `{ "ok": true }` and `Program coordination checks passed.`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with `presentCount=6`, `missingCount=1`, and only
  `CdpApiCompatibilityTest` missing.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json`
  exited 1 as expected because CDP compatibility plus production
  controller/endpoint counts remain blockers.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` exited 0
  with the known `RiskRuleValidator` advisory only.
- `git status --short -- backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java`
  produced no output; the target path has no pre-existing changes.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpEventIngestionApplicationServiceTest,CdpTagApplicationServiceTest,AudienceSnapshotApplicationServiceTest,CdpWarehouseReadinessApplicationServiceTest`
  exited 0 with 13 tests, 0 failures.

## Scope

Allowed write scope:

- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java`

Forbidden write scope includes coordinator docs/state, production
`backend/canvas-web/src/main/java/**`, existing compatibility tests,
`backend/canvas-context-cdp/**`, old `backend/canvas-engine/**`,
`backend/canvas-boot/**`, `backend/pom.xml`, frontend, and docs.

## Notes

- Active dispatch registry is empty before this reservation.
- The broader worktree remains dirty from prior completed DDD/OSG work; the
  reserved CDP target path is clean.
- `BiApiCompatibilityTest.java` is untracked from completed DDD-C09G and is
  outside this worker's write scope.
- The new active dispatch must remain `RESERVED` with `pending-spawn` until
  `multi_agent_v1.spawn_agent` returns a real worker id and nickname.

## Worker Spawn

- `node tools/program-coordination/generate-worker-prompt.mjs DDD-C09H .`
  exited 0 after the reservation was recorded.
- `multi_agent_v1.spawn_agent` returned worker `Ampere`
  (`019eb898-7ff3-7e00-981b-af63440725e6`).
- The active row was moved from `RESERVED` to `RUNNING` only after that
  id/nickname existed.
