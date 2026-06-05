# Engine Web Boundary

Date: 2026-06-05

Status: P3-09 boundary contract. This does not approve physical service extraction.

## Degradation Matrix

| Dependency | Default behavior | Reason |
|---|---|---|
| Redis unavailable for execution context, route, dedupe, or quota | fail-closed | Prevent duplicate execution, stale route use, cross-tenant cache leak, or quota bypass. |
| Redis unavailable for optional UI cache or non-critical preview cache | fail-open with stale or uncached read | User experience can degrade without side effects. |
| RocketMQ unavailable for execution command, delivery outbox wakeup, or domain event publish | fail-closed for new side-effect command; retry/outbox for committed work | Avoid lost commands and duplicate sends. |
| datasource unavailable for transactional domain writes | fail-closed | Source of truth cannot be updated safely. |
| datasource unavailable for read-only analytics summary | fail-open with stale cached/read-model response only if marked stale | Avoid blocking operators when stale data is acceptable. |
| WeCom unavailable | fail-closed for sends and callbacks that require verification; retry for accepted callback processing | Avoid unverified provider state and duplicate external sends. |
| analytics / warehouse unavailable | fail-open for online canvas execution; fail-closed for analytics export or certification job | Online execution must not depend on P3 data-platform components. |
| AI provider unavailable | fail-open only for optional prediction/recommendation; fail-closed for required AI node output unless node config allows fallback | Preserve workflow semantics and operator intent. |

Every dependency path must explicitly choose fail-open or fail-closed. Silent fallback is not allowed.

## API Boundary

Web/admin owns:

- authentication and authorization edge behavior;
- request validation and response wrapper compatibility;
- operator workflows;
- admin-only tenant visibility;
- frontend-facing DTOs;
- OpenAPI/API contract documentation.

Execution engine owns:

- trigger admission;
- execution request lifecycle;
- DAG parsing and execution;
- node handler contracts;
- runtime context persistence;
- idempotency and side-effect safety;
- execution DLQ and retry behavior.

API boundary rule: web/admin may call engine application services through stable request DTOs or command ports, but must not mutate engine tables directly from controllers.

## Event Boundary

event boundary rules:

- engine emits versioned execution lifecycle events;
- web/admin consumes read models or query APIs, not raw engine internals;
- notification/system-alert requests use event contracts before extraction;
- event envelopes include tenant ID, correlation ID, schemaVersion, eventId, idempotency key, and occurred-at timestamp;
- every event consumer defines retry, DLQ, replay, and idempotency behavior.

## Data Ownership

data ownership rules:

- web/admin owns UI/admin configuration tables only when they are not execution runtime state;
- execution engine owns execution request, execution state, execution context, DLQ, wait subscription, side-effect idempotency, and runtime metrics state;
- CDP owns identity, profile, tags, consent source data, and OneID graph;
- Reach/Notification owns delivery, receipt, notification, contactability decision records, and marketing policy effects;
- Integration owns connector metadata, credentials references, callback ledger, and provider state;
- Platform owns tenant, auth, audit, schema registry, event registry, and quota contracts.

## Deployment Boundary

Deployment boundary rules:

- current state remains one modular-monolith deployable;
- future web/admin and execution split requires ADR-0006 exit criteria;
- shared database tables must not be split before datasource ownership and event contracts are proven;
- frontend route changes must preserve API compatibility window;
- deployment order must be schema, backend compatibility, event envelope, frontend, dashboards, alerts, and runbooks.

## Rollback

rollback rules:

- keep old API paths and DTOs during compatibility window;
- keep old event consumer path until new event path proves parity;
- disable new boundary adapter by feature flag before reverting schema;
- replay or reconcile side effects by idempotency key;
- stop rollout if tenant propagation, event idempotency, or data ownership checks fail.

## Observability

observability requirements:

- API latency, error rate, auth failure, tenant failure, and validation failure by route;
- engine trigger admission, queue depth, lane saturation, execution duration, DLQ backlog, and retry count;
- event publish, consume, retry, DLQ, replay, and idempotency duplicate metrics;
- Redis, RocketMQ, datasource, WeCom, analytics, and AI dependency health;
- correlation ID propagation across API, command, event, and background work.

## Contract Tests

Required contract tests before any service extraction consumes these primitives:

- API boundary test for request, response, auth, validation, and tenant behavior;
- event boundary test for envelope, schemaVersion, eventId, idempotency key, ordering key, and replay;
- data ownership test that controllers cannot bypass owner service/port for engine-owned tables;
- deployment compatibility test that old and new routes/events can coexist;
- rollback test that feature flag or adapter switch restores old behavior;
- observability test that metrics and correlation IDs are emitted;
- degradation test for Redis, RocketMQ, datasource, WeCom, analytics, and AI fail-open/fail-closed decisions.
