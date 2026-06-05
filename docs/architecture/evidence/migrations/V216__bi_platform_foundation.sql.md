# V216 BI Platform Foundation

## Backup

Capture existing BI platform tables if they exist:

```bash
mysqldump --single-transaction canvas bi_workspace bi_workspace_member bi_data_source_ref bi_dataset bi_dataset_field bi_dataset_relation bi_metric bi_chart bi_dashboard bi_dashboard_widget bi_portal bi_portal_menu bi_query_history bi_export_job bi_subscription bi_alert_rule > bi-platform-before-v216.sql || true
```

## Restore

If rollback is required before users create BI assets, drop the new BI tables and restore the previous dump:

```bash
mysql canvas -e "DROP TABLE IF EXISTS bi_alert_rule, bi_subscription, bi_export_job, bi_query_history, bi_portal_menu, bi_portal, bi_dashboard_widget, bi_dashboard, bi_chart, bi_metric, bi_dataset_relation, bi_dataset_field, bi_dataset, bi_data_source_ref, bi_workspace_member, bi_workspace"
mysql canvas < bi-platform-before-v216.sql || true
```

## Dry run

Run the migration chain on MySQL 8:

```bash
cd backend && mvn -pl canvas-engine -am -P integration-tests -Dapi.version=1.54 -Dtest=CanvasExecutionRequestServiceIdempotencyIntegrationTest test
```

## Rollback owner

BI platform owner with Backend/DBA support. Coordinate rollback with report, dashboard, and export feature owners.
