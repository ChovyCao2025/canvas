# WeCom SCRM Test Plan

Date: 2026-06-05

Status: P3-08 test plan. Tests must be implemented before production WeCom functionality is enabled.

## Backend Tests

| Test | Required assertion |
|---|---|
| callback signature valid | Valid timestamp, nonce, signature, and body are accepted. |
| callback signature invalid | Invalid signature is rejected without persisting a successful event. |
| replay protection | Same timestamp/nonce/payload or provider event ID is classified as duplicate. |
| duplicate event idempotency | Same idempotency key does not create duplicate downstream commands. |
| retry classification | Temporary DB/Redis/MQ failure is marked retryable with retry count and next attempt. |
| adapter failure | WeCom client timeout or provider error maps to a typed adapter failure. |
| DLQ | Retry budget exhaustion writes a DLQ record with tenant, connector, event type, idempotency key, and payload hash. |
| handler output | Future channel send handler returns accepted, skipped, failed, external message ID, status, and reason without WeCom SDK types. |
| tenant isolation | Connector key cannot route callback into another tenant. |
| secret masking | Logs and API responses do not expose token, secret, encoding AES key, or raw credential values. |

## Frontend Tests

| Test | Required assertion |
|---|---|
| config form payload | Connector mode, provider, reason, and metadata payload match typed API contract. |
| validation | Missing required connector fields and invalid mode are blocked. |
| disabled state | WeCom configuration and callback view show disabled state when product/compliance/secret gates are not met. |
| typed API response | Connector rows, health result, callback event rows, retry state, and DLQ marker parse through TypeScript types. |
| secret redaction | UI never renders secret values returned from backend. |

## Manual Sandbox Verification

Manual verification requires a sandbox WeCom tenant and credentials, but secrets must not be committed.

Required steps:

1. Store sandbox corp ID, agent ID, secret, token, and encoding AES key in the approved secret store or local env.
2. Register callback URL for a staging or local tunnel endpoint.
3. Send WeCom verification callback and one event callback.
4. Confirm signature verification passes.
5. Confirm replay of the same callback is rejected or idempotently acknowledged.
6. Confirm callback ledger row is tenant-scoped and payload is redacted or hashed.
7. Force adapter failure and confirm retry/DLQ behavior.
8. Run reconciliation command and save redacted evidence.

## Evidence Rules

- Store only redacted payloads, payload hashes, event IDs, status, timestamps, and command output.
- Do not store sandbox secrets in git, screenshots, logs, issue comments, or evidence files.
- Raw payload capture requires compliance owner approval and expiration date.
- Evidence path: `docs/architecture/evidence/wecom/<yyyy-mm-dd>-sandbox-callback.md`.

## Exit Criteria

- Backend callback signature, replay, idempotency, retry, adapter failure, DLQ, and handler output tests pass.
- Frontend form payload, validation, disabled state, and typed API response tests pass.
- Sandbox manual verification passes without committed secrets.
- Product, security, compliance, Integration, and operations owners sign off.
