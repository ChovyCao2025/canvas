# P2-002 - Plugin And Integration Foundations Spec

Priority: P2
Sequence: 002
Source: `todo/p2/plugin-and-integration-foundations.md`
Implementation plan: `../plans/p2-002-plugin-and-integration-foundations-plan.md`

## Goal

Create safe internal plugin foundations and integration primitives without exposing unsafe third-party runtime loading.

## User And Business Value

Core handlers, channel adapters, data exporters, and rule/template packs can evolve with less coupling while preserving runtime safety.

## In Scope

- Plugin extension points for node handlers, channel adapters, data exporters, and rule/template packs.
- Plugin metadata, lifecycle, configuration schema, enable/disable, and compatibility checks.
- Internal or built-in plugin packaging before third-party hot loading.
- Official plugin pilots for WeCom, data export, batch operation, and AI Gateway adapter.
- API key and webhook foundations for integration partners.

## Out Of Scope

- Public marketplace UI, plugin ratings, revenue sharing, and runtime hot-loaded third-party code.
- Feishu and DingTalk before WeCom and data export unless demand changes.

## Functional Requirements

1. The feature must expose the smallest useful operator or platform workflow described in the source item.
2. The implementation must preserve tenant isolation, authorization, auditability, and rollback behavior for every new read or write path.
3. New UI must use existing React, Ant Design, router, service, and test patterns unless a child spec justifies a new pattern.
4. New backend behavior must use the existing Spring Boot, MyBatis, Flyway, controller, domain service, and test patterns.
5. The implementation must include focused automated tests before code changes and a manual verification checklist for the core workflow.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/HandlerRegistry.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ApiDefinitionController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/common/OutboundUrlValidator.java`

### Frontend Touchpoints

- `frontend/src/pages/api-docs/index.tsx`
- `frontend/src/pages/system-options/index.tsx`
- `frontend/src/services/api.ts`

### Data And Configuration Touchpoints

- `backend/canvas-engine/src/main/resources/db/migration/V101__plugin_integration_foundations.sql`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/plugin/PluginRegistryServiceTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/ApiDefinitionControllerTest.java`
- `frontend/src/pages/api-docs/pluginIntegrationDocs.test.tsx`

## Dependencies

- Permission, audit, configuration validation, and classloader or remote-call boundaries must be explicit.
- Channel plugins depend on adapter boundaries.

## Risks And Controls

- Scope creep: keep the first implementation to the workflow in this spec and move broader ideas to a follow-up spec.
- Tenant or permission regression: add backend tests for tenant-scoped data and role checks before exposing UI.
- UI complexity: use one page or one panel first, then expand only after the workflow is verified.
- Data migration risk: make every migration additive and reversible by disabling the new route or feature flag.

## Acceptance Criteria

- The source item has a visible implemented workflow or a documented discovery exit if this is a P3 strategy item.
- All changed backend endpoints reject unauthorized access and preserve tenant scoping.
- All changed frontend routes handle loading, empty, error, and permission states.
- Tests named in the plan pass in the local commands for backend and frontend slices.
- The implementation includes rollout notes covering feature flag, migration, and rollback behavior.
