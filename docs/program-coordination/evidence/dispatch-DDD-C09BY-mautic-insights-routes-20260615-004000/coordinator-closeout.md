# DDD-C09BY Coordinator Closeout

Task: DDD-C09BY `/canvas/mautic-insights`

Status: DONE_WITH_CONCERNS

## Scope

Implemented the six legacy read-only Mautic Insight route aliases in final modules:

- `GET /canvas/mautic-insights/audience-membership?audienceId=&userId=`
- `GET /canvas/mautic-insights/journey-path?executionId=`
- `GET /canvas/mautic-insights/channel-preference?userId=&preferredChannel=`
- `GET /canvas/mautic-insights/suppression-timeline?userId=`
- `GET /canvas/mautic-insights/publish-health?canvasId=`
- `GET /canvas/mautic-insights/frequency-templates`

## Files

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MauticInsightFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MauticInsightApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MauticInsightCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MauticInsightApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MauticInsightController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MauticInsightControllerCompatibilityTest.java`

## Verification

Passed:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=MauticInsightApplicationServiceTest`
  - `MauticInsightApplicationServiceTest` 5/5
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MauticInsightControllerCompatibilityTest test`
  - `MauticInsightControllerCompatibilityTest` 4/4
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests`
  - reactor built through `canvas-web`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - diagnostic pass; global cutover remains false
  - current `canvas-web`: 44 controllers / 622 endpoints
  - `/canvas/mautic-insights` removed from the reported top gaps
  - next top gap: `route:/canvas/notifications`
- strict old-coupling scan over final Mautic Insight production files
  - no matches for `canvas-engine`, old domain packages, `TenantContext`, `MauticInspiredInsightService`, or `AccessDeniedException`
- scoped `git diff --check`
  - no whitespace errors

## Test Rationale

The tests are intentionally compatibility-focused, not ceremonial. They cover legacy route shape parity, query forwarding, default channel normalization, deterministic client-facing payloads, and the API_001 bad-request envelope.

## Accepted Concerns

- Bacon was spawned as a real sidecar worker but did not return a packet before shutdown; coordinator-owned evidence is authoritative.
- The implementation is a compact deterministic compatibility seed for final-module route parity. Durable legacy `MauticInspiredInsightService` persistence/runtime integration remains outside this route-alias batch.
- Global DDD-C09 route parity still blocks final cutover; the next preflight gap is `route:/canvas/notifications`.
