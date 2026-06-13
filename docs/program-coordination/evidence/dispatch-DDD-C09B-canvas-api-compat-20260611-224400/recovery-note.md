# DDD-C09B Canvas API Compatibility Test Seed Note

Date: 2026-06-11

## Dispatch

- dispatch id: dispatch-DDD-C09B-canvas-api-compat-20260611-224400
- task id: DDD-C09B
- mode: code-writing
- branch: main
- worktree: /Users/photonpay/project/canvas
- base SHA: 01aac65697d524f4cf2e92d954db088895631004
- integration target: DDD_FINAL_MODULE
- exact reserved files:
  - `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java`

## Gate Evidence

- G0 passed: dispatch-state verifier and program coordination checks passed
  after DDD-C09A closure.
- G0B passed: backup manifest exists; branch is `main`; HEAD is
  `01aac65697d524f4cf2e92d954db088895631004`; worktree list inspected.
- G2 passed: DDD guardrail shell syntax and guardrail checks passed with the
  known `RiskRuleValidator` advisory only.
- Scoped target status showed no existing
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java`
  changes before reservation.
- Existing canvas-web DSL compatibility test passed 9/9 before reservation:
  `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest`.

## Reason

DDD-C09A preflight reports that zero of seven required
`backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/*CompatibilityTest.java`
targets exist. DDD-C09B seeds the first real required compatibility target for
the already implemented Canvas DSL route surface without starting full DDD-C09
cutover or creating placeholder tests for unimplemented route groups.

## Rollback

Remove
`backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java`
and this evidence directory only.
