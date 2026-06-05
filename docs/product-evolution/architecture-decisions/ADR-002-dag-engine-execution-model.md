# ADR-002 DAG Engine Execution Model

status: Deferred
owner: Execution Engine

## Source Evidence

Optimization plans proposed replacing the Reactor-based DAG execution flow with an imperative engine.

## Current-Code Evidence

`DagEngine` imports Reactor `Mono`, `Flux`, `Scheduler`, `Schedulers`, and `Retry`; it coordinates wait resumes, node gates, timeout scheduling, trace writes, and DLQ boundaries.

## Decision

Defer a full DAG rewrite. Prefer small, tested coordinators and lifecycle gates until proof shows a rewrite removes real operational risk.

## Expected Benefit

Potentially simpler control flow and fewer reactive lifecycle leaks.

## Cost

Very high: wait semantics, repeat gates, hub timeout, node retry, trace writes, DLQ behavior, and dry-run behavior can regress.

## Rollback

Keep current `DagEngine` as the only production execution path until the child spec provides dual-run comparison and a feature flag.

## Proof Command

`mvn -pl canvas-engine test -Dtest=RuntimeMigrationEvidenceTest`

## Accepted Evidence

Required: current behavior inventory, deterministic replay corpus, wait/resume compatibility, and dual-run trace equivalence.

## Child Spec

Required before implementation: `p2-018b-dag-engine-execution-model-proof`.

## Dependency Notes

Depends on P0-004 idempotency and context bounds. Do not combine with WebFlux migration.
