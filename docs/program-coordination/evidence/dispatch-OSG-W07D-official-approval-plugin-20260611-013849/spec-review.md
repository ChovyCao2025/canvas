# OSG-W07D Spec Review

review status: PASS_WITH_CONCERNS
reviewer: multi_agent_v1-explorer Darwin 019eb2af-f65b-7610-8a6f-e47c9d3e43bd
review id: review-OSG-W07D-spec-20260611-0157
review scope: spec compliance for official approval plugin reserved output,
read-only

## Files Reviewed

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/approval/OfficialApprovalNodeHandler.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/approval/OfficialApprovalPluginTest.java`
- `docs/open-source/plugins/official/approval.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07D-official-approval-plugin-20260611-013849/recovery-note.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07D-official-approval-plugin-20260611-013849/worker-return.md`
- OSG-W07 shared packet, OSG-C07 registry decision, manifest/node-handler/DSL
  contracts, template catalog/template docs, and sibling official handlers.

## Requirements Checked

- Scope: W07D reserved paths match the worker packet. Targeted untracked output
  contains exactly the approval handler, approval test, and approval docs file.
  Repo-wide status is dirty from broader program work, but no W07D evidence
  indicates registry, platform, old-engine, or shared official plugin writes.
- Handler pattern: `@Component`, `implements NodeHandler`, and unique
  `@NodeHandlerType(OfficialApprovalNodeHandler.NODE_TYPE)` are present.
- Node/plugin alignment: plugin id is `canvas-plugin-approval`; node type is
  `approval.request`, matching template/catalog execution-facing usage.
- Behavior/docs/tests: `approvalCode` is required, trimmed, and tested;
  requester defaults to `userId` then `anonymous`; output envelope includes
  plugin id, node type, approval code, requester, payload, context, stub request
  marker, and `APPROVED`.
- Side effects: no real approval provider/client/task/instance calls, and no
  registry, persistence, permission, or enablement ownership was introduced.

## Commands Inspected Or Run

- Ran scoped `git diff --check` over approval paths: clean.
- Ran targeted `git ls-files --others --exclude-standard` over approval paths:
  exactly 3 W07D files.
- Ran targeted `rg` for approval-provider, HTTP, JDBC, registry, permission,
  and enablement references in approval source/test: no matches.
- Inspected Surefire report summaries: approval 5/0, webhook 4/0, message 7/0,
  coupon 5/0, enablement 1/0.
- Did not rerun Maven tests in read-only review mode.

## Findings

No blocking findings.

Concern, non-blocking: `canvas-dsl-v1.md` lists coarse DSL node names including
`approval`, while current execution-facing template/catalog convention uses
`approval.request` in `templateCatalog.ts`, `coupon-approval-release.md`, and
`ai-copy-review-publish.md`. This is a cross-contract naming concern, not a
W07D blocker, because W07D correctly follows the current execution-facing
convention and its scope does not include DSL contract edits.

## Required Fixes

None.

## Residual Risks

- Workspace has substantial unrelated dirty/untracked state, so scope was
  assessed from W07D evidence plus targeted path inspection rather than
  repo-wide cleanliness.
- The DSL coarse-name versus execution node-type distinction should be resolved
  by the owning DSL/template contract work.

## Ledger Update

OSG-W07D spec review PASS_WITH_CONCERNS. No required fixes; non-blocking DSL
`approval` versus execution-facing `approval.request` naming concern remains
outside W07D scope.
