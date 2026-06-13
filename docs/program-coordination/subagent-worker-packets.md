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
