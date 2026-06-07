# Marketing Content Hub Design

## 1. Context

Marketing Canvas currently has journey orchestration, CDP, channel delivery, public marketing forms, and a basic message template center. It does not yet provide a production content-marketing surface for CMS-style content, DAM assets, video assets, or email design.

Active-worktree and recent-session audit on 2026-06-06 found no separate implementation branch for CMS/DAM/video/email design. Existing worktrees only contain roadmap references such as `docs/optimization/todo/plans/2026-05-31-direction-15-marketing-asset-center-P0P1P2.md`, plus adjacent but non-overlapping work such as `.worktrees/marketing-ops-platform-suite` with `CREATIVE_ASSET` work items and approval status tracking. The current main worktree already contains an untracked `message_template` foundation; this feature must build additively without replacing it.

External references used for product shape, checked on 2026-06-06:

- Mautic assets and builder model: `https://docs.mautic.org/en/7.1/components/assets.html`, `https://docs.mautic.org/en/builders`
- Strapi CMS content manager and draft/publish model: `https://docs.strapi.io/cms/features/content-manager`, `https://docs.strapi.io/cms/features/draft-and-publish`
- Cloudinary DAM metadata, tags, and video asset patterns: `https://cloudinary.com/documentation/custom_metadata`, `https://cloudinary.com/documentation/dam_manage_metadata`
- GrapesJS component and storage model for template design JSON: `https://grapesjs.com/docs/modules/Components.html`, `https://grapesjs.com/docs/modules/Storage.html`

## 2. Goal

Add a tenant-scoped Marketing Content Hub that gives operators one production-grade control plane for:

1. CMS-like reusable content entries.
2. DAM-style digital asset metadata and classification.
3. Video asset marketing metadata.
4. Email and multichannel template design data with variable preview.

The first implementation must be usable without real object-storage infrastructure by registering externally hosted assets, while keeping the schema and APIs compatible with later binary upload, CDN, and transcode workers.

## 3. Non-Goals

- Do not build a public website builder or full general-purpose CMS runtime in this slice.
- Do not stream or persist binary files in MySQL.
- Do not introduce a heavy WYSIWYG dependency before the design JSON and API contracts are stable.
- Do not replace the existing `message_template` center; expose it as a simpler legacy template workflow until migration is explicitly planned.

## 4. Architecture

Use a new `org.chovy.canvas.domain.content` boundary:

- `MarketingAssetService`: validates and stores asset metadata, tags, structured metadata, review state, and video-specific fields.
- `ContentTemplateService`: stores channel templates with subject/body/design JSON, extracted variables, asset references, preview rendering, and immutable versions.
- `ContentEntryService`: stores CMS-like content entries with slug, type, locale, body JSON, SEO JSON, asset references, draft/published/archive state, and versions.
- `MarketingContentController`: exposes authenticated tenant-scoped APIs under `/marketing/content`.

Persistence uses additive Flyway tables:

- `marketing_asset_folder`
- `marketing_asset`
- `marketing_content_template`
- `marketing_content_template_version`
- `marketing_content_entry`
- `marketing_content_entry_version`

Frontend adds `frontend/src/pages/content-hub` and `frontend/src/services/marketingContentApi.ts`. The page uses Ant Design tabs for Assets, Templates, and CMS Entries. It supports create/search, status tags, preview, and compact editor panels.

## 5. Data Model

### Asset Folders

Required fields: `tenant_id`, `folder_key`, `name`, `created_by`.

Optional fields: `parent_id`.

Folders are tenant-scoped and use normalized stable keys. The first slice supports flat or parent-linked folder organization so DAM assets can be grouped without requiring object-storage directory semantics.

### Assets

Required fields: `tenant_id`, `asset_key`, `name`, `asset_type`, `mime_type`, `storage_url`, `status`, `created_by`.

Optional but first-class fields: `folder_id`, `size_bytes`, `checksum_sha256`, `thumbnail_url`, `poster_url`, `width`, `height`, `duration_ms`, `transcode_status`, `tags_json`, `metadata_json`, `review_notes`, `reference_count`.

Allowed asset types: `IMAGE`, `FILE`, `VIDEO`, `AUDIO`.

Allowed statuses: `DRAFT`, `READY`, `REJECTED`, `ARCHIVED`.

Video-specific behavior: `VIDEO` assets may carry `durationMs`, dimensions, `posterUrl`, and `transcodeStatus` (`PENDING`, `READY`, `FAILED`, `EXTERNAL`). The first slice treats external URLs as already hosted.

### Templates

Required fields: `tenant_id`, `template_key`, `display_name`, `channel`, `status`, `body`, `created_by`.

Optional fields: `subject`, `description`, `design_json`, `asset_refs_json`, `variables_json`, `review_notes`.

Allowed channels: `EMAIL`, `SMS`, `PUSH`, `WECHAT`, `IN_APP`, `WEB`, `VIDEO`.

Allowed statuses: `DRAFT`, `PENDING_APPROVAL`, `APPROVED`, `REJECTED`, `ARCHIVED`.

Every save writes a version row with version number, content snapshot, variables, asset references, status, and creator.

### CMS Entries

Required fields: `tenant_id`, `entry_key`, `content_type`, `title`, `status`, `body_json`, `created_by`.

Optional fields: `slug`, `locale`, `summary`, `seo_json`, `asset_refs_json`, `published_at`.

Allowed statuses: `DRAFT`, `PUBLISHED`, `ARCHIVED`.

Every save or publish writes a version row.

## 6. API Requirements

All endpoints must require authenticated tenant context via `TenantContextResolver.currentOrError()`.

Assets:

- `GET /marketing/content/asset-folders`
- `POST /marketing/content/asset-folders`
- `GET /marketing/content/assets?keyword=&assetType=&status=`
- `POST /marketing/content/assets`
- `POST /marketing/content/assets/{assetKey}/status`

Templates:

- `GET /marketing/content/templates?keyword=&channel=&status=`
- `POST /marketing/content/templates`
- `POST /marketing/content/templates/{templateKey}/preview`
- `POST /marketing/content/templates/{templateKey}/status`

Entries:

- `GET /marketing/content/entries?keyword=&contentType=&status=`
- `POST /marketing/content/entries`
- `POST /marketing/content/entries/{entryKey}/publish`
- `POST /marketing/content/entries/{entryKey}/archive`

## 7. Validation And Safety

- Tenant id is required for every write and read.
- Stable keys are normalized to lower-case `[a-z0-9][a-z0-9_-]{0,127}`.
- URLs must use `http` or `https`.
- JSON fields must be valid JSON objects or arrays as appropriate.
- Tags are stored as a deduplicated JSON array of trimmed strings.
- Template preview replaces `{{variable}}` placeholders and returns missing variables.
- Writes are additive and do not mutate Flyway migrations already present.

## 8. Frontend Requirements

- Add a route and navigation item named `内容中心`.
- Assets tab: search by keyword/type/status, create asset folders, create asset metadata, assign assets to folders, show tags, type, status, storage URL, and video metadata.
- Templates tab: search by channel/status, create/edit template body, subject, design JSON, assets JSON, preview context JSON, and rendered preview.
- CMS Entries tab: search by type/status, create entries, publish/archive, show slug/locale/status.
- Handle loading, empty, error, and save states.
- Use focused presentation helpers for status labels, JSON parsing, variable extraction, and video metadata display.

## 9. Testing Requirements

Backend:

- Migration test proves all six tables and tenant indexes exist.
- Asset service tests prove folder creation/listing, tenant-safe folder references, normalization, type/status validation, tag dedupe, video metadata handling, and repository calls.
- Template service tests prove variable extraction, version writes, preview rendering, missing variables, and unsupported channel rejection.
- Entry service tests prove draft save, publish, archive, and version writes.
- Controller tests prove endpoints delegate through current tenant context.

Frontend:

- API tests prove endpoint paths and payload shapes.
- Presentation tests prove status labels, variable extraction, JSON parsing, asset/video formatting, and preview state.
- Page test proves the empty state renders and editor controls are reachable.

## 10. Rollout And Rollback

Rollout:

1. Apply additive migration.
2. Expose `/content-hub` to authenticated operators.
3. Keep existing `/message-templates` available during transition.

Rollback:

1. Hide the route/menu item.
2. Stop calling `/marketing/content/*` endpoints.
3. Leave additive tables in place; no runtime send path depends on them until a later wiring spec explicitly references content hub templates or assets.

## 11. Completion Criteria

- Spec and implementation plan exist and pass self-review.
- Backend migration, services, controllers, and focused tests are implemented.
- Frontend service, route/page, helpers, and focused tests are implemented.
- Targeted backend and frontend tests pass.
- Final audit maps CMS, DAM, video, and email-template requirements to concrete implemented evidence.
