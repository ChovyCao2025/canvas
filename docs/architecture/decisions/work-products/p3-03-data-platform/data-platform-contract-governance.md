# Data Platform Contract Governance

Date: 2026-06-05

Status: P3-03 governance contract for the audience compute history proof of concept.

## Governance Rules

| Rule | Requirement |
|---|---|
| schema versioning | Every source extract and event envelope has a schema owner and semantic version. Backward-compatible fields may be added; breaking changes require a new version and compatibility window. |
| ownership | Source tables remain owned by their bounded context. Data Platform / Analytics owns only derived models and warehouse jobs. |
| compatibility | A source field used by the POC cannot be removed, renamed, or change meaning without a deprecation window and contract test update. |
| replay | Replaying the same source range must be idempotent and produce the same serving keys. |
| ordering | Table polling orders by source watermark plus primary key. Events order by partition key and occurred-at timestamp. |
| late-arriving data | Late rows are accepted inside the retention window and update the serving model by idempotent key. |
| backfill | Backfills run by bounded source ranges, record row counts, and write evidence before replacing serving data. |
| deletion propagation | Source deletion or erasure must remove or anonymize derived rows according to compliance rules. |
| lineage | Every serving row records source table family, source primary key or aggregate key, source watermark, transform version, and build run ID. |

## Selected Slice Contracts

### Source Table Contract

| Source | Required fields | Owner | Compatibility rule |
|---|---|---|---|
| `audience_compute_run` | `id`, `audience_id`, `perf_run_id`, `perf_input_id`, `status`, `estimated_size`, `bitmap_size_kb`, `error_msg`, `created_at`, `updated_at` | CDP / Audience | Fields cannot be removed or narrowed while the POC is active. |
| `audience_definition` | `id`, `name`, `engine_type`, `data_source_type`, `evaluation_strategy`, `enabled`, `updated_at` | CDP / Audience | Rule JSON is not copied into the POC serving model unless masked and approved. |
| `audience_stat` | `audience_id`, `estimated_size`, `bitmap_size_kb`, `computed_at`, `status`, `error_msg` | CDP / Audience | Used only as aggregate status supplement. |
| `event_log` | `id`, `event_code`, `user_id`, `perf_run_id`, `canvas_triggered`, `canvas_count`, `created_at` | CDP / Audience | Raw attributes are excluded from POC serving model. |
| `canvas_execution` | `id`, `tenant_id`, `canvas_id`, `user_id`, `status`, `perf_run_id`, `created_at`, `updated_at` | Execution Runtime | Only aggregate counts by status/perf run are allowed in the POC model. |

## CDC Example

CDC is optional and requires a later ADR. If adopted for the slice, the first table-change envelope is:

```json
{
  "eventName": "AudienceComputeRunChanged",
  "schemaVersion": "1.0.0",
  "eventId": "evt_20260605_000001",
  "tenantId": 42,
  "sourceTable": "audience_compute_run",
  "sourcePrimaryKey": "10001",
  "operation": "UPSERT",
  "occurredAt": "2026-06-05T05:00:00Z",
  "idempotencyKey": "audience_compute_run:10001:2026-06-05T05:00:00Z",
  "traceId": "trace-1",
  "payload": {
    "audienceId": 7,
    "perfRunId": "perf_20260605_001",
    "status": "READY",
    "estimatedSize": 1200,
    "bitmapSizeKb": 64,
    "updatedAt": "2026-06-05T05:00:00Z"
  }
}
```

## Event Contract Example

The domain event equivalent is:

```json
{
  "eventName": "AudienceComputeRunCompleted",
  "schemaVersion": "1.0.0",
  "eventId": "evt_20260605_000002",
  "tenantId": 42,
  "audienceId": 7,
  "computeRunId": 10001,
  "status": "READY",
  "perfRunId": "perf_20260605_001",
  "occurredAt": "2026-06-05T05:00:00Z",
  "idempotencyKey": "audience-compute:10001:READY",
  "correlationId": "trace-1"
}
```

## PII And Retention Links

PII handling follows `docs/architecture/evidence/compliance/data-inventory.md`:

- CDP profile and identity fields are PII / confidential.
- Message send payloads are PII / compliance evidence.
- Execution trace input/output may contain PII.
- The POC serving model must not copy raw user identifiers, phone, email, open ID, raw event attributes, trace payloads, or message payloads.

Retention follows `docs/architecture/evidence/capacity/retention-policy.md`:

- `event_log` is 30 days online, then archived or deleted.
- `canvas_execution` is 180 days online.
- `canvas_execution_trace` is 30 days online and excluded from the first POC.
- Derived POC rows must carry source watermarks so retention and deletion can be replayed.

## Backfill And Replay Procedure

1. Select source range by `updated_at` or `created_at`.
2. Record source row count and checksum by source group.
3. Build derived rows into a staging target keyed by `(audience_id, compute_run_id)`.
4. Compare staging row count and checksum against previous run.
5. Swap or upsert to serving target.
6. Store build run ID, transform version, source watermark, dropped records, duration, and operator.
7. Re-run the same range to prove replay idempotency.

## Deletion Propagation

Deletion propagation must be testable before production use:

- if a source row is hard-deleted, the derived row is removed or tombstoned on the next build;
- if user erasure affects a source group, the derived model must not retain raw user identifiers;
- if aggregate rows cannot be traced to user identity, document why aggregate retention is allowed;
- legal hold overrides deletion only with compliance owner approval.

## Lineage Fields For Serving Rows

Every serving row should include:

- `build_run_id`;
- `transform_version`;
- `source_context`;
- `source_table_family`;
- `source_primary_key` or aggregate key;
- `source_watermark`;
- `tenant_id` or explicit system context;
- `created_at`;
- `updated_at`.
