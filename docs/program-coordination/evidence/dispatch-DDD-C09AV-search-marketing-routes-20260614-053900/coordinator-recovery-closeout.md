# DDD-C09AV Coordinator Recovery Closeout

Date: 2026-06-14
Dispatch: `dispatch-DDD-C09AV-search-marketing-routes-20260614-053900`
Task: `DDD-C09AV`
Worker: Ohm `019ec2ee-1c49-7290-987c-88cd59dbf8dc`

## Recovery Summary

Ohm was spawned only after exact Search Marketing scope reservation and
canonical prompt generation. After one wait timeout, the coordinator inspected
the reserved paths and evidence directory instead of continuing to wait.

Inspection showed no Search Marketing code/test files written and only
`reservation-note.md` in the evidence directory. The coordinator closed Ohm with
`previous_status: running` to avoid shared-workspace write conflicts, then
recovered the exact reserved scope locally using TDD.

## Implemented Scope

The closeout adds compact final-module compatibility for all 24 legacy
`/canvas/search-marketing` routes through:

- `SearchMarketingFacade`
- `SearchMarketingApplicationService`
- `SearchMarketingCatalog`
- `SearchMarketingController`

The implementation preserves the final web compatibility envelope, defaults
missing controller headers to tenant `7L` and actor `operator-1`, maps bad
requests to `API_001`, supports ISO date query params, defaults manual sync
`runType` to `PERFORMANCE`, and defaults due-run limits to `50`.

## Verification

RED command:

```text
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-marketing,canvas-web -am -Dtest=SearchMarketingApplicationServiceTest,SearchMarketingControllerCompatibilityTest,MarketingApiCompatibilityTest test
```

Result: failed as expected before production implementation because
`SearchMarketingFacade` and `SearchMarketingApplicationService` were missing.

GREEN command:

```text
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-marketing,canvas-web -am -Dtest=SearchMarketingApplicationServiceTest,SearchMarketingControllerCompatibilityTest,MarketingApiCompatibilityTest test
```

Result: passed, with `SearchMarketingApplicationServiceTest` 3/3,
`SearchMarketingControllerCompatibilityTest` 5/5, and
`MarketingApiCompatibilityTest` 7/7.

Cutover compatibility preflight:

```text
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Result: command passed. Current `canvas-web` advanced to 18 controllers and 271
endpoints. `/canvas/search-marketing` no longer appears in the top route gap
candidates. Global cutover remains blocked because old `canvas-engine` still has
142 controllers and 806 endpoints.

Strict old-coupling scan:

```text
rg -n "canvas-engine|org\.chovy\.canvas\.domain\.marketing|SearchMarketingService|SearchMarketingMutationService|SearchMarketingSyncRunService|SearchMarketingReadinessService|SearchMarketingReconciliationService|SearchMarketingImpactWindowService|TenantContextResolver" <final DDD-C09AV paths>
```

Result: clean; `rg` exited 1 with no matches.

Coordination checks before closeout state edit:

```text
node tools/program-coordination/check-dispatch-state.mjs .
bash docs/program-coordination/checks/program-coordination-checks.sh .
git diff --check -- <DDD-C09AV reserved files and coordination files>
```

Result: passed.

## Accepted Concerns

- No normal Ohm worker-return packet exists because the worker was closed after
  one timeout and evidence inspection.
- This is a compact deterministic in-memory route seed, not durable production
  search marketing provider, sync, mutation execution, reconciliation, or impact
  measurement parity.
- DDD final cutover remains blocked by global route parity.
