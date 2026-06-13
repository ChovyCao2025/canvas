# DDD-C09C Worker Return

status: DONE_WITH_CONCERNS
task id: DDD-C09C
dispatch id: dispatch-DDD-C09C-marketing-api-compat-20260612-003650
worker: multi_agent_v1-worker Arendt 019eb792-63e1-7303-a0cf-1e8518f4b556
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004

## Files Changed

- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`

## Contracts Changed

Added a marketing campaign HTTP compatibility seed for create, list, link,
list-links, readiness, and unlink using DDD-final marketing facade records with
a test-local controller adapter and legacy success envelope.

## Tests Run

- RED/baseline:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=MarketingApiCompatibilityTest`
- RED/baseline:
  `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
- GREEN:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=MarketingApiCompatibilityTest`
- GREEN:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=MarketingApiCompatibilityTest,CanvasApiCompatibilityTest`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json`

## Verification Result

Targeted and combined Maven checks passed. Normal preflight passed and now
detects `MarketingApiCompatibilityTest` present. `--require-ready` exited 1 as
expected because cutover still has unrelated remaining blockers.

## Verification Output Summary

- Baseline Maven exited 0 despite missing test class.
- Baseline preflight showed `MarketingApiCompatibilityTest` missing,
  `presentCount: 1`, and `missingCount: 6`.
- Post-change targeted Maven ran 6 marketing tests with 0 failures.
- Post-change combined Maven ran 11 tests with 0 failures.
- Post-change preflight showed `presentCount: 2`, `missingCount: 5`, and
  marketing present.

## Evidence Artifact Paths

- `backend/canvas-web/target/surefire-reports/TEST-org.chovy.canvas.web.compat.MarketingApiCompatibilityTest.xml`
- `backend/canvas-web/target/surefire-reports/org.chovy.canvas.web.compat.MarketingApiCompatibilityTest.txt`
- `backend/canvas-web/target/surefire-reports/TEST-org.chovy.canvas.web.compat.CanvasApiCompatibilityTest.xml`

## Risks

- `--require-ready` still reports remaining expected blockers: low canvas-web
  controller/endpoint counts and missing `ExecutionApiCompatibilityTest`,
  `CdpApiCompatibilityTest`, `BiApiCompatibilityTest`,
  `RiskApiCompatibilityTest`, and `ConversationApiCompatibilityTest`.
- Workspace has unrelated dirty changes outside this task.

## Coordinator Actions Needed

- Review and accept or rework the new compatibility seed.
- Continue remaining DDD-C09 compatibility targets and controller cutover work.

## Ledger Update

DDD-C09C added `MarketingApiCompatibilityTest`; targeted and combined
compatibility tests pass; preflight recognizes marketing target present; cutover
is not ready due remaining unrelated targets and controller gaps.

## Rollback Path

Remove
`backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`
only.
