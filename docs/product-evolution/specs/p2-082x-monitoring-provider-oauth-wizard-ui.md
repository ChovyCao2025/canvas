# P2-082X - Monitoring Provider OAuth Wizard UI Spec

Parent spec: `p2-082-marketing-platform-gap-closure.md`
Implementation plan: `../plans/p2-082x-monitoring-provider-oauth-wizard-ui-plan.md`
Status: Delivered frontend first slice

## Problem

P2-082U through P2-082W delivered the backend credential lifecycle: encrypted provider credentials, OAuth authorization-code onboarding, refresh, scheduled refresh, and revocation. Operators still cannot use that lifecycle from the monitoring workbench. The remaining workflow requires direct API calls, which is not production-grade for marketing operations.

## Research Notes

- RFC 6749 defines the authorization-code flow around authorization request, callback response, and token exchange.
- RFC 7636 defines PKCE, which the backend already generates and persists for authorization attempts.
- RFC 7009 defines provider token revocation as a separate endpoint flow.
- Frontend responsibilities should be limited to collecting provider/client configuration, opening the provider authorization URL, and completing callback metadata; raw provider tokens must remain backend-only.

Primary references:

- https://www.rfc-editor.org/rfc/rfc6749
- https://www.rfc-editor.org/rfc/rfc7636
- https://www.rfc-editor.org/rfc/rfc7009

## Scope

Frontend first slice:

- Add typed frontend API methods for provider credentials, OAuth authorizations, callbacks, refresh, due-refresh, revoke, disable, and recent events.
- Add a monitoring workbench credential panel for:
  - listing sanitized credentials,
  - starting OAuth authorization,
  - opening/copying the authorization URL,
  - completing an authorization callback with state/code or provider error,
  - refreshing, refreshing due credentials, revoking, and disabling credentials,
  - viewing recent authorization attempts and lifecycle events.
- Add presentation helpers for credential status, auth status, secret-safe form parsing, and default redirect URI generation.
- Keep token/secret values write-only; never render access tokens, refresh tokens, client secret, or API keys from backend state.

## Non-Goals

- Provider-specific preset catalog beyond generic form defaults.
- Browser popup callback automation that requires provider-specific redirect configuration.
- Persisting OAuth client secrets in local storage.
- Backend behavior changes beyond using existing endpoints.

## Acceptance Criteria

- Monitoring page initial load includes credential, authorization, and credential-event data without blocking existing mentions/alerts/trends.
- Operators can start an OAuth authorization from the workbench and open the returned authorization URL.
- Operators can complete a callback and see sanitized authorization status updates.
- Operators can refresh one credential, refresh due credentials, revoke a credential, disable a credential, and reload events.
- Frontend tests cover API endpoints and the core wizard/operations path.
- P2-082X docs are indexed after P2-082W.
