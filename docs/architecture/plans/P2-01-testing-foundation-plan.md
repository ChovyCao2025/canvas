# Testing Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute the architecture remediation package described in `../specs/P2-01-testing-foundation-spec.md`.

**Architecture:** Start from the archived evidence and current repository verification, add failing tests around the confirmed behavior, then implement the smallest scoped changes that satisfy the package acceptance criteria. Keep unrelated refactors out of the package unless a test proves the boundary must change.

**Tech Stack:** Java 21, Spring Boot 3.2, WebFlux, MyBatis-Plus, Reactor, Redis, RocketMQ, React 18, TypeScript, Vite, Vitest, JUnit 5.

---

## Source Material

- Spec: `../specs/P2-01-testing-foundation-spec.md`
- Source package: `../todo/p2/testing-foundation/`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Read: `../specs/P2-01-testing-foundation-spec.md`
- Read: `../todo/coverage-matrix.md`
- Read: `../todo/p2/testing-foundation/plan.md`
- Modify: repository files named in the spec evidence for this package
- Test: focused tests created beside the affected backend or frontend code before implementation

### Task 1: Define test layers

**Files:**
- Read: `../specs/P2-01-testing-foundation-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
   - Unit tests for pure logic and validators.
   - Integration tests for DB/Redis/MQ.
   - Frontend component/hook tests for editor workflows.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P2-01-testing-foundation-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P2-01-testing-foundation-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 2: Add Testcontainers or equivalent integration harness

**Files:**
- Read: `../specs/P2-01-testing-foundation-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
- Source task has no additional notes beyond its title.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P2-01-testing-foundation-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P2-01-testing-foundation-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 3: Add tests for P0 packages before implementation

**Files:**
- Read: `../specs/P2-01-testing-foundation-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
- Source task has no additional notes beyond its title.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P2-01-testing-foundation-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P2-01-testing-foundation-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 4: Add CI commands for backend and frontend tests

**Files:**
- Read: `../specs/P2-01-testing-foundation-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
- Source task has no additional notes beyond its title.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P2-01-testing-foundation-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P2-01-testing-foundation-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 5: Track residual manual verification where infrastructure cannot be containerized locally

**Files:**
- Read: `../specs/P2-01-testing-foundation-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
- Source task has no additional notes beyond its title.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P2-01-testing-foundation-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P2-01-testing-foundation-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

