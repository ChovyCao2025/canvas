date: 2026-06-12
dispatch id: dispatch-DDD-C09M-risk-decision-controller-20260612-202200
task id: DDD-C09M
status: RUNNING
worker: Hume 019ebbbd-15b5-7720-a51a-137ae1a42c1b

reason:
- Active dispatch registry was empty after DDD-C09K/L closeout.
- Cutover preflight still reports global route parity blockers.
- `route:/canvas/risk` has oldControllerCount 5 and oldEndpointCount 23, with currentControllerCount 0 and currentEndpointCount 0.
- `RiskDecisionFacade.evaluate` exists in the final risk module and is already covered by adapter-only compatibility tests.

scope:
- Add a compact production RiskDecisionController seed for POST `/canvas/risk/decisions/evaluate`.
- This is not full risk route parity.
- Trace/list/lab/list-management risk endpoints remain out of scope.

exact reserved files:
- backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/RiskDecisionController.java
- backend/canvas-web/src/test/java/org/chovy/canvas/web/risk/RiskDecisionControllerCompatibilityTest.java

pre-dispatch verification:
- `node tools/program-coordination/check-dispatch-state.mjs .` passed with activeDispatches empty.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed with activeDispatches empty.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` exited 0 and showed global cutoverReady false plus route:/canvas/risk current 0/0.

next action:
- Wait once for Hume.
- If timeout occurs, inspect exact reserved files, evidence, and focused tests before deciding recovery.
