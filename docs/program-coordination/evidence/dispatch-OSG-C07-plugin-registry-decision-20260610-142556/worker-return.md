# OSG-C07 Plugin Registry Decision

Date: 2026-06-10

Task id: OSG-C07

Status: DONE

Owner: coordinator

Explorers:

- Peirce `019eb02c-13a9-7dd3-810d-1d2be4e69462`
- Hilbert `019eb02c-9f02-7040-b916-4250ff4a54b9`

## Decision

Target backend state: `DDD_FINAL_MODULE`.

Final ownership is split by responsibility:

- `canvas-platform` owns plugin registry metadata, manifest validation,
  permission vocabulary, compatibility checks, persistence, and enablement
  state.
- `canvas-context-execution` owns handler discovery and binding through
  `NodeHandlerRegistry`, node metadata, publish and dry-run validation hooks,
  execution failure/trace integration, and consumption of the platform-owned
  enablement view.
- `canvas-web` may expose public HTTP APIs for plugin catalog, enablement, and
  node metadata, but must not own registry or handler binding logic.

The legacy `canvas-engine` plugin implementation remains a source row and, when
explicitly declared, a temporary bridge only:

- `PluginRegistryService`
- `JdbcPluginRepository`
- `PluginRegistryController`
- `HandlerRegistry`
- `built_in_plugin_registry`

New OSG backend implementation must not create `CanvasPluginRegistry` or any
second plugin registry surface. Any worker that touches old `canvas-engine`
registry or handler files must include a complete `CURRENT_ENGINE_BRIDGE`
declaration naming the exact old files, final DDD owner module, idempotency
rule, removal gate, rollback path, and G10/G12 gate impact.

## Evidence

Peirce confirmed:

- The existing old-engine registry is `PluginRegistryService` plus
  `JdbcPluginRepository`, `PluginRegistryController`, and
  `built_in_plugin_registry`.
- The old handler path is `HandlerRegistry`, `NodeHandler`, and
  `@NodeHandlerType`.
- DDD execution already has `NodeHandler`, `NodeHandlerType`,
  `NodeHandlerRegistry`, `NodeMetadata`, `NodeMetadataView`, and
  `PluginEnablementView`.
- Workers must not create `CanvasPluginRegistry`; no production implementation
  exists today.

Hilbert confirmed:

- `plugin-manifest-v1.md`, `node-handler-contract.md`,
  `canvas-execution-contract-spec.md`, DDD inventory, and integration docs
  already point to the platform/execution split.
- Several OSG and coordination docs still described old `canvas-engine`
  `PluginRegistryService` as the apparent final target and needed wording
  updates before plugin workers run.

## Dispatch Effect

- OSG-C07 is closed as a coordinator decision.
- OSG-W07A through OSG-W07F remain code-writing tasks with disjoint official
  plugin package and docs ownership.
- Those workers still require G10 public extension/API stability evidence and
  exact active dispatch reservations before they can be marked `RUNNING`.
- OSG-W09, OSG-W10, OSG-W11, and OSG-W12 remain blocked until G10 passes.

## Verification Plan

Required before claiming OSG-C07 closure:

- `node tools/program-coordination/check-dispatch-state.mjs .`
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
- `node --test tools/program-coordination/*.test.mjs`
- `node tools/open-source-growth/guardrail-verifier.mjs`
- `node --test tools/open-source-growth/guardrail-verifier.test.mjs`
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`
- `git diff --check`
