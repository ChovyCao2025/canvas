# Project Governance Design

Date: 2026-06-04

## Context

The comparison document `docs/optimizer/n8n-mica-comparison-2026-06.md` identifies n8n-style Project governance as the most relevant capability for Canvas. The current system already has tenant isolation, coarse user roles, canvas execution limits, audit-related tables, AI foundations, and a `canvas_project_folder` metadata service. What it does not yet have is a formal Project as a governance unit.

Today, project and folder metadata is attached to canvases as classification data. That is useful for navigation, import/export metadata, and organizing canvases, but it is not enough for enterprise governance. A Project should define who can work on a group of canvases, what default operating rules apply, and how administrators monitor the group.

This design adds Project as a long-lived governance domain inside a tenant. Folders remain classification and navigation inside a Project.

## Goals

- Add a formal Project entity under each tenant.
- Keep Folder as a lightweight organization view under Project.
- Let canvases belong to one Project and optionally one Folder.
- Add simple project-scoped roles for canvas management.
- Add project-level default policies and project-level statistics.
- Reuse the existing tenant model, auth context, canvas APIs, and `canvas_project_folder` compatibility surface.
- Keep the first phase focused on Canvas governance only.

## Non-Goals

- No credential isolation in the first phase.
- No variable or data-table permission matrix in the first phase.
- No SSO group provisioning in the first phase.
- No environment model such as dev/staging/prod in the first phase.
- No AI campaign generation work in this design.
- No CLI or browser extension work in this design.
- No replacement of Folder with Project.

## Recommended Product Model

The hierarchy is:

```text
Tenant
  Project
    Folder
      Canvas
```

Project is a governance boundary. It represents a long-lived business line, brand, region, or operating team, such as "Member Growth" or "Cross-Border Marketing".

Folder is a navigation boundary. It represents categories, campaign themes, lifecycle stages, or activity groupings inside a Project, such as "New User Conversion", "Dormant User Recall", or "618 Promotion".

Canvas is the executable workflow. It keeps its existing lifecycle, versions, trigger type, execution limits, and publish/offline/archive state.

## Data Model

Add `canvas_project`:

```text
id
tenant_id
project_key
project_name
description
status
default_settings_json
require_review_before_publish
quiet_hours_json
created_by
created_at
updated_by
updated_at
```

Rules:

- `project_key` is unique within `tenant_id`.
- `status` supports at least `ACTIVE` and `DISABLED`.
- `default_settings_json` stores project-level defaults such as default timeout and default execution limits. Canvas-specific fields still override project defaults.
- `quiet_hours_json` stores project-level quiet-hour configuration in a structured JSON shape.

Add `canvas_project_member`:

```text
id
tenant_id
project_id
user_id
username
role
source
created_at
updated_at
```

Rules:

- A user has at most one role per project.
- `source` is `MANUAL` in the first phase.
- Supported roles are `PROJECT_ADMIN`, `EDITOR`, `EXECUTOR`, and `VIEWER`.

Enhance `canvas_project_folder` as the compatibility and assignment bridge:

```text
canvas_id
project_id
project_key
project_name
folder_key
folder_name
updated_by
created_at
updated_at
```

Rules:

- `project_id` is the preferred authority once present.
- `project_key` and `project_name` remain for backward compatibility and import/export.
- Existing APIs that read or write project/folder metadata continue to work.
- Implementation must add or verify the migration for `canvas_project_folder`, because the service and DO exist but the table migration was not visible during design review.

Statistics can be served by real-time aggregation first. A snapshot table is optional if queries become expensive:

```text
project_id
canvas_count
published_canvas_count
execution_count_7d
failed_execution_count_7d
avg_duration_ms_7d
```

## Permissions

Tenant-level roles keep their current meaning:

- `SUPER_ADMIN` can access all tenants.
- `TENANT_ADMIN` can manage all Projects inside the current tenant.
- `OPERATOR` needs project membership for project-scoped operations.

Project roles:

- `PROJECT_ADMIN`: manage project settings, members, and canvases in the project.
- `EDITOR`: create and edit canvases in the project.
- `EXECUTOR`: execute and test-run canvases in the project.
- `VIEWER`: read project and canvas data.

Permission checks stay in service/controller paths for the first phase. A general resource-action permission matrix is intentionally deferred. The first phase should check project membership before canvas read, update, publish, execute, metadata update, and project detail access.

Action rules:

- Read project: tenant admin, project member.
- Create project: tenant admin.
- Update project settings: tenant admin, `PROJECT_ADMIN`.
- Manage project members: tenant admin, `PROJECT_ADMIN`.
- Read canvas in project: tenant admin, project member.
- Create or move canvas into project: tenant admin, `PROJECT_ADMIN`, `EDITOR`.
- Edit canvas: tenant admin, `PROJECT_ADMIN`, `EDITOR`.
- Publish/offline/archive canvas: tenant admin, `PROJECT_ADMIN`, `EDITOR`. If the Project requires review before publish, these roles can initiate the publish flow, but the publish action must be blocked until the approval condition is satisfied.
- Dry-run or direct execute canvas: tenant admin, `PROJECT_ADMIN`, `EDITOR`, `EXECUTOR`.

## Backend API

Add project administration APIs:

```text
GET    /admin/projects
POST   /admin/projects
GET    /admin/projects/{projectId}
PUT    /admin/projects/{projectId}
PUT    /admin/projects/{projectId}/disable

GET    /admin/projects/{projectId}/members
PUT    /admin/projects/{projectId}/members/{userId}
DELETE /admin/projects/{projectId}/members/{userId}

GET    /admin/projects/{projectId}/canvases
GET    /admin/projects/{projectId}/stats
```

Extend canvas APIs without breaking existing callers:

```text
POST /canvas
  accepts optional projectId, folderKey, folderName

GET /canvas/list
  accepts optional projectId

GET /canvas/{id}/project-folder-metadata
  returns projectId when available, plus existing projectKey/projectName/folderKey/folderName

PUT /canvas/{id}/project-folder-metadata
  accepts projectId and existing projectKey/projectName/folderKey/folderName
```

The existing `CanvasProjectFolderMetadataService` becomes the compatibility layer around assignment reads and writes. New project-specific services should own formal Project and Project Member logic.

## Frontend Experience

Add a Project management entry in the admin area:

```text
/admin/projects
  project list
  create/edit/disable project
  project status, member count, canvas count, recent failures

/admin/projects/:projectId
  Canvas tab
  Members tab
  Default Policies tab
  Statistics tab
```

Canvas-side changes stay lightweight:

- Canvas list adds a Project filter.
- New canvas flow can select Project and Folder.
- Canvas settings shows and edits the current Project and Folder.
- Edit, publish, execute, and member-management controls are hidden when the current user lacks permission, with backend 403 as the source of truth.

The Project pages should use existing frontend patterns: `frontend/src/services/api.ts` style API clients, Ant Design tables/forms, and focused helper tests for presentation logic.

## Policy Semantics

Project default policies are defaults, not hard overrides, in the first phase.

When a canvas has no explicit value for a supported execution limit, the project default is applied by canvas creation and settings update flows. Existing canvas fields remain authoritative once set. Runtime enforcement is added only for fields that already have a matching execution pre-check path; fields without runtime support remain stored configuration until that support is implemented.

Supported first-phase defaults:

- default execution timeout if the runtime has a matching setting path
- valid execution window defaults
- per-user daily limit
- per-user total limit
- cooldown seconds
- quiet hours
- require review before publish, enforced as a publish gate rather than as a role replacement

Project disable behavior:

- Disabling a Project does not delete canvases.
- Disabling a Project does not stop already published canvases from executing in the first phase.
- Disabling a Project blocks new canvas creation in the project, moving canvases into the project, project setting changes, and publishing from the project.

This avoids a governance rollout accidentally stopping production workflows.

## Error Handling

Use explicit errors:

- Project not found: 404 or business error `PROJECT_NOT_FOUND`.
- Project disabled for management action: `PROJECT_UNAVAILABLE`.
- User is not a project member: 403.
- User role does not allow the action: 403 with action-specific message.
- Moving a canvas to another Project without target permission: 403.
- Duplicate `project_key` in a tenant: validation error with a clear field message.

Tenant isolation remains mandatory on every Project, Project Member, Canvas, and statistics query.

## Migration and Compatibility

The first implementation should:

1. Add new Flyway migration files after the current latest migration.
2. Create `canvas_project`.
3. Create `canvas_project_member`.
4. Add `project_id` to `canvas_project_folder` or create the table if missing.
5. Backfill `canvas_project` rows from existing non-empty `project_key/project_name` metadata.
6. Link existing `canvas_project_folder` rows to the backfilled Project by tenant and project key where possible.
7. Preserve import/export support for `projectKey`, `projectName`, `folderKey`, and `folderName`.

No applied migration should be edited.

## Testing

Backend tests:

- Project CRUD service tests.
- Project member role validation tests.
- Project permission service tests for read, edit, publish, execute, and member management.
- Canvas project assignment compatibility tests.
- Canvas list filtering by project.
- Disabled Project management blocking tests.
- Tenant isolation tests for Project and Project Member queries.
- Migration verification for `canvas_project_folder` compatibility.

Frontend tests:

- `projectApi.test.ts` for new endpoints.
- Project list presentation/helper tests.
- Project member role display tests.
- Canvas create payload test for `projectId` and folder fields.
- Canvas settings payload test for project/folder metadata updates.

## Rollout

Phase 1 ships Project governance for Canvas only:

- Project CRUD
- Project membership
- Project-scoped canvas access checks
- Project assignment for canvas create/list/settings
- Project statistics
- Project default policies

Phase 2 can add environment isolation, stronger publish approvals, and broader audit reporting.

Phase 3 can add SSO group provisioning, credential isolation, variable/data-table permissions, and a general resource-action permission matrix if enterprise usage proves the need.

## Acceptance Criteria

- Tenant admins can create and disable Projects.
- Operators only see or manage Projects where they are members.
- A canvas can be assigned to a Project and optional Folder.
- Project membership controls canvas read, edit, publish, and execute actions.
- Existing canvas project/folder metadata remains readable and import/export compatible.
- Project details show canvas count, published canvas count, recent execution count, recent failure count, and average duration.
- Disabling a Project blocks management actions without stopping already published canvases.
- Tests cover the permission and compatibility paths described above.
