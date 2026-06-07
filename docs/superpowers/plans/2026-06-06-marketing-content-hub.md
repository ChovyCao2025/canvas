# Marketing Content Hub Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a tenant-scoped Marketing Content Hub for CMS entries, DAM assets, video asset metadata, and email/multichannel template design data.

**Architecture:** Add an additive schema, a focused `domain.content` service boundary, one authenticated controller under `/marketing/content`, and a frontend `内容中心` page. Binary upload and external transcode workers are future-compatible through storage URL, metadata, and video status fields, but the first slice is a production control plane for registered assets and reusable content.

**Tech Stack:** Java 21, Spring Boot WebFlux-style controllers, MyBatis-Plus, Flyway, JUnit 5, Mockito, AssertJ, React 18, TypeScript, Ant Design, Axios, Vitest.

---

## Spec Reference

- `docs/superpowers/specs/2026-06-06-marketing-content-hub-design.md`

## File Structure

Backend:

- Create: `backend/canvas-engine/src/main/resources/db/migration/V301__marketing_content_hub.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingAssetFolderDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingAssetDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentTemplateDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentTemplateVersionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentEntryDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentEntryVersionDO.java`
- Create: matching mapper interfaces under `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetService.java` with asset folder list/create and asset metadata operations.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/ContentTemplateService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/ContentEntryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingContentController.java`
- Create focused tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/` and `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MarketingContentControllerTest.java`

Frontend:

- Create: `frontend/src/services/marketingContentApi.ts`
- Create: `frontend/src/services/marketingContentApi.test.ts`
- Create: `frontend/src/pages/content-hub/contentHubPresentation.ts`
- Create: `frontend/src/pages/content-hub/contentHubPresentation.test.ts`
- Create: `frontend/src/pages/content-hub/index.tsx`
- Create: `frontend/src/pages/content-hub/index.test.tsx`
- Modify: `frontend/src/App.tsx`
- Modify: `frontend/src/components/layout/AppLayout.tsx`
- Modify: `frontend/src/components/accessibility/RouteA11y.tsx`

### Task 1: Backend RED Tests

- [x] Add `MarketingContentHubSchemaTest` proving `V301__marketing_content_hub.sql` contains all six tables, tenant-scoped unique keys, and status/type indexes.
- [x] Add `MarketingAssetServiceTest` proving asset folder creation/listing, tenant-safe folder references, asset key normalization, URL validation, tag dedupe, video metadata preservation, and unsupported type/status rejection.
- [x] Add `ContentTemplateServiceTest` proving template key normalization, variable extraction from `{{var}}`, version writes, preview rendering, missing-variable reporting, asset reference JSON preservation, and unsupported channel rejection.
- [x] Add `ContentEntryServiceTest` proving draft save, publish, archive, slug normalization, and version writes.
- [x] Add `MarketingContentControllerTest` proving controller methods use `TenantContextResolver.currentOrError()` and delegate tenant-scoped commands.
- [x] Run focused backend tests during TDD.
- [x] Expected RED result observed: compilation/test failures for missing schema/services/controller, then later folder-specific failures for missing folder endpoints.

### Task 2: Backend GREEN Implementation

- [x] Create `V301__marketing_content_hub.sql` with the six additive tables from the spec.
- [x] Create six DO classes and mapper interfaces following existing MyBatis-Plus naming patterns.
- [x] Implement `MarketingAssetService` with records `FolderCommand`, `FolderView`, `AssetCommand`, `AssetView`, `AssetStatusCommand`, tenant-safe folder reference validation, and repository interactions through `MarketingAssetMapper` / `MarketingAssetFolderMapper`.
- [x] Implement `ContentTemplateService` with records `TemplateCommand`, `TemplateView`, `PreviewResult`, `TemplateStatusCommand`; write a version row on create/update/status change.
- [x] Implement `ContentEntryService` with records `EntryCommand`, `EntryView`, `EntryStatusCommand`; write a version row on draft save, publish, and archive.
- [x] Implement `MarketingContentController` under `/marketing/content` with bounded-elastic wrapping around blocking service calls.
- [x] Run focused backend verification.
- [x] Expected GREEN result observed for content hub focused tests.

### Task 3: Frontend RED Tests

- [x] Add `marketingContentApi.test.ts` proving API calls hit folder, asset, template, preview/status, and entry endpoints.
- [x] Add `contentHubPresentation.test.ts` proving status labels, variable extraction, JSON parsing, asset reference parsing, and video metadata formatting.
- [x] Add `content-hub/index.test.tsx` proving the page renders loaded data and empty states.
- [x] Run focused frontend tests during TDD.
- [x] Expected RED result observed: imports/methods failed before implementation.

### Task 4: Frontend GREEN Implementation

- [x] Create `marketingContentApi.ts` with typed folder/asset/template/entry payloads and API methods.
- [x] Create `contentHubPresentation.ts` with pure helpers for statuses, variable extraction, JSON parsing, and formatting.
- [x] Create `content-hub/index.tsx` with tabs for Assets, Templates, and CMS Entries, including folder creation, create/search/preview/publish/archive controls.
- [x] Register `/content-hub` in `App.tsx`, navigation in `AppLayout.tsx`, and route announcement in `RouteA11y.tsx`.
- [x] Run the focused frontend test command again.
- [x] Expected GREEN result observed: focused frontend tests pass.

### Task 5: Verification And Audit

- [x] Run backend focused tests:
  `cd backend && mvn -pl canvas-engine test -Dtest=MarketingContentHubSchemaTest,MarketingAssetServiceTest,ContentTemplateServiceTest,ContentEntryServiceTest,MarketingContentControllerTest`
- [x] Run frontend focused tests:
  `cd frontend && npm run test -- src/services/marketingContentApi.test.ts src/pages/content-hub/contentHubPresentation.test.ts src/pages/content-hub/index.test.tsx`
- [x] Run frontend build if focused tests pass:
  `cd frontend && npm run build`
- [x] Audit the spec completion criteria against current files and command output.
- [x] Record incomplete global-test limitations honestly instead of treating unrelated dirty-worktree failures as content hub failures.

## Verification Log

- Active-session/worktree audit on 2026-06-06:
  - `git worktree list --porcelain` found active worktrees for homepage material ops, integration hub, marketing ops platform suite, optimization, reactive boundaries, and conversational session foundation.
  - Process inspection found multiple Codex sessions and Vite dev servers, including main `/Users/photonpay/project/canvas`, `.worktrees/marketing-ops-platform-suite`, and `.worktrees/integration-hub-control-plane`.
  - Filtered user-message scan of recent Codex JSONL sessions found the explicit CMS/DAM/content-marketing goal only in this active goal thread; other hits were roadmap/reference docs or goal continuation metadata.
  - Other-worktree changed-file audit found `marketing-ops` work-item/audit implementation (`V300__marketing_ops_platform_suite.sql`, `MarketingOpsPlatformService`, `CREATIVE_ASSET` domain) and integration/conversation work, but no CMS/DAM/content hub schema, services, controller, or frontend route. This content hub implementation is additive and uses `V301` to avoid the marketing-ops `V300` migration.
- Follow-up active-session/worktree audit after user challenge on 2026-06-06:
  - `git worktree list` again showed `.worktrees/marketing-ops-platform-suite`, `.worktrees/homepage-material-ops-dashboard`, `.worktrees/integration-hub-control-plane`, and other active branches.
  - Changed-file scan under `.worktrees/marketing-ops-platform-suite` showed `MarketingOpsPlatform*`, `MarketingOpsWorkItem*`, `MarketingOpsAuditEvent*`, and `V300__marketing_ops_platform_suite.sql`; it did not contain `MarketingContent*`, `marketing_content_*`, `/content-hub`, CMS, or DAM implementation files.
  - Process scan showed active Codex sessions plus Vite servers for the main worktree, marketing ops suite, and integration hub. No running process evidence changed the conclusion that CMS/DAM/content hub belongs in this implementation thread.
- Backend conversation testCompile drift check: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=ConversationAdapterCatalogTest,ConversationAdapterHarnessTest -DfailIfNoTests=false` compiled 538 test sources and ran 7 tests with 0 failures.
- Backend focused GREEN: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine test -Dtest=MarketingContentHubSchemaTest,MarketingAssetServiceTest,ContentTemplateServiceTest,ContentEntryServiceTest,MarketingContentControllerTest -DfailIfNoTests=false` compiled 538 test sources and ran 16 tests with 0 failures.
- Backend main clean compile: `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine clean compile -DskipTests` compiled 1170 main sources from a clean `target` and ended with `BUILD SUCCESS`.
- Backend hardening: JSON validation was tightened so `designJson`, `bodyJson`, and `seoJson` require JSON objects, while `assetRefsJson` requires a JSON array. The old broad `MarketingContentSupport.normalizeJson` helper was removed to prevent accidental reuse.
- Current backend focused GREEN after hardening: manually compiled the content hub DOs/mappers/services/controller/tests with Java 21 against the existing Maven classpath, then ran JUnit Console from `backend/canvas-engine`; `ContentTemplateServiceTest`, `ContentEntryServiceTest`, `MarketingAssetServiceTest`, `MarketingContentHubSchemaTest`, and `MarketingContentControllerTest` ran 16 tests with 0 failures.
- Current global backend limitation: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -f backend/canvas-engine/pom.xml -DskipTests compile` is blocked by unrelated dirty-worktree compile failures outside content hub, including duplicate `known(Double)` in `CdpWarehouseEnterpriseOlapEvidenceService` and missing generated accessor methods in analytics/audience/warehouse/ai classes. These failures pre-exist outside the content hub slice and were not fixed here to avoid overwriting other sessions.
- Backend TDD RED: initial content hub focused tests failed before schema/services/controller existed; DAM folder tests later failed before folder APIs existed.
- Frontend TDD RED: `marketingContentApi`, content hub presentation/page modules, then folder API methods failed before implementation.
- Frontend focused GREEN: `npm run test -- marketingContentApi.test.ts contentHubPresentation.test.ts src/pages/content-hub/index.test.tsx AppLayout.a11y.test.tsx src/pages/bi/biWorkbench.test.ts` passed 5 files / 62 tests. Vitest printed jsdom pseudo-element `getComputedStyle` not-implemented notices, with no test failures.
- Frontend build: `npm run build` passed and produced `content-hub-D3TPTPWw.js`.
- Runtime route: `curl -I --max-time 5 http://127.0.0.1:3000/content-hub` returned HTTP 200 from the Vite dev server.
- Current frontend focused GREEN: `npm run test -- src/services/marketingContentApi.test.ts src/pages/content-hub/contentHubPresentation.test.ts src/pages/content-hub/index.test.tsx` passed 3 files / 7 tests. Vitest printed the same jsdom pseudo-element `getComputedStyle` warnings, with no failures.
- Current frontend build: `npm run build` passed and produced `frontend/dist/assets/content-hub-Bvh-IhfT.js`.
- Current runtime route check: `curl -I --max-time 5 http://127.0.0.1:3000/content-hub` returned HTTP 200 from the Vite dev server.
- Browser verification limitation: the Browser plugin workflow was attempted after reading `browser:control-in-app-browser`, but the in-app browser returned `Browser is not available: iab`; screenshot/DOM browser verification is therefore unavailable in this session. HTTP route, frontend tests, and production build are the current UI evidence.

## Self-Review

- Spec coverage: tasks cover schema, services, controllers, frontend service/page/helpers, route wiring, tests, and audit.
- Placeholder scan: no `TODO`, `TBD`, or `FIXME` implementation markers were found in the content hub spec, plan, backend services/controller, frontend page/helpers, or API service; UI placeholder attributes are expected form hints.
- Scope control: this is a production control-plane slice for CMS/DAM/video/template design; binary upload, CDN, and transcode workers remain compatible but not implemented in this first slice.
