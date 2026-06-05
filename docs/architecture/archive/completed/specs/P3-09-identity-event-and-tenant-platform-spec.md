# Spec: Identity, Event, And Tenant Platform

Source package: `docs/architecture/reviewed-packages/p3/platform-evolution/`

Coverage matrix: `docs/architecture/reviewed-packages/coverage-matrix.md`

## Verification Status

Implemented as shared platform primitive contracts. No identity service, event platform, or engine/web service extraction is approved yet; these contracts are prerequisites. Evidence and contracts are in `docs/architecture/evidence/p3-09-platform-primitives.md`, `docs/architecture/work-products/p3-09-platform-primitives/platform-primitives.md`, `docs/architecture/work-products/p3-09-platform-primitives/tenant-platform-contract.md`, `docs/architecture/work-products/p3-09-platform-primitives/event-schema-governance.md`, and `docs/architecture/work-products/p3-09-platform-primitives/engine-web-boundary.md`.

## Source Documents

- `docs/architecture/archive/evolution/architect-critical-review.md`
- `docs/architecture/archive/evolution/target-architecture-overview.md`

## Scope

Define shared platform primitives that future service splits and data platform work depend on: OneID identity mapping, versioned event schema, tenant quota and visibility model, service degradation modes, engine/web separation, and strangler migration rules.

## Acceptance Criteria

- OneID mapping has canonical ID rules, source identity rules, merge/split behavior, and auditability.
- Event schemas are versioned and include compatibility, replay, ordering, and ownership rules.
- Tenant quotas and visibility are enforced through platform contracts rather than ad hoc query wrappers.
- Degradation modes define fail-open/fail-closed behavior for external dependencies.
- Engine/web separation has clear API, event, data, and deployment boundaries.
