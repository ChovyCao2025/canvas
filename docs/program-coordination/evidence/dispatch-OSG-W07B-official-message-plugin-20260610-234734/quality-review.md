# OSG-W07B Quality Review

review status: PASS_WITH_CONCERNS
reviewer: multi_agent_v1-explorer Singer 019eb268-a275-7361-beb0-0a1561f247e2
review scope: code quality review for official message plugin reserved output, read-only
ready to close: yes, with minor follow-ups tracked

## Files Reviewed

- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/message/OfficialMessageNodeHandler.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/message/OfficialMessagePluginTest.java`
- `docs/open-source/plugins/official/message.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07B-official-message-plugin-20260610-234734/worker-return.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07B-official-message-plugin-20260610-234734/spec-review.md`
- `docs/program-coordination/evidence/dispatch-OSG-W07B-official-message-plugin-20260610-234734/spec-rereview.md`
- Supporting context: `NodeExecutionContext`, `DagNode`,
  `NodeExecutionResult`, `NodeHandlerRegistry`, existing official webhook
  handler/tests, and execution service path.

## Commands Inspected Or Run

- `git status --short -- <reserved paths>`
- `nl -ba ... | sed ...` over reviewed files
- `rg` inspections for handler registration, forbidden external-send/platform
  touches, recipient coverage, and whitespace
- `git diff --check -- <reserved paths>`; reviewer noted this is weak for
  untracked files
- Direct trailing-whitespace/tab scans with `rg`
- No Maven or Node tests rerun by reviewer; coordinator evidence was used.

## Strengths

- Uses the existing `NodeHandler` plus `@NodeHandlerType` pattern cleanly.
- Keeps scope limited: no real SMS/email/push/workchat sending, no new
  registry, persistence, permission enforcement, or enablement ownership.
- Envelope fields align with the requirement: plugin id, node type, channel,
  template, recipient, payload, context, delivery, and status.
- The previous literal-recipient blocker is fixed. Literal recipients now
  survive resolution fallback, while unresolved syntactic references fall back
  to user id or `anonymous`.
- Tests cover registry registration, happy-path envelope, default
  channel/recipient, literal recipient, unresolved reference fallback,
  anonymous fallback, and missing template failure.

## Issues

### Critical

None.

### Important

None.

### Minor

- Recipient coverage is still thinner than the implemented contract. Tests prove
  `${payload.phone}`, literal values, and unresolved `${payload.missing}` /
  `payload.missing`, but not successful `${context.*}`, successful raw
  `payload.*`, successful raw `context.*`, or bare-key resolution branches.
- Recipient precedence is implemented but not documented or tested: bare keys
  prefer context over payload, then nested context over nested payload. That may
  be acceptable, but duplicate keys could otherwise route differently than a
  caller expects.
- `docs/open-source/plugins/official/message.md` says "payload/context
  reference" but does not spell out every accepted form or unresolved-reference
  fallback.
- Evidence hygiene: the initial worker-return test counts are stale, though the
  rework section records the corrected 7/12 passing counts. Because the files
  are untracked, plain `git diff --check` does not prove whitespace cleanliness;
  reviewer direct whitespace scans found no trailing whitespace or tabs.

## Recommendations

- Add a future table-driven recipient test covering `${context.userPhone}`,
  `payload.phone`, `context.userPhone`, bare `phone`, and duplicate bare-key
  precedence.
- Expand the docs recipient paragraph with accepted forms and fallback behavior.
- When these files are staged or committed, rerun `git diff --check`
  meaningfully over the staged/committed patch.

## Assessment

The implementation is production-acceptable for the intended deterministic stub
seed. No blocking quality or scope issue was found. OSG-W07B can close with the
minor coverage/docs/evidence concerns tracked as follow-up.

## Ledger Update

OSG-W07B quality review PASS_WITH_CONCERNS. No critical or important issues.
Ready to close with minor follow-ups for recipient coverage, recipient
precedence documentation, and evidence cleanup.
