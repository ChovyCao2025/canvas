# ADR-0004 Redis, RocketMQ, And MySQL Choices

## Status

Accepted

## Context

The current deployable uses MySQL for authoritative canvas, execution, request, DLQ, audit, quota, and configuration state; Redis for context snapshots, route tables, locks, quotas, cache invalidation, kill switches, and notifications; RocketMQ for asynchronous trigger and cache invalidation transport.

## Decision

Keep MySQL as the system of record, Redis as bounded runtime coordination/cache state, and RocketMQ as asynchronous delivery transport. Redis key families must be cataloged in `docs/architecture/reference/redis-key-catalog.md`; high-volume MySQL tables must follow the P2-02 retention policy; RocketMQ consumers must have retry, DLQ, and replay runbooks.

## Alternatives

- Store execution state primarily in Redis: rejected because recovery, audit, and retention need durable SQL evidence.
- Replace RocketMQ with Redis streams now: rejected because current trigger routing, tests, and operations target RocketMQ.
- Introduce a separate event platform immediately: deferred to P3 platform evolution specs.

## Consequences

- Operators can reason about source-of-truth and runtime-state ownership separately.
- Redis memory, MQ depth, DB pool saturation, and DLQ growth must be monitored together.
- Vendor abstraction work remains a P2-04 follow-up, not part of this ADR.

## Rollback Trigger

Revisit if RocketMQ availability becomes a release blocker, Redis memory pressure repeatedly breaches P2-02 critical thresholds, or service decomposition requires a dedicated event platform.

## Owner

Runtime platform owner.

## Linked Specs

- `docs/architecture/specs/P0-05-production-resilience-and-dr-spec.md`
- `docs/architecture/specs/P1-04-observability-and-ops-spec.md`
- `docs/architecture/specs/P2-02-cost-capacity-and-retention-spec.md`
- `docs/architecture/specs/P2-04-dependency-abstraction-and-vendor-lock-in-spec.md`
