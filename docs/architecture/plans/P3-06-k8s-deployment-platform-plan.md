# K8s Deployment Platform Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn the Kubernetes deployment proposal into deployable, testable platform assets.

**Architecture:** Build environment-aware deployment assets around explicit production SLOs and dependency ownership. Treat databases, Redis, and RocketMQ as managed dependencies unless the team commits to operating them.

**Tech Stack:** Kubernetes, Helm, Docker, Spring Boot actuator, Prometheus, Grafana, CI/CD pipeline tooling.

---

## Source Material

- Spec: `../specs/P3-06-k8s-deployment-platform-spec.md`
- Evolution doc: `../archive/evolution/k8s-deployment-plan.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `deploy/helm/canvas/`
- Create: `docs/architecture/k8s-operating-model.md`
- Test: template rendering, smoke checks, and rollback drills

### Task 1: Define Operating Model

- [ ] **Step 1: Document dependency ownership**

Write `docs/architecture/k8s-operating-model.md` naming who operates MySQL, Redis, RocketMQ, ingress, monitoring, and secrets.

- [ ] **Step 2: Add SLO and environment matrix**

Include dev, staging, and production resource/SLO differences.

- [ ] **Step 3: Verify completeness**

Run `rg -n "MySQL|Redis|RocketMQ|Ingress|Secrets|SLO|Rollback" docs/architecture/k8s-operating-model.md`. Expected: all terms appear.

### Task 2: Scaffold Deployment Assets

- [ ] **Step 1: Create Helm chart structure**

Create chart, values files, deployment, service, ingress, config, and secret templates.

- [ ] **Step 2: Render templates locally**

Run the chart template command used by this repository or CI. Expected: Kubernetes YAML renders without missing values.

- [ ] **Step 3: Review diff**

Run `git diff -- deploy/helm docs/architecture/k8s-operating-model.md`. Expected: only platform deployment assets and operating docs are changed.

### Task 3: Add Rollout And Rollback Checks

- [ ] **Step 1: Define smoke checks**

Document health, metrics, frontend load, login, and one safe API request.

- [ ] **Step 2: Define rollback**

Document image rollback, config rollback, migration rollback, and dependency rollback boundaries.

- [ ] **Step 3: Verify runbook sections**

Run `rg -n "Smoke|Rollback|Migration|Health|Metrics" docs/architecture/k8s-operating-model.md`. Expected: all sections exist.
