# P2-082E - Paid-Media Audience Sync Spec

Priority: P2
Sequence: 082E
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082e-paid-media-audience-sync-plan.md`

## Goal

Add a production-shaped paid-media audience activation foundation that can export tenant-scoped CDP audience members to sandbox, Google/Meta-style, or DSP destinations using hashed identifiers, explicit eligibility checks, and auditable sync runs without requiring real provider credentials in local tests.

## Current Baseline

The platform already has audience definitions, CDP user profiles, marketing consent, audience materialization, and operational run ledgers. What is missing for paid-media activation is:

- Provider destination records for ad accounts and external audience ids.
- A deterministic hashed member export contract.
- Consent and profile eligibility gates before export.
- A sync-run ledger with requested, eligible, skipped, failed, and provider operation evidence.
- APIs for operators or connector jobs to trigger and audit audience syncs.

## Product Design

The first slice is provider-agnostic and sandbox-safe. It does not call Google, Meta, or DSP APIs directly. Instead it persists the destination configuration, derives SHA-256 hashes from tenant-scoped CDP profile identifiers, records per-member eligibility, and writes an audit run. A later connector can read the same run/member records and perform real provider upload behind provider-specific policy gates.

Data model:

- `paid_media_audience_destination`: configured provider destination, account, external audience id, identifier types, consent channel, and policy metadata.
- `paid_media_audience_member`: one row per run member outcome, including eligible hashed identifiers and skipped reasons.
- `paid_media_audience_sync_run`: immutable-ish run ledger with counters, actor, provider operation id, error, and metadata.

## API Contract

### Upsert Destination

`POST /canvas/paid-media/audience-sync/destinations`

```json
{
  "provider": "META",
  "destinationKey": "meta-vip-lookalike",
  "displayName": "Meta VIP Custom Audience",
  "accountId": "act_123",
  "externalAudienceId": "2385000000",
  "identifierTypes": ["EMAIL", "PHONE"],
  "consentChannel": "PAID_MEDIA",
  "enforceConsent": true,
  "enabled": true,
  "metadata": { "policy": "customer-match" }
}
```

### Sync Audience

`POST /canvas/paid-media/audience-sync/runs`

```json
{
  "destinationId": 10,
  "audienceId": 20,
  "userIds": ["u-1", "u-2"],
  "externalOperationId": "sandbox-upload-1",
  "metadata": { "source": "manual" }
}
```

The service validates the destination, audience, tenant, consent channel, and CDP profile identifiers. It stores only normalized SHA-256 hashes for eligible identifiers.

### Query Runs

`GET /canvas/paid-media/audience-sync/runs?destinationId=10&audienceId=20&status=SUCCESS&limit=50`

### Query Members

`GET /canvas/paid-media/audience-sync/runs/{runId}/members?status=ELIGIBLE&limit=100`

## Functional Requirements

1. Destination provider names and identifier types must be normalized to uppercase.
2. Destination upsert must be idempotent by `tenant_id + provider + destination_key`.
3. A sync run must require an enabled tenant-scoped destination and enabled tenant-scoped audience definition.
4. Sync input user ids must be bounded and deduplicated while preserving first-seen order.
5. CDP profiles must be read with tenant scope.
6. When consent enforcement is enabled, only users with `OPT_IN` for the destination consent channel are eligible.
7. Email identifiers must be lowercased and trimmed before hashing.
8. Phone identifiers must be trimmed and punctuation-normalized before hashing.
9. Eligible exports must store only SHA-256 hashes, never raw email or phone values.
10. Missing profile, missing identifiers, and consent-denied cases must create member audit rows with explicit reasons.
11. Sync runs must record requested, eligible, skipped, failed counts, actor, provider, destination, audience, external operation id, metadata, and timestamps.
12. Query limits must be bounded to 1..100 rows.
13. Read APIs must never return cross-tenant runs or members.

## Out Of Scope

- Real Google Ads, Meta, DSP, or KOL/SEO/SEM API calls.
- Provider credential storage and OAuth.
- Offline audience membership resolution from bitmap storage.
- Frontend paid-media management screens.
- Revenue attribution back from providers.

## Acceptance Criteria

- This spec and plan are indexed after P2-082D2.
- Migration `V308__paid_media_audience_sync.sql` creates destination, member, and run tables with tenant/provider uniqueness and query indexes.
- Schema test proves table and index names exist.
- Service tests prove destination upsert, hashed export, consent/profile eligibility, failure run recording, and tenant-scoped queries.
- Controller tests prove tenant/operator propagation and bounded limits.
- Focused backend tests pass with Java 21.
