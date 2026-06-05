# Data Platform Source Inventory

Date: 2026-06-05

Status: P3-03 source inventory. This is not approval for a full data platform or a separate data service.

## Scope

This inventory covers core source tables and events for canvas, execution, trace, DLQ, CDP, audience, notification/reach, consent/suppression, and event log data. It follows the boundary gate in `docs/architecture/decisions/adr/ADR-0006-service-extraction-gate.md`.

Freshness targets are proof-of-concept targets, not production SLOs.

## Source Groups

| Source group | Tables or events | Owner context | Ingestion method | freshness target | retention | PII class | deletion behavior | Downstream consumers |
|---|---|---|---|---|---|---|---|---|
| canvas definitions | `canvas`, `canvas_version`, `canvas_template`, `canvas_project`, `canvas_audit_log` | Canvas Authoring | Snapshot table extract or versioned publish event after `CanvasPublished` exists | T+15 minutes for reporting, immediate for active runtime stays OLTP-only | Versions per authoring policy; audit at least 365 days | internal, possible PII in audit detail | Canvas metadata follows authoring deletion; audit follows compliance hold | Canvas stats, project governance, BI catalog |
| execution | `canvas_execution`, `canvas_execution_request`, `canvas_execution_stats`, `execution_rerun_audit`, `execution_retention_*` | Execution Runtime | Incremental pull by `updated_at` and `perf_run_id`; future CDC after contract | T+5 minutes for POC | `canvas_execution` 180 days online; request 90 days; stats 730 days | internal, possible PII in user and payload refs | Retention job archives/deletes terminal rows; user erasure must remove user-scoped rows | Execution funnel, audience compute attribution, ops capacity |
| trace | `canvas_execution_trace` | Execution Runtime | Excluded from first POC; later masked trace extract only | Not in first POC | 30 days online | possible PII in input/output/error | `DataDeletionService` deletes matching tenant/user trace rows | Debug analytics and incident review only |
| DLQ | `canvas_execution_dlq` | Execution Runtime | Incremental pull by `failed_at`, `perf_run_id`, status | T+5 minutes for ops summary | 90 days after resolution | internal, possible PII in trigger payload | Delete after replay/incident closure; user erasure applies to payload references | Failure funnel and replay operations |
| CDP profile/identity/tag | `cdp_user_profile`, `cdp_user_identity`, `cdp_user_tag`, `cdp_user_tag_history`, `cdp_tag_operation` | CDP / Audience | Snapshot plus change event after `CdpProfileUpdated` and `CdpTagChanged` contracts exist | T+15 minutes for POC read model | Profile while active; tag TTL/history per compliance policy | PII / internal | `DataDeletionService` deletes profile, identity, current tags; history needs retention/legal-hold rule | Audience compute, reach contactability, segmentation |
| audience | `audience_definition`, `audience_stat`, `audience_compute_run`, `audience_bitmap_version`, `audience_materialization_run`, `audience_quality_check` | CDP / Audience | Incremental pull by `updated_at`; event on compute completion for POC | T+5 minutes for audience compute history | Compute runs retained for capacity and proof window; bitmap retention by audience policy | internal, possible PII in rule JSON/config | Delete audience definition/stat/bitmap on audience delete; compute ledger retained until retention cutoff | Audience compute history POC, operator reporting |
| notification/reach | `notification`, `message_send_record`, `delivery_outbox`, `delivery_receipt_log` | Reach / Notification | Incremental pull by `created_at`/`updated_at`; event after delivery contracts exist | T+5 minutes for delivery reporting | Delivery dispute/incident window then compact/delete | PII / compliance evidence | `DataDeletionService` deletes message send rows by tenant/user; notification retention follows user scope | Reach delivery report and effect closure |
| consent/suppression | `marketing_consent`, `marketing_suppression`, `customer_channel`, `customer_profile` | Reach / Notification and CDP / Audience split | Snapshot with masking and legal-hold filters | T+15 minutes, excluded from first POC joins | Consent evidence audit window; suppression until expiry plus audit window | PII / compliance evidence | User erasure unless legal evidence requires tombstone | Contactability, compliance reporting |
| event log data | `event_log`, `analytics_event`, `analytics_event_trace`, CDP event logs | CDP / Audience and Data Platform / Analytics | Incremental pull by `created_at` and `perf_run_id`; event contract for reported events | T+5 minutes for first POC if used | `event_log` 30 days online then archive | possible PII in attributes | Raw attributes follow CDP policy and deletion propagation | Event funnels, trigger attribution, analytics |
| warehouse/BI | `cdp_warehouse_*`, `analytics_*`, `bi_*`, AI analytics tables | Data Platform / Analytics | Excluded from first POC as platform-internal state | Not in first POC | Per future P3-03/P3-07 decision | mixed | Must inherit source deletion and retention | BI, warehouse readiness, semantic metrics |

## First Proof-Of-Concept Inclusion

The first proof of concept includes:

- `audience_definition`
- `audience_stat`
- `audience_compute_run`
- `canvas_execution` only by aggregate keys such as `perf_run_id`, `canvas_id`, status, and timestamps
- `event_log` only when `perf_run_id` is needed to connect event inputs to audience compute runs

The first proof of concept excludes:

- raw `canvas_execution_trace` payloads;
- raw CDP identity values such as phone, email, device ID, and open ID;
- raw message payloads from `message_send_record`;
- `delivery_outbox` provider payloads;
- full BI and warehouse governance tables;
- external datasource row data queried by audience JDBC rules.

## Event Sources

Existing event and MQ anchors:

- `MqTriggerMessage` for external MQ-triggered canvas execution.
- `DeliveryOutboxConsumer` for delivery outbox wakeups on `CANVAS_DELIVERY`.
- `NotificationRealtimePublisher` and Redis notification events for notification fanout.
- Future data-platform events should be explicit contracts rather than direct reads from runtime services.

The first POC may use table polling. CDC or event streaming requires a separate ADR and must not change the online execution path.
