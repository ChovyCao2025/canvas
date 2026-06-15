# DDD-C09CX Worker Return

Task: DDD-C09CX `/canvas/mq-trigger-rejected` route compatibility seed

Worker:
- Cicero `019ec7dc-fb75-73e0-94d8-890956a4a3f3`
- Returned read-only sidecar evidence at `docs/program-coordination/evidence/dispatch-DDD-C09CX-mq-trigger-rejected-routes-20260615-sidecar/sidecar-review.md`.

Files changed by coordinator:
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/MqTriggerRejectedFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/MqTriggerRejectedApplicationService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/domain/MqTriggerRejectedCatalog.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/application/MqTriggerRejectedApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/MqTriggerRejectedController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/execution/MqTriggerRejectedControllerCompatibilityTest.java`

Implementation summary:
- Added final-module MQ rejected facade/application/domain seed for legacy list, detail, and replay routes.
- Preserved tag/reason filtering, paging defaults, page-size cap, and newest-first list ordering.
- Preserved missing-row message shape, invalid-body replay message, missing `userId/messageCode/payload` replay message, valid route sorting/skipping, and replay result keys.
- Added WebFlux controller with standard success envelope and `API_001` bad-request mapping.

Meaningful tests:
- `MqTriggerRejectedApplicationServiceTest` covers list filters, detail missing message, replay request creation, dispatch-failure accounting, malformed/incomplete body rejection, and invalid route skipping.
- `MqTriggerRejectedControllerCompatibilityTest` covers all three legacy routes, query/path forwarding, paging defaults, success envelope, and `API_001` error envelope.

Accepted concerns:
- This is a deterministic compatibility seed, not durable MQ rejected persistence.
- Real route service lookup, execution request enqueueing, immediate Disruptor dispatch, and old rejected-body JSON parsing with Jackson remain outside this route-parity batch.
