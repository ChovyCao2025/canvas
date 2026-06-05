# Spec: Production Platform Components

Source package: `docs/architecture/reviewed-packages/p3/platform-evolution/`

Coverage matrix: `docs/architecture/reviewed-packages/coverage-matrix.md`

## Verification Status

Implemented as a component decision package. Redisson is accepted only for a proof-of-value behind a local coordination interface; other platform components are deferred or rejected for first proof. Evidence and decisions are in `docs/architecture/evidence/p3-07-platform-components.md`, `docs/architecture/work-products/p3-07-platform-components/platform-component-decision-matrix.md`, `docs/architecture/adr/platform-component-first-pov.md`, and `docs/architecture/work-products/p3-07-platform-components/platform-component-abstraction-plan.md`.

## Source Documents

- `docs/architecture/archive/evolution/production-practice-review.md`
- `docs/architecture/archive/evolution/target-architecture-overview.md`

## Scope

Evaluate production platform components such as XXL-JOB, Redisson, Nacos, Knife4j, Feign/Sentinel, Spring Boot Admin, ClickHouse, and centralized logging. The goal is not to add every tool, but to decide which component solves a current validated problem.

## Acceptance Criteria

- Each proposed component has an ADR with problem, alternatives, operational cost, failure modes, and rollback.
- Components are introduced behind local abstractions when they affect core business code.
- Observability and operational ownership exist before production rollout.
- Component adoption is tied to a measurable capability gap, not tool preference.
