# P2-082I - Monitoring Webhook Ingestion Spec

Priority: P2
Sequence: 082I
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082i-monitoring-webhook-ingestion-plan.md`

## Goal

Turn the P2-082G/P2-082H monitoring data plane into a real inbound integration surface by adding signed public webhook ingestion for monitored mentions, with source-scoped secrets, replay protection, payload normalization, and idempotent item creation.

## Implementation Status

Delivered backend first slice:

- Additive `marketing_monitor_source` webhook credential columns.
- Source-scoped webhook secret rotation with BCrypt hash, AES-GCM ciphertext, prefix display, and raw secret returned once.
- Public signed webhook endpoint at `/public/marketing-monitoring/webhooks/{tenantId}/{sourceKey}`.
- HMAC-SHA256 raw-body signature verification with timestamp replay guard and constant-time comparison.
- Generic social-listening payload mapper with metadata fallback for brand and competitor terms.
- Security filter permit rule only for the signed public monitoring webhook path.

## Research Inputs

- Stripe webhook guidance treats signature verification over the unmodified request body as the primary authenticity control and uses timestamp tolerance to reduce replay risk: https://docs.stripe.com/webhooks/signature
- Slack signs requests with a signing secret, timestamp, and raw request body, and recommends rejecting stale timestamps: https://api.slack.com/authentication/verifying-requests-from-slack
- GitHub webhook delivery validation uses `X-Hub-Signature-256` HMAC-SHA256 and constant-time comparison against the raw payload: https://docs.github.com/en/webhooks/using-webhooks/validating-webhook-deliveries
- Social-listening webhook products expose near-real-time keyword/mention callbacks so brands can react to monitored conversations without polling. AnySearch surfaced examples from MentionsAPI and KWatch documenting webhook-style social-listening delivery.

## Current Baseline

P2-082G can ingest monitored items only through authenticated API calls to `POST /canvas/marketing-monitoring/items`. P2-082H gives operators a manual UI, but external social/search/review providers still cannot push mention events directly into the product. Monitoring sources also lack source-scoped webhook secrets, signature tolerance, and public callback paths.

Existing foundations to reuse:

- `marketing_monitor_source` for tenant-scoped source registry.
- `MarketingMonitoringService.ingestItem` for idempotent item persistence, lexicon sentiment, competitor extraction, and alert creation.
- `SecretCipher` for AES-GCM encrypted secrets.
- SecurityConfig pattern where anonymous public webhooks are permitted at the filter layer and validate signatures inside controllers.

## Approach Decision

Three options were considered:

- Provider-specific controllers first. This fits Meta/GitHub-style signatures, but it would duplicate parsing before the product has multiple live providers configured.
- Polling connectors first. This is useful for historical backfill, but it does not close the real-time alerting gap.
- Source-scoped generic signed webhook first. This gives immediate production-safe push ingestion and keeps provider-specific mappers as small later additions.

Chosen approach: source-scoped generic signed webhook first.

## Product Design

Add an authenticated management endpoint that rotates a monitoring source webhook secret and returns the raw secret once. The stored source row keeps only secret prefix, BCrypt hash, AES-GCM ciphertext, enabled flag, and timestamp tolerance.

Add a public callback endpoint:

`POST /public/marketing-monitoring/webhooks/{tenantId}/{sourceKey}`

Required headers:

- `X-Canvas-Monitoring-Timestamp`: Unix epoch seconds.
- `X-Canvas-Monitoring-Signature`: `sha256=<hex hmac sha256>` over `timestamp + "\n" + rawBody`.

The endpoint must:

- Resolve a tenant-scoped enabled source by `tenantId` and `sourceKey`.
- Reject disabled sources, missing secrets, missing headers, stale timestamps, and invalid signatures before JSON parsing or persistence.
- Parse a generic mention JSON payload into `MarketingMonitorItemIngestCommand`.
- Use source metadata as fallback for `defaultBrandKey` and `competitors`.
- Preserve the raw provider payload in `rawPayload`.
- Reuse the existing `MarketingMonitoringService.ingestItem` idempotency on `(tenant_id, source_id, external_item_id)`.

Supported generic payload fields:

- `externalItemId`, `external_item_id`, `id`, or `eventId`.
- `text`, `textContent`, `message`, or `content`.
- `sourceUrl`, `url`, or `permalink`.
- `authorKey`, `author_key`, `authorId`, or nested `author.id`.
- `brandKey`, `brand_key`, `brand`, or source metadata `defaultBrandKey`.
- `language` or `lang`.
- `publishedAt`, `published_at`, `createdAt`, `created_at`, or numeric epoch seconds/milliseconds.
- `competitors`, either object map or list of `{ key, terms }` objects.

## Functional Requirements

1. Add additive Flyway migration `V314__monitoring_webhook_ingestion.sql`.
2. Extend `MarketingMonitorSourceDO` with webhook enabled flag, secret prefix, secret hash, secret ciphertext, and signature tolerance seconds.
3. Add source webhook secret rotation API returning raw secret only in the rotation response.
4. Add HMAC-SHA256 verifier using raw body, timestamp, constant-time compare, and source-specific tolerance.
5. Add generic payload mapper with deterministic field fallback and metadata fallback.
6. Add public webhook controller path under `/public/marketing-monitoring/webhooks/{tenantId}/{sourceKey}`.
7. Add SecurityConfig permit rule for the public monitoring webhook path only.
8. Do not bypass existing tenant scoping, source enabled checks, item idempotency, sentiment analysis, competitor extraction, or alert creation.
9. Focused backend tests must cover migration contract, secret rotation, valid signed ingestion, replay rejection, invalid signature rejection, payload mapping, controller wiring, and security route access.

## Out Of Scope

- Polling crawlers and historical backfill workers.
- Provider-specific Meta, TikTok, YouTube, Reddit, app-store, or search-review mappers.
- IP allowlists, mTLS, KMS-backed secret material, and per-source rate limiting.
- LLM sentiment inference or AI-generated replies.
- Alert fanout to Feishu, Slack, PagerDuty, email, SMS, or push.

## Acceptance Criteria

- This spec and plan are indexed after P2-082H.
- Migration adds only new columns to `marketing_monitor_source`.
- A source secret can be rotated and stored encrypted while returning raw secret once.
- A correctly signed generic webhook payload produces a `MarketingMonitorIngestResult`.
- A stale timestamp or invalid signature does not call `MarketingMonitoringService.ingestItem`.
- `/public/marketing-monitoring/webhooks/**` is anonymous at the filter layer.
- Focused backend tests pass with Java 21.
