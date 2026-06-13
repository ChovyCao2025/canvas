# Execution Readiness Audit

Date: 2026-06-08

## Verdict

The files in `docs/program-coordination/` are sufficient to guide the
transformation only as a staged execution program. They are not a one-command
rewrite plan and must not be executed by jumping directly into context workers.

Current repository evidence:

- Existing backend modules are `canvas-engine`, `canvas-cache-sdk`, and
  `canvas-flink-jobs`.
- Target DDD modules such as `canvas-context-risk`, `canvas-context-marketing`,
  `canvas-platform`, `canvas-web`, and `canvas-boot` do not exist yet.
- Therefore DDD worker commands such as `mvn test -pl canvas-context-risk` are
  expected to fail until `DDD-C00` creates the module skeletons and Maven wiring.

Execution is acceptable only if workers follow the readiness gates below.

## Readiness Levels

| Level | Meaning | Allowed work | Forbidden work | Exit evidence |
| --- | --- | --- | --- | --- |
| R0: Documentation Ready | Coordination, DDD, Open Source Growth, and backup/rollback docs exist and cross-link. | Read-only review, docs-only cleanup, Open Source Growth docs/community tasks. | DDD context code-writing workers. | Program coordination checks pass. |
| R1: Backup And Baseline Captured | Pre-rewrite backup manifest, dirty worktree, backend/frontend baseline, and inventory commands have been recorded. | Inventory explorers, OSG docs/demo shell, coordinator foundation. | Context rewrite workers. | Backup manifest exists under `docs/program-coordination/evidence/`; baseline evidence files exist under `docs/ddd-rewrite/evidence/`. |
| R2: Foundation Ready | DDD module skeletons, root Maven wiring, `canvas-common`, `canvas-web`, and `canvas-boot` shells exist. | First DDD context wave: platform, risk, marketing. | CDP/BI/conversation wave; canvas/execution implementation. | `mvn -q -DskipTests install`, `mvn test -pl canvas-boot -Dtest=ModularArchitectureTest`, and DDD guardrails pass. |
| R3: First Context Wave Integrated | Platform, risk, and marketing workers returned acceptable status and were integrated. | CDP, BI, conversation workers. | Canvas/execution implementation before contract freeze. | Context module tests and guardrails pass. |
| R4: Canvas/Execution Contract Frozen | Published canvas definition, execution publication port, node metadata, plugin enablement, template import, DSL, dry-run, trace, and AI draft boundaries are frozen. | Canvas worker, then execution worker after canvas integration. | Parallel canvas and execution code-writing workers. | Child spec and mirrored OSG contracts are updated and reviewed. |
| R5: Context Rewrite Integrated | All DDD contexts compile and pass architecture guardrails. | OSG backend ecosystem work after G10 public extension/API stability gate: plugin runtime, template import, DSL backend, CLI API commands, AI backend. | Final cutover. | Module tests, contract tests, mirrored demo profile placement, and OSG phase gates pass. |
| R6: Cutover Ready | DDD and OSG ecosystem work are integrated. | Web/boot cutover and final verification. | Old `canvas-engine` removal before full verification. | Full backend build, frontend build/tests, compatibility tests, demo profile smoke/golden path, production-safety scan, CLI tests. |

## Preflight Commands

Run these commands from the repository root before dispatching any code-writing
worker:

```bash
git status --short
bash docs/program-coordination/checks/program-coordination-checks.sh .
test -f docs/program-coordination/evidence/pre-rewrite-backup-manifest.md
bash -n docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
node --test tools/open-source-growth/guardrail-verifier.test.mjs
node tools/open-source-growth/guardrail-verifier.mjs
git worktree list
```

Interpretation:

- If `program-coordination-checks.sh` fails, do not dispatch workers.
- If the backup manifest is missing, do not dispatch code-writing workers or
  create isolated worker worktrees.
- If the DDD guardrail script prints `No DDD rewrite module source directories
  found; nothing to scan yet.`, the repo is still before R2. That is acceptable
  for R0/R1, but context code workers must not start.
- If Open Source Growth guardrails fail, only docs/guardrail repair work can
  proceed.

## Dispatch Decision

Use this decision table before every subagent dispatch:

| Condition | Dispatch allowed? | Action |
| --- | --- | --- |
| Target DDD module directory does not exist and task is a DDD context worker | No | Run `DDD-C00` first. |
| Worker needs a coordinator-owned file | No | Coordinator reserves or edits the file directly. |
| Code-writing work would start before the backup manifest exists | No | Run G0B and create `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md`. |
| Worker has isolated workspace and disjoint write scope | Yes | Dispatch according to the max parallel plan. |
| Worker shares the same workspace with other code-writing workers | Only if it is the only writer | Keep other agents read-only or reviewers. |
| Code-writing work is ready and subagent tooling is available | Yes | Spawn the worker before marking the dispatch `RUNNING`, and record the actual worker id/nickname. |
| Code-writing work is ready but subagent tooling is unavailable | Only with explicit fallback | Keep status `RESERVED` until the coordinator records `fallback reason:` for inline execution. |
| OSG backend work targets old `canvas-engine` without `CURRENT_ENGINE_BRIDGE` | No | Rewrite prompt or wait for DDD final module ownership. |
| Worker only changes docs/examples/prompts and declares `DOCS_ONLY` | Yes | Dispatch if file ownership is disjoint. |

## Required Worker Packet

Before a worker starts, the coordinator must provide:

```text
Program:
Task id:
Dispatch id:
Readiness level:
Allowed write scope:
Forbidden write scope:
Read scope:
Target backend state:
Contracts to read:
Verification commands:
Rollback path:
Expected return format:
```

Workers must return `NEEDS_CONTEXT` instead of guessing when any packet field is
missing.

## Cannot Promise

These documents cannot guarantee that a large rewrite will produce zero compile
or test failures on the first attempt. They can guarantee a safer execution
protocol:

- invalid task ordering is blocked by readiness gates
- shared-file conflicts are serialized
- bridge work must be named
- backup and rollback evidence is required before code-writing dispatch
- final implementation cannot silently bind to old `canvas-engine`
- reviewers must approve spec compliance before quality review
- full verification is required before cutover
