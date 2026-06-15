# DDD-C09DT Ops Canvas Template/Review Routes Closeout

Status: DONE_WITH_CONCERNS
Date: 2026-06-15T07:45:00+08:00

## Scope

Ported the remaining legacy `OpsController` canvas-template/review routes into final modules:

- `GET /canvas/templates`
- `POST /canvas/{id}/save-as-template`
- `POST /canvas/from-template/{templateId}`
- `GET /canvas/pending-reviews`

No edits were made to `backend/canvas-engine/**` or any `pom.xml`.

## Worker coordination

Spawned worker Russell `019ec880-a323-7143-b937-eb9ccaf40c23` before marking DDD-C09DT RUNNING. Russell confirmed `/ops/cache/invalidate/{id}` was already covered in final modules and returned evidence at:

- `docs/program-coordination/evidence/dispatch-DDD-C09DT-ops-cache-invalidate-route-20260615-074000/worker-return.md`

Coordinator continued locally without idle waiting after identifying that `family:Ops` was caused by the old `OpsController` canvas template/review endpoints, not the already-covered cache route.

## Verification

- RED: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasCompatibilityApplicationServiceTest`
  - Failed before implementation with missing `TemplateView`, `saveAsTemplate`, `listTemplates`, `createFromTemplate`, and `pendingReviews` symbols.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasCompatibilityApplicationServiceTest`
  - Passed: 4 tests, 0 failures.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CanvasControllerCompatibilityTest test`
  - Passed: 10 tests, 0 failures.
- GREEN: `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - Passed.
- `node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs`
  - Passed: 7 tests, 0 failures.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Passed; current canvas-web 88 controllers / 790 endpoints; `family:Ops` removed; next top gap `route:/architecture`; cutoverReady remains false.
- Strict old-coupling scan over touched files returned no matches.
- `git diff --check` over touched DDD-C09DT files passed.

## Accepted concerns

- Compatibility seed is deterministic and in-memory; durable template persistence and approval workflow parity remain broader DDD work.
- Global cutover remains blocked by remaining controller/endpoint gaps.
