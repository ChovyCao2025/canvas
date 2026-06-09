# Automated Guardrail Checks

This document defines command-level checks for the DDD rewrite. The checks do
not replace review, but they catch common LLM drift quickly.

The script version lives at:

```text
docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh
```

Run from repository root:

```bash
bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
```

The script scans the new DDD rewrite modules when they exist:

```text
backend/canvas-context-*
backend/canvas-platform
backend/canvas-web
backend/canvas-common
backend/canvas-boot
```

It intentionally does not treat the old `backend/canvas-engine` as the target
architecture.

---

## Required Checks

### Check A: Domain Has No Infrastructure Imports

Failure when any file under `..domain..` imports:

```text
com.baomidou
org.springframework.web
org.springframework.data.redis
org.apache.rocketmq
org.springframework.web.reactive.function.client.WebClient
com.fasterxml.jackson.databind.ObjectMapper
```

Reason:

```text
domain must be testable without framework and infrastructure
```

### Check B: Non-Persistence Code Does Not Import DO

Failure when any non-`adapter.persistence` package imports a `*DO` class.

Reason:

```text
data objects are table mappings, not API or domain models
```

### Check C: Non-Persistence Code Does Not Import Mapper

Failure when any non-`adapter.persistence` package imports a `*Mapper` class.

Reason:

```text
mapper access must be isolated behind repository implementations
```

### Check D: Web Does Not Import Persistence Adapter

Failure when `canvas-web` imports:

```text
adapter.persistence
*Mapper
*DO
```

Reason:

```text
web is an HTTP adapter, not a persistence layer
```

### Check E: Contexts Do Not Import Other Context Adapters

Failure when a context imports another context's:

```text
adapter.persistence
adapter.messaging
adapter.external
config
```

Reason:

```text
cross-context collaboration must go through api or ports
```

### Check F: Module Directory Matches Java Package

Failure when a module declares a package outside its ownership prefix:

```text
backend/canvas-context-risk/** -> package org.chovy.canvas.risk...
backend/canvas-context-marketing/** -> package org.chovy.canvas.marketing...
backend/canvas-context-canvas/** -> package org.chovy.canvas.canvas...
backend/canvas-context-execution/** -> package org.chovy.canvas.execution...
backend/canvas-context-cdp/** -> package org.chovy.canvas.cdp...
backend/canvas-context-bi/** -> package org.chovy.canvas.bi...
backend/canvas-context-conversation/** -> package org.chovy.canvas.conversation...
backend/canvas-platform/** -> package org.chovy.canvas.platform...
backend/canvas-web/** -> package org.chovy.canvas.web...
backend/canvas-boot/** -> package org.chovy.canvas.boot...
backend/canvas-common/** -> package org.chovy.canvas.common...
```

Reason:

```text
filesystem module ownership and Java namespace ownership must match
```

### Check G: New Modules Do Not Import Old Engine Internals

Failure when a new module imports old package areas from `canvas-engine`:

```text
org.chovy.canvas.domain
org.chovy.canvas.engine
org.chovy.canvas.dal
org.chovy.canvas.infrastructure
```

Reason:

```text
new modules must own migrated code instead of depending on old internals
```

### Check H: New Module POMs Do Not Depend on `canvas-engine`

Failure when a new DDD rewrite module POM depends on:

```text
<artifactId>canvas-engine</artifactId>
```

Reason:

```text
canvas-engine is the old runtime reference, not a dependency for new contexts
```

### Check I: Common Has No Business Enums

Failure when `canvas-common` contains business-specific enums or packages such
as:

```text
NodeType
CanvasStatus
CampaignStatus
RiskStatus
ApprovalStatus
BiResourceType
```

Reason:

```text
common must stay business-neutral
```

### Check J: No Premature Generic Base Layer

Failure when new modules introduce names such as:

```text
BaseCrudService
GenericRepository
AbstractDomainService
BaseMapperHelper
```

Reason:

```text
generic abstractions hide business language
```

### Check K: Temporary Bridges Are Visible

Advisory when names include:

```text
Legacy
Compatibility
Bridge
```

Required manual review:

```text
owner
removal phase
cutover blocker
replacement path
```

---

## Review Use

Workers should include this in their response:

```text
guardrail checks:
  command: bash docs/ddd-rewrite/guardrails/checks/ddd-guardrail-checks.sh .
  result: PASS
```

If the script is not runnable because the module skeleton does not exist yet,
the worker must say:

```text
guardrail checks:
  not run because <specific reason>
  manual replacement evidence: <specific inspection>
```

Reviewers must reject vague evidence such as:

```text
looks clean
should be fine
no obvious issue
```

---

## Limitations

These checks are intentionally conservative. They do not prove the model is
well-designed. They only catch common structural failures.

Reviewers still need to inspect:

```text
business behavior placement
aggregate size
API compatibility
test coverage scope
temporary bridge removal gates
worker changed-file scope
```
