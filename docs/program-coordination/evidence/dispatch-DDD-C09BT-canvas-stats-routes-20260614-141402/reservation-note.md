# DDD-C09BT Reservation

Task: Canvas Stats Route Aliases
Dispatch: dispatch-DDD-C09BT-canvas-stats-routes-20260614-141402
Status: RESERVED
Worker: pending real spawn before RUNNING

Scope:
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CanvasStatsFacade.java
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CanvasStatsApplicationService.java
- backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/CanvasStatsCatalog.java
- backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/CanvasStatsApplicationServiceTest.java
- backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasStatsController.java
- backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasStatsControllerCompatibilityTest.java

Forbidden:
- backend/canvas-engine/**
- any pom.xml
- files outside the six allowed code/test paths

Preflight:
- Current canvas-web: 38 controllers / 583 endpoints
- Old canvas-engine web: 142 controllers / 806 endpoints
- Top gap: family:CanvasStats, old 1 controller / 7 endpoints, current 0 / 0
