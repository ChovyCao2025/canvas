# Dependency Inventory

Status date: 2026-06-05

Scope: direct uses of `RocketMQTemplate`, `StringRedisTemplate`, `WebClient`, `GroovyShell`,
`SecureASTCustomizer`, and `@xyflow/react` found in backend and frontend source.

## Summary

| Dependency | Decision | Current boundary |
| --- | --- | --- |
| RocketMQTemplate | contract now for message publishing; adapter only for RocketMQ implementation | `CanvasMessageBus` plus `RocketMqCanvasMessageBus` |
| StringRedisTemplate | contract now for replay rate limiting; adapter only for Redis implementation | `DistributedRateLimiter` plus `RedisDistributedRateLimiter` |
| GroovyShell / SecureASTCustomizer | contract now for script execution | `ExpressionEngine` plus `GroovyExpressionEngine` |
| WebClient | contract now for reach delivery calls; leave direct for unrelated stable HTTP clients | `ExternalHttpClient` plus `WebClientExternalHttpClient` |
| @xyflow/react | adapter only for graph conversion and connection helpers; runtime remains React Flow | `reactFlowAdapter.ts` |

## Backend Direct Usage

| Package / file | Dependency | Behavior | Decision | Rationale |
| --- | --- | --- | --- | --- |
| `infrastructure/mq/RocketMqCanvasMessageBus.java` | RocketMQTemplate | Publishes normal, orderly, and cache-invalidation messages. | adapter only | This is the RocketMQ-specific adapter behind `CanvasMessageBus`. |
| `engine/handlers/SendMqHandler.java` | RocketMQTemplate | Previously published MQ node messages directly. | contract now | Migrated to `CanvasMessageBus` so handler tests do not require RocketMQ. |
| `infrastructure/cache/RocketMqCacheInvalidationPublisher.java` | RocketMQTemplate | Previously published cache invalidation events directly. | contract now | Migrated to `CanvasMessageBus.publishCacheInvalidation`. |
| `engine/trigger/TriggerAdmissionService.java` | RocketMQTemplate | Admits trigger work and publishes execution requests. | leave direct | Delivery path already has an outbox; admission publishing needs a separate execution-command contract. |
| `engine/delivery/DeliveryOutboxService.java` | RocketMQTemplate | Enqueues delivery outbox messages. | leave direct | Outbox delivery has distinct transactional semantics; do not mix with generic message bus until outbox contract is specified. |
| `domain/cdp/CdpEventPublisher.java` | RocketMQTemplate | Publishes CDP domain events. | leave direct | Domain-event publishing should use a future event-bus contract, not the canvas message bus. |
| `infrastructure/redis/RedisDistributedRateLimiter.java` | StringRedisTemplate | Increments namespaced replay-rate keys and sets TTL. | adapter only | This is the Redis-specific adapter behind `DistributedRateLimiter`. |
| `engine/request/CanvasExecutionReplayRateLimiter.java` | StringRedisTemplate | Previously accepted Redis directly for replay limits. | contract now | Migrated to `DistributedRateLimiter`; local fallback remains for tests and single-instance local runs. |
| `config/JwtAuthFilter.java`, `web/AuthController.java` | StringRedisTemplate | Stores and checks auth/session token state. | leave direct | Auth token storage is security-specific and should be abstracted only with an auth/session store spec. |
| `service/impl/EventDefinitionServiceImpl.java` | StringRedisTemplate | Event metadata cache invalidation. | leave direct | Single bounded metadata cache use; low migration value today. |
| `engine/policy/MarketingPolicyService.java` | StringRedisTemplate | Consent, suppression, availability, quiet-hour, and frequency policy checks. | leave direct | Candidate for a marketing-policy store contract after policy data model stabilizes. |
| `engine/handlers/ApiCallHandler.java` | StringRedisTemplate | Per-node API call rate limit. | leave direct | Similar to replay rate limiting but part of API node policy; migrate after node policy contract is defined. |
| `infrastructure/redis/ContextPersistenceService.java` | StringRedisTemplate | Persists execution context snapshots. | adapter only | Infrastructure package owns Redis persistence details. |
| `infrastructure/redis/TriggerRouteService.java`, `CanvasRouteInitializer.java`, `MqRouteRefreshService.java`, `KillSwitchSubscriber.java` | StringRedisTemplate | Route, kill-switch, and trigger cache state. | adapter only | Redis use stays contained in route/cache infrastructure. |
| `engine/audience/AudienceBitmapStore.java`, `VersionedAudienceBitmapStore.java`, `AudienceBatchComputeService.java` | StringRedisTemplate | Audience bitmap and batch-compute state. | leave direct | Audience storage needs a dedicated bitmap/audience-store contract before migration. |
| `engine/trigger/InFlightExecutionRegistry.java`, `TriggerPreCheckService.java`, `CanvasExecutionService.java` | StringRedisTemplate | Trigger pre-checks, in-flight state, and execution state. | leave direct | Execution-state abstraction overlaps with platform decomposition work and is out of this package. |
| `domain/canvas/CanvasService.java`, `CanvasOpsService.java` | StringRedisTemplate | Canvas cache and ops controls. | leave direct | Cache/ops state should be handled under cache catalog and ops-control specs. |
| `domain/notification/*` | StringRedisTemplate | Notification realtime and websocket ticket state. | leave direct | Notification realtime store is a separate bounded context. |
| `engine/expression/GroovyExpressionEngine.java` | GroovyShell, SecureASTCustomizer | Owns Groovy shell pool, secure AST setup, compile cache, timeout, and result-size semantics. | adapter only | Groovy is contained behind `ExpressionEngine`. |
| `engine/handlers/GroovyScriptCache.java` | GroovyShell | Compiles and instantiates Groovy scripts for the adapter. | adapter only | Cache supports the Groovy adapter and does not leak into handler orchestration. |
| `engine/handlers/GroovyHandler.java` | GroovyShell, SecureASTCustomizer | Previously owned Groovy runtime details. | contract now | Migrated to `ExpressionEngine`; handler now owns config parsing, validation, and routing. |
| `infrastructure/http/WebClientExternalHttpClient.java` | WebClient | Posts JSON to named external integrations. | adapter only | WebClient is contained behind `ExternalHttpClient` for reach delivery. |
| `engine/delivery/ReachDeliveryService.java` | WebClient | Previously called reach platform `/send` directly. | contract now | Migrated to `ExternalHttpClient`. |
| `engine/handlers/CouponHandler.java` | WebClient | Calls coupon integration directly. | leave direct | Single handler-specific integration; abstract after coupon provider selection and retry policy are specified. |
| `engine/handlers/ApiCallHandler.java` | WebClient | Executes user-configured API nodes. | leave direct | This is dynamic user workflow HTTP, not a fixed vendor integration. |
| `engine/audience/AudienceBatchComputeService.java`, `AudienceEvaluationContextFetcher.java` | WebClient | Calls tagger/audience services. | leave direct | Candidate for a data-platform client contract in P3 data-platform work. |
| `engine/trigger/CanvasSchedulerService.java` | WebClient | Uses tagger/API clients in trigger scheduling. | leave direct | Coupled to scheduler runtime; migrate under scheduler/service-boundary work. |
| `domain/meta/TagImportSourceService.java` | WebClient | Pulls external tag import sources. | leave direct | Dynamic operator-configured source client; needs connector contract first. |
| `domain/canvas/ConnectedContentGatewayService.java`, `domain/cdp/WebhookDispatcherService.java` | WebClient | Connected content and webhook calls. | leave direct | Candidate for a future connector/webhook client contract. |
| `engine/llm/OpenAiCompatibleLlmClient.java` | WebClient | Calls OpenAI-compatible LLM endpoints. | leave direct | LLM provider abstraction already lives at the client boundary. |
| `domain/bi/subscription/*` | WebClient | BI delivery and snapshot renderer integrations. | leave direct | BI subscription delivery belongs to BI platform evolution specs, not this package. |
| `web/MetaController.java`, `config/WebClientConfig.java` | WebClient | Admin metadata proxy and global client builder. | adapter only | Configuration and web edge code may own framework setup. |

## Frontend Direct Usage

| Package / file | Dependency | Behavior | Decision | Rationale |
| --- | --- | --- | --- | --- |
| `pages/canvas-editor/reactFlowAdapter.ts` | @xyflow/react | Re-exports React Flow types and graph/connection helper boundary. | adapter only | Provides a stable seam for tests and future migration without replacing runtime. |
| `pages/canvas-editor/index.tsx` | @xyflow/react, ReactFlow, useReactFlow | Main interactive editor runtime. | leave direct | React Flow remains the approved runtime library. |
| `pages/canvas-editor/useCanvasGraphState.ts`, `useCanvasHistoryState.ts`, `useCanvasSelectionState.ts`, `useCanvasPublishWorkflow.ts`, `useCanvasTestRunWorkflow.ts` | @xyflow/react types/hooks | Editor state and workflow hooks. | leave direct | Tight runtime coupling is acceptable until replacement is approved. |
| `pages/canvas-editor/graphSerialization.ts`, `connectionInteraction.ts`, `insertNode.ts`, `outletRouting.ts`, `publishValidation.ts`, `editorSnapshot.ts` | @xyflow/react types | Pure graph helpers and type guards. | adapter only | Existing helpers are re-exported through `reactFlowAdapter.ts`; no runtime migration needed. |
| `components/canvas/CanvasNode.tsx`, `BranchPlaceholderNode.tsx`, `HoverEdge.tsx` | @xyflow/react | React Flow custom node and edge rendering. | leave direct | Component APIs are React Flow-specific by design. |
| `hooks/useBranchPlaceholders.ts` | @xyflow/react types | Placeholder graph derivation. | leave direct | Depends on React Flow node/edge shape; low replacement value now. |
| `types/canvas.ts` | @xyflow/react type imports | Shared graph DTO typing. | leave direct | Type-level coupling is acceptable while runtime remains React Flow. |
| Frontend tests under `pages/canvas-editor` and `components/config-panel` | @xyflow/react types | Unit test fixtures. | leave direct | Tests can use runtime types directly while adapter coverage exists. |

## Follow-Up Candidates

- Execution-command publishing: `TriggerAdmissionService` and related RocketMQTemplate usage.
- Delivery outbox publisher contract with transactional semantics.
- Marketing policy store and API node rate-limit contract.
- Audience bitmap store abstraction.
- Connector/webhook/connected-content HTTP client abstraction.
- React Flow runtime replacement only after product approval and migration test plan.
