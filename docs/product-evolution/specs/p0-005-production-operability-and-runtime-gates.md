# P0-005 - Production Operability And Runtime Gates Spec

Priority: P0
Sequence: 005
Source: `docs/optimization/production-readiness-checklist.md`, `docs/optimization/bmad-product-review-2026-05.md`, `docs/optimization/todo/2026-05-31-evolution-directions.md`
Implementation plan: `../plans/p0-005-production-operability-and-runtime-gates-plan.md`

## Goal

Add the minimum operational gates required to ship, observe, pause, and recover the platform in production.

## User And Business Value

Engineering and operations teams get repeatable quality gates, dashboards, alerts, runbooks, and emergency controls before traffic or tenant count grows.

## Evidence From Optimization

- Production readiness docs flag missing CI/CD, `.dockerignore`, non-root containers, frontend containerization, production configs, dashboards, alert rules, log aggregation, SLOs, runbooks, and capacity planning.
- BMAD review flags hidden ops endpoints, missing audit log writes, missing DLQ UI, failed execution notifications, and runtime recovery UX gaps.

## In Scope

- CI jobs for backend tests, frontend tests, build, lint where available, and migration validation.
- Docker and environment packaging fixes for backend and frontend.
- Production/staging configuration files with safe defaults for CORS, Swagger, Actuator, credentials, Flyway mode, and API base URL.
- Grafana dashboard and alert rules for execution success rate, queue depth, DLQ, delivery outbox, Redis latency, MySQL pool pressure, and trace buffer backlog.
- Runbooks for rollback, degrade, kill, DLQ replay, delivery reconciliation, and incident handoff.
- Operator-visible emergency controls for pause, resume, kill, rollback, and offline with audit events.

## Out Of Scope

- Full Kubernetes platform ownership beyond manifests and runtime gates.
- Enterprise SIEM integration.
- Replacing all observability tooling in one pass.

## Functional Requirements

1. A pull request must be able to run the documented backend and frontend quality gates.
2. Production builds must not require source edits for environment changes.
3. Ops endpoints and emergency controls must require authentication, authorization, and audit logging.
4. A failed execution or DLQ growth condition must produce an operator-visible notification.
5. Every emergency action must include a rollback or verification step in a runbook.

## Technical Scope

### Backend Touchpoints

- `backend/canvas-engine/Dockerfile`
- `backend/canvas-engine/src/main/resources/application-prod.yml`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/notification/NotificationEventService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`

### Frontend Touchpoints

- `frontend/Dockerfile`
- `frontend/nginx.conf`
- `frontend/src/pages/ops-dashboard/index.tsx`
- `frontend/src/services/opsApi.ts`

### DevOps And Docs Touchpoints

- `.github/workflows/ci.yml`
- `.dockerignore`
- `ops/grafana/canvas-runtime-dashboard.json`
- `ops/alerts/canvas-runtime-rules.yml`
- `docs/product-evolution/runbooks/production-runtime-runbook.md`

### Test Touchpoints

- `backend/canvas-engine/src/test/java/org/chovy/canvas/web/OpsControllerSecurityTest.java`
- `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/notification/RuntimeAlertNotificationTest.java`
- `frontend/src/pages/ops-dashboard/opsDashboardPresentation.test.ts`

## Dependencies

- Requires existing auth and role model to distinguish tenant operator, tenant admin, and system admin emergency permissions.
- Requires metrics exported by backend Actuator or a documented scrape endpoint.

## Risks And Controls

- Alert fatigue risk: start with a small required alert set and documented threshold rationale.
- Operational misuse risk: make high-impact controls require confirmation, reason text, and audit logging.
- CI runtime risk: split fast required jobs from longer nightly jobs.

## Acceptance Criteria

- CI can run backend and frontend gates on a clean checkout using documented commands.
- Backend and frontend containers build without running as root.
- Production/staging configs disable unsafe development defaults.
- Grafana dashboard JSON and alert rules are present and referenced by runbook.
- Ops emergency controls reject unauthorized users and write audit events.
