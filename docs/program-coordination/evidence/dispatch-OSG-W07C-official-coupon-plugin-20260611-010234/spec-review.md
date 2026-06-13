# OSG-W07C Spec Review

review status: PASS_WITH_CONCERNS
reviewer: multi_agent_v1-explorer Jason 019eb28c-dd01-7571-bfe5-1ca58b84b514
review id: review-OSG-W07C-spec-20260611-0119
review scope: spec compliance for official coupon plugin reserved output,
read-only

## Files Reviewed

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/coupon/OfficialCouponNodeHandler.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/coupon/OfficialCouponPluginTest.java`
- `docs/open-source/plugins/official/coupon.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07C-official-coupon-plugin-20260611-010234/recovery-note.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07C-official-coupon-plugin-20260611-010234/worker-return.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/evidence/dispatch-OSG-C07-plugin-registry-decision-20260610-142556/worker-return.md`
- `docs/open-source-growth/contracts/plugin-manifest-v1.md`
- `docs/open-source-growth/contracts/node-handler-contract.md`
- `docs/open-source-growth/contracts/canvas-dsl-v1.md`
- `frontend/src/pages/canvas-list/templateCatalog.ts`
- sampled `docs/open-source/templates/*.md`
- existing official webhook/message plugin files

## Requirements Checked

- Scope: PASS. Worker return claims only coupon handler, coupon tests, and
  coupon docs paths; OSG-W07C packet allows exactly those paths. No coupon
  implementation writes were found in registry, platform, or legacy handler
  files.
- Handler pattern: PASS. The handler is a Spring `@Component`, implements
  `NodeHandler`, and declares unique `@NodeHandlerType("coupon.grant")`.
- Node/plugin alignment: PASS_WITH_CONCERNS. Handler and coupon docs use
  `canvas-plugin-coupon` and `coupon.grant`, matching current catalog and
  templates.
- Behavior/docs/tests: PASS. `couponKey` is trimmed and required; recipient
  falls back to `userId`, then `anonymous`; output includes plugin id, node
  type, coupon key, recipient, payload, context, `grant: stub`, and
  `status: SENT`; no provider call or real grant side effect was added.
- Evidence: PASS. Worker RED/GREEN and coordinator verification evidence are
  internally consistent; Surefire reports show coupon, message, webhook, and
  plugin enablement tests passing.

## Commands Inspected Or Run

- Inspected with `nl`, `sed`, `rg`, `wc`, `find`, and scoped `git status`.
- Ran scoped `git diff --check` over coupon reserved paths: clean.
- Did not rerun Maven/Node verification in read-only review mode; inspected
  worker/coordinator evidence and Surefire report summaries.

## Findings

No blocking findings.

Concern: cross-contract node naming drift remains outside this worker.
`plugin-manifest-v1.md` still shows example node `COUPON_GRANT`,
`canvas-dsl-v1.md` lists coarse `coupon`, and frontend schema tests also use
`COUPON_GRANT`, while current execution-facing templates and catalog use
`coupon.grant`. This does not block OSG-W07C because the worker correctly
followed the current template/catalog convention and its reserved scope did not
include shared contracts or frontend registry examples.

## Required Fixes

None for OSG-W07C.

## Residual Risks

- Coordinator should schedule a separate contract/catalog cleanup to normalize
  `COUPON_GRANT`, `coupon`, and `coupon.grant` naming across manifest examples,
  DSL wording, and frontend schema fixtures.
- Repository remains broadly dirty from unrelated work, so scope attribution
  depends on worker evidence plus scoped path checks.

## Ledger Update

OSG-W07C spec review PASS_WITH_CONCERNS. No blocking findings; coupon
handler/docs/tests comply with reserved spec. Non-blocking cross-contract
naming drift remains between older manifest/DSL/frontend examples and current
`coupon.grant` execution convention.
