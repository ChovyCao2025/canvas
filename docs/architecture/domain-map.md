# Marketing Canvas Domain Map

Date: 2026-06-05

Status: Active modular-monolith boundary map. Physical service extraction remains blocked by `docs/architecture/adr/ADR-0006-service-extraction-gate.md`.

## Inventory Snapshot

Current repository inventory used by this map:

- 85 controller files under `backend/canvas-engine/src/main/java/org/chovy/canvas/web`.
- 146 mapper files under the shared `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper` package.
- 164 migration scripts under `backend/canvas-engine/src/main/resources/db/migration`.
- 85 migration files contain `CREATE TABLE`; there are 172 `CREATE TABLE` statements.
- Newer BI, CDP warehouse, analytics, AI, and channel-integration modules are already present in the monolith.

This map uses the seven bounded contexts from `docs/architecture/archive/specs/P3-00-architecture-boundary-review-spec.md` and `docs/architecture/evidence/p3-00-architecture-boundary-review.md`.

## Context Summary

| Context | Purpose | Current code anchors | Owned data candidates |
|---|---|---|---|
| Canvas Authoring | Draft, validate, publish, version, template, and project-level governance for canvas definitions. | `domain/canvas`, `web/CanvasController.java`, `web/CanvasBatchOperationController.java`, `web/CanvasStatsController.java`, `engine/rule`, `engine/template` | `canvas`, `canvas_version`, `canvas_template`, `canvas_audit_log`, `canvas_project`, `canvas_project_member`, `canvas_project_folder`, `canvas_control_group_holdout`, `canvas_conversion_attribution` |
| Execution Runtime | Admit triggers, create execution records, persist execution context, schedule/resume/wait, run DAG nodes, and record traces/DLQ. | `engine/trigger`, `engine/scheduler`, `engine/wait`, `engine/context`, `engine/dag`, `engine/disruptor`, `domain/execution`, `web/ExecutionController.java`, `web/CanvasExecutionManagementController.java`, `web/CanvasExecutionRequestManagementController.java` | `canvas_execution`, `canvas_execution_trace`, `canvas_execution_dlq`, `canvas_execution_request`, `canvas_execution_stats`, `canvas_wait_subscription`, `canvas_mq_trigger_rejected`, `execution_rerun_audit`, `execution_retention_*`, `node_side_effect_idempotency` |
| CDP / Audience | Maintain customer identity/profile/tag state, audience definitions, audience computation, CDP event ingestion, and user insights. | `domain/cdp`, `engine/audience`, `web/CdpUserController.java`, `web/AudienceController.java`, `web/CdpTagOperationController.java`, `web/CanvasUserController.java` | `cdp_user_profile`, `cdp_user_identity`, `cdp_user_tag`, `cdp_user_tag_history`, `cdp_tag_operation`, `cdp_event_log`, `cdp_write_key`, `audience_definition`, `audience_stat`, `audience_compute_run`, `cdp_computed_*`, `cdp_audience_snapshot`, `audience_bitmap_*` |
| Reach / Notification | Decide contactability, create notifications, dispatch messages, track receipts, apply suppression/consent/frequency policy, and surface user/task alerts. | `domain/notification`, `engine/delivery`, `engine/policy`, `web/NotificationController.java`, `web/MessageDeliveryController.java`, `web/DeliveryReceiptController.java`, `web/MarketingPreferenceCenterController.java`, send handlers under `engine/handlers` | `notification`, `message_send_record`, `delivery_outbox`, `delivery_receipt_log`, `customer_profile`, `customer_channel`, `marketing_consent`, `marketing_suppression`, `marketing_frequency_counter`, `channel_dedupe_record`, `channel_fallback_decision` |
| Integration | Own external system adapters, datasource credentials, API/MQ/webhook definitions, channel connectors, callback validation, and provider health checks. | `domain/datasource`, `engine/channel`, `infrastructure/http`, `infrastructure/mq`, `web/DataSourceConfigController.java`, `web/ChannelConnectorController.java`, `web/ApiDefinitionController.java`, `web/MqDefinitionController.java` | `data_source_config`, `api_definition`, `mq_message_definition`, `webhook_subscription`, `webhook_delivery_log`, `channel_connector`, `channel_provider_limit`, `channel_fallback_policy`, `connected_content_cache` |
| Platform | Provide tenant, auth, admin, options, audit, task, security, release/ops, and shared governance primitives. | `auth`, `common/tenant`, `domain/tenant`, `domain/meta`, `domain/ops`, `domain/compliance`, `config`, `security`, `web/AuthController.java`, `web/TenantController.java`, `web/OpsController.java`, `web/AdminController.java` | `tenant`, `sys_user`, `system_option`, `async_task`, `async_task_subscription`, `privacy_compliance_evidence`, `architecture_deployment_evidence`, `regional_expansion_evidence`, `product_led_growth_evidence`, shared audit/ops tables |
| Data Platform / Analytics | Provide analytical event capture, warehouse governance, CDP warehouse orchestration, BI semantic/query/portal capabilities, and AI analytics surfaces. | `domain/analytics`, `domain/warehouse`, `domain/bi`, `domain/ai`, `infrastructure/doris`, `infrastructure/bi`, `web/CdpWarehouse*Controller.java`, `web/AnalyticsController.java`, `web/bi/*Controller.java`, `web/Ai*Controller.java` | `analytics_*`, `cdp_warehouse_*`, `bi_*`, `ai_provider`, `ai_model_registry`, `ai_prompt_template`, `ai_usage_audit`, `ai_prediction_run`, `ai_user_prediction_snapshot` |

## Context Details

### Canvas Authoring

- Public APIs: canvas CRUD, draft/publish/archive, batch operation, templates, project folders, canvas statistics, and canvas-user exploration routes.
- Emitted events: should emit `CanvasPublished`, `CanvasArchived`, `CanvasVersionCreated`, and `CanvasRouteChanged`; today these are mostly direct calls or cache/route mutations rather than versioned events.
- Consumed events: none should be required for command correctness; authoring may consume read models for stats and runtime summaries.
- forbidden dependencies: direct calls into `DagEngine`, scheduler registration internals, Redis route/cache mutation, execution mappers, and CDP user/profile services.
- Current violations: `CanvasService` imports scheduler, execution, DAG parsing, Groovy validation, cache and Redis route helpers. This keeps Canvas Authoring and Execution Runtime too tightly coupled for service extraction.

### Execution Runtime

- Public APIs: direct trigger, dry run, execution management, request replay, DLQ/retry, wait/resume, MQ trigger ingestion, and runtime metrics.
- Emitted events: should emit `ExecutionStarted`, `ExecutionSucceeded`, `ExecutionFailed`, `ExecutionPaused`, `ExecutionDlqCreated`, and `TriggerRejected`.
- Consumed events: trigger commands, route update notifications, kill switch signals, retry commands, and scheduling ticks.
- forbidden dependencies: direct writes into CDP profile tables, direct notification creation for system alerts, provider-specific delivery calls from generic node handlers, and authoring table reads outside a graph-loading port.
- Current violations: `CanvasExecutionService` directly depends on `CdpUserService` through `ensureCdpUser`; `MqTriggerConsumer` calls `NotificationEventService`; execution config loading reads authoring tables directly; runtime owns Redis keys, mappers, Disruptor dispatch, and trace writes in one boundary.

### CDP / Audience

- Public APIs: `/cdp/users`, `/canvas/audiences`, user insight, tag write/remove, tag history, audience preview/compute/stat, CDP ingestion, and write-key management.
- Emitted events: should emit `CdpUserObserved`, `CdpProfileUpdated`, `CdpTagChanged`, `AudienceComputed`, and `AudienceSnapshotPublished`.
- Consumed events: execution lifecycle, delivery receipt, identity merge/split, consent update, and external CDP ingestion events.
- forbidden dependencies: direct `CanvasExecutionMapper` or `CanvasMapper` reads from CDP services, direct runtime invocation for audience computation, and warehouse checkpoint writes hidden behind CDP ingestion without an event contract.
- Current violations: `CanvasUserQueryService` and `CdpUserInsightService` read execution and canvas mappers directly. CDP ingestion also calls warehouse services inside the monolith, so source-of-truth and analytical read-model ownership are not separate.

### Reach / Notification

- Public APIs: `/canvas/notifications`, `/canvas/notifications/ws-ticket`, message delivery, delivery receipts, marketing preference center, and contactability checks.
- Emitted events: should emit `NotificationCreated`, `NotificationUpdated`, `DeliveryRequested`, `DeliverySent`, `DeliveryFailed`, `ReceiptReceived`, and `PreferenceChanged`.
- Consumed events: execution side-effect command, task result, system alert request, CDP contactability/read model, consent/suppression changes, and provider callback events.
- forbidden dependencies: generic DAG handlers directly calling a provider client, runtime code directly inserting notification rows, notification services reading execution state, and provider retries sharing runtime transactions.
- Current violations: `ReachDeliveryService` is under `engine/delivery` and calls provider adapters while writing delivery records. MQ trigger failure alerts call notification services directly. Delivery outbox is a good boundary-hardening candidate but still shares mapper, transaction, and Flyway ownership.

### Integration

- Public APIs: `/canvas/data-sources`, `/channels/connectors`, API definitions, MQ definitions, webhooks, provider health tests, and callback endpoints.
- Emitted events: should emit `ExternalCallbackReceived`, `DatasourceCredentialChanged`, `ConnectorModeChanged`, `ProviderHealthChanged`, and `WebhookDeliveryFailed`.
- Consumed events: delivery requests, route updates, credential rotation, and tenant policy changes.
- forbidden dependencies: controllers opening arbitrary external connections without an adapter policy, node handlers depending on concrete provider SDKs, and integration tables being used as generic runtime state.
- Current violations: `DataSourceConfigController` still owns JDBC metadata reads directly. Channel connector and provider policy tables sit in the shared mapper package and need context-owned ports before extraction.

### Platform

- Public APIs: auth/login, tenant management, admin, options, ops, audit, async tasks, and governance metadata.
- Emitted events: should emit `TenantCreated`, `TenantPolicyChanged`, `UserRoleChanged`, `AuditEventRecorded`, and `RuntimeGateChanged`.
- Consumed events: business-domain usage summaries through read models only.
- forbidden dependencies: platform services reading business tables for product metrics, domain controllers reimplementing tenant fallback rules, and tenant context being optional across cross-domain contracts.
- Current violations: `TenantService` reads canvas, execution, and DLQ tables to compute usage. Several controllers still normalize missing context locally rather than relying on a single policy.

### Data Platform / Analytics

- Public APIs: analytics queries, warehouse catalog/governance, CDP warehouse jobs/incidents/readiness, BI dataset/chart/dashboard/query/portal, and AI prediction/prompt/provider endpoints.
- Emitted events: should emit `WarehouseSyncCompleted`, `WarehouseQualityIncidentOpened`, `MetricContractChanged`, `BiResourcePublished`, and `AnalyticsExportCompleted`.
- Consumed events: CDP events, execution lifecycle, delivery receipt, identity changes, consent deletion, and schema changes.
- forbidden dependencies: data platform components driving online-domain service boundaries, OLAP jobs writing OLTP source-of-truth tables, and BI/warehouse controllers bypassing tenant/PII/retention gates.
- Current violations: `domain/warehouse`, `domain/bi`, `infrastructure/doris`, and `web/bi` are already inside the monolith but still share mapper and Flyway ownership. This is a platform/data boundary to harden, not a reason to split services first.

## First Extraction Candidate Scoring

Scoring uses 1 as easiest/lowest risk and 5 as hardest/highest risk. The decision in `docs/architecture/adr/ADR-0007-first-extraction-candidate.md` is `Deferred` for physical extraction.

| Candidate | coupling | data ownership clarity | traffic pressure | operational noise | tenant risk | rollback difficulty | Notes |
|---|---:|---:|---:|---:|---:|---:|---|
| CDP / Audience | 4 | 3 | 3 | 3 | 4 | 4 | Strong domain identity, but it still reads execution/canvas mappers and is now coupled to warehouse ingestion. |
| Reach / Notification | 3 | 3 | 4 | 4 | 3 | 3 | Best first boundary-hardening slice because outbox, notification, and delivery contracts can be isolated without moving authoring/runtime tables. |
| Integration / WeCom | 3 | 2 | 3 | 4 | 4 | 3 | Good adapter shape, but WeCom/callback compliance, replay, credential, and tenant rules must be proven before extraction. |

## Candidate Decision

No physical extraction candidate is approved yet. The first executable slice is Reach / Notification boundary hardening inside the modular monolith:

- move generic runtime code toward ports such as `NotificationCommandPort`, `DeliveryCommandPort`, and `ContactabilityReadModel`;
- keep current tables in the same datasource until ownership, migration, and rollback are proven;
- keep existing REST routes stable;
- use events/outbox and read models before package or deployable movement.

Canvas Authoring and Execution Runtime are not first extraction candidates because graph versioning, execution admission, scheduler registration, context persistence, Redis route/cache state, runtime policy, and state transitions still require shared transactions and direct method calls.
