# Plan: Security Hardening

1. Split local defaults from production requirements.
   - Keep local development convenience in a local profile or sample env file.
   - Add production fail-fast checks for datasource username/password, event report secret, CORS origins, and actuator health details.

2. Replace wildcard credentialed CORS.
   - Reject `*` when `allowCredentials=true` outside local/dev.
   - Add a config test for wildcard + credentials.

3. Secure public endpoints.
   - Add signed-secret or HMAC validation to `/canvas/events/report`, `/canvas/execute/direct/*`, and `/canvas/trigger/behavior`.
   - Require authenticated admin/operator access for `/ops/**`.
   - Decide whether Swagger remains public only in dev.

4. Sanitize generic errors.
   - Return a stable generic message and log details server-side with correlation fields.

5. Validate.
   - Run backend unit tests.
   - Add targeted WebFlux security tests for `SecurityConfig`.
   - Confirm production-like profile fails fast without required env vars.
