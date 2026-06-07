# P2-082G - Sentiment And Competitor Monitoring Spec

Priority: P2
Sequence: 082G
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082g-sentiment-competitor-monitoring-plan.md`

## Goal

Add the first backend slice for sentiment and competitor monitoring so marketing teams can ingest monitored mentions, classify sentiment, extract competitor references, and manage alert workflow state without relying on live social/search credentials in local tests.

## Delivery Status

Delivered backend first slice:

- Tenant-scoped monitoring source, item, sentiment, competitor mention, and alert tables.
- Domain service for source upsert, item ingestion, deterministic lexicon scoring, competitor extraction, alert creation, filtered reads, and alert resolution.
- WebFlux controller APIs under `/canvas/marketing-monitoring`.
- Focused schema, service, and controller tests verified with Java 21.

## Current Baseline

The platform now has conversation adapters, content management, BI reporting, paid-media activation, and AI decision recommendations. It still lacks a governed monitoring data plane for public or private-domain mentions:

- No source registry for social, web, review, community, or manual monitoring feeds.
- No normalized monitored-item table with provenance and raw payload audit.
- No deterministic sentiment analysis contract.
- No competitor mention extraction.
- No alert workflow for negative sentiment or competitor spikes.

## Product Design

The first slice is ingestion-first and connector-neutral. Real crawlers, social APIs, and search providers can later write into the same source/item contract. Local tests use manual and sandbox sources only.

Data model:

- `marketing_monitor_source`: tenant-scoped source registry with source key, type, display name, enabled flag, and metadata.
- `marketing_monitor_item`: normalized mention item with external id, text, author, URL, brand key, timestamps, and raw payload JSON.
- `marketing_sentiment_analysis`: deterministic sentiment label, score, confidence, keyword evidence, model key/version, and timestamp.
- `marketing_competitor_mention`: extracted competitor key/name, matched terms, sentiment, and score.
- `marketing_monitor_alert`: alert workflow row for negative sentiment, competitor mention, or future spike rules.

## API Contract

### Upsert Source

`POST /canvas/marketing-monitoring/sources`

```json
{
  "sourceKey": "manual-social-listening",
  "sourceType": "MANUAL",
  "displayName": "Manual Social Listening",
  "enabled": true,
  "metadata": { "owner": "brand-team" }
}
```

### Ingest Item

`POST /canvas/marketing-monitoring/items`

```json
{
  "sourceId": 10,
  "externalItemId": "post-1",
  "sourceUrl": "https://example.com/post-1",
  "authorKey": "author-1",
  "brandKey": "our-brand",
  "text": "CompetitorX launched faster checkout, but our support is better.",
  "publishedAt": "2026-06-06T10:00:00",
  "competitors": {
    "competitorx": ["CompetitorX", "CX"]
  },
  "rawPayload": { "provider": "manual" }
}
```

The service persists the item, analyzes sentiment, extracts competitor mentions, and creates alert rows when negative sentiment or competitor-negative evidence is found.

### Query Items

`GET /canvas/marketing-monitoring/items?sentimentLabel=NEGATIVE&competitorKey=competitorx&limit=50`

### Query Alerts

`GET /canvas/marketing-monitoring/alerts?status=OPEN&limit=50`

### Resolve Alert

`POST /canvas/marketing-monitoring/alerts/{alertId}/resolve`

## Functional Requirements

1. All sources, items, analyses, competitor mentions, and alerts must be tenant-scoped.
2. Source upsert must be idempotent by `tenant_id + source_key`.
3. Disabled or cross-tenant sources must reject ingestion.
4. Item ingestion must be idempotent by `tenant_id + source_id + external_item_id`.
5. Sentiment analysis must be deterministic and persist label, score, confidence, keyword evidence, model key, and model version.
6. Positive and negative keyword hits must both be retained in evidence JSON.
7. Competitor extraction must accept a caller-provided competitor term map and persist all matched competitors.
8. Negative monitored items must create an open `NEGATIVE_SENTIMENT` alert.
9. Negative competitor mentions must create an open `COMPETITOR_NEGATIVE` alert.
10. Query limits must be bounded to 1..100.
11. Read APIs must never return cross-tenant items or alerts.
12. Alert resolution must reject cross-tenant alert ids and persist resolver and timestamp.

## Sentiment Baseline

The first model is a transparent lexicon scorer:

- Negative terms include churn, bad, slow, broken, complaint, angry, fail, worse, refund, and poor.
- Positive terms include great, good, fast, love, better, excellent, happy, smooth, win, and recommend.
- Score is normalized to -1..1.
- Label is `NEGATIVE`, `POSITIVE`, or `NEUTRAL`.
- Confidence increases with keyword evidence and drops for empty or very short text.

## Out Of Scope

- Real social/search/review crawling.
- LLM sentiment inference.
- Automated public reply generation.
- Alert fanout to Feishu, Slack, PagerDuty, email, SMS, or push.
- Frontend monitoring workbench.

## Acceptance Criteria

- This spec and plan are indexed after P2-082F.
- Migration `V311__sentiment_competitor_monitoring.sql` creates source, item, sentiment, competitor mention, and alert tables.
- Schema test proves table and index names exist.
- Service tests prove source upsert, ingestion sentiment, competitor extraction, alert creation, tenant-scoped reads, and alert resolution.
- Controller tests prove tenant/operator propagation and bounded limits.
- Focused backend tests pass with Java 21.
