# ADR-0006 Service Extraction Gate

## Status

Accepted

## Context

`docs/architecture/archive/completed/specs/P3-00-architecture-boundary-review-spec.md` and `docs/architecture/evidence/p3-00-architecture-boundary-review.md` both show that the current system has real anchors for seven bounded contexts, but the code still relies on shared `dal` mappers, one Flyway migration stream, cross-context service calls, and mixed tenant propagation.

The historical P3 service-split material is useful as a capability map. It is not an implementation approval for immediate physical services.

## Decision

Keep Marketing Canvas as one deployable modular monolith until a specific bounded context satisfies this ADR's extraction gates. Downstream P3 work may create domain maps, contracts, adapters, read models, and strangler plans, but it must not move code into a separately deployed service until the gates below are proven.

The default bounded contexts are:

- Canvas Authoring
- Execution Runtime
- CDP / Audience
- Reach / Notification
- Integration
- Platform
- Data Platform / Analytics

## Alternatives

- Extract the archived service list directly: rejected because it mixes domains, platform components, infrastructure, and technical isolation candidates before ownership and rollback are clear.
- Start with Kubernetes, gateway, or scheduler platformization as the boundary driver: rejected because platform topology does not define business ownership.
- Keep all boundaries implicit: rejected because the current shared mapper and cross-context calls already hide data ownership and transaction risk.

## Data Ownership

Before extraction, the candidate context must have a table ownership map covering source tables, read models, retention, PII class, deletion behavior, backup owner, and migration path. Shared-table reads must be replaced by APIs, domain ports, events, or read models.

## API Contracts

Every synchronous dependency must have a documented route or port contract, request/response DTOs, auth and tenant assumptions, compatibility window, error model, and rollback trigger.

## Event Contracts

Every asynchronous dependency must have an event name, schema owner, versioning rule, ordering and replay behavior, idempotency key, DLQ behavior, and reconciliation owner.

## Rollout Plan

Extraction requires a strangler rollout plan with old path, new path, proxy or adapter layer, dual-read or dual-write decision, deployment order, smoke checks, and feature flag or traffic switch.

## Rollback Plan

Rollback must be possible without corrupting another context. The ADR for a candidate must name rollback commands or sequence, data reconciliation, queue and cache cleanup, and the owner who decides rollback.

## Observability

The candidate must have independent metrics, logs, traces, dashboards, alerts, SLOs, and runbooks before production traffic crosses the new boundary.

## Tenant Propagation

Tenant context, trace context, operator identity, and idempotency must propagate across every boundary. Cross-tenant reads and writes must have characterization tests before extraction.

## Idempotency

All cross-boundary commands and events must define duplicate behavior, retry class, idempotency key, and reconciliation path. Shared transactions are not allowed across the future boundary.

## Exit Criteria

Physical extraction is allowed only when all of these are true:

- the candidate context has current characterization tests for API behavior, mapper reads/writes, tenant behavior, failure behavior, and event or notification behavior;
- data ownership and migration path are documented for every table or read model it touches;
- cross-context calls are classified as synchronous API, event, read model, shared library, or prohibited coupling;
- no runtime handler directly mutates non-runtime context persistence for the candidate flow;
- rollout, rollback, reconciliation, observability, tenant propagation, and idempotency are proven in evidence;
- the candidate ADR is accepted and links this gate.

## Consequences

- P3 service decomposition becomes reversible and evidence-driven.
- Data platform, WeCom, Kubernetes, and platform-component work can proceed as modular-monolith or platform decisions without implying service extraction.
- Some desired service splits will be deferred until contracts and operating evidence exist.

## Rollback Trigger

If any downstream P3 plan proposes direct physical extraction without satisfying this gate, stop the extraction work and return it to modular-monolith boundary cleanup.

## Owner

Architecture owner with backend, frontend, data, and operations reviewers for each candidate context.

## Linked Specs

- `docs/architecture/archive/completed/specs/P3-00-architecture-boundary-review-spec.md`
- `docs/architecture/archive/completed/specs/P3-00-architecture-boundary-code-verification.md`
- `docs/architecture/archive/completed/plans/P3-00-architecture-boundary-review-plan.md`
- `docs/architecture/evidence/p3-00-architecture-boundary-review.md`
