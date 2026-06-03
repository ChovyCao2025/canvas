# Architecture Spec/Plan Materialization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Materialize every active architecture todo package into priority-prefixed spec and plan files under `docs/architecture/specs/` and `docs/architecture/plans/`.

**Architecture:** Treat `docs/architecture/todo/` as the source of truth and create flat, scan-friendly files grouped by document type. The new files keep source links back to the todo package and coverage matrix, while `todo/` remains the priority queue and traceability root. Verification is scriptable: every todo package must have exactly one matching spec and plan file with a priority prefix.

**Tech Stack:** Markdown, zsh, `find`, `rg`, `git diff`.

---

## File Structure

- Create: `docs/architecture/specs/README.md`
- Create: `docs/architecture/plans/README.md`
- Create: `docs/architecture/specs/P0-01-security-hardening-spec.md`
- Create: `docs/architecture/plans/P0-01-security-hardening-plan.md`
- Create: `docs/architecture/specs/P0-02-reactive-threading-and-transactions-spec.md`
- Create: `docs/architecture/plans/P0-02-reactive-threading-and-transactions-plan.md`
- Create: `docs/architecture/specs/P0-03-canvas-state-data-consistency-spec.md`
- Create: `docs/architecture/plans/P0-03-canvas-state-data-consistency-plan.md`
- Create: `docs/architecture/specs/P0-04-execution-concurrency-safety-spec.md`
- Create: `docs/architecture/plans/P0-04-execution-concurrency-safety-plan.md`
- Create: `docs/architecture/specs/P0-05-production-resilience-and-dr-spec.md`
- Create: `docs/architecture/plans/P0-05-production-resilience-and-dr-plan.md`
- Create: `docs/architecture/specs/P0-06-data-security-and-tenant-isolation-spec.md`
- Create: `docs/architecture/plans/P0-06-data-security-and-tenant-isolation-plan.md`
- Create: `docs/architecture/specs/P1-01-dag-engine-and-handler-boundaries-spec.md`
- Create: `docs/architecture/plans/P1-01-dag-engine-and-handler-boundaries-plan.md`
- Create: `docs/architecture/specs/P1-02-api-contract-and-validation-spec.md`
- Create: `docs/architecture/plans/P1-02-api-contract-and-validation-plan.md`
- Create: `docs/architecture/specs/P1-03-frontend-canvas-state-spec.md`
- Create: `docs/architecture/plans/P1-03-frontend-canvas-state-plan.md`
- Create: `docs/architecture/specs/P1-04-observability-and-ops-spec.md`
- Create: `docs/architecture/plans/P1-04-observability-and-ops-plan.md`
- Create: `docs/architecture/specs/P1-05-release-deployment-governance-spec.md`
- Create: `docs/architecture/plans/P1-05-release-deployment-governance-plan.md`
- Create: `docs/architecture/specs/P2-01-testing-foundation-spec.md`
- Create: `docs/architecture/plans/P2-01-testing-foundation-plan.md`
- Create: `docs/architecture/specs/P2-02-cost-capacity-and-retention-spec.md`
- Create: `docs/architecture/plans/P2-02-cost-capacity-and-retention-plan.md`
- Create: `docs/architecture/specs/P2-03-documentation-adr-and-runbooks-spec.md`
- Create: `docs/architecture/plans/P2-03-documentation-adr-and-runbooks-plan.md`
- Create: `docs/architecture/specs/P2-04-dependency-abstraction-and-vendor-lock-in-spec.md`
- Create: `docs/architecture/plans/P2-04-dependency-abstraction-and-vendor-lock-in-plan.md`
- Create: `docs/architecture/specs/P2-05-compliance-data-governance-spec.md`
- Create: `docs/architecture/plans/P2-05-compliance-data-governance-plan.md`
- Create: `docs/architecture/specs/P2-06-frontend-accessibility-and-quality-spec.md`
- Create: `docs/architecture/plans/P2-06-frontend-accessibility-and-quality-plan.md`
- Create: `docs/architecture/specs/P3-01-platform-evolution-spec.md`
- Create: `docs/architecture/plans/P3-01-platform-evolution-plan.md`
- Create: `docs/architecture/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md`
- Create: `docs/architecture/plans/P3-02-service-decomposition-and-domain-boundaries-plan.md`
- Create: `docs/architecture/specs/P3-03-data-platform-architecture-spec.md`
- Create: `docs/architecture/plans/P3-03-data-platform-architecture-plan.md`
- Create: `docs/architecture/specs/P3-04-multi-datasource-isolation-spec.md`
- Create: `docs/architecture/plans/P3-04-multi-datasource-isolation-plan.md`
- Create: `docs/architecture/specs/P3-05-webflux-to-mvc-migration-spec.md`
- Create: `docs/architecture/plans/P3-05-webflux-to-mvc-migration-plan.md`
- Create: `docs/architecture/specs/P3-06-k8s-deployment-platform-spec.md`
- Create: `docs/architecture/plans/P3-06-k8s-deployment-platform-plan.md`
- Create: `docs/architecture/specs/P3-07-production-platform-components-spec.md`
- Create: `docs/architecture/plans/P3-07-production-platform-components-plan.md`
- Create: `docs/architecture/specs/P3-08-wecom-scrm-module-spec.md`
- Create: `docs/architecture/plans/P3-08-wecom-scrm-module-plan.md`
- Create: `docs/architecture/specs/P3-09-identity-event-and-tenant-platform-spec.md`
- Create: `docs/architecture/plans/P3-09-identity-event-and-tenant-platform-plan.md`
- Modify: `docs/architecture/index.md`
- Modify: `docs/architecture/todo/README.md`
- Modify: `docs/architecture/todo/coverage-matrix.md`

### Task 1: Create The Materialized Specs

**Files:**
- Read: `docs/architecture/todo/*/*/spec.md`
- Create: `docs/architecture/specs/*.md`

- [ ] **Step 1: Define the canonical package order**

Use this exact package map:

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

- [ ] **Step 2: Copy each package spec into the specs folder**

Run:

```bash
find docs/architecture/todo -mindepth 3 -maxdepth 3 -name spec.md | wc -l
```

Expected: `26`.

- [ ] **Step 3: Add source metadata to each materialized spec**

Each generated spec must start with the existing title followed by:

```markdown
Source package: `docs/architecture/todo/p0/security-hardening/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`
```

For other packages, use the equivalent priority and slug path from the canonical package order.

- [ ] **Step 4: Verify spec count**

Run:

```bash
find docs/architecture/specs -maxdepth 1 -name 'P*-*-spec.md' | wc -l
```

Expected: `26`.

### Task 2: Create The Materialized Plans

**Files:**
- Read: `docs/architecture/todo/*/*/plan.md`
- Create: `docs/architecture/plans/P*-*-plan.md`

- [ ] **Step 1: Generate a skill-compliant header for each package plan**

Each package plan must start with:

```markdown
# Security Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute the architecture remediation package described in `../specs/P0-01-security-hardening-spec.md`.

**Architecture:** Start from the archived evidence and current repository verification, add failing tests around the confirmed behavior, then implement the smallest scoped changes that satisfy the package acceptance criteria. Keep unrelated refactors out of the package unless a test proves the boundary must change.

**Tech Stack:** Java 21, Spring Boot 3.2, WebFlux, MyBatis-Plus, Reactor, Redis, RocketMQ, React 18, TypeScript, Vite, Vitest, JUnit 5.

---
```

For other packages, replace the title and spec filename with the matching package title and priority-prefixed spec file from the canonical package order.

- [ ] **Step 2: Preserve original package plan items as implementation tasks**

For each numbered line from the source plan, create a task with this structure:

```markdown
### Task 1: Split local defaults from production requirements

**Files:**
- Read: `../specs/P0-01-security-hardening-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files listed by the spec evidence for this package
- Test: package-specific tests added before implementation

- [ ] **Step 1: Lock the failing behavior**

Read the matching spec evidence and write the smallest failing test that demonstrates this task's current gap. If the task is documentation-only, write the exact documentation section before editing implementation files.

- [ ] **Step 2: Run the focused test or documentation check**

Run the narrowest command that exercises the new test or generated document. Expected result before implementation: failing test, changed documentation diff, or a documented repository evidence mismatch.

- [ ] **Step 3: Implement the scoped change**

Change only files needed for this task and keep package boundaries aligned with the matching spec acceptance criteria.

- [ ] **Step 4: Verify the task**

Run the focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review diff**

Run `git diff -- .` and verify the diff only touches files in this task scope.
```

For other packages, keep the same structure and replace the example task title and spec file with the current source plan item.

- [ ] **Step 3: Verify plan count**

Run:

```bash
find docs/architecture/plans -maxdepth 1 -name 'P*-*-plan.md' | wc -l
```

Expected: `27` because this materialization plan is also priority-prefixed.

### Task 3: Add Folder Indexes

**Files:**
- Create: `docs/architecture/specs/README.md`
- Create: `docs/architecture/plans/README.md`

- [ ] **Step 1: Write `specs/README.md`**

The file must list all 26 spec files in priority order and link to the matching plan file.

- [ ] **Step 2: Write `plans/README.md`**

The file must list this materialization plan plus all 26 package plan files in priority order and link to the matching spec file.

- [ ] **Step 3: Verify index links**

Run:

```bash
rg -n "P0-01-security-hardening|P3-09-identity-event-and-tenant-platform" docs/architecture/specs/README.md docs/architecture/plans/README.md
```

Expected: both files contain the first and last package links.

### Task 4: Update Architecture Entry Points

**Files:**
- Modify: `docs/architecture/index.md`
- Modify: `docs/architecture/todo/README.md`
- Modify: `docs/architecture/todo/coverage-matrix.md`

- [ ] **Step 1: Update the top-level architecture index**

Add links to `specs/README.md` and `plans/README.md` next to the existing `todo/` and `archive/` links.

- [ ] **Step 2: Update the todo README**

Add a note that flattened spec and plan files are available under `../specs/` and `../plans/`.

- [ ] **Step 3: Update coverage matrix package legend**

For each package legend entry, include both the source todo package and the materialized spec/plan filenames.

- [ ] **Step 4: Verify old path references**

Run:

```bash
rg -n "docs/architecture/(architecture-|remediation/|evolution/|backend-architecture|frontend-architecture|security-considerations|testing-strategy|deployment-guide|database-schema|tech-stack|coding-standards|api-spec-summary|brownfield-architecture|production-deployment)" docs/architecture/specs docs/architecture/plans docs/architecture/todo docs/architecture/index.md
```

Expected: no output.

### Task 5: Final Verification

**Files:**
- Read: `docs/architecture/specs/`
- Read: `docs/architecture/plans/`
- Read: `docs/architecture/index.md`
- Read: `docs/architecture/todo/README.md`
- Read: `docs/architecture/todo/coverage-matrix.md`

- [ ] **Step 1: Verify package parity**

Run:

```bash
spec_count=$(find docs/architecture/specs -maxdepth 1 -name 'P*-*-spec.md' | wc -l | tr -d ' ')
plan_count=$(find docs/architecture/plans -maxdepth 1 -name 'P*-*-plan.md' ! -name 'P0-00-*' | wc -l | tr -d ' ')
test "$spec_count" = 26 && test "$plan_count" = 26
```

Expected: exit code `0`.

- [ ] **Step 2: Verify no unresolved marker strings**

Run:

```bash
marker_pattern='T''BD|TO''DO|FIX''ME|待''定|待''补|place''holder'
rg -n "$marker_pattern" docs/architecture/specs docs/architecture/plans docs/architecture/index.md docs/architecture/todo/README.md docs/architecture/todo/coverage-matrix.md
```

Expected: no output.

- [ ] **Step 3: Verify all generated files are visible to git**

Run:

```bash
git status --short docs/architecture/specs docs/architecture/plans docs/architecture/index.md docs/architecture/todo/README.md docs/architecture/todo/coverage-matrix.md
```

Expected: generated spec and plan files appear as untracked or modified files; no unrelated files are changed by this task.
