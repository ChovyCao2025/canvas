# DDD-C09G Quality Review

Reviewer: Kuhn (`019eb87c-447a-75e3-b0d2-881ee02919b6`)
Initial status: WITH_FIXES

## Strengths

- Uses final DDD BI APIs/application service only: `BiCatalogFacade`,
  `BiCatalogApplicationService`, and final `org.chovy.canvas.bi.*` contracts.
- Adapter is test-local and only registers the requested BI catalog routes.
- Success-path assertions are meaningful for envelope shape, normalized keys,
  tenant/actor propagation, dataset/chart/dashboard/readiness data, permission
  grants, and user-deny precedence.
- Targeted verification passed locally: `mvn test -pl canvas-web -am
  -Dtest=BiApiCompatibilityTest` ran 4 tests with 0 failures.

## Issues

### Critical

None.

### Important

- Negative chart dataset cases only asserted HTTP 400. The test-local adapter
  mapped service validation failures through `ResponseStatusException`, but no
  R-style error wrapper was asserted. Add R-envelope assertions for negative
  routes, either through a test-local exception handler or explicit failure
  envelope response.

### Minor

- Effective-access request passed `roles=analyst`, but grants were `ALL` allow
  and `USER` deny, so the test would still pass if roles were ignored. Add a
  role-based grant check or capture `BiAccessRequest`.

## Coordinator Fix

- Added assertions that missing/archived chart dataset failures return JSON
  error envelope fields `code=400`, `errorCode=API_001`, and message
  `dataset is not available for BI chart`.
- Added a test-local `@ExceptionHandler(ResponseStatusException.class)` that
  mirrors the legacy global handler's R-style bad-request envelope.
- Added a role-only permission grant/access check for resource `301` so
  `roles=analyst` must map through `BiAccessRequest` and produce a `ROLE`
  allow decision.

## Post-Fix Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl
  canvas-web -am -Dtest=BiApiCompatibilityTest` exited 0 with 4 tests,
  0 failures.

## Required Fixes

Resolved by coordinator fix above. Re-review may start if required.
