# CDP Warehouse Enterprise OLAP Readiness Implementation Plan

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an explicit enterprise OLAP readiness gate that fails closed when Doris and warehouse production operating evidence is missing.

**Architecture:** Add a focused domain service that normalizes existing production proof evidence plus enterprise OLAP operating gates into one PASS/WARN/FAIL summary. Integrate that summary into `CdpWarehouseProductionReadinessProofService` while keeping existing readiness, availability, contract, and privacy evidence intact.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito, AssertJ, shell verification script.

---

## Scope

- Add P2-081 spec, plan, and index rows.
- Add `CdpWarehouseEnterpriseOlapReadinessService`.
- Extend `CdpWarehouseProductionReadinessProofService` with optional enterprise OLAP readiness aggregation.
- Preserve record constructor compatibility for existing tests and callers.
- Add focused tests and a focused verification script.

## Files

- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapReadinessService.java`.
- Create `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapReadinessServiceTest.java`.
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofService.java`.
- Modify `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseProductionReadinessProofServiceTest.java`.
- Create `scripts/verify-enterprise-olap-focus.sh`.
- Modify `docs/product-evolution/IMPLEMENTATION_ORDER.md`.
- Modify `docs/product-evolution/specs/INDEX.md`.
- Modify `docs/product-evolution/plans/INDEX.md`.

## Tasks

- [x] **Step 1: Add spec, plan, and index rows**

Insert P2-081 rows after P2-080 in:

- `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`

Run:

```bash
rg -n "P2-081|p2-081-cdp-warehouse-enterprise-olap-readiness" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-081-cdp-warehouse-enterprise-olap-readiness.md docs/product-evolution/plans/p2-081-cdp-warehouse-enterprise-olap-readiness-plan.md
```

Expected: all five files contain the P2-081 slug or row.

- [x] **Step 2: Write failing enterprise readiness service tests**

Create `CdpWarehouseEnterpriseOlapReadinessServiceTest` with tests for:

- all required gates PASS returns PASS;
- missing `doris_metrics`, `evidence_collection`, `workload_isolation`, `query_slo`, `backup_restore`, `compaction_health`, `ingestion_replay`, and `runbook_drill` returns FAIL with missing gate keys;
- unknown status normalizes to FAIL;
- WARN returns WARN when no gate fails;
- production proof evidence is normalized into `warehouse_readiness`, `window_availability`, `consumer_contracts`, and `privacy_erasure_backlog`.

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CdpWarehouseEnterpriseOlapReadinessServiceTest
```

Expected before implementation: FAIL because the service class does not exist.

- [x] **Step 3: Implement enterprise readiness service**

Implement:

- constants for `PASS`, `WARN`, `FAIL`;
- `evaluate(Long tenantId, List<EnterpriseOlapGate> evidence)`;
- `evaluateFromProductionEvidence(Long tenantId, List<CdpWarehouseProductionReadinessProofService.ProofEvidence> evidence)`;
- aggregation for any `consumer_contract:*` proof evidence into one `consumer_contracts` gate;
- required gate insertion for missing gates with `FAIL`;
- record types `EnterpriseOlapReadiness` and `EnterpriseOlapGate`.

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CdpWarehouseEnterpriseOlapReadinessServiceTest
```

Expected: PASS.

- [x] **Step 4: Write failing production proof integration test**

Extend `CdpWarehouseProductionReadinessProofServiceTest` with a test that injects an enterprise readiness provider returning FAIL, then asserts:

- proof status is `FAIL`;
- `enterpriseOlapReadiness` is not null;
- evidence contains `enterprise_olap_readiness`;
- enterprise service receives the tenant and existing proof evidence.

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CdpWarehouseProductionReadinessProofServiceTest
```

Expected before integration: FAIL because the production proof does not expose enterprise OLAP readiness.

- [x] **Step 5: Integrate enterprise readiness into production proof**

Modify `CdpWarehouseProductionReadinessProofService`:

- add optional `ObjectProvider<CdpWarehouseEnterpriseOlapReadinessService>`;
- add constructor overload preserving current call sites;
- after privacy, operational, and collection-run evidence is added, evaluate enterprise OLAP readiness when the provider is present;
- add proof evidence key `enterprise_olap_readiness`;
- include enterprise status in overall `worstStatus`;
- add `enterpriseOlapReadiness` to the record plus a backward-compatible secondary constructor.

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=CdpWarehouseProductionReadinessProofServiceTest
```

Expected: PASS.

- [x] **Step 6: Add focused verification script**

Create `scripts/verify-enterprise-olap-focus.sh` that:

- sets Java 21;
- runs `CdpWarehouseEnterpriseOlapReadinessServiceTest`;
- runs `CdpWarehouseProductionReadinessProofServiceTest`;
- skips broad Maven test compilation that is currently blocked by unrelated dirty tests.

Run:

```bash
bash -n scripts/verify-enterprise-olap-focus.sh
scripts/verify-enterprise-olap-focus.sh
```

Expected: shell syntax check passes, focused tests pass.

- [x] **Step 7: Final verification**

Run:

```bash
git diff --check
rg -n "TO[D]O|TB[D]" docs/product-evolution/specs/p2-081-cdp-warehouse-enterprise-olap-readiness.md docs/product-evolution/plans/p2-081-cdp-warehouse-enterprise-olap-readiness-plan.md backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapReadinessService.java backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/CdpWarehouseEnterpriseOlapReadinessServiceTest.java
```

Expected: no whitespace errors and no placeholder tokens.
