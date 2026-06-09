# P2-046 - CDP Warehouse Realtime Schema Evolution Guard Spec

Priority: P2
Sequence: 046
Source: `docs/product-evolution/specs/p2-032-cdp-warehouse-realtime-pipeline-runtime.md`, `docs/product-evolution/specs/p2-033-cdp-warehouse-realtime-pipeline-incidents.md`, `docs/product-evolution/specs/p2-031-cdp-warehouse-physical-table-governance.md`
Implementation plan: `../plans/p2-046-cdp-warehouse-realtime-schema-evolution-guard-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add realtime warehouse schema-version registration and compatibility guards so CDC/Flink/Kafka-style pipelines cannot silently push breaking source or sink schema changes into CDP warehouse tables.

## Current Baseline

- P2-032 records realtime pipeline contracts, checkpoints, offsets, lag, and runtime health.
- P2-033 routes realtime pipeline WARN/FAIL status into incidents.
- P2-031 governs physical Doris table contracts.
- Checkpoint reports still do not carry source/sink schema versions.
- There is no durable schema-version ledger or compatibility decision for realtime pipeline schema evolution.

## In Scope

- Add realtime pipeline schema version registry for `SOURCE` and `SINK` roles.
- Add compatibility evaluation for registered schema versions.
- Add source/sink schema version fields to checkpoint evidence.
- Integrate schema guard results into checkpoint runtime status.
- Expose tenant-scoped APIs to register and inspect schema versions.
- Add focused schema/service/controller/pipeline tests.

## Out Of Scope

- Deploying Confluent Schema Registry, Debezium, Flink, or Kafka Connect.
- Full Avro/Protobuf compatibility parsing.
- Applying DDL to Doris.
- Column-level lineage.
- UI.

## Runtime Semantics

1. Schema versions are scoped by tenant, pipeline key, schema role, and schema version.
2. Schema role must be `SOURCE` or `SINK`.
3. Schema JSON must include a `fields` array of `{name,type,nullable}` objects.
4. The first version for a pipeline role is `COMPATIBLE`.
5. With `BACKWARD` policy, removing a previous field, changing a previous field type, or adding a new non-null field is `BREAKING`.
6. `NONE` policy stores the schema and marks it `COMPATIBLE` without comparing to the previous version.
7. Checkpoint reports may include `sourceSchemaVersion` and `sinkSchemaVersion`.
8. Unknown reported schema versions produce checkpoint `WARN`.
9. Reported `BREAKING` schema versions produce checkpoint `FAIL`.
10. Schema guard failures remain part of the existing realtime pipeline incident flow through P2-033.

## Functional Requirements

1. Operators can register source or sink schema versions for a realtime pipeline.
2. Registered schema rows store schema hash, JSON, compatibility policy, compatibility status, reasons, active flag, operator, and timestamps.
3. Operators can list schema versions and inspect latest active schema by pipeline and role.
4. Checkpoint evidence stores reported source and sink schema versions plus schema status.
5. Checkpoint runtime evaluation includes schema reasons and escalates to `WARN` or `FAIL`.
6. Existing pipeline checkpoint semantics for exactly-once, lag, and failure reports remain intact.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V205__cdp_warehouse_realtime_schema_evolution.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamSchemaDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamSchemaMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseStreamCheckpointDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseStreamCheckpointMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimeSchemaService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseRealtimePipelineService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeSchemaController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimePipelineController.java`
- Focused tests for schema, compatibility, checkpoint integration, and controller tenant scoping.

## Acceptance Criteria

- P2-046 spec and plan are indexed.
- Migration is additive and does not edit old Flyway migrations.
- Schema registration detects compatible and breaking changes.
- Checkpoint reports persist schema versions and schema status.
- Breaking schema checkpoint reports evaluate to `FAIL`.
- Unknown schema checkpoint reports evaluate to `WARN`.
- Focused backend tests pass.
- Warehouse/BI/audience regression passes.

## Rollout

1. Deploy with no external job changes; schema guard is passive until versions are reported.
2. Register source and sink schema version `1` for each realtime pipeline.
3. Update external jobs to include `sourceSchemaVersion` and `sinkSchemaVersion` in checkpoint reports.
4. Use `/warehouse/realtime/schemas` before production schema changes.
5. Let existing realtime pipeline incident automation surface schema WARN/FAIL states.

## Rollback

- Stop sending schema versions in checkpoint reports.
- Existing checkpoint, lag, and incident behavior remains available.
- Registered schema rows remain as audit evidence.
