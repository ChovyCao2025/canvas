# DDD-C09BA Reservation Note

Time: 2026-06-14T07:00:00+08:00

Coordinator reserved DDD-C09BA as a code-writing batch for the remaining
`/canvas/risk` production route parity gap after DDD-C09AZ closeout.

Scope:

- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskGovernanceFacade.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/application/RiskGovernanceApplicationService.java`
- `backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/domain/RiskGovernanceCatalog.java`
- `backend/canvas-context-risk/src/test/java/org/chovy/canvas/risk/application/RiskGovernanceApplicationServiceTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/RiskGovernanceController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/risk/RiskGovernanceControllerCompatibilityTest.java`

Existing final risk routes already own `POST /canvas/risk/decisions/evaluate`,
`GET /canvas/risk/lists`, `GET /canvas/risk/scenes`, and
`GET /canvas/risk/strategies`; this batch must not duplicate those mappings.

Scheduling rule for this dispatch: spawn a real worker before RUNNING, perform
meaningful non-overlapping coordinator work while it runs, and after one bounded
wait timeout inspect reserved paths/evidence and continue without idle polling.
