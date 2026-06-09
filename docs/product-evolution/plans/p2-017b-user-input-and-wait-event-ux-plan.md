# User Input And Wait Event UX Implementation Plan

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add user input response storage and richer WAIT event filter configuration with completed/timeout branches.

**Architecture:** Persist user input forms/responses and wait event filters in V113, add `UserInputHandler`, and extend `WaitSubscriptionService` without changing unrelated WAIT duration behavior.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Redis wait subscriptions, JUnit 5, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-017b-user-input-and-wait-event-ux.md`

## Current Status Note

The implementation files are present in the current worktree and fresh focused verification passed on 2026-06-09:

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=TemplateRenderServiceTest,UserInputHandlerTest,WaitEventFilterTest,WaitHandlerTest,ConnectedContentHandlerTest,ExecutionRerunControllerTest,CanvasBatchOperationControllerTest,RuntimeMigrationEvidenceTest` (covered `UserInputHandlerTest`, `WaitEventFilterTest`, and `WaitHandlerTest`; 48 total selected backend tests passed).

Historical RED-state checks were not reproduced because the current worktree already contains the implementation. No commit or merge was created in this audit, so commit and merge status remain unverified.

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V113__user_input_wait_event_tools.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/UserInputHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitSubscriptionService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/UserInputHandlerTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitEventFilterTest.java`
- Modify: `frontend/src/components/config-panel/index.tsx`

### Task 1: Schema And User Input Handler

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/UserInputHandlerTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V113__user_input_wait_event_tools.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/UserInputHandler.java`

- [x] **Step 1: Write tests**

Create tests for missing schema rejection, pending response creation, completed branch resume, timeout branch resume, and duplicate response idempotency.

Historical RED-state boundary: not reproduced in this audit because the current worktree already contains the implementation.

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=UserInputHandlerTest
```

Expected: FAIL because migration and handler do not exist.

- [x] **Step 3: Add migration**

Create tables `user_input_form`, `user_input_response`, and `user_input_resume_audit` with tenant id, execution id, node id, user id, schema JSON, response JSON, status, idempotency key, completed node id, timeout node id, and timestamps.

- [x] **Step 4: Implement handler**

`UserInputHandler` validates schema, inserts a pending response row, returns `NodeResult.pending(resumeAt, "USER_INPUT_PENDING", "waiting for user input")`, and writes trace output keys `inputStatus`, `inputResponseId`, and `timeoutNodeId`.

- [x] **Step 5: Run user input tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=UserInputHandlerTest
```

Expected: PASS.

### Task 2: WAIT Event Filter Builder

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitEventFilterTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitSubscriptionService.java`
- Modify: `frontend/src/components/config-panel/index.tsx`

- [x] **Step 1: Write filter tests**

Create tests for event code, property predicate match, timeout branch persistence, duplicate resume idempotency, and non-matching event skip.

- [x] **Step 2: Implement persisted filter matching**

Store filter JSON with WAIT subscription. Match incoming events by event code and predicates, then resume exactly once with completed or timeout branch metadata.

- [x] **Step 3: Add config panel controls**

Add event schema selector, filter builder, timeout duration, completed branch, timeout branch, and preview state for WAIT `UNTIL_EVENT`.

- [x] **Step 4: Run backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=UserInputHandlerTest,WaitEventFilterTest,WaitHandlerTest
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-017b-user-input-and-wait-event-ux.md`
- Modify: `docs/product-evolution/plans/p2-017b-user-input-and-wait-event-ux-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=UserInputHandlerTest,WaitEventFilterTest,WaitHandlerTest
```

Expected: PASS.

Commit boundary: no commit was created in this audit; commit and merge status remain unverified.

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V113__user_input_wait_event_tools.sql backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/UserInputHandler.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitSubscriptionService.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/UserInputHandlerTest.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitEventFilterTest.java frontend/src/components/config-panel docs/product-evolution/specs/p2-017b-user-input-and-wait-event-ux.md docs/product-evolution/plans/p2-017b-user-input-and-wait-event-ux-plan.md
git commit -m "feat: add user input and wait event UX"
```

Expected: commit contains only user input/wait-event schema, handler, filter logic, config panel updates, tests, and related docs.
