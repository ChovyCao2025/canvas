# DDD-C09AU Coordinator Recovery Closeout

Date: 2026-06-14
Dispatch: `dispatch-DDD-C09AU-growth-activities-routes-20260614-052400`
Task: `DDD-C09AU`
Worker: Harvey `019ec2df-9cdb-7023-a6ab-5a0827cac555`

## Recovery Summary

Harvey was spawned only after the exact Growth Activity route scope was reserved
and the canonical worker prompt was generated. After one wait timeout, the
coordinator inspected the reserved paths and evidence directory instead of
continuing to block on the worker.

Inspection showed that Harvey had written the focused RED tests only:

- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/GrowthActivityApplicationServiceTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/GrowthActivityControllerCompatibilityTest.java`

The evidence directory contained only `reservation-note.md`, and no production
files had been added. The coordinator closed Harvey to avoid shared-workspace
write conflicts, then implemented the exact reserved production scope locally.

## Implemented Scope

The closeout adds compact final-module compatibility for all 25 legacy
`/canvas/growth-activities` routes through:

- `GrowthActivityFacade`
- `GrowthActivityApplicationService`
- `GrowthActivityCatalog`
- `GrowthActivityController`

The implementation preserves the final web compatibility envelope, defaults
missing controller headers to tenant `7L` and actor `operator-1`, maps bad
requests to `API_001`, and supports deterministic in-memory activity, reward
pool, grant, referral, task, task progress, and activity state transitions.

## Verification

Focused JDK 21 Maven command:

```text
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-marketing,canvas-web -am -Dtest=GrowthActivityApplicationServiceTest,GrowthActivityControllerCompatibilityTest,MarketingApiCompatibilityTest test
```

Result: passed, with `GrowthActivityApplicationServiceTest` 3/3,
`GrowthActivityControllerCompatibilityTest` 5/5, and
`MarketingApiCompatibilityTest` 6/6.

Cutover compatibility preflight:

```text
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Result: command passed. Current `canvas-web` advanced to 17 controllers and 247
endpoints. `/canvas/growth-activities` no longer appears in the top route gap
candidates. Global cutover remains blocked because old `canvas-engine` still has
142 controllers and 806 endpoints.

Strict old-coupling scan:

```text
rg -n "canvas-engine|org\.chovy\.canvas\.domain\.marketing|GrowthActivityService|GrowthActivityReadinessService|GrowthActivityReportService|GrowthRewardPoolService|GrowthRewardGrantService|GrowthReferralService|GrowthTaskService|TenantContextResolver" <final DDD-C09AU paths>
```

Result: clean; `rg` exited 1 with no matches.

Coordination checks before closeout state edit:

```text
node tools/program-coordination/check-dispatch-state.mjs .
bash docs/program-coordination/checks/program-coordination-checks.sh .
git diff --check -- <DDD-C09AU reserved files and coordination files>
```

Result: passed.

## Accepted Concerns

- No normal Harvey worker-return packet exists because the worker was closed
  after one timeout and evidence inspection.
- This is a compact deterministic in-memory route seed, not durable production
  reward/referral/task/provider parity.
- DDD final cutover remains blocked by global route parity.
