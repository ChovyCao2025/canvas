# CDP Audience Browser Audit

Date: 2026-06-16
Branch: main
Latest continuation check: 2026-06-16 01:16:57 CST

## Scope

Requested in-app Browser E2E routes:

- `/cdp/users`
- `/cdp/users/:userId`
- `/audiences`
- `/audiences/new`
- `/audiences/:id/edit`
- `/cdp/computed-profile`
- `/cdp/computed-tags`
- `/cdp/realtime-audiences`

Dynamic local data identified from current code:

- `:userId`: `user-alice`
- `:id`: `100`
- Additional realtime audience ID for pair operations: `200`

## Result

Browser E2E was blocked and no route can be claimed as browser-tested.

Blocking evidence:

- Codex in-app Browser bootstrap returned `Browser is not available: iab`.
- Browser API reported no available browser handles: `[]`.
- Backend API server could not be started on `:8080` from the current workspace.
- Continuation recheck again returned no browser handles: `[]`, and acquiring `iab` returned `Browser is not available: iab`.
- Continuation port check showed both `:3000` and `:8080` were not listening.
- Second continuation recheck at 2026-06-16 01:16:57 CST again returned no browser handles: `[]`, and acquiring `iab` returned `Browser is not available: iab`.
- Second continuation port check again showed both `:3000` and `:8080` were not listening.

Backend startup attempts:

- Java 8 attempt failed because Spring Boot 3.2 plugin requires newer classfile support.
- Java 21 attempt before refreshing local modules failed on stale installed jars referencing `org.chovy.canvas.dal.dataobject.CanvasExecutionDO`.
- `mvn -pl canvas-boot -am install -DskipTests` succeeded and refreshed current workspace modules.
- Java 21 startup after module refresh failed on duplicate bean name `canvasTriggerApplicationService` between canvas and execution context services.

## Route Matrix

| Route | Intended data/action | Browser status | Blocker |
| --- | --- | --- | --- |
| `/cdp/users` | Load list, search/filter, table, refresh | Blocked | No in-app Browser handle; backend down |
| `/cdp/users/user-alice` | Dynamic user detail route, tags/history/canvas tables, refresh | Blocked | No in-app Browser handle; backend down |
| `/audiences` | List loading, stats table, filters/actions, refresh | Blocked | No in-app Browser handle; backend down |
| `/audiences/new` | Creation form basics, rule builder, validation | Blocked | No in-app Browser handle; backend down |
| `/audiences/100/edit` | Dynamic audience edit route, form load, snapshot mode | Blocked | No in-app Browser handle; backend down |
| `/cdp/computed-profile` | List loading, create modal, preview/audit | Blocked | No in-app Browser handle; backend down; static review found list contract mismatch |
| `/cdp/computed-tags` | List loading, create modal, preview/lineage | Blocked | No in-app Browser handle; backend down; static review found list contract mismatch |
| `/cdp/realtime-audiences` | Event, overlap, merge/exclude, snapshots | Blocked | No in-app Browser handle; backend down; static review found tenant isolation bug |

## Static Findings Relevant to E2E

- Realtime audience overlap/merge/exclude are not tenant-scoped. Controller methods for these operations do not accept `X-Tenant-Id`, and catalog member lookup searches all tenants by audience ID.
- Computed profile/tag pages pass backend page objects directly to Ant Design `Table` where arrays are expected.
- Computed profile/tag preview/run field names are not aligned between frontend types and backend catalog responses.
- Audience edit route uses `Number(id)` without finite/positive validation.
- Empty audience rule groups are not explicitly guarded or tested.

## Verification Completed

Frontend focused tests:

```bash
npm run test -- cdpApi.test.ts cdpPresentation.test.ts contactabilityPresentation.test.ts computedProfilePresentation.test.ts computedTagPresentation.test.ts realtimeAudiencePresentation.test.ts cdpAudienceFields.test.ts audienceSnapshotMode.test.ts audienceTaskPresentation.test.ts
```

Result: 9 files passed, 29 tests passed.

Backend focused tests:

```bash
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-web,canvas-context-cdp,canvas-context-marketing -Dtest='CdpUserReadControllerCompatibilityTest,CdpUserTagControllerCompatibilityTest,CdpComputedProfileControllerCompatibilityTest,CdpComputedTagControllerCompatibilityTest,RealtimeAudienceControllerCompatibilityTest,AudienceControllerCompatibilityTest,RealtimeAudienceApplicationServiceTest,CdpComputedProfileApplicationServiceTest,CdpComputedTagApplicationServiceTest,AudienceApplicationServiceTest' test
```

Result: 32 tests passed.

## Remaining Browser Checklist

Run after the in-app Browser and backend startup blockers are resolved:

- Start or restore Vite on `:3000`.
- Start backend API on `:8080`.
- Restore/expose the Codex in-app Browser `iab` handle.
- Confirm no blank screen or ErrorBoundary on each route.
- Capture console errors and failed network requests for each route.
- Check table rendering and empty/loading states.
- Check route refresh behavior for every path.
- For `/cdp/users`, search `user-alice` and verify row navigation.
- For `/cdp/users/user-alice`, verify detail, tags/history, contactability, and canvas tables render.
- For `/audiences`, verify list/stats load and action controls do not overflow.
- For `/audiences/new`, verify required validation and basic rule builder interaction.
- For `/audiences/100/edit`, verify dynamic ID load, snapshot mode, and save form basics without submitting destructive changes unless approved.
- For `/cdp/computed-profile` and `/cdp/computed-tags`, verify list load and modal validation, especially JSON parse errors.
- For `/cdp/realtime-audiences`, verify event JSON validation and use IDs `100` and `200` for overlap/operation forms.
