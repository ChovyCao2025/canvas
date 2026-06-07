# Marketing Content Closed Loop Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a production-grade content lifecycle loop for validating, publishing, auditing, resolving, and rolling back content hub releases.

**Architecture:** Extend the existing `domain.content` boundary with additive release/audit/upload tables and release/upload services. Runtime consumers read immutable release snapshots rather than draft tables; draft services remain responsible for authoring. The implementation includes frontend readiness UI, provider upload handoff/callback records, and a message-delivery seam through `contentReleaseKey`.

**Tech Stack:** Java 21, Spring Boot WebFlux controllers, MyBatis-Plus, Flyway, Jackson, JUnit 5, AssertJ, Mockito, React 18, TypeScript, Ant Design, Vitest.

---

## Spec Reference

- `docs/superpowers/specs/2026-06-06-marketing-content-closed-loop-design.md`

## File Structure

Backend create:

- `backend/canvas-engine/src/main/resources/db/migration/V327__marketing_content_release_loop.sql`
- `backend/canvas-engine/src/main/resources/db/migration/V333__marketing_asset_upload_intent.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentReleaseDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentReleaseItemDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingContentAuditEventDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingAssetUploadIntentDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingContentReleaseMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingContentReleaseItemMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingContentAuditEventMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/MarketingAssetUploadIntentMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingContentReleaseService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingAssetUploadService.java`

Backend modify:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/content/MarketingContentSupport.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingContentController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/SendMessageHandler.java`

Backend tests create:

- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingContentReleaseSchemaTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingContentReleaseServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MarketingContentReleaseControllerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingAssetUploadIntentSchemaTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/content/MarketingAssetUploadServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/MarketingContentUploadControllerTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerContentReleaseTest.java`

Frontend later:

- `frontend/src/services/marketingContentApi.ts`
- `frontend/src/services/marketingContentApi.test.ts`
- `frontend/src/pages/content-hub/contentHubPresentation.ts`
- `frontend/src/pages/content-hub/contentHubPresentation.test.ts`
- `frontend/src/pages/content-hub/index.tsx`
- `frontend/src/pages/content-hub/index.test.tsx`

## Task 1: Backend RED Tests For Release Loop

- [x] **Step 1: Write schema test**

Create `MarketingContentReleaseSchemaTest` that reads `V327__marketing_content_release_loop.sql` and asserts:

- `marketing_content_release`
- `marketing_content_release_item`
- `marketing_content_audit_event`
- unique key `uk_marketing_content_release_version`
- index `idx_marketing_content_release_latest`
- index `idx_marketing_content_audit_target`

- [x] **Step 2: Write service tests**

Create `MarketingContentReleaseServiceTest` with Mockito rows proving:

- `validate` fails when a referenced asset is missing.
- `validate` fails when a video asset is not READY/EXTERNAL.
- `publish` requires template `APPROVED`, writes release, release items, audit event, and increments asset reference count.
- `publish` requires entry `PUBLISHED`.
- `resolve` renders template variables from immutable release snapshot.
- `rollback` changes an active release to `ROLLED_BACK` and writes audit.

- [x] **Step 3: Write controller test**

Create `MarketingContentReleaseControllerTest` proving `/marketing/content/releases/*` endpoints use `TenantContextResolver.currentOrError()` and delegate to `MarketingContentReleaseService`.

- [x] **Step 4: Run backend RED**

Run focused compilation/tests. Expected: fail because migration, DOs, mappers, service, and controller endpoints do not exist yet.

## Task 2: Backend GREEN Schema And Data Objects

- [x] **Step 1: Add `V327__marketing_content_release_loop.sql`**

Create three additive tables with tenant scoped keys and indexes. Do not modify `V301`.

- [x] **Step 2: Add DO classes and mapper interfaces**

Follow existing MyBatis-Plus patterns with `@Data`, `@TableName`, `@TableId(type = IdType.AUTO)`, and `FieldFill` timestamps.

- [x] **Step 3: Run schema-focused test**

Expected: schema test passes; service/controller tests still fail.

## Task 3: Backend GREEN Release Service

- [x] **Step 1: Implement asset reference parsing**

Add helper support for arrays containing either string asset keys or objects like `{ "assetKey": "hero_video" }`. Normalize keys with existing key rules.

- [x] **Step 2: Implement validation**

Validate template/entry status and asset readiness. Return structured `ValidationResult` with `ready`, `blockers`, and `assetRefs`.

- [x] **Step 3: Implement publish**

Generate release key from `sourceType + sourceKey`; compute next release version from latest release; write release snapshot, release items, audit event, and reference-count updates.

- [x] **Step 4: Implement resolve**

Return release snapshot, asset snapshots, rendered subject/body for template releases, and missing variables.

- [x] **Step 5: Implement rollback**

Set active release to `ROLLED_BACK`, persist reason, and write audit.

- [x] **Step 6: Run service-focused tests**

Expected: release service tests pass.

## Task 4: Backend Controller Endpoints

- [x] **Step 1: Inject `MarketingContentReleaseService` into `MarketingContentController`**

Keep existing asset/template/entry methods unchanged.

- [x] **Step 2: Add endpoints**

Implement:

- `POST /marketing/content/releases/validate`
- `POST /marketing/content/releases/publish`
- `GET /marketing/content/releases`
- `POST /marketing/content/releases/{releaseKey}/resolve`
- `POST /marketing/content/releases/{releaseKey}/rollback`
- `GET /marketing/content/audit-events`

- [x] **Step 3: Run controller tests**

Expected: controller tests pass.

## Task 5: Frontend Readiness Panel

- [x] Add release-loop methods and tests to `marketingContentApi`.
- [x] Add status/blocker presentation helpers and tests.
- [x] Add a production readiness panel in `content-hub/index.tsx` for validate/publish/resolve/rollback/audit.
- [x] Run focused frontend tests and `npm run build`.

## Task 6: Verification And Audit

- [x] Run focused backend release-loop tests.
- [x] Run existing content-hub backend tests.
- [x] Run focused frontend content-hub tests.
- [x] Run frontend build.
- [x] Record remaining global backend blockers honestly if unrelated dirty-worktree compile failures still block full Maven.

## Task 7: Provider Upload And Delivery Seam

- [x] Add `V333__marketing_asset_upload_intent.sql` with tenant-scoped upload intent keys, upload token uniqueness, asset/status index, and provider asset index.
- [x] Add `MarketingAssetUploadIntentDO` and `MarketingAssetUploadIntentMapper`.
- [x] Add `MarketingAssetUploadService` for provider handoff creation, callback completion, DAM asset upsert, and audit events.
- [x] Add upload intent and callback endpoints to `MarketingContentController`.
- [x] Add `contentReleaseKey` resolution to `SendMessageHandler` so immutable release snapshots flow into delivery payloads.
- [x] Add frontend API methods for upload intent and provider callback.
- [x] Run focused backend upload/delivery tests, frontend content-hub tests, and frontend build.

## Progress Log

- 2026-06-06: Current state audited. Existing content hub is a control plane; missing production release gate, immutable runtime snapshots, audit, rollback, provider upload handoff, and delivery integration.
- 2026-06-06: External references checked for Contentful management/delivery split, Cloudinary metadata/upload/webhook model, Mux direct upload/webhook model, SendGrid template versions, and GrapesJS project JSON storage.
- 2026-06-06: Backend release loop implemented and verified with 25 focused content-hub tests: CMS, DAM, template, release schema, service, controller, and tenant delegation.
- 2026-06-06: Frontend readiness panel implemented and verified with 9 focused content-hub tests plus `npm run build`.
- 2026-06-06: Provider upload handoff/callback and message delivery release resolution implemented. Focused backend content-marketing aggregation passed with 26 tests; frontend content-hub tests passed with 9 tests; frontend build passed.
- 2026-06-06: Remaining deployment integration: real provider credentials, signed upload URL generation, and binary upload streaming are environment/provider adapter concerns. The application contract and persistence/audit seams are implemented.

## Self-Review

- Spec coverage: plan covers release schema, validation, publish, audit, resolve, rollback, controller endpoints, frontend readiness UI, upload intent/callback handoff, and delivery resolution.
- Scope control: real cloud signing and binary streaming stay outside the app core and are delegated to provider adapters behind the upload intent contract.
- Placeholder scan: no `TODO`/`TBD` placeholders; remaining work is deployment/provider integration, not an unimplemented application seam.
