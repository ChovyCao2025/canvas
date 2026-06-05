# Spec: Platform Evolution

Source package: `docs/architecture/active/reviewed-packages/p3/platform-evolution/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`


## Verification Status

Implemented as a governance/documentation package. Evidence, promotion gates, and focused P3 links are recorded in `docs/architecture/evidence/p3-01-platform-evolution.md`, `docs/architecture/decisions/work-products/p3-01-platform-evolution/platform-evolution-promotion-checklist.md`, `docs/architecture/active/plans/README.md`, and `docs/architecture/active/reviewed-packages/coverage-matrix.md`.

## Content Group

This spec is the umbrella for long-term target architecture. Boundary decisions are controlled by `P3-00-architecture-boundary-review-spec.md`. The previous `evolution/` documents have now been split into focused P3 specs:

- `P3-02-service-decomposition-and-domain-boundaries-spec.md`
- `P3-03-data-platform-architecture-spec.md`
- `P3-04-multi-datasource-isolation-spec.md`
- `P3-05-webflux-to-mvc-migration-spec.md`
- `P3-06-k8s-deployment-platform-spec.md`
- `P3-07-production-platform-components-spec.md`
- `P3-08-wecom-scrm-module-spec.md`
- `P3-09-identity-event-and-tenant-platform-spec.md`

These are not immediate bug-fix tasks. They should remain P3 until P0/P1 risks are reduced and product/team capacity is confirmed.

## Acceptance Criteria

- Evolution work is only promoted when it has an owner, success metrics, migration strategy, rollback plan, and dependency analysis.
- No large migration starts before security, consistency, and observability P0/P1 packages are addressed or explicitly accepted as risks.
