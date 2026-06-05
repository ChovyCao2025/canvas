# Spec: API Contract And Validation

Source package: `docs/architecture/reviewed-packages/p1/api-contract-and-validation/`

Coverage matrix: `docs/architecture/reviewed-packages/coverage-matrix.md`


## Verification Status

Confirmed.

## Problems

- Repository scan on 2026-06-04 found 40 controller files and 206 handler methods under `org.chovy.canvas.web`.
- Several APIs expose persistence objects or loosely typed request bodies.
- Error responses mix plain messages, prefixed strings, and generic `R.fail()` responses.
- Public OpenAPI-style endpoints need explicit contract, auth, and validation rules.

## Evidence

- `docs/architecture/evidence/P1-02-api-contract-inventory.md` records the current 40-controller inventory.
- First implementation pass adds Bean Validation to high-risk create/update/execute/report/preview endpoints.
- `GlobalExceptionHandler.java` now maps validation, malformed input, auth/forbidden, conflict, trigger rejection, and generic failures to stable `errorCode` values while preserving numeric `code`.
- `SecurityConfig.java` now emits JSON `AUTH_002` / `AUTH_003` responses for unauthenticated and forbidden security failures.
- `frontend/src/services` production code no longer has `any` service contracts; remaining `any` occurrences are test-only mock casts.

## Acceptance Criteria

- Controllers validate request DTOs at boundaries.
- Public endpoints have explicit schemas, authentication requirements, and error codes.
- API responses use a consistent error model.
- Frontend service layer uses typed request and response contracts for high-traffic APIs.
