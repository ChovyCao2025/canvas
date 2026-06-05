# ADR-0007 First Extraction Candidate

## Status

Accepted - Deferred for physical extraction.

## Context

`docs/architecture/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md` requires a reversible service or bounded-context split. `docs/architecture/adr/ADR-0006-service-extraction-gate.md` blocks physical service extraction until data ownership, API contracts, event contracts, tenant propagation, idempotency, observability, rollout, rollback, and characterization tests are proven.

The current codebase has real bounded-context anchors, but it also has 85 controllers, 146 shared mappers, one Flyway stream, direct cross-context mapper reads, and runtime calls into CDP and notification services. New CDP warehouse and BI packages strengthen the need for a gate because they are still in the same deployable and datasource ownership model.

## Decision

No first physical extraction candidate is approved in this iteration. The first executable step is Reach / Notification boundary hardening inside the modular monolith.

This means:

- keep the existing deployable and datasource topology;
- keep current REST routes and frontend payloads stable;
- introduce or document ports/read models before moving packages;
- treat `Reach / Notification` as the first boundary-hardening slice, not as a separate service;
- require a new candidate ADR before any process, database, or deployment boundary is introduced.

## Alternatives

- Extract CDP / Audience first: deferred because CDP still reads execution and canvas mappers directly, and CDP ingestion is now coupled to warehouse services.
- Extract Reach / Notification first: deferred as physical extraction, accepted as the first boundary-hardening slice because outbox, notification, and delivery contracts can be isolated inside the monolith.
- Extract Integration / WeCom first: deferred because callback replay, credential ownership, compliance, tenant propagation, and provider-specific rollback still need evidence.
- Extract Canvas Authoring or Execution Runtime first: rejected because graph versioning, scheduler registration, runtime admission, context persistence, Redis route/cache state, and execution state transitions are still tightly coupled.
- Split all services now: rejected by ADR-0006 and P3-00 because it would create deployment boundaries before ownership and rollback are proven.

## Candidate Scoring

Scoring uses 1 as easiest/lowest risk and 5 as hardest/highest risk.

| Candidate | coupling | data ownership clarity | traffic pressure | operational noise | tenant risk | rollback difficulty | Decision |
|---|---:|---:|---:|---:|---:|---:|---|
| CDP / Audience | 4 | 3 | 3 | 3 | 4 | 4 | Deferred |
| Reach / Notification | 3 | 3 | 4 | 4 | 3 | 3 | Deferred for extraction; first boundary-hardening slice |
| Integration / WeCom | 3 | 2 | 3 | 4 | 4 | 3 | Deferred |

Reach / Notification has the best near-term shape because notification rows, delivery outbox rows, provider responses, Redis notification fanout, and system-alert requests can be wrapped by explicit contracts without moving runtime execution tables.

## Data Ownership

Reach / Notification ownership candidates:

- `notification`
- `message_send_record`
- `delivery_outbox`
- `delivery_receipt_log`
- reach-specific policy state such as suppression, consent, frequency counters, and dedupe records once compliance ownership is confirmed

Tables that must not be silently absorbed:

- CDP profile, identity, tag, and customer master tables remain CDP / Audience or a CDP-owned read model.
- Channel connector credentials and provider health configuration remain Integration unless an explicit split assigns ownership.
- Execution runtime records remain Execution Runtime.

Before extraction, every row must have tenant scope, PII class, retention rule, deletion behavior, backup owner, and migration path.

## API Contracts

The first hardening slice must preserve these current routes:

- `GET /canvas/notifications`
- `GET /canvas/notifications/unread-count`
- `PUT /canvas/notifications/{notificationId}/read`
- `PUT /canvas/notifications/read-all`
- `PUT /canvas/notifications/{notificationId}/archive`
- `POST /canvas/notifications/ws-ticket`

New internal ports should be introduced before package movement:

- `NotificationCommandPort`
- `DeliveryCommandPort`
- `ContactabilityReadModel`
- `ProviderConnectorApi`

Each synchronous API or port must define request DTO, response DTO, tenant rule, auth principal, error model, compatibility window, and rollback trigger.

## Event Contracts

Required event contracts before physical extraction:

- `NotificationCreated`
- `NotificationUpdated`
- `DeliveryRequested`
- `DeliverySent`
- `DeliveryFailed`
- `ReceiptReceived`
- `SystemAlertRequested`

Each event must define schema owner, schema version, event ID, tenant ID, correlation ID, occurred-at timestamp, idempotency key, ordering rule, replay behavior, DLQ behavior, and reconciliation owner.

## Rollout Plan

1. Keep all code inside `canvas-engine`.
2. Add ports for notification command, delivery command, contactability read model, and provider connector reads.
3. Route existing runtime calls through those ports without changing behavior.
4. Add characterization tests for current request/response shape, mapper writes, tenant scope, idempotency, realtime notification event behavior, and provider failure behavior.
5. Add structured event envelopes while accepting current Redis/MQ payloads.
6. Add dashboards and reconciliation checks.
7. Revisit physical extraction only after ADR-0006 exit criteria are satisfied.

## Rollback Plan

Rollback stays inside the monolith during this ADR:

- switch runtime calls back to existing service implementations if a port adapter fails;
- keep old REST routes and DTOs unchanged;
- keep old Redis notification and RocketMQ delivery wakeup semantics until compatibility evidence is complete;
- reconcile `notification`, `delivery_outbox`, `message_send_record`, and `delivery_receipt_log` by tenant, trace ID, and provider message ID;
- use `docs/architecture/runbooks/deploy-rollback.md` for application rollback if release health or SLO checks fail.

## Observability

Before extraction, Reach / Notification must have:

- notification create/update counters by tenant and user scope;
- unread-count drift checks between REST and realtime paths;
- WebSocket ticket issue/consume/reject metrics;
- delivery outbox backlog, retry, dead, and sent counters;
- provider latency/error metrics by connector and tenant;
- reconciliation dashboard for outbox, send record, receipt log, and provider message ID;
- alerts for duplicate sends, cross-tenant updates, missing receipts, and event backlog.

## Tenant Propagation

Every command, event, Redis payload, MQ payload, provider callback, and read model must carry tenant context. Admin or system operations must be explicit and test-covered. Tenant context must not be inferred from nullable row fields after extraction.

## Idempotency

Every delivery and notification command must define:

- idempotency key source;
- duplicate behavior;
- retry class;
- TTL or cleanup owner;
- reconciliation path;
- whether a duplicate returns the existing row, no-op success, or a retryable error.

Shared transactions are allowed inside the current monolith but are not allowed across a future service boundary.

## Exit Criteria

Physical extraction can be reconsidered only when all conditions are true:

- `docs/architecture/domain-contract-inventory.md` has current REST, event, Redis key, MQ, table, DTO, tenant, compatibility window, rollback trigger, and reconciliation details.
- CDP/contactability dependencies use a read model or synchronous API instead of direct table reads.
- Runtime system alerts use an event contract instead of direct `NotificationEventService` calls.
- Delivery commands use a port and have idempotency tests.
- Realtime notification events have a versioned envelope.
- Characterization tests pass against the existing monolith.
- Rollout, rollback, observability, tenant propagation, and reconciliation evidence are attached to the candidate ADR.

## Consequences

- P3-02 becomes an executable boundary-hardening plan without starting a risky service split.
- Reach / Notification can improve contracts first and remain reversible.
- CDP / Audience and Integration / WeCom remain candidates, but both need stronger ownership and contract evidence.
- Canvas Authoring and Execution Runtime remain together until graph, scheduler, route/cache, and execution state coupling is reduced.

## Rollback Trigger

Stop boundary hardening and return to the previous implementation path if any of these happen:

- notification unread counts diverge from table state;
- duplicate provider sends increase;
- provider message IDs cannot be reconciled;
- cross-tenant notification read/write is detected;
- delivery outbox backlog grows after adapter introduction;
- WebSocket ticket replay or tenant leakage is detected;
- runtime alerts stop producing notifications.

## Owner

Architecture owner with backend, frontend, data, security/compliance, and operations reviewers. Reach / Notification needs a named domain owner before service extraction can be proposed again.

## Links

- `docs/architecture/specs/P3-00-architecture-boundary-review-spec.md`
- `docs/architecture/evidence/p3-00-architecture-boundary-review.md`
- `docs/architecture/adr/ADR-0006-service-extraction-gate.md`
- `docs/architecture/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`
- `docs/architecture/domain-map.md`
- `docs/architecture/domain-contract-inventory.md`
