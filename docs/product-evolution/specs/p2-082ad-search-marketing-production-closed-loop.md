# P2-082AD - Search Marketing Production Closed Loop Spec

Priority: P2
Sequence: 082AD
Parent spec: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082ad-search-marketing-production-closed-loop-plan.md`

## Implementation Status

In progress - backend closed-loop foundation.

## Problem

P2-082P created SEO/SEM sources, keywords, snapshots, opportunities, and summary APIs. P2-082Y added a governed SEM mutation ledger. P2-082AB exposed mutations in the generic marketing-platform provider-write queue, and P2-082AC defines provider-write adapter contracts.

That still does not give operators a production-grade SEO / SEM management loop. The current product can store evidence and safely queue writes, but it cannot yet onboard search-provider credentials, ingest real search evidence on a schedule, let operators manage sources and opportunities in a dedicated workbench, apply changes through real provider adapters, reconcile provider state after writes, or prove whether a change improved performance.

## Research Inputs

Official provider behavior drives this design:

- Google Ads API uses OAuth credentials and Google Ads account context for programmatic account access:
  - https://developers.google.com/google-ads/api/docs/oauth/overview
- Google Ads mutate requests support validation-only and partial-failure execution, which maps directly to dry-run-first governed mutations:
  - https://developers.google.com/google-ads/api/reference/rpc/latest/MutateGoogleAdsRequest
  - https://developers.google.com/google-ads/api/docs/best-practices/partial-failures
- Google Ads batch jobs are the production path for large asynchronous batches rather than blocking the operator UI on long mutate calls:
  - https://developers.google.com/google-ads/api/docs/batch-processing/overview
- Google Ads change status and change event resources provide account-change evidence for post-write reconciliation:
  - https://developers.google.com/google-ads/api/docs/change-status
  - https://developers.google.com/google-ads/api/docs/change-event
- Google Search Console Search Analytics returns query/page/device/country traffic rows with clicks, impressions, CTR, and average position:
  - https://developers.google.com/webmaster-tools/v1/searchanalytics/query
- Google Search Console URL Inspection and Sitemaps APIs provide SEO technical/index evidence that is separate from query performance:
  - https://developers.google.com/webmaster-tools/v1/urlInspection.index/inspect
  - https://developers.google.com/webmaster-tools/v1/sitemaps
- Microsoft Advertising exposes keyword, negative keyword, and reporting operations with OAuth user/account headers:
  - https://learn.microsoft.com/en-us/advertising/guides/authentication-oauth?view=bingads-13
  - https://learn.microsoft.com/en-us/advertising/campaign-management-service/addkeywords?view=bingads-13
  - https://learn.microsoft.com/en-us/advertising/campaign-management-service/updatekeywords?view=bingads-13
  - https://learn.microsoft.com/en-us/advertising/campaign-management-service/addnegativekeywordstoentities?view=bingads-13
  - https://learn.microsoft.com/en-us/advertising/reporting-service/keywordperformancereportcolumn?view=bingads-13
  - https://learn.microsoft.com/en-us/advertising/guides/bulk-download-upload?view=bingads-13

The production pattern is a closed loop:

1. Credential and source onboarding.
2. Provider evidence ingestion with run history, quotas, cursors, and sanitized errors.
3. Normalized SEO/SEM keyword, page, and performance evidence.
4. Deterministic opportunities and human approval.
5. Provider validation or dry-run.
6. Idempotent live apply through provider adapters.
7. Provider-state reconciliation.
8. Post-change impact proof and opportunity closure.

## Current Baseline

Existing code already provides these foundations:

- `SearchMarketingService` stores sources, keywords, daily snapshots, opportunity evaluation, and summaries.
- `SearchMarketingMutationService` stores SEM provider-write proposals, approvals, dry-run/apply status, request hashes, idempotency keys, and sanitized provider evidence.
- `SearchMarketingProviderWriteGateway` supports dry-run validation and fail-closed live apply unless a client is registered.
- `SandboxSearchMarketingProviderWriteClient` provides deterministic sandbox apply behavior.
- Marketing monitoring already owns encrypted provider credentials, OAuth authorization, refresh, revocation, and credential events through `MarketingMonitorProviderCredentialService` and related controllers.
- `MarketingPlatformPage` shows a generic provider-write queue but not a dedicated SEO/SEM workbench.

## Product Design

Add a first-class `/search-marketing` workbench and backend closed-loop runtime. The workbench is the operator surface for SEO and SEM; the marketing-platform page remains the executive control plane and links into this workbench.

The workbench has seven tabs:

1. **Overview** - source freshness, spend/conversion/ROAS, SEO clicks/position, open opportunities, failed writes, and readiness gate.
2. **Sources and Credentials** - Google Ads, Microsoft Advertising, Google Search Console, Bing Webmaster, and sandbox sources mapped to encrypted credential keys.
3. **Keyword Portfolio** - SEO and SEM keyword list, match type, landing page, labels, intent, status, and source provenance.
4. **Performance Evidence** - normalized snapshots by keyword, page, query group, country, device, and date.
5. **SEO Technical Evidence** - Search Console URL inspection, sitemap state, indexed/crawlability evidence, and stale page alerts.
6. **Opportunities** - low CTR, wasted spend, SEO page-two, index coverage, budget pacing, negative keyword, landing-page mismatch, and cannibalization opportunities with evidence and severity.
7. **Provider Writes and Impact** - proposals, approval, dry-run, live apply, reconciliation status, provider errors, and pre/post impact windows.

## Backend Architecture

### Provider Credentials

Search marketing reuses the existing encrypted provider credential store instead of creating another secret system. A new `SearchMarketingCredentialResolver` accepts tenant id, provider, and credential key, then returns only a runtime-only token view for provider clients. No controller response, mutation payload, sync-run evidence, or frontend model may include access tokens, refresh tokens, client secrets, developer tokens, or SOAP headers.

Supported provider credential types:

- `GOOGLE_ADS`
- `GOOGLE_SEARCH_CONSOLE`
- `MICROSOFT_ADS`
- `BING_WEBMASTER`
- `SANDBOX_SEARCH`

### Provider Read Clients

Add `SearchMarketingProviderReadClient` implementations behind a gateway:

- `GoogleAdsSearchMarketingReadClient`
  - Pulls SEM keyword metrics with GAQL.
  - Reads campaign/ad group/keyword status and provider resource names.
  - Uses request windows, page tokens, and provider error classification.
- `GoogleSearchConsoleReadClient`
  - Pulls Search Analytics rows by query, page, country, and device.
  - Pulls URL inspection and sitemap evidence for selected landing pages.
- `MicrosoftAdsSearchMarketingReadClient`
  - Pulls keyword performance reports and bulk keyword state.
  - Reads negative keyword and budget state where supported.
- `SandboxSearchMarketingReadClient`
  - Provides deterministic local/staging evidence without external credentials.

Each read client returns normalized rows. The service layer decides how to upsert keywords, snapshots, URL inspection rows, and source freshness.

### Sync Runs

Add `SearchMarketingSyncRunService` and a scheduler:

- Manual run: `POST /canvas/search-marketing/sources/{sourceId}/sync`
- Due run: scheduled scan of enabled sources with active credentials and stale freshness.
- Run types:
  - `PERFORMANCE`
  - `SEO_TECHNICAL`
  - `PROVIDER_STATE`
  - `CHANGE_RECONCILIATION`
- Every run records status, window, cursor, counts, provider request id, sanitized error, and retryable flag.
- Scheduler uses tenant/source-scoped leases so multiple app instances do not run the same source/window at the same time.
- Backoff is per source and provider error class. Quota/rate-limit errors do not disable sources automatically; authentication and permission errors mark readiness degraded until fixed.

### Provider Writes

Extend the SEM mutation gateway from P2-082Y/P2-082AC:

- Google Ads apply uses mutate validation for dry-run and mutate or batch jobs for live apply depending on operation count.
- Google Ads requests use partial failure where the operation is safe to split; atomic multi-resource changes stay grouped.
- Microsoft Ads apply uses campaign-management or bulk operations depending on operation count and supported entity type.
- Live apply remains impossible unless:
  - source is enabled;
  - source has an active compatible credential;
  - mutation is approved;
  - dry-run has passed when required;
  - readiness gate allows provider writes;
  - idempotency key has not already produced a successful provider operation.

### Reconciliation And Impact

Provider writes are not considered closed when the apply call returns. They close only after:

- provider operation id or batch job id is stored;
- provider state is re-read or change history confirms the mutation;
- local mutation status moves to `RECONCILED` or `RECONCILE_FAILED`;
- a post-change performance window is scheduled;
- the opportunity closes as `IMPACT_POSITIVE`, `IMPACT_NEUTRAL`, `IMPACT_NEGATIVE`, or `ROLLBACK_REQUIRED`.

Impact windows compare baseline and post-change periods with the same source, keyword/page, country/device filters, and attribution window. They store metric deltas for impressions, clicks, CTR, cost, conversions, revenue, ROAS, average position, and indexed state.

## Data Model

Additive migration only:

- `search_marketing_sync_run`
  - Tenant/source scoped provider run history for ingestion and reconciliation.
  - Unique idempotency key per tenant/source/run type/window/cursor.
  - Indexes by tenant/status/run type/started_at and tenant/source/status/updated_at.
- `search_marketing_url_inspection`
  - SEO technical evidence by tenant/source/page URL/date/provider.
  - Stores indexed state, crawl state, canonical URL, sitemap presence, mobile usability state, and sanitized raw evidence.
- `search_marketing_provider_change`
  - Provider-side change evidence linked to local mutation when possible.
  - Stores external resource name/id, change type, changed fields, provider actor, provider changed time, and reconciliation status.
- `search_marketing_impact_window`
  - Pre/post performance proof linked to opportunity and mutation.
  - Stores baseline window, post window, metric deltas, decision, confidence, and evidence JSON.

Existing tables remain the source of truth for normalized source, keyword, snapshot, opportunity, and mutation state.

## API Design

Extend `SearchMarketingController`:

- `GET /canvas/search-marketing/sources`
- `GET /canvas/search-marketing/keywords`
- `GET /canvas/search-marketing/snapshots`
- `GET /canvas/search-marketing/opportunities`
- `POST /canvas/search-marketing/opportunities/{opportunityId}/status`
- `POST /canvas/search-marketing/opportunities/{opportunityId}/mutations`
- `GET /canvas/search-marketing/url-inspections`
- `GET /canvas/search-marketing/sync-runs`
- `POST /canvas/search-marketing/sources/{sourceId}/sync`
- `POST /canvas/search-marketing/sources/sync-due`
- `GET /canvas/search-marketing/provider-changes`
- `POST /canvas/search-marketing/mutations/{mutationId}/reconcile`
- `GET /canvas/search-marketing/impact-windows`
- `POST /canvas/search-marketing/impact-windows/evaluate-due`
- `GET /canvas/search-marketing/readiness`

Existing write endpoints remain:

- `POST /canvas/search-marketing/sources`
- `POST /canvas/search-marketing/keywords`
- `POST /canvas/search-marketing/snapshots`
- `POST /canvas/search-marketing/opportunities/evaluate`
- `POST /canvas/search-marketing/mutations`
- `POST /canvas/search-marketing/mutations/{mutationId}/approve`
- `POST /canvas/search-marketing/mutations/{mutationId}/execute`
- `GET /canvas/search-marketing/mutations`
- `GET /canvas/search-marketing/summary`

## Frontend Design

Add `frontend/src/pages/search-marketing` with production-tool layout, not a landing page:

- Header: title, freshness status, date range, source selector, refresh button.
- KPI row: SEO clicks, SEM spend, conversions, ROAS, open opportunities, failed writes, unreconciled writes.
- Tabs map to the seven workbench areas above.
- Tables use existing Ant Design patterns with filters, status tags, compact row actions, and empty/error states.
- Mutation proposal flow is form-based and prefilled from an opportunity. It never exposes provider credentials.
- Live apply remains behind confirmation and requires dry-run success.
- The sidebar adds a child item under "自动化营销": `SEO / SEM 管理` -> `/search-marketing`.
- Marketing platform capability links for Search Marketing route to `/search-marketing`, while API root remains `/canvas/search-marketing`.

## Production Readiness Rules

The search-marketing readiness gate is `LIVE` only when:

1. At least one enabled source has an active compatible provider credential.
2. Latest successful performance sync is fresh within the tenant freshness SLA.
3. Latest failed sync is not a blocker-class auth, permission, schema, or quota-exhaustion error.
4. Provider write adapters are registered for every enabled SEM provider that allows live writes.
5. No live-applied mutation remains unreconciled past the reconciliation SLA.
6. Impact windows are evaluated for all reconciled mutations whose post window has elapsed.
7. Credential lifecycle has no expired active credential.
8. No secret-shaped key appears in sync-run, mutation, provider-change, impact, or UI evidence.

## Functional Requirements

1. All schema changes must be additive and must not edit applied Flyway migrations.
2. All read/write/sync APIs must be tenant scoped.
3. Sync runs must be idempotent for the same tenant, source, run type, window, and cursor.
4. Provider credentials must be resolved only at adapter execution time and must never be persisted in evidence JSON.
5. Source sync must store run history for success, partial success, and failure.
6. Performance sync must upsert normalized keyword and snapshot evidence without creating duplicate identities.
7. SEO technical sync must store URL inspection and sitemap-derived evidence separately from keyword snapshots.
8. Opportunity evaluation must be repeatable and must not create duplicate open opportunities for the same evidence identity.
9. Operators must be able to list, filter, accept, mute, close, and convert opportunities into governed mutations.
10. Dry-run must delegate to real provider validation when the provider supports it; otherwise it must record local validation evidence and keep live apply fail-closed unless an adapter exists.
11. Live apply must preserve provider request ids, operation ids, batch job ids, partial failure details, and sanitized errors.
12. Reconciliation must read provider state or change history before marking a mutation reconciled.
13. Post-change impact evaluation must compare baseline and post windows and persist the decision.
14. The frontend workbench must not depend on real provider credentials for unit tests.
15. Local and CI tests must use sandbox clients, fixtures, and mocked transport; real provider smoke tests must be opt-in integration tests.

## Non-Goals

- Automated bidding that applies changes without human approval.
- Full SEO crawler implementation beyond provider URL inspection, sitemap, and landing-page evidence.
- Cross-channel media-mix optimization.
- Keyword Planner forecasting UI.
- Vendor-specific advanced campaign builders beyond the listed mutation types.
- Storing raw provider OAuth tokens, developer tokens, SOAP headers, or API keys outside the encrypted credential store.

## Acceptance Criteria

- P2-082AD docs are indexed after P2-082AC and before P2-083.
- Backend schema tests prove the four new tables, uniqueness constraints, and indexes exist.
- Backend service tests prove sync run idempotency, source freshness, snapshot/url-inspection upserts, duplicate opportunity suppression, tenant isolation, and readiness gates.
- Provider contract tests prove Google/Microsoft/sandbox adapters classify retryable errors, sanitize evidence, delegate dry-run, and fail closed when credentials/adapters are missing.
- Mutation tests prove live apply requires approval, dry-run, compatible credential, registered adapter, idempotency, and successful readiness gate.
- Reconciliation tests prove provider changes link to local mutations and move statuses to `RECONCILED` or `RECONCILE_FAILED`.
- Impact tests prove baseline/post deltas and opportunity closure decisions are persisted.
- Controller tests prove every new API propagates tenant/operator context and enforces filters.
- Frontend service and presentation tests cover workbench loading, tab state, status labels, proposal creation, dry-run/apply gating, and secret redaction.
- Browser verification proves `/search-marketing` renders at desktop and mobile widths, uses the new sidebar item, and does not overlap text or expose secret-shaped fields.
- Focused backend and frontend commands pass:
  - `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/canvas-engine/pom.xml test -Dtest=SearchMarketingProductionClosedLoopSchemaTest,SearchMarketingSyncRunServiceTest,SearchMarketingProviderAdapterContractTest,SearchMarketingReconciliationServiceTest,SearchMarketingImpactWindowServiceTest,SearchMarketingControllerTest`
  - `cd frontend && npm run test -- searchMarketingApi.test.ts searchMarketingWorkbench.test.ts searchMarketingPage.test.tsx`
  - `cd frontend && npm run build`
