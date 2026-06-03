# 方向⑫：多租户SaaS化 — 功能清单

> 定位：不是"加个tenant_id"的补丁，而是从单租户应用改造为真正的SaaS平台
> 策略评估：没有多租户，所有商业化都是空谈；当前tenant_id虽存在但未强制隔离（CRITICAL）
> 竞品对标：Braze(原生多租户)、Iterable(原生多租户)、神策(SaaS+私有化双模式)
> 建议：**P0必须做**，是所有方向商业化的前提

---

## 现状盘点

| 功能 | 实现程度 | 核心现有代码 | 差距 |
|------|----------|-------------|------|
| 租户CRUD | **部分** | TenantDO+TenantService+TenantController（创建/禁用/激活/用量查询） | 缺租户自助注册、租户设置页、租户主题 |
| 租户上下文 | **部分** | TenantContext(tenantId+role+username)+TenantContextResolver(从JWT Claims解析) | 仅解析，未强制注入到所有查询 |
| 租户隔离 | **严重不足** | 仅TenantService.usage()中手动eq("tenant_id") | 大量Controller/Service查询未过滤tenant_id（CRITICAL） |
| 配额管理 | **部分** | CanvasUserQuotaDO(Redis INCR+DB异步落盘)+TenantDO.quotaJson | 仅画布级用户配额，无租户级资源配额 |
| 计费系统 | **不存在** | — | 完全缺失 |
| 套餐/订阅 | **不存在** | TenantDO.planCode仅字符串，无Plan定义表 | 完全缺失 |
| 租户自助 | **不存在** | — | 无注册/设置/账单页面 |
| 数据隔离验证 | **不存在** | — | 无自动化租户隔离测试 |
| 审计日志 | **不存在** | — | 有操作记录需求但无实现 |

### 关键问题：租户隔离不完整

当前状态：
- TenantContextResolver能从JWT解析tenantId ✅
- TenantController仅SuperAdmin可操作 ✅
- **但**：大部分Controller/Service查询未自动注入tenant_id过滤 ❌
- **但**：ApiDefinitionDO无tenant_id字段 ❌
- **但**：无MyBatis-Plus TenantLineInnerInterceptor自动拦截 ❌

---

## 功能清单

### P0 — 租户隔离（CRITICAL）

---

#### 1. MyBatis-Plus租户拦截器 [中复杂度 | 2.0人月]

**现状**：手动在部分查询中加tenant_id过滤，大量遗漏

**目标**：所有SQL自动追加`WHERE tenant_id = ?`

**技术方案**：

```java
@Configuration
public class MybatisPlusTenantConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(TenantContextResolver resolver) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new TenantLineHandler() {
            @Override
            public Expression getTenantId() {
                // 从Reactive上下文获取tenantId
                Long tenantId = TenantContextHolder.get();
                return new LongValue(tenantId != null ? tenantId : 0L);
            }

            @Override
            public boolean ignoreTable(String tableName) {
                // 忽略不需要租户隔离的表
                return IGNORE_TABLES.contains(tableName);
            }
        }));
        return interceptor;
    }
}
```

**需处理的问题**：

| 问题 | 解决方案 |
|------|---------|
| Reactive上下文传递 | TenantContextHolder基于Reactor Context传播，非ThreadLocal |
| 忽略表 | tenant表自身、sys_user表（跨租户）、flyway_schema_history |
| 管理员跨租户查询 | SuperAdmin角色绕过租户拦截器 |
| 现有无tenant_id表 | 逐表添加tenant_id列+数据迁移 |
| 联表查询 | JOIN的表也需自动追加tenant_id |

**需添加tenant_id的表**（当前缺失）：

```sql
-- 以下表缺少tenant_id字段，需补齐
ALTER TABLE api_definition ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE canvas_node ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0;
ALTER TABLE canvas_edge ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 0;
-- ... 逐表排查
```

---

#### 2. 租户上下文Reactor传播 [低复杂度 | 1.0人月]

**现状**：TenantContextResolver从JWT解析，但未传播到DB查询层

**需补齐**：

```java
// WebFilter：JWT解析后写入Reactor Context
@Component
public class TenantContextWebFilter implements WebFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange)
            .contextWrite(ctx -> {
                TenantContext tc = resolveFromExchange(exchange);
                return tc != null ? ctx.put(TenantContext.KEY, tc) : ctx;
            });
    }
}

// TenantContextHolder：Reactor Context读取
public class TenantContextHolder {
    public static Long get() {
        return Mono.deferContextual(ctx ->
            ctx.hasKey(TenantContext.KEY)
                ? Mono.justOrEmpty(ctx.get(TenantContext.KEY).tenantId())
                : Mono.empty()
        ).block(); // 仅在boundedElastic中调用
    }

    public static Mono<Long> getReactive() {
        return Mono.deferContextual(ctx ->
            ctx.hasKey(TenantContext.KEY)
                ? Mono.justOrEmpty(ctx.get(TenantContext.KEY).tenantId())
                : Mono.empty()
        );
    }
}
```

---

#### 3. 租户隔离自动化测试 [中复杂度 | 1.5人月]

**目标**：自动化验证所有API端点的租户隔离

**方案**：

| 测试类型 | 描述 |
|---------|------|
| 数据隔离测试 | 租户A创建的数据，租户B不可见 |
| 越权测试 | 租户A的token访问租户B的资源返回403/404 |
| 管理员测试 | SuperAdmin可跨租户访问 |
| 批量端点扫描 | 自动扫描所有Controller端点，验证tenant_id过滤 |

```java
// 隔离测试基类
@SpringBootTest
public abstract class TenantIsolationTestBase {

    @Test
    void tenantACannotSeeTenantBData() {
        // 1. 用租户A的token创建画布
        String canvasIdA = createCanvasWithToken(tokenA, "Canvas-A");
        // 2. 用租户B的token查询，应不可见
        List<CanvasDTO> results = listCanvasesWithToken(tokenB);
        assertThat(results).noneMatch(c -> c.getName().equals("Canvas-A"));
        // 3. 用租户B的token直接访问，应404
        getCanvasWithToken(tokenB, canvasIdA).expectStatus().isNotFound();
    }
}
```

---

### P1 — 套餐与计费

---

#### 4. 套餐定义与订阅管理 [中复杂度 | 2.0人月]

**现状**：TenantDO.planCode仅字符串，无Plan定义表

**需补齐**：

| 套餐 | MAU | 画布数 | 执行次数/月 | 触达次数/月 | 价格 |
|------|-----|--------|------------|------------|------|
| Free | 1,000 | 5 | 10,000 | 50,000 | ¥0 |
| Starter | 10,000 | 20 | 100,000 | 500,000 | ¥2,999/月 |
| Professional | 100,000 | 100 | 1,000,000 | 5,000,000 | ¥9,999/月 |
| Enterprise | 无限 | 无限 | 无限 | 无限 | 定制 |

**数据库DDL**：

```sql
CREATE TABLE subscription_plan (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_code VARCHAR(30) NOT NULL COMMENT '套餐标识 FREE/STARTER/PROFESSIONAL/ENTERPRISE',
    name VARCHAR(100) NOT NULL COMMENT '套餐名称',
    description VARCHAR(500),
    price_monthly DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '月价格(元)',
    price_yearly DECIMAL(10,2) COMMENT '年价格(元)',
    limits JSON NOT NULL COMMENT '资源限制 {"mau":10000,"canvasCount":20,"executionCount":100000,"deliveryCount":500000}',
    features JSON COMMENT '功能列表 ["api_call","ab_test","webhook"]',
    is_public TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否公开可订阅',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_code (plan_code)
) COMMENT '订阅套餐定义';

CREATE TABLE tenant_subscription (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/EXPIRED/CANCELLED/TRIAL',
    started_at DATETIME NOT NULL,
    expires_at DATETIME COMMENT '过期时间(null=永不过期)',
    cancelled_at DATETIME,
    trial_ends_at DATETIME COMMENT '试用结束时间',
    auto_renew TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (status),
    INDEX idx_expires (expires_at)
) COMMENT '租户订阅记录';
```

---

#### 5. 资源配额与限流 [中复杂度 | 2.0人月]

**现状**：CanvasUserQuotaDO仅画布级用户配额，无租户级资源配额

**需补齐**：

| 配额维度 | 描述 | 限流方式 |
|---------|------|---------|
| MAU(月活用户) | 每月触达的唯一用户数 | Redis HyperLogLog计数 |
| 画布数量 | 租户可创建的画布数 | DB COUNT检查 |
| 执行次数 | 每月画布执行次数 | Redis INCR + 月度重置 |
| 触达次数 | 每月消息发送次数 | Redis INCR + 月度重置 |
| 存储空间 | 附件/模板占用空间 | 对象存储配额 |
| API调用量 | 每月API调用次数 | Redis INCR + 月度重置 |

**配额检查流程**：

```
1. 画布发布 → 检查画布数量配额
2. 画布执行 → 检查执行次数配额 → 检查MAU配额
3. 消息发送 → 检查触达次数配额
4. API调用 → 检查API调用量配额
5. 文件上传 → 检查存储空间配额
6. 配额超限 → 返回429 Too Many Requests + 配额信息Header
```

**配额超限响应**：

```json
HTTP 429 Too Many Requests
X-RateLimit-Limit: 100000
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 2026-07-01T00:00:00+08:00

{
  "error": "QUOTA_EXCEEDED",
  "message": "月执行次数已达上限(100,000次)，请升级套餐",
  "quota": {
    "limit": 100000,
    "used": 100023,
    "resetAt": "2026-07-01T00:00:00+08:00"
  },
  "upgradeUrl": "/admin/subscription/upgrade"
}
```

---

#### 6. 用量统计与账单 [中复杂度 | 2.0人月]

**现状**：TenantUsageDTO仅统计画布数/执行数/失败数/DLQ数

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 用量采集 | 每日采集各维度用量数据 |
| 用量聚合 | 按月聚合用量 |
| 账单生成 | 月末自动生成账单 |
| 超量计费 | 超出套餐部分按量计费 |
| 用量看板 | 租户自助查看用量+趋势 |
| 用量预警 | 用量达80%/90%时通知 |

**数据库DDL**：

```sql
CREATE TABLE tenant_usage_daily (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    usage_date DATE NOT NULL,
    mau_count BIGINT NOT NULL DEFAULT 0 COMMENT 'MAU',
    execution_count BIGINT NOT NULL DEFAULT 0 COMMENT '执行次数',
    delivery_count BIGINT NOT NULL DEFAULT 0 COMMENT '触达次数',
    api_call_count BIGINT NOT NULL DEFAULT 0 COMMENT 'API调用次数',
    storage_bytes BIGINT NOT NULL DEFAULT 0 COMMENT '存储空间(字节)',
    canvas_count INT NOT NULL DEFAULT 0 COMMENT '画布数',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_tenant_date (tenant_id, usage_date),
    INDEX idx_date (usage_date)
) COMMENT '租户日用量';

CREATE TABLE tenant_bill (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    bill_period VARCHAR(20) NOT NULL COMMENT '账期 2026-06',
    plan_code VARCHAR(30) NOT NULL COMMENT '套餐',
    base_amount DECIMAL(10,2) NOT NULL COMMENT '基础费用',
    overage_amount DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '超量费用',
    total_amount DECIMAL(10,2) NOT NULL COMMENT '总费用',
    usage_summary JSON NOT NULL COMMENT '用量明细',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PAID/OVERDUE',
    due_date DATE NOT NULL COMMENT '应付日期',
    paid_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_tenant_period (tenant_id, bill_period),
    INDEX idx_status (status)
) COMMENT '租户账单';
```

---

### P2 — 租户自助与运营

---

#### 7. 租户自助注册 [中复杂度 | 1.5人月]

**现状**：仅SuperAdmin通过API创建租户

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 注册页面 | 企业名称+管理员邮箱+密码+套餐选择 |
| 邮箱验证 | 发送验证链接 |
| 自动初始化 | 注册后自动创建租户+管理员账号+默认配置 |
| 试用套餐 | 14天免费试用Professional套餐 |
| 邀请码 | 可选：通过邀请码注册 |

**注册流程**：

```
1. 用户填写注册信息 → 选择套餐
2. 系统创建租户(TENANT_INIT状态)
3. 发送验证邮件
4. 用户点击验证链接 → 租户激活(ACTIVE)
5. 自动创建管理员账号
6. 自动初始化：默认模板+示例画布+系统配置
7. 引导用户完成首次画布创建
```

---

#### 8. 租户设置中心 [中复杂度 | 1.5人月]

**现状**：无租户设置页面

**需补齐**：

| 设置项 | 描述 |
|--------|------|
| 基本信息 | 企业名称/Logo/行业/规模 |
| 安全设置 | 密码策略/IP白名单/2FA |
| 通知设置 | 告警通知渠道/通知人 |
| 渠道配置 | 各渠道API密钥配置 |
| 成员管理 | 邀请/移除成员+角色分配 |
| 数据管理 | 数据导出/数据删除/数据保留策略 |
| 品牌设置 | 退订页面品牌/邮件发件人名称 |

---

#### 9. 运维管理后台 [中复杂度 | 1.5人月]

**现状**：TenantController仅CRUD+用量查询

**需补齐**：

| 子功能 | 描述 |
|--------|------|
| 租户列表 | 搜索/筛选/排序/分页 |
| 租户详情 | 基本信息+用量+账单+成员+画布 |
| 租户操作 | 禁用/激活/删除/套餐变更 |
| 全局看板 | 总租户数/活跃租户/收入/MRR/Churn率 |
| 告警管理 | 租户配额告警/账单逾期告警 |
| 操作审计 | 管理员操作日志 |

---

## 工作量估算

| 优先级 | 功能 | 后端人月 | 前端人月 | 测试人月 | 总计 |
|--------|------|---------|---------|---------|------|
| P0 | MyBatis-Plus租户拦截器 | 1.5 | 0.5 | 1.0 | 3.0 |
| P0 | Reactor上下文传播 | 0.8 | 0.2 | 0.3 | 1.3 |
| P0 | 租户隔离自动化测试 | 1.0 | 0.5 | 1.0 | 2.5 |
| P1 | 套餐定义与订阅 | 1.5 | 1.0 | 0.3 | 2.8 |
| P1 | 资源配额与限流 | 1.5 | 0.5 | 0.5 | 2.5 |
| P1 | 用量统计与账单 | 1.5 | 1.0 | 0.3 | 2.8 |
| P2 | 租户自助注册 | 1.0 | 1.0 | 0.3 | 2.3 |
| P2 | 租户设置中心 | 0.8 | 1.0 | 0.2 | 2.0 |
| P2 | 运维管理后台 | 1.0 | 1.0 | 0.2 | 2.2 |
| | **合计** | **10.6** | **6.7** | **4.1** | **21.4** |

---

## 执行顺序

```
Sprint 1 (P0-隔离): 租户拦截器+Reactor传播 — 4.3人月
  → 产出：所有SQL自动追加tenant_id

Sprint 2 (P0-验证): 隔离自动化测试 — 2.5人月
  → 产出：所有端点隔离验证通过

Sprint 3 (P1-套餐): 套餐定义+订阅管理 — 2.8人月
  → 产出：可定义套餐+租户订阅

Sprint 4 (P1-配额): 资源配额+限流 — 2.5人月
  → 产出：配额检查+超限拦截

Sprint 5 (P1-账单): 用量统计+账单 — 2.8人月
  → 产出：用量看板+月度账单

Sprint 6 (P2-自助): 注册+设置+运维 — 6.5人月
  → 产出：租户自助平台
```

---

## 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 现有数据无tenant_id | 历史数据需迁移 | 默认tenant_id=0，逐步迁移 |
| Reactor上下文丢失 | 异步线程中tenant_id为null | WebFilter+Reactor Context传播+boundedElastic |
| 性能影响 | 所有查询加tenant_id索引 | 确保所有表tenant_id有索引 |
| 联表查询复杂 | JOIN需自动追加tenant_id | MyBatis-Plus TenantLineInnerInterceptor处理 |
| 超级管理员绕过 | 需要跨租户查询能力 | ignoreTable+条件绕过机制 |

---

## 与其他方向的关系

| 方向 | 依赖⑫的原因 |
|------|------------|
| ② 私域中台 | 企微配置按租户隔离 |
| ⑪ 开放平台 | API Key按租户隔离+配额 |
| ⑧ 营销审批 | 审批流程按租户隔离 |
| ⑨ 营销数据中台 | 用量统计依赖配额系统 |
| 所有方向 | 商业化前提=多租户 |
