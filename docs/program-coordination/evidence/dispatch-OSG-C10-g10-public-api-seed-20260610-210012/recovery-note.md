# OSG-C10 Recovery Note

Date: 2026-06-10
Coordinator: main session

## Dispatch

- Dispatch id: `dispatch-OSG-C10-g10-public-api-seed-20260610-210012`
- Task id: `OSG-C10`
- Status: `RUNNING`
- Mode: `coordinator`
- Integration target: `DDD_FINAL_MODULE`
- Branch: `main`
- Worktree: `/Users/photonpay/project/canvas`
- Base SHA: `01aac65697d524f4cf2e92d954db088895631004`

## Purpose

Create real G10 public extension/API stability evidence after preflight showed
that the required canvas and web named tests are absent. This coordinator task
does not run OSG backend ecosystem workers; it creates the minimal stable public
API/test surfaces required before those workers can be dispatched.

## Reserved Scope

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/dsl/**`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/**`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/template/**`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/**`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/template/**`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java`

## Preflight Evidence

- `git status --short -- <reserved paths>` returned no dirty paths.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `node tools/open-source-growth/guardrail-verifier.mjs` returned `{ "ok": true }`.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` passed with the known `RiskRuleValidator` TypeCompatibility advisory.
- Backup manifest exists at `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md`.

## TDD Plan

1. Add the missing G10 named tests first:
   - `TemplateImportServiceTest`
   - `CanvasDslValidatorTest`
   - `CanvasDslMapperTest`
   - `CanvasDslControllerCompatibilityTest`
2. Verify they fail because the public API surfaces are missing.
3. Implement the minimal stable API surfaces needed for those tests to pass.
4. Rerun the G10 commands and coordination/guardrail checks.

## Reopened Continuation

- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `git worktree list` shows the main worktree plus unrelated prunable worktrees.
- Active dispatch remains `dispatch-OSG-C10-g10-public-api-seed-20260610-210012`.
- Inline fallback reason: this is a coordinator-owned critical-path G10 public API seed; subagent tooling is available, but the current runtime only permits spawning on explicit user-authorized delegation, so the coordinator is continuing the already-registered inline task.

## Closure Evidence

- Coordinator return: `worker-return.md`
- Spec/contract review: `spec-review.md`
- Quality/guardrail review: `quality-review.md`
- Focused G10 Maven tests passed:
  - `canvas-context-canvas` named DSL/template tests: 6 tests.
  - `canvas-web` DSL controller compatibility test: 3 tests.
  - `canvas-context-execution` `*Plugin*Test`: 1 test.
- OSG guardrail verifier, program coordination checks, dispatch-state verifier,
  DDD guardrails, and scoped diff checks passed.
