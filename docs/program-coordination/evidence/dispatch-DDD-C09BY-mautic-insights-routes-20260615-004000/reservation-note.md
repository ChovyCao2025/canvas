# DDD-C09BY Reservation

Date: 2026-06-15
Coordinator: main agent

## Scope

Top preflight gap: `route:/canvas/mautic-insights`

Legacy controller inspected read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MauticInspiredInsightController.java`

Routes in scope:

- `GET /canvas/mautic-insights/audience-membership?audienceId=&userId=`
- `GET /canvas/mautic-insights/journey-path?executionId=`
- `GET /canvas/mautic-insights/channel-preference?userId=&preferredChannel=`
- `GET /canvas/mautic-insights/suppression-timeline?userId=`
- `GET /canvas/mautic-insights/publish-health?canvasId=`
- `GET /canvas/mautic-insights/frequency-templates`

Exact reserved files:

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MauticInsightFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MauticInsightApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MauticInsightCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MauticInsightApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MauticInsightController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MauticInsightControllerCompatibilityTest.java`

## Guardrails

- Do not edit `backend/canvas-engine/**`.
- Do not edit Maven `pom.xml` files.
- Avoid ceremonial tests; prove the six route shapes, parameter forwarding, compatibility envelope, deterministic insight payloads, and bad request behavior.
