# OpenAPI Runtime API Docs Design

## Background

The current API docs page is registered manually at `/api-docs` and renders data from `frontend/src/pages/api-docs/apiDocs.ts`. The page already has a useful developer-documentation UI: search, categories, internal API toggle, parameters, and request/response examples.

The weak point is data freshness. A scan of `backend/canvas-engine/src/main/java/org/chovy/canvas/web/*Controller.java` found more controller routes than the static page lists. The backend already includes `springdoc-openapi-starter-webflux-ui`, enables `/v3/api-docs`, and permits access to `/v3/api-docs/**` in `SecurityConfig`, so the page can use OpenAPI as its runtime source.

## Goal

Replace the static endpoint source with a runtime OpenAPI source while preserving the current API docs page experience.

The page should:

- Fetch `/v3/api-docs` when `/api-docs` opens.
- Convert OpenAPI `paths` into the existing `ApiDocEndpoint` model.
- Continue supporting keyword search, category navigation, and internal API filtering.
- Use a local override table for Chinese titles, business summaries, internal/external classification, and examples.
- Surface OpenAPI loading failures clearly.
- Avoid maintaining a full hand-written endpoint list.

## Non-Goals

- Do not embed Swagger UI in the frontend page.
- Do not add a new backend catalog endpoint.
- Do not annotate all controllers with Swagger metadata in this change.
- Do not implement online request execution from the docs page.
- Do not change route permissions beyond the dev proxy needed for `/v3/api-docs`.

## Chosen Approach

The frontend will read `/v3/api-docs` at runtime and adapt the OpenAPI document into the page's existing data model.

This keeps the interface useful for product and integration readers while making endpoint discovery automatic. OpenAPI provides the source of truth for method/path coverage; the local override table only carries presentation metadata that OpenAPI does not currently express well in this codebase.

## Alternatives Considered

### Backend `/api-docs/catalog`

A backend endpoint could return a fully normalized catalog for the frontend. This would centralize mapping and classification, but it adds a new API solely for documentation and still depends on OpenAPI or route inspection internally. It is more work than needed for the current page.

### Swagger UI or Raw OpenAPI Rendering

Embedding Swagger UI would eliminate coverage drift quickly. It would also discard the current Chinese, business-oriented docs experience and make the page feel like a generic technical explorer instead of a curated developer guide.

### Static List Plus Drift Test

Keeping `apiDocs.ts` static and adding a test to detect missing routes would reduce surprises, but every endpoint still requires manual entry. The user selected runtime registration, so this option is not the target.

## Architecture

Add an OpenAPI adapter beside the existing API docs page:

- `openApiDocs.ts`
  - Fetches `/v3/api-docs`.
  - Parses OpenAPI `paths`.
  - Converts supported HTTP methods into `ApiDocEndpoint`.
  - Extracts path, query, and request body parameters.
  - Applies categorization and internal/external rules.
  - Merges local overrides.

- `apiDocOverrides.ts`
  - Stores curated metadata keyed by `METHOD path`.
  - Provides Chinese titles, summaries, categories, examples, and explicit internal flags for important endpoints.
  - Starts with the current public and high-value internal endpoint descriptions from `apiDocs.ts`, not the full static endpoint list.

- `apiDocs.ts`
  - Keeps shared types, category definitions, filtering helpers, JSON formatting, and endpoint key helpers.
  - No longer exports a full static endpoint array as the primary data source.

- `index.tsx`
  - Loads OpenAPI endpoints with React state.
  - Shows loading, error, and empty states.
  - Reuses the current filter, category, and card UI.

## Data Flow

1. User opens `/api-docs`.
2. The page calls `fetchOpenApiSpec()`.
3. The adapter walks `spec.paths`.
4. Each operation becomes one `ApiDocEndpoint`.
5. The adapter enriches generated endpoints with override metadata.
6. The page filters by internal toggle, category, and keyword.
7. Endpoint cards render method, path, auth, summary, parameters, and examples.

The OpenAPI request should use a raw Axios or fetch call, not the existing `http` client from `services/api.ts`, because `/v3/api-docs` is not wrapped in the project's `R<T>` response shape.

## Classification Rules

Classification should be deterministic and easy to adjust:

- `/auth` -> `auth`
- `/canvas/events/report`, `/canvas/trigger/behavior`, `/canvas/execute/*` -> `external-trigger`
- `/canvas/execution/{executionId}/approve|reject` -> `approval`
- `/canvas/{id}/stats`, `/canvas/{id}/executions`, `/canvas/dlq`, `/canvas/execution-requests`, `/canvas/mq-trigger-rejected` -> `observability`
- `/ops`, `/canvas/templates`, `/canvas/pending-reviews`, `/canvas/async-tasks`, `/canvas/notifications` -> `operations`
- `/admin/users` -> `users`
- `/admin/system-options`, `/canvas/*-definitions`, `/canvas/audiences`, `/canvas/data-sources`, `/canvas/identity-types`, `/canvas/tag-imports`, `/canvas/tag-import-sources`, `/cdp` -> `configuration`
- `/meta` -> `metadata`
- Remaining `/canvas` routes -> `canvas`

Local overrides can replace the computed category when a path needs special handling.

## Internal API Rules

The default should be conservative:

- External by default:
  - `POST /auth/login`
  - `POST /canvas/events/report`
  - `POST /canvas/execute/direct/{canvasId}`
  - `POST /canvas/execute/dry-run/{canvasId}`
  - `POST /canvas/trigger/behavior`
  - manual approval callbacks if they remain integration-facing

- Internal by default:
  - `/admin/**`
  - `/canvas/api-definitions/**`
  - `/canvas/event-definitions/**`
  - `/canvas/mq-definitions/**`
  - `/canvas/tag-definitions/**`
  - `/canvas/audiences/**`
  - `/canvas/data-sources/**`
  - `/canvas/identity-types/**`
  - `/canvas/tag-imports/**`
  - `/canvas/tag-import-sources/**`
  - `/canvas/notifications/**`
  - `/canvas/async-tasks/**`
  - `/canvas/mq-trigger-rejected/**`
  - `/cdp/**`
  - `/meta/**`

Overrides can explicitly set `internal` for any endpoint.

## Error Handling

If `/v3/api-docs` fails:

- Show an Ant Design `Alert` with the failed URL and a short explanation.
- Offer a retry button.
- Do not silently claim that only the old static endpoints exist.

If OpenAPI contains an unsupported or malformed operation:

- Skip only that operation.
- Keep rendering valid endpoints.
- Expose a lightweight warning count in the page or console for debugging.

## Frontend Dev Proxy

`frontend/vite.config.ts` currently proxies `/auth`, `/admin`, `/meta`, and `/canvas`. Add a `/v3` proxy to `http://localhost:8080` so the runtime OpenAPI request works in local development.

Production deployment should rely on the same origin or reverse proxy that already serves backend API routes.

## Testing

Unit tests should focus on the adapter, not the visual layout:

- Parses a minimal OpenAPI spec into endpoints.
- Includes GET, POST, PUT, and DELETE operations.
- Extracts path and query parameters.
- Represents request body as a body parameter when present.
- Classifies common paths into expected categories.
- Applies local overrides by `METHOD path`.
- Computes internal/external visibility correctly.
- Handles missing `paths` and malformed operations without crashing.

Existing filter helper tests should remain and be updated to use generated endpoint fixtures instead of a full static list.

## Risks

- OpenAPI summaries may be sparse because controllers do not use `@Operation` annotations. The override table handles important descriptions without requiring annotation work.
- Runtime fetch can fail if springdoc is disabled in an environment. The page must make this visible; if production disables `/v3/api-docs`, the deployment needs to either allow this endpoint for admins or accept that the docs page cannot auto-register there.
- Generated examples from schemas may be weak. The override table should keep curated examples for externally consumed APIs.
- Classification by path is heuristic. Keep the rules centralized and covered by tests so future adjustments are small.

## Success Criteria

- `/api-docs` automatically lists all operations exposed by `/v3/api-docs`.
- New controller routes appear on the page without editing the endpoint array.
- Current public API docs retain Chinese titles and examples through overrides.
- Search, category filters, and internal API toggle keep working.
- Frontend tests cover OpenAPI parsing, classification, overrides, and filtering.
