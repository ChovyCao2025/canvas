# Event Attribute Discovery And Internal CDP Event Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add pending-review event attribute discovery and emit one compact internal CDP event for each accepted ingestion row.

**Architecture:** Extend event definitions with discovery controls, store discovered properties in `event_attr_definition`, and inject discovery/publisher collaborators into `CdpEventIngestionService`. Publish only after the accepted event row insert succeeds.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, RocketMQ, Jackson, JUnit 5, Mockito, AssertJ.

---

## Implementation Status

Status: implemented on 2026-06-05.

Notes:
- Actual migration version is `V102__event_attribute_discovery_internal_event.sql`; the original `V97_2` slot was superseded by existing migrations.
- `EventAttrDefinitionDO`, `EventAttrDefinitionMapper`, `EventDefinitionDO` discovery fields, `EventAttributeDiscoveryService`, `CdpEventPublisher`, and the ingestion hook already existed before this slice and were verified against this plan.
- This slice added explicit `canvas.cdp.event-topic` configuration plus focused schema, discovery service, publisher, and ingestion hook tests.
- Commit was not performed in this session.

Verification evidence:
- Red run: focused Maven initially failed before execution because `canvas.cdp.event-topic` was not configured, then exposed a test Mockito overload ambiguity that was fixed in test code.
- Green run: `JAVA_HOME=/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home PATH="/Users/photonpay/Library/Java/JavaVirtualMachines/ms-21.0.11/Contents/Home/bin:$PATH" mvn -pl canvas-engine test -Dtest=EventAttributeDiscoverySchemaTest,EventAttributeDiscoveryServiceTest,CdpEventPublisherTest,CdpEventIngestionServiceTest -DfailIfNoTests=true`
- Result: 11 tests run, 0 failures, 0 errors, 0 skipped.

## Spec Reference

- `docs/product-evolution/specs/p1-005a2-event-attribute-discovery-and-internal-cdp-event.md`
- Depends on `docs/product-evolution/specs/p1-005a-cdp-event-log-and-idempotent-track.md`

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V102__event_attribute_discovery_internal_event.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoverySchemaTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventAttrDefinitionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/EventAttrDefinitionMapper.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventDefinitionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoveryService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventPublisher.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoveryServiceTest.java`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionServiceTest.java`

### Task 1: Discovery Schema

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoverySchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V102__event_attribute_discovery_internal_event.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventAttrDefinitionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/EventAttrDefinitionMapper.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventDefinitionDO.java`

- [x] **Step 1: Write schema test**

```java
class EventAttributeDiscoverySchemaTest {

    @Test
    void migrationAddsDiscoveryControlsAndAttributeTable() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V102__event_attribute_discovery_internal_event.sql"));

        assertThat(sql)
                .contains("ALTER TABLE event_definition")
                .contains("auto_discover")
                .contains("discovery_mode")
                .contains("CREATE TABLE IF NOT EXISTS event_attr_definition")
                .contains("attr_type")
                .contains("PENDING_REVIEW")
                .contains("UNIQUE KEY uk_event_attr_definition");
    }
}
```

- [x] **Step 2: Run schema test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=EventAttributeDiscoverySchemaTest
```

Expected: FAIL because migration does not exist.

- [x] **Step 3: Add migration**

Create `V102__event_attribute_discovery_internal_event.sql`:

```sql
ALTER TABLE event_definition
    ADD COLUMN auto_discover TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether SDK ingestion can discover new attributes',
    ADD COLUMN discovery_mode VARCHAR(32) NOT NULL DEFAULT 'REJECT_UNKNOWN' COMMENT 'REJECT_UNKNOWN or PENDING_REVIEW';

CREATE TABLE IF NOT EXISTS event_attr_definition (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    event_code VARCHAR(128) NOT NULL,
    attr_name VARCHAR(128) NOT NULL,
    attr_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_REVIEW',
    sample_value VARCHAR(1000) NULL,
    first_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by VARCHAR(128) NULL,
    approved_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_event_attr_definition (tenant_id, event_code, attr_name),
    INDEX idx_event_attr_status (tenant_id, status, last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Discovered event attribute definitions';
```

- [x] **Step 4: Add data object and event definition fields**

Create `EventAttrDefinitionDO.java`:

```java
@Data
@TableName("event_attr_definition")
public class EventAttrDefinitionDO {
    public static final String PENDING_REVIEW = "PENDING_REVIEW";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String eventCode;
    private String attrName;
    private String attrType;
    private String status;
    private String sampleValue;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
    private String approvedBy;
    private LocalDateTime approvedAt;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

Create mapper:

```java
@Mapper
public interface EventAttrDefinitionMapper extends BaseMapper<EventAttrDefinitionDO> {
}
```

Add to `EventDefinitionDO`:

```java
private Integer autoDiscover;
private String discoveryMode;
```

- [x] **Step 5: Run schema test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=EventAttributeDiscoverySchemaTest
```

Expected: PASS.

### Task 2: Attribute Discovery Service

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoveryServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoveryService.java`

- [x] **Step 1: Write discovery tests**

```java
@Test
void discoverCreatesPendingReviewRowsWithInferredType() {
    EventAttributeDiscoveryService service = new EventAttributeDiscoveryService(attrMapper);
    when(attrMapper.selectOne(any())).thenReturn(null);

    service.discover(42L, "OrderComplete", Map.of("amount", 99.9, "paid", true));

    ArgumentCaptor<EventAttrDefinitionDO> captor = ArgumentCaptor.forClass(EventAttrDefinitionDO.class);
    verify(attrMapper, times(2)).insert(captor.capture());
    assertThat(captor.getAllValues()).extracting(EventAttrDefinitionDO::getStatus)
            .containsOnly("PENDING_REVIEW");
    assertThat(captor.getAllValues()).extracting(EventAttrDefinitionDO::getAttrType)
            .contains("NUMBER", "BOOLEAN");
}

@Test
void discoverUpdatesLastSeenWithoutChangingStatus() {
    EventAttrDefinitionDO existing = new EventAttrDefinitionDO();
    existing.setId(9L);
    existing.setStatus(EventAttrDefinitionDO.APPROVED);
    when(attrMapper.selectOne(any())).thenReturn(existing);

    new EventAttributeDiscoveryService(attrMapper).discover(42L, "OrderComplete", Map.of("amount", 99.9));

    verify(attrMapper).updateById(argThat(row -> row.getId().equals(9L)
            && row.getStatus().equals(EventAttrDefinitionDO.APPROVED)
            && row.getLastSeenAt() != null));
}

@Test
void inferTypeClassifiesJsonAndDateLikeStrings() {
    EventAttributeDiscoveryService service = new EventAttributeDiscoveryService(attrMapper);

    assertThat(service.inferType(Map.of("nested", true))).isEqualTo("JSON");
    assertThat(service.inferType("2026-05-30T10:00:00Z")).isEqualTo("DATE");
    assertThat(service.inferType("CNY")).isEqualTo("STRING");
}
```

- [x] **Step 2: Run discovery tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=EventAttributeDiscoveryServiceTest
```

Expected: FAIL because service does not exist.

- [x] **Step 3: Implement discovery service**

Create `EventAttributeDiscoveryService.java`:

```java
@Service
@RequiredArgsConstructor
public class EventAttributeDiscoveryService {
    private final EventAttrDefinitionMapper attrMapper;

    public void discover(Long tenantId, String eventCode, Map<String, Object> properties) {
        if (properties == null || properties.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (var entry : properties.entrySet()) {
            String attrName = entry.getKey();
            EventAttrDefinitionDO existing = attrMapper.selectOne(new LambdaQueryWrapper<EventAttrDefinitionDO>()
                    .eq(EventAttrDefinitionDO::getTenantId, tenantId)
                    .eq(EventAttrDefinitionDO::getEventCode, eventCode)
                    .eq(EventAttrDefinitionDO::getAttrName, attrName)
                    .last("LIMIT 1"));
            if (existing != null) {
                existing.setLastSeenAt(now);
                attrMapper.updateById(existing);
                continue;
            }
            EventAttrDefinitionDO created = new EventAttrDefinitionDO();
            created.setTenantId(tenantId);
            created.setEventCode(eventCode);
            created.setAttrName(attrName);
            created.setAttrType(inferType(entry.getValue()));
            created.setStatus(EventAttrDefinitionDO.PENDING_REVIEW);
            created.setSampleValue(sample(entry.getValue()));
            created.setFirstSeenAt(now);
            created.setLastSeenAt(now);
            attrMapper.insert(created);
        }
    }

    String inferType(Object value) {
        if (value instanceof Number) return "NUMBER";
        if (value instanceof Boolean) return "BOOLEAN";
        if (value instanceof Map<?, ?> || value instanceof Iterable<?>) return "JSON";
        String text = value == null ? "" : String.valueOf(value);
        return text.matches("\\d{4}-\\d{2}-\\d{2}.*") ? "DATE" : "STRING";
    }

    private String sample(Object value) {
        if (value == null) return null;
        String text = String.valueOf(value);
        return text.length() <= 1000 ? text : text.substring(0, 1000);
    }
}
```

- [x] **Step 4: Run discovery tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=EventAttributeDiscoveryServiceTest
```

Expected: PASS.

### Task 3: Internal Event Publisher And Ingestion Hook

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventPublisher.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Modify: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionServiceTest.java`

- [x] **Step 1: Extend ingestion tests**

Add tests:

```java
@Test
void acceptedAutoDiscoverableEventDiscoversAttributesAndPublishesInternalEvent() {
    EventDefinitionDO def = new EventDefinitionDO();
    def.setCode("OrderComplete");
    def.setAutoDiscover(1);
    when(eventDefinitionCacheService.getPublishedByCode("OrderComplete")).thenReturn(def);

    service.ingestBatch(key(), new BatchTrackReq(List.of(validEvent("msg-1")), OffsetDateTime.now()));

    verify(discoveryService).discover(eq(42L), eq("OrderComplete"), anyMap());
    verify(publisher).publishAccepted(any(CdpEventLogDO.class));
}

@Test
void duplicateEventDoesNotPublishInternalEvent() {
    when(eventLogMapper.selectCount(any())).thenReturn(1L);

    service.ingestBatch(key(), new BatchTrackReq(List.of(validEvent("msg-1")), OffsetDateTime.now()));

    verifyNoInteractions(discoveryService);
    verifyNoInteractions(publisher);
}
```

- [x] **Step 2: Add publisher**

Create `CdpEventPublisher.java`:

```java
@Service
@RequiredArgsConstructor
public class CdpEventPublisher {
    private final RocketMQTemplate rocketMQTemplate;

    @Value("${canvas.cdp.event-topic:CDP_EVENT_INGESTED}")
    private String topic;

    public void publishAccepted(CdpEventLogDO event) {
        Map<String, Object> payload = Map.of(
                "tenantId", event.getTenantId(),
                "eventLogId", event.getId(),
                "messageId", event.getMessageId(),
                "eventCode", event.getEventCode(),
                "userId", event.getUserId() == null ? "" : event.getUserId(),
                "anonymousId", event.getAnonymousId() == null ? "" : event.getAnonymousId(),
                "eventTime", event.getEventTime().toString(),
                "properties", event.getProperties() == null ? "{}" : event.getProperties());
        rocketMQTemplate.syncSend(topic + ":" + event.getEventCode(), payload);
    }
}
```

Add config:

```yaml
canvas:
  cdp:
    event-topic: ${CANVAS_CDP_EVENT_TOPIC:CDP_EVENT_INGESTED}
```

- [x] **Step 3: Hook discovery and publisher into ingestion**

Inject:

```java
private final EventAttributeDiscoveryService discoveryService;
private final CdpEventPublisher publisher;
```

After `eventLogMapper.insert(row)` succeeds:

```java
if (def != null && Integer.valueOf(1).equals(def.getAutoDiscover())) {
    discoveryService.discover(key.tenantId(), eventCode, event.properties());
}
publisher.publishAccepted(row);
return true;
```

Keep duplicate returns before this block so duplicates do not discover or publish.

- [x] **Step 4: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=EventAttributeDiscoverySchemaTest,EventAttributeDiscoveryServiceTest,CdpEventIngestionServiceTest
```

Expected: PASS.

### Task 4: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-005a2-event-attribute-discovery-and-internal-cdp-event.md`
- Read: `docs/product-evolution/plans/p1-005a2-event-attribute-discovery-and-internal-cdp-event-plan.md`

- [ ] **Step 1: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V102__event_attribute_discovery_internal_event.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventAttrDefinitionDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventDefinitionDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/EventAttrDefinitionMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoveryService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventPublisher.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java \
  backend/canvas-engine/src/main/resources/application.yml \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoverySchemaTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/EventAttributeDiscoveryServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/cdp/CdpEventIngestionServiceTest.java \
  docs/product-evolution/specs/p1-005a2-event-attribute-discovery-and-internal-cdp-event.md \
  docs/product-evolution/plans/p1-005a2-event-attribute-discovery-and-internal-cdp-event-plan.md
git commit -m "feat: add cdp event discovery and publisher"
```

Expected: commit contains only event discovery, internal CDP event publication, tests, and docs.
