# CDP Warehouse Field Governance And BI Policy Implementation Plan

**Goal:** Add field-level PII governance and enforce it before BI query SQL is returned or executed.

**Architecture:** Store warehouse field policies in MySQL with tenant-default merging. BI compile and execute paths call the governance service before returning SQL or hitting Doris/MySQL. Denied access is audited. Existing V191 BI field/permission foundations remain intact; this slice adds the runtime enforcement layer for the current registry-backed BI query path.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ.

## Scope

- Add `cdp_warehouse_field_policy` and `cdp_warehouse_field_access_audit`.
- Seed policies for CDP ODS/DWD/DWS and `canvas_daily_stats`.
- Add field governance DAL/service/controller.
- Extend `BiQueryContext` with role.
- Enforce field policies in BI compile and execute paths.
- Add schema, service, BI, and controller tests.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-034 docs.

- [x] **Step 2: Add schema and seeds**

Create `V200__cdp_warehouse_field_governance.sql` with policies and denied-access audit.

- [x] **Step 3: Add DAL and governance service**

Implement field policy upsert/list/evaluate, tenant override merge, role ranking, and denied audit.

- [x] **Step 4: Integrate BI compile and execute gates**

Pass tenant role through `BiQueryContext`; call the field governance service before returning compiled SQL or executing a query.

- [x] **Step 5: Add controller APIs**

Expose `/warehouse/fields/policies` and `/warehouse/fields/evaluate-bi-query`.

- [x] **Step 6: Add tests and verify**

Run:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CdpWarehouseFieldGovernanceSchemaTest,CdpWarehouseFieldGovernanceServiceTest,CdpWarehouseFieldGovernanceControllerTest,BiQueryExecutionServiceTest,BiQueryControllerTest test
```

Run compile:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -DskipTests compile
```

## Verification

- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] P2-034 focused tests passed: `25 tests, 0 failures`.
- [x] Warehouse/BI regression passed: `171 tests, 0 failures, 1 skipped`.
