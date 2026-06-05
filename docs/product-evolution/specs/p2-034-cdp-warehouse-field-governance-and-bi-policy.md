# P2-034 - CDP Warehouse Field Governance And BI Policy Spec

Priority: P2
Sequence: 034
Source: `docs/product-evolution/specs/p2-027-cdp-warehouse-catalog-and-lineage.md`, `docs/product-evolution/specs/p2-031-cdp-warehouse-physical-table-governance.md`, `docs/product-evolution/specs/p2-023-bi-dataset-query-compiler-foundation.md`
Implementation plan: `../plans/p2-034-cdp-warehouse-field-governance-and-bi-policy-plan.md`

## Goal

Add field-level warehouse governance and enforce it before BI query compilation/execution so PII-related CDP fields cannot be queried by unauthorized operators.

## Current Baseline

- P2-027 stores table-level dataset catalog and lineage.
- P2-031 stores Doris physical table contracts.
- V191 includes BI dataset fields and BI column permission tables, but runtime BI query compilation still uses an in-memory dataset registry and does not evaluate field policy.
- P2-023 safely compiles only allowlisted BI fields, but it does not classify or deny PII fields.

## In Scope

- A CDP warehouse field policy table for dataset/field PII classification, usage controls, minimum role, and mask strategy.
- A denied-access audit table for field policy violations.
- Field policy service APIs to list/upsert policies and evaluate BI query requests.
- BI compile and execute gates that reject denied fields before SQL is exposed or executed.
- Built-in policies for CDP ODS/DWD/DWS fields and BI canvas daily stats.

## Out Of Scope

- Returning masked SQL projections or masked BI result sets.
- Full subject-specific BI column permissions; V191 keeps those foundations.
- Column-level lineage graph traversal.
- Data subject request handling.

## Runtime Semantics

1. Tenant `0` field policies are built-in defaults.
2. Tenant-specific policies override built-ins by `(dataset_key, field_key)`.
3. Unknown fields remain governed by the dataset registry allowlist.
4. `DENY` policies always block BI query use.
5. `MASK` and sensitive policies require the configured minimum role until masked result rendering is implemented.
6. Denied BI usage writes an audit row without executing datasource SQL.

## Functional Requirements

1. Field policy rows must preserve dataset key, field key, physical name, column name, value type, semantic type, PII level, access policy, minimum role, allowed usages, mask strategy, status, owner, and description.
2. Policies must be listable and upsertable by tenant.
3. Built-in policies must classify user identifiers and event properties as PII-related.
4. BI compile and execute must evaluate fields used as dimensions, filters, sorts, and metric references.
5. Unauthorized access must throw before SQL is returned or executed.
6. Denied access must be auditable by tenant, dataset, field, actor, role, action, decision, and reason.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V200__cdp_warehouse_field_governance.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseFieldPolicyDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehouseFieldAccessAuditDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseFieldPolicyMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehouseFieldAccessAuditMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseFieldGovernanceService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseFieldGovernanceController.java`
- BI query context/controller/execution integration.

## Acceptance Criteria

- Schema tests prove field policy and access audit tables, indexes, and built-in seeds exist.
- Service tests prove tenant override behavior, upsert validation, role-based allow/deny, and denied audit.
- BI execution tests prove denied fields are rejected before datasource execution and history records failure.
- BI compile controller tests prove denied fields are not returned as SQL.
- Focused backend tests pass.
- Warehouse/BI regression tests pass.

## Rollout

1. Deploy the additive migration.
2. Review tenant-specific overrides for sensitive CDP fields.
3. Enable BI compile/execute policy gates in staging.
4. Monitor denied field audit rows and adjust roles or policies.

## Rollback

- Disable or relax tenant-specific field policies.
- Leave audit rows in place for compliance evidence.
- BI registry allowlisting remains active even if field governance policies are permissive.
