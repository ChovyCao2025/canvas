# DDD-C09N Coordinator Closeout

Date: 2026-06-12

## Dispatch

- Dispatch: `dispatch-DDD-C09N-marketing-campaign-controller-20260612-204000`
- Worker: Maxwell `019ebbc6-ef68-7231-8061-c847db104905`
- Reviewer: Beauvoir `019ebbd2-d672-7420-9928-4ed680960c54`
- Result: `DONE_WITH_CONCERNS`

## Reserved Scope

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingCampaignController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingCampaignControllerCompatibilityTest.java`

## Result

Maxwell added a compact production `MarketingCampaignFacade`-backed controller seed for:

- `POST /canvas/marketing-campaigns`
- `GET /canvas/marketing-campaigns`
- `POST /canvas/marketing-campaigns/links`
- `GET /canvas/marketing-campaigns/{campaignId}/links`
- `GET /canvas/marketing-campaigns/{campaignId}/readiness`
- `DELETE /canvas/marketing-campaigns/links/{linkId}`

The controller preserves compatibility envelopes, default `X-Tenant-Id` `7`,
default `X-Actor` `operator-1`, and `IllegalArgumentException` to `API_001`
bad-request envelope mapping.

## Verification

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=MarketingCampaignControllerCompatibilityTest`
  - `BUILD SUCCESS`; 7 tests run, 0 failures/errors.
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=MarketingApiCompatibilityTest,MarketingCampaignControllerCompatibilityTest`
  - `BUILD SUCCESS`; 13 tests run, 0 failures/errors.
- Beauvoir read-only review returned `PASS` with no findings and no required fixes.
- `node tools/program-coordination/check-dispatch-state.mjs .`
  - Passed with `activeDispatches` empty after closeout.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  - Passed after closeout.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
  - Exited 0 with current canvas-web 5 controllers and 26 endpoints; global
    `cutoverReady` remains false.
- `git diff --check -- <DDD-C09N files and coordination closeout files>`
  - Passed with no whitespace errors.

## Accepted Concerns

- Broader marketing-monitoring, search-marketing, and growth-activities route
  parity remains out of scope for this compact production seed.
- Global DDD-C09 cutover remains blocked by broader production controller and
  endpoint parity gaps.

## Rollback Pointer

Remove only:

- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingCampaignController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingCampaignControllerCompatibilityTest.java`
