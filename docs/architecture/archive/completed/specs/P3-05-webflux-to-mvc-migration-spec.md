# Spec: WebFlux To MVC Migration

Source package: `docs/architecture/active/reviewed-packages/p3/platform-evolution/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`

## Verification Status

Implemented as a runtime decision package. MVC migration is deferred; WebFlux hardening plus hybrid containment remains the current path. Evidence and decisions are in `docs/architecture/evidence/p3-05-webflux-mvc.md`, `docs/architecture/decisions/work-products/p3-05-runtime-model/webflux-mvc-migration-inventory.md`, `docs/architecture/decisions/adr/webflux-vs-mvc.md`, and `docs/architecture/decisions/work-products/p3-05-runtime-model/webflux-mvc-first-slice.md`.

## Source Documents

- `docs/architecture/archive/evolution/webflux-to-mvc-migration.md`
- `docs/architecture/archive/reference/tech-stack.md`
- `docs/architecture/archive/remediation/part2-security-concurrency.md`

## Scope

Decide whether Marketing Canvas should migrate from WebFlux to Spring MVC or harden the current WebFlux model. The decision must be based on workload, dependency profile, team familiarity, latency/throughput tests, and migration risk.

## Acceptance Criteria

- A decision record compares WebFlux hardening, MVC migration, and hybrid containment.
- Blocking call inventory and performance baseline are captured before migration.
- Endpoint migration order, compatibility, rollback, and test strategy are documented.
- Transaction semantics are explicit in the chosen model.
- No runtime migration starts until P0 reactive hazards have a containment plan.
