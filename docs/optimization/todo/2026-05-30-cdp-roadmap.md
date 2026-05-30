# CDP能力补齐实施路线图

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将CDP模块从"标签+人群"的基础能力升级为全功能CDP——具备实时事件采集、计算标签/画像、实时人群、数据导出激活、数据治理五大核心能力。

**Architecture:** 分三阶段推进——P0数据基建（采集+管线+治理底线）→ P1核心CDP能力（计算标签/画像+实时人群+导出）→ P2深度与集成（血缘/质量/SDK）。所有新能力通过扩展现有Service/Handler接入，不破坏现有CDP数据流。

**Tech Stack:** Java 21 + Spring Boot 3 + WebFlux + MyBatis-Plus + Redis + RocketMQ + RoaringBitmap + ElasticJob + Flyway + React 18 + antd 5

---

## 全局依赖关系

```
Phase 0: 数据基建
    #10 实时事件采集管线 ─── 基础，后续所有事件驱动能力依赖
    #13 Webhook/回调机制 ─── CDP数据可被外部消费
    #16 数据保留策略 ─── 防止数据无限增长
    #19 PIPL合规增强 ─── 合规底线
    #20 API限流与配额 ─── 系统保护
    ↓
Phase 1: 核心CDP能力
    #4 计算标签引擎 ─── 依赖 #10 事件管线
    #1 计算画像属性 ─── 依赖 #10 事件管线
    #7 实时人群 ─── 依赖 #10 事件管线 + MQ
    #8 人群排重与合并 ─── 依赖 RoaringBitmap 运算
    #11 事件属性自动发现 ─── 依赖 #10 事件管线
    ↓
Phase 2: 深度与集成
    #2~#3 画像增强
    #5~#6 标签深度
    #9 人群快照
    #14~#15 数据导出激活
    #17~#18 数据治理深度
    #12 SDK
```

---

## Phase 0: 数据基建（1-2周）

### Task 0.1: 实时事件采集管线（#10）

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V97__cdp_event_ingestion.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/CdpEventLogDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/CdpEventLogMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/CdpEventIngestionService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CdpEventIngestionController.java`

- [ ] **Step 1: 创建 Flyway 迁移 V97__cdp_event_ingestion.sql**

```sql
CREATE TABLE cdp_event_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    event_code      VARCHAR(64) NOT NULL,
    user_id         VARCHAR(128) NOT NULL,
    session_id      VARCHAR(64) COMMENT '会话ID',
    platform        VARCHAR(32) COMMENT 'WEB/IOS/ANDROID/MINI_PROGRAM/API',
    device_id       VARCHAR(128) COMMENT '设备ID',
    properties      JSON COMMENT '事件属性',
    idempotency_key VARCHAR(128) COMMENT '幂等键',
    event_time      DATETIME(3) NOT NULL COMMENT '事件发生时间(客户端上报)',
    received_at     DATETIME(3) DEFAULT CURRENT_TIMESTAMP(3),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_idempotency (tenant_id, idempotency_key),
    INDEX idx_user_time (tenant_id, user_id, event_time),
    INDEX idx_code_time (tenant_id, event_code, event_time)
) COMMENT='CDP事件日志';

-- 扩展 event_definition 表增加属性自动发现支持
ALTER TABLE event_definition ADD COLUMN auto_discover TINYINT DEFAULT 0 COMMENT '是否自动发现新属性';
ALTER TABLE event_definition ADD COLUMN discovered_attrs JSON COMMENT '自动发现的属性列表';
```

- [ ] **Step 2: 创建 CdpEventLogDO 实体**

```java
@Data
@TableName("cdp_event_log")
public class CdpEventLogDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String eventCode;
    private String userId;
    private String sessionId;
    private String platform;
    private String deviceId;
    private String properties; // JSON
    private String idempotencyKey;
    private LocalDateTime eventTime;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
}
```

- [ ] **Step 3: 创建 CdpEventIngestionService**

```java
@Service
@RequiredArgsConstructor
public class CdpEventIngestionService {
    private final CdpEventLogMapper eventLogMapper;
    private final CdpUserService userService;
    private final RocketMQTemplate rocketMQTemplate;

    public void ingest(CdpEventBatchReq batch) {
        for (CdpEventReq event : batch.getEvents()) {
            // 1. 幂等检查（idempotency_key唯一约束）
            if (event.getIdempotencyKey() != null && isDuplicate(event)) continue;

            // 2. 确保用户存在
            userService.ensureUser(event.getUserId(), "EVENT_SDK", event.getEventCode());

            // 3. 写入 cdp_event_log
            CdpEventLogDO log = toDO(event);
            eventLogMapper.insert(log);

            // 4. 发MQ事件供下游消费（实时人群、计算标签等）
            rocketMQTemplate.convertAndSend("cdp-event-ingested", log);
        }
    }

    private boolean isDuplicate(CdpEventReq event) {
        return eventLogMapper.selectCount(new LambdaQueryWrapper<CdpEventLogDO>()
            .eq(CdpEventLogDO::getTenantId, TenantContext.getTenantId())
            .eq(CdpEventLogDO::getIdempotencyKey, event.getIdempotencyKey())) > 0;
    }
}
```

- [ ] **Step 4: 创建 CdpEventIngestionController — 标准化事件上报API**

```java
@RestController
@RequestMapping("/cdp/events")
@RequiredArgsConstructor
public class CdpEventIngestionController {
    private final CdpEventIngestionService ingestionService;

    @PostMapping("/track")
    public R<Void> track(@RequestBody CdpEventBatchReq batch) {
        ingestionService.ingest(batch);
        return R.ok();
    }
}
```

- [ ] **Step 5: 编写测试**

```java
@SpringBootTest
class CdpEventIngestionServiceTest {
    @Autowired CdpEventIngestionService service;

    @Test void shouldIngestSingleEvent() {
        CdpEventReq event = new CdpEventReq("page_view", "user1", Map.of("page", "/home"));
        service.ingest(new CdpEventBatchReq(List.of(event)));
        // 验证 cdp_event_log 有记录
    }

    @Test void shouldSkipDuplicateEvent() {
        CdpEventReq event = new CdpEventReq("click", "user1", Map.of("button", "buy"));
        event.setIdempotencyKey("dup-123");
        service.ingest(new CdpEventBatchReq(List.of(event)));
        service.ingest(new CdpEventBatchReq(List.of(event))); // 第二次应跳过
        // 验证只有1条记录
    }
}
```

- [ ] **Step 6: 提交**

```bash
git commit -m "feat: add CDP real-time event ingestion pipeline (#10)"
```

---

### Task 0.2: Webhook/回调机制（#13）

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V98__webhook_subscription.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookSubscriptionDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/WebhookDeliveryLogDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/WebhookSubscriptionMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/WebhookDeliveryLogMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/webhook/WebhookDispatcherService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/WebhookSubscriptionController.java`

- [ ] **Step 1: 创建 Flyway 迁移 V98__webhook_subscription.sql**

```sql
CREATE TABLE webhook_subscription (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(100) NOT NULL,
    url             VARCHAR(500) NOT NULL,
    secret          VARCHAR(128) COMMENT 'HMAC签名密钥',
    event_types     JSON NOT NULL COMMENT '["TAG_CHANGED","AUDIENCE_CHANGED","PROFILE_CHANGED"]',
    status          VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/PAUSED/DISABLED',
    retry_max       INT DEFAULT 3,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_status (tenant_id, status)
);

CREATE TABLE webhook_delivery_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    subscription_id BIGINT NOT NULL,
    event_type      VARCHAR(64) NOT NULL,
    payload         JSON NOT NULL,
    http_status     INT COMMENT 'HTTP响应状态码',
    response_body   VARCHAR(500),
    attempt         INT DEFAULT 1,
    next_retry_at   DATETIME,
    status          VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/DEAD',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sub_status (tenant_id, subscription_id, status),
    INDEX idx_retry (status, next_retry_at)
);
```

- [ ] **Step 2: 创建 DO + Mapper**

WebhookSubscriptionDO 字段: id, tenantId, name, url, secret, eventTypes(JSON), status, retryMax, createdAt, updatedAt

WebhookDeliveryLogDO 字段: id, tenantId, subscriptionId, eventType, payload(JSON), httpStatus, responseBody, attempt, nextRetryAt, status, createdAt

- [ ] **Step 3: 创建 WebhookDispatcherService**

```java
@Service
@RequiredArgsConstructor
public class WebhookDispatcherService {
    private final WebhookSubscriptionMapper subMapper;
    private final WebhookDeliveryLogMapper logMapper;
    private final WebClient webClient;

    public void dispatch(String eventType, Map<String, Object> payload) {
        List<WebhookSubscriptionDO> subs = subMapper.selectList(
            new LambdaQueryWrapper<WebhookSubscriptionDO>()
                .eq(WebhookSubscriptionDO::getTenantId, TenantContext.getTenantId())
                .eq(WebhookSubscriptionDO::getStatus, "ACTIVE"));
        for (WebhookSubscriptionDO sub : subs) {
            if (!containsEventType(sub.getEventTypes(), eventType)) continue;
            sendWithRetry(sub, eventType, payload);
        }
    }

    private void sendWithRetry(WebhookSubscriptionDO sub, String eventType, Map<String, Object> payload) {
        // HMAC-SHA256签名
        String signature = HmacUtils.hmacSha256Hex(sub.getSecret(), toJson(payload));
        // POST到callback URL，header带 X-Webhook-Signature
        // 失败时记录delivery_log，下次retry
    }
}
```

- [ ] **Step 4: 在 CdpTagService.setTag() 成功后触发 TAG_CHANGED 事件**

```java
// CdpTagService.setTag() 末尾添加
webhookDispatcherService.dispatch("TAG_CHANGED", Map.of(
    "userId", userId, "tagCode", req.getTagCode(), "tagValue", req.getTagValue()));
```

- [ ] **Step 5: 创建 WebhookSubscriptionController（CRUD + 手动重试）**
- [ ] **Step 6: 编写测试**
- [ ] **Step 7: 提交**

---

### Task 0.3: 数据保留策略（#16）

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V99__data_retention_policy.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/DataRetentionPolicyDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/DataRetentionPolicyMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/DataRetentionService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/schedule/DataRetentionCleanupJob.java`

- [ ] **Step 1: 创建 Flyway 迁移 V99__data_retention_policy.sql**

```sql
CREATE TABLE data_retention_policy (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    table_name      VARCHAR(64) NOT NULL,
    retention_days  INT NOT NULL COMMENT '保留天数',
    archive_enabled TINYINT DEFAULT 0 COMMENT '是否归档到冷存储',
    last_cleaned_at DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_table (tenant_id, table_name)
);

-- 插入默认策略
INSERT INTO data_retention_policy (tenant_id, table_name, retention_days) VALUES
(0, 'cdp_event_log', 90),
(0, 'event_log', 90),
(0, 'canvas_execution_trace', 180),
(0, 'canvas_execution', 365);
```

- [ ] **Step 2: 创建 DataRetentionService — 分页删除**

```java
@Service
@RequiredArgsConstructor
public class DataRetentionService {
    private final DataRetentionPolicyMapper policyMapper;
    private final JdbcTemplate jdbcTemplate;

    public void executeCleanup() {
        List<DataRetentionPolicyDO> policies = policyMapper.selectList(null);
        for (DataRetentionPolicyDO policy : policies) {
            int deleted = deleteInBatches(policy.getTableName(), policy.getRetentionDays());
            policy.setLastCleanedAt(LocalDateTime.now());
            policyMapper.updateById(policy);
        }
    }

    private int deleteInBatches(String tableName, int retentionDays) {
        // 分页DELETE，每批1000条，避免锁表
        String cutoff = LocalDateTime.now().minusDays(retentionDays).toString();
        int total = 0;
        while (true) {
            int deleted = jdbcTemplate.update(
                "DELETE FROM " + tableName + " WHERE created_at < ? LIMIT 1000", cutoff);
            total += deleted;
            if (deleted < 1000) break;
        }
        return total;
    }
}
```

- [ ] **Step 3: 创建 ElasticJob 定时任务 DataRetentionCleanupJob（每天凌晨3点）**
- [ ] **Step 4: 编写测试**
- [ ] **Step 5: 提交**

---

### Task 0.4: PIPL合规增强（#19）

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V100__pii_field_config.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/PiiFieldConfigDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/PiiFieldConfigMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/PiiMaskingService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/cdp/PiiMaskingAspect.java`

- [ ] **Step 1: 创建 Flyway 迁移 V100__pii_field_config.sql**

```sql
CREATE TABLE pii_field_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    table_name      VARCHAR(64) NOT NULL,
    field_name      VARCHAR(64) NOT NULL,
    pii_level       VARCHAR(16) NOT NULL COMMENT 'LOW/MEDIUM/HIGH',
    mask_rule       VARCHAR(32) NOT NULL COMMENT 'FULL/PARTIAL/HASH/NONE',
    allowed_roles   JSON COMMENT '["ADMIN"] — 可看全值的角色',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_field (tenant_id, table_name, field_name)
);

INSERT INTO pii_field_config (tenant_id, table_name, field_name, pii_level, mask_rule, allowed_roles) VALUES
(0, 'cdp_user_profile', 'phone', 'HIGH', 'PARTIAL', '["ADMIN"]'),
(0, 'cdp_user_profile', 'email', 'MEDIUM', 'PARTIAL', '["ADMIN","OPERATOR"]'),
(0, 'cdp_user_identity', 'identity_value', 'HIGH', 'PARTIAL', '["ADMIN"]');
```

- [ ] **Step 2: 创建 PiiMaskingService**

```java
@Service
@RequiredArgsConstructor
public class PiiMaskingService {
    private final PiiFieldConfigMapper configMapper;

    public String mask(String tableName, String fieldName, String value) {
        PiiFieldConfigDO config = findConfig(tableName, fieldName);
        if (config == null) return value;
        // 检查当前用户角色是否有权看全值
        if (hasAllowedRole(config.getAllowedRoles())) return value;
        return applyMaskRule(config.getMaskRule(), value);
    }

    private String applyMaskRule(String rule, String value) {
        return switch (rule) {
            case "FULL" -> "***";
            case "PARTIAL" -> value.substring(0, 3) + "****" + value.substring(value.length() - 4);
            case "HASH" -> DigestUtils.sha256Hex(value).substring(0, 8);
            default -> value;
        };
    }
}
```

- [ ] **Step 3: 创建 PiiMaskingAspect — Controller返回时自动脱敏**

AOP拦截CDP相关Controller的返回DTO，对标记了 `@PiiField` 的字段自动调用 `PiiMaskingService.mask()`。

- [ ] **Step 4: 编写测试**
- [ ] **Step 5: 提交**

---

### Task 0.5: API限流与配额（#20）

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V101__api_quota_config.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ApiQuotaConfigDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ApiQuotaConfigMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/ApiRateLimiterService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/ApiRateLimiterAspect.java`

- [ ] **Step 1: 创建 Flyway 迁移 V101__api_quota_config.sql**

```sql
CREATE TABLE api_quota_config (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    source          VARCHAR(64) NOT NULL COMMENT 'API来源标识',
    daily_limit     INT NOT NULL COMMENT '每日配额',
    used_today      INT DEFAULT 0,
    reset_date      DATE,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_source (tenant_id, source)
);
```

- [ ] **Step 2: 创建 ApiRateLimiterService — Redis令牌桶+日配额**

```java
@Service
@RequiredArgsConstructor
public class ApiRateLimiterService {
    private final StringRedisTemplate redisTemplate;

    public boolean tryAcquire(Long tenantId, String source, int qpsLimit) {
        // 1. QPS限流（Redis令牌桶）
        String qpsKey = "api:ratelimit:" + tenantId + ":" + source;
        Long current = redisTemplate.opsForValue().increment(qpsKey);
        if (current == 1) redisTemplate.expire(qpsKey, 1, TimeUnit.SECONDS);
        if (current > qpsLimit) return false;

        // 2. 日配额检查
        // ... 查 api_quota_config 表，used_today < daily_limit 则+1通过
        return true;
    }
}
```

- [ ] **Step 3: 创建 ApiRateLimiterAspect — CDP写入接口前置拦截**

AOP拦截 `CdpEventIngestionController` 和 `CdpTagOperationController` 的写入方法，调用 `tryAcquire()`，超限返回429。

- [ ] **Step 4: 编写测试**
- [ ] **Step 5: 提交**

---

## Phase 1: 核心CDP能力（2-4周）

### Task 1.1: 计算标签引擎（#4）

**Files:**
- Create: `V102__computed_tag.sql`
- Create: `ComputedTagDefinitionDO` + Mapper
- Create: `ComputedTagEngine` — 规则/SQL/表达式三种计算模式
- Create: `ComputedTagJob` — ElasticJob定时执行
- Create: `ComputedTagMQConsumer` — 实时模式：消费 cdp-event-ingested MQ 事件
- Modify: `CdpTagService.setTag()` — 新增 sourceType=COMPUTED

- [ ] **Step 1: 创建 computed_tag_definition 表**

```sql
CREATE TABLE computed_tag_definition (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    tag_code        VARCHAR(64) NOT NULL COMMENT '关联tag_definition.code',
    compute_type    VARCHAR(16) NOT NULL COMMENT 'RULE/SQL/EXPR',
    expression      TEXT NOT NULL COMMENT '计算表达式',
    schedule_cron   VARCHAR(64) COMMENT '定时计算cron（为空=仅实时）',
    depends_on      JSON COMMENT '依赖的tag_code列表',
    refresh_mode    VARCHAR(16) DEFAULT 'BATCH' COMMENT 'BATCH/REALTIME/HYBRID',
    last_computed_at DATETIME,
    status          VARCHAR(16) DEFAULT 'ACTIVE',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_tag (tenant_id, tag_code)
);
```

- [ ] **Step 2: 创建 ComputedTagEngine — 三种计算模式**

```java
@Service
public class ComputedTagEngine {
    // RULE模式：基于用户属性/标签条件评估
    List<String> evaluateRule(Long tenantId, String ruleJson);
    // SQL模式：执行SQL查询返回userId列表
    List<String> evaluateSql(Long tenantId, String sql, Map<String,Object> params);
    // EXPR模式：SpEL表达式，传入用户facts map
    String evaluateExpr(String expression, Map<String,Object> facts);
    // 主入口：根据compute_type路由
    void compute(ComputedTagDefinitionDO def);
}
```

- [ ] **Step 3: 创建 ElasticJob 定时任务 + MQ Consumer**
- [ ] **Step 4: 在 CdpTagService.setTag() 成功后触发 Webhook TAG_CHANGED**
- [ ] **Step 5: 编写测试**
- [ ] **Step 6: 提交**

---

### Task 1.2: 计算画像属性（#1）

**Files:**
- Create: `V103__computed_profile_attribute.sql`
- Create: `ComputedProfileAttributeDO` + Mapper
- Create: `ComputedProfileEngine` — 表达式求值 + 结果写回 properties_json
- Create: `ComputedProfileJob` — ElasticJob

- [ ] **Step 1: 创建 computed_profile_attribute 表**

```sql
CREATE TABLE computed_profile_attribute (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    attr_name       VARCHAR(64) NOT NULL,
    attr_group      VARCHAR(32) DEFAULT 'default' COMMENT '属性分组',
    expression      TEXT NOT NULL,
    refresh_cron    VARCHAR(64),
    last_computed_at DATETIME,
    status          VARCHAR(16) DEFAULT 'ACTIVE',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_attr (tenant_id, attr_name)
);
```

- [ ] **Step 2: 创建 ComputedProfileEngine**

从 `cdp_event_log` 聚合计算结果，写回 `cdp_user_profile.properties_json`。支持 SUM/COUNT/AVG/MAX/MIN/LAST 等聚合函数。

- [ ] **Step 3: 创建 ElasticJob 定时任务**
- [ ] **Step 4: 编写测试**
- [ ] **Step 5: 提交**

---

### Task 1.3: 实时人群（#7）

**Files:**
- Create: `RealtimeAudienceConsumer` — 消费 cdp-event-ingested MQ
- Create: `RealtimeAudienceEvaluator` — Aviator实时规则评估
- Modify: `AudienceBitmapStore` — 新增实时更新方法

- [ ] **Step 1: 创建 RealtimeAudienceConsumer**

消费 `cdp-event-ingested` MQ 事件，加载所有启用实时的人群定义，用Aviator评估事件是否匹配规则。命中则 `bitmap.add(toUid(userId))` 并发 AUDIENCE_CHANGED 事件。

- [ ] **Step 2: 创建 RealtimeAudienceEvaluator**

```java
@Service
public class RealtimeAudienceEvaluator {
    // 评估事件是否匹配人群规则
    boolean matches(CdpEventLogDO event, String ruleJson);
    // 检查用户是否需要出组（反向规则）
    boolean shouldRemove(CdpEventLogDO event, String ruleJson);
}
```

- [ ] **Step 3: 在 AudienceBitmapStore 新增实时更新方法**

```java
public void addMember(Long audienceId, String userId) {
    RoaringBitmap bitmap = load(audienceId);
    bitmap.add(toUid(userId));
    save(audienceId, bitmap);
}

public void removeMember(Long audienceId, String userId) {
    RoaringBitmap bitmap = load(audienceId);
    bitmap.remove(toUid(userId));
    save(audienceId, bitmap);
}
```

- [ ] **Step 4: 编写测试**
- [ ] **Step 5: 提交**

---

### Task 1.4: 人群排重与合并（#8）

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/audience/AudienceSetOperationService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AudienceSetOperationController.java`

- [ ] **Step 1: 创建 AudienceSetOperationService — RoaringBitmap集合运算**

```java
@Service
@RequiredArgsConstructor
public class AudienceSetOperationService {
    private final AudienceBitmapStore bitmapStore;

    public long intersectionSize(Long audienceIdA, Long audienceIdB) {
        RoaringBitmap a = bitmapStore.load(audienceIdA);
        RoaringBitmap b = bitmapStore.load(audienceIdB);
        return RoaringBitmap.and(a, b).getLongCardinality();
    }

    public long unionSize(List<Long> audienceIds) {
        RoaringBitmap result = new RoaringBitmap();
        for (Long id : audienceIds) result.or(bitmapStore.load(id));
        return result.getLongCardinality();
    }

    public long differenceSize(Long audienceIdA, Long audienceIdB) {
        RoaringBitmap a = bitmapStore.load(audienceIdA);
        RoaringBitmap b = bitmapStore.load(audienceIdB);
        return RoaringBitmap.andNot(a, b).getLongCardinality();
    }

    public RoaringBitmap createIntersection(Long audienceIdA, Long audienceIdB) { ... }
    public RoaringBitmap createUnion(List<Long> audienceIds) { ... }
    public RoaringBitmap createDifference(Long audienceIdA, Long audienceIdB) { ... }
}
```

- [ ] **Step 2: 创建 AudienceSetOperationController**
- [ ] **Step 3: 编写测试**
- [ ] **Step 4: 提交**

---

### Task 1.5: 事件属性自动发现（#11）

**Files:**
- Modify: `CdpEventIngestionService.ingest()` — 上报时检测新属性
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/EventAttrDefinitionDO.java`
- Create: `V104__event_attr_definition.sql`

- [ ] **Step 1: 创建 event_attr_definition 表**

```sql
CREATE TABLE event_attr_definition (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    event_code      VARCHAR(64) NOT NULL,
    attr_name       VARCHAR(64) NOT NULL,
    attr_type       VARCHAR(16) COMMENT 'STRING/NUMBER/BOOLEAN/DATE',
    discover_status VARCHAR(16) DEFAULT 'PENDING_REVIEW' COMMENT 'PENDING_REVIEW/CONFIRMED/REJECTED',
    first_seen_at   DATETIME,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_event_attr (tenant_id, event_code, attr_name)
);
```

- [ ] **Step 2: 在 CdpEventIngestionService.ingest() 中自动发现新属性**

事件上报时，遍历 properties JSON 的每个key，与 `event_attr_definition` 已知属性对比。新属性自动插入（discover_status=PENDING_REVIEW），类型根据值推断。

- [ ] **Step 3: 编写测试**
- [ ] **Step 4: 提交**

---

## Phase 2: 深度与集成（1-2月）

> 依赖 Phase 0 + Phase 1 的基础设施。按模块分组，可并行。

### 画像增强

**Task 2.1: 画像属性分组（#2）** — `properties_json` 结构化 + 前端分组展示
**Task 2.2: 画像属性版本与历史（#3）** — 新增 `profile_change_log` 表 + 时间线组件

### 标签深度

**Task 2.3: 标签依赖图谱（#5）** — DAG可视化 + 拓扑排序 + 循环检测
**Task 2.4: 标签血缘与影响分析（#6）** — 引用索引 + @xyflow/react血缘图 + 删除前影响提示

### 人群深度

**Task 2.5: 人群快照与历史（#9）** — 新增 `audience_snapshot` 表 + ElasticJob每天保存 + 趋势图

### 数据导出

**Task 2.6: 广告平台对接（#14）** — Connector接口抽象 + Facebook CAPI + Google Customer Match + 巨量引擎
**Task 2.7: 邮件/短信工具同步（#15）** — Braze/SFMC Connector + 字段映射配置

### 数据治理深度

**Task 2.8: 数据质量监控（#17）** — 标签覆盖率/事件缺失率/人群波动 + 质量仪表盘
**Task 2.9: 数据血缘追踪（#18）** — 标签→人群→画布引用图谱 + 端到端血缘可视化

### 生态建设

**Task 2.10: Web/Mobile SDK（#12）** — 独立项目，或先对接神策SDK

---

## 优先级总览

| Phase | 时间 | 任务数 | 核心交付 |
|-------|------|--------|---------|
| **Phase 0** | 1-2周 | 5 | 事件管线+Webhook+保留策略+PIPL+限流 |
| **Phase 1** | 2-4周 | 5 | 计算标签/画像+实时人群+排重+属性发现 |
| **Phase 2** | 1-2月 | 10 | 画像/标签/人群深度+导出+治理+SDK |

**最短路径**：Phase 0 → Phase 1 中的 Task 1.1（计算标签）+ Task 1.3（实时人群），即可让CDP从"静态标签+离线人群"升级为"动态标签+实时人群"。

---

## 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| 实时人群MQ消费延迟 | 人群成员更新不及时 | 设消费告警 + 降级到离线批计算 |
| 计算标签表达式安全 | SQL注入/脚本逃逸 | SQL模式用预编译参数；EXPR模式用Groovy沙箱（已有） |
| RoaringBitmap全量反序列化 | 实时人群判断性能 | 超大人群（>100万）用Redis原生bitmap替代 |
| 事件量暴增压垮MySQL | event_log表膨胀 | Phase 0 已有保留策略 + 限流配额；中长期引入ClickHouse |
