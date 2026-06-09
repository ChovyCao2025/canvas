# P2-082P - Search Marketing Foundation Spec

Priority: P2
Sequence: 082P
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082p-search-marketing-foundation-plan.md`

## Goal

Add the first production-grade SEO/SEM management foundation: search sources, keyword portfolio, daily performance evidence, deterministic opportunity detection, and tenant-scoped reporting APIs.

## Implementation Status

Status: Delivered backend first slice on 2026-06-06.

- Search source registry for Google Ads, Microsoft Advertising, Google Search Console, or sandbox imports.
- Keyword portfolio with normalized keyword identity, match type, landing page, intent, labels, and status.
- Daily performance snapshot ledger for SEO and SEM metrics.
- Opportunity ledger for low CTR, SEO page-two opportunity, and wasted paid-search spend.
- Summary API that aggregates impressions, clicks, cost, conversions, revenue, CTR, CPC, conversion rate, ROAS, and average position.
- Additive Flyway migration is `V322__search_marketing_foundation.sql`.

## Current Baseline

P2-082E covers paid-media audience activation, P2-082O covers creator collaboration, and P2-082G/N cover monitoring. The platform still has no first-class search-marketing workspace for SEO/SEM keywords, search-console evidence, paid-search performance evidence, or deterministic optimization opportunities.

## Research Inputs

- Google Ads API keyword planning can generate keyword ideas from keyword, URL, keyword+URL, or site seeds, and returns historical statistics such as search volume for campaign planning:
  - https://developers.google.com/google-ads/api/docs/keyword-planning/generate-keyword-ideas
- Google Ads API exposes keyword and performance metrics such as `metrics.clicks`, `segments.keyword.info.text`, and `segments.keyword.info.match_type` for keyword-level reporting:
  - https://developers.google.com/google-ads/api/fields/v21/metrics
- Google Search Console Search Analytics API queries search traffic with custom filters, date ranges, and dimensions such as country, device, page, and query; responses include clicks, impressions, CTR, and average position:
  - https://developers.google.com/webmaster-tools/v1/searchanalytics/query
- Microsoft Advertising keyword performance reports expose keyword-level reporting columns for search advertising operations:
  - https://learn.microsoft.com/en-us/advertising/reporting-service/keywordperformancereportcolumn?view=bingads-13

The common production pattern is not only importing ad spend. Search operators need a governed keyword portfolio, source provenance, normalized SEO/SEM metrics, and recommendation evidence that can later be connected to provider APIs.

## Product Design

Backend adds four additive tables:

- `search_marketing_source` stores tenant-scoped source/property/account configuration.
- `search_marketing_keyword` stores normalized SEO/SEM keyword portfolio entries.
- `search_marketing_snapshot` stores daily keyword/search performance evidence.
- `search_marketing_opportunity` stores deterministic optimization opportunities and workflow status.

Backend adds `SearchMarketingService`:

- `upsertSource` normalizes provider, source key, channel, currency, timezone, and connector metadata.
- `upsertKeyword` normalizes keyword text, keyword key, match type, landing page, intent, labels, and status.
- `recordSnapshot` validates tenant-owned source and keyword records, then persists daily metrics with source provenance.
- `evaluateOpportunities` scans scoped snapshots and creates deterministic opportunity rows for low CTR, SEO page-two terms, and paid-search wasted spend.
- `summary` returns tenant-scoped aggregate performance for channel/source/keyword/date filters.

Backend adds `SearchMarketingController`:

- `POST /canvas/search-marketing/sources`
- `POST /canvas/search-marketing/keywords`
- `POST /canvas/search-marketing/snapshots`
- `POST /canvas/search-marketing/opportunities/evaluate`
- `GET /canvas/search-marketing/summary`

## Functional Requirements

1. Schema must be additive only and must not edit applied migrations.
2. Source identity must be unique per tenant, provider, and source key.
3. Keyword identity must be unique per tenant, channel, keyword key, match type, and landing page URL.
4. Snapshot identity must be unique per tenant, source, keyword, snapshot date, device, country, and query group.
5. Snapshot writes must require tenant-owned source and keyword records.
6. Summary must filter by channel, source, keyword, start date, and end date and remain tenant-bound.
7. Summary must calculate CTR as clicks / impressions, CPC as cost / clicks, conversion rate as conversions / clicks, ROAS as revenue / cost, and weighted average position by impressions.
8. Opportunity evaluation must produce:
   - `LOW_CTR` when impressions meet threshold and CTR is below threshold.
   - `SEO_PAGE_TWO` when SEO average position is greater than or equal to the page-two threshold and impressions meet threshold.
   - `WASTED_SPEND` when SEM cost exceeds threshold and conversions are zero.
9. Opportunity rows must carry evidence JSON and default to `OPEN`.
10. Local tests must not require real Google Ads, Google Search Console, Microsoft Advertising, or other provider credentials.

## Out Of Scope

- Real Google Ads, Microsoft Advertising, Baidu, or Google Search Console API calls.
- Keyword Planner remote calls and forecast generation.
- Bid mutation, budget mutation, negative keyword mutation, or campaign/ad creation.
- Frontend search-marketing workbench for this first slice.
- SEO crawler, rank tracking crawler, sitemap inspection, and technical SEO audit.

## Acceptance Criteria

- P2-082P docs are indexed after P2-082O.
- Schema test proves source/keyword/snapshot/opportunity tables and production uniqueness/indexes exist.
- Service tests prove source/keyword upsert, tenant-guarded snapshot writes, summary metric math, opportunity creation, and tenant isolation.
- Controller tests prove tenant/operator propagation for write endpoints and summary/evaluation filters.
- Focused backend tests pass with Java 21:
  - `SearchMarketingSchemaTest,SearchMarketingServiceTest,SearchMarketingControllerTest`: 8/8 passed.
  - `PaidMediaAudienceSyncServiceTest,PaidMediaAudienceSyncControllerTest,CreatorCollaborationSchemaTest,CreatorCollaborationServiceTest,CreatorCollaborationControllerTest,SearchMarketingSchemaTest,SearchMarketingServiceTest,SearchMarketingControllerTest`: 23/23 passed.
  - `AbstractProviderConversationReplyAdapterTest,ConversationAdapterContractMatrixTest`: 41/41 passed after a compatible provider-adapter compile fix.
