# Platform Evolution Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Keep P3 platform evolution as a governed umbrella until each promoted item has owner, success metrics, migration strategy, rollback plan, dependency analysis, and P0/P1 risk disposition.

**Architecture:** Treat P3-01 as the entry point for long-term platform work. Focused P3 plans own service decomposition, data platform, datasource isolation, WebFlux/MVC, Kubernetes, production components, WeCom, and platform primitives. No focused P3 item starts implementation without the boundary gate and promotion checklist.

**Tech Stack:** Markdown evidence docs, ADRs, Java 21/Spring Boot for backend readiness checks, React/TypeScript for frontend readiness checks, Maven, Vitest, GitHub Actions.

---

## Source Material

- Spec: `../specs/P3-01-platform-evolution-spec.md`
- Source package: `../todo/p3/platform-evolution/`
- Boundary review: `../specs/P3-00-architecture-boundary-review-spec.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Read: `docs/architecture/archive/specs/P3-01-platform-evolution-spec.md`
- Read: `docs/architecture/archive/specs/P3-00-architecture-boundary-review-spec.md`
- Read: `docs/architecture/todo/p3/platform-evolution/plan.md`
- Read: `docs/architecture/archive/evolution/service-architecture-design.md`
- Read: `docs/architecture/archive/evolution/target-architecture-overview.md`
- Read: `docs/architecture/archive/evolution/production-practice-review.md`
- Create: `docs/architecture/evidence/p3-01-platform-evolution.md`
- Create: `docs/architecture/platform-evolution-promotion-checklist.md`
- Modify: `docs/architecture/archive/plans/README.md`
- Modify: `docs/architecture/todo/coverage-matrix.md`

### Task 1: Archive the existing evolution documents as source material

**Files:**
- Create: `docs/architecture/evidence/p3-01-platform-evolution.md`
- Read: `docs/architecture/archive/evolution/service-architecture-design.md`
- Read: `docs/architecture/archive/evolution/target-architecture-overview.md`
- Read: `docs/architecture/archive/evolution/architect-critical-review.md`
- Read: `docs/architecture/archive/evolution/data-platform-architecture.md`
- Read: `docs/architecture/archive/evolution/production-practice-review.md`

- [x] Create an evidence index that lists every archived evolution document, the focused P3 spec it informs, and whether it is source evidence or an active decision.
- [x] Mark archived documents as non-executable until a focused P3 plan promotes a slice.
- [x] Link the boundary review as the first gate for service, data platform, WeCom, and Kubernetes work.

**Run:**
```bash
test -f docs/architecture/evidence/p3-01-platform-evolution.md
rg "service-architecture-design|target-architecture-overview|architect-critical-review|data-platform-architecture|production-practice-review|source evidence" docs/architecture/evidence/p3-01-platform-evolution.md
```

**Expected:** Evidence index maps each archived evolution doc to a focused P3 spec and status.

### Task 2: Keep P3-01 as the umbrella entry point

**Files:**
- Modify: `docs/architecture/archive/plans/README.md`
- Create: `docs/architecture/platform-evolution-promotion-checklist.md`
- Read: `docs/architecture/archive/specs/P3-01-platform-evolution-spec.md`
- Read: `docs/architecture/archive/plans/P3-02-service-decomposition-and-domain-boundaries-plan.md`
- Read: `docs/architecture/archive/plans/P3-03-data-platform-architecture-plan.md`
- Read: `docs/architecture/archive/plans/P3-06-k8s-deployment-platform-plan.md`

- [x] Add P3-01 to the plans README as the platform-evolution entry point.
- [x] Create a promotion checklist with owner, success metrics, user value, dependencies, migration, rollback, operations, security, compliance, tenant impact, and team capacity.
- [x] Link focused P3 plans from the checklist.

**Run:**
```bash
test -f docs/architecture/platform-evolution-promotion-checklist.md
rg "P3-01|platform-evolution entry point|owner|success metrics|migration|rollback|team capacity" docs/architecture/archive/plans/README.md docs/architecture/platform-evolution-promotion-checklist.md
```

**Expected:** P3-01 is the umbrella entry point, and the promotion checklist names every required gate.

### Task 3: Revisit after P0/P1 closure

**Files:**
- Modify: `docs/architecture/platform-evolution-promotion-checklist.md`
- Read: `docs/architecture/todo/coverage-matrix.md`
- Read: `docs/architecture/archive/specs/P0-01-security-hardening-spec.md`
- Read: `docs/architecture/archive/specs/P0-03-canvas-state-data-consistency-spec.md`
- Read: `docs/architecture/archive/specs/P1-04-observability-and-ops-spec.md`
- Read: `docs/architecture/archive/specs/P1-05-release-deployment-governance-spec.md`

- [x] Add a prerequisite checklist for P0 security, P0 state consistency, P0 tenant isolation, P1 observability, and P1 release governance.
- [x] For any prerequisite not closed, require explicit risk acceptance with owner and expiration date.
- [x] Add the focused test command that proves each prerequisite is closed.

**Run:**
```bash
rg "P0-01|P0-03|P0-06|P1-04|P1-05|risk acceptance|expiration date" docs/architecture/platform-evolution-promotion-checklist.md
cd backend && mvn test -pl canvas-engine -Dtest=SecurityConfigRouteTest,CanvasTransactionAnnotationTest,TenantServiceTest
```

**Expected:** Promotion checklist blocks P3 implementation unless P0/P1 prerequisites are closed or explicitly accepted, and prerequisite smoke tests pass.

### Task 4: Use the focused P3 specs for promoted evolution items

**Files:**
- Modify: `docs/architecture/platform-evolution-promotion-checklist.md`
- Read: `docs/architecture/archive/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`
- Read: `docs/architecture/archive/specs/P3-03-data-platform-architecture-spec.md`
- Read: `docs/architecture/archive/specs/P3-04-multi-datasource-isolation-spec.md`
- Read: `docs/architecture/archive/specs/P3-05-webflux-to-mvc-migration-spec.md`
- Read: `docs/architecture/archive/specs/P3-06-k8s-deployment-platform-spec.md`
- Read: `docs/architecture/archive/specs/P3-07-production-platform-components-spec.md`
- Read: `docs/architecture/archive/specs/P3-08-wecom-scrm-module-spec.md`
- Read: `docs/architecture/archive/specs/P3-09-identity-event-and-tenant-platform-spec.md`

- [x] Add one promotion row for each focused P3 spec with scope, evidence file, approval owner, and first executable plan task.
- [x] Require a fresh ADR for any promoted physical service, runtime, datasource, Kubernetes, or component decision.
- [x] Keep P3-01 itself documentation-only except for this governance checklist.

**Run:**
```bash
rg "P3-02|P3-03|P3-04|P3-05|P3-06|P3-07|P3-08|P3-09|ADR|evidence file" docs/architecture/platform-evolution-promotion-checklist.md
```

**Expected:** Every focused P3 spec has a promotion row and evidence requirement.

### Task 5: Require migration, rollback, operations, and team-capacity sections before implementation

**Files:**
- Modify: `docs/architecture/platform-evolution-promotion-checklist.md`
- Modify: `docs/architecture/todo/coverage-matrix.md`
- Read: `docs/architecture/archive/specs/P3-01-platform-evolution-spec.md`

- [x] Add checklist rows for migration plan, rollback plan, operating model, on-call owner, runbook, test plan, data migration, observability, and team capacity.
- [x] Add a coverage-matrix note that P3 plans are blocked until those rows are filled.
- [x] Require a command and expected evidence path for each promoted item.

**Run:**
```bash
rg "migration plan|rollback plan|operating model|on-call owner|runbook|test plan|data migration|observability|team capacity" docs/architecture/platform-evolution-promotion-checklist.md
rg "P3 platform evolution" docs/architecture/todo/coverage-matrix.md
```

**Expected:** Promotion checklist and coverage matrix both show the required implementation-readiness sections.

### Task 6: Review scoped platform evolution governance changes

**Files:**
- Modify: `docs/architecture/archive/plans/P3-01-platform-evolution-plan.md`
- Modify: `docs/architecture/archive/plans/README.md`
- Modify: `docs/architecture/todo/coverage-matrix.md`
- Create: `docs/architecture/evidence/p3-01-platform-evolution.md`
- Create: `docs/architecture/platform-evolution-promotion-checklist.md`

- [x] Review only files named in this plan.
- [x] Do not stage or commit in this session unless the user explicitly asks.
- [x] Record verification commands and remaining follow-ups in evidence.

**Run:**
```bash
git diff -- docs/architecture/archive/plans/P3-01-platform-evolution-plan.md docs/architecture/archive/plans/README.md docs/architecture/todo/coverage-matrix.md docs/architecture/evidence/p3-01-platform-evolution.md docs/architecture/platform-evolution-promotion-checklist.md
```

**Expected:** The diff contains only P3-01 governance, evidence, checklist, README, and coverage-matrix changes. No commit is created by default.
