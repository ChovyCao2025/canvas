# Vendor Alternatives

Status date: 2026-06-05

## Chosen Local Contracts

| Contract | Current adapter | Candidate alternatives | Operational cost | Decision |
| --- | --- | --- | --- | --- |
| `CanvasMessageBus` | RocketMQ via `RocketMqCanvasMessageBus` | Kafka, Pulsar, RabbitMQ, cloud event bus | Delivery semantics, ordering keys, retry/DLQ mapping, topic provisioning | Keep RocketMQ now; message publishing uses the local contract where migrated. |
| `DistributedRateLimiter` | Redis via `RedisDistributedRateLimiter` | Redisson, Bucket4j with Redis, database quota table | Atomic increment semantics, TTL precision, cluster topology, clock consistency | Keep Redis now; replay rate limiting uses the local contract. |
| `ExpressionEngine` | Groovy via `GroovyExpressionEngine` | Aviator, QLExpress, MVEL, JavaScript sandbox | Sandbox parity, compile cache, timeout enforcement, output-size guard, script compatibility | Keep Groovy now; Groovy-specific runtime stays behind the contract. |
| `ExternalHttpClient` | WebClient via `WebClientExternalHttpClient` | Feign, Retrofit, Apache HttpClient, JDK HttpClient | Timeout/retry policy, metrics, error mapping, auth signing, base URL config | Keep WebClient now; reach delivery calls use the local contract. |
| `reactFlowAdapter.ts` | React Flow / `@xyflow/react` | Rete.js, JointJS, custom canvas, SVG/D3 | Interaction parity, accessibility, edge/node migration, selection/history behavior | Keep React Flow runtime now; adapter covers graph helper boundary only. |

## Alternative Notes

### RocketMQ

RocketMQ remains the production message broker for canvas MQ sending and cache invalidation.
The local contract reduces handler and cache tests from requiring `RocketMQTemplate`.

Replacement trigger:
- sustained operations cost from RocketMQ topics/groups;
- need for stronger ecosystem support around Kafka/Pulsar;
- inability to meet ordering or DLQ requirements.

Migration tests required:
- normal publish maps topic, tag, payload, and timeout;
- orderly publish preserves sharding key;
- cache invalidation payload and consumer compatibility stay stable.

### Redis / Redisson

Redis remains the current shared state backend. Redisson is the main alternative when the
system needs standardized distributed locks, semaphores, and rate limiters beyond simple
`StringRedisTemplate` increments.

Replacement trigger:
- multiple services need the same lock/rate-limit semantics;
- Lua or Redisson primitives become necessary for fairness or burst control;
- Redis key ownership becomes too broad for direct call sites.

Migration tests required:
- namespace and TTL compatibility;
- local fallback behavior;
- concurrent cost accounting;
- key catalog updates.

### Groovy / Aviator / QLExpress

Groovy remains the expression runtime because existing canvas scripts depend on Groovy syntax.
Aviator and QLExpress are alternatives for smaller expression-only workloads, but neither can be
adopted without a script compatibility plan.

Replacement trigger:
- sandbox maintenance risk exceeds Groovy compatibility value;
- expression language is constrained to pure expressions;
- compile and runtime cost exceeds SLO.

Migration tests required:
- compile cache behavior;
- timeout behavior;
- result-size limit;
- validation output compatibility;
- aggregate expression compatibility.

### WebClient / Feign

WebClient remains the current HTTP implementation. Feign is a candidate for typed service clients
when endpoints are stable and request/response DTOs are owned by the repo.

Replacement trigger:
- reach delivery needs typed retries, auth, circuit breaker policy, or generated clients;
- multiple integrations need consistent metrics and error mapping;
- WebFlux-to-MVC migration changes HTTP client defaults.

Migration tests required:
- integration name and base URL routing;
- `/send` payload and response mapping;
- failure mapping into `DeliveryResult`;
- timeout and retry policy.

### React Flow

React Flow remains the editor runtime. The adapter is intentionally thin and only exposes helper
boundaries (`reactFlowAdapter`) for graph conversion and connection policy.

Replacement trigger:
- product approves a different graph editor;
- React Flow cannot satisfy accessibility or interaction requirements;
- licensing, performance, or mobile constraints become blocking.

Migration cost:
- custom node/edge rendering rewrite;
- selection, viewport, mini-map, drag/drop, and history behavior parity;
- snapshot, graph serialization, publish validation, and connection interaction test migration.

Tests required before replacement:
- `graphHydration`;
- `connectionInteraction`;
- graph serialization;
- insert-node and outlet-routing;
- browser screenshots for editor canvas interactions.

## Non-Goals

- MyBatis is a non-goal for this package. Persistence abstraction would duplicate mapper contracts
  without reducing current vendor migration risk.
- React Flow runtime replacement is a non-goal. This package only adds an adapter boundary for
  helper reuse and documents the replacement trigger and migration cost.
- Dynamic user-configured HTTP clients such as API_CALL and tag import sources are non-goals until
  connector contracts are specified.
