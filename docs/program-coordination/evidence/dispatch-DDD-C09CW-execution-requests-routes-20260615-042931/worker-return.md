# DDD-C09CW Worker Return

Task: DDD-C09CW `/canvas/execution-requests` route compatibility seed

Worker:
- Hegel `019ec7d3-24bf-7b30-883b-8b7d8facedf4`
- Returned read-only sidecar evidence at `docs/program-coordination/evidence/dispatch-DDD-C09CW-execution-requests-routes-20260615-sidecar/execution-requests-compatibility-sidecar.md`.

Files changed by coordinator:
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/ExecutionRequestFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/application/ExecutionRequestApplicationService.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/domain/ExecutionRequestCatalog.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/application/ExecutionRequestApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/execution/ExecutionRequestController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/execution/ExecutionRequestControllerCompatibilityTest.java`

Implementation summary:
- Added final-module execution request facade/application/domain seed for legacy list, single replay, and batch replay routes.
- Preserved list filters, one-based paging, page-size clamp, tenant scoping, and newest-first list ordering.
- Preserved replay safety rules: `FAILED`/`RETRY` only by default, `force=true` bypass, missing id and cross-tenant rejection messages.
- Preserved batch replay limit normalization, explicit status guard, oldest-first selection, and result keys: `count`, `limit`, `requestIds`, `dispatchFailureCount`, `dispatchFailedRequestIds`.
- Added WebFlux controller with standard success envelope and `API_001` bad-request mapping.

Meaningful tests:
- `ExecutionRequestApplicationServiceTest` covers filters, paging, single replay, force replay, tenant rejection, missing id, batch replay defaults, explicit status guard, limit cap, and fixture registration.
- `ExecutionRequestControllerCompatibilityTest` covers all three legacy routes, default parameters, query/path forwarding, success envelope, and `API_001` error envelope.

Accepted concerns:
- This is a deterministic compatibility seed, not durable execution request persistence.
- Real mapper-backed mutation, replay rate limiting, security principal operator extraction, and immediate Disruptor dispatch remain outside this route-parity batch.
