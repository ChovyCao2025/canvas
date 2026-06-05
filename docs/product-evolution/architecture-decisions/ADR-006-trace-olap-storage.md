# ADR-006 Trace OLAP Storage

status: Deferred
owner: Data Platform

## Source Evidence

Optimization plans proposed moving execution traces from MySQL pressure paths to Doris or ClickHouse.

## Current-Code Evidence

`TraceWriteBuffer` writes batches to MySQL via `CanvasExecutionTraceMapper.insertBatch` and optionally dual-writes to `DorisStreamLoader`. It treats Doris as a migration-side sink and keeps MySQL fallback.

## Decision

Defer production OLAP-only trace storage until P2-016 trace schema and sink work proves query, retention, and fallback behavior.

## Expected Benefit

Lower MySQL pressure and faster analytics queries over execution traces.

## Cost

Medium to high: schema mapping, stream load reliability, query fallback, retention, and operational ownership change.

## Rollback

Keep MySQL trace writes as fallback during dual-write. Roll back by disabling Doris stream load and querying MySQL.

## Proof Command

`mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest`

## Accepted Evidence

Required: dual-write parity, load error handling, retention policy, query latency comparison, and backfill procedure.

## Child Spec

Existing dependency: P2-016. Later rollout spec required before OLAP-only mode.

## Dependency Notes

P2-016 must precede any production OLAP migration. Delivery and DAG rewrites are independent and should not be bundled.
