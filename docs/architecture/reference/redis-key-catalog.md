# Redis Key Catalog

All Redis keys must be built through `RedisKeyUtil`. The default namespace prefix is `canvas`, configurable through `canvas.redis.key-prefix`.

| family | prefix / pattern | owner service | TTL | payload | invalidation | operational risk | cleanup link |
| --- | --- | --- | --- | --- | --- | --- | --- |
| MQ trigger route | `canvas:trigger:mq:<topicKey>` | `TriggerRouteService`, `CanvasRouteInitializer` | no TTL | Redis Set of canvas IDs | route rebuild replaces route sets | stale route drops or misroutes MQ triggers | `docs/architecture/capacity/retention-policy.md` |
| behavior trigger route | `canvas:trigger:behavior:<eventCode>` | `TriggerRouteService` | no TTL | Redis Set of canvas IDs | route rebuild or publish/offline refresh | stale event routing | `docs/architecture/capacity/retention-policy.md` |
| tagger trigger route | `canvas:trigger:tagger:<tagCodeKey>` | `TriggerRouteService` | no TTL | Redis Set of canvas IDs | route rebuild or publish/offline refresh | stale realtime tag routing | `docs/architecture/capacity/retention-policy.md` |
| route ready flag | `canvas:trigger:routes:ready` | `TriggerRouteService` | no TTL | string marker | deleted before rebuild, set after rebuild | consumers retry while absent | `docs/architecture/runbooks/route-rebuild.md` |
| route mutation lock | `canvas:trigger:routes:mutation-lock` | `TriggerRouteService` | 30 seconds | lock token | Lua token release or TTL | concurrent route replacement | `docs/architecture/runbooks/route-rebuild.md` |
| context snapshot | `canvas:<canvasId>:user:<userId>` | `ContextPersistenceService` | `canvas.execution.context-ttl-sec`, default 24h | JSON `ExecutionContext` | delete on execution completion or failure | Redis memory growth, stale wait/resume state | `docs/architecture/capacity/retention-policy.md` |
| resume lock | `canvas:resume-lock:<canvasId>:<userId>` | `ContextPersistenceService` | execution timeout | lock token | Lua token release or TTL | duplicate wait/resume execution | `docs/architecture/runbooks/dag-execution-flow.md` |
| message dedup | `canvas:dedup:<canvasId>:<userId>:<msgId>` | `ContextPersistenceService` and trigger services | caller supplied TTL | string marker | natural TTL, emergency delete only | duplicate trigger execution | `docs/architecture/capacity/retention-policy.md` |
| quota | `canvas:quota:<canvasId>:<userId>:<date>` | quota/precheck services | next day plus retention buffer | numeric counter | natural TTL and quota cleanup | quota bypass or Redis memory growth | `docs/architecture/capacity/retention-policy.md` |
| global quota | `canvas:global_count:<canvasId>` | quota/precheck services | canvas valid end plus buffer | numeric counter | offline/archive cleanup | global quota drift | `docs/architecture/capacity/retention-policy.md` |
| API rate limit | `canvas:ratelimit:<apiKey>:<epochSecond>` | API rate-limit service | short rolling window | numeric counter | natural TTL | false rejection or unbounded keys | `docs/architecture/capacity/retention-policy.md` |
| execution replay rate limit | `canvas:execution-request:replay:<scope>:<operator>:<epochMinute>` | replay management service | short rolling window | numeric counter | natural TTL | replay storm | `docs/architecture/runbooks/dlq-replay.md` |
| publish lock | `canvas:publish:lock:<canvasId>` | canvas publish service | bounded lock TTL | lock token | lock release or TTL | concurrent publish | `docs/architecture/runbooks/failure-triage.md` |
| inflight canvas | `canvas:inflight:canvas:<canvasId>` | execution admission registry | score expiry | ZSET execution IDs | timeout scanner removes stale entries | false saturation | `docs/architecture/capacity/retention-policy.md` |
| inflight lane | `canvas:inflight:lane:<lane>` | execution admission registry | score expiry | ZSET execution IDs | timeout scanner removes stale entries | lane admission rejection | `docs/architecture/capacity/slo-and-capacity-model.md` |
| inflight global | `canvas:inflight:global` | execution admission registry | score expiry | ZSET execution IDs | timeout scanner removes stale entries | global admission rejection | `docs/architecture/capacity/slo-and-capacity-model.md` |
| max concurrency config | `canvas:config:max-concurrency` | route/admission initializer | no TTL | string config value | deployment-controlled | mixed cluster limits | `docs/architecture/capacity/slo-and-capacity-model.md` |
| event dedup | `canvas:event:dedup:<idempotencyKey>` | event ingestion service | 24h | string marker | natural TTL | duplicate event acceptance | `docs/architecture/capacity/retention-policy.md` |
| login fail | `canvas:login:fail:<username>` | auth service | security window | numeric counter | natural TTL/reset on success | account lock noise | `docs/architecture/archive/specs/P0-01-security-hardening-spec.md` |
| login locked | `canvas:login:locked:<username>` | auth service | lock window | string marker | natural TTL/admin unlock | login outage for user | `docs/architecture/archive/specs/P0-01-security-hardening-spec.md` |
| revoked JWT | `canvas:jwt:revoked:<tokenHash>` | auth service | token expiry | string marker | natural TTL | revoked token reuse | `docs/architecture/archive/specs/P0-01-security-hardening-spec.md` |
| canvas config cache | `canvas:<canvasId>:v<versionId>:config` | `CanvasEntityCache`, `CanvasConfigCache` | cache policy TTL | serialized runtime config | `RocketMqCacheInvalidationPublisher` and `/ops/cache/invalidate/{id}` | stale graph execution | `docs/architecture/runbooks/cache-invalidation.md` |
| cache invalidation channel | `canvas:cache:invalidate` | cache invalidation publisher/subscriber | channel only | invalidation event | publish event | cross-node stale cache | `docs/architecture/runbooks/cache-invalidation.md` |
| kill switch | `canvas:kill:<canvasId>` | kill-switch subscriber/services | incident-defined | kill command marker/message | incident close or TTL | disabled active canvas | `docs/architecture/runbooks/failure-triage.md` |
| notification websocket ticket | `canvas:notification:ws-ticket:<ticket>` | notification websocket ticket service | short auth window | ticket payload | natural TTL | websocket auth leakage | `docs/architecture/archive/specs/P0-01-security-hardening-spec.md` |
| notification channel | `canvas:notification:events` | notification realtime publisher | channel only | notification event | publish event | missed realtime notice | `docs/architecture/archive/specs/P1-04-observability-and-ops-spec.md` |

## Update Rule

When a new Redis key is added, update this catalog in the same change as `RedisKeyUtil`, add or update the owning service test, and link the key to a retention or runbook path.
