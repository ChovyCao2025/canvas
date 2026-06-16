# Marketing Suite Module Quality Progress

Updated: 2026-06-16 01:23 Asia/Shanghai

## Scope And Guardrails

- Branch: `main`
- Initial command run this continuation: `git status --short`
- Current workspace was used directly. No git worktree and no new branch were created.
- Existing unrelated modified/untracked files were observed and not reverted.
- Write constraint: only this file and `docs/e2e-browser-audits/marketing-suite.md` were intentionally edited. Production code was not changed, so reviewed defects are documented but not fixed in this pass.

## Code Review Status

Reviewed marketing-suite evidence:

- Frontend routes: `frontend/src/App.tsx`
- Primary route pages: `/marketing-monitoring`, `/mautic-insights`, `/marketing-platform`, `/search-marketing`, `/risk`, `/growth-activities`, `/marketing-preferences`, `/marketing-forms`, `/content-hub`, `/message-templates`, `/message-deliveries`, `/channel-connectors`, `/demo-sandbox`
- Matching services: marketing monitoring, mautic insights, marketing platform, search marketing, risk, growth activity, marketing preferences, marketing forms, marketing content, message template, message delivery, channel connector, demo sandbox
- Backend/API scope: active `backend/canvas-web` controllers plus `canvas-context-marketing`, `canvas-context-risk`, `canvas-context-execution`, and `canvas-platform` domain/application services backing the reviewed routes.

## Review Findings

1. High: `/content-hub` frontend/backend payload contract mismatch breaks create/save flows.
   - Frontend forms use fields such as `name`, `displayName`, and `templateKey` in `frontend/src/pages/content-hub/index.tsx`.
   - Active backend catalog still requires legacy keys such as `assetName` and `templateName` in `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingContentCatalog.java:39` and `:94`.
   - Impact: operator create/save actions can fail with validation errors, blocking content production flows.

2. High: `/content-hub` edit forms can silently drop persisted content fields on re-save.
   - Asset/entry selection repopulates editable JSON/text fields with defaults instead of preserving persisted metadata/SEO/summary values.
   - Impact: editing a single visible field can erase metadata, SEO data, or summary content in production content workflows.

3. High: public marketing form definitions use a backend/frontend field-schema contract mismatch.
   - Public route page parses `definition.fieldSchemaJson` in `frontend/src/pages/public-marketing-form/index.tsx:61`.
   - Public ingress returns `fieldSchema` instead of `fieldSchemaJson` in `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/PublicIngressCatalog.java:20`.
   - Impact: `/public/forms/:publicKey` can render a form shell with zero fields even though the public form definition endpoint returned data. This affects forms created/previewed from `/marketing-forms`.

4. Medium: `/risk` exposes rollback for invalid strategy-version states.
   - UI disables rollback only when `record.version === state.activeVersion` in `frontend/src/pages/risk/index.tsx:661`.
   - Backend `rollbackStrategy` sets `activeVersion` directly from `targetVersion` without validating target version status in `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/domain/RiskGovernanceCatalog.java:146`.
   - Impact: an operator can promote a draft, failed, or otherwise invalid version into active rollback state if the row is visible.

5. Medium: `/growth-activities` reward grant state transitions are fire-and-forget.
   - Grant actions call `retryGrant`, `reconcileGrant`, and `cancelGrant` directly from button handlers without await/loading/error handling/detail refresh in `frontend/src/pages/growth-activities/index.tsx:428`.
   - Impact: failed reward-provider operations can be invisible to operators, and successful actions leave stale grant/readiness/report state until manual refresh or route reload.

6. Medium: `/channel-connectors` mode and health transitions do not await refresh.
   - `handleModeChange` and `handleHealthTest` call `fetchAll()` without awaiting it in `frontend/src/pages/channel-connectors/index.tsx:58` and `:69`.
   - Impact: refresh failures are unhandled, saving state clears before the post-action reload completes, and operators can see stale connector health/mode after a state transition.

Non-finding checked:

- `/message-templates` labels an existing template as editable but calls `create` on save. The current `MessageTemplateCatalog.create` behaves as an upsert by normalized template code, so this was not treated as a confirmed defect.

## Browser / E2E Status

Requested Codex in-app Browser route testing remains blocked:

- Browser setup was rechecked and returned `Browser is not available: iab`.
- Frontend `127.0.0.1:3000` was not serving during this continuation.
- Backend `127.0.0.1:8080` was not serving during this continuation.
- Prior backend boot evidence remains relevant: Java 21 startup failed while parsing `mapper/execution/CanvasExecutionMapper.xml` because MyBatis could not resolve `org.chovy.canvas.dal.dataobject.CanvasExecutionDO`.

Because the objective specifically requires the Codex in-app Browser, no alternate browser automation surface was substituted.

## Verification Run

Passed:

```bash
cd frontend
PATH=/opt/homebrew/bin:$PATH npm test -- --run \
  src/pages/marketing-forms/marketingFormsPresentation.test.ts \
  src/services/marketingFormsApi.test.ts \
  src/pages/growth-activities/index.test.tsx \
  src/pages/channel-connectors/channelConnectorPresentation.test.ts \
  src/services/channelConnectorApi.test.ts \
  src/pages/search-marketing/index.test.tsx \
  src/services/searchMarketingApi.test.ts
```

Result: 7 files passed, 29 tests passed.

```bash
cd frontend
PATH=/opt/homebrew/bin:$PATH npm run build
```

Result: `tsc && vite build` passed.

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn -pl canvas-context-marketing,canvas-context-risk,canvas-context-execution,canvas-platform \
  -Dtest=MarketingContentApplicationServiceTest,RiskGovernanceApplicationServiceTest,MarketingFormApplicationServiceTest,GrowthActivityApplicationServiceTest,SearchMarketingApplicationServiceTest,MessageDeliveryApplicationServiceTest,ChannelConnectorApplicationServiceTest,PublicIngressApplicationServiceTest test
```

Result: build success; 20 backend tests passed.

Failed / blocked:

- Same frontend test command under default `node v18.20.8` failed before tests started because installed `rolldown` imports `node:util.styleText`, which is not exported by that Node runtime. Rerun with `/opt/homebrew/bin/node v25.8.1` passed.
- Codex in-app Browser route testing is blocked by unavailable `iab`; this condition repeated in the current continuation.
- Backend/frontend route E2E is additionally blocked because neither local app port was serving during the current continuation.

## Needs Coordination

- To fix defects, the write constraint must be relaxed beyond these two markdown files.
- Browser E2E requires the Codex in-app Browser endpoint to be available.
- Full E2E route execution also requires a bootable backend on `:8080`; current local boot evidence points to a non-marketing MyBatis alias blocker.

## Remaining Risks

- No requested route has Browser evidence for blank screen, ErrorBoundary behavior, console/network errors, filters/search, modal open/close, safe create/edit paths, layout overflow, or refresh behavior.
- Findings are static-review findings plus focused unit/build verification, not Browser-regressed fixes.
