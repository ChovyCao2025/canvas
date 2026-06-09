# P2-082U - Monitoring Provider Credential Lifecycle Spec

Priority: P2
Sequence: 082U
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082u-monitoring-provider-credential-lifecycle-plan.md`

## Implementation Status

Delivered backend first slice. Verification results are recorded in `../plans/p2-082u-monitoring-provider-credential-lifecycle-plan.md`.

## Goal

Add a production-grade credential lifecycle layer for monitoring provider connectors so sources can reference tenant-scoped encrypted API keys and OAuth access tokens instead of environment variables or raw metadata secrets.

## Current Baseline

P2-082S added provider poll clients for X, YouTube, Google Business reviews, and TikTok Research. That slice intentionally limited credentials to `BEARER_ENV` and `API_KEY_ENV` references. This is safe for local tests, but not enough for production teams because operators need tenant-scoped credential records, refresh-token rotation, disabled states, and audit evidence.

## Research Inputs

- OAuth 2.0 defines refresh tokens as credentials used to obtain new access tokens and defines `grant_type=refresh_token` token-endpoint requests:
  - https://www.rfc-editor.org/rfc/rfc6749
- Google OAuth web-server apps require offline access to refresh access tokens without prompting the user, and HTTP refresh uses `POST` with `client_id`, optional `client_secret`, `grant_type=refresh_token`, and `refresh_token`:
  - https://developers.google.com/identity/protocols/oauth2/web-server#offline
  - https://developers.google.com/identity/protocols/oauth2/web-server#refresh
- X OAuth 2.0 documents `POST /2/oauth2/token` refresh-token exchange and token revocation:
  - https://docs.x.com/fundamentals/authentication/oauth-2-0/user-access-token
- TikTok recommends server-side token storage and describes refresh tokens as the mechanism used to renew access tokens:
  - https://developers.tiktok.com/doc/login-kit-manage-user-access-tokens

## Product Design

The first slice adds a provider credential registry and event ledger:

- `marketing_monitor_provider_credential`
- `marketing_monitor_provider_credential_event`

Credentials are tenant-scoped and keyed by `credentialKey`. Operators can create/update credentials, list sanitized records, refresh OAuth credentials, disable credentials, and inspect lifecycle events. Provider source metadata then references credentials:

```json
{
  "query": "brand OR product",
  "brandKey": "our-brand",
  "credentials": {
    "mode": "BEARER_REF",
    "credentialKey": "x-prod"
  }
}
```

The provider poll client keeps the existing environment modes and adds:

- `BEARER_REF`: resolves encrypted access token by tenant and credential key.
- `API_KEY_REF`: resolves encrypted API key by tenant and credential key.

Credential views never return raw token, API key, client id, client secret, or refresh token values. Secrets are AES-GCM encrypted through the existing `SecretCipher`; prefix/hash columns support audit and rotation evidence without exposing full values.

## Functional Requirements

1. Credentials must be tenant-scoped by `tenant_id` and `credential_key`.
2. Upsert must normalize credential keys and provider/auth types, encrypt supplied secret values, preserve existing encrypted values when new values are omitted, and write lifecycle events.
3. Views and controller responses must never include raw tokens, API keys, refresh tokens, client ids, or client secrets.
4. Disabled credentials must not resolve for provider poll clients.
5. `BEARER_REF` and `API_KEY_REF` source metadata must resolve through tenant-scoped credential records.
6. OAuth refresh must require refresh endpoint, refresh token, and client id; client secret is optional for public clients.
7. Refresh success must update encrypted access token, optional replacement refresh token, token type, scopes, expiry, status, and audit event.
8. Refresh failure must preserve existing usable credentials, mark refresh evidence, and write a failure event without exposing raw provider response secrets.
9. Query APIs must be tenant-scoped and bounded to 100 rows.
10. Tests must use fake HTTP transport and deterministic cipher keys; no external provider calls or real credentials.

## Out Of Scope

- OAuth authorization URL generation and callback exchange.
- Provider-specific consent wizard UI.
- Secret revocation calls against provider APIs.
- Background scheduler for refresh-due credentials.
- Migrating existing env-only source metadata.

## Acceptance Criteria

- P2-082U docs are indexed after P2-082T.
- Schema tests prove credential and event ledgers include encrypted secret fields, status fields, expiry fields, and tenant indexes.
- Service tests prove create/update/list/disable/refresh behavior, sanitized views, event writes, and failure preservation.
- Provider poll client tests prove `BEARER_REF` and `API_KEY_REF` modes resolve DB credentials and keep response metadata sanitized.
- Controller tests prove tenant/actor propagation for credential APIs.
- Focused backend tests pass with Java 21.

## Delivery Notes

- Delivered tenant-scoped encrypted credential and lifecycle event ledgers.
- Delivered sanitized upsert, list, disable, event query, value resolution, and OAuth refresh handling.
- Delivered `BEARER_REF` and `API_KEY_REF` monitoring source credential modes while keeping `BEARER_ENV` and `API_KEY_ENV`.
- Delivered bounded tenant/operator controller APIs under `/canvas/marketing-monitoring/provider-credentials`.
