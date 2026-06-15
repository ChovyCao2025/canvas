# Subagent Worker Packets

Date: 2026-06-08

## Purpose

This file expands the max parallel plan into worker packets that the
coordinator can hand to subagents. It does not replace the detailed DDD task
packs or Open Source Growth plans. Each packet tells the subagent exactly what
it may read, what it may write, which gate must already be satisfied, what it
must not touch, and how it must return results.

## Universal Worker Rules

Every subagent receives these rules:

```text
You are not alone in the codebase. Other workers may be editing other modules.
Do not revert edits you did not make. Do not modify files outside your assigned
write scope. If you need a coordinator-owned file, stop and return NEEDS_CONTEXT.
If a target module or target API does not exist, stop and return NEEDS_CONTEXT
instead of inventing a parallel implementation.
Do not edit docs/program-coordination/progress-ledger.md directly. Return your
status packet; the coordinator records accepted status in the ledger.
If the coordinator did not provide a dispatch id for code-writing work, return
NEEDS_CONTEXT before editing files.
```

Every code-writing worker must return:

```text
status: DONE, DONE_WITH_CONCERNS, NEEDS_CONTEXT, or BLOCKED
task id:
dispatch id:
branch:
worktree:
base commit:
head commit:
files changed:
contracts changed:
tests run:
verification result:
verification output summary/path:
evidence artifact paths:
risks:
coordinator actions needed:
ledger update:
rollback path:
```

Any worker allowed to write a `CURRENT_ENGINE_BRIDGE` must receive this bridge
declaration before editing old `canvas-engine` files:

```text
Bridge Declaration:
  exact old service/API:
  exact old files:
  final DDD owner module:
  idempotency rule:
  removal gate:
  rollback path:
```

If the bridge declaration is absent or incomplete, return `NEEDS_CONTEXT`.

DDD code-writing workers also receive these dispatch overlay rules:

```text
Inventory rows required:
  The coordinator handoff must paste exact rows from the generated inventory
  files for this task. Globs and package names are not ownership proof. Empty
  inventory rows block dispatch.
Module POM rule:
  The assigned module pom.xml is read-only unless the packet has an explicit
  "Allowed module POM edits" line naming the dependency or plugin to add.
  backend/pom.xml is coordinator-owned. Cross-module dependency edits are
  coordinator-owned.
If either rule is not satisfied, return NEEDS_CONTEXT before editing code.
```

Read-only explorers must return:

```text
status:
task id:
files read:
inventory rows or findings:
ambiguous ownership:
recommended coordinator decisions:
```

## Coordinator-Only Tasks

Do not delegate these as code-writing subagents:

| Task | Reason |
| --- | --- |
| `DDD-C00` | edits root Maven, module skeletons, architecture tests, and inventory outputs |
| `DDD-C07` | freezes shared canvas/execution/public extension contracts |
| `DDD-C09` | performs web/boot cutover and final old-engine removal |
| `OSG-C05B` | mirrors OSG contracts into DDD docs |
| `OSG-C07` | decides plugin registry and handler extension ownership |

Coordinator edits count as the single active writer in shared workspace mode.

## DDD Explorer Packets

### DDD-E01: HTTP API Inventory Explorer

```text
Program: DDD modular rewrite
Task id: DDD-E01
Mode: read-only
Readiness gate: R0
Allowed write scope: none
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/**/*.java
  docs/ddd-rewrite/inventory/context-ownership-seed.md
Contracts to read:
  docs/ddd-rewrite/inventory/README.md
Output:
  HTTP route inventory grouped by controller, method, path, request, response,
  target context, and compatibility level
Can run with:
  DDD-E02, DDD-E03, DDD-E04, OSG docs/frontend/local CLI workers
Must not run with:
  no restriction, because this worker is read-only
```

### DDD-E02: Persistence Ownership Explorer

```text
Program: DDD modular rewrite
Task id: DDD-E02
Mode: read-only
Readiness gate: R0
Allowed write scope: none
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/**/*.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/**/*.java
  docs/ddd-rewrite/inventory/context-ownership-seed.md
Contracts to read:
  docs/ddd-rewrite/inventory/README.md
Output:
  DO/Mapper ownership inventory with ambiguous ownership called out
Can run with:
  DDD-E01, DDD-E03, DDD-E04, OSG docs/frontend/local CLI workers
Must not run with:
  no restriction, because this worker is read-only
```

### DDD-E03: Service Ownership Explorer

```text
Program: DDD modular rewrite
Task id: DDD-E03
Mode: read-only
Readiness gate: R0
Allowed write scope: none
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/**/*.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/**/*.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform/**/*.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/**/*.java
Contracts to read:
  docs/ddd-rewrite/inventory/README.md
  docs/ddd-rewrite/references/class-placement-reference.md
Output:
  service ownership inventory with target role: api, application, domain,
  adapter, config, or deletion candidate
Can run with:
  DDD-E01, DDD-E02, DDD-E04, OSG docs/frontend/local CLI workers
Must not run with:
  no restriction, because this worker is read-only
```

### DDD-E04: Test Ownership Explorer

```text
Program: DDD modular rewrite
Task id: DDD-E04
Mode: read-only
Readiness gate: R0
Allowed write scope: none
Read scope:
  backend/canvas-engine/src/test/java/**/*.java
Contracts to read:
  docs/ddd-rewrite/inventory/README.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Output:
  test ownership inventory with target module, porting decision, and replacement
  plan where needed
Can run with:
  DDD-E01, DDD-E02, DDD-E03, OSG docs/frontend/local CLI workers
Must not run with:
  no restriction, because this worker is read-only
```

## DDD Cutover Preparation Worker Packets

### DDD-C09A: Cutover Compatibility Preflight Tool

```text
Program: DDD modular rewrite
Task id: DDD-C09A
Mode: code-writing
Readiness gate: R5 after DDD-E01/E02/E03/E04 read-only explorer closure
Target backend state: TOOLING_ONLY
Allowed write scope:
  tools/program-coordination/cutover-compatibility-preflight.mjs
  tools/program-coordination/cutover-compatibility-preflight.test.mjs
Forbidden write scope:
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/dispatch-state.json
  backend/**
  frontend/**
  docs/ddd-rewrite/**
  docs/open-source/**
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/**/*.java
  backend/canvas-web/src/main/java/**/*.java
  backend/canvas-web/src/test/java/**/*.java
  docs/program-coordination/evidence/dispatch-DDD-E01-http-inventory-20260611-200950/worker-return.md
  docs/program-coordination/evidence/dispatch-DDD-E04-test-inventory-20260611-200950/worker-return.md
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add a deterministic Node.js preflight tool for DDD-C09 cutover readiness that
  reports old canvas-engine web controller/endpoint counts, current canvas-web
  controller/compatibility-test counts, required compatibility test file
  presence, and cutoverReady false/true. Default mode must print JSON and exit
  0 even when cutover is not ready. `--require-ready` must exit 1 with the same
  JSON when cutoverReady is false. Tests must use temporary fixture directories
  to prove ready and blocked modes; do not depend on the real dirty worktree for
  assertions except in a documented smoke command.
Verification commands:
  node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
  node tools/program-coordination/check-dispatch-state.mjs .
Can run with:
  read-only reviewers and no other workers editing tools/program-coordination/**
Must not run with:
  DDD-C09 or any worker editing tools/program-coordination/**
Rollback path:
  revert the two assigned tool files only
```

### DDD-C09B: Canvas API Compatibility Test Seed

```text
Program: DDD modular rewrite
Task id: DDD-C09B
Mode: code-writing
Readiness gate: R5 after DDD-C09A cutover preflight tooling closure
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
Forbidden write scope:
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/dispatch-state.json
  backend/canvas-web/src/main/java/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java
  backend/canvas-engine/**
  backend/canvas-boot/**
  backend/pom.xml
  frontend/**
  docs/**
Read scope:
  backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/dsl/**
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/**
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
  tools/program-coordination/cutover-compatibility-preflight.mjs
Contracts to read:
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add the first required DDD-C09 HTTP compatibility test target under
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/` for the
  already implemented Canvas DSL web surface. The test must be named
  `CanvasApiCompatibilityTest` so the DDD-C09A preflight recognizes one
  compatibility target as present. It must assert real, stable route behavior
  for Canvas DSL validate/map/import/export/diff envelopes by reusing the
  existing controller behavior, not by creating placeholder tests for
  unimplemented route groups. Do not edit production code or the existing
  `CanvasDslControllerCompatibilityTest`.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasApiCompatibilityTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -Dtest=CanvasApiCompatibilityTest,CanvasDslControllerCompatibilityTest
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
Can run with:
  read-only reviewers and no other workers editing backend/canvas-web/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  remove backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java only
```

### DDD-C09C: Marketing API Compatibility Test Seed

```text
Program: DDD modular rewrite
Task id: DDD-C09C
Mode: code-writing
Readiness gate: R5 after DDD-C09B Canvas API compatibility seed closure
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
Forbidden write scope:
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/dispatch-state.json
  backend/canvas-web/src/main/java/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java
  backend/canvas-engine/**
  backend/canvas-boot/**
  backend/pom.xml
  frontend/**
  docs/**
Read scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingCampaignController.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/MarketingCampaignControllerTest.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/**
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingCampaignApplicationService.java
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MarketingCampaignApplicationServiceTest.java
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
  tools/program-coordination/cutover-compatibility-preflight.mjs
Contracts to read:
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add the second required DDD-C09 HTTP compatibility test target under
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/` for the
  marketing campaign route group. The test must be named
  `MarketingApiCompatibilityTest` so the DDD-C09A preflight recognizes the
  target as present. It must assert stable route/envelope behavior for
  marketing campaign create/list/link/list-links/readiness/unlink using the
  DDD-final marketing API/facade records or test-local controller adapter.
  Do not edit production code, do not import `canvas-engine` into `canvas-web`,
  and do not create placeholder assertions for unrelated route groups.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=MarketingApiCompatibilityTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=MarketingApiCompatibilityTest,CanvasApiCompatibilityTest
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
Can run with:
  read-only reviewers and no other workers editing backend/canvas-web/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  remove backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java only
```

### DDD-C09D: Conversation API Compatibility Test Seed

```text
Program: DDD modular rewrite
Task id: DDD-C09D
Mode: code-writing
Readiness gate: R5 after DDD-C09C Marketing API compatibility seed closure
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
Forbidden write scope:
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/dispatch-state.json
  backend/canvas-web/src/main/java/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java
  backend/canvas-engine/**
  backend/canvas-boot/**
  backend/pom.xml
  frontend/**
  docs/**
Read scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationWorkspaceController.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationControllerTest.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ConversationWorkspaceControllerTest.java
  backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/api/**
  backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/application/ConversationApplicationService.java
  backend/canvas-context-conversation/src/test/java/org/chovy/canvas/conversation/application/ConversationApplicationServiceTest.java
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
  tools/program-coordination/cutover-compatibility-preflight.mjs
Contracts to read:
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add the third required DDD-C09 HTTP compatibility test target under
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/` for the
  conversation route group. The test must be named
  `ConversationApiCompatibilityTest` so the DDD-C09A preflight recognizes the
  target as present. It must assert stable route/envelope behavior for
  conversation ingress, duplicate ingress, work-item creation, assignment,
  status update, routing agent/rule upsert, and route-work-item using the
  DDD-final conversation API/facade records or a test-local controller adapter.
  Do not edit production code, do not import `canvas-engine` into `canvas-web`,
  and do not create placeholder assertions for unrelated route groups.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=ConversationApiCompatibilityTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CanvasApiCompatibilityTest,MarketingApiCompatibilityTest,ConversationApiCompatibilityTest
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
Can run with:
  read-only reviewers and no other workers editing backend/canvas-web/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  remove backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java only
```

### DDD-C09E: Risk API Compatibility Test Seed

```text
Program: DDD modular rewrite
Task id: DDD-C09E
Mode: code-writing
Readiness gate: R5 after DDD-C09D Conversation API compatibility seed closure
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
Forbidden write scope:
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/dispatch-state.json
  backend/canvas-web/src/main/java/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java
  backend/canvas-context-risk/**
  backend/canvas-engine/**
  backend/canvas-boot/**
  backend/pom.xml
  frontend/**
  docs/**
Read scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskDecisionController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/dto/RiskDecisionEvaluateRequest.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/dto/RiskDecisionEvaluateResponse.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/RiskDecisionControllerTest.java
  backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskDecisionFacade.java
  backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskDecisionCommand.java
  backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskDecisionView.java
  backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/application/RiskDecisionApplicationService.java
  backend/canvas-context-risk/src/test/java/org/chovy/canvas/risk/application/RiskDecisionApplicationServiceTest.java
  backend/canvas-context-risk/src/test/java/org/chovy/canvas/risk/domain/runtime/RiskDecisionServiceTest.java
  frontend/src/services/riskApi.ts
  frontend/src/services/riskApi.test.ts
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
  tools/program-coordination/cutover-compatibility-preflight.mjs
Contracts to read:
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add the fourth required DDD-C09 HTTP compatibility test target under
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/` for the
  risk decision route group. The test must be named `RiskApiCompatibilityTest`
  so the DDD-C09A preflight recognizes the target as present. It must assert
  stable route/envelope behavior for `POST /canvas/risk/decisions/evaluate`
  and `GET /canvas/risk/decisions/traces` using the DDD-final risk API/facade
  records or a test-local controller adapter. Cover the R envelope, tenant
  context overriding body `tenantId`, request/body field mapping, decision
  fields, score/band/reasons/matchedRules/labels/missingFeatures/
  traceAvailable/latencyMs, validation failures for missing `sceneKey`,
  missing subject identifier, future `eventTime`, deadline above scene budget,
  replay mismatch to HTTP 409, and trace query `sceneKey`/`limit` handling.
  Do not edit production code, do not import `canvas-engine` into `canvas-web`,
  and do not add placeholder assertions for risk scene, strategy, list, or lab
  route groups because those still need separate final DDD facade/bridge
  decisions.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=RiskApiCompatibilityTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CanvasApiCompatibilityTest,MarketingApiCompatibilityTest,ConversationApiCompatibilityTest,RiskApiCompatibilityTest
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
Can run with:
  read-only reviewers and no other workers editing backend/canvas-web/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  remove backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java only
```

### DDD-C09F: Execution API Compatibility Test Seed

```text
Program: DDD modular rewrite
Task id: DDD-C09F
Mode: code-writing
Readiness gate: R5 after DDD-C09E Risk API compatibility seed closure
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ExecutionApiCompatibilityTest.java
Forbidden write scope:
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/dispatch-state.json
  backend/canvas-web/src/main/java/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java
  backend/canvas-context-execution/**
  backend/canvas-engine/**
  backend/canvas-boot/**
  backend/pom.xml
  frontend/**
  docs/**
Read scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerTest.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/ExecutionControllerMachineAuthTest.java
  backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/CanvasExecutionFacade.java
  backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/trace/ExecutionTraceView.java
  backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/dryrun/ExecutionDryRunFacade.java
  backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/application/CanvasExecutionApplicationServiceTest.java
  backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/trace/ExecutionTraceContractTest.java
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
  tools/program-coordination/cutover-compatibility-preflight.mjs
Contracts to read:
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add the fifth required DDD-C09 HTTP compatibility test target under
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/` for the
  execution trigger and trace route group. The test must be named
  `ExecutionApiCompatibilityTest` so the DDD-C09A preflight recognizes the
  target as present. Use a test-local controller adapter around
  `CanvasExecutionFacade`; do not edit production code.

  Cover this exact execution seed:
  - `POST /canvas/execute/direct/{canvasId}` preserves the top-level R
    envelope (`code=0`, `message=success`, JSON `data`) and accepts old body
    fields `userId`, `inputParams`, and `idempotencyKey`
  - direct execution delegates to `CanvasExecutionFacade.trigger(...)` and
    asserts command fields: default/fixed tenant, path `canvasId`,
    `triggerType="DIRECT_CALL"`, body `userId`, payload map, and
    `dryRun=false`
  - direct execution returns final `executionId` and `status`, and rejects a
    blank or missing `userId` with HTTP 400 before calling the facade
  - `GET /canvas/{canvasId}/execution/{executionId}/trace` preserves the old
    trace route from `CanvasStatsController`, delegates to
    `CanvasExecutionFacade.trace(tenantId, executionId)`, and returns
    `R<List<Map<String,Object>>>`
  - trace response maps stable final `ExecutionTraceView.NodeResultView`
    fields to old response keys: `nodeId`, `nodeType`, `status`, `errorMsg`,
    and `outputData`
  - when returned trace `canvasId` does not match the path `canvasId`, the
    route returns an empty `data` list

  Preserve the existing top-level R envelope and HTTP status behavior for the
  covered routes. Do not import old `canvas-engine` code into `canvas-web`.
  Do not add placeholder coverage for behavior trigger, dry-run, approval,
  execution-request replay, plugin registry, node metadata, template dry-run,
  or idempotency enforcement; those remain future cutover blockers until final
  DDD facade or bridge decisions exist.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=ExecutionApiCompatibilityTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CanvasApiCompatibilityTest,MarketingApiCompatibilityTest,ConversationApiCompatibilityTest,RiskApiCompatibilityTest,ExecutionApiCompatibilityTest
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
Can run with:
  read-only reviewers and no other workers editing backend/canvas-web/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  remove backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ExecutionApiCompatibilityTest.java only
```

### DDD-C09G: BI API Compatibility Test Seed

```text
Program: DDD modular rewrite
Task id: DDD-C09G
Mode: code-writing
Readiness gate: R5 after DDD-C09F Execution API compatibility seed closure
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Forbidden write scope:
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/subagent-worker-packets.md
  backend/canvas-web/src/main/java/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ExecutionApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java
  backend/canvas-context-bi/**
  backend/canvas-engine/**
  backend/canvas-boot/**
  backend/pom.xml
  frontend/**
  docs/**
Read scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ExecutionApiCompatibilityTest.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasetController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPermissionController.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
  tools/program-coordination/cutover-compatibility-preflight.mjs
Contracts to read:
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add the sixth required DDD-C09 HTTP compatibility test target under
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/` for the
  BI catalog route seed. The test must be named `BiApiCompatibilityTest` so
  the DDD-C09A preflight recognizes the target as present. Use a test-local
  controller adapter around `BiCatalogFacade` / `BiCatalogApplicationService`
  with test-local in-memory repositories; do not edit production code.

  Cover this exact BI catalog seed:
  - workspace setup/upsert through the final facade with normalized
    `workspaceKey`, tenant id, actor, status, and top-level R envelope
    (`code=0`, `message=success`, JSON `data`); a helper/setup route is
    acceptable because no legacy workspace controller was found
  - `POST /canvas/bi/datasets/resources/{datasetKey}/draft` preserves the R
    envelope, tenant scoping, path/body key precedence, field and metric
    arrays, model map preservation, and normalized dataset/field/metric keys
  - `POST /canvas/bi/charts/resources/{chartKey}/draft` succeeds after a
    dataset exists and rejects a missing or archived dataset before a
    successful facade mutation
  - `POST /canvas/bi/dashboards/resources/{dashboardKey}/draft` and
    `GET /canvas/bi/dashboards/resources/{dashboardKey}` preserve dashboard
    read-model envelopes with chart list, dataset list, readiness status, and
    missing-chart blocker
  - `POST /canvas/bi/permissions/resources` preserves grant envelope behavior
  - an effective-access route equivalent maps actor, roles, and action to
    `BiAccessRequest` and proves deny precedence

  Preserve the existing top-level R envelope and HTTP status behavior for the
  covered routes. Do not import old `canvas-engine` code into `canvas-web`.
  Do not add placeholder coverage for BI acceleration, SQL preview,
  datasource import, export/import file, dashboard runtime state, resource
  collaboration/transfer/favorite, portal/embed, subscription, AI, capacity,
  query, permission request, row, or column routes.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=BiApiCompatibilityTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CanvasApiCompatibilityTest,MarketingApiCompatibilityTest,ConversationApiCompatibilityTest,RiskApiCompatibilityTest,ExecutionApiCompatibilityTest,BiApiCompatibilityTest
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
Can run with:
  read-only reviewers and no other workers editing backend/canvas-web/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  remove backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java only
```

### DDD-C09H: CDP API Compatibility Test Seed

```text
Program: DDD modular rewrite
Task id: DDD-C09H
Mode: code-writing
Readiness gate: R5 after DDD-C09G BI API compatibility seed closure
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java
Forbidden write scope:
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/subagent-worker-packets.md
  backend/canvas-web/src/main/java/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ExecutionApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java
  backend/canvas-context-cdp/**
  backend/canvas-engine/**
  backend/canvas-boot/**
  backend/pom.xml
  frontend/**
  docs/**
Read scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/RiskApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ExecutionApiCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpEventIngestionController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpUserController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseReadinessController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAudienceMaterializationController.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/*Cdp*.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/**
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/warehouse/*Audience*.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/**
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/**
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/**
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpEventIngestionApplicationServiceTest.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpTagApplicationServiceTest.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/AudienceSnapshotApplicationServiceTest.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseReadinessApplicationServiceTest.java
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
  tools/program-coordination/cutover-compatibility-preflight.mjs
Contracts to read:
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add the seventh required DDD-C09 HTTP compatibility test target under
  `backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/` for a
  narrow CDP seed. The test must be named `CdpApiCompatibilityTest` so the
  DDD-C09A preflight recognizes the final missing compatibility target as
  present. Use test-local controller adapters around the final CDP facades /
  application services with test-local in-memory repositories and ports; do
  not edit production code.

  Cover this exact CDP seed:
  - `POST /cdp/events/track` preserves the top-level R envelope, write-key
    tenant scoping, accepted/rejected counts, event body mapping, profile
    ensure behavior, event definition validation, duplicate-message rejection,
    and no-mutation behavior for rejected events
  - `POST /cdp/users/{userId}/tags`, `GET /cdp/users/{userId}/tags`,
    `GET /cdp/users/{userId}/tag-history`, and
    `DELETE /cdp/users/{userId}/tags/{tagCode}` preserve R envelopes,
    tenant/user/path mapping, manual tag normalization, history creation,
    remove behavior, and validation error HTTP status/body shape
  - an audience snapshot route equivalent maps to `AudienceSnapshotFacade`
    and proves lock snapshot, users list, and contains behavior through
    stable envelope data
  - `GET /warehouse/readiness` preserves warehouse readiness envelope data
    through `CdpWarehouseReadinessFacade`, including section readiness and
    production-ready/blocker fields

  Preserve existing top-level R envelope and HTTP status behavior for covered
  routes. Do not import old `canvas-engine` code into `canvas-web`. Do not add
  placeholder coverage for computed profile/tag jobs, write-key lifecycle,
  warehouse materialization operations beyond the narrow snapshot helper,
  availability/incidents, catalog, production-readiness proof, realtime
  pipelines/jobs/schemas, privacy erasure/tombstones, table governance,
  semantic metrics, SLO policy, quality, field governance, lineage, drift, or
  external OLAP/evidence route families.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CdpApiCompatibilityTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=CanvasApiCompatibilityTest,MarketingApiCompatibilityTest,ConversationApiCompatibilityTest,RiskApiCompatibilityTest,ExecutionApiCompatibilityTest,BiApiCompatibilityTest,CdpApiCompatibilityTest
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
Can run with:
  read-only reviewers and no other workers editing backend/canvas-web/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  remove backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CdpApiCompatibilityTest.java only
```

### DDD-C09I: Cutover Route Gap Report Tooling

```text
Program: DDD modular rewrite
Task id: DDD-C09I
Mode: code-writing
Readiness gate: R5 after DDD-C09H CDP API compatibility seed closure
Target backend state: TOOLING_ONLY
Allowed write scope:
  tools/program-coordination/cutover-compatibility-preflight.mjs
  tools/program-coordination/cutover-compatibility-preflight.test.mjs
Forbidden write scope:
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/subagent-worker-packets.md
  backend/**
  frontend/**
  docs/**
Read scope:
  tools/program-coordination/cutover-compatibility-preflight.mjs
  tools/program-coordination/cutover-compatibility-preflight.test.mjs
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/**/*.java
  backend/canvas-web/src/main/java/**/*.java
  docs/ddd-rewrite/inventory/http-api-inventory.md
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Extend the deterministic DDD-C09 preflight tooling so the JSON report names
  actionable route/controller gaps after all required compatibility seed files
  are present. The report must keep existing controller/endpoint counts and
  blockers unchanged, and add a stable `routeGapSummary` section that groups
  old/current controller endpoints by a deterministic route prefix or
  controller family. It must identify candidate missing route groups with old
  controller count, old endpoint count, current controller count, current
  endpoint count, and representative old controller files. Limit the reported
  candidate list to a stable small number so future coordinator dispatch can
  reserve an exact production controller group without manually scanning 806
  endpoints.

  Follow TDD: add a fixture test that fails before implementation because
  `routeGapSummary` is absent or incomplete, run that RED test, then implement
  the minimal parser/report change and rerun the focused tests. Do not edit
  production backend/frontend code and do not change cutover readiness semantics.
Verification commands:
  node --test tools/program-coordination/cutover-compatibility-preflight.test.mjs
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
  node tools/program-coordination/check-dispatch-state.mjs .
Can run with:
  read-only reviewers and no other workers editing tools/program-coordination/**
Must not run with:
  DDD-C09 or any worker editing tools/program-coordination/**
Rollback path:
  revert DDD-C09I edits to the two assigned preflight tool files only
```

### DDD-C09J: BI Catalog Production Controller Seed

```text
Program: DDD modular rewrite
Task id: DDD-C09J
Mode: code-writing
Readiness gate: R5 after DDD-C09I route gap report tooling closure
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, and 0 current production controllers/endpoints. This
  task intentionally covers only the existing final BI catalog facade routes
  already proven by BiApiCompatibilityTest, not the full BI route family.
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the
  exact allowed controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the
  exact allowed test file
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiAccessRequest.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasetController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPermissionController.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add the first real production BI controller in canvas-web by moving the
  compact BI catalog/permission compatibility adapter out of test-only form and
  wiring it to the final DDD BI API. The controller must use
  org.chovy.canvas.bi.api.BiCatalogFacade only, expose the same stable
  envelope shape already proven by BiApiCompatibilityTest, and preserve tenant
  and actor header behavior.

  Required routes:
  - POST /canvas/bi/workspaces
  - POST /canvas/bi/datasets/resources/{datasetKey}/draft
  - POST /canvas/bi/charts/resources/{chartKey}/draft
  - POST /canvas/bi/dashboards/resources/{dashboardKey}/draft
  - GET /canvas/bi/dashboards/resources/{dashboardKey}?workspaceId=...
  - POST /canvas/bi/permissions/resources
  - GET /canvas/bi/permissions/effective-access

  Follow TDD: first add a production-controller compatibility test that fails
  because BiCatalogController is absent. Then implement the minimal controller,
  rerun the focused test, and rerun the existing BI compatibility seed and
  cutover preflight. Do not alter BiCatalogFacade, BI domain/application code,
  old canvas-engine controllers, or unrelated canvas-web controllers.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=BiCatalogControllerCompatibilityTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=BiApiCompatibilityTest,BiCatalogControllerCompatibilityTest
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|adapter\\.persistence|Mapper|DO" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-web/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  remove backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  and backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java only
```

### DDD-C09AH: BI Resource Favorite Route Seed

```text
Program: DDD modular rewrite
Task id: DDD-C09AH
Mode: code-writing
Readiness gate: R5 after DDD-C09AG BI chart reference impact route seed closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceFavoriteCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceFavoriteView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiResourceFavoriteCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 22 current
  production endpoints after DDD-C09AG. Legacy source for this slice is
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceFavoriteController.java.
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceFavoriteController.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for the legacy BI favorite resource
  route family without depending on old canvas-engine services. Expose these
  production routes through the existing BiCatalogController and BiCatalogFacade:
  - POST /canvas/bi/resources/favorites
  - GET /canvas/bi/resources/favorites
  - DELETE /canvas/bi/resources/favorites/{resourceType}/{resourceKey}

  Preserve the stable compatibility envelope used by existing BI routes:
  success responses use code=0 and message=success with no errorCode or traceId;
  missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to analyst.
  The compact seed may use an in-memory final BI catalog owned by
  canvas-context-bi. It must scope favorites by tenant and actor, normalize
  resourceType/resourceKey consistently, de-duplicate repeated favorites,
  support optional resourceType filtering, return deterministic ordering, and
  make delete idempotent. Do not add persistence, old-domain dependencies, or
  cross-module dependencies.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|adapter\\.persistence|Mapper|DO" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceFavoriteCommand.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceFavoriteView.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiResourceFavoriteCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AH reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09AI: BI Resource Operations Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AI
Mode: code-writing
Readiness gate: R5 after DDD-C09AH recovered favorite route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceCommentCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceCommentView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceLockCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceLockView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceLocationCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceLocationView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceMoveCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceTransferCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceOwnershipView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPublishApprovalCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPublishApprovalReviewCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPublishApprovalView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiResourceOperationsCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 25 current
  production endpoints after recovered DDD-C09AH. Legacy sources for this
  batch include:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceCommentController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceCollaborationController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceLocationController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceTransferController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPublishApprovalController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceCommentController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceCollaborationController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceLocationController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiResourceTransferController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPublishApprovalController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/resource/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add a compact final-module BI resources operations batch through the existing
  BiCatalogController and BiCatalogFacade without depending on old
  canvas-engine services. Cover these production routes:
  - POST /canvas/bi/resources/comments
  - GET /canvas/bi/resources/comments
  - DELETE /canvas/bi/resources/comments/{commentId}
  - POST /canvas/bi/resources/locks/acquire
  - GET /canvas/bi/resources/locks
  - POST /canvas/bi/resources/locks/release
  - POST /canvas/bi/resources/locations
  - POST /canvas/bi/resources/move
  - GET /canvas/bi/resources/locations
  - POST /canvas/bi/resources/transfer
  - GET /canvas/bi/resources/ownerships
  - GET /canvas/bi/resources/publish-approvals
  - POST /canvas/bi/resources/publish-approvals
  - POST /canvas/bi/resources/publish-approvals/{approvalId}/review

  Preserve the stable compatibility envelope used by existing BI routes:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  analyst. The compact seed may use an in-memory final BI catalog owned by
  canvas-context-bi. It must scope state by tenant, normalize resource type and
  keys, keep deterministic ordering, make comment delete and lock release
  idempotent, and model publish approval review state without persistence.
  Do not add persistence, old-domain dependencies, cross-module dependencies,
  or POM changes.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|adapter\\.persistence|Mapper|DO" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiResourceOperationsCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AI reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09AJ: BI Portal And Big-Screen Resource Lifecycle Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AJ
Mode: code-writing
Readiness gate: R5 after DDD-C09AI BI resource operations route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPortalResourceCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPortalResourceView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiBigScreenResourceCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiBigScreenResourceView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceVersionView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPresentationResourceCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 39 current
  production endpoints after DDD-C09AI. Legacy sources for this batch are:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPortalController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiBigScreenController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPortalController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiBigScreenController.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for BI portal and big-screen resource
  lifecycle routes through the existing BiCatalogController and BiCatalogFacade
  without depending on old canvas-engine services. Cover these production
  routes:
  - GET /canvas/bi/portals/resources
  - GET /canvas/bi/portals/resources/{portalKey}
  - POST /canvas/bi/portals/resources/{portalKey}/draft
  - POST /canvas/bi/portals/resources/{portalKey}/publish
  - DELETE /canvas/bi/portals/resources/{portalKey}
  - GET /canvas/bi/portals/resources/{portalKey}/versions
  - POST /canvas/bi/portals/resources/{portalKey}/versions/{version}/restore
  - GET /canvas/bi/big-screens/resources
  - GET /canvas/bi/big-screens/resources/{screenKey}
  - POST /canvas/bi/big-screens/resources/{screenKey}/draft
  - POST /canvas/bi/big-screens/resources/{screenKey}/publish
  - DELETE /canvas/bi/big-screens/resources/{screenKey}
  - GET /canvas/bi/big-screens/resources/{screenKey}/versions
  - POST /canvas/bi/big-screens/resources/{screenKey}/versions/{version}/restore

  Preserve the stable compatibility envelope used by existing BI routes:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  analyst. The compact seed may use an in-memory final BI catalog owned by
  canvas-context-bi. It must scope state by tenant, normalize portal/screen
  keys, support list/detail/draft/publish/archive/version-list/version-restore,
  keep deterministic ordering, and make archive idempotent enough for
  compatibility. Do not add persistence, old-domain dependencies, cross-module
  dependencies, or POM changes.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|adapter\\.persistence|Mapper|DO" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPresentationResourceCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AJ reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09AK: BI AI Assistant Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AK
Mode: code-writing
Readiness gate: R5 after DDD-C09AJ BI portal and big-screen lifecycle route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiAiRequestCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiAiResponseView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiAiAssistantCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 53 current
  production endpoints after DDD-C09AJ. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiAiController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiAiController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/ai/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for BI AI assistant routes through the
  existing BiCatalogController and BiCatalogFacade without depending on old
  canvas-engine services or old BI AI agent services. Cover these production
  routes:
  - POST /canvas/bi/ai/ask
  - POST /canvas/bi/ai/interpret
  - POST /canvas/bi/ai/report
  - POST /canvas/bi/ai/dashboard-draft
  - POST /canvas/bi/ai/insights

  Preserve the stable compatibility envelope used by existing BI routes:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  analyst. The compact seed may use an in-memory/deterministic final BI AI
  catalog owned by canvas-context-bi. It must scope results by tenant, normalize
  operation names, preserve request payload hints in response metadata, keep
  deterministic IDs/order, and return route-specific response types or fields
  that are stable enough for compatibility tests. Do not add persistence,
  old-domain dependencies, cross-module dependencies, external LLM calls, or
  POM changes.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|adapter\\.persistence|Mapper|DO|BiAskDataAgentService|BiInterpretationAgentService|BiReportAgentService|BiDashboardDraftAgentService|BiInsightAgentService" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiAiAssistantCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AK reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09AL: BI Spreadsheet Resource Lifecycle Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AL
Mode: code-writing
Readiness gate: R5 after DDD-C09AK BI AI assistant route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSpreadsheetResourceCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSpreadsheetResourceView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceVersionView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPresentationResourceCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 58 current
  production endpoints after DDD-C09AK. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSpreadsheetController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSpreadsheetController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/spreadsheet/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPresentationResourceCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for BI spreadsheet resource lifecycle
  routes through the existing BiCatalogController and BiCatalogFacade without
  depending on old canvas-engine services. Cover these production routes:
  - GET /canvas/bi/spreadsheets/resources
  - GET /canvas/bi/spreadsheets/resources/{spreadsheetKey}
  - POST /canvas/bi/spreadsheets/resources/{spreadsheetKey}/draft
  - POST /canvas/bi/spreadsheets/resources/{spreadsheetKey}/publish
  - DELETE /canvas/bi/spreadsheets/resources/{spreadsheetKey}
  - GET /canvas/bi/spreadsheets/resources/{spreadsheetKey}/versions
  - POST /canvas/bi/spreadsheets/resources/{spreadsheetKey}/versions/{version}/restore

  Preserve the stable compatibility envelope used by existing BI routes:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  analyst. The compact seed may extend the existing in-memory final BI
  presentation catalog owned by canvas-context-bi. It must scope state by
  tenant, normalize spreadsheet keys, support list/detail/draft/publish/archive/
  version-list/version-restore, keep deterministic ordering, and make archive
  idempotent enough for compatibility. Do not add persistence, old-domain
  dependencies, cross-module dependencies, or POM changes.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|adapter\\.persistence|Mapper|DO|BiSpreadsheetResourceService|BiSpreadsheetResource|BiSpreadsheetVersionView" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPresentationResourceCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AL reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09AM: BI Permission Administration Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AM
Mode: code-writing
Readiness gate: R5 after DDD-C09AL BI spreadsheet lifecycle route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourcePermissionCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourcePermissionView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiRowPermissionCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiRowPermissionView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiColumnPermissionCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiColumnPermissionView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPermissionAuditEntryView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPermissionRequestCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPermissionRequestReviewCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiPermissionRequestView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPermissionAdministrationCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 65 current
  production endpoints after DDD-C09AL. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPermissionController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiPermissionController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/permission/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPermissionPolicy.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPermissionGrant.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for BI permission administration and
  permission request routes through the existing BiCatalogController and
  BiCatalogFacade without depending on old canvas-engine services. Cover these
  production routes:
  - GET /canvas/bi/permissions/resources
  - POST /canvas/bi/permissions/resources
  - DELETE /canvas/bi/permissions/resources/{id}
  - GET /canvas/bi/permissions/rows
  - POST /canvas/bi/permissions/rows
  - DELETE /canvas/bi/permissions/rows/{id}
  - GET /canvas/bi/permissions/columns
  - POST /canvas/bi/permissions/columns
  - DELETE /canvas/bi/permissions/columns/{id}
  - GET /canvas/bi/permissions/audit
  - GET /canvas/bi/permissions/requests
  - POST /canvas/bi/permissions/requests
  - POST /canvas/bi/permissions/requests/{id}/review

  Preserve the stable compatibility envelope used by existing BI routes:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  analyst. The compact seed may use an in-memory final BI permission catalog
  owned by canvas-context-bi. It must scope state by tenant, normalize resource
  types and keys consistently, support list filters, deterministic ordering,
  idempotent deletes, bounded audit limit, permission request submit/review,
  and keep existing grant/effective-access behavior intact. Do not add
  persistence, old-domain dependencies, cross-module dependencies, or POM
  changes.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|adapter\\.persistence|Mapper|DO|BiPermissionAdminService|BiPermissionRequestService|BiResourcePermission|BiRowPermission|BiColumnPermission|BiPermissionRequestView" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiPermissionAdministrationCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AM reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09AN: BI Chart Lifecycle Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AN
Mode: code-writing
Readiness gate: R5 after DDD-C09AM BI permission administration route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiChartLifecycleCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 77 current
  production endpoints after DDD-C09AM. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI domain/API/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiChartController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/chart/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for the remaining legacy chart
  lifecycle routes through the existing BiCatalogController and BiCatalogFacade
  without depending on old canvas-engine services. Cover these production
  routes:
  - POST /canvas/bi/charts/resources/{chartKey}/publish
  - DELETE /canvas/bi/charts/resources/{chartKey}
  - GET /canvas/bi/charts/resources/{chartKey}/versions
  - POST /canvas/bi/charts/resources/{chartKey}/versions/{version}/restore

  Preserve the stable compatibility envelope used by existing BI routes:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  analyst. The compact seed may use an in-memory final BI chart lifecycle
  catalog owned by canvas-context-bi. It must scope lifecycle state by tenant
  and chart key, publish by updating the final chart status, archive
  idempotently, list versions newest-first, and restore a selected version as a
  new draft current chart snapshot. Do not add old-domain dependencies,
  cross-module dependencies, POM changes, or persistence unless already
  available inside the exact allowed scope.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|BiChartResourceService|BiChartController" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiChartLifecycleCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AN reserved BI domain/API/application/controller
  and BI test files listed in this packet
```

### DDD-C09AO: BI Subscription And Delivery Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AO
Mode: code-writing
Readiness gate: R5 after DDD-C09AN BI chart lifecycle route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSubscriptionCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSubscriptionView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiAlertRuleCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiAlertRuleView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryRunResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryLogView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryAuditSummary.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryRetryResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryAttachmentView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryAttachmentDownload.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliveryAttachmentCleanupResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDeliverySchedulerResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiSubscriptionDeliveryCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 81 current
  production endpoints after DDD-C09AN. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSubscriptionController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSubscriptionController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/subscription/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for the legacy BI subscription,
  alert, delivery log, delivery attachment, and delivery scheduler routes
  through the existing BiCatalogController and BiCatalogFacade without depending
  on old canvas-engine services. Cover these production routes:
  - GET /canvas/bi/subscriptions
  - POST /canvas/bi/subscriptions
  - DELETE /canvas/bi/subscriptions/{id}
  - POST /canvas/bi/subscriptions/{id}/run
  - GET /canvas/bi/alerts
  - POST /canvas/bi/alerts
  - DELETE /canvas/bi/alerts/{id}
  - POST /canvas/bi/alerts/{id}/run
  - GET /canvas/bi/delivery-logs
  - GET /canvas/bi/delivery-audit
  - POST /canvas/bi/delivery-logs/retry
  - GET /canvas/bi/delivery-attachments
  - GET /canvas/bi/delivery-attachments/{id}/download
  - POST /canvas/bi/delivery-attachments/cleanup
  - POST /canvas/bi/delivery-scheduler/run

  Preserve the stable compatibility envelope used by existing BI routes for
  JSON responses: success responses use code=0 and message=success with no
  errorCode or traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor
  defaults to analyst. The download route should return ResponseEntity<byte[]>
  with content type and content disposition headers, matching the legacy binary
  response shape. The compact seed may use an in-memory final BI subscription
  delivery catalog owned by canvas-context-bi. It must scope state by tenant,
  normalize keys and job types, support idempotent deletes, deterministic
  newest-first log/attachment ordering, audit counts, retry/run/scheduler
  summaries, and safe empty/default behavior where optional legacy services
  previously returned empty results. Do not add old-domain dependencies,
  cross-module dependencies, POM changes, or persistence.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|BiSubscriptionAdminService|BiDeliveryRuntimeService|BiDeliverySchedulerService|BiDeliveryAttachmentService|BiSubscriptionController" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiSubscriptionDeliveryCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AO reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09AP: BI Query Operations Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AP
Mode: code-writing
Readiness gate: R5 after DDD-C09AO BI subscription and delivery route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCompileResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryResultView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryExplainResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCancelResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGateCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGateResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryContractGateCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryHistoryItemView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryHistoryDetailView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGovernanceSummaryView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGovernancePolicyCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGovernancePolicyView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryGovernanceAuditEntryView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCachePolicyCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCachePolicyView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCacheInvalidationCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCacheInvalidationResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiQueryCacheStatsView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceHealthView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceHealthSnapshotView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceHealthSloView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedTicketCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedTicketView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedTicketVerifyCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedTicketPayloadView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedQueryCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiEmbedTicketCleanupResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiQueryOperationsCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 96 current
  production endpoints after DDD-C09AO. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiQueryController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/embed/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for legacy BI query compile/execute,
  query history, governance policy/audit, cache policy/stats, datasource health,
  and embed-ticket/query routes through the existing BiCatalogController and
  BiCatalogFacade without depending on old canvas-engine services. Cover these
  production routes:
  - POST /canvas/bi/query/compile
  - POST /canvas/bi/query/execute
  - POST /canvas/bi/query/explain
  - POST /canvas/bi/query/cancel/{sqlHash}
  - POST /canvas/bi/query/execute-gated
  - POST /canvas/bi/query/execute-contract-gated
  - GET /canvas/bi/query/history
  - GET /canvas/bi/query/history/{historyId}
  - GET /canvas/bi/query/governance-summary
  - GET /canvas/bi/query/governance-policy
  - POST /canvas/bi/query/governance-policy
  - GET /canvas/bi/query/governance-audit
  - GET /canvas/bi/query/cache-policy
  - POST /canvas/bi/query/cache-policy
  - POST /canvas/bi/query/cache/invalidate
  - GET /canvas/bi/query/cache-stats
  - GET /canvas/bi/datasources/health
  - GET /canvas/bi/datasources/health/history
  - GET /canvas/bi/datasources/health/slo
  - POST /canvas/bi/embed-tickets
  - POST /canvas/bi/embed-tickets/verify
  - POST /canvas/bi/embed/query/execute
  - POST /canvas/bi/embed-tickets/cleanup

  Preserve the stable compatibility envelope used by existing BI routes for
  JSON responses: success responses use code=0 and message=success with no
  errorCode or traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor
  defaults to analyst. The compact seed may use an in-memory final BI query
  operations catalog owned by canvas-context-bi. It must scope mutable state by
  tenant, normalize dataset keys and SQL hashes, provide deterministic query
  results/explanations/history ordering, support idempotent cancel and cache
  invalidation behavior, enforce simple embed ticket origin/use behavior, and
  return safe empty/default datasource health and governance/cache summaries.
  Do not add old-domain dependencies, cross-module dependencies, POM changes,
  or persistence.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|BiQueryController|BiQueryExecutionService|BiEmbedTicketService|BiQueryGovernancePolicyService|BiQueryCachePolicyService|BiDatasourceHealthProvider" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiQueryOperationsCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AP reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09AQ: BI Datasource Operations Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AQ
Mode: code-writing
Readiness gate: R5 after DDD-C09AP BI query operations route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceConnectorView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceOnboardingCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceOnboardingView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceFileMaterializationResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceConnectionTestResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceCredentialRotationCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceCredentialRotationView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceSchemaPreviewView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceApiPreviewCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceApiPreviewView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDatasourceSchemaSnapshotView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiDatasourceOperationsCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 119 current
  production endpoints after DDD-C09AP. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasourceController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDatasourceController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/datasource/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/datasource/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for legacy BI datasource connector,
  onboarding, file-upload/materialize, connection-test, credential rotation,
  API/schema preview, schema sync, and schema snapshot routes through the
  existing BiCatalogController and BiCatalogFacade without depending on old
  canvas-engine services. Cover these production routes:
  - GET /canvas/bi/datasources/connectors
  - GET /canvas/bi/datasources/onboarding
  - POST /canvas/bi/datasources/onboarding
  - POST /canvas/bi/datasources/file-upload
  - POST /canvas/bi/datasources/file-upload/materialize
  - PUT /canvas/bi/datasources/onboarding/{id}
  - POST /canvas/bi/datasources/{id}/connection-test
  - POST /canvas/bi/datasources/{id}/credential-rotation
  - GET /canvas/bi/datasources/{id}/schema-preview
  - POST /canvas/bi/datasources/{id}/api-preview
  - POST /canvas/bi/datasources/{id}/schema-sync
  - GET /canvas/bi/datasources/{id}/schema-snapshot
  - GET /canvas/bi/datasources/{id}/schema-snapshots

  Preserve the stable compatibility envelope used by existing BI routes for
  JSON responses: success responses use code=0 and message=success with no
  errorCode or traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor
  defaults to analyst. The compact seed may use an in-memory final BI
  datasource operations catalog owned by canvas-context-bi. It must scope state
  by tenant, normalize connector/source keys, provide deterministic connector
  catalog and onboarding ordering, support idempotent-ish update/test/rotation
  behavior, return safe schema/API preview structures, and handle multipart
  file upload routes without persisting bytes. Do not add old-domain
  dependencies, cross-module dependencies, POM changes, or persistence.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|BiDatasourceController|BiDatasourceOnboardingService|BiDatasourceRuntimeService|BiDatasourceFileUploadService|BiDatasourceFileMaterializationService|DataSourceConfigService" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiDatasourceOperationsCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AQ reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09AR: BI Self-Service Export Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AR
Mode: code-writing
Readiness gate: R5 after DDD-C09AQ BI datasource operations route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServicePreviewCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportReviewCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportJobView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportJobDetailView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportDownload.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportCleanupResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportRetryResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiSelfServiceExportQueueResult.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiSelfServiceExportCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary top candidate route:/canvas/bi shows 20 old controllers,
  169 old endpoints, 1 current production controller, and 132 current
  production endpoints after DDD-C09AQ. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSelfServiceController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiSelfServiceController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/export/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/query/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for legacy BI self-service preview,
  export lifecycle, download, cleanup, retry, and queue-run routes through the
  existing BiCatalogController and BiCatalogFacade without depending on old
  canvas-engine services. Cover these production routes:
  - POST /canvas/bi/self-service/preview
  - POST /canvas/bi/self-service/exports
  - GET /canvas/bi/self-service/exports
  - POST /canvas/bi/self-service/exports/{id}/review
  - GET /canvas/bi/self-service/exports/{id}
  - GET /canvas/bi/self-service/exports/{id}/download
  - POST /canvas/bi/self-service/exports/{id}/cancel
  - POST /canvas/bi/self-service/exports/cleanup
  - POST /canvas/bi/self-service/exports/retry
  - POST /canvas/bi/self-service/exports/queue/run

  Preserve the stable compatibility envelope used by existing BI routes for
  JSON responses: success responses use code=0 and message=success with no
  errorCode or traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor
  defaults to analyst and missing X-Role defaults to ANALYST. Download returns
  bytes with content type and attachment disposition. The compact seed may use
  an in-memory final BI self-service export catalog owned by canvas-context-bi,
  scoped by tenant, with deterministic preview rows, export status transitions,
  review/cancel behavior, cleanup/retry/queue counters, and no persistence.
  Do not add old-domain dependencies, cross-module dependencies, POM changes, or
  direct persistence.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|BiSelfServiceController|BiSelfServiceExportService|BiSelfServicePreviewRequest|BiExportJobCommand|BiExportApprovalReviewCommand" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiSelfServiceExportCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AR reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09AS: BI Dashboard Resource Runtime Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AS
Mode: code-writing
Readiness gate: R5 after DDD-C09AR BI self-service export route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardCloneCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardExportPackageView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardImportCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardRuntimeStateCommand.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiDashboardRuntimeStateView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiResourceVersionView.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiDashboardResourceOperationsCatalog.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary route:/canvas/bi shows 20 old controllers, 169 old endpoints,
  1 current production controller, and 142 current production endpoints after
  DDD-C09AR. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/BiDashboardController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/dashboard/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/BiCatalogApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for legacy BI dashboard resource
  lifecycle, export/import, versions, and runtime-state routes through the
  existing BiCatalogController and BiCatalogFacade without depending on old
  canvas-engine services. Cover these production routes:
  - POST /canvas/bi/dashboards/resources/{dashboardKey}/clone
  - GET /canvas/bi/dashboards/resources/{dashboardKey}/export
  - GET /canvas/bi/dashboards/resources/{dashboardKey}/export-file
  - POST /canvas/bi/dashboards/resources/import
  - POST /canvas/bi/dashboards/resources/import-file
  - DELETE /canvas/bi/dashboards/resources/{dashboardKey}
  - GET /canvas/bi/dashboards/resources/{dashboardKey}/versions
  - POST /canvas/bi/dashboards/resources/{dashboardKey}/versions/{version}/restore
  - GET /canvas/bi/dashboards/resources/{dashboardKey}/runtime-state
  - POST /canvas/bi/dashboards/resources/{dashboardKey}/runtime-state

  Preserve the stable compatibility envelope used by existing BI routes for
  JSON responses: success responses use code=0 and message=success with no
  errorCode or traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor
  defaults to analyst and missing X-Role defaults to ANALYST. Export-file
  returns bytes with content type and attachment disposition. The compact seed
  may use an in-memory final BI dashboard operations catalog owned by
  canvas-context-bi, scoped by tenant, with deterministic clone/import/export,
  archive, version restore, and runtime-state behavior. Do not add old-domain
  dependencies, cross-module dependencies, POM changes, or direct persistence.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement the minimal final BI
  API/service/controller changes and rerun focused tests plus BI compatibility.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-context-bi,canvas-web -am -Dtest=BiCatalogApplicationServiceTest,BiCatalogControllerCompatibilityTest,BiApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.bi|BiDashboardController|BiDashboardResourceService|BiDashboardRuntimeStateService|BiDashboardCloneCommand|BiDashboardImportCommand" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/BiDashboardResourceOperationsCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AS reserved BI API/domain/application/controller
  and BI test files listed in this packet
```

### DDD-C09K: Conversation Production Controller Seed

```text
Program: DDD modular rewrite
Task id: DDD-C09K
Mode: code-writing
Readiness gate: R5 after DDD-C09J BI catalog production controller seed closure
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-web/src/main/java/org/chovy/canvas/web/conversation/ConversationController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/conversation/ConversationControllerCompatibilityTest.java
Inventory rows required:
  routeGapSummary candidate route:/canvas/conversations shows 4 old controllers,
  24 old endpoints, and 0 current production controllers/endpoints. This task
  intentionally covers only the existing final ConversationFacade routes already
  proven by ConversationApiCompatibilityTest, not the full legacy conversation
  workspace/provider/private-domain route family.
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-conversation
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-conversation/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the
  exact allowed controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the
  exact allowed test file
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/ConversationApiCompatibilityTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
  backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/api/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationWorkspaceController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationProviderWebhookController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationPrivateDomainController.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add the first real production conversation controller in canvas-web by moving
  the compact conversation compatibility adapter out of test-only form and
  wiring it to the final DDD ConversationFacade. The controller must use
  org.chovy.canvas.conversation.api.ConversationFacade only, expose the same
  stable envelope shape already proven by ConversationApiCompatibilityTest, and
  preserve tenant and actor header defaults for the final facade calls.

  Required routes:
  - POST /canvas/conversations/ingress
  - POST /canvas/conversations/workspace/sessions/{sessionId}/work-item
  - POST /canvas/conversations/workspace/work-items/{workItemId}/assign
  - POST /canvas/conversations/workspace/work-items/{workItemId}/status
  - POST /canvas/conversations/workspace/routing-agents
  - POST /canvas/conversations/workspace/routing-rules
  - POST /canvas/conversations/workspace/work-items/{workItemId}/route

  Follow TDD: first add a production-controller compatibility test that fails
  because ConversationController is absent. Then implement the minimal
  controller, rerun the focused test, and rerun the existing conversation
  compatibility seed and cutover preflight. Do not alter ConversationFacade,
  conversation domain/application code, old canvas-engine controllers, or
  unrelated canvas-web controllers.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=ConversationControllerCompatibilityTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-web -am -Dtest=ConversationApiCompatibilityTest,ConversationControllerCompatibilityTest
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --require-ready --json
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.conversation|adapter\\.persistence|Mapper|DO" backend/canvas-web/src/main/java/org/chovy/canvas/web/conversation/ConversationController.java backend/canvas-web/src/test/java/org/chovy/canvas/web/conversation/ConversationControllerCompatibilityTest.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-web/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  remove backend/canvas-web/src/main/java/org/chovy/canvas/web/conversation/ConversationController.java
  and backend/canvas-web/src/test/java/org/chovy/canvas/web/conversation/ConversationControllerCompatibilityTest.java only
```

## DDD Code Worker Packets

### DDD-W01: Platform Worker

```text
Program: DDD modular rewrite
Task id: DDD-W01
Readiness gate: R2 / G4
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-platform/**
Inventory rows required:
  exact platform rows from service-ownership.md, persistence-ownership.md,
  test-ownership.md, and cross-context-dependencies.md
Allowed module POM edits:
  none unless coordinator handoff names an exact dependency or plugin and the
  exact module pom.xml
Forbidden write scope:
  backend/pom.xml
  backend/canvas-common/**
  backend/canvas-web/**
  backend/canvas-boot/**
  backend/canvas-context-*/**
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture/**
  backend/canvas-engine/src/test/java/org/chovy/canvas/platform/**
Contracts to read:
  docs/ddd-rewrite/task-packs/01-worker-platform.md
  docs/ddd-rewrite/inventory/service-ownership.md
  docs/ddd-rewrite/inventory/test-ownership.md
Verification commands:
  cd backend && mvn test -pl canvas-platform
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
Can run with:
  DDD-W02, DDD-W03, OSG docs/frontend/local CLI workers
Must not run with:
  DDD-C00, DDD-C07, DDD-C09, any worker editing backend/pom.xml
Rollback path:
  revert files under backend/canvas-platform/**
```

### DDD-W02: Risk Worker

```text
Program: DDD modular rewrite
Task id: DDD-W02
Readiness gate: R2 / G4
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-risk/**
Inventory rows required:
  exact risk rows from persistence-ownership.md, service-ownership.md,
  test-ownership.md, and cross-context-dependencies.md
Allowed module POM edits:
  none unless coordinator handoff names an exact dependency or plugin and the
  exact module pom.xml
Forbidden write scope:
  backend/pom.xml
  backend/canvas-common/**
  backend/canvas-web/**
  backend/canvas-boot/**
  backend/canvas-context-execution/**
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Risk*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Risk*Mapper.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/**
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/**
Contracts to read:
  docs/ddd-rewrite/task-packs/02-worker-risk.md
  docs/ddd-rewrite/inventory/persistence-ownership.md
  docs/ddd-rewrite/inventory/test-ownership.md
Verification commands:
  cd backend && mvn test -pl canvas-context-risk
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
Can run with:
  DDD-W01, DDD-W03, OSG docs/frontend/local CLI workers
Must not run with:
  DDD-W08, because execution will later consume risk API and must not share
  risk persistence
Rollback path:
  revert files under backend/canvas-context-risk/**
```

### DDD-W03: Marketing Worker

```text
Program: DDD modular rewrite
Task id: DDD-W03
Readiness gate: R2 / G4
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-marketing/**
Inventory rows required:
  exact marketing rows from persistence-ownership.md, service-ownership.md,
  test-ownership.md, and cross-context-dependencies.md
Allowed module POM edits:
  none unless coordinator handoff names an exact dependency or plugin and the
  exact module pom.xml
Forbidden write scope:
  backend/pom.xml
  backend/canvas-common/**
  backend/canvas-web/**
  backend/canvas-boot/**
  backend/canvas-context-cdp/**
  backend/canvas-context-execution/**
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/growth/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Marketing*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Growth*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Marketing*Mapper.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Growth*Mapper.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/**
Contracts to read:
  docs/ddd-rewrite/task-packs/03-worker-marketing.md
  docs/ddd-rewrite/child-specs/marketing-pilot-spec.md
  docs/ddd-rewrite/inventory/persistence-ownership.md
Verification commands:
  cd backend && mvn test -pl canvas-context-marketing
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
Can run with:
  DDD-W01, DDD-W02, OSG docs/frontend/local CLI workers
Must not run with:
  DDD-W04, DDD-W08, OSG-W12 backend implementation
Rollback path:
  revert files under backend/canvas-context-marketing/**
```

### DDD-W04: CDP Worker

```text
Program: DDD modular rewrite
Task id: DDD-W04
Readiness gate: R3 / G5
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-cdp/**
Inventory rows required:
  exact CDP and warehouse rows from persistence-ownership.md,
  service-ownership.md, test-ownership.md, and cross-context-dependencies.md
Allowed module POM edits:
  none unless coordinator handoff names an exact dependency or plugin and the
  exact module pom.xml
Forbidden write scope:
  backend/pom.xml
  backend/canvas-common/**
  backend/canvas-web/**
  backend/canvas-boot/**
  backend/canvas-context-bi/**
  backend/canvas-context-execution/**
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/warehouse/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/doris/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Cdp*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Audience*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Tag*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Cdp*Mapper.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Audience*Mapper.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Tag*Mapper.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/**
Contracts to read:
  docs/ddd-rewrite/task-packs/04-worker-cdp.md
  docs/ddd-rewrite/inventory/persistence-ownership.md
Verification commands:
  cd backend && mvn test -pl canvas-context-cdp
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
Can run with:
  DDD-W05, DDD-W06, OSG docs/frontend/local CLI workers
Must not run with:
  DDD-W08, because execution consumes CDP API later
Rollback path:
  revert files under backend/canvas-context-cdp/**
```

### DDD-W05: BI Worker

```text
Program: DDD modular rewrite
Task id: DDD-W05
Readiness gate: R3 / G5
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/**
Inventory rows required:
  exact BI rows from persistence-ownership.md, service-ownership.md,
  test-ownership.md, and cross-context-dependencies.md
Allowed module POM edits:
  none unless coordinator handoff names an exact dependency or plugin and the
  exact module pom.xml
Forbidden write scope:
  backend/pom.xml
  backend/canvas-common/**
  backend/canvas-web/**
  backend/canvas-boot/**
  backend/canvas-context-cdp/**
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Bi*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Bi*Mapper.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/**
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/**
Contracts to read:
  docs/ddd-rewrite/task-packs/05-worker-bi.md
  docs/ddd-rewrite/inventory/persistence-ownership.md
Verification commands:
  cd backend && mvn test -pl canvas-context-bi
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
Can run with:
  DDD-W04, DDD-W06, OSG docs/frontend/local CLI workers
Must not run with:
  DDD-C09 or any worker moving BI controllers to canvas-web
Rollback path:
  revert files under backend/canvas-context-bi/**
```

### DDD-W06: Conversation Worker

```text
Program: DDD modular rewrite
Task id: DDD-W06
Readiness gate: R3 / G5
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-conversation/**
Inventory rows required:
  exact conversation rows from persistence-ownership.md,
  service-ownership.md, test-ownership.md, and cross-context-dependencies.md
Allowed module POM edits:
  none unless coordinator handoff names an exact dependency or plugin and the
  exact module pom.xml
Forbidden write scope:
  backend/pom.xml
  backend/canvas-common/**
  backend/canvas-web/**
  backend/canvas-boot/**
  backend/canvas-context-marketing/**
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Conversation*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Conversation*Mapper.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/**
Contracts to read:
  docs/ddd-rewrite/task-packs/06-worker-conversation.md
  docs/ddd-rewrite/inventory/persistence-ownership.md
Verification commands:
  cd backend && mvn test -pl canvas-context-conversation
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
Can run with:
  DDD-W04, DDD-W05, OSG docs/frontend/local CLI workers
Must not run with:
  DDD-C09 or any worker moving conversation controllers to canvas-web
Rollback path:
  revert files under backend/canvas-context-conversation/**
```

### DDD-W07: Canvas Worker

```text
Program: DDD modular rewrite
Task id: DDD-W07
Readiness gate: R4 / G7
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-canvas/**
Inventory rows required:
  exact canvas draft/version/publish rows from persistence-ownership.md,
  service-ownership.md, test-ownership.md, http-api-inventory.md, and
  cross-context-dependencies.md
Allowed module POM edits:
  none unless coordinator handoff names an exact dependency or plugin and the
  exact module pom.xml
Forbidden write scope:
  backend/pom.xml
  backend/canvas-common/**
  backend/canvas-web/**
  backend/canvas-boot/**
  backend/canvas-context-execution/**
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Canvas*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Canvas*Mapper.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/**
Contracts to read:
  docs/ddd-rewrite/task-packs/07-worker-canvas.md
  docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md
  docs/open-source-growth/contracts/template-pack-v1.md
  docs/open-source-growth/contracts/canvas-dsl-v1.md
Verification commands:
  cd backend && mvn test -pl canvas-context-canvas
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
Can run with:
  docs-only OSG workers and frontend-only workers that do not edit canvas-editor
Must not run with:
  DDD-W08, OSG-W09, OSG-W10 backend, OSG-W12 backend
Rollback path:
  revert files under backend/canvas-context-canvas/**
```

### DDD-W08: Execution Worker

```text
Program: DDD modular rewrite
Task id: DDD-W08
Readiness gate: R4 after DDD-W07 integration / G8
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-execution/**
Inventory rows required:
  exact execution, trigger, scheduler, handler, wait/resume, MQ, Redis, trace,
  and recovery rows from persistence-ownership.md, service-ownership.md,
  test-ownership.md, http-api-inventory.md, and cross-context-dependencies.md
Allowed module POM edits:
  none unless coordinator handoff names an exact dependency or plugin and the
  exact module pom.xml
Forbidden write scope:
  backend/pom.xml
  backend/canvas-common/**
  backend/canvas-web/**
  backend/canvas-boot/**
  backend/canvas-context-canvas/**
  backend/canvas-context-risk/**
  backend/canvas-context-cdp/**
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/redis/**
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/*Execution*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasWait*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasMqTrigger*DO.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/*Execution*Mapper.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasWait*Mapper.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CanvasMqTrigger*Mapper.java
  backend/canvas-engine/src/test/java/org/chovy/canvas/engine/**
Contracts to read:
  docs/ddd-rewrite/task-packs/08-worker-execution.md
  docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md
  docs/open-source-growth/contracts/node-handler-contract.md
  docs/open-source-growth/contracts/plugin-manifest-v1.md
Verification commands:
  cd backend && mvn test -pl canvas-context-execution
  bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
Can run with:
  docs-only OSG workers and frontend-only workers not editing execution-owned files
Must not run with:
  DDD-W07, OSG-W07A through OSG-W07F, OSG-W09, OSG-W10 backend, OSG-W12 backend
Rollback path:
  revert files under backend/canvas-context-execution/**
```

## Open Source Growth Worker Packets

### OSG-W01: Open Source Entry Docs

```text
Program: Open Source Growth
Task id: OSG-W01
Readiness gate: R0 / G0 / G1
Target backend state: DOCS_ONLY
Allowed write scope:
  README.md
  .github/ISSUE_TEMPLATE/**
  .github/pull_request_template.md
  CONTRIBUTING.md
  CODE_OF_CONDUCT.md
  SECURITY.md
  docs/open-source/quickstart.md
  docs/open-source/positioning.md
Forbidden write scope:
  backend/**
  frontend/**
  docker-compose.local.yml
  production or staging config
Contracts to read:
  docs/open-source-growth/open-source-growth-plan.md
  docs/open-source-growth/phase-gates.md
Verification commands:
  node tools/open-source-growth/guardrail-verifier.mjs
Can run with:
  DDD explorers, DDD-C00, OSG-W02, OSG-W03, OSG-W04, OSG-W05A, OSG-W06
Must not run with:
  another worker editing README.md or root community files
Rollback path:
  revert only files in allowed write scope
```

### OSG-W02: Demo Shell And Mock Catalog

```text
Program: Open Source Growth
Task id: OSG-W02
Readiness gate: R0 / G0 / G1
Target backend state: DOCS_ONLY or CURRENT_ENGINE_BRIDGE for demo seed only
Bridge Declaration required before editing old canvas-engine files:
  exact old service/API:
  exact old files:
  final DDD owner module:
  idempotency rule:
  removal gate:
  rollback path:
Allowed write scope:
  docker-compose.demo.yml
  wiremock/**
  docs/open-source/playground.md
  docs/open-source/quickstart.md if reserved by coordinator
  backend/canvas-engine/src/main/resources/application-demo.yml only when the
  Bridge Declaration assigns this exact file
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/** only when
  the Bridge Declaration assigns these exact files
Forbidden write scope:
  backend/canvas-engine/src/main/resources/application-prod.yml
  backend/canvas-engine/src/main/resources/application-staging.yml
  production secrets
  execution shortcuts that bypass trace, tenant, auth, or publish state
Contracts to read:
  docs/open-source-growth/contracts/demo-profile-contract.md
  docs/program-coordination/execution-readiness-audit.md
Verification commands:
  docker compose -f docker-compose.demo.yml config
  node tools/open-source-growth/guardrail-verifier.mjs
Can run with:
  DDD explorers, DDD-C00, OSG-W01 if quickstart ownership is reserved
Must not run with:
  DDD-C09 or another worker editing runtime profile/config
Rollback path:
  revert demo compose, wiremock, docs, and approved bridge files
```

### OSG-W03: Frontend Schema Config Foundation

```text
Program: Open Source Growth
Task id: OSG-W03
Readiness gate: R0 / G0 / G1
Target backend state: DOCS_ONLY for backend; frontend-only implementation allowed
Allowed write scope:
  frontend/src/components/config-panel/**
  frontend/src/plugins/pluginManifest.ts
  frontend/src/plugins/pluginRegistry.ts
  frontend/src/plugins/**/*.test.ts
Forbidden write scope:
  frontend/src/App.tsx
  frontend/src/types/index.ts
  backend/**
Contracts to read:
  docs/open-source-growth/contracts/plugin-manifest-v1.md
  docs/open-source-growth/contracts/node-handler-contract.md
Verification commands:
  cd frontend && npm run test -- --run schemaConfigPanel
  cd frontend && npm run build
Can run with:
  DDD explorers, DDD-C00, OSG-W01, OSG-W02, OSG-W04, OSG-W06
Must not run with:
  OSG-W07 frontend plugin UI worker unless coordinator reserves plugin files
Rollback path:
  revert config-panel and frontend plugin files
```

### OSG-W04: Local CLI Validate And Diff

```text
Program: Open Source Growth
Task id: OSG-W04
Readiness gate: R0 / G0 / G1
Target backend state: DOCS_ONLY for backend; local CLI allowed
Allowed write scope:
  tools/canvas-cli/**
  docs/open-source/marketingops-as-code.md
Forbidden write scope:
  backend/**
  frontend/**
  CLI commands that call backend write APIs before G10 public extension/API
  stability gate passes
Contracts to read:
  docs/open-source-growth/contracts/canvas-dsl-v1.md
Verification commands:
  cd tools/canvas-cli && npm test
  cd tools/canvas-cli && node src/index.mjs --help
Can run with:
  DDD explorers, DDD-C00, OSG-W01, OSG-W02, OSG-W03, OSG-W06
Must not run with:
  OSG-W11 unless coordinator serializes tools/canvas-cli/**
Rollback path:
  revert tools/canvas-cli/** and MarketingOps docs
```

### OSG-W05A: Contract Draft Worker

```text
Program: Open Source Growth
Task id: OSG-W05A
Readiness gate: R0 / G0 / G1
Target backend state: DOCS_ONLY
Allowed write scope:
  exactly one file under docs/open-source-growth/contracts/**
Forbidden write scope:
  docs/ddd-rewrite/**
  backend/**
  frontend/**
Contracts to read:
  docs/program-coordination/ddd-open-source-growth-integration.md
  docs/program-coordination/execution-readiness-audit.md
Verification commands:
  node tools/open-source-growth/guardrail-verifier.mjs
Can run with:
  other OSG-W05A workers only if each owns a different contract file
Must not run with:
  OSG-C05B on the same contract mirror
Rollback path:
  revert the assigned contract file
```

### OSG-W06: English Docs And Release Drafts

```text
Program: Open Source Growth
Task id: OSG-W06
Readiness gate: R0 / G0 / G1
Target backend state: DOCS_ONLY
Allowed write scope:
  docs/open-source/en/**
  docs/open-source/release-posts/**
Forbidden write scope:
  backend/**
  frontend/**
  root README.md unless reserved by coordinator
Contracts to read:
  docs/open-source-growth/open-source-growth-spec.md
  docs/open-source-growth/success-metrics.md
Verification commands:
  node tools/open-source-growth/guardrail-verifier.mjs
Can run with:
  DDD explorers, DDD-C00, OSG-W01, OSG-W02, OSG-W03, OSG-W04
Must not run with:
  another worker editing the same English docs or release post files
Rollback path:
  revert docs/open-source/en/** and docs/open-source/release-posts/**
```

### OSG-W07A Through OSG-W07F: Official Plugin Workers

Use one worker per row.

| Task | Plugin | Allowed write scope | Docs file |
| --- | --- | --- | --- |
| OSG-W07A | webhook | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/webhook/**`; `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/webhook/**` | `docs/open-source/plugins/official/webhook.md` |
| OSG-W07B | message | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/message/**`; `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/message/**` | `docs/open-source/plugins/official/message.md` |
| OSG-W07C | coupon | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/coupon/**`; `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/coupon/**` | `docs/open-source/plugins/official/coupon.md` |
| OSG-W07D | approval | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/approval/**`; `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/approval/**` | `docs/open-source/plugins/official/approval.md` |
| OSG-W07E | ai | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/ai/**`; `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/ai/**` | `docs/open-source/plugins/official/ai.md` |
| OSG-W07F | risk-check | `backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/adapter/plugin/official/risk/**`; `backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/adapter/plugin/official/risk/**` | `docs/open-source/plugins/official/risk-check.md` |

Shared packet:

```text
Readiness gate: R5 after G9 and OSG-C07
Target backend state: DDD_FINAL_MODULE
OSG-C07 decision:
  canvas-platform owns plugin registry metadata, manifest validation,
  permissions, compatibility, persistence, and enablement state
  canvas-context-execution owns handler discovery/binding, node metadata,
  runtime validation hooks, trace failure paths, and PluginEnablementView
  consumption
  old canvas-engine PluginRegistryService, JdbcPluginRepository,
  PluginRegistryController, HandlerRegistry, and built_in_plugin_registry are
  legacy source rows or CURRENT_ENGINE_BRIDGE inputs only
G10 requirement:
  before a code-writing plugin dispatch is marked RUNNING, rerun the G10
  public extension/API stability checks or record the exact passing evidence
Forbidden write scope:
  HandlerRegistry
  PluginRegistryService
  JdbcPluginRepository
  backend/pom.xml
  any other plugin package
Contracts to read:
  docs/program-coordination/evidence/dispatch-OSG-C07-plugin-registry-decision-20260610-142556/worker-return.md
  docs/open-source-growth/contracts/plugin-manifest-v1.md
  docs/open-source-growth/contracts/node-handler-contract.md
Verification commands:
  cd backend && mvn test -pl canvas-context-execution -Dtest='*Plugin*Test'
  node tools/open-source-growth/guardrail-verifier.mjs
Can run with:
  other official plugin workers if each owns a different plugin package
Must not run with:
  DDD-W08, OSG-C07, or any worker editing shared registry/handler files
Rollback path:
  revert assigned plugin package, assigned test files, and docs file
```

### OSG-W08: Template Content And Catalog

```text
Program: Open Source Growth
Task id: OSG-W08
Readiness gate: R0 for docs/catalog, R5 for backend import integration
Target backend state: DOCS_ONLY unless coordinator assigns DDD_FINAL_MODULE work
Allowed write scope:
  docs/open-source/templates/**
  assigned template data files
  frontend/src/pages/canvas-list/templateCatalog.ts if reserved by coordinator
Forbidden write scope:
  backend template import service
  frontend route shell
Contracts to read:
  docs/open-source-growth/contracts/template-pack-v1.md
Verification commands:
  node tools/open-source-growth/guardrail-verifier.mjs
Can run with:
  DDD context workers if only docs/data files are edited
Must not run with:
  OSG-W09 on backend template import or same catalog file
Rollback path:
  revert assigned template docs/data/catalog changes
```

### OSG-W09: Template Import Backend

```text
Program: Open Source Growth
Task id: OSG-W09
Readiness gate: R5 / G10
Target backend state: DDD_FINAL_MODULE, or CURRENT_ENGINE_BRIDGE only with a
complete Bridge Declaration
Bridge Declaration required before editing old canvas-engine files:
  exact old service/API:
  exact old files:
  final DDD owner module:
  idempotency rule:
  removal gate:
  rollback path:
Allowed write scope:
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/template/**
  backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/template/TemplateImportServiceTest.java
  backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/template/**
  backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/template/TemplateDryRunContractTest.java
Forbidden write scope:
  direct database writes outside context adapters
  old canvas-engine final implementation
  any canvas/execution package not listed in allowed write scope
Contracts to read:
  docs/open-source-growth/contracts/template-pack-v1.md
  docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md
Verification commands:
  cd backend && mvn test -pl canvas-context-canvas -Dtest=TemplateImportServiceTest
  cd backend && mvn test -pl canvas-context-execution -Dtest=TemplateDryRunContractTest
  node tools/open-source-growth/guardrail-verifier.mjs
Can run with:
  OSG-W11 CLI if no backend files overlap
Must not run with:
  DDD-W07, DDD-W08, OSG-W10 backend on same canvas/web files
Rollback path:
  revert assigned backend files and tests
```

### OSG-W10: Canvas DSL Backend

```text
Program: Open Source Growth
Task id: OSG-W10
Readiness gate: R5 / G10
Target backend state: DDD_FINAL_MODULE, or CURRENT_ENGINE_BRIDGE only with a
complete Bridge Declaration
Bridge Declaration required before editing old canvas-engine files:
  exact old service/API:
  exact old files:
  final DDD owner module:
  idempotency rule:
  removal gate:
  rollback path:
Allowed write scope:
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/dsl/**
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/dsl/**
  backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslValidatorTest.java
  backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/dsl/CanvasDslMapperTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasDslController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasDslControllerCompatibilityTest.java
Forbidden write scope:
  direct database writes
  final binding to old CanvasService internals
  execution runtime files, unless a separate coordinator packet assigns an
  execution API validation adapter
Contracts to read:
  docs/open-source-growth/contracts/canvas-dsl-v1.md
  docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md
Verification commands:
  cd backend && mvn test -pl canvas-context-canvas -Dtest=CanvasDslValidatorTest,CanvasDslMapperTest
  cd backend && mvn test -pl canvas-web -Dtest=CanvasDslControllerCompatibilityTest
  node tools/open-source-growth/guardrail-verifier.mjs
Can run with:
  OSG-W11 CLI local work after G10 public extension/API stability gate passes
Must not run with:
  DDD-W07, OSG-W09 on same canvas files, DDD-C09 web cutover
Rollback path:
  revert assigned DSL backend/controller files and tests
```

### OSG-W11: CLI Backend API Commands

```text
Program: Open Source Growth
Task id: OSG-W11
Readiness gate: R5 / G10
Target backend state: DOCS_ONLY for backend; CLI code allowed
Allowed write scope:
  tools/canvas-cli/**
  docs/open-source/marketingops-as-code.md if reserved
Forbidden write scope:
  backend/**
  direct database access
Contracts to read:
  docs/open-source-growth/contracts/canvas-dsl-v1.md
  docs/program-coordination/gate-verification-matrix.md
Verification commands:
  cd tools/canvas-cli && npm test
  cd tools/canvas-cli && node src/index.mjs --help
Can run with:
  OSG-W09 or OSG-W10 only after G10 public extension/API stability gate passes
  and tools files are reserved for this worker
Must not run with:
  OSG-W04 if both edit tools/canvas-cli/**
Rollback path:
  revert tools/canvas-cli/** and assigned docs
```

### OSG-W12: AI Journey Backend

```text
Program: Open Source Growth
Task id: OSG-W12
Readiness gate: R5 / G10
Target backend state: DDD_FINAL_MODULE, or CURRENT_ENGINE_BRIDGE only with a
complete Bridge Declaration
Bridge Declaration required before editing old canvas-engine files:
  exact old service/API:
  exact old files:
  final DDD owner module:
  idempotency rule:
  removal gate:
  rollback path:
Allowed write scope:
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/ai/JourneyGenerationService.java
  backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/ai/JourneyGenerationServiceTest.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/ai/JourneyRiskAuditService.java
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/ai/JourneyRiskAuditServiceTest.java
  backend/canvas-context-execution/src/main/java/org/chovy/canvas/execution/api/trace/TraceExplanationFacade.java
  backend/canvas-context-execution/src/test/java/org/chovy/canvas/execution/api/trace/TraceExplanationFacadeTest.java
Forbidden write scope:
  direct published canvas overwrite
  direct execution persistence access outside execution adapter/API
  real provider secret defaults
  any risk decision implementation owned by canvas-context-risk unless a
  separate risk-context worker packet is assigned
Contracts to read:
  docs/open-source-growth/contracts/ai-operator-contract.md
  docs/open-source-growth/contracts/canvas-dsl-v1.md
Verification commands:
  cd backend && mvn test -pl canvas-context-canvas -Dtest=JourneyGenerationServiceTest
  cd backend && mvn test -pl canvas-context-marketing -Dtest=JourneyRiskAuditServiceTest
  cd backend && mvn test -pl canvas-context-execution -Dtest=TraceExplanationFacadeTest
  node tools/open-source-growth/guardrail-verifier.mjs
Can run with:
  OSG-W13 frontend AI assistant if editor integration files are reserved
Must not run with:
  DDD-W07, DDD-W08, OSG-W09, OSG-W10 on overlapping canvas/execution files
Rollback path:
  revert assigned backend AI files and tests
```

### DDD-C09AT: Marketing Monitoring Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AT
Mode: code-writing
Readiness gate: R5 after DDD-C09AS BI dashboard resource runtime route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingMonitoringFacade.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingMonitoringApplicationService.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingMonitoringCatalog.java
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MarketingMonitoringApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingMonitoringController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingMonitoringControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary route:/canvas/marketing-monitoring shows 3 old controllers,
  30 old endpoints, 0 current production controllers, and 0 current production
  endpoints after DDD-C09AS. Legacy sources for this batch are:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitorAnomalyController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringWebhookAdminController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-marketing
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-marketing/** except the exact allowed marketing API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed marketing controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed marketing test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitorAnomalyController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingMonitoringWebhookAdminController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/monitoring/**
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/**
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for all legacy
  /canvas/marketing-monitoring routes through a final marketing context facade,
  application service, in-memory catalog, and production controller. Cover these
  production routes:
  - POST /canvas/marketing-monitoring/sources
  - POST /canvas/marketing-monitoring/items
  - GET /canvas/marketing-monitoring/items
  - GET /canvas/marketing-monitoring/alerts
  - POST /canvas/marketing-monitoring/alerts/{alertId}/resolve
  - POST /canvas/marketing-monitoring/alert-channels
  - POST /canvas/marketing-monitoring/alerts/{alertId}/dispatch
  - GET /canvas/marketing-monitoring/alert-deliveries
  - POST /canvas/marketing-monitoring/sources/{sourceId}/polling
  - POST /canvas/marketing-monitoring/sources/{sourceId}/poll
  - POST /canvas/marketing-monitoring/trends/snapshots/build
  - GET /canvas/marketing-monitoring/trends/snapshots
  - POST /canvas/marketing-monitoring/items/{itemId}/inferences
  - GET /canvas/marketing-monitoring/inferences
  - POST /canvas/marketing-monitoring/provider-credentials
  - GET /canvas/marketing-monitoring/provider-credentials
  - POST /canvas/marketing-monitoring/provider-credentials/{credentialKey}/refresh
  - POST /canvas/marketing-monitoring/provider-credentials/refresh-due
  - POST /canvas/marketing-monitoring/provider-credentials/{credentialKey}/revoke
  - POST /canvas/marketing-monitoring/provider-credentials/{credentialKey}/disable
  - GET /canvas/marketing-monitoring/provider-credentials/events
  - POST /canvas/marketing-monitoring/provider-credentials/oauth/authorizations
  - POST /canvas/marketing-monitoring/provider-credentials/oauth/callback
  - GET /canvas/marketing-monitoring/provider-credentials/oauth/authorizations
  - GET /canvas/marketing-monitoring/provider-credentials/oauth/events
  - POST /canvas/marketing-monitoring/anomaly-rules
  - POST /canvas/marketing-monitoring/anomalies/detect
  - GET /canvas/marketing-monitoring/anomalies
  - POST /canvas/marketing-monitoring/anomalies/{eventId}/resolve
  - POST /canvas/marketing-monitoring/sources/{sourceId}/webhook-secret/rotate

  Preserve compatibility envelope style used by final web controllers:
  success responses use code=0 and message=success with no errorCode or traceId;
  bad requests map to API_001. Missing X-Tenant-Id defaults to 7L and missing
  X-Actor defaults to operator-1. The compact seed may use generic
  Map<String,Object> command/view payloads in the final marketing API to avoid
  recreating legacy domain object graphs, but it must scope state by tenant,
  apply safe limits, return deterministic IDs/statuses, and support the major
  state transitions (resolve alert/event, dispatch, credential refresh/revoke/
  disable, OAuth callback, webhook secret rotate). Do not add old-domain
  dependencies, cross-module dependencies, POM changes, or persistence.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement minimal final
  marketing API/service/controller changes and rerun focused tests plus
  MarketingApiCompatibilityTest.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-marketing,canvas-web -am -Dtest=MarketingMonitoringApplicationServiceTest,MarketingMonitoringControllerCompatibilityTest,MarketingApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.monitoring|MarketingMonitoringController|MarketingMonitorAnomalyController|MarketingMonitoringWebhookAdminController|MarketingMonitoringService|MarketingMonitorAlertFanoutService|MarketingMonitorPollingService|MarketingMonitorInferenceService|MarketingMonitorProviderCredentialService|MarketingMonitorProviderOAuthAuthorizationService|MarketingMonitorAnomalyDetectionService|MarketingMonitorWebhookIngestionService" backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingMonitoringController.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingMonitoringFacade.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingMonitoringApplicationService.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingMonitoringCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-marketing/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AT reserved marketing API/domain/application/controller
  and marketing test files listed in this packet
```

### DDD-C09AU: Growth Activities Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AU
Mode: code-writing
Readiness gate: R5 after DDD-C09AT Marketing Monitoring route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/GrowthActivityFacade.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/GrowthActivityApplicationService.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/GrowthActivityCatalog.java
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/GrowthActivityApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/GrowthActivityController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/GrowthActivityControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary route:/canvas/growth-activities shows 1 old controller,
  25 old endpoints, 0 current production controllers, and 0 current production
  endpoints after DDD-C09AT. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/GrowthActivityController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-marketing
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-marketing/** except the exact allowed marketing API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed marketing controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed marketing test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/GrowthActivityController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/**
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/**
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for all legacy
  /canvas/growth-activities routes through a final marketing context facade,
  application service, in-memory catalog, and production controller. Cover these
  production routes:
  - POST /canvas/growth-activities
  - GET /canvas/growth-activities
  - GET /canvas/growth-activities/{activityId}
  - GET /canvas/growth-activities/{activityId}/report
  - GET /canvas/growth-activities/{activityId}/readiness
  - GET /canvas/growth-activities/{activityId}/reward-pools
  - POST /canvas/growth-activities/{activityId}/reward-pools
  - GET /canvas/growth-activities/{activityId}/grants
  - POST /canvas/growth-activities/{activityId}/grants
  - POST /canvas/growth-activities/{activityId}/grants/{grantId}/retry
  - POST /canvas/growth-activities/{activityId}/grants/{grantId}/reconcile
  - POST /canvas/growth-activities/{activityId}/grants/{grantId}/cancel
  - GET /canvas/growth-activities/{activityId}/referral-codes
  - POST /canvas/growth-activities/{activityId}/referral-codes
  - GET /canvas/growth-activities/{activityId}/referrals
  - POST /canvas/growth-activities/{activityId}/referrals
  - POST /canvas/growth-activities/{activityId}/referrals/{relationId}/qualify
  - GET /canvas/growth-activities/{activityId}/tasks
  - POST /canvas/growth-activities/{activityId}/tasks
  - GET /canvas/growth-activities/{activityId}/task-progress
  - POST /canvas/growth-activities/{activityId}/task-progress
  - POST /canvas/growth-activities/{activityId}/task-progress/{progressId}/reset
  - POST /canvas/growth-activities/{activityId}/publish
  - POST /canvas/growth-activities/{activityId}/pause
  - POST /canvas/growth-activities/{activityId}/close

  Preserve compatibility envelope style used by final web controllers:
  success responses use code=0 and message=success with no errorCode or traceId;
  bad requests map to API_001. Missing X-Tenant-Id defaults to 7L and missing
  X-Actor defaults to operator-1. The legacy controller's internal fallback was
  tenant 0/system through TenantContextResolver, but final canvas-web
  compatibility controllers use header defaults; follow final controller style.

  The compact seed may use generic Map<String,Object> command/view payloads in
  the final marketing API to avoid recreating legacy domain object graphs, but
  it must scope state by tenant, apply safe limits, return deterministic
  IDs/statuses, and support the major state transitions:
  publish/pause/close activity, grant retry/reconcile/cancel, referral qualify,
  task progress reset, reward pool/task/referral/grant creation. Some legacy
  path params are compatibility-only at controller level: activityId is not
  passed to old service calls for grant retry/reconcile/cancel, referral
  qualify, or task progress reset. The final seed may include it in payloads for
  traceability but should transition by grantId/relationId/progressId.

  Do not add old-domain dependencies, cross-module dependencies, POM changes, or
  persistence.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement minimal final
  marketing API/service/controller changes and rerun focused tests plus
  MarketingApiCompatibilityTest.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-marketing,canvas-web -am -Dtest=GrowthActivityApplicationServiceTest,GrowthActivityControllerCompatibilityTest,MarketingApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.marketing|GrowthActivityController|GrowthActivityService|GrowthActivityReadinessService|GrowthActivityReportService|GrowthRewardPoolService|GrowthRewardGrantService|GrowthReferralService|GrowthTaskService|TenantContextResolver" backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/GrowthActivityController.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/GrowthActivityFacade.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/GrowthActivityApplicationService.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/GrowthActivityCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-marketing/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AU reserved growth activity API/domain/application/controller
  and marketing test files listed in this packet
```

### DDD-C09AV: Search Marketing Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AV
Mode: code-writing
Readiness gate: R5 after DDD-C09AU Growth Activities route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/SearchMarketingFacade.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/SearchMarketingApplicationService.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/SearchMarketingCatalog.java
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/SearchMarketingApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/SearchMarketingController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/SearchMarketingControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary route:/canvas/search-marketing shows 1 old controller,
  24 old endpoints, 0 current production controllers, and 0 current production
  endpoints after DDD-C09AU. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/SearchMarketingController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-marketing
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-marketing/** except the exact allowed marketing API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed marketing controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed marketing test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/SearchMarketingController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/**
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/**
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for all legacy
  /canvas/search-marketing routes through a final marketing context facade,
  application service, in-memory catalog, and production controller. Cover
  these production routes:
  - GET /canvas/search-marketing/sources
  - POST /canvas/search-marketing/sources
  - GET /canvas/search-marketing/keywords
  - POST /canvas/search-marketing/keywords
  - GET /canvas/search-marketing/snapshots
  - POST /canvas/search-marketing/snapshots
  - GET /canvas/search-marketing/opportunities
  - POST /canvas/search-marketing/opportunities/evaluate
  - POST /canvas/search-marketing/opportunities/{opportunityId}/status
  - POST /canvas/search-marketing/opportunities/{opportunityId}/mutations
  - GET /canvas/search-marketing/mutations
  - POST /canvas/search-marketing/mutations
  - POST /canvas/search-marketing/mutations/{mutationId}/approve
  - POST /canvas/search-marketing/mutations/{mutationId}/execute
  - GET /canvas/search-marketing/url-inspections
  - GET /canvas/search-marketing/sync-runs
  - POST /canvas/search-marketing/sources/{sourceId}/sync
  - POST /canvas/search-marketing/sources/sync-due
  - GET /canvas/search-marketing/provider-changes
  - POST /canvas/search-marketing/mutations/{mutationId}/reconcile
  - GET /canvas/search-marketing/impact-windows
  - POST /canvas/search-marketing/impact-windows/evaluate-due
  - GET /canvas/search-marketing/readiness
  - GET /canvas/search-marketing/summary

  Preserve compatibility envelope style used by final web controllers:
  success responses use code=0 and message=success with no errorCode or traceId;
  bad requests map to API_001. Missing X-Tenant-Id defaults to 7L and missing
  X-Actor defaults to operator-1. Query date params use ISO yyyy-MM-dd for
  startDate/endDate. Manual sync runType defaults to PERFORMANCE. sync-due and
  evaluate-due default limit to 50.

  The compact seed may use generic Map<String,Object> command/view payloads in
  the final marketing API to avoid recreating legacy domain object graphs, but
  it must scope state by tenant, apply safe limits, return deterministic IDs and
  statuses, and support source/keyword/snapshot/mutation/opportunity/sync/
  provider-change/impact-window/readiness/summary flows well enough for route
  compatibility tests.

  Do not add old-domain dependencies, cross-module dependencies, POM changes, or
  persistence.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement minimal final
  marketing API/service/controller changes and rerun focused tests plus
  MarketingApiCompatibilityTest.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-marketing,canvas-web -am -Dtest=SearchMarketingApplicationServiceTest,SearchMarketingControllerCompatibilityTest,MarketingApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.marketing|SearchMarketingService|SearchMarketingMutationService|SearchMarketingSyncRunService|SearchMarketingReadinessService|SearchMarketingReconciliationService|SearchMarketingImpactWindowService|TenantContextResolver" backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/SearchMarketingController.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/SearchMarketingFacade.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/SearchMarketingApplicationService.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/SearchMarketingCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-marketing/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AV reserved search marketing API/domain/application/controller
  and marketing test files listed in this packet
```

### DDD-C09AW: AI Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AW
Mode: code-writing
Readiness gate: R5 after DDD-C09AV Search Marketing route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/AiFacade.java
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/AiApplicationService.java
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AiCatalog.java
  backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/AiApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/ai/AiController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/ai/AiControllerCompatibilityTest.java
Inventory rows required:
  routeGapSummary route:/ai shows 4 old controllers, 23 old endpoints, 0
  current production controllers, and 0 current production endpoints after
  DDD-C09AV. Legacy sources for this batch are:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiDecisionController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPredictionController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiProviderController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-platform
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-platform/** except the exact allowed platform API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed AI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed AI test file
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiDecisionController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPredictionController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiPromptTemplateController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiProviderController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/**
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add compact final-module compatibility for all legacy /ai routes through a
  final platform facade, application service, in-memory catalog, and production
  controller. Cover these production routes:
  - POST /ai/decisions/recompute
  - GET /ai/decisions/latest-run
  - GET /ai/decisions/recommendations
  - POST /ai/decisions/recommendations/{recommendationId}/feedback
  - GET /ai/predictions/latest-run
  - GET /ai/predictions/readiness
  - GET /ai/predictions/churn-distribution
  - GET /ai/predictions/top-risk-users
  - POST /ai/predictions/recompute
  - GET /ai/prompt-templates
  - POST /ai/prompt-templates
  - GET /ai/prompt-templates/{id}
  - PUT /ai/prompt-templates/{id}
  - POST /ai/prompt-templates/{id}/disable
  - POST /ai/prompt-templates/render
  - POST /ai/prompt-templates/evaluate
  - GET /ai/prompt-templates/evaluation-audits
  - GET /ai/providers
  - POST /ai/providers
  - GET /ai/providers/{id}
  - PUT /ai/providers/{id}
  - POST /ai/providers/{id}/disable
  - GET /ai/providers/{id}/models

  Preserve compatibility envelope style used by final web controllers:
  success responses use code=0 and message=success with no errorCode or traceId;
  bad requests map to API_001. Missing X-Tenant-Id defaults to 7L and missing
  X-Actor defaults to operator-1. The legacy controllers use admin context
  fallbacks when TenantContextResolver is absent; the final compact controller
  should use explicit final header defaults.

  The compact seed may use generic Map<String,Object> command/view payloads in
  the final platform API to avoid recreating legacy domain object graphs, but it
  must scope state by tenant, apply safe limits, return deterministic IDs and
  statuses, and support decision run/recommendation/feedback, prediction
  readiness/risk distribution/top users/recompute, prompt template CRUD/render/
  evaluate/audits, and provider CRUD/disable/models flows.

  Do not add old-domain dependencies, cross-module dependencies, POM changes, or
  persistence.

  Follow TDD: first add focused failing tests for service behavior and
  controller compatibility, run them RED, then implement minimal final
  platform API/service/controller changes and rerun focused tests.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-platform,canvas-web -am -Dtest=AiApplicationServiceTest,AiControllerCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.ai|TenantContextResolver|AiDecisionModelService|ChurnPredictionService|AiPromptTemplateService|AiPromptEvaluationService|AiProviderModelRegistryService" backend/canvas-web/src/main/java/org/chovy/canvas/web/ai/AiController.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/AiFacade.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/AiApplicationService.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AiCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-platform/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/ai/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AW reserved AI API/domain/application/controller
  and test files listed in this packet
```

### DDD-C09AX: Marketing Content Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AX
Mode: code-writing
Readiness gate: R5 after DDD-C09AW AI route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingContentFacade.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingContentApplicationService.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingContentCatalog.java
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MarketingContentApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingContentController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingContentControllerCompatibilityTest.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
Inventory rows required:
  routeGapSummary after DDD-C09AW shows route:/marketing with 1 old controller,
  21 old endpoints, 0 current production controllers, and 0 current production
  endpoints. Legacy source for this batch is:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingContentController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-marketing
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-marketing/** except the exact allowed marketing content API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed MarketingContentController file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed marketing content test files
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingContentController.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/**
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/**
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/MarketingApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add a compact final-module marketing content route batch without depending
  on old canvas-engine services. Cover these production route shapes:
  - GET /marketing/content/asset-folders
  - POST /marketing/content/asset-folders
  - GET /marketing/content/assets
  - POST /marketing/content/assets
  - POST /marketing/content/assets/upload-intents
  - POST /marketing/content/assets/upload-intents/expire-stale
  - POST /marketing/content/assets/{assetKey}/status
  - GET /marketing/content/templates
  - POST /marketing/content/templates
  - POST /marketing/content/templates/{templateKey}/preview
  - POST /marketing/content/templates/{templateKey}/status
  - GET /marketing/content/entries
  - POST /marketing/content/entries
  - POST /marketing/content/entries/{entryKey}/publish
  - POST /marketing/content/entries/{entryKey}/archive
  - POST /marketing/content/releases/validate
  - POST /marketing/content/releases/publish
  - GET /marketing/content/releases
  - POST /marketing/content/releases/{releaseKey}/resolve
  - POST /marketing/content/releases/{releaseKey}/rollback
  - GET /marketing/content/audit-events

  Preserve the stable compatibility envelope used by final web controllers:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  operator-1. Bad request validation errors should map to API_001. The compact
  seed may use in-memory final marketing content state scoped by tenant. It
  should model folders, assets, upload intents, templates, entries, releases,
  release validation/resolve/rollback, and audit events with deterministic
  keys and ordering. Do not add persistence, old-domain dependencies,
  cross-module dependencies, POM changes, or old TenantContextResolver usage.

  Follow TDD: first add focused failing tests for application service behavior
  and controller compatibility, run them RED, then implement minimal final
  marketing API/service/catalog/controller changes and rerun focused tests plus
  MarketingApiCompatibilityTest.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-marketing,canvas-web -am -Dtest=MarketingContentApplicationServiceTest,MarketingContentControllerCompatibilityTest,MarketingApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.domain\\.content|TenantContextResolver|MarketingAssetService|ContentTemplateService|ContentEntryService|MarketingAssetUploadService|MarketingContentReleaseService" backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingContentController.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingContentFacade.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingContentApplicationService.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingContentCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-marketing/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AX reserved marketing content API/domain/application/controller
  and test files listed in this packet
```

### DDD-C09AY: Admin Platform Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AY
Mode: code-writing
Readiness gate: R5 after DDD-C09AX Marketing Content route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/AdminPlatformFacade.java
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/AdminPlatformApplicationService.java
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AdminPlatformCatalog.java
  backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/AdminPlatformApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/admin/AdminPlatformController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/admin/AdminPlatformControllerCompatibilityTest.java
Inventory rows required:
  routeGapSummary after DDD-C09AX shows route:/admin with 4 old controllers,
  21 old endpoints, 0 current production controllers, and 0 current production
  endpoints. Legacy sources for this batch are:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/AdminController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasProjectController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/SystemOptionController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/TenantController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-platform
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-platform/** except the exact allowed admin platform API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed admin controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed admin controller test file
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AdminController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasProjectController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/SystemOptionController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/TenantController.java
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add a compact final-module admin platform route batch without depending on
  old canvas-engine services. Cover these production route shapes:
  - GET /admin/users
  - POST /admin/users
  - PUT /admin/users/{id}
  - PUT /admin/users/{id}/disable
  - GET /admin/projects
  - POST /admin/projects
  - GET /admin/projects/{projectId}
  - PUT /admin/projects/{projectId}
  - PUT /admin/projects/{projectId}/disable
  - GET /admin/projects/{projectId}/members
  - PUT /admin/projects/{projectId}/members/{userId}
  - DELETE /admin/projects/{projectId}/members/{userId}
  - GET /admin/projects/{projectId}/canvases
  - GET /admin/projects/{projectId}/stats
  - GET /admin/system-options
  - PUT /admin/system-options/{id}
  - GET /admin/tenants
  - POST /admin/tenants
  - PUT /admin/tenants/{id}/disable
  - PUT /admin/tenants/{id}/activate
  - GET /admin/tenants/{id}/usage

  Preserve the stable compatibility envelope used by final web controllers:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  operator-1. Bad request validation errors should map to API_001. The compact
  seed may use in-memory final platform state scoped by tenant where relevant.
  It should model users, projects, project members, project canvases/stats,
  system options, tenants, tenant activation/disable, and tenant usage with
  deterministic ids and ordering. Do not add persistence, old-domain
  dependencies, cross-module dependencies, POM changes, or old
  TenantContextResolver usage.

  Follow TDD: first add focused failing tests for application service behavior
  and controller compatibility, run them RED, then implement minimal final
  platform API/service/catalog/controller changes and rerun focused tests.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-platform,canvas-web -am -Dtest=AdminPlatformApplicationServiceTest,AdminPlatformControllerCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(auth|domain|dto|query|dal)|TenantContextResolver|SysUserService|CanvasProjectService|CanvasProjectPermissionService|SystemOptionService|TenantService|AuditEventService" backend/canvas-web/src/main/java/org/chovy/canvas/web/admin/AdminPlatformController.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/AdminPlatformFacade.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/AdminPlatformApplicationService.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/AdminPlatformCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-platform/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/admin/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AY reserved admin platform API/domain/application/controller
  and test files listed in this packet
```

### OSG-W13: Frontend AI Assistant

```text
Program: Open Source Growth
Task id: OSG-W13
Readiness gate: R0 for mock preview, R5 for live API integration
Target backend state: DOCS_ONLY for backend; frontend code allowed
Allowed write scope:
  frontend/src/pages/canvas-editor/AiJourneyAssistant.tsx
  frontend/src/pages/canvas-editor/aiJourneyAssistant.test.tsx
  optional editor integration file only when the handoff names the exact path,
  such as frontend/src/pages/canvas-editor/index.tsx or an editor store file
Forbidden write scope:
  backend/**
  frontend/src/App.tsx unless coordinator reserves it
Contracts to read:
  docs/open-source-growth/contracts/ai-operator-contract.md
  docs/open-source-growth/contracts/canvas-dsl-v1.md
Verification commands:
  cd frontend && npm run test -- --run aiJourneyAssistant
  cd frontend && npm run build
Can run with:
  OSG-W12 if frontend editor files are reserved for this worker
Must not run with:
  another worker editing canvas-editor/index.tsx or shared editor store files
Rollback path:
  revert assigned frontend AI files
```

### DDD-C09AZ: Warehouse Realtime Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09AZ
Mode: code-writing
Readiness gate: R5 after DDD-C09AY Admin Platform route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseRealtimeFacade.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseRealtimeApplicationService.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseRealtimeCatalog.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseRealtimeApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseRealtimeController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseRealtimeControllerCompatibilityTest.java
Inventory rows required:
  routeGapSummary after DDD-C09AY shows route:/warehouse/realtime with
  8 old controllers, 21 old endpoints, 1 current production controller, and
  1 current production endpoint. The existing final endpoint is
  GET /warehouse/realtime/cutover-readiness and must not be duplicated.
  Legacy sources for this batch are:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeSchemaController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimePipelineController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeJobIncidentController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimeJobController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtimePipelineIncidentController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseExternalRealtimeJobProbeController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-cdp
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-cdp/** except the exact allowed warehouse realtime API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed cdp controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed cdp controller test file
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseRealtime*.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseExternalRealtimeJobProbeController.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/**
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add a compact final-module warehouse realtime route batch without depending
  on old canvas-engine services. Cover these production route shapes:
  - GET /warehouse/realtime/status
  - POST /warehouse/realtime/schemas
  - GET /warehouse/realtime/schemas
  - GET /warehouse/realtime/schemas/latest
  - GET /warehouse/realtime/pipelines/contracts
  - POST /warehouse/realtime/pipelines/contracts
  - POST /warehouse/realtime/pipelines/checkpoints
  - GET /warehouse/realtime/pipelines/status
  - POST /warehouse/realtime/jobs/incidents/scan
  - POST /warehouse/realtime/jobs/heartbeats
  - GET /warehouse/realtime/jobs/status
  - POST /warehouse/realtime/jobs/actions
  - GET /warehouse/realtime/jobs/actions/pending
  - POST /warehouse/realtime/jobs/actions/{actionId}/ack
  - POST /warehouse/realtime/jobs/actions/{actionId}/complete
  - POST /warehouse/realtime/pipelines/incidents/scan
  - POST /warehouse/realtime/job-probes/targets
  - GET /warehouse/realtime/job-probes/targets
  - POST /warehouse/realtime/job-probes/targets/{targetId}/enabled
  - POST /warehouse/realtime/job-probes/scan

  Preserve the stable compatibility envelope used by final web controllers:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  operator-1. Bad request validation errors should map to API_001. The compact
  seed may use in-memory final CDP state scoped by tenant. It should model
  realtime status, schema registration/list/latest, pipeline contracts,
  checkpoints, pipeline and job status, job incidents/actions, pipeline
  incidents, external job probe targets, enable toggles, and scans with
  deterministic ids and ordering. Do not add persistence, old-domain
  dependencies, cross-module dependencies, POM changes, old TenantContextResolver
  usage, or a duplicate /warehouse/realtime/cutover-readiness mapping.

  Follow TDD: first add focused failing tests for application service behavior
  and controller compatibility, run them RED, then implement minimal final CDP
  API/service/catalog/controller changes and rerun focused tests.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-cdp,canvas-web -am -Dtest=CdpWarehouseRealtimeApplicationServiceTest,CdpWarehouseRealtimeControllerCompatibilityTest,CdpApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal)|TenantContextResolver|CdpWarehouseRealtimeCheckpointService|CdpWarehouseRealtimeSchemaService|CdpWarehouseRealtimePipelineService|CdpWarehouseRealtimeJobControlService|CdpWarehouseRealtimeJobIncidentService|CdpWarehouseRealtimePipelineIncidentService|CdpWarehouseExternalRealtimeJobProbeService" backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseRealtimeController.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseRealtimeFacade.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseRealtimeApplicationService.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseRealtimeCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-cdp/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09AZ reserved warehouse realtime API/domain/application/controller
  and test files listed in this packet
```

### DDD-C09BA: Risk Governance Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09BA
Mode: code-writing
Readiness gate: R5 after DDD-C09AZ Warehouse Realtime route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskGovernanceFacade.java
  backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/application/RiskGovernanceApplicationService.java
  backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/domain/RiskGovernanceCatalog.java
  backend/canvas-context-risk/src/test/java/org/chovy/canvas/risk/application/RiskGovernanceApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/RiskGovernanceController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/risk/RiskGovernanceControllerCompatibilityTest.java
Inventory rows required:
  routeGapSummary after DDD-C09AZ shows route:/canvas/risk with 5 old
  controllers, 23 old endpoints, 4 current production controllers, and
  4 current production endpoints. Existing final endpoints are:
  - POST /canvas/risk/decisions/evaluate
  - GET /canvas/risk/lists
  - GET /canvas/risk/scenes
  - GET /canvas/risk/strategies
  This batch must not duplicate those mappings. Legacy sources for this batch are:
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskDecisionController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskListController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskStrategyController.java
  - backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskLabController.java
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-risk
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-risk/** except the exact allowed risk governance API/domain/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed risk governance controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed risk governance controller test file
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskDecisionController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskListController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskStrategyController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/RiskLabController.java
  backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/risk/**
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add a compact final-module risk governance route batch without depending on
  old canvas-engine services. Cover these production route shapes:
  - GET /canvas/risk/decisions/traces
  - POST /canvas/risk/lists
  - POST /canvas/risk/lists/{listKey}/entries
  - GET /canvas/risk/lists/{listKey}/entries
  - DELETE /canvas/risk/lists/{listKey}/entries/{entryId}
  - POST /canvas/risk/lists/{listKey}/entries/import
  - POST /canvas/risk/strategies
  - GET /canvas/risk/strategies/{strategyKey}
  - GET /canvas/risk/strategies/{strategyKey}/versions
  - POST /canvas/risk/strategies/{strategyKey}/versions/{version}/validate
  - POST /canvas/risk/strategies/{strategyKey}/versions/{version}/simulate
  - POST /canvas/risk/strategies/{strategyKey}/versions/{version}/submit
  - POST /canvas/risk/strategies/{strategyKey}/versions/{version}/approve
  - POST /canvas/risk/strategies/{strategyKey}/versions/{version}/activate
  - POST /canvas/risk/strategies/{strategyKey}/rollback
  - POST /canvas/risk/strategies/{strategyKey}/pause
  - GET /canvas/risk/strategies/{strategyKey}/versions/{left}/diff/{right}
  - POST /canvas/risk/lab/simulations
  - GET /canvas/risk/lab/simulations

  Preserve the stable compatibility envelope used by final web controllers:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  operator-1. Bad request validation errors should map to API_001. The compact
  seed may use in-memory final risk state scoped by tenant. It should model
  decision traces, lists, list entries, list import, strategy drafts,
  strategy versions, validation/simulation/submit/approve/activate/rollback/
  pause/diff, and lab simulations with deterministic ids and ordering. Do not
  add persistence, old-domain dependencies, cross-module dependencies, POM
  changes, old TenantContextResolver usage, or duplicate any existing final
  risk route mapping.

  Follow TDD: first add focused failing tests for application service behavior
  and controller compatibility, run them RED, then implement minimal final risk
  API/service/catalog/controller changes and rerun focused tests.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-risk,canvas-web -am -Dtest=RiskGovernanceApplicationServiceTest,RiskGovernanceControllerCompatibilityTest,RiskApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal)|TenantContextResolver|RiskListService|RiskStrategyService|RiskSimulationService|RiskDecisionTraceReader" backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/RiskGovernanceController.java backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/api/RiskGovernanceFacade.java backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/application/RiskGovernanceApplicationService.java backend/canvas-context-risk/src/main/java/org/chovy/canvas/risk/domain/RiskGovernanceCatalog.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-risk/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/risk/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09BA reserved risk governance API/domain/application/controller
  and test files listed in this packet
```

### DDD-C09BB: Canvas Route Batch

```text
Program: DDD modular rewrite
Task id: DDD-C09BB
Mode: code-writing
Readiness gate: R5 after DDD-C09BA Risk Governance route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CanvasCompatibilityFacade.java
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CanvasCompatibilityApplicationService.java
  backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/CanvasCompatibilityApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasControllerCompatibilityTest.java
Inventory rows required:
  routeGapSummary after DDD-C09BA shows family:Canvas with one old
  controller, 24 old endpoints, one current production controller, and
  6 current production endpoints. Existing final Canvas endpoints are:
  - GET /canvas/{id}/versions
  - GET /canvas/{id}/versions/{versionId}
  - POST /canvas/{id}/publish
  - POST /canvas/{id}/offline
  - POST /canvas/{id}/archive
  - POST /canvas/{id}/kill
  Existing separate final controllers also cover:
  - /canvas/dsl/** routes
  - GET/PUT /canvas/{id}/project-folder-metadata
  This batch must not duplicate those mappings.
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-canvas
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-canvas/** except the exact allowed Canvas compatibility API/application/test files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed Canvas controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed Canvas controller test file
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/CanvasApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add a compact final-module Canvas compatibility route batch without depending
  on old canvas-engine services. Cover these missing production route shapes:
  - POST /canvas
  - GET /canvas/{id}
  - PUT /canvas/{id}
  - GET /canvas/list
  - POST /canvas/{id}/submit-review
  - GET /canvas/{id}/approval-status
  - GET /canvas/{id}/pre-publish-checks
  - POST /canvas/{id}/revert/{versionId}
  - POST /canvas/{id}/canary
  - POST /canvas/{id}/promote-canary
  - POST /canvas/{id}/rollback-canary
  - POST /canvas/{id}/rollback
  - POST /canvas/{id}/clone
  - GET /canvas/{id}/versions/{v1}/diff/{v2}
  - PUT /canvas/{id}/safe
  - POST /canvas/{id}/message-preview
  - GET /canvas/{id}/export
  - POST /canvas/import

  Preserve the stable compatibility envelope used by final CanvasController:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  operator-1. Bad request validation errors should map to API_001. The compact
  seed may use in-memory final Canvas state scoped by tenant. It should model
  create/get/update/list, review status, pre-publish checks, clone, revert,
  canary/promote/rollback, safe update conflict, message preview, export, and
  import with deterministic ids and ordering. Do not add persistence, old-domain
  dependencies, cross-module dependencies, POM changes, old TenantContextResolver
  usage, or duplicate any existing final Canvas route mapping.

  Follow TDD: first add focused failing tests for application service behavior
  and controller compatibility, run them RED, then implement minimal final
  Canvas compatibility API/service/controller changes and rerun focused tests.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn clean test -pl canvas-context-canvas,canvas-web -am -Dtest=CanvasCompatibilityApplicationServiceTest,CanvasControllerCompatibilityTest,CanvasApiCompatibilityTest test
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal)|TenantContextResolver|CanvasService|CanvasOpsService|CanvasPublishApprovalService|CanvasPrePublishCheckService|CanvasMessagePreviewService|CanvasImportExportService" backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasController.java backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CanvasCompatibilityFacade.java backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CanvasCompatibilityApplicationService.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-canvas/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09BB reserved Canvas compatibility API/application/controller
  and test files listed in this packet
```

### DDD-C09BC: BI Remaining Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BC
Mode: code-writing
Readiness gate: R5 after DDD-C09BB Canvas route closeout
Target backend state: DDD_FINAL_MODULE
Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/BiCatalogControllerCompatibilityTest.java
Inventory rows required:
  routeGapSummary after DDD-C09BB shows route:/canvas/bi with 20 old
  controllers, 169 old endpoints, 1 current production controller, and
  152 current production endpoints. The current final BI controller already
  covers most query, dashboard, chart, spreadsheet, resource collaboration,
  datasource operations, subscription/delivery, permission, self-service,
  and embed ticket routes. This batch should not duplicate existing mappings.
Allowed module POM edits:
  none; backend/canvas-web already depends on canvas-context-bi
Forbidden write scope:
  backend/canvas-engine/**
  backend/canvas-context-bi/** except the exact allowed BI facade/application files
  backend/canvas-web/src/main/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller file
  backend/canvas-web/src/test/java/org/chovy/canvas/web/**/*.java except the exact allowed BI controller test file
  backend/pom.xml
  frontend/**
  docs/** except worker return evidence if explicitly instructed by coordinator
Read scope:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/**
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/**
  backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/bi/**
  backend/canvas-web/src/test/java/org/chovy/canvas/web/compat/BiApiCompatibilityTest.java
Contracts to read:
  docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md
  docs/ddd-rewrite/contract-tests/compatibility-test-plan.md
Goal:
  Add a compact final-module BI route alias batch without depending on old
  canvas-engine services. Prefer delegating to existing BiCatalogFacade methods
  and compact deterministic compatibility responses. Cover remaining old route
  shapes that are not already mapped, especially:
  - POST /canvas/bi/datasets/resources
  - DELETE /canvas/bi/datasets/resources
  - POST /canvas/bi/datasets/resources/from-datasource-schema
  - POST /canvas/bi/datasets/resources/from-datasource-schema/multi-table
  - POST /canvas/bi/datasets/resources/sql-preview
  - POST /canvas/bi/datasets/resources/acceleration-scheduler/run
  - POST /canvas/bi/charts/resources
  - DELETE /canvas/bi/charts/resources
  - POST /canvas/bi/dashboards/resources
  - DELETE /canvas/bi/dashboards/resources
  - POST /canvas/bi/portals/resources
  - DELETE /canvas/bi/portals/resources
  - GET /canvas/bi/portals/runtime
  - GET /canvas/bi/portals/runtime/{portalKey}
  - POST /canvas/bi/big-screens/resources
  - DELETE /canvas/bi/big-screens/resources
  - POST /canvas/bi/spreadsheets/resources
  - DELETE /canvas/bi/spreadsheets/resources
  - GET /canvas/bi/datasources
  - POST /canvas/bi/datasources
  - PUT /canvas/bi/datasources
  - POST /canvas/bi/embed/resources/dashboard
  - POST /canvas/bi/embed/resources/dashboard/runtime-state
  - POST /canvas/bi/embed/resources/portal
  - POST /canvas/bi/self-service
  - GET /canvas/bi/self-service

  Preserve the stable compatibility envelope used by final web controllers:
  success responses use code=0 and message=success with no errorCode or
  traceId; missing X-Tenant-Id defaults to 7L; missing X-Actor defaults to
  analyst. Bad request validation errors should map to API_001. Do not add
  persistence, old-domain dependencies, cross-module dependencies, POM changes,
  old TenantContextResolver usage, or duplicate an existing final BI route.

  Follow TDD: first add focused failing controller compatibility tests for the
  new alias routes, run them RED, then implement minimal facade/application/
  controller changes and rerun focused tests.
Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-bi -Dtest=BiCatalogApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal)|TenantContextResolver|BiDatasetResourceService|BiDatasourceOnboardingService|BiEmbedTicketService|BiPortalRuntimeService|BiSelfServiceExportService" backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/BiCatalogController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/BiCatalogFacade.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/BiCatalogApplicationService.java
Can run with:
  read-only reviewers and no other workers editing backend/canvas-context-bi/**
  or backend/canvas-web/src/main/java/org/chovy/canvas/web/bi/**
Must not run with:
  DDD-C09 or any worker editing backend/**
Rollback path:
  revert only the exact DDD-C09BC reserved BI facade/application/controller/test
  files listed in this packet
```

### OSG-W14: Playground Flow

```text
Program: Open Source Growth
Task id: OSG-W14
Readiness gate: R5 for live flow, R0 for docs-only narrative
Target backend state: DOCS_ONLY unless coordinator assigns live integration
Allowed write scope:
  docs/open-source/playground.md
  frontend/src/pages/home/** files named exactly in the coordinator handoff
  frontend/src/pages/canvas-list/** files named exactly in the coordinator handoff
  frontend/src/pages/canvas-editor/** files named exactly in the coordinator handoff
Forbidden write scope:
  backend seed/runtime changes unless declared CURRENT_ENGINE_BRIDGE
  production profile or secret defaults
Contracts to read:
  docs/open-source-growth/contracts/demo-profile-contract.md
  docs/open-source-growth/contracts/template-pack-v1.md
  docs/open-source-growth/contracts/canvas-dsl-v1.md
  docs/open-source-growth/contracts/ai-operator-contract.md
Verification commands:
  docker compose -f docker-compose.demo.yml config
  cd frontend && npm run build
  node tools/open-source-growth/guardrail-verifier.mjs
Can run with:
  English docs/release workers if file scopes are separate
Must not run with:
  DDD-C09, OSG-W02, or any worker editing the same home/list/editor files
Rollback path:
  revert assigned playground docs/frontend files
```

### DDD-C09BD: Conversation Remaining Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BD
Dispatch id: dispatch-DDD-C09BD-conversation-remaining-routes-20260614-074000
Readiness gate: R5 after DDD-C09BC closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Allowed write scope:
  backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/api/ConversationFacade.java
  backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/application/ConversationApplicationService.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/conversation/ConversationController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/conversation/ConversationControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BD-conversation-remaining-routes-20260614-074000/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the four allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationWorkspaceController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationPrivateDomainController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ConversationProviderWebhookController.java

Goal:
  Add final-module compatibility coverage for the remaining /canvas/conversations
  route aliases so preflight moves the route:/canvas/conversations gap forward.
  Preserve the compatibility envelope used by ConversationController:
  code=0/message=success on success, API_001 for bad requests, default
  X-Tenant-Id=7L, default X-Actor=operator-1.

Target routes:
  POST /canvas/conversations/adapters/{adapterKey}/ingress
  GET /canvas/conversations
  GET /canvas/conversations/{sessionId}/messages
  GET /canvas/conversations/workspace/inbox
  POST /canvas/conversations/workspace/work-items/{workItemId}/tasks
  POST /canvas/conversations/workspace/tasks/{taskId}/complete
  GET /canvas/conversations/workspace/work-items/{workItemId}/timeline
  POST /canvas/conversations/workspace/sla-breaches/evaluate
  GET /canvas/conversations/workspace/sla-breaches
  POST /canvas/conversations/workspace/work-items/{workItemId}/ai-reply-suggestions/generate
  POST /canvas/conversations/workspace/work-items/{workItemId}/ai-reply-suggestions/{suggestionId}/review
  GET /canvas/conversations/workspace/work-items/{workItemId}/ai-reply-suggestions
  POST /canvas/conversations/private-domain/sync-runs
  GET /canvas/conversations/private-domain/contacts
  GET /canvas/conversations/private-domain/groups
  GET /canvas/conversations/private-domain/sync-runs
  POST /canvas/conversations/provider-webhooks/whatsapp

Implementation guidance:
  Prefer compact final-module facade methods returning Map/List structures over
  adding many new DTO files. Keep behavior deterministic and compatibility
  focused. Do not import old org.chovy.canvas.domain/dto/query/dal packages,
  TenantContextResolver, or old services. You are not alone in the worktree:
  preserve unrelated dirty changes and do not revert others' edits.

TDD:
  First add focused RED tests in ConversationControllerCompatibilityTest for
  the missing aliases. Run the focused Maven command and capture expected
  failure. Then implement minimal controller/facade/application behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-conversation -Dtest=ConversationApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ConversationControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal)|TenantContextResolver|ConversationIngressService|ConversationWorkspaceService|ConversationRoutingService|ConversationAiReplyService|ConversationPrivateDomainSyncService|ConversationAdapterHarness|WhatsAppWebhookPayloadMapper" backend/canvas-web/src/main/java/org/chovy/canvas/web/conversation/ConversationController.java backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/api/ConversationFacade.java backend/canvas-context-conversation/src/main/java/org/chovy/canvas/conversation/application/ConversationApplicationService.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BN: Webhooks Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BN
Dispatch id: dispatch-DDD-C09BN-webhooks-routes-20260614-104200
Readiness gate: R5 after DDD-C09BM closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/cdp/webhooks
  oldControllerCount=1
  oldEndpointCount=9
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java
  ]

Allowed write scope:
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWebhookFacade.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWebhookApplicationService.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWebhookCatalog.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWebhookApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWebhookController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWebhookControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BN-webhooks-routes-20260614-104200/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java

Goal:
  Add compact final-module compatibility coverage for route:/cdp/webhooks
  without depending on old canvas-engine services. Preserve final web
  CompatibilityEnvelope behavior: code=0/message=success on success, API_001
  for bad requests, default X-Tenant-Id=7L, default X-Actor=operator-1.

Target routes:
  GET /cdp/webhooks
  POST /cdp/webhooks
  PUT /cdp/webhooks/{id}
  PUT /cdp/webhooks/{id}/pause
  PUT /cdp/webhooks/{id}/resume
  DELETE /cdp/webhooks/{id}
  POST /cdp/webhooks/{id}/rotate-secret
  POST /cdp/webhooks/{id}/test
  GET /cdp/webhooks/{id}/deliveries

Implementation guidance:
  Use compact deterministic final CDP webhook structures. Do not import old
  org.chovy.canvas.domain/dto/query/dal/engine packages, TenantContextResolver,
  WebhookSubscriptionValidator, WebhookDispatcherService, SecretCipher,
  WebhookSubscriptionMapper, WebhookDeliveryLogMapper, old webhook DOs, or old
  webhook DTOs. Preserve unrelated dirty changes and do not revert others'
  edits.

TDD:
  First add focused RED tests in CdpWebhookControllerCompatibilityTest and
  CdpWebhookApplicationServiceTest. Run focused Maven and capture expected
  failure. Then implement minimal facade/application/domain/controller
  behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWebhookApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWebhookControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|WebhookSubscriptionValidator|WebhookDispatcherService|SecretCipher|WebhookSubscriptionMapper|WebhookDeliveryLogMapper|WebhookSubscriptionDO|WebhookDeliveryLogDO|WebhookSubscriptionDTO|WebhookSubscriptionReq|WebhookDeliveryDTO|WebhookRotateSecretResp" backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWebhookController.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWebhookFacade.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWebhookApplicationService.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWebhookCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BO: Ops Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BO
Dispatch id: dispatch-DDD-C09BO-ops-routes-20260614-111207
Readiness gate: R5 after DDD-C09BN closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/ops
  oldControllerCount=1
  oldEndpointCount=9
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java
  ]

Allowed write scope:
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/OpsFacade.java
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/OpsApplicationService.java
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/OpsCatalog.java
  backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/OpsApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/ops/OpsController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/ops/OpsControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BO-ops-routes-20260614-111207/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/OpsController.java

Goal:
  Add compact final-module compatibility coverage for route:/ops without
  depending on old canvas-engine services. Preserve final web
  CompatibilityEnvelope behavior: code=0/message=success on success, API_001
  for bad requests, default X-Tenant-Id=7L, default X-Actor=operator-1.

Target routes:
  POST /ops/cache/invalidate/{id}
  POST /ops/recovery/runtime-state/rebuild
  GET /ops/runtime/status
  GET /ops/audit-events
  POST /ops/canvas/{id}/pause
  POST /ops/canvas/{id}/offline
  POST /ops/canvas/{id}/resume
  POST /ops/canvas/{id}/kill
  POST /ops/canvas/{id}/rollback

Scope note:
  Do not implement the old OpsController's /canvas/templates,
  /canvas/{id}/save-as-template, /canvas/from-template/{templateId}, or
  /canvas/pending-reviews routes in this task; they are outside route:/ops.

TDD:
  First add focused RED tests in OpsApplicationServiceTest and
  OpsControllerCompatibilityTest. Run the focused Maven commands and capture
  expected failure. Then implement minimal facade/application/domain/controller
  behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-platform -Dtest=OpsApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=OpsControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|CanvasOpsService|CanvasService|OpsAuditEventService|NotificationEventService|CanvasConfigCache|TriggerRouteRecoveryService|CanvasMapper|CanvasTemplateMapper|CanvasVersionMapper|CanvasManualApprovalMapper" backend/canvas-web/src/main/java/org/chovy/canvas/web/ops/OpsController.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/OpsFacade.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/OpsApplicationService.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/OpsCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BM: Computed Tags Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BM
Dispatch id: dispatch-DDD-C09BM-computed-tags-routes-20260614-102400
Readiness gate: R5 after DDD-C09BL closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/cdp/computed-tags
  oldControllerCount=1
  oldEndpointCount=9
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java
  ]

Allowed write scope:
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpComputedTagFacade.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpComputedTagApplicationService.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpComputedTagCatalog.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpComputedTagApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpComputedTagController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpComputedTagControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BM-computed-tags-routes-20260614-102400/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedTagController.java

Goal:
  Add compact final-module compatibility coverage for route:/cdp/computed-tags
  without depending on old canvas-engine services. Preserve final web
  CompatibilityEnvelope behavior: code=0/message=success on success, API_001
  for bad requests, default X-Tenant-Id=7L, default X-Actor=operator-1.

Target routes:
  GET /cdp/computed-tags
  POST /cdp/computed-tags
  POST /cdp/computed-tags/{tagCode}/preview
  POST /cdp/computed-tags/{tagCode}/activate
  POST /cdp/computed-tags/{tagCode}/pause
  POST /cdp/computed-tags/{tagCode}/run
  GET /cdp/computed-tags/{tagCode}/runs
  GET /cdp/computed-tags/{tagCode}/lineage
  POST /cdp/computed-tags/{tagCode}/impact-check

Implementation guidance:
  Use compact deterministic final CDP computed tag structures. Do not import
  old org.chovy.canvas.domain/dto/query/dal/engine packages,
  TenantContextResolver, ComputedTagService, CdpLineageService, or old computed
  tag mapper/dataobject classes. Preserve unrelated dirty changes and do not
  revert others' edits.

TDD:
  First add focused RED tests in CdpComputedTagControllerCompatibilityTest and
  CdpComputedTagApplicationServiceTest. Run focused Maven and capture expected
  failure. Then implement minimal facade/application/domain/controller
  behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpComputedTagApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpComputedTagControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|ComputedTagService|CdpLineageService|CdpComputedTagDefinitionDO|CdpComputedTagRunDO|CdpComputedTagDefinitionMapper|CdpComputedTagDependencyMapper|CdpComputedTagRunMapper" backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpComputedTagController.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpComputedTagFacade.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpComputedTagApplicationService.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpComputedTagCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BL: Creator Collaboration Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BL
Dispatch id: dispatch-DDD-C09BL-creator-collaboration-routes-20260614-100500
Readiness gate: R5 after DDD-C09BK closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/canvas/creator-collaboration
  oldControllerCount=1
  oldEndpointCount=9
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/CreatorCollaborationController.java
  ]

Allowed write scope:
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CreatorCollaborationFacade.java
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CreatorCollaborationApplicationService.java
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/CreatorCollaborationCatalog.java
  backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/CreatorCollaborationApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CreatorCollaborationController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CreatorCollaborationControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BL-creator-collaboration-routes-20260614-100500/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CreatorCollaborationController.java

Goal:
  Add compact final-module compatibility coverage for
  route:/canvas/creator-collaboration without depending on old canvas-engine
  services. Preserve final web CompatibilityEnvelope behavior:
  code=0/message=success on success, API_001 for bad requests, default
  X-Tenant-Id=7L, default X-Actor=operator-1.

Target routes:
  POST /canvas/creator-collaboration/creators
  POST /canvas/creator-collaboration/campaigns
  POST /canvas/creator-collaboration/collaborations
  POST /canvas/creator-collaboration/deliverables
  POST /canvas/creator-collaboration/mutations
  POST /canvas/creator-collaboration/mutations/{mutationId}/approve
  POST /canvas/creator-collaboration/mutations/{mutationId}/execute
  GET /canvas/creator-collaboration/mutations
  GET /canvas/creator-collaboration/summary

Implementation guidance:
  Use compact deterministic final Canvas creator-collaboration structures. Do
  not import old org.chovy.canvas.domain/dto/query/dal/engine packages,
  TenantContextResolver, CreatorCollaborationService, or
  CreatorProviderMutationService. Preserve unrelated dirty changes and do not
  revert others' edits.

TDD:
  First add focused RED tests in CreatorCollaborationControllerCompatibilityTest
  and CreatorCollaborationApplicationServiceTest. Run focused Maven and capture
  expected failure. Then implement minimal facade/application/domain/controller
  behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CreatorCollaborationApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CreatorCollaborationControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|CreatorCollaborationService|CreatorProviderMutationService" backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CreatorCollaborationController.java backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CreatorCollaborationFacade.java backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CreatorCollaborationApplicationService.java backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/CreatorCollaborationCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BK: Warehouse Tables Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BK
Dispatch id: dispatch-DDD-C09BK-warehouse-tables-routes-20260614-094500
Readiness gate: R5 after DDD-C09BJ closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/warehouse/tables
  oldControllerCount=2
  oldEndpointCount=9
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableDriftIncidentController.java,
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableGovernanceController.java
  ]

Allowed write scope:
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseTableFacade.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseTableApplicationService.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseTableCatalog.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseTableApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseTableController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseTableControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BK-warehouse-tables-routes-20260614-094500/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableDriftIncidentController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseTableGovernanceController.java

Goal:
  Add compact final-module compatibility coverage for route:/warehouse/tables
  without depending on old canvas-engine services. Preserve final web
  CompatibilityEnvelope behavior: code=0/message=success on success, API_001
  for bad requests, default X-Tenant-Id=7L, default X-Actor=operator-1.

Target routes:
  GET /warehouse/tables/contracts
  POST /warehouse/tables/contracts
  POST /warehouse/tables/contracts/{tableKey}/inspect
  POST /warehouse/tables/inspect
  POST /warehouse/tables/contracts/{tableKey}/inspect-live
  POST /warehouse/tables/inspect-live
  POST /warehouse/tables/contracts/{tableKey}/remediation-plan
  POST /warehouse/tables/remediation-plan
  POST /warehouse/tables/incidents/scan

Implementation guidance:
  Use compact deterministic final CDP warehouse table structures. Do not import
  old org.chovy.canvas.domain/dto/query/dal/engine packages,
  TenantContextResolver, CdpWarehouseTableGovernanceService, or
  CdpWarehouseTableDriftIncidentService. Preserve unrelated dirty changes and
  do not revert others' edits.

TDD:
  First add focused RED tests in CdpWarehouseTableControllerCompatibilityTest
  and CdpWarehouseTableApplicationServiceTest. Run focused Maven and capture
  expected failure. Then implement minimal facade/application/domain/controller
  behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseTableApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehouseTableControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|CdpWarehouseTableGovernanceService|CdpWarehouseTableDriftIncidentService" backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseTableController.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseTableFacade.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseTableApplicationService.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseTableCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BE: Warehouse Privacy Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BE
Dispatch id: dispatch-DDD-C09BE-warehouse-privacy-routes-20260614-080300
Readiness gate: R5 after DDD-C09BD closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Allowed write scope:
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehousePrivacyFacade.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehousePrivacyApplicationService.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehousePrivacyCatalog.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehousePrivacyApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehousePrivacyController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehousePrivacyControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BE-warehouse-privacy-routes-20260614-080300/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyErasureController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehousePrivacyTombstoneController.java

Goal:
  Add compact final-module compatibility coverage for route:/warehouse/privacy
  without depending on old canvas-engine services. Preserve final web
  CompatibilityEnvelope behavior: code=0/message=success on success, API_001
  for bad requests, default X-Tenant-Id=7L, default X-Actor=operator-1.

Target routes:
  POST /warehouse/privacy/erasure/requests
  POST /warehouse/privacy/erasure/requests/{id}/proofs
  POST /warehouse/privacy/erasure/requests/{id}/execute
  POST /warehouse/privacy/erasure/requests/{id}/audience-rebuild
  POST /warehouse/privacy/erasure/audience-rebuild/automation/run
  GET /warehouse/privacy/erasure/audience-rebuild/automation/runs
  GET /warehouse/privacy/erasure/audience-rebuild/automation/runs/{id}
  GET /warehouse/privacy/erasure/requests
  GET /warehouse/privacy/erasure/requests/{id}
  GET /warehouse/privacy/erasure/summary
  POST /warehouse/privacy/tombstones
  POST /warehouse/privacy/tombstones/from-erasure-request
  POST /warehouse/privacy/tombstones/{id}/revoke
  GET /warehouse/privacy/tombstones
  GET /warehouse/privacy/tombstones/decision

Implementation guidance:
  Use compact deterministic Map/List structures and a small final CDP privacy
  catalog if useful. Do not import old org.chovy.canvas.domain/dto/query/dal
  packages, TenantContextResolver, or old services. You are not alone in the
  worktree: preserve unrelated dirty changes and do not revert others' edits.

TDD:
  First add focused RED tests in CdpWarehousePrivacyControllerCompatibilityTest
  and CdpWarehousePrivacyApplicationServiceTest. Run focused Maven and capture
  expected failure. Then implement minimal facade/application/domain/controller
  behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehousePrivacyApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehousePrivacyControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal)|TenantContextResolver|CdpWarehousePrivacyErasureService|CdpWarehousePrivacyErasureExecutionService|CdpWarehousePrivacyAudienceBitmapRebuildService|CdpWarehousePrivacyAudienceBitmapRebuildAutomationService|CdpWarehousePrivacyAudienceBitmapRebuildAutomationRunService|CdpWarehousePrivacyTombstoneService" backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehousePrivacyController.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehousePrivacyFacade.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehousePrivacyApplicationService.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehousePrivacyCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BF: Marketing Integrations Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BF
Dispatch id: dispatch-DDD-C09BF-marketing-integrations-routes-20260614-081400
Readiness gate: R5 after DDD-C09BE closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/canvas/marketing-integrations
  oldControllerCount=2
  oldEndpointCount=11
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingIntegrationContractController.java,
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingIntegrationContractProbeController.java
  ]

Allowed write scope:
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingIntegrationFacade.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingIntegrationApplicationService.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingIntegrationCatalog.java
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/MarketingIntegrationApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingIntegrationController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/MarketingIntegrationControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BF-marketing-integrations-routes-20260614-081400/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingIntegrationContractController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingIntegrationContractProbeController.java

Goal:
  Add compact final-module compatibility coverage for route:/canvas/marketing-integrations
  without depending on old canvas-engine services. Preserve final web
  CompatibilityEnvelope behavior: code=0/message=success on success, API_001
  for bad requests, default X-Tenant-Id=7L, default X-Actor=operator-1.

Target routes:
  POST /canvas/marketing-integrations/contracts
  GET /canvas/marketing-integrations/contracts
  GET /canvas/marketing-integrations/contracts/{contractId}/audit-events
  DELETE /canvas/marketing-integrations/contracts/{contractId}
  POST /canvas/marketing-integrations/contracts/{contractId}/probe-runs
  GET /canvas/marketing-integrations/contract-probe-runs
  POST /canvas/marketing-integrations/contract-probe-runs/scan
  GET /canvas/marketing-integrations/contract-slo-evaluations
  POST /canvas/marketing-integrations/contracts/{contractId}/probes
  GET /canvas/marketing-integrations/contracts/{contractId}/probes
  GET /canvas/marketing-integrations/probes

Implementation guidance:
  Use compact deterministic Map/List structures and a small final Marketing
  integration catalog if useful. Do not import old org.chovy.canvas.domain/dto/
  query/dal packages, TenantContextResolver, or old services. You are not alone
  in the worktree: preserve unrelated dirty changes and do not revert others'
  edits.

TDD:
  First add focused RED tests in MarketingIntegrationControllerCompatibilityTest
  and MarketingIntegrationApplicationServiceTest. Run focused Maven and capture
  expected failure. Then implement minimal facade/application/domain/controller
  behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=MarketingIntegrationApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=MarketingIntegrationControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal)|TenantContextResolver|MarketingIntegrationContractService|MarketingIntegrationContractProbeService|MarketingIntegrationContractProbeAutomationService|MarketingIntegrationContractSloService" backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/MarketingIntegrationController.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingIntegrationFacade.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingIntegrationApplicationService.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingIntegrationCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BG: Analytics Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BG
Dispatch id: dispatch-DDD-C09BG-analytics-routes-20260614-082400
Readiness gate: R5 after DDD-C09BF closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/analytics
  oldControllerCount=1
  oldEndpointCount=10
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsController.java
  ]

Allowed write scope:
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/AnalyticsFacade.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/AnalyticsViews.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/AnalyticsApplicationService.java
  backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/AnalyticsCatalog.java
  backend/canvas-context-bi/src/test/java/org/chovy/canvas/bi/application/AnalyticsApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/analytics/AnalyticsController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/analytics/AnalyticsControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BG-analytics-routes-20260614-082400/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the seven allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AnalyticsController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/analytics/AnalyticsQueryService.java

Goal:
  Add compact final-module compatibility coverage for route:/analytics
  without depending on old canvas-engine services. Preserve final web
  CompatibilityEnvelope behavior: code=0/message=success on success, API_001
  for bad requests, default X-Tenant-Id=7L.

Target routes:
  GET /analytics/events/counts
  GET /analytics/events
  GET /analytics/events/count
  GET /analytics/users/{userId}/timeline
  GET /analytics/events/attributes/{attribute}/distribution
  GET /analytics/attributes/{attribute}/distribution
  GET /analytics/funnels/{funnelKey}
  POST /analytics/alerts/preview
  POST /analytics/exports
  GET /analytics/exports/{id}

Implementation guidance:
  Use compact deterministic final BI analytics structures. Do not import old
  org.chovy.canvas.domain/dto/query/dal packages, TenantContextResolver, or
  AnalyticsQueryService. You are not alone in the worktree: preserve unrelated
  dirty changes and do not revert others' edits.

TDD:
  First add focused RED tests in AnalyticsControllerCompatibilityTest and
  AnalyticsApplicationServiceTest. Run focused Maven and capture expected
  failure. Then implement minimal facade/application/domain/controller behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-bi -Dtest=AnalyticsApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=AnalyticsControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal)|TenantContextResolver|AnalyticsQueryService|AnalyticsEventMapper|AnalyticsFunnelDefinitionMapper|AnalyticsAlertRuleMapper|AnalyticsExportJobMapper" backend/canvas-web/src/main/java/org/chovy/canvas/web/analytics/AnalyticsController.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/AnalyticsFacade.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/api/AnalyticsViews.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/application/AnalyticsApplicationService.java backend/canvas-context-bi/src/main/java/org/chovy/canvas/bi/domain/AnalyticsCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BH: Audience Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BH
Dispatch id: dispatch-DDD-C09BH-audience-routes-20260614-083805
Readiness gate: R5 after DDD-C09BG closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/canvas/audiences
  oldControllerCount=1
  oldEndpointCount=10
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java
  ]

Allowed write scope:
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/AudienceFacade.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/AudienceApplicationService.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/AudienceCatalog.java
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/AudienceApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/AudienceController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/AudienceControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BH-audience-routes-20260614-083805/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java

Goal:
  Add compact final-module compatibility coverage for route:/canvas/audiences
  without depending on old canvas-engine services. Preserve final web
  CompatibilityEnvelope behavior: code=0/message=success on success, API_001
  for bad requests, default X-Tenant-Id=7L, default X-Actor=operator-1.

Target routes:
  GET /canvas/audiences
  GET /canvas/audiences/source-fields
  POST /canvas/audiences/preview
  GET /canvas/audiences/{id}
  GET /canvas/audiences/ready
  POST /canvas/audiences
  PUT /canvas/audiences/{id}
  DELETE /canvas/audiences/{id}
  POST /canvas/audiences/{id}/compute
  GET /canvas/audiences/{id}/stat

Implementation guidance:
  Use compact deterministic final Marketing audience structures. Do not import
  old org.chovy.canvas.domain/dto/query/dal/engine packages,
  TenantContextResolver, or old audience services. Preserve unrelated dirty
  changes and do not revert others' edits.

TDD:
  First add focused RED tests in AudienceControllerCompatibilityTest and
  AudienceApplicationServiceTest. Run focused Maven and capture expected
  failure. Then implement minimal facade/application/domain/controller behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=AudienceApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=AudienceControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|AudienceDefinitionMapper|AudienceStatMapper|AudienceBatchComputeService|AudienceSchedulerService|AsyncTaskService|AudienceComputeTaskRunner|CdpAudienceSourceService" backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/AudienceController.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/AudienceFacade.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/AudienceApplicationService.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/AudienceCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BI: Programmatic DSP Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BI
Dispatch id: dispatch-DDD-C09BI-programmatic-dsp-routes-20260614-085000
Readiness gate: R5 after DDD-C09BH closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/canvas/programmatic-dsp
  oldControllerCount=1
  oldEndpointCount=10
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProgrammaticDspController.java
  ]

Allowed write scope:
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/ProgrammaticDspFacade.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/ProgrammaticDspApplicationService.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/ProgrammaticDspCatalog.java
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/ProgrammaticDspApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/ProgrammaticDspController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/ProgrammaticDspControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BI-programmatic-dsp-routes-20260614-085000/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/ProgrammaticDspController.java

Goal:
  Add compact final-module compatibility coverage for
  route:/canvas/programmatic-dsp without depending on old canvas-engine services.
  Preserve final web CompatibilityEnvelope behavior: code=0/message=success on
  success, API_001 for bad requests, default X-Tenant-Id=7L, default
  X-Actor=operator-1.

Target routes:
  POST /canvas/programmatic-dsp/seats
  POST /canvas/programmatic-dsp/campaigns
  POST /canvas/programmatic-dsp/line-items
  POST /canvas/programmatic-dsp/supply-paths
  POST /canvas/programmatic-dsp/snapshots
  GET /canvas/programmatic-dsp/summary
  POST /canvas/programmatic-dsp/mutations
  POST /canvas/programmatic-dsp/mutations/{mutationId}/approve
  POST /canvas/programmatic-dsp/mutations/{mutationId}/execute
  GET /canvas/programmatic-dsp/mutations

Implementation guidance:
  Use compact deterministic final Marketing programmatic DSP structures. Do not
  import old org.chovy.canvas.domain/dto/query/dal/engine packages,
  TenantContextResolver, ProgrammaticDspService, or
  ProgrammaticDspMutationService. Preserve unrelated dirty changes and do not
  revert others' edits.

TDD:
  First add focused RED tests in ProgrammaticDspControllerCompatibilityTest and
  ProgrammaticDspApplicationServiceTest. Run focused Maven and capture expected
  failure. Then implement minimal facade/application/domain/controller behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=ProgrammaticDspApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=ProgrammaticDspControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|ProgrammaticDspService|ProgrammaticDspMutationService|ProgrammaticDspSeatMapper|ProgrammaticDspCampaignMapper|ProgrammaticDspLineItemMapper|ProgrammaticDspSupplyPathMapper|ProgrammaticDspSnapshotMapper|ProgrammaticDspMutationMapper" backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/ProgrammaticDspController.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/ProgrammaticDspFacade.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/ProgrammaticDspApplicationService.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/ProgrammaticDspCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BQ: Warehouse Availability Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BQ
Dispatch id: dispatch-DDD-C09BQ-warehouse-availability-routes-20260614-123929
Readiness gate: R5 after DDD-C09BP Public Ingress route closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/warehouse/availability
  oldControllerCount=3
  oldEndpointCount=8
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAvailabilityController.java,
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAvailabilityIncidentController.java,
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseConsumerAvailabilityIncidentController.java
  ]

Allowed write scope:
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseAvailabilityFacade.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseAvailabilityApplicationService.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseAvailabilityCatalog.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpWarehouseAvailabilityApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseAvailabilityController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpWarehouseAvailabilityControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BQ-warehouse-availability-routes-20260614-123929/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAvailabilityController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseAvailabilityIncidentController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpWarehouseConsumerAvailabilityIncidentController.java

Goal:
  Add compact final-module compatibility coverage for
  route:/warehouse/availability without depending on old canvas-engine services.
  Preserve final web CompatibilityEnvelope behavior: code=0/message=success on
  success, API_001 for bad requests, default X-Tenant-Id=7L, default
  X-Actor=operator-1.

Target routes:
  GET /warehouse/availability
  POST /warehouse/availability/assets
  GET /warehouse/availability/assets
  POST /warehouse/availability/contracts
  GET /warehouse/availability/contracts
  POST /warehouse/availability/contracts/{contractKey}/evaluate
  POST /warehouse/availability/incidents/scan
  POST /warehouse/availability/consumer-incidents/scan

Implementation guidance:
  Use compact deterministic final CDP warehouse availability structures. Do not
  import old org.chovy.canvas.domain/dto/query/dal/engine packages,
  TenantContextResolver, CdpWarehouseAvailabilityService,
  CdpWarehouseConsumerAvailabilityService,
  CdpWarehouseAvailabilityIncidentService, or
  CdpWarehouseConsumerAvailabilityIncidentService. Preserve unrelated dirty
  changes and do not revert others' edits.

TDD:
  First add focused RED tests in CdpWarehouseAvailabilityControllerCompatibilityTest
  and CdpWarehouseAvailabilityApplicationServiceTest. Run focused Maven and
  capture expected failure. Then implement minimal facade/application/domain/controller behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpWarehouseAvailabilityApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpWarehouseAvailabilityControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|CdpWarehouseAvailabilityService|CdpWarehouseConsumerAvailabilityService|CdpWarehouseAvailabilityIncidentService|CdpWarehouseConsumerAvailabilityIncidentService" backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpWarehouseAvailabilityController.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpWarehouseAvailabilityFacade.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpWarehouseAvailabilityApplicationService.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpWarehouseAvailabilityCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BS: Computed Profile Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BS
Dispatch id: dispatch-DDD-C09BS-computed-profile-routes-20260614-134941
Readiness gate: R5 after DDD-C09BR Tag Definitions route closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/cdp/computed-profile-attributes
  oldControllerCount=1
  oldEndpointCount=8
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java
  ]

Allowed write scope:
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpComputedProfileFacade.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpComputedProfileApplicationService.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpComputedProfileCatalog.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpComputedProfileApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpComputedProfileController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpComputedProfileControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BS-computed-profile-routes-20260614-134941/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpComputedProfileController.java

Goal:
  Add compact final-module compatibility coverage for
  route:/cdp/computed-profile-attributes without depending on old
  canvas-engine services. Preserve final web CompatibilityEnvelope behavior:
  code=0/message=success on success, API_001 for bad requests where
  controller/application validation rejects input, default X-Tenant-Id=7L,
  and default X-Actor=operator-1.

Target routes:
  GET /cdp/computed-profile-attributes
  POST /cdp/computed-profile-attributes
  POST /cdp/computed-profile-attributes/{id}/preview
  POST /cdp/computed-profile-attributes/{id}/activate
  POST /cdp/computed-profile-attributes/{id}/pause
  POST /cdp/computed-profile-attributes/{id}/run
  GET /cdp/computed-profile-attributes/{id}/runs
  GET /cdp/computed-profile-attributes/{id}/changes

Implementation guidance:
  Use compact deterministic final CDP computed-profile structures. Do not
  import old org.chovy.canvas.domain/dto/query/dal/engine packages,
  TenantContextResolver, ComputedProfileAttributeService,
  CdpComputedProfileAttributeDO, CdpComputedProfileRunDO,
  CdpProfileAttributeChangeLogDO, or old R. Preserve unrelated dirty changes
  and do not revert others' edits.

TDD:
  First add focused RED tests in CdpComputedProfileControllerCompatibilityTest
  and CdpComputedProfileApplicationServiceTest. Run focused Maven and capture
  expected failure. Then implement minimal facade/application/domain/controller
  behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpComputedProfileApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpComputedProfileControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|ComputedProfileAttributeService|CdpComputedProfileAttributeDO|CdpComputedProfileRunDO|CdpProfileAttributeChangeLogDO" backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpComputedProfileController.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpComputedProfileFacade.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpComputedProfileApplicationService.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpComputedProfileCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BR: Tag Definitions Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BR
Dispatch id: dispatch-DDD-C09BR-tag-definitions-routes-20260614-131323
Readiness gate: R5 after DDD-C09BQ Warehouse Availability route closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/canvas/tag-definitions
  oldControllerCount=1
  oldEndpointCount=8
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/TagDefinitionController.java
  ]

Allowed write scope:
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpTagDefinitionFacade.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpTagDefinitionApplicationService.java
  backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpTagDefinitionCatalog.java
  backend/canvas-context-cdp/src/test/java/org/chovy/canvas/cdp/application/CdpTagDefinitionApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpTagDefinitionController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/cdp/CdpTagDefinitionControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BR-tag-definitions-routes-20260614-131323/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/TagDefinitionController.java

Goal:
  Add compact final-module compatibility coverage for
  route:/canvas/tag-definitions without depending on old canvas-engine
  services. Preserve final web CompatibilityEnvelope behavior:
  code=0/message=success on success, API_001 for bad requests where
  controller/application validation rejects input, default X-Tenant-Id=7L,
  and default X-Actor=operator-1.

Target routes:
  GET /canvas/tag-definitions
  POST /canvas/tag-definitions
  PUT /canvas/tag-definitions/{id}
  DELETE /canvas/tag-definitions/{id}
  GET /canvas/tag-definitions/{tagCode}/values
  POST /canvas/tag-definitions/{tagCode}/values
  PUT /canvas/tag-definitions/values/{id}
  DELETE /canvas/tag-definitions/values/{id}

Implementation guidance:
  Use compact deterministic final CDP tag-definition structures. Do not import
  old org.chovy.canvas.domain/dto/query/dal/engine packages,
  TenantContextResolver, TagDefinitionService, TagDefinitionDO,
  TagValueDefinitionDO, PageResult, or old R. Preserve unrelated dirty changes
  and do not revert others' edits.

TDD:
  First add focused RED tests in CdpTagDefinitionControllerCompatibilityTest
  and CdpTagDefinitionApplicationServiceTest. Run focused Maven and capture
  expected failure. Then implement minimal facade/application/domain/controller
  behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-cdp -Dtest=CdpTagDefinitionApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CdpTagDefinitionControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|TagDefinitionService|TagDefinitionDO|TagValueDefinitionDO|PageResult" backend/canvas-web/src/main/java/org/chovy/canvas/web/cdp/CdpTagDefinitionController.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/api/CdpTagDefinitionFacade.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/application/CdpTagDefinitionApplicationService.java backend/canvas-context-cdp/src/main/java/org/chovy/canvas/cdp/domain/CdpTagDefinitionCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BT: Canvas Stats Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BT
Dispatch id: dispatch-DDD-C09BT-canvas-stats-routes-20260614-141402
Readiness gate: R5 after DDD-C09BS Computed Profile route closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=family:CanvasStats
  oldControllerCount=1
  oldEndpointCount=7
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java
  ]

Allowed write scope:
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CanvasStatsFacade.java
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CanvasStatsApplicationService.java
  backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/CanvasStatsCatalog.java
  backend/canvas-context-canvas/src/test/java/org/chovy/canvas/canvas/application/CanvasStatsApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasStatsController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/canvas/CanvasStatsControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BT-canvas-stats-routes-20260614-141402/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java

Goal:
  Add compact final-module compatibility coverage for family:CanvasStats
  without depending on old canvas-engine services. Preserve final web
  CompatibilityEnvelope behavior: code=0/message=success on success, API_001
  for bad requests where controller/application validation rejects input.

Target routes:
  GET /canvas/{id}/execution/{executionId}/trace
  GET /canvas/{id}/executions
  GET /canvas/{id}/stats
  GET /canvas/{id}/funnel
  GET /canvas/{id}/trend
  GET /canvas/{id}/receipts
  GET /canvas/{id}/attribution-summary

Implementation guidance:
  Use compact deterministic final Canvas stats structures. Do not import old
  org.chovy.canvas.domain/dto/query/dal/engine packages,
  CanvasExecutionMapper, CanvasExecutionTraceMapper, CanvasExecutionStatsMapper,
  DorisQueryService, DailyStatsDTO, MessageSendRecordMapper,
  CanvasConversionAttributionMapper, MapFieldKeys, QueryWrapper, or
  LambdaQueryWrapper. Preserve unrelated dirty changes and do not revert others'
  edits.

TDD:
  First add focused RED tests in CanvasStatsControllerCompatibilityTest and
  CanvasStatsApplicationServiceTest. Run focused Maven and capture expected
  failure. Then implement minimal facade/application/domain/controller behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-canvas -Dtest=CanvasStatsApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=CanvasStatsControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|CanvasExecutionMapper|CanvasExecutionTraceMapper|CanvasExecutionStatsMapper|DorisQueryService|DailyStatsDTO|MessageSendRecordMapper|CanvasConversionAttributionMapper|MapFieldKeys|QueryWrapper|LambdaQueryWrapper" backend/canvas-web/src/main/java/org/chovy/canvas/web/canvas/CanvasStatsController.java backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/api/CanvasStatsFacade.java backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/application/CanvasStatsApplicationService.java backend/canvas-context-canvas/src/main/java/org/chovy/canvas/canvas/domain/CanvasStatsCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BP: Public Ingress Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BP
Dispatch id: dispatch-DDD-C09BP-public-ingress-routes-20260614-115134
Readiness gate: R5 after DDD-C09BO Ops route closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/public
  oldControllerCount=4
  oldEndpointCount=8
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingFormController.java,
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicConversationWebhookController.java,
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicMarketingContentUploadWebhookController.java,
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicMarketingMonitoringWebhookController.java
  ]

Allowed write scope:
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/PublicIngressFacade.java
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/PublicIngressApplicationService.java
  backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/PublicIngressCatalog.java
  backend/canvas-platform/src/test/java/org/chovy/canvas/platform/application/PublicIngressApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/publicingress/PublicIngressController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/publicingress/PublicIngressControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BP-public-ingress-routes-20260614-115134/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/MarketingFormController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicConversationWebhookController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicMarketingContentUploadWebhookController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/PublicMarketingMonitoringWebhookController.java

Goal:
  Add compact final-module compatibility coverage for route:/public without
  depending on old canvas-engine services. Preserve final web
  CompatibilityEnvelope behavior: code=0/message=success on success, API_001
  for bad requests where controller/application validation rejects input.

Target routes:
  GET /public/marketing-forms/{publicKey}
  POST /public/marketing-forms/{publicKey}/submit
  GET /public/conversation-webhooks/{tenantId}/whatsapp
  POST /public/conversation-webhooks/{tenantId}/whatsapp
  GET /public/conversations/webhooks/{tenantId}/whatsapp
  POST /public/conversations/webhooks/{tenantId}/whatsapp
  POST /public/marketing/content/assets/upload-callbacks/{tenantId}/{provider}
  POST /public/marketing-monitoring/webhooks/{tenantId}/{sourceKey}

Implementation guidance:
  Use compact deterministic final Platform public-ingress structures. Do not
  import old org.chovy.canvas.domain/dto/query/dal/engine packages,
  TenantContextResolver, MarketingFormService, ConversationAdapterHarness,
  WhatsAppWebhookPayloadMapper, WhatsAppWebhookSecurityService,
  DeliveryOutboxService, MarketingAssetUploadService,
  MarketingAssetUploadWebhookSignatureService, or
  MarketingMonitorWebhookIngestionService. Preserve unrelated dirty changes and
  do not revert others' edits.

TDD:
  First add focused RED tests in PublicIngressControllerCompatibilityTest and
  PublicIngressApplicationServiceTest. Run focused Maven and capture expected
  failure. Then implement minimal facade/application/domain/controller behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-platform -Dtest=PublicIngressApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=PublicIngressControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|MarketingFormService|ConversationAdapterHarness|WhatsAppWebhookPayloadMapper|WhatsAppWebhookSecurityService|DeliveryOutboxService|MarketingAssetUploadService|MarketingAssetUploadWebhookSignatureService|MarketingMonitorWebhookIngestionService" backend/canvas-web/src/main/java/org/chovy/canvas/web/publicingress/PublicIngressController.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/api/PublicIngressFacade.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/application/PublicIngressApplicationService.java backend/canvas-platform/src/main/java/org/chovy/canvas/platform/domain/PublicIngressCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```

### DDD-C09BJ: AB Experiments Route Aliases

```text
Program: DDD modular rewrite
Task id: DDD-C09BJ
Dispatch id: dispatch-DDD-C09BJ-ab-experiments-routes-20260614-090800
Readiness gate: R5 after DDD-C09BI closeout
Worker type: code-writing
Branch/worktree: main / /Users/photonpay/project/canvas
Base commit: 2a1cdec07ec27a5298958822014aa28d9312869c

Inventory rows required:
  group=route:/canvas/ab-experiments
  oldControllerCount=2
  oldEndpointCount=9
  currentControllerCount=0
  currentEndpointCount=0
  representativeOldControllerFiles=[
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/AbExperimentController.java,
    backend/canvas-engine/src/main/java/org/chovy/canvas/web/AbExperimentGovernanceController.java
  ]

Allowed write scope:
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/AbExperimentFacade.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/AbExperimentApplicationService.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/AbExperimentCatalog.java
  backend/canvas-context-marketing/src/test/java/org/chovy/canvas/marketing/application/AbExperimentApplicationServiceTest.java
  backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/AbExperimentController.java
  backend/canvas-web/src/test/java/org/chovy/canvas/web/marketing/AbExperimentControllerCompatibilityTest.java

Coordinator-owned files, do not edit:
  docs/program-coordination/dispatch-state.json
  docs/program-coordination/progress-ledger.md
  docs/program-coordination/evidence/dispatch-DDD-C09BJ-ab-experiments-routes-20260614-090800/**
  docs/program-coordination/subagent-worker-packets.md

Forbidden write scope:
  backend/canvas-engine/**
  any pom.xml
  files outside the six allowed code/test paths

Legacy reference only:
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AbExperimentController.java
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/AbExperimentGovernanceController.java

Goal:
  Add compact final-module compatibility coverage for
  route:/canvas/ab-experiments without depending on old canvas-engine services.
  Preserve final web CompatibilityEnvelope behavior: code=0/message=success on
  success, API_001 for bad requests, default X-Tenant-Id=7L, default
  X-Actor=operator-1.

Target routes:
  GET /canvas/ab-experiments
  POST /canvas/ab-experiments
  PUT /canvas/ab-experiments/{id}
  DELETE /canvas/ab-experiments/{id}
  GET /canvas/ab-experiments/{id}/groups
  POST /canvas/ab-experiments/{id}/groups
  PUT /canvas/ab-experiments/{id}/groups/{groupId}
  DELETE /canvas/ab-experiments/{id}/groups/{groupId}
  POST /canvas/ab-experiments/{experimentId}/governance/evaluate

Implementation guidance:
  Use compact deterministic final Marketing AB experiment structures. Do not
  import old org.chovy.canvas.domain/dto/query/dal/engine packages,
  TenantContextResolver, AbExperimentMapper, AbExperimentGroupService, or
  AbExperimentGovernanceService. Preserve unrelated dirty changes and do not
  revert others' edits.

TDD:
  First add focused RED tests in AbExperimentControllerCompatibilityTest and
  AbExperimentApplicationServiceTest. Run focused Maven and capture expected
  failure. Then implement minimal facade/application/domain/controller behavior.

Verification commands:
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn test -pl canvas-context-marketing -Dtest=AbExperimentApplicationServiceTest
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-web -am -Dtest=AbExperimentControllerCompatibilityTest test
  cd backend && JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn compile -pl canvas-web -am -DskipTests
  node tools/program-coordination/cutover-compatibility-preflight.mjs . --json
  rg -n "canvas-engine|org\\.chovy\\.canvas\\.(domain|dto|query|dal|engine)|TenantContextResolver|AbExperimentMapper|AbExperimentGroupService|AbExperimentGovernanceService" backend/canvas-web/src/main/java/org/chovy/canvas/web/marketing/AbExperimentController.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/AbExperimentFacade.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/AbExperimentApplicationService.java backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/AbExperimentCatalog.java

Return:
  Use the canonical return packet fields from this file. Include files changed,
  contracts changed, tests run, verification result, risks, and rollback path.
```
