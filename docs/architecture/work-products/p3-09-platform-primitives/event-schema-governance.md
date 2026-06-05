# Event Schema Governance

Date: 2026-06-05

Status: P3-09 event contract. Versioned events are required before service extraction or data-platform pipelines depend on cross-boundary events.

## Schema Owner

Every event has a schema owner. The owner is accountable for schema review, compatibility, documentation, tests, retention, and deprecation.

Required owner fields:

- owner context;
- owning team;
- event family;
- schema registry path;
- reviewer list;
- compatibility window;
- rollback owner.

## Versioning

versioning rules:

- `schemaVersion` uses semantic versioning.
- Adding optional fields is backward compatible.
- Renaming, deleting, changing type, or changing meaning is breaking.
- Breaking changes require a new schema version and dual-publish or adapter window.
- Consumers must declare supported versions.

## Compatibility

compatibility rules:

- producers must validate event envelope before publish;
- consumers must ignore unknown optional fields;
- consumers must reject unsupported major versions with DLQ or explicit skip;
- compatibility tests must include oldest supported and newest supported versions;
- frontend or BI consumers must not consume raw internal events without a serving contract.

## Replay

replay rules:

- every event has an idempotency key;
- replay must not create duplicate side effects;
- replay command records reason, actor, time range, and target event family;
- replay uses the same schemaVersion or a documented upcaster;
- replay evidence records count read, count published, count skipped, count failed, and DLQ count.

## Ordering

ordering rules:

- each event family defines its ordering key;
- ordering key examples: `tenantId + canvasId`, `tenantId + executionId`, `tenantId + oneId`, `tenantId + deliveryId`;
- consumers must tolerate duplicate and late events unless the family explicitly requires strict ordering;
- state transition events carry previous and next state.

## Idempotency

idempotency rules:

- every event has `eventId` and `idempotencyKey`;
- `eventId` is unique for one publish attempt;
- `idempotencyKey` is stable across retries of the same logical event;
- duplicate behavior is documented per consumer;
- idempotency key TTL or retention owner is defined.

## Deprecation

deprecation rules:

- mark schema version deprecated before removing producer support;
- name affected consumers;
- provide migration path and test command;
- keep deprecated schema through the compatibility window;
- publish removal date and rollback trigger.

## Retention

retention rules:

- event ledger retention follows PII class and business need;
- DLQ retention follows incident/replay policy;
- data-platform retained aggregates must document whether source event deletion propagates;
- sensitive payloads are redacted, hashed, encrypted, or omitted;
- retention owner is named per event family.

## Initial Event Families

| Family | schema owner | ordering key | Examples |
|---|---|---|---|
| canvas lifecycle | Canvas Authoring | `tenantId + canvasId` | `CanvasCreated`, `CanvasPublished`, `CanvasArchived` |
| execution lifecycle | Execution Runtime | `tenantId + executionId` | `ExecutionRequested`, `ExecutionStarted`, `ExecutionCompleted`, `ExecutionFailed` |
| customer identity | CDP / Audience | `tenantId + oneId` | `SourceIdentityLinked`, `OneIdMerged`, `OneIdSplit`, `IdentityConflictOpened` |
| reach delivery | Reach / Notification | `tenantId + deliveryId` | `DeliveryRequested`, `DeliverySent`, `DeliveryFailed`, `ReceiptReceived` |
| CDP profile | CDP / Audience | `tenantId + userId` or `tenantId + oneId` | `CdpProfileUpdated`, `CdpTagChanged`, `CdpConsentChanged` |
| ops | Platform/Ops | `tenantId + operationId` or `system + operationId` | `AuditEventRecorded`, `ConfigChanged`, `TenantQuotaChanged`, `SystemAlertRequested` |

## Example Event Envelope

```json
{
  "eventId": "evt_01JZ0000000000000000000001",
  "eventType": "ExecutionCompleted",
  "eventFamily": "execution lifecycle",
  "schemaVersion": "1.0.0",
  "tenantId": 42,
  "producer": "canvas-engine",
  "ownerContext": "Execution Runtime",
  "correlationId": "trace-20260605-0001",
  "causationId": "cmd_01JZ0000000000000000000001",
  "idempotencyKey": "execution:42:9001:completed:v1",
  "occurredAt": "2026-06-05T08:30:00Z",
  "subject": {
    "type": "execution",
    "id": "9001"
  },
  "payload": {
    "canvasId": 1001,
    "status": "COMPLETED"
  }
}
```

## Contract Tests

- envelope validation for required fields;
- producer test for `schemaVersion`, `eventId`, tenant, correlation, idempotency key, and occurred-at timestamp;
- consumer test for unknown optional fields;
- replay test for duplicate idempotency key;
- ordering test for same ordering key;
- deprecation test for oldest supported version.
