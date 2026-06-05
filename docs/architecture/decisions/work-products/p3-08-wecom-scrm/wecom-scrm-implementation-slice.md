# WeCom SCRM Implementation Slice

Date: 2026-06-05

Status: Deferred for implementation. First candidate slice is defined, but product, compliance, sandbox tenant, and secret-ownership gates are not yet closed.

## Candidate Capabilities

| Capability | Value | Risk | Decision |
|---|---|---|---|
| Callback ingestion | Establishes secure WeCom event intake and event ledger. | Signature, replay, idempotency, tenant routing, PII handling. | First candidate slice after gates. |
| Customer sync | Imports external contacts and maps them to CDP identity. | PII, deletion, merge/split, quota, rate limits. | Deferred. |
| Group operations | Syncs group chats and members, supports group operations. | Group-member PII, operator permissions, external API quota. | Deferred. |
| Journey send node | Sends WeCom messages from canvas execution. | Generic DAG coupling, consent, rate limits, duplicate sends. | Deferred until callback and connector contracts exist. |
| Session tracking | Tracks customer sessions and interactions. | Consent, retention, message content PII, compliance evidence. | Deferred. |
| Frontend configuration | Lets operators configure connector mode and health checks. | Secret handling, disabled state, tenant-specific validation. | Covered by first candidate slice through existing channel connector surface. |

## Scope

The first candidate slice is WeCom connector configuration plus Callback ingestion.

In scope:

- register a WeCom provider as an Integration context adapter inside the modular monolith;
- extend the existing channel connector model only where needed for WeCom provider metadata;
- define a callback endpoint contract with signature verification and replay protection;
- persist callback event metadata in a tenant-scoped event ledger;
- classify retry, DLQ, and reconciliation behavior;
- expose frontend configuration state through typed APIs without exposing secrets;
- keep all WeCom client calls inside adapter packages.

## Out of scope

- full customer sync;
- group operations;
- journey send node implementation;
- session tracking;
- moments or social posting;
- CDP OneID merge/split logic;
- physical WeCom service extraction;
- direct WeCom client dependencies inside generic DAG handlers;
- committing sandbox or production secrets.

## API

Candidate backend API contracts:

| API | Purpose | Auth |
|---|---|---|
| `GET /channels/connectors` | Reuse existing connector list for `channel=wecom`. | JWT and tenant context. |
| `POST /channels/connectors/{id}/mode` | Reuse existing connector mode control. | JWT and tenant context. |
| `POST /channels/connectors/{id}/health-test` | Reuse existing health test surface; adapter returns sandbox/disabled/real state. | JWT and tenant context. |
| `POST /integrations/wecom/callback/{connectorKey}` | WeCom callback endpoint for events. | WeCom signature plus tenant/connector lookup. |
| `GET /integrations/wecom/callback-events` | Operator/event review endpoint for latest ingested events. | JWT and tenant context. |

Candidate frontend API contracts:

- typed connector row with `channel`, `provider`, `mode`, `healthStatus`, and redacted metadata;
- callback status row with event type, received time, processing status, retry status, and DLQ marker;
- no secret value in any response.

## Data

Integration owns:

- `channel_connector` rows for WeCom provider registration and mode;
- encrypted credential references or secret IDs, not raw secrets;
- `wecom_callback_event` or equivalent event ledger with tenant ID, connector key, event ID, event type, received time, payload hash, status, retry count, and DLQ marker;
- `wecom_callback_replay_guard` or Redis key namespace for callback replay protection.

CDP/customer ownership is deferred. WeCom external user IDs must not be silently merged into CDP profiles until P3-09 identity rules are accepted.

## Consent

No journey send node, bulk customer sync, session content, or marketing action is allowed in this slice. Any future send/sync slice must check:

- marketing consent;
- suppression state;
- contactability;
- tenant-scoped connector mode;
- provider rate limits;
- user deletion and retention policy.

## Compliance

Compliance gates before implementation:

- product owner names the WeCom business purpose;
- compliance owner approves PII fields and retention;
- security owner approves credential storage and callback signature policy;
- operations owner approves DLQ and reconciliation handling;
- sandbox tenant credentials are stored outside git;
- evidence path is defined for callback test payloads without storing raw sensitive payloads.

## Callback

The first candidate slice accepts callbacks only after:

- connector key resolves to one tenant or explicit system scope;
- signature verification passes;
- timestamp skew is within the accepted window;
- nonce or event ID replay protection passes;
- idempotency key is calculated;
- event metadata is persisted before processing;
- failed processing is classified as retryable, dead-letter, or ignored duplicate.

## Rollback

Rollback for the first candidate slice:

- disable the WeCom connector mode;
- disable the callback route at ingress or application config;
- stop callback processing but keep the event ledger for audit;
- replay only reviewed events after fix;
- remove frontend entry point if the API contract changes;
- do not delete callback evidence until compliance owner approves retention handling.

## Owner

Implementation remains Deferred until these owners are named:

- product owner for WeCom business scope;
- Integration owner for adapter and callback endpoint;
- security owner for signature and secret handling;
- compliance owner for PII, consent, and retention;
- operations owner for DLQ, reconciliation, and provider health.
