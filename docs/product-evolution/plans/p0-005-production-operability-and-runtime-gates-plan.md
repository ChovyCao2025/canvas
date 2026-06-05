# Production Operability And Runtime Gates Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add the minimum operational gates required to ship, observe, pause, and recover the platform in production.

**Architecture:** Add CI and packaging gates first, then secure and audit operational controls, then add dashboards, alerts, and runbooks. Keep runtime controls behind role checks and make every high-impact operation produce an audit event and verification trail.

**Tech Stack:** GitHub Actions, Maven, npm/Vite/Vitest, Docker, nginx, Spring Boot, Micrometer/Actuator, Grafana dashboard JSON, Prometheus alert rules, React 18, Ant Design.

## Implementation Status

- Status: implemented and verified on 2026-06-05.
- Commit: not created in this session because the worktree contains many unrelated and parallel product-evolution changes.

---

## Spec Reference

- `docs/product-evolution/specs/p0-005-production-operability-and-runtime-gates.md`
- Optimization sources: `docs/optimization/production-readiness-checklist.md`, `docs/optimization/bmad-product-review-2026-05.md`, `docs/optimization/todo/2026-05-31-evolution-directions.md`

## File Structure

**CI And Packaging**
- Create: `.github/workflows/ci.yml`
- Create: `.dockerignore`
- Modify: `backend/canvas-engine/Dockerfile`
- Create: `frontend/Dockerfile`
- Create: `frontend/nginx.conf`

**Backend**
- Create: `backend/canvas-engine/src/main/resources/application-prod.yml`
- Create: `backend/canvas-engine/src/main/resources/application-staging.yml`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationEventService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`

**Frontend And Ops Docs**
- Create: `frontend/src/pages/ops-dashboard/index.tsx`
- Create: `frontend/src/services/opsApi.ts`
- Create: `ops/grafana/canvas-runtime-dashboard.json`
- Create: `ops/alerts/canvas-runtime-rules.yml`
- Create: `docs/product-evolution/runbooks/production-runtime-runbook.md`

**Tests**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/OpsControllerSecurityTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/RuntimeAlertNotificationTest.java`
- Create: `frontend/src/pages/ops-dashboard/opsDashboardPresentation.test.ts`

### Task 1: CI, Docker, And Environment Gates

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `.dockerignore`
- Modify: `backend/canvas-engine/Dockerfile`
- Create: `frontend/Dockerfile`
- Create: `frontend/nginx.conf`
- Create: `backend/canvas-engine/src/main/resources/application-prod.yml`
- Create: `backend/canvas-engine/src/main/resources/application-staging.yml`

- [x] **Step 1: Add CI workflow**

Add jobs for backend test, frontend test, frontend build, and migration validation. Use Java 21 and Node version from `frontend/package.json` or the existing lockfile environment.

- [x] **Step 2: Add Docker build hygiene**

Add root `.dockerignore` entries for `.git`, `frontend/node_modules`, `frontend/dist`, `backend/**/target`, logs, temp files, and local env files. Make backend and frontend containers run as non-root users.

- [x] **Step 3: Add production and staging config files**

Set safe production defaults: no wildcard CORS, Swagger UI disabled, Actuator details restricted, no root DB user examples, no default event secret, explicit frontend API base URL, and Flyway mode documented.

- [x] **Step 4: Run gate commands locally**

Run:

```bash
cd backend && mvn -pl canvas-engine test
cd frontend && npm test -- --run
cd frontend && npm run build
docker build -f backend/canvas-engine/Dockerfile backend
docker build -f frontend/Dockerfile frontend
```

Expected: PASS or record each environment-specific failure with command and missing prerequisite.

Verified 2026-06-05:

- `cd backend && mvn -pl canvas-engine clean test` PASS: 1295 tests, 0 failures, 0 errors, 1 skipped.
- `cd frontend && npm test` PASS: 65 test files, 242 tests.
- `cd frontend && npm run build` PASS.
- `docker build -f backend/canvas-engine/Dockerfile backend` PASS.
- `docker build -f frontend/Dockerfile frontend` PASS.

### Task 2: Secured Ops Controls And Audit

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationEventService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/OpsControllerSecurityTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/RuntimeAlertNotificationTest.java`

- [x] **Step 1: Write security tests**

Create `OpsControllerSecurityTest` methods named `rejectsUnauthenticatedRequests`, `allowsOperatorReadOnlyAccess`, `limitsTenantAdminEmergencyActionsToTenantScope`, `allowsSystemAdminGlobalActions`, `requiresReasonText`, and `createsAuditEvent`.

- [x] **Step 2: Implement guarded endpoints**

Expose emergency pause, resume, kill, rollback, offline, DLQ replay, and delivery reconciliation endpoints. Each endpoint must validate role, tenant scope, reason text, target existence, and current state transition.

- [x] **Step 3: Emit notifications for failure gates**

Create notification events for failed execution spike, DLQ growth, delivery outbox dead rows, trace buffer overflow, and emergency action completion.

- [x] **Step 4: Run ops tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=OpsControllerSecurityTest,RuntimeAlertNotificationTest
```

Expected: PASS.

Verified 2026-06-05:

- `cd backend && mvn -pl canvas-engine test -Dtest=OpsControllerSecurityTest,RuntimeAlertNotificationTest,SecurityConfigRoleTest,SecurityConfigRouteTest,OpsControllerRecoveryTest,OpsControllerTemplateTest,NotificationEventServiceTest,CanvasMetricsTest` PASS: 25 tests.

### Task 3: Dashboard, Alerts, And Runbook

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- Create: `ops/grafana/canvas-runtime-dashboard.json`
- Create: `ops/alerts/canvas-runtime-rules.yml`
- Create: `docs/product-evolution/runbooks/production-runtime-runbook.md`
- Create: `frontend/src/pages/ops-dashboard/index.tsx`
- Create: `frontend/src/services/opsApi.ts`
- Create: `frontend/src/pages/ops-dashboard/opsDashboardPresentation.test.ts`

- [x] **Step 1: Add required metrics**

Ensure metrics exist for execution success rate, execution active by lane, queue depth, retry backlog, DLQ count, delivery outbox status counts, Redis registry latency, MySQL pool pressure, trace buffer pending/dropped, and downstream latency.

- [x] **Step 2: Add Grafana and alert files**

Create dashboard panels and alert rules for the metrics above. Alert rules must include summary, severity, runbook URL, and tenant/system scope labels where available.

- [x] **Step 3: Add ops dashboard page**

Render runtime status, active incidents, alert summaries, emergency actions, and latest audit events. Include loading, empty, error, and permission states.

- [x] **Step 4: Add runbook**

Document rollback, degrade, kill, DLQ replay, delivery reconciliation, trace backlog response, Redis registry outage, MySQL pressure, and incident handoff.

- [x] **Step 5: Run frontend ops test**

Run:

```bash
cd frontend && npm test -- opsDashboardPresentation.test.ts
```

Expected: PASS.

Verified 2026-06-05:

- `cd frontend && npm test -- opsDashboardPresentation.test.ts` PASS: 1 test file, 2 tests.
- `node -e "JSON.parse(require('fs').readFileSync('ops/grafana/canvas-runtime-dashboard.json','utf8'))"` PASS.
- `ruby -e "require 'yaml'; YAML.load_file('ops/alerts/canvas-runtime-rules.yml')"` PASS.

### Verification Evidence

- BI publish approval resource-service compile unblock:

```bash
cd backend && mvn -pl canvas-engine clean test -Dtest=BiPublishApprovalServiceTest,BiPublishApprovalControllerTest,BiDatasetResourceServiceTest,BiDashboardResourceServiceTest,BiChartResourceServiceTest,BiPortalResourceServiceTest
```

Result: 54 tests, 0 failures, 0 errors, 0 skipped.

- Focused backend ops slice:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=OpsControllerSecurityTest,RuntimeAlertNotificationTest,SecurityConfigRoleTest,SecurityConfigRouteTest,OpsControllerRecoveryTest,OpsControllerTemplateTest,NotificationEventServiceTest,CanvasMetricsTest
```

Result: 25 tests, 0 failures, 0 errors, 0 skipped.

- Frontend ops dashboard slice:

```bash
cd frontend && npm test -- opsDashboardPresentation.test.ts
```

Result: 1 test file, 2 tests passed.

- Ops asset parse checks:

```bash
node -e "JSON.parse(require('fs').readFileSync('ops/grafana/canvas-runtime-dashboard.json','utf8'))"
ruby -e "require 'yaml'; YAML.load_file('ops/alerts/canvas-runtime-rules.yml')"
```

Result: Grafana dashboard JSON and alert YAML parsed successfully.

### Task 4: Verification And Commit

**Files:**
- Modify: `docs/product-evolution/specs/p0-005-production-operability-and-runtime-gates.md`
- Modify: `docs/product-evolution/plans/p0-005-production-operability-and-runtime-gates-plan.md`

- [x] **Step 1: Run focused verification**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=OpsControllerSecurityTest,RuntimeAlertNotificationTest
cd frontend && npm test -- opsDashboardPresentation.test.ts
```

Expected: PASS.

- [x] **Step 2: Run packaging verification**

Run:

```bash
cd frontend && npm run build
docker build -f backend/canvas-engine/Dockerfile backend
docker build -f frontend/Dockerfile frontend
```

Expected: PASS or documented prerequisite issue.

- [ ] **Step 3: Commit implementation slice**

Run:

```bash
git add .github .dockerignore backend/canvas-engine frontend ops docs/product-evolution/specs docs/product-evolution/plans docs/product-evolution/runbooks
git commit -m "chore: add production operability gates"
```

Expected: commit contains only CI, packaging, ops security, observability, runbook, spec, and plan files.

Deferred 2026-06-05: commit was not created because the current worktree contains broad unrelated in-progress changes. Slice and commit after review.
