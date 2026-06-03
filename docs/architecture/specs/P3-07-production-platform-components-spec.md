# Spec: Production Platform Components

Source package: `docs/architecture/todo/p3/platform-evolution/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`

## Verification Status

Planning material. Some component risks are already covered by P1 release/observability and P2 dependency abstraction; this spec tracks larger platform component choices.

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
