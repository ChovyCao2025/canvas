# Spec: Execution Concurrency Safety

## Verification Status

Confirmed.

## Problems

- `CircuitBreakerRegistry` uses volatile state plus separate atomic counters. State transitions are not a single compare-and-set operation.
- `ExecutionContext.putNodeOutput()` mutates grouped outputs, flat context, and size accounting as separate operations.
- `CanvasSchedulerService.closed` is a plain boolean.
- `PendingJitterGroup.add()` has improved rollback logic, but scheduler lifecycle and group state still require concurrency tests.
- Several virtual-thread starts are unmanaged and not tracked for shutdown or completion.

## Evidence

- `CircuitBreakerRegistry.java:96-143`
- `ExecutionContext.java:128-147`
- `CanvasSchedulerService.java:88-92`
- `CanvasSchedulerService.java:496-513`
- `CanvasService.java:537`
- `TriggerPreCheckService.java:259,294`
- `CanvasExecutionService.java:1399`
- `AudienceComputeTaskRunner.java:88`

## Acceptance Criteria

- Circuit breaker transitions are atomic and deterministic under concurrent success/failure/check calls.
- Execution context writes either become atomic snapshots or are confined to a single execution lane with documented ownership.
- Scheduler lifecycle state is thread-safe and covered by close/register/add race tests.
- Background virtual-thread work is tracked, bounded, and shut down cleanly.
