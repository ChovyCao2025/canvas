# Release And Deployment Governance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn release and deployment governance from checklist material into executable CI, migration, deployment, and rollback assets.

**Architecture:** Keep release governance in versioned scripts and runbooks that can be executed from a clean checkout. Validate backend, frontend, Flyway, container, profile, and deployment configuration in CI before production rollout, and require backup/rollback evidence for every migration-bearing release.

**Tech Stack:** Java 21, Maven, Node.js 18, Vite, Vitest, Flyway, Docker, GitHub Actions, shell scripts, Kubernetes/Helm documentation.

---

## Source Material

- Spec: `../specs/P1-05-release-deployment-governance-spec.md`
- Source package: `../../../todo/p1/release-deployment-governance/`
- Coverage matrix: `../../../todo/coverage-matrix.md`
- Archive checklist: `../archive/reviews/production-deployment-checklist-2026-06-02.md`
- K8s plan: `../archive/evolution/k8s-deployment-plan.md`

## File Structure

- CI: `.github/workflows/canvas-ci.yml`
- Script: `scripts/release/validate-production-profile.sh`
- Script: `scripts/release/check-flyway-migration.sh`
- Script: `scripts/release/build-image.sh`
- Script: `scripts/release/pre-deploy-check.sh`
- Script: `scripts/release/post-deploy-check.sh`
- Script: `scripts/release/rollback-drill.sh`
- Config: `backend/canvas-engine/src/main/resources/application-prod.yml`
- Config: `backend/canvas-engine/src/main/resources/application-staging.yml`
- Deployment: `deploy/k8s/canvas-engine-deployment.yaml`
- Deployment: `deploy/k8s/canvas-engine-service.yaml`
- Deployment: `deploy/k8s/canvas-engine-hpa.yaml`
- Deployment: `deploy/k8s/canvas-engine-network-policy.yaml`
- Runbook: `docs/architecture/runbooks/release-deployment.md`
- Runbook: `docs/architecture/runbooks/flyway-backup-rollback.md`
- Evidence: `docs/architecture/evidence/P1-05-release-deployment-governance.md`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionProfileValidationTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/migration/FlywayMigrationPolicyTest.java`

### Task 1: Inventory deployable assets and checklist-only gaps

**Files:**
- Create: `docs/architecture/evidence/P1-05-release-deployment-governance.md`
- Read: `docs/architecture/archive/reviews/production-deployment-checklist-2026-06-02.md`
- Read: `docs/architecture/archive/evolution/k8s-deployment-plan.md`
- Read: `.github/workflows/`
- Read: `deploy/`

- [x] Create an evidence table with columns: asset, current path, executable status, owner, missing command, and remediation task.
- [x] Classify CI, production profile, migration rollback, container build, staging deploy, post-deploy validation, and emergency rollback as executable or checklist-only.
- [x] Add a release-gate summary that names the first missing executable gate.

Run:

```bash
test -f docs/architecture/evidence/P1-05-release-deployment-governance.md
rg -n "CI|production profile|migration rollback|container build|staging deploy|post-deploy|emergency rollback" docs/architecture/evidence/P1-05-release-deployment-governance.md
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: evidence names which release assets are executable and which remain checklist-only.

### Task 2: Add production and staging profile validation

**Files:**
- Script: `scripts/release/validate-production-profile.sh`
- Config: `backend/canvas-engine/src/main/resources/application-prod.yml`
- Config: `backend/canvas-engine/src/main/resources/application-staging.yml`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionProfileValidationTest.java`

- [x] Add staging and production profile files with no root DB credentials, no weak JWT/event secrets, no wildcard credentialed CORS, and explicit actuator exposure.
- [x] Add `validate-production-profile.sh` to fail when required env vars are missing or unsafe defaults are present.
- [x] Add `ProductionProfileValidationTest` to enforce the same invariants in Maven.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=ProductionProfileValidationTest test
bash scripts/release/validate-production-profile.sh
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: Maven test and shell script fail for unsafe production profile values and pass for safe env-backed configuration.

### Task 3: Add Flyway backup and migration policy checks

**Files:**
- Script: `scripts/release/check-flyway-migration.sh`
- Runbook: `docs/architecture/runbooks/flyway-backup-rollback.md`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/migration/FlywayMigrationPolicyTest.java`
- Read: `backend/canvas-engine/src/main/resources/db/migration/`

- [x] Add a script that lists new Flyway migrations, verifies version monotonicity, and requires backup notes for destructive or high-risk changes.
- [x] Add a runbook with backup command, restore command, migration dry-run/check command, and rollback decision owner.
- [x] Add a test that scans migration filenames and rejects duplicate version numbers.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=FlywayMigrationPolicyTest test
bash scripts/release/check-flyway-migration.sh
rg -n "Backup|Restore|Dry run|Rollback owner" docs/architecture/runbooks/flyway-backup-rollback.md
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: migration policy test passes, script exits 0 for current migrations, and rollback runbook contains backup/restore/dry-run/owner sections.

### Task 4: Add CI pipeline checks

**Files:**
- CI: `.github/workflows/canvas-ci.yml`
- Script: `scripts/release/build-image.sh`
- Read: `backend/pom.xml`
- Read: `frontend/package.json`
- Read: `docker-compose.local.yml`

- [x] Add CI jobs for backend Maven tests, frontend tests, frontend build, profile validation, Flyway migration policy, and container build.
- [x] Keep backend commands rooted at `cd backend && mvn ...`.
- [x] Keep frontend commands rooted at `cd frontend && npm ...`.
- [x] Add `build-image.sh` so the CI container-build step has a local equivalent.

Run:

```bash
bash scripts/release/build-image.sh --dry-run
rg -n "mvn test|npm test|npm run build|validate-production-profile|check-flyway-migration|build-image" .github/workflows/canvas-ci.yml
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: workflow contains backend, frontend, profile, migration, and container gates; dry-run image build script exits 0.

### Task 5: Add deployment and rollback runbooks with commands

**Files:**
- Script: `scripts/release/pre-deploy-check.sh`
- Script: `scripts/release/post-deploy-check.sh`
- Script: `scripts/release/rollback-drill.sh`
- Deployment: `deploy/k8s/canvas-engine-deployment.yaml`
- Deployment: `deploy/k8s/canvas-engine-service.yaml`
- Deployment: `deploy/k8s/canvas-engine-hpa.yaml`
- Deployment: `deploy/k8s/canvas-engine-network-policy.yaml`
- Runbook: `docs/architecture/runbooks/release-deployment.md`

- [x] Create Kubernetes deployment, service, HPA, and network-policy templates for the engine with secret references instead of embedded secrets.
- [x] Add pre-deploy checks for image tag, migration backup, profile validation, and dependency reachability.
- [x] Add post-deploy checks for health, Prometheus, smoke test, and key runtime alerts.
- [x] Add rollback drill script with deployment rollback, database restore decision point, and evidence capture path.

Run:

```bash
bash scripts/release/pre-deploy-check.sh --dry-run
bash scripts/release/post-deploy-check.sh --dry-run
bash scripts/release/rollback-drill.sh --dry-run
rg -n "kubectl|helm|health|prometheus|rollback|evidence" docs/architecture/runbooks/release-deployment.md deploy/k8s
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: dry-run scripts exit 0, K8s templates avoid embedded secrets, and runbook includes deploy, post-deploy, rollback, and evidence commands.

### Task 6: Validate release governance end to end

**Files:**
- Evidence: `docs/architecture/evidence/P1-05-release-deployment-governance.md`
- Plan: `docs/architecture/archive/completed/plans/P1-05-release-deployment-governance-plan.md`
- Spec: `docs/architecture/archive/completed/specs/P1-05-release-deployment-governance-spec.md`

- [x] Run backend, frontend, profile, migration, and dry-run release commands.
- [x] Record command output summary, CI workflow path, scripts, runbooks, and rollback drill evidence in the evidence file.
- [x] Keep unresolved external environment assumptions in an explicit residual-risk table.

Run:

```bash
cd backend && mvn test
cd frontend && npm test
cd frontend && npm run build
bash scripts/release/validate-production-profile.sh
bash scripts/release/check-flyway-migration.sh
bash scripts/release/pre-deploy-check.sh --dry-run
bash scripts/release/post-deploy-check.sh --dry-run
bash scripts/release/rollback-drill.sh --dry-run
rg -n "CI workflow|rollback drill|residual risk|evidence" docs/architecture/evidence/P1-05-release-deployment-governance.md
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: local release-governance commands pass or evidence records a named residual risk with owner and next gate; spec and plan reflect executable release assets rather than checklist-only material.
