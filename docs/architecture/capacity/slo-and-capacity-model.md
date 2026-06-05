# Canvas SLO And Capacity Model

Date: 2026-06-05

## Product SLOs

| Behavior | Target | Metric source | Error budget window | Alert severity | Owner |
| --- | --- | --- | --- | --- | --- |
| availability | 99.9% successful health/API availability | `up{application="canvas-engine"}` and HTTP 5xx rate | 30 days | P0 when burn rate exceeds 2% per hour | Runtime platform |
| trigger admission latency | p95 <= 100 ms for accepted direct/event/MQ admission | add `canvas.trigger.admission.duration` timer | 7 days | P1 when p95 breaches 15 minutes | Runtime platform |
| execution completion latency | p95 <= 3 seconds for light lane, p95 <= 30 seconds for standard lane | `canvas.execution.duration` plus lane tag follow-up | 7 days | P1 for light, P2 for standard | Runtime platform |
| API p95 latency | p95 <= 500 ms for authenticated CRUD/read APIs | HTTP server request timer from Actuator | 7 days | P1 when p95 breaches 15 minutes | Backend API |
| trace durability | >= 99.99% trace write success; zero silent trace drops | `canvas.trace.dropped.total` and trace writer logs | 30 days | P0 for any sustained drops | Runtime platform |
| notification delivery | p95 <= 5 seconds to websocket or persisted notification | add notification delivery timer/counter | 7 days | P2 unless delivery is unavailable | Notification owner |

## Current Config Limits

| Limit | Current value | Source | Workload input affected | Saturation symptom | First mitigation | Owner |
| --- | --- | --- | --- | --- | --- | --- |
| Hikari maximum pool | 33 | `spring.datasource.hikari.maximum-pool-size` | QPS, execution count, reporting scans | DB waits, API p95, scheduler lag | Reduce per-request SQL, add indexes, then tune pool | Backend/DBA |
| Redis pool max active | 64 | `spring.data.redis.lettuce.pool.max-active` | DAU, quota checks, route refresh, context load | Redis wait, trigger admission latency | Add TTL, reduce round trips, tune pool | Runtime platform |
| Disruptor ring buffer | 65,536 | `canvas.disruptor.ring-buffer-size` | trigger bursts and execution count | `canvas.disruptor.overflow.total` increases | throttle admission, split lanes, increase buffer | Runtime platform |
| Global execution concurrency | 3,000 | `canvas.execution.max-concurrency` | concurrent executions | in-flight registry rejects, backlog growth | lane shaping, horizontal scale | Runtime platform |
| Light lane | 600 concurrency / 2,000 queue | `canvas.execution-lane.light` | direct light workflows | lane saturation, queue depth | move heavy work out of light lane | Runtime platform |
| Standard lane | 1,800 concurrency / 10,000 queue | `canvas.execution-lane.standard` | normal workflows | standard queue depth, p95 execution latency | tune worker count, split heavy handlers | Runtime platform |
| Heavy lane | 300 concurrency / 1,000 queue | `canvas.execution-lane.heavy` | expensive integrations/scripts | heavy queue depth, timeout/retry growth | isolate integrations, increase timeout budget carefully | Runtime platform |
| Retry lane | 300 concurrency / 3,000 queue | `canvas.execution-lane.retry` | retries and replay | retry backlog, delayed recovery | backoff tuning, DLQ review | Runtime platform |
| RocketMQ consumer threads | 20 | `canvas.mq.consume-thread-number` | MQ trigger QPS and MQ depth | broker queue depth, consumer lag | tune consumers, add instances | Runtime platform |
| Scheduler trigger concurrency | 100 | `canvas.scheduler.trigger-concurrency` | scheduled canvas count | delayed scheduled triggers | shard schedules, add scheduler instances | Runtime platform |
| HTTP client pool | 500 connections / 2,000 pending | `canvas.http-client` | API_CALL, send message, connected content | pending acquire timeout, API p95 | bulkhead by integration, tune pool | Integration owner |

## Workload Mapping

| Input | Capacity driver | Current control | Storage/memory growth |
| --- | --- | --- | --- |
| QPS | API threads, Hikari pool, Redis pool, HTTP client pool | API p95, DB pool saturation, Redis wait | request logs and execution request rows |
| DAU | context keys, quota keys, dedup keys, user-level execution rows | Redis TTL and quota key families | Redis memory and `canvas_execution` rows |
| canvas count | route keys, scheduler scans, published version retention | route rebuild and scheduler concurrency | route memory and version snapshots |
| execution count | Disruptor, lanes, MySQL execution ledger | global/lane concurrency and backlog | `canvas_execution` growth |
| trace rows | trace writer and Doris/MySQL trace table | trace drop counter and retention policy | `canvas_execution_trace` growth |
| Redis memory | context/quota/dedup/route/cache keys | Redis memory gauge and TTL policy | used_memory and eviction risk |
| MQ depth | RocketMQ topic backlog and consumer threads | queue depth gauge | broker disk growth |

## Capacity Cliff Table

| Metric | Limit | Symptom | First mitigation | Owner |
| --- | --- | --- | --- | --- |
| `canvas.capacity.pool.saturation.percent{resource="hikari"}` | warn 80%, critical 90% | API p95 and scheduler lag rise | inspect slow SQL, reduce scans, tune pool | Backend/DBA |
| `canvas.capacity.redis.memory.bytes` | warn 70% maxmemory, critical 85% | Redis evictions or trigger admission latency | enforce TTL, remove stale context/quota keys | Runtime platform |
| `canvas.capacity.queue.depth{queue="rocketmq:CANVAS_MQ_TRIGGER"}` | warn 50k, critical 100k | delayed MQ trigger execution | increase consumers, throttle producers | Runtime platform |
| `canvas.capacity.lane.saturation.percent{lane="standard"}` | warn 80%, critical 95% | execution completion p95 breach | move slow handlers to heavy lane | Runtime platform |
| `canvas.trace.dropped.total` | critical > 0 sustained | incomplete execution audit | stop load increase, inspect trace writer | Runtime platform |
| `canvas.capacity.dlq.backlog{queue="canvas_execution_dlq"}` | warn 100, critical 500 | unreplayed execution failures | run DLQ triage and replay | Runtime platform |
