# 4000 Concurrency Readiness Runbook

## Entry Requirements

- P1-004 and P1-004D 3000 hardening evidence is accepted and retained.
- `canvas.execution.max-concurrency` remains at the last accepted value until this runbook passes.
- 4000 readiness lane budgets are `LIGHT=800`, `STANDARD=2400`, `HEAVY=400`, `RETRY=400`.
- `canvas.execution-request.lane-isolation.enabled=true` in the readiness environment.
- Redis role readiness is enabled and execution-state, route-cache, bitmap, and rate-limit traffic do not all share one logical Redis connection.
- MySQL, Redis, RocketMQ, downstream test doubles, and backend focused baseline are reachable.

## Baseline Command

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" \
mvn -f backend/pom.xml -pl canvas-boot -am test \
  -Dtest=DagCostProfilerTest,ExecutionLaneResolverTest,LaneWorkerIsolationTest,AdaptiveRetryBackoffPolicyTest,DownstreamBulkheadRegistryTest,CanvasExecutionRequestExecutorTest,RedisRoleConfigurationTest
```

Pass condition: `BUILD SUCCESS`.

## Profile Validation

```bash
node --test tools/perf/4000-readiness-profile.test.mjs
node tools/perf/hardening-profile.mjs --profile 4000-readiness --validate-only
```

The validation summary must print `totalConcurrency: 4000` and `blockedUntil: p1-004-accepted`.

## Profile Execution

```bash
node tools/perf/hardening-profile.mjs \
  --profile-file tools/perf/4000-readiness-profiles.json \
  --profile readiness-mixed-4000 \
  --out-dir tmp/perf-4000-readiness \
  --run-id-prefix perf_4000_readiness_$(date +%Y%m%d_%H%M%S) \
  --write-evidence true
```

Run the printed threshold-runner command and retain the generated evidence manifest under the run directory.

## Stop Gates

- LIGHT or STANDARD p95 exceeds the accepted 3000 gate while HEAVY or RETRY is saturated.
- Trace, audit, stats, or console-view writer backlog grows for two consecutive samples.
- Redis role latency is sustained above the accepted execution-state gate.
- MySQL active connections are at or above 85% of pool max.
- RocketMQ normal backlog grows while RETRY drains.
- Retry backlog grows after downstream recovery.
- Adaptive retry cannot drain backlog within one observation window.
- Downstream bulkhead remains open with no half-open recovery.

## Rollback Actions

- Restore previous lane budgets and keep global concurrency at the last accepted value.
- Disable `canvas.execution-request.lane-isolation.enabled` if readiness workers cause dispatch starvation.
- Disable 4000 readiness profile traffic.
- Pause scheduled, replay, and heavy jobs before protecting normal traffic.
- Re-run the backend baseline and 3000 profile validation after rollback.

## Degrade Actions

- Lower `HEAVY` lane budget or pause heavy audience/tagger jobs.
- Lower `RETRY` lane budget or lengthen adaptive retry backoff.
- Open provider-specific downstream bulkheads for unhealthy dependencies.
- Fail closed on Redis role health uncertainty.
- Keep LIGHT and STANDARD protected; do not borrow their capacity for HEAVY or RETRY.

## Evidence Storage

Store the profile command, threshold-runner output, evidence manifest, metric samples, and operator notes under `tmp/perf-4000-readiness/<run-id>/` and copy accepted artifacts to the release evidence store before any promotion decision.

## Promotion Block

This runbook is readiness only. Production 4000 enablement remains blocked until the 3000 evidence is accepted, this runbook passes, and release owners explicitly approve the resulting evidence bundle.
