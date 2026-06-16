# CDP Audience Module Quality Progress

Date: 2026-06-16
Branch: main
Latest continuation check: 2026-06-16 01:16:57 CST

## Workspace Guardrails

- First command run: `git status --short`.
- Current workspace has unrelated modified/untracked files outside this module. I did not revert them.
- Source edit constraint observed for this pass: only write `tmp/module-quality-cdp-audience-progress.md` and `docs/e2e-browser-audits/cdp-audience.md`.
- Continuation `git status --short` still shows unrelated dirty files plus this cdp-audience report file; no unrelated files were reverted.
- Local test data found from current code:
  - CDP user IDs: `user-alice`, `user-bob` from `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpUserReadCatalog.java`.
  - Realtime audience IDs: `100`, `200` from `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/RealtimeAudienceCatalog.java`.

## Code Review Status

Completed scoped static review for:

- Frontend pages:
  - `frontend/src/pages/cdp-users`
  - `frontend/src/pages/cdp-user-detail`
  - `frontend/src/pages/audience-list`
  - `frontend/src/pages/audience-edit`
  - `frontend/src/pages/cdp-computed-profile`
  - `frontend/src/pages/cdp-computed-tags`
  - `frontend/src/pages/realtime-audiences`
- Frontend services/types:
  - `frontend/src/services/cdpApi.ts`
  - `frontend/src/services/audienceApi.ts`
- Backend/API scope:
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp`
  - `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/AudienceController.java`
  - `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application`
  - `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain`
  - `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/AudienceCatalog.java`

## Findings

### High: realtime audience set operations are not tenant-scoped

Evidence:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/RealtimeAudienceController.java:61` to `:76` exposes overlap/merge/exclude without accepting `X-Tenant-Id`.
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/RealtimeAudienceCatalog.java:58` to `:76` calls `members(audienceId)`.
- `RealtimeAudienceCatalog.java:87` to `:93` searches `tenants.values()` and returns the first audience with the ID, regardless of tenant.

Risk:

- If two tenants have the same audience ID, overlap/merge/exclude can read or combine another tenant's member set. This directly affects tenant isolation and realtime audience operations.

Suggested fix when source edits are allowed:

- Add tenant ID to `RealtimeAudienceFacade.overlap/merge/exclude`, controller params, application service, and catalog operations.
- Resolve both input audience IDs through `audience(tenantId, id)` instead of cross-tenant `members(id)`.
- Add a regression test with duplicate audience IDs under two tenants.

### High: computed profile/tag pages expect arrays but backend returns page objects

Evidence:

- `frontend/src/pages/cdp-computed-profile/index.tsx:55` to `:60` calls `setRows(res.data)`.
- `frontend/src/pages/cdp-computed-tags/index.tsx:42` to `:47` calls `setRows(res.data)`.
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpComputedProfileCatalog.java:26` to `:32` returns `page(records)`.
- `backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpComputedTagCatalog.java:25` to `:31` returns `page(records)`.

Risk:

- Ant Design `Table` receives an object instead of an array on `/cdp/computed-profile` and `/cdp/computed-tags`. This can cause blank/error-boundary behavior once API data loads.

Suggested fix when source edits are allowed:

- Either normalize frontend service/page handling to `res.data.records ?? []`, or change backend compatibility response shape to match the frontend's array contract.
- Add page-level tests that mock the actual backend shape.

### Medium: computed profile/tag preview and run field names also appear mismatched

Evidence:

- Frontend expects `scannedCount`, `matchedCount`, `changedCount`, `unchangedCount`, and `samples`.
- Current computed profile catalog preview returns `sampleProfileCount`, `matchedProfileCount`, and `sampleValues`.
- Current computed tag catalog preview returns `matchedProfileCount` and `sampleValues`.
- Run responses return `affectedProfileCount`, while frontend run summary types expect scanned/matched/updated/skipped/failed style counters.

Risk:

- Preview drawers and success messages can show `undefined` counters or empty sample tables even after backend calls succeed.

Suggested fix when source edits are allowed:

- Align response DTOs in the backend compatibility facade or add frontend adapters that map legacy/current names.
- Add contract tests covering list, preview, run, runs, changes, and lineage payloads.

### Medium: audience edit route has dynamic ID parsing gaps

Evidence:

- `frontend/src/pages/audience-edit/index.tsx:282` calls `audienceApi.get(Number(id))`.
- `frontend/src/pages/audience-edit/index.tsx:325` to `:326` calls update with `Number(id)`.
- There is no finite/positive validation before API calls.

Risk:

- `/audiences/foo/edit`, `/audiences/NaN/edit`, or `/audiences/0/edit` can issue malformed API calls and rely on backend failure rather than rendering a controlled not-found/error state.

Suggested fix when source edits are allowed:

- Validate route id with `Number.isSafeInteger(id) && id > 0`; render a controlled error state or navigate back before API calls.
- Add a focused route/component test for invalid dynamic IDs.

### Low: audience creation/editing validates basic required fields but not empty rule groups

Evidence:

- `frontend/src/pages/audience-edit/index.tsx:311` to `:321` serializes the current query builder state directly.
- Initial query is `{ combinator: 'and', rules: [] }`.

Risk:

- It may be possible to create or update an enabled audience with no conditions, depending on backend acceptance. This is a boundary-value test gap for audience rule construction.

Suggested fix when source edits are allowed:

- Decide product semantics for empty rules. If disallowed, block submit with a form-level validation message. If allowed, document and test "all users" behavior explicitly.

## Browser E2E Status

Not completed in this pass.

Blockers:

- Codex in-app Browser plugin bootstrap returned `Browser is not available: iab`.
- `agent.browsers.list()` returned `[]`, so there was no in-app Browser handle to drive.
- Backend on `:8080` was initially down.
- Continuation recheck still returned no in-app Browser handles and `Browser is not available: iab`.
- Continuation port recheck showed neither `:3000` nor `:8080` listening.
- Second continuation recheck at 2026-06-16 01:16:57 CST again returned no in-app Browser handles (`[]`) and `Browser is not available: iab`.
- Second continuation port recheck again showed neither `:3000` nor `:8080` listening.
- `mvn -f canvas-boot/pom.xml spring-boot:run` first failed because Maven used Java 8; rerun with Java 21 got further.
- After refreshing current workspace modules with `mvn -pl canvas-boot -am install -DskipTests`, backend launch failed on a non-cdp-audience bean conflict:
  - `org.chovy.canvas.execution.application.CanvasTriggerApplicationService`
  - `org.chovy.canvas.canvas.application.CanvasTriggerApplicationService`
  - duplicate bean name: `canvasTriggerApplicationService`

Routes still requiring in-app Browser E2E:

- `/cdp/users`
- `/cdp/users/user-alice`
- `/audiences`
- `/audiences/new`
- `/audiences/100/edit`
- `/cdp/computed-profile`
- `/cdp/computed-tags`
- `/cdp/realtime-audiences`

## Verification Run

Passed:

- `npm run test -- cdpApi.test.ts cdpPresentation.test.ts contactabilityPresentation.test.ts computedProfilePresentation.test.ts computedTagPresentation.test.ts realtimeAudiencePresentation.test.ts cdpAudienceFields.test.ts audienceSnapshotMode.test.ts audienceTaskPresentation.test.ts`
  - 9 test files passed.
  - 29 tests passed.
- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-web,canvas-context-cdp,canvas-context-marketing -Dtest='CdpUserReadControllerCompatibilityTest,CdpUserTagControllerCompatibilityTest,CdpComputedProfileControllerCompatibilityTest,CdpComputedTagControllerCompatibilityTest,RealtimeAudienceControllerCompatibilityTest,AudienceControllerCompatibilityTest,RealtimeAudienceApplicationServiceTest,CdpComputedProfileApplicationServiceTest,CdpComputedTagApplicationServiceTest,AudienceApplicationServiceTest' test`
  - 32 tests passed across the selected modules.
- `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-boot -am install -DskipTests`
  - Build/install succeeded for the current workspace reactor.

Failed or blocked:

- `CANVAS_JWT_SECRET=... mvn -f canvas-boot/pom.xml spring-boot:run`
  - Failed under Java 8 due Spring Boot plugin classfile version.
- Same launch with Java 21 before installing reactor modules:
  - Failed on stale installed jar/type alias `org.chovy.canvas.dal.dataobject.CanvasExecutionDO`.
- Same launch after installing reactor modules:
  - Failed on duplicate bean name `canvasTriggerApplicationService`.
- Codex in-app Browser:
  - Blocked because no `iab` browser handle was available.

## Next Coordination Needed

- Resolve the duplicate `CanvasTriggerApplicationService` bean naming conflict so `canvas-boot` can start from the current workspace.
- Restore/expose the Codex in-app Browser `iab` handle for this session.
- Start or restore the frontend dev server on `:3000` after Browser is available.
- Allow source edits beyond the two report files if the cdp-audience-owned bugs above should be fixed in this workstream.
- Current status meets the repeated-blocker condition for Browser E2E: the same unavailable Browser handle and unavailable local runtime state have recurred across consecutive goal continuations.
