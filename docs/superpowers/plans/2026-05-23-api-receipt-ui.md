# API Receipt UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add API receipt configuration and improve the API configuration modal with a two-column live preview layout.

**Architecture:** Extend existing API definition persistence with receipt fields, keep preview generation in `requestPreview.ts`, and keep the modal as a single page with left-side configuration and right-side JSON previews. Receipt receiving remains a later backend workflow.

**Tech Stack:** React 18, Ant Design, Vitest, Spring Boot WebFlux, MyBatis Plus, Flyway, JUnit 5, Mockito.

---

### Task 1: Frontend Receipt Preview Helper

**Files:**
- Modify: `frontend/src/pages/api-config/requestPreview.ts`
- Modify: `frontend/src/pages/api-config/requestPreview.test.ts`

- [ ] Add failing tests for `buildApiReceiptPreview` and receipt submit normalization.
- [ ] Run `npm test -- src/pages/api-config/requestPreview.test.ts` and verify the tests fail because receipt helpers do not exist.
- [ ] Implement `ApiReceiptStatus`, `buildApiReceiptPreview`, and receipt fields in `normalizeApiDefinitionPayload`.
- [ ] Run the same Vitest command and verify it passes.

### Task 2: API Config Modal Layout

**Files:**
- Modify: `frontend/src/pages/api-config/index.tsx`

- [ ] Add `Tabs` and `InputNumber` imports.
- [ ] Add a `ReceiptStatusEditor` component.
- [ ] Parse and set receipt defaults in create/edit flows.
- [ ] Render two-column modal layout: left configuration, right preview.
- [ ] Add request Body and receipt JSON tabs.
- [ ] Run frontend test and build.

### Task 3: Backend Receipt Persistence Defaults

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V45__api_receipt_settings.sql`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/ApiDefinition.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/controller/ApiDefinitionController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/ApiDefinitionControllerTest.java`

- [ ] Add failing controller test for default receipt fields on create.
- [ ] Run `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ApiDefinitionControllerTest test` and verify it fails.
- [ ] Add Flyway fields, domain fields, and controller defaulting.
- [ ] Run the same backend test and verify it passes.

### Task 4: Final Verification

- [ ] Run `npm test -- src/pages/api-config/requestPreview.test.ts`.
- [ ] Run `npm run build`.
- [ ] Run `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ApiDefinitionControllerTest,ApiCallPayloadBuilderTest test`.
- [ ] Run `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -DskipTests compile`.
- [ ] Run `git diff --check`.
