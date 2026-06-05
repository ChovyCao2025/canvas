# Production Platform Components Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Decide which production platform components are justified by validated capability gaps and stage only one proof-of-value at a time.

**Architecture:** Use ADRs and local abstractions before adopting platform components. Each component must have a problem statement, alternatives, operational owner, failure mode, rollback path, and measurable proof before production rollout. Per `../specs/P3-00-architecture-boundary-review-spec.md`, component choices are platform decisions and must not create domain-service boundaries by themselves.

**Tech Stack:** Spring Boot, Redis/Redisson, RocketMQ, Nacos, Sentinel, WebClient/Feign, Spring Boot Admin, ClickHouse, Prometheus/Grafana, Markdown ADRs.

---

## Source Material

- Spec: `../specs/P3-07-production-platform-components-spec.md`
- Boundary review: `../specs/P3-00-architecture-boundary-review-spec.md`
- Boundary evidence: `../evidence/p3-00-architecture-boundary-review.md`
- Evolution doc: `../archive/evolution/production-practice-review.md`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Create: `docs/architecture/evidence/p3-07-platform-components.md`
- Create: `docs/architecture/platform-component-decision-matrix.md`
- Create: `docs/architecture/adr/platform-component-first-pov.md`
- Create: `docs/architecture/platform-component-abstraction-plan.md`
- Read: `docs/architecture/archive/evolution/production-practice-review.md`

### Task 1: Build The Decision Matrix

**Files:**
- Create: `docs/architecture/platform-component-decision-matrix.md`
- Create: `docs/architecture/evidence/p3-07-platform-components.md`

- [x] Extract candidate components from the production-practice review.
- [x] For each candidate, record problem, current workaround, alternatives, operational owner, cost, failure mode, rollback, and decision.
- [x] Mark components as accepted for proof, needs evidence, deferred, or rejected.

Run:

```bash
test -f docs/architecture/platform-component-decision-matrix.md
rg -n "XXL|Redisson|Nacos|Knife4j|Sentinel|Spring Boot Admin|ClickHouse|owner|failure mode|rollback|decision" docs/architecture/platform-component-decision-matrix.md
```

Expected: matrix includes all named candidates plus owner, failure mode, rollback, and decision fields.

### Task 2: Pick One Component For Proof Of Value

**Files:**
- Create: `docs/architecture/adr/platform-component-first-pov.md`
- Modify: `docs/architecture/evidence/p3-07-platform-components.md`
- Read: `docs/architecture/platform-component-decision-matrix.md`

- [x] Select the component that closes a confirmed P0/P1/P2 gap with the smallest rollout risk.
- [x] Write ADR sections for problem, decision, alternatives, rollout, rollback, owner, success metric, and stop criteria.
- [x] State why all other candidates are deferred.

Run:

```bash
test -f docs/architecture/adr/platform-component-first-pov.md
rg -n "Problem|Decision|Alternatives|Rollout|Rollback|Owner|Success metric|Stop criteria|Deferred" docs/architecture/adr/platform-component-first-pov.md
```

Expected: ADR chooses at most one proof-of-value component and defers the rest with reasons.

### Task 3: Define The Implementation Boundary

**Files:**
- Create: `docs/architecture/platform-component-abstraction-plan.md`
- Modify: `docs/architecture/evidence/p3-07-platform-components.md`
- Modify: `docs/architecture/plans/P3-07-production-platform-components-plan.md`

- [x] Define the local interface that shields business code from the selected component.
- [x] Define proof tests, operational drill, metrics, dashboard signal, and rollback command.
- [x] Define production rollout prerequisites and owner signoff.

Run:

```bash
test -f docs/architecture/platform-component-abstraction-plan.md
rg -n "interface|proof test|operational drill|metric|dashboard|rollback command|owner signoff" docs/architecture/platform-component-abstraction-plan.md
git diff -- docs/architecture/evidence/p3-07-platform-components.md docs/architecture/platform-component-decision-matrix.md docs/architecture/adr/platform-component-first-pov.md docs/architecture/platform-component-abstraction-plan.md docs/architecture/plans/P3-07-production-platform-components-plan.md
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: documentation diff contains only platform component evidence, decision matrix, ADR, abstraction plan, and plan changes; no files are staged or committed.
