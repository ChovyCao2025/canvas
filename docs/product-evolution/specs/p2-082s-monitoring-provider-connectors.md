# P2-082S - Monitoring Provider Connectors Spec

Priority: P2
Sequence: 082S
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082s-monitoring-provider-connectors-plan.md`

## Goal

Add the first provider-specific monitoring connector layer so polling sources can fetch normalized mentions from real social, video-search, review, and creator-public-content APIs without storing raw provider secrets in source metadata.

## Implementation Status

Status: Delivered backend first slice on 2026-06-06.

- Provider-specific `MarketingMonitorPollClient` support for X recent search, YouTube search, Google Business reviews, and TikTok Research video query.
- Runtime credential resolution through environment/system-property references instead of raw source metadata secrets.
- Injectable HTTP transport plus Java `HttpClient` production transport.
- Provider request builders for auth, query, time windows, max-item bounds, page cursors, and TikTok structured cursor/search id.
- Normalized provider response mapping into `MarketingMonitorPollItem`.
- Sanitized response metadata that records provider/status/request id without raw token or API key values.
- Focused fake-transport tests with no external provider dependency.

## Current Baseline

Before P2-082S, P2-082M added `MarketingMonitorPollClient`, poll-run evidence, cursor tracking, and a sandbox metadata-backed poll client. P2-082N/R added scheduler, trend snapshots, and anomaly detection. Source polling could run, but only sandbox or caller-supplied data could supply mentions.

## Research Inputs

- X recent search uses the v2 recent search endpoint, bearer authorization, and pagination tokens such as `next_token`:
  - https://docs.x.com/x-api/posts/search-recent-posts
  - https://docs.x.com/x-api/posts/search/integrate/paginate
- YouTube Data API search.list returns `nextPageToken` and accepts request parameters such as `q`, `type`, `maxResults`, and page tokens:
  - https://developers.google.com/youtube/v3/docs/search/list
- Google Business Profile reviews.list retrieves a paginated list of reviews for a verified location and returns `nextPageToken`:
  - https://developers.google.com/my-business/reference/rest/v4/accounts.locations.reviews/list
- TikTok Research API video query uses bearer authorization, `max_count`, cursor, and `search_id` for pagination:
  - https://developers.tiktok.com/doc/research-api-get-started

## Product Design

Backend adds a provider poll client that implements the existing `MarketingMonitorPollClient` contract for:

- `X_RECENT_SEARCH`
- `YOUTUBE_SEARCH`
- `GOOGLE_BUSINESS_REVIEWS`
- `TIKTOK_RESEARCH_VIDEO`

Source metadata stores provider configuration, not provider secrets. Credentials are resolved at runtime from an explicit reference:

```json
{
  "query": "brand OR product",
  "brandKey": "our-brand",
  "credentials": {
    "mode": "BEARER_ENV",
    "tokenEnv": "MARKETING_MONITOR_X_BEARER"
  }
}
```

Supported credential modes:

- `BEARER_ENV`: resolves a bearer token from an environment variable or system property.
- `API_KEY_ENV`: resolves an API key from an environment variable or system property.

The client builds provider-specific HTTP requests, maps provider responses to `MarketingMonitorPollItem`, returns provider cursors through `MarketingMonitorPollResponse.nextCursor`, and writes sanitized metadata. Tests use an injected HTTP transport and credential resolver so no real network or credential is required.

## Functional Requirements

1. The connector must support the four source types listed above case-insensitively.
2. The connector must reject missing credential refs and missing resolved credential values before making an HTTP call.
3. The connector must never include raw access tokens or API keys in `MarketingMonitorPollResponse.metadata`.
4. X recent search must send bearer authorization, query, max results, optional time window, cursor token, and map post id/text/author/time.
5. YouTube search must send API key, query, max results, optional time window, page token, and map video id/title/description/channel/time.
6. Google Business reviews must send bearer authorization, account/location path, page size, page token, and map review id/comment/reviewer/rating/time.
7. TikTok Research video query must send bearer authorization, JSON query body, cursor/search_id, date window, and map video id/description/user/time.
8. Provider HTTP errors must fail the poll run through the existing polling service error path without partial ingestion.
9. Local tests must not require X, YouTube, Google, TikTok, OAuth, API keys, or external network access.

## Out Of Scope

- OAuth authorization UI and token refresh lifecycle.
- Storing encrypted provider credentials in database.
- Provider write operations, moderation actions, replies, ad activation, or campaign management.
- Full official API coverage beyond the first polling endpoints above.
- LLM sentiment inference.

## Acceptance Criteria

- P2-082S docs are indexed after P2-082R.
- Provider connector tests prove request construction, credential-ref resolution, sanitized metadata, cursor handling, and item mapping for X, YouTube, Google Business reviews, and TikTok.
- Polling service regression proves the provider client remains compatible with the existing ingestion and poll-run contract.
- Focused backend tests pass with Java 21.
