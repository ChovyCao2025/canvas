# Test Users And Single User Rerun Implementation Plan

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add seed/test user management and single-user rerun modes with explicit side-effect controls.

**Architecture:** Store test user sets and rerun audit rows, expose controllers for seed-user preview and rerun requests, and require reason text plus safe default rerun modes.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, JUnit 5, React 18, TypeScript, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-017d-test-users-and-single-user-rerun.md`

## Current Status Note

The implementation files are present in the current worktree and fresh focused verification passed on 2026-06-09:

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=TemplateRenderServiceTest,UserInputHandlerTest,WaitEventFilterTest,WaitHandlerTest,ConnectedContentHandlerTest,ExecutionRerunControllerTest,CanvasBatchOperationControllerTest,RuntimeMigrationEvidenceTest` (covered `ExecutionRerunControllerTest`; 48 total selected backend tests passed).
- `npm run build` (frontend TypeScript and Vite production build passed).

Historical RED-state checks were not reproduced because the current worktree already contains the implementation. No commit or merge was created in this audit, so commit and merge status remain unverified.

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V131__test_users_and_rerun_audit.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TestUserController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionRerunController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionRerunControllerTest.java`
- Create: `frontend/src/pages/test-users/index.tsx`
- Create: `frontend/src/services/executionRerunApi.ts`

### Task 1: Backend Rerun Controls

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionRerunControllerTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V131__test_users_and_rerun_audit.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/TestUserController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionRerunController.java`

- [x] **Step 1: Write tests**

Create tests for seed-user creation, preview context, rerun reason required, default dry-run mode, side-effect skip mode, admin replay mode, original execution reference, and audit row creation.

Historical RED-state boundary: not reproduced in this audit because the current worktree already contains the implementation.

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ExecutionRerunControllerTest
```

Expected: FAIL because migration and controllers do not exist.

- [x] **Step 3: Add migration**

Create `test_user_set`, `test_user`, and `execution_rerun_audit` tables with tenant id, user id, profile JSON, input params JSON, original execution id, mode, reason, operator, status, and timestamps.

- [x] **Step 4: Implement controllers**

Expose seed-user list/create/detail/preview and rerun request/status. Rerun mode defaults to `DRY_RUN`; `ADMIN_REPLAY` requires admin role and reason length at least 10 characters.

- [x] **Step 5: Run backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ExecutionRerunControllerTest
```

Expected: PASS.

### Task 2: Frontend Test User And Rerun API

**Files:**
- Create: `frontend/src/services/executionRerunApi.ts`
- Create: `frontend/src/pages/test-users/index.tsx`

- [x] **Step 1: Add typed API wrapper**

Add calls for test user sets, test user detail, preview context, rerun request, rerun status, and rerun audit list.

- [x] **Step 2: Add test user page**

Add test user list, create/edit drawer, selected user preview, and rerun action entry that requires mode and reason.

- [x] **Step 3: Run frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-017d-test-users-and-single-user-rerun.md`
- Modify: `docs/product-evolution/plans/p2-017d-test-users-and-single-user-rerun-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ExecutionRerunControllerTest
cd frontend && npm run build
```

Expected: PASS.

Commit boundary: no commit was created in this audit; commit and merge status remain unverified.

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V131__test_users_and_rerun_audit.sql backend/canvas-engine/src/main/java/org/chovy/canvas/web/TestUserController.java backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionRerunController.java backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionRerunControllerTest.java frontend/src/pages/test-users frontend/src/services/executionRerunApi.ts docs/product-evolution/specs/p2-017d-test-users-and-single-user-rerun.md docs/product-evolution/plans/p2-017d-test-users-and-single-user-rerun-plan.md
git commit -m "feat: add test users and single user rerun"
```

Expected: commit contains only test-user/rerun schema, controllers, frontend wrapper/page, tests, and related docs.
