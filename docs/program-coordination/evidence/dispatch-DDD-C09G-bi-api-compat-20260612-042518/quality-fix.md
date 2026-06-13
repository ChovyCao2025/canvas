# DDD-C09G Quality Fix

Date: 2026-06-12

## Feedback Addressed

- Negative chart draft cases needed R-style JSON error envelope assertions, not
  only HTTP 400 status checks.
- Effective-access coverage needed to prove role mapping, not only user deny
  precedence over an `ALL` allow grant.

## RED

After adding the stronger negative-envelope assertions and role-only access
check, this command failed as expected:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=BiApiCompatibilityTest
```

Observed failure:

- `chartDraftRouteRejectsMissingOrArchivedDatasetBeforeSuccessfulMutation`
  failed because the missing-dataset route returned `400 BAD_REQUEST` with no
  `Content-Type` and no JSON body.

## GREEN

Implemented the minimal test-local adapter fix:

- Added `@ExceptionHandler(ResponseStatusException.class)` to the test-local
  BI controller adapter.
- Returned `CompatibilityEnvelope.fail("API_001", 400, reason)` with HTTP 400
  and JSON content type, mirroring the legacy global handler's R-style error
  body.
- Added a role-only permission grant/access assertion for `resourceId=301`,
  proving `roles=analyst` maps into `BiAccessRequest`.

Post-fix command:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=BiApiCompatibilityTest
```

Result: passed with 4 tests, 0 failures.
