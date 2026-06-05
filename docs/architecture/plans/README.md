# Architecture Plans

This folder contains priority-prefixed implementation plans materialized from `docs/architecture/todo/`.

The package plans use the `superpowers:writing-plans` structure: implementation header, source material, file structure, checkbox tasks, and verification steps.

## Active Decision Policy

Active decisions live in specs, plans, ADRs, guides, reference docs, and runbooks. Archived reviews are historical evidence, not active implementation authority. Before implementation, cite the relevant archive source from an active spec, plan, ADR, guide, reference doc, or runbook so each plan has current ownership, verification status, and rollback expectations.

## P3 Platform Evolution Entry Point

`P3-01-platform-evolution-plan.md` is the platform-evolution entry point. Focused P3 implementation remains blocked until `../platform-evolution-promotion-checklist.md` has owner, success metrics, migration, rollback, operations, security, compliance, tenant impact, team capacity, verification command, and evidence path filled for the promoted item.

| Priority | Plan | Spec |
|---|---|---|
| P0 | [P0-00-architecture-spec-plan-materialization-plan.md](P0-00-architecture-spec-plan-materialization-plan.md) | documentation generation plan |
| P0 | [P0-01-security-hardening-plan.md](P0-01-security-hardening-plan.md) | [spec](../specs/P0-01-security-hardening-spec.md) |
| P0 | [P0-02-reactive-threading-and-transactions-plan.md](P0-02-reactive-threading-and-transactions-plan.md) | [spec](../specs/P0-02-reactive-threading-and-transactions-spec.md) |
| P0 | [P0-03-canvas-state-data-consistency-plan.md](P0-03-canvas-state-data-consistency-plan.md) | [spec](../specs/P0-03-canvas-state-data-consistency-spec.md) |
| P0 | [P0-04-execution-concurrency-safety-plan.md](P0-04-execution-concurrency-safety-plan.md) | [spec](../specs/P0-04-execution-concurrency-safety-spec.md) |
| P0 | [P0-05-production-resilience-and-dr-plan.md](P0-05-production-resilience-and-dr-plan.md) | [spec](../specs/P0-05-production-resilience-and-dr-spec.md) |
| P0 | [P0-06-data-security-and-tenant-isolation-plan.md](P0-06-data-security-and-tenant-isolation-plan.md) | [spec](../specs/P0-06-data-security-and-tenant-isolation-spec.md) |
| P1 | [P1-01-dag-engine-and-handler-boundaries-plan.md](P1-01-dag-engine-and-handler-boundaries-plan.md) | [spec](../specs/P1-01-dag-engine-and-handler-boundaries-spec.md) |
| P1 | [P1-02-api-contract-and-validation-plan.md](P1-02-api-contract-and-validation-plan.md) | [spec](../specs/P1-02-api-contract-and-validation-spec.md) |
| P1 | [P1-03-frontend-canvas-state-plan.md](P1-03-frontend-canvas-state-plan.md) | [spec](../specs/P1-03-frontend-canvas-state-spec.md) |
| P1 | [P1-04-observability-and-ops-plan.md](P1-04-observability-and-ops-plan.md) | [spec](../specs/P1-04-observability-and-ops-spec.md) |
| P1 | [P1-05-release-deployment-governance-plan.md](P1-05-release-deployment-governance-plan.md) | [spec](../specs/P1-05-release-deployment-governance-spec.md) |
| P2 | [P2-01-testing-foundation-plan.md](P2-01-testing-foundation-plan.md) | [spec](../specs/P2-01-testing-foundation-spec.md) |
| P2 | [P2-02-cost-capacity-and-retention-plan.md](P2-02-cost-capacity-and-retention-plan.md) | [spec](../specs/P2-02-cost-capacity-and-retention-spec.md) |
| P2 | [P2-03-documentation-adr-and-runbooks-plan.md](P2-03-documentation-adr-and-runbooks-plan.md) | [spec](../specs/P2-03-documentation-adr-and-runbooks-spec.md) |
| P2 | [P2-04-dependency-abstraction-and-vendor-lock-in-plan.md](P2-04-dependency-abstraction-and-vendor-lock-in-plan.md) | [spec](../specs/P2-04-dependency-abstraction-and-vendor-lock-in-spec.md) |
| P2 | [P2-05-compliance-data-governance-plan.md](P2-05-compliance-data-governance-plan.md) | [spec](../specs/P2-05-compliance-data-governance-spec.md) |
| P2 | [P2-06-frontend-accessibility-and-quality-plan.md](P2-06-frontend-accessibility-and-quality-plan.md) | [spec](../specs/P2-06-frontend-accessibility-and-quality-spec.md) |
| P3 | [P3-00-architecture-boundary-review-plan.md](P3-00-architecture-boundary-review-plan.md) | [spec](../specs/P3-00-architecture-boundary-review-spec.md) |
| P3 | [P3-01-platform-evolution-plan.md](P3-01-platform-evolution-plan.md) | [spec](../specs/P3-01-platform-evolution-spec.md) |
| P3 | [P3-02-service-decomposition-and-domain-boundaries-plan.md](P3-02-service-decomposition-and-domain-boundaries-plan.md) | [spec](../specs/P3-02-service-decomposition-and-domain-boundaries-spec.md) |
| P3 | [P3-03-data-platform-architecture-plan.md](P3-03-data-platform-architecture-plan.md) | [spec](../specs/P3-03-data-platform-architecture-spec.md) |
| P3 | [P3-04-multi-datasource-isolation-plan.md](P3-04-multi-datasource-isolation-plan.md) | [spec](../specs/P3-04-multi-datasource-isolation-spec.md) |
| P3 | [P3-05-webflux-to-mvc-migration-plan.md](P3-05-webflux-to-mvc-migration-plan.md) | [spec](../specs/P3-05-webflux-to-mvc-migration-spec.md) |
| P3 | [P3-06-k8s-deployment-platform-plan.md](P3-06-k8s-deployment-platform-plan.md) | [spec](../specs/P3-06-k8s-deployment-platform-spec.md) |
| P3 | [P3-07-production-platform-components-plan.md](P3-07-production-platform-components-plan.md) | [spec](../specs/P3-07-production-platform-components-spec.md) |
| P3 | [P3-08-wecom-scrm-module-plan.md](P3-08-wecom-scrm-module-plan.md) | [spec](../specs/P3-08-wecom-scrm-module-spec.md) |
| P3 | [P3-09-identity-event-and-tenant-platform-plan.md](P3-09-identity-event-and-tenant-platform-plan.md) | [spec](../specs/P3-09-identity-event-and-tenant-platform-spec.md) |
