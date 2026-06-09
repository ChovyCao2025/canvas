# Rich Domain Model Reference

This reference explains how to recognize and implement a rich domain model
during the DDD modular rewrite.

It is guidance for reviewers, implementers, and subagents. It does not replace
the rewrite spec or plan.

---

## 1. Definition

A rich domain model is a model where business objects own meaningful business
behavior and protect their own invariants.

In a rich model:

- Entities are not just field containers.
- Value objects validate and normalize their own values.
- Aggregates protect consistency boundaries.
- Domain policies express pure business decisions.
- Application services orchestrate use cases but do not own all business rules.
- Persistence adapters save and load models but do not decide business behavior.

In short:

```text
domain = business meaning and business rules
application = use-case orchestration
adapter = technical detail
```

---

## 2. Anemic Model vs Rich Model

### Anemic Model

An anemic model usually looks like this:

```text
MarketingCampaignDO
  fields only: status, startAt, endAt, budgetAmount

MarketingCampaignService
  validates status
  validates date range
  validates budget
  decides whether campaign can launch
  writes mapper
  serializes JSON
```

The object is only data. The service owns nearly all behavior.

This is common in CRUD systems, but it becomes hard to maintain when the domain
has real rules, state transitions, and cross-feature invariants.

### Rich Model

A richer model separates responsibilities:

```text
MarketingCampaign
  owns identity and campaign lifecycle
  exposes activate(), archive(), reschedule()
  protects campaign-specific invariants

CampaignKey
  validates and normalizes campaign key

CampaignBudget
  validates amount and currency

MarketingCampaignReadinessPolicy
  evaluates launch readiness from campaign and linked resources

MarketingCampaignApplicationService
  opens transaction
  loads campaign
  calls domain behavior
  saves through repository
```

The service still exists, but it is no longer the only place where business
logic lives.

---

## 3. What Belongs in Domain

Put logic in domain when it answers a business question without needing a
technical system.

Examples:

```text
Can this canvas be published?
Can this campaign be activated?
Is this risk strategy effective?
Is this budget valid for this campaign?
Does this date range make sense?
Is this resource required for launch readiness?
Can this execution be resumed from WAITING?
```

Good domain methods:

```text
Canvas.publish(version, actor)
Canvas.archive(actor)
CanvasVersion.markPublished()
MarketingCampaign.activate()
MarketingCampaign.reschedule(dateRange)
RiskStrategy.enable()
RiskStrategyVersion.publish()
ExecutionRun.markWaiting(waitSubscription)
```

Good value objects:

```text
TenantId
CanvasId
CampaignKey
ExecutionId
Money
DateRange
RiskSceneKey
AudienceSnapshotId
```

Good domain policies:

```text
CanvasPublishPolicy
MarketingCampaignReadinessPolicy
RiskDecisionPolicy
AudienceEligibilityPolicy
ExecutionResumePolicy
```

---

## 4. What Does Not Belong in Domain

Do not put technical behavior in domain.

Forbidden domain dependencies:

```text
Spring Web
WebFlux request/response types
MyBatis Mapper
MyBatis LambdaQueryWrapper
Persistence DO
RedisTemplate
RocketMQ listener/publisher
WebClient
Doris JDBC client
ObjectMapper for database JSON columns
```

Forbidden domain behavior:

```text
building SQL query wrappers
serializing database JSON columns
publishing MQ messages
calling HTTP providers
resolving current HTTP tenant context
using Spring Security request state
writing audit tables directly
updating Redis route keys directly
```

If domain needs an external effect, define a port and let application/adapters
provide the implementation.

---

## 5. Aggregate Size Rule

Aggregates should be small consistency boundaries, not large object graphs.

Prefer:

```text
Canvas
CanvasVersion
MarketingCampaign
MarketingCampaignLink
RiskStrategy
RiskList
ConversationSession
BiDashboard
```

Avoid:

```text
MarketingPlatformAggregate containing campaign, content, CDP, BI, risk, conversation
CanvasAggregate containing all execution traces and all user responses
TenantAggregate containing every tenant-owned object
```

Rule:

- If two objects must change atomically to protect a business invariant, they
  may belong in the same aggregate.
- If they are only displayed together, keep them separate and compose in a query
  service.
- If they belong to different bounded contexts, do not force them into one
  aggregate.

---

## 6. Application Service Boundary

Application services are still necessary.

They own:

```text
transaction boundary
tenant and actor input
authorization checks
loading aggregates
calling domain behavior
saving through repositories
calling ports/facades
returning API views
```

They should not own:

```text
all state transition rules
all validation rules
all readiness policies
all status normalization
all domain object construction details
```

Application service smell:

```text
method has 200 lines
method imports multiple mappers
method mutates many unrelated tables
method contains many if/else blocks about business state
method directly builds HTTP/MQ/Redis side effects
```

When this happens, split:

```text
business rule -> domain entity/value object/policy
persistence -> repository adapter
external call -> port + external adapter
workflow orchestration -> application service
```

---

## 7. Rich Model Review Checklist

Use this checklist during review.

- [ ] Does the domain object have behavior, not only getters/setters?
- [ ] Are business invariants protected by entity/value object/policy?
- [ ] Can the domain behavior be unit tested without Spring?
- [ ] Does the domain avoid MyBatis, Redis, MQ, WebClient, and Controller types?
- [ ] Does the application service call domain behavior instead of duplicating
      domain rules inline?
- [ ] Does the persistence adapter map between DO and domain?
- [ ] Are value objects immutable?
- [ ] Are aggregate boundaries small enough to reason about?
- [ ] Are cross-context decisions made through API/ports rather than shared
      persistence?

---

## 8. Drift Risks With Large Models or Long Implementations

The rewrite can drift even when the initial spec is good.

Common drift:

```text
old service copied into application package unchanged
DO reused as domain object
mapper injected into domain or controller
business enum moved into common for convenience
BaseService or GenericRepository introduced too early
cross-context mapper access added to "save time"
temporary bridge kept permanently
worker changes shared files outside assigned scope
```

Mitigations:

```text
architecture tests
class placement reference
worker write-scope limits
worker output template
spec compliance review
code quality review
coordinator-only integration
temporary bridge removal phase
```

The key operating principle is to reduce freedom where consistency matters.

---

## 9. Minimum Richness Standard

For each context, at least the central business concepts should have real domain
types.

Examples:

```text
canvas-context-canvas
  Canvas
  CanvasVersion
  CanvasStateTransitionPolicy

canvas-context-execution
  ExecutionRun
  ExecutionStatus
  ExecutionResumePolicy

canvas-context-marketing
  MarketingCampaign
  CampaignKey
  CampaignBudget
  MarketingCampaignReadinessPolicy

canvas-context-risk
  RiskStrategy
  RiskList
  RiskDecisionPolicy

canvas-context-bi
  BiDashboard
  BiDataset
  BiResourcePermissionPolicy

canvas-context-cdp
  AudienceDefinition
  AudienceSnapshot
  CdpTagDefinition

canvas-context-conversation
  ConversationSession
  ConversationRoutingRule
  ConversationSlaPolicy
```

Not every table needs a rich aggregate. Ledger rows, audit rows, trace rows, and
read-only projections can remain persistence/read models.

---

## 10. Practical Rule

Do not over-model simple CRUD. Do not under-model business-critical workflows.

Use a rich model when the code has:

```text
state transitions
business invariants
readiness checks
eligibility decisions
cross-field validation
policies that product/business people discuss
rules likely to change independently from storage
```

Use simple application/read model code when the code is:

```text
plain listing
simple lookup
audit history read
append-only ledger read
admin metadata view
```

The rewrite should be richer where the domain is real, and boring where the
domain is just data access.
