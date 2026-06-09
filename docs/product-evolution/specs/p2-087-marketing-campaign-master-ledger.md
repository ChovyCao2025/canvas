# P2-087 - Marketing Campaign Master Ledger Spec

Priority: P2
Sequence: 087
Source: user-requested marketing middle-platform production completeness gap
Implementation plan: `../plans/p2-087-marketing-campaign-master-ledger-plan.md`

Status: Historical plan evidence records implementation and verification; runtime verification plus commit and merge status was not verified in this docs-only audit.

## Goal

Add the campaign master-data ledger that turns the marketing middle platform from a control-plane summary into an operator-managed glue layer. A campaign must be represented once per tenant and linked to the journeys, content releases, paid-media assets, measurement dashboards, and provider-write gateways required for launch.

## Current Baseline

P2-086 exposes marketing-platform capabilities, integration lanes, runtime evidence, and a readiness gate. That slice intentionally did not introduce campaign master data. Without a campaign ledger, operators can see gaps but cannot bind cross-domain assets into one governed launch object.

## Required Behavior

- Add tenant-scoped campaign master persistence with stable `campaignKey`, name, objective, status, primary channel, owner team, budget, schedule, brief JSON, created/updated actor, and timestamps.
- Add tenant-scoped campaign resource links with resource type, resource id/key/name/route, dependency role, link status, launch-required flag, metadata JSON, created/updated actor, and timestamps.
- Enforce unique campaign keys per tenant and unique resource links per tenant, campaign, resource type, and resource key.
- Enforce tenant ownership before listing, linking, or unlinking campaign resources.
- Normalize campaign keys and resource keys into stable lower-case identifiers; normalize statuses, channels, resource types, and dependency roles into upper-case values.
- Reject invalid lifecycle states and invalid campaign date windows.
- Expose a single-campaign readiness gate that evaluates whether a campaign can be launched:
  - Campaign status must be `ACTIVE`.
  - At least one resource must be marked `requiredForLaunch`.
  - Every launch-required resource must have `linkStatus=ACTIVE`.
  - At least one active launch-required resource must have `dependencyRole=PRIMARY`.
  - At least one active launch-required measurement resource must exist via `dependencyRole=MEASUREMENT` or `resourceType=BI_DASHBOARD`.
  - Optional linked resources in `MISSING` or `BLOCKED` state must produce warnings without blocking launch.
- Expose authenticated APIs:
  - `POST /canvas/marketing-campaigns`
  - `GET /canvas/marketing-campaigns`
  - `POST /canvas/marketing-campaigns/links`
  - `GET /canvas/marketing-campaigns/{campaignId}/links`
  - `GET /canvas/marketing-campaigns/{campaignId}/readiness`
  - `DELETE /canvas/marketing-campaigns/links/{linkId}`
- Feed campaign master counts, resource link counts, required link counts, and blocked link counts into the marketing-platform control-plane readiness gate.
- Add an operator-visible Campaign master ledger section to `/marketing-platform` so operators can create/update campaigns, bind resources, inspect dependencies, and remove bad links.

## Non-Goals

- No live provider adapter is introduced in this slice.
- No campaign execution scheduler is introduced in this slice.
- No existing campaign-like domain tables are renamed or removed.

## Acceptance Criteria

- Schema tests prove the campaign master and resource link ledgers, unique keys, indexes, and cascade FK exist.
- Service tests prove tenant scoping, key normalization, status validation, date validation, upsert behavior, resource-link upsert behavior, and unlink tenant guard.
- Service tests prove single-campaign readiness returns `BLOCKED`, `READY`, and `DEGRADED` from campaign status, required resources, active links, primary dependency, measurement dependency, and optional-resource warnings.
- Controller tests prove tenant context and operator identity are passed into campaign APIs.
- Controller tests prove the readiness endpoint evaluates against the current tenant.
- Control-plane tests prove campaign master/resource-link runtime evidence affects capability and integration-asset readiness.
- Frontend API tests prove campaign ledger endpoints are called with the expected paths and payloads.
- Frontend page tests prove the Campaign master ledger and readiness gate controls appear in the marketing-platform operator page.
