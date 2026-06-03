# Deep Architecture Audit — Round 7

> 第七轮：架构配置/安全/运维/事务/可观测性维度深度扫描
> 审计日期: 2026-05-31

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **P0 CRITICAL** | 5 | Dockerfile跑root、4个公开端点无认证、15个@Transactional缺rollbackFor、零分布式追踪、零HealthIndicator |
| **P1 HIGH** | 8 | 5个bare subscribe()无错误handler、11个catch(Exception ignored)、R类无traceId字段、MDC零使用、无application-prod.yml、无CI/CD、synchronized在WebFlux中、单ObjectMapper静态实例 |
| **P2 MEDIUM** | 6 | Redis密码注释掉+MySQL root明文、JWT Secret空默认值+硬编码HMAC Secret、默认Profile非prod、8处虚拟线程无统一管理、CanvasMetrics缺少关键指标、3个TODO/FIXME未处理 |
| **P3 LOW** | 3 | JwtUtil用java.util.Date、Vite test environment=node而非jsdom、4处ThreadLocalRandom不可控 |

---

## P0 — CRITICAL

### P0-1: Dockerfile 以 root 运行 — 容器逃逸风险

**文件**: `canvas-engine/Dockerfile`

**问题**: Dockerfile 没有 `USER` 指令，容器以 root 运行。如果应用有漏洞（如 Groovy 沙箱逃逸），攻击者获得 root 权限可逃逸到宿主机。

**修复**: 添加 `RUN addgroup -S app && adduser -S app -G app && chown -R app:app /app` + `USER app`

---

### P0-2: 4 个公开端点 + ops 完全无认证 — 入侵入口

**文件**: `SecurityConfig.java:62-70`

**问题**:
```java
.pathMatchers(HttpMethod.POST, "/canvas/events/report").permitAll()
.pathMatchers(HttpMethod.POST, "/canvas/execute/direct/*").permitAll()
.pathMatchers(HttpMethod.POST, "/canvas/trigger/behavior").permitAll()
.pathMatchers("/canvas/ws/notifications").permitAll()
.pathMatchers("/ops/**").permitAll()
```

5 个路径 permitAll：
- `/canvas/execute/direct/*` — 直调执行，任何人可触发画布执行
- `/canvas/trigger/behavior` — 行为触发，任何人可触发
- `/ops/**` — 运维端点，任何人对画布做运维操作

events/report 有 HMAC 签名保护，但其他 4 个端点无任何认证/限流。

**修复**:
1. `/canvas/execute/direct/*` 和 `/canvas/trigger/behavior` 必须添加 HMAC/API Key 认证
2. `/ops/**` 必须移除 permitAll，至少要求 ADMIN 角色
3. `/canvas/ws/notifications` 需验证票据后才能连

---

### P0-3: 15 个 @Transactional 无 rollbackFor — 事务回滚不完整

**问题**: 19 个 @Transactional 中仅 1 个声明 `rollbackFor = Exception.class`（TagImportService），其余 15 个使用默认 `@Transactional`（仅回滚 RuntimeException）。

如果任何 checked Exception 被抛出（如 IOException），DB 事务不会回滚，但业务语义期望回滚。

关键影响位置：
- `CanvasTransactionService` 4 个事务方法 — 画布发布/下线/回滚的 DB 操作
- `CanvasOpsService` 6 个事务方法 — 画布运维操作
- `CanvasService` 3 个事务方法 — 画布 CRUD

**修复**: 所有 `@Transactional` 加 `rollbackFor = Exception.class`

---

### P0-4: 零分布式追踪 — 生产故障排查从分钟级变小时级

**问题**:
- 无 Micrometer Tracing / Zipkin / Jaeger / Sleuth 依赖
- pom.xml 仅 `micrometer-registry-prometheus`（指标，非追踪）
- MDC 字段定义在 logback-spring.xml（traceId, canvasId, nodeId, userId）但 **零代码使用 MDC.put()**
- R 类无 traceId 字段 — GlobalExceptionHandler 注释说返回 `{ traceId: "..." }` 但实际 R 类只有 code/message/data

**影响**:
1. 请求从入口到执行到节点完成的完整链路无法追踪
2. 多个画布并行执行时日志无法关联
3. API 错误响应无 traceId，前端无法上报关联信息

**修复**:
1. 添加 `micrometer-tracing-bridge-brave` + `zipkin-reporter` 依赖
2. 在关键入口（JwtAuthFilter、CanvasExecutionService.execute）设置 MDC
3. R 类添加 traceId 字段，GlobalExceptionHandler 自动填充

---

### P0-5: 零 HealthIndicator — 生产环境无健康检查语义

**问题**: Actuator 依赖存在但无自定义 HealthIndicator。Dockerfile 的 HEALTHCHECK 用 `/actuator/health`，但该端点只返回 UP/DOWN 状态（基于 DB/Redis 连接），不检查：
- Disruptor ring buffer 是否满
- HikariCP 活跃连接是否耗尽
- 4 个 lane 的队列深度
- 灰度比例是否健康
- TraceWriteBuffer 是否溢出

**影响**: 生产环境看似"健康"但实际已无法处理新请求

**修复**: 添加 CanvasEngineHealthIndicator 检查关键运行状态

---

## P1 — HIGH

### P1-1: 5 个 bare .subscribe() 无错误 handler — 静默失败

**文件**:
- `KillSwitchSubscriber.java:73` — Kill 开关触发无错误处理
- `CanvasExecutionRequestExecutor.java:287` — 请求执行失败静默消失
- `TransferJourneyHandler.java:74` — 旅程转移失败静默消失
- `CanvasTriggerHandler.java:121` — 子画布触发失败静默消失
- `WaitResumeService.java:288` — 恢复执行失败（有部分错误处理）

**修复**: 所有 `.subscribe()` 改为 `.subscribe(onSuccess, onError)` 或使用 `.onErrorResume()` 链

---

### P1-2: 11 个 catch(Exception ignored) — 关键错误被吞没

**关键位置**:
- `DagEngine.java:1237` — 画布执行引擎吞异常
- `CanvasExecutionService.java:1379` — 执行服务吞异常
- `MetaController.java:449,479` — 元数据操作吞异常
- `AuthController.java:127` — 认证操作吞异常
- `EventDefinitionServiceImpl.java:178` — 事件定义吞异常
- `MarketingPolicyService.java:119` — 营销策略吞异常
- `CanvasOpsService.java:320` — 运维操作吞异常

**修复**: 至少 `log.warn` 或 `log.debug` 记录被忽略的异常，确保关键路径不静默失败

---

### P1-3: R 类无 traceId 字段 — 错误响应不可追踪

**文件**: `R.java`

**问题**: GlobalExceptionHandler 注释声称响应格式包含 `traceId`，但 R 类实际只有 code/message/data 三个字段。前端收到 500 错误后无法上报 traceId 给运维。

**修复**: R 类添加 `traceId` 字段，从 MDC 或 request context 获取

---

### P1-4: MDC 零使用 — 结构化日志无上下文关联

**文件**: `logback-spring.xml:16-19`

**问题**: logback-spring.xml 配置了 MDC 字段提取（traceId, canvasId, nodeId, userId），但全项目零 `MDC.put()` 调用。生产 JSON 日志中这些字段永远是空。

**修复**:
1. JwtAuthFilter: `MDC.put("userId", userId)`
2. CanvasExecutionService.execute: `MDC.put("canvasId", canvasId)`
3. DagEngine.executeNode: `MDC.put("nodeId", nodeId)`
4. 添加 MDC cleanup（try-finally 或 Reactor contextWrite）

---

### P1-5: 无 application-prod.yml — 所有危险默认值在生产生效

**问题**: 仅 1 个 application.yml（无 profile 分化）。Dockerfile 设置 `spring.profiles.active=prod` 但无对应配置文件。生产将使用：
- MySQL password: `root`（明文）
- Redis password: 无（注释掉）
- JWT secret: 空默认值 `${CANVAS_JWT_SECRET:}`
- Event report secret: 硬编码 `canvas-event-report-secret-2026!!`
- CORS wildcard
- SSL: `useSSL=false`

**修复**: 创建 `application-prod.yml` 覆盖所有危险默认值

---

### P1-6: synchronized 在 WebFlux 线程模型中 — 潜在阻塞

**文件**: `CanvasSchedulerService.java:171-269` (7 处)

**问题**: CanvasSchedulerService 使用 `synchronized(lifecycleLock)` 保护 7 个操作。WebFlux 的 Netty 事件循环只有少量线程，synchronized 阻塞可能导致事件循环饥饿。

**修复**: 改用 `ReentrantLock` 或 `AtomicReference<State>` + CAS 模式

---

### P1-7: 静态 ObjectMapper 实例 — 不可测试、不可配置

**文件**: `ConditionEvaluator.java:19`

```java
private static final RuleParser RULE_PARSER = new RuleParser(new ObjectMapper());
```

ObjectMapper 是重量级对象且配置敏感（日期格式、特性开关），静态创建无法注入测试配置或项目统一配置。

**修复**: 通过构造器注入 ObjectMapper

---

### P1-8: 无 CI/CD — 部署全手工

**问题**: 无 Jenkins/GitHub Actions/GitLab CI 配置文件。Dockerfile 存在但无构建流水线。部署依赖开发者手动打包和推送。

**影响**: 无法自动化测试、无法保证每次部署经过验证、无法回滚到已知版本

**修复**: 添加 GitHub Actions 或类似 CI/CD 配置

---

## P2 — MEDIUM

### P2-1: Redis 密码注释掉 + MySQL root 密码明文

**文件**: `application.yml:9,29`

```yaml
password: root          # MySQL root明文
# password: （无密码时注释掉）  # Redis无密码
```

**修复**: 生产配置通过 Vault/K8s Secret 注入，不写在配置文件

---

### P2-2: JWT Secret 空默认值 + 硬编码 HMAC Secret

**文件**: `application.yml:52,55`

```yaml
secret: ${CANVAS_JWT_SECRET:}                          # 空！
report-secret: ${CANVAS_EVENT_REPORT_SECRET:canvas-event-report-secret-2026!!}  # 硬编码
```

空 JWT secret = 启动时随机生成 = 每次重启所有 Token 失效。硬编码 HMAC secret = 任何看到源码的人可伪造事件上报。

**修复**: JWT secret 必须通过环境变量注入且非空；HMAC secret 不能硬编码

---

### P2-3: 默认 Profile 非 prod — 开发配置在生产生效

**文件**: `logback-spring.xml:36`

```xml
<springProfile name="!prod">
    <logger name="org.chovy.canvas" level="DEBUG"/>
```

Dockerfile 设置 `spring.profiles.active=prod`，但如果不通过 Dockerfile 启动（如直接 `mvn spring-boot:run`），默认是非 prod profile + DEBUG 日志。

**修复**: application.yml 添加 `spring.profiles.default: prod` 或启动脚本强制设置

---

### P2-4: 8 处虚拟线程无统一管理 — 不可监控

**问题**: 8 处 `Thread.ofVirtual().start()` 和 1 处 `Executors.newVirtualThreadPerTaskExecutor()`，无统一管理、无监控、无命名。

**修复**: 创建 `VirtualThreadFactory`（带命名和 MDC 传播），统一入口

---

### P2-5: CanvasMetrics 缺少关键指标

**现有指标**: execution.total/duration, node.execution.total/duration/retry, dlq.size, trigger.deduplicated/quota.rejected, disruptor.published/overflow, execution.request.*, mq.trigger/route.rejected

**缺少指标**:
- HikariCP 活跃/等待连接数（依赖 Actuator 自动暴露）
- Lane 队列深度（light/standard/heavy/retry）
- ExecutionContext 内存占用估算
- Circuit breaker 状态（OPEN/HALF_OPEN/CLOSED）x nodeType
- 缓存命中率（Caffeine L1 + Redis L2）

**修复**: 添加 Gauge 指标暴露运行状态

---

### P2-6: 3 个 TODO/FIXME 未处理

**文件**:
- `CanvasExecutionService.java:893` — FIXME: dedupTtl = globalTimeoutSec + 600 过期时间问题
- `CanvasExecutionService.java:903` — FIXME: 过期时间会发生变化, 判断是否需要调整
- `InAppNotifyHandler.java:43` — TODO: 接入 MQTT 推送客户端

**修复**: FIXME 应尽快解决（影响去重逻辑正确性），TODO 应跟踪为 backlog item

---

## P3 — LOW

### P3-1: JwtUtil 用 java.util.Date — 旧式 API

**文件**: `JwtUtil.java:59`

现代 Java 应使用 `java.time.Instant`。

### P3-2: Vitest test environment=node — 无 DOM 测试

**文件**: `frontend/vite.config.ts`

Tests run in Node, not browser-like environment. No component rendering tests possible.

### P3-3: 4 处 ThreadLocalRandom — 测试不可控

DelayHandler, WeightedChoice, AudienceComputeTaskRunner, 和 CanvasSchedulerService 使用 `ThreadLocalRandom`。GoalCheckHandler 和 WaitHandler 已使用 Clock 注入作为良好模式。

---

## Cumulative Findings (Rounds 1-7)

| Severity | R1 | R2 | R3 | R4 | R5 | R6 | **R7** | **Total** |
|----------|----|----|----|----|----|----|----|-----------|
| P0 CRITICAL | 21 | 8 | 3 | 2 | 1 | 0 | **5** | **40** |
| P1 HIGH | 39 | 12 | 6 | 5 | 5 | 3 | **8** | **78** |
| P2 MEDIUM | — | 9 | 5 | 4 | 5 | 5 | **6** | **34** |
| P3 LOW | — | 4 | 2 | 1 | 2 | 2 | **3** | **14** |

### 新发现趋势

| 轮次 | P0 | P1 | 总新发现 | 变化 |
|------|----|----|---------|------|
| R1 | 21 | 39 | 60 | 基线 |
| R2 | 8 | 12 | 20 | -67% |
| R3 | 3 | 6 | 9 | -55% |
| R4 | 2 | 5 | 7 | -22% |
| R5 | 1 | 5 | 6 | -14% |
| R6 | 0 | 3 | 3 | -50% |
| R7 | 5 | 8 | 13 | +333% |

**R7 新发现大幅反弹**。原因是本轮从代码级转向架构配置级扫描，发现了之前 6 轗"代码审核"未覆盖的维度：安全配置、事务语义、分布式追踪、运维健康检查、Docker 安全。这些是架构级问题，代码审核不会触及。

**建议继续 R8**，聚焦：Flyway 迁移安全性、API 版本策略、数据保留策略、环境隔离。
