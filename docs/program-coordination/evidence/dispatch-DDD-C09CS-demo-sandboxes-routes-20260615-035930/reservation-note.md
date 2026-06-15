# DDD-C09CS Reservation

Reserved route gap: `route:/demo-sandboxes`

Worker: `Euclid 019ec7b8-8b30-75c2-b088-e8d5b21ac8c0`

The worker was spawned before the dispatch moved to `RUNNING`.

Exact reserved files:

- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/api/DemoSandboxFacade.java`
- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/application/DemoSandboxApplicationService.java`
- `backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/domain/DemoSandboxCatalog.java`
- `backend/canvas-context-conversation/src/test/java/org/chovy/canvas/conversation/application/DemoSandboxApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/conversation/DemoSandboxController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/conversation/DemoSandboxControllerCompatibilityTest.java`

Coordinator will not idle-wait beyond one bounded wait; after timeout it will
inspect changed paths/evidence/tests and continue locally if needed.
