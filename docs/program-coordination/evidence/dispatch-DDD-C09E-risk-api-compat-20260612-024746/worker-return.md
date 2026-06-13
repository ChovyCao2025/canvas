# DDD-C09E Worker Return

status: DONE

task id: DDD-C09E

dispatch id: dispatch-DDD-C09E-risk-api-compat-20260612-024746

branch: main

worktree: /Users/photonpay/project/canvas

base commit: 01aac65697d524f4cf2e92d954db088895631004

head commit: 01aac65697d524f4cf2e92d954db088895631004

files changed:

- backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java

contracts changed: none

tests run:

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=RiskApiCompatibilityTest` - passed, 7 tests
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CanvasApiCompatibilityTest,MarketingApiCompatibilityTest,ConversationApiCompatibilityTest,RiskApiCompatibilityTest` - passed, 22 tests
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` - exit 0, RiskApiCompatibilityTest present
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json` - exit 1 as expected, remaining blockers are Execution/Cdp/Bi tests and broader controller cutover

verification result: passed for assigned scope; broader cutover remains not ready as expected

verification output summary/path:

- `backend/canvas-web/target/surefire-reports/org.chovy.canvas.web.compat.RiskApiCompatibilityTest.txt`
- `backend/canvas-web/target/surefire-reports/TEST-org.chovy.canvas.web.compat.RiskApiCompatibilityTest.xml`

evidence artifact paths:

- `backend/canvas-web/target/surefire-reports/org.chovy.canvas.web.compat.RiskApiCompatibilityTest.txt`
- `backend/canvas-web/target/surefire-reports/TEST-org.chovy.canvas.web.compat.RiskApiCompatibilityTest.xml`

risks:

- Overall cutover preflight still fails outside this task because Execution/Cdp/Bi compatibility tests and controller migration are not complete.
- Trace query coverage uses a test-local adapter because the final `RiskDecisionFacade` only exposes evaluate today.

coordinator actions needed:

- Review and record DDD-C09E result in the ledger/dispatch state.
- Continue remaining C09 compatibility targets or cutover work.

ledger update:

- DDD-C09E RETURNED/DONE candidate: added risk decision HTTP compatibility seed and verified assigned commands.

rollback path:

- Remove `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java` only.
