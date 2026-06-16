# Public Auth Module Quality Progress

Date: 2026-06-16
Branch: main

## Scope

- Reviewed current workspace directly; no worktree and no branch were created.
- Initial `git status --short` showed an already dirty tree with unrelated changes, including `backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java`; nothing was reverted.
- Write scope honored: only this file and `docs/e2e-browser-audits/public-auth.md` were written.

## Code Review Findings

1. Public form API shape does not match the public form page.
   - Frontend reads `definition.fieldSchemaJson` to build fields and `definition.name` for title: `frontend/src/pages/public-marketing-form/index.tsx:61`, `frontend/src/pages/public-marketing-form/index.tsx:112`.
   - Current public ingress seed returns `fieldSchema` as `List.of("email", "company", "message")`, plus `name`/`formName`, but no `fieldSchemaJson`: `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/PublicIngressCatalog.java:14`.
   - Impact: real local public key `lead-capture` can render as a form shell with no expected inputs, so public-form validation and missing-data tests do not cover the live compatibility contract.

2. Login discards the intended redirect target after an unauthorized redirect.
   - Unauthorized handler passes `state.from` to `/login`: `frontend/src/App.tsx:127`.
   - Login success always navigates to `/`: `frontend/src/pages/login/index.tsx:38`.
   - Impact: users kicked to login from a protected route do not return to their original page after successful login.

3. Public routes are correctly outside frontend auth guards and backend API permits anonymous access.
   - Frontend routes `/login`, `/bi/embed/:resourceType/:resourceKey`, and `/public/forms/:publicKey` are declared before protected route groups: `frontend/src/App.tsx:158`.
   - Backend security permits `/auth/login`, BI embed verify/resource/query endpoints, and public marketing form GET/POST: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java:101`.

4. BI embed page has good defensive behavior for ticket handling.
   - Missing ticket returns a page-level error before API calls.
   - Verified ticket resource type/key must match URL before dashboard/portal resource fetch.
   - Query failures are isolated per widget.

## Real Local Test Data

- Public form key found: `lead-capture` from `PublicIngressCatalog`.
- Additional marketing form catalog key found but inactive in the marketing context seed: `event-signup`.
- BI resource keys found: `DASHBOARD/canvas-effect` and `PORTAL/executive-home`.
- Real BI embed ticket unavailable because backend startup is blocked; ticket creation requires a running backend and authenticated API access.

## Verification

- `git status --short`: completed first; dirty worktree observed and preserved.
- Backend focused compile: `cd backend && mvn -q -pl canvas-web -am -DskipTests compile` passed.
- Frontend focused tests attempted: `cd frontend && npm test -- --run src/services/marketingFormsApi.test.ts src/services/biApi.test.ts src/pages/bi/embed.test.tsx src/context/AuthContext.test.tsx` failed before test execution with Rolldown startup error: `node:util` did not provide export `styleText`.
- Follow-up frontend focused tests with Homebrew Node pinned passed: `cd frontend && PATH=/opt/homebrew/bin:$PATH /opt/homebrew/bin/npm test -- --run src/services/marketingFormsApi.test.ts src/services/biApi.test.ts src/pages/bi/embed.test.tsx src/context/AuthContext.test.tsx` ran 4 files / 29 tests successfully.
- Follow-up backend focused compile with JDK 21 pinned passed: `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -q -pl canvas-web -am -DskipTests compile`.
- Frontend dev server launched with explicit Homebrew Node 25 because regular `npm run dev` selected Node 18 and Vite rejected it. Active URL during checks: `http://127.0.0.1:3002/`.
- A subsequent existing Vite listener was available on `http://127.0.0.1:3001/`; `curl` confirmed route fallback HTML for `/login`, `/public/forms/lead-capture`, and `/bi/embed/DASHBOARD/canvas-effect`.
- Backend startup attempted with JDK 21 and `CANVAS_JWT_SECRET`; Spring context failed on MyBatis mapper parse: `Cannot find class: org.chovy.canvas.dal.dataobject.CanvasExecutionDO` from `CanvasExecutionMapper.xml`.
- `curl` confirmed Vite fallback served HTML for `/login`, `/public/forms/lead-capture`, and `/bi/embed/DASHBOARD/canvas-effect`; backend health on `:8080` returned no connection.
- Follow-up backend health check on `http://127.0.0.1:8080/actuator/health` still returned no connection (`000`).
- Second continuation re-ran `git status --short` first. New user-owned mapper/test changes were present and preserved.
- Second continuation retried backend startup with JDK 21 and `CANVAS_JWT_SECRET`. The prior mapper alias failure was no longer the observed failure; startup now stops during component scan with a conflicting bean definition: `canvasTriggerApplicationService` for `org.chovy.canvas.execution.application.CanvasTriggerApplicationService` conflicts with `org.chovy.canvas.canvas.application.CanvasTriggerApplicationService`.
- Second continuation backend health check still returned no connection (`000`) because boot failed before binding `:8080`.

## Browser E2E Status

- Codex in-app Browser setup was attempted via the Browser plugin runtime.
- Browser handle lookup returned no available `iab` browser and `agent.browsers.list()` returned `[]`.
- Follow-up Browser retry in this continuation again returned `available: []` and `iabStatus: "Browser is not available: iab"`.
- Second continuation Browser retry again returned `available: []` and `iabStatus: "Browser is not available: iab"`.
- Required in-app Browser E2E checks are therefore blocked by tool availability in this session.

## Fix Status

- No source fixes were applied because the objective explicitly constrained writes to only this progress file and the audit document.
- Owned fixes needed when write scope is available:
  - Adapt public form page/API contract so seeded public forms render fields and validate required input.
  - Preserve and consume login `state.from` after successful login.
  - Add focused tests for public form missing schema/API mismatch and login intended-path redirect.

## Remaining Risks

- Browser-level blank-screen, ErrorBoundary, console, and network inspection is unproven until the in-app Browser is available.
- Real BI embed rendering with a valid ticket is unproven until backend startup and ticket issuance are available.
- Public form submit behavior with backend is unproven because backend `:8080` is unavailable.
- Backend runtime blocker has changed from mapper alias resolution to a duplicate Spring bean name, so API-backed E2E still needs a bootable `canvas-boot` runtime before public-auth can be completed.
