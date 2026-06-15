# DDD-C09BH Audience Route Reservation

Date: 2026-06-14 08:38 +08:00

## Selection

Preflight selected `route:/canvas/audiences` as the current top gap:

- oldControllerCount: 1
- oldEndpointCount: 10
- currentControllerCount: 0
- currentEndpointCount: 0
- legacy reference: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`

## Reserved Scope

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/AudienceFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/AudienceApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/AudienceCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/AudienceApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/AudienceController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/AudienceControllerCompatibilityTest.java`

## Target Routes

- `GET /canvas/audiences`
- `GET /canvas/audiences/source-fields`
- `POST /canvas/audiences/preview`
- `GET /canvas/audiences/{id}`
- `GET /canvas/audiences/ready`
- `POST /canvas/audiences`
- `PUT /canvas/audiences/{id}`
- `DELETE /canvas/audiences/{id}`
- `POST /canvas/audiences/{id}/compute`
- `GET /canvas/audiences/{id}/stat`

## Constraints

- Do not edit `backend/canvas-engine/**`.
- Do not edit any `pom.xml`.
- Do not import old `org.chovy.canvas.domain`, `dto`, `query`, `dal`, or `engine` packages.
- Preserve the final web compatibility envelope: `code=0`, `message=success`, no `errorCode` or `traceId` on success, `API_001` for bad requests.
- Default tenant is `X-Tenant-Id=7`; default actor is `X-Actor=operator-1`.
- Main coordinator stays on the critical path; subagent use is bounded and non-blocking to avoid idle waiting.
