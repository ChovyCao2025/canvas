# Spec: Dependency Abstraction And Vendor Lock-In

Source package: `docs/architecture/active/reviewed-packages/p2/dependency-abstraction-and-vendor-lock-in/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`


## Verification Status

Confirmed and partially remediated in repo-controlled code. Message publishing, replay rate
limiting, Groovy execution, reach delivery HTTP calls, and React Flow graph helper reuse now have
local contracts or adapter boundaries. Remaining direct usages are documented as `leave direct`,
`adapter only`, or future migration candidates in `docs/architecture/evidence/dependencies/`.

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

## Implementation Artifacts

- `docs/architecture/evidence/dependencies/dependency-inventory.md`
- `docs/architecture/evidence/dependencies/vendor-alternatives.md`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/CanvasMessageBus.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/DistributedRateLimiter.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/expression/ExpressionEngine.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/http/ExternalHttpClient.java`
- `frontend/src/pages/canvas-editor/reactFlowAdapter.ts`
