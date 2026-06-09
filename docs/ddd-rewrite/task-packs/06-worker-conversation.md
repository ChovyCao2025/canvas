# Task Pack 06: Conversation Worker

**Worker:** `conversation-worker`

**Goal:** Rewrite conversation sessions, private domain contacts/groups,
routing, work items, SOP, and AI reply behavior into
`canvas-context-conversation`.

---

## Allowed Write Scope

```text
backend/canvas-context-conversation/**
```

## Read Scope

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/conversation/**
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Conversation*DO.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Conversation*Mapper.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/conversation/**
```

## Forbidden Changes

```text
backend/pom.xml
backend/canvas-common/**
backend/canvas-web/**
backend/canvas-boot/**
backend/canvas-context-marketing/**
```

---

## Target Packages

```text
org.chovy.canvas.conversation.api
org.chovy.canvas.conversation.application
org.chovy.canvas.conversation.domain
org.chovy.canvas.conversation.adapter.persistence
org.chovy.canvas.conversation.adapter.external
org.chovy.canvas.conversation.adapter.messaging
org.chovy.canvas.conversation.config
```

---

## Required Migration

- [ ] Move conversation command/view/facade types to `api`.
- [ ] Move `Conversation*DO` and `Conversation*Mapper` classes to
      `adapter.persistence`.
- [ ] Keep routing and SLA policies in domain.
- [ ] Move provider webhook translation to application or external adapter.
- [ ] Keep AI provider calls in `adapter.external`.
- [ ] Do not directly import marketing message persistence.

---

## Acceptance Tests

Run:

```bash
cd backend
mvn test -pl canvas-context-conversation
cd ..
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

Expected:

```text
conversation module compiles
session/routing/work item tests pass
domain has no MyBatis/WebClient/Spring Web dependency
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
