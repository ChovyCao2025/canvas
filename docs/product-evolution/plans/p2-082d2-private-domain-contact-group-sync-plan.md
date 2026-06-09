# Private-Domain Contact And Group Sync Implementation Plan

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add tenant-scoped private-domain contact, owner, customer group, group member, and sync-run persistence plus API access for SCRM provider collectors.

**Architecture:** Keep provider collectors outside this slice and expose a normalized snapshot ingestion service. Persist provider-specific raw payloads for audit, project synced contacts into the existing `conversation_contact_profile` table, and expose bounded read APIs for operators and later frontend surfaces.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

---

## Scope

This plan implements the P2-082D2 backend first slice:

- Schema and MyBatis data objects/mappers.
- Domain command/view records.
- Snapshot ingestion service with idempotent upserts.
- Private-domain controller APIs.
- Focused schema, service, controller, and existing SCRM workspace regression tests.

Real provider credentials, live WeCom HTTP clients, webhook signature verification, and frontend pages are intentionally deferred.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V307__private_domain_contact_group_sync.sql`.
- Create data objects:
  - `ConversationPrivateContactDO`
  - `ConversationPrivateContactOwnerDO`
  - `ConversationPrivateGroupDO`
  - `ConversationPrivateGroupMemberDO`
  - `ConversationPrivateSyncRunDO`
- Create mappers:
  - `ConversationPrivateContactMapper`
  - `ConversationPrivateContactOwnerMapper`
  - `ConversationPrivateGroupMapper`
  - `ConversationPrivateGroupMemberMapper`
  - `ConversationPrivateSyncRunMapper`
- Create domain records and service under `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationPrivateDomainController.java`.
- Add tests:
  - `ConversationPrivateDomainSchemaTest`
  - `ConversationPrivateDomainSyncServiceTest`
  - `ConversationPrivateDomainControllerTest`
- Update P2-082 parent docs and indexes.

## Tasks

### Task 1: Index P2-082D2 Docs

**Files:**
- Create: `docs/product-evolution/specs/p2-082d2-private-domain-contact-group-sync.md`
- Create: `docs/product-evolution/plans/p2-082d2-private-domain-contact-group-sync-plan.md`
- Modify: `docs/product-evolution/specs/INDEX.md`
- Modify: `docs/product-evolution/plans/INDEX.md`
- Modify: `docs/product-evolution/IMPLEMENTATION_ORDER.md`
- Modify: `docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md`
- Modify: `docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md`

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082D2 index rows after P2-082D**
- [x] **Step 3: Update parent P2-082 status**
- [x] **Step 4: Verify indexability**

Run:

```bash
rg -n "P2-082D2|p2-082d2-private-domain-contact-group-sync" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-082d2-private-domain-contact-group-sync.md docs/product-evolution/plans/p2-082d2-private-domain-contact-group-sync-plan.md docs/product-evolution/specs/p2-082-marketing-platform-gap-closure.md docs/product-evolution/plans/p2-082-marketing-platform-gap-closure-plan.md
```

Expected: every file has a P2-082D2 or slug match.

### Task 2: Add Private-Domain Sync Schema

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationPrivateDomainSchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V307__private_domain_contact_group_sync.sql`

- [x] **Step 1: Write failing schema test**

Assert the migration contains:

- `conversation_private_contact`
- `conversation_private_contact_owner`
- `conversation_private_group`
- `conversation_private_group_member`
- `conversation_private_sync_run`
- `uk_conversation_private_contact`
- `uk_conversation_private_contact_owner`
- `uk_conversation_private_group`
- `uk_conversation_private_group_member`
- `idx_conversation_private_sync_run_provider`

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationPrivateDomainSchemaTest test
```

Expected before migration: FAIL because the migration is missing.

- [x] **Step 2: Add migration**

Create the five tables with tenant/provider uniqueness, owner lookup indexes, keyword-friendly display fields, raw JSON payload fields, sync counters, cursor, and error fields.

- [x] **Step 3: Verify schema test passes**

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
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationPrivateDomainSchemaTest test
```

Expected: PASS.

### Task 4: Implement Sync Service With TDD

**Files:**
- Create: `ConversationPrivateDomainSyncServiceTest.java`
- Create: `ConversationPrivateDomainSyncService.java`
- Create domain records for commands, snapshots, queries, and views.

- [x] **Step 1: Write failing service tests**

Cover:

- contact snapshot creates contact, owner relation, profile projection, and success sync run counters;
- repeated contact snapshot updates existing rows instead of inserting duplicates;
- group snapshot creates group plus members and success counters;
- blank external ids are rejected and failed sync run details are recorded;
- contact, group, and sync-run queries are tenant scoped and bounded.

- [x] **Step 2: Verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationPrivateDomainSyncServiceTest test
```

Expected: FAIL because service/records do not exist.

- [x] **Step 3: Implement minimal service**

Use MyBatis-Plus `selectOne`, `insert`, and `updateById`. Serialize tags, attributes, metadata, and raw payloads with Jackson. Normalize provider to uppercase and use profile user ids in the form `{PROVIDER}:{externalContactId}`.

- [x] **Step 4: Verify GREEN**

Run the same Maven command and expect PASS.

### Task 5: Add Controller

**Files:**
- Create: `ConversationPrivateDomainControllerTest.java`
- Create: `ConversationPrivateDomainController.java`

- [x] **Step 1: Write failing controller tests**

Cover:

- ingest endpoint passes tenant and actor;
- contacts endpoint passes filters and bounded limit;
- groups endpoint passes filters and bounded limit;
- sync-runs endpoint passes provider and bounded limit.

- [x] **Step 2: Verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationPrivateDomainControllerTest test
```

Expected: FAIL because controller does not exist.

- [x] **Step 3: Implement controller**

Follow existing conversation controller patterns: `TenantContextResolver.currentOrError()`, `R.ok`, bounded limits, actor from tenant context, and `Schedulers.boundedElastic()`.

- [x] **Step 4: Verify GREEN**

Run controller tests and expect PASS.

### Task 6: Verify P2-082D2 Focused Suite

- [x] **Step 1: Verify P2-082D2 focused suite**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationPrivateDomainSchemaTest,ConversationPrivateDomainSyncServiceTest,ConversationPrivateDomainControllerTest,ConversationWorkspaceSchemaTest,ConversationWorkspaceServiceTest,ConversationWorkspaceControllerTest test
```

Expected: all tests pass.

Latest focused verification:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationPrivateDomainSchemaTest,ConversationPrivateDomainSyncServiceTest,ConversationPrivateDomainControllerTest,ConversationWorkspaceSchemaTest,ConversationWorkspaceServiceTest,ConversationWorkspaceControllerTest
```

Result: 24 tests run, 0 failures, 0 errors, 0 skipped.
