# P2-082O - Creator Collaboration Foundation Spec

Priority: P2
Sequence: 082O
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082o-creator-collaboration-foundation-plan.md`

## Goal

Add the first production-grade KOL/KOC collaboration foundation: creator profiles, campaign briefs, collaboration offers, deliverable tracking, attributed performance snapshots, and tenant-scoped reporting APIs.

## Implementation Status

Status: Delivered backend first slice on 2026-06-06.

- Creator registry with provider/channel identity, audience metrics, engagement rate, tags, profile status, risk status, and metadata.
- Campaign brief registry with objective, budget, currency, date window, status, and metadata.
- Collaboration ledger that joins a campaign to a creator with offer type, commercial terms, tracking link, discount code, status, and permissions metadata.
- Deliverable ledger with planned/posting lifecycle, content URL, due/post times, and content-level metrics.
- Summary API that aggregates collaboration deliverables into impressions, engagement, clicks, conversions, revenue, cost, commission, ROI, and overdue counts.
- Additive Flyway migration is `V321__creator_collaboration_foundation.sql` because `V320` is already allocated to BI Quick Engine capacity policy in the current workspace.

## Current Baseline

P2-082E covers paid-media audience activation and hashed audience sync, but it does not manage creator/KOL/KOC workflows. The platform has no first-class model for creator profiles, campaign briefs, content deliverables, influencer tracking links, discount codes, or creator-specific performance evidence.

## Research Inputs

- Shopify Collabs centers merchant workflows around creators, affiliate links, discounts, sales attribution, and commission tracking:
  - https://help.shopify.com/en/manual/promoting-marketing/collabs
  - https://help.shopify.com/en/manual/promoting-marketing/collabs/merchants/affiliates
- Meta's creator marketplace and partnership-ad patterns emphasize creator discovery, campaign collaboration, permissions, and branded content activation:
  - https://business.instagram.com/creators/creator-marketplace
  - https://www.facebook.com/business/help/788160621327601
- TikTok One/TikTok Creator Marketplace patterns emphasize creator discovery, campaign collaboration, content performance, and first-party campaign reporting:
  - https://ads.tiktok.com/business/creativecenter
  - https://creatormarketplace.tiktok.com/
- Later's influencer reporting patterns emphasize creator/campaign analytics, posts, engagement, clicks, and conversion-style reporting:
  - https://developers.later.com/

The common production pattern is not only "store influencers"; it is a governed workflow tying creator identity to a campaign, contractual terms, deliverables, traceable links/codes, and performance snapshots.

## Product Design

Backend adds four additive tables:

- `creator_profile` stores tenant-scoped creator/KOL/KOC profiles.
- `creator_campaign` stores campaign briefs and commercial envelope.
- `creator_collaboration` stores one creator's commercial relationship to one campaign.
- `creator_deliverable` stores content obligations and performance evidence.

Backend adds `CreatorCollaborationService`:

- `upsertCreator` normalizes provider, handle, creator tier, tags, risk status, status, and metrics.
- `upsertCampaign` normalizes campaign key, objective, budget, currency, status, and date window.
- `upsertCollaboration` validates tenant ownership of campaign and creator, normalizes offer and status, and persists tracking link/discount code/commission terms.
- `upsertDeliverable` validates tenant ownership of the collaboration and records lifecycle plus latest metrics.
- `summary` returns tenant-scoped aggregate performance for campaign, creator, or collaboration filters.
- All JSON metadata is stored as JSON text with empty-object/list defaults; views expose parsed maps/lists.

Backend adds `CreatorCollaborationController`:

- `POST /canvas/creator-collaboration/creators`
- `POST /canvas/creator-collaboration/campaigns`
- `POST /canvas/creator-collaboration/collaborations`
- `POST /canvas/creator-collaboration/deliverables`
- `GET /canvas/creator-collaboration/summary`

## Functional Requirements

1. Schema must be additive only and must not edit applied migrations.
2. Creator identity must be unique per tenant, provider, and normalized handle.
3. Campaign key must be unique per tenant.
4. Collaboration must require tenant-owned creator and campaign records.
5. Deliverable writes must require tenant-owned collaboration records.
6. Summary must filter by campaign, creator, and collaboration and bound result windows to the current tenant.
7. Summary must calculate engagement as likes + comments + shares + saves, cost as fixed fee plus commission, commission as revenue times commission rate, and ROI as `(revenue - cost) / cost` when cost is positive.
8. Overdue deliverables must be counted when `due_at` is before evaluation time and status is not `POSTED`, `APPROVED`, `CANCELLED`, or `REJECTED`.
9. Local tests must not require real Shopify, Meta, TikTok, or Later credentials.

## Out Of Scope

- Real creator discovery/search APIs.
- Real Shopify, Meta, TikTok, Later, or affiliate-network API calls.
- Contract e-signature, payment execution, tax withholding, or invoice settlement.
- Frontend workbench for this first slice.
- Fraud detection and audience authenticity scoring.

## Acceptance Criteria

- P2-082O docs are indexed after P2-082N.
- Schema test proves creator/campaign/collaboration/deliverable tables and production indexes exist.
- Service tests prove creator/campaign upsert, tenant-guarded collaboration/deliverable writes, metric aggregation, ROI, commission, and overdue counting.
- Controller tests prove tenant/operator propagation for write endpoints and summary filters.
- Focused backend tests pass with Java 21:
  - `CreatorCollaborationSchemaTest,CreatorCollaborationServiceTest,CreatorCollaborationControllerTest`: 7/7 passed.
  - `PaidMediaAudienceSyncServiceTest,PaidMediaAudienceSyncControllerTest,CreatorCollaborationSchemaTest,CreatorCollaborationServiceTest,CreatorCollaborationControllerTest`: 15/15 passed.
  - `AbstractProviderConversationReplyAdapterTest,ConversationAdapterContractMatrixTest`: 37/37 passed after compatible conversation testCompile fixes.
