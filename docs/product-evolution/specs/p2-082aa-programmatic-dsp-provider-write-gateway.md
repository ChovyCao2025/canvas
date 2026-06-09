# P2-082AA - Programmatic DSP Provider Write Gateway Spec

Priority: P2
Sequence: 082AA
Parent spec: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082aa-programmatic-dsp-provider-write-gateway-plan.md`

## Problem

P2-082Q stores DSP seats, campaigns, line items, supply paths, performance snapshots, and pacing summaries. Operators can model DSP activation internally, but the platform still lacks a safe control plane for external DSP write operations such as creating insertion orders, updating line item budgets or bids, changing line item status, assigning targeting, attaching private marketplace deals, and syncing provider status. Direct scripts would bypass tenant ownership, seat/campaign/line-item relationship checks, approvals, idempotency, dry-run validation, and provider evidence.

## Research Notes

- Google Display & Video 360 exposes separate advertiser-scoped write surfaces for insertion orders, line items, and assigned targeting options. Production systems need to preserve advertiser, insertion order, and line item hierarchy when preparing provider writes.
- DSP write operations change spend, targeting, pacing, and supply access. They need approval gates and dry-run evidence before live execution.
- Most DSP APIs have provider-specific credential, validation, activation, and rate-limit behavior. The first production slice should land a provider-neutral mutation ledger and fail closed for live writes unless a concrete client is registered.

Primary references:

- https://developers.google.com/display-video/api/reference/rest/v4/advertisers.insertionOrders
- https://developers.google.com/display-video/api/reference/rest/v4/advertisers.lineItems
- https://developers.google.com/display-video/api/reference/rest/v4/advertisers.lineItems.targetingTypes.assignedTargetingOptions
- https://developers.google.com/display-video/api/guides

## Scope

Backend first slice:

- Add an additive `programmatic_dsp_mutation` ledger for external DSP write proposals, approval state, dry-run/apply execution state, request hash, idempotency key, sanitized payload, provider request/response evidence, and errors.
- Add typed commands and views for proposing, approving/rejecting, executing, and listing DSP provider mutations.
- Add a programmatic DSP provider write gateway abstraction that dry-runs supported payloads locally and fails closed for live writes when no provider client is registered.
- Add programmatic DSP controller endpoints for mutation proposal, approval, execution, and listing.
- Validate tenant-owned seat, campaign, line item, and supply-path relationships before any mutation is proposed or executed.
- Keep provider credentials out of mutation payloads and views.

## Supported First-Slice Mutation Types

- `CREATE_INSERTION_ORDER`
- `UPDATE_CAMPAIGN_BUDGET`
- `CREATE_LINE_ITEM`
- `UPDATE_LINE_ITEM_BID`
- `UPDATE_LINE_ITEM_BUDGET`
- `UPDATE_LINE_ITEM_STATUS`
- `ASSIGN_TARGETING`
- `ATTACH_DEAL`
- `SYNC_PROVIDER_STATUS`

## Non-Goals

- Real DV360, The Trade Desk, Amazon DSP, or other DSP network calls in local tests.
- Autonomous bid optimization or budget shifts without human approval.
- Creative approval, ad serving, attribution, or payment settlement.
- Frontend workbench for DSP provider mutations.

## Acceptance Criteria

- P2-082AA docs are indexed after P2-082Z.
- Schema test proves the mutation ledger and production indexes exist.
- Service tests prove proposal validation, tenant isolation, relationship validation, idempotency/request-hash handling, approval gating, dry-run-before-apply enforcement, and fail-closed unsupported live writes.
- Controller tests prove tenant/operator propagation for propose, approve, execute, and list endpoints.
- Focused backend tests pass with Java 21.

## Implementation Status

Status: Delivered.

- Added `programmatic_dsp_mutation` as an additive mutation ledger.
- Added tenant-owned seat, campaign, line item, and supply-path relationship validation before proposals execute.
- Added approval and dry-run-first gates for live DSP provider writes.
- Added fail-closed provider gateway behavior when no real provider client is registered.
- Added controller endpoints for propose, approve/reject, execute, and list.
- Added recursive provider-secret key rejection so nested auth fields cannot enter mutation payload or provider evidence JSON.
- Verified with focused mutation tests and programmatic DSP regression tests.
