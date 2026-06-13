# OSG-W07B Spec Review

review status: FAIL
reviewer: multi_agent_v1-explorer Halley 019eb250-f04f-72c1-86dd-3cfd81f98ba0
review scope: OSG-W07B official message plugin spec compliance, read-only

## Files Reviewed

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/message/OfficialMessageNodeHandler.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/message/OfficialMessagePluginTest.java`
- `docs/open-source/plugins/official/message.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07B-official-message-plugin-20260610-234734/worker-return.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/open-source-growth/contracts/plugin-manifest-v1.md`
- `docs/open-source-growth/contracts/node-handler-contract.md`
- `docs/open-source-growth/contracts/canvas-dsl-v1.md`
- `frontend/src/pages/canvas-list/templateCatalog.ts`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidator.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidatorTest.java`
- sampled `docs/open-source/templates/new-user-welcome.md`

## Requirements Checked

1. Scope: PASS. Worker-return lists only the message handler, message test, and message docs; this matches the OSG-W07B reserved row.
2. Handler pattern: PASS. `OfficialMessageNodeHandler` is a Spring `@Component`, implements `NodeHandler`, and declares `@NodeHandlerType`.
3. Manifest/docs/type consistency: PASS. Plugin id and node type are aligned across docs, handler, and tests.
4. `message.send` versus DSL v1 `message`: ACCEPTED CONCERN. OSG-W07B follows existing template/catalog and execution-facing conventions; changing shared DSL contracts is outside the reserved scope.
5. Tests cover registry registration, successful envelope behavior, required template validation, and trim/default behavior.

## Commands Inspected Or Run

- `git status --short`
- `git diff --name-only`
- `git ls-files -- ...`
- `git ls-files --others --exclude-standard -- ...`
- multiple `rg` and `nl -ba` inspections over scoped files and contracts
- No Maven or Node verification rerun by reviewer; coordinator reran focused commands separately.

## Findings

- BLOCKER: `docs/open-source/plugins/official/message.md` advertises `recipient` as an optional literal recipient or template reference, but the handler does not preserve a literal recipient. `recipient()` always sends the configured value through `resolve()` and falls back to `userId` or `anonymous` when no payload/context key resolves. Example: `recipient: "+15550001111"` would output `user-1`, not the literal. Tests cover `${payload.phone}` and default fallback but do not cover a literal.
- Concern, not blocker: `message.send` versus DSL v1 `message` is a real cross-contract inconsistency. OSG-W07B follows existing template/catalog and execution plugin conventions, and resolving the shared DSL naming is outside this worker scope.

## Required Fixes

- Fix recipient behavior or docs inside OSG-W07B scope. Preferred fix: support literal recipients after trim, while still resolving `${payload.*}` and `${context.*}` references, and add a test proving a literal recipient is emitted unchanged.
- Update docs/tests to document or cover the `anonymous` fallback if that fallback is intentional.

## Residual Risks

- The reviewed files are untracked in this workspace, so `git diff` cannot prove the worker's exact patch boundary. Worker-return declares the scoped file set, and reviewed paths match the OSG-W07B reservation.
- Channel enum validation is declared in docs but not enforced by the handler; acceptable only if schema validation is owned elsewhere.

## Ledger Update

OSG-W07B spec review FAIL. Do not advance past spec review until recipient
literal behavior is aligned with `docs/open-source/plugins/official/message.md`
and covered by tests.

## Coordinator Analysis

The blocker is valid. The root cause is that `recipient()` has no unresolved
literal branch after reference lookup. A scoped fix can preserve `${...}` and
payload/context references while returning trimmed plain strings as literals.
