# Spec: Release And Deployment Governance

Source package: `docs/architecture/active/reviewed-packages/p1/release-deployment-governance/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`


## Verification Status

Implemented and locally verified on 2026-06-04.

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

- [x] Release assets are executable, not only prose.
- [x] Production profiles fail fast on unsafe config.
- [x] Database migration rollback and backup rules are documented and scripted.
- [x] CI validates backend, frontend, migration, container, and deployment config checks.
- [x] Emergency rollback is rehearsable from committed docs/scripts.

## Implemented Assets

- `.github/workflows/canvas-ci.yml`
- `scripts/release/validate-production-profile.sh`
- `scripts/release/check-flyway-migration.sh`
- `scripts/release/build-image.sh`
- `scripts/release/pre-deploy-check.sh`
- `scripts/release/post-deploy-check.sh`
- `scripts/release/rollback-drill.sh`
- `deploy/k8s/canvas-engine-deployment.yaml`
- `deploy/k8s/canvas-engine-service.yaml`
- `deploy/k8s/canvas-engine-hpa.yaml`
- `deploy/k8s/canvas-engine-network-policy.yaml`
- `docs/architecture/evidence/runbooks/flyway-backup-rollback.md`
- `docs/architecture/evidence/runbooks/release-deployment.md`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionProfileValidationTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/migration/FlywayMigrationPolicyTest.java`

## Verification Evidence

See `docs/architecture/evidence/P1-05-release-deployment-governance.md`.

Local verification passed for backend Maven tests, frontend tests/build, profile validation, Flyway migration policy, image dry-run, local Docker image build, pre-deploy dry-run, post-deploy dry-run, and rollback dry-run.

## Residual Production Gates

- Execute `.github/workflows/canvas-ci.yml` in GitHub Actions.
- Capture real database backup checksum and restore decision evidence.
- Run staging and production Kubernetes rollout with environment secrets.
- Run post-deploy checks with real `CANVAS_BASE_URL` and alert source.
