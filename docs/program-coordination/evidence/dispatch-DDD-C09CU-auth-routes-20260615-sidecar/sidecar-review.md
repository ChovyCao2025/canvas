# DDD-C09CU /auth Compatibility Sidecar Review

Scope: focused read-only review of legacy `AuthController` behavior and current
`canvas-web` controller/test compatibility patterns. No production or test source
files were edited.

## Legacy route list

Legacy source:
`backend/canvas-engine/src/main/java/org/chovy/canvas/web/AuthController.java`

Routes under `@RequestMapping("/auth")`:

| Method | Route | Authentication | Expected compatibility envelope / behavior |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Public in old `SecurityConfig` via `.pathMatchers("/auth/login").permitAll()` | Request body has `username`, `password`. Success returns `code=0`, `message=success`, absent/null `errorCode`, `traceId`, and `data` with `token`, `userId`, `tenantId`, `username`, `displayName`, `role`. |
| `POST` | `/auth/logout` | Bearer token optional at controller level, but route is protected by security unless the final security config says otherwise | Always returns success envelope `code=0`, `message=success`, `data=null`. If `Authorization` starts with exact prefix `Bearer ` and token parses with remaining lifetime, old behavior writes a server-side revoked-token marker. Malformed/expired token is swallowed by the controller and still returns success if the request reaches it. |
| `GET` | `/auth/me` | Requires authenticated `ReactiveSecurityContextHolder` principal with JWT `Claims` | Reads `Claims.subject` as user id, reloads current user, and returns same user data shape as login except no `token` is set by the controller. Missing/deleted user throws `IllegalArgumentException("用户不存在")`. |

Current final-module pattern:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/platform/TestUserController.java`
  wraps route responses with local `CompatibilityEnvelope<T>(code, message,
  errorCode, data, traceId)`.
- Success is consistently `code=0`, `message="success"`, `errorCode=null`,
  `traceId=null`.
- `IllegalArgumentException` is commonly mapped to HTTP 400 with
  `code=400`, `errorCode="API_001"`, `data=null`.
- Focused compatibility tests usually bind the controller directly with
  `WebTestClient.bindToController(...)`, assert route shapes and envelope fields,
  and use a small recording facade instead of booting the app.

No final `canvas-web` `/auth` controller or `/auth` compatibility test was present
in the inspected final-module source tree at review time.

## Edge cases worth meaningful coverage

- Login success should assert the full compatibility data contract, especially
  `token`, `userId`, `tenantId`, `username`, `displayName`, and `role`.
- Invalid username, disabled user, and wrong password should remain
  indistinguishable to callers; legacy controller throws
  `IllegalArgumentException("用户名或密码错误")` and records a failed attempt.
- Five failed attempts lock the username for 15 minutes in legacy behavior. The
  externally visible branch is HTTP 429 from `ResponseStatusException` with
  `AUTH_004: 账号已锁定，请 15 分钟后重试`.
- Login success clears only the failed-attempt key for that username; it does not
  delete the locked key.
- Logout should cover all three header states: no `Authorization`, non-`Bearer`
  header, and valid `Bearer ` token. All should return success if allowed to hit
  the controller, but only a valid unexpired bearer token should create a revoked
  token marker.
- `Bearer ` is case-sensitive and requires the trailing space in old code.
- Logout should preserve the token hash algorithm if interoperating with the old
  auth filter: SHA-256 of the raw token, first 16 bytes rendered as 32 lowercase
  hex chars.
- `/auth/me` should use the authenticated principal subject as the user id and
  reload current user fields instead of trusting stale `displayName`, `role`, or
  `tenantId` claims. This matters for role/display-name updates after token issue.
- `/auth/me` data should not include password or enabled state, and should not
  accidentally return the bearer token unless the new contract intentionally adds
  one.

## Old-engine coupling strings to avoid in new `/auth` files

Do not import or depend on old `canvas-engine` auth, DAL, or security internals
from final modules. Specific strings to keep out of new `/auth` files unless
there is an explicit bridge decision:

- `org.chovy.canvas.auth.domain.SysUserService`
- `org.chovy.canvas.auth.dto.LoginReq`
- `org.chovy.canvas.auth.dto.LoginResp`
- `org.chovy.canvas.auth.util.JwtUtil`
- `org.chovy.canvas.dal.dataobject.SysUserDO`
- `org.chovy.canvas.dal.mapper.SysUserMapper`
- `org.chovy.canvas.config.JwtAuthFilter`
- `org.chovy.canvas.config.SecurityConfig`
- `org.chovy.canvas.common.R`
- `org.chovy.canvas.common.ErrorCode`
- `backend/canvas-engine`
- `canvas-engine-jwt-secret-key`

Redis/key strings observed in old auth behavior:

- `canvas:login:fail:`
- `canvas:login:locked:`
- `canvas:jwt:revoked:`

Those key names are compatibility-sensitive if the final security filter must
interoperate with existing sessions or logout blacklist entries, but they should
be owned by a final auth/security module rather than copied through an old-engine
dependency.

## Suggested test shape

If the main implementation adds coverage, prefer a compact
`AuthControllerCompatibilityTest` that binds the final controller directly and
uses recording final-module auth services/facades. Useful assertions:

- success envelope fields for login/logout/me;
- login delegates credentials and returns the exact `LoginResp`-compatible data
  keys;
- bad credentials and not-found current user map to the selected final error
  envelope;
- lockout maps to HTTP 429 and an `AUTH_004`-compatible message/envelope if the
  final compatibility layer wraps `ResponseStatusException`;
- logout only records revocation for exact `Bearer ` valid tokens.

Do not add route-only tests that merely prove Spring annotations exist; the
coverage should exercise compatibility behavior and the auth-specific branches
above.
