# 3000 Concurrency Hardening Runbook

## Entry Requirements

- Java 21 backend baseline passes.
- `canvas.execution.max-concurrency=3000`.
- Lane budgets are `LIGHT=600`, `STANDARD=1800`, `HEAVY=300`, `RETRY=300`.
- Redis, MySQL, RocketMQ, downstream test doubles, and local backend are reachable.
- `tools/perf/3000-hardening-profiles.json` validates lane totals and required profile names.

## Baseline Command

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest,InFlightExecutionRegistryTest,ExecutionLaneResolverTest,InFlightExecutionRegistryLaneTest,CanvasServicePublishTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest test
```

Pass condition: `BUILD SUCCESS`.

## Profile Execution

```bash
node tools/perf/hardening-profile.mjs \
  --profile-file tools/perf/3000-hardening-profiles.json \
  --profile default-mixed-3000 \
  --out-dir tmp/perf-3000-hardening \
  --run-id-prefix perf_3000_hardening_$(date +%Y%m%d_%H%M%S) \
  --write-evidence true
```

Run the command printed by `hardening-profile.mjs` and keep the generated `evidence-manifest.json`.

## Stop Gates

- Redis p95 above 20 ms or p99 above 50 ms for one observation window.
- MySQL active connections at or above 85% of pool max.
- Slow SQL above 1000 ms in two consecutive samples.
- Normal MQ backlog grows while RETRY drains.
- Disruptor overflow grows for two consecutive samples.
- Retry backlog grows after downstream recovery.
- DLQ grows after downstream recovery.
- LIGHT or STANDARD p95 exceeds 1000 ms.

## Rollback Actions

- Restore previous `canvas.execution.max-concurrency`.
- Restore previous lane budgets.
- Pause scheduled, replay, and heavy traffic when needed.
- Keep normal traffic on the last passing profile.
- Rerun the backend baseline after rollback.

## Degrade Actions

- Lower `RETRY` or lengthen retry backoff.
- Lower `HEAVY` or pause heavy jobs.
- Disable low-priority scheduled/replay traffic.
- Keep `LIGHT` and `STANDARD` protected when their dependencies remain healthy.
- Reject new admission conservatively if Redis registry health is unknown.

## 4000 Block

Do not start 4000 readiness until every required 3000 profile passes and evidence artifacts are retained.
