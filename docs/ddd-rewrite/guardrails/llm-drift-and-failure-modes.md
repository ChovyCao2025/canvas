# LLM Drift and Failure Modes

This document defines failure modes for the DDD modular rewrite. A worker or
reviewer must treat these as hard failure conditions unless the coordinator has
approved a temporary bridge in writing.

Each failure mode includes:

```text
symptom
why it matters
failure evidence
required correction
```

---

## LLM-001: Package-Only Rewrite

Symptom:

```text
An old service is copied into a new package with minimal internal change.
```

Why it matters:

```text
The rewrite appears complete but preserves the same mixed responsibilities.
```

Failure evidence:

```text
application service still imports multiple mappers
application service still uses Redis/MQ/WebClient directly
application service still contains state transition rules, persistence queries,
JSON database serialization, and external side effects in one class
```

Required correction:

```text
split into application service, domain entity/policy/value object, repository
interface, persistence adapter, and external/messaging adapter
```

---

## LLM-002: DO as Domain Model

Symptom:

```text
*DO is used outside adapter.persistence.
```

Why it matters:

```text
Persistence shape leaks into web, application, or domain and prevents true
domain modeling.
```

Failure evidence:

```text
controller imports *DO
application imports *DO
domain imports *DO
api command/view exposes *DO
```

Required correction:

```text
map DO to domain object or API view inside adapter.persistence or application
boundary
```

---

## LLM-003: Mapper Penetration

Symptom:

```text
Controller, domain, or cross-context application code imports *Mapper.
```

Why it matters:

```text
Direct mapper access bypasses repository ports and data ownership.
```

Failure evidence:

```text
*Controller imports *Mapper
..domain.. imports *Mapper
one context imports another context's mapper
application service uses LambdaQueryWrapper directly
```

Required correction:

```text
move mapper access to owning context adapter.persistence and call a repository
or facade
```

---

## LLM-004: Common Dumping Ground

Symptom:

```text
Business enums, business services, or broad helpers are moved to canvas-common.
```

Why it matters:

```text
common becomes the new shared monolith and hides context ownership.
```

Failure evidence:

```text
CanvasStatusEnum, NodeType, CampaignStatus, RiskStatus, BiResourceType, or
ApprovalStatus placed in canvas-common without coordinator approval
CommonDomainService or CommonBusinessService introduced
```

Required correction:

```text
move business language back to the owning context; expose only stable API values
when another context needs them
```

---

## LLM-005: Fake Rich Domain Model

Symptom:

```text
Domain classes have fields and accessors, but business behavior remains in the
application service.
```

Why it matters:

```text
The new structure looks like DDD but preserves an anemic model where services
own all rules.
```

Failure evidence:

```text
core aggregate has no meaningful methods
state transitions are implemented as if/else blocks in application service
readiness policies are implemented in controller or application service
value objects do not validate their values
```

Required correction:

```text
move invariants to entities/value objects/policies and keep application service
as orchestration
```

---

## LLM-006: Over-Generic Abstraction

Symptom:

```text
Generic base services or repositories are introduced before concrete duplication
is proven.
```

Why it matters:

```text
Technical abstraction erases business language and recreates layer-first design.
```

Failure evidence:

```text
BaseCrudService
GenericRepository
AbstractDomainService
BaseMapperHelper
```

Required correction:

```text
use explicit context-specific services until at least three stable duplicated
patterns prove an abstraction is worthwhile
```

---

## LLM-007: Cross-Context Persistence Access

Symptom:

```text
One context imports another context's adapter.persistence package.
```

Why it matters:

```text
This breaks bounded-context data ownership and creates hidden coupling.
```

Failure evidence:

```text
execution imports risk.adapter.persistence
risk imports execution.adapter.persistence
marketing imports cdp.adapter.persistence
web imports any adapter.persistence
```

Required correction:

```text
use the owning context api/facade/port or a domain event
```

---

## LLM-008: Temporary Bridge Without Removal Gate

Symptom:

```text
Legacy or compatibility adapters are introduced without removal criteria.
```

Why it matters:

```text
Temporary bridges tend to become permanent and preserve the old architecture.
```

Failure evidence:

```text
class name includes Legacy or Compatibility but has no owner and removal phase
bridge is used in Phase 10 cutover path
architecture tests exclude bridge package without written coordinator approval
```

Required correction:

```text
document owner, removal phase, blocked cutover condition, and replacement path
```

---

## LLM-009: Architecture Correct, Behavior Broken

Symptom:

```text
New modules look clean but old API behavior changed unintentionally.
```

Why it matters:

```text
The first milestone requires behavioral compatibility.
```

Failure evidence:

```text
contract test fails
response envelope changes
status normalization changes
default value changes
tenant scoping changes
existing frontend test fails due to API shape change
```

Required correction:

```text
restore old behavior for first cutover or create a separate approved behavior
change spec
```

---

## LLM-010: Worker Write-Scope Violation

Symptom:

```text
A worker edits files outside its assigned task pack scope.
```

Why it matters:

```text
Parallel execution becomes unsafe and integration conflicts multiply.
```

Failure evidence:

```text
marketing worker edits canvas-common
risk worker edits backend/pom.xml
cdp worker edits canvas-web
execution worker edits canvas-context-canvas contract without coordinator task
```

Required correction:

```text
reject out-of-scope changes, ask coordinator to make shared edits, and rerun
worker task with narrowed scope
```

---

## LLM-011: Contract Guessing

Symptom:

```text
A worker invents cross-context API fields or semantics without using the child
spec or inventory.
```

Why it matters:

```text
Independent workers will build incompatible APIs.
```

Failure evidence:

```text
execution and canvas define different published definition fields
risk decision command differs between execution node and risk module
CDP audience facade fields do not match execution usage
```

Required correction:

```text
stop implementation, update the child spec or API contract, then resume workers
```

---

## LLM-012: Test Substitution

Symptom:

```text
Worker reports narrow unit tests as proof of broad compatibility.
```

Why it matters:

```text
The rewrite can pass local tests while breaking API or runtime behavior.
```

Failure evidence:

```text
only domain tests run for a controller/API migration
only compile run for a behavior-sensitive migration
contract test plan not executed before cutover
```

Required correction:

```text
run tests matching the scope: domain, application, adapter, web contract, and
smoke tests as required by the task pack
```

---

## LLM-013: Old Runtime Dependency Retained

Symptom:

```text
New modules compile only because they still depend on canvas-engine internals.
```

Why it matters:

```text
The new architecture is not independent and cutover cannot happen.
```

Failure evidence:

```text
new canvas-context-* module imports org.chovy.canvas.domain.* from canvas-engine
new module POM depends on canvas-engine as a shortcut
canvas-boot starts through old CanvasEngineApplication
```

Required correction:

```text
move the owned code into the new module or define an approved temporary
compatibility bridge with removal gate
```

---

## LLM-014: Demo or Open-Source Growth Contamination

Symptom:

```text
Open-source growth features are mixed into the DDD rewrite core.
```

Why it matters:

```text
The DDD rewrite is a structural compatibility project. Product-growth features
expand the blast radius and obscure architecture validation.
```

Failure evidence:

```text
plugin marketplace, DSL feature expansion, README product work, or playground
changes appear inside a DDD context worker task
```

Required correction:

```text
move product-growth work to the open-source-growth plan or schedule it after the
DDD boundary is stable
```

---

## Final Failure Rule

If any failure mode is present, the worker cannot return `DONE`. It must return
`DONE_WITH_CONCERNS`, `NEEDS_CONTEXT`, or `BLOCKED`, depending on whether the
issue was fixed, needs coordinator input, or cannot be resolved inside the
assigned scope.
