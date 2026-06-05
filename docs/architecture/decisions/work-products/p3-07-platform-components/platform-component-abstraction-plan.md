# Platform Component Abstraction Plan

Date: 2026-06-05

Status: P3-07 proof boundary. No runtime code change is approved by this document.

## Local Interface

Business code must depend on a local interface, not Redisson APIs:

```java
public interface DistributedCoordination {
    <T> T withLease(LeaseRequest request, Supplier<T> action);
    LeaseAttempt tryAcquire(LeaseRequest request);
    void release(LeaseHandle handle);
}
```

`LeaseRequest` owns resource name, tenant ID, wait timeout, lease TTL, owner ID, and idempotency key. `LeaseAttempt` records acquired, skipped, timed out, failed, and owner-mismatch results. `LeaseHandle` carries only local fields and never exposes Redisson lock objects.

## Proof Boundary

- Default implementation: current scheduler lease implementation.
- Proof implementation: Redisson adapter behind a feature flag.
- First target: one low-risk CDP warehouse scheduler lease.
- Excluded: canvas execution admission, DAG node coordination, delivery outbox dispatch, notification realtime, auth sessions, and all general Redis cache operations.

## Proof Test Plan

Required proof test coverage:

- interface contract maps acquire, skip, timeout, release, owner mismatch, and exception results.
- proof test for duplicate workers proves only one action runs for the same lease key.
- proof test for action failure proves release or TTL expiry behavior is safe.
- proof test for timeout proves caller receives a classified result, not an untyped Redis exception.
- proof test for feature flag proves rollback to current implementation.
- proof test for tenant scoping proves lease keys include tenant or explicit system scope.

## Operational Drill

The operational drill must run in staging before production:

1. Start two replicas with the proof adapter enabled for the selected lease.
2. Trigger the selected scheduler cycle.
3. Confirm only one worker acquires the lease.
4. Restart the winning pod during the lease.
5. Confirm TTL expiry or watchdog release behavior.
6. Verify no duplicate job run and no permanently skipped job.
7. Disable the feature flag and confirm the current implementation resumes.

## Metrics And Dashboard

Minimum metric set:

- `canvas_coordination_lease_acquired_total`
- `canvas_coordination_lease_skipped_total`
- `canvas_coordination_lease_timeout_total`
- `canvas_coordination_lease_failed_total`
- `canvas_coordination_lease_release_failed_total`
- `canvas_coordination_lease_wait_seconds`

The dashboard must show acquisition rate, skip rate, timeout rate, failure rate, p95 wait time, and selected lease key family. Alerts must fire on duplicate-run detection, timeout spikes, release failures, and missing scheduler run.

## Rollback Command

The rollback command is configuration-based:

```bash
kubectl -n canvas set env deployment/canvas-engine CANVAS_COORDINATION_REDISSON_ENABLED=false
kubectl -n canvas rollout status deployment/canvas-engine
```

If Helm owns the environment, use:

```bash
helm upgrade canvas deploy/helm/canvas -f deploy/helm/canvas/values-prod.yaml --set backend.config.canvasCoordinationRedissonEnabled=false
```

The actual values key must be added only when the implementation PR introduces the feature flag.

## Owner Signoff

Production rollout requires owner signoff from:

- Platform owner for Redis topology, Redisson version, watchdog settings, and secret/config ownership.
- Runtime owner for selected lease semantics and duplicate-run stop conditions.
- Operations owner for metrics, dashboard, alert routing, and rollback drill.
- Security owner if Redis credentials, namespace, or multi-tenant key isolation changes.

## Production Prerequisites

- ADR moved from proof-only to accepted for rollout.
- Feature flag exists and defaults to disabled.
- Current implementation remains available.
- Proof tests and staging operational drill pass.
- Metrics and dashboard are deployed.
- Rollback command is validated.
- Redis failover behavior is documented by the platform owner.
- P3-00 and ADR-0006 boundaries remain unchanged.
