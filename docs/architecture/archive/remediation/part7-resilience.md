# 架构整改方案 — resilience
> 详见 [README.md](README.md) 获取完整索引


---

### 问题二十八：可观测性与韧性缺口（28项）

#### 28.1 健康检查 — 仅浅层

无自定义 `HealthIndicator`。Actuator默认health不检查MySQL/Redis/RocketMQ/外部服务连通性。

#### 28.2 优雅关闭 — 无协调排空（4项）

| 组件 | 关闭行为 | 缺口 |
|------|---------|------|
| CanvasDisruptorService | `disruptor.shutdown()` | 非阻塞，不等事件处理完成 |
| TraceWriteBuffer | `shutdownFlush()` | OK |
| CanvasSchedulerService | `cancelAll()` | OK |
| 虚拟线程 | 无 | 4处spawn无tracking无shutdown |

**缺失**: Spring `server.shutdown=graceful` 未配置。无InFlight执行排空。GroovyHandler的VirtualThreadPerTaskExecutor无@PreDestroy。

#### 28.3 备份恢复 — 无策略

- 无MySQL自动备份/ PITR
- 无Redis持久化配置（AOF/RDB）
- 执行上下文仅存Redis（TTL过期即丢失），无MySQL持久化备份
- Flyway无undo脚本

#### 28.4 容量规划 — 部分限制未强制（6项）

| 参数 | 配置值 | 强制？ |
|------|--------|--------|
| max-concurrency | 3000 | 是（Redis ZSET） |
| global-timeout | 600s | 是（TTL） |
| MAX_NODE_DEPTH | 200 | 是（代码） |
| 每画布最大节点数 | — | **否** |
| 每画布最大触发器数 | — | **否** |
| 执行历史保留策略 | — | **否** |

无界集合增长：`InFlightExecutionRegistry.localRegistry`/`localExecutionLanes`无大小限制无LRU淘汰。`AudienceSchedulerService.refreshAll()` 和 `CanvasVersionCleanupJob` 全表加载。

#### 28.5 混沌韧性 — 外部服务无降级（6项）

| 场景 | 当前行为 | 应有行为 |
|------|---------|---------|
| Tagger宕机 | 执行立即失败 | 降级/缓存兜底 |
| Coupon服务宕机 | 执行立即失败 | 熔断+降级通知 |
| Redis宕机 | InFlight返回"rejected" | 本地模式降级 |
| MySQL宕机 | 无显式处理 | 熔断+告警 |
| 外部HTTP超时 | 无熔断器 | Resilience4j熔断 |
| 全局连接池共享 | 无隔离 | 按服务Bulkhead隔离 |

#### 28.6 性能瓶颈（7项）

| 瓶颈 | 位置 | 影响 |
|------|------|------|
| .block()在Reactive链中 | CanvasSchedulerService:424,451 / AudienceBatchComputeService:250 | 阻塞调度线程 |
| 全表加载selectList(null) | 5处（audience/canvas/user/tenant/context_field） | O(n)内存+DB |
| synchronized热路径 | CanvasSchedulerService 7处 | 高负载竞争 |
| 无Redis Pipeline批量操作 | TriggerPreCheckService:298 | N次RTT |
| VirtualThreadPerTaskExecutor无关闭 | GroovyHandler:58 | 资源泄漏 |
| InFlight本地Map无界 | InFlightExecutionRegistry | 僵尸执行内存泄漏 |
| 本地Map vs Redis不一致 | InFlightExecutionRegistry:207-210 | 多实例低估活跃数 |

#### 28.7 Reactor线程模型违规 — 精确清单（5类，共80+处）

**A. .block() 未包裹 Schedulers.boundedElastic()（5处 CRITICAL）**

| 文件 | 行号 | 代码 | 影响 |
|------|------|------|------|
| CanvasSchedulerService | 424 | `taggerClient.get()...block()` | 阻塞调度线程 |
| CanvasSchedulerService | 451 | `apiCallClient.post()...block()` | 阻塞调度线程 |
| AudienceBatchComputeService | 250 | `client.get()...block()` | 阻塞人群计算线程 |
| AudienceEvaluationContextFetcher | 51 | `client.post()...block()` | 阻塞上下文获取 |
| TagImportSourceService | 143 | `spec.retrieve()...block()` | 阻塞导入线程 |

**B. .subscribe() 无调度器（4处 HIGH）**

| 文件 | 行号 | 代码 | 影响 |
|------|------|------|------|
| KillSwitchSubscriber | 73 | `.doOnError(...).subscribe()` | 错误处理在调用者线程 |
| TransferJourneyHandler | 74 | `executionService.trigger(...).subscribe()` | 旅程触发在调用者线程 |
| CanvasTriggerHandler | 121 | `dagEngine.execute(...).subscribe()` | DAG执行在调用者线程 |
| CanvasExecutionRequestExecutor | 287 | `Flux.interval(...).subscribe()` | 定时轮询无调度 |

**C. Thread.sleep() 阻塞（3处 HIGH）**

| 文件 | 行号 | 代码 | 影响 |
|------|------|------|------|
| TriggerRouteService | 190 | `Thread.sleep(50)` 锁重试 | 浪费线程 |
| CanvasRouteInitializer | 76 | `Thread.sleep(2000)` 初始化 | 启动延迟 |
| AudienceComputeTaskRunner | 216 | `Thread.sleep(lockRetryDelay)` | 阻塞计算线程 |

**D. synchronized 虚拟线程钉住风险（8处 MEDIUM）**

| 文件 | 行号 | 代码 | 影响 |
|------|------|------|------|
| CanvasSchedulerService | 171,198,222,234,249,257,269 | `synchronized(lifecycleLock)` ×7 | 虚拟线程钉住 |
| CanvasExecutionReplayRateLimiter | 160 | `synchronized tryAcquire` | 限流热路径竞争 |

**E. Mono.fromCallable() 无 subscribeOn（50+处 MEDIUM）**

29个Controller中**零个**使用 `@Valid/@Validated`，所有 `Mono.fromCallable()` 包装的阻塞DB调用中，部分缺少 `subscribeOn(Schedulers.boundedElastic())`。高频问题Controller：

| Controller | 端点数 | fromCallable数 | 缺subscribeOn |
|-----------|--------|---------------|--------------|
| MetaController | 22 | 21 | 部分缺失 |
| AudienceController | 11 | 10 | 部分缺失 |
| TagDefinitionController | 9 | ~8 | 部分缺失 |
| AbExperimentController | 9 | ~8 | 部分缺失 |
| CanvasController | 19 | ~15 | 部分缺失 |

**F. 阻塞Redis/MQ调用未调度（4处 HIGH）**

| 文件 | 行号 | 代码 | 影响 |
|------|------|------|------|
| ApiCallHandler | 275 | `redis.opsForValue().increment(key)` | 阻塞Reactor线程 |
| CanvasExecutionReplayRateLimiter | 198 | `redis.opsForValue().increment(key, cost)` | 阻塞限流路径 |
| RocketMqCacheInvalidationPublisher | 35 | `rocketMQTemplate.syncSend(...)` | 阻塞缓存失效 |
| AuthController | 96 | `redis.opsForValue().increment(failKey)` | 阻塞登录路径 |

#### 28.7实施方案

**Step 1: .block() → 响应式链（28.7.A）**

```java
// Before (CRITICAL)
Map result = taggerClient.get().uri("/tagger/resolve")
    .retrieve().bodyToMono(Map.class).block();

// After
return taggerClient.get().uri("/tagger/resolve")
    .retrieve().bodyToMono(Map.class)
    .flatMap(result -> /* 继续业务逻辑 */);
```

**Step 2: .subscribe() → 指定调度器（28.7.B）**

```java
// Before
dagEngine.execute(canvasId, userId, triggerType).subscribe();

// After
dagEngine.execute(canvasId, userId, triggerType)
    .subscribeOn(Schedulers.boundedElastic())
    .subscribe();
```

**Step 3: Thread.sleep() → Mono.delay()（28.7.C）**

```java
// Before
Thread.sleep(50);

// After
return Mono.delay(Duration.ofMillis(50));
```

**Step 4: synchronized → ReentrantLock（28.7.D）**

```java
// Before
synchronized (lifecycleLock) { ... }

// After
private final ReentrantLock lifecycleLock = new ReentrantLock();
lifecycleLock.lock();
try { ... } finally { lifecycleLock.unlock(); }
```

**Step 5: Controller统一subscribeOn（28.7.E）**

创建AOP切面自动为所有Controller的 `Mono.fromCallable()` 添加 `subscribeOn(Schedulers.boundedElastic())`：

```java
@Aspect
@Component
public class ReactiveControllerAspect {
    @Around("execution(reactor.core.publisher.Mono org.chovy.canvas.web..*(..))")
    public Object ensureScheduler(ProceedingJoinPoint pjp) {
        Mono<?> result = (Mono<?>) pjp.proceed();
        return result.subscribeOn(Schedulers.boundedElastic());
    }
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 5处.block()→响应式链 | 4h |
| 2 | 4处.subscribe()加调度器 | 2h |
| 3 | 3处Thread.sleep()→Mono.delay() | 2h |
| 4 | 8处synchronized→ReentrantLock | 3h |
| 5 | Controller AOP切面统一subscribeOn | 4h |
| 6 | 4处阻塞Redis/MQ加subscribeOn | 2h |

**总工时**: ~17h

---

#### 28.8 API输入验证零覆盖 — 29个Controller零@Valid

**现状**: 全部29个Controller、170+个端点，**零个**使用 `@Valid/@Validated` 注解。任何请求体可传入任意字段。

**风险**:
- 恶意输入可注入超长字符串、负数、空必填字段
- 批量接口无分页限制，可DoS
- 缺少@Size/@Min/@Max导致DB层才报错（500而非400）

**实施方案**:

```java
// 1. 创建通用DTO基类
public abstract class BaseRequest {
    @NotNull(message = "tenantId不能为空")
    private Long tenantId;
}

// 2. 为每个Controller端点创建Request DTO
public class CreateCanvasRequest {
    @NotBlank(message = "画布名称不能为空")
    @Size(max = 100, message = "画布名称不能超过100字符")
    private String name;
    @NotNull(message = "画布类型不能为空")
    private Integer type;
    @NotBlank(message = "graphJson不能为空")
    @Size(max = 65535, message = "graphJson不能超过64KB")
    private String graphJson;
}

// 3. Controller添加@Valid
@PostMapping
public Mono<R<CanvasDTO>> create(@Valid @RequestBody CreateCanvasRequest req) { ... }
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 创建BaseRequest+通用验证注解 | 2h |
| 2 | CanvasController 19端点DTO+@Valid | 6h |
| 3 | MetaController 22端点DTO+@Valid | 6h |
| 4 | 其余27个Controller DTO+@Valid | 12h |
| 5 | GlobalExceptionHandler添加MethodArgumentNotValidException处理 | 1h |

**总工时**: ~27h

---

#### 28.9 硬删除vs软删除不一致 — 13处deleteById

**现状**: 13处直接 `deleteById()` 硬删除，与CdpTagService的软删除（`@TableLogic`）不一致。审计追踪断裂。

| 文件 | 行号 | 删除对象 |
|------|------|---------|
| EventDefinitionController | 116 | 事件定义 |
| DlqController | 123 | 死信消息 |
| DataSourceConfigController | 128 | 数据源配置 |
| MqDefinitionController | 75 | MQ定义 |
| AbExperimentController | 97 | AB实验 |
| ApiDefinitionController | 120 | API定义 |
| AudienceBatchComputeService | 157-158 | 人群定义+统计 |
| ManualApprovalHandler | 115 | 审批记录 |
| IdentityTypeService | 73 | 身份类型 |
| TagDefinitionService | 80,142 | 标签定义+标签值 |
| TagImportSourceService | 86 | 标签导入源 |

**实施方案**:

```java
// 1. 所有DO添加 @TableLogic
@TableLogic
@TableField("is_deleted")
private Integer isDeleted;

// 2. 替换 deleteById → 逻辑删除
// Before: mapper.deleteById(id);
// After:  mapper.selectById(id).setIsDeleted(1); mapper.updateById(entity);
// 或直接用 MyBatis-Plus 逻辑删除（自动转换 deleteById → UPDATE SET is_deleted=1）
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 全部DO添加@TableLogic字段 | 4h |
| 2 | Flyway迁移：添加is_deleted列 | 3h |
| 3 | 替换13处deleteById | 2h |
| 4 | 验证查询自动过滤is_deleted=1 | 2h |

**总工时**: ~11h

---

#### 28.10 @Transactional在Reactive上下文中的陷阱

**现状**: 17处 `@Transactional` 分布在CanvasTransactionService(5)、CanvasOpsService(6)、CanvasService(4)、CdpTagService(1)、TagImportService(1)。其中CanvasTransactionService的注释已承认问题：

> "混在 @Transactional 内，DB 回滚时 Redis 路由状态已改变，造成不可逆不一致"

**核心问题**:
1. Spring WebFlux下 `@Transactional` 不生效（Reactor线程与事务线程不同）
2. DB事务+Redis操作混合 → DB回滚时Redis已变更，不可逆不一致
3. 虚拟线程+`@Transactional` → 事务传播可能断裂

**实施方案**:

```java
// 方案A（推荐，配合Spring MVC回退）：
// 回退Spring MVC后@Transactional自然生效

// 方案B（保持WebFlux）：
// 使用 TransactionalOperator 替代 @Transactional
@Autowired private TransactionalOperator txOperator;

public Mono<CanvasDO> publishWithTx(CanvasDO canvas) {
    return canvasMapper.updateById(canvas)
        .then(redisOps.set(key, value))
        .as(txOperator::transactional)  // 只包裹DB操作
        .doOnSuccess(v -> publishRedisEvent());  // Redis在事务外
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 审计17处@Transactional的调用链 | 3h |
| 2 | DB+Redis混合操作拆分（Redis移到事务外） | 4h |
| 3 | 改用TransactionalOperator或回退MVC | 6h |

**总工时**: ~13h

---

#### 28.11 前端状态与数据流缺陷（62处any + 91处useEffect + WebSocket）

**A. TypeScript any泛滥（62处）**

前端代码中62处使用 `any` 类型，完全绕过类型检查。关键位置：
- `api.ts` — 响应类型全部any
- `canvas.ts` — 节点配置类型any
- `AuthContext.tsx` — 用户对象any

**B. useEffect依赖问题（91处）**

91处 `useEffect` 中大量缺少依赖数组或依赖不完整，导致：
- Stale closure：回调引用旧状态
- 无限重渲染：依赖包含每次新建的对象
- 内存泄漏：缺少cleanup函数

**C. WebSocket重连（NotificationContext.tsx）**

已有指数退避重连，但缺少：
- 最大重连次数限制
- 连接状态全局暴露（组件无法感知WS状态）
- 心跳检测（依赖服务端超时断开）

**D. localStorage同步（AuthContext.tsx）**

Token和用户信息存localStorage，但：
- 无过期检查（JWT过期后localStorage仍有效）
- 无跨标签页同步（`storage` 事件未监听）
- console.log输出token前缀

**实施方案**:

```typescript
// 1. 消灭any — 创建API响应类型
interface ApiResponse<T> { code: number; data: T; message: string; }
interface CanvasDTO { id: number; name: string; status: CanvasStatus; ... }

// 2. useEffect lint — 启用react-hooks/exhaustive-deps规则
// .eslintrc: "react-hooks/exhaustive-deps": "error"

// 3. WebSocket增强
const MAX_RECONNECT_ATTEMPTS = 10;
const wsState$ = new BehaviorSubject<WsState>('disconnected');

// 4. localStorage增强
window.addEventListener('storage', (e) => {
  if (e.key === 'canvas_token') refreshAuthState();
});
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | API层类型化（消灭62处any） | 8h |
| 2 | useEffect依赖修复+ESLint规则 | 6h |
| 3 | WebSocket增强（最大重连+状态暴露+心跳） | 4h |
| 4 | localStorage跨标签页同步+过期检查 | 3h |

**总工时**: ~21h

---

#### 原有实施方案

**Step 1: 深层健康检查（28.1）**

```java
@Component
public class RedisHealthIndicator implements HealthIndicator {
    public Health health() {
        String pong = redis.getConnectionFactory().getConnection().ping();
        return "PONG".equals(pong) ? Health.up().build() : Health.down().build();
    }
}
// 同理: MySQLHealthIndicator, RocketMQHealthIndicator
```

**Step 2: 优雅关闭（28.2）**

```yaml
server:
  shutdown: graceful
spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

**Step 3: 外部服务韧性（28.5）**

```java
@Bean
public CircuitBreaker taggerCircuitBreaker {
    return CircuitBreaker.of("tagger", CircuitBreakerConfig.custom()
        .failureRateThreshold(50)
        .waitDurationInOpenState(Duration.ofSeconds(30))
        .slidingWindowSize(10)
        .build());
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 自定义HealthIndicator（MySQL/Redis/MQ/外部服务） | 4h |
| 2 | Spring优雅关闭+VirtualThread tracking | 6h |
| 3 | 外部服务Resilience4j熔断器 | 8h |
| 4 | 全表加载改分页 | 4h |
| 5 | InFlight本地Map清理/限容 | 4h |
| 6 | 容量限制添加（节点/触发器/保留期） | 4h |
| 7 | 备份恢复策略文档 | 4h |
| 8 | .block()→reactive转换 | 6h |

**总工时**: ~40h

---


---
> 问题总览、优先级排序和路线图见 [README.md](README.md)
