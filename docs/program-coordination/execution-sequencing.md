# Execution Sequencing

Date: 2026-06-08

## Purpose

This file gives the coordinator and agents concrete rules for executing the DDD
rewrite and Open Source Growth program in the same repository.

For the actual subagent wave schedule, task IDs, and maximum concurrency limits,
read `docs/program-coordination/max-parallel-subagent-execution-plan.md` first.
For current-state readiness gates, read
`docs/program-coordination/execution-readiness-audit.md` before dispatching
code-writing workers.
For active dispatch registry, worker state transitions, review closure, wave
closure, and breakpoint recovery, read
`docs/program-coordination/collaboration-and-recovery-protocol.md`.

## Coordinator Duties

The main coordinator owns:

- `docs/program-coordination/**`
- edits to DDD mirror files caused by Open Source Growth contract changes
- cross-program sequencing decisions
- shared contract conflicts
- final approval for tasks that touch both programs
- assignment of write scopes
- verification that docs, code, and tests point to the same target architecture

The coordinator must not dispatch two code-writing workers to the same file set.

## Worker Streams

| Stream | Primary scope | Can run in parallel with | Cannot run in parallel with |
| --- | --- | --- | --- |
| `osg-docs-worker` | README, `docs/open-source/**`, community files | DDD inventory, DDD module skeleton | Another worker editing README or root community files |
| `osg-demo-worker` | demo compose, demo profile, WireMock, demo docs | DDD inventory | DDD worker moving runtime config into `canvas-boot` |
| `ddd-inventory-worker` | read-only old code inventory and docs | most workers | code-writing workers only if they require stable inventory output first |
| `ddd-platform-worker` | `backend/canvas-platform/**` | OSG docs, DDD context workers with separate modules | workers editing `backend/pom.xml` or shared contracts |
| `ddd-context-worker` | one `backend/canvas-context-*` module | other context workers with separate modules | another worker editing the same context or shared API |
| `ddd-web-boot-worker` | `backend/canvas-web/**`, `backend/canvas-boot/**`, runtime assembly | read-only workers | OSG demo worker changing runtime profile or config |
| `osg-ecosystem-worker` | plugin/template/DSL/AI code | only after DDD target API is stable | DDD workers still defining the same API |

## Assignment Rule

Every task must declare:

```text
Program:
Task id:
Dispatch id:
Write scope:
Read scope:
Target backend state:
  CURRENT_ENGINE_BRIDGE or DDD_FINAL_MODULE or DOCS_ONLY
Contracts read:
Contracts changed:
Verification commands:
Rollback path:
Ledger update:
```

`CURRENT_ENGINE_BRIDGE` means the worker is intentionally writing a temporary
bridge under current `canvas-engine` paths. The worker must name the future DDD
module that will own the final version.

`DDD_FINAL_MODULE` means the worker is writing the final implementation under
the new module boundary. The worker must not depend on old `canvas-engine`
internals except through compatibility tests or named adapters.

`DOCS_ONLY` means the worker is changing documentation, examples, prompts, or
release material only. It must not modify backend, frontend, runtime config, or
build files.

## Ordering Algorithm

Use this algorithm before starting any Open Source Growth task:

1. If the task only changes docs, community files, screenshots, or issue/PR
   templates, run it in the Open Source Growth lane.
2. If the task changes demo compose, WireMock, or demo profile defaults, check
   whether DDD web/boot cutover work is active. If active, coordinate file
   ownership first.
3. If the task changes plugin, template, DSL, CLI backend write behavior, AI
   journey backend, execution dry-run, trace, or node metadata, check the DDD
   canvas/execution contract status first.
4. If the target DDD API is not explicit, convert the task into a contract
   documentation task or pause it.
5. If the target DDD API is explicit, implement against the target module or a
   named compatibility adapter.

## Review Gates

### Gate A: Program Fit

Before implementation, the reviewer checks:

- The task belongs to exactly one primary program.
- Any cross-program dependency is listed.
- The task does not expand DDD scope with product features.
- The task does not use DDD as a reason to delay low-risk Open Source Growth
  docs or demo shell work.

### Gate B: Module Fit

For backend code, the reviewer checks:

- Domain code has no Spring Web, MyBatis, Redis, RocketMQ, or WebClient
  dependency.
- Persistence code lives under the target context adapter package.
- Web request/response mapping lives in `canvas-web` or current bridge
  controller code.
- Cross-context calls use API/facade/port types.
- Open Source Growth extension code uses the handler, plugin, template, or DSL
  contract named in the task.

### Gate C: Product Contract Fit

For Open Source Growth work, the reviewer checks:

- Contract files in `docs/open-source-growth/contracts/` still match behavior.
- `docs/open-source-growth/traceability-matrix.md` maps the task to evidence.
- Demo changes do not weaken production profile safety.
- CLI or playground behavior uses supported HTTP APIs.
- AI generation produces drafts or review proposals, not direct published
  overwrites.
- Contract drafts that affect DDD have a coordinator-owned mirror task instead
  of direct DDD doc edits from the Open Source Growth worker.

### Gate D: Combined Verification

Before merging cross-program work, run the relevant subset:

```bash
bash docs/program-coordination/checks/program-coordination-checks.sh .
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
rg -n "CURRENT_ENGINE_BRIDGE|DDD_FINAL_MODULE" docs/program-coordination docs/ddd-rewrite docs/open-source-growth
rg -n "PluginRegistryService|HandlerRegistry|CanvasDsl|TemplatePack|demo profile" docs/program-coordination docs/open-source-growth docs/ddd-rewrite
```

For code tasks, also run the module or frontend tests named by the task.

### Gate E: Wave Closure

Before opening the next wave, the reviewer checks:

- the progress ledger has no `RUNNING`, `RETURNED`, or unreviewed `REVIEWING`
  rows for the current wave
- active dispatch registry rows are closed or deliberately carried forward
- reviewer board has no `FAIL` rows
- gate evidence in `gate-verification-matrix.md` is recorded
- every accepted concern has an owner and follow-up action

## Escalation Conditions

Escalate to the coordinator instead of continuing when:

- Two valid plans require different final owners for the same class or contract.
- The old `canvas-engine` bridge would become longer-lived than the next DDD
  cutover stage.
- A task needs to change `backend/pom.xml` while another module task is active.
- A contract change affects public API or CLI behavior.
- A demo shortcut would bypass permission, tenant isolation, publish state,
  execution trace, or audit behavior.
