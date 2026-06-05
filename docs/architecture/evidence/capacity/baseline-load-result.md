# Baseline Load Result

Date: 2026-06-05

## Status

No publishable capacity number was produced in this pass. The repeatable local and distributed load procedures are documented in `docs/stressTest/local-capacity-runbook.md` and `docs/stressTest/distributed-capacity-runbook.md`, but a valid result requires the Docker dependency stack, backend perf container, fixture generation, smoke, accuracy, monitor capture, and threshold run to complete without verifier failures.

## Commands Verified In This Pass

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasExecutionRequestServiceTest,CanvasExecutionRequestExecutorTest test
node --test tools/perf/*.test.mjs
node tools/perf/perf-guide.mjs doctor
```

These commands cover the pre-load backend unit guard and perf tooling preflight from the runbooks. Full load execution remains a manual item tracked by `docs/architecture/evidence/testing/manual-verification.md`.

## Local Dependency Versions

| Dependency | Local version |
| --- | --- |
| MySQL | `mysql:8.0` |
| Redis | `redis:7-alpine` |
| RocketMQ | `apache/rocketmq:5.3.1` |
| RocketMQ Dashboard | `apacherocketmq/rocketmq-dashboard:latest` |
| WireMock | `wiremock/wiremock:3.3.1` |
| Doris FE | `apache/doris:2.0.3-fe` |
| Doris BE | `apache/doris:2.0.3-be` |

## Required Baseline Fields

| Field | Current value |
| --- | --- |
| QPS | not measured |
| p95 latency | not measured |
| execution failures | not measured |
| queue depth | not measured |
| Redis memory | not measured |
| DB pool saturation | not measured |
| CPU | not measured |
| memory | not measured |
| bottleneck | not measured |

## Acceptance Rule

Do not copy the `not measured` values into release notes or capacity claims. Replace this file only after `perf-guide.mjs smoke`, `accuracy`, and a threshold run pass and their artifacts are retained under `tmp/perf-runs/<perfRunId>/`.
