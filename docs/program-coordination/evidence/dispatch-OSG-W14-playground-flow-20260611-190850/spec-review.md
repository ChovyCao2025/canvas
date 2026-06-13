# OSG-W14 Spec Review

review status: FAIL
reviewer: multi_agent_v1-explorer Popper 019eb66e-fc43-7e91-8b79-2b10bc4d1b44
review scope: read-only spec compliance review for OSG-W14 Playground Flow
dispatch id: dispatch-OSG-W14-playground-flow-20260611-190850

## Files Reviewed

- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/collaboration-and-recovery-protocol.md`
- `docs/program-coordination/evidence/dispatch-OSG-W14-playground-flow-20260611-190850/recovery-note.md`
- `docs/program-coordination/evidence/dispatch-OSG-W14-playground-flow-20260611-190850/worker-return.md`
- `docs/open-source-growth/contracts/demo-profile-contract.md`
- `docs/open-source-growth/contracts/template-pack-v1.md`
- `docs/open-source-growth/contracts/canvas-dsl-v1.md`
- `docs/open-source-growth/contracts/ai-operator-contract.md`
- `docs/open-source/playground.md`
- `frontend/src/pages/canvas-list/templateCatalog.ts`
- `frontend/src/pages/canvas-list/templateCloneFlow.test.ts`
- `frontend/src/pages/canvas-editor/AiJourneyAssistant.tsx`
- `frontend/src/pages/canvas-editor/aiJourneyAssistant.test.tsx`

## Requirements Checked

1. Reserved-file scope.
2. Playground docs coverage of the G10/G11-seed-aware flow without claiming
   end-to-end smoke.
3. Template catalog golden-path helper contents and safety boundary.
4. Tests for golden-path helper and AI assistant safety boundary.
5. AI assistant frontend-only/mock-provider/preview-only behavior.
6. No backend, production profile, secret, or bypass behavior introduced.

## Commands Inspected Or Run

Popper inspected coordinator-reported verification from the worker return and
brief, then ran read-only `sed`, `rg`, `git status --short`,
`git diff --name-only`, scoped `git diff`, `nl -ba`, and `test -f` checks for
referenced docs/CLI paths.

## Findings

FAIL: The DSL/CLI validation step is concrete but invalid. The docs and helper
used:

```bash
cd tools/canvas-cli && npm run canvas -- validate --file ./examples/new-user-welcome.canvas.yaml
```

The current CLI package has no `canvas` npm script, its usage is
`validate <file>`, and `tools/canvas-cli/examples/new-user-welcome.canvas.yaml`
is missing. The existing current fixture is
`tools/canvas-cli/test/fixtures/valid-journey.json`, whose metadata name is
`new-user-welcome`.

No other blockers were found. The docs correctly avoid claiming completed
runtime smoke, and the AI assistant is mock-provider/draft-preview-only with
disabled publish and no backend calls.

## Required Fixes

Update the playground docs, `getPlaygroundGoldenPath()` command, and test
expectation to use a valid current CLI validation path/syntax, or coordinate an
additional reserved CLI example file and command that actually exists. Keep the
fix within reservation rules or obtain a new reservation for CLI example
changes.

## Residual Risks

- Runtime smoke remains intentionally pending final live wiring.
- The repository has many unrelated dirty/untracked files from the broader
  program; this review only evaluated the OSG-W14 reserved files and required
  contracts.

## Ledger Update

Record OSG-W14 reviewer result as FAIL. Keep dispatch in review/fix state until
the CLI validation step is corrected and focused verification is rerun.
