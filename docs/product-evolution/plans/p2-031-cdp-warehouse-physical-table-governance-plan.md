# CDP Warehouse Physical Table Governance Implementation Plan

**Goal:** Make Doris physical table design a durable, inspectable warehouse contract rather than unchecked SQL assets.

**Architecture:** Store built-in and tenant-specific table contracts in MySQL. Inspect configured DDL assets against each contract and append inspection evidence. Keep inspection read-only: this slice detects drift and production-readiness gaps, while later slices can add live Doris introspection and ALTER remediation.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Doris DDL assets, JUnit 5, Mockito, AssertJ.

## Scope

- Add `cdp_warehouse_table_contract` and `cdp_warehouse_table_inspection`.
- Seed built-in contracts for six core Doris ODS/DWD/DWS tables.
- Upgrade Doris DDL assets with range partitioning, dynamic partition retention, bucket settings, and replica count.
- Add `CdpWarehouseTableGovernanceService`.
- Add `/warehouse/tables` APIs.
- Add schema, service, controller, and DDL contract tests.

## Tasks

- [x] **Step 1: Add schema and seed contracts**

Create `V198__cdp_warehouse_table_contract.sql` with table contracts, inspection ledger, indexes, and built-in contract rows.

- [x] **Step 2: Add Doris physical governance to DDL assets**

Update `cdp-audience-ddl.sql` and `trace-ddl.sql` so each core table has `PARTITION BY RANGE`, dynamic partition properties, expected buckets, and `replication_num`.

- [x] **Step 3: Add DAL records and mappers**

Add contract and inspection DO/mapper classes. Contract mapper needs upsert and latest-inspection update helpers.

- [x] **Step 4: Add governance service**

Implement list/upsert/inspect-one/inspect-all. Merge tenant defaults with overrides and evaluate asset DDL against each physical contract.

- [x] **Step 5: Add operator API**

Expose tenant-scoped contract listing, contract upsert, single-table inspection, and all-table inspection under `/warehouse/tables`.

- [x] **Step 6: Add tests and verify**

Add focused tests:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CdpWarehouseTableGovernanceSchemaTest,CdpWarehouseTableGovernanceServiceTest,CdpWarehouseTableGovernanceControllerTest test
```

Also run compile:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -DskipTests compile
```

## Verification

- [x] Main compile passed with `mvn -pl canvas-engine -DskipTests compile`.
- [x] P2-031 focused tests passed: `13 tests, 0 failures`.
- [x] Warehouse/BI/Doris regression passed: `106 tests, 0 failures`.
