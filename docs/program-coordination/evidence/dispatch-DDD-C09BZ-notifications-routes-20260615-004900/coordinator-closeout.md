# DDD-C09BZ Coordinator Closeout

Task: DDD-C09BZ `/canvas/notifications`

Status: DONE_WITH_CONCERNS

## Scope

Implemented the six legacy notification route aliases in final modules:

- `GET /canvas/notifications?unreadOnly=&category=&archived=&page=&size=`
- `GET /canvas/notifications/unread-count`
- `PUT /canvas/notifications/{notificationId}/read`
- `PUT /canvas/notifications/read-all`
- `PUT /canvas/notifications/{notificationId}/archive`
- `POST /canvas/notifications/ws-ticket`

## Files

- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/NotificationFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/NotificationApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/NotificationCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/NotificationApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/notifications/NotificationController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/notifications/NotificationControllerCompatibilityTest.java`

Additional compile fix:

- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MauticInsightControllerCompatibilityTest.java`
  - added the missing `MauticInsightApplicationService` import found by the C09BZ web test compile.

## Verification

Passed:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-platform -Dtest=NotificationApplicationServiceTest`
  - `NotificationApplicationServiceTest` 4/4
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=NotificationControllerCompatibilityTest test`
  - `NotificationControllerCompatibilityTest` 4/4
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - reactor built through `canvas-web`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - diagnostic pass; global cutover remains false
  - current `canvas-web`: 45 controllers / 628 endpoints
  - `/canvas/notifications` removed from the reported top gaps
  - next top gap: `route:/test-users`
- strict old-coupling scan over final Notification production files
  - no matches for `canvas-engine`, old domain packages, `TenantContext`, `NotificationService`, `NotificationWebSocketTicketService`, or `AccessDeniedException`
- scoped `git diff --check`
  - no whitespace errors

## Test Rationale

The tests are compatibility-focused and intentionally non-ceremonial. They cover all six route shapes, legacy DTO field names, paging/default clamp behavior, unread/archived filtering, scoped read/archive/read-all mutation semantics, unread count changes, ws-ticket prefix and 60-second TTL, and bad-request/forbidden envelopes.

## Accepted Concerns

- Lorentz was spawned as a real sidecar worker but was closed before returning to avoid same-file write conflicts; coordinator-owned evidence is authoritative.
- This is a compact deterministic compatibility seed for final-module route parity. Durable legacy Redis ticket storage, realtime websocket delivery, and database-backed notification persistence remain outside this route-alias batch.
- Global DDD-C09 route parity still blocks final cutover; the next preflight gap is `route:/test-users`.
