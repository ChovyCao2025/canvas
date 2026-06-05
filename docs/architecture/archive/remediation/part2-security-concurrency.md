# 架构整改方案 — security-concurrency
> 详见 [README.md](./README.md) 获取完整索引


## 第二部分：安全与并发问题（31项）

> 以下问题来自对后端代码的安全/并发/异常/资源泄露深度扫描。

---

### 问题七：安全漏洞（10项）

#### 现状摘要

| # | 位置 | 问题 | 级别 |
|---|------|------|------|
| S1 | `application.yml:8-9` | 数据库凭据硬编码 `username: root, password: root` | P0 |
| S2 | `application.yml:55` | 事件上报密钥弱默认值 `canvas-event-report-secret-2026!!` | P0 |
| S3 | `application.yml:52-53` | JWT Secret 默认为空，无启动校验 | P0 |
| S4 | `application.yml:59` | CORS `allowed-origins: "*"` | P1 |
| S5 | `WebConfig.java:36-38,45` | `addAllowedOriginPattern("*")` + `setAllowCredentials(true)` 组合，凭据窃取风险 | P0 |
| S6 | `application.yml:166` | Actuator health `show-details: always` 暴露内部组件状态 | P1 |
| S7 | `SecurityConfig.java:59-61` | Swagger UI 无需认证即可访问 | P1 |
| S8 | `SecurityConfig.java:64-66` | `/canvas/execute/direct/*` 和 `/canvas/trigger/behavior` 无需认证 | P1 |
| S9 | `SecurityConfig.java:70` | `/ops/**` 运维接口无需认证 | P1 |
| S10 | `GlobalExceptionHandler.java:84` | 500 响应中泄露异常堆栈信息 | P1 |

#### 实施方案

**Step 1: 凭据外部化 + 启动校验（S1/S2/S3）**

```yaml
# application.yml — 所有敏感值改为必填环境变量
spring:
  datasource:
    username: ${SPRING_DATASOURCE_USERNAME}     # 无默认值，启动必须设置
    password: ${SPRING_DATASOURCE_PASSWORD}     # 无默认值
canvas:
  jwt:
    secret: ${CANVAS_JWT_SECRET}                # 无默认值
  events:
    report-secret: ${CANVAS_EVENT_REPORT_SECRET} # 无默认值
```

新增启动校验：
```java
@Configuration
public class SecurityStartupValidator {
    @Value("${canvas.jwt.secret:}")
    private String jwtSecret;

    @PostConstruct
    public void validate() {
        if (jwtSecret.isBlank()) {
            throw new IllegalStateException("CANVAS_JWT_SECRET must be set");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("CANVAS_JWT_SECRET too short (min 32 chars)");
        }
    }
}
```

**Step 2: CORS 修复（S4/S5）**

```yaml
canvas:
  cors:
    allowed-origins: ${CANVAS_CORS_ALLOWED_ORIGINS}  # 必填，逗号分隔白名单
```

```java
// WebConfig.java — 移除通配符，改为配置白名单
@Override
public void addCorsMappings(CorsRegistry registry) {
    String[] origins = allowedOrigins.split(",");
    registry.addMapping("/api/**")
        .allowedOrigins(origins)      // 显式白名单
        .allowedMethods("GET","POST","PUT","DELETE")
        .allowCredentials(true);
}
```

**Step 3: 生产环境接口保护（S6/S7/S8/S9）**

```java
// SecurityConfig.java — 引入 profile 控制
@Profile("prod")
@Configuration
static class ProdSecurityConfig {
    @Bean
    public SecurityFilterChain prodSecurityFilterChain(HttpSecurity http) {
        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").denyAll()  // 生产禁用
            .requestMatchers("/actuator/health").permitAll()                  // 仅基本健康检查
            .requestMatchers("/actuator/**").denyAll()                        // 其他 actuator 禁用
            .requestMatchers("/ops/**").hasRole("ADMIN")                      // 运维需 ADMIN
            .anyRequest().authenticated()
        );
        return http.build();
    }
}
```

**Step 4: 异常信息脱敏（S10）**

```java
// GlobalExceptionHandler.java
@ExceptionHandler(Exception.class)
public R<Void> handleException(Exception e) {
    log.error("Unhandled exception", e);           // 完整堆栈仅记日志
    return R.fail("系统错误，请稍后重试");            // 客户端不泄露详情
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 凭据外部化 + 启动校验 | 2h |
| 2 | CORS 白名单修复 | 1h |
| 3 | 生产环境接口保护 | 3h |
| 4 | 异常信息脱敏 | 1h |
| 5 | 全量测试 | 2h |

**总工时**: ~9h

---

### 问题八：并发安全缺陷（8项）

#### 现状摘要

| # | 位置 | 问题 | 级别 |
|---|------|------|------|
| C1 | `CircuitBreakerRegistry:96-116` | `checkState()` TOCTOU 竞态 | P0 |
| C2 | `CircuitBreakerRegistry:119-125` | `recordSuccess()` 非原子状态转换 | P0 |
| C3 | `CircuitBreakerRegistry:128-143` | `recordFailure()` 非原子状态转换 | P0 |
| C4 | `ExecutionContext:58,61` | `volatile boolean` 复合条件判断非线程安全 | P1 |
| C5 | `ExecutionContext:128-143` | `putNodeOutput()` 非原子复合操作 | P1 |
| C6 | `CanvasSchedulerService:92` | `private boolean closed` 非 volatile | P1 |
| C7 | `CanvasSchedulerService:497-513` | `PendingJitterGroup.add()` check-then-act 竞态 | P1 |
| C8 | `NodeGate:18,21` | `AtomicBoolean` 字段 public 暴露 | P2 |

#### 实施方案

**Step 1: CircuitBreaker 状态原子化（C1/C2/C3）— 最紧急**

```java
// Before: volatile + 非原子判断
private volatile State state = State.CLOSED;

public void recordSuccess() {
    if (state == State.HALF_OPEN) {
        state = State.CLOSED;       // 竞态！
        failureCounter.set(0);
    }
}

// After: AtomicReference + CAS
private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);

public void recordSuccess() {
    State current;
    while ((current = state.get()) == State.HALF_OPEN) {
        if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
            failureCounter.set(0);
            log.info("[CB] HALF_OPEN → CLOSED (success)");
            return;
        }
    }
}
```

**Step 2: ExecutionContext 复合操作同步化（C4/C5）**

```java
// Before: volatile + 复合检查
private volatile boolean benefitGranted = false;
public boolean tryMarkBenefit() {
    if (!benefitGranted && !userReached) {  // 竞态
        benefitGranted = true;
        return true;
    }
    return false;
}

// After: AtomicBoolean + CAS
private final AtomicBoolean benefitGranted = new AtomicBoolean(false);
public boolean tryMarkBenefit() {
    return benefitGranted.compareAndSet(false, true) && !userReached.get();
}
```

对于 `putNodeOutput()`，使用 `synchronized` 保护复合操作：
```java
public synchronized void putNodeOutput(String nodeId, Map<String, Object> output) {
    nodeOutputs.put(nodeId, output);
    flatContext.putAll(output);
    approxSizeBytes.addAndGet(estimateSize(output));
}
```

**Step 3: CanvasSchedulerService 修复（C6/C7）**

```java
// C6: volatile 修饰
private volatile boolean closed = false;

// C7: synchronized 保护 check-then-act
public synchronized void add(Registration reg) {
    if (terminated || closeWhenIdle) return;
    pending.add(reg);
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | CircuitBreaker CAS 重构 | 4h |
| 2 | ExecutionContext 同步化 | 4h |
| 3 | CanvasSchedulerService volatile/synchronized | 2h |
| 4 | NodeGate 封装 + 测试 | 2h |
| 5 | 并发压测验证 | 4h |

**总工时**: ~16h

---

### 问题九：异常处理缺陷（8项）

#### 现状摘要

| # | 位置 | 问题 | 级别 |
|---|------|------|------|
| E1 | `DagEngine:1237` | `catch (Exception ignored)` 吞掉 JSON 序列化异常 | P1 |
| E2 | `CanvasExecutionService:1379` | `catch (Exception ignored)` 吞掉统计序列化异常 | P1 |
| E3 | `CanvasRouteInitializer:76` | `InterruptedException` 吞掉且未恢复中断标志 | P1 |
| E4 | `AuthController:127` | `catch (Exception ignored)` 吞掉 token 解析异常 | P2 |
| E5 | `ApiCallHandler:206` | `catch (Exception ignored)` 吞掉 JSON 解析异常 | P2 |
| E6 | `TraceWriteBuffer:123` | 仅记录消息不记录堆栈 | P2 |
| E7 | `CanvasExecutionService:1406` | 仅记录消息不记录堆栈 | P2 |
| E8 | `GlobalExceptionHandler:28,36` | 内部异常信息直接透传客户端 | P2 |

#### 实施方案

**Step 1: 消除吞异常（E1/E2/E4/E5）**

```java
// Before
catch (Exception ignored) {}

// After
catch (Exception e) {
    log.warn("[TRACE] Failed to serialize node outputs for trace", e);
}
```

**Step 2: 恢复中断标志（E3）**

```java
// Before
catch (InterruptedException ignored) {}

// After
catch (InterruptedException e) {
    Thread.currentThread().interrupt();  // 恢复中断标志
    log.warn("[ROUTE] Route initialization interrupted", e);
    break;
}
```

**Step 3: 补充堆栈记录（E6/E7）**

```java
// Before
log.error("...: {}", e.getMessage());

// After
log.error("...: {}", e.getMessage(), e);  // 附加完整堆栈
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 修复所有空 catch 块 | 2h |
| 2 | 恢复中断标志 | 0.5h |
| 3 | 补充堆栈记录 | 1h |
| 4 | 全量测试 | 1h |

**总工时**: ~4.5h

---

### 问题十：资源泄露（5项）

#### 现状摘要

| # | 位置 | 问题 | 级别 |
|---|------|------|------|
| R1 | `DagEngine:637,726,777,807` | `Mono.delay().subscribe()` 火忘模式，Disposable 未存储 | P0 |
| R2 | `DagEngine:835-856` | `handleSpecialNodeTimeout` 多个 `.subscribe()` 火忘 | P1 |
| R3 | `DagEngine:562` | DLQ 写入 `.subscribe(null, ...)` 火忘 | P1 |
| R4 | `CanvasSchedulerService:363-381` | `executionService.trigger().subscribe()` 火忘 | P1 |
| R5 | `CanvasSchedulerService:475-565` | `PendingJitterGroup` 可能遗漏已提交任务 | P2 |

#### 实施方案

**Step 1: Disposable 统一管理（R1/R2/R3/R4）**

```java
// DagEngine 中新增
private final ConcurrentLinkedDeque<Disposable> activeSubscriptions = new ConcurrentLinkedDeque<>();

// 封装安全 subscribe
private void trackSubscribe(Mono<?> mono, Consumer<Throwable> onError) {
    Disposable d = mono.subscribe(null, onError, null);
    activeSubscriptions.add(d);
}

// 画布取消/下线时清理
public void disposeAll() {
    Disposable d;
    while ((d = activeSubscriptions.poll()) != null) {
        if (!d.isDisposed()) d.dispose();
    }
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | DagEngine Disposable 追踪 | 4h |
| 2 | CanvasSchedulerService 修复 | 2h |
| 3 | 画布取消/下线清理逻辑 | 2h |
| 4 | 集成测试 | 2h |

**总工时**: ~10h

---
