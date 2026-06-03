# Spec: Security Hardening

Source package: `docs/architecture/todo/p0/security-hardening/`

Coverage matrix: `docs/architecture/todo/coverage-matrix.md`


## Verification Status

Mixed: mostly confirmed, with one stale subclaim.

## Confirmed Problems

- `application.yml` still defaults datasource credentials to `root/root`.
- `canvas.events.report-secret` still has a weak default.
- CORS still uses `addAllowedOriginPattern("*")` with `allowCredentials(true)`.
- Swagger, direct execution, behavior trigger, notification websocket, and `/ops/**` are public in `SecurityConfig`.
- `GlobalExceptionHandler` still returns `"系统错误: " + e.getMessage()` for generic 500s.

## Corrected Finding

The previous "JWT Secret no startup validation" claim is stale. `JwtUtil` validates blank, default, and short secrets. The remaining task is to make deployment configuration explicit and tested.

## Evidence

- `backend/canvas-engine/src/main/resources/application.yml:8-9`
- `backend/canvas-engine/src/main/resources/application.yml:52-59`
- `backend/canvas-engine/src/main/resources/application.yml:166`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/WebConfig.java:36-45`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:58-70`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java:84`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/auth/util/JwtUtil.java:45-54`

## Acceptance Criteria

- No production profile can start with root DB credentials, blank JWT secret, weak event secret, wildcard credentialed CORS, or health details exposed.
- Public machine-to-machine endpoints require either signed request authentication, a gateway-only network boundary, or a documented allowlist.
- `/ops/**` requires admin/operator authorization.
- Generic 500 responses do not expose internal exception messages.
- Security tests cover CORS, public endpoints, ops endpoints, and startup validation.
