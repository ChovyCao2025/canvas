# Combined Roadmap

Date: 2026-06-08

## Goal

Run DDD modular rewrite and Open Source Growth together without duplicate
backend implementation, unclear ownership, or conflicting agent work scopes.

## Roadmap

### Stage 0: Coordination Freeze

Purpose: make the relationship explicit before assigning parallel workers.

Allowed work:

- Maintain `docs/program-coordination/**`.
- Link coordination docs from both program READMEs and `docs/INDEX.md`.
- Record any new cross-program decision in the relevant program decision log.

Exit criteria:

- Both program READMEs point to this coordination directory.
- Workers have one place to check ordering and conflict rules.

### Stage 1: Open Source Shell and DDD Baseline in Parallel

Purpose: get visible open-source entry value while DDD records the current
behavioral baseline.

Open Source Growth can do:

- README restructuring.
- License decision support, after a human chooses the license.
- CONTRIBUTING, SECURITY, CODE_OF_CONDUCT, issue templates, PR template.
- Quickstart documentation.
- Demo compose configuration review.
- WireMock response documentation and low-risk mock additions.

DDD can do:

- Baseline build/test evidence.
- API inventory.
- persistence ownership inventory.
- module skeleton and architecture guardrails.

Must not do yet:

- Final plugin registry implementation.
- Final template import backend.
- Final Canvas DSL backend.
- Final CLI import/export against unstable APIs.
- AI journey generation backend.

Exit criteria:

- DDD Phase 0 inventory is recorded.
- DDD Phase 1 module skeleton and guardrails are accepted.
- Open Source Growth Month 1 docs and demo shell have no core-engine semantic
  changes.

### Stage 2: DDD Foundation and Non-Canvas Context Migration

Purpose: create target modules and migrate contexts that do not own the
canvas/execution publication contract.

DDD owns:

- `DDD-C00` foundation.
- first DDD wave: `canvas-platform`, `canvas-context-risk`,
  `canvas-context-marketing`.
- second DDD wave: `canvas-context-cdp`, `canvas-context-bi`,
  `canvas-context-conversation`.

Open Source Growth can do:

- Continue docs/community/demo shell work.
- Prepare frontend-only schema configuration work behind stable mock contracts.
- Prepare CLI command shape without final import/export write path.
- Prepare template, plugin, DSL, and AI contract drafts.

Must not do yet:

- Build a final DSL backend against old `CanvasService`.
- Add a new plugin registry that bypasses the DDD execution boundary.
- Add official plugins before handler enablement and permission rules are
  settled.

Exit criteria:

- Foundation and first two DDD waves pass the gate matrix.
- OSG backend ecosystem tasks are still either docs-only or named
  `CURRENT_ENGINE_BRIDGE`.

### Stage 3: Canvas/Execution Contract Freeze

Purpose: stabilize the backend APIs that plugin, template, DSL, CLI, AI, and
playground work depend on.

DDD owns:

- `PublishedCanvasDefinition`.
- `ExecutionPublicationPort`.
- node metadata and handler enablement boundaries.
- dry-run, trace, wait/resume, and publish precheck boundaries.

Open Source Growth can do:

- Mirror affected plugin, template, DSL, CLI, demo, and AI contracts.
- Continue frontend-only and docs-only work.

Backend Open Source Growth final implementations still wait for the target DDD
API to compile.

Exit criteria:

- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md` names the
  extension points needed by Open Source Growth.
- Open Source Growth contracts that depend on canvas/execution have matching
  mirror notes in DDD docs.

### Stage 4: Canvas and Execution Rewrite

Purpose: implement the two contexts that own authoring and runtime.

Order:

1. `canvas-context-canvas`
2. integrate and verify
3. `canvas-context-execution`
4. integrate and verify

Do not run canvas and execution code-writing workers at the same time.

Exit criteria:

- Canvas and execution module tests pass.
- DDD guardrails pass.
- publication, handler, dry-run, trace, and precheck contracts are stable.

### Stage 5: Open Source Ecosystem on DDD Boundaries

Purpose: implement the ecosystem features on stable module boundaries.

Open Source Growth can implement:

- Plugin manifest validation and enablement.
- Official plugin registration through the execution boundary.
- Template pack import/export through the canvas boundary.
- Canvas DSL backend and CLI import/export/diff.
- AI draft generation, risk audit, and trace explanation.
- Playground golden path.

DDD still owns:

- Dependency direction.
- package placement.
- architecture tests.
- runtime assembly in `canvas-boot`.
- compatibility tests.

Exit criteria:

- Plugin, template, DSL, CLI, AI, and playground tests run against the DDD
  module APIs or a named compatibility adapter.
- No final implementation depends on old `canvas-engine` internals.

### Stage 6: Runtime Cutover and External Launch

Purpose: switch runtime assembly after behavior and product entry are both
credible.

DDD exit criteria:

- `canvas-boot` is the runtime assembly.
- old `canvas-engine` is no longer a compile dependency.
- controller compatibility tests pass.
- Flyway migrations are preserved without renumbering applied migrations.

Open Source Growth exit criteria:

- quickstart starts the supported runtime.
- demo profile uses safe mock defaults.
- plugin and template docs match actual extension points.
- CLI and playground use supported APIs.

## Recommended First Work

1. Finish coordination docs and links.
2. Get human license decision for Open Source Growth.
3. Capture DDD baseline and inventory.
4. Build DDD module skeleton and guardrails.
5. Stabilize canvas/execution contracts.
6. Resume backend Open Source Growth ecosystem features on the new boundaries.
