# DDD Rewrite Task Pack Index

Task packs are the direct handoff documents for subagents or engineers.

Each task pack defines:

- standard worker packet fields
- allowed write scope
- read scope
- migration goals
- package targets
- forbidden changes
- tests and review gates
- expected worker response

Use task packs only after reading:

```text
docs/ddd-rewrite/2026-06-08-ddd-modular-rewrite-spec.md
docs/ddd-rewrite/2026-06-08-ddd-modular-rewrite-plan.md
docs/ddd-rewrite/2026-06-08-ddd-rewrite-conventions-and-examples.md
docs/ddd-rewrite/references/rich-domain-model-reference.md
docs/ddd-rewrite/references/class-placement-reference.md
docs/ddd-rewrite/guardrails/README.md
```

---

## Packs

Recommended sequence:

```text
00-coordinator-foundation.md
01-worker-platform.md
02-worker-risk.md
03-worker-marketing.md
04-worker-cdp.md
05-worker-bi.md
06-worker-conversation.md
06a-coordinator-canvas-execution-contract-freeze.md
07-worker-canvas.md
08-worker-execution.md
09-coordinator-web-boot-cutover.md
```

Parallel execution:

- `01`, `02`, and `03` can run in parallel after foundation.
- `04`, `05`, and `06` can run in parallel after the first wave integrates.
- `06a` is coordinator-owned and freezes canvas/execution contracts before
  `07` or `08`.
- `07` and `08` require `06a`; code-writing should be sequential.
- `09` is coordinator-owned and sequential.

## Standard Worker Packet

Every task pack is a template. Before dispatch, the coordinator must add or
attach these fields to the worker prompt:

```text
Program:
Task id:
Readiness level:
Allowed write scope:
Forbidden write scope:
Read scope:
Target backend state:
Contracts to read:
Contracts changed:
Verification commands:
Rollback path:
Inventory rows:
Expected return format:
```

No worker may start from a task pack alone if the five inventory files listed in
`../inventory/README.md` do not exist or if its `Inventory rows` field is empty.
The task pack globs are read-scope hints, not ownership proof.

---

## Worker Status Values

Workers must return one of:

```text
DONE
DONE_WITH_CONCERNS
NEEDS_CONTEXT
BLOCKED
```

`DONE_WITH_CONCERNS` is required when code compiles but compatibility, behavior,
or architecture risks remain.

Workers must not return `DONE` if any failure mode in
`../guardrails/llm-drift-and-failure-modes.md` applies to their changes.
