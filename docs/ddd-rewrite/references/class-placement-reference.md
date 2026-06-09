# Class Placement Reference

This reference defines how to classify existing and new classes during the DDD
modular rewrite.

Use it when deciding where a class belongs, what it should be named, and whether
it should be split.

---

## 1. First Decision: Which Module Owns It?

Choose the module by business ownership.

| Business area | Module |
| --- | --- |
| Canvas draft, version, publish, folder/project assignment | `canvas-context-canvas` |
| DAG runtime, trigger, scheduler, node handlers, execution trace | `canvas-context-execution` |
| Campaign, growth, content, marketing preference, integration contract | `canvas-context-marketing` |
| CDP profile, event, tag, audience, warehouse governance | `canvas-context-cdp` |
| BI dataset, metric, chart, dashboard, portal, subscription, permission | `canvas-context-bi` |
| Risk strategy, risk list, decision, rule hit, audit | `canvas-context-risk` |
| Conversation session, private domain, routing, work item, AI reply | `canvas-context-conversation` |
| Workstream, control plane, architecture evidence, migration candidate | `canvas-platform` |
| HTTP controller and HTTP-only request/response adaptation | `canvas-web` |
| Startup, global config, Flyway, runtime assembly | `canvas-boot` |
| Minimal business-neutral shared primitives | `canvas-common` |

If ownership is unclear, do not put the class in common. Record an explicit
coordinator decision.

---

## 2. Second Decision: Which Package Role?

After choosing the module, classify by role.

| Class responsibility | Package |
| --- | --- |
| Public command/query/view/facade/port used by other modules or web | `api` |
| Use-case orchestration, transaction boundary, tenant/actor handling | `application` |
| Entity, aggregate, value object, domain policy, domain event, repository interface | `domain` |
| MyBatis DO, Mapper, repository implementation, persistence converter | `adapter.persistence` |
| RocketMQ consumer, publisher, message DTO, outbox adapter | `adapter.messaging` |
| HTTP client, Doris adapter, AI provider adapter, third-party client | `adapter.external` |
| Module-local Spring config/properties | `config` |

If one class fits multiple roles, split it.

---

## 3. Class Type Rules

### 3.1 Controller

Target:

```text
canvas-web/src/main/java/org/chovy/canvas/web/<area>/<Name>Controller.java
```

Allowed dependencies:

```text
canvas-common
<context>.api
<context>.application facade when intentionally exposed
Spring WebFlux
```

Forbidden dependencies:

```text
*Mapper
*DO
adapter.persistence
RedisTemplate
RocketMQTemplate
WebClient provider clients
```

### 3.2 Command

Target:

```text
<context>.api.<UseCase>Command
```

Use for:

```text
write request
state transition request
external operation request
```

Rules:

- Command should be stable enough for controller/application use.
- Command should not contain mapper/data-object types.
- Command can be a Java record if immutable input is enough.

### 3.3 View

Target:

```text
<context>.api.<UseCase>View
```

Use for:

```text
read response
application result
cross-context read contract
```

Rules:

- View is not a persistence DO.
- View should not expose internal table structure unless API compatibility
  requires it.

### 3.4 Facade or Port

Target:

```text
<context>.api.<Capability>Facade
<context>.api.<Capability>Port
```

Use facade when:

```text
another module or web needs a stable use-case entry
```

Use port when:

```text
this context needs an external capability implemented by another context or
adapter
```

### 3.5 Application Service

Target:

```text
<context>.application.<Capability>ApplicationService
```

Use for:

```text
transaction
tenant/actor handling
loading domain objects
calling domain behavior
saving via repository
calling external ports
returning API views
```

Do not use for:

```text
large all-purpose service
direct mapper-heavy logic
HTTP request processing
pure domain invariant
```

### 3.6 Entity or Aggregate

Target:

```text
<context>.domain.<BusinessName>
```

Use for:

```text
business object with identity
state transition
invariant protection
lifecycle
```

Examples:

```text
Canvas
CanvasVersion
MarketingCampaign
RiskStrategy
ConversationSession
BiDashboard
AudienceDefinition
```

### 3.7 Value Object

Target:

```text
<context>.domain.<BusinessValue>
```

Use for:

```text
validated identifier
normalized key
money
date range
status-specific value
```

Examples:

```text
CampaignKey
CanvasId
Money
DateRange
RiskSceneKey
ExecutionId
```

Rules:

- Immutable.
- Validates itself.
- No persistence annotations.

### 3.8 Domain Policy

Target:

```text
<context>.domain.<DecisionName>Policy
```

Use for:

```text
business decision that depends on several domain values
readiness checks
eligibility checks
state transition permission
```

Examples:

```text
CanvasPublishPolicy
MarketingCampaignReadinessPolicy
RiskDecisionPolicy
ExecutionResumePolicy
```

### 3.9 Repository Interface

Target:

```text
<context>.domain.<Aggregate>Repository
```

Use for:

```text
loading/saving aggregates
loading domain-owned read models
```

Rules:

- Interface only.
- No MyBatis types.
- No data object types.

### 3.10 Data Object

Target:

```text
<context>.adapter.persistence.<TableMeaning>DO
```

Use for:

```text
table mapping
MyBatis annotations
database column representation
```

Rules:

- Never expose to controller.
- Never expose across modules.
- Never use as domain object.

### 3.11 Mapper

Target:

```text
<context>.adapter.persistence.<TableMeaning>Mapper
```

Use for:

```text
MyBatis mapper
```

Rules:

- Called by repository implementation or persistence query adapter.
- Not called by controller.
- Not called by domain.

### 3.12 Repository Implementation

Target:

```text
<context>.adapter.persistence.Mybatis<Aggregate>Repository
```

Use for:

```text
implementing domain repository with mapper/DO
mapping between domain and persistence
```

### 3.13 External Adapter

Target:

```text
<context>.adapter.external.<Provider>Client
<context>.adapter.external.<Provider>Adapter
```

Use for:

```text
WebClient calls
third-party provider calls
Doris/OLAP access when treated as external system
AI provider calls
```

### 3.14 Messaging Adapter

Target:

```text
<context>.adapter.messaging.<MessageName>Consumer
<context>.adapter.messaging.<MessageName>Publisher
```

Use for:

```text
RocketMQ consumer/listener
message publisher
outbox event publisher
```

---

## 4. Existing Package Migration Map

| Current package | Target |
| --- | --- |
| `org.chovy.canvas.web.*` | `canvas-web` controller packages |
| `org.chovy.canvas.web.bi.*` | `canvas-web` BI controller packages |
| `org.chovy.canvas.web.risk.*` | `canvas-web` risk controller packages |
| `org.chovy.canvas.domain.canvas.*` | `canvas-context-canvas` split across application/domain/adapter |
| `org.chovy.canvas.engine.*` | mostly `canvas-context-execution` |
| `org.chovy.canvas.domain.marketing.*` | `canvas-context-marketing` |
| `org.chovy.canvas.domain.cdp.*` | `canvas-context-cdp` |
| `org.chovy.canvas.domain.warehouse.*` | `canvas-context-cdp` unless BI-owned by inventory |
| `org.chovy.canvas.domain.bi.*` | `canvas-context-bi` |
| `org.chovy.canvas.domain.risk.*` | `canvas-context-risk` |
| `org.chovy.canvas.domain.conversation.*` | `canvas-context-conversation` |
| `org.chovy.canvas.platform.*` | `canvas-platform` |
| `org.chovy.canvas.architecture.*` | `canvas-platform` |
| `org.chovy.canvas.dal.dataobject.*` | owning context `adapter.persistence` |
| `org.chovy.canvas.dal.mapper.*` | owning context `adapter.persistence` |
| `org.chovy.canvas.infrastructure.mq.*` | owning context `adapter.messaging`, usually execution |
| `org.chovy.canvas.infrastructure.redis.*` | owning context adapter, usually execution or boot config |
| `org.chovy.canvas.infrastructure.doris.*` | CDP or BI external/persistence adapter by ownership |
| `org.chovy.canvas.config.*` | `canvas-boot` or module-local `config` |
| `org.chovy.canvas.common.*` | `canvas-common`, only after admission review |

---

## 5. Split Rules for Large Old Services

If an old service has these imports:

```text
Controller DTOs
Mapper
DO
RedisTemplate
RocketMQ
ObjectMapper
Scheduler
Domain validation
```

it must be split.

Split pattern:

```text
HTTP logic -> canvas-web controller/support
transaction/use case -> application service
business state/rules -> domain entity/value object/policy
mapper/DO/query -> adapter.persistence
Redis/MQ/HTTP -> adapter.messaging or adapter.external
```

Example:

```text
Old:
  org.chovy.canvas.domain.canvas.CanvasService

New:
  canvas.application.CanvasDraftApplicationService
  canvas.application.CanvasVersionApplicationService
  canvas.application.CanvasPublishApplicationService
  canvas.application.CanvasQueryApplicationService
  canvas.domain.Canvas
  canvas.domain.CanvasVersion
  canvas.domain.CanvasStateTransitionPolicy
  canvas.domain.CanvasRepository
  canvas.adapter.persistence.MybatisCanvasRepository
```

---

## 6. Cross-Context Class Rules

Allowed cross-context import:

```text
org.chovy.canvas.<other>.api.*
```

Forbidden cross-context imports:

```text
org.chovy.canvas.<other>.domain.*
org.chovy.canvas.<other>.application.*
org.chovy.canvas.<other>.adapter.*
org.chovy.canvas.<other>.config.*
```

Exception:

- `canvas-boot` may depend on modules for runtime assembly.
- Tests may use internal classes only when testing that module itself.

---

## 7. Class Placement Examples

### 7.1 `MarketingCampaignMasterDO`

```text
module:
  canvas-context-marketing

package:
  org.chovy.canvas.marketing.adapter.persistence

reason:
  table mapping owned by marketing context
```

### 7.2 `MarketingCampaignService`

```text
module:
  canvas-context-marketing

target split:
  MarketingCampaignApplicationService
  MarketingCampaign
  MarketingCampaignRepository
  MybatisMarketingCampaignRepository
  MarketingCampaignReadinessPolicy

reason:
  old service mixes use case, business rules, persistence, and mapping
```

### 7.3 `RiskDecisionHandler`

```text
module:
  canvas-context-execution

package:
  org.chovy.canvas.execution.domain.node or execution.application.node

allowed dependency:
  org.chovy.canvas.risk.api.RiskDecisionFacade

reason:
  node runtime belongs to execution; risk rules belong to risk
```

### 7.4 `RiskDecisionRunDO`

```text
module:
  canvas-context-risk

package:
  org.chovy.canvas.risk.adapter.persistence

reason:
  risk owns decision persistence
```

### 7.5 `TenantContext`

```text
module:
  canvas-common

package:
  org.chovy.canvas.common.tenant

reason:
  business-neutral request context used across web/application boundaries
```

### 7.6 `NodeType`

```text
module:
  canvas-context-execution

package:
  org.chovy.canvas.execution.domain

reason:
  node type is execution/canvas runtime language, not generic common language
```

If canvas authoring also needs node type, expose a stable API type or place the
type in the context that owns the language after coordinator decision. Do not
default to common.

---

## 8. Placement Anti-Patterns

Rejected:

```text
canvas-common/src/main/java/.../enums/*
```

Reason:

```text
business enums are not common just because many modules reference them
```

Rejected:

```text
application service imports LambdaQueryWrapper
```

Reason:

```text
SQL query details belong in adapter.persistence
```

Rejected:

```text
controller imports MarketingCampaignMasterDO
```

Reason:

```text
HTTP layer must use API commands/views
```

Rejected:

```text
risk module imports execution.adapter.persistence.ExecutionTraceDO
```

Reason:

```text
cross-context persistence access breaks ownership
```

Rejected:

```text
BaseCrudService<T, M extends BaseMapper<T>>
```

Reason:

```text
generic CRUD abstractions hide business language and encourage technical-layer design
```

---

## 9. Review Questions

Before approving a class placement, ask:

1. Which bounded context owns this language?
2. Is this class part of a public contract or an internal implementation?
3. Does this class make a business decision?
4. Does this class know about a technical system?
5. Would another module need this class, or only its API result?
6. Can this class be tested without Spring?
7. Does the name reveal business meaning or table/framework meaning?
8. Is this class hiding multiple responsibilities that should be split?

If the answers conflict, split the class.

---

## 10. Final Placement Rule

The correct package is the one that makes the dependency direction obvious.

If future readers can understand what a class does and what it is allowed to
depend on just from its module and package, the placement is probably correct.
If they need to open the file to know whether it is a controller, use case,
domain rule, mapper wrapper, or external adapter, the placement is probably
wrong.
