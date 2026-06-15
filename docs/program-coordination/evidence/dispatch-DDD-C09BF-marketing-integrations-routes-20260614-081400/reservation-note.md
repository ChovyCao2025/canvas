# DDD-C09BF Reservation Note

Date: 2026-06-14

## Selection

After DDD-C09BE closeout, `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` reported `route:/canvas/marketing-integrations` as the top route gap with 2 old controllers, 11 old endpoints, and 0 current endpoints.

Exact preflight inventory row:

```text
group=route:/canvas/marketing-integrations
oldControllerCount=2
oldEndpointCount=11
currentControllerCount=0
currentEndpointCount=0
representativeOldControllerFiles=[
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingIntegrationContractController.java,
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingIntegrationContractProbeController.java
]
```

Legacy reference files are read-only:

- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingIntegrationContractController.java`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingIntegrationContractProbeController.java`

## Exact Reserved Files

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingIntegrationFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingIntegrationApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingIntegrationCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MarketingIntegrationApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingIntegrationController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingIntegrationControllerCompatibilityTest.java`

## Scheduler Rule

Spawn a real code-writing worker before moving the dispatch to RUNNING. The coordinator must not repeatedly wait on the worker: wait at most once, inspect reserved paths/evidence if timed out, close the worker if no useful result exists, and recover locally.
