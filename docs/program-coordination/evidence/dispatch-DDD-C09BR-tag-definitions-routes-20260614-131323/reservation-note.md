# DDD-C09BR Reservation Note

Task: DDD-C09BR Tag Definitions Route Aliases
Dispatch: dispatch-DDD-C09BR-tag-definitions-routes-20260614-131323
Status: RESERVED
Time: 2026-06-14T13:13:23+08:00

Reserved exact files:
- backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpTagDefinitionFacade.java
- backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpTagDefinitionApplicationService.java
- backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpTagDefinitionCatalog.java
- backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpTagDefinitionApplicationServiceTest.java
- backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpTagDefinitionController.java
- backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpTagDefinitionControllerCompatibilityTest.java

Coordinator-owned files:
- docs/program-coordination/dispatch-state.json
- docs/program-coordination/progress-ledger.md
- docs/program-coordination/subagent-worker-packets.md
- docs/program-coordination/evidence/dispatch-DDD-C09BR-tag-definitions-routes-20260614-131323/**

Pre-dispatch evidence:
- `node tools/program-coordination/check-dispatch-state.mjs .` passed before reservation.
- Preflight top gap is `route:/canvas/tag-definitions`: old 1 controller / 8 endpoints, current 0 / 0.
- All six reserved code/test paths were absent before reservation.

Legacy reference only:
- backend/canvas-engine/src/main/java/org/chovy/canvas/web/TagDefinitionController.java

Forbidden:
- backend/canvas-engine/**
- any pom.xml
- old `org.chovy.canvas.domain`, `dto`, `query`, `dal`, or `engine` dependencies.
