# DDD-C09BI Programmatic DSP Routes Reservation

Date: 2026-06-14

## Selection

`node tools/program-coordination/cutover-compatibility-preflight.mjs . --json`
reported `route:/canvas/programmatic-dsp` as the top current route gap after
DDD-C09BH:

- old controllers: 1
- old endpoints: 10
- current controllers: 0
- current endpoints: 0
- legacy reference:
  `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProgrammaticDspController.java`

## Legacy Routes

- `POST /canvas/programmatic-dsp/seats`
- `POST /canvas/programmatic-dsp/campaigns`
- `POST /canvas/programmatic-dsp/line-items`
- `POST /canvas/programmatic-dsp/supply-paths`
- `POST /canvas/programmatic-dsp/snapshots`
- `GET /canvas/programmatic-dsp/summary`
- `POST /canvas/programmatic-dsp/mutations`
- `POST /canvas/programmatic-dsp/mutations/{mutationId}/approve`
- `POST /canvas/programmatic-dsp/mutations/{mutationId}/execute`
- `GET /canvas/programmatic-dsp/mutations`

## Exact Reserved Files

- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/ProgrammaticDspFacade.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/ProgrammaticDspApplicationService.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/ProgrammaticDspCatalog.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/ProgrammaticDspApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/ProgrammaticDspController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/ProgrammaticDspControllerCompatibilityTest.java`

Coordinator-owned exceptions:

- `docs/program-coordination/dispatch-state.json`
- `docs/program-coordination/progress-ledger.md`
- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/evidence/dispatch-DDD-C09BI-programmatic-dsp-routes-20260614-085000/**`

## Scheduling Rule

Spawn one real sidecar worker for the reserved code scope, then keep the
coordinator on the critical path with RED tests, implementation, and verification.
Harvest the worker at most once when useful; do not idle poll.
