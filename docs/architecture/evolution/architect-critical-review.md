# 架构师审查报告：8个关键缺失（2026-06-01）

> 现有6份架构文档共5659行，覆盖了服务划分、数据中台、部署等方向。但以下8个问题若不解决，系统会在上线后出事故，或在3个月后被迫重构。

---

## 一、多租户数据隔离：从 ORM 到网关的全链路

### 问题

现有文档仅在 `multi-datasource-isolation.md` 末尾（第八节）提到了 `@TableField(fill = FieldFill.INSERT)` 自动填充和 `TenantLineInnerInterceptor` 全局过滤。这远远不够。

**真实风险**：
- 一个租户的 SQL 如果没有被拦截器拦截，就能读到所有租户的数据
- 缓存 Key 不带 tenant_id，租户 A 会命中租户 B 的缓存
- MQ 消息不带 tenant_id，消费者无法判断归属
- 异步任务（画布执行/人群计算）丢失 tenant 上下文

### 方案

```
请求链路: 
  Gateway(TenantFilter: 从JWT提取tenant_id 写入Header)
    → Service(TenantInterceptor: 读取Header 写入TenantContext)
      → MyBatis(TenantLineInnerInterceptor: 自动追加WHERE tenant_id=?)
      → Redis Key: {tenant_id}:business:key
      → MQ Header: tenant_id
      → @Async线程: TenantContext 自动传播(InheritableThreadLocal→虚拟线程适配)
```

#### 1.1 网关层提取租户

```java
// Gateway Filter
@Component
public class TenantFilter implements GlobalFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String tenantId = extractFromJwt(exchange.getRequest());
        if (tenantId == null) {
            return Mono.error(new UnauthorizedException("缺少租户标识"));
        }
        // 写入 Header 传递给下游
        exchange = exchange.mutate()
            .request(exchange.getRequest().mutate()
                .header("X-Tenant-Id", tenantId)
                .build())
            .build();
        return chain.filter(exchange);
    }
}
```

#### 1.2 服务层拦截器

```java
// 每个服务配置 (可以放在 canvas-common)
@Component
public class TenantInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId == null) {
            throw new BusinessException("X-Tenant-Id header is required");
        }
        TenantContext.set(tenantId);
        return true;
    }
    
    @Override
    public void afterCompletion(...) {
        TenantContext.clear(); // 防止线程池泄漏
    }
}
```

#### 1.3 缓存 Key 规则

```java
// 统一 Key 生成器 (canvas-common)
public class CacheKeyBuilder {
    public static String build(String businessKey, Object... params) {
        String tenantId = TenantContext.get();
        return String.format("{%s}:%s:%s", tenantId, businessKey, 
            String.join(":", Arrays.stream(params).map(Object::toString).toArray(String[]::new)));
    }
}

// 使用:
// {tenant_001}:canvas:execution:12345
// {tenant_001}:cdp:tag:user:67890
```

#### 1.4 MQ 消息传播

```java
// Producer: 自动注入 tenant_id 到消息 Header
@Component
public class TenantAwareRocketMQTemplate {
    public void send(String topic, Object message) {
        MessageBuilder builder = MessageBuilder.withPayload(message)
            .setHeader("TENANT_ID", TenantContext.get())
            .setHeader("TRACE_ID", MDC.get("traceId"));
        rocketMQTemplate.send(topic, builder.build());
    }
}

// Consumer: 自动还原
@Component
public class TenantAwareMessageListener {
    @RocketMQMessageListener(topic = "...", consumerGroup = "...")
    @Override
    public void onMessage(MessageExt message) {
        String tenantId = message.getProperties().get("TENANT_ID");
        TenantContext.set(tenantId);
        // ... 业务逻辑
        TenantContext.clear();
    }
}
```

#### 1.5 异步线程传播

```java
// 虚拟线程下 ThreadLocal 需要显式传播
// 使用 ScopedValue (Java 21 预览) 或自定义 TaskDecorator

@Configuration
public class AsyncConfig {
    @Bean
    public AsyncTaskExecutor asyncTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor();
        executor.setTaskDecorator(task -> () -> {
            String tenantId = TenantContext.get();
            try {
                TenantContext.set(tenantId);
                return task.call();
            } finally {
                TenantContext.clear();
            }
        });
        return executor;
    }
}
```

#### 1.6 安全底线：旁路检查

```java
// 每个服务启动时注册一个 SQL 审计拦截器
// 检测所有 SQL 是否带 tenant_id 条件
@Component
public class TenantSafetyInterceptor implements StatementInspector {
    @Override
    public String inspect(String sql) {
        // 白名单: flyway_history, 跨租户管理操作
        if (isWhitelisted(sql)) return sql;
        // 检查: SELECT/UPDATE/DELETE 是否包含 tenant_id
        if (!sql.toLowerCase().contains("tenant_id")) {
            log.error("SQL missing tenant_id, possible data leak: {}", sql);
            metrics.counter("tenant.safety.missing_tenant_id").increment();
        }
        return sql;
    }
}
```

---

## 二、OneID 身份融合：CDP 最难的问题

### 问题

现有文档提到 `IdentityMergeService` 和 Kafka CDC 消费，但身份融合是 CDP 的核心难题。处理不好，用户画像就是错的。

**场景**：
- 用户匿名浏览 → 分配 `device_id`（Cookie/设备指纹）
- 用户注册 → 绑定 `phone` + `email`
- 用户关注企微 → 关联 `external_userid`
- 用户下单 → 新增 `order_member_id`

**典型坑**：
- 一个手机号被多人使用（家庭共用账号）
- 一个人有多个手机号（换号）
- 设备和人的关联时间窗口（设备被转卖/借用）

### 方案

#### 2.1 确定型 vs 概率型融合

| 类型 | 条件 | 置信度 | 处理 |
|------|------|--------|------|
| 确定型 | 同一用户登录状态下操作 | 100% | 实时融合 |
| 确定型 | 企微 external_userid 对应 union_id | 100% | 实时融合 |
| 概率型 | 同一 device_id + 同 IP → 多个 phone | 80% | 离线 Spark 天级融合 |
| 冲突型 | 一个 phone 被多个 union_id 使用 | 需人工 | 推送到运营后台确认 |

#### 2.2 身份图（Identity Graph）

```
用户 A (user_id=1001):
  ├── phone: 138xxxx1234 ─────────────┐
  ├── email: a@example.com            │
  ├── union_id: oXXXXXXX1             │  同一用户
  ├── device_id: DEVICE_A             │
  └── external_userid: ext_12345 ─────┘

用户 B (user_id=1002):
  ├── phone: 139xxxx5678
  ├── device_id: DEVICE_B
  └── channel_open_id: wx_openid_xxx

→ 合并后: user_id=1001 和 user_id=1002 合并为 OneID=U_0001
```

#### 2.3 数据模型

```sql
-- 身份表 (cdp_db)
CREATE TABLE identity_link (
    id BIGINT PRIMARY KEY,
    one_id VARCHAR(64) NOT NULL COMMENT '全局唯一ID',
    identity_type VARCHAR(32) NOT NULL COMMENT 'phone/email/union_id/device_id/external_userid',
    identity_value VARCHAR(256) NOT NULL COMMENT '身份值',
    user_id BIGINT COMMENT '关联的业务user_id',
    confidence DECIMAL(3,2) DEFAULT 1.00 COMMENT '置信度',
    linked_at DATETIME COMMENT '关联时间',
    unlinked_at DATETIME COMMENT '解关联时间',
    tenant_id BIGINT NOT NULL,
    INDEX idx_identity (tenant_id, identity_type, identity_value),
    INDEX idx_one_id (one_id)
);

-- OneID 到 user_id 的当前映射 (最新视图)
CREATE TABLE one_id_mapping (
    one_id VARCHAR(64) PRIMARY KEY,
    user_id BIGINT NOT NULL,
    merged_from JSON COMMENT '合并来源 [1001, 1002]',
    version INT DEFAULT 1,
    updated_at DATETIME,
    tenant_id BIGINT NOT NULL,
    UNIQUE KEY uk_user (tenant_id, user_id)
);
```

#### 2.4 融合流程

```java
@Service
public class IdentityMergeService {
    
    // 实时融合 (消费 Kafka CDC 事件)
    public void onUserIdentityChanged(UserIdentityEvent event) {
        // 1. 查是否有已关联的 one_id
        String existingOneId = findExistingOneId(event.getIdentityType(), event.getIdentityValue());
        
        if (existingOneId != null) {
            // 2a. 已有 one_id → 追加身份
            addIdentityToGraph(existingOneId, event);
        } else {
            // 2b. 新身份 → 创建新的 one_id
            createOneId(event);
        }
        
        // 3. 检查因新身份产生的间接关联
        // 例如: 原来 phone_a 和 device_a 是 one_id_1
        //       现在 phone_a 又关联了 union_b
        //       → 需要合并 one_id_1 和 one_id_2
        checkGraphConnected();
    }
    
    // 离线批量融合 (Spark 天级)
    // 处理概率型关联 + 解决冲突
    public void batchReconcile() {
        // Spark SQL → Iceberg 全表扫描
        // 规则1: 同一设备+IP → 不同账户 → 标记待确认
        // 规则2: 同一手机号被多 one_id 引用 → 选取最近活跃的
        // 规则3: 超过7天无互动的设备关联 → 降置信度
    }
}
```

#### 2.5 对外接口

```java
// 任何时候只需传入任意身份，返回唯一用户
@GetMapping("/api/v1/users/resolve")
public UserIdentity resolve(
    @RequestParam(required = false) String phone,
    @RequestParam(required = false) String email,
    @RequestParam(required = false) String externalUserId,
    @RequestParam(required = false) String deviceId
) {
    // 查找: 任意身份 → identity_link → one_id → one_id_mapping → user_id → UserProfile
    return identityService.resolve(phone, email, externalUserId, deviceId);
}
```

---

## 三、事件 Schema 治理：服务间通信的第一公民

### 问题

12 个服务通过 RocketMQ 通信。没有事件 Schema 治理，3 个月后会变成：A 服务改了一个字段，B/C/D 服务同时挂掉，谁也找不到根因。

### 方案

#### 3.1 事件注册中心

```
canvas-api/src/main/java/com/canvas/api/event/
├── EventRegistry.java            # 所有事件的注册表
├── canvas/
│   ├── CanvasPublishedEvent      # 画布发布
│   ├── CanvasExecutionStartedEvent  # 画布开始执行
│   ├── CanvasExecutionCompletedEvent # 画布执行完成
│   └── CanvasNodeExecutedEvent   # 节点执行完成
├── cdp/
│   ├── UserIdentityChangedEvent  # 用户身份变更
│   ├── TagChangedEvent           # 标签变更
│   └── AudienceChangedEvent      # 人群变更
├── wecom/
│   ├── WeComContactAddedEvent    # 企微客户新增
│   ├── WeComContactDeletedEvent  # 企微客户删除
│   └── WeComGroupChangedEvent    # 群变更
└── notification/
    ├── MessageSentEvent          # 消息已发送
    └── DeliveryReceiptEvent      # 投递回执
```

#### 3.2 事件规范

```java
// 所有事件必须实现此基类
@Data
public abstract class CanvasEvent<T> {
    private String eventId;        // UUID, 幂等键
    private String eventType;      // 全限定类名, 如 "canvas.node.executed"
    private Long tenantId;         // 租户
    private String traceId;        // 链路追踪
    private Long timestamp;        // 事件发生时间
    private Integer version;       // Schema 版本号 ← 关键: 支持升级
    private T payload;             // 事件内容
    
    // 前向兼容检查
    public boolean isCompatibleWith(int consumerVersion) {
        return this.version >= consumerVersion;
    }
}

// 具体事件
@Data
@EqualsAndHashCode(callSuper = true)
public class CanvasExecutionCompletedEvent extends CanvasEvent<CanvasExecutionCompletedPayload> {
    public CanvasExecutionCompletedEvent() {
        setEventType("canvas.execution.completed");
        setVersion(1);
    }
}

@Data
public class CanvasExecutionCompletedPayload {
    private Long canvasId;
    private Long executionId;
    private String status;         // SUCCESS / FAIL / TIMEOUT
    private Map<String, NodeResultPayload> nodeResults; // 节点 → 结果
    private Long durationMs;
}
```

#### 3.3 版本演化规则

| 变更类型 | 是否兼容 | 处理方式 |
|---------|---------|---------|
| 新增字段（有默认值） | 兼容 | 旧消费者忽略新字段 |
| 新增字段（无默认值） | 不兼容 | 主版本号 +1 |
| 删除字段 | 不兼容 | 主版本号 +1 |
| 修改字段类型 | 不兼容 | 主版本号 +1 |
| 修改字段名 | 不兼容 | 新建字段 + 废弃旧字段（过渡期） |

```java
// 消费者声明可接受的版本范围
@CanvasEventHandler(
    topic = "canvas.execution",
    eventType = CanvasExecutionCompletedEvent.class,
    minVersion = 1,
    maxVersion = 2
)
```

#### 3.4 事件审计日志

```
所有事件必须写入事件审计表 (meta_db.event_audit_log):
  - event_id, event_type, tenant_id, trace_id, timestamp
  - payload_checksum (SHA256, 用于幂等检查)
  - consumer_status (PENDING / PROCESSED / FAILED)

用途:
  1. 幂等: 消费前查 event_id 是否已处理
  2. 排查: trace_id 串联全链路
  3. 对账: 每天对比生产端和消费端的事件数
```

---

## 四、DAG 引擎与 Web 服务分离

### 问题

当前 Canvas-Engine 中 Web 请求处理和 DAG 执行在同一个 JVM。画布执行可能长达 10 分钟，占用大量内存和 CPU。一个租户的批量执行会饿死其他租户的 CRUD 操作。

### 方案

```
┌─────────────────────────────────────────────────────────────┐
│  Canvas-Engine (Web 层, 2-3 replicas)                       │
│  ┌────────────────┐  ┌────────────────┐                     │
│  │ Canvas CRUD    │  │ Execution API  │                     │
│  │ (Controller)   │  │ (触发/查询)    │                     │
│  └────────┬───────┘  └───────┬────────┘                     │
│           │                  │                               │
│           │    ┌─────────────▼─────────────┐                 │
│           │    │ ExecutionManager (内存)    │                 │
│           │    │ - 提交到执行队列           │                 │
│           │    │ - 查询执行状态(Redis)      │                 │
│           │    └─────────────┬─────────────┘                 │
│           │                  │ RocketMQ                       │
└───────────┼──────────────────┼───────────────────────────────┘
            │                  │
            │     ┌────────────▼───────────────────────────┐
            │     │  Canvas-Worker (独立 Pod, 3-5 replicas) │
            │     │  ┌────────────────────────────────┐     │
            │     │  │ DagEngine + Handlers            │     │
            │     │  │ - 从 MQ 消费执行请求            │     │
            │     │  │ - 多租户隔离 (每个租户单独虚拟   │     │
            │     │  │   线程池, 避免互相影响)         │     │
            │     │  │ - 进度上报 Redis               │     │
            │     │  │ - 结果写入 MySQL + MQ 事件      │     │
            │     │  └────────────────────────────────┘     │
            │     │  JVM: -Xmx4g -XX:+UseZGC                │
            │     │  ResourceQuota: cpu=4, mem=8Gi           │
            │     └──────────────────────────────────────────┘
            │
            │ 画布CRUD ←→ MySQL canvas_db
            │ Worker执行 ←→ MySQL canvas_db (执行状态) + Redis (进度)
```

#### 4.1 Worker 多租户隔离

```java
@Component
public class TenantIsolatedExecutor {
    
    // 每个租户最多同时执行 N 个画布
    // 大租户 (付费) → 10 并发
    // 小租户 (试用) → 2 并发
    private final Map<Long, Semaphore> tenantLimits = new ConcurrentHashMap<>();
    
    public void execute(Long tenantId, Long canvasId, Runnable task) {
        Semaphore limit = tenantLimits.computeIfAbsent(tenantId, 
            id -> new Semaphore(TenantContext.getQuota("canvas.concurrency", 2)));
        
        if (!limit.tryAcquire(30, TimeUnit.SECONDS)) {
            throw new TenantQuotaExceededException("画布并发达到上限");
        }
        
        try {
            task.run();
        } finally {
            limit.release();
        }
    }
}
```

#### 4.2 执行状态外置到 Redis

```java
// Worker 执行过程中持续上报心跳+进度
// Web 层查询时直接从 Redis 读取，不需要 RPC Worker

// Redis Key: {tenant_id}:execution:{execution_id}
//  - status: RUNNING
//  - progress: 45% (15/33 nodes completed)
//  - currentNode: "wait_for_payment"
//  - lastHeartbeat: 1717200000000
//  - TTL: 1h (执行完成后保留1小时供查询，之后靠 MySQL 持久化)
```

---

## 五、分布式链路追踪：调试的基础设施

### 问题

文档中提到 OpenTelemetry + Tempo，但没有设计 trace 传播的具体策略。12 个服务，一个画布执行请求可能穿越 5-6 个服务。没有全链路 trace，排查一次故障 = 逐个服务翻日志。

### 方案

#### 5.1 TraceId 全链路传播

```
每个请求进入 Gateway → 生成或继承 traceId → 写入 HTTP Header (X-Trace-Id)
  → 每个服务 Interceptor 提取 traceId → 写入 MDC
    → 日志自动带 [traceId]
    → Feign 调用自动传播 (RequestInterceptor)
    → MQ 消息自动传播 (Message Header)
    → @Async 线程自动传播 (TaskDecorator)
    → DB 操作可选写入 (MySQL general_log 或 comment)
```

#### 5.2 关键 Span 设计

```
Request: POST /api/v1/canvas/{id}/execute
├── Gateway: authenticate + route                    [span: 5ms]
├── Canvas-Engine: ExecutionController.execute()     [span: 10ms]
│   ├── Canvas-Engine: loadCanvas()                  [span: 15ms] DB
│   ├── Canvas-Engine: sendToWorker()                [span: 5ms]  MQ
│   └── Canvas-Engine: return executionId            [span: 1ms]
│
├── Canvas-Worker: consumeExecutionRequest()          [span: 50ms]
│   ├── Canvas-Worker: initContext()                  [span: 20ms] DB
│   ├── Canvas-Worker: NodeHandler[SendMessage]       [span: 100ms]
│   │   ├── Notification-Service: send()              [span: 80ms]
│   │   │   ├── Notification-Service: renderTemplate()[span: 10ms]
│   │   │   └── WeCom-Service: sendMessage()          [span: 50ms] HTTP→企微API
│   │   └── Canvas-Worker: saveNodeResult()           [span: 10ms] DB
│   └── Canvas-Worker: finalize()                     [span: 10ms]
│
└── Total: ~270ms
```

#### 5.3 业务标识：在 Trace 上挂载业务属性

```java
// 除了 technical span，还需要业务属性便于搜索
Span.current()
    .setAttribute("tenant.id", tenantId)
    .setAttribute("canvas.id", canvasId)
    .setAttribute("execution.id", executionId)
    .setAttribute("user.id", userId)
    .setAttribute("node.type", nodeType);

// 这样在 Grafana Tempo 中可以查询:
// { tenant.id = 1001 && canvas.id = 42 } → 这个画布的所有请求
// { node.type = "WaitForPayment" && duration > 60s } → 所有慢支付节点
```

---

## 六、租户级配额与限流

### 问题

现有文档只提到了 API Gateway 的全局限流（`redis-rate-limiter.replenishRate: 100`）。没有租户级别的配额控制。一个试用租户可以发起和付费租户同样多的画布执行。

### 方案

#### 6.1 配额模型

```sql
-- meta_db
CREATE TABLE tenant_quota (
    id BIGINT PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    quota_key VARCHAR(64) NOT NULL COMMENT '配额项',
    quota_limit BIGINT NOT NULL COMMENT '限额',
    quota_window VARCHAR(16) NOT NULL COMMENT '时间窗口: minute/hour/day/month',
    used BIGINT DEFAULT 0 COMMENT '当前使用量',
    reset_at DATETIME COMMENT '下次重置时间',
    version INT DEFAULT 1 COMMENT '乐观锁',
    UNIQUE KEY uk_quota (tenant_id, quota_key, quota_window)
);

-- 默认配额项
INSERT INTO tenant_quota (tenant_id, quota_key, quota_limit, quota_window) VALUES
(1, 'canvas.execution.count',      1000, 'day'),     -- 日执行次数
(1, 'canvas.execution.concurrent',  5,    'minute'),  -- 并发执行数
(1, 'audience.compute.count',      100,  'day'),     -- 日人群计算次数
(1, 'audience.max_size',           100000, 'forever'), -- 最大人群规模
(1, 'message.send.count',          10000, 'day'),    -- 日消息发送量
(1, 'wecom.api.call',              2000,  'minute'), -- 企微API调用
(1, 'data.export.count',           50,    'day'),    -- 日数据导出次数
(1, 'data.export.max_rows',        500000, 'forever'); -- 单次导出上限
```

#### 6.2 配额拦截器

```java
@Component
public class TenantQuotaInterceptor implements HandlerInterceptor {
    
    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        Long tenantId = TenantContext.get();
        
        // 根据 API 路径匹配配额项
        String quotaKey = matchQuotaKey(request.getRequestURI());
        if (quotaKey == null) return true; // 不限流的 API
        
        // 检查 + 扣减 (Redis Lua 脚本保证原子性)
        boolean allowed = quotaService.checkAndIncrement(tenantId, quotaKey);
        
        if (!allowed) {
            response.setStatus(429); // Too Many Requests
            response.getWriter().write("{\"error\":\"配额已用完\",\"quota\":\"" + quotaKey + "\"}");
            return false;
        }
        return true;
    }
}
```

#### 6.3 当前可快速落地的限制

```
无需新建 quota 表，直接在 Canvas-Engine 的已有配置上做:

canvas:
  tenant:
    default:                 # 默认配额（试用租户）
      max-canvas: 10          # 最多创建10个画布
      max-concurrent-exec: 2   # 最大并发执行2个
      max-nodes-per-canvas: 20 # 每个画布最多20个节点
    quotas:
      1001:                   # 租户1001（付费）
        max-canvas: 100
        max-concurrent-exec: 10
        max-nodes-per-canvas: 100
      1002:                   # 租户1002（试用，已被限制）
        max-canvas: 5
        max-concurrent-exec: 1
        max-nodes-per-canvas: 10
```

---

## 七、服务间熔断与降级

### 问题

12 个服务互相调用：Canvas → Notification → WeCom → 企微 API。如果企微 API 超时，调用链上的每个服务都会堆积线程，雪崩。

### 方案

#### 7.1 熔断策略

```java
// 使用 Resilience4j (轻量，不需要引入 Sentinel 的 Dashboard 复杂度)

// Canvas-Engine → Notification-Service
resilience4j:
  circuitbreaker:
    configs:
      default:
        slidingWindowType: COUNT_BASED
        slidingWindowSize: 10
        failureRateThreshold: 50     # 50% 失败率熔断
        waitDurationInOpenState: 30s # 熔断30秒后半开探测
        permittedNumberOfCallsInHalfOpenState: 3
    instances:
      notification-service:
        baseConfig: default
      cdp-service:
        baseConfig: default
      wecom-service:
        baseConfig: default
        slowCallDurationThreshold: 5s  # 企微API较慢
```

#### 7.2 降级策略

```java
@Service
public class NotificationClient {
    
    @CircuitBreaker(name = "notification-service", fallbackMethod = "sendFallback")
    public MessageVO sendMessage(SendMessageRequest request) {
        // Feign 调用 notification-service
        return notificationFeignClient.send(request);
    }
    
    // 熔断时降级: 消息写入本地表 + 定时重试
    public MessageVO sendFallback(SendMessageRequest request, Exception e) {
        // 写入 offline_message 表
        offlineMessageRepository.save(toOfflineMessage(request));
        
        // 返回 PENDING 状态
        return MessageVO.builder()
            .status("PENDING")
            .message("消息已受理，稍后投递")
            .retryAt(LocalDateTime.now().plusMinutes(1))
            .build();
    }
}
```

#### 7.3 降级层次

```
Layer 1: 企微 API 不可用 → WeCom-Service 返回 fallback，消息入本地队列
Layer 2: WeCom-Service 不可用 → Notification-Service 选择 SMS/Email 替代通道
Layer 3: Notification-Service 不可用 → Canvas-Engine 写 offline_message 表
Layer 4: CDP-Service 人群计算不可用 → Canvas-Engine 使用本地缓存的 Bitmap
Layer 5: DataHub/ClickHouse 不可用 → Analytics-Engine 返回 "数据查询暂时不可用"
```

---

## 八、从单体到服务的迁移策略

### 问题

文档描述的是"最终状态"。但 65 个 DO、30 个 Controller、49 个 Handler 都在一个仓库里，不可能在一个周末切完。

### 方案：绞杀者模式（Strangler Fig）

```
Week 1-4: 原地模块化 (不改部署)
  在同一个 Spring Boot 应用中按 Maven 模块拆分包结构:
    com.canvas.engine.controller.* (原 Controller)
    com.canvas.cdp.controller.*   (新包路径)
    com.canvas.wecom.controller.* (新包路径)
  
  同时:
  - API 路径保持 /api/v1/xxx 不变
  - 数据库保持单库 (暂不拆)
  - 只是包结构整洁 + 依赖方向收敛

Week 5-8: 数据拆分 + 读切换
  - 创建 cdp_db + meta_db + 对应用户
  - 双写: 所有写操作同时写旧表(canvas_db)和新表(cdp_db)
  - 读操作仍然读旧表
  - 数据校验脚本: 每天对比双方数据直到一致

Week 9-12: 读切换 + 独立部署
  - 读操作切换到新表
  - CDP-Service 独立部署（新 JVM）
  - API Gateway 路由: /api/v1/tags/** → CDP-Service
  - 观察一周 → 停止双写 → 删除旧表

Week 13-16: WeCom-Service 独立部署
  - 同理: 双写 → 数据校验 → 读切换 → 独立部署

Week 17-20: Notification-Service 独立部署
  - 当前 delivery 包是 stub，可直接新建 → 切流

之后: 逐个拆分剩余服务
```

### 不变的原则

```
1. 每一步都可回滚
2. API 路径不改变（前端零改动）
3. 双写期间性能不退化（异步双写，失败不阻塞主流程）
4. 先在 staging 跑满 1 周，再生产切换
```

---

## 总结：优先级排序

| # | 问题 | 严重度 | 不解决的后果 | 建议解决阶段 |
|---|------|--------|-------------|-------------|
| 1 | 多租户全链路隔离 | CRITICAL | 数据泄露，SaaS 不可用 | Phase 1 Week 1-2 |
| 2 | OneID 身份融合 | CRITICAL | 用户画像错误，CDP 核心价值丧失 | Phase 2 启动即做 |
| 3 | 事件 Schema 治理 | HIGH | 服务间通信混乱，排查困难 | Phase 2 Week 1 |
| 4 | DAG 引擎分离 | HIGH | 执行和 CRUD 互相影响 | Phase 2 后 |
| 5 | 分布式链路追踪 | HIGH | 12个服务无全链路trace=盲调 | Phase 1 Week 7-8 |
| 6 | 租户级配额限流 | MEDIUM | 试用租户拖垮付费租户 | Phase 1 Week 7-8 |
| 7 | 服务间熔断降级 | MEDIUM | 雪崩 | Phase 2 + |
| 8 | 绞杀者迁移策略 | — | 一次性切换必然失败 | Phase 1 Week 1 |

---

## 相关文档

- [服务划分与新应用搭建方案](service-architecture-design.md)
- [架构演进路线图](architecture-evolution-roadmap.md)
- [目标架构总览](target-architecture-overview.md)
- [数据平台架构设计](data-platform-architecture.md)
