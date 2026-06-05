# Architecture Specs

This folder is the navigation entry for flattened, priority-prefixed specs materialized from `docs/architecture/active/reviewed-packages/`.

Completed spec files are archived under `../archive/completed/specs/`.

For source-to-package traceability, keep using `../reviewed-packages/coverage-matrix.md`.

Supporting verification artifacts:

- [P3-00-architecture-boundary-code-verification.md](../../archive/completed/specs/P3-00-architecture-boundary-code-verification.md): code-level validation for the architecture boundary review and service extraction order.

## Active Decision Policy

Active decisions live in archived specs and plans, ADRs, guides, reference docs, and runbooks. Archived source reviews are historical evidence, not active implementation authority. Before implementation, cite the relevant source from an archived spec, archived plan, ADR, guide, reference doc, or runbook so the current owner, verification status, and rollback path are clear.

| Priority | Spec | Plan |
|---|---|---|
| P0 | [P0-01-security-hardening-spec.md](../../archive/completed/specs/P0-01-security-hardening-spec.md) | [plan](../../archive/completed/plans/P0-01-security-hardening-plan.md) |
| P0 | [P0-02-reactive-threading-and-transactions-spec.md](../../archive/completed/specs/P0-02-reactive-threading-and-transactions-spec.md) | [plan](../../archive/completed/plans/P0-02-reactive-threading-and-transactions-plan.md) |
| P0 | [P0-03-canvas-state-data-consistency-spec.md](../../archive/completed/specs/P0-03-canvas-state-data-consistency-spec.md) | [plan](../../archive/completed/plans/P0-03-canvas-state-data-consistency-plan.md) |
| P0 | [P0-04-execution-concurrency-safety-spec.md](../../archive/completed/specs/P0-04-execution-concurrency-safety-spec.md) | [plan](../../archive/completed/plans/P0-04-execution-concurrency-safety-plan.md) |
| P0 | [P0-05-production-resilience-and-dr-spec.md](../../archive/completed/specs/P0-05-production-resilience-and-dr-spec.md) | [plan](../../archive/completed/plans/P0-05-production-resilience-and-dr-plan.md) |
| P0 | [P0-06-data-security-and-tenant-isolation-spec.md](../../archive/completed/specs/P0-06-data-security-and-tenant-isolation-spec.md) | [plan](../../archive/completed/plans/P0-06-data-security-and-tenant-isolation-plan.md) |
| P1 | [P1-01-dag-engine-and-handler-boundaries-spec.md](../../archive/completed/specs/P1-01-dag-engine-and-handler-boundaries-spec.md) | [plan](../../archive/completed/plans/P1-01-dag-engine-and-handler-boundaries-plan.md) |
| P1 | [P1-02-api-contract-and-validation-spec.md](../../archive/completed/specs/P1-02-api-contract-and-validation-spec.md) | [plan](../../archive/completed/plans/P1-02-api-contract-and-validation-plan.md) |
| P1 | [P1-03-frontend-canvas-state-spec.md](../../archive/completed/specs/P1-03-frontend-canvas-state-spec.md) | [plan](../../archive/completed/plans/P1-03-frontend-canvas-state-plan.md) |
| P1 | [P1-04-observability-and-ops-spec.md](../../archive/completed/specs/P1-04-observability-and-ops-spec.md) | [plan](../../archive/completed/plans/P1-04-observability-and-ops-plan.md) |
| P1 | [P1-05-release-deployment-governance-spec.md](../../archive/completed/specs/P1-05-release-deployment-governance-spec.md) | [plan](../../archive/completed/plans/P1-05-release-deployment-governance-plan.md) |
| P2 | [P2-01-testing-foundation-spec.md](../../archive/completed/specs/P2-01-testing-foundation-spec.md) | [plan](../../archive/completed/plans/P2-01-testing-foundation-plan.md) |
| P2 | [P2-02-cost-capacity-and-retention-spec.md](../../archive/completed/specs/P2-02-cost-capacity-and-retention-spec.md) | [plan](../../archive/completed/plans/P2-02-cost-capacity-and-retention-plan.md) |
| P2 | [P2-03-documentation-adr-and-runbooks-spec.md](../../archive/completed/specs/P2-03-documentation-adr-and-runbooks-spec.md) | [plan](../../archive/completed/plans/P2-03-documentation-adr-and-runbooks-plan.md) |
| P2 | [P2-04-dependency-abstraction-and-vendor-lock-in-spec.md](../../archive/completed/specs/P2-04-dependency-abstraction-and-vendor-lock-in-spec.md) | [plan](../../archive/completed/plans/P2-04-dependency-abstraction-and-vendor-lock-in-plan.md) |
| P2 | [P2-05-compliance-data-governance-spec.md](../../archive/completed/specs/P2-05-compliance-data-governance-spec.md) | [plan](../../archive/completed/plans/P2-05-compliance-data-governance-plan.md) |
| P2 | [P2-06-frontend-accessibility-and-quality-spec.md](../../archive/completed/specs/P2-06-frontend-accessibility-and-quality-spec.md) | [plan](../../archive/completed/plans/P2-06-frontend-accessibility-and-quality-plan.md) |
| P3 | [P3-00-architecture-boundary-review-spec.md](../../archive/completed/specs/P3-00-architecture-boundary-review-spec.md) | [plan](../../archive/completed/plans/P3-00-architecture-boundary-review-plan.md) |
| P3 | [P3-01-platform-evolution-spec.md](../../archive/completed/specs/P3-01-platform-evolution-spec.md) | [plan](../../archive/completed/plans/P3-01-platform-evolution-plan.md) |
| P3 | [P3-02-service-decomposition-and-domain-boundaries-spec.md](../../archive/completed/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md) | [plan](../../archive/completed/plans/P3-02-service-decomposition-and-domain-boundaries-plan.md) |
| P3 | [P3-03-data-platform-architecture-spec.md](../../archive/completed/specs/P3-03-data-platform-architecture-spec.md) | [plan](../../archive/completed/plans/P3-03-data-platform-architecture-plan.md) |
| P3 | [P3-04-multi-datasource-isolation-spec.md](../../archive/completed/specs/P3-04-multi-datasource-isolation-spec.md) | [plan](../../archive/completed/plans/P3-04-multi-datasource-isolation-plan.md) |
| P3 | [P3-05-webflux-to-mvc-migration-spec.md](../../archive/completed/specs/P3-05-webflux-to-mvc-migration-spec.md) | [plan](../../archive/completed/plans/P3-05-webflux-to-mvc-migration-plan.md) |
| P3 | [P3-06-k8s-deployment-platform-spec.md](../../archive/completed/specs/P3-06-k8s-deployment-platform-spec.md) | [plan](../../archive/completed/plans/P3-06-k8s-deployment-platform-plan.md) |
| P3 | [P3-07-production-platform-components-spec.md](../../archive/completed/specs/P3-07-production-platform-components-spec.md) | [plan](../../archive/completed/plans/P3-07-production-platform-components-plan.md) |
| P3 | [P3-08-wecom-scrm-module-spec.md](../../archive/completed/specs/P3-08-wecom-scrm-module-spec.md) | [plan](../../archive/completed/plans/P3-08-wecom-scrm-module-plan.md) |
| P3 | [P3-09-identity-event-and-tenant-platform-spec.md](../../archive/completed/specs/P3-09-identity-event-and-tenant-platform-spec.md) | [plan](../../archive/completed/plans/P3-09-identity-event-and-tenant-platform-plan.md) |
