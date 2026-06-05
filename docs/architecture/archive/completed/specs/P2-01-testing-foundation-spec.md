# Spec: Testing Foundation

Source package: `docs/architecture/active/reviewed-packages/p2/testing-foundation/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`


## Verification Status

Confirmed for the automated testing foundation scope on 2026-06-05.

## Corrected Finding

The old "zero tests" framing is stale. Backend and frontend tests exist.

## Remaining Problems

- RocketMQ broker-level execution remains a documented manual substitute until a stable RocketMQ Testcontainer is introduced.
- Capacity and distributed performance runs remain manual because they require the local Docker stack or multi-worker environment.

## Evidence

- `docs/architecture/evidence/testing/test-layer-map.md` maps backend unit, backend integration, migration/schema, controller/API, and frontend behavior layers.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport/CanvasIntegrationTestBase.java` starts MySQL and Redis Testcontainers and applies Flyway.
- `backend/canvas-engine/src/test/java/org/chovy/canvas/testsupport/CanvasRocketMqTestSupport.java` documents the RocketMQ substitute gate.
- `docs/architecture/evidence/testing/manual-verification.md` records RocketMQ and capacity manual verification with owners and expiration dates.
- `docs/architecture/evidence/P2-01-testing-foundation.md` records the verification commands and results.

## Acceptance Criteria

- Critical P0 packages have named tests in `docs/architecture/evidence/testing/test-layer-map.md`.
- Integration tests cover MySQL and Redis through Testcontainers; RocketMQ has a documented substitute in `manual-verification.md`.
- Frontend editor behavior is protected by graph hydration, form value, and local draft tests.
- CI includes backend unit tests, backend integration tests, frontend tests/build, Flyway policy, profile validation, and deployment config checks.
