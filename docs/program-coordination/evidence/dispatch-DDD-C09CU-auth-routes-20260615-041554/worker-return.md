# DDD-C09CU Worker Return

Task: DDD-C09CU /auth route compatibility seed

Worker:
- Kant `019ec7c6-77e6-7622-98af-c6983444466c`
- Sidecar evidence only; main implementation completed by coordinator while worker reviewed legacy contract.

Files changed:
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/AuthFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/AuthApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AuthCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/AuthApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/platform/AuthController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/platform/AuthControllerCompatibilityTest.java`
- `docs/program-coordination/evidence/dispatch-DDD-C09CU-auth-routes-20260615-sidecar/sidecar-review.md`

Implementation summary:
- Added final-module local auth facade/application/domain seed for `/auth/login`, `/auth/logout`, and `/auth/me`.
- Preserved legacy-compatible login response fields: `token`, `userId`, `tenantId`, `username`, `displayName`, and `role`.
- Added deterministic failed-login counting and five-attempt account lock behavior with `AUTH_004` message.
- Added bearer-token logout revocation with SHA-256 first-16-byte lowercase hex hash.
- Added `/auth/me` bearer-token lookup and revoked/unknown token rejection.
- Added WebFlux compatibility controller with standard `CompatibilityEnvelope` and `API_001` bad-request mapping.

Meaningful tests:
- `AuthApplicationServiceTest` covers successful login clearing failures, failed login lockout, logout revocation, and malformed/unknown token behavior.
- `AuthControllerCompatibilityTest` covers legacy route mapping, login body/header forwarding, success envelope, and `API_001` error envelope.

Accepted concerns:
- This is a compact deterministic compatibility seed, not durable auth persistence.
- Real JWT interoperability, Redis blacklist TTL, Spring Security context principal handling, and production auth filter cutover remain outside this route-parity batch.
