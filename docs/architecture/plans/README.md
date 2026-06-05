# Architecture Plans

This folder is the navigation entry for priority-prefixed implementation plans materialized from `docs/architecture/reviewed-packages/`.

Completed plan files are archived under `../archive/completed/plans/`.

The package plans use the `superpowers:writing-plans` structure: implementation header, source material, file structure, checkbox tasks, and verification steps.

## Active Decision Policy

Active decisions live in archived specs and plans, ADRs, guides, reference docs, and runbooks. Archived source reviews are historical evidence, not active implementation authority. Before implementation, cite the relevant source from an archived spec, archived plan, ADR, guide, reference doc, or runbook so each plan has current ownership, verification status, and rollback expectations.

## P3 Platform Evolution Entry Point

`../archive/completed/plans/P3-01-platform-evolution-plan.md` is the platform-evolution entry point. Focused P3 implementation remains blocked until `../work-products/p3-01-platform-evolution/platform-evolution-promotion-checklist.md` has owner, success metrics, migration, rollback, operations, security, compliance, tenant impact, team capacity, verification command, and evidence path filled for the promoted item.

| Priority | Plan | Spec |
|---|---|---|
| P0 | [P0-00-architecture-spec-plan-materialization-plan.md](../archive/completed/plans/P0-00-architecture-spec-plan-materialization-plan.md) | documentation generation plan |
| P0 | [P0-01-security-hardening-plan.md](../archive/completed/plans/P0-01-security-hardening-plan.md) | [spec](../archive/completed/specs/P0-01-security-hardening-spec.md) |
| P0 | [P0-02-reactive-threading-and-transactions-plan.md](../archive/completed/plans/P0-02-reactive-threading-and-transactions-plan.md) | [spec](../archive/completed/specs/P0-02-reactive-threading-and-transactions-spec.md) |
| P0 | [P0-03-canvas-state-data-consistency-plan.md](../archive/completed/plans/P0-03-canvas-state-data-consistency-plan.md) | [spec](../archive/completed/specs/P0-03-canvas-state-data-consistency-spec.md) |
| P0 | [P0-04-execution-concurrency-safety-plan.md](../archive/completed/plans/P0-04-execution-concurrency-safety-plan.md) | [spec](../archive/completed/specs/P0-04-execution-concurrency-safety-spec.md) |
| P0 | [P0-05-production-resilience-and-dr-plan.md](../archive/completed/plans/P0-05-production-resilience-and-dr-plan.md) | [spec](../archive/completed/specs/P0-05-production-resilience-and-dr-spec.md) |
| P0 | [P0-06-data-security-and-tenant-isolation-plan.md](../archive/completed/plans/P0-06-data-security-and-tenant-isolation-plan.md) | [spec](../archive/completed/specs/P0-06-data-security-and-tenant-isolation-spec.md) |
| P1 | [P1-01-dag-engine-and-handler-boundaries-plan.md](../archive/completed/plans/P1-01-dag-engine-and-handler-boundaries-plan.md) | [spec](../archive/completed/specs/P1-01-dag-engine-and-handler-boundaries-spec.md) |
| P1 | [P1-02-api-contract-and-validation-plan.md](../archive/completed/plans/P1-02-api-contract-and-validation-plan.md) | [spec](../archive/completed/specs/P1-02-api-contract-and-validation-spec.md) |
| P1 | [P1-03-frontend-canvas-state-plan.md](../archive/completed/plans/P1-03-frontend-canvas-state-plan.md) | [spec](../archive/completed/specs/P1-03-frontend-canvas-state-spec.md) |
| P1 | [P1-04-observability-and-ops-plan.md](../archive/completed/plans/P1-04-observability-and-ops-plan.md) | [spec](../archive/completed/specs/P1-04-observability-and-ops-spec.md) |
| P1 | [P1-05-release-deployment-governance-plan.md](../archive/completed/plans/P1-05-release-deployment-governance-plan.md) | [spec](../archive/completed/specs/P1-05-release-deployment-governance-spec.md) |
| P2 | [P2-01-testing-foundation-plan.md](../archive/completed/plans/P2-01-testing-foundation-plan.md) | [spec](../archive/completed/specs/P2-01-testing-foundation-spec.md) |
| P2 | [P2-02-cost-capacity-and-retention-plan.md](../archive/completed/plans/P2-02-cost-capacity-and-retention-plan.md) | [spec](../archive/completed/specs/P2-02-cost-capacity-and-retention-spec.md) |
| P2 | [P2-03-documentation-adr-and-runbooks-plan.md](../archive/completed/plans/P2-03-documentation-adr-and-runbooks-plan.md) | [spec](../archive/completed/specs/P2-03-documentation-adr-and-runbooks-spec.md) |
| P2 | [P2-04-dependency-abstraction-and-vendor-lock-in-plan.md](../archive/completed/plans/P2-04-dependency-abstraction-and-vendor-lock-in-plan.md) | [spec](../archive/completed/specs/P2-04-dependency-abstraction-and-vendor-lock-in-spec.md) |
| P2 | [P2-05-compliance-data-governance-plan.md](../archive/completed/plans/P2-05-compliance-data-governance-plan.md) | [spec](../archive/completed/specs/P2-05-compliance-data-governance-spec.md) |
| P2 | [P2-06-frontend-accessibility-and-quality-plan.md](../archive/completed/plans/P2-06-frontend-accessibility-and-quality-plan.md) | [spec](../archive/completed/specs/P2-06-frontend-accessibility-and-quality-spec.md) |
| P3 | [P3-00-architecture-boundary-review-plan.md](../archive/completed/plans/P3-00-architecture-boundary-review-plan.md) | [spec](../archive/completed/specs/P3-00-architecture-boundary-review-spec.md) |
| P3 | [P3-01-platform-evolution-plan.md](../archive/completed/plans/P3-01-platform-evolution-plan.md) | [spec](../archive/completed/specs/P3-01-platform-evolution-spec.md) |
| P3 | [P3-02-service-decomposition-and-domain-boundaries-plan.md](../archive/completed/plans/P3-02-service-decomposition-and-domain-boundaries-plan.md) | [spec](../archive/completed/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md) |
| P3 | [P3-03-data-platform-architecture-plan.md](../archive/completed/plans/P3-03-data-platform-architecture-plan.md) | [spec](../archive/completed/specs/P3-03-data-platform-architecture-spec.md) |
| P3 | [P3-04-multi-datasource-isolation-plan.md](../archive/completed/plans/P3-04-multi-datasource-isolation-plan.md) | [spec](../archive/completed/specs/P3-04-multi-datasource-isolation-spec.md) |
| P3 | [P3-05-webflux-to-mvc-migration-plan.md](../archive/completed/plans/P3-05-webflux-to-mvc-migration-plan.md) | [spec](../archive/completed/specs/P3-05-webflux-to-mvc-migration-spec.md) |
| P3 | [P3-06-k8s-deployment-platform-plan.md](../archive/completed/plans/P3-06-k8s-deployment-platform-plan.md) | [spec](../archive/completed/specs/P3-06-k8s-deployment-platform-spec.md) |
| P3 | [P3-07-production-platform-components-plan.md](../archive/completed/plans/P3-07-production-platform-components-plan.md) | [spec](../archive/completed/specs/P3-07-production-platform-components-spec.md) |
| P3 | [P3-08-wecom-scrm-module-plan.md](../archive/completed/plans/P3-08-wecom-scrm-module-plan.md) | [spec](../archive/completed/specs/P3-08-wecom-scrm-module-spec.md) |
| P3 | [P3-09-identity-event-and-tenant-platform-plan.md](../archive/completed/plans/P3-09-identity-event-and-tenant-platform-plan.md) | [spec](../archive/completed/specs/P3-09-identity-event-and-tenant-platform-spec.md) |
