# Collaboration And Recovery Protocol

Date: 2026-06-09

## Purpose

This protocol defines how the coordinator, workers, and reviewers collaborate
without losing state. It is the operational contract for shared progress,
worker handoff, review closure, wave closure, and session recovery.

Use this file together with:

- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/dispatch-state.json`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/max-parallel-subagent-execution-plan.md`
- `docs/program-coordination/gate-verification-matrix.md`
- `docs/program-coordination/isolated-worktree-protocol.md`
- `docs/program-coordination/backup-and-rollback-runbook.md`

## Roles

| Role | Owns | Must not do |
| --- | --- | --- |
| Coordinator | ledger writes, file reservations, wave start/stop, gate evidence, integration decisions | delegate coordinator-only tasks as code-writing work |
| Worker | assigned write scope, assigned tests, return packet | edit outside scope, edit the ledger, invent missing APIs or modules |
| Reviewer | read-only review against worker packet, specs, and gates | change files, approve work without naming evidence |

The coordinator is the only role that can move a worker to `DONE`,
`DONE_WITH_CONCERNS`, `INTEGRATED`, or `ABORTED` in the progress ledger.

## Single Source Of Truth

`docs/program-coordination/progress-ledger.md` is the human-readable shared
status file. `docs/program-coordination/dispatch-state.json` is its
machine-readable companion for active dispatches, reservations, recovery audit,
and verified evidence.

The coordinator owns both files. If the Markdown ledger and JSON state disagree,
do not treat the mismatch as an automatic `NEEDS_CONTEXT`. First classify the
state:

- `CONTINUE`: ledger, JSON state, evidence, and changed paths agree.
- `RECOVER`: one record is stale, but actual worktree evidence, worker return
  files, command output, or session logs make the correct state clear. The
  coordinator must make the smallest ledger/JSON recovery edit, run
  `node tools/program-coordination/check-dispatch-state.mjs .`, and continue.
- `NEEDS_CONTEXT`: the correct state cannot be proven, continuing could
  overwrite unowned changes, active reservations overlap, or the next step needs
  destructive rollback.

Only `NEEDS_CONTEXT` stops implementation. `RECOVER` is coordinator work, not a
reason to hand the problem back to the user.

The ledger must record:

- current readiness level
- active dispatch registry
- worker board status
- reviewer board status
- last verified evidence
- open decisions
- event log
- recovery audit notes

Subagents report proposed ledger updates in their return packet. The coordinator
reviews the files and command output before applying any ledger or JSON state
change.

## Active Dispatch Registry

Before dispatching a code-writing worker, the coordinator must reserve its write
scope in the ledger and in `dispatch-state.json`.

Registry record:

```text
dispatch id:
task id:
status:
worker:
mode:
branch:
worktree:
base SHA:
integration target:
exact reserved files:
coordinator-owned exceptions:
gate at dispatch:
last command/result:
evidence path:
next action:
rollback pointer:
```

Registry rules:

- A reservation can cover exact files or directories named in a worker packet.
- The JSON record is authoritative for machine checks; the Markdown row is the
  readable handoff summary.
- `RESERVED` means the coordinator has created the reservation but has not yet
  sent a worker handoff. `RUNNING` means the handoff was actually sent.
- Before moving a code-writing dispatch from `RESERVED` to `RUNNING`, the
  coordinator must check for available subagent tooling in the current runtime.
  If subagents are available, spawn the worker and record the actual worker
  nickname or id in the registry. Do not leave a running code-writing dispatch
  as `main-agent-inline`, `multi_agent_v1-worker`, `pending`, or another generic
  placeholder.
- Inline code-writing is allowed only when subagent tooling is unavailable or
  the coordinator is intentionally taking a local critical-path task. The
  registry `worker` field must include `fallback reason:` and the recovery audit
  must record why no real worker was spawned.
- A broad directory reservation is valid only when no other active reservation
  can write inside that directory.
- Coordinator-owned files cannot be reserved for workers unless the handoff
  explicitly says the coordinator is lending that file for one task.
- `docs/program-coordination/progress-ledger.md` cannot be reserved for a worker.
- `docs/program-coordination/dispatch-state.json` cannot be reserved for a
  worker.
- If two active reservations overlap, stop the newer worker before it edits.
- Run `node tools/program-coordination/check-dispatch-state.mjs .` after adding,
  changing, or closing any active dispatch row.
- A worker board row in `RESERVED`, `RUNNING`, `RETURNED`, `REVIEWING`,
  `NEEDS_CONTEXT`, or `BLOCKED` must have a matching active dispatch row. If the
  active registry is `none`, no worker board row may remain active.

## Worker State Machine

Allowed worker states:

```text
NOT_STARTED
READY
RESERVED
RUNNING
RETURNED
REVIEWING
INTEGRATED
DONE
DONE_WITH_CONCERNS
NEEDS_CONTEXT
BLOCKED
ABORTED
```

Allowed transitions:

| From | To | Who moves it | Required evidence |
| --- | --- | --- | --- |
| `NOT_STARTED` | `READY` | coordinator | readiness gate permits dispatch |
| `READY` | `RESERVED` | coordinator | active dispatch registry row exists |
| `RESERVED` | `RUNNING` | coordinator | worker handoff sent |
| `RUNNING` | `RETURNED` | worker reports, coordinator records | worker return packet received |
| `RETURNED` | `REVIEWING` | coordinator | reviewer scope assigned |
| `REVIEWING` | `DONE` | coordinator | reviewer finds no blocker or important issue; commands pass |
| `REVIEWING` | `DONE_WITH_CONCERNS` | coordinator | concerns are accepted with written risk and owner |
| `DONE` | `INTEGRATED` | coordinator | changed paths checked and integration commands pass |
| any active state | `NEEDS_CONTEXT` | coordinator or worker | missing field, missing file, unclear owner, or failed gate |
| any active state | `BLOCKED` | coordinator | repeated blocker needs external decision |
| any active state | `ABORTED` | coordinator | reservation is cancelled and rollback path recorded |

The coordinator must not repeatedly wait on one worker without doing recovery
work. After one meaningful `wait_agent` timeout, inspect the worker return,
session log, changed paths, and evidence directory. If the worker already
produced verifiable output, record `RETURNED` or close it. If it has not, either
continue a disjoint task, keep only read-only reviewers active, or mark the
dispatch `NEEDS_CONTEXT` with a specific missing fact.

`DONE_WITH_CONCERNS` is not enough to open a downstream gate unless the gate row
explicitly names the accepted concern and the owner of the follow-up.

## Worker Return Contract

Every worker return must include:

```text
status:
task id:
dispatch id:
branch:
worktree:
base commit:
head commit:
files changed:
contracts changed:
tests run:
verification result:
verification output summary/path:
evidence artifact paths:
risks:
coordinator actions needed:
ledger update:
rollback path:
```

If a worker cannot provide any required field, it must return `NEEDS_CONTEXT`.

## Reviewer Contract

Every reviewer must be read-only and must return:

```text
review status: PASS, PASS_WITH_CONCERNS, or FAIL
review scope:
files reviewed:
requirements checked:
commands inspected or run:
findings:
required fixes:
residual risks:
ledger update:
```

Review closure requires:

1. spec compliance review passes for the worker output
2. architecture or contract review passes when the worker touches backend,
   public API, plugins, DSL, templates, demo runtime, or AI draft behavior
3. coordinator verifies changed paths stay inside the reservation
4. gate commands from `gate-verification-matrix.md` pass or the ledger records
   why the current readiness level does not require them yet

## Breakpoint Recovery

When a session reopens, the coordinator must run this recovery sequence before
dispatching new writers:

1. Read `docs/program-coordination/progress-ledger.md`.
2. Read this protocol.
3. Read `docs/program-coordination/dispatch-state.json`.
4. Read `docs/program-coordination/backup-and-rollback-runbook.md`.
5. Run `node tools/program-coordination/check-dispatch-state.mjs .`.
6. Run `git status --short`.
7. Run `git worktree list` when isolated worker mode is in use.
8. If code-writing dispatch has started or will start, verify that
   `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` exists.
9. Compare active dispatch registry rows with actual branches, worktrees, and
   changed paths.
10. For each `RUNNING`, `RETURNED`, or `REVIEWING` worker, decide one action:
   continue, review, integrate, mark `NEEDS_CONTEXT`, or abort.
11. Re-run G0, G0B when code-writing is planned, G1, and G2 from
    `gate-verification-matrix.md`.
12. Record the recovery audit in the progress ledger and `dispatch-state.json`
    before assigning new work.

No reopened session may infer progress from memory alone.

## Wave Closure Checklist

A wave is closed only when all items below are true:

```text
worker board has no RUNNING or RETURNED rows for the wave
active dispatch registry rows for the wave are closed or deliberately carried forward
reviewer board has no FAIL rows for the wave
all required gate commands passed for the current readiness level
changed paths are inside assigned scopes
open concerns are owned and recorded
rollback paths exist for integrated workers
backup manifest and wave checkpoints exist when code-writing has started
event log has the wave closure entry
```

If any item is false, the wave remains open and the next wave cannot start.

## Stop Conditions

Stop dispatch immediately when:

- a worker asks for a file outside its reservation
- a worker edits the progress ledger
- a worker edits `dispatch-state.json`
- a worker cannot name its target backend state
- a worker cannot name its rollback path
- the backup manifest is missing before code-writing dispatch
- a reviewer returns `FAIL`
- a gate command fails
- an active dispatch registry row overlaps another active row
- `node tools/program-coordination/check-dispatch-state.mjs .` fails
- a reopened session cannot reconcile ledger state with worktree state

The coordinator then records `NEEDS_CONTEXT`, `BLOCKED`, or `ABORTED` in the
ledger with the evidence that caused the stop.
