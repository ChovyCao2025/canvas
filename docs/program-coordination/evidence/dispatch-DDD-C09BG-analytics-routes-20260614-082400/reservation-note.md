# DDD-C09BG Analytics Route Reservation

Date: 2026-06-14 08:24 +08:00

## Selection

Preflight selected `route:/analytics` as the current top gap:

- oldControllerCount: 1
- oldEndpointCount: 10
- currentControllerCount: 0
- currentEndpointCount: 0
- legacy reference: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsController.java`

## Reserved Scope

- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/AnalyticsFacade.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/AnalyticsViews.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/AnalyticsApplicationService.java`
- `backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/AnalyticsCatalog.java`
- `backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/AnalyticsApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/analytics/AnalyticsController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/analytics/AnalyticsControllerCompatibilityTest.java`

## Constraints

- Do not edit `backend/canvas-engine/**`.
- Do not edit any `pom.xml`.
- Do not import old `org.chovy.canvas.domain`, `dto`, `query`, or `dal` packages.
- Preserve the final web compatibility envelope: `code=0`, `message=success`, no `errorCode` or `traceId` on success, `API_001` for bad requests.
- Default tenant is `X-Tenant-Id=7`.
- Main coordinator stays on the critical path; subagent use is bounded and non-blocking to avoid idle waiting.
