# WebFlux → Spring MVC + Virtual Threads 迁移方案 (2026-05-31)

> **定位**: 解决WebFlux+MyBatis根本矛盾，消除30+.block()阻塞Netty事件循环

---

## 一、问题分析

### 1.1 当前矛盾

| 问题 | 代码位置 | 影响 |
|------|---------|------|
| 5处.block()未包裹boundedElastic | CanvasSchedulerService:424,451 / AudienceBatchComputeService:250 / AudienceEvaluationContextFetcher:51 / TagImportSourceService:143 / AuthController:96 | 阻塞Netty事件循环，并发能力丧失 |
| 4处.subscribe()无调度器 | DagEngine / JourneyTriggerService / CanvasSchedulerService / AudienceService | 在调用者线程执行，可能阻塞 |
| 3处Thread.sleep() | DagEngine / CircuitBreaker / WaitHandler | 阻塞Reactor线程 |
| 8处synchronized | CircuitBreaker / ExecutionContext / MarketingPolicyService | 虚拟线程pinning风险 |
| 50+处Mono.fromCallable()无subscribeOn | 全局 | 在Netty线程执行阻塞DB操作 |
| 17处@Transactional在Reactive上下文 | CanvasTransactionService(5) / CanvasOpsService(6) / CanvasService(4) / CdpTagService(1) / TagImportService(1) | 事务不生效 |

### 1.2 为什么选Spring MVC + Virtual Threads

| 方案 | 优点 | 缺点 | 推荐 |
|------|------|------|------|
| 继续WebFlux | 响应式全链路 | 需重写所有DB操作为Reactive，MyBatis-Plus不支持 | 不推荐 |
| WebFlux + boundedElastic | 最小改动 | 仍需包裹所有阻塞调用，易遗漏 | 临时方案 |
| **Spring MVC + Virtual Threads** | **Java 21原生支持，MyBatis-Plus天然兼容，代码简洁** | **需移除Reactor依赖** | **推荐** |

---

## 二、迁移策略

### 2.1 总体原则

1. **渐进式迁移**：不一次性重写，按模块逐步切换
2. **保持API兼容**：前端无需改动
3. **保留DAG引擎Reactor**：DagEngine内部仍用Mono/Flux，但Controller层改为同步
4. **虚拟线程配置**：Tomcat/NIO + 虚拟线程，每个请求一个虚拟线程

### 2.2 迶回路线

```
当前: WebFlux (Netty) + MyBatis-Plus (阻塞)
  │
  │ Step 1: Controller层改为同步返回
  │ Step 2: 移除WebFlux依赖，引入Spring MVC
  │ Step 3: 配置虚拟线程
  │ Step 4: DAG引擎内部Mono改为同步调用
  │
  │ (可选) Step 5: DAG引擎内部也改为同步
  │
目标: Spring MVC (Tomcat) + Virtual Threads + MyBatis-Plus
```

---

## 三、详细迁移步骤

### Step 1: Controller层改为同步返回（2周）

#### 1.1 改造模式

```java
// 改造前 (WebFlux)
@GetMapping("/canvas/{id}")
public Mono<ResponseEntity<CanvasVO>> getCanvas(@PathVariable Long id) {
    return canvasService.getCanvas(id)
        .map(ResponseEntity::ok)
        .defaultIfEmpty(ResponseEntity.notFound().build());
}

// 改造后 (Spring MVC + Virtual Threads)
@GetMapping("/canvas/{id}")
public ResponseEntity<CanvasVO> getCanvas(@PathVariable Long id) {
    CanvasVO canvas = canvasService.getCanvasSync(id);
    return canvas != null ? ResponseEntity.ok(canvas) : ResponseEntity.notFound().build();
}
```

#### 1.2 Service层适配

```java
// 改造前
public Mono<CanvasVO> getCanvas(Long id) {
    return Mono.fromCallable(() -> canvasMapper.selectById(id))
        .subscribeOn(Schedulers.boundedElastic())
        .map(this::toVO);
}

// 改造后 (虚拟线程下直接调用)
public CanvasVO getCanvasSync(Long id) {
    CanvasDO canvas = canvasMapper.selectById(id);
    return this.toVO(canvas);
}
```

#### 1.3 DAG引擎调用适配

```java
// 改造前 (Controller调用DagEngine)
public Mono<ResponseEntity<ExecutionVO>> executeCanvas(@RequestBody ExecutionRequest request) {
    return dagEngine.execute(request)
        .map(ResponseEntity::ok);
}

// 改造后 (DagEngine内部仍用Mono，Controller同步等待)
public ResponseEntity<ExecutionVO> executeCanvas(@RequestBody ExecutionRequest request) {
    ExecutionVO result = dagEngine.execute(request).block();
    return ResponseEntity.ok(result);
}
// 注意：在虚拟线程下，.block()不会阻塞平台线程，而是挂起虚拟线程
```

### Step 2: 移除WebFlux依赖，引入Spring MVC（1周）

#### 2.1 pom.xml变更

```xml
<!-- 移除 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webflux</artifactId>
</dependency>

<!-- 引入 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

#### 2.2 application.yml变更

```yaml
# 移除 Netty 配置
# server.netty... 全部删除

# 新增 Tomcat + 虚拟线程配置
server:
  port: 8080
  tomcat:
    threads:
      max: 200  # 平台线程上限（虚拟线程不受此限制）
    max-connections: 10000
    accept-count: 100

spring:
  threads:
    virtual:
      enabled: true  # 启用虚拟线程
```

#### 2.3 移除Reactor相关配置

```java
// 移除 Netty 相关配置类
// - NettyServerCustomizer
// - WebClientCustomizer (改为RestClient)

// 移除 Schedulers.boundedElastic() 包裹
// 所有 Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())
// 改为直接调用（虚拟线程下自动调度）
```

### Step 3: 配置虚拟线程（1天）

#### 3.1 启用虚拟线程

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

#### 3.2 自定义虚拟线程执行器（可选）

```java
@Configuration
public class VirtualThreadConfig {

    @Bean
    public TomcatProtocolHandlerCustomizer<?> virtualThreadCustomizer() {
        return protocolHandler -> {
            protocolHandler.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        };
    }

    // 异步任务也使用虚拟线程
    @Bean
    public TaskExecutor asyncTaskExecutor() {
        return new SimpleAsyncTaskExecutor();  // Spring自动使用虚拟线程
    }
}
```

### Step 4: DAG引擎内部Mono改为同步调用（2周）

#### 4.1 DagEngine核心改造

```java
// 改造前
public Mono<ExecutionResult> execute(ExecutionRequest request) {
    return Mono.fromCallable(() -> initContext(request))
        .flatMap(ctx -> walkGraph(ctx))
        .flatMap(ctx -> finalize(ctx));
}

// 改造后 (同步，在虚拟线程中运行)
public ExecutionResult execute(ExecutionRequest request) {
    ExecutionContext ctx = initContext(request);
    walkGraph(ctx);
    return finalize(ctx);
}
```

#### 4.2 Handler改造

```java
// 改造前
@Override
public Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx) {
    return Mono.fromCallable(() -> doExecute(config, ctx))
        .subscribeOn(Schedulers.boundedElastic());
}

// 改造后 (同步)
@Override
public NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
    return doExecute(config, ctx);
}
```

#### 4.3 NodeHandler接口变更

```java
// 改造前
public interface NodeHandler {
    Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx);
    boolean isReachNode() { return false; }
    boolean isTriggerNode() { return false; }
}

// 改造后 (保留executeAsync兼容，新增execute同步方法)
public interface NodeHandler {
    /** 同步执行（推荐，虚拟线程下自动调度） */
    default NodeResult execute(Map<String, Object> config, ExecutionContext ctx) {
        return executeAsync(config, ctx).block();
    }

    /** 异步执行（兼容旧Handler） */
    Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx);

    boolean isReachNode() { return false; }
    boolean isTriggerNode() { return false; }
}
```

### Step 5: 外部API调用改造（1周）

#### 5.1 WebClient → RestClient

```java
// 改造前
private final WebClient webClient;

public Mono<ApiResponse> callExternalApi(String url, Object body) {
    return webClient.post()
        .uri(url)
        .bodyValue(body)
        .retrieve()
        .bodyToMono(ApiResponse.class);
}

// 改造后
private final RestClient restClient;

public ApiResponse callExternalApi(String url, Object body) {
    return restClient.post()
        .uri(url)
        .body(body)
        .retrieve()
        .body(ApiResponse.class);
}
```

#### 5.2 Redis操作改造

```java
// 改造前 (Lettuce Reactive)
private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

public Mono<String> getValue(String key) {
    return reactiveRedisTemplate.opsForValue().get(key);
}

// 改造后 (Lettuce Sync，虚拟线程下不阻塞平台线程)
private final StringRedisTemplate redisTemplate;

public String getValue(String key) {
    return redisTemplate.opsForValue().get(key);
}
```

---

## 四、需要保留Reactor的部分

| 模块 | 原因 | 处理方式 |
|------|------|---------|
| DagEngine内部调度 | 并行分支执行需要异步 | 保留Mono，Controller层.block() |
| Disruptor事件分发 | 异步事件驱动 | 保留，不依赖WebFlux |
| 企微API调用 | 需要异步HTTP | RestClient + 虚拟线程 |
| MQ消息发送 | 异步消息 | RocketMQTemplate同步发送 |

---

## 五、迁移风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| Handler接口变更 | 57个Handler需适配 | 保留executeAsync兼容，新增execute默认方法 |
| DAG引擎并行执行 | 同步化可能降低并行度 | 虚拟线程天然并行，无需Reactor |
| 前端API兼容 | 响应格式可能变化 | 保持JSON格式不变 |
| 测试覆盖 | 迁移后需重新测试 | 逐模块迁移+测试 |
| 性能回退 | 初期可能不如WebFlux | 虚拟线程+ZGC优化后性能持平 |

---

## 六、性能对比预期

| 场景 | WebFlux (当前) | Spring MVC + VT (目标) |
|------|---------------|----------------------|
| 简单查询 | ~5ms | ~5ms (持平) |
| 画布执行 | ~200ms | ~200ms (持平) |
| 并发连接 | ~5000 (理论) | ~10000+ (虚拟线程) |
| DB阻塞操作 | 需boundedElastic | 自动调度到虚拟线程 |
| 内存占用 | ~512MB | ~1GB (虚拟线程开销) |
| 代码复杂度 | 高 (Reactor) | 低 (同步) |

---

## 七、实施时间表

| 周 | 内容 | 工时 |
|---|------|------|
| Week 1 | Controller层同步化改造 | 16h |
| Week 2 | Service层适配 + 测试 | 16h |
| Week 3 | 移除WebFlux + 引入Spring MVC + 虚拟线程配置 | 8h |
| Week 4 | DAG引擎内部改造 | 16h |
| Week 5 | Handler接口适配 + RestClient改造 | 16h |
| Week 6 | Redis/MQ改造 + 全量测试 | 8h |
| **合计** | | **80h** |

---

## 八、相关文档

- [目标架构总览](target-architecture-overview.md)
- [企微SCRM模块设计](wecom-scrm-module-design.md)
- [K8s部署方案](k8s-deployment-plan.md)
- [多数据源隔离方案](multi-datasource-isolation.md)