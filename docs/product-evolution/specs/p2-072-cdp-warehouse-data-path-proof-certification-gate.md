# P2-072 - CDP Warehouse Data Path Proof Certification Gate Spec

Priority: P2
Sequence: 072
Source: `docs/product-evolution/specs/p2-066-cdp-warehouse-physical-e2e-certification.md`, `docs/product-evolution/specs/p2-069-cdp-warehouse-realtime-physical-e2e-certification.md`, `docs/product-evolution/specs/p2-071-cdp-warehouse-synthetic-ods-data-path-proof.md`
Implementation plan: `../plans/p2-072-cdp-warehouse-data-path-proof-certification-gate-plan.md`

## Goal

Wire P2-071 synthetic ODS data-path proof into physical E2E certification, persisted certification history, scheduled certification, and production gates so production promotion can require evidence that a CDP event traversed the warehouse sink and became query-visible in Doris ODS.

## Current Baseline

- P2-066 certifies production readiness, Doris JDBC connectivity, and live physical table contracts.
- P2-069 adds realtime pipeline and job heartbeat evidence to certification and gates.
- P2-071 can run a synthetic ODS data-path proof and persist its own proof run.
- P2-071 is still a standalone operator API; production gates can pass without requiring that synthetic data-path evidence.

## In Scope

- Add `requireDataPathProof` to immediate physical E2E certification.
- Add synthetic ODS proof evidence key `synthetic_ods_data_path`.
- Persist `require_data_path_proof` and `data_path_proof_json` in certification run history.
- Add `requireDataPathProof` to scheduled certification configuration.
- Add `requireDataPathProof` to production gate matching.
- Keep existing Java overloads source-compatible by defaulting the new flag to `false`.
- Default HTTP API, persisted run API, scheduler, and gate requests to `requireDataPathProof=true`.
- Add focused tests without requiring real Doris.

## Out Of Scope

- Replacing P2-071 proof generation logic.
- Running DWD/DWS aggregation on synthetic rows.
- Cleaning synthetic ODS rows.
- Requiring data-path proof in BI query execution or audience materialization gates; those still consume certification/gate results indirectly.
- UI.

## Runtime Semantics

1. When `requireDataPathProof=true`, certification runs P2-071 with strict mode.
2. Data-path evidence is PASS only when the P2-071 proof status is PASS.
3. Data-path evidence is FAIL when P2-071 is missing, unavailable, throws, or returns FAIL/WARN in strict certification mode.
4. When `requireDataPathProof=false`, certification skips synthetic writes and does not produce PASS data-path evidence.
5. Overall certification status remains worst evidence status: FAIL beats WARN beats PASS.
6. Persisted certification runs record whether they required data-path proof and store the proof summary JSON.
7. Gates requiring data-path proof only match persisted runs where `require_data_path_proof=1`.
8. Existing callers that do not pass the new flag remain source-compatible.

## Functional Requirements

1. Operators can request immediate certification with explicit data-path proof requirement.
2. Certification output includes `requireDataPathProof`.
3. Certification output includes `synthetic_ods_data_path` evidence.
4. Certification output includes the P2-071 proof summary when the proof runs.
5. Persisted certification history stores the data-path requirement and proof JSON.
6. Scheduled certification can require data-path proof.
7. Gate evaluation rejects runs that did not require data-path proof when the gate request requires it.
8. Controller request binding supports `requireDataPathProof`.

## Technical Scope

- Add migration `backend/canvas-engine/src/main/resources/db/migration/V253__cdp_warehouse_e2e_data_path_proof.sql`
- Modify `CdpWarehouseE2eCertificationRunDO`
- Modify `CdpWarehousePhysicalE2eCertificationService`
- Modify `CdpWarehouseE2eCertificationRunService`
- Modify `CdpWarehouseE2eCertificationGateService`
- Modify `CdpWarehouseE2eCertificationScheduler`
- Modify `CdpWarehousePhysicalE2eCertificationController`
- Modify `CdpWarehouseE2eCertificationRunController`
- Modify `CdpWarehouseE2eCertificationGateController`
- Add or update focused schema, service, scheduler, gate, and controller tests.

## Acceptance Criteria

- P2-072 spec and plan are indexed.
- Migration test proves certification history has `require_data_path_proof` and `data_path_proof_json`.
- Physical certification tests prove PASS when data-path proof is required and P2-071 returns PASS.
- Physical certification tests prove FAIL when data-path proof is required and P2-071 returns WARN/FAIL or is missing.
- Run service tests prove data-path requirement and proof JSON are persisted.
- Gate tests prove data-path-required gates reject runs that did not require data-path proof.
- Scheduler tests prove configured data-path requirement is delegated.
- Controller tests prove `requireDataPathProof` request binding for immediate certification, persisted run trigger, and gate.
- Focused backend tests pass.
- Warehouse regression passes.

## Rollout

1. Deploy migration and code with scheduler disabled.
2. In staging, run P2-071 manual proof until it consistently passes.
3. Run immediate E2E certification with `requirePhysical=true&requireRealtime=true&requireDataPathProof=true`.
4. Run persisted certification and verify the gate only passes for data-path-certified runs.
5. Enable scheduled certification with `canvas.warehouse.e2e-certification-scheduler.require-data-path-proof=true`.

## Rollback

- Temporarily set `requireDataPathProof=false` on diagnostic calls while investigating data-path proof failures.
- Disable scheduled certification if synthetic ODS proof is unstable.
- Existing physical/realtime certification evidence remains available through source-compatible service overloads.
