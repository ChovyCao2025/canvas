# K8s Deployment Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the Kubernetes deployment proposal into environment-aware, renderable, and rollback-ready platform assets.

**Architecture:** Build deployment assets around explicit ownership and SLOs. Treat MySQL, Redis, and RocketMQ as managed dependencies unless an operating model assigns ownership, backup, HA, and restore responsibilities. Per `../specs/P3-00-architecture-boundary-review-spec.md`, Kubernetes topology is a platform deployment decision and must not define domain or service boundaries.

**Tech Stack:** Kubernetes, Helm, Docker, Spring Boot actuator, Prometheus, Grafana, GitHub Actions, shell validation.

---

## Source Material

- Spec: `../specs/P3-06-k8s-deployment-platform-spec.md`
- Boundary review: `../specs/P3-00-architecture-boundary-review-spec.md`
- Boundary evidence: `../evidence/p3-00-architecture-boundary-review.md`
- Evolution doc: `../archive/evolution/k8s-deployment-plan.md`
- Coverage matrix: `../../../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/evidence/p3-06-k8s-platform.md`
- Create: `docs/architecture/k8s-operating-model.md`
- Create: `deploy/helm/canvas/Chart.yaml`
- Create: `deploy/helm/canvas/values.yaml`
- Create: `deploy/helm/canvas/values-staging.yaml`
- Create: `deploy/helm/canvas/values-prod.yaml`
- Create: `deploy/helm/canvas/templates/deployment.yaml`
- Create: `deploy/helm/canvas/templates/service.yaml`
- Create: `deploy/helm/canvas/templates/ingress.yaml`
- Create: `deploy/helm/canvas/templates/configmap.yaml`
- Create: `deploy/helm/canvas/templates/secret-ref.yaml`
- Create: `docs/architecture/runbooks/k8s-rollout-rollback.md`

### Task 1: Define Operating Model

**Files:**
- Create: `docs/architecture/k8s-operating-model.md`
- Create: `docs/architecture/evidence/p3-06-k8s-platform.md`

- [x] Name owners for MySQL, Redis, RocketMQ, ingress, monitoring, secrets, image registry, and cluster upgrades.
- [x] Define dev, staging, and production SLO/resource differences.
- [x] Define which dependencies are managed services and which are self-operated.

Run:

```bash
test -f docs/architecture/k8s-operating-model.md
rg -n "MySQL|Redis|RocketMQ|Ingress|Monitoring|Secrets|SLO|staging|production|managed service|self-operated" docs/architecture/k8s-operating-model.md
```

Expected: operating model names dependency owners, environments, SLOs, and operating responsibility.

### Task 2: Scaffold Renderable Helm Assets

**Files:**
- Create: `deploy/helm/canvas/Chart.yaml`
- Create: `deploy/helm/canvas/values.yaml`
- Create: `deploy/helm/canvas/values-staging.yaml`
- Create: `deploy/helm/canvas/values-prod.yaml`
- Create: `deploy/helm/canvas/templates/deployment.yaml`
- Create: `deploy/helm/canvas/templates/service.yaml`
- Create: `deploy/helm/canvas/templates/ingress.yaml`
- Create: `deploy/helm/canvas/templates/configmap.yaml`
- Create: `deploy/helm/canvas/templates/secret-ref.yaml`

- [x] Create chart metadata and environment-specific values.
- [x] Define deployment, service, ingress, configmap, and secret-reference templates without embedding secrets.
- [x] Include readiness/liveness probes, resource requests/limits, HPA-compatible labels, and actuator paths.

Run:

```bash
test -f deploy/helm/canvas/Chart.yaml
helm template canvas deploy/helm/canvas -f deploy/helm/canvas/values-staging.yaml >/tmp/canvas-staging.yaml
rg -n "readinessProbe|livenessProbe|resources|secretKeyRef|actuator|Service|Ingress" /tmp/canvas-staging.yaml
```

Expected: Helm template renders staging YAML and includes probes, resources, secret references, actuator paths, service, and ingress resources.

### Task 3: Add Rollout And Rollback Runbook

**Files:**
- Create: `docs/architecture/runbooks/k8s-rollout-rollback.md`
- Modify: `docs/architecture/evidence/p3-06-k8s-platform.md`
- Modify: `docs/architecture/archive/completed/plans/P3-06-k8s-deployment-platform-plan.md`

- [x] Define smoke checks for health, metrics, frontend load, login, and one safe API request.
- [x] Define rollback for image, config, migration, ingress, and dependency failure.
- [x] Define evidence capture paths for rendered YAML, deployed image tag, smoke output, and rollback drill.

Run:

```bash
test -f docs/architecture/runbooks/k8s-rollout-rollback.md
rg -n "Smoke|Health|Metrics|Login|Rollback|Migration|Image|Config|Evidence" docs/architecture/runbooks/k8s-rollout-rollback.md
git diff -- deploy/helm/canvas docs/architecture/k8s-operating-model.md docs/architecture/runbooks/k8s-rollout-rollback.md docs/architecture/evidence/p3-06-k8s-platform.md docs/architecture/archive/completed/plans/P3-06-k8s-deployment-platform-plan.md
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: documentation and Helm diff contains only Helm assets, K8s operating model, rollout/rollback runbook, evidence, and plan changes; no files are staged or committed.
