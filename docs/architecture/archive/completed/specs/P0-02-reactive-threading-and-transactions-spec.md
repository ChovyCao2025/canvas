# Spec: Reactive Threading And Transaction Boundaries

Source package: `docs/architecture/active/reviewed-packages/p0/reactive-threading-and-transactions/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`


## Verification Status

Implemented and verified for the first runtime batch.

## Problems

- The app is WebFlux/reactive (`spring.main.web-application-type: reactive`) while much of the data layer is blocking MyBatis/JDBC.
- Original repository scan found `.block()`, fire-and-forget `.subscribe()`, `Thread.sleep()`, and many `@Transactional` usages in runtime paths.
- The first runtime batch now has no direct `.block()`, `.subscribe()`, or `Thread.sleep()` in the target files; blocking waits and background Reactor tasks are centralized through `BlockingWorkScheduler` and `TrackedReactiveTaskRegistry`.
- Remaining `.subscribe()` hits are lifecycle subscriptions or locally tracked infrastructure bridges and are classified in evidence.
- `@Transactional` boundaries are synchronous; lifecycle operations use DB-only transaction methods followed by repairable external side effects rather than a durable outbox.

## Evidence

- `application.yml:5`
- Evidence inventory: `docs/architecture/evidence/P0-02-reactive-threading-inventory.md`
- Converted `.block()` examples: `TagImportSourceService`, `CanvasSchedulerService`, `AudienceBatchComputeService`, `AudienceEvaluationContextFetcher`
- Converted `Thread.sleep()` examples: `TriggerRouteService`, `CanvasRouteInitializer`, `AudienceComputeTaskRunner`, `CanvasDisruptorService`
- Converted fire-and-forget examples: `CanvasSchedulerService`, `DagEngine`, `WaitResumeService`, `CanvasExecutionRequestExecutor`
- `@Transactional` examples: `CanvasService.java`, `CanvasTransactionService.java`, `CanvasOpsService.java`, `CdpTagService.java`, `TagImportService.java`
- Existing comments acknowledge DB/Redis inconsistency risk and the current DB-only transaction split in `CanvasService` and `CanvasTransactionService`.

## Acceptance Criteria

- Blocking DB, Redis, HTTP, and MQ operations do not run on Netty event-loop threads.
- Fire-and-forget `.subscribe()` calls in business paths are replaced by explicit orchestration or tracked background execution.
- Transactional DB writes and external side effects are separated by an outbox, compensating action, or repairable state transition.
- Tests include StepVerifier or integration coverage for the converted paths.
