# OSG-W12 Recovery Note

status: DONE_WITH_CONCERNS
task id: OSG-W12
dispatch id: dispatch-OSG-W12-ai-journey-backend-20260611-163007
worker: multi_agent_v1-worker Anscombe 019eb5e5-2ba4-7200-bd59-915e7b5fe023
branch: main
worktree: /Users/photonpay/project/canvas
base SHA: 01aac65697d524f4cf2e92d954db088895631004

## Reservation

OSG-W12 is reserved for the AI Journey Backend slice after G10 public
extension/API stability and OSG-W09 template import backend closure. The
assignment is DDD-final only: no old `canvas-engine` files, no direct database
writes, no real provider secret defaults, and no risk-context implementation
unless a separate risk-context packet is assigned.

## Exact Write Scope

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/ai/JourneyGenerationService.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/ai/JourneyGenerationServiceTest.java`
- `backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/ai/JourneyRiskAuditService.java`
- `backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/ai/JourneyRiskAuditServiceTest.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/trace/TraceExplanationFacade.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/trace/TraceExplanationFacadeTest.java`

## Required Reading For Worker

- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/collaboration-and-recovery-protocol.md`
- `docs/program-coordination/gate-verification-matrix.md`
- `docs/open-source-growth/contracts/ai-operator-contract.md`
- `docs/open-source-growth/contracts/canvas-dsl-v1.md`
- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`

## Existing Context

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ai/AiJourneyDraftProposal.java`
  is the frozen canvas/execution AI draft boundary from DDD-C07.
- OSG-W13 already added a standalone frontend mock assistant and did not reserve
  backend files.
- OSG-W12 must keep draft generation deterministic and local; it must not add
  live LLM/provider calls or secret defaults.
- OSG-W12 must not overwrite published canvases or bypass approval/publish
  boundaries.

## Verification Commands

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=JourneyGenerationServiceTest`
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=JourneyRiskAuditServiceTest`
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=TraceExplanationFacadeTest`
- `node tools/open-source-growth/guardrail-verifier.mjs`

## Preflight Evidence

- Backup manifest exists: `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md`
- Branch: `main`
- Base SHA: `01aac65697d524f4cf2e92d954db088895631004`
- Active dispatches before reservation: none
- `node tools/program-coordination/check-dispatch-state.mjs .` passed after
  OSG-W09 closure.
- `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  passed after OSG-W09 closure.
- `node --test tools/open-source-growth/guardrail-verifier.test.mjs` and
  `node tools/open-source-growth/guardrail-verifier.mjs` passed before
  reservation.
- `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` passed
  before reservation with the known RiskRuleValidator advisory only.
- Scoped `git status --short` for all six reserved OSG-W12 files returned no
  existing changes before reservation.

## Next Action

Worker prompt generation passed, real worker Anscombe
`019eb5e5-2ba4-7200-bd59-915e7b5fe023` was spawned, and the dispatch registry
was moved to `RUNNING`.

After one `wait_agent` call timed out at 180 seconds, the coordinator inspected
the reserved paths, evidence directory, and dispatch-state verifier:

- scoped `git status --short` over the six reserved files returned no visible
  file changes in the coordinator worktree
- scoped `git diff --name-only` over the six reserved files returned no visible
  file changes in the coordinator worktree
- this evidence directory contains this recovery note only; no
  `worker-return.md` has been produced yet
- `node tools/program-coordination/check-dispatch-state.mjs .` still passed

Do not wait repeatedly. The next coordinator cycle should inspect any worker
completion notification or returned files before moving to review, recovery, or
handoff.

## Worker Return

Anscombe returned `DONE_WITH_CONCERNS` after adding the six assigned files.
The coordinator wrote the canonical return packet to `worker-return.md` and
reran:

- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=JourneyGenerationServiceTest`
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=JourneyRiskAuditServiceTest`
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=TraceExplanationFacadeTest`
- `node tools/open-source-growth/guardrail-verifier.mjs`
- scoped reserved-file status, forbidden-reference scan, and
  trailing-whitespace scan

All focused coordinator verification passed with Java 21. The accepted return
concerns are Java 21 environment sensitivity and untracked-file attribution
until staging or commit.

## Review Closure

- Spec review: Carver `019eb5fe-d0ba-7821-9840-f2e8b0a69a30` returned
  `PASS_WITH_CONCERNS` with no required fixes. See `spec-review.md`.
- Quality review: Aquinas `019eb609-6845-7c11-a4bd-13b93f0697d7` returned
  `PASS_WITH_CONCERNS` with no required fixes. See `quality-review.md`.
- Final coordinator verification passed:
  - `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=JourneyGenerationServiceTest`
  - `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=JourneyRiskAuditServiceTest`
  - `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=TraceExplanationFacadeTest`
  - `node tools/open-source-growth/guardrail-verifier.mjs`
  - `node tools/program-coordination/check-dispatch-state.mjs .`
  - `bash docs/program-coordination/checks/program-coordination-checks.sh .`
  - scoped forbidden-reference, status, trailing-whitespace, and diff checks

Accepted concerns:

- Java 21 remains required for Maven verification.
- The six assigned files are untracked until the larger integration is staged or
  committed.
- `proposalId` can collide for same-prompt preview generations in the same
  millisecond; harden before durable proposal storage or concurrent UI caching.
- The risk audit exit-path heuristic is shallow and should be hardened before
  it becomes a publish gate.
- Tests do not cover every invalid request and disconnected-end edge case.
- The packet rollback wording has a stale frontend reference, but this dispatch
  return and ledger record carry the correct backend rollback path.

The active dispatch registry was cleared after closure.
