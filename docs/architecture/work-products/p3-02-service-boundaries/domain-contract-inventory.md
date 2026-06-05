# Domain Contract Inventory

Date: 2026-06-05

Status: Active P3-02 contract inventory. It supports `docs/architecture/work-products/p3-02-service-boundaries/domain-map.md` and `docs/architecture/adr/ADR-0007-first-extraction-candidate.md`.

Chosen context for boundary hardening: Reach / Notification. Physical extraction is deferred until `docs/architecture/adr/ADR-0006-service-extraction-gate.md` is satisfied.

## Contract Scope

The first slice keeps the existing modular monolith and defines contracts around notification, delivery, contactability, and provider-facing reach behavior. No package, table, REST route, or frontend payload is moved by this document.

The slice depends on:

- Execution Runtime for delivery commands and system-alert requests.
- CDP / Audience for customer/contactability read models.
- Integration for provider connectors, datasource credentials, callbacks, and WeCom-style adapters.
- Platform for tenant context, auth principal, audit, and operator identity.
- Redis and MQ infrastructure for notification fanout, delivery outbox wakeups, retry, and idempotency.

## REST Inventory

| Area | REST route | DTO or payload | Auth and tenant assumptions | Classification | compatibility window | rollback trigger |
|---|---|---|---|---|---|---|
| Notification list | `GET /canvas/notifications` | `NotificationDTO`, query params `unreadOnly`, `category`, `archived`, `page`, `size` | Current user comes from JWT claims; tenant comes from `TenantContextResolver`; tenant may be null only for admin role. | synchronous API | Keep response shape for at least two frontend releases after any adapter is introduced. | Roll back if unread count, pagination, category, or tenant filtering changes for existing users. |
| Notification count | `GET /canvas/notifications/unread-count` | `Map<String, Long>` with `count` | Same as notification list. | synchronous API | Keep `count` key stable. | Roll back if frontend badge count diverges from table count. |
| Mark read | `PUT /canvas/notifications/{notificationId}/read` | Empty success body through `R<Void>` | User and tenant must match row scope. | synchronous API | Keep idempotent success for repeated read operations. | Roll back if another user's notification can be updated. |
| Mark all read | `PUT /canvas/notifications/read-all` | Empty success body through `R<Void>` | User and tenant scoped bulk update. | synchronous API | Keep route and result wrapper stable. | Roll back if unread rows outside the caller scope are updated. |
| Archive | `PUT /canvas/notifications/{notificationId}/archive` | Empty success body through `R<Void>` | User and tenant must match row scope. | synchronous API | Keep route stable while adapter is introduced. | Roll back if archived notifications reappear in default list. |
| WebSocket ticket | `POST /canvas/notifications/ws-ticket` | `NotificationWebSocketTicketDTO` | User and tenant scoped short-TTL ticket. | synchronous API | Ticket TTL and route stay compatible for existing frontend. | Roll back if ticket validation failure rate rises. |
| Delivery and receipt | `web/MessageDeliveryController.java`, `web/DeliveryReceiptController.java` | Delivery command/receipt DTOs | Must carry tenant, customer/channel, provider, correlation, and idempotency data before extraction. | synchronous API plus event | Keep current API surface until receipts are reconciled through events. | Roll back if provider message ID or receipt status cannot be reconciled. |
| CDP dependency | `GET /cdp/users`, `GET /cdp/users/{userId}`, `GET /cdp/users/{userId}/tags` | `CanvasUserRowDTO`, `CdpUserDetailDTO`, `CdpUserTagDTO` | Tenant from `TenantContextResolver`; admin can query all tenants through null tenant today. | read model | Replace direct reads with a versioned contactability/profile read model before extraction. | Roll back if reach decisions diverge from CDP profile/tag state. |
| Integration dependency | `GET/POST/PUT/DELETE /canvas/data-sources`, `GET /channels/connectors`, `POST /channels/connectors/{id}/health-test` | `DataSourceConfigReq`, connector DTOs | Tenant scoped; credentials encrypted; audit on credential writes. | synchronous API | Provider and datasource contracts remain in Integration until adapters are stable. | Roll back if connector health or credential access breaks delivery. |

## Event And MQ Inventory

| Contract | Current carrier | Producer | Consumer | Payload fields required before extraction | Classification | compatibility window | rollback trigger |
|---|---|---|---|---|---|---|---|
| Notification created/updated | Redis pub/sub key from `RedisKeyUtil.notificationChannel()` | `NotificationService` through `NotificationRealtimePublisher` | WebSocket notification path | `eventType`, `tenantId`, `userId`, notification identity, unread count, occurred-at, trace ID | event | Keep existing WebSocket event shape for two frontend releases. | Roll back if realtime update count diverges from REST unread count. |
| Notification WebSocket ticket | Redis key from `RedisKeyUtil.notificationWsTicket(ticket)` | `NotificationWebSocketTicketService` | WebSocket handshake | `ticket`, `tenantId`, `userId`, TTL, consumed marker | shared infrastructure contract | Keep TTL and single-use behavior stable. | Roll back if ticket replay or cross-tenant connection is possible. |
| Delivery wakeup | RocketMQ topic `${canvas.delivery.outbox.topic:CANVAS_DELIVERY}` | Delivery outbox writer or scheduler | `DeliveryOutboxConsumer` | outbox wakeup only today; future event needs `tenantId`, `outboxId`, `channel`, `provider`, idempotency key, trace ID | event | Introduce structured payload while accepting current wakeup semantics. | Roll back if delivery backlog increases or duplicate sends occur. |
| MQ trigger system alert | Direct `NotificationEventService.systemAlert(...)` call in `MqTriggerConsumer` | Execution Runtime | Reach / Notification | `alertType`, `tenantId`, `operator`, severity, dedup key, source message ID, trace ID | prohibited coupling today, event target later | Replace with `SystemAlertRequested` event before extraction. | Roll back if parse/validation failures stop creating alerts. |
| Delivery provider response | Return value from `ReachDeliveryService.dispatchToProvider` | Integration/provider adapter | Delivery outbox service | provider message ID, status, raw response digest, retry class, tenant, trace ID | synchronous API | Keep old provider adapter path active during proof. | Roll back if provider ID cannot be persisted or receipts cannot be matched. |
| Cache invalidation | `CanvasMessageBus.publishCacheInvalidation` and `RedisKeyUtil.cacheInvalidateChannel()` | Domain services | Cache SDK and subscribers | cache name, key, tenant, source version, trace ID | event | No extraction until cache ownership is explicit. | Roll back if stale cache affects reach policy or connector config. |

## Redis Key Inventory

| Redis key | Current owner | Future owner | Contract rule |
|---|---|---|---|
| `canvas:notification:events` | Reach / Notification | Reach / Notification | Event envelope must include tenant context, user ID, event ID, schema version, occurred-at, and trace context before service extraction. |
| `canvas:notification:ws-ticket:{ticket}` | Reach / Notification | Reach / Notification | Single-use, short TTL, tenant scoped, and rejected after consumption. |
| `canvas:event:dedup:{idempotencyKey}` | Execution Runtime today | Platform idempotency or owning context | Cross-boundary commands must define idempotency key source, TTL, duplicate result, and reconciliation path. |
| `canvas:trigger:*` | Execution Runtime | Execution Runtime | Reach must not rely on route keys except through events or command ports. |
| `canvas:cache:invalidate` | Platform/infrastructure | Platform | Cache event schema and ownership must be documented before a service consumes it across process boundaries. |

## Table Inventory

| Table family | Current mapper family | Proposed owner | Notes |
|---|---|---|---|
| `notification` | `NotificationMapper` | Reach / Notification | Tenant/user scoped notification state and unread count source. |
| `message_send_record` | `MessageSendRecordMapper` | Reach / Notification | Provider send result and reporting state. Needs provider message ID contract. |
| `delivery_outbox`, `delivery_receipt_log` | Delivery outbox and receipt mappers/services | Reach / Notification | Best first boundary-hardening table pair. Needs event payload, retry, DLQ, and reconciliation proof. |
| `customer_profile`, `customer_channel` | Customer mappers | CDP / Audience or Reach read model | Must not become shared source-of-truth. Reach should consume a contactability read model. |
| `marketing_consent`, `marketing_suppression`, `marketing_frequency_counter` | Marketing policy mappers | Reach / Notification with compliance input | Must carry tenant, PII class, retention, and deletion behavior. |
| `channel_connector`, `channel_provider_limit`, `channel_fallback_policy`, `channel_fallback_decision`, `channel_dedupe_record` | Channel connector/policy mappers | Integration for connector config, Reach for delivery decisions | Split ownership must be explicit before datasource or service extraction. |

## Cross-Domain Call Classification

| Current call | Desired contract | Classification | Risk |
|---|---|---|---|
| `MqTriggerConsumer` calls `NotificationEventService.systemAlert(...)` | `SystemAlertRequested` event with tenant, trace, dedup key, source message, and severity. | prohibited coupling today | Runtime failure handling currently creates notifications directly. |
| `ReachDeliveryService` writes delivery records and calls provider adapters | `DeliveryCommandPort` plus provider adapter response contract. | synchronous API plus event | Provider retry and delivery state are still in one runtime package. |
| Send handlers under `engine/handlers` invoke reach behavior | `DeliveryCommandPort` from runtime to reach. | synchronous API | Generic DAG handlers should not depend on provider-specific logic. |
| Notification WebSocket publisher uses Redis directly | Versioned notification realtime event envelope. | event | Redis payload must become an explicit compatibility contract. |
| Reach/contactability reads customer/CDP data | `ContactabilityReadModel` owned by CDP / Audience and consumed by Reach. | read model | Current table sharing risks tenant and deletion-policy drift. |
| Reach uses tenant/auth helpers | Shared platform library or internal API. | shared library | Allowed while it contains policy-free context propagation only. |
| Provider connector health/config is read from Integration tables | `ProviderConnectorApi` or `ProviderConfigReadModel`. | synchronous API or read model | Direct table reads would block extraction. |

## Strangler Migration Plan

This is a reversible modular-monolith strangler plan. It does not approve physical extraction.

| Item | Decision |
|---|---|
| old path | Runtime handlers, `ReachDeliveryService`, `NotificationService`, `DeliveryOutboxConsumer`, shared mappers, and shared Flyway scripts inside `canvas-engine`. |
| new path | Add ports inside the monolith first: `NotificationCommandPort`, `DeliveryCommandPort`, `ContactabilityReadModel`, and `ProviderConnectorApi`. Implement them with existing services before moving any package. |
| proxy/adapter layer | Runtime calls local ports. The port implementation can later proxy to a service only after ADR-0006 gates pass. |
| dual-read | No dual-read at first. Add a read-model shadow comparison only after `ContactabilityReadModel` exists. |
| dual-write | No dual-write until delivery outbox reconciliation can prove no duplicate provider sends. |
| compatibility window | Keep REST routes, DTOs, notification WebSocket payloads, and delivery outbox wakeup semantics stable for at least two frontend releases or one full delivery reconciliation cycle, whichever is longer. |
| rollback trigger | Any increase in duplicate sends, missing notifications, unread-count drift, ticket replay, cross-tenant notification access, provider message ID loss, or unreconciled receipt backlog. |
| reconciliation | Compare `delivery_outbox`, provider message IDs, `message_send_record`, `delivery_receipt_log`, notification unread counts, and WebSocket event counts by tenant and trace ID. |
| deployment order | 1. Add ports and tests. 2. Add structured event envelope while accepting old payloads. 3. Add metrics and dashboards. 4. Add read-model shadow checks. 5. Only then consider package movement. 6. Physical extraction requires a new ADR linked to ADR-0006. |
| tenant context | Every command, event, Redis key payload, and provider callback must carry tenant ID or an explicit admin/system context. |
| trace context | Every synchronous API and event must carry correlation ID from `CorrelationIdWebFilter` or an equivalent trace context. |
| idempotency | Every delivery command and notification command must name an idempotency key, duplicate result, retry class, and cleanup TTL. |

## Test And Evidence Requirements

Backend characterization coverage required before extraction:

- `CanvasUserQueryServiceTest` for CDP read behavior and direct execution mapper coupling.
- `NotificationServiceTest` for notification creation, tenant-scoped publication, duplicate handling, list/count/update/archive behavior, and realtime events.
- `CanvasExecutionServiceCdpTest` for the current Execution Runtime to CDP dependency that must be removed before extraction.

Frontend API contract tests are not required for this P3-02 implementation because no route payload changes are made. If `NotificationDTO`, WebSocket payloads, or delivery API shapes change later, add focused frontend tests beside the changed service.

No physical service extraction can start until this inventory, `docs/architecture/work-products/p3-02-service-boundaries/domain-map.md`, and `docs/architecture/adr/ADR-0007-first-extraction-candidate.md` are updated with passing verification evidence.
