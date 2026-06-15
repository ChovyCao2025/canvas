# DDD-C09BU Reservation Note

Status: RESERVED

Task:
- `DDD-C09BU`
- `dispatch-DDD-C09BU-channel-connectors-routes-20260614-232715`
- Route gap: `route:/channels`

Scope:
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/ChannelConnectorFacade.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/ChannelConnectorApplicationService.java`
- `backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/ChannelConnectorCatalog.java`
- `backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/ChannelConnectorApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/channels/ChannelConnectorController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/channels/ChannelConnectorControllerCompatibilityTest.java`

Pre-dispatch evidence:
- `node tools/program-coordination/check-dispatch-state.mjs .` passed.
- `git worktree list` inspected; existing prunable worktrees are unrelated.
- `git status --short --branch` inspected; many DDD-C09 uncommitted files exist from prior route batches.
- `node tools/program-coordination/cutover-compatibility-preflight.mjs . --json` passed and reported `route:/channels` as the top route gap with 1 old controller and 7 old endpoints.
- Active dispatch registry was empty before this reservation.

Constraints:
- Do not edit `backend/canvas-engine/**`.
- Do not edit Maven `pom.xml` files.
- Keep the route seed compact and deterministic; durable old mapper/provider parity remains out of scope for this compatibility batch.
- A real worker must be spawned before this dispatch moves to `RUNNING`.
