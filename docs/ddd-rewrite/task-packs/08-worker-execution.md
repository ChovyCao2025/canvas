# Task Pack 08: Execution Worker

**Worker:** `execution-worker`

**Goal:** Rewrite DAG runtime, node handlers, triggers, scheduling, wait/resume,
execution trace, and recovery behavior into `canvas-context-execution`.

---

## Allowed Write Scope

```text
backend/canvas-context-execution/**
```

## Read Scope

```text
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
```

## Forbidden Changes

```text
backend/pom.xml
backend/canvas-common/**
backend/canvas-web/**
backend/canvas-boot/**
backend/canvas-context-canvas/**
backend/canvas-context-risk/**
backend/canvas-context-cdp/**
```

---

## Required Child Spec

Read first:

```text
docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md
```

---

## Target Packages

```text
org.chovy.canvas.execution.api
org.chovy.canvas.execution.application
org.chovy.canvas.execution.domain
org.chovy.canvas.execution.adapter.persistence
org.chovy.canvas.execution.adapter.messaging
org.chovy.canvas.execution.adapter.external
org.chovy.canvas.execution.config
```

---

## Required Split

```text
CanvasExecutionFacade
CanvasExecutionApplicationService
CanvasTriggerApplicationService
CanvasSchedulerApplicationService
ExecutionRecoveryApplicationService
DagRuntimeService
NodeHandlerRegistry
ExecutionTraceService
ExecutionPublicationApplicationService
```

---

## Required Migration

- [ ] Execution implements `ExecutionPublicationPort`.
- [ ] Runtime reads `PublishedCanvasDefinition`, not canvas persistence.
- [ ] Node handlers stay in execution.
- [ ] Risk node calls `risk.api.RiskDecisionFacade`.
- [ ] Audience/CDP nodes call CDP API.
- [ ] Redis trigger routing moves to execution adapter.
- [ ] RocketMQ consumers/publishers move to `adapter.messaging`.
- [ ] Execution trace persistence moves to `adapter.persistence`.

---

## Acceptance Tests

Run:

```bash
cd backend
mvn test -pl canvas-context-execution
cd ..
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

Expected:

```text
execution module compiles
DAG/parser/handler/trigger/wait tests pass
domain has no canvas persistence dependency
node handlers do not import risk or CDP persistence
```

---

## Worker Response

Return:

```text
status:
task id:
dispatch id:
branch:
worktree:
base commit:
head commit:
assigned task pack:
files changed:
contracts changed:
old classes migrated:
new public api:
domain model changes:
persistence ownership changes:
tests run:
verification result:
verification output summary/path:
evidence artifact paths:
guardrail checks:
failure modes reviewed:
compatibility evidence:
temporary bridges:
open risks:
coordinator actions needed:
ledger update:
rollback path:
```

This is the canonical DDD worker final response format from
`docs/ddd-rewrite/guardrails/worker-enforcement-protocol.md` and
`docs/program-coordination/subagent-worker-packets.md`. Do not return a shorter
summary.
