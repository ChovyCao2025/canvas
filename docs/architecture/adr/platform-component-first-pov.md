# ADR Platform Component First POV

## Status

Accepted for proof only.

## Problem

The codebase uses Redis, local synchronization, database leases, and custom coordination patterns across runtime, warehouse, delivery, and BI paths. P0-04 covers execution concurrency safety, P0-05 covers production resilience and recovery, and P2-04 documents Redis/Redisson as a vendor alternative. The platform needs a standard distributed-coordination proof without exposing Redisson APIs directly to business code.

## Decision

Run a proof-of-value for Redisson behind a local `DistributedCoordination` interface. The first target is a non-hot scheduler lease such as the CDP warehouse job lease path, not canvas execution admission, DAG execution, delivery outbox dispatch, or user-facing request handling.

This decision does not approve production rollout and does not approve replacing all `StringRedisTemplate` usage.

## Alternatives

| Alternative | Reason not selected first |
|---|---|
| Keep current Redis primitives | Lowest change, but it does not prove a standard lock/semaphore abstraction for future multi-instance workloads. |
| Database lease table only | Useful for auditability, but slower and not a general distributed coordination primitive. |
| Kubernetes Lease API | Good for infrastructure controllers, less portable for application-level tenant/resource locks. |
| XXL-JOB first | Solves schedule visibility but has larger operational blast radius and does not directly standardize locks. |
| Nacos first | Solves dynamic config but adds refresh-risk before config ownership and test gates are ready. |
| Spring Boot Admin first | Low risk, but current Prometheus/Grafana/actuator coverage already closes the immediate observability baseline. |

## Rollout

1. Define a local interface in infrastructure or platform package; business code must not import Redisson types.
2. Keep the current implementation as default.
3. Add a Redisson adapter behind a feature flag.
4. Target one low-risk scheduler lease.
5. Run concurrency proof tests and a Redis failover/timeout drill in staging.
6. Compare duplicate-run, skipped-run, lock-wait, timeout, and release metrics against the current implementation.
7. Decide whether to expand, keep as optional, or remove the adapter.

## Rollback

- Turn off the Redisson adapter feature flag.
- Route the local interface back to the current lease implementation.
- Keep lock keys and lease names compatible during the proof.
- Re-run the scheduler idempotency and duplicate-run checks.
- If Redis failover causes stale locks, expire the proof namespace keys and keep the old implementation active.

## Owner

Platform owner for Redis/Redisson operations, runtime owner for scheduler lease behavior, and operations owner for dashboard and alert review.

## Success metric

The proof succeeds only if all are true:

- duplicate scheduled job execution remains zero in the proof window;
- skipped scheduled job executions caused by lock errors remain zero;
- p95 lock acquisition time is within the agreed threshold;
- lock release, timeout, and Redis failover behavior are observable;
- the old implementation can be restored without data migration;
- no business package imports Redisson APIs directly.

## Stop criteria

Stop the proof and do not expand Redisson if:

- the adapter leaks Redisson types into domain or engine business code;
- Redis failover produces stale locks or duplicate job execution;
- lock acquisition latency breaches the scheduler SLO;
- metrics cannot distinguish acquired, skipped, timed out, failed, and released leases;
- rollback requires data migration or manual database repair;
- operations owner does not accept the Redis topology and watchdog settings.

## Deferred Components

- XXL-JOB is deferred until scheduler ownership, per-job idempotency, and migration ordering are documented.
- Nacos is deferred until dynamic config ownership and refresh safety tests exist.
- Knife4j is rejected for first proof because Springdoc already exists and it does not close a P0/P1/P2 runtime gap.
- Feign + Sentinel is deferred until connector contracts are stable.
- Spring Boot Admin needs evidence because current Prometheus/Grafana/actuator coverage already exists.
- ClickHouse and DolphinScheduler are deferred by the P3-03 data-platform decision.
- Spring Cloud Gateway is deferred because it would change topology before service boundaries are ready.
- Harbor, Nexus, SonarQube, and load-test tooling need pipeline evidence before tool adoption.
- dynamic-datasource is deferred by P3-04 datasource ownership gates.

## Links

- `docs/architecture/work-products/p3-07-platform-components/platform-component-decision-matrix.md`
- `docs/architecture/work-products/p3-07-platform-components/platform-component-abstraction-plan.md`
- `docs/architecture/evidence/P0-04-execution-concurrency-safety.md`
- `docs/architecture/evidence/P0-05-production-resilience-and-dr.md`
- `docs/architecture/evidence/P2-04-dependency-abstraction-and-vendor-lock-in.md`
- `docs/architecture/dependencies/vendor-alternatives.md`
