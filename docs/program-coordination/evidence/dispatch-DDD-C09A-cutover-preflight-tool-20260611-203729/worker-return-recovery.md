# DDD-C09A Worker Return Recovery

status: DONE_WITH_CONCERNS
task id: DDD-C09A
dispatch id: dispatch-DDD-C09A-cutover-preflight-tool-20260611-203729
worker: multi_agent_v1-worker Kuhn 019eb6b4-f6a9-75f3-8c74-d7e92c8f668e
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
head commit: uncommitted shared worktree

## Recovery Context

`multi_agent_v1.wait_agent` timed out once after 180 seconds. The coordinator
audited the reserved files and evidence directory instead of repeating waits.
The two reserved tool files existed, were inside the exact write scope, and
passed fresh verification. The worker was closed while still reported as
running, so this is a coordinator recovery return rather than a normal worker
packet.

## Files Changed

- `tools/program-coordination/cutover-compatibility-preflight.mjs`
- `tools/program-coordination/cutover-compatibility-preflight.test.mjs`

## Contracts Changed

- Added a tooling-only preflight surface for DDD-C09 readiness:
  `node tools/program-coordination/cutover-compatibility-preflight.mjs [root] --json [--require-ready]`.
- Default JSON mode exits 0 even when cutover is blocked.
- `--require-ready --json` exits 1 when `cutoverReady` is false and prints the
  same JSON report.

## Tests Run

- `node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json`
- `git diff --check -- tools/program-coordination/cutover-compatibility-preflight.mjs tools/program-coordination/cutover-compatibility-preflight.test.mjs`

## Verification Result

Fresh coordinator verification passed. The focused Node test file passed 3/3
tests. Real-repo default JSON mode exited 0 and reported `cutoverReady: false`.
Real-repo `--require-ready --json` exited 1 with the same blocker report.

Current blocker report:

- old `canvas-engine` web controllers: 142
- old `canvas-engine` web endpoints counted by the tool: 806
- current `canvas-web` controllers: 1
- current `canvas-web` endpoints: 5
- required compatibility tests present: 0
- required compatibility tests missing: 7

## Risks

- The worker did not provide a canonical final packet because it was closed
  after a timeout audit.
- The tool's endpoint parser is a deterministic static approximation and should
  be treated as a preflight blocker signal, not a replacement for real
  `canvas-web` HTTP compatibility tests.
- The static endpoint count is 806 while the DDD-E01 explorer reported 804;
  reviewers should check whether this is acceptable as a conservative preflight
  count or needs parser adjustment before closure.

## Coordinator Actions Needed

- Run read-only spec review.
- If spec review passes, run read-only quality review.
- Record accepted concerns and close or return for fixes.

## Ledger Update

DDD-C09A returned through coordinator recovery as DONE_WITH_CONCERNS after
timeout audit; focused tests and real-repo preflight commands passed and showed
DDD-C09 remains blocked.

## Rollback Path

Remove the two assigned tool files and DDD-C09A evidence directory only.

