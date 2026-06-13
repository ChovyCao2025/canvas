# DDD-C09A Quality Review

review status: FAIL
reviewer: multi_agent_v1-explorer Dewey 019eb706-8695-7181-861c-b75b6a294d44
review id: review-DDD-C09A-quality-20260611-2109

## Review Scope

Read-only quality review for DDD-C09A,
`dispatch-DDD-C09A-cutover-preflight-tool-20260611-203729`.

## Files Reviewed

- `docs/program-coordination/evidence/dispatch-DDD-C09A-cutover-preflight-tool-20260611-203729/recovery-note.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09A-cutover-preflight-tool-20260611-203729/worker-return-recovery.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09A-cutover-preflight-tool-20260611-203729/spec-review.md`
- DDD-C09A packet section in `docs/program-coordination/subagent-worker-packets.md`
- `docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md`
- `docs/ddd-rewrite/contract-tests/compatibility-test-plan.md`
- `tools/program-coordination/cutover-compatibility-preflight.mjs`
- `tools/program-coordination/cutover-compatibility-preflight.test.mjs`

## Requirements Checked

- Deterministic and maintainable Node.js implementation.
- Missing or partial repository path handling.
- Default JSON and `--require-ready` exit behavior.
- Fixture coverage for ready and blocked modes.
- Write-scope attribution.
- Endpoint-count discrepancy risk classification.

## Commands Inspected Or Run

- `node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs`
  passed 3/3 before the quality fix.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  exited 0 with JSON and `cutoverReady: false`.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json`
  exited 1 with JSON.
- Scoped `git status --short` over the two reserved tool files.
- Missing-root and partial-fixture smoke checks.

## Findings

FAIL: partial repositories could be reported ready when the old baseline path
was absent. `listJavaFiles` normalized missing directories to an empty file list,
and `buildBlockers` only compared current counts against those zeroed old
counts. Dewey reproduced a false pass with one `canvas-web` controller, all
seven required compatibility test filenames, and no
`backend/canvas-engine/src/main/java/org/chovy/canvas/web` tree.

## Required Fixes

- Preserve source path existence metadata instead of silently normalizing
  missing inventories to zero-count success inputs.
- Add blockers for missing required baseline/current source paths.
- Add fixture coverage proving a partial repo without the old baseline path
  cannot report `cutoverReady: true`, and that `--require-ready --json` exits 1
  while preserving JSON.

## Residual Risks

- The real repo endpoint count is 806 while prior DDD-E01 inventory evidence
  reported 804. This remains non-blocking because the count is deterministic
  and currently only strengthens the blocked result.
- The shared worktree is broadly dirty, so scoped DDD-C09A attribution relies on
  the exact two reserved tool files and evidence directory.

## Ledger Update

DDD-C09A quality review FAIL; missing baseline path handling could false-pass
partial repositories and must be fixed before closure.
