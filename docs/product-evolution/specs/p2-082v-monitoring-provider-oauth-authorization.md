# P2-082V - Monitoring Provider OAuth Authorization Spec

Priority: P2
Sequence: 082V
Parent: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082v-monitoring-provider-oauth-authorization-plan.md`
Status: Delivered backend first slice

## Goal

Add a production-grade OAuth authorization-code onboarding flow for monitoring provider credentials so operators can generate provider authorization URLs, complete callbacks, and persist encrypted credentials without manually pasting access or refresh tokens.

## Current Baseline

P2-082U added tenant-scoped encrypted credential storage, refresh-token exchange, lifecycle events, and `BEARER_REF`/`API_KEY_REF` provider poll client resolution. It still assumes operators already have tokens. Production OAuth onboarding needs a state ledger, PKCE, callback validation, token exchange, failure audit, and integration with the credential lifecycle service.

## Research Inputs

- OAuth 2.0 authorization-code grant uses `response_type=code`, `client_id`, `redirect_uri`, `scope`, and `state` to bind callback responses to initiation requests:
  - https://www.rfc-editor.org/rfc/rfc6749
- PKCE protects authorization-code exchange by storing a high-entropy `code_verifier` and sending an S256 `code_challenge` in the authorization request:
  - https://www.rfc-editor.org/rfc/rfc7636
- Google OAuth web-server integrations require offline access to receive refresh tokens and use `access_type=offline` and consent prompting when appropriate:
  - https://developers.google.com/identity/protocols/oauth2/web-server
- X OAuth 2.0 documents authorization-code with PKCE and token exchange for user-context access:
  - https://docs.x.com/fundamentals/authentication/oauth-2-0/authorization-code

## Product Design

The backend first slice adds a tenant-scoped OAuth authorization ledger:

- `marketing_monitor_provider_oauth_authorization`
- `marketing_monitor_provider_oauth_authorization_event`

Operators start authorization by posting provider endpoints, credential key, client id/secret, redirect URI, scopes, and optional provider-specific authorization params such as `access_type=offline` or `prompt=consent`. The service generates:

- opaque random `state`
- PKCE `code_verifier`
- S256 `code_challenge`
- bounded expiration timestamp
- sanitized authorization URL

The callback endpoint accepts `state`, `code`, and provider error fields. On success it exchanges the code with the provider token endpoint, writes or updates a `marketing_monitor_provider_credential` through P2-082U, and records authorization events. On provider error, expiry, duplicate callback, or token exchange failure, it marks the authorization failed without exposing tokens, code verifier, client secret, or raw provider secret payloads.

## Functional Requirements

1. Authorization state must be tenant-scoped, single-use, high entropy, and expire within a bounded window.
2. Start authorization must encrypt client id, client secret, and PKCE verifier at rest.
3. Authorization URLs must include `response_type=code`, `client_id`, `redirect_uri`, `scope`, `state`, `code_challenge`, and `code_challenge_method=S256`.
4. Extra authorization params must be allowlisted against reserved OAuth keys so operators cannot override generated `state`, `client_id`, `redirect_uri`, `response_type`, `code_challenge`, or `code_challenge_method`.
5. Callback success must exchange the authorization code with `grant_type=authorization_code`, `code`, `redirect_uri`, `client_id`, `code_verifier`, and optional `client_secret`.
6. Token exchange must parse access token, optional refresh token, token type, scopes, and expiry, then upsert encrypted provider credentials through P2-082U.
7. Provider callback errors, expired state, duplicate callbacks, and token HTTP/JSON failures must be recorded as failed authorization events without writing credentials.
8. Views and controller responses must never include raw authorization code, access token, refresh token, code verifier, client id, or client secret.
9. Query APIs must be tenant-scoped and bounded to 100 rows.
10. Tests must use fake HTTP transport and deterministic cipher keys; no external provider calls or real credentials.

## Out Of Scope

- Frontend provider wizard UI.
- Provider-specific discovery documents or SDKs.
- Public unauthenticated callback route.
- Token revocation calls.
- Scheduled refresh; P2-082U manual refresh remains the credential lifecycle mechanism.

## Acceptance Criteria

- P2-082V docs are indexed after P2-082U.
- Schema tests prove authorization and event ledgers include tenant, state, encrypted secret, callback, token endpoint, status, expiry, credential id, and event fields.
- Service tests prove authorization URL creation, encrypted state storage, token exchange success, credential upsert, provider-error failure, expired-state failure, and sanitized views.
- Controller tests prove tenant/actor propagation for start, callback, list, and event query APIs.
- Focused backend tests pass with Java 21.

## Delivery Notes

- Delivered tenant-scoped OAuth authorization and authorization event ledgers.
- Delivered PKCE S256 state generation, encrypted verifier/client secret storage, authorization URL creation, and reserved-param protection.
- Delivered authorization-code callback exchange into the P2-082U encrypted credential lifecycle service.
- Delivered sanitized failure handling for provider callback errors, expired state, duplicate callback, token HTTP failure, and malformed token responses.
- Delivered bounded tenant/operator APIs under `/canvas/marketing-monitoring/provider-credentials/oauth`.
