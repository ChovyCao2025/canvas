# Spec: API Contract And Validation

Source package: `docs/architecture/todo/p1/api-contract-and-validation/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Confirmed.

## Problems

- Repository scan found 29 controller files and zero `@Valid` / `@Validated` usage in controller classes.
- Several APIs expose persistence objects or loosely typed request bodies.
- Error responses mix plain messages, prefixed strings, and generic `R.fail()` responses.
- Public OpenAPI-style endpoints need explicit contract, auth, and validation rules.

## Evidence

- `find backend/canvas-engine/src/main/java -name '*Controller.java'` returns 29.
- `rg -l '@Valid|@Validated' ... *Controller.java` returns 0.
- `GlobalExceptionHandler.java:84`
- `SecurityConfig.java:62-70`
- `frontend/src/services/api.ts` still has many `any` request/response definitions.

## Acceptance Criteria

- Controllers validate request DTOs at boundaries.
- Public endpoints have explicit schemas, authentication requirements, and error codes.
- API responses use a consistent error model.
- Frontend service layer uses typed request and response contracts for high-traffic APIs.
