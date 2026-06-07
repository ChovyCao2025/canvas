# P2-082Q - Programmatic DSP Foundation Spec

Priority: P2
Sequence: 082Q
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082q-programmatic-dsp-foundation-plan.md`

## Goal

Add the first production-grade DSP/programmatic advertising foundation: DSP seats, campaign flights, line items, supply-path governance, daily performance evidence, and tenant-scoped pacing summaries.

## Delivery Status

Delivered backend first slice on 2026-06-06:

- DSP seat registry with provider identity, advertiser account, currency, timezone, supply-chain enforcement, and connector metadata.
- Campaign flight registry with objective, budget, date window, currency, and status.
- Line item ledger with bid strategy, max CPM, budget, pacing mode, targeting JSON, frequency cap, status, and tenant-owned campaign/seat linkage.
- Supply-path ledger with exchange, deal/package, seller identity, domain, ads.txt/sellers.json/SupplyChain status, and activation status.
- Daily performance snapshots for bid, win, impression, click, conversion, spend, revenue, and viewability evidence.
- Summary API that aggregates spend, impressions, bids, wins, clicks, conversions, revenue, win rate, CTR, CVR, CPA, ROAS, viewability rate, budget spent percent, and pacing status.
- Additive Flyway migration is `V324__programmatic_dsp_foundation.sql`.

## Current Baseline

P2-082E covers hashed paid-media audience sync, P2-082O covers creator collaboration, and P2-082P covers SEO/SEM evidence. The platform still lacks a DSP/programmatic operating model for seats, line items, supply-path governance, RTB-like evidence, pacing, and performance summaries.

## Research Inputs

- IAB Tech Lab OpenRTB is the canonical protocol family for real-time bid requests and responses across programmatic buying:
  - https://github.com/InteractiveAdvertisingBureau/openrtb2.x
- IAB Tech Lab ads.txt/app-ads.txt helps buyers identify authorized digital sellers and reduce counterfeit inventory:
  - https://iabtechlab.com/ads-txt/
- IAB Tech Lab sellers.json and SupplyChain object improve transparency into intermediaries and seller identities:
  - https://iabtechlab.com/sellers-json/
  - https://iabtechlab.com/supplychain-object/
- Google Display & Video 360 API models programmatic buying around advertisers, insertion orders, line items, budgets, pacing, and targeting resources:
  - https://developers.google.com/display-video/api/guides/managing-line-items
  - https://developers.google.com/display-video/api/reference/rest/v4/advertisers.insertionOrders

The common production pattern is connector-neutral governance first: model advertiser seats, flights, line items, supply paths, and normalized delivery evidence before allowing real provider mutation.

## Product Design

Backend adds five additive tables:

- `programmatic_dsp_seat` stores tenant-scoped DSP seats/accounts.
- `programmatic_dsp_campaign` stores campaign/insertion-order-like flights.
- `programmatic_dsp_line_item` stores line items with commercial controls and targeting evidence.
- `programmatic_dsp_supply_path` stores exchange/deal/seller/supply-chain authorization evidence.
- `programmatic_dsp_performance_snapshot` stores daily line-item performance evidence.

Backend adds `ProgrammaticDspService`:

- `upsertSeat` normalizes provider, seat key, currency, timezone, enforcement mode, and metadata.
- `upsertCampaign` normalizes campaign key, objective, budget, date window, currency, and status.
- `upsertLineItem` validates tenant-owned seat and campaign, then stores bid strategy, CPM cap, budget, pacing, targeting, frequency cap, and status.
- `upsertSupplyPath` validates tenant-owned line item and stores exchange/deal/seller/supply-chain evidence.
- `recordSnapshot` validates tenant-owned line item/campaign/seat and records daily delivery/performance metrics.
- `summary` returns tenant-scoped aggregate performance and pacing status.

Backend adds `ProgrammaticDspController`:

- `POST /canvas/programmatic-dsp/seats`
- `POST /canvas/programmatic-dsp/campaigns`
- `POST /canvas/programmatic-dsp/line-items`
- `POST /canvas/programmatic-dsp/supply-paths`
- `POST /canvas/programmatic-dsp/snapshots`
- `GET /canvas/programmatic-dsp/summary`

## Functional Requirements

1. Schema must be additive only and must not edit applied migrations.
2. Seat identity must be unique per tenant, provider, and seat key.
3. Campaign key must be unique per tenant.
4. Line item identity must be unique per tenant, campaign, and line item key.
5. Supply path identity must be unique per tenant, line item, exchange, deal id, and seller id.
6. Snapshot identity must be unique per tenant, seat, campaign, line item, and snapshot date.
7. Line item writes must require tenant-owned seat and campaign records.
8. Supply path and snapshot writes must require tenant-owned line item records.
9. Summary must filter by seat, campaign, line item, start date, and end date and remain tenant-bound.
10. Summary must calculate win rate as wins / bids, CTR as clicks / impressions, CVR as conversions / clicks, CPA as spend / conversions, ROAS as revenue / spend, viewability rate as viewable impressions / impressions, budget spent percent as spend / line-item or campaign budget, and pacing status from elapsed flight ratio versus spend ratio.
11. Local tests must not require real DV360, The Trade Desk, OpenRTB endpoint, exchange, or ad-server credentials.

## Out Of Scope

- Real OpenRTB bidder, bid request ingestion, or bid response generation.
- Real DV360, The Trade Desk, Google Ad Manager, exchange, or SSP API calls.
- Budget mutation, bid mutation, creative approval, ad serving, attribution import, or billing reconciliation.
- Frontend DSP workbench for this first slice.
- Fraud detection, brand-safety classification, and algorithmic bid optimization.

## Acceptance Criteria

- P2-082Q docs are indexed after P2-082P.
- Schema test proves seat/campaign/line-item/supply-path/snapshot tables and production uniqueness/indexes exist.
- Service tests prove seat/campaign/line-item upsert, tenant-guarded supply path and snapshot writes, summary metric math, pacing status, and tenant isolation.
- Controller tests prove tenant/operator propagation for all write endpoints and summary filters.
- Focused backend tests pass with Java 21.
