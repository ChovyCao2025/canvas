# Task Pack 04: CDP Worker

**Worker:** `cdp-worker`

**Goal:** Rewrite CDP profile, tag, audience, event, and warehouse governance
behavior into `canvas-context-cdp`.

---

## Allowed Write Scope

```text
backend/canvas-context-cdp/**
```

## Read Scope

```text
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
```

## Forbidden Changes

```text
backend/pom.xml
backend/canvas-common/**
backend/canvas-web/**
backend/canvas-boot/**
backend/canvas-context-bi/**
backend/canvas-context-execution/**
```

---

## Target Packages

```text
org.chovy.canvas.cdp.api
org.chovy.canvas.cdp.application
org.chovy.canvas.cdp.domain
org.chovy.canvas.cdp.adapter.persistence
org.chovy.canvas.cdp.adapter.external
org.chovy.canvas.cdp.adapter.messaging
org.chovy.canvas.cdp.config
```

---

## Required API

```text
AudienceSnapshotFacade
CustomerProfileLookupPort
CdpTagFacade
CdpEventIngestionFacade
CdpWarehouseReadinessFacade
```

---

## Required Migration

- [ ] Move audience/profile/tag persistence to `adapter.persistence`.
- [ ] Move Doris-specific access to `adapter.external` unless it is internal
      persistence for the CDP module.
- [ ] Expose audience lookup for execution through `api`.
- [ ] Preserve warehouse readiness and incident behavior.
- [ ] Do not let execution import CDP mappers or data objects.

---

## Acceptance Tests

Run:

```bash
cd backend
mvn test -pl canvas-context-cdp
cd ..
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

Expected:

```text
CDP module compiles
audience/tag/profile tests pass
domain has no MyBatis/Doris client/Redis dependency
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
