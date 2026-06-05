# Spec: Reactive Threading And Transaction Boundaries

## Verification Status

Confirmed.

## Problems

- The app is WebFlux/reactive (`spring.main.web-application-type: reactive`) while much of the data layer is blocking MyBatis/JDBC.
- Repository scan found `.block()`, fire-and-forget `.subscribe()`, `Thread.sleep()`, and many `@Transactional` usages in runtime paths.
- Some blocking work is correctly wrapped in `Schedulers.boundedElastic()`, but several direct blocking calls remain.
- `@Transactional` boundaries are synchronous and do not cover asynchronous side effects such as Redis, scheduler registration, MQ publishing, or fire-and-forget work.

## Evidence

- `application.yml:5`
- `.block()` examples: `TagImportSourceService.java:143`, `CanvasSchedulerService.java:424`, `AudienceBatchComputeService.java:250`, `AudienceEvaluationContextFetcher.java:51`
- `Thread.sleep()` examples: `TriggerRouteService.java:190`, `CanvasRouteInitializer.java:76`, `AudienceComputeTaskRunner.java:216`
- `@Transactional` examples: `CanvasService.java`, `CanvasTransactionService.java`, `CanvasOpsService.java`, `CdpTagService.java`, `TagImportService.java`
- Existing comments acknowledge DB/Redis inconsistency risk in `CanvasService.java:188-195` and `CanvasTransactionService.java:40-42`.

## Acceptance Criteria

- Blocking DB, Redis, HTTP, and MQ operations do not run on Netty event-loop threads.
- Fire-and-forget `.subscribe()` calls in business paths are replaced by explicit orchestration or tracked background execution.
- Transactional DB writes and external side effects are separated by an outbox, compensating action, or repairable state transition.
- Tests include StepVerifier or integration coverage for the converted paths.
