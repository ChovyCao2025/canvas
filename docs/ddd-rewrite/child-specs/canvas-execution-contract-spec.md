# Canvas and Execution Contract Child Spec

This child spec defines the contract between canvas authoring and execution
runtime. It must be completed before rewriting either `canvas-context-canvas` or
`canvas-context-execution`.

---

## Problem

The old `CanvasService` mixes canvas lifecycle, version persistence, DAG
validation, trigger routing, scheduler registration, config cache updates,
execution orchestration, Redis operations, and pre-publish checks.

The rewrite must separate:

```text
canvas-context-canvas
  owns authoring, version, publish lifecycle, project/folder assignment

canvas-context-execution
  owns runtime DAG, node handlers, triggers, scheduler, wait/resume, trace
```

---

## Required API Types

### `PublishedCanvasDefinition`

Location:

```text
backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinition.java
```

Required fields:

```text
tenantId
canvasId
versionId
version
graphJson
publishedAt
executionOptions
```

Rules:

- immutable
- rejects missing tenant ID
- rejects missing canvas ID
- rejects blank graph JSON
- copies collection/map fields defensively

### `PublishedCanvasNodeDefinition`

Location:

```text
backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasNodeDefinition.java
```

Required fields:

```text
nodeId
nodeType
displayName
configJson
position
metadata
```

Rules:

- immutable
- rejects missing node ID
- rejects missing node type
- stores config as JSON text, not handler-specific Java classes

### `PublishedCanvasEdgeDefinition`

Location:

```text
backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasEdgeDefinition.java
```

Required fields:

```text
edgeId
sourceNodeId
targetNodeId
conditionJson
metadata
```

Rules:

- immutable
- rejects missing source or target node ID
- does not import execution runtime edge classes

### `PublishedCanvasDefinitionProvider`

Location:

```text
backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/PublishedCanvasDefinitionProvider.java
```

Required operations:

```text
PublishedCanvasDefinition getPublished(Long tenantId, Long canvasId)
```

Rules:

- execution can use this API
- execution cannot use canvas mappers

### `ExecutionPublicationPort`

Location:

```text
backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ExecutionPublicationPort.java
```

Required operations:

```text
void publish(PublishedCanvasDefinition definition)
void unpublish(Long tenantId, Long canvasId)
```

Rules:

- canvas publish calls this port
- execution implements this port
- the port does not expose Redis, scheduler, or cache implementation details

### `CanvasExecutionFacade`

Location:

```text
backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/CanvasExecutionFacade.java
```

Required operations:

```text
ExecutionResultView trigger(ExecutionRequestCommand command)
ExecutionTraceView trace(Long tenantId, String executionId)
```

Rules:

- web uses this facade for runtime operations
- canvas does not use execution internals
- `executionId` is frozen as a string because the current runtime exposes UUID
  style execution IDs through dry-run, trace, rerun, and frontend workflows.

### `NodeMetadataView`

Owner and location:

```text
backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/node/NodeMetadataView.java
```

Required fields:

```text
nodeType
displayName
category
configSchemaJson
inputPorts
outputPorts
requiredPluginId
enabled
disabledReason
```

Rules:

- execution owns runtime node metadata
- canvas, web, DSL, templates, CLI, and AI can read this view
- callers cannot mutate handler registry state through this view

### `PluginEnablementView`

Owner and location:

```text
backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/plugin/PluginEnablementView.java
```

Required fields:

```text
pluginId
version
enabled
permissions
nodeTypes
disabledReason
```

Rules:

- execution owns plugin enablement as runtime capability metadata
- Open Source Growth plugin work extends the existing registry contract
- no worker may create a second plugin registry surface

### `ExecutionDryRunFacade`

Owner and location:

```text
backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/dryrun/ExecutionDryRunFacade.java
```

Required operations:

```text
DryRunResultView dryRun(DryRunCommand command)
```

Required command fields:

```text
tenantId
canvasId
versionId
payloadJson
mockMode
```

Rules:

- dry-run uses execution validation and trace logic
- dry-run must not publish or mutate a canvas
- demo shortcuts must not bypass tenant, auth, trace, or audit behavior
- demo mock mode must be explicitly requested by the demo profile or dry-run
  command, and production/staging defaults must not enable mock providers
- demo dry-run output is evidence only; it must not publish, overwrite, or
  mutate a published canvas
- demo seeds and sample payloads must still resolve tenant, canvas, version,
  plugin enablement, execution validation, trace, and audit boundaries through
  the same public APIs used outside demo mode

### `ExecutionTraceView`

Owner and location:

```text
backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/trace/ExecutionTraceView.java
```

Required fields:

```text
tenantId
executionId
canvasId
status
startedAt
finishedAt
nodeResults
failureReason
```

Rules:

- trace is execution-owned
- AI failure explanation reads trace views, not persistence adapters
- node results must preserve the frontend-observed fields `nodeId`,
  `nodeType`, `status`, `error`, and output data without exposing
  `CanvasExecutionTraceDO`.

### `TemplateValidationPort`

Owner and location:

```text
backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/template/TemplateValidationPort.java
```

Required operations:

```text
TemplateValidationResult validateTemplate(TemplateValidationCommand command)
```

Rules:

- canvas owns template import into drafts
- execution may be called only through public validation or dry-run APIs
- template validation must not reach into execution adapters

### `AiJourneyDraftProposal`

Owner and location:

```text
backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/ai/AiJourneyDraftProposal.java
```

Required fields:

```text
tenantId
proposalId
sourcePrompt
dslDraft
riskFindings
traceReferences
createdAt
```

Rules:

- AI creates proposals or drafts only
- AI must not directly overwrite a published canvas
- risk and trace details are referenced through public views or findings

---

## Publish Flow

Target flow:

```text
CanvasPublishApplicationService
  validates canvas state through CanvasStateTransitionPolicy
  loads draft/version through CanvasRepository and CanvasVersionRepository
  builds PublishedCanvasDefinition
  marks canvas/version published
  calls ExecutionPublicationPort.publish(definition)
  saves canvas/version state
```

Execution implementation:

```text
ExecutionPublicationApplicationService
  validates graph through DagRuntimeService
  registers triggers through CanvasTriggerApplicationService
  registers schedules through CanvasSchedulerApplicationService
  updates execution runtime cache through execution-owned adapter
```

---

## Frozen API Decisions

DDD-C07 freezes these compile-time API artifacts as the handoff from canvas
authoring to execution runtime:

```text
canvas-context-canvas:
  PublishedCanvasDefinition
  PublishedCanvasNodeDefinition
  PublishedCanvasEdgeDefinition
  PublishedCanvasDefinitionProvider
  ExecutionPublicationPort
  TemplateValidationPort
  AiJourneyDraftProposal

canvas-context-execution:
  CanvasExecutionFacade
  ExecutionDryRunFacade
  ExecutionTraceView
  NodeMetadataView
  PluginEnablementView
```

Field mapping from the current implementation:

- `PublishedCanvasDefinition` carries tenant/canvas/version identity, raw
  `graphJson`, parsed node/edge views, `publishedAt`, and an
  `executionOptions` map for legacy publish/runtime options such as trigger
  type, schedule cron, valid window, control group, max total, per-user limits,
  cooldown, and attribution settings.
- `PublishedCanvasNodeDefinition` keeps node configuration as JSON text and
  generic metadata. It must not depend on handler-specific Java config classes.
- `PublishedCanvasEdgeDefinition` is frozen as the authoring/runtime boundary,
  but DDD-W07 and DDD-W08 must decide whether frontend `edges` are execution
  significant or authoring metadata because the current runtime derives most
  routing from node config.
- `ExecutionTraceView.executionId` and dry-run result execution IDs are strings.
  No DDD worker should introduce a numeric execution ID contract unless a
  separate migration is approved.
- Publish review and runtime manual approval are separate concerns: canvas owns
  publish review before version publication; execution owns runtime pause,
  manual approve/reject, wait, and resume.
- Legacy `/canvas/templates` routes remain bridge-compatible HTTP surface.
  Final template import ownership is `canvas-context-canvas`; dependency and
  dry-run validation use `canvas-context-execution`.

---

## Forbidden Dependencies

Canvas must not import:

```text
execution.adapter.persistence
execution.adapter.messaging
StringRedisTemplate for trigger routes
CanvasSchedulerService concrete runtime implementation
CanvasExecutionService concrete runtime implementation
```

Execution must not import:

```text
canvas.adapter.persistence
CanvasMapper
CanvasVersionMapper
CanvasDO
CanvasVersionDO
```

---

## Tests

Required contract tests:

```text
PublishedCanvasDefinitionTest
ExecutionPublicationPortContractTest
CanvasPublishApplicationServiceTest
ExecutionPublicationApplicationServiceTest
NodeMetadataContractTest
PluginEnablementContractTest
ExecutionDryRunContractTest
ExecutionTraceContractTest
TemplateValidationContractTest
AiJourneyDraftBoundaryContractTest
```

Required assertions:

```text
definition rejects missing required fields
definition defensively copies execution options
canvas publish calls execution publication port
execution publication validates graph before registering triggers
execution never imports canvas persistence
canvas never imports execution persistence
node metadata exposes schema without mutating handler registry state
plugin enablement cannot create a second registry
dry-run does not publish or mutate a canvas
trace explanation reads execution API views only
template validation does not import execution adapters
AI journey proposals do not overwrite published canvases
```

---

## Completion Criteria

The contract is complete when:

- [ ] API types compile.
- [ ] Contract tests pass.
- [ ] Canvas worker can implement publish without execution internals.
- [ ] Execution worker can implement runtime publication without canvas
      persistence.
- [ ] Architecture tests enforce the forbidden dependencies.
