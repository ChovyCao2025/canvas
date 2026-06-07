# P2-071 - CDP Warehouse Synthetic ODS Data Path Proof Spec

Priority: P2
Sequence: 071
Source: `docs/product-evolution/specs/p2-022-cdp-warehouse-ingestion-and-aggregation.md`, `docs/product-evolution/specs/p2-066-cdp-warehouse-physical-e2e-certification.md`, `docs/product-evolution/specs/p2-069-cdp-warehouse-realtime-physical-e2e-certification.md`
Implementation plan: `../plans/p2-071-cdp-warehouse-synthetic-ods-data-path-proof-plan.md`

## Goal

Add a tenant-scoped synthetic ODS data-path proof that writes one reserved CDP event through an explicit source mode, reads it back from Doris ODS through JDBC, and persists the proof as auditable warehouse evidence.

## Current Baseline

- P2-022 can mirror accepted CDP events to Doris ODS through `CdpWarehouseEventSink`.
- P2-066 proves Doris JDBC connectivity and live table contracts, but explicitly excludes writing synthetic events.
- P2-069 proves realtime pipeline and job evidence, but still does not prove that a CDP event can traverse the actual sink-to-ODS data path.
- Existing aggregation jobs can process ODS rows into DWD/DWS, but running aggregation with synthetic rows would pollute downstream metric tables unless a cleanup and exclusion model is added later.

## In Scope

- Add an operator API to run a synthetic ODS data-path proof for the current tenant.
- Generate a bounded reserved event:
  - `event_code=__warehouse_probe__` by default
  - unique `message_id`
  - unique synthetic `user_id`
  - JSON properties containing `synthetic=true`, `probeKey`, and `messageId`
- Support source modes:
  - `DIRECT_SINK`: write through the existing `CdpWarehouseEventSink` and validate the direct Doris Stream Load fallback path.
  - `MYSQL_CDC`: insert into `cdp_event_log`, skip direct Stream Load, and require the MySQL CDC/Flink path to make the row visible in Doris ODS.
- Verify Doris ODS visibility with bounded JDBC read attempts.
- Persist proof runs in a MySQL audit table.
- Return step-level evidence for source write, sink write, and ODS read.
- Fail closed when strict mode is enabled and the selected source write, Doris JDBC, or ODS read proof fails.
- Add focused tests that mock the sink and Doris JDBC; no real Doris is required.

## Out Of Scope

- Public CDP ingestion API calls.
- Mutating `cdp_event_log` MySQL audit data outside `sourceMode=MYSQL_CDC` synthetic proof runs.
- Running DWD/DWS aggregation on synthetic probe rows.
- Cleaning Doris ODS rows.
- Replacing P2-022 backfill/aggregation logic.
- Adding this proof to P2-069 certification gates; that is a later wiring slice.
- UI.

## Runtime Semantics

1. The proof is tenant scoped.
2. A probe run always creates a persisted run row before writing to the requested source mode.
3. `DIRECT_SINK` writes through `CdpWarehouseEventSink`; `MYSQL_CDC` writes to `cdp_event_log` and skips the direct Doris Stream Load sink.
4. The event is read back through Doris JDBC from `canvas_ods.cdp_event_log`.
5. ODS proof passes only when at least one row matches tenant, message id, and event code.
6. Missing Doris JDBC, sink failure, ODS read failure, or zero matching ODS rows produces `FAIL` in strict mode.
7. Non-strict dry-run mode records the same evidence but allows `WARN` for missing Doris JDBC or zero matching rows.
8. Error messages and evidence JSON are bounded.
9. The default proof does not call aggregation to avoid downstream metric pollution.

## Functional Requirements

1. Operators can run a synthetic ODS data-path proof manually.
2. Operators can list recent proof runs for the current tenant.
3. The proof records source mode and source status.
4. `DIRECT_SINK` writes through the existing warehouse sink abstraction.
5. `MYSQL_CDC` writes to `cdp_event_log` and requires the CDC/Flink path for ODS visibility.
6. The proof reads back from Doris ODS with bounded attempts.
7. The proof persists status, source mode, source status, message id, event code, synthetic user id, step evidence, and error details.
8. Strict proofs fail when Doris JDBC is unavailable.
9. Strict proofs fail when the selected source write fails.
10. Strict proofs fail when ODS read returns zero rows.
11. Dry-run proofs retain diagnostic evidence without claiming production PASS on missing physical proof.

## Technical Scope

- Add migration `backend/canvas-engine/src/main/resources/db/migration/V251__cdp_warehouse_synthetic_ods_data_path_probe.sql`
- Add `CdpWarehouseSyntheticDataPathProbeRunDO`
- Add `CdpWarehouseSyntheticDataPathProbeRunMapper`
- Add `CdpWarehouseSyntheticDataPathProbeService`
- Add `CdpWarehouseSyntheticDataPathProbeController`
- Add focused schema, service, and controller tests.

## Acceptance Criteria

- P2-071 spec and plan are indexed.
- Migration test proves the synthetic data-path probe run table exists.
- Service tests prove PASS when sink write succeeds and Doris ODS read returns a row.
- Service tests prove FAIL when strict mode has no Doris JDBC.
- Service tests prove FAIL when sink write throws.
- Service tests prove `MYSQL_CDC` writes the source table and skips direct Stream Load.
- Service tests prove WARN in dry-run mode when ODS read returns no row.
- Controller tests prove run and recent-list request binding and tenant scoping.
- Focused backend tests pass.
- Warehouse regression passes.

## Rollout

1. Deploy migration and code.
2. Ensure Doris ODS table contracts are present.
3. Run `/warehouse/data-path-probes/synthetic-ods/run?strict=true` in staging for direct sink proof.
4. For realtime Flink proof, run `/warehouse/data-path-probes/synthetic-ods/run?sourceMode=MYSQL_CDC&strict=true`.
5. Inspect the persisted run and verify P2-066/P2-069 evidence still passes.
6. Run the proof on a low cadence as an operator check until a later slice wires it into E2E certification gates.

## Rollback

- Stop calling the synthetic ODS proof API.
- Existing P2-022 sink, backfill, aggregation, P2-066 certification, and P2-069 realtime certification remain unchanged.
