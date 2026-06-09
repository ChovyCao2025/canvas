# Program Coordination

Date: 2026-06-08

This directory coordinates the DDD modular rewrite and the Open Source Growth
program. It is intentionally separate from `docs/ddd-rewrite/`,
`docs/open-source-growth/`, and `docs/superpowers/` so neither program hides the
other program's constraints.

## Core Decision

The two programs can run together, but they must not be merged into one
implementation stream.

- DDD modular rewrite owns backend structure, bounded contexts, module
  boundaries, package rules, architecture tests, and cutover.
- Open Source Growth owns external productization: README, demo, plugin
  ecosystem, template packs, DSL/CLI, AI-assisted operations, playground, and
  community documentation.
- Coordination owns ordering, dependency gates, conflict resolution, and
  cross-program contract ownership.

## Folder Scope

This folder is the execution-control packet. It is sufficient for deciding:

- whether work can start from the current repository state
- which work must be serialized
- which work can be delegated to subagents
- how many workers can run in each wave
- which verification gates block the next wave

This folder is not a replacement for the DDD and Open Source Growth task packs.
Code-writing workers must receive the external task/spec files named by their
worker packet. A worker that reads only this folder and then changes business
code is operating outside the plan.

## Files

- [Program Progress Ledger](./progress-ledger.md) is the first reopen and
  handoff file; it records the current snapshot, worker board, verified
  evidence, and coordinator-recorded worker results.
- [Dispatch State](./dispatch-state.json) is the machine-readable companion to
  the ledger; the verifier rejects missing fields, overlapping active
  reservations, undeclared bridges, invalid parallel groups, and worker attempts
  to reserve coordinator state files.
- [Evidence](./evidence/README.md) defines the per-dispatch evidence directory
  convention used by worker returns, reviewer returns, verification output, and
  rollback notes.
- [DDD and Open Source Growth Integration](./ddd-open-source-growth-integration.md)
  defines the relationship, ownership model, and shared contract rules.
- [Combined Roadmap](./combined-roadmap.md) defines the practical sequence for
  executing both programs without stepping on the same code.
- [Conflict Matrix](./conflict-matrix.md) classifies each Open Source Growth
  workstream by its DDD dependency and execution risk.
- [Execution Sequencing](./execution-sequencing.md) gives agent and worker rules
  for parallel execution, stop conditions, and handoff format.
- [Collaboration And Recovery Protocol](./collaboration-and-recovery-protocol.md)
  defines active dispatch registry, worker state transitions, reviewer closure,
  wave closure, and reopened-session recovery.
- [Backup And Rollback Runbook](./backup-and-rollback-runbook.md) defines the
  required pre-rewrite backup manifest, external backup artifacts, per-worker
  rollback paths, per-wave checkpoints, and cutover rollback rules.
- [Max Parallel Subagent Execution Plan](./max-parallel-subagent-execution-plan.md)
  lists concrete worker tasks, wave limits, subagent-safe write scopes, and the
  highest safe concurrency by stage.
- [Subagent Worker Packets](./subagent-worker-packets.md) expands each delegated
  worker into a dispatch-ready packet with gates, scopes, commands, conflicts,
  and rollback.
- [Execution Readiness Audit](./execution-readiness-audit.md) states whether the
  current repository can start each class of work and defines readiness gates
  from documentation-only work through final cutover.
- [Gate Verification Matrix](./gate-verification-matrix.md) lists commands and
  evidence required at each readiness gate.
- [Isolated Worktree Protocol](./isolated-worktree-protocol.md) defines how to
  run concurrent code-writing workers safely.

Directory ownership:

- `docs/open-source-growth/**` stores Open Source Growth strategy, plans,
  guardrails, contracts, gates, and traceability.
- `docs/open-source/**` is the public-facing documentation output created by
  Open Source Growth tasks, such as quickstart, plugin development, template
  guides, MarketingOps as Code docs, playground docs, English docs, and release
  posts.

Checks:

```bash
bash docs/program-coordination/checks/program-coordination-checks.sh .
node tools/program-coordination/check-dispatch-state.mjs .
node --test tools/program-coordination/*.test.mjs
node tools/program-coordination/generate-worker-prompt.mjs DDD-E01 .
```

Prompt generation:

```bash
node tools/program-coordination/generate-worker-prompt.mjs <TASK_ID> .
```

The prompt generator allows read-only explorer prompts without an active
dispatch row. It rejects code-writing prompts until the coordinator has added a
matching active dispatch object to `dispatch-state.json`.

## Required Reading Order

Use this order before assigning any work that touches both programs:

1. `docs/program-coordination/README.md`
2. `docs/program-coordination/progress-ledger.md`
3. `docs/program-coordination/dispatch-state.json`
4. `docs/program-coordination/collaboration-and-recovery-protocol.md`
5. `docs/program-coordination/backup-and-rollback-runbook.md`
6. `docs/program-coordination/ddd-open-source-growth-integration.md`
7. `docs/program-coordination/conflict-matrix.md`
8. `docs/program-coordination/execution-readiness-audit.md`
9. `docs/program-coordination/gate-verification-matrix.md`
10. `docs/program-coordination/isolated-worktree-protocol.md`
11. `docs/program-coordination/max-parallel-subagent-execution-plan.md`
12. `docs/program-coordination/subagent-worker-packets.md`
13. `docs/program-coordination/execution-sequencing.md`
14. The owning program README and guardrails:
   - `docs/ddd-rewrite/README.md`
   - `docs/ddd-rewrite/guardrails/README.md`
   - `docs/open-source-growth/README.md`
   - `docs/open-source-growth/implementation-guardrails.md`

## Short Rule

Open Source Growth Month 1 documentation, community, and low-risk demo shell
work can run before or beside the DDD rewrite. Open Source Growth backend core
features that touch plugins, template import, Canvas DSL, AI journey generation,
or execution trace must wait for the DDD canvas/execution contracts and target
module ownership to be explicit.
