# Spec: Release And Deployment Governance

Source package: `docs/architecture/todo/p1/release-deployment-governance/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Partially confirmed.

## Problems Covered

- Production deployment material exists mostly as checklist or proposed YAML, not as a complete executable release system.
- Flyway rollback strategy is documented as missing in the remediation material.
- CI/CD, image scanning, environment profile enforcement, staging deployment, and rollback runbooks are scattered across archived docs.

## Source Coverage

- `archive/remediation/part4-ops.md`: production deployment, config management, RocketMQ config, Flyway rollback.
- `archive/reviews/production-deployment-checklist-2026-06-02.md`: deployment checklist, rollback, post-deploy checks, KPI monitoring.
- `archive/reviews/brownfield-architecture.md`: enhancement deployment, rollback strategy, CI proposal.
- `archive/evolution/k8s-deployment-plan.md`: Helm/K8s deployment plan, HPA, secrets, network policy, CI/CD.

## Acceptance Criteria

- Release assets are executable, not only prose.
- Production profiles fail fast on unsafe config.
- Database migration rollback and backup rules are documented and scripted.
- CI validates backend, frontend, migration, container, and deployment config checks.
- Emergency rollback is rehearsable from committed docs/scripts.
