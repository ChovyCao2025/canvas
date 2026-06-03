# Spec: K8s Deployment Platform

Source package: `docs/architecture/todo/p3/platform-evolution/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`

## Verification Status

Planning material. Current release/deployment governance remains P1; Kubernetes platformization is P3 until production topology, ownership, and SLOs are clear.

## Source Documents

- `docs/architecture/archive/evolution/k8s-deployment-plan.md`
- `docs/architecture/archive/reviews/production-deployment-checklist-2026-06-02.md`
- `docs/architecture/archive/reference/deployment-guide.md`

## Scope

Define deployable Kubernetes assets and operating model for backend, frontend, Redis/RocketMQ/MySQL dependencies, configuration, secrets, autoscaling, observability, and rollback.

## Acceptance Criteria

- Helm or equivalent manifests are versioned and environment-aware.
- Probes, resources, HPA, secrets, config, and ingress are defined.
- Stateful dependencies are either managed services or explicitly owned with HA and backup plans.
- CI/CD includes image build, scan, migration gate, smoke test, and rollback.
- Observability includes logs, metrics, traces, dashboards, and alert ownership.
