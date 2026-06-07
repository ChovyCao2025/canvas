# P1-008C - Channel Connector Operator Surface Spec

Priority: P1
Sequence: 008C
Source: `docs/optimization/todo/marketing_platform_gap_analysis.md`, `docs/optimization/bmad-product-review-2026-05.md`
Implementation plan: `../plans/p1-008c-channel-connector-operator-surface-plan.md`

## Goal

Expose connector modes, health, provider limits, fallback decisions, and dedupe records to operators.

## Current Baseline

- P1-008 and P1-008B define backend connector and policy behavior.
- The frontend config panel validates selected SEND_MESSAGE channel but cannot show connector availability or provider health.
- There is no connector management page or API wrapper.

## In Scope

- `ChannelConnectorController` for list/detail, mode update, health test, limit update, fallback validation/save, decision list, and dedupe list.
- `frontend/src/services/channelConnectorApi.ts`.
- `frontend/src/pages/channel-connectors/index.tsx` and presentation helpers.
- Config-panel disabled/sandbox warnings for selected channel/provider.

## Out Of Scope

- New provider integrations.
- Multi-tenant RBAC redesign; use existing tenant/admin patterns.
- Cost accounting dashboards.

## Functional Requirements

1. Operators must see connector mode `REAL`, `SANDBOX`, or `DISABLED`.
2. Operators must see health status and latest checked time.
3. Limit editor must show provider window, quota, and last updated source.
4. Fallback policy validation must display cycles and disabled targets before save.
5. Node config panels must warn when selected channel/provider is not REAL.

## Acceptance Criteria

- Backend API tests cover list/detail/update/health/fallback validation.
- Frontend tests cover mode badge, health badge, limit editor, validation error, fallback decision table, dedupe table, and permission denied state.
- Config panel tests cover disabled/sandbox warning rendering.

## Implementation Status

Completed on 2026-06-05.

- Backend `ChannelConnectorController` exposes connector list, limits, mode update, health test, fallback validation, fallback decisions, and dedupe records with tenant-scoped service access.
- Frontend `channelConnectorApi`, channel connector page, and presentation helpers expose mode/health badges, provider limits, fallback decisions, and dedupe records.
- `/channel-connectors` is routed under the authenticated admin app shell and appears in the integration navigation group.
- Config-panel presentation renders warnings for SEND_MESSAGE nodes when the connector mode is not `REAL`.
- Verified with backend clean compile, `ChannelConnectorControllerTest`, isolated P1-008B/P1-008C backend runner `44/44`, focused frontend tests, and frontend production build.
