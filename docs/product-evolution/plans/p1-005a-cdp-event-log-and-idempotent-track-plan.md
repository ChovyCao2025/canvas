# CDP Event Log And Idempotent Track Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `/cdp/events/track` batch ingestion with enriched event log rows and duplicate protection.

**Architecture:** Keep `/canvas/events/report` unchanged. The new controller authenticates write keys, then calls `CdpEventIngestionService`, which validates event definitions, persists accepted rows, ensures profile identity when available, and reports per-item errors.

**Tech Stack:** Java 21, Spring Boot WebFlux, MyBatis-Plus, Flyway, Jackson, JUnit 5, Mockito, AssertJ.

---

## Implementation Status

Status: implemented on 2026-06-05.

Notes:
- Actual migration version is `V101__cdp_event_log_and_track_endpoint.sql`; the original `V97_1` slot was superseded by existing migrations.
- `CdpEventLogDO`, `CdpEventLogMapper`, DTOs, and `CdpEventIngestionService` already existed before this slice and were verified against this plan.
- This slice added `CdpEventIngestionController`, security filter-level permit for `POST /cdp/events/track`, explicit `canvas.cdp.ingestion.max-batch-size`, and focused schema/service/controller/security tests.
- Commit was not performed in this session.

Verification evidence:
- Red run: `mvn -pl canvas-engine test -Dtest=CdpEventLogSchemaTest,CdpEventIngestionServiceTest,CdpEventIngestionControllerTest,SecurityConfigRouteTest -DfailIfNoTests=true` failed at test compile because `CdpEventIngestionController` was missing.
- Green run: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=CdpEventLogSchemaTest,CdpEventIngestionServiceTest,CdpEventIngestionControllerTest,SecurityConfigRouteTest -DfailIfNoTests=true`
- Result: 11 tests run, 0 failures, 0 errors, 0 skipped.

## Spec Reference

- `docs/product-evolution/specs/p1-005a-cdp-event-log-and-idempotent-track.md`
- Depends on `docs/product-evolution/specs/p1-005-cdp-write-key-management-and-authentication.md`

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V101__cdp_event_log_and_track_endpoint.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventLogSchemaTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpEventLogDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpEventLogMapper.java`
- Create: DTOs under `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpEventIngestionController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpEventIngestionControllerTest.java`

### Task 1: Event Log Schema And DTOs

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventLogSchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V101__cdp_event_log_and_track_endpoint.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpEventLogDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpEventLogMapper.java`
- Create: DTOs under `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp/`

- [x] **Step 1: Write schema test**

```java
class CdpEventLogSchemaTest {

    @Test
    void migrationCreatesEnrichedEventLogWithDuplicateKeys() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V101__cdp_event_log_and_track_endpoint.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS cdp_event_log")
                .contains("write_key_id")
                .contains("message_id")
                .contains("anonymous_id")
                .contains("session_id")
                .contains("device_id")
                .contains("sdk_context")
                .contains("properties")
                .contains("UNIQUE KEY uk_cdp_event_message")
                .contains("UNIQUE KEY uk_cdp_event_idempotency");
    }
}
```

- [x] **Step 2: Run schema test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpEventLogSchemaTest
```

Expected: FAIL because migration does not exist.

- [x] **Step 3: Add migration**

Create `V101__cdp_event_log_and_track_endpoint.sql` with `cdp_event_log`:

```sql
CREATE TABLE IF NOT EXISTS cdp_event_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    write_key_id BIGINT NOT NULL,
    message_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(20) NOT NULL DEFAULT 'track',
    event_code VARCHAR(128) NULL,
    user_id VARCHAR(128) NULL,
    anonymous_id VARCHAR(128) NULL,
    session_id VARCHAR(128) NULL,
    device_id VARCHAR(128) NULL,
    platform VARCHAR(32) NULL,
    sdk_context JSON NULL,
    properties JSON NULL,
    idempotency_key VARCHAR(160) NULL,
    event_time DATETIME(3) NOT NULL,
    sent_at DATETIME(3) NULL,
    received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    status VARCHAR(20) NOT NULL DEFAULT 'ACCEPTED',
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cdp_event_message (tenant_id, message_id),
    UNIQUE KEY uk_cdp_event_idempotency (tenant_id, idempotency_key),
    INDEX idx_cdp_event_user_time (tenant_id, user_id, event_time),
    INDEX idx_cdp_event_code_time (tenant_id, event_code, event_time),
    INDEX idx_cdp_event_anonymous_time (tenant_id, anonymous_id, event_time),
    INDEX idx_cdp_event_session_time (tenant_id, session_id, event_time),
    INDEX idx_cdp_event_write_key_time (tenant_id, write_key_id, received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP event ingestion log';
```

- [x] **Step 4: Add data object, mapper, and DTOs**

Create `CdpEventLogDO.java`:

```java
@Data
@TableName("cdp_event_log")
public class CdpEventLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long writeKeyId;
    private String messageId;
    private String eventType;
    private String eventCode;
    private String userId;
    private String anonymousId;
    private String sessionId;
    private String deviceId;
    private String platform;
    private String sdkContext;
    private String properties;
    private String idempotencyKey;
    private LocalDateTime eventTime;
    private LocalDateTime sentAt;
    private LocalDateTime receivedAt;
    private String status;
    private String errorMessage;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
```

Create records:

```java
public record BatchTrackReq(List<TrackEventReq> batch, OffsetDateTime sentAt) {
}

public record TrackEventReq(
        String messageId,
        String type,
        String event,
        String userId,
        String anonymousId,
        String idempotencyKey,
        Map<String, Object> properties,
        Map<String, Object> context,
        OffsetDateTime timestamp,
        OffsetDateTime sentAt
) {
}

public record IngestionResult(int accepted, int rejected, List<IngestionError> errors) {
}

public record IngestionError(String messageId, String code, String message) {
}
```

- [x] **Step 5: Run schema test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpEventLogSchemaTest
```

Expected: PASS.

### Task 2: Ingestion Service

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`

- [x] **Step 1: Write service tests**

Create `CdpEventIngestionServiceTest.java` with these tests:

```java
@Test
void ingestStoresBatchEventsWithSdkContext() {
    TrackEventReq event = new TrackEventReq(
            "msg-1",
            "track",
            "OrderComplete",
            "user-1",
            "anon-1",
            "idem-1",
            Map.of("amount", 99.9, "currency", "CNY"),
            Map.of("session", Map.of("sessionId", "sess-1"),
                   "device", Map.of("id", "dev-1"),
                   "library", Map.of("name", "@canvas/analytics-web")),
            OffsetDateTime.parse("2026-05-30T10:00:00Z"),
            OffsetDateTime.parse("2026-05-30T10:00:01Z"));

    IngestionResult result = service.ingestBatch(key(), new BatchTrackReq(List.of(event), event.sentAt()));

    ArgumentCaptor<CdpEventLogDO> captor = ArgumentCaptor.forClass(CdpEventLogDO.class);
    verify(eventLogMapper).insert(captor.capture());
    assertThat(result.accepted()).isEqualTo(1);
    assertThat(captor.getValue().getMessageId()).isEqualTo("msg-1");
    assertThat(captor.getValue().getEventCode()).isEqualTo("OrderComplete");
    assertThat(captor.getValue().getSessionId()).isEqualTo("sess-1");
    assertThat(captor.getValue().getDeviceId()).isEqualTo("dev-1");
    assertThat(captor.getValue().getProperties()).contains("amount");
    verify(userService).ensureUser("user-1", "CDP_EVENT", "OrderComplete");
}

@Test
void ingestSkipsDuplicateMessageIdWithinTenant() {
    when(eventLogMapper.selectCount(any())).thenReturn(1L);

    IngestionResult result = service.ingestBatch(key(), new BatchTrackReq(List.of(validEvent("msg-1")), OffsetDateTime.now()));

    assertThat(result.accepted()).isZero();
    verify(eventLogMapper, never()).insert(any());
}

@Test
void ingestRejectsUnknownTrackEvent() {
    when(eventDefinitionCacheService.getPublishedByCode("Unknown")).thenReturn(null);

    IngestionResult result = service.ingestBatch(key(), new BatchTrackReq(List.of(validEvent("msg-1", "Unknown")), OffsetDateTime.now()));

    assertThat(result.rejected()).isEqualTo(1);
    assertThat(result.errors().get(0).message()).contains("unknown event code");
}
```

Use helper methods in the same test class to build `key()` and `validEvent(...)`; mock `eventDefinitionCacheService.getPublishedByCode("OrderComplete")` to return a published `EventDefinitionDO`.

- [x] **Step 2: Run service tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpEventIngestionServiceTest
```

Expected: FAIL because ingestion service does not exist.

- [x] **Step 3: Implement service core**

Create `CdpEventIngestionService.java`:

```java
@Service
@RequiredArgsConstructor
public class CdpEventIngestionService {
    private final CdpEventLogMapper eventLogMapper;
    private final EventDefinitionCacheService eventDefinitionCacheService;
    private final CdpUserService userService;
    private final ObjectMapper objectMapper;

    @Value("${canvas.cdp.ingestion.max-batch-size:100}")
    private int maxBatchSize;

    public IngestionResult ingestBatch(CdpWriteKeyAuthService.AuthenticatedWriteKey key, BatchTrackReq req) {
        List<TrackEventReq> batch = req.batch() == null ? List.of() : req.batch();
        if (batch.size() > maxBatchSize) {
            return new IngestionResult(0, batch.size(),
                    List.of(new IngestionError(null, "BATCH_TOO_LARGE", "batch size exceeds " + maxBatchSize)));
        }
        int accepted = 0;
        List<IngestionError> errors = new ArrayList<>();
        for (TrackEventReq event : batch) {
            try {
                if (ingestOne(key, event, req.sentAt())) {
                    accepted++;
                }
            } catch (IllegalArgumentException ex) {
                errors.add(new IngestionError(event.messageId(), "INVALID_EVENT", ex.getMessage()));
            }
        }
        return new IngestionResult(accepted, errors.size(), errors);
    }

    protected boolean ingestOne(CdpWriteKeyAuthService.AuthenticatedWriteKey key, TrackEventReq event, OffsetDateTime batchSentAt) {
        requireText(event.messageId(), "messageId");
        requireText(event.type(), "type");
        if (isDuplicateMessage(key.tenantId(), event.messageId()) || isDuplicateIdempotency(key.tenantId(), event.idempotencyKey())) {
            return false;
        }
        if ("track".equalsIgnoreCase(event.type())) {
            requireText(event.event(), "event");
            if (eventDefinitionCacheService.getPublishedByCode(event.event()) == null) {
                throw new IllegalArgumentException("unknown event code: " + event.event());
            }
        }
        if (event.userId() != null && !event.userId().isBlank()) {
            userService.ensureUser(event.userId(), "CDP_EVENT", event.event());
        }
        CdpEventLogDO row = toRow(key, event, batchSentAt);
        try {
            eventLogMapper.insert(row);
            return true;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }
}
```

Add private helpers `toRow`, `isDuplicateMessage`, `isDuplicateIdempotency`, `readContextString`, `writeJson`, `toLocal`, `blankToNull`, and `requireText` exactly matching the DTO fields in the spec.

- [x] **Step 4: Run service tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpEventIngestionServiceTest
```

Expected: PASS.

### Task 3: Controller And Security

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpEventIngestionControllerTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpEventIngestionController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`

- [x] **Step 1: Write controller auth test**

```java
@Test
void trackAuthenticatesBeforeIngestion() {
    CdpWriteKeyAuthService auth = mock(CdpWriteKeyAuthService.class);
    CdpEventIngestionService ingestion = mock(CdpEventIngestionService.class);
    CdpEventIngestionController controller = new CdpEventIngestionController(auth, ingestion);
    var key = new CdpWriteKeyAuthService.AuthenticatedWriteKey(7L, 42L, "ck_test_abc", "WEB", 100, null);
    when(auth.authenticate(any())).thenReturn(key);
    when(ingestion.ingestBatch(eq(key), any())).thenReturn(new IngestionResult(1, 0, List.of()));

    BatchTrackReq req = new BatchTrackReq(List.of(validEvent()), OffsetDateTime.now());
    var httpReq = MockServerHttpRequest.post("/cdp/events/track").build();

    IngestionResult result = controller.track(httpReq, Mono.just(req)).block().getData();

    assertThat(result.accepted()).isEqualTo(1);
    verify(auth).authenticate(httpReq.getHeaders());
    verify(ingestion).ingestBatch(eq(key), eq(req));
}
```

- [x] **Step 2: Add controller**

Create `CdpEventIngestionController.java`:

```java
@RestController
@RequestMapping("/cdp/events")
@RequiredArgsConstructor
public class CdpEventIngestionController {
    private final CdpWriteKeyAuthService writeKeyAuthService;
    private final CdpEventIngestionService ingestionService;

    @PostMapping("/track")
    public Mono<R<IngestionResult>> track(ServerHttpRequest request, @RequestBody Mono<BatchTrackReq> body) {
        return body.flatMap(req -> Mono.fromCallable(() -> {
            var key = writeKeyAuthService.authenticate(request.getHeaders());
            return R.ok(ingestionService.ingestBatch(key, req));
        }).subscribeOn(Schedulers.boundedElastic()));
    }
}
```

- [x] **Step 3: Permit route and add config**

In `SecurityConfig`, permit:

```java
.pathMatchers(HttpMethod.POST, "/cdp/events/track").permitAll()
```

In `application.yml`:

```yaml
canvas:
  cdp:
    ingestion:
      max-batch-size: ${CANVAS_CDP_INGESTION_MAX_BATCH_SIZE:100}
```

- [x] **Step 4: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=CdpEventLogSchemaTest,CdpEventIngestionServiceTest,CdpEventIngestionControllerTest
```

Expected: PASS.

### Task 4: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-005a-cdp-event-log-and-idempotent-track.md`
- Read: `docs/product-evolution/plans/p1-005a-cdp-event-log-and-idempotent-track-plan.md`

- [ ] **Step 1: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V101__cdp_event_log_and_track_endpoint.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpEventLogDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpEventLogMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dto/cdp \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpEventIngestionController.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/config/SecurityConfig.java \
  backend/canvas-engine/src/main/resources/application.yml \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventLogSchemaTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/web/CdpEventIngestionControllerTest.java \
  docs/product-evolution/specs/p1-005a-cdp-event-log-and-idempotent-track.md \
  docs/product-evolution/plans/p1-005a-cdp-event-log-and-idempotent-track-plan.md
git commit -m "feat: add cdp track ingestion"
```

Expected: commit contains only CDP track endpoint, event log, ingestion service, tests, and docs.
