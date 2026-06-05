# WeCom SCRM Integration Boundary

Date: 2026-06-05

Status: P3-08 boundary definition. WeCom remains inside the Integration bounded context in the modular monolith.

## Boundary Principle

WeCom-specific code must stay in external adapters and Integration domain services. Generic DAG execution, node handlers, delivery outbox, CDP identity, and frontend shared API code must depend on stable contracts, not raw WeCom SDK/client types.

## Adapter Interfaces

| adapter | Responsibility | Must not expose |
|---|---|---|
| `WeComClient` | Calls WeCom APIs for health checks, token validation, callback acknowledgement, future customer sync, and future send operations. | Raw credentials, SDK objects, HTTP client implementation. |
| `WeComCallbackVerifier` | Verifies callback signature, timestamp, nonce, and payload integrity. | Secret material or decrypted credential values. |
| `WeComCredentialStore` | Reads encrypted credential references and resolves secret material at the adapter edge. | Plain secrets to controllers or handlers. |
| `WeComEventMapper` | Maps provider event payloads into local event envelopes. | Provider payloads outside the Integration package. |
| `ChannelConnector` adapter | Bridges WeCom provider mode, health, send, and receipt behavior to the existing generic channel connector contract. | WeCom-only request fields in generic handler signatures. |

## Domain Services

| domain service | Responsibility |
|---|---|
| `WeComCallbackIngestionService` | Validate, persist, classify, and enqueue callback events. |
| `WeComConnectorService` | Manage provider metadata, mode, health, and tenant ownership. |
| `WeComEventProcessingService` | Convert accepted callback events into Integration-owned domain events or future read models. |
| `WeComReconciliationService` | Compare callback ledger, DLQ, provider API state, and downstream processing state. |

## Handler Contract

The generic DAG handler contract stays channel-based:

- handler input: tenant ID, channel, provider, user or target reference, template/message payload, idempotency key;
- handler output: accepted, skipped, failed, external message ID, status, reason;
- no handler import of `WeComClient`, WeCom SDK objects, WeCom token classes, or callback payload classes;
- WeCom-specific validation lives in the channel adapter and domain service;
- future journey send node uses `ChannelConnector.send(...)` or a delivery command port.

This is how generic DAG handlers avoid direct WeCom client dependencies.

## Callback Endpoint Contract

| Field | Contract |
|---|---|
| Route | `POST /integrations/wecom/callback/{connectorKey}` |
| Lookup | `connectorKey` maps to tenant, provider, mode, and credential reference. |
| Headers/query | Provider timestamp, nonce, signature, message signature, or equivalent WeCom callback fields. |
| Body | Raw body is verified before JSON/XML binding. |
| Response | Ack only after event metadata is durably recorded or duplicate is safely recognized. |
| Errors | Signature failure returns unauthorized; replay returns idempotent duplicate or rejected status; malformed payload returns bad request; retryable processing failure is recorded for retry/DLQ. |

## Frontend API Contract

The frontend API must expose:

- connector list with WeCom row, mode, health status, and redacted metadata;
- mode update request with reason;
- health test response;
- callback event list with event type, processing status, retry count, DLQ status, received time, and payload hash;
- disabled state when product/compliance/secret gates are missing;
- no raw credentials, callback token, encoding AES key, or provider secret.

## Data Ownership

Integration owns:

- connector metadata and credential references;
- callback event ledger;
- replay guard records or Redis keys;
- DLQ rows for callback processing failures;
- reconciliation records for callback/provider drift.

CDP owns identity merge, OneID, customer profile enrichment, deletion propagation, and visibility rules. Reach/Notification owns marketing send policy and delivery reporting. WeCom must not take ownership of CDP or Reach tables without a new ADR.

## Signature Verification

Signature verification requires:

- raw request body preserved before parsing;
- connector-specific credential lookup;
- timestamp skew window;
- nonce validation;
- message signature validation;
- constant-time comparison where applicable;
- audit-safe failure logging without secret or raw payload leakage.

## Replay Protection

Replay protection uses:

- provider event ID when available;
- otherwise `tenantId + connectorKey + timestamp + nonce + payloadHash`;
- TTL aligned with provider retry window;
- duplicate behavior that returns a safe ack only if the previous event was durably recorded.

## Idempotency Key

The idempotency key format is:

```text
wecom:callback:<tenantId>:<connectorKey>:<eventType>:<providerEventId-or-payloadHash>
```

Every downstream event or command created from the callback carries this key.

## Retry Classification

| Class | Examples | Behavior |
|---|---|---|
| duplicate | Same idempotency key already processed or recorded. | Ack/no-op. |
| retryable | Temporary DB, Redis, MQ, or downstream service failure after signature passes. | Persist pending/retry state and retry with backoff. |
| non-retryable | Signature failure, expired timestamp, malformed payload, unknown connector. | Reject or mark ignored with audit-safe reason. |
| dead-letter | Retry budget exceeded or payload cannot map to supported event. | Move to DLQ and require manual review. |

## DLQ

DLQ records must include tenant ID, connector key, event type, idempotency key, error class, retry count, payload hash, received time, and redacted diagnostic message. Raw sensitive payload storage requires compliance approval.

## Reconciliation

The reconciliation command must compare:

- callback event ledger;
- DLQ records;
- downstream event/processing status;
- provider-side state where API access exists;
- duplicate/replay guard records.

Candidate command:

```bash
POST /ops/integrations/wecom/reconcile?tenantId=<tenant>&connectorKey=<key>&since=<timestamp>
```

The command is operator-only and must produce evidence without printing secrets.
