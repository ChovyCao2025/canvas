# P1-05 Release Deployment Governance Evidence

## Asset Inventory

| Asset | Current path | Executable status | Owner | Missing command | Remediation task |
| --- | --- | --- | --- | --- | --- |
| CI | `.github/workflows/canvas-ci.yml` | Executable | Platform | None locally; GitHub run pending | Task 4 |
| production profile | `backend/canvas-engine/src/main/resources/application-prod.yml` | Executable | Backend | `bash scripts/release/validate-production-profile.sh` | Task 2 |
| staging profile | `backend/canvas-engine/src/main/resources/application-staging.yml` | Executable | Backend | `bash scripts/release/validate-production-profile.sh` | Task 2 |
| migration rollback | `backend/canvas-engine/src/main/resources/db/migration/` and `docs/architecture/evidence/migrations/` | Executable policy; real backup remains environment-specific | Backend/DBA | `bash scripts/release/check-flyway-migration.sh` | Task 3 |
| container build | `scripts/release/build-image.sh` | Executable | Platform | `CANVAS_IMAGE_TAG=<tag> bash scripts/release/build-image.sh` | Task 4 |
| staging deploy | `deploy/k8s/` and `scripts/release/pre-deploy-check.sh` | Executable template and dry-run gate | Platform | `bash scripts/release/pre-deploy-check.sh --dry-run` | Task 5 |
| post-deploy validation | `scripts/release/post-deploy-check.sh` | Executable | Runtime | `CANVAS_BASE_URL=<url> bash scripts/release/post-deploy-check.sh` | Task 5 |
| emergency rollback | `scripts/release/rollback-drill.sh` and `docs/architecture/evidence/runbooks/release-deployment.md` | Executable drill | Runtime/DBA | `bash scripts/release/rollback-drill.sh --dry-run` | Task 5 |

## Classification

- CI: executable workflow with backend, frontend, profile, Flyway, deployment dry-run, and container build gates.
- production profile: executable validator and Maven test enforce env-backed production-like settings.
- migration rollback: executable policy checks duplicate versions, current baseline, and high-risk notes for new migrations after V185.
- container build: executable local build script with dry-run and real Docker build path.
- staging deploy: executable Kubernetes templates and pre-deploy dry-run gate.
- post-deploy validation: executable health, Prometheus, smoke, and alert-source checks.
- emergency rollback: executable rollback drill with evidence capture and database restore decision point.

## Release Gate Summary

First missing executable gate: none in committed release-governance assets.

Production deployment remains blocked until environment-specific evidence exists for registry push, database backup checksum, Kubernetes rollout, post-deploy health/Prometheus checks, and rollback owner approval.

## Implemented Assets

- CI workflow: `.github/workflows/canvas-ci.yml`
- Profile validator: `scripts/release/validate-production-profile.sh`
- Migration policy: `scripts/release/check-flyway-migration.sh`
- Migration baseline: `docs/architecture/evidence/migrations/released-baseline.version`
- Image build: `scripts/release/build-image.sh`
- Pre-deploy check: `scripts/release/pre-deploy-check.sh`
- Post-deploy check: `scripts/release/post-deploy-check.sh`
- Rollback drill: `scripts/release/rollback-drill.sh`
- K8s templates: `deploy/k8s/canvas-engine-deployment.yaml`, `deploy/k8s/canvas-engine-service.yaml`, `deploy/k8s/canvas-engine-hpa.yaml`, `deploy/k8s/canvas-engine-network-policy.yaml`
- Runbooks: `docs/architecture/evidence/runbooks/flyway-backup-rollback.md`, `docs/architecture/evidence/runbooks/release-deployment.md`
- Maven tests: `ProductionProfileValidationTest`, `FlywayMigrationPolicyTest`

## Verification

Date: 2026-06-04

| Command | Result | Notes |
| --- | --- | --- |
| `bash scripts/release/validate-production-profile.sh` | Pass | Production and staging profiles are env-backed and safe by static policy. |
| `bash scripts/release/check-flyway-migration.sh` | Pass | Baseline V185, highest V185, 119 migrations, 0 new migrations. |
| `bash scripts/release/build-image.sh --dry-run` | Pass | Prints Maven package and Docker build commands. |
| `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH CANVAS_IMAGE_TAG=p1-05-local bash scripts/release/build-image.sh` | Pass | Built local Docker image `canvas-engine:p1-05-local`. |
| `bash scripts/release/pre-deploy-check.sh --dry-run` | Pass | Runs profile and Flyway gates, then prints required deployment env. |
| `bash scripts/release/post-deploy-check.sh --dry-run` | Pass | Prints health, Prometheus, smoke, and alert-source checks. |
| `bash scripts/release/rollback-drill.sh --dry-run` | Pass | Prints rollout history, undo, status, evidence path, and DB restore decision point. |
| `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=ProductionProfileValidationTest,FlywayMigrationPolicyTest test` | Pass | 2 tests passed. |
| `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn test` | Pass | 704 tests, 0 failures, 0 errors, 1 skipped. |
| `cd frontend && npm test` | Pass | 55 test files, 201 tests passed. |
| `cd frontend && npm run build` | Pass | TypeScript and Vite production build passed. |
| `rg -n "mvn test|npm test|npm run build|validate-production-profile|check-flyway-migration|pre-deploy-check|post-deploy-check|rollback-drill|build-image" .github/workflows/canvas-ci.yml` | Pass | CI contains backend, frontend, profile, migration, deployment, and image gates. |
| `rg -n "Backup|Restore|Dry run|Rollback owner" docs/architecture/evidence/runbooks/flyway-backup-rollback.md` | Pass | Flyway runbook contains required sections. |
| `rg -n "kubectl|helm|health|prometheus|rollback|evidence" docs/architecture/evidence/runbooks/release-deployment.md deploy/k8s` | Pass | Deployment runbook and templates contain required operational commands. |

## Residual Risks

| Risk | Owner | Next gate |
| --- | --- | --- |
| GitHub Actions workflow was updated but not executed in this local run. | Platform | Confirm the next PR or push run for `.github/workflows/canvas-ci.yml`. |
| Actual production/staging Kubernetes rollout was not executed locally. | Platform | Run `scripts/release/pre-deploy-check.sh`, apply `deploy/k8s`, then capture `kubectl rollout status`. |
| Production database backup and restore were not executed against a real database. | DBA/Runtime | Capture backup checksum and restore decision evidence before release. |
| Alertmanager/Prometheus alert-source check was dry-run only. | Runtime | Set `CANVAS_ALERTS_URL` and run `scripts/release/post-deploy-check.sh` after staging deploy. |
| Image push to registry was not executed locally. | Platform | Run `scripts/release/build-image.sh --push` with release registry credentials. |
