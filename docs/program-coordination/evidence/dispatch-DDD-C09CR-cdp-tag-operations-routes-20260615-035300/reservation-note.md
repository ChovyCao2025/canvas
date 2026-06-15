# DDD-C09CR Reservation

Reserved route gap: `route:/cdp/tag-operations`

Worker: `Popper 019ec7b2-4cdb-7393-b8e5-346ad8c84f92`

The worker was spawned before the dispatch moved to `RUNNING`.

Exact reserved files:

- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpTagOperationFacade.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpTagOperationApplicationService.java`
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpTagOperationCatalog.java`
- `backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpTagOperationApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpTagOperationController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpTagOperationControllerCompatibilityTest.java`

Coordinator will not idle-wait beyond one bounded wait; after timeout it will
inspect changed paths/evidence/tests and continue locally if needed.
