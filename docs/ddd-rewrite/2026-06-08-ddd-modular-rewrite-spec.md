# DDD Modular Rewrite Specification

**Date:** 2026-06-08

**Scope:** Backend architecture rewrite for the Marketing Canvas platform.

**Repository:** `/Users/photonpay/project/canvas`

**Decision:** Build a new DDD-style modular monolith in parallel with the
current `canvas-engine`, then cut over when contracts, tests, and runtime checks
prove compatibility.

---

## 1. Purpose

The current backend has grown into a mixed structure: Spring Web controllers,
domain-like services, MyBatis data objects, mapper interfaces, execution engine
code, Redis/MQ adapters, and platform governance code all live inside
`backend/canvas-engine`. Some packages already use `domain/*`, but the boundary
is not enforceable because services can still depend directly on global
`dal.mapper`, `dal.dataobject`, Redis, schedulers, and execution components.

The rewrite must create enforceable module boundaries:

- Business contexts are separated as Maven modules.
- Each context owns its persistence adapter.
- Domain code is isolated from framework and infrastructure dependencies.
- Controllers do not access persistence classes.
- Cross-context collaboration happens through API packages, application
  facades, ports, or events, not through another context's mapper or data
  object.

The first rewrite milestone must preserve observable behavior. API routes,
request/response shapes, database tables, and existing Flyway history should
remain compatible unless a later migration spec explicitly changes them.

---

## Dispatch Scope Clarification

This spec describes the target architecture and source inventory. It is not a
worker dispatch document.

When a worker is dispatched, write scope, gate evidence, and stop conditions are
controlled by:

```text
docs/program-coordination/subagent-worker-packets.md
docs/program-coordination/gate-verification-matrix.md
docs/ddd-rewrite/guardrails/README.md
```

All "Moves from current packages" lists below identify source code to inventory.
They do not authorize a context worker to edit `backend/canvas-web/**`,
`backend/canvas-boot/**`, root Maven, or another context module. Controller
migration to `canvas-web` is coordinator-owned in `DDD-C09` unless a worker
packet explicitly says otherwise.

---

## 2. Architecture Principles

### 2.1 Strategic DDD

The system is divided by bounded context, not by technical layer. A bounded
context owns its vocabulary, rules, persistence mapping, and application
services.

Recommended top-level contexts:

- `canvas`: canvas draft, versioning, publish lifecycle, project/folder
  assignment.
- `execution`: DAG runtime, node handlers, triggers, scheduling, wait/resume,
  execution traces.
- `marketing`: campaign, growth activities, marketing content, preferences,
  integration contracts, provider write readiness.
- `cdp`: user profiles, tags, audiences, warehouse readiness, realtime and
  batch CDP governance.
- `bi`: datasets, metrics, dashboards, portals, charts, subscriptions, resource
  permissions.
- `risk`: risk strategies, lists, decisions, hit records, audit views.
- `conversation`: sessions, private domain contacts/groups, routing, work
  items, AI reply suggestions.
- `platform`: platform control plane, workstreams, technical migration
  candidates, architecture/governance evidence.

### 2.2 Tactical DDD

Use tactical patterns only where they simplify behavior and ownership:

- Entity: object with stable identity and lifecycle, such as `Canvas`,
  `CanvasVersion`, `MarketingCampaign`, `RiskStrategy`.
- Value Object: immutable domain value, such as `TenantId`, `CampaignKey`,
  `Money`, `DateRange`, `CanvasId`, `ExecutionId`.
- Aggregate: consistency boundary that protects invariants. Keep aggregates
  small.
- Domain Service: pure business logic that does not naturally belong to one
  entity.
- Repository Interface: domain/application-facing contract for storing and
  loading aggregates or read models.
- Application Service: use-case orchestration, transaction boundary, tenant and
  actor handling, calls to repositories and external ports.
- Adapter: MyBatis, Redis, MQ, HTTP, Doris, AI provider, or third-party
  integration implementation.

### 2.3 Modular Monolith First

The rewrite must not jump directly to microservices. Maven modules and
dependency tests provide the first boundary. Service extraction can be evaluated
only after module boundaries and data ownership are stable.

---

## 3. Target Maven Modules

```text
backend/
  pom.xml
  canvas-common/
  canvas-cache-sdk/
  canvas-context-canvas/
  canvas-context-execution/
  canvas-context-marketing/
  canvas-context-cdp/
  canvas-context-bi/
  canvas-context-risk/
  canvas-context-conversation/
  canvas-platform/
  canvas-web/
  canvas-boot/
  canvas-flink-jobs/
```

### 3.1 Module Responsibilities

| Module | Responsibility |
| --- | --- |
| `canvas-common` | Minimal shared primitives: result envelope, page result, tenant context, validation helpers, common exceptions. |
| `canvas-cache-sdk` | Existing reusable tiered cache SDK. Keep independent from business contexts. |
| `canvas-context-canvas` | Canvas authoring, draft, version, publish state, project/folder assignment. |
| `canvas-context-execution` | Runtime execution, DAG parsing, node handlers, triggers, scheduling, wait/resume, trace. |
| `canvas-context-marketing` | Campaign, growth, content, marketing preferences, integration contract readiness. |
| `canvas-context-cdp` | Profiles, events, tags, audiences, warehouse, realtime/batch governance. |
| `canvas-context-bi` | BI workspace, dataset, metric, chart, dashboard, portal, subscription, permission. |
| `canvas-context-risk` | Strategy, list, decision, hit, audit, budget/rule evaluation. |
| `canvas-context-conversation` | Conversation sessions, private domain, routing, AI suggestions, work items. |
| `canvas-platform` | Control plane, workstreams, technical migration candidates, architecture evidence. |
| `canvas-web` | HTTP controllers, request/response adapters, auth context resolution, response envelope. |
| `canvas-boot` | Spring Boot application, configuration, Flyway resources, component scanning, runtime assembly. |
| `canvas-flink-jobs` | Existing Flink jobs. Keep as separate runtime module. |

### 3.2 Dependency Direction

Allowed direction:

```text
canvas-boot
  -> canvas-web
  -> all context modules
  -> canvas-cache-sdk
  -> canvas-common

canvas-web
  -> context api/application facade
  -> canvas-common

context application
  -> same-context domain
  -> same-context api
  -> other-context api only when needed
  -> canvas-common

context adapter
  -> same-context domain/application ports
  -> framework/infrastructure libraries

context domain
  -> JDK
  -> canvas-common primitives only when unavoidable
```

Forbidden direction:

```text
domain -> Spring Web
domain -> MyBatis / mapper / data object
domain -> Redis / RocketMQ / WebClient / Doris client
web -> mapper / data object / adapter.persistence
one context -> another context's adapter package
one context -> another context's persistence classes
```

---

## 4. Package Layout Rules

Every bounded context uses the same internal package layout.

The detailed class placement decision table, naming examples, migration
patterns, and anti-patterns are maintained in
[`2026-06-08-ddd-rewrite-conventions-and-examples.md`](./2026-06-08-ddd-rewrite-conventions-and-examples.md).
Workers must use that document when deciding where a class belongs.

```text
org.chovy.canvas.<context>
  api/
  application/
  domain/
  adapter/
    persistence/
    messaging/
    external/
  config/
```

### 4.1 `api`

Contains only stable types other modules or `canvas-web` may use.

Allowed:

- Command records/classes.
- Query records/classes.
- View/read model records/classes.
- Facade interfaces.
- Cross-context ports.
- Domain events intended for public consumption.

Not allowed:

- MyBatis mapper.
- Data object.
- Spring controller.
- Redis/MQ implementation.
- Internal domain helper.

Example:

```text
org.chovy.canvas.marketing.api.MarketingCampaignCommand
org.chovy.canvas.marketing.api.MarketingCampaignView
org.chovy.canvas.marketing.api.MarketingCampaignFacade
```

### 4.2 `application`

Contains use-case orchestration.

Allowed:

- Application services.
- Transaction boundaries.
- Tenant and actor checks.
- Calls to repositories and ports.
- Mapping from API command to domain command/value object.

Not allowed:

- HTTP request/response handling.
- MyBatis query construction.
- Redis key manipulation.
- RocketMQ listener implementation.
- Large business invariants that belong in domain.

Naming:

```text
<Capability>ApplicationService
<Capability>CommandHandler
<Capability>QueryService
```

Example:

```text
org.chovy.canvas.canvas.application.CanvasPublishApplicationService
org.chovy.canvas.execution.application.CanvasExecutionApplicationService
org.chovy.canvas.marketing.application.MarketingCampaignApplicationService
```

### 4.3 `domain`

Contains the business model and business rules.

Allowed:

- Aggregate roots.
- Entities.
- Value objects.
- Domain services.
- Domain policies.
- Repository interfaces.
- Domain events.

Not allowed:

- Spring stereotype annotations.
- MyBatis annotations.
- Data objects.
- JSON parsing for database columns.
- Redis/MQ/HTTP client usage.

Naming:

```text
<Aggregate>
<Aggregate>Id
<BusinessValue>
<BusinessPolicy>
<Aggregate>Repository
```

Example:

```text
org.chovy.canvas.marketing.domain.MarketingCampaign
org.chovy.canvas.marketing.domain.CampaignKey
org.chovy.canvas.marketing.domain.MarketingCampaignReadinessPolicy
org.chovy.canvas.marketing.domain.MarketingCampaignRepository
```

### 4.4 `adapter.persistence`

Contains persistence implementation details owned by the context.

Allowed:

- MyBatis mapper interfaces.
- MyBatis data objects.
- Repository implementations.
- Persistence converters.
- SQL-specific query helpers.

Not allowed:

- Domain invariants.
- HTTP response classes.
- Public API types unless used only as mapping target.

Naming:

```text
<TableMeaning>DO
<TableMeaning>Mapper
Mybatis<Aggregate>Repository
<Aggregate>PersistenceConverter
```

Example:

```text
org.chovy.canvas.marketing.adapter.persistence.MarketingCampaignMasterDO
org.chovy.canvas.marketing.adapter.persistence.MarketingCampaignMasterMapper
org.chovy.canvas.marketing.adapter.persistence.MybatisMarketingCampaignRepository
```

### 4.5 `adapter.messaging`

Contains MQ and event transport implementations.

Allowed:

- RocketMQ consumers.
- RocketMQ publishers.
- Message DTOs.
- Outbox implementations.

Naming:

```text
<Event>Consumer
<Event>Publisher
<Event>Message
```

### 4.6 `adapter.external`

Contains external system integration.

Allowed:

- WebClient clients.
- Doris adapters.
- Third-party marketing provider clients.
- AI provider clients.
- Anti-corruption layer translators.

Naming:

```text
Http<Provider>Client
Jdbc<ExternalSystem>Adapter
<Provider>Translator
```

### 4.7 `config`

Contains module-local Spring configuration.

Allowed:

- Configuration properties.
- Bean wiring that is local to the module.
- Mapper scan markers if needed.

Not allowed:

- Business rules.
- Runtime orchestration.

---

## 5. Context Ownership Matrix

### 5.1 Canvas Context

Owns:

- Canvas draft lifecycle.
- Canvas version lifecycle.
- Publish/offline/archive state.
- Project/folder assignment.
- Canvas pre-publish validation.
- Published canvas definition as an API contract to execution.

Moves from current packages:

```text
org.chovy.canvas.domain.canvas.*
org.chovy.canvas.dto.canvas.*
org.chovy.canvas.query.CanvasListQuery
Canvas-related data objects and mappers from org.chovy.canvas.dal.*
```

Primary application services:

```text
CanvasDraftApplicationService
CanvasVersionApplicationService
CanvasPublishApplicationService
CanvasQueryApplicationService
CanvasProjectFolderApplicationService
```

### 5.2 Execution Context

Owns:

- DAG graph parsing and runtime representation.
- Node handler registry and node handler execution.
- Trigger pre-check.
- Trigger routing.
- Scheduler registration.
- Wait/resume lifecycle.
- Execution trace and recovery.

Moves from current packages:

```text
org.chovy.canvas.engine.dag.*
org.chovy.canvas.engine.handler.*
org.chovy.canvas.engine.handlers.*
org.chovy.canvas.engine.trigger.*
org.chovy.canvas.engine.schedule.*
org.chovy.canvas.engine.wait.*
org.chovy.canvas.engine.trace.*
Execution-related data objects and mappers from org.chovy.canvas.dal.*
```

Primary application services:

```text
CanvasExecutionApplicationService
CanvasTriggerApplicationService
CanvasSchedulerApplicationService
ExecutionRecoveryApplicationService
```

### 5.3 Marketing Context

Owns:

- Marketing campaigns.
- Growth activities and tasks.
- Marketing content lifecycle.
- Marketing preferences.
- Integration contracts and probes.
- Provider-write readiness.

Moves from current packages:

```text
org.chovy.canvas.domain.marketing.*
org.chovy.canvas.strategy.growth.*
Marketing-related controllers in org.chovy.canvas.web.*
Marketing-related data objects and mappers from org.chovy.canvas.dal.*
```

Primary application services:

```text
MarketingCampaignApplicationService
GrowthActivityApplicationService
MarketingContentApplicationService
MarketingIntegrationContractApplicationService
MarketingPreferenceApplicationService
```

### 5.4 CDP Context

Owns:

- CDP events.
- User profiles.
- Tags.
- Audiences.
- Warehouse readiness and quality.
- Realtime/batch CDP governance.

Moves from current packages:

```text
org.chovy.canvas.domain.cdp.*
org.chovy.canvas.domain.warehouse.*
org.chovy.canvas.engine.audience.*
Cdp* data objects and mappers from org.chovy.canvas.dal.*
```

### 5.5 BI Context

Owns:

- BI workspace.
- Datasource onboarding.
- Dataset and metric.
- Chart, dashboard, portal.
- Subscription and delivery.
- BI resource permissions and collaboration.

Moves from current packages:

```text
org.chovy.canvas.domain.bi.*
org.chovy.canvas.web.bi.*
Bi* data objects and mappers from org.chovy.canvas.dal.*
```

### 5.6 Risk Context

Owns:

- Risk scenes.
- Risk strategy lifecycle.
- Risk lists and entries.
- Risk decision run.
- Rule hits.
- Audit sink interfaces and persistence.

Moves from current packages:

```text
org.chovy.canvas.domain.risk.*
org.chovy.canvas.web.risk.*
Risk* data objects and mappers from org.chovy.canvas.dal.*
```

### 5.7 Conversation Context

Owns:

- Conversation sessions.
- Private contacts and groups.
- Routing rules.
- Work items.
- SOP tasks.
- AI reply suggestions.

Moves from current packages:

```text
org.chovy.canvas.domain.conversation.*
Conversation* data objects and mappers from org.chovy.canvas.dal.*
Conversation-related controllers in org.chovy.canvas.web.*
```

### 5.8 Platform Module

Owns:

- Platform workstream.
- Marketing platform control plane.
- Technical migration candidates.
- Architecture evidence records.

Moves from current packages:

```text
org.chovy.canvas.platform.*
org.chovy.canvas.architecture.*
org.chovy.canvas.strategy.architecture.*
```

---

## 6. Cross-Context Contracts

### 6.1 Canvas to Execution

Canvas publishes an immutable execution definition. Execution consumes that
definition without reaching into canvas persistence.

```text
canvas-context-canvas/api
  PublishedCanvasDefinition
  PublishedCanvasDefinitionProvider
  ExecutionPublicationPort

canvas-context-execution/application
  implements ExecutionPublicationPort
  depends on PublishedCanvasDefinitionProvider through api
```

Rule:

- Canvas may request execution publication through `ExecutionPublicationPort`.
- Execution may read published definitions through
  `PublishedCanvasDefinitionProvider`.
- Execution must not use `CanvasMapper` or `CanvasVersionMapper`.

### 6.2 Execution to CDP

Execution can evaluate audience or profile-dependent nodes through CDP API.

```text
canvas-context-cdp/api
  AudienceSnapshotFacade
  CustomerProfileLookupPort
```

Rule:

- Execution must not directly query `Cdp*Mapper`.
- CDP owns audience and profile persistence.

### 6.3 Marketing to Canvas / BI / CDP

Marketing campaigns can link resources from canvas, BI, CDP, and content areas.
Links store stable resource references, not foreign module data objects.

```text
marketing.api.MarketingResourceReference
marketing.domain.CampaignResourceLink
```

Rule:

- Marketing owns campaign readiness.
- Other contexts may expose resource status through API views or events.

### 6.4 Risk to Execution

Execution can invoke risk decisions through a risk API.

```text
risk.api.RiskDecisionCommand
risk.api.RiskDecisionView
risk.api.RiskDecisionFacade
```

Rule:

- Execution may call `RiskDecisionFacade`.
- Risk owns strategy/list/decision persistence.
- Risk decision node handlers live in execution if they are runtime handlers,
  but they call risk through the API.

---

## 7. Data Ownership Rules

1. Each table belongs to exactly one context.
2. The owning context contains the `DO`, `Mapper`, repository implementation,
   and persistence converter for that table.
3. Non-owning contexts cannot import the owning context's persistence classes.
4. Shared reads must use API views, domain events, or dedicated read facades.
5. Cross-context writes must go through the owning context's application
   facade or command API.
6. Existing table names and Flyway migration history are preserved in the
   first rewrite milestone.

---

## 8. Web and Boot Rules

### 8.1 `canvas-web`

Owns:

- Controllers.
- HTTP request DTOs when they are not stable API commands.
- HTTP response wrapping.
- Tenant context extraction.
- HTTP error translation.

Must not:

- Use MyBatis mappers.
- Use persistence DOs.
- Build Redis keys.
- Publish MQ messages directly.
- Contain business invariants.

### 8.2 `canvas-boot`

Owns:

- `CanvasEngineApplication`.
- Runtime configuration.
- Spring Security configuration.
- WebFlux configuration.
- Flyway resources.
- Mapper scanning.
- Actuator and observability wiring.

Must not:

- Contain use-case logic.
- Contain domain rules.
- Become a dumping ground for module-specific configuration.

---

## 9. Testing and Architecture Verification

Required verification layers:

1. Unit tests for domain policies and value objects.
2. Application service tests with mocked repositories/ports.
3. Persistence adapter tests for MyBatis mapping where practical.
4. Controller tests for HTTP compatibility.
5. Contract tests comparing old and new API behavior for critical routes.
6. Architecture tests for forbidden dependencies.
7. End-to-end smoke tests for critical flows.
8. Guardrail checks for LLM drift and worker-scope violations.

Architecture tests must enforce:

```text
domain packages do not depend on Spring Web, MyBatis, Redis, RocketMQ
web packages do not depend on adapter.persistence or *Mapper or *DO
context modules do not depend on another context's adapter package
module dependencies are acyclic
```

The operational guardrails are defined in:

```text
docs/ddd-rewrite/guardrails/README.md
```

No worker delivery can be accepted while a failure mode from
`docs/ddd-rewrite/guardrails/llm-drift-and-failure-modes.md` remains unresolved.

---

## 10. Cutover Criteria

The rewrite can replace `canvas-engine` only when all criteria are met:

- `mvn clean install` passes from `backend`.
- Frontend build and tests pass against the new backend API.
- Critical API contract tests pass.
- Critical flow smoke tests pass:
  - login/tenant context
  - create canvas
  - edit canvas
  - publish canvas
  - trigger execution
  - inspect execution trace
  - marketing campaign readiness
  - CDP audience lookup
  - BI dashboard read path
  - risk decision node path
  - conversation webhook/session path
- Architecture tests pass.
- Old `canvas-engine` is no longer needed as a compile dependency.
- Operational configuration points to `canvas-boot`.

---

## 11. Non-Goals for First Rewrite Milestone

The first milestone does not include:

- Microservice extraction.
- Database table renaming.
- Rewriting frontend routing.
- Replacing MyBatis with another persistence technology.
- Redesigning all APIs.
- Full event sourcing.
- Rebuilding the product workflow.

Those changes require separate specs after the modular boundary is stable.

---

## 12. References

- Eric Evans, Domain-Driven Design Reference:
  `https://www.domainlanguage.com/ddd/reference/`
- Martin Fowler, Bounded Context:
  `https://martinfowler.com/bliki/BoundedContext.html`
- Spring Modulith Reference:
  `https://docs.spring.io/spring-modulith/reference/`
- Microsoft Azure Architecture Center, Tactical DDD:
  `https://learn.microsoft.com/en-us/azure/architecture/microservices/model/tactical-domain-driven-design`
