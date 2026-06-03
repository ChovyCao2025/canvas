# Observability And Ops Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute the architecture remediation package described in `../specs/P1-04-observability-and-ops-spec.md`.

**Architecture:** Start from the archived evidence and current repository verification, add failing tests around the confirmed behavior, then implement the smallest scoped changes that satisfy the package acceptance criteria. Keep unrelated refactors out of the package unless a test proves the boundary must change.

**Tech Stack:** Java 21, Spring Boot 3.2, WebFlux, MyBatis-Plus, Reactor, Redis, RocketMQ, React 18, TypeScript, Vite, Vitest, JUnit 5.

---

## Source Material

- Spec: `../specs/P1-04-observability-and-ops-spec.md`
- Source package: `../todo/p1/observability-and-ops/`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Read: `../specs/P1-04-observability-and-ops-spec.md`
- Read: `../todo/coverage-matrix.md`
- Read: `../todo/p1/observability-and-ops/plan.md`
- Modify: repository files named in the spec evidence for this package
- Test: focused tests created beside the affected backend or frontend code before implementation

### Task 1: Add request/execution correlation ID propagation

**Files:**
- Read: `../specs/P1-04-observability-and-ops-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
- Source task has no additional notes beyond its title.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P1-04-observability-and-ops-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P1-04-observability-and-ops-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 2: Populate MDC consistently across boundedElastic and async execution paths

**Files:**
- Read: `../specs/P1-04-observability-and-ops-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
- Source task has no additional notes beyond its title.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P1-04-observability-and-ops-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P1-04-observability-and-ops-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 3: Decide tracing implementation: Micrometer Tracing with Brave/OTel or a simpler internal trace table first

**Files:**
- Read: `../specs/P1-04-observability-and-ops-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
- Source task has no additional notes beyond its title.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P1-04-observability-and-ops-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P1-04-observability-and-ops-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 4: Add deployable Prometheus alert rules and dashboard JSON/YAML

**Files:**
- Read: `../specs/P1-04-observability-and-ops-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
- Source task has no additional notes beyond its title.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P1-04-observability-and-ops-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P1-04-observability-and-ops-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 5: Add runbooks for DLQ, route rebuild, cache invalidation, and shutdown

**Files:**
- Read: `../specs/P1-04-observability-and-ops-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
- Source task has no additional notes beyond its title.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P1-04-observability-and-ops-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P1-04-observability-and-ops-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 6: Validate by running the app locally and checking logs plus `/actuator/prometheus`

**Files:**
- Read: `../specs/P1-04-observability-and-ops-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
- Source task has no additional notes beyond its title.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P1-04-observability-and-ops-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P1-04-observability-and-ops-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

