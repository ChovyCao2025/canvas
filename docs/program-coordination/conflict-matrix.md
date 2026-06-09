# Conflict Matrix

Date: 2026-06-08

## Purpose

This matrix tells coordinators and agents whether an Open Source Growth task can
run now, can run with limits, or must wait for DDD module boundaries.

## Matrix

| Workstream | Conflict Level | Can Run Before DDD Cutover | DDD Dependency | Rule |
| --- | --- | --- | --- | --- |
| README and positioning | Low | Yes | None | Can proceed immediately. Keep deployment details accurate. |
| Community files | Low | Yes | None | License requires human decision before creating `LICENSE`. |
| Quickstart docs | Low | Yes | Runtime command names | Use current commands first; update after `canvas-boot` cutover. |
| Demo compose shell | Low to Medium | Yes, with limits | Runtime profile and mock services | Do not change core execution semantics for demo. |
| Demo seed service | Medium | Yes only as isolated bridge | Canvas draft/import API | Must be idempotent and easy to move into `canvas-boot` or context module. |
| WireMock mocks | Low | Yes | Provider adapter names | Add mock responses without forcing production defaults. |
| Frontend demo entry | Low to Medium | Yes | Stable route/API expectations | Can proceed if API calls remain compatible or mocked. |
| Plugin manifest contract docs | Medium | Yes | Execution extension points and platform registry ownership | Document now; final backend implementation waits for DDD ownership. |
| Plugin registry backend | High | Wait unless named bridge | `canvas-platform` and `canvas-context-execution` ownership | Do not create a parallel final registry under old `canvas-engine`. |
| Official plugins | High | Wait | Handler registry, permission model, provider ports | Implement after handler enablement and extension metadata are stable. |
| Node metadata API | High | Wait or expose compatibility adapter | `canvas-context-execution/api` | Must not bypass handler registry or plugin enablement. |
| Template pack docs | Low to Medium | Yes | Template import behavior | Docs and examples can proceed; backend import waits for canvas API. |
| Template import backend | High | Wait | `canvas-context-canvas/api` and execution validation | Import creates draft/version through canvas API; dry-run uses execution API. |
| Schema-driven frontend config | Medium | Yes with mock/stable schema | Node metadata schema | Can be frontend-first if it degrades for unsupported nodes. |
| Canvas DSL contract docs | Medium | Yes | Canvas graph and publish contract | Freeze syntax carefully; do not promise unsupported node behavior. |
| Canvas DSL backend | High | Wait | `canvas-context-canvas/api` | Final implementation must not bind to old `CanvasService` internals. |
| CLI validate command | Medium | Yes | DSL schema | Can run locally against DSL schema without backend writes. |
| CLI import/export/diff/publish | High | Wait | G10 public extension and API stability gate | Must use supported APIs, not internal module classes. |
| AI prompt/docs | Low to Medium | Yes | Safety and draft rules | Can prepare UX and docs; generated output must remain draft-only. |
| AI journey backend | High | Wait | canvas draft API, risk audit rules, trace explanation API | Must not overwrite published canvases or bypass approval/publish. |
| Playground docs and scripted story | Medium | Yes with mock data | Demo profile and templates | Can prepare narrative; final golden path waits for demo + DSL + trace. |
| Playground live backend flow | High | Wait | demo profile, template pack, dry-run, trace, CLI/API | Implement after Stage 4 ecosystem APIs are stable. |

## Stop Rules

Stop the task and escalate to the main coordinator when any condition is true:

- A worker wants to create a new backend registry with the same responsibility
  as `PluginRegistryService` without defining it as a temporary bridge.
- A worker wants to implement DSL import/export directly against old
  `CanvasService` and call it final.
- A worker wants to add Open Source Growth features inside a DDD context worker
  task whose stated goal is behavior-preserving migration.
- A worker needs to edit `backend/pom.xml`, `canvas-web`, `canvas-boot`, or a
  shared contract file while another worker is editing the same file.
- A worker changes Flyway migration numbers or edits an applied migration.
- A worker weakens production security or tenant isolation for demo convenience.
- A worker treats docs/demo readiness as proof of DDD readiness.
- A worker treats DDD module skeletons as proof of Open Source Growth readiness.

## Green-Lane Work

These tasks are safe to run while DDD is still in inventory or module skeleton
stages:

- README, quickstart, contribution, security, and issue/PR templates.
- Open Source Growth success metrics and traceability maintenance.
- Mock response cataloging.
- Screenshots and demo narrative using existing behavior.
- Contract documentation that clearly labels unresolved backend placement.
- DDD inventory and architecture guardrail work.

## Red-Lane Work

These tasks must wait until DDD canvas/execution contracts are explicit:

- Final plugin registry backend.
- Official plugin runtime behavior.
- Template import backend.
- Canvas DSL backend import/export.
- CLI backend write commands.
- AI journey generation backend.
- Playground live golden path that depends on dry-run and trace.
