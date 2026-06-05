# Spec: Production Resilience And Disaster Recovery

## Verification Status

Partially confirmed.

## Problems

- `server.shutdown: graceful` and `spring.lifecycle.timeout-per-shutdown-phase` are not configured.
- Several components have `@PreDestroy`, but in-flight canvas execution drain is incomplete.
- `CanvasDisruptorService.shutdown()` shuts down the disruptor, but async chains launched through `.subscribe()` may outlive the WorkHandler.
- Execution context and route state depend heavily on Redis; repository evidence does not show a full cold recovery path for all paused/resumed executions.
- Raw virtual threads and Groovy executor usage are not fully managed.

## Evidence

- `application.yml` has no graceful shutdown settings.
- `CanvasDisruptorService.java:275-277`
- `TraceWriteBuffer.java:72-73`
- `CanvasSchedulerService.java:167`
- `GroovyHandler.java:58`
- `docs/architecture/archive/reviews/architecture-supplement-review-2026-05.md` disaster-recovery section.
- `docs/architecture/archive/remediation/part7-resilience.md` shutdown section.

## Acceptance Criteria

- The service stops accepting new trigger work during shutdown.
- MQ consumption, direct execution, scheduler triggers, and disruptor publishing drain or reject consistently.
- Running executions are either completed, checkpointed, or explicitly marked resumable.
- Redis loss has a documented and tested recovery behavior for paused/resumed contexts and trigger routes.
- Shutdown behavior is covered by tests or scripted verification.
