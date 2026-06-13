# OSG-W07A Spec Review

review status: FAIL
reviewer: multi_agent_v1-explorer Kepler 019eb20b-8e9e-7901-8d14-afc96e182427
review scope: OSG-W07A official webhook plugin spec compliance, read-only

## Files Reviewed

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/webhook/OfficialWebhookNodeHandler.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/webhook/OfficialWebhookPluginTest.java`
- `docs/open-source/plugins/official/webhook.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07A-official-webhook-plugin-20260610-223145/worker-return.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/open-source-growth/contracts/plugin-manifest-v1.md`
- `docs/open-source-growth/contracts/node-handler-contract.md`
- selected template and DSL files for webhook shape confirmation

## Requirements Checked

1. Worker output scope: PASS
2. Execution-owned `NodeHandler` pattern and no second registry: PASS
3. Manifest/node shape consistency: FAIL
4. Test coverage for implemented skeleton: PASS_WITH_CONCERNS
5. Blockers before spec review exit: FAIL

## Commands Inspected Or Run

- `git status --short`
- `rg -n "OfficialWebhook|webhook|NodeHandlerType|NodeHandler" ...`
- `rg --files ...`
- `nl -ba` on scoped source, tests, docs, contracts, templates, DSL validator/mapper
- `find` scoped webhook package/docs paths
- No tests run; read-only review only.

## Findings

- BLOCKER: `docs/open-source/plugins/official/webhook.md` documented node type `WEBHOOK_CALL` and manifest `nodes: ["WEBHOOK_CALL"]`, while the handler registers `@NodeHandlerType("webhook")` and tests assert registry metadata/type `webhook`.
- BLOCKER: `docs/open-source/plugins/official/webhook.md` said required config is `url` with optional `method`/`headers` and URL validation/dispatch envelope. The implementation instead requires `event` and emits `event`, `source`, `payload`, `context`, and `received`.
- Evidence favors `webhook`/`event` as the current repo convention: the DSL contract, templates, validator, and publish lifecycle tests use lowercase `webhook` with `event`.
- The contradiction is severe because the node-handler contract requires plugin node type to match manifest or registry declaration, and the plugin-manifest contract requires manifest nodes to be backed by a handler or schema.
- Scope appears clean for OSG-W07A. The worker return lists only reserved handler, test, and docs files.

## Required Fixes

- Align `docs/open-source/plugins/official/webhook.md` with the implemented and DSL-backed contract: manifest `nodes` should use `webhook`, and node config/envelope docs should describe `event`/`source` behavior; or change the handler/tests/templates/DSL to the documented `WEBHOOK_CALL`/`url` contract.
- Based on current repo evidence, fixing the plugin doc is the lower-risk required fix.

## Residual Risks

- Tests manually construct `NodeHandlerRegistry`; they do not start a Spring context to prove component auto-discovery, though `@Component` is present.
- Enablement/manifest persistence remains platform-owned and intentionally out of scope, so this review only verifies the plugin did not create a competing registry surface.

## Ledger Update

OSG-W07A spec review FAIL: implementation stays in reserved scope and follows execution-owned `NodeHandler` registration, but `docs/open-source/plugins/official/webhook.md` contradicts handler/tests/templates/DSL by documenting `WEBHOOK_CALL`/`url` instead of the repo-conventional `webhook`/`event`; fix required before advancing.

## Coordinator Resolution

Coordinator applied the lower-risk required fix by updating
`docs/open-source/plugins/official/webhook.md` to document `webhook`, required
`event`, optional `source`, and the implemented trigger envelope.
