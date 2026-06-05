# P0-01 Security Hardening Evidence

## Implemented Behavior

- Production-like profiles fail startup when local defaults are still active: root datasource credentials, blank/short/default JWT secret, weak/default event report secret, wildcard credentialed CORS, detailed health output, or enabled Springdoc API docs/Swagger UI.
- `application-prod.yml` requires deployment-provided secrets and disables Springdoc API docs and Swagger UI.
- CORS keeps wildcard support only for non-production developer profiles; production-like profiles reject `*` when credentials are allowed.
- `/ops/**` requires ADMIN/SUPER_ADMIN authorization.
- Public machine-trigger endpoints remain anonymous at the Spring Security route layer but are authenticated in controllers through HMAC signatures:
  - `POST /canvas/events/report`
  - `POST /canvas/execute/direct/{canvasId}`
  - `POST /canvas/trigger/behavior`
- Notification websocket remains anonymous at the route layer so the handshake can occur, but the websocket ticket workflow remains responsible for connection authorization.
- Generic 500 responses return a stable `"系统错误"` message plus `traceId`; internal exception messages stay in server logs.
- Datasource credential encryption and production cipher-key default rejection are covered by P0-06 and the production guard.

## Focused Verification

Command:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine -Dtest=ProductionConfigGuardTest,ProductionSecurityValidatorTest,WebConfigTest,SecurityConfigRouteTest,SecurityConfigRoleTest,PublicTriggerAuthServiceTest,ExecutionControllerMachineAuthTest,GlobalExceptionHandlerTest,EventDefinitionControllerTest,ExecutionControllerTest test
```

Result: 28 tests, 0 failures, 0 errors.

Module command:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -pl canvas-engine test
```

Result: 439 tests, 0 failures, 0 errors.

Expected log noise:

- `GlobalExceptionHandlerTest` logs synthetic `boom` and sensitive-message exceptions intentionally.
- `ApiCallHandlerRateLimitTest` and `AsyncTaskServiceTest` log synthetic downstream failures intentionally.

## Remaining Risk

- Swagger/API-doc paths are still `permitAll` in `SecurityConfig`; production safety depends on `application-prod.yml` disabling Springdoc and `ProductionSecurityValidator` rejecting enabled Springdoc flags in production-like profiles.
- Local `application.yml` intentionally keeps developer defaults for datasource, CORS, and event secrets; production-like profiles must never run without the prod guard.
