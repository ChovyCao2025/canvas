# Public Auth E2E Browser Audit

Date: 2026-06-16
Branch: main

## Status

Blocked for required Codex in-app Browser E2E execution. The browser runtime was loaded from the Browser plugin, but `agent.browsers.get("iab")` reported `Browser is not available: iab`, and `agent.browsers.list()` returned `[]`.

Follow-up retry during continuation produced the same result: `available: []` and `iabStatus: "Browser is not available: iab"`.

Second continuation retry again produced the same result: `available: []` and `iabStatus: "Browser is not available: iab"`.

## Routes Requested

| Route | Local data | Browser result | Blocker |
| --- | --- | --- | --- |
| `/login` | none required | Not executed in in-app Browser | No available `iab` browser handle |
| `/public/forms/:publicKey` | `lead-capture` | Not executed in in-app Browser | No available `iab` browser handle; backend unavailable for API response |
| `/bi/embed/:resourceType/:resourceKey` | `DASHBOARD/canvas-effect`; `PORTAL/executive-home` | Not executed in in-app Browser | No available `iab` browser handle; no live backend ticket |

## Non-Browser Evidence Gathered

- Vite served route fallback HTML on:
  - `http://127.0.0.1:3002/login`
  - `http://127.0.0.1:3002/public/forms/lead-capture`
  - `http://127.0.0.1:3002/bi/embed/DASHBOARD/canvas-effect`
- A subsequent existing Vite listener on `http://127.0.0.1:3001/` also served route fallback HTML for `/login`, `/public/forms/lead-capture`, and `/bi/embed/DASHBOARD/canvas-effect`.
- Backend `http://127.0.0.1:8080/actuator/health` was unavailable.
- Backend startup with JDK 21 failed during Spring context initialization because `CanvasExecutionMapper.xml` references missing type alias `org.chovy.canvas.dal.dataobject.CanvasExecutionDO`.
- Second continuation backend startup reached a different blocker after user-owned mapper changes: Spring component scan now fails on duplicate bean name `canvasTriggerApplicationService` between `org.chovy.canvas.execution.application.CanvasTriggerApplicationService` and `org.chovy.canvas.canvas.application.CanvasTriggerApplicationService`.
- Second continuation backend health probe still returned no connection (`000`).

## Review Findings Relevant To E2E

1. `/public/forms/lead-capture` is likely to render without expected input fields even when backend is running, because frontend expects `fieldSchemaJson` while public ingress returns `fieldSchema`.
2. `/login` does not consume `location.state.from`; successful login always navigates to `/`.
3. `/bi/embed/DASHBOARD/canvas-effect` should show `缺少嵌入 ticket` without a ticket and should reject mismatched or invalid tickets, but valid-ticket rendering could not be browser-tested without a live backend ticket.

## Verification Commands

- Passed: `cd backend && mvn -q -pl canvas-web -am -DskipTests compile`
- Failed before tests ran: `cd frontend && npm test -- --run src/services/marketingFormsApi.test.ts src/services/biApi.test.ts src/pages/bi/embed.test.tsx src/context/AuthContext.test.tsx`
  - Startup error: Rolldown import of `node:util.styleText`.
- Passed with Homebrew Node pinned: `cd frontend && PATH=/opt/homebrew/bin:$PATH /opt/homebrew/bin/npm test -- --run src/services/marketingFormsApi.test.ts src/services/biApi.test.ts src/pages/bi/embed.test.tsx src/context/AuthContext.test.tsx`
  - Result: 4 test files passed, 29 tests passed.
- Passed with JDK 21 pinned: `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH mvn -q -pl canvas-web -am -DskipTests compile`
- Failed: `cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH CANVAS_JWT_SECRET=0123456789abcdef0123456789abcdef mvn -f canvas-boot/pom.xml spring-boot:run`
  - Runtime error: missing MyBatis alias class `org.chovy.canvas.dal.dataobject.CanvasExecutionDO`.
- Failed on second continuation with the same command after current workspace changes:
  - Runtime error: conflicting bean definition for `canvasTriggerApplicationService` between execution and canvas application services.

## Needs Coordination

- Restore the in-app Browser handle for this Codex session.
- Fix backend boot conflict before API-backed page behavior can be tested. The latest observed blocker is duplicate bean name `canvasTriggerApplicationService`.
- Confirm whether the public ingress API should return the frontend's `fieldSchemaJson` contract or whether the frontend should normalize the public `fieldSchema` shape.
- Allow source/test writes if this pass should fix the identified public-auth bugs.
