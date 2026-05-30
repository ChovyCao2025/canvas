# 营销平台35项能力缺项实施路线图

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将营销画布平台从"能执行"升级为"可信、可控、可分析、可持续"的完整营销系统，覆盖合规风控、权益激励、预算ROI、生命周期运营、分析能力五大维度。

**Architecture:** 按四阶段推进——P0合规风控（堵风险）→ P1运营核心（补能力）→ P2规模化（提效率）→ P3进阶分析（做深度）。每阶段内的任务尽量并行，阶段间有序依赖。所有新能力通过现有 Handler 扩展或新增 Handler 接入，不改 DagEngine 核心。

**Tech Stack:** Java 21 + Spring Boot 3 + WebFlux + MyBatis-Plus + Redis + RocketMQ + Flyway + React 18 + antd 5

---

## 全局依赖关系图

```
Phase 0: 合规风控（#24 #25 #26 #27）
    ↓ 提供合规拦截基础
Phase 1: 运营核心
    ├─ 权益激励（#28 #29）— 依赖 #26 黑名单/退订
    ├─ 预算管理（#30 #31 #32）— 依赖 #27 频次合规 + #2 归因
    ├─ 生命周期（#33 #34 #35）— 依赖 CDP + EVENT_TRIGGER
    ├─ 分析能力（#17.1-17.15）— 依赖 event_log + 可选 OLAP
    └─ 第一层痛点（#1-#5）— 依赖各自技术要点
Phase 2: 规模化（#6-#16 #19-#23）
    ↓ 多画布/多团队/多渠道
Phase 3: 进阶分析（#18 广告分析 + OLAP 依赖项）
```

---

## Phase 0: 合规风控（P0，1-2周）

> 目标：堵住合规风险，触达前必须有授权检查、黑名单检查、合规频控。这是上线前提。

### Task 0.1: 用户授权管理（#25）

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V89__user_consent.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/UserConsentDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/UserConsentMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/UserConsentService.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java` — 新增 `consentCheck()` 方法
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/UserConsentController.java`
- Create: `frontend/src/pages/consent-management/index.tsx`

- [ ] **Step 1: 创建 Flyway 迁移 V89__user_consent.sql**

```sql
CREATE TABLE user_consent (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    user_id         VARCHAR(128) NOT NULL,
    channel         VARCHAR(32) NOT NULL COMMENT 'SMS/PUSH/EMAIL/WECHAT',
    consent_status  VARCHAR(16) NOT NULL COMMENT 'OPT_IN/OPT_OUT',
    source          VARCHAR(64) COMMENT 'USER_SELF/ADMIN/API/IMPORT',
    consent_at      DATETIME NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_channel (tenant_id, user_id, channel),
    INDEX idx_user (tenant_id, user_id)
) COMMENT='用户渠道授权表';
```

- [ ] **Step 2: 创建 UserConsentDO 实体**

```java
@Data
@TableName("user_consent")
public class UserConsentDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tenantId;
    private String userId;
    private String channel;
    private String consentStatus; // OPT_IN, OPT_OUT
    private String source;
    private LocalDateTime consentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 3: 创建 UserConsentMapper**

```java
@Mapper
public interface UserConsentMapper extends BaseMapper<UserConsentDO> {
}
```

- [ ] **Step 4: 创建 UserConsentService**

```java
@Service
@RequiredArgsConstructor
public class UserConsentService {
    private final UserConsentMapper consentMapper;

    public boolean isOptedIn(Long tenantId, String userId, String channel) {
        LambdaQueryWrapper<UserConsentDO> qw = new LambdaQueryWrapper<>();
        qw.eq(UserConsentDO::getTenantId, tenantId)
          .eq(UserConsentDO::getUserId, userId)
          .eq(UserConsentDO::getChannel, channel);
        UserConsentDO record = consentMapper.selectOne(qw);
        return record != null && "OPT_IN".equals(record.getConsentStatus());
    }

    @Transactional
    public void updateConsent(Long tenantId, String userId, String channel, String status, String source) {
        LambdaQueryWrapper<UserConsentDO> qw = new LambdaQueryWrapper<>();
        qw.eq(UserConsentDO::getTenantId, tenantId)
          .eq(UserConsentDO::getUserId, userId)
          .eq(UserConsentDO::getChannel, channel);
        UserConsentDO existing = consentMapper.selectOne(qw);
        if (existing != null) {
            existing.setConsentStatus(status);
            existing.setSource(source);
            existing.setConsentAt(LocalDateTime.now());
            consentMapper.updateById(existing);
        } else {
            UserConsentDO record = new UserConsentDO();
            record.setTenantId(tenantId);
            record.setUserId(userId);
            record.setChannel(channel);
            record.setConsentStatus(status);
            record.setSource(source);
            record.setConsentAt(LocalDateTime.now());
            consentMapper.insert(record);
        }
    }
}
```

- [ ] **Step 5: 在 AbstractSendMessageHandler 新增 consentCheck**

在 `executeAsync()` 方法中，`deliveryService.send(request)` 调用前，插入授权检查：

```java
// 在 AbstractSendMessageHandler.executeAsync() 中添加
private boolean consentCheck(ExecutionContext ctx, String channel) {
    // 默认策略：无授权记录视为未授权（可配置为默认授权）
    String requireConsent = ctx.getContextValue("requireConsent");
    if (!"true".equals(requireConsent)) return true;
    return userConsentService.isOptedIn(ctx.getTenantId(), ctx.getUserId(), channel);
}
```

- [ ] **Step 6: 创建 UserConsentController（API端点）**

```java
@RestController
@RequestMapping("/api/consent")
@RequiredArgsConstructor
public class UserConsentController {
    private final UserConsentService consentService;

    @GetMapping("/{userId}")
    public R<List<UserConsentDO>> getConsentStatus(@PathVariable String userId) { ... }

    @PutMapping("/{userId}/{channel}")
    public R<Void> updateConsent(@PathVariable String userId, @PathVariable String channel,
                                  @RequestParam String status, @RequestParam(defaultValue = "API") String source) { ... }
}
```

- [ ] **Step 7: 编写测试**

```java
@SpringBootTest
class UserConsentServiceTest {
    @Autowired UserConsentService service;

    @Test void shouldReturnFalse_whenNoConsentRecord() {
        assertThat(service.isOptedIn(1L, "user1", "SMS")).isFalse();
    }

    @Test void shouldReturnTrue_whenOptedIn() {
        service.updateConsent(1L, "user1", "SMS", "OPT_IN", "TEST");
        assertThat(service.isOptedIn(1L, "user1", "SMS")).isTrue();
    }

    @Test void shouldReturnFalse_whenOptedOut() {
        service.updateConsent(1L, "user1", "SMS", "OPT_IN", "TEST");
        service.updateConsent(1L, "user1", "SMS", "OPT_OUT", "USER_SELF");
        assertThat(service.isOptedIn(1L, "user1", "SMS")).isFalse();
    }
}
```

- [ ] **Step 8: 提交**

```bash
git add backend/canvas-engine/src/main/resources/db/migration/V89__user_consent.sql \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/UserConsentDO.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/UserConsentMapper.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/UserConsentService.java \
  backend/canvas-engine/src/main/java/org/chovy/canvas/web/UserConsentController.java
git commit -m "feat: add user consent management for PIPL compliance (#25)"
```

---

### Task 0.2: 退订/黑名单管理（#26）

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V90__blacklist_unsubscribe.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/UserBlacklistDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/UserUnsubscribeDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/UserBlacklistMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/UserUnsubscribeMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/UserBlacklistService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/UserBlacklistController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/AbstractSendMessageHandler.java` — 新增黑名单/退订检查

- [ ] **Step 1: 创建 Flyway 迁移 V90__blacklist_unsubscribe.sql**

```sql
CREATE TABLE user_blacklist (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT NOT NULL DEFAULT 0,
    user_id     VARCHAR(128) NOT NULL,
    reason      VARCHAR(500),
    added_by    VARCHAR(64),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user (tenant_id, user_id),
    INDEX idx_user (tenant_id, user_id)
) COMMENT='用户黑名单';

CREATE TABLE user_unsubscribe (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    user_id         VARCHAR(128) NOT NULL,
    channel         VARCHAR(32) NOT NULL COMMENT 'SMS/PUSH/EMAIL/WECHAT/GLOBAL',
    canvas_id       BIGINT COMMENT 'NULL=全局退订, 非NULL=某画布退订',
    unsubscribed_at DATETIME NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_channel_canvas (tenant_id, user_id, channel, canvas_id),
    INDEX idx_user (tenant_id, user_id)
) COMMENT='用户退订记录';
```

- [ ] **Step 2: 创建 DO 实体 + Mapper**

UserBlacklistDO 字段: id, tenantId, userId, reason, addedBy, createdAt

UserUnsubscribeDO 字段: id, tenantId, userId, channel, canvasId, unsubscribedAt, createdAt

- [ ] **Step 3: 创建 UserBlacklistService**

```java
@Service
@RequiredArgsConstructor
public class UserBlacklistService {
    private final UserBlacklistMapper blacklistMapper;
    private final UserUnsubscribeMapper unsubscribeMapper;

    public boolean isBlacklisted(Long tenantId, String userId) {
        return blacklistMapper.selectCount(new LambdaQueryWrapper<UserBlacklistDO>()
            .eq(UserBlacklistDO::getTenantId, tenantId)
            .eq(UserBlacklistDO::getUserId, userId)) > 0;
    }

    public boolean isUnsubscribed(Long tenantId, String userId, String channel) {
        // 检查全局退订 + 渠道退订
        return unsubscribeMapper.selectCount(new LambdaQueryWrapper<UserUnsubscribeDO>()
            .eq(UserUnsubscribeDO::getTenantId, tenantId)
            .eq(UserUnsubscribeDO::getUserId, userId)
            .and(w -> w.eq(UserUnsubscribeDO::getChannel, "GLOBAL")
                       .or().eq(UserUnsubscribeDO::getChannel, channel))) > 0;
    }

    public boolean shouldSkip(Long tenantId, String userId, String channel) {
        return isBlacklisted(tenantId, userId) || isUnsubscribed(tenantId, userId, channel);
    }
}
```

- [ ] **Step 4: 在 AbstractSendMessageHandler 新增黑名单/退订检查**

在 `consentCheck()` 之后，`deliveryService.send()` 之前，添加：

```java
if (blacklistService.shouldSkip(ctx.getTenantId(), ctx.getUserId(), channel)) {
    // 写 SKIPPED 记录到 message_send_record
    return NodeResult.suppressed("blacklist_or_unsubscribe", config);
}
```

- [ ] **Step 5: 创建 UserBlacklistController（CRUD + 批量导入）**

端点：
- `GET /api/blacklist/check?userId=xxx&channel=SMS` — 检查黑名单/退订状态
- `POST /api/blacklist` — 添加黑名单
- `DELETE /api/blacklist/{userId}` — 移除黑名单
- `POST /api/blacklist/import` — CSV批量导入
- `POST /api/unsubscribe/{userId}/{channel}` — 退订
- `DELETE /api/unsubscribe/{userId}/{channel}` — 取消退订

- [ ] **Step 6: 编写测试**

测试用例：黑名单拦截、全局退订拦截、渠道退订拦截、非黑名单用户通过、退订后取消退订恢复

- [ ] **Step 7: 提交**

```bash
git commit -m "feat: add blacklist and unsubscribe management (#26)"
```

---

### Task 0.3: 频次合规管控（#27）

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/MarketingPolicyService.java` — 新增合规频控层
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers/FrequencyCapHandler.java` — 调用合规检查
- Create: `backend/canvas-engine/src/main/resources/db/migration/V91__compliance_frequency_config.sql` — system_option 种子数据

- [ ] **Step 1: 在 system_option 插入合规频次配置种子数据**

```sql
INSERT INTO system_option (tenant_id, category, option_key, label, sort_order, enabled, system_builtin) VALUES
(0, 'compliance_frequency', 'SMS_DAILY_MAX', '营销短信每日上限', 1, 1, 1),
(0, 'compliance_frequency', 'WECHAT_DAILY_MAX', '微信模板消息每日上限', 2, 1, 1),
(0, 'compliance_frequency', 'PUSH_DAILY_MAX', 'Push每日上限(0=不限)', 3, 1, 1),
(0, 'compliance_frequency', 'EMAIL_DAILY_MAX', '邮件每日上限(0=不限)', 4, 1, 1);
```

- [ ] **Step 2: 在 MarketingPolicyService 新增合规频控方法**

```java
public PolicyDecision complianceFrequencyAllowed(Long tenantId, String userId, String channel) {
    // 1. 从 system_option 读取该渠道合规频次上限
    String maxKey = channel.toUpperCase() + "_DAILY_MAX";
    int maxCount = systemOptionService.getIntValue(tenantId, "compliance_frequency", maxKey, 0);
    if (maxCount <= 0) return PolicyDecision.allowed(); // 0=不限

    // 2. Redis 查今日已发数
    String key = "canvas:compliance:freq:" + channel + ":" + userId + ":" + todayKey();
    Long current = redisTemplate.opsForValue().increment(key);
    if (current == 1) redisTemplate.expire(key, 1, TimeUnit.DAYS);
    if (current > maxCount) {
        redisTemplate.opsForValue().decrement(key); // 回滚
        return PolicyDecision.blocked("COMPLIANCE_FREQUENCY", "渠道合规频次超限: " + channel + " 今日上限" + maxCount);
    }
    return PolicyDecision.allowed();
}
```

- [ ] **Step 3: 在 FrequencyCapHandler 中先调合规检查再调业务检查**

```java
// FrequencyCapHandler.executeAsync() 中
// 1. 先检查合规频控（优先级最高）
PolicyDecision compliance = marketingPolicyService.complianceFrequencyAllowed(ctx.getTenantId(), ctx.getUserId(), channel);
if (compliance.isBlocked()) {
    return NodeResult.suppressed("compliance_capped", config);
}
// 2. 再检查业务频控（原有逻辑）
```

- [ ] **Step 4: 编写测试**

测试用例：合规频控超限被拦、合规频控未超限通过业务频控、不同渠道独立计数、Redis key 过期正确

- [ ] **Step 5: 提交**

```bash
git commit -m "feat: add compliance frequency control layer above business cap (#27)"
```

---

### Task 0.4: 营销合规审核（#24）

**Files:**
- Create: `backend/canvas-engine/src/main/resources/db/migration/V92__content_audit.sql`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/dataobject/ContentAuditDO.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/ContentAuditMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/SensitiveWordService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/ContentAuditService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/ContentAuditController.java`
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasController.java` — 发布前审核拦截

- [ ] **Step 1: 创建 Flyway 迁移 V92__content_audit.sql**

```sql
CREATE TABLE content_audit (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    canvas_id       BIGINT NOT NULL,
    node_id         VARCHAR(64),
    content_type    VARCHAR(32) COMMENT 'SMS/PUSH/EMAIL/WECHAT',
    content_text    TEXT COMMENT '待审核内容',
    audit_status    VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    auditor         VARCHAR(64),
    audit_comment   VARCHAR(500),
    audit_at        DATETIME,
    sensitive_words VARCHAR(500) COMMENT '命中的敏感词列表(逗号分隔)',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_canvas (tenant_id, canvas_id),
    INDEX idx_status (tenant_id, audit_status)
) COMMENT='内容审核记录';

CREATE TABLE sensitive_word (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id   BIGINT NOT NULL DEFAULT 0,
    word        VARCHAR(100) NOT NULL,
    category    VARCHAR(32) COMMENT 'POLITICAL/AD_LAW/PORNOGRAPHY/CUSTOM',
    enabled     TINYINT DEFAULT 1,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_word (tenant_id, word),
    INDEX idx_enabled (tenant_id, enabled)
) COMMENT='敏感词库';
```

- [ ] **Step 2: 创建 SensitiveWordService — 敏感词检测**

```java
@Service
@RequiredArgsConstructor
public class SensitiveWordService {
    private final SensitiveWordMapper wordMapper;

    public List<String> detect(Long tenantId, String text) {
        List<String> enabledWords = wordMapper.selectList(
            new LambdaQueryWrapper<SensitiveWordDO>()
                .eq(SensitiveWordDO::getTenantId, tenantId)
                .eq(SensitiveWordDO::getEnabled, 1))
            .stream().map(SensitiveWordDO::getWord).toList();
        return enabledWords.stream().filter(text::contains).toList();
    }
}
```

- [ ] **Step 3: 创建 ContentAuditService — 审核流程**

方法：`submitForAudit()` 提交审核、`approve()` 通过、`reject()` 驳回、`autoCheck()` 自动敏感词检测

- [ ] **Step 4: 画布发布前审核拦截**

在 `CanvasController.publish()` 中，发布前调用 `contentAuditService.autoCheck()`，命中敏感词则拒绝发布并返回命中的词列表。

- [ ] **Step 5: 创建 ContentAuditController — 审核API**

端点：提交审核、审批、驳回、查询审核状态、敏感词CRUD

- [ ] **Step 6: 编写测试**

测试用例：敏感词检测、审核通过后可发布、审核驳回不可发布、空内容不报错

- [ ] **Step 7: 提交**

```bash
git commit -m "feat: add content audit and sensitive word detection (#24)"
```

---

## Phase 1: 运营核心能力（P1，3-6周）

> 目标：补齐营销运营最常用的核心能力——权益、预算、生命周期、分析。可与 Phase 0 部分并行。

### Task 1.1: 优惠券管理系统（#28）

**Files:**
- Create: `V93__coupon_management.sql`
- Create: `CouponDO` + `CouponCodeDO` + Mapper
- Create: `CouponService` — 优惠券CRUD + 券码生成 + 发放 + 核销
- Create: `CouponController` — API
- Modify: `CouponHandler` — 扩展支持从本地 coupon 表发放
- Create: `frontend/src/pages/coupon-management/index.tsx`

- [ ] **Step 1: 创建 coupon + coupon_code 表**
- [ ] **Step 2: 创建 DO + Mapper**
- [ ] **Step 3: 创建 CouponService — 核心方法**

```java
// 创建优惠券定义
CouponDO createCoupon(CreateCouponRequest req);
// 批量生成券码
void generateCodes(Long couponId, int count, String prefix);
// 发放给用户（画布中调用）
CouponCodeDO issueToUser(Long couponId, String userId, String idempotencyKey);
// 核销回调
boolean redeemCode(String code, String userId);
// 查询统计
CouponStatsDTO getCouponStats(Long couponId);
```

- [ ] **Step 4: 扩展 CouponHandler 支持本地 coupon 表**

当前 CouponHandler 调外部API，扩展为：如果 config 中 `couponSource=LOCAL`，从本地 coupon 表发放；否则走外部API（兼容现有逻辑）。

- [ ] **Step 5: 创建 CouponController**
- [ ] **Step 6: 编写测试**
- [ ] **Step 7: 提交**

---

### Task 1.2: 活动预算管理（#30）

**Files:**
- Create: `V94__canvas_budget.sql`
- Create: `CanvasBudgetDO` + `ChannelCostDO` + Mapper
- Create: `BudgetService` — 预算设置/检查/消耗/预警
- Create: `BudgetController`
- Modify: `AbstractSendMessageHandler` — 发送前预算检查

- [ ] **Step 1: 创建 canvas_budget + channel_cost 表**

```sql
CREATE TABLE canvas_budget (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    canvas_id       BIGINT NOT NULL,
    total_budget    DECIMAL(12,2) COMMENT '总预算(元)',
    daily_budget    DECIMAL(12,2) COMMENT '日预算(元)',
    used_budget     DECIMAL(12,2) DEFAULT 0 COMMENT '已消耗(元)',
    channel_budgets JSON COMMENT '{"SMS":1000,"PUSH":0,"EMAIL":500}',
    status          VARCHAR(16) DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXHAUSTED/PAUSED',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_canvas (tenant_id, canvas_id)
);

CREATE TABLE channel_cost (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    channel         VARCHAR(32) NOT NULL,
    unit_cost       DECIMAL(8,4) NOT NULL COMMENT '单价(元)',
    effective_date  DATE NOT NULL,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_channel_date (tenant_id, channel, effective_date)
);
```

- [ ] **Step 2: 创建 BudgetService**

```java
@Service
@RequiredArgsConstructor
public class BudgetService {
    // 设置画布预算
    void setBudget(Long tenantId, Long canvasId, BigDecimal total, BigDecimal daily, Map<String,BigDecimal> channelBudgets);
    // 检查是否有预算
    boolean hasBudget(Long tenantId, Long canvasId, String channel);
    // 消耗预算（发送后调用）
    void consumeBudget(Long tenantId, Long canvasId, String channel, BigDecimal amount);
    // 预算预警检查
    void checkBudgetAlert(Long tenantId, Long canvasId);
}
```

- [ ] **Step 3: 在 AbstractSendMessageHandler 发送前检查预算**

```java
// 在 consentCheck + blacklistCheck 之后，send 之前
if (!budgetService.hasBudget(ctx.getTenantId(), canvasId, channel)) {
    return NodeResult.suppressed("budget_exhausted", config);
}
```

- [ ] **Step 4: 发送成功后消耗预算**

在 `deliveryService.send()` 成功回调中调用 `budgetService.consumeBudget()`。

- [ ] **Step 5: 编写测试**
- [ ] **Step 6: 提交**

---

### Task 1.3: 渠道成本追踪（#31）

**Files:**
- Modify: `V94__canvas_budget.sql` — channel_cost 表（已在 Task 1.2 创建）
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/delivery/ReachDeliveryService.java` — 发送后写成本
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java` — 新增成本统计端点

- [ ] **Step 1: 在 ReachDeliveryService 发送成功后计算并写入成本**

在 `message_send_record` 更新为 SENT 后，调用 `budgetService.consumeBudget()`。成本 = 1 × channel_unit_cost。

- [ ] **Step 2: 在 CanvasStatsController 新增成本统计端点**

`GET /canvas/{id}/cost` — 按渠道/按日汇总成本趋势

- [ ] **Step 3: 编写测试**
- [ ] **Step 4: 提交**

---

### Task 1.4: 用户生命周期阶段识别（#33）

**Files:**
- Create: `V95__lifecycle_stage.sql`
- Create: `LifecycleStageDO` + Mapper
- Create: `LifecycleService` — 阶段定义 + 规则计算
- Create: `LifecycleJob` — ElasticJob 定时计算
- Modify: CDP 用户属性 — 新增 lifecycle_stage 字段
- Create: `frontend/src/pages/lifecycle-config/index.tsx`

- [ ] **Step 1: 创建 lifecycle_stage 表**

```sql
CREATE TABLE lifecycle_stage (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    name            VARCHAR(64) NOT NULL COMMENT '新用户/活跃/成熟/衰退/流失',
    display_order   INT NOT NULL,
    rules           JSON NOT NULL COMMENT '[{"field":"days_since_last_active","op":"GT","value":30}]',
    color           VARCHAR(16) COMMENT '#52c41a',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_tenant_name (tenant_id, name)
);
```

- [ ] **Step 2: 创建 LifecycleService — 阶段计算逻辑**

```java
// 根据用户行为数据计算当前阶段
String computeStage(Long tenantId, String userId);
// 批量计算所有用户阶段（定时任务调用）
void batchComputeAllStages(Long tenantId);
```

- [ ] **Step 3: 创建 ElasticJob 定时任务 LifecycleJob**
- [ ] **Step 4: 画布 SELECTOR 节点支持按生命周期阶段筛选**
- [ ] **Step 5: 编写测试**
- [ ] **Step 6: 提交**

---

### Task 1.5: 生命周期自动化编排（#34）+ 阶段转换触发器（#35）

**Files:**
- Modify: `LifecycleService` — 阶段变更时发MQ事件
- Create: `LifecycleTriggerHandler` — 新增 LIFECYCLE_TRIGGER 节点类型
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/common/enums/NodeType.java` — 注册 LIFECYCLE_TRIGGER
- Modify: `TriggerRouteService` — 注册 lifecycle 事件路由

- [ ] **Step 1: LifecycleService 阶段变更时发 MQ 事件**

```java
// 在 computeStage() 检测到阶段变更时
if (!newStage.equals(oldStage)) {
    String topic = "lifecycle_stage_changed";
    rocketMQTemplate.convertAndSend(topic, new LifecycleChangeEvent(userId, oldStage, newStage, tenantId));
}
```

- [ ] **Step 2: 创建 LifecycleTriggerHandler**

```java
@NodeHandlerType("LIFECYCLE_TRIGGER")
public class LifecycleTriggerHandler extends AbstractNodeHandler {
    // 校验 config 中的 stageName 与事件中的 newStage 匹配
    // 匹配则通过，不匹配则 terminal
}
```

- [ ] **Step 3: 注册到 NodeType 枚举 + TriggerRouteService**
- [ ] **Step 4: 编写测试**
- [ ] **Step 5: 提交**

---

### Task 1.6: 事件分析（#17.1）+ 业务漏斗（#17.2）

**Files:**
- Modify: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasStatsController.java` — 新增事件分析 + 漏斗端点
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/dal/mapper/EventAnalyticsMapper.java`
- Create: `frontend/src/pages/canvas-analytics/index.tsx`

- [ ] **Step 1: 新增事件聚合分析端点**

`GET /analytics/events` — 按事件类型/属性维度聚合统计
`GET /analytics/events/trend` — 事件趋势

- [ ] **Step 2: 新增业务漏斗分析端点**

`POST /analytics/funnel` — 自定义步骤漏斗计算

```java
// 漏斗定义
public record FunnelDefinition(List<FunnelStep> steps, int windowHours) {}
public record FunnelStep(String eventCode, Map<String,String> filters) {}
```

- [ ] **Step 3: 创建 EventAnalyticsMapper — 漏斗SQL**

用窗口函数实现多步骤漏斗匹配：同一用户在时间窗口内按序完成各步骤。

- [ ] **Step 4: 前端分析页面**
- [ ] **Step 5: 编写测试**
- [ ] **Step 6: 提交**

---

### Task 1.7: 用户细查（#17.14）+ 智能预警（#17.15）

**Files:**
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/UserInsightController.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/AlertService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/policy/AlertJob.java`

- [ ] **Step 1: 用户细查端点**

`GET /user-insight/{userId}/timeline` — 用户行为时间线
`GET /user-insight/{userId}/profile` — 用户属性+标签+触达记录

- [ ] **Step 2: 智能预警服务**

```java
@Service
public class AlertService {
    // 定义告警规则
    void createAlertRule(Long tenantId, AlertRule rule);
    // 检查告警（定时任务调用）
    List<AlertEvent> checkAlerts(Long tenantId);
    // 发送告警通知（飞书Webhook）
    void notifyAlert(AlertEvent event);
}
```

- [ ] **Step 3: 创建 ElasticJob 定时告警检查**
- [ ] **Step 4: 编写测试**
- [ ] **Step 5: 提交**

---

### Task 1.8: 效果归因（#2）

**Files:**
- Create: `V96__attribution.sql`
- Create: `AttributionDO` + Mapper
- Create: `AttributionService` — 归因计算引擎
- Create: `AttributionController`
- Create: `AttributionJob` — 离线批处理归因

- [ ] **Step 1: 创建归因表**

```sql
CREATE TABLE attribution_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_id       BIGINT NOT NULL DEFAULT 0,
    user_id         VARCHAR(128) NOT NULL,
    conversion_event VARCHAR(64) NOT NULL COMMENT '转化事件',
    attribution_model VARCHAR(32) NOT NULL COMMENT 'LAST_TOUCH/FIRST_TOUCH/LINEAR/TIME_DECAY',
    touch_canvas_id BIGINT COMMENT '归因画布ID',
    touch_node_id   VARCHAR(64) COMMENT '归因节点ID',
    touch_channel   VARCHAR(32) COMMENT '触达渠道',
    touch_at        DATETIME NOT NULL COMMENT '触达时间',
    conversion_at   DATETIME NOT NULL COMMENT '转化时间',
    attribution_weight DECIMAL(5,4) DEFAULT 1.0 COMMENT '归因权重',
    window_hours    INT NOT NULL COMMENT '归因窗口(小时)',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_canvas (tenant_id, touch_canvas_id),
    INDEX idx_user (tenant_id, user_id)
);
```

- [ ] **Step 2: 创建 AttributionService — 4种归因模型**

末次触达：取归因窗口内最近一次触达
首次触达：取归因窗口内最早一次触达
线性：窗口内所有触达均分权重
时间衰减：越接近转化的触达权重越高

- [ ] **Step 3: 创建归因批处理定时任务**
- [ ] **Step 4: 创建归因报表端点**
- [ ] **Step 5: 编写测试**
- [ ] **Step 6: 提交**

---

## Phase 2: 规模化能力（P1-P2，2-4周）

> 依赖 Phase 0 合规基础 + Phase 1 核心能力。聚焦多画布/多团队/多渠道的规模化运营。

### Task 2.1: 全局疲劳度控制 UI（#1）

扩展 FrequencyCapHandler + 新增全局策略配置页面。技术要点已在缺项文档详述。

### Task 2.2: 全局静默期（#4）

扩展 QuietHoursHandler，新增 `system_option` 中 `quiet_hours_default` 配置作为全局默认。

### Task 2.3: 触达预览与试跑（#5）

在 AbstractSendMessageHandler 中新增 `dryRun` 模式，不实际发送只写 SKIPPED 记录。

### Task 2.4: 画布版本管理（#6）

扩展 `canvas_version` 表，每次保存生成版本快照，发布切换 active_version 指针。

### Task 2.5: 全局旅程视图（#7）

新增用户旅程查询API `GET /user-journey/{userId}`，前端时间线组件。

### Task 2.6: 变量与表达式（#10）

前端变量选择器 + 后端 SpEL 表达式求值。

### Task 2.7: 运营日历（#11）

甘特图/日历视图，展示所有画布运行时段 + 冲突检测。

### Task 2.8: 数据回流闭环（#12）

GoalCheckHandler + 自动动作触发（转化率低于阈值→自动暂停）。

### Task 2.9: 权限细化（#13）

RBAC → ABAC，新增 team 表 + canvas_team 关联表。

### Task 2.10: 审批流（#14）

画布新增 `approval_status` 字段，对接飞书审批API。

### Task 2.11: 模板市场（#3）

模板浏览、一键复制、模板贡献。前端新增模板市场页面。

### Task 2.12: 画布间编排（#9）

A画布结束时发MQ事件，B画布 EVENT_TRIGGER 订阅。循环检测在画布保存时静态分析。

### Task 2.13: 智能触达时机（#8）

从CDP读取用户偏好时段字段，作为 Delay 的目标时间。

### Task 2.14: 批量操作（#15）

前端列表页加复选框 + 批量操作栏。后端新增批量操作API。

### Task 2.15: 开放API/SDK（#16）

标准化 OpenAPI spec + Webhook 机制 + SDK 封装。

### Task 2.16: 运营计划（#19）

新增 `campaign_plan` 表，画布关联到计划。计划级别聚合统计。

### Task 2.17: 内容管理（#21）

新增 `content_template` 表 + 版本管理。模板引擎和触达Handler变量替换对齐。

### Task 2.18: 自定义查询（#23）

SQL查询沙箱化（只读、超时限制、行数限制）+ react-querybuilder 可视化查询。

### Task 2.19: 积分/会员体系（#29）

扩展 PointsOperationHandler 支持积分账户管理 + 会员等级规则。

### Task 2.20: A/B实验深化（#22）

调试设备管理 + 4步创建向导 + 动态/静态分流 + 标签自动生成 + 运行时长。详见缺项文档 #22 P0 优先级拆解。

---

## Phase 3: 进阶分析（P2-P3，3-6月）

> 依赖 OLAP 引擎引入。属于中长期规划。

### Task 3.1: OLAP 引擎引入

引入 ClickHouse/Doris 作为 event_log 的 OLAP 存储。双写：MySQL(实时) + ClickHouse(分析)。

### Task 3.2: 留存分析（#17.3）
### Task 3.3: 分布分析（#17.4）
### Task 3.4: 用户路径分析（#17.7）+ Sankey 图
### Task 3.5: 间隔分析（#17.8）
### Task 3.6: 用户群画像分析（#17.11）+ 画像报告（#17.12）+ 属性分析（#17.13）
### Task 3.7: ROI/ROAS计算（#32）— 依赖归因(#2) + 订单数据对接
### Task 3.8: 渠道追踪与广告分析（#18）— 需对接广告平台API
### Task 3.9: 微信互动运营（#20）— 需对接微信公众平台/企微API
### Task 3.10: 权益ROI分析（#26 原文档编号，实际为权益ROI）— 依赖订单系统

---

## 优先级总览

| Phase | 时间 | 任务数 | 核心交付 |
|-------|------|--------|---------|
| **Phase 0** | 1-2周 | 4 | 合规风控4项（授权+黑名单+频控+审核） |
| **Phase 1** | 3-6周 | 8 | 权益+预算+生命周期+分析+归因 |
| **Phase 2** | 2-4周 | 20 | 规模化运营全系列 |
| **Phase 3** | 3-6月 | 10 | OLAP+进阶分析+渠道追踪 |

**最短路径**：先做 Phase 0（4项合规），再做 Phase 1 中的 Task 1.8（归因）+ Task 1.6（分析）+ Task 1.7（用户细查+预警），即可获得最大的运营价值提升。

---

## 风险与缓解

| 风险 | 影响 | 缓解 |
|------|------|------|
| OLAP 引入成本高 | Phase 3 可能延期 | Phase 1/2 的分析用 MySQL 聚合，数据量可控时够用 |
| 外部系统对接（订单/广告/微信） | Task 1.2/2.20/3.8/3.9 依赖外部 | 先做内部闭环，外部对接用 WireMock 模拟 |
| 归因计算复杂度高 | Task 1.8 可能延期 | 先实现末次触达归因（最简单），逐步增加其他模型 |
| 预算管理精度 | 渠道单价可能变化 | channel_cost 表支持 effective_date，按日期取最新单价 |
