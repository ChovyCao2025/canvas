# Marketing Content Closed Loop Design

## 1. Context

The current content hub gives operators a tenant-scoped control plane for CMS entries, DAM assets, video metadata, and template design JSON. The production closure work adds a release gate, immutable runtime snapshots, audit evidence, formal asset reference validation, provider upload handoff/callback records, and a delivery seam for message nodes.

External product references checked on 2026-06-06:

- Contentful environments and delivery separation: `https://www.contentful.com/developers/docs/concepts/multiple-environments/`
- Contentful Content Management and Content Delivery APIs: `https://www.contentful.com/developers/docs/references/content-management-api/`, `https://www.contentful.com/developers/docs/references/content-delivery-api/`
- Cloudinary upload parameters, upload presets, metadata/tags, and webhooks: `https://cloudinary.com/documentation/upload_parameters`, `https://cloudinary.com/documentation/upload_presets`, `https://cloudinary.com/documentation/notifications`
- GrapesJS project JSON storage model: `https://grapesjs.com/docs/modules/Storage.html`
- SendGrid dynamic templates and versions: `https://www.twilio.com/docs/sendgrid/ui/sending-email/how-to-send-an-email-with-dynamic-templates`
- Mux direct-upload and webhook patterns: `https://docs.mux.com/guides/upload-files-directly`

## 2. Goal

Build a production-grade content lifecycle loop:

1. Author or register content and assets.
2. Validate referenced assets and content status before production use.
3. Approve and publish immutable runtime snapshots.
4. Resolve snapshots for downstream journeys, message sending, landing pages, or external delivery adapters.
5. Write audit evidence and update usage/reference counters for operational traceability.
6. Prepare clean seams for provider upload intents and asynchronous media webhooks.
7. Allow downstream message delivery nodes to resolve approved release snapshots by release key.

This spec extends the existing content hub without replacing the first slice.

## 3. Non-Goals For This Slice

- Do not store real cloud credentials or stream binary uploads through the Java application. Provider-specific signing is supplied by deployment adapters behind the upload intent handoff.
- Do not wire every canvas node to content releases immediately. The message send node is the first delivery integration point through `contentReleaseKey`.
- Do not introduce a heavy visual editor dependency before the stored design JSON, versioning, and release APIs are stable.

## 4. Production Loop Architecture

Use a new `MarketingContentReleaseService` inside `org.chovy.canvas.domain.content`.

Core responsibilities:

- `validate`: resolve a template or CMS entry and its `assetRefsJson`, require production-ready statuses, collect missing/invalid dependencies, and return a gate report.
- `publish`: require the gate report to pass, write a release snapshot row and release items, update asset reference counters, and write an audit event.
- `resolve`: fetch an immutable release snapshot by release key and version or by latest active release, render template variables when requested, and return assets/content needed by downstream delivery.
- `rollback`: move a release to `ROLLED_BACK` and optionally reactivate a previous version.

Keep runtime reads separate from draft management, following the same practical split as CMS management APIs versus delivery APIs. Use additive tables only.

## 5. Data Model

### `marketing_content_release`

Stores one immutable production snapshot per content key and version.

Required fields:

- `tenant_id`
- `release_key`
- `source_type`: `TEMPLATE` or `ENTRY`
- `source_key`
- `source_version`
- `channel`
- `status`: `ACTIVE`, `SUPERSEDED`, `ROLLED_BACK`, `FAILED`
- `snapshot_json`
- `asset_refs_json`
- `created_by`

Operational fields:

- `checksum_sha256`
- `published_at`
- `rollback_reason`
- timestamps

Uniqueness:

- `(tenant_id, release_key, source_version)` is unique.
- `(tenant_id, source_type, source_key, status, published_at)` is indexed for latest-active lookup.

### `marketing_content_release_item`

Stores each dependency included in a release.

Fields:

- `tenant_id`
- `release_id`
- `item_type`: `ASSET`, `TEMPLATE`, `ENTRY`
- `item_key`
- `item_status`
- `snapshot_json`

### `marketing_content_audit_event`

Stores append-only lifecycle evidence.

Fields:

- `tenant_id`
- `event_type`
- `target_type`
- `target_key`
- `actor`
- `old_value_json`
- `new_value_json`
- `note`
- `created_at`

### `marketing_asset_upload_intent`

Stores upload handoff metadata for Cloudinary/Mux/S3-style adapters.

Fields:

- `tenant_id`
- `intent_key`
- `asset_key`
- `asset_type`
- `provider`
- `mime_type`
- `file_name`
- `size_bytes`
- `upload_token`
- `upload_url`
- `upload_params_json`
- `status`: `PENDING`, `COMPLETED`, `FAILED`
- `provider_asset_id`
- `callback_json`
- `expires_at`

## 6. Release Gate Rules

Template releases:

- Template must exist in tenant.
- Template status must be `APPROVED`.
- `designJson` must be a JSON object.
- `assetRefsJson` must be a JSON array of string keys or objects with `assetKey`.
- Every referenced asset must exist in tenant and have status `READY`.
- Video assets must have `transcodeStatus` `READY` or `EXTERNAL`.
- Render preview may leave missing variables only when explicitly called in preview mode; publish mode records variables but does not require sample values.

CMS entry releases:

- Entry must exist in tenant.
- Entry status must be `PUBLISHED`.
- `bodyJson` and `seoJson` must be JSON objects.
- `assetRefsJson` follows the same asset rules as templates.

Rollback:

- Only `ACTIVE` releases can be rolled back.
- Rollback writes audit evidence and changes status to `ROLLED_BACK`.

## 7. API Requirements

Authenticated tenant context is required for all endpoints.

- `POST /marketing/content/releases/validate`
- `POST /marketing/content/releases/publish`
- `GET /marketing/content/releases?sourceType=&sourceKey=&status=`
- `POST /marketing/content/releases/{releaseKey}/resolve`
- `POST /marketing/content/releases/{releaseKey}/rollback`
- `GET /marketing/content/audit-events?targetType=&targetKey=&limit=`
- `POST /marketing/content/assets/upload-intents`

Provider/scanner upload completion is not a tenant management API. It is accepted only through the signed public webhook:

- `POST /public/marketing/content/assets/upload-callbacks/{tenantId}/{provider}`

The API returns validation failures as structured data, not as string-only errors, so the frontend can show actionable blockers.

## 8. Frontend Requirements

Add a production-readiness panel to `内容中心`:

- Validate selected template or entry.
- Show gate blockers grouped by asset/template/entry.
- Publish a release when the gate passes.
- Show active release version, release checksum, and last audit events.
- Resolve a release with sample context for delivery preview.

## 9. Testing Requirements

Backend:

- Schema test proves all three closed-loop tables, keys, and indexes.
- Release service tests cover passing validation, missing asset, unready video, template publish, entry publish, reference count updates, audit writes, resolve rendering, and rollback.
- Controller tests prove tenant-scoped endpoints delegate to the release service.
- Upload intent tests cover provider handoff creation, audit writes, callback completion, and DAM asset upsert.
- Delivery seam test proves `contentReleaseKey` resolves immutable snapshots into message delivery payloads.

Frontend:

- API tests cover validation, publish, list, resolve, rollback, and audit endpoints.
- API tests cover upload intent and provider callback endpoints.
- Presentation tests cover blocker grouping and release status labels.
- Page tests cover readiness panel behavior.

## 10. Completion Criteria

- Spec and plan exist.
- Backend release gate, release snapshot, audit, resolve, and rollback are implemented with focused tests.
- Provider upload intent/callback handoff and message delivery release resolution are implemented with focused tests.
- Frontend production-readiness panel is implemented with focused tests.
- Focused backend and frontend verifications pass.
- Remaining non-goals are documented as follow-up work, not implied complete.
