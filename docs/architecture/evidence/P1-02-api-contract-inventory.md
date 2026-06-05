# P1-02 API Contract Inventory

Date: 2026-06-04

## Scan Summary

- Controller files under `backend/canvas-engine/src/main/java/org/chovy/canvas/web`: 40.
- Handler methods annotated with `@GetMapping`, `@PostMapping`, `@PutMapping`, `@PatchMapping`, or `@DeleteMapping`: 206.
- Validation annotations in web/dto packages after this pass: 14 matches.
- First-pass implementation scope: canvas create/update, direct/dry-run execution, behavior trigger, event report, datasource create/update, audience preview, and CDP batch tag operations.

Commands:

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas/web -name '*Controller.java' | wc -l
rg -n "@(GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)" backend/canvas-engine/src/main/java/org/chovy/canvas/web | wc -l
rg -n "@Valid|@Validated" backend/canvas-engine/src/main/java/org/chovy/canvas/web backend/canvas-engine/src/main/java/org/chovy/canvas/dto | wc -l
```

## Controller Inventory

| Controller | Route Prefix | Handlers | Auth Policy | Validation Status | Contract Risk |
| --- | --- | ---: | --- | --- | --- |
| AbExperimentController | `/canvas/ab-experiments` | 8 | JWT | pending | Exposes `AbExperimentDO` / `AbExperimentGroupDO` |
| AdminController | `/admin/users` | 4 | tenant admin role | partial local req DTO | returns `SysUserDO` |
| AiPromptTemplateController | `/ai/prompt-templates` | 6 | JWT | pending | service DTOs, lower risk |
| AiProviderController | `/ai/providers` | 6 | JWT | pending | service DTOs, lower risk |
| AnalyticsController | `/analytics` | 4 | JWT | query only | read-only |
| ApiDefinitionController | `/canvas/api-definitions` | 4 | tenant admin role | domain validation only | exposes `ApiDefinitionDO`, accepts `JsonNode` update |
| AsyncTaskController | `/canvas/async-tasks` | 2 | JWT | query/path only | read-only |
| AudienceController | `/canvas/audiences` | 10 | JWT | first pass applied to preview | create/update still expose `AudienceDefinitionDO` |
| AuthController | `/auth` | 3 | login public, rest JWT | local request DTO | auth boundary |
| CanvasBatchOperationController | `/canvas/batch` | 1 | JWT | pending | batch mutation contract needs DTO review |
| CanvasController | `/canvas` | 23 | mixed JWT / tenant admin actions | first pass applied to create/update | still returns `CanvasDO` / `CanvasVersionDO` |
| CanvasExecutionManagementController | `/canvas/execution` | 2 | JWT | query/path only | approval/reject mutation |
| CanvasExecutionRequestManagementController | `/canvas/execution-requests` | 3 | JWT | pending | replay request contract |
| CanvasMqTriggerRejectedController | `/canvas/mq-trigger-rejected` | 3 | JWT | query/path only | operational DO exposure |
| CanvasStatsController | `/canvas/{id}` | 5 | JWT | query/path only | read-only |
| CanvasUserController | `/canvas/{id}/users` | 3 | JWT | query/path only | returns `CanvasExecutionDO` for execution detail |
| CdpTagOperationController | `/cdp/tag-operations` | 4 | JWT | first pass applied to create | returns operation DO |
| CdpUserController | `/cdp/users` | 7 | JWT | pending | tag write contract already typed in frontend |
| ChannelConnectorController | `/channels/connectors` | 7 | JWT | pending | connector mutation payloads need DTO review |
| DataSourceConfigController | `/canvas/data-sources` | 5 | tenant admin role | first pass applied to create/update | write request moved off `DataSourceConfigDO`; read still returns DO |
| DeliveryReceiptController | `/delivery/receipts` | 1 | JWT or integration route depending security chain | pending | public/integration receipt contract needs review |
| DlqController | `/canvas/dlq` | 3 | JWT | query/path only | operational DO exposure |
| EventDefinitionController | `/canvas` | 5 | event report public HMAC; CRUD JWT | first pass applied to report | CRUD still exposes `EventDefinitionDO` |
| ExecutionController | `/canvas` | 3 | direct/behavior public HMAC; dry-run JWT | first pass applied | raw body validation preserves verify-before-parse |
| ExecutionRerunController | `/execution-reruns` | 3 | JWT | typed service DTOs | returns audit DO |
| HomeOverviewController | `/canvas/home` | 1 | JWT | query only | read-only |
| IdentityTypeController | `/canvas/identity-types` | 4 | JWT | pending | exposes `IdentityTypeDO` |
| MessageDeliveryController | `/message-deliveries` | 5 | JWT | query/path only | operational DO exposure |
| MetaController | `/meta` | 21 | JWT | query/path only | mostly read-only metadata |
| MqDefinitionController | `/canvas/mq-definitions` | 4 | JWT | pending | exposes `MqMessageDefinitionDO` |
| NotificationController | `/canvas/notifications` | 6 | JWT except websocket path ticket flow | local DTOs / query | user notification boundary |
| OpsController | mixed `/ops/**` and `/canvas/**` | 6 | admin role for `/ops/**`; tenant admin for template actions | pending | operator mutation DTOs need review |
| PluginRegistryController | `/canvas/plugins` | 2 | JWT | pending | simple enable toggle |
| SystemOptionController | `/admin/system-options` | 2 | admin role | pending | exposes `SystemOptionDO` |
| TagDefinitionController | `/canvas/tag-definitions` | 8 | JWT | pending | exposes tag DOs |
| TagImportController | `/canvas/tag-imports` | 5 | JWT | mixed multipart/API push | import payloads need review |
| TagImportSourceController | `/canvas/tag-import-sources` | 5 | tenant admin role | pending | exposes `TagImportSourceDO` |
| TenantController | `/admin/tenants` | 5 | super admin role | local create req | returns `TenantDO` |
| TestUserController | `/test-users` | 6 | JWT | typed service DTOs | test tooling API |
| UserInputController | `/user-input` | 1 | JWT/public depends route policy | pending | response payload contract needs review |

## Public Endpoint Contracts

| Endpoint | Auth | Request Contract | Error Contract |
| --- | --- | --- | --- |
| `POST /canvas/execute/direct/{canvasId}` | HMAC headers `X-Canvas-Timestamp` and `X-Canvas-Signature`; no JWT required | raw body is verified first, then parsed as `DirectCallReq`; `userId` required by controller, `idempotencyKey` and `graphJson` capped by validation | `401 AUTH_002` for missing/invalid/expired signature, `400 API_001/API_002` for validation or malformed JSON |
| `POST /canvas/trigger/behavior` | HMAC headers; no JWT required | raw body verified first, then parsed as `BehaviorTriggerReq`; `canvasId`, `userId`, `eventCode`, `eventId` required | `401 AUTH_002`, `400 API_001/API_002` |
| `POST /canvas/events/report` | HMAC headers; no JWT required | raw body verified first, then parsed as `EventReportReq`; `eventCode` and `userId` required, `idempotencyKey` capped | `401 AUTH_002`, `400 API_001/API_002` |
| `/swagger-ui/**`, `/v3/api-docs/**` | public | read-only API documentation | standard Spring static/doc responses |
| `/ops/**` | admin role | operator-only mutation/read endpoints | `401 AUTH_002`, `403 AUTH_003` |

## First-Pass Changes Verified

- Added `spring-boot-starter-validation`.
- Added `R.errorCode` while preserving numeric `R.code`.
- Added stable API error codes `API_001` through `API_005`.
- Added global handling for Bean Validation, malformed WebFlux input, `ResponseStatusException`, Spring `AccessDeniedException`, and generic failures.
- Added DTO constraints for canvas create/update, event report, audience preview, CDP batch tag operation, execution raw bodies, and datasource write requests.
- Moved datasource create/update request binding from `DataSourceConfigDO` to `DataSourceConfigReq`.
- Updated frontend `R<T>` and error classification to include `errorCode` and `traceId`.
- Replaced production `any` service types in `frontend/src/services` and `frontend/src/types`; remaining `any` matches are test-only mock casts.

## Verification

Backend focused API tests:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=GlobalExceptionHandlerTest,ApiRequestValidationTest,AudienceControllerTest,DataSourceConfigControllerTest,ExecutionControllerTest,EventDefinitionControllerTest,CdpTagOperationControllerTest,SecurityConfigRouteTest,PublicTriggerAuthServiceTest,ExecutionControllerMachineAuthTest test
```

Result: 35 tests, 0 failures.

Frontend service tests:

```bash
cd frontend && npm run test -- src/services/api.test.ts src/services/apiResilience.test.ts
```

Result: 2 test files, 5 tests, 0 failures.

Frontend production build:

```bash
cd frontend && npm run build
```

Result: `tsc && vite build` completed successfully.

Backend module tests:

```bash
cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
```

Result: 628 tests, 0 failures, 1 skipped.

## Remaining Contract Debt

- Many low-frequency admin/metadata controllers still expose persistence DOs directly.
- `ApiDefinitionController` update still accepts `JsonNode` to support explicit null semantics; it needs a typed patch DTO if this API becomes external.
- Audience create/update still bind `AudienceDefinitionDO`.
- Tag, MQ, AB experiment, import source, and system option mutation endpoints still need DTO boundary work.
- Operational read APIs still return DOs; a response DTO layer should be introduced before these APIs are treated as stable external contracts.
