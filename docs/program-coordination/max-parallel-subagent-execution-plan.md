# Max Parallel Subagent Execution Plan

Date: 2026-06-08

## Purpose

This plan answers one operational question: which concrete DDD and Open Source
Growth tasks can be implemented by subagents, and how to maximize concurrency
without reducing scope.

The plan does not remove work. It only changes execution order and assignment
so independent work runs at the same time.

For dispatch-ready packets per worker, use
`docs/program-coordination/subagent-worker-packets.md`.
For active dispatch registry, worker state transitions, reviewer closure, wave
closure, and reopened-session recovery, use
`docs/program-coordination/collaboration-and-recovery-protocol.md`.

## Hard Constraint

There are two valid execution modes.

| Mode | Meaning | Max code-writing concurrency |
| --- | --- | --- |
| Isolated worker mode | Each worker edits in a forked workspace or dedicated git worktree and returns changed paths for coordinator integration. | Use the wave limits in this file. |
| Shared workspace mode | Multiple agents write in the same working tree. | One code-writing worker at a time. Read-only explorers and reviewers can still run in parallel. |

Use isolated worker mode for real parallel rewrite work. If isolated workspaces
are not available, do not run multiple code-writing workers at the same time.

Before any code-writing worker starts, pass G0B from
`docs/program-coordination/gate-verification-matrix.md` and create the
pre-rewrite backup manifest defined in
`docs/program-coordination/backup-and-rollback-runbook.md`. Maximum parallelism
does not override the backup gate.

## Coordinator-Owned Bottlenecks

The coordinator owns these files and must serialize edits to them:

- `backend/pom.xml`
- cross-module dependencies in module `pom.xml` files
- `backend/canvas-engine/src/main/resources/application*.yml`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/HandlerRegistry.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandlerType.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/JdbcPluginRepository.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PluginRegistryController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/DagEngine.java`
- `backend/canvas-engine/src/main/resources/db/migration/**`
- `frontend/src/App.tsx`
- shared frontend API and type entry files
- `frontend/package.json`
- frontend lockfiles
- `docker-compose.local.yml`
- deploy manifests
- `docs/INDEX.md`
- `docs/program-coordination/**`

Workers may edit their own module `pom.xml` after `DDD-C00` pre-seeds baseline
dependencies. Root `backend/pom.xml` and cross-module dependency changes remain
coordinator-owned.

Workers receive directories and exact file lists. They do not receive broad
"feature across the stack" ownership unless the coordinator first reserves all
shared files in that feature.

## Subagent Task Inventory

### DDD Tasks

| ID | Can be subagent implemented? | Scope | Write scope | Dependencies | Parallel group |
| --- | --- | --- | --- | --- | --- |
| DDD-C00 | No, coordinator-owned | Foundation, module skeleton, root Maven wiring, guardrails, inventory integration | `backend/pom.xml`, `backend/canvas-common/**`, `backend/canvas-web/**`, `backend/canvas-boot/**`, `backend/canvas-platform/**`, `backend/canvas-context-*/**`, `docs/ddd-rewrite/**` | none | Sequential bottleneck |
| DDD-E01 | Yes, read-only | HTTP API inventory explorer | no writes; return inventory in final report | DDD-C00 may consume output | Explorer wave |
| DDD-E02 | Yes, read-only | Persistence ownership explorer | no writes; return inventory in final report | DDD-C00 may consume output | Explorer wave |
| DDD-E03 | Yes, read-only | Service ownership explorer | no writes; return inventory in final report | DDD-C00 may consume output | Explorer wave |
| DDD-E04 | Yes, read-only | Test ownership explorer | no writes; return inventory in final report | DDD-C00 may consume output | Explorer wave |
| DDD-W01 | Yes | Platform governance rewrite | `backend/canvas-platform/**` | DDD-C00 complete | DDD wave 1 |
| DDD-W02 | Yes | Risk rewrite | `backend/canvas-context-risk/**` | DDD-C00 complete | DDD wave 1 |
| DDD-W03 | Yes | Marketing rewrite | `backend/canvas-context-marketing/**` | DDD-C00 complete, marketing pilot spec read | DDD wave 1 |
| DDD-W04 | Yes | CDP rewrite | `backend/canvas-context-cdp/**` | DDD wave 1 integrated | DDD wave 2 |
| DDD-W05 | Yes | BI rewrite | `backend/canvas-context-bi/**` | DDD wave 1 integrated | DDD wave 2 |
| DDD-W06 | Yes | Conversation rewrite | `backend/canvas-context-conversation/**` | DDD wave 1 integrated | DDD wave 2 |
| DDD-C07 | No, coordinator-owned | Canvas/execution contract freeze | child spec, shared API decisions | DDD wave 2 integrated | Sequential bottleneck |
| DDD-W07 | Yes, but not parallel with DDD-W08 implementation | Canvas authoring rewrite | `backend/canvas-context-canvas/**` | DDD-C07 complete | DDD wave 3A |
| DDD-W08 | Yes, but after DDD-W07 integration | Execution runtime rewrite | `backend/canvas-context-execution/**` | DDD-C07 complete, risk/CDP APIs available, canvas API integrated | DDD wave 3B |
| DDD-C09 | No, coordinator-owned | Web/boot cutover and old engine removal | `backend/canvas-web/**`, `backend/canvas-boot/**`, `backend/pom.xml`, docs | all context workers integrated | Sequential bottleneck |

### Open Source Growth Tasks

| ID | Can be subagent implemented? | Scope | Write scope | Dependencies | Parallel group |
| --- | --- | --- | --- | --- | --- |
| OSG-W01 | Yes | Open-source entry docs and community files | `README.md`, `.github/ISSUE_TEMPLATE/**`, `.github/pull_request_template.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `SECURITY.md`, `docs/open-source/quickstart.md`, `docs/open-source/positioning.md` | human license decision before final `LICENSE` | OSG immediate |
| OSG-W02 | Yes, with coordinator file reservation | Demo shell and mock catalog | `docker-compose.demo.yml`, `wiremock/**`, demo docs, limited demo profile files assigned by coordinator | must not weaken production profile or bypass execution | OSG immediate |
| OSG-W03 | Yes | Frontend schema-driven config foundation | `frontend/src/components/config-panel/**`, `frontend/src/plugins/**` | stable or mock manifest shape | OSG immediate |
| OSG-W04 | Yes | Local CLI validate and local diff skeleton | `tools/canvas-cli/**`, `docs/open-source/marketingops-as-code.md` | no backend write commands until G10 public extension/API stability gate | OSG immediate |
| OSG-W05A | Yes | Open Source Growth contract drafts | one assigned file under `docs/open-source-growth/contracts/**` per worker | must label backend placement as bridge or final module; no DDD mirror edits | OSG immediate |
| OSG-C05B | No, coordinator-owned | Mirror affected OSG contracts into DDD docs | exact DDD child spec or task-pack files assigned by coordinator | DDD-C00 complete; relevant OSG contract draft reviewed | Sequential mirror |
| OSG-W06 | Yes | English docs and release drafts | `docs/open-source/en/**`, `docs/open-source/release-posts/**` | commands must be reverified before release | OSG immediate |
| OSG-C07 | No, coordinator-owned | Plugin registry extension point decision | `PluginRegistryService`, `JdbcPluginRepository`, `HandlerRegistry`, DDD platform/execution API decisions | DDD canvas/execution and platform ownership explicit | Sequential bottleneck |
| OSG-W07A | Yes after OSG-C07 | Webhook official plugin | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/webhook/**`, `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/webhook/**`, `docs/open-source/plugins/official/webhook.md` | plugin extension points compile | OSG plugin burst |
| OSG-W07B | Yes after OSG-C07 | Message official plugin | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/message/**`, `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/message/**`, `docs/open-source/plugins/official/message.md` | plugin extension points compile | OSG plugin burst |
| OSG-W07C | Yes after OSG-C07 | Coupon official plugin | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/coupon/**`, `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/coupon/**`, `docs/open-source/plugins/official/coupon.md` | plugin extension points compile | OSG plugin burst |
| OSG-W07D | Yes after OSG-C07 | Approval official plugin | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/approval/**`, `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/approval/**`, `docs/open-source/plugins/official/approval.md` | plugin extension points compile | OSG plugin burst |
| OSG-W07E | Yes after OSG-C07 | AI LLM official plugin | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/ai/**`, `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/ai/**`, `docs/open-source/plugins/official/ai.md` | plugin extension points compile | OSG plugin burst |
| OSG-W07F | Yes after OSG-C07 | Risk check official plugin | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/risk/**`, `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/risk/**`, `docs/open-source/plugins/official/risk-check.md` | plugin extension points compile | OSG plugin burst |
| OSG-W08 | Yes | Template content/docs/catalog | `docs/open-source/templates/**`, assigned template data files, `frontend/src/pages/canvas-list/templateCatalog.ts` if reserved | template import backend may still wait | OSG template burst |
| OSG-W09 | Yes after G10 and canvas API owner is explicit | Template import backend | target canvas module or named `CURRENT_ENGINE_BRIDGE` files | canvas draft/version API, plugin dependency API | OSG ecosystem |
| OSG-W10 | Yes after G10 and canvas/web API owner is explicit | Canvas DSL backend | target canvas/web module or named `CURRENT_ENGINE_BRIDGE` files | G10 canvas graph/import/export API | OSG ecosystem |
| OSG-W11 | Yes after G10 | CLI import/export/publish | `tools/canvas-cli/**` | public canvas/execution/web APIs pass G10 | OSG ecosystem |
| OSG-W12 | Yes after G10 public extension/API stability gate | AI journey backend | target marketing/canvas/execution APIs or named bridge files | draft API, risk audit rules, trace API | OSG ecosystem |
| OSG-W13 | Yes | Frontend AI assistant | `frontend/src/pages/canvas-editor/AiJourneyAssistant.tsx`, `frontend/src/pages/canvas-editor/aiJourneyAssistant.test.tsx`, optional exact editor integration file named by coordinator | AI preview contract | OSG ecosystem |
| OSG-W14 | Yes after demo path stable | Playground docs/front-end flow | `docs/open-source/playground.md`, assigned home/canvas-list/canvas-editor files | demo, templates, dry-run, trace, DSL export, AI audit | Final OSG wave |

## Maximum Parallel Waves

The numbers below assume isolated worker mode. In shared workspace mode, replace
every "writers" value with one writer and keep only read-only explorers parallel.

The same wave structure is mirrored in
`docs/program-coordination/dispatch-state.json` under `parallelGroups`. The JSON
state is the machine-checkable source for prompt generation and active dispatch
validation; keep it synchronized when changing wave limits or worker IDs.

| Wave | Start condition | Parallel subagent writers | Parallel read-only/review agents | Max total useful subagents | Notes |
| --- | --- | ---: | ---: | ---: | --- |
| P0 discovery | Current repo available | 0 | 8 | 8 | Run DDD explorers, OSG explorers, repo conflict scanner, and test baseline readers. |
| P1 immediate shell | Coordination docs accepted | 6 | 4 | 10 | Coordinator runs DDD-C00 while OSG-W01 through OSG-W06 run in disjoint scopes. |
| P2 DDD first context wave | DDD-C00 complete | 6 | 3 | 9 | DDD-W01/W02/W03 plus unfinished OSG immediate workers. If OSG immediate is done, max writers is 3. |
| P3 DDD second context wave | DDD wave 1 integrated | 5 | 3 | 8 | DDD-W04/W05/W06 plus OSG-W08 docs/catalog and OSG-W04 CLI local work if not done. |
| P4 contract freeze | DDD wave 2 integrated | 1 | 4 | 5 | Coordinator freezes canvas/execution and plugin extension contracts; use explorers/reviewers only. |
| P5 canvas rewrite | contracts frozen | 3 | 3 | 6 | DDD-W07 plus OSG frontend/docs-only workers. Do not run DDD-W08 implementation yet. |
| P6 execution rewrite | DDD-W07 integrated | 3 | 3 | 6 | DDD-W08 plus OSG frontend/docs-only workers. Do not run backend ecosystem workers against the same execution files. |
| P7 plugin burst | DDD-W08 integrated and OSG-C07 done | 6 | 2 | 8 | Six official plugin workers can run in parallel if each owns a separate plugin package and docs file. |
| P8 ecosystem backend | G10 public extension/API stability gate passed | 4 | 3 | 7 | Template import, DSL backend, CLI API commands, AI backend can run if exact files are disjoint. |
| P9 final playground/release | ecosystem APIs integrated | 3 | 3 | 6 | Playground, English/release docs, frontend polish. |
| P10 cutover | all context and OSG ecosystem work integrated | 1 | 4 | 5 | Coordinator performs web/boot cutover, full verification, and old engine deletion. |

The practical peak is Wave P1 with up to six writers and four explorers if
isolated workspaces are available. The backend rewrite peak is three DDD context
workers at once. The plugin burst peak is six official plugin workers only after
extension points are stable.

## Dispatch Order

### Round 1: Start Immediately

Dispatch these together in isolated worker mode:

1. `DDD-E01` HTTP API inventory explorer.
2. `DDD-E02` persistence ownership explorer.
3. `DDD-E03` service ownership explorer.
4. `DDD-E04` test ownership explorer.
5. `OSG-W01` open-source entry docs.
6. `OSG-W02` demo shell and mock catalog.
7. `OSG-W03` frontend schema config foundation.
8. `OSG-W04` local CLI validate and local diff.
9. `OSG-W05A` contract documentation alignment, one worker per assigned contract
   file if needed.
10. `OSG-W06` English docs and release drafts.

The coordinator keeps `DDD-C00` local because it edits root Maven, module
skeletons, and shared architecture guardrails.

### Round 2: First DDD Rewrite Wave

After DDD-C00 passes its checks, dispatch:

1. `DDD-W01` platform.
2. `DDD-W02` risk.
3. `DDD-W03` marketing.

Keep OSG backend ecosystem workers paused. OSG docs, frontend-only schema work,
and local CLI work can continue.

### Round 3: Second DDD Rewrite Wave

After Round 2 is integrated, dispatch:

1. `DDD-W04` CDP.
2. `DDD-W05` BI.
3. `DDD-W06` conversation.

Keep DDD canvas/execution coding paused until the child contract is frozen.

### Round 4: Contract Freeze

Run one coordinator-owned task:

1. Freeze `PublishedCanvasDefinition`.
2. Freeze `ExecutionPublicationPort`.
3. Freeze node metadata, handler enablement, plugin permission, template import,
   DSL import/export, dry-run, trace, and AI draft boundaries.
4. Mirror affected Open Source Growth contracts.

Use reviewer agents, not implementation workers, for this round.

### Round 5: Canvas Then Execution

Run:

1. `DDD-W07` canvas.
2. Integrate and verify.
3. `DDD-W08` execution.
4. Integrate and verify.

Do not run these two as simultaneous code writers even though their directories
are different. The shared publication contract makes parallel implementation
too likely to diverge.

### Round 6: Plugin Burst

After execution extension points compile, dispatch the six official plugin
workers in parallel:

1. `OSG-W07A` webhook.
2. `OSG-W07B` message.
3. `OSG-W07C` coupon.
4. `OSG-W07D` approval.
5. `OSG-W07E` AI LLM.
6. `OSG-W07F` risk check.

Each worker owns one plugin package, one test package, and one docs file. The
coordinator owns shared registry and handler files.

### Round 7: Ecosystem Backend and CLI

After G10 public extension/API stability gate passes, dispatch:

1. `OSG-W09` template import backend.
2. `OSG-W10` Canvas DSL backend.
3. `OSG-W11` CLI backend API commands.
4. `OSG-W12` AI journey backend.
5. `OSG-W13` frontend AI assistant, if editor integration files are reserved.

If any two workers need the same controller, facade, or frontend editor file,
serialize those workers or have the coordinator create a small integration
adapter first.

### Round 8: Final Productization and Cutover

Run:

1. `OSG-W14` playground.
2. English docs/release post verification.
3. `DDD-C09` web/boot cutover.
4. Full backend, frontend, guardrail, demo, CLI, and contract verification.

The final cutover remains coordinator-owned.

## Worker Prompt Contract

Every worker prompt must include this block:

```text
You are not alone in the codebase. Other workers may be editing other modules.
Do not revert edits you did not make. Do not modify files outside your assigned
write scope. If you need a coordinator-owned file, stop and return NEEDS_CONTEXT.

Program:
Task id:
Dispatch id:
Allowed write scope:
Forbidden write scope:
Read scope:
Target backend state:
  CURRENT_ENGINE_BRIDGE or DDD_FINAL_MODULE or DOCS_ONLY
Contracts to read:
Contracts changed:
Verification commands:
Rollback path:

Return:
  status: DONE, DONE_WITH_CONCERNS, NEEDS_CONTEXT, or BLOCKED
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

## Reviewer Parallelism

Reviewers can run in parallel after workers return if they review disjoint
outputs.

| Reviewer | Scope | Can run in parallel |
| --- | --- | --- |
| Spec reviewer | One worker output against its owning spec/plan | Yes, one per completed worker |
| Architecture reviewer | One backend module against DDD guardrails | Yes, one per module |
| Contract reviewer | One public contract or API surface | Yes, if files are disjoint |
| Integration reviewer | Cross-worker merge result | No, coordinator-owned |

Never start a code-quality review before spec compliance review passes for the
same worker output.

## Stop Rules

Stop dispatching new writers when any of these is true:

- two active workers need the same file
- a worker needs a coordinator-owned file
- a worker wants to create a parallel plugin registry instead of extending
  `PluginRegistryService`
- a worker wants to make a final backend implementation under old
  `canvas-engine` without declaring `CURRENT_ENGINE_BRIDGE`
- a worker edits applied Flyway migrations
- a demo shortcut weakens tenant isolation, permission, trace, audit, or
  production safety
- DDD context migration starts adding Open Source Growth product features
- Open Source Growth backend work starts before its DDD owner API is explicit
