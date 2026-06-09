# Gate Verification Matrix

Date: 2026-06-08

## Purpose

This matrix turns terms such as `DDD-C00 complete`, `integrated`, `contracts
frozen`, and `APIs stable` into executable checks and required evidence.

Run commands from the repository root unless a command changes directory.

## Gate Matrix

| Gate | Blocks | Required commands | Required evidence |
| --- | --- | --- | --- |
| G0: Coordination docs valid | all dispatch | `bash docs/program-coordination/checks/program-coordination-checks.sh .`; `node tools/program-coordination/check-dispatch-state.mjs .`; `node --test tools/program-coordination/*.test.mjs` | commands pass; dispatch state has no invalid reservations or missing canonical fields |
| G0B: Backup and rollback checkpoint captured | first code-writing dispatch, isolated worktrees, and final cutover | `git status --short`; `git branch --show-current`; `git rev-parse HEAD`; `git worktree list`; `test -f docs/program-coordination/evidence/pre-rewrite-backup-manifest.md` | backup manifest exists; manifest names backup tag, bundle path, dirty diff path, untracked manifest path, data backup decision, and restore notes |
| G1: OSG guardrails valid | Open Source Growth workers | `node --test tools/open-source-growth/guardrail-verifier.test.mjs`; `node tools/open-source-growth/guardrail-verifier.mjs` | tests pass and verifier returns `{ "ok": true }` |
| G2: DDD guardrails runnable | DDD workers | `bash -n docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh`; `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` | script passes; before module creation it may print `No DDD rewrite module source directories found; nothing to scan yet.` |
| G3: Baseline captured | DDD-C00 and inventory consumers | `git status --short`; `cd backend && mvn clean install`; `cd frontend && npm run build && npm run test` | baseline evidence file records pass/fail status and dirty worktree state |
| G4: DDD-C00 foundation complete | DDD-W01/W02/W03 | `cd backend && mvn -q -DskipTests install`; `cd backend && mvn test -pl canvas-boot -Dtest=ModularArchitectureTest`; `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`; `bash docs/ddd-rewrite/inventory/check-inventory-readiness.sh .` | target module directories exist; architecture test exists; inventory files have exact source rows and target ownership rows; worker handoffs paste exact assigned rows |
| G5: DDD first wave integrated | DDD-W04/W05/W06 | `cd backend && mvn test -pl canvas-platform,canvas-context-risk,canvas-context-marketing`; `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` | platform, risk, and marketing workers returned `DONE` or resolved concerns |
| G6: DDD second wave integrated | DDD-C07 | `cd backend && mvn test -pl canvas-context-cdp,canvas-context-bi,canvas-context-conversation`; `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` | CDP, BI, and conversation workers returned `DONE` or resolved concerns |
| G7: Canvas/execution contract frozen | DDD-W07 and DDD-W08 | `test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinition.java`; `test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasNodeDefinition.java`; `test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasEdgeDefinition.java`; `test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ExecutionPublicationPort.java`; `test -f backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/CanvasExecutionFacade.java`; `test -f backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/node/NodeMetadataView.java`; `test -f backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/plugin/PluginEnablementView.java`; `test -f backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/dryrun/ExecutionDryRunFacade.java`; `test -f backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/trace/ExecutionTraceView.java`; `test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/template/TemplateValidationPort.java`; `test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ai/AiJourneyDraftProposal.java`; `cd backend && mvn test -pl canvas-context-canvas,canvas-context-execution -Dtest='PublishedCanvasDefinitionTest,ExecutionPublicationPortContractTest,NodeMetadataContractTest,PluginEnablementContractTest,ExecutionDryRunContractTest,ExecutionTraceContractTest,TemplateValidationContractTest,AiJourneyDraftBoundaryContractTest'`; `rg -n "PublishedCanvasDefinition|PublishedCanvasNodeDefinition|PublishedCanvasEdgeDefinition|ExecutionPublicationPort|NodeMetadataView|PluginEnablementView|ExecutionDryRunFacade|ExecutionTraceView|TemplateValidationPort|AiJourneyDraftProposal" docs/ddd-rewrite docs/open-source-growth docs/program-coordination` | API source files exist; named contract tests compile and pass; child spec and mirrored OSG contracts name the same fields and ownership rules |
| G8: Canvas integrated | DDD-W08 | `cd backend && mvn test -pl canvas-context-canvas`; `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` | canvas worker returned `DONE` or resolved concerns |
| G9: Execution integrated | OSG-C07 and OSG backend ecosystem workers | `cd backend && mvn test -pl canvas-context-execution`; `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .` | execution worker returned `DONE` or resolved concerns; node/handler extension points compile |
| G10: Public extension and API stability gate | OSG plugin, template, DSL, CLI API, AI backend workers | `node tools/open-source-growth/guardrail-verifier.mjs`; `cd backend && mvn test -pl canvas-context-execution -Dtest='*Plugin*Test'`; `cd backend && mvn test -pl canvas-context-canvas -Dtest=TemplateImportServiceTest,CanvasDslValidatorTest,CanvasDslMapperTest`; `cd backend && mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest` | plugin registry extends `PluginRegistryService`; no second registry exists; canvas/execution/web public APIs are named, compiled, tested, and mirrored in OSG contracts |
| G11: Ecosystem backend stable | playground and final release work | `cd backend && mvn test -pl canvas-context-canvas -Dtest=TemplateImportServiceTest,CanvasDslValidatorTest,CanvasDslMapperTest,JourneyGenerationServiceTest`; `cd backend && mvn test -pl canvas-context-execution -Dtest=TemplateDryRunContractTest,TraceExplanationFacadeTest`; `cd backend && mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest`; `cd backend && mvn test -pl canvas-context-marketing -Dtest=JourneyRiskAuditServiceTest`; `cd tools/canvas-cli && npm test` | template import, DSL backend, CLI API commands, and AI backend are integrated |
| G12: Final cutover | old `canvas-engine` removal | `cd backend && mvn clean install`; `cd frontend && npm run build && npm run test`; `bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .`; `node tools/open-source-growth/guardrail-verifier.mjs`; `docker compose -f docker-compose.demo.yml config`; `cd tools/canvas-cli && npm test` | full backend/frontend/guardrail/demo/CLI evidence recorded; compatibility contract test plan passes |

## Coordination Closure Gates

These checks apply to every wave before the next wave starts:

| Closure gate | Required evidence |
| --- | --- |
| Dispatch registry closure | `progress-ledger.md` and `dispatch-state.json` have no active `RUNNING` or `RETURNED` rows for the wave; `node tools/program-coordination/check-dispatch-state.mjs .` passes |
| Reviewer closure | reviewer board has no `FAIL` rows and no unassigned required fixes |
| Recovery closure | recovery audit reconciles ledger rows with `git status --short` and `git worktree list` |
| Concern closure | every accepted concern has an owner, follow-up action, and gate impact |
| Backup closure | every integrated worker has an actionable rollback path; every completed wave has a recorded checkpoint tag or branch; database-affecting work has a rollback or forward-fix decision |

## Notes

- If a module does not exist, any Maven command targeting it is expected to fail.
  That means the repository is before the corresponding readiness gate, not that
  the later worker task should be skipped.
- A worker return value of `DONE_WITH_CONCERNS` is not gate evidence until the
  coordinator resolves or explicitly accepts the concern.
- A local command passing in a narrow module does not prove the next gate unless
  the required evidence column is also satisfied.
