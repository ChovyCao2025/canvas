# CDP Warehouse Privacy Tombstone Ingestion Guard Implementation Plan

Spec: `../specs/p2-074-cdp-warehouse-privacy-tombstone-ingestion-guard.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add privacy tombstones that prevent erased CDP subjects from being recreated by future ingestion, user ensure paths, or warehouse mirroring.

**Architecture:** Store tenant-scoped tombstones in MySQL, hash/mask subject values, expose a domain service and operator controller, inject the service optionally into CDP ingestion and user ensure paths, and record blocked attempts for audit.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ.

---

## Scope

- Add P2-074 spec, plan, and index rows.
- Add tombstone schema, DO, mapper.
- Implement tombstone create/revoke/list/decision/enforcement service.
- Add operator controller.
- Wire optional guard into `CdpEventIngestionService` and `CdpUserService`.
- Support tombstone creation from PASS P2-073 erasure request evidence without re-submitting raw subject values.
- Add focused tests and regression verification.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V256__cdp_warehouse_privacy_tombstone_guard.sql`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpWarehousePrivacySubjectTombstoneDO.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpWarehousePrivacySubjectTombstoneMapper.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/CdpWarehousePrivacyTombstoneService.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyTombstoneController.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`
- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserService.java`
- Add focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/`, `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/`, and `backend/canvas-engine/src/test/java/org/chovy/canvas/web/`.

## Tasks

- [x] **Step 1: Add spec, plan, and index**

Create P2-074 docs and index rows.

- [x] **Step 2: Add schema and tombstone service tests**

Cover migration fields, hash/mask behavior, active/revoked decisions, erasure-request evidence creation, and blocked attempt updates.

- [x] **Step 3: Add ingestion, user ensure, and controller tests**

Cover blocked ingestion side effects, blocked profile/identity recreation, tenant-scoped endpoints, and create-from-erasure-request binding.

- [x] **Step 4: Implement schema, DO, and mapper**

Add additive migration and MyBatis-Plus objects.

- [x] **Step 5: Implement tombstone service and controller**

Create, create from PASS erasure request evidence, revoke, list, decide, enforce, and record blocked attempts.

- [x] **Step 6: Wire ingestion and user ensure guard**

Call the optional guard before durable inserts or profile/identity changes.

- [x] **Step 7: Verify**

Run focused tests and warehouse/CDP regression. Update this plan with observed evidence.

## Verification

- Focused P2-074 tests passed on 2026-06-05:
  `CdpWarehousePrivacyTombstoneSchemaTest,CdpWarehousePrivacyTombstoneServiceTest,CdpEventIngestionPrivacyTombstoneGuardTest,CdpUserServiceTest,CdpWarehousePrivacyTombstoneControllerTest`
- Warehouse/CDP regression passed on 2026-06-05:
  `CdpWarehouse*Test,Doris*Test,CdpEventIngestion*Test,CdpUserServiceTest`
