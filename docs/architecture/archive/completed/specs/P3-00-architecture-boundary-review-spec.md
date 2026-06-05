# Spec: Architecture Boundary Review

Source package: `docs/architecture/active/reviewed-packages/p3/platform-evolution/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`

## Review Status

Decision review. This document evaluates the service-split and platform-evolution documents before implementation planning.

Supporting code verification: [P3-00-architecture-boundary-code-verification.md](./P3-00-architecture-boundary-code-verification.md).

## Source Documents Reviewed

- `docs/architecture/archive/evolution/service-architecture-design.md`
- `docs/architecture/archive/evolution/target-architecture-overview.md`
- `docs/architecture/archive/evolution/architect-critical-review.md`
- `docs/architecture/archive/evolution/data-platform-architecture.md`
- `docs/architecture/archive/evolution/multi-datasource-isolation.md`
- `docs/architecture/archive/evolution/webflux-to-mvc-migration.md`
- `docs/architecture/archive/evolution/k8s-deployment-plan.md`
- `docs/architecture/archive/evolution/production-practice-review.md`
- `docs/architecture/archive/evolution/wecom-scrm-module-design.md`

## Verdict

The archived service-split direction is useful as a long-term capability map, but it is not a reasonable immediate architecture split.

The current architecture should first be split by bounded context inside a modular monolith. Physical services should be extracted later, only after contracts, data ownership, operational readiness, and rollback paths are clear.

The code verification confirms the same conclusion: the current code contains real anchors for Canvas Authoring, Execution Runtime, CDP / Audience, Reach / Notification, Integration, Platform, and future Data Platform / Analytics, but shared `dal` access and cross-context imports still prevent service extraction.

## Why The Previous Service Split Is Not Ready

The previous split mixes different kinds of things into one service list:

- business domains: Canvas, CDP, WeCom, Notification;
- platform products: gateway, scheduler, data platform, analytics;
- infrastructure components: XXL-JOB, DolphinScheduler, Redis, ClickHouse, K8s;
- technical isolation candidates: sandbox, webhook gateway, file service, ML serving.

That creates deployment units before domain contracts exist. It also hides the hardest work: tenant propagation, data ownership, cross-domain transaction boundaries, event contracts, compatibility windows, and rollback.

Immediate risks:

- Too many deployables before observability, release governance, and tenant isolation are ready.
- Data ownership remains unclear, so service boundaries would still share database tables or duplicate business rules.
- The data platform scope is too broad and should start from one business slice, not a full lakehouse platform.
- WeCom is a valid integration domain, but a dedicated WeCom service is premature until callbacks, sync, material, message, and compliance flows are proven.
- Scheduler, gateway, K8s, and component choices are platform decisions, not bounded contexts.

## Recommended Current Boundary Model

Use seven bounded contexts first.

| Context | Owns | Does Not Own | Current Code Anchors |
|---|---|---|---|
| Canvas Authoring | canvas CRUD, draft, version, publish, state machine | runtime execution internals, user profile, channel delivery | `domain/canvas`, `web/CanvasController` |
| Execution Runtime | trigger admission, DAG execution, scheduler, wait/resume, lanes, node runtime state | canvas authoring policy, CDP storage, channel implementation | `engine/*`, `domain/execution`, `engine/trigger` |
| CDP / Audience | user profile, identity, tag, audience, segment, audience compute contracts | canvas versioning, channel delivery | `domain/cdp`, `engine/audience`, `dto/cdp` |
| Reach / Notification | message intent, delivery, channel adapter contract, receipt, notification center | journey orchestration, CDP identity ownership | `domain/notification`, `engine/delivery`, message handlers |
| Integration | WeCom, external API, webhook/callback adapters, external credentials | generic DAG scheduling, CDP core identity rules | WeCom design docs, `domain/datasource`, API handlers |
| Platform | auth, tenant, system options, audit, config, operator APIs | business-domain state transitions | `auth`, `common/tenant`, `domain/tenant`, `domain/meta`, `web/*Admin*` |
| Data Platform / Analytics | CDC/event ingestion, OLAP, reports, retention, lineage, data quality | OLTP execution path, online transaction decisions | evolution data-platform docs, future analytics modules |

## Dependency Rules

- Canvas Authoring publishes immutable version/runtime policy for Execution Runtime.
- Execution Runtime may call CDP, Reach, and Integration only through explicit domain ports or client contracts.
- Handlers must not directly own CDP, notification, tenant, or external API persistence.
- CDP owns identity, profile, tag, audience, and segment data.
- Reach owns delivery and receipt semantics; canvas nodes create message intent, not channel-specific side effects.
- Integration owns external protocol details, callbacks, signing, retry, replay, and adapter credentials.
- Platform owns tenant/auth/config/audit primitives and exposes context contracts to other domains.
- Data Platform consumes events or CDC and must not be required for the online execution path to complete.

## Recommended Evolution Path

### Phase 1: Modular Monolith First

Keep one deployable while enforcing package/module boundaries, DTO contracts, and domain services.

Required before service extraction:

- P0 security, state consistency, data isolation, concurrency, and reactive/transaction issues are contained.
- P1 API validation, DAG boundaries, observability, and release governance are in place.
- Domain map and dependency rules are committed.
- Characterization tests exist for the context being moved.

### Phase 2: Data Ownership And Event Contracts

Define table ownership, event ownership, tenant propagation, idempotency, and reconciliation.

The first split should not happen while multiple contexts still depend on the same mapper tables or implicit transaction side effects.

### Phase 3: First Physical Service Candidate

The best first candidates are:

1. CDP / Audience, if audience computation or profile queries are the main scaling pressure.
2. Reach / Notification, if channel delivery and receipt retries are operationally noisy.
3. Integration / WeCom, if callback volume, external API limits, or compliance requirements justify isolation.

Canvas Authoring and Execution Runtime should be separated later. They are currently too tightly coupled through graph version, runtime policy, scheduler, execution context, and route/cache behavior.

### Phase 4: Platformization

Gateway, K8s, platform components, and data platform work should follow proven domain boundaries. They should not drive the first split.

## Service Extraction Triggers

A bounded context can become a separate service only when all conditions are true:

- It owns its data tables or has a documented migration path.
- It has stable API and event contracts.
- Its failure mode is known and acceptable.
- It has independent metrics, logs, alerts, dashboards, and runbooks.
- It can be deployed and rolled back without corrupting another context.
- Cross-context writes use events, outbox, saga, or reconciliation instead of shared transactions.
- Tenant context, trace context, and idempotency propagate across the boundary.

## Items To Defer

- Full data platform: start with one thin slice before selecting full CDC/Flink/ClickHouse/LakeHouse scope.
- Dedicated WeCom service: start as Integration context unless callbacks and sync workloads justify isolation.
- File service: keep as storage adapter until multiple domains need independent file lifecycle.
- Sandbox service: first harden in-process limits and script contracts; isolate later if resource/security pressure remains.
- ML serving: defer until product requirements and model lifecycle exist.
- Scheduler products: evaluate as platform components after current scheduler semantics are stable.

## Impact On Existing P3 Specs

- `P3-02-service-decomposition-and-domain-boundaries-spec.md` must use this bounded-context model instead of promoting the previous service list directly.
- `P3-03-data-platform-architecture-spec.md` must define a thin vertical slice before platform component selection.
- `P3-04-multi-datasource-isolation-spec.md` must follow context data ownership.
- `P3-06-k8s-deployment-platform-spec.md` must not drive domain boundaries.
- `P3-07-production-platform-components-spec.md` must treat tools as decisions, not services.
- `P3-08-wecom-scrm-module-spec.md` must start as Integration context unless service extraction triggers are satisfied.
- `P3-09-identity-event-and-tenant-platform-spec.md` is a prerequisite for any serious service split.

## Acceptance Criteria

- Architecture work uses the seven bounded contexts as the default boundary model.
- Physical service extraction is blocked until the service extraction triggers are satisfied.
- P3 specs distinguish business domains, platform components, infrastructure, and technical isolation candidates.
- New implementation plans reference this review when proposing service boundaries.
- Service-split proposals cite or refresh the supporting code verification before creating ADRs or implementation plans.
