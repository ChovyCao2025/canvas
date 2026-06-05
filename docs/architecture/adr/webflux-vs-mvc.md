# ADR WebFlux Vs MVC Runtime Model

## Status

Deferred.

## Context

P3-05 asks whether the platform should migrate from Spring WebFlux to Spring MVC, potentially with Java 21 virtual threads. The archived evolution document recommended MVC plus virtual threads because the application uses MyBatis, Redis, RocketMQ, and other blocking dependencies.

The current repository has changed since that recommendation. P0-02 introduced `BlockingWorkScheduler` and `TrackedReactiveTaskRegistry`, removed several scattered direct waits, and documented transaction side-effect boundaries. The current scan still shows a broad WebFlux surface: 89 controller files, 89 controller files with `Mono` or `Flux`, 97 `web` or `engine` files using `Schedulers.boundedElastic`, 39 `@Transactional` matches, and several direct blocking/subscription review candidates.

## Decision

Do not start a Spring MVC runtime migration in this P3 iteration. Keep WebFlux as the runtime model and continue hybrid containment until benchmark, transaction, rollback, and compatibility gates are satisfied.

This decision is `Deferred`, not `Rejected`. MVC plus virtual threads remains a candidate after evidence proves it is safer than WebFlux containment for the actual Marketing Canvas workload.

## Options

| Option | Benefits | Risks | Decision |
|---|---|---|---|
| WebFlux hardening | Smallest runtime blast radius; preserves existing controller signatures, filters, raw body handling, WebSocket behavior, and tests. | Requires continued discipline around blocking dependency containment and tracked subscriptions. | Continue now. |
| MVC migration | Aligns naturally with MyBatis and synchronous transactions; could simplify controller/service code after migration. | Touches nearly every controller, changes web stack behavior, needs actuator/security/error/streaming compatibility proof, and has no current benchmark. | Deferred. |
| Hybrid containment | Keeps WebFlux externally while isolating blocking calls and preparing low-risk MVC characterization slices. | Can become permanent complexity if gates are not enforced. | Accepted as the current path. |

## Benchmark Gate

No runtime migration can be accepted until a repeatable benchmark records:

- current WebFlux with bounded worker containment;
- MVC with platform threads;
- MVC with virtual threads;
- MySQL, Redis, RocketMQ, WebSocket, file upload/download, and long-running job assumptions;
- latency percentiles, throughput, error rate, worker saturation, DB pool saturation, GC, memory, and CPU;
- representative endpoint families: admin CRUD, canvas lifecycle, execution ingress, notification/reach, CDP/audience, BI/warehouse.

## Team readiness Gate

Before revisiting this ADR, owners must confirm:

- backend team can operate both WebFlux and MVC during a compatibility window;
- frontend team has API contract tests for changed routes;
- operations team has deployment, rollback, metrics, and incident procedures for either runtime;
- reviewers understand virtual-thread pinning risks around synchronized blocks, JDBC pools, Redis clients, provider clients, and blocking file operations.

## Transaction safety Gate

The selected runtime must prove:

- `@Transactional` methods remain active on the executing thread;
- rollback does not publish Redis, MQ, notification, scheduler, cache, or provider side effects prematurely;
- tenant and trace context propagate through request handling, background work, and outbox processing;
- execution and delivery idempotency behavior is unchanged;
- tests cover commit, rollback, timeout, retry, duplicate request, and cross-tenant denial behavior.

## Rollback Gate

Every approved migration slice must define:

- old and new request handling path;
- feature flag, route-level switch, or deployment rollback mechanism;
- database and cache rollback behavior;
- metric and log signals that trigger rollback;
- owner who can execute rollback during the compatibility window.

## Compatibility Gate

Compatibility must include:

- route paths and HTTP methods;
- request body binding, including raw signed bodies;
- response wrapper and error codes;
- auth and tenant context behavior;
- validation errors;
- CORS and security filters;
- actuator health and metrics;
- WebSocket and file upload/download behavior;
- frontend API tests and any public API contract tests.

## Revisit Trigger

Revisit this ADR only when all are true:

- P0 reactive review candidates in `docs/architecture/webflux-mvc-migration-inventory.md` are closed or risk-accepted by owner.
- A benchmark report exists and covers the endpoint families listed above.
- A first-slice characterization test suite exists and passes on the current WebFlux runtime.
- The selected slice has rollback, compatibility, observability, and owner signoff.
- ADR-0006 and P3-00 gates are not bypassed by runtime migration.

## Owner

Architecture owner with backend, frontend, operations, and security reviewers. A named implementation owner must be added before this ADR can move from `Deferred` to `Accepted`.

## Consequences

- No Spring MVC starter, Tomcat runtime, or virtual-thread configuration should be added under P3-05 without a follow-up ADR.
- Blocking dependency containment remains the required WebFlux discipline.
- Direct `.block()`, `.subscribe()`, and `Thread.sleep(...)` findings stay in the P0/P3 gate list.
- The first migration slice document records characterization requirements but does not approve implementation.

## Links

- `docs/architecture/archive/completed/specs/P3-05-webflux-to-mvc-migration-spec.md`
- `docs/architecture/webflux-mvc-migration-inventory.md`
- `docs/architecture/webflux-mvc-first-slice.md`
- `docs/architecture/evidence/P0-02-reactive-threading-inventory.md`
- `docs/architecture/evidence/p3-05-webflux-mvc.md`
