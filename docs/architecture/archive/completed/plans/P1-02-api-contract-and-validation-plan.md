# API Contract And Validation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make backend controller boundaries typed and validated, make public endpoint contracts explicit, standardize error responses, and replace high-traffic frontend `any` API usage with concrete request/response models.

**Architecture:** Inventory controller routes first, then apply DTO-based Bean Validation at the boundary. Keep persistence objects out of public API contracts, centralize error mapping in `GlobalExceptionHandler`, and mirror the most-used backend contracts in frontend services.

**Tech Stack:** Java 21, Spring Boot WebFlux, Jakarta Bean Validation, JUnit 5, AssertJ, Maven, React 18, TypeScript, Vitest.

---

## Source Material

- Spec: `../specs/P1-02-api-contract-and-validation-spec.md`
- Source package: `../../../reviewed-packages/p1/api-contract-and-validation/`
- Coverage matrix: `../../../reviewed-packages/coverage-matrix.md`

## File Structure

- Inventory: `docs/architecture/evidence/P1-02-api-contract-inventory.md`
- Error model: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java`
- Error model: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/R.java`
- Error model: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/ErrorCode.java`
- Security routes: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Controller: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Controller: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`
- Controller: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventDefinitionController.java`
- Controller: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DataSourceConfigController.java`
- Controller: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- DTO: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java`
- DTO: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasUpdateReq.java`
- DTO: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/EventReportReq.java`
- DTO: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudiencePreviewReq.java`
- DTO: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/CdpBatchTagReq.java`
- Frontend service: `frontend/src/services/api.ts`
- Frontend service: `frontend/src/services/audienceApi.ts`
- Frontend service: `frontend/src/services/cdpApi.ts`
- Frontend service: `frontend/src/services/dataSourceConfigApi.ts`
- Frontend types: `frontend/src/types/canvas.ts`
- Backend tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/GlobalExceptionHandlerTest.java`
- Backend tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/ApiDefinitionControllerTest.java`
- Backend tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTest.java`
- Backend tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DataSourceConfigControllerTest.java`
- Backend tests: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerTest.java`
- Frontend tests: `frontend/src/services/api.test.ts`

### Task 1: Inventory all controller endpoints

**Files:**
- Docs: `docs/architecture/evidence/P1-02-api-contract-inventory.md`
- Plan: `docs/architecture/archive/completed/plans/P1-02-api-contract-and-validation-plan.md`
- Spec: `docs/architecture/archive/completed/specs/P1-02-api-contract-and-validation-spec.md`

- [x] List every `*Controller.java` with route prefix, method, request type, response type, auth policy, and current validation status.
- [x] Identify endpoints that expose persistence objects or loosely typed maps.
- [x] Prioritize create/update/execute/report endpoints for the first implementation pass.

Run:

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas/web -name '*Controller.java' | sort > /tmp/p1-02-controller-files.txt
rg -n "@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping|RequestMapping)|@Valid|@Validated" backend/canvas-engine/src/main/java/org/chovy/canvas/web backend/canvas-engine/src/main/java/org/chovy/canvas/dto > /tmp/p1-02-controller-routes.txt
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: inventory names all controller routes and marks the first endpoints that need DTO boundary work.

### Task 2: Apply DTO boundary validation to high-risk endpoints

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventDefinitionController.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DataSourceConfigController.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasCreateReq.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasUpdateReq.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/EventReportReq.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/audience/AudiencePreviewReq.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/AudienceControllerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/controller/DataSourceConfigControllerTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerTest.java`

- [x] Put `@Validated` on selected controllers and `@Valid` on request-body parameters.
- [x] Add field-level constraints to create/update/execute/report DTOs.
- [x] Cover malformed body, missing required field, invalid enum, and too-long string cases.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=AudienceControllerTest,DataSourceConfigControllerTest,ExecutionControllerTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: invalid requests return structured 400 responses and service methods are not invoked for invalid request bodies.

### Task 3: Introduce a stable error code model

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/ErrorCode.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/R.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/GlobalExceptionHandlerTest.java`

- [x] Map validation failures, auth failures, forbidden access, not-found responses, conflict responses, and generic server failures to stable codes.
- [x] Ensure `R.fail()` includes code, public message, and correlation identifier when present.
- [x] Keep internal exception messages out of client-visible generic failures.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=GlobalExceptionHandlerTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: error handler tests assert one response shape for validation, authorization, domain, and generic failures.

### Task 4: Make public endpoint contracts explicit

**Files:**
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/security/PublicTriggerAuthService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventReportAuthService.java`
- Production: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`
- DTO: `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/EventReportReq.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/security/PublicTriggerAuthServiceTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerMachineAuthTest.java`
- Docs: `docs/architecture/evidence/P1-02-api-contract-inventory.md`

- [x] Document request schema, auth requirement, and error codes for event report, direct execution, behavior trigger, Swagger/API docs, and `/ops/**`.
- [x] Keep machine-auth failure behavior aligned with the stable error model.
- [x] Cover unsigned, malformed signed, expired signed, and valid signed requests.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=SecurityConfigRouteTest,PublicTriggerAuthServiceTest,ExecutionControllerMachineAuthTest test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: public endpoint tests pass and inventory records schema, auth, and error contract for each public route.

### Task 5: Type frontend service calls

**Files:**
- Frontend service: `frontend/src/services/api.ts`
- Frontend service: `frontend/src/services/audienceApi.ts`
- Frontend service: `frontend/src/services/cdpApi.ts`
- Frontend service: `frontend/src/services/dataSourceConfigApi.ts`
- Frontend types: `frontend/src/types/canvas.ts`
- Frontend types: `frontend/src/types/index.ts`
- Test: `frontend/src/services/api.test.ts`

- [x] Replace high-traffic `any` request and response types for canvas, execution, metadata, auth, audience, CDP, and datasource APIs.
- [x] Keep response wrapper types aligned with backend `R` and error code changes.
- [x] Cover typed request serialization and error response parsing in Vitest.

Run:

```bash
cd frontend && npm run test -- src/services/api.test.ts
cd frontend && npm run build
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: service tests and production build pass; high-traffic frontend service calls no longer use `any` for request or response bodies.

### Task 6: Validate API contract package

**Files:**
- Docs: `docs/architecture/evidence/P1-02-api-contract-inventory.md`
- Plan: `docs/architecture/archive/completed/plans/P1-02-api-contract-and-validation-plan.md`
- Spec: `docs/architecture/archive/completed/specs/P1-02-api-contract-and-validation-spec.md`

- [x] Run backend focused API tests.
- [x] Run frontend service tests and build.
- [x] Run backend module tests after focused checks pass.

Run:

```bash
cd backend && mvn -pl canvas-engine -Dtest=GlobalExceptionHandlerTest,ApiDefinitionControllerTest,AudienceControllerTest,DataSourceConfigControllerTest,ExecutionControllerTest,SecurityConfigRouteTest,PublicTriggerAuthServiceTest,ExecutionControllerMachineAuthTest test
cd frontend && npm run test -- src/services/api.test.ts
cd frontend && npm run build
cd backend && mvn -pl canvas-engine test
# Do not stage or commit in this session unless the user explicitly asks.
```

Expected: backend API tests, frontend service tests, frontend build, and backend module tests pass; inventory records remaining low-traffic endpoints for later package ownership.
