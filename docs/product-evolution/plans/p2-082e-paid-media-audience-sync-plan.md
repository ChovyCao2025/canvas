# Paid-Media Audience Sync Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-scoped paid-media destination configuration, hashed audience export, eligibility audit, and sync-run APIs.

**Architecture:** Keep real provider calls out of this slice. Persist provider destinations and run/member audit rows, derive SHA-256 hashes from CDP profiles, and gate export by audience definition, tenant, consent, and identifier availability.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

This plan implements the P2-082E backend first slice:

- Schema and MyBatis data objects/mappers.
- Domain command/view records.
- Destination upsert and sandbox-safe sync service.
- Paid-media audience sync controller APIs.
- Focused schema, service, and controller tests.

Real provider credentials, live Google/Meta/DSP HTTP clients, frontend pages, and attribution imports are deferred.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V308__paid_media_audience_sync.sql`.
- Create data objects:
  - `PaidMediaAudienceDestinationDO`
  - `PaidMediaAudienceMemberDO`
  - `PaidMediaAudienceSyncRunDO`
- Create mappers:
  - `PaidMediaAudienceDestinationMapper`
  - `PaidMediaAudienceMemberMapper`
  - `PaidMediaAudienceSyncRunMapper`
- Create domain records and service under `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/paidmedia/`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/PaidMediaAudienceSyncController.java`.
- Add tests:
  - `PaidMediaAudienceSyncSchemaTest`
  - `PaidMediaAudienceSyncServiceTest`
  - `PaidMediaAudienceSyncControllerTest`
- Update P2-082 parent docs and indexes.

## Tasks

### Task 1: Index P2-082E Docs

**Files:**
- Create: `docs/product-evolution/specs/p2-082e-paid-media-audience-sync.md`
- Create: `docs/product-evolution/plans/p2-082e-paid-media-audience-sync-plan.md`
- Modify: `docs/product-evolution/specs/INDEX.md`
- Modify: `docs/product-evolution/plans/INDEX.md`
- Modify: `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- Modify: `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`
- Modify: `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082E index rows after P2-082D2**
- [x] **Step 3: Update parent P2-082 status**
- [x] **Step 4: Verify indexability**

Run:

```bash
rg -n "P2-082E|p2-082e-paid-media-audience-sync" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-082e-paid-media-audience-sync.md docs/product-evolution/plans/p2-082e-paid-media-audience-sync-plan.md docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md
```

Expected: every file has a P2-082E or slug match.

### Task 2: Add Paid-Media Sync Schema

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/paidmedia/PaidMediaAudienceSyncSchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V308__paid_media_audience_sync.sql`

- [x] **Step 1: Write failing schema test**

Assert the migration contains:

- `paid_media_audience_destination`
- `paid_media_audience_member`
- `paid_media_audience_sync_run`
- `uk_paid_media_destination`
- `idx_paid_media_member_run_status`
- `idx_paid_media_sync_run_destination`

- [x] **Step 2: Verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=PaidMediaAudienceSyncSchemaTest test
```

Expected before migration: FAIL because the migration is missing.

- [x] **Step 3: Add migration**

Create the three tables with tenant/provider uniqueness, run/member query indexes, JSON metadata fields, and counter fields.

- [x] **Step 4: Verify GREEN**

Run the same Maven command and expect PASS.

### Task 3: Add Data Objects And Mappers

**Files:**
- Create data objects and mappers listed in the Files section.

- [x] **Step 1: Add Lombok/MyBatis data objects**
- [x] **Step 2: Add mapper interfaces**
- [x] **Step 3: Compile schema test**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=PaidMediaAudienceSyncSchemaTest test
```

Expected: PASS.

### Task 4: Implement Sync Service With TDD

**Files:**
- Create: `PaidMediaAudienceSyncServiceTest.java`
- Create: `PaidMediaAudienceSyncService.java`
- Create domain records for commands, queries, and views.

- [x] **Step 1: Write failing service tests**

Cover:

- destination upsert normalizes provider and identifier types and is idempotent;
- sync hashes eligible email/phone identifiers and records success counters;
- consent-denied, missing-profile, and missing-identifier users create skipped audit rows;
- disabled destination/audience failures record failed run details;
- run and member queries are tenant scoped and bounded.

- [x] **Step 2: Verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=PaidMediaAudienceSyncServiceTest test
```

Expected: FAIL because service/records do not exist.

- [x] **Step 3: Implement minimal service**

Use MyBatis-Plus `selectOne`, `selectById`, `selectList`, `insert`, and `updateById`. Serialize identifier types and metadata with Jackson. Hash with SHA-256 over normalized values.

- [x] **Step 4: Verify GREEN**

Run the same Maven command and expect PASS.

### Task 5: Add Controller

**Files:**
- Create: `PaidMediaAudienceSyncControllerTest.java`
- Create: `PaidMediaAudienceSyncController.java`

- [x] **Step 1: Write failing controller tests**

Cover:

- upsert destination passes tenant and actor;
- sync run passes tenant and actor;
- runs endpoint passes filters and bounded limit;
- members endpoint passes run id, status, and bounded limit.

- [x] **Step 2: Verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=PaidMediaAudienceSyncControllerTest test
```

Expected: FAIL because controller does not exist.

- [x] **Step 3: Implement controller**

Follow existing reactive controller patterns: `TenantContextResolver.currentOrError()`, `R.ok`, bounded limits, actor from tenant context, and `Schedulers.boundedElastic()`.

- [x] **Step 4: Verify GREEN**

Run controller tests and expect PASS.

### Task 6: Verify P2-082E Focused Suite

- [x] **Step 1: Verify focused suite**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=PaidMediaAudienceSyncSchemaTest,PaidMediaAudienceSyncServiceTest,PaidMediaAudienceSyncControllerTest test
```

Expected: all tests pass.
