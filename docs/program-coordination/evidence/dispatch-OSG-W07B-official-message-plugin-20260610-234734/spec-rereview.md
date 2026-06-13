# OSG-W07B Post-Fix Spec Re-Review

review status: PASS_WITH_CONCERNS
reviewer: multi_agent_v1-explorer Halley 019eb250-f04f-72c1-86dd-3cfd81f98ba0
review scope: post-fix spec compliance for official message plugin recipient behavior, read-only

## Files Reviewed

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/message/OfficialMessageNodeHandler.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/message/OfficialMessagePluginTest.java`
- `docs/open-source/plugins/official/message.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07B-official-message-plugin-20260610-234734/spec-review.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07B-official-message-plugin-20260610-234734/worker-return.md`
- `docs/program-coordination/subagent-worker-packets.md`
- sampled DSL/catalog evidence in `canvas-dsl-v1.md`,
  `CanvasDslValidator.java`, `CanvasDslValidatorTest.java`,
  `templateCatalog.ts`, and `new-user-welcome.md`

## Commands Inspected Or Run

- `nl -ba ... | sed ...` on scoped handler, test, docs, and evidence files.
- `rg -n ...` for registry/platform forbidden touches and `message.send` versus
  `message`.
- `git status --short -- ...` on scoped message/plugin/evidence paths.
- No Maven or Node tests rerun by reviewer; coordinator reran focused commands
  separately.

## Findings

- Original blocker is fixed. Literal configured recipients now return the
  trimmed literal when resolution fails and the value is not syntactic reference
  syntax. The new literal test covers `recipient: "+15550001111"` emitted
  unchanged.
- `${payload.phone}` still resolves in the envelope test. Handler logic also
  supports `${context.*}`, `payload.*`, and `context.*` through normalization
  and prefix handling.
- Unresolved syntactic references fall back instead of leaking the expression.
  `${payload.missing}` and `payload.missing` are covered, and fallback to
  `anonymous` is covered.
- Docs now align with literal, reference, and default recipient behavior.
- Scope remains consistent with OSG-W07B. Rework files are limited to the
  message handler, message tests, message docs, and coordinator-owned evidence.
- Existing `message.send` versus DSL v1 `message` concern remains non-blocking.
  Changing the shared DSL naming contract is outside OSG-W07B.

## Required Fixes

None for spec review.

## Residual Risks

- There is no dedicated success test for `${context.*}`, `payload.*`, or
  `context.*`; support is confirmed from handler logic, but coverage is not
  exhaustive.
- The workspace still shows broad untracked parent directories, so `git status`
  cannot independently prove exact per-worker authorship.
- Initial worker-return test counts remain stale in the first section, but the
  rework section records the updated 7/12 passing counts.

## Ledger Update

OSG-W07B post-fix spec re-review PASS_WITH_CONCERNS. Original recipient blocker
is resolved; the dispatch may move past spec review with the DSL naming concern
and minor coverage risk tracked as non-blocking.
