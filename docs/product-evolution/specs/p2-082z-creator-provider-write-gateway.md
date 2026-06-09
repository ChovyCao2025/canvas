# P2-082Z - Creator Provider Write Gateway Spec

Priority: P2
Sequence: 082Z
Parent spec: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082z-creator-provider-write-gateway-plan.md`

## Problem

P2-082O stores creator profiles, campaign briefs, collaborations, deliverables, tracking links, discount codes, and performance evidence. Operators can manage the internal workflow, but the platform still cannot safely coordinate external creator-provider write operations such as publishing a campaign brief, inviting a creator, generating affiliate links or discount codes, requesting Spark/partnership content authorization, or syncing deliverable status. Direct provider scripts would bypass tenant ownership, creator/campaign relationship checks, approvals, idempotency, dry-run validation, and audit evidence.

## Research Notes

- TikTok Spark Ads workflows depend on creator authorization for using organic creator posts as ads; production systems must keep authorization evidence and not assume brand-side write control.
- Shopify Collabs workflows center affiliate links, discount codes, commission programs, and creator invitations; provider writes need commerce-side auditability and rollback evidence.
- Meta/Instagram partnership and branded-content advertising workflows require creator/brand permission boundaries; platform writes need explicit permission context.
- The common safe implementation pattern is a local mutation ledger before real provider adapters: tenant validation, relationship validation, approval, dry-run, request hash, idempotency, sanitized request/response evidence, and fail-closed live execution unless a provider client is registered.

Primary references:

- https://ads.tiktok.com/help/article/spark-ads
- https://help.shopify.com/en/manual/promoting-marketing/collabs/merchants/affiliates
- https://help.shopify.com/en/manual/discounts/discount-types
- https://www.facebook.com/business/help/788160621327601

## Scope

Backend first slice:

- Add an additive `creator_provider_mutation` ledger for external creator-provider write proposals, approval state, dry-run/apply execution state, request hash, idempotency key, sanitized payload, provider request/response evidence, and errors.
- Add typed commands and views for proposing, approving/rejecting, executing, and listing creator provider mutations.
- Add a creator provider write gateway abstraction that dry-runs supported payloads locally and fails closed for live writes when no provider client is registered.
- Add creator collaboration controller endpoints for mutation proposal, approval, execution, and listing.
- Validate tenant-owned campaign, collaboration, deliverable, and creator relationships before any mutation is proposed or executed.
- Keep provider credentials out of mutation payloads and views.

## Supported First-Slice Mutation Types

- `PUBLISH_BRIEF`
- `INVITE_CREATOR`
- `GENERATE_AFFILIATE_LINK`
- `CREATE_DISCOUNT_CODE`
- `REQUEST_CONTENT_AUTHORIZATION`
- `SYNC_DELIVERABLE_STATUS`

## Non-Goals

- Real TikTok, Meta, Shopify, Later, affiliate-network, or creator marketplace network calls in local tests.
- Payment, invoice, tax, or contract e-signature execution.
- Autonomous creator outreach without human approval.
- Frontend workbench for creator provider mutations.

## Acceptance Criteria

- P2-082Z docs are indexed after P2-082Y.
- Schema test proves the mutation ledger and production indexes exist.
- Service tests prove proposal validation, tenant isolation, relationship validation, idempotency/request-hash handling, approval gating, dry-run-before-apply enforcement, and fail-closed unsupported live writes.
- Controller tests prove tenant/operator propagation for propose, approve, execute, and list endpoints.
- Focused backend tests pass with Java 21.

## Implementation Status

Status: Delivered.

- Added `creator_provider_mutation` as an additive mutation ledger.
- Added tenant-owned campaign, collaboration, deliverable, and creator relationship validation before proposals execute.
- Added approval and dry-run-first gates for live provider writes.
- Added fail-closed provider gateway behavior when no real provider client is registered.
- Added controller endpoints for propose, approve/reject, execute, and list.
- Verified with focused mutation tests and creator-collaboration regression tests.
