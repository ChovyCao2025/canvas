# Architecture Spec/Plan Materialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Materialize the reviewed architecture work into priority-prefixed spec and plan files under `docs/architecture/archive/completed/specs/` and `docs/architecture/archive/completed/plans/`, with stable indexes and coverage-matrix traceability.

**Architecture:** `docs/architecture/todo/` remains the active work queue and traceability root. `docs/architecture/archive/completed/specs/` and `docs/architecture/archive/completed/plans/` are flattened handoff views. The current set contains 18 source todo packages plus 9 expanded P3 architecture evolution/boundary packages, for 27 package specs and 27 package plans. This P0-00 plan is the materialization closure plan, and `P3-00-architecture-boundary-code-verification.md` is a supporting verification sidecar rather than a package spec.

**Tech Stack:** Markdown, zsh, `find`, `rg`, `git status`.

**Current Status:** Completed and verified on 2026-06-04.

---

## Materialized Package Order

```text
P0-01 security-hardening
P0-02 reactive-threading-and-transactions
P0-03 canvas-state-data-consistency
P0-04 execution-concurrency-safety
P0-05 production-resilience-and-dr
P0-06 data-security-and-tenant-isolation
P1-01 dag-engine-and-handler-boundaries
P1-02 api-contract-and-validation
P1-03 frontend-canvas-state
P1-04 observability-and-ops
P1-05 release-deployment-governance
P2-01 testing-foundation
P2-02 cost-capacity-and-retention
P2-03 documentation-adr-and-runbooks
P2-04 dependency-abstraction-and-vendor-lock-in
P2-05 compliance-data-governance
P2-06 frontend-accessibility-and-quality
P3-00 architecture-boundary-review
P3-01 platform-evolution
P3-02 service-decomposition-and-domain-boundaries
P3-03 data-platform-architecture
P3-04 multi-datasource-isolation
P3-05 webflux-to-mvc-migration
P3-06 k8s-deployment-platform
P3-07 production-platform-components
P3-08 wecom-scrm-module
P3-09 identity-event-and-tenant-platform
```

## File Structure

- Specs index: `docs/architecture/specs/README.md`
- Plans index: `docs/architecture/plans/README.md`
- Architecture entry point: `docs/architecture/index.md`
- Todo source index: `docs/architecture/todo/README.md`
- Coverage matrix: `docs/architecture/todo/coverage-matrix.md`
- Package specs: `docs/architecture/archive/completed/specs/P*-*-spec.md`
- Package plans: `docs/architecture/archive/completed/plans/P*-*-plan.md`
- Supporting sidecar: `docs/architecture/archive/completed/specs/P3-00-architecture-boundary-code-verification.md`

## Task 1: Materialize Specs

**Files:**
- Source: `docs/architecture/todo/*/*/spec.md`
- Destination: `docs/architecture/archive/completed/specs/P*-*-spec.md`
- Supporting: `docs/architecture/todo/coverage-matrix.md`

- [x] Define the canonical materialized package order.
- [x] Materialize package specs into `docs/architecture/archive/completed/specs/`.
- [x] Add `Source package:` and `Coverage matrix:` metadata to every package spec.
- [x] Verify package spec count.

Verification:

```bash
find docs/architecture/specs -maxdepth 1 -name 'P*-*-spec.md' | wc -l | tr -d ' '
```

Result: `27`.

Metadata check:

```bash
missing=0
for f in docs/architecture/archive/completed/specs/P*-*-spec.md; do
  rg -q '^Source package:' "$f" || { echo "missing source $f"; missing=1; }
  rg -q '^Coverage matrix:' "$f" || { echo "missing coverage $f"; missing=1; }
done
exit $missing
```

Result: exit code `0`.

## Task 2: Materialize Plans

**Files:**
- Source: `docs/architecture/todo/*/*/plan.md`
- Destination: `docs/architecture/archive/completed/plans/P*-*-plan.md`

- [x] Generate a skill-compliant header for each package plan.
- [x] Preserve package implementation tasks and verification commands.
- [x] Verify package plan count and total plan count.

Verification:

```bash
find docs/architecture/plans -maxdepth 1 -name 'P*-*-plan.md' ! -name 'P0-00-*' | wc -l | tr -d ' '
find docs/architecture/plans -maxdepth 1 -name 'P*-*-plan.md' | wc -l | tr -d ' '
```

Result: `27` package plans, `28` total plans including this P0-00 closure plan.

Header check:

```bash
missing=0
for f in docs/architecture/archive/completed/plans/P*-*-plan.md; do
  rg -q 'REQUIRED SUB-SKILL' "$f" || { echo "missing skill header $f"; missing=1; }
done
exit $missing
```

Result: exit code `0`.

## Task 3: Add Folder Indexes

**Files:**
- `docs/architecture/specs/README.md`
- `docs/architecture/plans/README.md`

- [x] Write `specs/README.md` with all 27 package specs in priority order.
- [x] Write `plans/README.md` with this materialization plan plus all 27 package plans in priority order.
- [x] Verify index links include the first and last materialized packages.

Verification:

```bash
rg -n "P0-01-security-hardening|P3-09-identity-event-and-tenant-platform" \
  docs/architecture/specs/README.md docs/architecture/plans/README.md
```

Result: both README files contain the first and last package links.

## Task 4: Update Architecture Entry Points

**Files:**
- `docs/architecture/index.md`
- `docs/architecture/todo/README.md`
- `docs/architecture/todo/coverage-matrix.md`

- [x] Update the top-level architecture index with `todo/`, `specs/`, `plans/`, and `archive/` links.
- [x] Update the todo README with flattened spec/plan locations.
- [x] Update the coverage matrix package legend with materialized spec and plan filenames.
- [x] Verify old unarchived architecture path references are absent from architecture working docs.

Verification:

```bash
rg -n "docs/architecture/(architecture-|remediation/|evolution/|backend-architecture|frontend-architecture|security-considerations|testing-strategy|deployment-guide|database-schema|tech-stack|coding-standards|api-spec-summary|brownfield-architecture|production-deployment)" \
  docs/architecture/specs docs/architecture/plans docs/architecture/todo docs/architecture/index.md
```

Result: no output.

## Task 5: Final Verification

**Files:**
- `docs/architecture/archive/completed/specs/`
- `docs/architecture/archive/completed/plans/`
- `docs/architecture/index.md`
- `docs/architecture/todo/README.md`
- `docs/architecture/todo/coverage-matrix.md`

- [x] Verify every package spec has exactly one matching package plan.
- [x] Verify every package plan, excluding this P0-00 closure plan, has a matching package spec.
- [x] Verify every package spec appears in the folder indexes or coverage matrix.
- [x] Verify generated files are visible to git.
- [x] Record accepted marker-scan exceptions.

Parity check:

```bash
for f in docs/architecture/archive/completed/specs/P*-*-spec.md; do
  b=$(basename "$f" -spec.md)
  p="docs/architecture/archive/completed/plans/${b}-plan.md"
  test -f "$p" || echo "missing plan for $f"
done

for f in docs/architecture/archive/completed/plans/P*-*-plan.md; do
  case "$(basename "$f")" in P0-00-*) continue;; esac
  b=$(basename "$f" -plan.md)
  s="docs/architecture/archive/completed/specs/${b}-spec.md"
  test -f "$s" || echo "missing spec for $f"
done
```

Result: no output.

Index coverage check:

```bash
for f in docs/architecture/archive/completed/specs/P*-*-spec.md; do
  base=$(basename "$f")
  rg -q "$base" docs/architecture/specs/README.md docs/architecture/plans/README.md docs/architecture/todo/coverage-matrix.md \
    || echo "missing index coverage for $base"
done
```

Result: no output.

Marker scan:

```bash
marker_pattern='T''BD|TO''DO|FIX''ME|待''定|待''补|place''holder'
rg -n "$marker_pattern" docs/architecture/specs docs/architecture/plans docs/architecture/index.md docs/architecture/todo/README.md docs/architecture/todo/coverage-matrix.md
```

Accepted results:

- `docs/architecture/archive/completed/plans/P1-03-frontend-canvas-state-plan.md`: `insert placeholder` is a real frontend canvas state term.
- `docs/architecture/archive/completed/plans/P2-03-documentation-adr-and-runbooks-plan.md`: `dashboard link placeholder` is an explicit runbook template field to be filled by the implementing team.

Git visibility check:

```bash
git status --short docs/architecture/specs docs/architecture/plans docs/architecture/index.md docs/architecture/todo/README.md docs/architecture/todo/coverage-matrix.md
```

Result: generated architecture spec/plan/index files are visible as modified or untracked files in the current worktree.

No commit was created in this step because the current task is implementation and verification; version-control commit timing is left to the repository owner.
