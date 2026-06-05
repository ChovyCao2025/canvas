# ADR-0001 Web Runtime Model

## Status

Accepted

## Context

The backend currently exposes WebFlux controllers and uses Reactor return types while many domain services, MyBatis mappers, Flyway, Redis, RocketMQ, and cache operations remain blocking. P0 and P1 remediation already established bounded elastic scheduling, transaction boundaries, admission controls, and shutdown drain behavior. P3-05 tracks whether a future WebFlux to MVC migration is warranted.

## Decision

Keep the current WebFlux runtime for now, but treat blocking service and mapper work as bounded blocking sections that must run on `Schedulers.boundedElastic()` or the managed executor chosen by the owning service. Keep `CanvasDisruptorService` as the trigger burst buffer for asynchronous behavior triggers. Do not add new `.block()` calls in request threads.

## Alternatives

- Migrate to MVC immediately: deferred to `P3-05-webflux-to-mvc-migration-spec.md` because it changes controller contracts, test infrastructure, and runtime tuning in one step.
- Make the full persistence path reactive: rejected for now because MyBatis, Flyway, and existing mapper contracts are blocking and would require a larger data-access rewrite.

## Consequences

- Controller code can keep existing reactive API contracts while isolating blocking sections.
- Runtime behavior depends on executor sizing and bounded queue protection.
- New endpoints must document whether they are public HMAC, bearer-authenticated, or internal operations.

## Rollback Trigger

Revisit this ADR if request-thread blocking reappears in tests, bounded elastic saturation exceeds the P2-02 SLO threshold for two consecutive releases, or P3-05 produces an approved MVC migration plan.

## Owner

Runtime platform owner.

## Linked Specs

- `docs/architecture/archive/completed/specs/P0-02-reactive-threading-and-transactions-spec.md`
- `docs/architecture/archive/completed/specs/P0-04-execution-concurrency-safety-spec.md`
- `docs/architecture/archive/completed/specs/P1-04-observability-and-ops-spec.md`
- `docs/architecture/archive/completed/specs/P3-05-webflux-to-mvc-migration-spec.md`
