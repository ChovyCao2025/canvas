# DDD-C09CA Coordinator Closeout

Task: DDD-C09CA `/test-users`

Status: DONE_WITH_CONCERNS

## Scope

Implemented the six legacy Test User route aliases in final modules:

- `GET /test-users/sets`
- `POST /test-users/sets`
- `GET /test-users/sets/{setId}/users`
- `POST /test-users/sets/{setId}/users`
- `GET /test-users/{id}`
- `GET /test-users/{id}/preview`

## Files

- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/TestUserFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/TestUserApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/TestUserCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/TestUserApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/platform/TestUserController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/platform/TestUserControllerCompatibilityTest.java`

## Verification

Passed:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-platform -Dtest=TestUserApplicationServiceTest`
  - `TestUserApplicationServiceTest` 4/4
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=TestUserControllerCompatibilityTest test`
  - `TestUserControllerCompatibilityTest` 4/4
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - reactor built through `canvas-web`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - diagnostic pass; global cutover remains false
  - current `canvas-web`: 46 controllers / 634 endpoints
  - `/test-users` removed from the reported top gaps
  - next top gap: `route:/warehouse/e2e-certification`
- strict old-coupling scan over final Test User production files
  - no matches for `canvas-engine`, old domain packages, `TenantContext`, `TestUserRerunService`, or `AccessDeniedException`
- scoped `git diff --check`
  - no whitespace errors

## Test Rationale

The tests are compatibility-focused and intentionally non-ceremonial. They cover all six route shapes, default tenant `0L`, default actor `system`, tenant isolation, direct DO-style response field names, create set/user behavior, JSON string fields on normal user responses, parsed maps on preview, and `API_001` not-found envelopes.

## Accepted Concerns

- Averroes was spawned as a real sidecar worker but was closed before returning after a same-file test API mismatch; coordinator-owned evidence is authoritative.
- This is a compact deterministic compatibility seed for final-module route parity. Durable legacy DB-backed `TestUserRerunService` persistence and execution-rerun coupling remain outside this route-alias batch.
- Global DDD-C09 route parity still blocks final cutover; the next preflight gap is `route:/warehouse/e2e-certification`.
