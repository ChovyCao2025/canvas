# 方向①：营销自动化深度 — 功能清单（详细版）

> 定位：把现有营销画布做到Braze级别，补齐运营管控层
> 策略评估：画布编排已L2，继续投入边际收益递减；补齐后仍是"中国版Braze"，无差异化护城河
> 建议：作为方向②的子集执行，不宜独立成主方向
> 竞品对标：Braze（全功能标杆）、Klaviyo（电商MA标杆）、Iterable（灵活事件驱动）、Convertlab（国内最接近）

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 对标Braze成熟度 |
|------|----------|-------------|----------------|
| 疲劳度策略 | 部分实现 | FrequencyCapHandler + MarketingPolicyService.consumeFrequency() | 40% |
| 归因引擎 | 不存在 | — | 0% |
| 合规门 | **完整** | MarketingPolicyService + SuppressionCheckHandler + QuietHoursHandler + ChannelAvailabilityHandler | 80% |
| 模板市场 | 部分实现 | CanvasTemplateDO/Mapper（缺Service/API/UI） | 15% |
| 画布版本管理 | **完整** | CanvasService.publish()/revertToVersion() + CanvasVersionDO | 70% |
| 运营日历 | 不存在 | — | 0% |
| 渠道回执 | 部分实现 | ReachDeliveryService（缺回调处理） | 20% |
| Webhook/事件订阅 | 部分实现 | WaitSubscriptionService（仅内部订阅） | 10% |
| 批量操作+审批流 | 部分实现 | ManualApprovalHandler（单节点审批） | 25% |
| 智能触达时机 | Stub | AiNextBestActionHandler（返回fallback） | 0% |
| 内容管理+模板渲染 | Stub | TemplateNodeHandler（透传）+ AbstractSendMessageHandler（$field变量解析） | 10% |

---

## P0 — 必须有，否则不可商用

---

### 1. 全局疲劳度策略 [中复杂度 | 2.5人月]

**现状**：FrequencyCapHandler仅支持固定阈值计数，作用域为GLOBAL/CHANNEL/NODE/JOURNEY四级。MarketingPolicyService.consumeFrequency()用Redis固定窗口计数器实现，无衰减、无画像、无跨画布协调。

**竞品对标**：
- Braze：全局频次上限+频道级+标签级+智能疲劳度（基于用户参与度自动调整）
- Iterable：频次上限+疲劳度保护+用户级疲劳度评分
- Klaviyo：智能发送时间+疲劳度自动抑制

#### 需补齐子功能

##### 1.1 跨画布疲劳度协调

**描述**：同一用户在多个运行中画布中被触达时，全局频次上限生效。当前FrequencyCapHandler仅检查单画布内频次。

**技术方案**：
```
发送前检查流程：
1. AbstractSendMessageHandler.executeAsync() 中，send前调用 GlobalFatigueService.check()
2. GlobalFatigueService 聚合检查：
   a. Redis GET user:{userId}:fatigue:global:{date} → 当日全局触达次数
   b. Redis GET user:{userId}:fatigue:{channel}:{date} → 当日渠道触达次数
   c. Redis GET user:{userId}:fatigue:weekly → 近7天触达次数
3. 任一计数超过策略上限 → 返回 FatigueExceeded + 记录拦截日志
4. 通过检查 → INCR计数器 + SET EX 86400（当日过期）+ 继续发送
```

**Redis Key设计**：
```
user:{userId}:fatigue:global:{yyyyMMdd}    → 当日全局触达次数 (TTL 48h)
user:{userId}:fatigue:{channel}:{yyyyMMdd}  → 当日渠道触达次数 (TTL 48h)
user:{userId}:fatigue:weekly                → 近7天触达次数 (ZSET, score=timestamp, 每24h清理)
```

**并发安全**：Redis INCR原子操作，无需加锁

**后端改动**：
- 新增 `GlobalFatigueService` — 全局疲劳度检查+计数
- 修改 `AbstractSendMessageHandler.executeAsync()` — 在send前插入全局疲劳度检查
- 修改 `FrequencyCapHandler` — 节点级疲劳度检查改为调用GlobalFatigueService

**前端改动**：
- 系统设置页 → "疲劳度策略"Tab → 全局策略配置表单
  - 每日全局上限（默认5次）
  - 每日渠道上限（EMAIL默认2次/SMS默认1次/PUSH默认3次/WECHAT默认2次）
  - 每周全局上限（默认15次）
  - 静默时段（默认22:00-08:00）

##### 1.2 疲劳度等级画像

**描述**：根据近期触达频次给用户打疲劳度标签，供画布条件节点使用。

**疲劳度等级定义**：
| 等级 | 条件 | 触达建议 |
|------|------|---------|
| FRESH | 7天内触达0-1次 | 可正常触达 |
| LIGHT | 7天内触达2-3次 | 注意频次 |
| MODERATE | 7天内触达4-5次 | 建议降频 |
| HEAVY | 7天内触达6-8次 | 强烈建议暂停 |
| DORMANT | 7天内触达9+次 | 强制暂停 |

**技术方案**：
```
更新触发：GlobalFatigueService.check() 每次检查时顺带更新
存储：
  - Redis: user:{userId}:fatigue:level → 等级字符串 (TTL 7d)
  - DB: user_fatigue_profile 表（批量持久化，每小时一次）
消费方式：IF_CONDITION节点支持 fatigue_level 条件判断
```

**后端改动**：
- 新增 `UserFatigueProfile` DO + Mapper
- `GlobalFatigueService` 增加 `updateFatigueLevel()` 方法
- `IfConditionHandler` 支持疲劳度等级条件判断

**前端改动**：
- 用户画像详情页 → 疲劳度标签展示（彩色Badge）
- IF_CONDITION节点配置 → 条件变量下拉增加"用户疲劳度等级"

##### 1.3 疲劳度恢复衰减

**描述**：超过N天未触达的用户自动降低疲劳等级，避免永久锁定。

**衰减算法**：
```
阶梯衰减（默认）：
  1天未触达：HEAVY→MODERATE / DORMANT→HEAVY
  3天未触达：MODERATE→LIGHT / HEAVY→MODERATE
  7天未触达：LIGHT→FRESH

指数衰减（可选）：
  fatigue_score = base_score * e^(-λ * days_since_last_touch)
  λ = 0.3 (半衰期约2.3天)
```

**触发方式**：定时任务（每小时）扫描 `user_fatigue_profile` 表，更新衰减后等级

##### 1.4 疲劳度Dashboard

**指标**：
| 指标 | 计算方式 |
|------|---------|
| 全局拦截率 | 被疲劳度拦截的发送数 / 总发送数 |
| 渠道拦截率 | 按渠道分别统计 |
| 疲劳用户占比 | HEAVY+DORMANT用户数 / 活跃用户数 |
| 日触达中位数 | 当日所有被触达用户的触达次数中位数 |
| 拦截趋势 | 最近7/30天拦截率变化 |

#### 数据库DDL

```sql
-- V82__fatigue_policy_and_profile.sql

CREATE TABLE global_fatigue_policy (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    policy_name VARCHAR(100) NOT NULL COMMENT '策略名称',
    is_default TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认策略',
    daily_global_limit INT NOT NULL DEFAULT 5 COMMENT '每日全局触达上限',
    weekly_global_limit INT NOT NULL DEFAULT 15 COMMENT '每周全局触达上限',
    channel_limits JSON COMMENT '渠道级上限 {"EMAIL":2,"SMS":1,"PUSH":3,"WECHAT":2}',
    quiet_hours_start TIME COMMENT '静默开始时间',
    quiet_hours_end TIME COMMENT '静默结束时间',
    decay_type VARCHAR(20) NOT NULL DEFAULT 'STEP' COMMENT '衰减类型 STEP/EXPONENTIAL',
    decay_config JSON COMMENT '衰减配置',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) COMMENT '全局疲劳度策略';

CREATE TABLE user_fatigue_profile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    fatigue_level VARCHAR(20) NOT NULL DEFAULT 'FRESH' COMMENT '疲劳度等级 FRESH/LIGHT/MODERATE/HEAVY/DORMANT',
    last_touch_time DATETIME COMMENT '最近触达时间',
    touch_count_1d INT NOT NULL DEFAULT 0 COMMENT '当日触达次数',
    touch_count_7d INT NOT NULL DEFAULT 0 COMMENT '近7天触达次数',
    touch_count_30d INT NOT NULL DEFAULT 0 COMMENT '近30天触达次数',
    suppressed_until DATETIME COMMENT '强制静默截止时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_user_tenant (user_id, tenant_id),
    INDEX idx_level (fatigue_level),
    INDEX idx_tenant (tenant_id)
) COMMENT '用户疲劳度画像';

CREATE TABLE fatigue_intercept_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    canvas_id BIGINT NOT NULL,
    node_id VARCHAR(64) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    intercept_reason VARCHAR(50) NOT NULL COMMENT '拦截原因 DAILY_LIMIT/WEEKLY_LIMIT/CHANNEL_LIMIT/QUIET_HOURS/FATIGUE_LEVEL',
    current_count INT COMMENT '当前计数',
    limit_value INT COMMENT '上限值',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_canvas (canvas_id),
    INDEX idx_created (created_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '疲劳度拦截日志';
```

#### API接口定义

```
# 全局疲劳度策略
POST   /api/fatigue/policy              创建策略
PUT    /api/fatigue/policy/{id}          更新策略
DELETE /api/fatigue/policy/{id}          删除策略
GET    /api/fatigue/policy/{id}          查询策略详情
GET    /api/fatigue/policy/list          查询策略列表

# 用户疲劳度画像
GET    /api/fatigue/profile/{userId}     查询用户疲劳度画像
POST   /api/fatigue/profile/{userId}/reset  重置用户疲劳度（手动解除）

# 疲劳度统计
GET    /api/fatigue/stats/overview       全局概览（拦截率/用户分布/趋势）
GET    /api/fatigue/stats/channel        按渠道统计
GET    /api/fatigue/stats/trend?days=30  拦截率趋势

# 拦截日志
GET    /api/fatigue/intercept-log        拦截日志查询（分页+筛选）
```

#### 前端组件拆分

| 组件 | 位置 | 描述 |
|------|------|------|
| FatiguePolicyForm | 系统设置/疲劳度策略 | 策略编辑表单（全局+渠道+静默时段+衰减配置） |
| FatigueProfileBadge | 用户画像详情页 | 疲劳度等级Badge（FRESH绿/LIGHT黄/MODERATE橙/HEAVY红/DORMANT灰） |
| FatigueStatsDashboard | 系统设置/疲劳度统计 | 统计卡片+拦截率趋势图+渠道分布饼图+用户等级分布 |
| FatigueConditionSelector | IF_CONDITION节点配置 | 疲劳度等级条件选择器 |

#### 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Redis Key空间膨胀 | 千万级用户 × 3个Key = 3千万Key | Key设TTL自动过期；大量用户用Bitmap替代 |
| 计数不精确 | Redis INCR与DB持久化之间可能不一致 | 以Redis为准，DB仅做备份；每小时对账 |
| 全局疲劳度影响发送性能 | 每次发送前多2-3次Redis查询 | Pipeline批量查询；本地Caffeine缓存策略配置 |

---

### 2. 合规门（发布前审核+发送链路拦截） [中复杂度 | 1.5人月]

**现状**：MarketingPolicyService已实现consentAllowed/suppressionAllowed/channelAvailable/quietHoursAllowed四个检查方法，SuppressionCheckHandler/QuietHoursHandler/ChannelAvailabilityHandler三个Handler存在，但**未接入实际发送链路**。合规检查仅在画布中显式配置合规节点时生效，无强制拦截。

**竞品对标**：
- Braze：全局合规策略+发送前强制拦截+PIPL/GDPR双合规+自动退订
- Iterable：合规门+发送前审计+自动退订+黑名单管理
- 国内平台：普遍弱，仅Convertlab有基础合规

#### 需补齐子功能

##### 2.1 发送链路强制接入

**描述**：每次发送前必须经过MarketingPolicyService检查，无论画布是否配置了合规节点。

**技术方案**：
```java
// AbstractSendMessageHandler.executeAsync() 改动点
@Override
public Mono<NodeResult> executeAsync(NodeConfig config, ExecutionContext ctx) {
    String userId = ctx.getFlatContext().get("userId");
    String channel = getChannelType();

    // === 新增：强制合规检查 ===
    return marketingPolicyService.checkAll(userId, channel, ctx.getCanvasId())
        .flatMap(result -> {
            if (!result.isAllowed()) {
                // 记录拦截日志
                complianceAuditService.logIntercept(userId, channel, ctx, result.getReason());
                return Mono.just(NodeResult.suppressed("COMPLIANCE_BLOCKED", result.getReason()));
            }
            return doSend(config, ctx);  // 原有发送逻辑
        });
}
```

**MarketingPolicyService.checkAll() 检查顺序**：
1. `consentAllowed()` — 用户是否授权（marketing_consent表）
2. `suppressionAllowed()` — 是否在抑制名单（marketing_suppression表）
3. `channelAvailable()` — 渠道是否可达（customer_channel表）
4. `quietHoursAllowed()` — 是否在静默时段
5. `fatigueAllowed()` — 疲劳度是否超限（对接GlobalFatigueService）

**关键决策**：合规检查是**硬拦截**（不可绕过）还是**软警告**（可配置）？
- 建议：P0级别（consent/suppression）硬拦截，P1级别（quiet_hours/fatigue）可配置

##### 2.2 发布前合规预检

**描述**：画布发布前自动检查合规配置是否完整。

**检查项**：
| 检查项 | 严重级别 | 说明 |
|--------|---------|------|
| 无授权检查节点 | ERROR | 画布应包含consent检查或依赖全局策略 |
| 无频次控制节点 | WARN | 建议至少包含FrequencyCap或全局策略已配置 |
| 无静默时段节点 | WARN | 建议包含QuietHours或全局策略已配置 |
| SMS渠道无退订链接 | ERROR | 短信必须包含退订方式 |
| EMAIL渠道无退订链接 | ERROR | 邮件必须包含Unsubscribe链接 |
| 人群包过大无审核 | WARN | 超过10万人群的画布建议配置审批节点 |

**技术方案**：
```java
public class CanvasComplianceValidator {
    public ValidationResult validate(CanvasDraft draft) {
        List<ValidationIssue> issues = new ArrayList<>();
        // 1. 检查是否包含合规节点
        checkComplianceNodes(draft, issues);
        // 2. 检查渠道内容合规
        checkContentCompliance(draft, issues);
        // 3. 检查人群规模
        checkAudienceSize(draft, issues);
        // 4. 检查全局策略是否已配置
        checkGlobalPolicy(draft, issues);
        return new ValidationResult(issues);
    }
}
```

**接入点**：`CanvasService.publish()` 方法中，发布前调用 `complianceValidator.validate()`

##### 2.3 合规拦截日志

**描述**：记录所有被合规拦截的发送及其原因，用于审计和排查。

##### 2.4 退订/黑名单管理

**描述**：用户点击退订后自动加入全局黑名单，支持渠道级退订（只退某渠道）。

**退订流程**：
```
1. 渠道商回调退订事件 → /api/compliance/unsubscribe/{channel}/{token}
2. 解析token → 获取userId + canvasId + channel
3. 写入 marketing_suppression 表（type=UNSUBSCRIBE, scope=CHANNEL/GLOBAL）
4. 同步到Redis缓存
5. 后续所有发送前检查 → suppressionAllowed() → 拦截
```

**退订Token生成**：
```java
// 发布时生成退订token，嵌入到消息内容中
String token = AES.encrypt(userId + ":" + canvasId + ":" + channel, JWT_SECRET);
String unsubscribeUrl = BASE_URL + "/api/compliance/unsubscribe/" + channel + "/" + token;
```

**前端退订页面**：
- 简洁的退订确认页（"您已成功退订XXX渠道的营销信息"）
- 可选：退订偏好页（选择保留哪些渠道）

#### 数据库DDL

```sql
-- V83__compliance_audit_and_unsubscribe.sql

CREATE TABLE compliance_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    canvas_id BIGINT NOT NULL COMMENT '画布ID',
    node_id VARCHAR(64) COMMENT '节点ID',
    channel VARCHAR(20) NOT NULL COMMENT '渠道',
    check_type VARCHAR(50) NOT NULL COMMENT '检查类型 CONSENT/SUPPRESSION/CHANNEL_AVAILABLE/QUIET_HOURS/FATIGUE/CONTENT',
    check_result VARCHAR(20) NOT NULL COMMENT '检查结果 ALLOWED/BLOCKED/WARNED',
    block_reason VARCHAR(200) COMMENT '拦截原因',
    detail JSON COMMENT '检查详情',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_canvas (canvas_id),
    INDEX idx_type_result (check_type, check_result),
    INDEX idx_created (created_at),
    INDEX idx_tenant (tenant_id)
) COMMENT '合规审计日志';

-- 退订记录表（扩展已有marketing_suppression表）
ALTER TABLE marketing_suppression ADD COLUMN unsubscribe_token VARCHAR(200) COMMENT '退订token';
ALTER TABLE marketing_suppression ADD COLUMN unsubscribe_channel VARCHAR(20) COMMENT '退订渠道';
ALTER TABLE marketing_suppression ADD COLUMN unsubscribe_source VARCHAR(50) COMMENT '退订来源 LINK/CALLBACK/MANUAL';
ALTER TABLE marketing_suppression ADD COLUMN scope VARCHAR(20) NOT NULL DEFAULT 'GLOBAL' COMMENT '抑制范围 GLOBAL/CHANNEL';
```

#### API接口定义

```
# 合规检查
POST   /api/compliance/validate/{canvasId}  发布前合规预检（返回检查结果列表）

# 合规审计
GET    /api/compliance/audit-log            查询合规审计日志（分页+筛选）
GET    /api/compliance/audit-log/stats       审计统计（拦截率/按类型分布/趋势）

# 退订
GET    /api/compliance/unsubscribe/{channel}/{token}  退订确认页
POST   /api/compliance/unsubscribe/{channel}/{token}  确认退订
POST   /api/compliance/resubscribe/{channel}/{token}  重新订阅

# 黑名单管理
GET    /api/compliance/suppression/list      查询黑名单（分页+筛选）
DELETE /api/compliance/suppression/{id}      移除黑名单
POST   /api/compliance/suppression/batch     批量添加黑名单
```

#### 前端组件拆分

| 组件 | 位置 | 描述 |
|------|------|------|
| ComplianceCheckStep | 画布发布对话框 | 发布前合规检查步骤（检查项列表+通过/警告/错误状态） |
| ComplianceAuditLog | 系统设置/合规审计 | 审计日志表格（筛选：时间/画布/渠道/检查类型/结果） |
| ComplianceStats | 系统设置/合规概览 | 合规统计卡片（拦截率/按类型分布/趋势图） |
| SuppressionManage | 系统设置/黑名单 | 黑名单管理表格（添加/移除/批量操作） |
| UnsubscribePage | 独立页面 | 退订确认页（无侧边栏，简洁样式） |
| UnsubscribePreference | 独立页面 | 退订偏好页（渠道级退订选择） |

#### 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 合规检查增加发送延迟 | 每次发送多1-5ms | Redis缓存consent/suppression状态；Pipeline查询 |
| 退订Token被篡改 | 恶意退订他人 | AES加密+HMAC签名+Token过期时间 |
| 审计日志量大 | 千万级发送→千万级日志 | 按月分表；90天后归档到对象存储 |
| 全局拦截误伤 | 合规策略过严影响正常发送 | 可配置策略级别（硬拦截/软警告）；紧急白名单机制 |

---

## P1 — 应该有，提升竞争力

---

### 3. 归因引擎 [高复杂度 | 4.5人月]

**现状**：不存在任何归因相关代码

**竞品对标**：
- Braze：多触点归因（末次/首次/线性/衰减）+ 归因窗口 + Canvas归因报告
- Iterable：归因分析 + 转化追踪 + ROI报告
- HubSpot：全链路归因 + 收入归因 + 自定义归因模型

#### 需从零构建子功能

##### 3.1 触点记录

**描述**：记录每个用户在画布中的所有触达事件，作为归因计算的基础数据。

**触点采集时机**：
```
AbstractSendMessageHandler.doSend() 成功后 →
  attributionService.recordTouchpoint(userId, canvasId, nodeId, channel, contentHash)
```

**触点数据结构**：
```json
{
  "userId": "u_12345",
  "canvasId": 456,
  "canvasVersion": 3,
  "nodeId": "send_email_1",
  "nodeType": "SEND_EMAIL",
  "channel": "EMAIL",
  "touchTime": "2026-06-01T10:30:00+08:00",
  "contentHash": "sha256:abc123",
  "campaignTag": "618大促",
  "tenantId": 1
}
```

**性能考虑**：
- 异步写入：Disruptor Ring Buffer → 批量INSERT（每1000条或每5秒）
- 归因查询走ES或ClickHouse（日千万级数据MySQL扛不住）
- 冷数据归档（90天前 → 对象存储）

##### 3.2 归因模型

**四种归因模型实现**：

| 模型 | 算法 | 适用场景 |
|------|------|---------|
| 末次触达 | 100%权重给归因窗口内最后一次触点 | 短决策链路/促销类 |
| 首次触达 | 100%权重给归因窗口内第一次触点 | 品牌认知/拉新类 |
| 线性 | 所有触点均分权重 | 长旅程/持续培育 |
| 时间衰减 | weight_i = e^(-λ × (T_conversion - T_touchpoint))，λ默认0.15 | 中等旅程/再营销 |

**归因计算流程**：
```
1. 转化事件到达 → attributionService.onConversion(userId, eventKey, conversionValue)
2. 查询归因窗口内（默认7天）该用户的所有触点
3. 按选定模型计算各触点权重
4. 写入 attribution_result 表
5. 更新画布/节点归因统计缓存
```

##### 3.3 转化事件定义

**描述**：用户自定义哪些事件算"转化"。

**转化事件来源**：
- API上报：`POST /api/conversion-event/report` — 业务系统上报转化事件
- Webhook触发：外部系统通过Webhook推送转化事件
- 行为事件：用户在App/小程序中完成购买/注册等行为

##### 3.4 ROI计算

**ROI = 归因收入 / 触达成本**

**成本模型**：
| 渠道 | 计费方式 | 默认单价 |
|------|---------|---------|
| SMS | 按条 | ¥0.05/条 |
| EMAIL | 按千条 | ¥2.0/千条 |
| PUSH | 免费 | ¥0 |
| WECHAT | 按千条 | ¥3.0/千条 |
| IN_APP | 免费 | ¥0 |

#### 数据库DDL

```sql
-- V84__attribution_engine.sql

CREATE TABLE attribution_touchpoint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    canvas_id BIGINT NOT NULL,
    canvas_version INT NOT NULL DEFAULT 1,
    node_id VARCHAR(64) NOT NULL,
    node_type VARCHAR(30) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    touch_time DATETIME(3) NOT NULL COMMENT '触达时间（毫秒精度）',
    content_hash VARCHAR(64) COMMENT '内容哈希（用于区分A/B变体）',
    campaign_tag VARCHAR(100) COMMENT '活动标签',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, touch_time),
    INDEX idx_canvas (canvas_id),
    INDEX idx_touch_time (touch_time),
    INDEX idx_tenant (tenant_id)
) COMMENT '归因触点记录';

CREATE TABLE conversion_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_name VARCHAR(100) NOT NULL COMMENT '事件名称',
    event_key VARCHAR(100) NOT NULL COMMENT '事件标识',
    description VARCHAR(500) COMMENT '描述',
    value_field VARCHAR(100) COMMENT '转化值字段（如order_amount）',
    default_value DECIMAL(12,2) COMMENT '默认转化值',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_key_tenant (event_key, tenant_id)
) COMMENT '转化事件定义';

CREATE TABLE conversion_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL,
    event_id BIGINT NOT NULL,
    conversion_time DATETIME(3) NOT NULL,
    conversion_value DECIMAL(12,2) COMMENT '转化金额',
    source VARCHAR(50) COMMENT '来源 API/WEBHOOK/BEHAVIOR',
    raw_data JSON COMMENT '原始事件数据',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_time (user_id, conversion_time),
    INDEX idx_event (event_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '转化记录';

CREATE TABLE attribution_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversion_record_id BIGINT NOT NULL,
    touchpoint_id BIGINT NOT NULL,
    model_type VARCHAR(20) NOT NULL COMMENT 'LAST_TOUCH/FIRST_TOUCH/LINEAR/TIME_DECAY',
    weight DECIMAL(5,4) NOT NULL COMMENT '归因权重 0.0000-1.0000',
    attribution_value DECIMAL(12,2) COMMENT '归因金额 = conversion_value × weight',
    window_days INT NOT NULL DEFAULT 7,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversion (conversion_record_id),
    INDEX idx_touchpoint (touchpoint_id),
    INDEX idx_model (model_type),
    INDEX idx_tenant (tenant_id)
) COMMENT '归因结果';

CREATE TABLE channel_cost_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    channel VARCHAR(20) NOT NULL,
    cost_model VARCHAR(20) NOT NULL DEFAULT 'PER_UNIT' COMMENT 'PER_UNIT/PER_THOUSAND/FIXED',
    unit_cost DECIMAL(10,4) NOT NULL COMMENT '单价',
    effective_from DATE NOT NULL COMMENT '生效日期',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    UNIQUE INDEX uk_channel_from (channel, effective_from, tenant_id)
) COMMENT '渠道成本配置';
```

#### API接口定义

```
# 归因模型配置
GET    /api/attribution/model/{canvasId}     查询画布归因模型配置
PUT    /api/attribution/model/{canvasId}     更新画布归因模型（模型类型+归因窗口）

# 转化事件
POST   /api/conversion-event                 定义转化事件
PUT    /api/conversion-event/{id}            更新转化事件
DELETE /api/conversion-event/{id}            删除转化事件
GET    /api/conversion-event/list            查询转化事件列表
POST   /api/conversion-event/report          上报转化事件（外部调用）

# 归因报告
GET    /api/attribution/report/{canvasId}    画布归因报告（总览）
GET    /api/attribution/report/{canvasId}/channels  按渠道归因
GET    /api/attribution/report/{canvasId}/nodes     按节点归因
GET    /api/attribution/report/{canvasId}/timeline  归因时间线
GET    /api/attribution/report/{canvasId}/roi       ROI报告

# 渠道成本
GET    /api/channel-cost/list                查询渠道成本配置
PUT    /api/channel-cost/{id}                更新渠道成本
```

#### 前端组件拆分

| 组件 | 位置 | 描述 |
|------|------|------|
| AttributionModelSelector | 画布设置 | 归因模型选择器（4种模型+归因窗口配置） |
| AttributionReport | 画布详情/归因报告 | 归因报告页（总览卡片+按渠道/节点/时间线Tab） |
| ConversionEventManage | 系统设置/转化事件 | 转化事件CRUD管理 |
| ROIBoard | 画布详情/ROI | ROI看板（投入/产出/ROI值+趋势图） |
| AttributionFunnelChart | 归因报告 | 触点→转化归因漏斗图 |

#### 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 触点数据量大 | 日千万级触点记录 | Disruptor异步批量写入；ES/ClickHouse查询；冷数据归档 |
| 归因计算实时性 | 转化后需实时计算归因 | 异步计算+缓存结果；报告页允许T+1 |
| 转化事件上报不可靠 | 外部系统可能漏报/重复 | 幂等处理（userId+eventKey+time去重）；补偿机制 |
| 归因模型选择困难 | 用户不知道选哪种 | 默认末次触达；提供模型对比视图 |

---

### 4. 模板市场UI [低复杂度 | 2.0人月]

**现状**：CanvasTemplateDO/Mapper存在，缺Service/API/UI

**竞品对标**：
- Braze：模板库+分类+评分+行业模板
- Iterable：模板市场+社区分享
- Convertlab：模板中心+场景模板

#### 需补齐子功能

##### 4.1 模板CRUD Service + API

**CanvasTemplateService**：
```java
public interface CanvasTemplateService {
    CanvasTemplateVO create(CreateTemplateReq req);          // 从画布保存为模板
    CanvasTemplateVO update(Long id, UpdateTemplateReq req); // 更新模板
    void delete(Long id);                                     // 删除模板
    CanvasTemplateVO getById(Long id);                        // 查询详情
    Page<CanvasTemplateVO> list(TemplateQueryReq req);        // 分页查询
    CanvasVO createFromTemplate(Long templateId);             // 从模板创建画布
    void rate(Long id, int score);                            // 评分
}
```

##### 4.2 模板市场页面

**页面结构**：
```
模板市场
├── 搜索栏（关键词搜索）
├── 分类标签栏
│   ├── 全部
│   ├── 欢迎旅程
│   ├── 流失挽回
│   ├── 活动通知
│   ├── 会员运营
│   ├── 购物车挽回
│   └── 节日营销
├── 模板卡片网格
│   ├── 模板缩略图（画布截图）
│   ├── 模板名称
│   ├── 分类标签
│   ├── 使用次数
│   ├── 评分
│   └── "使用此模板"按钮
└── 模板详情弹窗
    ├── 画布预览（大图）
    ├── 描述
    ├── 包含节点列表
    ├── 使用次数+评分
    └── "使用此模板"按钮
```

##### 4.3 模板截图生成

**描述**：保存模板时自动生成画布缩略图。

**技术方案**：
- 后端使用Playwright/Puppeteer无头浏览器渲染画布截图
- 或前端保存模板时截图并上传（更轻量）

#### 数据库DDL

```sql
-- V85__template_market.sql

ALTER TABLE canvas_template ADD COLUMN category VARCHAR(50) COMMENT '分类 WELCOME/CHURN/ACTIVITY/MEMBERSHIP/CART_RECOVERY/HOLIDAY';
ALTER TABLE canvas_template ADD COLUMN rating DECIMAL(3,2) NOT NULL DEFAULT 0.00 COMMENT '评分 0.00-5.00';
ALTER TABLE canvas_template ADD COLUMN rating_count INT NOT NULL DEFAULT 0 COMMENT '评分人数';
ALTER TABLE canvas_template ADD COLUMN use_count INT NOT NULL DEFAULT 0 COMMENT '使用次数';
ALTER TABLE canvas_template ADD COLUMN thumbnail_url VARCHAR(500) COMMENT '缩略图URL';
ALTER TABLE canvas_template ADD COLUMN description VARCHAR(500) COMMENT '模板描述';
ALTER TABLE canvas_template ADD COLUMN node_types JSON COMMENT '包含节点类型列表';
ALTER TABLE canvas_template ADD COLUMN is_official TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否官方模板';
ALTER TABLE canvas_template ADD COLUMN tags JSON COMMENT '标签列表';

CREATE TABLE template_rating (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    score TINYINT NOT NULL COMMENT '评分 1-5',
    comment VARCHAR(500) COMMENT '评论',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_template_user (template_id, user_id, tenant_id)
) COMMENT '模板评分';
```

#### API接口定义

```
# 模板CRUD
POST   /api/template                         从画布保存为模板
PUT    /api/template/{id}                    更新模板
DELETE /api/template/{id}                    删除模板
GET    /api/template/{id}                    查询模板详情
GET    /api/template/list                    分页查询（支持分类/关键词/排序筛选）
POST   /api/template/{id}/use               从模板创建画布
POST   /api/template/{id}/rate              评分

# 模板分类
GET    /api/template/categories              查询分类列表
```

#### 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 模板内容质量差 | 用户使用后体验差 | 官方预置10-20个高质量模板；审核机制 |
| 截图生成性能 | Playwright启动慢 | 前端截图方案更轻量；异步生成 |

---

### 5. 画布版本管理增强 [中复杂度 | 1.5人月]

**现状**：CanvasService已完整实现publish/revertToVersion/getVersions

#### 需补齐子功能

##### 5.1 版本对比

**技术方案**：
```java
// 对比两个版本的节点和连线差异
public class CanvasDiffService {
    public CanvasDiffResult diff(Long canvasId, Integer v1, Integer v2) {
        CanvasVersionDO ver1 = versionMapper.getByCanvasAndVersion(canvasId, v1);
        CanvasVersionDO ver2 = versionMapper.getByCanvasAndVersion(canvasId, v2);
        // 对比节点列表
        List<NodeDiff> nodeDiffs = diffNodes(ver1.getNodes(), ver2.getNodes());
        // 对比连线列表
        List<EdgeDiff> edgeDiffs = diffEdges(ver1.getEdges(), ver2.getEdges());
        // 对比全局配置
        ConfigDiff configDiff = diffConfig(ver1.getConfig(), ver2.getConfig());
        return new CanvasDiffResult(nodeDiffs, edgeDiffs, configDiff);
    }
}
```

**Diff类型**：ADDED / REMOVED / MODIFIED（高亮变更字段）

##### 5.2 版本备注

**改动**：`CanvasVersionDO` 增加 `changelog` 字段；`publish()` API增加changelog参数

##### 5.3 版本回滚确认

**预检内容**：
- 当前运行中的实例数
- 关联的定时触发器数量
- 与当前版本的节点差异摘要

##### 5.4 版本时间线

**前端**：垂直时间线组件，每条记录显示版本号+发布时间+发布人+changelog

#### 数据库DDL

```sql
ALTER TABLE canvas_version ADD COLUMN changelog VARCHAR(500) COMMENT '版本变更说明';
```

#### API接口定义

```
GET    /api/canvas/{id}/diff?v1=1&v2=2       版本对比
POST   /api/canvas/{id}/revert-check/{version} 回滚预检（返回影响分析）
```

---

### 6. 运营日历+冲突检测 [中复杂度 | 3.5人月]

**现状**：不存在

**竞品对标**：
- Braze：Calendar View + 冲突提示 + 人群重叠分析
- Iterable：Campaign Calendar + 排期管理
- HubSpot：Marketing Calendar + 拖拽排期

#### 需从零构建子功能

##### 6.1 运营日历视图

**技术方案**：
- 前端：FullCalendar React组件
- 数据源：画布的scheduled_start/scheduled_end + 定时触发器cron表达式展开
- 视图：日/周/月三种视图，每个画布一个色块

**日历数据API**：
```java
GET /api/calendar?start=2026-06-01&end=2026-06-30&view=month

Response:
[
  {
    "canvasId": 456,
    "canvasName": "618大促",
    "status": "RUNNING",
    "start": "2026-06-01T00:00:00+08:00",
    "end": "2026-06-18T23:59:59+08:00",
    "channels": ["EMAIL", "SMS", "WECHAT"],
    "audienceSize": 50000,
    "color": "#1890ff"
  }
]
```

##### 6.2 排期冲突检测

**冲突类型**：
| 冲突类型 | 检测逻辑 | 严重级别 |
|---------|---------|---------|
| 人群重叠 | 两个画布的目标人群Bitmap交集 > 阈值 | HIGH |
| 渠道冲突 | 同一渠道同一时段触达同一人群 | MEDIUM |
| 频次冲突 | 两个画布组合触达频次超过疲劳度上限 | HIGH |
| 时间重叠 | 两个画布执行时间完全重叠 | LOW |

**冲突检测算法**：
```
1. 查询时间范围内所有运行中画布
2. 对每对画布：
   a. 计算人群Bitmap交集 → 重叠率 = 交集大小 / 较小人群大小
   b. 如果重叠率 > 30% → 标记人群冲突
   c. 如果重叠率 > 30% 且渠道有交集 → 标记渠道冲突
3. 返回冲突列表
```

##### 6.3 人群重叠分析

**技术方案**：利用现有RoaringBitmap计算人群交集

##### 6.4 排期建议

**策略**：
- 人群重叠>50%：建议错开至少1天
- 渠道冲突：建议使用不同渠道触达重叠人群
- 频次冲突：建议降低触达频次或合并画布

#### 数据库DDL

```sql
-- V86__operation_calendar.sql

CREATE TABLE canvas_schedule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canvas_id BIGINT NOT NULL,
    scheduled_start DATETIME COMMENT '计划开始时间',
    scheduled_end DATETIME COMMENT '计划结束时间',
    audience_id BIGINT COMMENT '人群包ID',
    estimated_volume INT COMMENT '预估触达人数',
    actual_volume INT COMMENT '实际触达人数',
    color VARCHAR(7) COMMENT '日历色块颜色 #RRGGBB',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_canvas (canvas_id),
    INDEX idx_time_range (scheduled_start, scheduled_end),
    INDEX idx_tenant (tenant_id)
) COMMENT '画布排期';

CREATE TABLE schedule_conflict (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    canvas_id_1 BIGINT NOT NULL,
    canvas_id_2 BIGINT NOT NULL,
    conflict_type VARCHAR(30) NOT NULL COMMENT 'AUDIENCE_OVERLAP/CHANNEL_CONFLICT/FREQUENCY_CONFLICT/TIME_OVERLAP',
    severity VARCHAR(10) NOT NULL COMMENT 'HIGH/MEDIUM/LOW',
    overlap_rate DECIMAL(5,4) COMMENT '人群重叠率',
    detail JSON COMMENT '冲突详情',
    status VARCHAR(20) NOT NULL DEFAULT 'DETECTED' COMMENT 'DETECTED/ACKNOWLEDGED/RESOLVED/IGNORED',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_canvas1 (canvas_id_1),
    INDEX idx_canvas2 (canvas_id_2),
    INDEX idx_type (conflict_type),
    INDEX idx_tenant (tenant_id)
) COMMENT '排期冲突记录';
```

#### API接口定义

```
# 日历
GET    /api/calendar                         日历数据（按时间范围）
PUT    /api/calendar/{canvasId}/schedule     更新画布排期

# 冲突检测
POST   /api/calendar/conflict/detect         手动触发冲突检测
GET    /api/calendar/conflict/list           查询冲突列表
PUT    /api/calendar/conflict/{id}/status    更新冲突状态（确认/忽略/解决）

# 人群重叠
POST   /api/calendar/overlap                 计算两个人群包重叠率
```

#### 前端组件拆分

| 组件 | 位置 | 描述 |
|------|------|------|
| OperationCalendar | 运营日历页 | FullCalendar集成（日/周/月视图+画布色块） |
| ConflictAlert | 日历页/画布列表 | 冲突提示弹窗（冲突类型+重叠率+建议） |
| AudienceOverlapChart | 冲突详情 | 韦恩图展示人群重叠 |
| ScheduleSuggestion | 冲突详情 | 排期建议面板 |

---

### 7. 渠道回执追踪 [中复杂度 | 3.0人月]

**现状**：ReachDeliveryService有prepareRecord/markSent/markFailed，缺回调处理

**竞品对标**：
- Braze：全链路回执（发送→送达→打开→点击→退订）+ 实时Dashboard
- Iterable：回执追踪+漏斗分析+渠道对比
- Klaviyo：邮件回执（打开率/点击率/退订率）+ 实时统计

#### 需补齐子功能

##### 7.1 回执回调端点

**各渠道回调协议**：

| 渠道 | 回调协议 | 回调字段 |
|------|---------|---------|
| EMAIL | 邮件服务回调（如SendGrid/SES） | 送达/退回/打开/点击/退订/投诉 |
| SMS | 短信服务商回调（如阿里云SMS/Twilio） | 送达/失败/回复 |
| PUSH | APNs/FCM回调 | 送达/展示/点击 |
| WECHAT | 企微/公众号事件推送 | 送达/阅读/点击菜单/关注/取关 |

**统一回调端点**：
```
POST /api/receipt/callback/{channel}

Request Headers:
  X-Signature: HMAC-SHA256签名
  X-Channel: EMAIL/SMS/PUSH/WECHAT

Request Body (统一格式):
{
  "externalMessageId": "msg_xxx",
  "events": [
    {
      "type": "DELIVERED",       // DELIVERED/BOUNCED/OPENED/CLICKED/UNSUBSCRIBED/COMPLAINED
      "timestamp": "2026-06-01T10:30:00+08:00",
      "metadata": {              // 渠道特有字段
        "url": "https://...",
        "userAgent": "...",
        "ip": "1.2.3.4"
      }
    }
  ]
}
```

**安全校验**：
- HMAC-SHA256签名验证（防伪造回调）
- IP白名单（可选，仅允许渠道商IP）

##### 7.2 回执状态机

```
PENDING ──→ SENT ──→ DELIVERED ──→ OPENED ──→ CLICKED
   │           │
   │           └──→ BOUNCED (永久失败)
   │
   └──→ FAILED ──→ RETRY ──→ SENT (重试成功)
                   └──→ FAILED (重试耗尽)

任意状态 ──→ UNSUBSCRIBED (退订)
任意状态 ──→ COMPLAINED (投诉)
```

**状态更新规则**：
- 仅允许向前推进（PENDING→SENT→DELIVERED→OPENED→CLICKED）
- BOUNCED/FAILED为终态
- UNSUBSCRIBED/COMPLAINED触发合规流程

##### 7.3 回执统计

**统计维度**：
| 维度 | 指标 |
|------|------|
| 画布级 | 发送数/送达数/送达率/打开数/打开率/点击数/点击率/退订数/退订率 |
| 节点级 | 同上 |
| 渠道级 | 同上 |
| 时间趋势 | 按日/周/月的指标趋势 |

**打开率/点击率计算**：
```
送达率 = DELIVERED数 / SENT数 × 100%
打开率 = OPENED数 / DELIVERED数 × 100%
点击率 = CLICKED数 / DELIVERED数 × 100%
退订率 = UNSUBSCRIBED数 / DELIVERED数 × 100%
```

**防虚假打开**：
- 排除机器人打开（User-Agent过滤+IP频次限制）
- 1秒内多次打开只计1次

#### 数据库DDL

```sql
-- V87__receipt_tracking.sql

CREATE TABLE message_receipt (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    send_record_id BIGINT NOT NULL COMMENT '关联message_send_record.id',
    external_message_id VARCHAR(200) COMMENT '外部消息ID',
    channel VARCHAR(20) NOT NULL,
    event_type VARCHAR(20) NOT NULL COMMENT 'DELIVERED/BOUNCED/OPENED/CLICKED/UNSUBSCRIBED/COMPLAINED',
    event_time DATETIME(3) NOT NULL COMMENT '事件时间',
    url_clicked VARCHAR(500) COMMENT '点击的URL',
    user_agent VARCHAR(500) COMMENT 'User-Agent',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    metadata JSON COMMENT '渠道特有元数据',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_send_record (send_record_id),
    INDEX idx_external_msg (external_message_id),
    INDEX idx_event_type (event_type),
    INDEX idx_event_time (event_time),
    INDEX idx_tenant (tenant_id)
) COMMENT '消息回执记录';

-- message_send_record 扩展字段
ALTER TABLE message_send_record ADD COLUMN receipt_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '回执状态 PENDING/SENT/DELIVERED/OPENED/CLICKED/BOUNCED/FAILED/UNSUBSCRIBED/COMPLAINED';
ALTER TABLE message_send_record ADD COLUMN delivered_at DATETIME COMMENT '送达时间';
ALTER TABLE message_send_record ADD COLUMN opened_at DATETIME COMMENT '打开时间';
ALTER TABLE message_send_record ADD COLUMN clicked_at DATETIME COMMENT '点击时间';
ALTER TABLE message_send_record ADD COLUMN open_count INT NOT NULL DEFAULT 0 COMMENT '打开次数（去重后）';
ALTER TABLE message_send_record ADD COLUMN click_count INT NOT NULL DEFAULT 0 COMMENT '点击次数（去重后）';
```

#### API接口定义

```
# 回执回调（渠道商调用）
POST   /api/receipt/callback/{channel}       接收回执回调

# 回执统计
GET    /api/receipt/stats/{canvasId}         画布回执统计
GET    /api/receipt/stats/{canvasId}/nodes   按节点统计
GET    /api/receipt/stats/{canvasId}/channels 按渠道统计
GET    /api/receipt/stats/{canvasId}/trend   指标趋势

# 发送记录查询
GET    /api/receipt/records                   发送记录查询（分页+筛选）
GET    /api/receipt/records/{id}/detail       单条记录详情（含所有回执事件）
```

#### 前端组件拆分

| 组件 | 位置 | 描述 |
|------|------|------|
| ReceiptFunnelChart | 画布详情/回执 | 回执漏斗图（发送→送达→打开→点击） |
| ReceiptTrendChart | 画布详情/回执 | 指标趋势图（送达率/打开率/点击率） |
| ReceiptStatsCards | 画布详情/回执 | 统计卡片（关键指标一览） |
| SendRecordTable | 发送记录页 | 发送记录表格（筛选+分页+详情展开） |

#### 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 回调量大 | 日千万级回执事件 | Disruptor异步处理；批量写入 |
| 回调安全 | 伪造回调篡改数据 | HMAC签名验证+IP白名单 |
| 虚假打开 | 机器扫描导致打开率虚高 | UA过滤+去重+频次限制 |
| 渠道回调格式不一致 | 每个渠道回调格式不同 | 统一适配层，每渠道一个CallbackParser |

---

### 8. Webhook/事件订阅 [中复杂度 | 2.0人月]

**现状**：WaitSubscriptionService仅内部订阅

**竞品对标**：
- Braze：Webhook + 事件订阅 + 自定义事件
- Iterable：Webhook + 事件流API
- HubSpot：Webhook + 事件订阅 + 扩展点

#### 需补齐子功能

##### 8.1 Webhook注册

**订阅的事件类型**：
| 事件类型 | 触发时机 |
|---------|---------|
| CANVAS_STARTED | 画布启动 |
| CANVAS_COMPLETED | 画布完成 |
| CANVAS_FAILED | 画布失败 |
| NODE_EXECUTED | 节点执行完成 |
| MESSAGE_SENT | 消息发送 |
| MESSAGE_DELIVERED | 消息送达 |
| MESSAGE_OPENED | 消息打开 |
| MESSAGE_CLICKED | 消息点击 |
| MESSAGE_FAILED | 消息发送失败 |
| USER_UNSUBSCRIBED | 用户退订 |
| AUDIENCE_ENTERED | 用户进入人群 |
| CONVERSION_OCCURRED | 转化事件 |

##### 8.2 事件推送

**推送协议**：
```json
{
  "id": "evt_uuid",
  "type": "MESSAGE_SENT",
  "timestamp": "2026-06-01T10:30:00+08:00",
  "data": {
    "canvasId": 456,
    "canvasName": "618大促",
    "nodeId": "send_email_1",
    "userId": "u_12345",
    "channel": "EMAIL"
  },
  "signature": "hmac_sha256_hex"
}
```

##### 8.3 重试策略

```
重试间隔：1min → 5min → 30min → 2h → 6h（指数退避）
最大重试次数：5次
超时判定：HTTP响应超时10秒
成功判定：HTTP 2xx
失败判定：HTTP非2xx或超时
DLQ：5次重试失败后进入死信队列，保留7天
```

#### 数据库DDL

```sql
-- V88__webhook_subscription.sql

CREATE TABLE webhook_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '订阅名称',
    url VARCHAR(500) NOT NULL COMMENT '回调URL',
    secret VARCHAR(100) NOT NULL COMMENT 'HMAC签名密钥',
    events JSON NOT NULL COMMENT '订阅事件列表 ["MESSAGE_SENT","MESSAGE_OPENED"]',
    canvas_filter JSON COMMENT '画布ID过滤 [456, 789]，空=全部',
    channel_filter JSON COMMENT '渠道过滤 ["EMAIL","SMS"]，空=全部',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_active (is_active)
) COMMENT 'Webhook订阅';

CREATE TABLE webhook_delivery (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    subscription_id BIGINT NOT NULL,
    event_id VARCHAR(64) NOT NULL COMMENT '事件ID',
    event_type VARCHAR(50) NOT NULL,
    payload JSON NOT NULL COMMENT '推送内容',
    response_code INT COMMENT 'HTTP响应码',
    response_body TEXT COMMENT '响应内容',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/SUCCESS/FAILED/DLQ',
    attempts INT NOT NULL DEFAULT 0 COMMENT '尝试次数',
    max_attempts INT NOT NULL DEFAULT 5,
    next_retry_at DATETIME COMMENT '下次重试时间',
    last_attempt_at DATETIME COMMENT '最近尝试时间',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_subscription (subscription_id),
    INDEX idx_status (status),
    INDEX idx_next_retry (next_retry_at),
    INDEX idx_tenant (tenant_id)
) COMMENT 'Webhook推送记录';
```

#### API接口定义

```
# Webhook订阅CRUD
POST   /api/webhook/subscription              创建订阅
PUT    /api/webhook/subscription/{id}         更新订阅
DELETE /api/webhook/subscription/{id}         删除订阅
GET    /api/webhook/subscription/{id}         查询详情
GET    /api/webhook/subscription/list         查询列表

# Webhook推送记录
GET    /api/webhook/delivery                  推送记录查询（分页+筛选）
POST   /api/webhook/delivery/{id}/retry       手动重试

# Webhook测试
POST   /api/webhook/subscription/{id}/test    发送测试事件
```

#### 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 推送风暴 | 大量事件同时推送压垮订阅方 | 限流（每秒最多100条/订阅）；批量聚合推送 |
| 回调URL不可达 | 大量重试消耗资源 | 指数退避；5次失败进DLQ；连续失败自动暂停 |
| 安全风险 | 暴露内部数据结构 | HMAC签名+最小化payload+字段过滤 |

---

### 9. 批量操作+审批流 [低复杂度 | 2.0人月]

**现状**：ManualApprovalHandler支持单节点审批

#### 需补齐子功能

##### 9.1 批量画布操作

**支持的批量操作**：
| 操作 | 条件 | 影响 |
|------|------|------|
| 批量启动 | 仅DRAFT/STOPPED状态 | 创建执行实例 |
| 批量停止 | 仅RUNNING状态 | 优雅停止运行实例 |
| 批量归档 | 仅STOPPED/OFFLINE状态 | 修改状态 |
| 批量删除 | 仅ARCHIVED状态 | 软删除 |

**技术方案**：
```
1. 用户勾选多个画布 → 选择批量操作
2. 前端发送 batchIds + operation 到后端
3. 后端逐个校验状态 → 生成操作计划
4. 用户确认 → 异步执行（Disruptor事件驱动）
5. 返回操作结果（成功N个/失败N个/跳过N个）
```

##### 9.2 多级审批流

**审批流定义**：
```json
{
  "name": "画布发布审批",
  "steps": [
    { "level": 1, "name": "运营主管审批", "approvers": ["user_key_1", "user_key_2"], "type": "ANY" },
    { "level": 2, "name": "营销总监审批", "approvers": ["user_key_3"], "type": "ALL" }
  ],
  "scope": {
    "trigger": "PUBLISH",
    "conditions": [
      { "field": "audienceSize", "operator": ">", "value": 100000 }
    ]
  }
}
```

**审批类型**：
- ANY：任一审批人通过即可
- ALL：所有审批人都需通过

**触发条件**：
- 画布发布时
- 人群包超过阈值时
- 特定渠道（如SMS）时

##### 9.3 审批代理

**描述**：审批人请假时指定代理人，审批通知自动转交。

#### 数据库DDL

```sql
-- V89__batch_and_approval_flow.sql

CREATE TABLE approval_flow (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '审批流名称',
    steps JSON NOT NULL COMMENT '审批步骤定义',
    trigger_type VARCHAR(30) NOT NULL COMMENT '触发类型 PUBLISH/START/DELETE',
    conditions JSON COMMENT '触发条件',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id)
) COMMENT '审批流定义';

CREATE TABLE approval_chain (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    flow_id BIGINT NOT NULL COMMENT '审批流ID',
    canvas_id BIGINT COMMENT '关联画布ID',
    current_step INT NOT NULL DEFAULT 1 COMMENT '当前步骤',
    total_steps INT NOT NULL COMMENT '总步骤数',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED/CANCELLED',
    submitted_by VARCHAR(64) NOT NULL COMMENT '提交人',
    result JSON COMMENT '各步骤审批结果',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_canvas (canvas_id),
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '审批链实例';

CREATE TABLE approval_delegation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    delegator VARCHAR(64) NOT NULL COMMENT '原审批人',
    delegate VARCHAR(64) NOT NULL COMMENT '代理人',
    start_time DATETIME NOT NULL COMMENT '代理开始时间',
    end_time DATETIME NOT NULL COMMENT '代理结束时间',
    reason VARCHAR(200) COMMENT '代理原因',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_delegator (delegator),
    INDEX idx_delegate (delegate),
    INDEX idx_tenant (tenant_id)
) COMMENT '审批代理配置';

CREATE TABLE batch_operation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operation_type VARCHAR(30) NOT NULL COMMENT 'START/STOP/ARCHIVE/DELETE',
    target_ids JSON NOT NULL COMMENT '目标画布ID列表',
    total_count INT NOT NULL COMMENT '总数',
    success_count INT NOT NULL DEFAULT 0,
    failed_count INT NOT NULL DEFAULT 0,
    skipped_count INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/COMPLETED/FAILED',
    results JSON COMMENT '逐项结果',
    submitted_by VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_tenant (tenant_id)
) COMMENT '批量操作记录';
```

---

### 10. 内容管理+模板渲染引擎 [中复杂度 | 3.5人月]

**现状**：TemplateNodeHandler透传，AbstractSendMessageHandler仅支持$field变量解析

**竞品对标**：
- Braze：内容卡片+动态内容+模板+个性化变量+拖拽编辑器
- Iterable：内容模板+变量插值+条件内容块
- Klaviyo：拖拽邮件编辑器+动态内容+模板库

#### 需补齐子功能

##### 10.1 模板引擎集成

**选型**：Mustache（轻量、逻辑-less、安全）

**为什么选Mustache而非FreeMarker/Thymeleaf**：
- FreeMarker功能强但有过多的逻辑能力，安全风险高（可执行任意Java方法）
- Thymeleaf面向HTML模板，不适合消息模板
- Mustache逻辑-less，天然防注入，适合用户自定义模板

**集成方案**：
```java
public class TemplateRenderService {
    private final MustacheFactory mustacheFactory;

    public String render(String template, Map<String, Object> context) {
        Mustache mustache = mustacheFactory.compile(new StringReader(template), "template");
        StringWriter writer = new StringWriter();
        mustache.execute(writer, context);
        return writer.toString();
    }
}
```

**支持的变量语法**：
```
{{user.name}}              → 用户姓名
{{user.email}}             → 用户邮箱
{{user.tags.行业}}          → 用户标签
{{canvas.name}}            → 画布名称
{{node.config.discount}}   → 节点配置
{{#if user.vip}}尊享{{/if}} → 条件内容块
```

##### 10.2 内容库

**内容类型**：
| 类型 | 字段 | 用途 |
|------|------|------|
| TEXT | body | 短信正文/推送文案 |
| RICH_TEXT | subject, body, previewText | 邮件正文 |
| IMAGE | imageUrl, altText | 图片素材 |
| LINK | url, text, trackingEnabled | 链接 |
| COUPON | couponCode, discount, validUntil | 优惠券 |

##### 10.3 内容预览

**技术方案**：
```java
// 用真实用户数据渲染模板预览
GET /api/content/preview?contentId=123&userId=u_12345

Response:
{
  "subject": "张三，618大促来了！",
  "body": "亲爱的张三，您的专属优惠券CODE_123已到账...",
  "previewText": "张三，您的618专属优惠"
}
```

##### 10.4 A/B内容变体

**描述**：同一节点可配置多个内容变体，按流量比例随机分配。

**技术方案**：
- 扩展节点配置：`contentVariants: [{name:"A", contentId:1, percent:50}, {name:"B", contentId:2, percent:50}]`
- 执行时：`ThreadLocalRandom.nextDouble() < 0.5 → A : B`
- 回执统计按变体分组

#### 数据库DDL

```sql
-- V90__content_management.sql

CREATE TABLE content_library (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '内容名称',
    type VARCHAR(20) NOT NULL COMMENT 'TEXT/RICH_TEXT/IMAGE/LINK/COUPON',
    category VARCHAR(50) COMMENT '分类',
    content_body JSON NOT NULL COMMENT '内容主体（不同类型结构不同）',
    variables JSON COMMENT '变量列表 ["user.name","user.email"]',
    tags JSON COMMENT '标签列表',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_category (category),
    INDEX idx_tenant (tenant_id)
) COMMENT '内容库';

CREATE TABLE content_variant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    node_id VARCHAR(64) NOT NULL COMMENT '画布节点ID',
    canvas_id BIGINT NOT NULL COMMENT '画布ID',
    variant_name VARCHAR(50) NOT NULL COMMENT '变体名称 A/B/C',
    content_id BIGINT COMMENT '关联content_library.id',
    traffic_percent DECIMAL(5,2) NOT NULL DEFAULT 100.00 COMMENT '流量百分比',
    is_control TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否对照组',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_node (canvas_id, node_id),
    INDEX idx_tenant (tenant_id)
) COMMENT '内容变体';
```

---

## P2 — 锦上添花

---

### 11. 智能触达时机 [高复杂度 | 4.0人月]

**现状**：AiNextBestActionHandler是Stub

**竞品对标**：
- Braze：Intelligent Timing（基于用户历史行为预测最佳发送时间）
- Klaviyo：Smart Send Time
- Iterable：Send Time Optimization

**前置依赖**：渠道回执数据积累（功能7），至少3个月历史数据

#### 需从零构建子功能

##### 11.1 用户活跃时段分析

**算法**：
```
1. 收集用户过去90天的行为事件（打开邮件/点击链接/App活跃/小程序访问）
2. 按24小时分桶（0:00-1:00, 1:00-2:00, ...）
3. 每个桶计算活跃概率 = 该时段活跃天数 / 90
4. 输出：user_active_pattern = {0: 0.01, 1: 0.005, ..., 9: 0.15, 10: 0.25, ..., 21: 0.12, ...}
```

**存储**：
- Redis Hash: `user:{userId}:active_pattern` → 24个field（0-23），value为活跃概率
- 每周更新一次

##### 11.2 最佳触达时机推荐

**算法**：
```
1. 取用户活跃时段分布
2. 找到最高概率时段 P_max
3. 推荐发送时间 = P_max 对应时段的中点 ± 30分钟
4. 如果用户无历史数据 → 使用人群包全局活跃时段
5. 如果人群包也无数据 → 使用行业默认值（10:00/14:00/20:00）
```

##### 11.3 时段热度图

**描述**：可视化用户群的活跃时段分布（24h × 7day 热度矩阵）

##### 11.4 A/B验证

**描述**：对比智能时机组 vs 固定时间组的效果差异

#### 风险点

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 冷启动 | 新用户无历史数据 | 使用人群包全局模式/行业默认值 |
| 数据不足 | 用户行为数据太少无法预测 | 最低30天数据才启用智能时机 |
| 计算量大 | 千万用户×24时段概率 | 离线计算+缓存；增量更新 |

---

## 工作量估算（详细）

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | 疲劳度策略 | 1.5 | 1.0 | 0.5 | 3.0 |
| P0 | 合规门接入 | 1.0 | 0.5 | 0.5 | 2.0 |
| P1 | 归因引擎 | 3.0 | 1.5 | 1.0 | 5.5 |
| P1 | 模板市场UI | 0.5 | 1.5 | 0.5 | 2.5 |
| P1 | 画布版本管理增强 | 0.5 | 1.0 | 0.3 | 1.8 |
| P1 | 运营日历 | 2.0 | 1.5 | 0.5 | 4.0 |
| P1 | 渠道回执 | 2.0 | 1.0 | 0.5 | 3.5 |
| P1 | Webhook/事件订阅 | 1.5 | 0.5 | 0.5 | 2.5 |
| P1 | 批量操作+审批流 | 1.0 | 1.0 | 0.3 | 2.3 |
| P1 | 内容管理+模板渲染 | 2.0 | 1.5 | 0.5 | 4.0 |
| P2 | 智能触达时机 | 3.0 | 1.0 | 0.5 | 4.5 |
| | **合计** | **18.0** | **11.0** | **5.1** | **35.6** |

---

## 执行顺序建议

```
Sprint 1 (P0, 5人月): 疲劳度策略 + 合规门接入
  → 产出：可安全商用的合规营销引擎
  → 里程碑：发送链路强制合规检查+全局疲劳度上线

Sprint 2 (P1-速胜, 6.6人月): 模板市场UI + 画布版本管理增强 + 批量操作+审批流
  → 产出：运营效率大幅提升
  → 里程碑：模板市场上线+审批流可用

Sprint 3 (P1-核心数据, 10人月): 渠道回执 + Webhook/事件订阅 + 内容管理+模板渲染
  → 产出：闭环数据+开放能力+内容能力
  → 里程碑：回执漏斗可视化+外部Webhook推送+Mustache模板渲染

Sprint 4 (P1-深度分析, 9.5人月): 归因引擎 + 运营日历
  → 产出：效果可量化+运营可规划
  → 里程碑：4种归因模型上线+日历冲突检测

Sprint 5 (P2, 4.5人月): 智能触达时机
  → 产出：AI辅助（需回执数据积累3个月）
  → 里程碑：智能时机推荐上线
```

---

## 与方向②的关系

方向①中的功能是方向②私域中台的**基础层**：

| 方向①功能 | 方向②依赖方式 |
|-----------|-------------|
| 疲劳度策略(P0) | 私域触达**必须**有疲劳度控制 |
| 合规门(P0) | 企微/短信触达**必须**合规 |
| 渠道回执(P1) | 企微消息回执追踪 |
| Webhook(P1) | 企微事件回调基础 |
| 内容管理(P1) | 私域内容素材管理 |
| 归因引擎(P1) | 私域ROI量化 |
| 运营日历(P1) | 私域活动排期 |

建议将方向①作为方向②的**必要前置**，Sprint 1-3与方向②的Phase 0-1并行推进。
