date: 2026-06-12
dispatch id: dispatch-DDD-C09M-risk-decision-controller-20260612-202200
task id: DDD-C09M
status: DONE_WITH_CONCERNS

worker:
- Hume 019ebbbd-15b5-7720-a51a-137ae1a42c1b returned DONE.
- Kepler 019ebbc1-92b6-7862-9514-6e30031812b5 returned read-only review PASS.

files changed:
- backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/RiskDecisionController.java
- backend/canvas-web/src/test/java/org/chovy/canvas/web/risk/RiskDecisionControllerCompatibilityTest.java

closed result:
- Added production POST `/canvas/risk/decisions/evaluate` seed backed by final-module `RiskDecisionFacade.evaluate`.
- Preserved compatibility envelope shape and API_001 error envelope for controller-raised bad-request/conflict errors.
- Header `X-Tenant-Id` defaults to 7 and overrides/ignores body tenantId.
- Controller validates requestId, sceneKey, ISO eventTime, subject identifier, future eventTime > 24h, and deadline 1..50ms before facade invocation.
- `RiskDecisionReplayMismatchException` maps to HTTP 409.
- Trace route and broader risk route parity are intentionally out of scope.

verification:
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=RiskDecisionControllerCompatibilityTest` -> BUILD SUCCESS, 6 tests run.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=RiskApiCompatibilityTest,RiskDecisionControllerCompatibilityTest` -> BUILD SUCCESS, 13 tests run.
- `node tools/program-coordination/check-dispatch-state.mjs . && bash docs/program-coordination/checks/program-coordination-checks.sh .` -> both passed while DDD-C09M was active.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` -> exit 0, currentCanvasWeb controllers 4/endpoints 20, route:/canvas/risk current 1/1, global cutoverReady false.
- Kepler read-only review -> PASS, no findings.

accepted concerns:
- Broader `/canvas/risk` parity remains incomplete by design.
- New controller compatibility test does not directly cover missing requestId, missing/malformed eventTime, invalid deadline values, malformed JSON, or invalid X-Tenant-Id binding errors.
- Framework-level binding failures may not use the controller-local API_001 handler; this matches compact scope and nearby controller style, but remains a broader hardening gap.

next action:
- Choose the next exact production controller/endpoint migration scope from cutover preflight routeGapSummary.
