# Task Pack 07: Canvas Worker

**Worker:** `canvas-worker`

**Goal:** Rewrite canvas authoring, draft, version, publish lifecycle, project
folder assignment, and user-input authoring behavior into
`canvas-context-canvas`.

---

## Allowed Write Scope

```text
backend/canvas-context-canvas/**
```

## Read Scope

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/**
backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/**
backend/canvas-engine/src/main/java/org/chovy/canvas/query/CanvasListQuery.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Canvas*DO.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Canvas*Mapper.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/**
```

## Forbidden Changes

```text
backend/pom.xml
backend/canvas-common/**
backend/canvas-web/**
backend/canvas-boot/**
backend/canvas-context-execution/**
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
org.chovy.canvas.canvas.api
org.chovy.canvas.canvas.application
org.chovy.canvas.canvas.domain
org.chovy.canvas.canvas.adapter.persistence
org.chovy.canvas.canvas.config
```

---

## Required Split

```text
CanvasDraftApplicationService
CanvasVersionApplicationService
CanvasPublishApplicationService
CanvasQueryApplicationService
CanvasProjectFolderApplicationService
Canvas
CanvasVersion
CanvasStateTransitionPolicy
CanvasRepository
CanvasVersionRepository
MybatisCanvasRepository
MybatisCanvasVersionRepository
PublishedCanvasDefinition
ExecutionPublicationPort
```

---

## Required Migration

- [ ] Move canvas state transition rules into domain policy.
- [ ] Move Canvas/Version persistence into `adapter.persistence`.
- [ ] Publish flow calls `ExecutionPublicationPort`.
- [ ] Canvas domain does not know Redis, scheduler, execution service, or cache.
- [ ] Published definition API contains all runtime data execution needs.
- [ ] User input authoring stays in canvas unless execution runtime ownership is
      proven by inventory.

---

## Acceptance Tests

Run:

```bash
cd backend
mvn test -pl canvas-context-canvas
cd ..
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

Expected:

```text
canvas module compiles
draft/version/publish state tests pass
domain has no MyBatis/Redis/execution adapter dependency
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
