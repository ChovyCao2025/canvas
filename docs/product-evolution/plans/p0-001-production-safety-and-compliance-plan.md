# Production Safety And Compliance Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Secure backend production access by protecting ops routes, adding internal API token checks, enforcing tenant query scope, and applying marketing policy before every real send.

**Architecture:** Keep this as four backend slices: route security, tenant-scope plumbing, delivery policy enforcement, and governed-node assertions. The plan uses additive migrations and focused unit tests so each slice can be merged independently while leaving frontend resilience to P0-002 and runtime dashboards to P0-005.

**Tech Stack:** Java 21, Spring Boot WebFlux security, MyBatis-Plus, Flyway SQL, Redis-backed `MarketingPolicyService`, Reactor `Mono`, JUnit 5, Mockito, AssertJ.

## Implementation Status

- Status: implemented and verified on 2026-06-05.
- Commit: not created in this session because the worktree contains many unrelated and parallel product-evolution changes.

---

## Spec Reference

- `docs/product-evolution/specs/p0-001-production-safety-and-compliance.md`
- Source item: `docs/product-evolution/todo/p0/production-safety-and-compliance-stopgaps.md`

## File Structure

**Backend Security**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/InternalApiAuthFilter.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRoleTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/InternalApiAuthFilterTest.java`

**Tenant Scope**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContextResolver.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantScopeSupport.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/NotificationController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpUserController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasVersionDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CanvasExecutionTraceDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/AudienceDefinitionDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/NotificationDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CustomerProfileDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CustomerChannelDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingConsentDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MarketingSuppressionDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/MessageSendRecordDO.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V185__production_safety_and_compliance.sql`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/common/tenant/TenantContextResolverTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/common/tenant/TenantScopeSupportTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceTenantScopeTest.java`

**Delivery Policy**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/ReachDeliveryServicePolicyTest.java`

**Node Governance**
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java`

### Task 1: Protect Ops And Internal Execution Routes

**Files:**
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRoleTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/InternalApiAuthFilterTest.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/InternalApiAuthFilter.java`

- [x] **Step 1: Extend route-role tests**

Add these assertions to `SecurityConfigRoleTest`.

```java
@Test
void opsRoutesRequireSuperAdminOrLegacyAdmin() {
    assertThat(SecurityConfig.OPS_ROUTE_ROLES)
            .containsExactly(RoleNames.ADMIN, RoleNames.SUPER_ADMIN);
}

@Test
void internalOpenApiRoutesStayExplicitlyEnumerated() {
    assertThat(SecurityConfig.INTERNAL_OPEN_API_ROUTES)
            .containsExactly(
                    "/canvas/events/report",
                    "/canvas/execute/direct/*",
                    "/canvas/trigger/behavior");
}
```

- [x] **Step 2: Write internal token filter tests**

Create `InternalApiAuthFilterTest` with this behavior contract.

```java
@Test
void rejectsConfiguredInternalRouteWhenTokenIsMissing() {
    InternalApiAuthFilter filter = new InternalApiAuthFilter("secret");
    MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/canvas/trigger/behavior").build());

    StepVerifier.create(filter.filter(exchange, e -> Mono.empty()))
            .verifyComplete();

    assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
}

@Test
void allowsConfiguredInternalRouteWhenTokenMatches() {
    InternalApiAuthFilter filter = new InternalApiAuthFilter("secret");
    MockServerWebExchange exchange = MockServerWebExchange.from(
            MockServerHttpRequest.post("/canvas/events/report")
                    .header("X-Canvas-Internal-Token", "secret")
                    .build());
    AtomicBoolean invoked = new AtomicBoolean(false);

    StepVerifier.create(filter.filter(exchange, e -> {
        invoked.set(true);
        return Mono.empty();
    })).verifyComplete();

    assertThat(invoked).isTrue();
}
```

- [x] **Step 3: Run security tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=SecurityConfigRoleTest,InternalApiAuthFilterTest
```

Expected: FAIL because `OPS_ROUTE_ROLES`, `INTERNAL_OPEN_API_ROUTES`, and `InternalApiAuthFilter` do not exist yet.

- [x] **Step 4: Implement route constants and filter wiring**

In `SecurityConfig`, add constants and accept the new filter in `securityWebFilterChain`.

```java
static final String[] OPS_ROUTE_ROLES = {RoleNames.ADMIN, RoleNames.SUPER_ADMIN};
static final String[] INTERNAL_OPEN_API_ROUTES = {
        "/canvas/events/report",
        "/canvas/execute/direct/*",
        "/canvas/trigger/behavior"
};
```

Change the security chain so `/ops/**` is role-gated and the internal filter runs before authorization.

```java
.pathMatchers(INTERNAL_OPEN_API_ROUTES).permitAll()
.pathMatchers("/ops/**").hasAnyRole(OPS_ROUTE_ROLES)
```

```java
.addFilterAt(internalApiAuthFilter, SecurityWebFiltersOrder.FIRST)
.addFilterAt(jwtAuthFilter, SecurityWebFiltersOrder.AUTHENTICATION)
```

Create `InternalApiAuthFilter` as a `WebFilter` with constructor parameter `@Value("${canvas.internal-api.token:}") String token`. Its matching rule is POST plus one of the three configured paths. If the configured token is blank, it allows the request for local and dev compatibility. If configured, it compares `X-Canvas-Internal-Token` with `MessageDigest.isEqual`.

- [x] **Step 5: Run security tests and confirm green state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=SecurityConfigRoleTest,InternalApiAuthFilterTest
```

Expected: PASS; `/ops/**` is no longer in a permit-all rule and internal execution routes have a production token gate.

### Task 2: Add Tenant Fields And Query Scope Support

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantContextResolver.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/tenant/TenantScopeSupport.java`
- Modify: tenant-owned data objects listed in File Structure
- Create: `backend/canvas-engine/src/main/resources/db/migration/V185__production_safety_and_compliance.sql`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/common/tenant/TenantContextResolverTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/common/tenant/TenantScopeSupportTest.java`

- [x] **Step 1: Write resolver and helper tests**

Add this test to `TenantContextResolverTest`.

```java
@Test
void currentOrErrorRejectsMissingTenantContext() {
    StepVerifier.create(resolver.currentOrError())
            .expectErrorMatches(error -> error instanceof SecurityException
                    && error.getMessage().equals("AUTH_003: missing tenant context"))
            .verify();
}
```

Create `TenantScopeSupportTest`.

```java
@Test
void tenantFilterAddsEqPredicateForTenantUsers() {
    TenantScopeSupport support = new TenantScopeSupport();
    LambdaQueryWrapper<CanvasDO> wrapper = new LambdaQueryWrapper<>();

    support.applyTenantFilter(wrapper, CanvasDO::getTenantId, new TenantContext(7L, RoleNames.TENANT_ADMIN, "alice"));

    assertThat(wrapper.getExpression().getNormal().toString()).contains("tenant_id");
}

@Test
void tenantFilterIsSkippedForLegacyAdminWithoutTenant() {
    TenantScopeSupport support = new TenantScopeSupport();
    LambdaQueryWrapper<CanvasDO> wrapper = new LambdaQueryWrapper<>();

    support.applyTenantFilter(wrapper, CanvasDO::getTenantId, new TenantContext(null, RoleNames.ADMIN, "root"));

    assertThat(wrapper.getExpression().getNormal()).isEmpty();
}
```

- [x] **Step 2: Run tenant tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TenantContextResolverTest,TenantScopeSupportTest
```

Expected: FAIL because `currentOrError`, `TenantScopeSupport`, and several `tenantId` getters do not exist.

- [x] **Step 3: Add tenant fields to data objects**

For each tenant-owned data object listed in File Structure, add this field directly after the `id` field. Use `@TableField("tenant_id")` because the database column is snake case.

```java
@TableField("tenant_id")
private Long tenantId;
```

- [x] **Step 4: Add resolver and helper implementation**

Add this method to `TenantContextResolver`.

```java
public Mono<TenantContext> currentOrError() {
    return current().switchIfEmpty(Mono.error(
            new SecurityException("AUTH_003: missing tenant context")));
}
```

Create `TenantScopeSupport`.

```java
@Component
public class TenantScopeSupport {
    public <T> LambdaQueryWrapper<T> applyTenantFilter(
            LambdaQueryWrapper<T> wrapper,
            SFunction<T, Long> tenantGetter,
            TenantContext context) {
        if (context.tenantId() == null && RoleNames.ADMIN.equals(context.role())) {
            return wrapper;
        }
        if (context.tenantId() == null) {
            throw new SecurityException("AUTH_003: missing tenant context");
        }
        return wrapper.eq(tenantGetter, context.tenantId());
    }
}
```

- [x] **Step 5: Add additive tenant migration**

Create `V185__production_safety_and_compliance.sql`.

Use the repository's existing `information_schema` guarded migration style so replays and partially upgraded databases do not fail when a column or index already exists. The migration must:

- Add nullable `tenant_id` columns to `audience_definition`, `notification`, `customer_profile`, `customer_channel`, `marketing_consent`, `marketing_suppression`, and `message_send_record` only when missing.
- Backfill those columns from the default tenant, except `message_send_record`, which first backfills from parent `canvas.tenant_id`.
- Replace legacy user-only unique indexes on `customer_profile`, `customer_channel`, and `marketing_consent` with tenant-scoped unique indexes when the old indexes exist.
- Add tenant-scoped read indexes for audience, notification, suppression, and send-record queries when missing.

Representative guarded statement:

```sql
SET @audience_tenant_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'audience_definition'
      AND column_name = 'tenant_id'
);
SET @audience_tenant_sql := IF(
    @audience_tenant_exists = 0,
    "ALTER TABLE audience_definition ADD COLUMN tenant_id BIGINT NULL AFTER id",
    "SELECT 1"
);
PREPARE audience_tenant_stmt FROM @audience_tenant_sql;
EXECUTE audience_tenant_stmt;
DEALLOCATE PREPARE audience_tenant_stmt;
```

- [x] **Step 6: Run tenant tests and Flyway smoke test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TenantContextResolverTest,TenantScopeSupportTest,FlywayConfigTest
```

Expected: PASS; the migration name is unique after existing `V184__product_led_growth_evidence.sql`.

### Task 3: Apply Tenant Scope To High-Risk Surfaces

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasExecutionRequestManagementController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/NotificationController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpUserController.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/canvas/CanvasServiceTenantScopeTest.java`

- [x] **Step 1: Write canvas tenant scope service tests**

Create `CanvasServiceTenantScopeTest` with Mockito field injection so the test stays aligned with the existing `@RequiredArgsConstructor` dependencies.

```java
@ExtendWith(MockitoExtension.class)
class CanvasServiceTenantScopeTest {
@Mock CanvasMapper mapper;
@Mock CanvasVersionMapper versionMapper;
@Mock DagParser dagParser;
@Mock TriggerRouteService triggerRouteService;
@Mock CanvasSchedulerService schedulerService;
@Mock CanvasConfigCache configCache;
@Mock CanvasExecutionService canvasExecutionService;
@Mock TriggerPreCheckService preCheckService;
@Mock GroovyHandler groovyHandler;
@Mock MqTriggerHandler mqTriggerHandler;
@Mock CanvasRuleGraphValidator canvasRuleGraphValidator;
@Mock StringRedisTemplate redis;
@Mock CanvasTransactionService canvasTransactionService;
@Mock CanvasExamplesProperties examplesProperties;
@Spy TenantScopeSupport tenantScopeSupport = new TenantScopeSupport();
@InjectMocks CanvasService service;

@Test
void listAddsTenantFilterForTenantAdmin() {
    when(examplesProperties.isEnabled()).thenReturn(true);
    when(mapper.selectPage(any(), any())).thenReturn(new Page<CanvasDO>().setRecords(List.of()));

    service.list(new CanvasListQuery(), new TenantContext(3L, RoleNames.TENANT_ADMIN, "tenant_admin"));

    ArgumentCaptor<LambdaQueryWrapper<CanvasDO>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
    verify(tenantScopeSupport).applyTenantFilter(captor.capture(), any(), any());
    assertThat(captor.getValue().getExpression().getNormal().toString()).contains("tenant_id");
}
}
```

- [x] **Step 2: Run scope tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasServiceTenantScopeTest
```

Expected: FAIL because `CanvasService.list` does not yet accept `TenantContext` and controller methods do not pass it.

- [x] **Step 3: Thread tenant context through controller methods**

For WebFlux controllers, resolve tenant before entering blocking mapper work. Use this pattern in `CanvasStatsController`, execution request management, audience, notification, and CDP user endpoints.

```java
return tenantContextResolver.currentOrError().flatMap(tenant ->
        Mono.fromCallable(() -> serviceMethod(id, tenant))
                .subscribeOn(Schedulers.boundedElastic())
                .map(R::ok));
```

For `CanvasController`, keep the public method signature stable and call the new `CanvasService` overloads with `TenantContext`.

- [x] **Step 4: Apply `TenantScopeSupport` to query wrappers**

When a query reads tenant-owned tables, apply the helper before mapper execution.

```java
LambdaQueryWrapper<CanvasDO> q = new LambdaQueryWrapper<>();
tenantScopeSupport.applyTenantFilter(q, CanvasDO::getTenantId, tenant);
```

For detail reads such as canvas stats, read the parent canvas with tenant filter first. Return an empty result or `R.fail("画布不存在")` when the canvas is outside the tenant.

- [x] **Step 5: Run high-risk controller and service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasServiceTenantScopeTest,CanvasStatsControllerTest,CanvasExecutionRequestManagementControllerTest,AudienceControllerTest,NotificationControllerTest,CdpUserControllerTest
```

Expected: PASS; captured wrappers include `tenant_id` for tenant users and omit it only for legacy `ADMIN` with null tenant.

### Task 4: Enforce Marketing Policy In Reach Delivery

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/delivery/ReachDeliveryServicePolicyTest.java`
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/handlers/SendMessageHandlerTest.java`

- [x] **Step 1: Write policy-blocked delivery tests**

Create `ReachDeliveryServicePolicyTest` with three focused assertions.

```java
private final AtomicInteger reachCalls = new AtomicInteger();

@BeforeEach
void startReachServer() throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext("/send", exchange -> {
        reachCalls.incrementAndGet();
        byte[] body = "{\"messageId\":\"msg-1\"}".getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    });
    server.start();
}

@Test
void optOutCreatesSkippedRecordAndDoesNotCallReachPlatform() {
    when(policyService.consentAllowed("user-1", "SMS", true))
            .thenReturn(PolicyDecision.blocked("MARKETING_OPT_OUT", "opted out"));

    DeliveryResult result = service.send(requestWithPolicy("SMS")).block();

    assertThat(result.sent()).isFalse();
    assertThat(result.errorMessage()).contains("MARKETING_OPT_OUT");
    verify(recordMapper).insert(argThat(record -> MessageSendRecordDO.STATUS_SKIPPED.equals(record.getStatus())));
    assertThat(reachCalls).hasValue(0);
}

@Test
void duplicateSkippedRecordDoesNotConsumeFrequencyAgain() {
    when(recordMapper.selectOne(any())).thenReturn(existingSkippedRecord());

    DeliveryResult result = service.send(requestWithPolicy("EMAIL")).block();

    assertThat(result.duplicate()).isTrue();
    verify(policyService, never()).consumeFrequency(any(), any(), any(), any(), any(), anyInt(), any());
}

@Test
void allowedPolicyCallsReachPlatform() {
    allowAllPolicyChecks();

    DeliveryResult result = service.send(requestWithPolicy("PUSH")).block();

    assertThat(result.sent()).isTrue();
    assertThat(reachCalls).hasValue(1);
}
```

- [x] **Step 2: Run delivery tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ReachDeliveryServicePolicyTest,SendMessageHandlerTest
```

Expected: FAIL because `ReachDeliveryService` does not receive `MarketingPolicyService`, `DeliveryRequest` has no policy options, and blocked sends are not marked `SKIPPED`.

- [x] **Step 3: Extend send handler request construction**

In `AbstractSendMessageHandler`, read policy configuration from node config and pass it to a new `ReachDeliveryService.PolicyOptions` record.

```java
ReachDeliveryService.PolicyOptions policy = new ReachDeliveryService.PolicyOptions(
        bool(config, "requireExplicitConsent", true),
        string(config, "quietStart", "22:00"),
        string(config, "quietEnd", "08:00"),
        string(config, "quietTimezone", "USER_LOCAL"),
        string(config, "frequencyScope", "JOURNEY"),
        integer(config, "frequencyMax", 1),
        integer(config, "frequencyWindowSeconds", 86400));
```

Add `bool` and `integer` helpers next to the existing `string` helper.

- [x] **Step 4: Enforce policy before external delivery**

In `ReachDeliveryService.send`, keep the existing duplicate check first. For new records, evaluate policy before `callReachPlatform`.

```java
PolicyDecision decision = evaluatePolicy(request);
if (!decision.allowed()) {
    return Mono.just(markSkipped(prepared.record(), decision));
}
return callReachPlatform(request)
        .flatMap(response -> markSent(prepared.record(), response))
        .onErrorResume(e -> markFailed(prepared.record(), e));
```

`evaluatePolicy` must call `consentAllowed`, `suppressionAllowed`, `channelAvailable`, `quietHoursAllowed`, and `consumeFrequency` in that order, stopping at the first blocked decision.

- [x] **Step 5: Persist skipped records**

Add `markSkipped` in `ReachDeliveryService`.

```java
private DeliveryResult markSkipped(MessageSendRecordDO record, PolicyDecision decision) {
    record.setStatus(MessageSendRecordDO.STATUS_SKIPPED);
    record.setErrorMessage(decision.reasonCode() + ": " + decision.reasonMessage());
    record.setUpdatedAt(LocalDateTime.now());
    recordMapper.updateById(record);
    return new DeliveryResult(false, false, record.getId(), null, record.getErrorMessage());
}
```

- [x] **Step 6: Run delivery tests and confirm green state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ReachDeliveryServicePolicyTest,SendMessageHandlerTest
```

Expected: PASS; policy-blocked requests do not reach `/send`, duplicate records do not consume counters again, and existing send handler output still includes `sendStatus`.

### Task 5: Assert Governed Node Catalog Safety

**Files:**
- Test: `backend/canvas-engine/src/test/java/org/chovy/canvas/common/enums/NodeTypeGovernanceTest.java`
- Read: `backend/canvas-engine/src/main/resources/db/migration/V90__register_commit_action_node.sql`

- [x] **Step 1: Add catalog exclusion assertions**

Extend `NodeTypeGovernanceTest`.

```java
@Test
void futureStubNodesAreNotGenerallyAvailable() throws Exception {
    String sql = Files.readString(Path.of("src/main/resources/db/migration/V90__register_commit_action_node.sql"));

    assertThat(sql).doesNotContain("'AI_NEXT_BEST_ACTION'");
    assertThat(sql).doesNotContain("'RECOMMENDATION'");
    assertThat(sql).doesNotContain("'IN_APP_NOTIFY'");
}
```

- [x] **Step 2: Run governance tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=NodeTypeGovernanceTest
```

Expected: PASS; future or stub-like node types stay out of the generally available registry until their dedicated specs implement them.

### Task 6: Regression And Rollout

**Files:**
- Modify: `docs/product-evolution/specs/p0-001-production-safety-and-compliance.md`
- Modify: `docs/product-evolution/plans/p0-001-production-safety-and-compliance-plan.md`
- Read: `docs/product-evolution/IMPLEMENTATION_ORDER.md`

- [x] **Step 1: Run focused backend suite**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=SecurityConfigRoleTest,InternalApiAuthFilterTest,TenantContextResolverTest,TenantScopeSupportTest,CanvasServiceTenantScopeTest,ReachDeliveryServicePolicyTest,NodeTypeGovernanceTest
```

Expected: PASS for all production-safety tests.

- [x] **Step 2: Run canvas-engine regression tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test
```

Expected: PASS for the module test suite.

- [x] **Step 3: Add rollout notes to the implementation PR**

Use this exact checklist in the PR body.

```markdown
Rollout notes:
- `canvas.internal-api.token` remains blank in local/dev.
- Staging callers send `X-Canvas-Internal-Token` before the staging property is set.
- Production property is set after staging callers pass.
- Tenant migration `V185__production_safety_and_compliance.sql` is additive and backfills default tenant values.
- Rollback switch: clear `canvas.internal-api.token` and revert the application deploy; additive tenant columns remain in place.
```

### Verification Evidence

- Focused tenant and Flyway suite:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=TenantContextResolverTest,TenantScopeSupportTest,FlywayConfigTest
```

Result: 7 tests, 0 failures, 0 errors, 0 skipped.

- High-risk controller and service tenant-scope suite:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CanvasServiceTenantScopeTest,CanvasStatsControllerTest,CanvasExecutionRequestManagementControllerTest,AudienceControllerTest,NotificationControllerTest,CdpUserControllerTest
```

Result: 22 tests, 0 failures, 0 errors, 0 skipped.

- Delivery policy suite:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=ReachDeliveryServicePolicyTest,SendMessageHandlerTest
```

Result: 4 tests, 0 failures, 0 errors, 0 skipped.

- Focused production-safety suite:

```bash
cd backend && mvn -pl canvas-engine clean test -Dtest=SecurityConfigRoleTest,InternalApiAuthFilterTest,TenantContextResolverTest,TenantScopeSupportTest,CanvasServiceTenantScopeTest,ReachDeliveryServicePolicyTest,NodeTypeGovernanceTest
```

Result: 21 tests, 0 failures, 0 errors, 0 skipped.

- Backend module regression:

```bash
cd backend && mvn -pl canvas-engine test
```

Result: 1402 tests, 0 failures, 0 errors, 1 skipped.

- [ ] **Step 4: Commit the implementation slice**

Run:

```bash
git add backend/canvas-engine/src docs/product-evolution/specs/p0-001-production-safety-and-compliance.md docs/product-evolution/plans/p0-001-production-safety-and-compliance-plan.md && git commit -m "feat: harden production safety controls"
```

Expected: commit contains route security, tenant-scope, delivery-policy, and node-governance changes for this spec.
