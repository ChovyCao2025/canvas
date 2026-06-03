# User Input And Wait Event UX Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add user input response storage and richer WAIT event filter configuration with completed/timeout branches.

**Architecture:** Persist user input forms/responses and wait event filters in V113, add `UserInputHandler`, and extend `WaitSubscriptionService` without changing unrelated WAIT duration behavior.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, Redis wait subscriptions, JUnit 5, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-017b-user-input-and-wait-event-ux.md`

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

- [ ] **Step 1: Write tests**

Create tests for missing schema rejection, pending response creation, completed branch resume, timeout branch resume, and duplicate response idempotency.

- [ ] **Step 2: Run tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=UserInputHandlerTest
```

Expected: FAIL because migration and handler do not exist.

- [ ] **Step 3: Add migration**

Create tables `user_input_form`, `user_input_response`, and `user_input_resume_audit` with tenant id, execution id, node id, user id, schema JSON, response JSON, status, idempotency key, completed node id, timeout node id, and timestamps.

- [ ] **Step 4: Implement handler**

`UserInputHandler` validates schema, inserts a pending response row, returns `NodeResult.pending(resumeAt, "USER_INPUT_PENDING", "waiting for user input")`, and writes trace output keys `inputStatus`, `inputResponseId`, and `timeoutNodeId`.

- [ ] **Step 5: Run user input tests**

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

- [ ] **Step 1: Write filter tests**

Create tests for event code, property predicate match, timeout branch persistence, duplicate resume idempotency, and non-matching event skip.

- [ ] **Step 2: Implement persisted filter matching**

Store filter JSON with WAIT subscription. Match incoming events by event code and predicates, then resume exactly once with completed or timeout branch metadata.

- [ ] **Step 3: Add config panel controls**

Add event schema selector, filter builder, timeout duration, completed branch, timeout branch, and preview state for WAIT `UNTIL_EVENT`.

- [ ] **Step 4: Run backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=UserInputHandlerTest,WaitEventFilterTest,WaitHandlerTest
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-017b-user-input-and-wait-event-ux.md`
- Modify: `docs/product-evolution/plans/p2-017b-user-input-and-wait-event-ux-plan.md`

- [ ] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=UserInputHandlerTest,WaitEventFilterTest,WaitHandlerTest
```

Expected: PASS.

- [ ] **Step 2: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V113__user_input_wait_event_tools.sql backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/UserInputHandler.java backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitSubscriptionService.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/UserInputHandlerTest.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitEventFilterTest.java frontend/src/components/config-panel docs/product-evolution/specs/p2-017b-user-input-and-wait-event-ux.md docs/product-evolution/plans/p2-017b-user-input-and-wait-event-ux-plan.md
git commit -m "feat: add user input and wait event UX"
```

Expected: commit contains only user input/wait-event schema, handler, filter logic, config panel updates, tests, and related docs.
