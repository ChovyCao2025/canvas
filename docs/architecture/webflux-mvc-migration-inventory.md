# WebFlux MVC Migration Inventory

Date: 2026-06-05

Status: P3-05 planning artifact. This inventory does not approve runtime migration.

## Source Commands

```bash
rg -n "\.block\(|\.subscribe\(|Thread\.sleep\(|@Transactional|Mono<|Flux<|Redis|RocketMQ|MyBatis" backend/canvas-engine/src/main/java/org/chovy/canvas > /tmp/webflux_mvc_inventory.txt
wc -l /tmp/webflux_mvc_inventory.txt
rg -l "@RestController" backend/canvas-engine/src/main/java/org/chovy/canvas/web | wc -l
rg -l "Mono<|Flux<" backend/canvas-engine/src/main/java/org/chovy/canvas/web | wc -l
rg -l "Schedulers\.boundedElastic" backend/canvas-engine/src/main/java/org/chovy/canvas/web backend/canvas-engine/src/main/java/org/chovy/canvas/engine | wc -l
rg -n "\.block\(|\.subscribe\(|Thread\.sleep\(" backend/canvas-engine/src/main/java/org/chovy/canvas
rg -n "@Transactional" backend/canvas-engine/src/main/java/org/chovy/canvas | wc -l
rg -n "spring-boot-starter-webflux|spring-boot-starter-web|spring\.threads\.virtual|server\.netty|actuator" backend/canvas-engine/pom.xml backend/canvas-engine/src/main/resources/application*.yml
```

## Current Runtime Shape

| Signal | Current result | Interpretation |
|---|---:|---|
| Runtime inventory matches | 1143 lines | The reactive/blocking surface is broad enough that a migration must be gated. |
| `@RestController` files under `web` | 89 | The HTTP layer is already consistently WebFlux-shaped. |
| Controller files with `Mono` or `Flux` | 89 | MVC migration would touch almost every controller. |
| `web` or `engine` files with `Schedulers.boundedElastic` | 97 | Blocking dependency containment is common and intentional. |
| `@Transactional` matches in main code | 39 | Transaction semantics need characterization before runtime changes. |
| Web starter | `spring-boot-starter-webflux` only | The app is not currently configured as MVC. |
| Actuator starter | present | Actuator behavior must be part of compatibility tests. |
| Virtual threads config | no match | MVC plus virtual threads is not currently enabled. |

## Direct Blocking And Subscription Findings

| Pattern | Current files | Classification | Required action |
|---|---|---|---|
| `.block()` central adapter | `BlockingWorkScheduler` | Accepted adapter from P0-02 | Keep as the only approved synchronous Reactor wait surface. |
| `.block()` runtime consumer | `DeliveryOutboxConsumer` | Review candidate | Prove MQ listener thread is non-event-loop and add shutdown/backpressure evidence. |
| `.block(Duration...)` remote HTTP | `WebhookDispatcherService`, `HttpBiSnapshotRenderer`, `BiDeliveryAdapterService` | Review candidate | Move behind `BlockingWorkScheduler` or document thread ownership and timeout fallback. |
| `.subscribe()` central registry | `TrackedReactiveTaskRegistry` | Accepted adapter from P0-02 | Keep all fire-and-forget work named and drainable. |
| `.subscribe()` lifecycle subscription | `KillSwitchSubscriber`, `NotificationRealtimeService` | Accepted if disposed | Keep `@PreDestroy` disposal and shutdown tests. |
| `.subscribe()` business path | `UserInputService` | Review candidate | Route through `TrackedReactiveTaskRegistry` or convert to a durable outbox/resume command. |
| `.subscribe()` Disruptor bridge | `CanvasDisruptorService` | Accepted local bridge | Keep in-flight accounting and shutdown drain evidence. |
| `Thread.sleep(...)` | `CdpWarehouseSyntheticDataPathProbeService` | P0 reactive hazard | Replace with interrupt-aware parking, scheduler delay, or bounded retry policy. |

## Endpoint Group Inventory

| endpoint group | Representative controllers | blocking dependency | transaction need | streaming/reactive benefit | migration risk | Notes |
|---|---|---|---|---|---|---|
| Platform/admin CRUD | `TenantController`, `ApiDefinitionController`, `DataSourceConfigController`, `SystemOptionController`, `PluginRegistryController` | MyBatis/service calls wrapped with `boundedElastic` | Low to medium; mostly service-local | Low | Low to medium | Best candidate family if gates later approve an MVC slice, but it still shares auth, tenant, response wrapper, validation, and exception behavior. |
| Canvas authoring and lifecycle | `CanvasController`, `CanvasBatchOperationController`, `CanvasExecutionManagementController`, `CanvasStatsController` | MyBatis, Redis route/cache, scheduler, export/import, policy services | High for publish/offline/archive/canary/rollback | Low | High | Must stay behind P3-00 and ADR-0006 gates because route/cache side effects and version state are coupled. |
| Execution ingress | `ExecutionController`, `ExecutionRerunController`, `UserInputController`, `CanvasMqTriggerRejectedController` | DAG engine, Redis context/dedupe, MQ, raw signed body parsing | Medium to high | Medium for raw reactive body and async trigger paths | High | Do not use as first MVC slice; it is part of the runtime hot path. |
| Notification and reach | `NotificationController`, `MessageDeliveryController`, `DeliveryReceiptController`, `MarketingPreferenceCenterController`, `ContactabilityController` | Redis realtime, delivery outbox, provider adapters, MyBatis | Medium | Medium for realtime fanout | High | Must align with the P3-02 Reach / Notification boundary-hardening decision. |
| CDP, audience, and metadata | `AudienceController`, `CdpUserController`, `CdpTagOperationController`, `TagImportController`, `TagImportSourceController`, `MetaController` | MyBatis, Redis bitmaps, external Tagger/API calls, import jobs | Medium | Low to medium | Medium to high | Can be characterized after P3-03 data-platform and P3-04 datasource ownership gates. |
| Warehouse and BI | `CdpWarehouse*Controller`, `bi/*Controller`, `AnalyticsController` | MyBatis, warehouse operations, file upload/download, snapshot rendering, export packaging | Medium | Medium for `FilePart`, download, and long-running jobs | Medium to high | Broad new surface. Migration should wait until BI/warehouse tests and async task operations settle. |
| AI and integration | `Ai*Controller`, `ChannelConnectorController`, `MauticInspiredInsightController` | Remote providers, credentials, MyBatis, resilience wrappers | Medium | Low to medium | Medium | Needs provider timeout, secret, and rollback evidence before runtime changes. |

## P0 reactive Prerequisites

The runtime decision is invalid until these P0 reactive gates are closed:

- Every direct `.block()` outside `BlockingWorkScheduler` is either removed, wrapped, or documented with non-event-loop thread proof.
- Every business-path `.subscribe()` goes through `TrackedReactiveTaskRegistry`, a lifecycle subscription with disposal, or a durable outbox.
- Every `Thread.sleep(...)` is removed from main runtime paths.
- Controller blocking dependency calls stay on bounded workers until a different runtime is selected.
- Transaction boundaries have tests for commit, rollback, side-effect ordering, and tenant context.
- Benchmark evidence compares current WebFlux containment, MVC plus platform threads, and MVC plus virtual threads under the same MySQL, Redis, RocketMQ, and WebSocket assumptions.
- Rollback keeps the same route, payload, auth, tenant, error, actuator, metrics, and frontend API behavior.

## Recommendation

The current codebase should keep WebFlux while hardening containment. A full MVC migration is deferred because the controller surface is large, the blocking dependency model is already partially contained, and no benchmark proves that MVC plus virtual threads is safer for this workload.
