# Task Pack 06a: Coordinator Canvas/Execution Contract Freeze

**Owner:** Main coordinator

**Program:** DDD modular rewrite

**Task id:** DDD-C07

**Readiness level:** R4 candidate

**Target backend state:** DDD_FINAL_MODULE

**Goal:** Freeze the contract between canvas authoring and execution runtime
before dispatching `canvas-worker` or `execution-worker`.

---

## Allowed Write Scope

```text
backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/**
backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/api/**
backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/**
backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/**
docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md
docs/open-source-growth/contracts/node-handler-contract.md
docs/open-source-growth/contracts/plugin-manifest-v1.md
docs/open-source-growth/contracts/template-pack-v1.md
docs/open-source-growth/contracts/canvas-dsl-v1.md
docs/open-source-growth/contracts/ai-operator-contract.md
```

## Forbidden Changes

```text
backend/pom.xml
backend/canvas-web/**
backend/canvas-boot/**
backend/canvas-engine/**
runtime plugin implementations
template import implementation
DSL import/export implementation
```

## Run-With Constraints

Can run with:

```text
read-only reviewers
OSG docs-only workers that do not edit mirrored contract files
OSG frontend-only workers that do not depend on live canvas/execution APIs
```

Must not run with:

```text
DDD-W07
DDD-W08
OSG-C05B
OSG-C07
OSG-W07A through OSG-W07F
OSG-W09
OSG-W10
OSG-W12 backend implementation
any worker editing canvas/execution API packages or OSG contract files
```

This task is the single writer for canvas/execution shared contracts. If a
worker needs a contract change, it must return `NEEDS_CONTEXT`.

## Required Inputs

```text
docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md
docs/program-coordination/gate-verification-matrix.md
docs/open-source-growth/contracts/*.md
docs/ddd-rewrite/inventory/http-api-inventory.md
docs/ddd-rewrite/inventory/persistence-ownership.md
```

## Required Outputs

- `PublishedCanvasDefinition`
- `PublishedCanvasNodeDefinition`
- `PublishedCanvasEdgeDefinition`
- `PublishedCanvasDefinitionProvider`
- `ExecutionPublicationPort`
- `CanvasExecutionFacade`
- `NodeMetadataView`
- `PluginEnablementView`
- `ExecutionDryRunFacade`
- `ExecutionTraceView`
- `TemplateValidationPort`
- `AiJourneyDraftProposal`
- `PublishedCanvasDefinitionTest`
- `ExecutionPublicationPortContractTest`
- `CanvasPublishApplicationServiceTest`
- `ExecutionPublicationApplicationServiceTest`
- `NodeMetadataContractTest`
- `PluginEnablementContractTest`
- `ExecutionDryRunContractTest`
- `ExecutionTraceContractTest`
- `TemplateValidationContractTest`
- `AiJourneyDraftBoundaryContractTest`
- mirrored Open Source Growth contract placement notes

## Steps

- [ ] Define canvas API types needed by execution without importing canvas
      persistence.
- [ ] Define execution API types needed by canvas publish without importing
      execution persistence.
- [ ] Define node metadata and handler enablement boundary used by plugins,
      templates, DSL, CLI, AI, dry-run, and trace.
- [ ] Add contract tests for required assertions in the child spec.
- [ ] Mirror affected ownership rules into Open Source Growth contracts.
- [ ] Run the gate matrix G7 checks.

## Verification

```bash
test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinition.java
test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasNodeDefinition.java
test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasEdgeDefinition.java
test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinitionProvider.java
test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ExecutionPublicationPort.java
test -f backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/CanvasExecutionFacade.java
test -f backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/node/NodeMetadataView.java
test -f backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/plugin/PluginEnablementView.java
test -f backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/dryrun/ExecutionDryRunFacade.java
test -f backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/trace/ExecutionTraceView.java
test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/template/TemplateValidationPort.java
test -f backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ai/AiJourneyDraftProposal.java
(cd backend && mvn test -pl canvas-context-canvas,canvas-context-execution -Dtest='PublishedCanvasDefinitionTest,ExecutionPublicationPortContractTest,CanvasPublishApplicationServiceTest,ExecutionPublicationApplicationServiceTest,NodeMetadataContractTest,PluginEnablementContractTest,ExecutionDryRunContractTest,ExecutionTraceContractTest,TemplateValidationContractTest,AiJourneyDraftBoundaryContractTest')
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
rg -n "PublishedCanvasDefinition|PublishedCanvasNodeDefinition|PublishedCanvasEdgeDefinition|PublishedCanvasDefinitionProvider|ExecutionPublicationPort|CanvasPublishApplicationServiceTest|ExecutionPublicationApplicationServiceTest|NodeMetadataView|PluginEnablementView|ExecutionDryRunFacade|ExecutionTraceView|TemplateValidationPort|AiJourneyDraftProposal|Backend Placement / Owner" docs/ddd-rewrite docs/open-source-growth docs/program-coordination
```

## Rollback

Revert only the API and contract files created by this task. Do not revert
context worker output from earlier waves.

## Coordinator Response

Return:

```text
status:
files changed:
api contracts changed:
contracts changed:
tests run:
guardrail checks:
open risks:
coordinator actions needed:
worker dispatch constraints:
```
