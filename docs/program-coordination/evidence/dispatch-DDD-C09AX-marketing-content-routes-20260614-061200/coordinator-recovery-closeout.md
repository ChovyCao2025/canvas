# DDD-C09AX Marketing Content Routes Coordinator Recovery Closeout

## Dispatch

- Dispatch: `dispatch-DDD-C09AX-marketing-content-routes-20260614-061200`
- Worker: Planck `019ec30b-7810-7d83-ae44-e550acadd158`
- Scope: all 21 legacy `/marketing/content` routes
- Status: `DONE_WITH_CONCERNS`

## Recovery Notes

- A real code-writing worker was spawned before the dispatch moved to `RUNNING`.
- After one wait timeout, the coordinator inspected reserved paths and evidence instead of continuing to wait.
- Planck had written RED tests only:
  - `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MarketingContentApplicationServiceTest.java`
  - `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingContentControllerCompatibilityTest.java`
  - `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`
- The coordinator closed Planck after timeout; `close_agent` returned `previous_status: running`, and the later notification reported shutdown.
- The coordinator ran the RED command and observed the expected compile failure because `MarketingContentFacade` and `MarketingContentApplicationService` were missing.

## Implementation

- Added `MarketingContentFacade` in `canvas-context-marketing`.
- Added `MarketingContentApplicationService` in `canvas-context-marketing`.
- Added compact in-memory `MarketingContentCatalog` in `canvas-context-marketing`.
- Added `MarketingContentController` in `canvas-web` exposing the 21 legacy `/marketing/content` route shapes.
- Kept implementation free of old `canvas-engine` imports, old `org.chovy.canvas.domain.content` services, and old `TenantContextResolver` coupling.
- Did not edit POM files.

## Verification

Command:

```bash
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-marketing,canvas-web -am -Dtest=MarketingContentApplicationServiceTest,MarketingContentControllerCompatibilityTest,MarketingApiCompatibilityTest test
```

Result:

- `MarketingContentApplicationServiceTest`: 2 tests, 0 failures, 0 errors
- `MarketingContentControllerCompatibilityTest`: 2 tests, 0 failures, 0 errors
- `MarketingApiCompatibilityTest`: 8 tests, 0 failures, 0 errors
- Reactor result: `BUILD SUCCESS`

Command:

```bash
node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
```

Result:

- `canvas-web` controllers: 20
- `canvas-web` endpoints: 315
- `route:/marketing` removed from top route gap candidates
- Global cutover remains blocked by route parity: old 142 controllers / 806 endpoints vs current 20 controllers / 315 endpoints

Command:

```bash
rg -n "canvas-engine|org\.chovy\.canvas\.domain\.content|TenantContextResolver|MarketingAssetService|ContentTemplateService|ContentEntryService|MarketingAssetUploadService|MarketingContentReleaseService" backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingContentController.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingContentFacade.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingContentApplicationService.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingContentCatalog.java
```

Result:

- Exit 1 with no matches.

Command:

```bash
node tools/program-coordination/check-dispatch-state.mjs .
git diff --check -- DDD-C09AX reserved files and coordination files
```

Result:

- Dispatch state check passed before closeout edit.
- Scoped diff whitespace check passed before closeout edit.

## Accepted Concerns

- No normal Planck worker-return packet.
- Coordinator recovered implementation locally after Planck produced RED tests only.
- Marketing content behavior is a compact in-memory compatibility seed; durable asset/template/entry/release persistence, upload session integration, and channel publishing semantics remain out of scope for this batch.
- Global DDD-C09 final cutover remains blocked by broader route parity gaps.
