# DDD-C09I Cutover Route Gap Report Reservation Note

Date: 2026-06-12T12:30:02+08:00
Coordinator: main session
Dispatch id: dispatch-DDD-C09I-cutover-gap-report-20260612-123002
Task id: DDD-C09I
Branch: main
Worktree: /Users/photonpay/project/canvas
Base SHA: 01aac65697d524f4cf2e92d954db088895631004
Mode: code-writing
Integration target: TOOLING_ONLY

## Decision

Reserve an exact tooling-only follow-up after DDD-C09H closure:

- `tools/program-coordination/cutover-compatibility-preflight.mjs`
- `tools/program-coordination/cutover-compatibility-preflight.test.mjs`

The goal is to extend the cutover preflight report with a deterministic
`routeGapSummary` so future DDD-C09 production controller migration can reserve
an exact route/controller group from evidence instead of relying on aggregate
controller and endpoint counts.

## Fresh Pre-Dispatch Checks

- `node tools/program-coordination/check-dispatch-state.mjs .` exited 0 with
  `{ "ok": true }`.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  exited 0 with `{ "ok": true }` and `Program coordination checks passed.`
- `test -f docs/program-coordination/evidence/pre-rewrite-backup-manifest.md`
  passed.
- `git branch --show-current` returned `main`.
- `git rev-parse HEAD` returned
  `01aac65697d524f4cf2e92d954db088895631004`.
- `git worktree list` ran before reservation.
- `bash -n docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh`
  passed.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`
  exited 0 with the known `RiskRuleValidator` advisory only.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  reports `presentCount=7`, `missingCount=0`, and `cutoverReady=false` because
  `canvas-web` still has 1 controller / 5 endpoints while old
  `canvas-engine` web has 142 controllers / 806 endpoints.

## Scope

Allowed write scope:

- `tools/program-coordination/cutover-compatibility-preflight.mjs`
- `tools/program-coordination/cutover-compatibility-preflight.test.mjs`

Forbidden write scope includes coordinator docs/state, backend, frontend, and
all docs outside the coordinator-owned reservation update.

## Notes

- Active dispatch registry is empty before this reservation.
- The assigned tool files are already untracked from accepted DDD-C09A output.
  This is an attribution concern, not an ownership blocker: no active worker
  owns them now, and DDD-C09I intentionally builds on that accepted tooling.
- The worker must follow TDD: add the `routeGapSummary` fixture expectation,
  run the focused test and observe the expected RED failure, then implement the
  minimal parser/report change.
- The new active dispatch must remain `RESERVED` with `pending-spawn` until
  `multi_agent_v1.spawn_agent` returns a real worker id and nickname.
