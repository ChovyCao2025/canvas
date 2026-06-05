# Spec: Security Hardening

Source package: `docs/architecture/active/reviewed-packages/p0/security-hardening/`

Coverage matrix: `docs/architecture/active/reviewed-packages/coverage-matrix.md`


## Verification Status

Implemented for the P0-01 focused backend scope, with focused verification passing for production startup guards, CORS policy, privileged route authorization, public trigger HMAC verification, and generic error-message sanitization.

Verification evidence:

- `ProductionConfigGuardTest`
- `ProductionSecurityValidatorTest`
- `WebConfigTest`
- `SecurityConfigRouteTest`
- `SecurityConfigRoleTest`
- `PublicTriggerAuthServiceTest`
- `ExecutionControllerMachineAuthTest`
- `EventDefinitionControllerTest`
- `ExecutionControllerTest`
- `GlobalExceptionHandlerTest`
- Command: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=ProductionConfigGuardTest,ProductionSecurityValidatorTest,WebConfigTest,SecurityConfigRouteTest,SecurityConfigRoleTest,PublicTriggerAuthServiceTest,ExecutionControllerMachineAuthTest,GlobalExceptionHandlerTest,EventDefinitionControllerTest,ExecutionControllerTest test`
- Module command: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test`

## Resolved Problems

- Production-like profiles fail fast for root datasource credentials, blank/short/default JWT secrets, weak/default event report secrets, wildcard credentialed CORS, exposed health details, enabled Springdoc API docs, and enabled Swagger UI.
- `application-prod.yml` requires deployment-provided secrets and disables Springdoc API docs and Swagger UI.
- CORS wildcard origin remains available only for local developer convenience; production-like profiles reject it.
- `/ops/**` requires ADMIN/SUPER_ADMIN authorization.
- Public trigger endpoints use HMAC verification before parsing or dispatching caller-controlled payloads.
- Generic 500 responses no longer expose `Throwable#getMessage()` and return a trace id for log correlation.

## Evidence

- `backend/canvas-engine/src/main/resources/application.yml`
- `backend/canvas-engine/src/main/resources/application-prod.yml`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ProductionConfigGuard.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ProductionSecurityValidator.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/WebConfig.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/security/CanvasHmacVerifier.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/security/PublicTriggerAuthService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventReportAuthService.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventDefinitionController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java`
- `docs/architecture/evidence/P0-01-security-hardening.md`

## Remaining Risks

- Swagger/API-doc paths are still route-level `permitAll`, but production profiles disable Springdoc and startup validation rejects enabled Springdoc flags.
- Public trigger endpoints are intentionally anonymous at the route layer and protected by controller-level HMAC verification.
- Notification websocket is intentionally anonymous at the route layer and depends on the websocket ticket flow for connection authorization.

## Acceptance Criteria

- No production profile can start with root DB credentials, blank JWT secret, weak event secret, wildcard credentialed CORS, or health details exposed.
- Public machine-to-machine endpoints require either signed request authentication, a gateway-only network boundary, or a documented allowlist.
- `/ops/**` requires admin/operator authorization.
- Generic 500 responses do not expose internal exception messages.
- Security tests cover CORS, public endpoints, ops endpoints, and startup validation.
