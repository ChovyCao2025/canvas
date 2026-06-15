# DDD-C09AY Admin Platform Routes Coordinator Recovery Closeout

## Dispatch

- Dispatch: `dispatch-DDD-C09AY-admin-platform-routes-20260614-062800`
- Worker: Descartes `019ec316-70c5-7341-b9de-9b7911bd91ad`
- Scope: all 21 legacy `/admin` routes
- Status: `DONE_WITH_CONCERNS`

## Recovery Notes

- A real code-writing worker was spawned before the dispatch moved to `RUNNING`.
- After one wait timeout, the coordinator inspected reserved paths and evidence instead of continuing to wait.
- Descartes had written RED tests only:
  - `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/AdminPlatformApplicationServiceTest.java`
  - `backend/canvas-web/src/test/java/org/chovy/canvas/web/admin/AdminPlatformControllerCompatibilityTest.java`
- The coordinator closed Descartes after timeout; `close_agent` returned `previous_status: running`, and the later notification reported shutdown.
- The coordinator ran the RED command and observed the expected compile failure because `AdminPlatformCatalog` and `AdminPlatformApplicationService` were missing.

## Implementation

- Added `AdminPlatformFacade` in `canvas-platform`.
- Added `AdminPlatformApplicationService` in `canvas-platform`.
- Added compact in-memory `AdminPlatformCatalog` in `canvas-platform`.
- Added `AdminPlatformController` in `canvas-web` exposing the 21 legacy `/admin` route shapes.
- Kept implementation free of old `canvas-engine`, old auth/domain/dto/query/dal dependencies, and old `TenantContextResolver` coupling.
- Did not edit POM files.

## Verification

Command:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-platform,canvas-web -am -Dtest=AdminPlatformApplicationServiceTest,AdminPlatformControllerCompatibilityTest test
```

Result:

- `AdminPlatformApplicationServiceTest`: 4 tests, 0 failures, 0 errors
- `AdminPlatformControllerCompatibilityTest`: 4 tests, 0 failures, 0 errors
- Reactor result: `BUILD SUCCESS`

Command:

```bash
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Result:

- `canvas-web` controllers: 21
- `canvas-web` endpoints: 336
- `route:/admin` removed from top route gap candidates
- Global cutover remains blocked by route parity: old 142 controllers / 806 endpoints vs current 21 controllers / 336 endpoints

Command:

```bash
rg -n "canvas-engine|org\.chovy\.canvas\.(auth|domain|dto|query|dal)|TenantContextResolver|SysUserService|CanvasProjectService|CanvasProjectPermissionService|SystemOptionService|TenantService|AuditEventService" backend/canvas-web/src/main/java/org/chovy/canvas/web/admin/AdminPlatformController.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/AdminPlatformFacade.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/AdminPlatformApplicationService.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AdminPlatformCatalog.java
```

Result:

- Exit 1 with no matches.

Command:

```bash
node tools/program-coordination/check-dispatch-state.mjs .
git diff --check -- DDD-C09AY reserved files and coordination files
```

Result:

- Dispatch state check passed before closeout edit.
- Scoped diff whitespace check passed before closeout edit.

## Accepted Concerns

- No normal Descartes worker-return packet.
- Coordinator recovered implementation locally after Descartes produced RED tests only.
- Admin platform behavior is a compact in-memory compatibility seed; durable user/project/member/system-option/tenant persistence, permission semantics, and audit behavior remain out of scope for this batch.
- Global DDD-C09 final cutover remains blocked by broader route parity gaps.
