# P1-007C - Frontend HTTP Client And Runtime Schemas Spec

Priority: P1
Sequence: 007C
Source: `docs/optimization/production-design-gaps.md`, `docs/optimization/production-readiness-checklist.md`
Implementation plan: `../plans/p1-007c-frontend-http-client-and-runtime-schemas-plan.md`

## Goal

Replace fragile frontend API behavior with a tested HTTP client wrapper and runtime validation for critical canvas contracts.

## Current Baseline

- `frontend/src/services/api.ts` creates one Axios instance, injects JWT, unwraps backend `R<T>`, and uses `window.location.href = '/login'` on 401.
- There is no request dedupe, explicit abort wiring, idempotent retry policy, typed error normalization, or runtime validation for graph JSON.
- `frontend/package.json` does not include `zod`.

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
