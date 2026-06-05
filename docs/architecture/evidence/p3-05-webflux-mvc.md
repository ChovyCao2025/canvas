# P3-05 WebFlux MVC Evidence

Date: 2026-06-05

## Verdict

Runtime migration from WebFlux to Spring MVC is deferred. The current codebase should continue WebFlux hardening and hybrid containment until P0 reactive hazards, benchmark evidence, transaction tests, and rollback/compatibility gates are complete.

## Created Documents

- `docs/architecture/decisions/work-products/p3-05-runtime-model/webflux-mvc-migration-inventory.md`
- `docs/architecture/decisions/adr/webflux-vs-mvc.md`
- `docs/architecture/decisions/work-products/p3-05-runtime-model/webflux-mvc-first-slice.md`

## Inventory Commands

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

## Inventory Result

- Runtime inventory: 1143 matching lines.
- Controllers: 89 `@RestController` files.
- Reactive controller files: 89 files with `Mono` or `Flux`.
- Blocking wrappers: 97 `web` or `engine` files with `Schedulers.boundedElastic`.
- Transactions: 39 `@Transactional` matches in main code.
- Runtime dependency: `spring-boot-starter-webflux` is present; `spring-boot-starter-web` and virtual-thread config were not found by the scan.
- Actuator starter is present and must be included in compatibility tests.

## Direct Review Findings

- Accepted P0-02 adapters: `BlockingWorkScheduler`, `TrackedReactiveTaskRegistry`.
- Accepted lifecycle or local bridges if disposal/drain evidence remains current: `KillSwitchSubscriber`, `NotificationRealtimeService`, `CanvasDisruptorService`.
- Review candidates before any runtime decision: `DeliveryOutboxConsumer`, `WebhookDispatcherService`, `HttpBiSnapshotRenderer`, `BiDeliveryAdapterService`, `UserInputService`, `CdpWarehouseSyntheticDataPathProbeService`.

## Decision Summary

- WebFlux hardening: continue.
- MVC migration: deferred.
- Hybrid containment: accepted as the current path.
- First endpoint group: deferred; platform/admin CRUD is only a possible candidate after gates pass.

## Verification Commands

```bash
test -f docs/architecture/decisions/work-products/p3-05-runtime-model/webflux-mvc-migration-inventory.md
rg -n "Controller|blocking dependency|transaction|streaming|migration risk|P0 reactive" docs/architecture/decisions/work-products/p3-05-runtime-model/webflux-mvc-migration-inventory.md
test -f docs/architecture/decisions/adr/webflux-vs-mvc.md
rg -n "Decision|Options|Benchmark|Team readiness|Transaction safety|Rollback|Compatibility|Revisit" docs/architecture/decisions/adr/webflux-vs-mvc.md
test -f docs/architecture/decisions/work-products/p3-05-runtime-model/webflux-mvc-first-slice.md
rg -n "endpoint group|request|response|error|auth|transaction|actuator|rollback|compatibility window|deferred" docs/architecture/decisions/work-products/p3-05-runtime-model/webflux-mvc-first-slice.md
```

Result: all documentation checks passed.

## Follow-ups

- Close or risk-accept the direct `.block()`, `.subscribe()`, and `Thread.sleep(...)` review candidates.
- Build a benchmark that compares WebFlux containment, MVC with platform threads, and MVC with virtual threads.
- Add characterization tests for the selected endpoint group before any runtime migration.
- Keep P3-00 and ADR-0006 gates in force; runtime migration must not bypass service, datasource, tenant, or event-contract gates.

No P3 files were staged or committed by this task.
