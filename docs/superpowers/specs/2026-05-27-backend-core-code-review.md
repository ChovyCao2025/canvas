# Backend Core Code Review Findings - 2026-05-27

Scope: backend core execution logic, scheduler/resume behavior, MQ trigger routing, persistence, cache SDK, and backend API security.

## Fix Order

1. Critical execution correctness: failure propagation, persistent retry deduplication, unauthenticated execution endpoints, and L1 cache expiry.
2. High-risk runtime correctness: manual approval pause, context persistence, timeout resume targeting/status, quota bypass for internal resumes, branch skip propagation.
3. High-risk infrastructure/security: MQ route initialization/refresh, data source/API definition exposure, CDP tag idempotency, scheduler pagination, parsed DAG invalidation.
4. Medium-risk consistency: trace filtering, wait filters, approval completion race, force cancel status, LOOP/GOTO idempotency.

## Findings

| ID | Severity | Area | Finding | References | Remediation |
| --- | --- | --- | --- | --- | --- |
| BE-001 | Critical | Execution status | Node execution exceptions are converted to a normal map and then marked as successful. Failed DAG runs can be persisted as `SUCCESS`/`SUCCEEDED`. | `DagEngine.java`, `CanvasExecutionService.java`, `CanvasExecutionRequestExecutor.java` | Return/throw a typed failure result and make all service/request paths mark failures terminally. |
| BE-002 | Critical | Persistent request retry | `request_retry` self-deduplicates before admission and does not release dedup keys on early return, so overflow retries can be dropped forever. | `CanvasExecutionService.java` | Bypass or release dedup for internal retry triggers and guarantee cleanup on ignored retries. |
| BE-003 | Critical | API security | Direct workflow execution endpoints are publicly permitted and accept arbitrary `canvasId`, `userId`, and payload. | `SecurityConfig.java`, `ExecutionController.java` | Require authentication and bind user identity/authorization for direct execution. |
| BE-004 | Critical | Cache SDK | L1 Caffeine cache has no TTL and can return stale or null-sentinel values indefinitely after L2 expiry. | `TieredCacheImpl.java` | Configure `expireAfterWrite`/refresh semantics using the configured TTL and invalidate L1 consistently. |
| BE-005 | High | Manual approval | Manual approval handler marks the node `WAITING` but returns success, so the scheduler overwrites it to `SUCCESS` and execution context is deleted. | `ManualApprovalHandler.java`, `CanvasExecutionService.java` | Return a pending outcome/result and keep context until approval resume. |
| BE-006 | High | Context persistence | Persisted execution context is masked before saving and then reloaded as live state, corrupting secrets and payload values. | `ContextPersistenceService.java` | Persist raw execution state; apply masking only on read/display boundaries. |
| BE-007 | High | Timeout resume | Timeout resumes target nodes by type because the resume trigger omits the exact node id. | `DagEngine.java`, `CanvasExecutionService.java` | Carry the pending node id through timeout resume metadata and match by node id first. |
| BE-008 | High | Timeout recovery | Timeout fallback can mark a terminal failed node back to `WAITING` during recursive re-entry. | `DagEngine.java` | Do not overwrite terminal node states when re-entering resume paths. |
| BE-009 | High | Quota/cooldown | Internal timeout/request retry resumes consume user quota and trigger cooldown checks. | `CanvasExecutionService.java` | Treat all internal resume/retry trigger types as system continuations for precheck/quota bypass. |
| BE-010 | High | Branch/merge | Skipped branch propagation only marks the branch entry as skipped, so downstream merge nodes can hang. | `DagEngine.java`, `HubHandler.java` | Propagate skip through the inactive branch until merge/terminal semantics are satisfied. |
| BE-011 | High | MQ routing | Cold-start MQ route initialization can acknowledge and drop valid trigger messages before routes are loaded. | `CanvasRouteInitializer.java`, `MqTriggerConsumer.java` | Make route readiness explicit and reject/retry messages while routes are not initialized. |
| BE-012 | High | MQ route refresh | Full route refresh deletes all routes before adding replacements, racing publish/offline flows and causing transient misses. | `MqRouteRefreshService.java`, `TriggerRouteService.java`, `CanvasService.java` | Use atomic replace/versioned route sets or per-canvas transactional refresh. |
| BE-013 | High | API security | Data source passwords are returned/mutable by any authenticated user. | `DataSourceConfigController.java`, `DataSourceConfigDO.java` | Mask passwords in responses and restrict mutation/read to admin/owner roles. |
| BE-014 | High | API security/SSRF | API definition configuration enables authenticated non-admin users to register arbitrary outbound URLs that handlers call server-side. | `ApiDefinitionController.java`, `ApiCallHandler.java` | Restrict mutation to privileged users and validate allowed schemes/hosts. |
| BE-015 | High | CDP idempotency | CDP tag idempotency is checked after mutation and without a transaction, allowing duplicate side effects/races. | `CdpTagService.java`, `V74__cdp_core.sql` | Reserve idempotency key before mutation in one transaction and treat duplicate key as replay. |
| BE-016 | High | Scheduler | Scheduled `TAGGER_GROUP` pagination repeatedly reads page 1 and never advances. | `CanvasSchedulerService.java` | Increment page index/cursor across batches. |
| BE-017 | High | Parsed DAG cache | Parsed DAG cache is local-only and not invalidated across instances on canvas publication/update. | `CanvasConfigCache.java` | Add version-based keying or Redis-backed invalidation. |
| BE-018 | Medium | Trace API | Trace endpoint accepts canvas id but does not filter by it. | `CanvasStatsController.java` | Include canvas id in trace query predicates and tests. |
| BE-019 | Medium | Wait filters | WAIT event filters are persisted but never evaluated when resuming subscriptions. | `WaitHandler.java`, `WaitSubscriptionService.java`, `CanvasWaitSubscriptionDO.java` | Evaluate event filters before resume and ignore non-matching events. |
| BE-020 | Medium | Approval race | Approval completion can double-resume because update/resume is not atomic/idempotent. | `CanvasExecutionManagementController.java` | Perform compare-and-set approval status update and resume only on transition. |
| BE-021 | Medium | Cancellation | Force cancellation can leave execution status `RUNNING` after in-flight thread interruption. | `InFlightExecutionRegistry.java`, `CanvasExecutionService.java` | Persist cancellation status when force-cancel is accepted and stop later success updates. |
| BE-022 | Medium | Idempotency/control flow | LOOP/GOTO revisit semantics conflict with node execution idempotency keyed only by node id. | `DagEngine.java`, `LoopHandler.java`, `GotoHandler.java` | Include visit/iteration token in idempotency key or explicitly reset node execution state on loop/goto. |

## Verification Notes

Status: remediations BE-001 through BE-022 have been implemented in the backend/cache SDK code paths covered by this review. During verification, additional stale unit tests were removed per request where they encoded obsolete constructor or concurrency-threshold assumptions instead of current behavior.

Additional fixes found during verification:
- Execution request payload parse failures are now handled inside the request executor error path, so claimed requests are marked `FAILED` instead of escaping and remaining `RUNNING`.
- DAG special-node timeout timers now use a Reactor scheduler that supports delayed scheduling; the previous virtual-thread executor-backed scheduler rejected `Mono.delay`.

Fresh verification:
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test` from `backend/`
- Result: backend reactor `BUILD SUCCESS`; `canvas-cache-sdk` 30 tests passed, `canvas-engine` 362 tests passed.
