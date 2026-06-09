# Program Progress Ledger

Date: 2026-06-09

## Purpose

This is the shared handoff record for the DDD modular rewrite and Open Source
Growth work. Reopen a session here first, then follow the links to the detailed
worker packets and gates.

Operational rules for active dispatch registry, review closure, wave closure,
and recovery live in
`docs/program-coordination/collaboration-and-recovery-protocol.md`.
Backup, checkpoint, and rollback rules live in
`docs/program-coordination/backup-and-rollback-runbook.md`.

`docs/program-coordination/dispatch-state.json` is the machine-readable
companion for this ledger. The coordinator updates both files together whenever
active dispatch state, recovery state, or verified evidence changes.

The ledger answers four questions:

- what phase the program is in
- which workers are ready, running, blocked, or complete
- what evidence was last verified
- what the next coordinator action is

## Write Rule

The coordinator is the single writer for this file.

Subagents must not edit this file or
`docs/program-coordination/dispatch-state.json` directly unless the coordinator
explicitly reserves the file for one worker. Subagents report status in their
required return format from `subagent-worker-packets.md`; the coordinator
records the result here and in the JSON state after reviewing the returned files
and verification output.

This prevents parallel workers from conflicting on one shared file.

## Reopen Checklist

When a new session starts:

1. Read this file from top to bottom.
2. Read `docs/program-coordination/collaboration-and-recovery-protocol.md`.
3. Read `docs/program-coordination/dispatch-state.json`.
4. Read `docs/program-coordination/backup-and-rollback-runbook.md`.
5. Run `node tools/program-coordination/check-dispatch-state.mjs .`.
6. Check `Current Snapshot`.
7. Check `Active Decisions`.
8. Check `Active Dispatch Registry`.
9. Check `Worker Board`.
10. Check `Reviewer Board`.
11. Check `Recovery Audit`.
12. If code-writing work will start, verify G0B and the pre-rewrite backup
    manifest before creating worktrees or prompts.
13. If work will be dispatched, read
   `docs/program-coordination/subagent-worker-packets.md`.
14. For read-only worker prompts, run
   `node tools/program-coordination/generate-worker-prompt.mjs <TASK_ID> .`.
   For code-writing worker prompts, first add an active dispatch row to
   `dispatch-state.json`; the generator must reject the prompt without it.
15. Run the verification commands listed in `Last Verified Evidence` before
   claiming the plan is still valid.

## Current Snapshot

| Field | Value |
| --- | --- |
| Overall state | Coordination and planning ready; implementation not started from this ledger |
| Current readiness | R0 documentation/coordination ready |
| Current backend target | no code worker active |
| Current write mode | coordinator-only for planning docs |
| Next coordinator action | capture pre-rewrite backup manifest, then choose first execution wave and capture baseline evidence |
| Highest safe parallelism now | read-only explorers plus docs-only or frontend-only OSG workers |

## Active Dispatch Registry

There are no active code-writing dispatches.

When dispatch starts, the coordinator records one row per active worker here and
the matching object in `dispatch-state.json`:

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

## Last Verified Evidence

The following commands were last used to verify the coordination package:

```bash
bash docs/program-coordination/checks/program-coordination-checks.sh .
node tools/program-coordination/check-dispatch-state.mjs .
node --test docs/program-coordination/checks/program-coordination-checks.test.mjs
node --test tools/program-coordination/*.test.mjs
node --test tools/open-source-growth/guardrail-verifier.test.mjs
node tools/open-source-growth/guardrail-verifier.mjs
bash -n docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
bash -n docs/ddd-rewrite/inventory/check-inventory-readiness.sh
```

Recorded result:

```text
program coordination checks passed
dispatch state verifier returned ok
program coordination tests passed; 6 tests
program coordination tool tests passed; 13 tests
open-source-growth guardrail verifier tests passed; 9 tests
open-source-growth guardrail verifier returned ok
DDD guardrail syntax passed
DDD guardrail runtime passed; no DDD rewrite module source directories found yet
inventory readiness script syntax passed; runtime check is G4-only after inventory files exist
```

Subagent review evidence:

| Review | Result | Notes |
| --- | --- | --- |
| Coordination recovery review | PASS | No issues found after active dispatch registry, canonical return fields, and G10 wording were aligned |
| DDD guardrail review | PASS | No blocker, important, or minor issue after inventory counts, G7 contract list, boot package, POM, and response formats were tightened |
| OSG integration review | PASS | No blocker, important, or minor issue after backend path authority, bridge declaration, traceability, and verifier checks were added |
| DDD parallel packet review | PASS | No blocker after section-aware worker/coordinator guardrails were added |
| OSG + DDD integration review | PASS | No blocker on cross-program worker sequencing or backend ownership |

## Active Decisions

| Decision | Current value | Source |
| --- | --- | --- |
| DDD and OSG run together | yes, but not as one implementation stream | `ddd-open-source-growth-integration.md` |
| Shared progress file writer | coordinator only | this file |
| Machine-readable dispatch state | `dispatch-state.json`, coordinator-owned | this file |
| Active dispatch registry | required before any code-writing worker | `collaboration-and-recovery-protocol.md` |
| Pre-rewrite backup manifest | required before any code-writing worker | `backup-and-rollback-runbook.md`; G0B |
| Worker dispatch source | `subagent-worker-packets.md` | `README.md` reading order |
| DDD worker ownership proof | generated inventory rows, not globs | `subagent-worker-packets.md` |
| Module POM edits | read-only unless coordinator names exact dependency | `subagent-worker-packets.md` |
| G7 contract freeze | compiled API contracts and contract tests required | `gate-verification-matrix.md` |
| G10 OSG backend gate | public extension/API stability gate | `gate-verification-matrix.md` |
| Old `canvas-engine` bridge work | complete Bridge Declaration required | `subagent-worker-packets.md` |
| Old `canvas-engine` backend work | bridge only when explicitly declared | `execution-sequencing.md` |

## Worker Board

Status values:

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

### Coordination

| Task | Status | Owner | Gate | Notes |
| --- | --- | --- | --- | --- |
| DDD-C00 foundation | READY | coordinator | G3 then G4 | Capture baseline before code execution |
| DDD-C07 contract freeze | NOT_STARTED | coordinator | G6 then G7 | Single writer for canvas/execution contracts |
| DDD-C09 cutover | NOT_STARTED | coordinator | G12 | Single writer for final web/boot cutover |
| OSG-C05B contract mirror | NOT_STARTED | coordinator | after relevant OSG contract draft | Mirrors OSG placement into DDD docs |
| OSG-C07 plugin registry decision | NOT_STARTED | coordinator | G9 then G10 | Decides platform/execution ownership |

### DDD Workers

| Task | Status | Gate | Write scope | Parallel rule |
| --- | --- | --- | --- | --- |
| DDD-E01 HTTP inventory explorer | READY | R0 | none | Can run with all read-only explorers |
| DDD-E02 persistence explorer | READY | R0 | none | Can run with all read-only explorers |
| DDD-E03 service explorer | READY | R0 | none | Can run with all read-only explorers |
| DDD-E04 test explorer | READY | R0 | none | Can run with all read-only explorers |
| DDD-W01 platform | NOT_STARTED | G4 | `backend/canvas-platform/**` | Can run with W02/W03 after inventory rows are assigned |
| DDD-W02 risk | NOT_STARTED | G4 | `backend/canvas-context-risk/**` | Can run with W01/W03 after inventory rows are assigned |
| DDD-W03 marketing | NOT_STARTED | G4 | `backend/canvas-context-marketing/**` | Can run with W01/W02 after inventory rows are assigned |
| DDD-W04 CDP | NOT_STARTED | G5 | `backend/canvas-context-cdp/**` | Can run with W05/W06 after first wave integration |
| DDD-W05 BI | NOT_STARTED | G5 | `backend/canvas-context-bi/**` | Can run with W04/W06 after first wave integration |
| DDD-W06 conversation | NOT_STARTED | G5 | `backend/canvas-context-conversation/**` | Can run with W04/W05 after first wave integration |
| DDD-W07 canvas | NOT_STARTED | G7 | `backend/canvas-context-canvas/**` | Do not run with W08 or OSG backend ecosystem workers |
| DDD-W08 execution | NOT_STARTED | G8 | `backend/canvas-context-execution/**` | Runs after W07 integration |

### Open Source Growth Workers

| Task | Status | Gate | Write scope | Parallel rule |
| --- | --- | --- | --- | --- |
| OSG-W01 entry docs | READY | G0/G1 | README and community docs | Can run with docs-only workers when files are reserved |
| OSG-W02 demo shell | READY | G0/G1 | demo compose, WireMock, demo docs | Bridge files require coordinator approval |
| OSG-W03 schema config frontend | READY | G0/G1 | config-panel and frontend plugin files | Frontend-only until backend APIs stabilize |
| OSG-W04 local CLI validate/diff | READY | G0/G1 | `tools/canvas-cli/**` | No backend write commands yet |
| OSG-W05A contract draft | READY | G0/G1 | one contract file per worker | Different contract files can run in parallel |
| OSG-W06 English docs | READY | G0/G1 | English docs and release posts | Docs-only |
| OSG-W07A official webhook plugin | NOT_STARTED | G9/G10 plus OSG-C07 | webhook plugin package and docs | Can run with W07B/W07C/W07D/W07E/W07F after extension points are stable |
| OSG-W07B official message plugin | NOT_STARTED | G9/G10 plus OSG-C07 | message plugin package and docs | Can run with W07A/W07C/W07D/W07E/W07F after extension points are stable |
| OSG-W07C official coupon plugin | NOT_STARTED | G9/G10 plus OSG-C07 | coupon plugin package and docs | Can run with W07A/W07B/W07D/W07E/W07F after extension points are stable |
| OSG-W07D official approval plugin | NOT_STARTED | G9/G10 plus OSG-C07 | approval plugin package and docs | Can run with W07A/W07B/W07C/W07E/W07F after extension points are stable |
| OSG-W07E official AI plugin | NOT_STARTED | G9/G10 plus OSG-C07 | AI plugin package and docs | Can run with W07A/W07B/W07C/W07D/W07F after extension points are stable |
| OSG-W07F official risk-check plugin | NOT_STARTED | G9/G10 plus OSG-C07 | risk-check plugin package and docs | Can run with W07A/W07B/W07C/W07D/W07E after extension points are stable |
| OSG-W08 template content/catalog | READY | G0/G1 for docs/catalog | template docs/data/catalog | Backend import waits for G10 |
| OSG-W09 template import backend | NOT_STARTED | G10 | canvas/execution assigned files | Do not overlap with W07/W08/W10 |
| OSG-W10 Canvas DSL backend | NOT_STARTED | G10 | canvas/web assigned files | Do not overlap with W07/W09/C09 |
| OSG-W11 CLI API commands | NOT_STARTED | G10 public extension/API stability gate | `tools/canvas-cli/**` | Serialize with W04 on CLI files |
| OSG-W12 AI journey backend | NOT_STARTED | G10 | canvas/marketing/execution assigned files | Do not overlap with W07/W08/W09/W10 |
| OSG-W13 frontend AI assistant | READY | R0 mock preview | AI assistant frontend files | Needs exact editor integration reservation |
| OSG-W14 playground flow | NOT_STARTED | R5 live flow or R0 docs-only | playground docs/frontend files | Do not overlap with C09/W02 |

## Reviewer Board

There are no active reviewer assignments.

When reviews start, the coordinator records one row per reviewer:

```text
review id:
worker task id:
review scope:
reviewer:
status:
files reviewed:
commands inspected or run:
findings:
required fixes:
ledger update:
```

## Recovery Audit

Latest recovery audit:

```text
date: 2026-06-09
result: no active dispatch registry rows; no code-writing worker is running from this ledger
commands to rerun before dispatch: node tools/program-coordination/check-dispatch-state.mjs ., G0, G0B before code-writing, G1, G2
```

## Event Log

| Date | Actor | Event | Evidence |
| --- | --- | --- | --- |
| 2026-06-09 | coordinator | Added backup and rollback runbook plus G0B backup gate for first code-writing dispatch and cutover | `backup-and-rollback-runbook.md`; `gate-verification-matrix.md` |
| 2026-06-09 | reviewers | Coordination, DDD, and OSG final re-reviews found no remaining issues in reviewed scopes | subagent final review reports |
| 2026-06-09 | coordinator | Added machine-readable dispatch state, evidence directory convention, dispatch-state verifier, and worker prompt generator | `dispatch-state.json`; `tools/program-coordination/**` |
| 2026-06-09 | coordinator | Tightened DDD inventory readiness, G7 contract freeze, OSG bridge authority, G10 gate naming, canonical return fields, and package guardrails | checks and guardrail scripts |
| 2026-06-09 | coordinator | Added collaboration and recovery protocol plus active dispatch, reviewer, and recovery ledger fields | `collaboration-and-recovery-protocol.md` |
| 2026-06-09 | coordinator | Wired shared progress ledger into README, subagent worker rules, and program coordination checks | `program-coordination-checks.sh`; `program-coordination-checks.test.mjs` |
| 2026-06-09 | coordinator | Added shared progress ledger and coordinator-only write rule | this file |
| 2026-06-08 | coordinator | Added dispatch-ready worker packets for DDD and OSG | `subagent-worker-packets.md` |
| 2026-06-08 | reviewer | DDD packet review passed after section-aware checks | final read-only review result |
| 2026-06-08 | reviewer | OSG/DDD integration review passed | final read-only review result |

## Worker Result Recording Template

When a worker returns, the coordinator records one row here:

```text
date:
task id:
status:
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

## Stop Conditions

Stop dispatch and update this ledger with `NEEDS_CONTEXT` when:

- a worker needs a file outside its assigned scope
- a worker cannot find required inventory rows
- a backend worker would target old `canvas-engine` without a declared bridge
- a worker needs to edit this ledger directly without coordinator reservation
- a gate command fails
- two workers need the same file or package
