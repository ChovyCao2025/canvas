# Compliance And Data Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make audit coverage, data retention, deletion workflows, PII masking, consent/suppression checks, and compliance evidence concrete enough to implement and verify.

**Architecture:** Add compliance primitives inside the current backend deployable. Audit and data-governance services live under `domain/compliance`, existing marketing policy checks remain in `engine/policy`, schema changes use a new Flyway migration, and evidence checklists live under `docs/architecture/compliance`. Sensitive-field rules must apply to logs, DTOs, API responses, and deletion workflows.

**Tech Stack:** Java 21, Spring Boot 3.2, MyBatis-Plus, Flyway/MySQL, Jackson, JUnit 5, AssertJ, Redis key catalog, Markdown evidence docs.

---

## Source Material

- Spec: `../specs/P2-05-compliance-data-governance-spec.md`
- Source package: `../todo/p2/compliance-data-governance/`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Read: `docs/architecture/archive/specs/P2-05-compliance-data-governance-spec.md`
- Read: `docs/architecture/todo/p2/compliance-data-governance/plan.md`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V3__auth_and_supplements.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V60__marketing_policy_tables.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V71__data_source_config.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V74__cdp_core.sql`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TenantController.java`
- Create: `docs/architecture/compliance/data-inventory.md`
- Create: `docs/architecture/compliance/audit-event-matrix.md`
- Create: `docs/architecture/compliance/deletion-and-retention-workflows.md`
- Create: `docs/architecture/compliance/compliance-evidence-checklist.md`
- Use existing: `backend/canvas-engine/src/main/resources/db/migration/V3__auth_and_supplements.sql`
- Note: do not create `V92__compliance_governance.sql`; `V92` already exists. Use the next available migration version if schema changes are needed.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/AuditEventService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/DataDeletionService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/PiiMaskingService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/AuditEventServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/DataDeletionServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/PiiMaskingServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceTest.java`

### Task 1: Inventory PII and credential fields

**Files:**
- Create: `docs/architecture/compliance/data-inventory.md`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V3__auth_and_supplements.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V60__marketing_policy_tables.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V71__data_source_config.sql`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V74__cdp_core.sql`
- Read: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject`

- [x] Inventory fields for user identity, phone/email/open_id, customer profile, customer channel, tag data, consent/suppression, data-source credentials, JWT/security secrets, and audit actor identifiers.
- [x] Classify each field as public, internal, confidential, PII, credential, or compliance evidence.
- [x] Record masking rule, retention rule, deletion rule, and owning context for each sensitive field.

**Run:**
```bash
test -f docs/architecture/compliance/data-inventory.md
rg "PII|credential|masking rule|retention rule|deletion rule|owning context|data_source_config|marketing_consent" docs/architecture/compliance/data-inventory.md
```

**Expected:** The inventory names every sensitive table family from the spec and maps each field to owner, retention, deletion, and masking rules.

### Task 2: Define audit events and implement audit writes for critical operations

**Files:**
- Create: `docs/architecture/compliance/audit-event-matrix.md`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/AuditEventService.java`
- Use existing: `backend/canvas-engine/src/main/resources/db/migration/V3__auth_and_supplements.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TenantController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DataSourceConfigController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/AuditEventServiceTest.java`

- [x] Define audit events for login/admin user changes, tenant changes, canvas create/update/publish/delete, execution replay, data-source credential changes, consent/suppression changes, and deletion requests.
- [x] Create `AuditEventService` to write `canvas_audit_log` records with actor, tenant, operation, target, request id, and masked metadata.
- [x] Add controller/service calls for privileged and canvas lifecycle operations.
- [x] Add tests that assert audit rows are written and metadata is masked.

**Run:**
```bash
test -f docs/architecture/compliance/audit-event-matrix.md
cd backend && mvn test -pl canvas-engine -Dtest=AuditEventServiceTest,CanvasStatsControllerTest,TenantServiceTest
rg "canvas publish|execution replay|data-source credential|consent|deletion request" docs/architecture/compliance/audit-event-matrix.md
```

**Expected:** Audit matrix names critical operations, tests prove audit writes, and masked metadata is used for sensitive values.

### Task 3: Define retention and deletion workflows

**Files:**
- Create: `docs/architecture/compliance/deletion-and-retention-workflows.md`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/DataDeletionService.java`
- Use existing: `backend/canvas-engine/src/main/resources/db/migration/V3__auth_and_supplements.sql`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/DataDeletionServiceTest.java`
- Read: `docs/architecture/capacity/retention-policy.md`

- [x] Define request intake, authorization, identity matching, dry-run, deletion, tombstone, audit, rollback limit, and evidence capture for delete requests.
- [x] Implement deletion workflow for CDP user profile, identities, tags, marketing consent/suppression, and message send records.
- [x] Extend deletion/redaction workflow for execution traces that reference a user id inside JSON payloads.
- [x] Keep compliance evidence records when legal retention requires them.
- [x] Add a dry-run test and an executed-deletion test.

**Run:**
```bash
test -f docs/architecture/compliance/deletion-and-retention-workflows.md
cd backend && mvn test -pl canvas-engine -Dtest=DataDeletionServiceTest
rg "dry-run|tombstone|audit|CDP user profile|marketing consent|message send records|execution traces" docs/architecture/compliance/deletion-and-retention-workflows.md
```

**Expected:** Deletion workflow is documented, dry-run and execution tests pass, and retained evidence is explicit.

### Task 4: Add PII masking for logs and API responses

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/PiiMaskingService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpUserController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasUserController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpUserService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/PiiMaskingServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/CdpUserControllerTest.java`

- [x] Centralize phone, email, open_id, credential, token, and secret masking in `PiiMaskingService`.
- [x] Apply masking to exception payloads.
- [x] Migrate user-facing CDP/canvas-user API responses from legacy masking helpers to `PiiMaskingService`.
- [x] Add tests for masking formats and exception response payloads.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=PiiMaskingServiceTest,CdpUserControllerTest,CanvasUserControllerTest
rg "secret-token|password|open_id" backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance backend/canvas-engine/src/test/java/org/chovy/canvas/controller
```

**Expected:** Masking tests pass and no test fixture expects raw sensitive values in API responses or exception payloads.

### Task 5: Extend suppression and consent checks

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V60__marketing_policy_tables.sql`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerTest.java`

- [x] Enforce explicit consent, opt-out, suppression, frequency cap, and channel-specific policy decisions before delivery.
- [x] Return policy reasons to handlers without leaking PII.
- [x] Add tests for opt-in, opt-out, missing consent, active suppression, and no active/unexpired suppression.
- [x] Add explicit SQL-wrapper or integration coverage for expired suppression and all-channel suppression matching.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=MarketingPolicyServiceTest,SendMessageHandlerTest
rg "OPT_IN|OPT_OUT|suppression|frequency" backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy
```

**Expected:** Marketing policy tests pass and delivery paths reject disallowed sends before external channel calls.

### Task 6: Add tests and compliance evidence checklist

**Files:**
- Create: `docs/architecture/compliance/compliance-evidence-checklist.md`
- Modify: `docs/architecture/compliance/data-inventory.md`
- Modify: `docs/architecture/compliance/audit-event-matrix.md`
- Modify: `docs/architecture/compliance/deletion-and-retention-workflows.md`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/AuditEventServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/DataDeletionServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/PiiMaskingServiceTest.java`

- [x] Checklist must include audit evidence, deletion evidence, retention evidence, masking evidence, consent/suppression evidence, incident response evidence, and owners.
- [x] Link every checklist row to a test, runbook, log query, dashboard, or document path.
- [x] Add the compliance test command to the checklist.

**Run:**
```bash
cd backend && mvn test -pl canvas-engine -Dtest=AuditEventServiceTest,DataDeletionServiceTest,PiiMaskingServiceTest,MarketingPolicyServiceTest
test -f docs/architecture/compliance/compliance-evidence-checklist.md
rg "audit evidence|deletion evidence|retention evidence|masking evidence|incident response|owner" docs/architecture/compliance/compliance-evidence-checklist.md
```

**Expected:** Compliance-focused backend tests pass and the checklist links every evidence type to a concrete source.

### Task 7: Handoff scoped compliance and governance changes

**Files:**
- Modify: `docs/architecture/archive/plans/P2-05-compliance-data-governance-plan.md`
- Create: `docs/architecture/compliance/`
- Use existing: `backend/canvas-engine/src/main/resources/db/migration/V3__auth_and_supplements.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance/`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance/`

- [x] Review compliance files named in this plan and document unrelated testCompile blockers separately.
- [x] Do not stage or commit in this session unless the user explicitly asks.
- [x] Record remaining follow-ups in evidence instead of hiding partial work.

**Run:**
```bash
git diff -- docs/architecture/archive/plans/P2-05-compliance-data-governance-plan.md docs/architecture/compliance docs/architecture/evidence/P2-05-compliance-data-governance.md backend/canvas-engine/src/main/java/org/chovy/canvas/domain/compliance backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasAuditLogDO.java backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasAuditLogMapper.java backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java backend/canvas-engine/src/test/java/org/chovy/canvas/domain/compliance backend/canvas-engine/src/test/java/org/chovy/canvas/engine/policy/MarketingPolicyServiceTest.java backend/canvas-engine/src/test/java/org/chovy/canvas/config/GlobalExceptionHandlerTest.java
```

**Expected:** The diff contains only compliance docs, services, audit persistence boundary, exception masking, and compliance tests. No commit is created by default.
