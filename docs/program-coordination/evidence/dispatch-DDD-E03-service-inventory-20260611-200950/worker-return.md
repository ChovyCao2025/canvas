# DDD-E03 Service Inventory Worker Return

status: DONE_WITH_CONCERNS
task id: DDD-E03
dispatch id: dispatch-DDD-E03-service-inventory-20260611-200950
worker: multi_agent_v1-explorer Newton 019eb695-33c7-7a91-b27b-e5fb0fbdd2b5

## Files Read

- Required coordination and DDD docs listed in the dispatch.
- `docs/ddd-rewrite/inventory/service-ownership.md`.
- Java scope enumerated: 1,088 files under `domain`, `engine`, `platform`,
  and `architecture`.
- Targeted old-engine files inspected: `CanvasExecutionService`, `DagEngine`,
  `PluginRegistryService`, `HandlerRegistry`, node handlers, execution request
  services, delivery services, audience services, and lifecycle gates.

## Findings

- Existing service inventory exists: 1,089 rows and 22,873 lines.
- Target module counts: BI 285, marketing 250, platform 149, risk 115, CDP 105,
  execution 92, conversation 67, canvas 26.
- Target role counts: domain 694, application 250, adapter 139, config 6,
  deletion candidate 0.
- `CanvasExecutionService`: execution/application. Must split DB mappers,
  Redis/context state, CDP user creation, Disruptor, metrics, lifecycle gate,
  and DAG handoff.
- `DagEngine`: execution/domain. Still owns persistence, trace, timeout,
  context concerns, and a circular dependency on `CanvasExecutionService`.
- `CanvasExecutionRequestService`, `CanvasExecutionRequestDispatcher`, and
  `CanvasExecutionRequestExecutor`: execution/application, with persistence and
  scheduler/disruptor adapters extracted.
- `HandlerRegistry`, `NodeHandler`, `NodeHandlerType`, `NodeResult`, and
  `NodeRouteResolver`: execution/domain.
- Most `engine.handlers.*`: execution/domain, but cross-context calls must go
  through APIs or ports.
- `PluginRegistryService`: currently platform/application, but G10 requires
  split ownership.
- `DeliveryOutboxService` and `ReachDeliveryService`: currently
  platform/application; code mixes execution retry/outbox, marketing delivery
  policy, connectors, RocketMQ, and send-record persistence.
- `AudienceBatchComputeService` and `AudienceSnapshotService`: CDP/application,
  but execution still depends on audience bitmap/runtime services.
- No confident deletion candidate found from source alone.

## Ambiguous Ownership

- Plugin registry: platform metadata/manifest/permissions/enablement versus
  execution handler binding/node metadata/runtime validation/read models.
- Delivery: execution outbox/retry versus marketing channel policy/connectors
  versus messaging adapter.
- Risk, approval, user-input, content, CDP, and AI handlers: execution owns node
  runtime; other contexts own business decisions.
- Audience/rule services: CDP audience ownership versus execution runtime usage.
- Duplicate `engine.lifecycle.ExecutionLifecycleGate` and
  `engine.trigger.ExecutionLifecycleGate`: possible consolidation, but not safe
  to mark deletion yet.

## Recommended Coordinator Decisions

- Enforce the G10 plugin registry split and require a named bridge or migration;
  no second registry.
- Keep node handler runtime in execution; require API facades/ports for risk,
  approval, canvas user input, CDP points/audience, content, MQ metadata, and
  AI.
- Decide delivery ownership explicitly before cutover: likely execution owns
  outbox/retry mechanics, marketing owns delivery policy/channel contracts, and
  adapters own provider/MQ details.
- Break the `CanvasExecutionService`/`DagEngine` circular dependency before
  `DDD-C09`.
- Decide lifecycle gate consolidation after usage review; do not delete either
  gate blindly.

## Verification Commands Run Or Inspected

- `sed -n` on required dispatch docs and placement reference.
- `find ... -name '*.java' | wc -l` for service scope: 1,088.
- `wc -l` and `rg -c '^old class:'` on service inventory: 22,873 lines and
  1,089 rows.
- `awk` counts for target modules and roles.
- `rg` scans for cross-context imports `domain -> engine` and `engine -> domain`.
- `git status --short` inspected; no files edited by this worker.

## Risks / Cutover Blockers

- `DDD-C09` remains blocked if old `CanvasExecutionService` continues owning
  persistence, Redis, CDP, lifecycle, metrics, and DAG orchestration directly.
- Handler cross-context imports will violate final DDD dependency rules unless
  converted to API ports.
- Plugin registry split is a hard G10/G12 risk.
- Delivery/channel ownership can create duplicate outbox/provider behavior if
  not assigned before migration.
- Worktree is already dirty with unrelated/coordination changes; this dispatch
  stayed read-only.

