# DDD-C09H Worker Return Recovery

status: DONE_WITH_CONCERNS
task id: DDD-C09H
dispatch id: dispatch-DDD-C09H-cdp-api-compat-20260612-052334
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004 plus uncommitted dispatch output

## Recovery Note

`multi_agent_v1.wait_agent` for Ampere
`019eb898-7ff3-7e00-981b-af63440725e6` returned `not_found` in the reopened
coordinator runtime before a canonical worker packet was available. The exact
reserved output file existed, the evidence directory contained the reservation
note, and fresh coordinator verification passed. This packet records the
recoverable worker output without claiming a direct Ampere final response.

## Files Changed

- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java`

## Contracts Changed

- Added the seventh required DDD-C09 canvas-web HTTP compatibility seed:
  `CdpApiCompatibilityTest`.
- The seed uses test-local controller adapters around final CDP facades and
  application services. It does not edit production `canvas-web` controllers.

## Verification

Commands run from `/Users/photonpay/project/canvas` unless noted:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CdpApiCompatibilityTest`
  - passed; `CdpApiCompatibilityTest` ran 4 tests, 0 failures, 0 errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CanvasApiCompatibilityTest,MarketingApiCompatibilityTest,ConversationApiCompatibilityTest,RiskApiCompatibilityTest,ExecutionApiCompatibilityTest,BiApiCompatibilityTest,CdpApiCompatibilityTest`
  - passed; seven compatibility classes ran 34 tests, 0 failures, 0 errors.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - exited 0; `presentCount=7`, `missingCount=0`, `cutoverReady=false`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json`
  - exited 1 as expected; remaining blockers are controller and endpoint count
    gaps, not missing compatibility test files.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`
  - passed with the known `RiskRuleValidator` advisory only.
- `rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dal|engine)" backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java`
  - no matches.
- `git diff --check -- backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java docs/program-coordination/evidence/dispatch-DDD-C09H-cdp-api-compat-20260612-052334/recovery-note.md`
  - passed.
- trailing-whitespace scan over the CDP test and recovery note
  - passed.

## Verification Result

PASS with accepted process and scope concerns.

## Evidence Artifact Paths

- `docs/program-coordination/evidence/dispatch-DDD-C09H-cdp-api-compat-20260612-052334/recovery-note.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09H-cdp-api-compat-20260612-052334/worker-return.md`

## Risks

- The worker handle was not recoverable in the reopened runtime, so the return
  packet is coordinator-recovered from reserved-path evidence and fresh tests.
- Coverage is intentionally adapter-only and can pass before production
  `canvas-web` CDP controller wiring exists.
- The seed is narrow by packet design. It does not cover computed profile/tag
  jobs, write-key lifecycle, broad warehouse materialization/readiness families,
  privacy tombstones, governance, quality, lineage, drift, SLO, or external
  OLAP/evidence route families.
- The broader worktree remains dirty and untracked from prior accepted DDD/OSG
  work; attribution relies on the exact reserved path and evidence.

## Coordinator Actions Needed

- Start read-only spec review for DDD-C09H.
- If spec review passes, start read-only quality review.
- Close the active dispatch only after reviewer closure and final coordination
  checks pass.

## Ledger Update

Move DDD-C09H from `RUNNING` to `REVIEWING` after this recovered worker packet
and fresh verification are recorded.

## Rollback Path

Remove:

- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java`
- `docs/program-coordination/evidence/dispatch-DDD-C09H-cdp-api-compat-20260612-052334/worker-return.md`
