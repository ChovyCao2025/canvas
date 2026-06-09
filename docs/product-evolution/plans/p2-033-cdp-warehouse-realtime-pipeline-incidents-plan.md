# CDP Warehouse Realtime Pipeline Incidents Implementation Plan

Spec: `../specs/p2-033-cdp-warehouse-realtime-pipeline-incidents.md`

**Goal:** Route realtime warehouse pipeline health warnings and failures into the existing warehouse incident lifecycle.

**Architecture:** Reuse `cdp_warehouse_incident`. Add a realtime pipeline incident input to `CdpWarehouseIncidentService`, call it best-effort from checkpoint reporting, and add a scan service/API for stale status derived from runtime evaluation.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Scope

- Extend incident service with realtime pipeline incident recording.
- Add best-effort incident side effect to realtime checkpoint reporting.
- Add `CdpWarehouseRealtimePipelineIncidentService` scan workflow.
- Add `/warehouse/realtime/pipelines/incidents/scan`.
- Add service/controller tests and focused regression.

## Tasks

- [x] **Step 1: Add spec and plan**

Create P2-033 spec/plan and index them.

- [x] **Step 2: Extend incident service**

Add a realtime pipeline incident input record and map `WARN`/`FAIL` status to incident rows.

- [x] **Step 3: Integrate checkpoint reporting**

Call incident recording from `CdpWarehouseRealtimePipelineService.reportCheckpoint` as a best-effort side effect.

- [x] **Step 4: Add scan service and API**

Use `CdpWarehouseRealtimePipelineService.status` to find non-pass pipelines and record incidents.

- [x] **Step 5: Add tests and verify**

Run:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=CdpWarehouseIncidentServiceTest,CdpWarehouseRealtimePipelineServiceTest,CdpWarehouseRealtimePipelineIncidentServiceTest,CdpWarehouseRealtimePipelineIncidentControllerTest test
```

Run compile:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -DskipTests compile
```

## Verification

- [x] Main compile passed.
- [x] P2-033 focused tests passed: `20 tests, 0 failures`.
- [x] Warehouse realtime/incident regression passed: `67 tests, 0 failures`.
- [x] Warehouse/BI/Doris regression passed: `126 tests, 0 failures`.
