# DDD-C09E Risk API Compatibility Reservation

Date: 2026-06-12
Coordinator: main agent
Task id: DDD-C09E
Dispatch id: dispatch-DDD-C09E-risk-api-compat-20260612-024746
Status at reservation: `RESERVED`
Branch: `main`
Base SHA: `01aac65697d524f4cf2e92d954db088895631004`
Worktree: `/Users/photonpay/project/canvas`

Status after spawn: `RUNNING`
Worker: Lovelace `019eb80a-25b5-70f2-9bd8-e878865c2f18`

Final status: `DONE_WITH_CONCERNS`
Spec reviewer: Mencius `019eb816-29de-7340-9e10-32d3b73d17e2`
Quality reviewer: Turing `019eb81c-85b2-7720-aeaa-93f03ecd93ef`

## Scope

Reserved worker write scope:

```text
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
```

Coordinator-owned exceptions: none.

The dispatch must not be marked `RUNNING` until a real `multi_agent_v1`
code-writing worker is spawned and the returned id/nickname is recorded in
`dispatch-state.json` and `progress-ledger.md`.

The coordinator generated the worker prompt with
`node tools/program-coordination/generate-worker-prompt.mjs DDD-C09E .`, then
spawned Lovelace `019eb80a-25b5-70f2-9bd8-e878865c2f18` before moving the
dispatch to `RUNNING`. Lovelace returned `DONE`, Mencius returned
`PASS_WITH_CONCERNS` for spec review, and Turing returned `PASS_WITH_CONCERNS`
for quality review. No required fixes remained.

## Target Choice

Tesla `019eb7fd-e1bc-73d1-ac3f-366c37b534f6` returned a read-only
recommendation to choose Risk next because `canvas-context-risk` has a compact
final API surface around `RiskDecisionFacade.evaluate`, baseline coverage in
`RiskDecisionApplicationServiceTest` / `RiskDecisionServiceTest`, and old HTTP
coverage in `RiskDecisionControllerTest`.

Accepted caveat: this C09E seed is intentionally scoped to
`/canvas/risk/decisions` only. Legacy risk scene, strategy, list, and lab routes
still need separate final DDD facade/bridge decisions before full DDD-C09
cutover.

## Pre-Dispatch Evidence

```text
node tools/program-coordination/check-dispatch-state.mjs .
result: passed (`{"ok": true}`)

bash docs/program-coordination/checks/program-coordination-checks.sh .
result: passed

git status --short -- backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
result: no output; target file had no pre-existing changes

bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
result: passed with known RiskRuleValidator TypeCompatibility advisory only

cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-risk -Dtest=RiskDecisionApplicationServiceTest
result: passed; 1 test, 0 failures

node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
result: exited 0 with presentCount 3, missingCount 4, cutoverReady false

node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
result: exited 1 as expected; missing ExecutionApiCompatibilityTest, CdpApiCompatibilityTest, BiApiCompatibilityTest, RiskApiCompatibilityTest
```

## Next Action

Generate the worker prompt after the RESERVED state row is present, spawn a real
`multi_agent_v1` worker, then move the dispatch to `RUNNING` with the actual
worker id/nickname. Completed. Final closeout verification passed the focused
Risk compatibility test, the combined Canvas/Marketing/Conversation/Risk
compatibility suite, default and require-ready preflight behavior, DDD
guardrails, coordination checks, scoped diff check, and old-engine import scan.

Next target selection is limited to the remaining missing DDD-C09 compatibility
targets: Execution, CDP, or BI.

## Rollback

Remove only:

```text
backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
docs/program-coordination/evidence/dispatch-DDD-C09E-risk-api-compat-20260612-024746/
```
