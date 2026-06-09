# Task Pack 05: BI Worker

**Worker:** `bi-worker`

**Goal:** Rewrite BI workspace, datasource, dataset, metric, chart, dashboard,
portal, permission, and subscription behavior into `canvas-context-bi`.

---

## Allowed Write Scope

```text
backend/canvas-context-bi/**
```

## Read Scope

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/bi/**
backend/canvas-engine/src/main/java/org/chovy/canvas/web/bi/**
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Bi*DO.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Bi*Mapper.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/bi/**
backend/canvas-engine/src/test/java/org/chovy/canvas/web/bi/**
```

## Forbidden Changes

```text
backend/pom.xml
backend/canvas-common/**
backend/canvas-web/**
backend/canvas-boot/**
backend/canvas-context-cdp/**
```

---

## Target Packages

```text
org.chovy.canvas.bi.api
org.chovy.canvas.bi.application
org.chovy.canvas.bi.domain
org.chovy.canvas.bi.adapter.persistence
org.chovy.canvas.bi.adapter.external
org.chovy.canvas.bi.config
```

---

## Required Migration

- [ ] Move BI command/view/facade types to `api`.
- [ ] Move `Bi*DO` and `Bi*Mapper` classes to `adapter.persistence`.
- [ ] Keep dashboard/chart/dataset business rules in domain or domain policy.
- [ ] Keep resource permission and collaboration rules out of controllers.
- [ ] Expose stable read API views for `canvas-web`.
- [ ] Do not directly import CDP or warehouse persistence.

---

## Acceptance Tests

Run:

```bash
cd backend
mvn test -pl canvas-context-bi
cd ..
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

Expected:

```text
BI module compiles
dataset/chart/dashboard/permission tests pass
domain has no MyBatis/Spring Web dependency
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
