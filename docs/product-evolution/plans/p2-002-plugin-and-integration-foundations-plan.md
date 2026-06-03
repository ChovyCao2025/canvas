# Plugin And Integration Foundations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create safe internal plugin foundations and integration primitives without exposing unsafe third-party runtime loading.

**Architecture:** Implement the capability as a thin vertical slice: failing tests first, then backend/domain contracts, then frontend service and route integration, then rollout documentation. Keep scope bounded to the spec and use additive migrations or feature flags for risky changes.

**Tech Stack:** Java 21, Spring Boot WebFlux style controllers currently returning Mono, MyBatis, Flyway, Redis/RocketMQ where needed, React 18, Vite, TypeScript, Ant Design, Vitest, JUnit 5, Mockito.

---

## Spec Reference

- `docs/product-evolution/specs/p2-002-plugin-and-integration-foundations.md`
- Source item: `docs/product-evolution/todo/p2/plugin-and-integration-foundations.md`

## File Structure

**Backend**
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/HandlerRegistry.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ApiDefinitionController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/common/OutboundUrlValidator.java`

**Frontend**
- `frontend/src/pages/api-docs/index.tsx`
- `frontend/src/pages/system-options/index.tsx`
- `frontend/src/services/api.ts`

**Data And Config**
- `backend/canvas-engine/src/main/resources/db/migration/V101__plugin_integration_foundations.sql`

**Tests**
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/ApiDefinitionControllerTest.java`
- `frontend/src/pages/api-docs/pluginIntegrationDocs.test.tsx`

### Task 1: Contract And Failing Tests

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java`
- Create: `frontend/src/pages/api-docs/pluginIntegrationDocs.test.tsx`
- Read: `docs/product-evolution/specs/p2-002-plugin-and-integration-foundations.md`

- [ ] **Step 1: Write backend contract tests**

Create `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java` with tests for authorization, tenant scoping, and the main success path named after `plugin-and-integration-foundations`. Use existing controller or service tests in `backend/canvas-engine/src/test/java/org/chovy/canvas` as style references.

- [ ] **Step 2: Run backend contract tests and confirm red state**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=PluginRegistryServiceTest`

Expected: FAIL because the new route, service method, or behavior has not been implemented yet.

- [ ] **Step 3: Write frontend workflow tests**

Create `frontend/src/pages/api-docs/pluginIntegrationDocs.test.tsx` with Vitest coverage for loading, empty, success, permission, and server-error states for the first UI slice.

- [ ] **Step 4: Run frontend workflow tests and confirm red state**

Run: `cd frontend && npm test -- pluginIntegrationDocs.test.tsx`

Expected: FAIL because the new page, component, service call, or state handling does not exist yet.

### Task 2: Backend And Data Slice

**Files:**
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/HandlerRegistry.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ApiDefinitionController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/common/OutboundUrlValidator.java`
- `backend/canvas-engine/src/main/resources/db/migration/V101__plugin_integration_foundations.sql`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java`

- [ ] **Step 1: Add additive data structures when the spec requires storage**

If this plan has a Flyway file, create it exactly at the data path listed above. Use additive tables, indexes, and nullable columns so rollout can be disabled without rollback data loss.

- [ ] **Step 2: Implement the domain service**

Implement the service behavior in the backend files listed above. Keep business rules in the domain service and keep controllers thin.

- [ ] **Step 3: Implement or extend the controller contract**

Expose only the endpoints needed by the first workflow. Return `R<T>` or existing project response types consistently with nearby controllers.

- [ ] **Step 4: Run focused backend tests**

Run: `cd backend && mvn -pl canvas-engine test -Dtest=PluginRegistryServiceTest`

Expected: PASS for the new contract tests.

### Task 3: Frontend Slice

**Files:**
- `frontend/src/pages/api-docs/index.tsx`
- `frontend/src/pages/system-options/index.tsx`
- `frontend/src/services/api.ts`
- Test: `frontend/src/pages/api-docs/pluginIntegrationDocs.test.tsx`

- [ ] **Step 1: Add the typed API wrapper**

Implement the API wrapper in the service file listed above. Reuse `frontend/src/services/api.ts` and return typed request and response objects.

- [ ] **Step 2: Add the route, page, panel, or component**

Implement the first visible workflow in the frontend files listed above. Include loading, empty, error, permission, and success states.

- [ ] **Step 3: Wire navigation only where needed**

Add navigation entry points only if the feature needs a top-level route. Prefer contextual entry points for editor, analytics, or settings capabilities.

- [ ] **Step 4: Run focused frontend tests**

Run: `cd frontend && npm test -- pluginIntegrationDocs.test.tsx`

Expected: PASS for the new workflow tests.

### Task 4: Integration Verification And Rollout Notes

**Files:**
- Modify: `docs/product-evolution/specs/p2-002-plugin-and-integration-foundations.md`
- Modify: `docs/product-evolution/plans/p2-002-plugin-and-integration-foundations-plan.md`
- Read: `docs/product-evolution/todo/INDEX.md`

- [ ] **Step 1: Run backend regression slice**

Run: `cd backend && mvn -pl canvas-engine test`

Expected: PASS for the canvas-engine module test suite.

- [ ] **Step 2: Run frontend regression slice**

Run: `cd frontend && npm test -- --run`

Expected: PASS for the Vitest suite.

- [ ] **Step 3: Run frontend build**

Run: `cd frontend && npm run build`

Expected: PASS with TypeScript and Vite build success.

- [ ] **Step 4: Add rollout notes to the implementation PR**

Document feature flag or route guard, migration order, tenant and role impact, manual verification steps, and rollback command or disable switch.

- [ ] **Step 5: Commit the implementation slice**

Run: `git add backend/canvas-engine/src frontend/src docs/product-evolution/specs docs/product-evolution/plans && git commit -m "feat: implement plugin-and-integration-foundations slice"`

Expected: commit contains only files required by this plan.
