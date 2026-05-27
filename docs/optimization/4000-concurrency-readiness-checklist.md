# 4000 Concurrency Readiness Checklist

## Purpose

4000 is a readiness target after 3000 is stable. It must not be enabled by only changing `canvas.execution.max-concurrency`.

## Entry Requirements

- The 3000 production gate passes for the full observation window.
- `LIGHT` and `STANDARD` p95 and p99 still have measurable headroom.
- Redis, MySQL, RocketMQ, and downstream dependencies are below sustained saturation thresholds.
- Retry backlog and DLQ growth are bounded.
- `HEAVY` and `RETRY` do not starve `LIGHT` or `STANDARD`.

## Starting Budget

| Lane | Concurrency | Share |
| --- | ---: | ---: |
| LIGHT | 800 | 20% |
| STANDARD | 2400 | 60% |
| HEAVY | 400 | 10% |
| RETRY | 400 | 10% |
| Global | 4000 | 100% |

The lane total must equal the global budget. Production rollout requires passing the readiness gate, not only applying this configuration.

## Required Architecture Before Production

- per-lane worker pool or per-lane ring buffer
- async writer for trace, audit, stats, and console-view state
- physical Redis separation or equivalent isolation for execution-state, route-cache, and bitmap or large-object roles
- adaptive retry backoff based on backlog, downstream timeout, DLQ growth, and main-lane latency
- DAG cost profile computed at publish time and used by `ExecutionLaneResolver`
- downstream bulkhead per dependency

## Stop And Rollback Conditions

- `LIGHT` or `STANDARD` p99 degrades because `HEAVY` or `RETRY` is saturated.
- Async write buffer grows for the full observation window.
- Execution-state Redis latency is affected by route/cache or bitmap traffic.
- Adaptive retry backoff cannot reduce backlog growth.
- A downstream bulkhead opens for a critical dependency and main-lane RT does not recover.

## Verification

Validate the 4000 profile file:

```bash
node -e "const p=require('./tools/perf/4000-readiness-profiles.json'); const total=Object.values(p.lanes).reduce((sum,l)=>sum+l.concurrency,0); if (total !== p.targetConcurrency) throw new Error(String(total)); console.log(total)"
```

Render the mixed readiness command:

```bash
node tools/perf/hardening-profile.mjs \
  --profile-file tools/perf/4000-readiness-profiles.json \
  --profile readiness-mixed-4000 \
  --out-dir tmp/perf-4000-readiness \
  --run-id-prefix "perf_4000_readiness_$(date +%Y%m%d_%H%M%S)"
```
