# DDD-C09AF Quality Review

Date: 2026-06-13
Reviewer: Hilbert `019ec088-7c73-74f3-b5c2-078182ad8c81`
Verdict: PASS_WITH_CONCERNS

## Findings

1. Medium: the modular no-row/default alert policy used `enabled=true`, while the legacy `BiQuickEngineCapacityService.view()` no-row default is `enabled=false`.
2. Low: notification list normalization preserved duplicate trimmed values, while legacy normalization uses ordered de-duplication.

## Coordinator Recovery

- Changed `BiQuickEngineCapacityCatalog.defaultAlertPolicy()` to expose `enabled=false` and empty `notificationChannels` / `notificationReceivers`.
- Changed notification normalization to trim, drop blanks, canonicalize channels, and preserve first-seen unique values.
- Made alert-policy and tenant-pool policy updates tolerate null command bodies by substituting empty command records.
- Adjusted service and web compatibility tests so service/domain tests assert normalization and controller tests assert raw request binding boundaries.

## Recovery Verification

- `cd backend && mvn -pl canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test`
  - Result: BUILD SUCCESS, 49 tests passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Result: current `canvas-web` 15 controllers / 61 endpoints; `/canvas/bi` 1 controller / 21 endpoints; `cutoverReady=false`.
- `node tools/program-coordination/check-dispatch-state.mjs .`
  - Result: passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  - Result: passed.
- `rg -n "org\\.chovy\\.canvas\\.domain\\.bi|canvas-engine" <DDD-C09AF exact BI source/test paths> -S`
  - Result: no matches, exit 1.
- `git diff --check -- <DDD-C09AF exact files and evidence>`
  - Result: passed.

