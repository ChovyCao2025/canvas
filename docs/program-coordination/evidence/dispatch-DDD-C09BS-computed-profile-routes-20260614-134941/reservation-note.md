# DDD-C09BS Reservation Note

Task: DDD-C09BS Computed Profile Route Aliases
Dispatch: dispatch-DDD-C09BS-computed-profile-routes-20260614-134941
Status: RESERVED
Time: 2026-06-14T13:49:41+08:00

Reserved exact files:
- backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpComputedProfileFacade.java
- backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpComputedProfileApplicationService.java
- backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpComputedProfileCatalog.java
- backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpComputedProfileApplicationServiceTest.java
- backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpComputedProfileController.java
- backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpComputedProfileControllerCompatibilityTest.java

Coordinator-owned files:
- docs/program-coordination/dispatch-state.json
- docs/program-coordination/progress-ledger.md
- docs/program-coordination/subagent-worker-packets.md
- docs/program-coordination/evidence/dispatch-DDD-C09BS-computed-profile-routes-20260614-134941/**

Pre-dispatch evidence:
- `node tools/program-coordination/check-dispatch-state.mjs .` passed before reservation.
- Preflight top gap is `route:/cdp/computed-profile-attributes`: old 1 controller / 8 endpoints, current 0 / 0.
- All six reserved code/test paths were absent before reservation.

Legacy reference only:
- backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java

Forbidden:
- backend/canvas-engine/**
- any pom.xml
- old `org.chovy.canvas.domain`, `dto`, `query`, `dal`, or `engine` dependencies.
