# P2-082Y - Search Marketing Provider Write Gateway Spec

Priority: P2
Sequence: 082Y
Parent spec: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082y-search-marketing-provider-write-gateway-plan.md`

## Problem

P2-082P delivered the SEO/SEM source, keyword, snapshot, opportunity, and summary foundation. Operators can see search performance and deterministic opportunities, but the platform still cannot safely turn approved opportunities into provider write operations such as adding negative keywords, changing keyword bids, pausing keywords, or changing campaign budgets. Direct provider calls from scripts would bypass tenant ownership, approvals, idempotency, dry-run validation, and audit evidence.

## Research Notes

- Google Ads exposes resource-specific mutate services and `GoogleAdsService.Mutate` for grouped resource changes; this makes provider writes naturally operation-based and audit-friendly.
- Google Ads partial failure guidance says valid operations can succeed while failed operations return errors; operators must preserve both success and failure evidence.
- Microsoft Advertising Campaign Management exposes keyword and negative keyword operations such as `AddKeywords`, `UpdateKeywords`, `AddNegativeKeywordsToEntities`, and `DeleteNegativeKeywordsFromEntities`; writes require OAuth/user account headers and return partial error structures.
- Search marketing write clients need a local control plane before real provider adapters: tenant validation, approval, dry-run requirement, idempotency, request hash, status transitions, sanitized request/response evidence, and a provider gateway that can refuse unsupported live writes.

Primary references:

- https://developers.google.com/google-ads/api/docs/mutating/overview
- https://developers.google.com/google-ads/api/docs/best-practices/partial-failures
- https://learn.microsoft.com/en-us/advertising/campaign-management-service/addkeywords?view=bingads-13
- https://learn.microsoft.com/en-us/advertising/campaign-management-service/addnegativekeywordstoentities?view=bingads-13
- https://learn.microsoft.com/en-us/advertising/guides/negative-keywords?view=bingads-13

## Scope

Backend first slice:

- Add an additive `search_marketing_mutation` ledger for provider write proposals, approval state, dry-run/apply execution state, request hash, idempotency key, sanitized payload, provider request/response evidence, and errors.
- Add typed commands and views for proposing, approving/rejecting, executing, and listing mutations.
- Add a provider write gateway abstraction that can dry-run supported payloads and fail closed when a live provider client is unavailable.
- Add search-marketing controller endpoints for mutation proposal, approval, execution, and listing.
- Validate tenant-owned source, keyword, and opportunity references before any mutation is proposed or executed.
- Keep provider credentials out of mutation payloads and rendered views; credential-backed real provider clients remain a later adapter layer.

## Supported First-Slice Mutation Types

- `ADD_KEYWORD`
- `UPDATE_KEYWORD_BID`
- `ADD_NEGATIVE_KEYWORD`
- `UPDATE_CAMPAIGN_BUDGET`
- `PAUSE_KEYWORD`

Each mutation must include a provider source, mutation type, entity type, idempotency key or mutation key, and JSON payload with the minimum fields required for the mutation type.

## Non-Goals

- Real Google Ads or Microsoft Advertising network calls in local tests.
- Bidding automation that applies changes without human approval.
- Campaign creation flows with cross-resource temporary IDs.
- Full provider-specific payload builders for every Google/Microsoft resource type.
- Frontend workbench for search marketing mutations.

## Acceptance Criteria

- P2-082Y docs are indexed after P2-082X.
- Schema test proves the mutation ledger and production indexes exist.
- Service tests prove proposal validation, tenant isolation, idempotency/request-hash handling, approval gating, dry-run-before-apply enforcement, provider gateway success, and fail-closed unsupported live writes.
- Controller tests prove tenant/operator propagation for propose, approve, execute, and list endpoints.
- Focused backend tests pass with Java 21.

## Implementation Status

Status: Delivered backend first slice on 2026-06-06.

- Additive Flyway migration `V338__search_marketing_mutation_gateway.sql`.
- Tenant-scoped `search_marketing_mutation` ledger with mutation key, idempotency key, request hash, approval status, dry-run/apply status, sanitized payload, provider request/response evidence, and error fields.
- `SearchMarketingMutationService` for propose, approve/reject, execute, and list flows.
- Fail-closed `SearchMarketingProviderWriteGateway`: dry-run validation can record evidence locally; live apply fails with `PROVIDER_CLIENT_UNAVAILABLE` unless a provider client is registered.
- Search marketing controller endpoints for mutation proposal, approval, execution, and listing.
