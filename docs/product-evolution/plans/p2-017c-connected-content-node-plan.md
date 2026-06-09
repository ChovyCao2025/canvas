# Connected Content Node Implementation Plan

Status: Current implementation and focused verification passed on 2026-06-09; commit and merge status remain unverified in this audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a safe connected-content node with bounded fetch, cache, JSON path extraction, and preview.

**Architecture:** Reuse `OutboundUrlValidator`, store cache metadata in MySQL, and keep network behavior behind `ConnectedContentHandler` tests before adding config-panel controls.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, WebClient, JUnit 5, React 18, TypeScript, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p2-017c-connected-content-node.md`

## Current Status Note

The implementation files are present in the current worktree and fresh focused verification passed on 2026-06-09:

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=TemplateRenderServiceTest,UserInputHandlerTest,WaitEventFilterTest,WaitHandlerTest,ConnectedContentHandlerTest,ExecutionRerunControllerTest,CanvasBatchOperationControllerTest,RuntimeMigrationEvidenceTest` (covered `ConnectedContentHandlerTest`; 48 total selected backend tests passed).
- `npm test -- presentation.test.ts formValues.test.ts` (21 config-panel test files, 87 tests passed).

Historical RED-state checks were not reproduced because the current worktree already contains the implementation. No commit or merge was created in this audit, so commit and merge status remain unverified.

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V130__connected_content_cache.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ConnectedContentHandler.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ConnectedContentHandlerTest.java`
- Modify: `frontend/src/components/config-panel/index.tsx`

### Task 1: Cache Schema And Handler Tests

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ConnectedContentHandlerTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V130__connected_content_cache.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ConnectedContentHandler.java`

- [x] **Step 1: Write tests**

Create tests for URL allowlist rejection, timeout fallback route, payload size cap, cache hit, cache expiry, JSON path extraction, output variable binding, preview response, and provider error trace output.

Historical RED-state boundary: not reproduced in this audit because the current worktree already contains the implementation.

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ConnectedContentHandlerTest
```

Expected: FAIL because handler and cache schema do not exist.

- [x] **Step 3: Add migration**

Create `connected_content_cache` with tenant id, cache key, url hash, request hash, response JSON, status, expires at, created at, and indexes on tenant/cache key/expires at.

- [x] **Step 4: Implement handler**

Validate URL with `OutboundUrlValidator`, enforce timeout and payload byte limit, use tenant/url/body hash cache key, extract JSON paths into output keys, and return timeout/error branches with `NodeResult.routed`.

- [x] **Step 5: Run backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ConnectedContentHandlerTest
```

Expected: PASS.

### Task 2: Config Panel Controls

**Files:**
- Modify: `frontend/src/components/config-panel/index.tsx`

- [x] **Step 1: Add controls**

Add URL, method, headers, cache TTL, timeout milliseconds, payload size limit, JSON path mapping, timeout branch, and preview button for `CONNECTED_CONTENT`.

- [x] **Step 2: Run config panel tests**

Run:

```bash
cd frontend && npm test -- presentation.test.ts formValues.test.ts
```

Expected: PASS.

### Task 3: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p2-017c-connected-content-node.md`
- Modify: `docs/product-evolution/plans/p2-017c-connected-content-node-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ConnectedContentHandlerTest
cd frontend && npm test -- presentation.test.ts formValues.test.ts
```

Expected: PASS.

Commit boundary: no commit was created in this audit; commit and merge status remain unverified.

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V130__connected_content_cache.sql backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ConnectedContentHandler.java backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/ConnectedContentHandlerTest.java frontend/src/components/config-panel docs/product-evolution/specs/p2-017c-connected-content-node.md docs/product-evolution/plans/p2-017c-connected-content-node-plan.md
git commit -m "feat: add connected content node"
```

Expected: commit contains only connected content handler, cache schema, config panel controls, tests, and related docs.
