# DDD Rewrite Inventory Guide

This directory tracks the inventory needed before code-writing workers start.
The inventory is the bridge between the architecture documents and executable
subagent task packs.

The current repository snapshot has roughly:

```text
141 backend controllers under org.chovy.canvas.web
279 persistence data objects under org.chovy.canvas.dal.dataobject
278 MyBatis mappers under org.chovy.canvas.dal.mapper
```

That scale is large enough that worker agents must not infer ownership from
memory. Each worker must receive an explicit old-code read scope and a target
ownership rule.

---

## Required Inventory Files

The first execution phase must produce these files:

```text
docs/ddd-rewrite/inventory/http-api-inventory.md
docs/ddd-rewrite/inventory/persistence-ownership.md
docs/ddd-rewrite/inventory/service-ownership.md
docs/ddd-rewrite/inventory/test-ownership.md
docs/ddd-rewrite/inventory/cross-context-dependencies.md
```

The seed ownership rules are in:

```text
docs/ddd-rewrite/inventory/context-ownership-seed.md
```

---

## Inventory Commands

Run these commands from the repository root.

### Controllers

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas/web -name '*Controller.java' \
  | sed 's#backend/canvas-engine/src/main/java/##' \
  | sort
```

### Data Objects

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject -name '*DO.java' \
  | sed 's#backend/canvas-engine/src/main/java/##' \
  | sort
```

### Mappers

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper -name '*Mapper.java' \
  | sed 's#backend/canvas-engine/src/main/java/##' \
  | sort
```

### Domain and Engine Services

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas/domain \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine \
  backend/canvas-engine/src/main/java/org/chovy/canvas/platform \
  backend/canvas-engine/src/main/java/org/chovy/canvas/architecture \
  backend/canvas-engine/src/main/java/org/chovy/canvas/strategy/architecture \
  -name '*.java' \
  | sed 's#backend/canvas-engine/src/main/java/##' \
  | sort
```

### Tests

```bash
find backend/canvas-engine/src/test/java -name '*Test.java' \
  | sed 's#backend/canvas-engine/src/test/java/##' \
  | sort
```

---

## Inventory Row Format

Use this row format for each old class:

```text
old class:
current path:
target module:
target package:
target role:
new class or split:
owning worker:
required tests:
compatibility notes:
coordinator decision:
```

Example:

```text
old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignService

current path:
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/marketing/MarketingCampaignService.java

target module:
  canvas-context-marketing

target package:
  split across api, application, domain, adapter.persistence

target role:
  mixed service migration

new class or split:
  MarketingCampaignFacade
  MarketingCampaignApplicationService
  MarketingCampaign
  CampaignKey
  CampaignStatus
  MarketingCampaignRepository
  MybatisMarketingCampaignRepository

owning worker:
  marketing-worker

required tests:
  MarketingCampaignApplicationServiceTest
  CampaignKeyTest
  MarketingCampaignController compatibility test

compatibility notes:
  preserve status normalization, date validation, list limit behavior, and response shape

coordinator decision:
  marketing owns campaign persistence
```

---

## Inventory Completion Criteria

Inventory is ready when:

- Every controller has a target context and compatibility level.
- Every `*DO` has exactly one owning context.
- Every `*Mapper` has exactly one owning context.
- Every old service is classified as application, domain, adapter, config, or
  deletion candidate.
- Every test has a target module or an explicit replacement plan.
- Ambiguous classes have coordinator decisions.
- Worker task packs reference the inventory rows they are responsible for.
