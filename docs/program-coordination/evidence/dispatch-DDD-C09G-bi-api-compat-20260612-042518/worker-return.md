# DDD-C09G Worker Return

status: DONE_WITH_CONCERNS
task id: DDD-C09G
dispatch id: dispatch-DDD-C09G-bi-api-compat-20260612-042518
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: 01aac65697d524f4cf2e92d954db088895631004 plus uncommitted reserved-path changes
files changed:
- backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
- docs/program-coordination/evidence/dispatch-DDD-C09G-bi-api-compat-20260612-042518/recovery-note.md
- docs/program-coordination/evidence/dispatch-DDD-C09G-bi-api-compat-20260612-042518/worker-return.md
- docs/program-coordination/dispatch-state.json
- docs/program-coordination/progress-ledger.md
- docs/program-coordination/subagent-worker-packets.md
contracts changed: none
tests run:
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=BiApiCompatibilityTest`
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CanvasApiCompatibilityTest,MarketingApiCompatibilityTest,ConversationApiCompatibilityTest,RiskApiCompatibilityTest,ExecutionApiCompatibilityTest,BiApiCompatibilityTest`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json`
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`
- `node tools/program-coordination/check-dispatch-state.mjs . && bash docs/program-coordination/checks/program-coordination-checks.sh .`
- scoped old-engine import scan for `BiApiCompatibilityTest.java`
- scoped trailing-whitespace scan for DDD-C09G files
- `git diff --check -- backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java docs/program-coordination/evidence/dispatch-DDD-C09G-bi-api-compat-20260612-042518 docs/program-coordination/dispatch-state.json docs/program-coordination/progress-ledger.md docs/program-coordination/subagent-worker-packets.md`
verification result:
- BI focused compatibility test passed with 4 tests, 0 failures.
- Combined Canvas/Marketing/Conversation/Risk/Execution/BI compatibility suite passed with 30 tests, 0 failures.
- Cutover compatibility preflight now reports `presentCount=6`, `missingCount=1`, with only `CdpApiCompatibilityTest` missing.
- `--require-ready` exited 1 as expected because production canvas-web controller/endpoint counts remain below old canvas-engine and CDP compatibility is still missing.
- DDD guardrails passed with the known `RiskRuleValidator` advisory only.
- Dispatch-state verifier and program coordination checks passed.
- Scoped old-engine import scan, trailing-whitespace scan, and `git diff --check` passed.
verification output summary/path:
- `backend/canvas-web/target/surefire-reports/org.chovy.canvas.web.compat.BiApiCompatibilityTest.txt`
- `backend/canvas-web/target/surefire-reports/org.chovy.canvas.web.compat.CanvasApiCompatibilityTest.txt`
- `backend/canvas-web/target/surefire-reports/org.chovy.canvas.web.compat.MarketingApiCompatibilityTest.txt`
- `backend/canvas-web/target/surefire-reports/org.chovy.canvas.web.compat.ConversationApiCompatibilityTest.txt`
- `backend/canvas-web/target/surefire-reports/org.chovy.canvas.web.compat.RiskApiCompatibilityTest.txt`
- `backend/canvas-web/target/surefire-reports/org.chovy.canvas.web.compat.ExecutionApiCompatibilityTest.txt`
evidence artifact paths:
- docs/program-coordination/evidence/dispatch-DDD-C09G-bi-api-compat-20260612-042518/recovery-note.md
- docs/program-coordination/evidence/dispatch-DDD-C09G-bi-api-compat-20260612-042518/worker-return.md
risks:
- The test uses a test-local controller adapter; production `canvas-web` still has not wired BI routes to final DDD facades.
- The seed intentionally covers BI catalog routes only. BI acceleration, SQL preview, datasource import, export/import file, dashboard runtime state, collaboration/transfer/favorite, portal/embed, subscription, AI, capacity, query, permission request, row, and column routes remain future cutover blockers.
- Worker Bacon timed out and was closed with no reserved-path changes; implementation was completed by coordinator inline fallback under the same reservation.
- Maven Surefire currently exits 0 for missing specified tests, so preflight presence and actual Surefire reports are required to prove this target exists.
coordinator actions needed:
- Run spec compliance review.
- Run code quality review if spec compliance passes.
- Close DDD-C09G only after reviewer outcomes and final verification are recorded.
ledger update:
- DDD-C09G added `BiApiCompatibilityTest` with 4 passing BI catalog compatibility tests; combined compatibility suite now passes 30/30 and DDD-C09A preflight sees 6/7 required compatibility targets.
rollback path:
- Remove `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java` and DDD-C09G evidence/state entries only.
