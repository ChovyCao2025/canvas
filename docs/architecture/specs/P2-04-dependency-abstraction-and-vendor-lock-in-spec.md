# Spec: Dependency Abstraction And Vendor Lock-In

Source package: `docs/architecture/todo/p2/dependency-abstraction-and-vendor-lock-in/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Confirmed as an architecture risk; replacement urgency requires product/ops decision.

## Problems Covered

- Redis, RocketMQ, MySQL/MyBatis, Groovy, and React Flow are used directly in many places.
- Distributed locks, rate limiting, MQ publishing/consuming, expression evaluation, and graph editing lack stable internal abstractions.
- Direct dependency use makes migration and testing harder.

## Source Coverage

- `archive/reviews/architecture-supplement-review-2026-05.md`: vendor lock-in section.
- `archive/evolution/production-practice-review.md`: Redisson, Feign/Sentinel, Nacos, Knife4j recommendations.
- `archive/evolution/service-architecture-design.md`: service communication and event bus.

## Acceptance Criteria

- Message publishing/consuming has a local interface.
- Distributed lock and rate limiter usage go through local interfaces.
- Expression engine contract exists before Groovy alternatives are evaluated.
- Dependency abstraction work has migration tests and does not create unused indirection.
