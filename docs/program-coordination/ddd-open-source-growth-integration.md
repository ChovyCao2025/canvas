# DDD and Open Source Growth Integration

Date: 2026-06-08

## Decision

Integrate the two programs through a coordination layer, not by merging their
plans.

The DDD modular rewrite remains a structural backend rewrite into a modular
monolith. Open Source Growth remains a six-month productization and ecosystem
program. They share contracts and sequencing, but they have different success
criteria.

## Why They Should Not Be Merged

`docs/open-source-growth/README.md` explicitly lists full DDD refactor as a
non-goal for the six-month open-source program. That is the right constraint:
Open Source Growth needs visible external value, while DDD needs internal
module correctness and behavioral compatibility.

The two plans also assume different backend paths:

- Open Source Growth currently names paths under
  `backend/canvas-engine/src/main/java/org/chovy/canvas/...`.
- DDD rewrites toward modules such as `canvas-context-canvas`,
  `canvas-context-execution`, `canvas-platform`, `canvas-web`, and
  `canvas-boot`.

If both streams write the same backend feature into the old `canvas-engine`
while DDD is creating the new module boundary, the result is duplicate work and
unclear ownership. The integration rule is therefore:

```text
Productization can start early.
Backend ecosystem features wait for target module ownership.
Compatibility bridges are allowed only when named as bridges.
```

## Shared Ownership Model

| Area | Open Source Growth owns | DDD owns | Coordination rule |
| --- | --- | --- | --- |
| Repository landing experience | README, quickstart, community files, screenshots, demo narrative | No direct ownership | Can run immediately. Do not present documentation readiness as DDD completion. |
| Demo profile | Demo compose, mock providers, seed data, example journeys | Final runtime placement in `canvas-boot` and context modules | Early demo work must be isolated and reversible. Final placement follows DDD cutover. |
| Plugin manifest | External contract, extension vocabulary, documentation | Module placement and runtime enforcement | Manifest metadata and enablement can live in `canvas-platform`; node handler binding belongs to `canvas-context-execution`. |
| Node handler contract | Public extension guide and examples | Execution API, handler registry boundary, validation rules | Contract changes must update both `docs/open-source-growth/contracts/node-handler-contract.md` and the DDD canvas/execution child spec. |
| Template pack | Template authoring format, import/export docs, official templates | Canvas draft/version/publish ownership and execution validation | Template import belongs to `canvas-context-canvas`; dry-run validation crosses into `canvas-context-execution` through API. |
| Canvas DSL and CLI | YAML syntax, CLI UX, diff/import/export commands | Stable canvas API, publish contract, web adapters | DSL backend must not be final against old `CanvasService`; it must target the DDD canvas API or a named compatibility adapter. |
| AI operator | UX, prompts, generated journey review, risk audit narrative | Application boundaries, draft safety, trace explanation ownership | AI may create drafts and audit proposals. It must not bypass publish, permission, trace, or execution contracts. |
| Playground | External trial flow and docs | Runtime assembly and environment boundaries | Playground depends on demo profile, template pack, DSL export, dry-run, and trace contracts. |

## Contract Mirroring Rules

When an Open Source Growth contract affects backend module boundaries, mirror it
into the DDD rewrite material.

| Open Source Growth contract | DDD mirror target | Required mirror content |
| --- | --- | --- |
| `contracts/node-handler-contract.md` | `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md` | Handler identity, type uniqueness, enabled/disabled behavior, execution-time validation. |
| `contracts/plugin-manifest-v1.md` | `docs/ddd-rewrite/task-packs/08-worker-execution.md` and `docs/ddd-rewrite/task-packs/01-worker-platform.md` | Manifest storage, enablement, permission model, node binding boundary. |
| `contracts/template-pack-v1.md` | `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md` | Draft creation, published graph validation, dependency checks. |
| `contracts/canvas-dsl-v1.md` | `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md` | DSL mapping to draft canvas, import/export compatibility, publish precheck behavior. |
| `contracts/demo-profile-contract.md` | `docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md` | Demo profile location, config defaults, mock provider wiring, production safety. |
| `contracts/ai-operator-contract.md` | `docs/ddd-rewrite/task-packs/03-worker-marketing.md`, `07-worker-canvas.md`, and `08-worker-execution.md` | Draft-only generation, risk audit input, trace explanation output, permissions. |

## Practical Integration Rules

1. Documentation and community work can proceed without DDD readiness.
2. Demo compose and mock provider work can proceed if it does not alter core
   execution semantics.
3. Plugin registry, official plugins, template import, Canvas DSL backend, CLI
   import/export, AI journey generation, and playground dry-run must declare
   whether they target old `canvas-engine` as a bridge or the new DDD modules as
   the final implementation.
4. No worker may create a second plugin registry under a different name to avoid
   a DDD decision.
5. No worker may put product feature expansion inside a DDD context migration
   task. DDD tasks preserve behavior first.
6. No worker may claim the Open Source Growth program is complete because DDD
   module skeletons exist.
7. No worker may claim DDD is complete because the demo or README is polished.

