# CDP Warehouse Quality Incident Loop Implementation Plan

**Goal:** Convert warehouse quality warnings into durable incidents with acknowledge and resolve lifecycle APIs.

**Architecture:** Keep `cdp_warehouse_quality_check` as the immutable check ledger. Add a deduplicated incident table keyed by tenant and incident key. `CdpWarehouseQualityService` records incidents as a best-effort side effect after warning checks are persisted.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, MySQL, JUnit 5, Mockito, AssertJ.

## Current State

- Warehouse quality checks record PASS/WARN/SKIPPED rows.
- Operators can list recent checks, but cannot manage unresolved warning state.

## Desired State

- Quality warnings open or update incidents.
- Operators can list open incidents, acknowledge them, and resolve them.
- Incident recording does not compromise quality-check persistence.

## Implementation Tasks

### Task 1: Register P2-030 Spec And Plan

- [x] **Step 1: Create spec and plan**

Create:
- `docs/product-evolution/specs/p2-030-cdp-warehouse-quality-incident-loop.md`
- `docs/product-evolution/plans/p2-030-cdp-warehouse-quality-incident-loop-plan.md`

- [x] **Step 2: Update indexes**

Update:
- `docs/product-evolution/specs/INDEX.md`
- `docs/product-evolution/plans/INDEX.md`
- `docs/product-evolution/IMPLEMENTATION_ORDER.md`

### Task 2: Incident Schema

- [x] **Step 1: Write schema test**

Create `CdpWarehouseIncidentSchemaTest` proving:
- `cdp_warehouse_incident` exists;
- `(tenant_id, incident_key)` is unique;
- status and severity indexes exist.

- [x] **Step 2: Add migration and DAL**

Create:
- `V197__cdp_warehouse_quality_incident.sql`;
- `CdpWarehouseIncidentDO`;
- `CdpWarehouseIncidentMapper`.

### Task 3: Incident Service

- [x] **Step 1: Write service tests**

Create `CdpWarehouseIncidentServiceTest` covering:
- warning check opens/updates an incident;
- pass/skipped checks are ignored;
- list is tenant scoped and bounded;
- acknowledge and resolve are tenant scoped.

- [x] **Step 2: Implement service**

Create `CdpWarehouseIncidentService` with:
- `recordQualityIncident(CdpWarehouseQualityService.QualityCheckResult check)`;
- `listIncidents(Long tenantId, String status, int limit)`;
- `acknowledge(Long tenantId, Long incidentId, String operator)`;
- `resolve(Long tenantId, Long incidentId, String operator)`.

### Task 4: Quality Integration And Controller

- [x] **Step 1: Integrate quality service**

Record incidents from ODS count and aggregate lag warnings as best-effort side effects.

- [x] **Step 2: Add controller**

Expose:
- `GET /warehouse/incidents`;
- `POST /warehouse/incidents/{id}/ack`;
- `POST /warehouse/incidents/{id}/resolve`.

### Task 5: Verification

- [x] **Step 1: Run focused tests**

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test -Dtest=CdpWarehouseIncidentSchemaTest,CdpWarehouseIncidentServiceTest,CdpWarehouseIncidentControllerTest,CdpWarehouseQualityServiceTest
```

- [x] **Step 2: Run warehouse regression**

Run P2-022 through P2-030 focused warehouse tests.

## Acceptance Checklist

- [x] P2-030 spec and plan are indexed.
- [x] Incident table is additive and tenant scoped.
- [x] Quality warnings open/update incidents.
- [x] Incident APIs support list, acknowledge, and resolve.
- [x] Incident side effects do not break quality checks.
- [x] Focused warehouse tests pass.
