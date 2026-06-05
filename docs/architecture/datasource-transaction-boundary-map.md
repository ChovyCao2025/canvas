# Datasource Transaction Boundary Map

Date: 2026-06-05

Status: P3-04 planning artifact. It forbids distributed transactions as the default split strategy.

## Classification Rules

| Classification | Meaning |
|---|---|
| same-datasource | All writes stay in one future datasource group and may remain a local transaction. |
| outbox | Source group writes its own state plus an outbox row; another group consumes asynchronously. |
| saga | Multiple groups progress through explicit steps with compensation. |
| reconciliation | Drift is allowed temporarily and repaired by an idempotent command or scheduled comparison. |
| blocked | Must not move until contract, tenant, idempotency, and rollback evidence exist. |

## Current Cross-Group Write Flows

| Flow | Current writes | Future groups | Classification | Event | idempotency key | reconciliation command | rollback owner |
|---|---|---|---|---|---|---|---|
| Canvas create/update/publish | `canvas`, `canvas_version`, route/cache side effects | control + runtime cache | blocked until route/cache contract exists | `CanvasPublished` / `CanvasUnpublished` | `canvas:{id}:version:{version}` | rebuild trigger routes and compare published version | Canvas owner |
| Canvas import/export | `canvas`, `canvas_version`, project folder metadata | control | same-datasource | none | imported canvas external key | delete imported draft/version set | Canvas owner |
| Execution trigger | `canvas_execution`, `cdp_user_profile`, Redis context/dedup | runtime + CDP/customer + Redis | blocked until CDP user observation is an event/read model | `CdpUserObserved` | `execution:{executionId}:user:{userId}` | compare execution rows to CDP profile first/last seen | Runtime owner |
| Execution failure to DLQ | `canvas_execution`, `canvas_execution_dlq`, notification alert | runtime + CDP/customer notification | outbox | `ExecutionDlqCreated` / `SystemAlertRequested` | `dlq:{executionId}:{failedNodeId}` | replay DLQ and alert ledger comparison | Runtime owner |
| MQ trigger rejection | `canvas_mq_trigger_rejected`, notification alert | runtime + CDP/customer notification | outbox | `TriggerRejected` / `SystemAlertRequested` | `mq:{msgId}:reject` | rejected table to notification dedup comparison | Runtime owner |
| Wait subscription and resume | `canvas_wait_subscription`, Redis context, execution update | runtime + Redis | reconciliation | `WaitResumed` | `wait:{subscriptionId}:{eventId}` | scan waiting subscriptions and context state | Runtime owner |
| Audience compute | `audience_compute_run`, `audience_stat`, bitmap Redis, optional CDP reads | CDP/customer + Redis + analytics proof | reconciliation | `AudienceComputed` | `audience:{audienceId}:run:{runId}` | rebuild bitmap/stat from compute run | CDP owner |
| CDP tag write | `cdp_user_tag`, `cdp_user_tag_history`, execution side-effect idempotency | CDP/customer + runtime | saga | `CdpTagChanged` | `tag:{userId}:{tagCode}:{sourceRefId}` | compare tag history to current tag | CDP owner |
| Reach delivery | `message_send_record`, `delivery_outbox`, provider response, receipt log | CDP/customer + Integration provider | outbox | `DeliveryRequested` / `DeliverySent` / `ReceiptReceived` | delivery request idempotency key | compare outbox, send record, receipt, provider ID | Reach owner |
| Notification create/update | `notification`, Redis realtime event | CDP/customer + Redis | outbox | `NotificationCreated` / `NotificationUpdated` | notification dedup key | compare unread count REST vs realtime event count | Reach owner |
| Marketing preference update | `marketing_consent`, `marketing_suppression`, `customer_channel` | CDP/customer | same-datasource | `MarketingPreferenceChanged` | `preference:{tenant}:{user}:{channel}` | replay preference state from latest consent/suppression | Reach owner |
| Data source config change | `data_source_config`, audit event | control + ops | outbox | `DatasourceCredentialChanged` / `AuditEventRecorded` | `datasource:{id}:version:{updatedAt}` | compare data source rows to audit trail | Integration owner |
| Audit event write | domain write plus `canvas_audit_log` or compliance evidence | source group + ops | outbox | `AuditEventRecorded` | `audit:{source}:{sourceId}:{action}` | audit gap scan by source ID | Compliance owner |
| Warehouse sync/backfill | `cdp_warehouse_sync_run`, watermarks, Doris updates | analytics + external OLAP | reconciliation | `WarehouseSyncCompleted` | `warehouse:{job}:{windowStart}:{windowEnd}` | rerun by watermark and compare row counts | Data platform owner |
| BI publish approval | `bi_*` resources, `bi_publish_approval`, permission rows | analytics | same-datasource until BI split | `BiResourcePublished` | `bi:{type}:{key}:version:{version}` | compare published resource to approval record | BI owner |
| Async task notification | `async_task`, `notification` | ops + CDP/customer notification | outbox | `TaskNotificationRequested` | `task:{taskId}:notification` | compare task terminal state to notification row | Platform owner |

## Blocked Flows

The following flows are blocked for datasource movement:

- Canvas publish route/cache mutation until trigger-route rebuild and rollback are explicit.
- Execution trigger to CDP user creation until `CdpUserObserved` event or read model replaces direct service calls.
- Runtime-created notification alerts until `SystemAlertRequested` replaces direct service calls.
- Trace movement until payload masking and PII deletion propagation are test-covered.
- BI and warehouse movement until tenant, row/column permission, lineage, and retention gates are closed.

## Required Evidence Per Cross-Group Write

Every cross-group write needs:

- event contract name and schema version;
- idempotency key;
- retry and DLQ behavior;
- reconciliation command;
- rollback owner;
- tenant propagation proof;
- trace/correlation propagation proof;
- stop condition when reconciliation fails.
