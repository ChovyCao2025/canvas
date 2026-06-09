# P2-021 - CDP OLAP Audience Materialization Spec

Priority: P2
Sequence: 021
Source: `docs/optimization/todo/specs/2026-05-30-data-infrastructure-spec.md`, `docs/optimization/todo/specs/2026-05-30-audience-streaming-load-spec.md`, `docs/optimization/todo/specs/2026-05-30-roaringbitmap-collision-fix-spec.md`, `docs/product-evolution/specs/p1-006c-realtime-audiences-overlap-and-snapshots.md`
Implementation plan: `../plans/p2-021-cdp-olap-audience-materialization-plan.md`

Status: Historical plan evidence records implementation and verification; historical RED-state checks plus commit and merge status were not verified in this docs-only audit.

## Goal

Build the first production-oriented bridge between the current CDP, Doris OLAP, and audience segmentation: CDP events become OLAP facts, behavior-based audience rules materialize into versioned online bitmaps, and runtime TAGGER membership stays fast and deterministic.

## Current Baseline

- `CdpEventIngestionService` accepts tenant-scoped CDP track events, stores them in `cdp_event_log`, discovers attributes, and publishes internal RocketMQ events.
- Doris is optional and currently focused on execution traces and daily canvas stats.
- `AudienceBatchComputeService` computes offline audiences from JDBC, `TAGGER_API`, `CDP_TAG`, `CDP_PROFILE`, and `CDP_IDENTITY`.
- `AudienceBitmapStore` stores Redis RoaringBitmap payloads with `murmur3_32` user ID hashes, which creates collision risk for large audiences.
- `TaggerHandler` already uses bitmap membership for online audience branching.
- P1-006C introduces realtime audience event logs and snapshots but does not define a production OLAP materialization path.

## In Scope

- Stable tenant-scoped user index mapping for audience bitmap membership.
- Versioned audience bitmap metadata so materialization can publish a new ready version without overwriting the active one mid-run.
- Doris DDL for CDP behavior facts and user-level event aggregates.
- A bounded behavior-audience rule contract for first-slice segmentation.
- `AudienceMaterializationService` that compiles approved behavior rules, queries an OLAP repository boundary, writes a versioned bitmap, and records a run ledger.
- Concrete MyBatis and Doris adapters for enabled audience definitions and bounded behavior-audience OLAP queries.
- Data quality checks for row count parity, freshness lag, and bitmap size drift.
- Tests proving safe rule compilation, deterministic user indexing, versioned bitmap publishing, and quality verdicts.

## Out Of Scope

- Full Flink CDC or Flink SQL deployment.
- Replacing runtime TAGGER membership with live Doris queries.
- Arbitrary self-service SQL from operators.
- Full cohort, retention, attribution, and experimentation analytics.
- Large-scale bitmap migration of every existing audience; this spec creates a compatible path for new materialized audiences and follow-up migration.

## Architecture

OLTP CDP remains the authoritative ingestion and audit surface. Doris stores behavior facts and windowed aggregates for analytical computation. Audience materialization reads bounded OLAP outputs and writes versioned online membership artifacts.

Runtime execution does not synchronously query Doris. TAGGER continues to use bitmap or locked snapshot membership, which keeps canvas execution isolated from analytical query latency and OLAP outages.

```text
CDP track / delivery receipt / conversion / execution trace
        |
        v
MySQL audit tables + RocketMQ internal events
        |
        v
Doris ODS/DWD/DWS behavior facts
        |
        v
AudienceMaterializationService
        |
        +--> stable user index
        +--> versioned Redis bitmap
        +--> materialization run ledger
        +--> quality check ledger
        |
        v
TAGGER membership / snapshots / analytics UI / alerts
```

## Behavior Audience Rule Contract

The first production slice supports an explicit JSON contract instead of arbitrary SQL:

```json
{
  "source": "CDP_EVENT_METRIC",
  "eventCode": "OrderPaid",
  "windowDays": 30,
  "metric": "COUNT",
  "operator": ">=",
  "value": 2,
  "filters": [
    { "field": "channel", "operator": "=", "value": "SMS" }
  ]
}
```

Supported metrics:

- `COUNT`
- `SUM_PROPERTY`
- `MAX_PROPERTY`
- `LAST_SEEN_DAYS_AGO`

Supported operators:

- `=`
- `!=`
- `>`
- `>=`
- `<`
- `<=`

Supported filters:

- Dot-separated JSON property paths.
- Scalar string, number, and boolean values.
- A hard maximum of 10 filters per rule.

All compiled queries must include tenant, event code, bounded time window, row limit, and generated parameters. Operators cannot provide raw SQL.

## Data Model

### MySQL Metadata

- `cdp_user_index`
  - Tenant-scoped deterministic mapping from business `user_id` to numeric `user_index`.
  - Eliminates hash collision risk for newly materialized audiences.
- `audience_materialization_run`
  - Tracks audience id, version, status, counts, error, freshness, and operator.
- `audience_bitmap_version`
  - Tracks active and historical bitmap keys, estimated size, creation source, and version status.
- `audience_quality_check`
  - Tracks row parity, freshness lag, bitmap drift, verdict, and detail JSON.

### Doris

- `canvas_ods.cdp_event_log`
  - Raw accepted CDP events mirrored from MySQL or internal events.
- `canvas_dwd.cdp_user_event_fact`
  - Tenant/user/event normalized facts with key fields extracted for segmentation.
- `canvas_dws.user_event_metric_daily`
  - User-level daily event aggregates for bounded behavior audience rules.

## Runtime Semantics

1. CDP event ingestion writes MySQL first and publishes the internal event as it does today.
2. A future consumer or backfill job writes the event into Doris ODS/DWD.
3. Materialization evaluates only approved behavior rules against the OLAP repository boundary.
4. User IDs returned from OLAP are mapped through `cdp_user_index`.
5. A new bitmap version is written under a versioned Redis key.
6. The version is marked ready after write completion and quality checks.
7. Runtime TAGGER reads the latest ready version or the publish-time locked snapshot.

## Production Controls

- Materialization must fail closed when the rule has no tenant, no event code, no bounded window, unsupported operator, or invalid filter path.
- OLAP query timeout and max rows are configured and enforced before bitmap write.
- Bitmap publication is two-phase: write versioned key first, then mark version as ready.
- Runtime membership can use the latest ready bitmap; incomplete versions are never active.
- Quality checks record lag and count drift but do not block the first implementation unless configured as strict.
- Rollback disables materialization and keeps existing audience bitmap behavior.

## Functional Requirements

1. Stable user indexing must allocate one numeric `user_index` per `(tenant_id, user_id)` and reuse it on repeated requests.
2. Behavior rule compilation must reject arbitrary SQL and unsupported fields.
3. Materialization must query only through a bounded OLAP repository interface.
4. Materialization must save a versioned bitmap and record the run status.
5. Materialization failures must record `FAILED` run status with a bounded error message.
6. Quality checks must return `PASS`, `WARN`, or `FAIL` based on freshness and drift thresholds.
7. Existing non-materialized audience runtime behavior must remain compatible.

## Technical Scope

- `backend/canvas-engine/src/main/resources/db/migration/V214__cdp_olap_audience_materialization.sql`
- `backend/canvas-engine/src/main/resources/infrastructure/doris/cdp-audience-ddl.sql`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpUserIndexDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceBitmapVersionDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceMaterializationRunDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceQualityCheckDO.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpUserIndexMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceBitmapVersionMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceMaterializationRunMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/AudienceQualityCheckMapper.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/StableUserIndexService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/VersionedAudienceBitmapStore.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/BehaviorAudienceRuleCompiler.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceMaterializationService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AudienceQualityService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/MyBatisAudienceDefinitionRepository.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/DorisBehaviorAudienceOlapRepository.java`

## Acceptance Criteria

- Schema tests prove MySQL metadata and Doris DDL contain the required tables and indexes.
- Unit tests prove stable user index reuse and duplicate-safe allocation.
- Unit tests prove behavior rule compilation rejects unsupported source, operator, filter paths, missing tenant, and unbounded windows.
- Unit tests prove materialization writes a ready versioned bitmap and records a successful run.
- Unit tests prove materialization records failed runs without publishing incomplete bitmaps.
- Unit tests prove quality verdicts for freshness lag and bitmap drift.
- Unit tests prove the Doris behavior-audience repository fails closed when Doris is disabled and generates bounded parameterized SQL for behavior rules.
- Unit tests prove the MyBatis audience definition repository loads only tenant-scoped enabled definitions.
- Focused backend test command passes for all new P2-021 tests.

## Rollout

1. Deploy metadata migration and Doris DDL.
2. Enable OLAP event mirroring in staging.
3. Materialize one small behavior audience with strict row limit.
4. Compare MySQL event counts, Doris ODS counts, returned user count, and bitmap cardinality.
5. Enable materialized audiences for operator-created behavior rules.

## Rollback

- Disable the materialization scheduler or API entry point.
- Keep existing audience bitmaps and TAGGER behavior.
- Ignore `audience_bitmap_version` rows newer than the rollback timestamp.
- Doris ODS/DWD/DWS tables are additive and can remain for audit.
