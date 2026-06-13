# DDD-C07 Coordinator Return

Date: 2026-06-10

## Result

```text
task id: DDD-C07
status: DONE
owner: coordinator
branch: main
worktree: /Users/photonpay/project/canvas
base commit: 01aac65697d524f4cf2e92d954db088895631004
```

## Files Changed

- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinition.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasNodeDefinition.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasEdgeDefinition.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinitionProvider.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ExecutionPublicationPort.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/template/TemplateValidationPort.java`
- `backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ai/AiJourneyDraftProposal.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinitionTest.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/api/ExecutionPublicationPortContractTest.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/api/CanvasPublishApplicationServiceTest.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/api/template/TemplateValidationContractTest.java`
- `backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/api/ai/AiJourneyDraftBoundaryContractTest.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/CanvasExecutionFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/node/NodeMetadataView.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/plugin/PluginEnablementView.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/dryrun/ExecutionDryRunFacade.java`
- `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/trace/ExecutionTraceView.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/ExecutionPublicationApplicationServiceTest.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/node/NodeMetadataContractTest.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/plugin/PluginEnablementContractTest.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/dryrun/ExecutionDryRunContractTest.java`
- `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/trace/ExecutionTraceContractTest.java`
- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`
- `docs/open-source-growth/contracts/node-handler-contract.md`
- `docs/open-source-growth/contracts/plugin-manifest-v1.md`
- `docs/open-source-growth/contracts/template-pack-v1.md`
- `docs/open-source-growth/contracts/canvas-dsl-v1.md`
- `docs/open-source-growth/contracts/ai-operator-contract.md`

## Implementation Summary

DDD-C07 froze the canvas/execution API boundary before DDD-W07 and DDD-W08:

- canvas-owned published definition records and provider/publication port
- execution-owned trigger facade, dry-run facade, trace view, node metadata view,
  and plugin enablement view
- template validation and AI journey draft proposal boundaries
- contract tests for immutability, defensive copies, publish handoff, dry-run
  non-mutation, trace view shape, plugin enablement, node metadata, template
  validation, and AI draft-only semantics
- OSG contract mirror notes aligned to the DDD-C07 API names

Explorer inputs:

- Mill inspected the canvas-side legacy contract surface.
- Confucius inspected the execution-side legacy contract surface.

The contract freezes `executionId` as `String`, matching current dry-run, trace,
rerun, and frontend behavior.

## Verification

```text
RED:
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas,canvas-context-execution -Dtest='PublishedCanvasDefinitionTest,ExecutionPublicationPortContractTest,CanvasPublishApplicationServiceTest,ExecutionPublicationApplicationServiceTest,NodeMetadataContractTest,PluginEnablementContractTest,ExecutionDryRunContractTest,ExecutionTraceContractTest,TemplateValidationContractTest,AiJourneyDraftBoundaryContractTest'
  failed at testCompile because DDD-C07 API types did not exist

GREEN:
cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas,canvas-context-execution -Dtest='PublishedCanvasDefinitionTest,ExecutionPublicationPortContractTest,CanvasPublishApplicationServiceTest,ExecutionPublicationApplicationServiceTest,NodeMetadataContractTest,PluginEnablementContractTest,ExecutionDryRunContractTest,ExecutionTraceContractTest,TemplateValidationContractTest,AiJourneyDraftBoundaryContractTest'
  passed; canvas 7 tests, execution 6 tests

G7 file existence checks
  passed for all required API files

rg -n "PublishedCanvasDefinition|PublishedCanvasNodeDefinition|PublishedCanvasEdgeDefinition|PublishedCanvasDefinitionProvider|ExecutionPublicationPort|CanvasPublishApplicationServiceTest|ExecutionPublicationApplicationServiceTest|NodeMetadataView|PluginEnablementView|ExecutionDryRunFacade|ExecutionTraceView|TemplateValidationPort|AiJourneyDraftProposal|Backend Placement / Owner" docs/ddd-rewrite docs/open-source-growth docs/program-coordination
  passed; required names are mirrored

rg -n "trace\\(Long tenantId, Long executionId\\)|Long executionId" docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md backend/canvas-context-execution/src/main/java backend/canvas-context-execution/src/test/java
  no matches

cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp,canvas-context-bi,canvas-context-conversation
  passed; CDP 21 tests, BI 10 tests, conversation 8 tests

bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
  passed; advisory matches only pre-existing risk TypeCompatibility names

git diff --check
  passed
```

## Deferred Risks

- DDD-W07 must implement canvas import/export, template clone idempotency,
  project/folder metadata, approval-gated publish, canary, rollback, and graph
  parser mapping behind the frozen API.
- DDD-W08 must implement scheduler/cache/Redis/runtime publication, trace,
  wait/resume, request replay, manual approval, and node handler binding behind
  the frozen API.
- DDD-C09 must preserve HTTP route compatibility in `canvas-web`.
- The execution significance of frontend `edges` versus node-config-derived
  routing remains a DDD-W07/DDD-W08 implementation decision, but the boundary
  now carries both node and edge views.

## Rollback

Revert only the API, contract test, and contract documentation files listed
above. Do not revert prior context worker output.
