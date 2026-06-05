# Datasource Ownership Map

Date: 2026-06-05

Status: P3-04 planning artifact. This map does not approve physical datasource split.

## Target Groups

| Group | Owner package | tenant-scope rule | PII class | retention expectation | backup owner |
|---|---|---|---|---|---|
| control | `domain/canvas`, `domain/meta`, integration definition controllers | Tenant scoped for canvas/project/config rows; global rows require explicit system context. | internal, possible PII in audit/config metadata | Keep active authoring metadata while canvas exists; versions and audit per authoring policy. | Backend owner plus DBA |
| runtime | `engine/*`, `domain/execution`, runtime controllers | Tenant ID must be persisted on execution/request/trace/DLQ rows; Redis context must carry tenant in payload before split. | internal, possible PII in payload/trace | Execution 180 days, trace 30 days, request 90 days, DLQ 90 days after resolution. | Runtime owner plus DBA |
| CDP/customer | `domain/cdp`, `engine/audience`, `domain/notification`, `engine/delivery`, reach policy services | Every profile, identity, audience, consent, channel, notification, and send row must be tenant scoped before movement. | PII / compliance evidence / internal | CDP active lifecycle, consent audit window, delivery dispute window, audience compute proof window. | CDP/Reach owner plus compliance |
| analytics | `domain/analytics`, `domain/warehouse`, `domain/bi`, `domain/ai`, `infrastructure/doris` | Derived rows must preserve source tenant or explicit system context and lineage. | mixed, must inherit source PII class | Per P3-03 data platform and BI retention policy; raw PII excluded unless approved. | Data platform owner plus DBA |
| ops | `auth`, `domain/tenant`, `domain/task`, `domain/compliance`, `strategy/*`, release/ops controllers | Tenant/admin/system context must be explicit; global ops rows require owner approval. | credential / compliance evidence / internal | Audit 365 days minimum; deployment/compliance evidence per governance. | Platform/operations owner |

## Complete Table Assignment

### control

Tables:

- `ab_experiment`
- `ab_experiment_group`
- `api_definition`
- `canvas`
- `canvas_audit_log`
- `canvas_control_group_holdout`
- `canvas_conversion_attribution`
- `canvas_manual_approval`
- `canvas_project`
- `canvas_project_folder`
- `canvas_project_member`
- `canvas_schedule`
- `canvas_template`
- `canvas_user_quota`
- `canvas_version`
- `connected_content_cache`
- `context_field`
- `data_source_config`
- `event_attr_definition`
- `event_definition`
- `identity_type`
- `mq_message_definition`
- `node_type_registry`
- `tag_definition`
- `tag_import_batch`
- `tag_import_error`
- `tag_import_source`
- `tag_value_definition`
- `test_user`
- `test_user_set`
- `user_input_form`
- `user_input_response`
- `user_input_resume_audit`
- `webhook_delivery_log`
- `webhook_subscription`

Cannot move until P0 data-security and tenant-isolation criteria are met:

- `data_source_config`, because it stores encrypted credentials and requires audit plus secret rotation.
- `canvas_audit_log`, because it is compliance evidence and must retain legal-hold semantics.
- `canvas`, `canvas_version`, and `canvas_project*`, because authoring and runtime still share graph and publish state.

### runtime

Tables:

- `canvas_execution`
- `canvas_execution_dlq`
- `canvas_execution_request`
- `canvas_execution_stats`
- `canvas_execution_trace`
- `canvas_mq_trigger_rejected`
- `canvas_node_funnel_stats`
- `canvas_wait_subscription`
- `execution_rerun_audit`
- `execution_retention_archive_manifest`
- `execution_retention_policy`
- `execution_retention_run`
- `node_side_effect_idempotency`

Cannot move until P0 data-security and tenant-isolation criteria are met:

- `canvas_execution_trace`, because trace payloads may contain PII.
- `canvas_execution_request` and `canvas_execution_dlq`, because retry/replay correctness depends on idempotency and tenant scope.
- `canvas_execution`, because Canvas Authoring and CDP still read it directly in current services.

### CDP/customer

Tables:

- `audience_bitmap_rollback`
- `audience_bitmap_version`
- `audience_compute_run`
- `audience_definition`
- `audience_demo_user`
- `audience_materialization_run`
- `audience_quality_check`
- `audience_stat`
- `cdp_audience_snapshot`
- `cdp_computed_profile_attribute`
- `cdp_computed_profile_run`
- `cdp_computed_tag_definition`
- `cdp_computed_tag_dependency`
- `cdp_computed_tag_run`
- `cdp_event_log`
- `cdp_profile_attribute_change_log`
- `cdp_realtime_audience_event_log`
- `cdp_tag_operation`
- `cdp_user_identity`
- `cdp_user_index`
- `cdp_user_profile`
- `cdp_user_tag`
- `cdp_user_tag_history`
- `cdp_write_key`
- `channel_connector`
- `channel_dedupe_record`
- `channel_fallback_decision`
- `channel_fallback_policy`
- `channel_provider_limit`
- `customer_channel`
- `customer_points_ledger`
- `customer_profile`
- `customer_tag`
- `customer_task_record`
- `delivery_outbox`
- `delivery_receipt_log`
- `marketing_consent`
- `marketing_frequency_counter`
- `marketing_suppression`
- `message_send_record`
- `notification`

Cannot move until P0 data-security and tenant-isolation criteria are met:

- `cdp_user_profile`, `cdp_user_identity`, `customer_channel`, and `message_send_record`, because they contain PII.
- `marketing_consent` and `marketing_suppression`, because they are compliance evidence.
- `delivery_outbox`, because duplicate provider sends require idempotency and reconciliation proof.

### analytics

Tables:

- `ai_model_registry`
- `ai_prediction_run`
- `ai_prompt_template`
- `ai_provider`
- `ai_usage_audit`
- `ai_user_prediction_snapshot`
- `analytics_alert_rule`
- `analytics_event`
- `analytics_event_trace`
- `analytics_export_job`
- `analytics_funnel_definition`
- `analytics_retention_policy`
- `analytics_retention_run`
- `bi_alert_rule`
- `bi_audit_log`
- `bi_chart`
- `bi_chart_version`
- `bi_column_permission`
- `bi_dashboard`
- `bi_dashboard_version`
- `bi_dashboard_widget`
- `bi_data_source_ref`
- `bi_dataset`
- `bi_dataset_field`
- `bi_dataset_relation`
- `bi_dataset_version`
- `bi_delivery_attachment`
- `bi_delivery_log`
- `bi_delivery_scheduler_lease`
- `bi_embed_token`
- `bi_export_job`
- `bi_metric`
- `bi_portal`
- `bi_portal_menu`
- `bi_portal_version`
- `bi_publish_approval`
- `bi_query_history`
- `bi_resource_comment`
- `bi_resource_favorite`
- `bi_resource_location`
- `bi_resource_lock`
- `bi_resource_ownership`
- `bi_resource_permission`
- `bi_row_permission`
- `bi_subscription`
- `bi_workspace`
- `bi_workspace_member`
- `cdp_warehouse_asset_availability`
- `cdp_warehouse_consumer_availability_contract`
- `cdp_warehouse_dataset_catalog`
- `cdp_warehouse_e2e_certification_run`
- `cdp_warehouse_external_realtime_job_probe_target`
- `cdp_warehouse_field_access_audit`
- `cdp_warehouse_field_policy`
- `cdp_warehouse_incident`
- `cdp_warehouse_job_lease`
- `cdp_warehouse_lineage_edge`
- `cdp_warehouse_metric_change_review`
- `cdp_warehouse_quality_check`
- `cdp_warehouse_realtime_checkpoint`
- `cdp_warehouse_realtime_retry`
- `cdp_warehouse_slo_policy`
- `cdp_warehouse_stream_checkpoint`
- `cdp_warehouse_stream_job_action`
- `cdp_warehouse_stream_job_instance`
- `cdp_warehouse_stream_pipeline`
- `cdp_warehouse_stream_schema`
- `cdp_warehouse_sync_run`
- `cdp_warehouse_synthetic_data_path_probe_run`
- `cdp_warehouse_table_contract`
- `cdp_warehouse_table_inspection`
- `cdp_warehouse_watermark`
- `event_log`

Cannot move until P0 data-security and tenant-isolation criteria are met:

- `analytics_event`, `analytics_event_trace`, and `event_log`, because event attributes may contain PII.
- all `bi_*` tables, because row/column permissions and publish approval gates are still being hardened.
- all `cdp_warehouse_*` tables, because they require source lineage and deletion propagation.

### ops

Tables:

- `architecture_deployment_evidence`
- `async_task`
- `async_task_subscription`
- `built_in_plugin_registry`
- `privacy_compliance_evidence`
- `product_led_growth_evidence`
- `regional_expansion_evidence`
- `sys_user`
- `system_option`
- `tenant`

Cannot move until P0 data-security and tenant-isolation criteria are met:

- `sys_user`, because auth and role management need security proof.
- `tenant`, because tenant identity is a platform primitive used by every datasource group.
- `privacy_compliance_evidence`, because it is compliance proof and legal-hold sensitive.

## Movement Rule

No table moves to a separate datasource until:

- the owning group has a named owner and backup owner;
- tenant-scope tests exist;
- PII classification and retention are documented;
- backup and restore owner is named;
- every cross-group write has outbox, saga, reconciliation, or blocked status;
- rollback owner and data reconciliation command are documented.
