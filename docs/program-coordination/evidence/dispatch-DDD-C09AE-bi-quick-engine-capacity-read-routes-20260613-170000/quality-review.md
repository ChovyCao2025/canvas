# DDD-C09AE Quality Review

Date: 2026-06-13 17:31 +08:00

Reviewer: Pauli `019ec04d-4c03-77e2-a771-963beeeefa28`

## Verdict

FAIL

## Findings

1. Blocking: quick-engine capacity response did not preserve the legacy field
   contract. Legacy response exposed `alertEnabled`; the final DTO exposed
   `overLimit`.
2. Blocking: quick-engine queue snapshot response did not preserve legacy
   aggregate fields. Legacy response exposed `queued`, `claimed`, `completed`,
   `blocked`, and `total`; the final DTO exposed `running` and `failed`
   instead of `claimed` and `total`.
3. Blocking: queue job item JSON shape diverged from legacy. Legacy response
   exposed `tenantId`, `attemptCount`, `expiresAt`, and `blockedReason`; the
   final DTO omitted or renamed those fields.

## Recovery

Coordinator verified the findings against the legacy records in
`backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dataset/` and
patched the final-context DTOs, deterministic catalog, and compatibility tests
to preserve the legacy JSON field names.

Recovery verification:

- `mvn -pl canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test`
  passed after the fix: `BiCatalogApplicationServiceTest` 20/20,
  `BiApiCompatibilityTest` 10/10,
  `BiCatalogControllerCompatibilityTest` 16/16.

## Re-Review

Reviewer: Darwin `019ec05a-68c1-7573-863b-0cdd516d8915`

Verdict: PASS_WITH_CONCERNS

Darwin confirmed the recovered legacy field names match the old engine records
and found no old domain coupling. The only residual concern was missing explicit
test coverage for `blockedReason`.

## Final Recovery

Coordinator added explicit service/API compatibility coverage for
`blockedReason` and reran the focused Java 21 Maven command successfully:

- `BiCatalogApplicationServiceTest` 20/20
- `BiApiCompatibilityTest` 10/10
- `BiCatalogControllerCompatibilityTest` 16/16
