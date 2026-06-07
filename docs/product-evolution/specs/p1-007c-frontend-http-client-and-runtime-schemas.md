# P1-007C - Frontend HTTP Client And Runtime Schemas Spec

Priority: P1
Sequence: 007C
Source: `docs/optimization/production-design-gaps.md`, `docs/optimization/production-readiness-checklist.md`
Implementation plan: `../plans/p1-007c-frontend-http-client-and-runtime-schemas-plan.md`

## Goal

Replace fragile frontend API behavior with a tested HTTP client wrapper and runtime validation for critical canvas contracts.

## Current Baseline

- Implemented on 2026-06-05: `frontend/package.json` now includes `zod`.
- `frontend/src/services/httpClient.ts` provides a tested wrapper for request `signal`, `requestKey` GET dedupe, bounded GET retry, business error normalization, network/cancel normalization, and route-preserving unauthorized callbacks.
- `frontend/src/services/api.ts` keeps the existing `http` export for compatibility and now also exports `apiClient` for gradual migration.
- `frontend/src/types/canvasSchemas.ts` validates critical canvas graph JSON, graph edges, node registry outlet schemas, and canvas detail response shape.
- `frontend/src/pages/canvas-editor/index.tsx` validates graph JSON before React Flow hydration and renders a visible error alert for invalid graph data.

## In Scope

- Create `httpClient.ts` with request `signal`, `requestKey`, GET dedupe, idempotent retry, business error normalization, and route-preserving unauthorized callback.
- Refactor `api.ts` to use the wrapper while preserving existing exported API functions.
- Add `canvasSchemas.ts` with Zod schemas for backend graph JSON, canvas node, outlet schema, node registry, and critical canvas detail responses.
- Validate critical graph hydration paths before data reaches React Flow.

## Out Of Scope

- Editor store and save queue; split into P1-007B.
- Broad backend API changes.
- Full strict validation of every legacy API response.

## Functional Requirements

1. Component unmount must be able to cancel in-flight requests.
2. Identical GET requests with a shared `requestKey` must dedupe while in flight.
3. GET retries must be bounded; POST must not retry by default.
4. 401 handling must clear auth and navigate without a hard reload while preserving intended route.
5. Invalid graph JSON must produce a visible typed error instead of crashing the editor.

## Acceptance Criteria

- HTTP tests cover cancellation, dedupe, retry, business error, normalized network errors, and 401 callback.
- Zod tests accept seeded valid graph data and reject invalid root/node/edge route shapes.
- Existing API tests continue to pass.

## Implementation Notes

- `createHttpClient` is compatible with both raw Axios responses and the existing `api.ts` interceptor payload shape, so services can migrate incrementally without changing every API call at once.
- GET requests dedupe only when callers provide a shared `requestKey`; non-GET calls never dedupe and do not retry by default.
- Retry is limited to idempotent GET calls and retryable network/server failures. Canceled requests are normalized as non-retryable `ApiHttpError`.
- `parseCanvasGraphJson` falls back to an empty graph for empty strings but rejects malformed JSON, invalid root shapes, invalid nodes, invalid edge route shapes, and invalid outlet target fields.
- Canvas editor graph loading catches schema errors, sets `graphLoadError`, and displays an Ant Design error alert instead of passing invalid graph data into React Flow.

## Verification

- `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run test -- httpClient.test.ts canvasSchemas.test.ts` passed: 2 files, 11 tests.
- `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run test -- httpClient.test.ts api.test.ts canvasSchemas.test.ts graphHydration.test.ts` passed: 16 files, 43 tests.
- `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run build` passed.
