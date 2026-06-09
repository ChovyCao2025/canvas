# Task Pack 03: Marketing Worker

**Worker:** `marketing-worker`

**Goal:** Rewrite marketing campaign, growth, content, preferences, provider,
and integration contract behavior into `canvas-context-marketing`.

---

## Allowed Write Scope

```text
backend/canvas-context-marketing/**
```

## Read Scope

```text
backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/**
backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/growth/**
backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/**
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Marketing*DO.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/Growth*DO.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Marketing*Mapper.java
backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/Growth*Mapper.java
backend/canvas-engine/src/test/java/org/chovy/canvas/domain/marketing/**
```

`Channel*` and `Message*` persistence may be added only after
`docs/ddd-rewrite/inventory/persistence-ownership.md` assigns exact rows to
marketing. They are ambiguous in the seed inventory and are not included by
default.

## Forbidden Changes

```text
backend/pom.xml
backend/canvas-common/**
backend/canvas-web/**
backend/canvas-boot/**
backend/canvas-context-cdp/**
backend/canvas-context-execution/**
```

---

## Target Packages

```text
org.chovy.canvas.marketing.api
org.chovy.canvas.marketing.application
org.chovy.canvas.marketing.domain
org.chovy.canvas.marketing.adapter.persistence
org.chovy.canvas.marketing.adapter.external
org.chovy.canvas.marketing.adapter.messaging
org.chovy.canvas.marketing.config
```

---

## Required Pilot Slice

Implement the marketing campaign slice first, following:

```text
docs/ddd-rewrite/child-specs/marketing-pilot-spec.md
```

Required split:

```text
MarketingCampaignFacade
MarketingCampaignApplicationService
MarketingCampaign
CampaignKey
CampaignStatus
CampaignBudget
MarketingCampaignRepository
MybatisMarketingCampaignRepository
MarketingCampaignPersistenceConverter
MarketingCampaignReadinessPolicy
```

---

## Required Migration

- [ ] Move command/view types to `api`.
- [ ] Split old services into application, domain, and adapters.
- [ ] Move HTTP probe client to `adapter.external`.
- [ ] Move all marketing-owned DO/Mapper classes to `adapter.persistence`.
- [ ] Preserve campaign status normalization and readiness behavior.
- [ ] Keep growth reward/activity rules in domain/application, not controllers.
- [ ] Do not directly depend on CDP, BI, canvas, or execution persistence.

---

## Acceptance Tests

Run:

```bash
cd backend
mvn test -pl canvas-context-marketing
cd ..
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

Expected:

```text
marketing module compiles
campaign tests pass
integration contract tests pass
domain has no MyBatis/WebFlux/Redis/RocketMQ dependency
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
