# Security Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Execute the architecture remediation package described in `../specs/P0-01-security-hardening-spec.md`.

**Architecture:** Start from the archived evidence and current repository verification, add failing tests around the confirmed behavior, then implement the smallest scoped changes that satisfy the package acceptance criteria. Keep unrelated refactors out of the package unless a test proves the boundary must change.

**Tech Stack:** Java 21, Spring Boot 3.2, WebFlux, MyBatis-Plus, Reactor, Redis, RocketMQ, React 18, TypeScript, Vite, Vitest, JUnit 5.

---

## Source Material

- Spec: `../specs/P0-01-security-hardening-spec.md`
- Source package: `../todo/p0/security-hardening/`
- Coverage matrix: `../todo/coverage-matrix.md`

## File Structure

- Read: `../specs/P0-01-security-hardening-spec.md`
- Read: `../todo/coverage-matrix.md`
- Read: `../todo/p0/security-hardening/plan.md`
- Modify: repository files named in the spec evidence for this package
- Test: focused tests created beside the affected backend or frontend code before implementation

### Task 1: Split local defaults from production requirements

**Files:**
- Read: `../specs/P0-01-security-hardening-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
   - Keep local development convenience in a local profile or sample env file.
   - Add production fail-fast checks for datasource username/password, event report secret, CORS origins, and actuator health details.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P0-01-security-hardening-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P0-01-security-hardening-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 2: Replace wildcard credentialed CORS

**Files:**
- Read: `../specs/P0-01-security-hardening-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
   - Reject `*` when `allowCredentials=true` outside local/dev.
   - Add a config test for wildcard + credentials.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P0-01-security-hardening-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P0-01-security-hardening-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 3: Secure public endpoints

**Files:**
- Read: `../specs/P0-01-security-hardening-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
   - Add signed-secret or HMAC validation to `/canvas/events/report`, `/canvas/execute/direct/*`, and `/canvas/trigger/behavior`.
   - Require authenticated admin/operator access for `/ops/**`.
   - Decide whether Swagger remains public only in dev.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P0-01-security-hardening-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P0-01-security-hardening-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 4: Sanitize generic errors

**Files:**
- Read: `../specs/P0-01-security-hardening-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
   - Return a stable generic message and log details server-side with correlation fields.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P0-01-security-hardening-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P0-01-security-hardening-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

### Task 5: Validate

**Files:**
- Read: `../specs/P0-01-security-hardening-spec.md`
- Read: `../todo/coverage-matrix.md`
- Modify: repository files named in this package spec evidence
- Test: focused tests created beside the affected code

Existing package notes:
   - Run backend unit tests.
   - Add targeted WebFlux security tests for `SecurityConfig`.
   - Confirm production-like profile fails fast without required env vars.

- [ ] **Step 1: Lock the failing behavior**

Read `../specs/P0-01-security-hardening-spec.md` and write the smallest failing test or documentation check that demonstrates this task gap before changing implementation files.

- [ ] **Step 2: Run the focused check before implementation**

Run the narrowest backend, frontend, or documentation command that exercises the new check. Expected result before implementation: the new check fails or the documentation diff shows the missing section.

- [ ] **Step 3: Implement the scoped change**

Change only the files required by this task and keep the behavior aligned with the acceptance criteria in `../specs/P0-01-security-hardening-spec.md`.

- [ ] **Step 4: Verify the task**

Run the same focused command again. Expected result after implementation: pass, or documentation check exits 0.

- [ ] **Step 5: Review the scoped diff**

Run `git diff -- .` and verify the diff only touches files justified by this task and the package spec.

