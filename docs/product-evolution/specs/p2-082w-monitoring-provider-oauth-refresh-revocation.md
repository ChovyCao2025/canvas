# P2-082W - Monitoring Provider OAuth Refresh And Revocation Spec

Priority: P2
Sequence: 082W
Parent spec: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082w-monitoring-provider-oauth-refresh-revocation-plan.md`

## Implementation Status

Delivered backend first slice. Verification results are recorded in `../plans/p2-082w-monitoring-provider-oauth-refresh-revocation-plan.md`.

## Problem

P2-082V can onboard OAuth authorization-code credentials and P2-082U can manually refresh a single credential. Production monitoring connectors still need an operator and scheduler path that keeps expiring OAuth credentials usable without manual work, plus a provider revocation path that disables credentials only after a sanitized, auditable revoke attempt.

Without this slice:

- OAuth credentials can expire silently unless an operator manually calls refresh.
- Operators can locally disable credentials, but provider-side grants may remain valid.
- Monitoring evidence cannot answer which credentials were refreshed automatically or revoked at the provider.

## Research Notes

- RFC 6749 defines refresh-token grants for obtaining new access tokens.
- RFC 7009 defines token revocation as a form POST with `token` and optional `token_type_hint`.
- RFC 7009 treats unknown or already invalid tokens as a successful 200 response, so local handling should not require leaked provider error bodies for that case.
- Provider docs vary on whether revocation endpoints require public-client `client_id`, confidential-client `client_secret`, or HTTP Basic; this first slice uses the existing form-client-secret pattern already used by refresh/token exchange.

Primary references:

- https://www.rfc-editor.org/rfc/rfc6749
- https://www.rfc-editor.org/rfc/rfc7009

## Scope

Backend first slice:

- Add credential fields for provider revoke endpoint, revoked timestamp, and last revoke status/error.
- Allow OAuth authorization and direct credential upsert to persist a revoke endpoint.
- Add tenant-scoped due-refresh service and API that refresh active OAuth credentials expiring within a bounded window.
- Add disabled-by-default scheduler that invokes due-refresh under the existing warehouse lease pattern.
- Add tenant-scoped revoke service and API that posts a sanitized RFC 7009-style revocation request and disables the local credential after provider success.
- Extend credential lifecycle events with scheduled refresh and revoke outcomes.

## Non-Goals

- Provider-specific revocation variants such as HTTP Basic client auth or nonstandard JSON revoke bodies.
- Frontend credential wizard UI.
- Multi-tenant scheduler fanout. This slice follows the current configured-tenant scheduler pattern.
- Refresh-token expiry prediction beyond storing `refresh_token_expires_at`.

## Data Model

Additive migration extends `marketing_monitor_provider_credential`:

- `revoke_endpoint`
- `revoked_at`
- `last_revoke_status`
- `last_revoke_error`
- index on `(tenant_id, status, expires_at, last_refresh_status)`

The same migration adds `revoke_endpoint` to `marketing_monitor_provider_oauth_authorization` so the authorization callback can persist provider revoke configuration into the credential.

No new ledger table is needed because `marketing_monitor_provider_credential_event` already records lifecycle events.

## API

- `POST /canvas/marketing-monitoring/provider-credentials/refresh-due`
  - Body: optional `windowMinutes`, `limit`.
  - Returns candidate/due/success/failure counts and sanitized credential views.

- `POST /canvas/marketing-monitoring/provider-credentials/{credentialKey}/revoke`
  - Body: optional override `revokeEndpoint`, `tokenTypeHint`, `revokeRefreshToken`, `disableAfterRevoke`, `metadata`.
  - Returns sanitized credential view with local status updated after provider success.

## Acceptance Criteria

- Due refresh only scans the current tenant, active credentials, and a bounded expiry window.
- Due refresh never returns or stores raw provider tokens in result metadata.
- Revoke request body contains token material only on the outbound HTTP request, never in persisted events/views.
- Revoke success writes `REVOKED` lifecycle evidence and disables the local credential by default.
- Revoke failure writes `REVOKE_FAILED` evidence and preserves the existing local token material/status.
- P2-082W docs are indexed after P2-082V.
- Focused backend tests pass with Java 21.
