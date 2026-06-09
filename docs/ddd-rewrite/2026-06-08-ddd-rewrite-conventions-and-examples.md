# DDD Rewrite Conventions and Examples

**Date:** 2026-06-08

**Purpose:** Provide executable rules and examples for the DDD modular rewrite.
The specification defines the target architecture. The plan defines execution
order. This document defines how engineers and subagents decide where classes
go, how names are chosen, what migration output looks like, and what patterns
are rejected during review.

---

## 1. Core Placement Rule

For every class, answer these questions in order:

1. Is it part of an HTTP endpoint?
2. Is it a cross-module contract?
3. Is it a use case or transaction boundary?
4. Is it a business concept or invariant?
5. Is it a database mapping detail?
6. Is it a message transport detail?
7. Is it an external system detail?
8. Is it runtime assembly or global Spring configuration?

The first matching answer determines the package.

| Question | Package |
| --- | --- |
| HTTP endpoint, request adapter, response wrapping | `canvas-web/...` |
| Cross-module command, query, view, facade, port | `<context>.api` |
| Use case orchestration, transaction, tenant/actor handling | `<context>.application` |
| Entity, value object, aggregate, policy, repository interface | `<context>.domain` |
| MyBatis `*Mapper`, `*DO`, persistence converter, repository implementation | `<context>.adapter.persistence` |
| RocketMQ consumer/publisher/message DTO | `<context>.adapter.messaging` |
| WebClient/Doris/third-party/AI provider adapter | `<context>.adapter.external` |
| Spring Boot startup, global security, Flyway, component scanning | `canvas-boot/...` |
| Minimal shared primitive used by several contexts | `canvas-common/...` |

If a class seems to match several rows, split it. A class that handles HTTP,
transactions, MyBatis, and business rules is not a DDD class; it is a migration
candidate.

---

## 2. Naming Rules

### 2.1 Context Modules

Maven artifact names:

```text
canvas-context-<context>
```

Java package names:

```text
org.chovy.canvas.<context>
```

Examples:

```text
backend/canvas-context-marketing
org.chovy.canvas.marketing

backend/canvas-context-execution
org.chovy.canvas.execution
```

### 2.2 API Types

Use API names that describe external intent, not persistence.

Good:

```text
MarketingCampaignCommand
MarketingCampaignView
MarketingCampaignFacade
RiskDecisionCommand
RiskDecisionView
CanvasExecutionFacade
PublishedCanvasDefinition
```

Rejected:

```text
MarketingCampaignMasterDOView
RiskStrategyMapperRequest
CanvasVersionTableResponse
```

### 2.3 Application Services

Application services are named by use case.

Good:

```text
CanvasPublishApplicationService
CanvasVersionApplicationService
MarketingCampaignApplicationService
RiskDecisionApplicationService
BiDashboardApplicationService
```

Rejected:

```text
CanvasService
MarketingService
CommonBusinessService
BaseDomainService
```

### 2.4 Domain Types

Domain names use business language.

Good:

```text
MarketingCampaign
CampaignKey
CampaignStatus
CampaignBudget
MarketingCampaignReadinessPolicy
MarketingCampaignRepository
```

Rejected:

```text
MarketingCampaignMasterDomainDO
CampaignTableModel
CampaignUtils
MarketingCampaignRepositoryImpl
```

### 2.5 Persistence Types

Persistence names may reflect table/data mapping because they are isolated in
`adapter.persistence`.

Good:

```text
MarketingCampaignMasterDO
MarketingCampaignMasterMapper
MybatisMarketingCampaignRepository
MarketingCampaignPersistenceConverter
```

Rejected outside `adapter.persistence`:

```text
MarketingCampaignMasterDO
MarketingCampaignMasterMapper
LambdaQueryWrapper<MarketingCampaignMasterDO>
```

---

## 3. Migration Example: Marketing Campaign

### 3.1 Current Shape

Existing code has this style:

```text
org.chovy.canvas.web.MarketingCampaignController
  -> org.chovy.canvas.domain.marketing.MarketingCampaignService
    -> org.chovy.canvas.dal.mapper.MarketingCampaignMasterMapper
    -> org.chovy.canvas.dal.dataobject.MarketingCampaignMasterDO
```

Problems:

- Service is called `domain`, but it owns transaction orchestration,
  persistence queries, JSON conversion, validation, and view mapping.
- Data objects and mappers live in global `dal`.
- Controller depends directly on a concrete service instead of a module API or
  application facade.

### 3.2 Target Shape

```text
canvas-web
  org.chovy.canvas.web.marketing.MarketingCampaignController

canvas-context-marketing
  org.chovy.canvas.marketing.api.MarketingCampaignCommand
  org.chovy.canvas.marketing.api.MarketingCampaignView
  org.chovy.canvas.marketing.api.MarketingCampaignFacade

  org.chovy.canvas.marketing.application.MarketingCampaignApplicationService

  org.chovy.canvas.marketing.domain.MarketingCampaign
  org.chovy.canvas.marketing.domain.CampaignKey
  org.chovy.canvas.marketing.domain.CampaignStatus
  org.chovy.canvas.marketing.domain.CampaignBudget
  org.chovy.canvas.marketing.domain.MarketingCampaignReadinessPolicy
  org.chovy.canvas.marketing.domain.MarketingCampaignRepository

  org.chovy.canvas.marketing.adapter.persistence.MarketingCampaignMasterDO
  org.chovy.canvas.marketing.adapter.persistence.MarketingCampaignMasterMapper
  org.chovy.canvas.marketing.adapter.persistence.MybatisMarketingCampaignRepository
  org.chovy.canvas.marketing.adapter.persistence.MarketingCampaignPersistenceConverter
```

### 3.3 Example API Contract

```java
package org.chovy.canvas.marketing.api;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public record MarketingCampaignCommand(
        String campaignKey,
        String campaignName,
        String objective,
        String status,
        String primaryChannel,
        String ownerTeam,
        LocalDateTime startAt,
        LocalDateTime endAt,
        BigDecimal budgetAmount,
        String currency,
        Map<String, Object> brief) {
}
```

Rule:

- API command mirrors the external use case.
- It does not mention table names, mapper names, or data objects.

### 3.4 Example Domain Model

```java
package org.chovy.canvas.marketing.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public final class MarketingCampaign {
    private final Long id;
    private final Long tenantId;
    private final CampaignKey campaignKey;
    private String campaignName;
    private CampaignStatus status;
    private LocalDateTime startAt;
    private LocalDateTime endAt;

    public MarketingCampaign(Long id,
                             Long tenantId,
                             CampaignKey campaignKey,
                             String campaignName,
                             CampaignStatus status) {
        this.id = id;
        this.tenantId = Objects.requireNonNull(tenantId, "tenantId");
        this.campaignKey = Objects.requireNonNull(campaignKey, "campaignKey");
        this.campaignName = campaignName == null || campaignName.isBlank()
                ? campaignKey.value()
                : campaignName.trim();
        this.status = status == null ? CampaignStatus.DRAFT : status;
    }

    public void reschedule(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt must be after startAt");
        }
        this.startAt = startAt;
        this.endAt = endAt;
    }

    public Long id() {
        return id;
    }

    public Long tenantId() {
        return tenantId;
    }

    public CampaignKey campaignKey() {
        return campaignKey;
    }

    public String campaignName() {
        return campaignName;
    }

    public CampaignStatus status() {
        return status;
    }

    public LocalDateTime startAt() {
        return startAt;
    }

    public LocalDateTime endAt() {
        return endAt;
    }
}
```

Rule:

- Domain owns invariants such as schedule validity.
- Domain does not parse JSON, build SQL wrappers, or know MyBatis.

### 3.5 Example Repository Interface

```java
package org.chovy.canvas.marketing.domain;

import java.util.List;
import java.util.Optional;

public interface MarketingCampaignRepository {
    Optional<MarketingCampaign> findByKey(Long tenantId, CampaignKey campaignKey);

    Optional<MarketingCampaign> findById(Long tenantId, Long campaignId);

    MarketingCampaign save(MarketingCampaign campaign);

    List<MarketingCampaign> list(Long tenantId, CampaignStatus status, int limit);
}
```

Rule:

- Repository interface is domain-facing.
- It does not expose `DO`, `Mapper`, `Page`, or `LambdaQueryWrapper`.

### 3.6 Example Application Service

```java
package org.chovy.canvas.marketing.application;

import org.chovy.canvas.marketing.api.MarketingCampaignCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignFacade;
import org.chovy.canvas.marketing.api.MarketingCampaignView;
import org.chovy.canvas.marketing.domain.CampaignKey;
import org.chovy.canvas.marketing.domain.CampaignStatus;
import org.chovy.canvas.marketing.domain.MarketingCampaign;
import org.chovy.canvas.marketing.domain.MarketingCampaignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketingCampaignApplicationService implements MarketingCampaignFacade {
    private final MarketingCampaignRepository repository;

    public MarketingCampaignApplicationService(MarketingCampaignRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MarketingCampaignView upsertCampaign(Long tenantId,
                                                MarketingCampaignCommand command,
                                                String actor) {
        CampaignKey key = CampaignKey.of(command.campaignKey());
        CampaignStatus status = CampaignStatus.from(command.status());
        MarketingCampaign campaign = repository.findByKey(tenantId, key)
                .orElseGet(() -> new MarketingCampaign(null, tenantId, key, command.campaignName(), status));

        campaign.reschedule(command.startAt(), command.endAt());

        MarketingCampaign saved = repository.save(campaign);
        return MarketingCampaignView.from(saved);
    }
}
```

Rule:

- Application service owns transaction.
- Application service maps API command to domain values.
- Application service does not use `MarketingCampaignMasterMapper`.

### 3.7 Example Persistence Adapter

```java
package org.chovy.canvas.marketing.adapter.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.chovy.canvas.marketing.domain.CampaignKey;
import org.chovy.canvas.marketing.domain.CampaignStatus;
import org.chovy.canvas.marketing.domain.MarketingCampaign;
import org.chovy.canvas.marketing.domain.MarketingCampaignRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisMarketingCampaignRepository implements MarketingCampaignRepository {
    private final MarketingCampaignMasterMapper mapper;
    private final MarketingCampaignPersistenceConverter converter;

    public MybatisMarketingCampaignRepository(MarketingCampaignMasterMapper mapper,
                                              MarketingCampaignPersistenceConverter converter) {
        this.mapper = mapper;
        this.converter = converter;
    }

    @Override
    public Optional<MarketingCampaign> findByKey(Long tenantId, CampaignKey campaignKey) {
        MarketingCampaignMasterDO row = mapper.selectOne(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                .eq(MarketingCampaignMasterDO::getTenantId, tenantId)
                .eq(MarketingCampaignMasterDO::getCampaignKey, campaignKey.value())
                .last("LIMIT 1"));
        return Optional.ofNullable(row).map(converter::toDomain);
    }

    @Override
    public MarketingCampaign save(MarketingCampaign campaign) {
        MarketingCampaignMasterDO row = converter.toRow(campaign);
        if (row.getId() == null) {
            mapper.insert(row);
        } else {
            mapper.updateById(row);
        }
        return converter.toDomain(row);
    }

    @Override
    public Optional<MarketingCampaign> findById(Long tenantId, Long campaignId) {
        MarketingCampaignMasterDO row = mapper.selectOne(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                .eq(MarketingCampaignMasterDO::getTenantId, tenantId)
                .eq(MarketingCampaignMasterDO::getId, campaignId)
                .last("LIMIT 1"));
        return Optional.ofNullable(row).map(converter::toDomain);
    }

    @Override
    public List<MarketingCampaign> list(Long tenantId, CampaignStatus status, int limit) {
        return mapper.selectList(new LambdaQueryWrapper<MarketingCampaignMasterDO>()
                        .eq(MarketingCampaignMasterDO::getTenantId, tenantId)
                        .eq(status != null, MarketingCampaignMasterDO::getStatus, status == null ? null : status.name())
                        .orderByDesc(MarketingCampaignMasterDO::getUpdatedAt)
                        .last("LIMIT " + Math.max(1, Math.min(limit, 200))))
                .stream()
                .map(converter::toDomain)
                .toList();
    }
}
```

Rule:

- MyBatis is isolated in persistence adapter.
- The adapter implements the domain repository interface.

---

## 4. Migration Example: Canvas Publish and Execution

### 4.1 Current Problem

Canvas publish currently mixes responsibilities such as:

```text
Canvas state transition
CanvasVersion persistence
DAG parsing
Trigger route registration
Scheduler registration
Config cache update
Execution service coordination
Redis lock/key operations
```

This must be split because canvas authoring and execution runtime are different
contexts.

### 4.2 Target Contract

Canvas exposes immutable published definition:

```java
package org.chovy.canvas.canvas.api;

import java.time.LocalDateTime;
import java.util.Map;

public record PublishedCanvasDefinition(
        Long tenantId,
        Long canvasId,
        Long versionId,
        int version,
        String graphJson,
        LocalDateTime publishedAt,
        Map<String, Object> executionOptions) {

    public PublishedCanvasDefinition {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        if (canvasId == null) {
            throw new IllegalArgumentException("canvasId is required");
        }
        if (graphJson == null || graphJson.isBlank()) {
            throw new IllegalArgumentException("graphJson is required");
        }
        executionOptions = executionOptions == null ? Map.of() : Map.copyOf(executionOptions);
    }
}
```

Canvas calls execution through a port:

```java
package org.chovy.canvas.canvas.api;

public interface ExecutionPublicationPort {
    void publish(PublishedCanvasDefinition definition);

    void unpublish(Long tenantId, Long canvasId);
}
```

Execution implements the port:

```java
package org.chovy.canvas.execution.application;

import org.chovy.canvas.canvas.api.ExecutionPublicationPort;
import org.chovy.canvas.canvas.api.PublishedCanvasDefinition;
import org.springframework.stereotype.Service;

@Service
public class ExecutionPublicationApplicationService implements ExecutionPublicationPort {
    private final DagRuntimeService dagRuntimeService;
    private final CanvasSchedulerApplicationService schedulerService;

    public ExecutionPublicationApplicationService(DagRuntimeService dagRuntimeService,
                                                  CanvasSchedulerApplicationService schedulerService) {
        this.dagRuntimeService = dagRuntimeService;
        this.schedulerService = schedulerService;
    }

    @Override
    public void publish(PublishedCanvasDefinition definition) {
        dagRuntimeService.validate(definition.graphJson());
        schedulerService.registerPublishedCanvas(definition);
    }

    @Override
    public void unpublish(Long tenantId, Long canvasId) {
        schedulerService.unregisterPublishedCanvas(tenantId, canvasId);
    }
}
```

Rule:

- Canvas does not depend on execution persistence.
- Execution does not depend on canvas persistence.
- Both sides depend on stable API contracts.

---

## 5. Migration Example: Risk Decision Node

### 5.1 Target Placement

```text
canvas-context-execution
  org.chovy.canvas.execution.domain.node.RiskDecisionNodeHandler

canvas-context-risk
  org.chovy.canvas.risk.api.RiskDecisionFacade
  org.chovy.canvas.risk.api.RiskDecisionCommand
  org.chovy.canvas.risk.api.RiskDecisionView
  org.chovy.canvas.risk.application.RiskDecisionApplicationService
  org.chovy.canvas.risk.adapter.persistence.RiskDecisionRunDO
```

### 5.2 Rule

The node handler belongs to execution because node execution is runtime
behavior. The risk decision rules and persistence belong to risk.

Allowed:

```text
RiskDecisionNodeHandler -> RiskDecisionFacade
```

Forbidden:

```text
RiskDecisionNodeHandler -> RiskStrategyMapper
RiskDecisionNodeHandler -> RiskDecisionRunDO
RiskDecisionNodeHandler -> org.chovy.canvas.risk.adapter.persistence.*
```

---

## 6. Data Object Ownership Examples

| Existing class pattern | Target module | Target package |
| --- | --- | --- |
| `Canvas*DO`, `Canvas*Mapper` | `canvas-context-canvas` | `org.chovy.canvas.canvas.adapter.persistence` |
| `*Execution*DO`, `CanvasExecution*Mapper` | `canvas-context-execution` | `org.chovy.canvas.execution.adapter.persistence` |
| `Marketing*DO`, `Growth*DO` | `canvas-context-marketing` | `org.chovy.canvas.marketing.adapter.persistence` |
| `Cdp*DO`, `Audience*DO`, `Tag*DO` | `canvas-context-cdp` | `org.chovy.canvas.cdp.adapter.persistence` |
| `Bi*DO`, `Bi*Mapper` | `canvas-context-bi` | `org.chovy.canvas.bi.adapter.persistence` |
| `Risk*DO`, `Risk*Mapper` | `canvas-context-risk` | `org.chovy.canvas.risk.adapter.persistence` |
| `Conversation*DO`, `Conversation*Mapper` | `canvas-context-conversation` | `org.chovy.canvas.conversation.adapter.persistence` |
| `TechnicalMigrationCandidate*` | `canvas-platform` | `org.chovy.canvas.platform.adapter.persistence` |

Ambiguous names require coordinator decision. Do not put ambiguous data objects
in `canvas-common`.

---

## 7. Common Admission Rules

`canvas-common` is intentionally small.

Allowed:

```text
R
PageResult
ErrorCode
TenantContext
TenantContextResolver
TenantScopeSupport
ApiRequestValidation
DataMaskingUtil
```

Conditionally allowed:

```text
TenantId
Actor
PageQuery
ClockProvider
```

Rejected:

```text
CanvasStatusEnum
NodeType
ApprovalStatus
CampaignStatus
RiskDecisionStatus
BiResourceType
CommonMarketingUtils
CommonDomainService
BaseMapperHelper
```

Rule:

- A type can enter `canvas-common` only if it is business-neutral and used by at
  least three contexts.
- Business enums stay with the context that owns their language.

---

## 8. Controller Migration Rules

Controller before migration:

```text
web.Controller -> domain.Service -> dal.Mapper
```

Controller after migration:

```text
canvas-web.Controller -> context.api.Facade or context.application.ApplicationService
```

Controller may:

- Resolve tenant context.
- Validate HTTP body shape.
- Call facade/application service.
- Convert exceptions to response envelope.

Controller must not:

- Open transactions.
- Call MyBatis.
- Reference `*DO`.
- Build Redis keys.
- Decide business state transition.
- Construct domain aggregates directly unless the API contract requires it.

Example controller dependency:

```java
package org.chovy.canvas.web.marketing;

import org.chovy.canvas.common.R;
import org.chovy.canvas.marketing.api.MarketingCampaignCommand;
import org.chovy.canvas.marketing.api.MarketingCampaignFacade;
import org.chovy.canvas.marketing.api.MarketingCampaignView;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/canvas/marketing-campaigns")
public class MarketingCampaignController {
    private final MarketingCampaignFacade facade;

    public MarketingCampaignController(MarketingCampaignFacade facade) {
        this.facade = facade;
    }

    @PostMapping
    public Mono<R<MarketingCampaignView>> upsertCampaign(@RequestBody MarketingCampaignCommand command) {
        return Mono.fromCallable(() -> R.ok(facade.upsertCampaign(0L, command, "system")))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
```

The tenant resolution detail should use the shared web support class in the real
implementation. This example focuses on dependency direction.

---

## 9. Worker Output Example

Every worker must return this shape:

```text
status: DONE

assigned context:
  canvas-context-marketing

files changed:
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/api/MarketingCampaignCommand.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/application/MarketingCampaignApplicationService.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/domain/MarketingCampaign.java
  backend/canvas-context-marketing/src/main/java/org/chovy/canvas/marketing/adapter/persistence/MybatisMarketingCampaignRepository.java

old classes migrated:
  org.chovy.canvas.domain.marketing.MarketingCampaignService
  org.chovy.canvas.dal.dataobject.MarketingCampaignMasterDO
  org.chovy.canvas.dal.mapper.MarketingCampaignMasterMapper

tests run:
  mvn test -pl canvas-context-marketing
  result: PASS

architecture notes:
  domain has no MyBatis, Redis, RocketMQ, WebFlux, or Spring Web dependency
  persistence adapter owns Mapper and DO

open risks:
  MarketingIntegrationContractProbeClient still needs external adapter split in follow-up task
```

Workers must use `DONE_WITH_CONCERNS` when behavior is migrated but risks remain.

---

## 10. Review Checklist

### 10.1 Placement Review

For every new or moved class:

- [ ] Package matches the decision table.
- [ ] Name reflects role and context.
- [ ] It does not import forbidden packages.
- [ ] It does not duplicate another context's API type.
- [ ] It does not move business-specific concepts into common.

### 10.2 Domain Review

- [ ] Domain classes contain business decisions, not persistence code.
- [ ] Value objects validate their own invariants.
- [ ] Repository interfaces expose domain/API language, not table language.
- [ ] Domain services are pure business services.
- [ ] No domain class imports `org.springframework.web`, `com.baomidou`,
      `org.springframework.data.redis`, or `org.apache.rocketmq`.

### 10.3 Application Review

- [ ] Application service name is use-case specific.
- [ ] Transactions live here, not in controllers or domain entities.
- [ ] Tenant and actor handling is explicit.
- [ ] Cross-context calls go through `api` or ports.
- [ ] No SQL wrapper or mapper usage appears here unless coordinator approves a
      temporary migration bridge.

### 10.4 Persistence Review

- [ ] DO and Mapper are inside `adapter.persistence`.
- [ ] Persistence converter exists when mapping is non-trivial.
- [ ] Repository implementation is the only place that uses mapper for the
      aggregate/read model.
- [ ] Other modules do not import these classes.

### 10.5 Web Review

- [ ] Controller path and response shape remain compatible.
- [ ] Controller depends on facade/application API only.
- [ ] Controller does not import `*Mapper`, `*DO`, or `adapter.persistence`.
- [ ] Blocking calls are scheduled consistently with the current WebFlux pattern.

---

## 11. Anti-Patterns

### 11.1 Fake Domain Package

Rejected:

```text
domain.MarketingCampaignService
  imports MarketingCampaignMasterMapper
  imports ObjectMapper
  imports LambdaQueryWrapper
  has @Transactional
```

Fix:

```text
application.MarketingCampaignApplicationService
domain.MarketingCampaign
domain.MarketingCampaignRepository
adapter.persistence.MybatisMarketingCampaignRepository
```

### 11.2 Common Dumping Ground

Rejected:

```text
common.enums.NodeType
common.enums.CampaignStatus
common.service.CommonStatusService
```

Fix:

```text
execution.domain.NodeType
marketing.domain.CampaignStatus
```

### 11.3 Cross-Context Persistence Access

Rejected:

```text
execution.handlers.RiskDecisionHandler imports RiskStrategyMapper
```

Fix:

```text
execution.handlers.RiskDecisionHandler imports RiskDecisionFacade
```

### 11.4 Controller as Application Service

Rejected:

```text
Controller validates state transition, writes mapper, publishes MQ
```

Fix:

```text
Controller -> ApplicationService -> Domain/Repository/Port
```

### 11.5 Over-Generic Base Services

Rejected:

```text
BaseCrudApplicationService<T>
AbstractDomainService
GenericMybatisRepository<T>
```

Fix:

Use explicit services and repositories until duplication is proven meaningful.

---

## 12. Temporary Bridge Rules

Temporary bridges are allowed only during migration and must be removed before
cutover.

Allowed temporary bridge example:

```text
canvas-web controller calls old canvas-engine service through a compatibility
adapter while the new context is incomplete.
```

Bridge rules:

- Name must include `Compatibility` or `Legacy`.
- File must include a comment with removal phase.
- Bridge cannot be in `domain`.
- Bridge cannot be used after Phase 10.
- Architecture tests may exclude bridge packages only with coordinator approval.

Example name:

```text
LegacyCanvasPublishCompatibilityAdapter
```

---

## 13. Minimum Test Examples

### 13.1 Value Object Test

```java
package org.chovy.canvas.marketing.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CampaignKeyTest {
    @Test
    void normalizesKey() {
        assertThat(CampaignKey.of(" Spring-Sale ").value()).isEqualTo("spring-sale");
    }

    @Test
    void rejectsBlankKey() {
        assertThatThrownBy(() -> CampaignKey.of(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("campaignKey is required");
    }
}
```

### 13.2 Architecture Test Expectation

```java
@Test
void domainDoesNotDependOnMybatis() {
    noClasses()
            .that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage("com.baomidou..")
            .check(importedClasses);
}
```

### 13.3 Controller Dependency Test Expectation

```java
@Test
void controllersDoNotUseMappers() {
    noClasses()
            .that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Mapper")
            .check(importedClasses);
}
```

---

## 14. Migration Checklist Per Class

For each old class, record:

```text
old class:
target module:
target package:
new class name:
role:
dependencies removed:
tests moved or added:
compatibility risk:
owner:
```

Example:

```text
old class:
  org.chovy.canvas.domain.marketing.MarketingCampaignService

target module:
  canvas-context-marketing

target package:
  application + domain + adapter.persistence

new class names:
  MarketingCampaignApplicationService
  MarketingCampaign
  MarketingCampaignRepository
  MybatisMarketingCampaignRepository

role:
  split mixed service into use case, domain model, repository port, persistence adapter

dependencies removed:
  MyBatis from domain/application boundary
  ObjectMapper from domain

tests moved or added:
  MarketingCampaignApplicationServiceTest
  CampaignKeyTest
  MarketingCampaignReadinessPolicyTest

compatibility risk:
  campaign status normalization and briefJson serialization must match old behavior

owner:
  marketing-worker
```

---

## 15. Final Rule

When in doubt, prefer a smaller explicit class in the owning context over a
shared abstraction. The rewrite succeeds when boundaries are obvious,
dependencies are enforceable, and behavior is compatible. It fails if the new
system simply recreates the old global `service` and `dal` structure under new
folder names.
