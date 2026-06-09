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
Forbidden write scope:
  HandlerRegistry
  PluginRegistryService
  JdbcPluginRepository
  backend/pom.xml
  any other plugin package
Contracts to read:
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
