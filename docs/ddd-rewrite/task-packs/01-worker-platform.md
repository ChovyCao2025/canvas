# Task Pack 01: Platform Worker

**Worker:** `platform-worker`

**Goal:** Rewrite platform governance behavior into `canvas-platform` using the
standard DDD module layout.

---

## Allowed Write Scope

```text
backend/canvas-platform/**
```

## Read Scope

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/platform/**
backend/canvas-engine/src/main/java/org/chovy/canvas/architecture/**
backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture/**
backend/canvas-engine/src/test/java/org/chovy/canvas/platform/**
```

## Forbidden Changes

```text
backend/pom.xml
backend/canvas-common/**
backend/canvas-web/**
backend/canvas-boot/**
backend/canvas-context-*/**
```

---

## Target Packages

```text
org.chovy.canvas.platform.api
org.chovy.canvas.platform.application
org.chovy.canvas.platform.domain
org.chovy.canvas.platform.adapter.persistence
org.chovy.canvas.platform.config
```

---

## Required Migration

- [ ] Move workstream commands/views/facade to `api`.
- [ ] Move workstream orchestration to `application`.
- [ ] Keep workstream key validation and readiness rule in `domain`.
- [ ] Move JDBC/MyBatis persistence implementations to `adapter.persistence`.
- [ ] Move technical migration candidate behavior into the platform module.
- [ ] Preserve existing behavior from platform tests.

---

## Acceptance Tests

Run:

```bash
cd backend
mvn test -pl canvas-platform
cd ..
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

Expected:

```text
platform module compiles
ported platform tests pass
domain package has no MyBatis or Spring Web dependency
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
