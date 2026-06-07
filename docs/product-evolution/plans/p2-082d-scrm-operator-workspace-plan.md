# SCRM Operator Workspace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a production-grade SCRM operator workspace on top of conversation sessions.

**Architecture:** Add tenant-scoped workspace tables for work items, contact profiles, SOP tasks, and audit events. Implement a focused service/controller that reuse existing conversation sessions/messages, then expose the contract through a compact Ant Design operator inbox.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ, React 18, Vite, TypeScript, Ant Design, Vitest.

---

## Scope

This plan implements the P2-082D workspace slice:

- Schema and data objects.
- Work item lifecycle service.
- Workspace controller APIs.
- Optional inbound-message hook from `ConversationIngressService`.
- Frontend workspace API client, presentation helpers, inbox page, route, and navigation.
- Focused backend verification.

Real private-domain provider sync remains deferred to later slices after the operator workspace contract is stable.

## Files

- Create `backend/canvas-engine/src/main/resources/db/migration/V305__scrm_operator_workspace.sql`.
- Create data objects:
  - `ConversationContactProfileDO`
  - `ConversationWorkItemDO`
  - `ConversationSopTaskDO`
  - `ConversationWorkItemAuditDO`
- Create mappers:
  - `ConversationContactProfileMapper`
  - `ConversationWorkItemMapper`
  - `ConversationSopTaskMapper`
  - `ConversationWorkItemAuditMapper`
- Create domain records and service under `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/`.
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationWorkspaceController.java`.
- Modify `ConversationIngressService` to optionally notify the workspace service after a new inbound message.
- Modify frontend conversation presentation/API files.
- Create frontend conversation workspace page.
- Modify `App.tsx`, `AppLayout.tsx`, and `RouteA11y.tsx` for `/conversations`.
- Add tests:
  - `ConversationWorkspaceSchemaTest`
  - `ConversationWorkspaceServiceTest`
  - `ConversationWorkspaceControllerTest`
  - `conversationPresentation.test.ts`
  - `conversationApi.test.ts`
  - `pages/conversations/index.test.tsx`
  - `AppLayout.a11y.test.tsx`
- Update P2-082 parent docs and indexes.

## Tasks

### Task 1: Index P2-082D Docs

**Files:**
- Create: `docs/product-evolution/specs/p2-082d-scrm-operator-workspace.md`
- Create: `docs/product-evolution/plans/p2-082d-scrm-operator-workspace-plan.md`
- Modify: `docs/product-evolution/specs/INDEX.md`
- Modify: `docs/product-evolution/plans/INDEX.md`
- Modify: `docs/product-evolution/IMPLEMENTATION_ORDER.md`

- [x] **Step 1: Write spec and plan**
- [x] **Step 2: Insert P2-082D index rows after P2-082**
- [x] **Step 3: Verify indexability**

Run:

```bash
rg -n "P2-082D|p2-082d-scrm-operator-workspace" docs/product-evolution/IMPLEMENTATION_ORDER.md docs/product-evolution/specs/INDEX.md docs/product-evolution/plans/INDEX.md docs/product-evolution/specs/p2-082d-scrm-operator-workspace.md docs/product-evolution/plans/p2-082d-scrm-operator-workspace-plan.md
```

Expected: every file has a P2-082D or slug match.

### Task 2: Add Workspace Schema

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/ConversationWorkspaceSchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V305__scrm_operator_workspace.sql`

- [x] **Step 1: Write failing schema test**

The test reads `V305__scrm_operator_workspace.sql` and asserts table/index names:

- `conversation_contact_profile`
- `conversation_work_item`
- `conversation_sop_task`
- `conversation_work_item_audit`
- `uk_conversation_work_item_session`
- `uk_conversation_contact_profile_user`
- `idx_conversation_work_item_inbox`
- `idx_conversation_sop_task_work_item`
- `idx_conversation_work_item_audit_item`

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationWorkspaceSchemaTest test
```

Expected before migration: FAIL because the migration is missing.

- [x] **Step 2: Add migration**

Create the four tables with tenant-scoped indexes and JSON metadata columns.

- [x] **Step 3: Verify schema test passes**

Run the same Maven command and expect PASS.

### Task 3: Add Data Objects And Mappers

**Files:**
- Create data objects and mappers listed in the Files section.

- [x] **Step 1: Add Lombok/MyBatis data objects**
- [x] **Step 2: Add mapper interfaces**
- [x] **Step 3: Compile focused test package**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationWorkspaceSchemaTest test
```

Expected: PASS.

### Task 4: Implement Workspace Service With TDD

**Files:**
- Create: `ConversationWorkspaceServiceTest.java`
- Create: `ConversationWorkspaceService.java`
- Create domain records for commands and views.

- [x] **Step 1: Write failing service tests**

Cover:

- idempotent `ensureWorkItemForSession`;
- assignment update with audit;
- status, priority, and follow-up update with audit;
- task creation and completion;
- timeline tenant isolation;
- inbound message hook updates customer activity without overwriting assignment.

- [x] **Step 2: Verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationWorkspaceServiceTest test
```

Expected: FAIL because service/records do not exist.

Historical RED was satisfied before `ConversationWorkspaceService` and its records were added. Current verification is GREEN through `ConversationWorkspaceServiceTest` and the P2-080 conversation focused gate.

- [x] **Step 3: Implement minimal service**

Use existing `ConversationSessionMapper` and `ConversationMessageMapper` for session/message reads. Use workspace mappers for lifecycle writes. Serialize tags, attributes, task metadata, and audit details with Jackson.

- [x] **Step 4: Verify GREEN**

Run the same Maven command and expect PASS.

### Task 5: Add Workspace Controller

**Files:**
- Create: `ConversationWorkspaceControllerTest.java`
- Create: `ConversationWorkspaceController.java`

- [x] **Step 1: Write failing controller tests**

Cover:

- ensure work item passes tenant and operator;
- inbox query passes filters and bounded limit;
- assignment endpoint passes actor;
- status endpoint passes actor;
- create task endpoint passes actor;
- complete task endpoint passes actor;
- timeline endpoint passes tenant.

- [x] **Step 2: Verify RED**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationWorkspaceControllerTest test
```

Expected: FAIL because controller does not exist.

Historical RED was satisfied before `ConversationWorkspaceController` was added. Current verification is GREEN through `ConversationWorkspaceControllerTest` and the P2-080 conversation focused gate.

- [x] **Step 3: Implement controller**

Follow `ConversationController` patterns: `TenantContextResolver.currentOrError()`, `R.ok`, bounded limits, and `Schedulers.boundedElastic()`.

- [x] **Step 4: Verify GREEN**

Run controller tests and expect PASS.

### Task 6: Integrate Inbound Conversation Hook

**Files:**
- Modify: `ConversationIngressService.java`
- Modify: `ConversationIngressServiceTest.java`

- [x] **Step 1: Add failing test for inbound workspace update**

Prove a new inbound message calls `ConversationWorkspaceService.recordInboundMessage(...)` and duplicate messages do not.

- [x] **Step 2: Implement optional dependency**

Use `ObjectProvider<ConversationWorkspaceService>` in the Spring constructor while preserving test constructors.

- [x] **Step 3: Verify conversation focused tests**

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationIngressServiceTest,ConversationAdapterCatalogTest,ConversationAdapterHarnessTest,WhatsAppConversationReplyAdapterTest,WebChatConversationReplyAdapterTest test
```

Expected: PASS.

### Task 7: Verify P2-082D Focused Suite

Run:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationWorkspaceSchemaTest,ConversationWorkspaceServiceTest,ConversationWorkspaceControllerTest,ConversationIngressServiceTest,ConversationControllerTest,ConversationAdapterCatalogTest,ConversationAdapterHarnessTest,WhatsAppConversationReplyAdapterTest,WebChatConversationReplyAdapterTest test
```

Expected: all tests pass.

Verified:

```bash
cd backend
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ConversationWorkspaceSchemaTest,ConversationWorkspaceServiceTest,ConversationWorkspaceControllerTest,ConversationIngressServiceTest,ConversationControllerTest,ConversationAdapterCatalogTest,ConversationAdapterHarnessTest,WhatsAppConversationReplyAdapterTest,WebChatConversationReplyAdapterTest test
```

Result: 40 tests, 0 failures, 0 errors.

Verified on 2026-06-06 with:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-engine test -Dtest=ConversationWorkspaceSchemaTest,ConversationWorkspaceServiceTest,ConversationWorkspaceControllerTest,ConversationIngressServiceTest,ConversationControllerTest,ConversationAdapterCatalogTest,ConversationAdapterHarnessTest,WhatsAppConversationReplyAdapterTest,WebChatConversationReplyAdapterTest,SocialDmConversationReplyAdapterTest,RcsConversationReplyAdapterTest
```

Result: 48 tests run, 0 failures, 0 errors, 0 skipped.

### Task 8: Add Frontend Operator Workspace

**Files:**
- Modify: `frontend/src/pages/conversations/conversationPresentation.ts`
- Modify: `frontend/src/pages/conversations/conversationPresentation.test.ts`
- Modify: `frontend/src/services/conversationApi.ts`
- Modify: `frontend/src/services/conversationApi.test.ts`
- Create: `frontend/src/pages/conversations/index.tsx`
- Create: `frontend/src/pages/conversations/index.test.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/components/layout/AppLayout.a11y.test.tsx`
- Modify: `frontend/src/components/accessibility/RouteA11y.tsx`

- [x] **Step 1: Add failing frontend presentation, API, page, and navigation tests**

Run:

```bash
cd frontend
npm run test -- conversationPresentation.test.ts conversationApi.test.ts index.test.tsx
npm run test -- src/components/layout/AppLayout.a11y.test.tsx
```

Expected before implementation: FAIL because workspace helpers, workspace API functions, page module, `/conversations` route announcement, and navigation item are missing.

- [x] **Step 2: Implement presentation types and helpers**

Add workspace view types for work items, contact profiles, SOP tasks, audits, timeline payloads, command payloads, status/priority/channel options, filter normalization, date formatting, work item titles, and audit lines.

- [x] **Step 3: Implement workspace API client**

Add client functions for:

- `POST /canvas/conversations/workspace/sessions/{sessionId}/work-item`
- `GET /canvas/conversations/workspace/inbox`
- `POST /canvas/conversations/workspace/work-items/{workItemId}/assign`
- `POST /canvas/conversations/workspace/work-items/{workItemId}/status`
- `POST /canvas/conversations/workspace/work-items/{workItemId}/tasks`
- `POST /canvas/conversations/workspace/tasks/{taskId}/complete`
- `GET /canvas/conversations/workspace/work-items/{workItemId}/timeline`

- [x] **Step 4: Implement workspace page**

Create a dense Ant Design page with inbox filters, work item table, customer timeline drawer, assignment form, status/reminder form, SOP task form, task completion controls, message list, and audit list.

- [x] **Step 5: Wire route, menu, and route announcement**

Add `/conversations` to `App.tsx`, add a “会话工作台” menu entry under “运营值班”, and announce the route as “已进入会话工作台”.

- [x] **Step 6: Verify frontend focused suite and production build**

Run:

```bash
cd frontend
npm run test -- src/pages/conversations/conversationPresentation.test.ts src/services/conversationApi.test.ts src/pages/conversations/index.test.tsx src/components/layout/AppLayout.a11y.test.tsx src/pages/bi/index.test.tsx
npm run build
```

Verified: 5 test files, 17 tests, 0 failures; TypeScript and Vite production build succeeded.
