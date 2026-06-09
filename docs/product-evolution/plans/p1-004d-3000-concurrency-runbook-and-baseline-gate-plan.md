# 3000 Concurrency Runbook And Baseline Gate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Document the executable 3000 hardening release gate with entry requirements, baseline tests, profile execution, stop gates, rollback, degradation actions, and the 4000 block.

**Architecture:** Add a dedicated runbook under `docs/product-evolution/runbooks/` and link it from the perf tooling README. This slice only documents the operator workflow after P1-004, P1-004B, and P1-004C are implemented.

**Tech Stack:** Markdown, Node.js perf tooling references, Maven command references.

**Implementation Status:** Implemented and focused-verified on 2026-06-05. The 3000 concurrency hardening runbook now exists under `docs/product-evolution/runbooks/`, and `tools/perf/README.md` links to it from the 3000 hardening profile section.

---

## Spec Reference

- `docs/product-evolution/specs/p1-004d-3000-concurrency-runbook-and-baseline-gate.md`
- Source: `docs/optimization/archive/3000-concurrency-hardening-checklist.md`

## File Structure

**Runbook**
- Create: `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md` - operator entry, baseline, profile, stop, rollback, degrade, and 4000 block instructions.
- Modify: `tools/perf/README.md` - links the runbook from the 3000 hardening profile section.

### Task 1: Runbook

**Files:**
- Create: `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md`
- Modify: `tools/perf/README.md`

- [x] **Step 1: Add runbook**

Create `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md`:

````markdown
# 3000 Concurrency Hardening Runbook

## Entry Requirements

- Java 21 backend baseline passes.
- `canvas.execution.max-concurrency=3000`.
- Lane budgets are `LIGHT=600`, `STANDARD=1800`, `HEAVY=300`, `RETRY=300`.
- Redis, MySQL, RocketMQ, downstream test doubles, and local backend are reachable.
- `tools/perf/3000-hardening-profiles.json` validates lane totals and required profile names.

## Baseline Command

```bash
JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -f backend/pom.xml -pl canvas-engine -Dtest=CanvasExecutionServiceTest,CanvasExecutionServiceTriggerNodeTest,InFlightExecutionRegistryTest,ExecutionLaneResolverTest,InFlightExecutionRegistryLaneTest,CanvasServicePublishTest,CanvasServiceExampleFilterTest,CanvasOpsServiceExampleCloneTest test
```

Pass condition: `BUILD SUCCESS`.

## Profile Execution

```bash
node tools/perf/hardening-profile.mjs \
  --profile-file tools/perf/3000-hardening-profiles.json \
  --profile default-mixed-3000 \
  --out-dir tmp/perf-3000-hardening \
  --run-id-prefix perf_3000_hardening_$(date +%Y%m%d_%H%M%S) \
  --write-evidence true
```

Run the command printed by `hardening-profile.mjs` and keep the generated `evidence-manifest.json`.

## Stop Gates

- Redis p95 above 20 ms or p99 above 50 ms for one observation window.
- MySQL active connections at or above 85% of pool max.
- Slow SQL above 1000 ms in two consecutive samples.
- Normal MQ backlog grows while RETRY drains.
- Disruptor overflow grows for two consecutive samples.
- Retry backlog grows after downstream recovery.
- DLQ grows after downstream recovery.
- LIGHT or STANDARD p95 exceeds 1000 ms.

## Rollback Actions

- Restore previous `canvas.execution.max-concurrency`.
- Restore previous lane budgets.
- Pause scheduled, replay, and heavy traffic when needed.
- Keep normal traffic on the last passing profile.
- Rerun the backend baseline after rollback.

## Degrade Actions

- Lower `RETRY` or lengthen retry backoff.
- Lower `HEAVY` or pause heavy jobs.
- Disable low-priority scheduled/replay traffic.
- Keep `LIGHT` and `STANDARD` protected when their dependencies remain healthy.
- Reject new admission conservatively if Redis registry health is unknown.

## 4000 Block

Do not start 4000 readiness until every required 3000 profile passes and evidence artifacts are retained.
````

- [x] **Step 2: Link runbook from perf README**

Modify `tools/perf/README.md` in the `3000 Hardening Profiles` section:

```markdown
Detailed operational steps live in `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md`.
```

### Task 2: Verification And Commit

**Files:**
- Create: `docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md`
- Modify: `tools/perf/README.md`
- Modify: `docs/product-evolution/specs/p1-004d-3000-concurrency-runbook-and-baseline-gate.md`
- Modify: `docs/product-evolution/plans/p1-004d-3000-concurrency-runbook-and-baseline-gate-plan.md`

- [x] **Step 1: Verify required runbook sections**

Run:

```bash
rg -n "Entry Requirements|Baseline Command|Profile Execution|Stop Gates|Rollback Actions|Degrade Actions|4000 Block" docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md
```

Expected: output includes all seven section headings.

Observed: PASS on 2026-06-05; output included all seven section headings.

- [x] **Step 2: Verify README link**

Run:

```bash
rg -n "3000-concurrency-hardening-runbook.md" tools/perf/README.md
```

Expected: output includes the runbook link in the 3000 hardening profile section.

Observed: PASS on 2026-06-05; output included the runbook link at `tools/perf/README.md:193`.

- [x] **Step 3: Document commit boundary**
Boundary: No git commit or merge was created in this docs-only audit; the command below remains the future scoped staging recipe.

Run:

```bash
git add \
  docs/product-evolution/runbooks/3000-concurrency-hardening-runbook.md \
  tools/perf/README.md \
  docs/product-evolution/specs/p1-004d-3000-concurrency-runbook-and-baseline-gate.md \
  docs/product-evolution/plans/p1-004d-3000-concurrency-runbook-and-baseline-gate-plan.md
git commit -m "docs: add 3000 concurrency hardening runbook"
```

Expected: commit contains only runbook, README link, and related docs.
