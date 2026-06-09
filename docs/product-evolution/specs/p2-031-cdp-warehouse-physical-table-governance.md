# P2-031 - CDP Warehouse Physical Table Governance Spec

Priority: P2
Sequence: 031
Source: `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`, `docs/product-evolution/specs/p2-027-cdp-warehouse-catalog-and-lineage.md`
Implementation plan: `../plans/p2-031-cdp-warehouse-physical-table-governance-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add physical table governance for the CDP warehouse so Doris ODS/DWD/DWS tables have explicit production contracts for partitioning, retention, bucket count, replica count, distribution keys, and DDL asset drift checks.

## Current Baseline

- P2-022 through P2-030 provide warehouse ingestion, backfill, aggregation, operations, retry, quality, catalog, lineage, scheduler lease, checkpoint, lag, and incident loops.
- Existing Doris DDL assets create the required ODS/DWD/DWS tables, but the physical shape is not governed as a durable contract.
- Catalog/lineage records describe datasets, but they do not verify whether Doris table DDL can survive production scale.

## In Scope

- A tenant-scoped physical table contract table with built-in contracts for core Doris warehouse assets.
- A durable table inspection ledger.
- Contract APIs to list and upsert physical table governance rows.
- Inspection APIs to validate checked-in Doris DDL assets against the saved contracts.
- Doris DDL upgrades for dynamic partitions, retention windows, replica count, and bucket governance.

## Out Of Scope

- Live Doris `SHOW CREATE TABLE` drift detection.
- Automatic ALTER TABLE remediation.
- Kafka, CDC, Flink, or exactly-once runtime state.
- Column-level policy enforcement.

## Runtime Semantics

1. Built-in table contracts are tenant `0` defaults.
2. Tenant-specific contracts can override the built-in row with the same `table_key`.
3. Inspections read the configured DDL asset path and evaluate it against the contract.
4. Inspection rows are append-only and update the latest status summary on the contract.
5. Missing assets or missing tables fail the inspection. Missing partition, retention, replica, bucket, or distribution settings produce violations.

## Functional Requirements

1. The service must persist physical table contracts for table key, dataset key, layer, physical name, engine type, DDL asset path, partition column, partition granularity, retention days, replica count, bucket count, distribution columns, owner, lifecycle status, and expected properties.
2. Built-in contracts must cover:
   - `canvas_ods.cdp_event_log`
   - `canvas_dwd.cdp_user_event_fact`
   - `canvas_dws.user_event_metric_daily`
   - `canvas_ods.canvas_execution_trace`
   - `canvas_dws.canvas_daily_stats`
   - `canvas_dws.node_daily_stats`
3. Contract listing must merge tenant `0` defaults with tenant overrides.
4. Contract inspection must check DDL asset existence, table name, partition range column, dynamic partition enablement, retention start, replica count, bucket count, and distribution columns.
5. Every inspection must write a durable inspection row and update latest inspection metadata on the contract.
6. APIs must be tenant scoped and bounded to current tenant context.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V198__cdp_warehouse_table_contract.sql`
- `backend/canvas-engine/src/main/resources/infrastructure/doris/cdp-audience-ddl.sql`
- `backend/canvas-engine/src/main/resources/infrastructure/doris/trace-ddl.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseTableContractDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseTableInspectionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseTableContractMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseTableInspectionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseTableGovernanceService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableGovernanceController.java`

## Acceptance Criteria

- Schema tests prove contract and inspection tables, indexes, and built-in contract seeds exist.
- DDL asset tests prove core Doris tables define range partitioning, dynamic partition retention, bucket count, and replica count.
- Service tests prove contract upsert validation, tenant override list behavior, successful inspections, and violation recording.
- Controller tests prove tenant-scoped list, upsert, single inspection, and all-table inspection endpoints.
- Focused backend tests pass for P2-031 and existing warehouse regressions.

## Rollout

1. Deploy the additive MySQL migration.
2. Apply updated Doris DDL to staging or compare existing staging DDL through inspection first.
3. Run `/warehouse/tables/inspect` and confirm no production-critical violations.
4. Keep inspection output visible in warehouse operations dashboards before enabling larger realtime/CDP jobs.

## Rollback

- Stop calling table governance APIs.
- Leave contract and inspection tables in place for audit.
- Doris ingestion, backfill, aggregation, quality, and incident loops continue independently.
