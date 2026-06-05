# Spec: Service Decomposition And Domain Boundaries

Source package: `docs/architecture/active/reviewed-packages/p3/platform-evolution/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`

## Verification Status

Implemented as a bounded-context and extraction-gate decision package. Physical extraction is deferred; Reach / Notification boundary hardening is the first modular-monolith slice. Evidence and active decisions are in `docs/architecture/evidence/p3-02-service-decomposition.md`, `docs/architecture/decisions/work-products/p3-02-service-boundaries/domain-map.md`, `docs/architecture/decisions/work-products/p3-02-service-boundaries/domain-contract-inventory.md`, and `docs/architecture/decisions/adr/ADR-0007-first-extraction-candidate.md`.

## Source Documents

- `docs/architecture/archive/evolution/service-architecture-design.md`
- `docs/architecture/archive/evolution/target-architecture-overview.md`
- `docs/architecture/archive/evolution/architecture-evolution-roadmap.md`
- `docs/architecture/archive/evolution/architect-critical-review.md`

## Scope

Define the service or bounded-context split for Marketing Canvas under the decision rules in `P3-00-architecture-boundary-review-spec.md`. The preferred path is modular-monolith hardening first, then strangler-style extraction only where ownership, data boundaries, traffic patterns, and operational readiness are clear.

Candidate domains:

- canvas authoring and versioning;
- execution engine and trigger admission;
- CDP/audience/tag domain;
- reach/notification/channel delivery;
- tenant/admin/identity;
- observability and operations.

## Acceptance Criteria

- A context map names each domain, owner, data ownership boundary, public API, and event contracts.
- The first migration step is reversible and does not require splitting all services at once.
- Cross-domain calls are classified as synchronous API, event, shared library, or prohibited coupling.
- Data ownership is explicit before any table or database is moved.
- Rollback, deployment order, and compatibility windows are documented before implementation.
