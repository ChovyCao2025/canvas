# Security Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the Spring Boot engine refuse unsafe production security configuration, require authentication for privileged routes, protect public machine-trigger endpoints, and stop leaking internal exception messages.

**Architecture:** Keep local developer defaults isolated from production profiles. Enforce production startup checks through explicit config guards, enforce route policy in Spring Security and endpoint-specific HMAC services, and verify behavior through WebFlux security/config tests before broad backend regression.

**Tech Stack:** Java 21, Spring Boot WebFlux, Spring Security, Reactor, JUnit 5, AssertJ, Maven, YAML application profiles.

**Implementation Status:** Focused backend scope implemented and verified. Production-like startup guards, restricted production CORS/Springdoc settings, public trigger HMAC checks, `/ops/**` authorization, and generic 500 sanitization are in place. Route-level public access remains intentional for HMAC/ticket-authenticated machine endpoints.

---

## Source Material

- Spec: `../specs/P0-01-security-hardening-spec.md`
- Source package: `../../../reviewed-packages/p0/security-hardening/`
- Coverage matrix: `../../../reviewed-packages/coverage-matrix.md`

## File Structure

- Config: `backend/canvas-engine/src/main/resources/application.yml`
- Config: `backend/canvas-engine/src/main/resources/application-prod.yml`
- Security guard: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ProductionConfigGuard.java`
- Security guard: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ProductionSecurityValidator.java`
- CORS: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/WebConfig.java`
- Routes: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Public trigger auth: `backend/canvas-engine/src/main/java/org/chovy/canvas/security/CanvasHmacVerifier.java`
- Public trigger auth: `backend/canvas-engine/src/main/java/org/chovy/canvas/security/PublicTriggerAuthService.java`
- Public trigger auth: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventReportAuthService.java`
- Error model: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java`
- Error model: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/R.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionConfigGuardTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionSecurityValidatorTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/WebConfigTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRoleTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/security/PublicTriggerAuthServiceTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerMachineAuthTest.java`
- Tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/GlobalExceptionHandlerTest.java`
- Evidence: `docs/architecture/evidence/P0-01-security-hardening.md`

### Task 1: Split local defaults from production requirements

**Files:**
- Production: `backend/canvas-engine/src/main/resources/application.yml`
- Production: `backend/canvas-engine/src/main/resources/application-prod.yml`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ProductionConfigGuard.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ProductionSecurityValidator.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionConfigGuardTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionSecurityValidatorTest.java`

- [x] Move root DB credentials, weak event secret values, wildcard CORS origins, Swagger/API docs, and detailed health exposure out of production defaults.
- [x] Make production startup fail for root/root datasource credentials, blank or weak `CANVAS_JWT_SECRET`, weak `canvas.events.report-secret`, wildcard credentialed CORS, detailed actuator health, and enabled Springdoc docs.
- [x] Keep local development examples explicit in non-production profile settings or sample environment documentation.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=ProductionConfigGuardTest,ProductionSecurityValidatorTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: production-like configuration tests fail before the guard change and pass after it; the diff contains only config guard files, tests, and this plan. No commit is created by default.

### Task 2: Replace wildcard credentialed CORS

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/WebConfig.java`
- Production: `backend/canvas-engine/src/main/resources/application.yml`
- Production: `backend/canvas-engine/src/main/resources/application-prod.yml`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/WebConfigTest.java`

- [x] Replace `addAllowedOriginPattern("*")` with configured origins for non-local profiles.
- [x] Reject `*` when credentials are enabled outside local development.
- [x] Cover allowed-origin and rejected-wildcard cases in `WebConfigTest`.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=WebConfigTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: local configured origins still work; production wildcard plus credentials is rejected during configuration or test setup.

### Task 3: Secure public endpoints

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/security/CanvasHmacVerifier.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/security/PublicTriggerAuthService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventReportAuthService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRoleTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/security/PublicTriggerAuthServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerMachineAuthTest.java`

- [x] Require signed HMAC or shared-secret authentication for `/canvas/events/report`, `/canvas/execute/direct/**`, and `/canvas/trigger/behavior`.
- [x] Require admin/operator authorization for `/ops/**`.
- [x] Restrict Swagger and API-doc routes to the non-production policy chosen in `SecurityConfigRouteTest`.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest,SecurityConfigRoleTest,PublicTriggerAuthServiceTest,ExecutionControllerMachineAuthTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: unsigned public trigger requests are rejected, signed requests are accepted, `/ops/**` is unavailable to anonymous and non-operator users, and Swagger policy is profile-aware.

### Task 4: Sanitize generic errors

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/R.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/ErrorCode.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/GlobalExceptionHandlerTest.java`

- [x] Return a stable generic 500 response that does not include `Throwable#getMessage()`.
- [x] Preserve detailed exception information in server logs with request or execution correlation fields.
- [x] Align generic failure output with the shared `R` and `ErrorCode` response model.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=GlobalExceptionHandlerTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: generic 500 responses contain the public error code/message only; test fixtures with sensitive exception text do not expose that text to clients.

### Task 5: Validate the security hardening package

**Files:**
- Docs: `docs/architecture/evidence/P0-01-security-hardening.md`
- Plan: `docs/architecture/archive/completed/plans/P0-01-security-hardening-plan.md`
- Spec: `docs/architecture/archive/completed/specs/P0-01-security-hardening-spec.md`

- [x] Record the final commands, profile assumptions, and remaining security decisions in the evidence file.
- [x] Run the security package tests as a group.
- [x] Run the backend module test suite after the focused checks pass.

Run:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=ProductionConfigGuardTest,ProductionSecurityValidatorTest,WebConfigTest,SecurityConfigRouteTest,SecurityConfigRoleTest,PublicTriggerAuthServiceTest,ExecutionControllerMachineAuthTest,GlobalExceptionHandlerTest,EventDefinitionControllerTest,ExecutionControllerTest test
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: focused security tests and the backend module suite pass; evidence states the exact production profile values used for verification.
