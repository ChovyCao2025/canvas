# P0-05 Production Resilience And DR Evidence

## Implemented Behavior

- Graceful shutdown is configured through `server.shutdown=graceful` and explicit Spring lifecycle timeout settings.
- `ExecutionLifecycleGate` rejects new canvas execution chains after shutdown begins and waits for in-flight chains.
- Disruptor publishing rejects after shutdown starts and waits for subscribed execution monos to finish.
- Fire-and-forget virtual-thread work is routed through `ManagedVirtualThreadExecutor`, including Groovy precompile and DLQ side writes.
- Redis Pub/Sub subscriptions used by notifications and kill switch have explicit `@PreDestroy` disposal.
- DAG special-node timeout scheduling uses an owned scheduler that is disposed with `DagEngine`.
- Redis runtime routes can be rebuilt from published DB versions through `POST /ops/recovery/runtime-state/rebuild`.
- Runtime recovery rebuilds MQ, behavior, and Tagger Redis routes and replaces local scheduled-trigger registrations.
- If an internal continuation trigger loses its Redis `ExecutionContext`, it is skipped and the original `PAUSED` execution is marked `FAILED` when the execution id is available.

## Recovery Command

Manual runtime-state rebuild:

```bash
curl -X POST http://localhost:8080/ops/recovery/runtime-state/rebuild
```

The response contains:

- `publishedCanvasCount`
- `mqRouteCount`
- `behaviorRouteCount`
- `taggerRouteCount`
- `scheduledRegistrationCount`

Direct-call triggers do not use Redis route tables; recovery for direct calls is limited to normal canvas config cache reload/invalidation.

## Focused Verification

Command:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=ApplicationShutdownConfigTest,ExecutionLifecycleGateTest,CanvasExecutionServiceResumeTest,CanvasDisruptorServiceLifecycleTest,ManagedVirtualThreadExecutorTest,KillSwitchSubscriberTest,DagEngineLifecycleTest,TraceWriteBufferTest,TriggerRouteRecoveryServiceTest,OpsControllerRecoveryTest,OpsControllerTemplateTest,CanvasSchedulerServiceTest,MqRouteRefreshServiceTest,TriggerRouteServiceTest,ContextPersistenceServiceTest test
```

Result: 30 tests, 0 failures, 0 errors.

Module command:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
```

Result: 428 tests, 0 failures, 0 errors.

Expected log noise:

- `CanvasExecutionServiceResumeTest` logs missing Redis context failures intentionally.
- `CanvasDisruptorServiceLifecycleTest` logs a synthetic `boom` failure intentionally.
- `DagEngineLifecycleTest` logs a synthetic DLQ warning intentionally.
