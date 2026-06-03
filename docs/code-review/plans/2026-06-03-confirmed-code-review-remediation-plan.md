# Confirmed Code Review Remediation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remediate the confirmed and partially confirmed risks from the 2026-06-03 `docs/code-review` verification pass without reintroducing stale or false review findings.

**Architecture:** Treat this as a phased hardening program, not a single refactor. Each task produces a working, testable increment across backend security/config, engine correctness, HTTP/handler safety, frontend reliability, and observability. Keep compatibility by adding guarded prod behavior, tests first, and explicit migration paths.

**Tech Stack:** Java 21, Spring Boot WebFlux, Reactor, MyBatis-Plus, Flyway, Redis, RocketMQ, LMAX Disruptor, Groovy, React, Vite, Vitest, Axios, Docker.

---

## File Structure

- Modify: `backend/canvas-engine/src/main/resources/application.yml` for safe defaults and config keys.
- Create: `backend/canvas-engine/src/main/resources/application-prod.yml` for fail-closed production overrides.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java` for endpoint auth, headers, CSRF comments, and role restrictions.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ProductionConfigGuard.java` for startup validation.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/EventReportAuthService.java` for default-secret rejection and key version support.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java` for direct/behavior auth and request validation.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDO.java` and data source services for password encryption and log exclusion.
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/security/SecretCipher.java` for application-level encryption.
- Create migration: `backend/canvas-engine/src/main/resources/db/migration/V91__security_and_tenant_remediation.sql`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistry.java`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/infrastructure/mq/MqTriggerConsumer.java`.
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/WebClientConfig.java`.
- Modify selected handlers in `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/`.
- Modify frontend auth/API/notification/editor files in `frontend/src/`.
- Create/update tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/` and `frontend/src/**/*.test.ts(x)`.
- Create CI config under `.github/workflows/canvas-ci.yml`.

## Task 1: Production Configuration Guard

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/ProductionConfigGuard.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionConfigGuardTest.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Create: `backend/canvas-engine/src/main/resources/application-prod.yml`

- [ ] **Step 1: Write failing startup guard tests**

```java
class ProductionConfigGuardTest {
    @Test
    void rejectsWildcardCorsInProd() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("*"), true, "strong-secret-strong-secret-1234",
                "jwt-secret-jwt-secret-jwt-secret-1234", "canvas_app", "not-root");

        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::validate);

        assertTrue(ex.getMessage().contains("CORS wildcard"));
    }

    @Test
    void rejectsDefaultEventSecret() {
        ProductionConfigGuard guard = new ProductionConfigGuard(
                List.of("https://app.photonpay.com"), true,
                "canvas-event-report-secret-2026!!",
                "jwt-secret-jwt-secret-jwt-secret-1234", "canvas_app", "not-root");

        IllegalStateException ex = assertThrows(IllegalStateException.class, guard::validate);

        assertTrue(ex.getMessage().contains("event report secret"));
    }
}
```

- [ ] **Step 2: Run failing test**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=ProductionConfigGuardTest test
```

Expected: fail because `ProductionConfigGuard` does not exist.

- [ ] **Step 3: Implement guard**

```java
@Component
@Profile("prod")
public class ProductionConfigGuard implements SmartInitializingSingleton {
    private final List<String> allowedOrigins;
    private final boolean allowCredentials;
    private final String eventReportSecret;
    private final String jwtSecret;
    private final String datasourceUsername;
    private final String datasourcePassword;

    public ProductionConfigGuard(
            @Value("${canvas.cors.allowed-origins:}") List<String> allowedOrigins,
            @Value("${canvas.cors.allow-credentials:true}") boolean allowCredentials,
            @Value("${canvas.events.report-secret:}") String eventReportSecret,
            @Value("${canvas.jwt.secret:}") String jwtSecret,
            @Value("${spring.datasource.username:}") String datasourceUsername,
            @Value("${spring.datasource.password:}") String datasourcePassword) {
        this.allowedOrigins = allowedOrigins;
        this.allowCredentials = allowCredentials;
        this.eventReportSecret = eventReportSecret;
        this.jwtSecret = jwtSecret;
        this.datasourceUsername = datasourceUsername;
        this.datasourcePassword = datasourcePassword;
    }

    @Override
    public void afterSingletonsInstantiated() {
        validate();
    }

    void validate() {
        if (allowCredentials && allowedOrigins.contains("*")) {
            throw new IllegalStateException("CORS wildcard is forbidden in prod when credentials are allowed");
        }
        if (eventReportSecret == null || eventReportSecret.isBlank()
                || "canvas-event-report-secret-2026!!".equals(eventReportSecret)) {
            throw new IllegalStateException("event report secret must be configured and cannot use the default value");
        }
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("jwt secret must be configured in prod");
        }
        if ("root".equals(datasourceUsername) || "root".equals(datasourcePassword)) {
            throw new IllegalStateException("root database credentials are forbidden in prod");
        }
    }
}
```

- [ ] **Step 4: Add prod profile defaults**

```yaml
canvas:
  cors:
    allowed-origins: ${CANVAS_CORS_ALLOWED_ORIGINS}
  events:
    report-secret: ${CANVAS_EVENT_REPORT_SECRET}
  jwt:
    secret: ${CANVAS_JWT_SECRET}
management:
  endpoint:
    health:
      show-details: when-authorized
springdoc:
  swagger-ui:
    enabled: false
```

- [ ] **Step 5: Run verification**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=ProductionConfigGuardTest test
```

Expected: pass.

- [ ] **Step 6: Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/config/ProductionConfigGuard.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/config/ProductionConfigGuardTest.java \
  backend/canvas-engine/src/main/resources/application.yml \
  backend/canvas-engine/src/main/resources/application-prod.yml
git commit -m "fix: add production configuration guard"
```

## Task 2: Endpoint Security and Rate Limiting

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ExecutionController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/security/PublicTriggerAuthService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/security/PublicTriggerAuthServiceTest.java`
- Create/update: `backend/canvas-engine/src/test/java/org/chovy/canvas/config/SecurityConfigRouteTest.java`

- [ ] **Step 1: Test HMAC auth for public trigger endpoints**

```java
class PublicTriggerAuthServiceTest {
    @Test
    void rejectsMissingSignature() {
        PublicTriggerAuthService service = new PublicTriggerAuthService("strong-secret-strong-secret-1234", Clock.systemUTC());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.verify(new HttpHeaders(), "{}"));

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatusCode());
    }
}
```

- [ ] **Step 2: Implement shared HMAC verifier**

Use the same header contract as `EventReportAuthService`: `X-Canvas-Timestamp` and `X-Canvas-Signature`. Keep the signer implementation in one service, then inject it into `ExecutionController` for direct and behavior trigger.

```java
public void verify(HttpHeaders headers, String body) {
    if (secret == null || secret.isBlank()) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "public trigger secret not configured");
    }
    String timestamp = headers.getFirst(EventReportAuthService.TIMESTAMP_HEADER);
    String signature = headers.getFirst(EventReportAuthService.SIGNATURE_HEADER);
    if (timestamp == null || signature == null) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "missing signature headers");
    }
    verifyTimestamp(timestamp);
    verifySignature(timestamp, body == null ? "" : body, signature);
}
```

- [ ] **Step 3: Restrict ops**

Change `SecurityConfig` so `/ops/**` requires `ROLE_ADMIN` or the local role constants already used by admin routes.

```java
.pathMatchers("/ops/**").hasAnyRole(SUPER_ADMIN_ROUTE_ROLES)
```

- [ ] **Step 4: Add route authorization tests**

```java
@Test
void opsRequiresAdmin() {
    webTestClient.post().uri("/ops/canvas/1/cache/invalidate")
            .exchange()
            .expectStatus().isUnauthorized();
}
```

- [ ] **Step 5: Run tests**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=PublicTriggerAuthServiceTest,SecurityConfigRouteTest test
```

Expected: pass.

## Task 3: Data Source Password Encryption and Log Safety

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/security/SecretCipher.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDO.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/DataSourceConfigController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/JdbcConfigResolver.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/security/SecretCipherTest.java`
- Update: `backend/canvas-engine/src/test/java/org/chovy/canvas/dal/dataobject/DataSourceConfigDOTest.java`

- [ ] **Step 1: Write cipher round-trip test**

```java
class SecretCipherTest {
    @Test
    void encryptsAndDecrypts() {
        SecretCipher cipher = SecretCipher.fromBase64Key("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=");

        String encrypted = cipher.encrypt("db-password");

        assertNotEquals("db-password", encrypted);
        assertEquals("db-password", cipher.decrypt(encrypted));
    }
}
```

- [ ] **Step 2: Implement AES-GCM cipher**

```java
public String encrypt(String plaintext) {
    byte[] iv = new byte[12];
    secureRandom.nextBytes(iv);
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, iv));
    byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
    return "v1:" + Base64.getEncoder().encodeToString(iv) + ":" +
            Base64.getEncoder().encodeToString(ciphertext);
}
```

- [ ] **Step 3: Exclude password from Lombok toString/hash**

```java
@ToString.Exclude
@EqualsAndHashCode.Exclude
@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private String password;
```

- [ ] **Step 4: Encrypt writes and decrypt reads**

In `DataSourceConfigController.create/update`, encrypt `body.getPassword()` before persistence. In `JdbcConfigResolver`, decrypt after loading the DO and before building `JdbcConfig`.

- [ ] **Step 5: Run tests**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=SecretCipherTest,DataSourceConfigDOTest test
```

Expected: pass.

## Task 4: Transaction and In-Flight Concurrency Fixes

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasOpsService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasTransactionService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistry.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/trigger/InFlightExecutionRegistryConcurrencyTest.java`

- [ ] **Step 1: Add rollbackFor to transactions**

Change every bare transaction in the canvas service layer to:

```java
@Transactional(rollbackFor = Exception.class)
```

- [ ] **Step 2: Test deregister does not remove new entries**

```java
@ExtendWith(MockitoExtension.class)
class InFlightExecutionRegistryConcurrencyTest {
    @Mock StringRedisTemplate redis;
    @Mock RedisKeyUtil keys;

    InFlightExecutionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new InFlightExecutionRegistry(redis, keys);
        when(keys.inflightCanvas(1L)).thenReturn("canvas:inflight:1");
        when(keys.inflightLane(ExecutionLane.STANDARD)).thenReturn("canvas:inflight:lane:standard");
        when(keys.inflightGlobal()).thenReturn("canvas:inflight:global");
        when(redis.execute(any(RedisScript.class), anyList(), any(String[].class))).thenReturn(1L);
    }

    @RepeatedTest(100)
    void deregisterDoesNotRemoveConcurrentAcquire() throws Exception {
        CountDownLatch emptyObserved = new CountDownLatch(1);
        CountDownLatch newPutObserved = new CountDownLatch(1);
        CountDownLatch continueRemove = new CountDownLatch(1);
        PausingExecutionMap executionMap = new PausingExecutionMap(emptyObserved, newPutObserved, continueRemove);
        executionMap.put("old", Disposables.swap());

        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Long, ConcurrentHashMap<String, Disposable.Swap>> localRegistry =
                (ConcurrentHashMap<Long, ConcurrentHashMap<String, Disposable.Swap>>)
                        ReflectionTestUtils.getField(registry, "localRegistry");
        localRegistry.put(1L, executionMap);

        Thread remove = Thread.ofVirtual().start(() -> registry.deregister(1L, "old"));
        emptyObserved.await();
        Thread acquire = Thread.ofVirtual().start(() ->
                registry.tryAcquire(1L, "new", ExecutionLane.STANDARD, 10, 10, 10));
        newPutObserved.await(500, TimeUnit.MILLISECONDS);
        continueRemove.countDown();
        remove.join();
        acquire.join();

        assertNotNull(localRegistry.get(1L));
        assertTrue(localRegistry.get(1L).containsKey("new"));
    }

    static final class PausingExecutionMap extends ConcurrentHashMap<String, Disposable.Swap> {
        private final CountDownLatch emptyObserved;
        private final CountDownLatch newPutObserved;
        private final CountDownLatch continueRemove;

        PausingExecutionMap(CountDownLatch emptyObserved, CountDownLatch newPutObserved,
                            CountDownLatch continueRemove) {
            this.emptyObserved = emptyObserved;
            this.newPutObserved = newPutObserved;
            this.continueRemove = continueRemove;
        }

        @Override
        public Disposable.Swap put(String key, Disposable.Swap value) {
            Disposable.Swap previous = super.put(key, value);
            if ("new".equals(key)) {
                newPutObserved.countDown();
            }
            return previous;
        }

        @Override
        public boolean isEmpty() {
            boolean empty = super.isEmpty();
            if (empty) {
                emptyObserved.countDown();
                try {
                    assertTrue(continueRemove.await(2, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("interrupted while forcing deregister race");
                }
            }
            return empty;
        }
    }
}
```

- [ ] **Step 3: Replace check-then-act**

```java
localRegistry.computeIfPresent(canvasId, (id, executions) -> {
    executions.remove(executionId);
    return executions.isEmpty() ? null : executions;
});
```

- [ ] **Step 4: Run tests**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=InFlightExecutionRegistryConcurrencyTest test
```

Expected: pass.

## Task 5: ExecutionContext and CircuitBreaker Concurrency

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/context/ExecutionContext.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistry.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/context/ExecutionContextConcurrencyTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/scheduler/CircuitBreakerRegistryTest.java`

- [ ] **Step 1: Test ExecutionContext concurrent mutation**

```java
@Test
void triggerPayloadAndCallStackAreConcurrent() {
    ExecutionContext ctx = new ExecutionContext();

    IntStream.range(0, 1000).parallel().forEach(i -> {
        ctx.getTriggerPayload().put("k" + i, i);
        ctx.getCallStack().add((long) i);
    });

    assertEquals(1000, ctx.getTriggerPayload().size());
    assertEquals(1000, ctx.getCallStack().size());
}
```

- [ ] **Step 2: Use concurrent containers**

```java
private Map<String, Object> triggerPayload = new ConcurrentHashMap<>();
private List<Long> callStack = new CopyOnWriteArrayList<>();
```

- [ ] **Step 3: Synchronize CircuitBreaker transitions**

```java
public synchronized void checkState() {
    transitionIfOpenWindowElapsed();
    rejectIfOpenOrHalfOpenProbeExceeded();
}

public synchronized void recordSuccess() {
    if (state == State.HALF_OPEN) close();
    if (state == State.CLOSED) failures.set(0);
}

public synchronized void recordFailure() {
    if (state == State.HALF_OPEN || failures.incrementAndGet() >= failureThreshold) open();
}
```

- [ ] **Step 4: Run tests**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=ExecutionContextConcurrencyTest,CircuitBreakerRegistryTest test
```

Expected: pass.

## Task 6: Disruptor Lifecycle and Event Reset Safety

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/disruptor/CanvasDisruptorServiceLifecycleTest.java`

- [ ] **Step 1: Add test for error logs retaining event identifiers**

```java
@Test
void asyncErrorKeepsEventIdentifiers() {
    CanvasExecutionService executionService = mock(CanvasExecutionService.class);
    when(executionService.triggerFromDisruptor(anyLong(), anyString(), anyString(), anyString(), anyString(), anyMap(), any(), any(), any()))
            .thenReturn(Mono.error(new RuntimeException("boom")));

    CanvasDisruptorService service = new CanvasDisruptorService(executionService, requestExecutor, metrics, 1024, 1);

    service.publish(10L, "u1", "MQ", "MQ_TRIGGER", "topic", Map.of(), "msg-1");

    await().untilAsserted(() -> assertThat(logs).contains("canvasId=10"));
}
```

- [ ] **Step 2: Copy event fields before subscribing**

```java
Long canvasId = event.canvasId;
String userId = event.userId;
String triggerType = event.triggerType;
Map<String, Object> payload = event.payload == null ? Map.of() : Map.copyOf(event.payload);

executionService.triggerFromDisruptor(canvasId, userId, triggerType, triggerNodeType,
        matchKey, payload, msgId, lane, dispatchOptions)
    .doFinally(signal -> inFlight.decrementAndGet())
    .subscribe(null, e -> log.error("[DISRUPTOR] 执行失败 canvasId={} userId={}: {}",
            canvasId, userId, e.getMessage()));
```

- [ ] **Step 3: Track in-flight Mono count and shutdown**

```java
private final AtomicInteger inFlight = new AtomicInteger();
private final AtomicBoolean accepting = new AtomicBoolean(true);

@PreDestroy
public void shutdown() {
    accepting.set(false);
    waitForInFlight(Duration.ofSeconds(10));
    disruptor.shutdown();
}
```

- [ ] **Step 4: Run tests**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=CanvasDisruptorServiceLifecycleTest test
```

Expected: pass.

## Task 7: MQ Request Idempotency Regression

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestServiceIdempotencyTest.java`
- Create/update: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/request/CanvasExecutionRequestExecutorTest.java`
- Update: `backend/canvas-engine/src/test/java/org/chovy/canvas/infra/mq/MqTriggerConsumerTest.java`

- [ ] **Step 1: Test duplicate MQ sourceMsgId maps to the same request ID**

```java
@ExtendWith(MockitoExtension.class)
class CanvasExecutionRequestServiceIdempotencyTest {
    @Mock CanvasExecutionRequestMapper mapper;
    ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void duplicateMqSourceMessageBuildsSameRequestId() {
        CanvasExecutionRequestService service = new CanvasExecutionRequestService(mapper, objectMapper);

        String first = service.enqueue(10L, "user-1", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "ORDER_PAID", Map.of("orderId", "O-1"), "MSG-1");
        String second = service.enqueue(10L, "user-1", TriggerType.MQ, NodeType.MQ_TRIGGER,
                "ORDER_PAID", Map.of("orderId", "O-1"), "MSG-1");

        assertEquals(first, second);
        ArgumentCaptor<CanvasExecutionRequestDO> captor =
                ArgumentCaptor.forClass(CanvasExecutionRequestDO.class);
        verify(mapper, times(2)).insertIgnore(captor.capture());
        assertThat(captor.getAllValues()).extracting(CanvasExecutionRequestDO::getId)
                .containsExactly(first, first);
    }
}
```

- [ ] **Step 2: Test terminal duplicate request is skipped by executor**

```java
@Test
void terminalRequestIsNotExecutedAgain() {
    CanvasExecutionRequestMapper mapper = mock(CanvasExecutionRequestMapper.class);
    CanvasExecutionService executionService = mock(CanvasExecutionService.class);
    CanvasExecutionRequestDO request = new CanvasExecutionRequestDO();
    request.setId("mq-10-abc");
    request.setStatus(CanvasExecutionRequestStatus.SUCCEEDED);
    when(mapper.selectById("mq-10-abc")).thenReturn(request);

    CanvasExecutionRequestExecutor executor = new CanvasExecutionRequestExecutor(
            mapper, executionService, new ObjectMapper(), mock(CanvasMetrics.class),
            5000, 5, 300, 60000);

    executor.execute("mq-10-abc").block();

    verify(executionService, never()).triggerFromExecutionRequest(
            anyLong(), anyString(), anyString(), anyString(), any(), anyMap(), any(), anyInt(), any());
}
```

- [ ] **Step 3: Test consumer still publishes deterministic request IDs**

```java
@Test
void onMessagePublishesDeterministicRequestIdForDuplicateMessage() {
    when(routeService.getCanvasByMqTopic("ORDER_PAID")).thenReturn(Set.of("10"));
    when(requestService.enqueue(eq(10L), eq("user-1"), eq(TriggerType.MQ),
            eq(NodeType.MQ_TRIGGER), eq("ORDER_PAID"), anyMap(), eq("MSG-1")))
            .thenReturn("mq-10-deterministic");

    consumer.onMessage(message("ORDER_PAID", "MSG-1",
            "{\"userId\":\"user-1\",\"messageCode\":\"PAYMENT\",\"payload\":{\"orderId\":\"O-1\"}}"));
    consumer.onMessage(message("ORDER_PAID", "MSG-1",
            "{\"userId\":\"user-1\",\"messageCode\":\"PAYMENT\",\"payload\":{\"orderId\":\"O-1\"}}"));

    verify(disruptorService, times(2)).publishRequest("mq-10-deterministic");
}
```

- [ ] **Step 4: Run tests**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=CanvasExecutionRequestServiceIdempotencyTest,CanvasExecutionRequestExecutorTest,MqTriggerConsumerTest test
```

Expected: pass.

- [ ] **Step 5: Document schema decision**

Do not add a `UNIQUE KEY(source_msg_id)` as an idempotency fix. Current idempotency boundary is per `canvasId + triggerType + sourceMsgId`; a global `source_msg_id` unique key would break the valid case where one RocketMQ message fans out to multiple canvases.

## Task 8: WebClient Centralization and Response Size Limits

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/WebClientConfig.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ApiCallHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/CouponHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/ReachPlatformHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/TaggerOfflineHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/meta/TagImportSourceService.java`
- Create/update handler HTTP tests.

- [ ] **Step 1: Add WebClient codec and pool limits**

```java
return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemoryBytes));
```

Add provider options:

```java
.maxIdleTime(Duration.ofSeconds(maxIdleSeconds))
.maxLifeTime(Duration.ofMinutes(maxLifeMinutes))
.evictInBackground(Duration.ofSeconds(30))
```

- [ ] **Step 2: Inject builder instead of direct builder creation**

```java
public CouponHandler(WebClient.Builder builder,
                     @Value("${canvas.integration.coupon-service-url}") String url) {
    this.webClient = builder.baseUrl(url).build();
}
```

- [ ] **Step 3: Test oversized response**

```java
@Test
void oversizedCouponResponseFailsNode() {
    wireMock.stubFor(post("/issue").willReturn(okJson(largeJson(2_000_000))));

    NodeResult result = couponHandler.executeAsync(config, ctx).block();

    assertEquals(NodeResult.Status.FAIL, result.status());
}
```

- [ ] **Step 4: Run grep gate**

Run:

```bash
rg -n "WebClient\\.builder\\(" backend/canvas-engine/src/main/java
```

Expected: only `WebClientConfig` and explicitly justified construction sites remain.

## Task 9: Blocking Handler Remediation

**Files:**
- Modify handlers: `PointsOperationHandler`, `TagOperationHandler`, `GoalCheckHandler`, `TrackEventHandler`, `CreateTaskHandler`, `ManualApprovalHandler`, `SubFlowRefHandler`, `CanvasTriggerHandler`, `UpdateProfileHandler`, `CdpTagWriteHandler`, `FrequencyCapHandler`, `SuppressionCheckHandler`.
- Create/update per-handler tests.

- [ ] **Step 1: Wrap mapper/service calls**

Pattern:

```java
return Mono.fromCallable(() -> {
            CustomerProfileDO profile = profileMapper.selectOne(query);
            updateProfile(profile, ctx, config);
            return NodeResult.ok(nextNodeId, output);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> Mono.just(NodeResult.fail("UPDATE_PROFILE: " + e.getMessage())));
```

- [ ] **Step 2: Add bad-config tests**

```java
@Test
void couponRejectsMissingCouponTypeKey() {
    NodeResult result = couponHandler.executeAsync(Map.of(), ctx).block();

    assertEquals(NodeResult.Status.FAIL, result.status());
    assertTrue(result.errorMessage().contains("couponTypeKey"));
}
```

- [ ] **Step 3: Add idempotency conflict handling**

For points/tag upsert, catch duplicate-key exceptions and return a deterministic idempotent result.

```java
catch (DuplicateKeyException e) {
    return NodeResult.ok(nextNodeId, Map.of("idempotent", true));
}
```

- [ ] **Step 4: Run handler test suite**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest='*HandlerTest' test
```

Expected: pass.

## Task 10: WAIT Resume Regression and Stale Comment Cleanup

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/wait/WaitResumeService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/trigger/CanvasExecutionService.java`
- Update: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/wait/WaitResumeServiceTest.java`

- [ ] **Step 1: Add regression test**

```java
@Test
void waitResumeBypassesQuotaAndCooldown() {
    waitResumeService.resume(wait, payload, WaitSubscriptionService.STATUS_COMPLETED);

    verify(preCheckService, never()).checkWithoutQuotaAccounting(any(), anyString());
    verify(preCheckService, never()).consumeQuota(any(), anyString());
}
```

- [ ] **Step 2: Remove stale warning comment**

Replace the old “已知问题” comment with:

```java
/**
 * WAIT/GOAL 恢复触发会使用 TriggerType.WAIT_RESUME/GOAL_CHECK_RESUME。
 * CanvasExecutionService.isInternalContinuationTrigger 会跳过冷却期和配额扣减，
 * 防止恢复路径被当成新触发。
 */
```

- [ ] **Step 3: Run tests**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=WaitResumeServiceTest test
```

Expected: pass.

## Task 11: Frontend Auth/API/Notification Reliability

**Files:**
- Modify: `frontend/src/context/AuthContext.tsx`
- Modify: `frontend/src/services/api.ts`
- Modify: `frontend/src/context/NotificationContext.tsx`
- Create/update: `frontend/src/context/AuthContext.test.tsx`
- Create/update: `frontend/src/services/api.test.ts`
- Create/update: `frontend/src/context/NotificationContext.test.tsx`

- [ ] **Step 1: Remove token logging test**

```tsx
it('does not log token on auth init', () => {
  const spy = vi.spyOn(console, 'log').mockImplementation(() => undefined)
  localStorage.setItem('canvas_token', 'secret-token')

  render(<AuthProvider><Probe /></AuthProvider>)

  expect(spy).not.toHaveBeenCalledWith(expect.stringContaining('secret-token'))
})
```

- [ ] **Step 2: Check business code in interceptor**

```ts
http.interceptors.response.use((res) => {
  const payload = res.data
  if (payload && typeof payload === 'object' && 'code' in payload && payload.code !== 0) {
    return Promise.reject(new ApiBusinessError(payload.code, payload.message, payload.data))
  }
  return payload
})
```

- [ ] **Step 3: Make notification socket ownership explicit**

```ts
const socketId = crypto.randomUUID()
activeSocketIdRef.current = socketId

socket.onclose = () => {
  if (activeSocketIdRef.current !== socketId) return
  wsRef.current = null
  scheduleReconnect()
}
```

- [ ] **Step 4: Add retry cap**

```ts
if (reconnectAttemptsRef.current >= MAX_RECONNECT_ATTEMPTS) {
  setRealtimeState('polling')
  return
}
reconnectAttemptsRef.current += 1
```

- [ ] **Step 5: Run frontend tests**

Run:

```bash
npm --prefix frontend test
```

Expected: pass.

## Task 12: Canvas Editor Safety Tests

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Create/update: `frontend/src/pages/canvas-editor/canvasEditorAutosave.test.tsx`
- Create/update: `frontend/src/pages/canvas-editor/canvasEditorClipboard.test.tsx`
- Create/update: `frontend/src/pages/canvas-editor/outletRouting.test.ts`

- [ ] **Step 1: Test auto-save fires after edits**

```tsx
vi.useFakeTimers()
fireEvent.change(screen.getByLabelText('画布名称'), { target: { value: 'new name' } })
vi.advanceTimersByTime(3000)

expect(localStorage.getItem(expect.stringContaining('canvas-draft'))).toContain('new name')
```

- [ ] **Step 2: Use refs for latest editor state**

```ts
const latestGraphRef = useRef({ nodes, edges })
useEffect(() => {
  latestGraphRef.current = { nodes, edges }
}, [nodes, edges])
```

- [ ] **Step 3: Deep-clone pasted node config**

```ts
const clonedBizConfig = typeof structuredClone === 'function'
  ? structuredClone(source.data.bizConfig)
  : JSON.parse(JSON.stringify(source.data.bizConfig))
```

- [ ] **Step 4: Run targeted tests**

Run:

```bash
npm --prefix frontend test -- canvas-editor
```

Expected: pass.

## Task 13: Observability and Health Checks

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/R.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/GlobalExceptionHandler.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/scheduler/CanvasMetrics.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/health/CanvasEngineHealthIndicator.java`
- Create tests under `backend/canvas-engine/src/test/java/org/chovy/canvas/health/`.

- [ ] **Step 1: Add traceId to response wrapper**

```java
private String traceId;

public static <T> R<T> fail(int code, String message, String traceId) {
    R<T> r = fail(code, message);
    r.setTraceId(traceId);
    return r;
}
```

- [ ] **Step 2: Populate traceId in exception handler**

```java
String traceId = Optional.ofNullable(MDC.get("traceId"))
        .orElseGet(() -> UUID.randomUUID().toString());
return Mono.just(R.fail(500, "系统错误", traceId));
```

- [ ] **Step 3: Add HealthIndicator checks**

```java
public Health health() {
    Health.Builder builder = Health.up();
    builder.withDetail("disruptorBacklog", disruptorService.backlog());
    builder.withDetail("traceBufferPending", traceWriteBuffer.pendingCount());
    return builder.build();
}
```

- [ ] **Step 4: Run health tests**

Run:

```bash
mvn -f backend/pom.xml -pl canvas-engine -am -Dtest=CanvasEngineHealthIndicatorTest,GlobalExceptionHandlerTest test
```

Expected: pass.

## Task 14: CI and Audit Gates

**Files:**
- Create: `.github/workflows/canvas-ci.yml`
- Modify: `frontend/package.json` only if adding an audit script is useful.

- [ ] **Step 1: Add workflow**

```yaml
name: canvas-ci
on:
  pull_request:
  push:
    branches: [main]
jobs:
  backend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '21'
      - run: mvn -f backend/pom.xml test
  frontend:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: '22'
          cache: npm
          cache-dependency-path: frontend/package-lock.json
      - run: npm ci
        working-directory: frontend
      - run: npm test
        working-directory: frontend
      - run: npm run build
        working-directory: frontend
      - run: npm audit --audit-level=critical
        working-directory: frontend
```

- [ ] **Step 2: Run local equivalent**

Run:

```bash
mvn -f backend/pom.xml test
npm --prefix frontend test
npm --prefix frontend run build
npm --prefix frontend audit --audit-level=critical
```

Expected: tests/build pass; audit fails only if an accepted advisory is documented in the PR.

## Task 15: Runtime Verification Scripts

**Files:**
- Create: `scripts/verify-cors.sh`
- Create: `scripts/verify-mq-idempotency.sh`
- Create: `scripts/verify-ws-limit.sh`
- Create: `docs/code-review/runtime-verification.md`

- [ ] **Step 1: Add CORS smoke script**

```bash
#!/usr/bin/env bash
set -euo pipefail
BASE_URL="${BASE_URL:-http://localhost:8080}"
origin="${1:-https://evil.example}"
curl -sS -D - -o /dev/null \
  -H "Origin: ${origin}" \
  -H "Access-Control-Request-Method: POST" \
  -X OPTIONS "${BASE_URL}/canvas/trigger/behavior" |
  tee /tmp/canvas-cors-check.txt
! grep -q "Access-Control-Allow-Origin: ${origin}" /tmp/canvas-cors-check.txt
```

- [ ] **Step 2: Add runtime verification doc**

```markdown
# Runtime Verification

## CORS
Run: `BASE_URL=http://localhost:8080 scripts/verify-cors.sh https://evil.example`
Expected: command exits 0 and does not echo the malicious origin as allowed.

## MQ Idempotency
Publish the same RocketMQ message id twice through the test producer.
Expected: one deterministic `requestId` per matched canvas, and duplicate publishes of the same `requestId` do not execute terminal requests again.
```

- [ ] **Step 3: Run shellcheck or basic syntax check**

Run:

```bash
bash -n scripts/verify-cors.sh
bash -n scripts/verify-mq-idempotency.sh
bash -n scripts/verify-ws-limit.sh
```

Expected: all commands exit 0.

## Final Verification

- [ ] **Step 1: Run backend tests**

```bash
mvn -f backend/pom.xml test
```

Expected: all tests pass.

- [ ] **Step 2: Run frontend tests and build**

```bash
npm --prefix frontend test
npm --prefix frontend run build
```

Expected: all tests pass and build succeeds.

- [ ] **Step 3: Run dependency checks**

```bash
npm --prefix frontend audit --audit-level=critical
mvn -f backend/pom.xml dependency:list -DincludeArtifactIds=jackson-databind,hutool-all,commons-validator
```

Expected: no unaccepted critical frontend advisory; backend dependency versions are documented in the PR.

- [ ] **Step 4: Reconcile verification ledger**

Update `docs/code-review/verification/2026-06-03-code-review-verification.md` rows from `成立/部分成立/需运行验证` to `已修复` only for items directly fixed and verified by tests or runtime scripts.

## Execution Order

1. Tasks 1-2: close public-entry and prod-config risk.
2. Tasks 3-7: close data credential, transaction, concurrency, Disruptor, and MQ idempotency risks.
3. Tasks 8-10: close HTTP/handler/WAIT regressions.
4. Tasks 11-12: close frontend auth/notification/editor reliability.
5. Tasks 13-15: close observability, CI, and runtime validation.

Do not combine unrelated tasks into one commit. Each task should leave the repo testable.
