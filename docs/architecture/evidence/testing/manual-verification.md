# Manual Verification Register

Manual verification is allowed only when the dependency is not yet stable in the automated harness. Each row must be removed when the automated replacement path lands.

| Dependency | Command | Environment | Owner | Evidence file | Expiration date | Automated replacement path |
| --- | --- | --- | --- | --- | --- | --- |
| RocketMQ broker integration | `docker compose -f docker-compose.local.yml up -d rocketmq-namesrv rocketmq-broker && cd backend && mvn -pl canvas-engine -Dtest=MqTriggerConsumerTest,SendMqHandlerTest test` | Local Docker stack | Runtime platform | `docs/architecture/evidence/testing/rocketmq-<date>.md` | 2026-07-31 | Replace `CanvasRocketMqTestSupport` substitute with dedicated namesrv + broker Testcontainers. |
| Local capacity smoke/accuracy | `node tools/perf/perf-guide.mjs smoke` and `node tools/perf/perf-guide.mjs accuracy` after `docs/stressTest/local-capacity-runbook.md` setup | Local Docker stack with MySQL, Redis, RocketMQ, WireMock, backend container | Performance owner | `tmp/perf-runs/<perfRunId>/` plus summary in `docs/architecture/evidence/testing/capacity-local-<date>.md` | 2026-08-31 | Add deterministic smoke/accuracy CI job against ephemeral Compose services. |
| Distributed capacity run | `node tools/perf/perf-guide.mjs distributed-plan`, worker `distributed-worker`, then `distributed-report` | Multi-worker environment and shared backend dependencies | Performance owner | `tmp/perf-distributed/<perfRunId>/` plus summary in `docs/architecture/evidence/testing/capacity-distributed-<date>.md` | 2026-09-30 | Add scheduled non-PR performance pipeline with retained artifacts. |

## Rules

- Do not publish capacity or reliability numbers without the evidence file named in the table.
- Do not extend an expiration date without adding the blocker and the new automated replacement owner.
- Delete the row in the same change that adds the automated test or CI job.
