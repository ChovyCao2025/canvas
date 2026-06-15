# DDD-C09AU Growth Activities Route Batch Reservation

Timestamp: 2026-06-14T05:24:00+08:00

## Reservation

- Dispatch ID: `dispatch-DDD-C09AU-growth-activities-routes-20260614-052400`
- Task ID: `DDD-C09AU`
- Status: `RESERVED`
- Worker: pending real spawn
- Gate: R5 after DDD-C09AT Marketing Monitoring route closeout

## Scope

This batch covers all 25 legacy `/canvas/growth-activities` endpoints from:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/GrowthActivityController.java`

Exact reserved files:

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/GrowthActivityFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/GrowthActivityApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/GrowthActivityCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/GrowthActivityApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/GrowthActivityController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/GrowthActivityControllerCompatibilityTest.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java`

## Pre-Reservation Evidence

- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .` passed.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` reported current `canvas-web` 16 controllers / 222 endpoints and `/canvas/growth-activities` 0/25.
- Read-only sidecar Carver `019ec2db-cf1f-7510-9ddb-e5163a47ff74` summarized all 25 legacy routes with no file edits.

## Next Step

Generate the canonical worker prompt and spawn a real code-writing worker before moving this dispatch to `RUNNING`.
