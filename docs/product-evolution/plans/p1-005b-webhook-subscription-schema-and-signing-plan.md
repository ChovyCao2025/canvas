# Webhook Subscription Schema And Signing Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add webhook subscription and delivery-log storage, callback/event validation, and deterministic HMAC signing.

**Architecture:** Keep storage and signing independent from dispatch. The validator checks callback URL and event type inputs before persistence; the signer provides the exact header value used later by P1-005B2.

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, HMAC-SHA256, JUnit 5, Mockito, AssertJ.

---

## Spec Reference

- `docs/product-evolution/specs/p1-005b-webhook-subscription-schema-and-signing.md`
- Depends on: `docs/product-evolution/specs/p1-005a-cdp-event-log-and-idempotent-track.md`
- Depends on: `docs/product-evolution/specs/p1-005a2-event-attribute-discovery-and-internal-cdp-event.md`

## File Structure

- Create: `backend/canvas-engine/src/main/resources/db/migration/V98__webhook_subscription_schema.sql`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookSubscriptionSchemaTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookSubscriptionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookDeliveryLogDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/WebhookSubscriptionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/WebhookDeliveryLogMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookSignatureService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookSubscriptionValidator.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookSignatureServiceTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookSubscriptionValidatorTest.java`

### Task 1: Schema And Data Objects

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookSubscriptionSchemaTest.java`
- Create: `backend/canvas-engine/src/main/resources/db/migration/V98__webhook_subscription_schema.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookSubscriptionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookDeliveryLogDO.java`
- Create: mapper files under `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/`

- [ ] **Step 1: Write schema test**

Create `WebhookSubscriptionSchemaTest.java`:

```java
class WebhookSubscriptionSchemaTest {

    @Test
    void migrationCreatesSubscriptionsAndDeliveryLogs() throws Exception {
        String sql = Files.readString(Path.of(
                "src/main/resources/db/migration/V98__webhook_subscription_schema.sql"));

        assertThat(sql)
                .contains("CREATE TABLE IF NOT EXISTS webhook_subscription")
                .contains("event_types")
                .contains("secret_hash")
                .contains("secret_ciphertext")
                .contains("CREATE TABLE IF NOT EXISTS webhook_delivery_log")
                .contains("delivery_id")
                .contains("attempt")
                .contains("terminal_reason")
                .contains("idx_webhook_retry");
    }
}
```

- [ ] **Step 2: Run schema test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookSubscriptionSchemaTest
```

Expected: FAIL because migration does not exist.

- [ ] **Step 3: Add migration**

Create `V98__webhook_subscription_schema.sql`:

```sql
CREATE TABLE IF NOT EXISTS webhook_subscription (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    name VARCHAR(128) NOT NULL,
    callback_url VARCHAR(1000) NOT NULL,
    secret_prefix VARCHAR(16) NOT NULL,
    secret_hash VARCHAR(120) NOT NULL,
    secret_ciphertext VARCHAR(1000) NOT NULL,
    event_types JSON NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    max_attempts INT NOT NULL DEFAULT 3,
    created_by VARCHAR(128) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_webhook_subscription_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Outbound webhook subscriptions';

CREATE TABLE IF NOT EXISTS webhook_delivery_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    subscription_id BIGINT NOT NULL,
    delivery_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSON NOT NULL,
    attempt INT NOT NULL DEFAULT 1,
    http_status INT NULL,
    response_body VARCHAR(1000) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    next_retry_at DATETIME NULL,
    error_message VARCHAR(1000) NULL,
    terminal_reason VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_webhook_delivery_attempt (tenant_id, delivery_id, attempt),
    INDEX idx_webhook_delivery_sub_status (tenant_id, subscription_id, status),
    INDEX idx_webhook_retry (status, next_retry_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Outbound webhook delivery attempts';
```

- [ ] **Step 4: Add data objects and mappers**

Create `WebhookSubscriptionDO.java`:

```java
@Data
@TableName("webhook_subscription")
public class WebhookSubscriptionDO {
    public static final String ACTIVE = "ACTIVE";
    public static final String PAUSED = "PAUSED";
    public static final String DISABLED = "DISABLED";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String name;
    private String callbackUrl;
    private String secretPrefix;
    private String secretHash;
    private String secretCiphertext;
    private String eventTypes;
    private String status;
    private Integer maxAttempts;
    private String createdBy;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

Create `WebhookDeliveryLogDO.java`:

```java
@Data
@TableName("webhook_delivery_log")
public class WebhookDeliveryLogDO {
    public static final String PENDING = "PENDING";
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String RETRYING = "RETRYING";
    public static final String DEAD = "DEAD";

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private Long subscriptionId;
    private String deliveryId;
    private String eventType;
    private String payload;
    private Integer attempt;
    private Integer httpStatus;
    private String responseBody;
    private String status;
    private LocalDateTime nextRetryAt;
    private String errorMessage;
    private String terminalReason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
```

Create `WebhookSubscriptionMapper.java` and `WebhookDeliveryLogMapper.java`:

```java
@Mapper
public interface WebhookSubscriptionMapper extends BaseMapper<WebhookSubscriptionDO> {
}
```

```java
@Mapper
public interface WebhookDeliveryLogMapper extends BaseMapper<WebhookDeliveryLogDO> {
}
```

- [ ] **Step 5: Run schema test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookSubscriptionSchemaTest
```

Expected: PASS.

### Task 2: Signature Service

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookSignatureServiceTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookSignatureService.java`

- [ ] **Step 1: Write signature tests**

Create `WebhookSignatureServiceTest.java`:

```java
class WebhookSignatureServiceTest {

    @Test
    void signUsesTimestampNewlinePayloadCanonicalString() {
        WebhookSignatureService service = new WebhookSignatureService();

        String signature = service.sign("secret-123", "1717200000000", "{\"event\":\"x\"}");

        assertThat(signature).startsWith("sha256=");
        assertThat(service.verify("secret-123", "1717200000000", "{\"event\":\"x\"}", signature)).isTrue();
        assertThat(service.verify("secret-123", "1717200000000", "{\"event\":\"y\"}", signature)).isFalse();
    }
}
```

- [ ] **Step 2: Run signature test and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookSignatureServiceTest
```

Expected: FAIL because service does not exist.

- [ ] **Step 3: Add signature service**

Create `WebhookSignatureService.java`:

```java
@Service
public class WebhookSignatureService {
    private static final String ALGORITHM = "HmacSHA256";

    public String sign(String secret, String timestamp, String rawPayload) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] digest = mac.doFinal((timestamp + "\n" + rawPayload).getBytes(StandardCharsets.UTF_8));
            return "sha256=" + HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("webhook signature failed", e);
        }
    }

    public boolean verify(String secret, String timestamp, String rawPayload, String supplied) {
        String expected = sign(secret, timestamp, rawPayload);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied.getBytes(StandardCharsets.UTF_8));
    }
}
```

- [ ] **Step 4: Run signature test**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookSignatureServiceTest
```

Expected: PASS.

### Task 3: Subscription Validator

**Files:**
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookSubscriptionValidatorTest.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookSubscriptionValidator.java`

- [ ] **Step 1: Write validator tests**

Create `WebhookSubscriptionValidatorTest.java`:

```java
class WebhookSubscriptionValidatorTest {

    @Test
    void rejectsLocalhostCallbackUrl() {
        WebhookSubscriptionValidator validator = new WebhookSubscriptionValidator();

        assertThatThrownBy(() -> validator.validate("http://localhost:8080/hook", List.of("cdp.event.ingested")))
                .hasMessageContaining("callbackUrl is not allowed");
    }

    @Test
    void rejectsBlankEventTypeList() {
        WebhookSubscriptionValidator validator = new WebhookSubscriptionValidator();

        assertThatThrownBy(() -> validator.validate("https://example.com/hook", List.of()))
                .hasMessageContaining("eventTypes cannot be empty");
    }

    @Test
    void acceptsHttpsCallbackAndNonblankEventTypes() {
        WebhookSubscriptionValidator validator = new WebhookSubscriptionValidator();

        validator.validate("https://example.com/hook", List.of("cdp.event.ingested"));
    }
}
```

- [ ] **Step 2: Run validator tests and confirm red state**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookSubscriptionValidatorTest
```

Expected: FAIL because validator does not exist.

- [ ] **Step 3: Add validator**

Create `WebhookSubscriptionValidator.java`:

```java
@Service
public class WebhookSubscriptionValidator {

    public void validate(String callbackUrl, List<String> eventTypes) {
        try {
            OutboundUrlValidator.validateHttpUrl(callbackUrl);
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("callbackUrl is not allowed: " + ex.getMessage(), ex);
        }
        List<String> normalized = eventTypes == null ? List.of() : eventTypes.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("eventTypes cannot be empty");
        }
    }
}
```

- [ ] **Step 4: Run focused backend tests**

Run:

```bash
cd backend && mvn -pl canvas-engine test -Dtest=WebhookSubscriptionSchemaTest,WebhookSignatureServiceTest,WebhookSubscriptionValidatorTest
```

Expected: PASS.

### Task 4: Commit This Slice

**Files:**
- Read: `docs/product-evolution/specs/p1-005b-webhook-subscription-schema-and-signing.md`
- Read: `docs/product-evolution/plans/p1-005b-webhook-subscription-schema-and-signing-plan.md`

- [ ] **Step 1: Commit**

Run:

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V98__webhook_subscription_schema.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookSubscriptionDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookDeliveryLogDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/WebhookSubscriptionMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/WebhookDeliveryLogMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookSignatureService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/domain/webhook/WebhookSubscriptionValidator.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookSubscriptionSchemaTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookSignatureServiceTest.java \
  backend/canvas-engine/src/test/java/org/chovy/canvas/domain/webhook/WebhookSubscriptionValidatorTest.java \
  docs/product-evolution/specs/p1-005b-webhook-subscription-schema-and-signing.md \
  docs/product-evolution/plans/p1-005b-webhook-subscription-schema-and-signing-plan.md
git commit -m "feat: add webhook subscription schema and signing"
```

Expected: commit contains only webhook schema, signing, validation, tests, and docs.
