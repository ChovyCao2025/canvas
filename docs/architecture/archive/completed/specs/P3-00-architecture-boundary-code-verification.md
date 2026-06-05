# Architecture Boundary Code Verification

Date: 2026-06-03

Related spec: [P3-00-architecture-boundary-review-spec.md](./P3-00-architecture-boundary-review-spec.md)

## Scope

This verification checks whether the recommended seven bounded contexts match the current codebase, and whether the code is ready for physical service extraction.

Reviewed evidence:

- backend package layout under `backend/canvas-engine/src/main/java/org/chovy/canvas`
- web controllers under `web/`
- domain services under `domain/`
- execution runtime under `engine/`
- shared persistence under `dal/`
- database migrations under `backend/canvas-engine/src/main/resources/db/migration`

## Verdict

The seven-context model is supported by current code structure, but the boundaries are not enforceable yet.

Physical service extraction should not start from the archived "many services" design. The current state is still a modular-monolith cleanup problem: shared mapper access, cross-domain service calls, shared schema ownership, and incomplete tenant propagation must be addressed before a service split.

## Context Mapping

| Recommended context | Current code anchors | Verification result |
|---|---|---|
| Canvas Authoring | `domain/canvas`, `web/CanvasController.java`, `CanvasMapper`, `CanvasVersionMapper` | Real context exists, but it imports execution runtime, scheduler, Redis route/cache, and Groovy validation directly. |
| Execution Runtime | `engine/*`, `engine/trigger`, `engine/scheduler`, `engine/wait`, `domain/execution` | Real context exists, but it reads canvas tables, execution tables, Redis, RocketMQ, CDP, and handlers directly. |
| CDP / Audience | `domain/cdp`, `engine/audience`, audience mappers | Real context exists, but CDP services read canvas execution and canvas authoring tables directly. |
| Reach / Notification | `domain/notification`, `engine/delivery`, send handlers, policy handlers | Real context exists, but delivery and policy code sit inside `engine` and write reach/policy tables directly. |
| Integration | `domain/datasource`, API/MQ definitions, external trigger/send handlers | Partial context exists; protocol and adapter ownership are mixed into Canvas and Execution Runtime. |
| Platform | `auth`, `common/tenant`, `domain/tenant`, `domain/meta`, system controllers | Real platform code exists, but tenant usage queries read canvas, execution, and DLQ tables directly. |
| Data Platform / Analytics | event logs, audience stats/compute tables, future evolution docs | Not a separate runtime context yet; should begin as a thin analytics slice, not a full platform extraction. |

## Evidence: Current Structure

- The backend has separate top-level packages for `domain`, `engine`, `dal`, `infrastructure`, `auth`, `common`, and `web`.
- There are 29 controller files under `web/`, covering Canvas, Execution, Audience, CDP, Notification, Tenant, Meta, API/MQ/Event definitions, Ops, and admin functions.
- There are 49 mapper files under one shared `dal/mapper` package, not under context-owned repositories.
- The migration tree is a single shared schema from `V1__init_schema.sql` through `V90__register_commit_action_node.sql`.

This supports bounded-context classification, but not physical separation.

## Evidence: Boundary Violations

### Canvas Authoring Reaches Into Runtime And Infrastructure

`CanvasService` imports runtime and infrastructure directly:

- `engine.dag.DagParser`, `engine.handlers.GroovyHandler`, `engine.rule.CanvasRuleGraphValidator`
- `engine.trigger.CanvasSchedulerService`, `CanvasExecutionService`, `TriggerPreCheckService`
- `infrastructure.cache.CanvasConfigCache`
- `infrastructure.redis.TriggerRouteService`
- `StringRedisTemplate`

Evidence: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java:12-20`, `:56-74`.

Conclusion: Canvas Authoring and Execution Runtime are too coupled to be the first physical split.

### Execution Runtime Reaches Into Canvas, CDP, Redis, RocketMQ, And DAL

`CanvasExecutionService` imports and owns direct dependencies on:

- canvas tables: `CanvasMapper`, `CanvasVersionMapper`
- execution tables: `CanvasExecutionMapper`, `CanvasExecutionDlqMapper`, `CanvasExecutionStatsMapper`
- CDP service: `CdpUserService`
- runtime infrastructure: `CanvasConfigCache`, `CanvasEntityCache`, Redis context persistence, `RocketMQTemplate`, Disruptor

Evidence: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java:14-40`, `:70-110`.

Conclusion: Execution Runtime cannot be extracted before it has explicit ports for canvas version lookup, CDP user resolution, context persistence, retry messaging, and execution record ownership.

### Handlers Directly Own Cross-Context Persistence

Examples:

- `TagOperationHandler` imports `CustomerTagMapper` and mutates customer tags from inside a DAG node handler. Evidence: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TagOperationHandler.java:7-8`, `:29-40`, `:63-68`.
- `MqTriggerHandler` imports `MqMessageDefinitionMapper` and resolves MQ definitions from inside the handler. Evidence: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/MqTriggerHandler.java:5-6`, `:26-28`, `:67-75`.
- `ManualApprovalHandler` imports `CanvasManualApprovalMapper` and `NotificationEventService`, combining wait-state persistence and notification side effects. Evidence: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ManualApprovalHandler.java:5-10`, `:37-45`, `:85-100`.

Conclusion: `P1-01-dag-engine-and-handler-boundaries` remains a prerequisite for any serious architecture split.

### Reach / Notification Is A Real Candidate But Still Embedded

`ReachDeliveryService` owns `MessageSendRecordMapper`, builds a `WebClient`, writes send records, and calls the external reach platform from the engine package.

Evidence: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java:7-13`, `:29-45`, `:47-65`, `:69-90`.

Conclusion: Reach / Notification is a plausible early extraction candidate, but only after delivery intent, receipt, idempotency, retry, and channel adapters are moved behind explicit contracts.

### CDP / Audience Is A Real Candidate But Cross-Reads Canvas State

CDP user query and insight services read execution and canvas tables directly:

- `CanvasUserQueryService` imports `CanvasExecutionMapper` and returns execution history. Evidence: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CanvasUserQueryService.java:5-8`, `:27-40`, `:52-71`.
- `CdpUserInsightService` imports `CanvasMapper` and `CanvasExecutionMapper`. Evidence: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserInsightService.java:5-9`, `:37-40`, `:45-59`.

Conclusion: CDP / Audience can be an early extraction candidate only after read models or query APIs replace direct canvas/execution table access.

### Platform Tenant Code Cross-Reads Business Tables

`TenantService.usage()` reads canvas, execution, and DLQ tables directly, including a subquery against `canvas`.

Evidence: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/tenant/TenantService.java:7-14`, `:61-83`.

Conclusion: Platform should provide tenant context and policy, but usage analytics should move to a domain-owned query model or analytics read model before service extraction.

### Tenant Propagation Is Incomplete

`V78__saas_foundation.sql` adds nullable `tenant_id` only to:

- `sys_user`
- `system_option`
- `canvas`
- `canvas_version`
- `canvas_execution`
- `canvas_execution_trace`

Evidence: `backend/canvas-engine/src/main/resources/db/migration/V78__saas_foundation.sql:26-58`.

Many core tables remain outside this migration, including notification, message send records, CDP profile/identity/tag tables, audience tables, data source config, manual approval, wait subscription, and policy tables.

Conclusion: tenant propagation is not ready for cross-service boundaries.

## Data Ownership Check

The current single `dal` package and single migration tree imply shared ownership. A reasonable first ownership map is:

| Context | Candidate owned tables |
|---|---|
| Canvas Authoring | `canvas`, `canvas_version`, `canvas_template`, `node_type_registry`, `context_field`, `api_definition`, `event_definition`, `mq_message_definition`, `ab_experiment` |
| Execution Runtime | `canvas_execution`, `canvas_execution_trace`, `canvas_execution_dlq`, `canvas_execution_request`, `canvas_execution_stats`, `canvas_wait_subscription`, `canvas_manual_approval`, `canvas_mq_trigger_rejected`, `canvas_user_quota` |
| CDP / Audience | `cdp_user_profile`, `cdp_user_identity`, `cdp_user_tag`, `cdp_user_tag_history`, `cdp_tag_operation`, `audience_definition`, `audience_stat`, `audience_compute_run`, tag import tables |
| Reach / Notification | `notification`, `message_send_record`, `customer_profile`, `customer_channel`, `marketing_consent`, `marketing_suppression`, `marketing_frequency_counter`, `customer_tag`, `customer_points_ledger`, `customer_task_record` |
| Integration | `data_source_config`, external API/MQ definitions where used as adapter configuration |
| Platform | `tenant`, `sys_user`, `system_option`, `async_task`, `async_task_subscription`, `canvas_audit_log` |
| Data Platform / Analytics | event log, CDC/event ingestion, OLAP/reporting tables when introduced |

This map is a starting point, not an extraction-ready contract. Each table must have one owner and all cross-context reads must be replaced by APIs, domain ports, events, or read models.

## Service Extraction Ranking

Recommended order:

1. Boundary cleanup inside the modular monolith.
2. Define ports/adapters and table ownership for each context.
3. Extract CDP / Audience if profile/audience compute scale is the dominant pressure.
4. Extract Reach / Notification if channel delivery, receipts, retries, or provider limits are operationally noisy.
5. Extract Integration / WeCom if callbacks, sync jobs, external rate limits, or compliance requirements justify isolation.
6. Defer Canvas Authoring and Execution Runtime split until graph version, scheduler, route/cache, execution context, and state-machine contracts are stable.
7. Defer full Data Platform until at least one thin analytics slice is implemented and measured.

## Required Gates Before Physical Services

- context-owned repository or persistence adapter per table group
- explicit API/event contracts for every cross-context interaction
- no handler direct access to non-runtime mappers
- no direct Canvas/CDP/Reach mapper use from Execution Runtime except through approved ports
- tenant context, trace context, and idempotency propagated across every boundary
- characterization tests for the context being extracted
- observability, rollout, rollback, reconciliation, and runbooks for the new service

## Final Assessment

The current code validates the decision in `P3-00-architecture-boundary-review-spec.md`: start with seven bounded contexts inside the existing deployable. Do not turn the archived service list into immediate deployables.

The first practical architecture work should be package/data boundary enforcement and handler side-effect isolation, not K8s deployment, gateway rollout, or a full data platform build.
