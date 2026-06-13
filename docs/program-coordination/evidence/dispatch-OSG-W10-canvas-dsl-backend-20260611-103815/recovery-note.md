# OSG-W10 Recovery Note

status: REVIEWING
task id: OSG-W10
dispatch id: dispatch-OSG-W10-canvas-dsl-backend-20260611-103815
worker: multi_agent_v1-worker Goodall 019eb491-4c8a-7201-8165-7bf0ac56b1b8
branch: main
worktree: /Users/photonpay/project/canvas
base SHA: 01aac65697d524f4cf2e92d954db088895631004

## Reservation

OSG-W10 is reserved for the Canvas DSL backend slice after OSG-C10 supplied the
G10 seed and OSG-W11 exposed CLI API commands. The assignment is DDD-final only:
no `canvas-engine` files, no old `CanvasService` bridge, no direct database
writes.

## Exact Write Scope

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/dsl/**`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/**`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidatorTest.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMapperTest.java`
- `backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java`
- `backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java`

## Required Reading For Worker

- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/collaboration-and-recovery-protocol.md`
- `docs/program-coordination/gate-verification-matrix.md`
- `docs/open-source-growth/contracts/canvas-dsl-v1.md`
- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`

## Verification Commands

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasDslValidatorTest,CanvasDslMapperTest`
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest`
- `node tools/open-source-growth/guardrail-verifier.mjs`

## Preflight Evidence

- Backup manifest exists: `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md`
- Branch: `main`
- Base SHA: `01aac65697d524f4cf2e92d954db088895631004`
- Active dispatches before reservation: none

## Next Action

Wait for OSG-W10 spec re-review from Banach
`019eb4a6-cd72-7892-affa-b463826f458b`. Goodall returned Banach blocker fixes,
coordinator focused verification passed, and re-review was requested.
