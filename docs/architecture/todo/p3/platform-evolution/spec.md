# Spec: Platform Evolution

## Verification Status

Split into focused P3 decision packages. These are architecture decisions and implementation gates, not immediate verified implementation defects.

## Content Group

The previous `evolution/` documents describe long-term target architecture and have been split into active P3 specs, plans, evidence, ADRs, and runbooks:

- service split: `docs/architecture/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`, `docs/architecture/plans/P3-02-service-decomposition-and-domain-boundaries-plan.md`;
- data platform: `docs/architecture/specs/P3-03-data-platform-architecture-spec.md`, `docs/architecture/plans/P3-03-data-platform-architecture-plan.md`;
- multi-datasource isolation: `docs/architecture/specs/P3-04-multi-datasource-isolation-spec.md`, `docs/architecture/plans/P3-04-multi-datasource-isolation-plan.md`;
- WebFlux to MVC migration: `docs/architecture/specs/P3-05-webflux-to-mvc-migration-spec.md`, `docs/architecture/plans/P3-05-webflux-to-mvc-migration-plan.md`;
- K8s deployment: `docs/architecture/specs/P3-06-k8s-deployment-platform-spec.md`, `docs/architecture/plans/P3-06-k8s-deployment-platform-plan.md`;
- production platform components: `docs/architecture/specs/P3-07-production-platform-components-spec.md`, `docs/architecture/plans/P3-07-production-platform-components-plan.md`;
- WeCom SCRM module: `docs/architecture/specs/P3-08-wecom-scrm-module-spec.md`, `docs/architecture/plans/P3-08-wecom-scrm-module-plan.md`;
- identity, event, and tenant platform primitives: `docs/architecture/specs/P3-09-identity-event-and-tenant-platform-spec.md`, `docs/architecture/plans/P3-09-identity-event-and-tenant-platform-plan.md`.

These are not immediate bug-fix todo items. They should remain P3 until P0/P1 risks are reduced and product/team capacity is confirmed.

## Acceptance Criteria

- Evolution work is only promoted when it has an owner, success metrics, migration strategy, rollback plan, and dependency analysis.
- No large migration starts before security, consistency, and observability P0/P1 packages are addressed or explicitly accepted as risks.
