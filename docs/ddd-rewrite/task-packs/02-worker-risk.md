# Task Pack 02: Risk Worker

**Worker:** `risk-worker`

**Goal:** Rewrite risk strategy, list, decision, simulation, and runtime rule
behavior into `canvas-context-risk`.

---

## Allowed Write Scope

```text
backend/canvas-context-risk/**
```

## Read Scope

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/risk/**
backend/canvas-engine/src/main/java/org/chovy/canvas/web/risk/**
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Risk*DO.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Risk*Mapper.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/risk/**
backend/canvas-engine/src/test/java/org/chovy/canvas/web/risk/**
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

## Target Packages

```text
org.chovy.canvas.risk.api
org.chovy.canvas.risk.application
org.chovy.canvas.risk.domain
org.chovy.canvas.risk.adapter.persistence
org.chovy.canvas.risk.adapter.external
org.chovy.canvas.risk.config
```

---

## Required API

Create or preserve these public contracts:

```text
RiskDecisionFacade
RiskDecisionCommand
RiskDecisionView
RiskStrategyCommand
RiskStrategyView
RiskListCommand
RiskListView
RiskSimulationCommand
RiskSimulationView
```

---

## Required Migration

- [ ] Keep DSL parser and validator as domain code if they do not depend on
      infrastructure.
- [ ] Move Redis feature store to `adapter.external` or `adapter.persistence`
      based on coordinator decision; do not keep it in domain.
- [ ] Move risk data objects and mappers to `adapter.persistence`.
- [ ] Keep decision merge/evaluation policies in `domain`.
- [ ] Expose decision evaluation through `RiskDecisionFacade`.
- [ ] Do not move execution node handlers into risk.

---

## Acceptance Tests

Run:

```bash
cd backend
mvn test -pl canvas-context-risk
cd ..
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

Expected:

```text
risk module compiles
risk DSL tests pass
risk decision tests pass
risk domain has no Redis/MyBatis/Spring Web dependency
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
