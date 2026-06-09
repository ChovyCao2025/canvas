# CDP Warehouse Readiness Incident Automation Implementation Plan

Spec: `../specs/p2-037-cdp-warehouse-readiness-incident-automation.md`

**Goal:** Add a tenant-scoped scanner that turns readiness WARN/FAIL sections into warehouse incidents without adding duplicate schema or recomputing readiness sources.

**Architecture:** `CdpWarehouseReadinessIncidentService` reads `CdpWarehouseReadinessService`, skips PASS and `incidents`, and delegates stable incident upsert to `CdpWarehouseIncidentService`.

**Tech Stack:** Java 21, Spring Boot, JUnit 5, Mockito, AssertJ.

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Scope

- Add P2-037 spec, plan, and indexes.
- Extend `CdpWarehouseIncidentService` with a readiness incident input.
- Add readiness incident scanner service.
- Add `/warehouse/readiness/incidents/scan`.
- Add focused tests.
- No Flyway migration.

## Tasks

- [x] **Step 1: Add spec, plan, and indexes**

Create and index P2-037 docs.

- [x] **Step 2: Extend incident service**

Add `recordReadinessIncident` with stable `READINESS:{SECTION_KEY}` keys and existing `upsertOpen`.

- [x] **Step 3: Add readiness incident scanner**

Scan readiness sections, skip PASS and `incidents`, count write failures, and return scan metrics.

- [x] **Step 4: Add controller**

Expose tenant-scoped manual scan under `/warehouse/readiness/incidents/scan`.

- [x] **Step 5: Add tests and verify**

Run focused service/controller tests, compile, and warehouse/BI/audience regression.

## Verification

- [x] Focused tests passed: `14 tests, 0 failures`.
- [x] Main compile passed: `mvn -pl canvas-engine -DskipTests compile`.
- [x] Warehouse/BI/audience regression passed: `216 tests, 0 failures, 1 skipped`.
