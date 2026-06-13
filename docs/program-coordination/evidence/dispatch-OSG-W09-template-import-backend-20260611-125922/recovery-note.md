# OSG-W09 Recovery Note

status: RESERVED
task id: OSG-W09
dispatch id: dispatch-OSG-W09-template-import-backend-20260611-125922
worker: pending spawn
branch: main
worktree: /Users/photonpay/project/canvas
base SHA: 01aac65697d524f4cf2e92d954db088895631004

## Reservation

OSG-W09 is reserved for the Template Import Backend slice after G10 public
extension/API stability and OSG-W10 DSL backend closure. The assignment is
DDD-final only: no old `canvas-engine` files, no direct database writes, and no
execution implementation packages outside the public template API scope.

## Exact Write Scope

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/template/**`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/template/TemplateImportServiceTest.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/template/**`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/template/TemplateDryRunContractTest.java`

## Required Reading For Worker

- `docs/program-coordination/subagent-worker-packets.md`
- `docs/program-coordination/collaboration-and-recovery-protocol.md`
- `docs/program-coordination/gate-verification-matrix.md`
- `docs/open-source-growth/contracts/template-pack-v1.md`
- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`

## Existing Context

- `TemplateImportService` already checks required plugin enablement before
  draft creation and delegates template validation through
  `TemplateValidationPort`.
- `TemplateImportServiceTest` already covers missing plugin blocking and
  successful draft creation.
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/template/**`
  and `TemplateDryRunContractTest` do not exist yet and are reserved for this
  worker.

## Verification Commands

- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=TemplateImportServiceTest`
- `cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-execution -Dtest=TemplateDryRunContractTest`
- `node tools/open-source-growth/guardrail-verifier.mjs`

## Preflight Evidence

- Backup manifest exists: `docs/program-coordination/evidence/pre-rewrite-backup-manifest.md`
- Branch: `main`
- Base SHA: `01aac65697d524f4cf2e92d954db088895631004`
- Active dispatches before reservation: none
- `node tools/program-coordination/check-dispatch-state.mjs .` passed after
  OSG-W10 closure.

## Next Action

Generate OSG-W09 worker prompt, spawn the real code-writing worker, then update
the dispatch registry with the actual worker id before marking `RUNNING`.
