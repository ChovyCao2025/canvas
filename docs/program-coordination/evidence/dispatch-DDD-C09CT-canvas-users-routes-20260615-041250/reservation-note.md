# DDD-C09CT Reservation

Reserved route gap: `family:CanvasUser`

Worker: `Herschel 019ec7bf-4119-7412-bf66-5647f04d4193`

The worker was spawned before the dispatch moved to `RUNNING`.

Exact reserved files:

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CanvasUserFacade.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CanvasUserApplicationService.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/CanvasUserCatalog.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/CanvasUserApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasUserController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasUserControllerCompatibilityTest.java`

Coordinator will not idle-wait beyond one bounded wait; after timeout it will
inspect changed paths/evidence/tests and continue locally if needed.
