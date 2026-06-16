# Marketing Suite Browser Audit

Date: 2026-06-16
Branch: `main`

## Requested Routes

- `/marketing-monitoring`
- `/mautic-insights`
- `/marketing-platform`
- `/search-marketing`
- `/risk`
- `/growth-activities`
- `/marketing-preferences`
- `/marketing-forms`
- `/content-hub`
- `/message-templates`
- `/message-deliveries`
- `/channel-connectors`
- `/demo-sandbox`

## Current Result

Browser E2E route testing is blocked, not completed.

Blocking evidence:

- Required Codex in-app Browser setup was rechecked and returned `Browser is not available: iab`.
- `curl -I --max-time 5 http://127.0.0.1:3000/marketing-monitoring` could not connect in this continuation.
- `curl -I --max-time 5 http://127.0.0.1:8080/actuator/health` could not connect in this continuation.
- Prior backend boot attempt with Java 21 failed during Spring context startup because `CanvasExecutionMapper.xml` references unresolved alias `org.chovy.canvas.dal.dataobject.CanvasExecutionDO`.

No route is marked passed from Browser evidence.

## Route Audit Matrix

| Route | Browser status | Notes |
| --- | --- | --- |
| `/marketing-monitoring` | Blocked | In-app Browser unavailable; local frontend/backend unavailable. |
| `/mautic-insights` | Blocked | In-app Browser unavailable; local frontend/backend unavailable. |
| `/marketing-platform` | Blocked | In-app Browser unavailable; local frontend/backend unavailable. |
| `/search-marketing` | Blocked | In-app Browser unavailable; focused tests/build passed. |
| `/risk` | Blocked | Static review found rollback gating risk; Browser regression not possible. |
| `/growth-activities` | Blocked | Static review found fire-and-forget reward grant transitions; Browser regression not possible. |
| `/marketing-preferences` | Blocked | In-app Browser unavailable; local frontend/backend unavailable. |
| `/marketing-forms` | Blocked | Static review found public form schema contract mismatch; Browser regression not possible. |
| `/content-hub` | Blocked | Static review found API contract mismatch and edit data-loss risk; Browser regression not possible. |
| `/message-templates` | Blocked | Static review did not confirm the suspected create/update defect. |
| `/message-deliveries` | Blocked | Focused backend tests/build passed; Browser regression not possible. |
| `/channel-connectors` | Blocked | Static review found post-transition refresh handling risk; Browser regression not possible. |
| `/demo-sandbox` | Blocked | In-app Browser unavailable; local frontend/backend unavailable. |

## Required Browser Checks Still Pending

- Blank screen and ErrorBoundary detection.
- Console and network error capture.
- Main tables/cards/forms render checks.
- Filter/search behavior.
- Modal/drawer open-close behavior.
- Safe create/edit form paths.
- Layout overflow checks.
- Refresh behavior.
- Browser regression for any fixed route.

## Verification Evidence Available

Frontend focused tests passed with `/opt/homebrew/bin/node v25.8.1`:

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

Frontend production build passed:

```bash
cd frontend
PATH=/opt/homebrew/bin:$PATH npm run build
```

Result: `tsc && vite build` passed.

Backend focused tests passed with Java 21:

```bash
cd backend
JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home \
PATH=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH \
mvn -pl canvas-context-marketing,canvas-context-risk,canvas-context-execution,canvas-platform \
  -Dtest=MarketingContentApplicationServiceTest,RiskGovernanceApplicationServiceTest,MarketingFormApplicationServiceTest,GrowthActivityApplicationServiceTest,SearchMarketingApplicationServiceTest,MessageDeliveryApplicationServiceTest,ChannelConnectorApplicationServiceTest,PublicIngressApplicationServiceTest test
```

Result: build success; 20 backend tests passed.

Blocked verification:

- Default `node v18.20.8` could not start Vitest with the installed rolldown package; `/opt/homebrew/bin/node v25.8.1` was used for passing frontend verification.
- Codex in-app Browser was unavailable again in the current continuation.
- Browser route E2E was not possible because the required Browser surface is unavailable and no frontend/backend servers were serving on `:3000` or `:8080`.
