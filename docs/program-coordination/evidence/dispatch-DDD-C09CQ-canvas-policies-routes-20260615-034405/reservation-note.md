# DDD-C09CQ Reservation

Reserved route gap: `route:/canvas/policies`

Worker: `Halley 019ec7ab-b4d9-7e93-b814-854edb5b11c9`

The worker was spawned before the dispatch moved to `RUNNING`.

Exact reserved files:

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingPolicyFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingPolicyApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingPolicyCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MarketingPolicyApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingPolicyController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingPolicyControllerCompatibilityTest.java`

Coordinator will not idle-wait beyond one bounded wait; after timeout it will
inspect changed paths/evidence/tests and continue locally if needed.
