# Spec: Testing Foundation

## Verification Status

Partially confirmed.

## Corrected Finding

The old "zero tests" framing is stale. Backend and frontend tests exist.

## Remaining Problems

- No Testcontainers usage was found for MySQL, Redis, or RocketMQ integration tests.
- Current tests skew toward unit and presentation tests.
- Critical behavior still needs coverage: canvas state machine, Redis/DB side effects, trigger admission, scheduler races, circuit breaker races, direct execution auth, and end-to-end canvas execution.

## Evidence

- Backend test files exist under `backend/canvas-engine/src/test`.
- Frontend has 30 test files under `frontend/src`.
- Search found StepVerifier and RocketMQ mocked tests, but no Testcontainers.
- P0 packages above identify missing tests as acceptance criteria.

## Acceptance Criteria

- Critical P0 packages have tests that fail before fixes and pass after fixes.
- Integration tests cover MySQL + Redis + RocketMQ or documented substitutes.
- Frontend editor refactors preserve behavior through component/hook tests.
