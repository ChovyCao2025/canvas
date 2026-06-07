# P0-001 - Production Safety And Compliance Spec

Priority: P0
Sequence: 001
Source: `todo/p0/production-safety-and-compliance-stopgaps.md`
Implementation plan: `../plans/p0-001-production-safety-and-compliance-plan.md`

## Implementation Status

Implemented and verified on 2026-06-05. Verification evidence is recorded in `../plans/p0-001-production-safety-and-compliance-plan.md`.

## Goal

Make backend production access safe before growth work by closing unauthenticated operational routes, missing tenant model fields, and unguarded marketing send paths.

## User And Business Value

Operators can run production workflows without cross-tenant data exposure, anonymous operational mutation, or marketing messages that bypass consent and suppression rules.

## Scope Split

This spec is the backend safety slice for the original production-safety item.

Frontend white-screen recovery, 403/404 pages, request timeout UI, and route accessibility are implemented by `p0-002-frontend-resilience-and-a11y`.
Runtime gates, dashboards, runbooks, and degradation operations are implemented by `p0-005-production-operability-and-runtime-gates`.
AI node productization is implemented by `p2-019-ai-llm-node-productionization`.

## In Scope

- Protect `/ops/**` with admin roles in `SecurityConfig`.
- Add an internal-token gate for anonymous OpenAPI-style execution routes that must remain callable by backend systems.
- Surface `tenantId` fields in data objects for tables that already have `tenant_id`, and add tenant columns to marketing, audience, notification, and send-record tables that currently lack them.
- Add a reusable tenant-scope helper for MyBatis queries and apply it to the first high-risk read/write surfaces: canvas list/detail, canvas stats, execution requests, audiences, notifications, and CDP user lookup.
- Enforce consent, suppression, channel availability, quiet hours, and frequency caps inside the unified `ReachDeliveryService` send path.
- Persist policy-blocked sends as `MessageSendRecordDO.STATUS_SKIPPED` records with reason codes for audit and reconciliation.
- Keep governed node catalog behavior production-safe by asserting that stub or unimplemented node types are not registered as generally available.

## Out Of Scope

- Full customer profile tenancy backfill from external CDP sources.
- Public compliance attestations, privacy-computing architecture, and global certification work.
- Provider-specific channel fallback and backpressure; those are implemented by `p1-008b-provider-backpressure-fallback-and-dedupe`.
- Delivery outbox, receipts, and reconciliation jobs; those are implemented by `p0-003-delivery-outbox-receipts-and-reconciliation`.

## Functional Requirements

1. `/ops/**` must reject anonymous requests and must be reachable only by `ADMIN` or `SUPER_ADMIN` roles during the current rollout.
2. `POST /canvas/events/report`, `POST /canvas/execute/direct/*`, `POST /canvas/trigger/behavior`, and `POST /warehouse/realtime/pipelines/checkpoints` must require `X-Canvas-Internal-Token` when `canvas.internal-api.token` is configured.
3. Tenant-scoped controllers must read tenant identity from `TenantContextResolver` and must filter by tenant for both list and detail reads.
4. Inserts for tenant-owned records must set `tenantId` from the current security context unless the caller is a legacy `ADMIN` system flow with no tenant.
5. The send path must evaluate marketing policy before calling the external reach platform.
6. A policy-blocked send must create or update a send record with `SKIPPED`, must not call the external reach platform, and must route the node through the configured fail branch.
7. Existing idempotency behavior must remain stable: repeated sends with the same idempotency key return the existing record and must not consume policy counters twice.
8. Stub or future node types must stay absent from the generally available node registry until their dedicated spec marks them production-ready.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/InternalApiAuthFilter.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContextResolver.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantScopeSupport.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/NotificationController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpUserController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Tenant-owned data objects under `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V185__production_safety_and_compliance.sql`
- `backend/canvas-engine/src/main/resources/application.yml`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRoleTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/InternalApiAuthFilterTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/common/tenant/TenantContextResolverTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/common/tenant/TenantScopeSupportTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceTenantScopeTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/ReachDeliveryServicePolicyTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java`

## Dependencies

- Existing SaaS migration `V78__saas_foundation.sql` has introduced tenant columns for canvas and execution tables; this spec surfaces those columns in Java data objects and adds missing tenant columns for adjacent tables.
- Existing marketing policy tables and `MarketingPolicyService` provide consent, suppression, channel, quiet-hour, and frequency decisions.
- P0-003 depends on skipped-send records retaining the same idempotency and audit semantics as sent or failed records.

## Risks And Controls

- Tenant filtering can hide data if backfill is incomplete. `V185__production_safety_and_compliance.sql` must backfill nullable tenant columns from canvas or the default tenant before adding indexes.
- Internal-token enforcement can break upstream systems. Roll out with `canvas.internal-api.token` unset in local/dev, set in staging, then set in production after callers send the header.
- Policy checks can increase latency. Keep database checks in `MarketingPolicyService`, keep Redis frequency checks bounded, and reuse existing indexes from `V60__marketing_policy_tables.sql` plus new tenant indexes.
- A skipped send could be interpreted as provider failure. Use `STATUS_SKIPPED` and reason codes so reporting separates policy blocks from provider errors.

## Acceptance Criteria

- `SecurityConfigRoleTest` proves `/ops/**` is no longer anonymous.
- `InternalApiAuthFilterTest` proves internal execution routes reject missing or wrong tokens when configured and pass when the token matches.
- Tenant model tests prove the listed data objects expose `tenantId` and high-risk controller/service reads add tenant filters.
- `ReachDeliveryServicePolicyTest` proves opt-out, suppression, unavailable channel, quiet hours, and frequency cap decisions create `SKIPPED` records without calling the reach platform.
- `NodeTypeGovernanceTest` proves unavailable future nodes are not registered as generally available.
- Focused backend tests named in the implementation plan pass with the documented Maven command.
