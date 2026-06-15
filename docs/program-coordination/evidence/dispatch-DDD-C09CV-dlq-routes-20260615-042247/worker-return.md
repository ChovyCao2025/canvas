# DDD-C09CV Worker Return

Task: DDD-C09CV `/canvas/dlq` route compatibility seed

Worker:
- Hypatia `019ec7cc-e9b3-7163-b361-196a1dfe8e1b`
- Returned read-only sidecar evidence at `docs/program-coordination/evidence/dispatch-DDD-C09CV-dlq-routes-20260615-sidecar/sidecar-review.md`.

Files changed by coordinator:
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/DlqFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/DlqApplicationService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/domain/DlqCatalog.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/application/DlqApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/DlqController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/execution/DlqControllerCompatibilityTest.java`

Implementation summary:
- Added final-module DLQ facade/application/domain seed for legacy `/canvas/dlq` list, replay, and delete routes.
- Preserved one-based list paging, `canvasId` filtering, size clamp to `100`, and newest-first ordering.
- Added replay output that keeps original dead-letter metadata, payload, `skipSuccessNodes`, and `dlq-replay-` id prefix without auto-deleting the DLQ entry.
- Added explicit delete result for final-module visibility while keeping the route success envelope compatible.
- Added WebFlux controller with standard `CompatibilityEnvelope` and `API_001` bad-request mapping.

Meaningful tests:
- `DlqApplicationServiceTest` covers paging/filtering, replay metadata, non-auto-delete, explicit delete, missing replay error, and fixture registration.
- `DlqControllerCompatibilityTest` covers all three legacy routes, query/path/default forwarding, success envelope, and missing replay `API_001` mapping.

Accepted concerns:
- This is a deterministic compatibility seed, not durable DLQ persistence or real execution-engine replay.
- Mapper-backed persistence, malformed JSON fallback, and integration with a real execution trigger remain outside this route-parity batch.
