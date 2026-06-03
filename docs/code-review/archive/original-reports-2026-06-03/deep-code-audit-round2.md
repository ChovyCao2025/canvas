# Deep Code Audit — Round 2

> 在第一轮 38% 通过率审查基础上，深入扫描并发安全、Reactor 合规、数据正确性、异常处理、资源泄漏、前端安全、API 设计缺陷。
> 审计日期: 2026-05-31

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **P0 CRITICAL** | 8 | SQL注入、Reactor阻塞、资源泄漏、认证绕过、数据正确性 |
| **P1 HIGH** | 12 | 异常吞没、并发竞态、线程安全、类型安全、事务一致性 |
| **P2 MEDIUM** | 9 | 代码规范、性能、可观测性、前端安全 |
| **P3 LOW** | 4 | 风格、TODO、命名 |

---

## P0 — CRITICAL

### P0-1: Audience JDBC 动态 DataSource 连接泄漏

**文件**: `engine/audience/AudienceUserResolver.java:42-79`, `AudienceBatchComputeService.java:166-195`

**问题**: 每次人群解析都 `DataSourceBuilder.create().build()` 创建新连接池，finally 中 `AutoCloseable.close()` 仅关闭连接但不等连接池回收。高频人群触发时，HikariCP 连接池会快速耗尽外部数据库的连接限额。

**影响**: 外部数据库连接数耗尽 → 所有人群计算失败 → 画布执行阻塞

**修复**: 引入 `DataSource` 池化（按 dataSourceId 缓存 + TTL 回收），或使用 Spring 的 `AbstractRoutingDataSource`。

---

### P0-2: SQL 注入 — 表名/列名拼接虽有限制但不充分

**文件**: `engine/audience/AudienceUserResolver.java:54`, `AudienceBatchComputeService.java:177`

**问题**: 
```java
String sql = "SELECT " + jdbcConfig.userIdColumn() + " FROM " + jdbcConfig.baseTable() + " WHERE " + where.sql();
```
`JdbcConfigResolver` 用正则 `[A-Za-z_][A-Za-z0-9_]*` 验证表名/列名，但这仅阻止简单注入。`SqlWhereGenerator` 生成的 WHERE 子句来自用户定义的规则 JSON，经 `RuleSqlCompiler` 编译。如果 `RuleSqlCompiler` 存在漏洞或未来修改放宽限制，这里就是 SQL 注入入口。

**影响**: 用户通过人群规则配置注入 SQL → 读取任意表数据

**修复**: 
1. `SqlWhereGenerator` 输出必须 100% 使用 `:namedParam` 占位符，禁止任何字符串拼接
2. 增加 `RuleSqlCompiler` 输出的白名单校验（仅允许 `AND/OR/=/!=/>/</IS NULL/IS NOT NULL` + 列名占位符）
3. 添加显式 SQL 注入测试

---

### P0-3: GroovyHandler 虚拟线程池永不关闭

**文件**: `engine/handlers/GroovyHandler.java:58`

**问题**: 
```java
private final ExecutorService vte = Executors.newVirtualThreadPerTaskExecutor();
```
该 `ExecutorService` 作为字段直接初始化，无 `@PreDestroy` 关闭逻辑。应用关闭时，正在执行的 Groovy 脚本不会被优雅中断，可能导致：
- 脚本持有的外部连接（DB/HTTP）泄漏
- 虚拟线程未完成导致 JVM 退出延迟或 hang

**影响**: 应用重启时脚本执行无法优雅终止 → 连接泄漏 / JVM hang

**修复**: 添加 `@PreDestroy void shutdown() { vte.shutdownNow(); }`，并处理 `InterruptedException`。

---

### P0-4: .block() 在 Reactor 链路中调用 — 阻塞 Netty EventLoop

**文件**: 5 处生产代码

| 文件 | 行号 | 调用 |
|------|------|------|
| `CanvasSchedulerService` | 424, 451 | `.block()` 在定时触发链路 |
| `AudienceBatchComputeService` | 250 | `.block()` 在人群计算链路 |
| `AudienceEvaluationContextFetcher` | 51 | `.block()` 在上下文获取 |
| `TagImportSourceService` | 143 | `.block()` 在 WebClient 调用 |

**影响**: 如果这些代码在 Netty EventLoop 线程上执行，会阻塞整个事件循环 → 所有请求卡死

**修复**: 
- `CanvasSchedulerService`: 定时任务已在 `ThreadPoolTaskScheduler` 上执行，可接受，但需加注释说明
- `AudienceBatchComputeService/ContextFetcher`: 改为响应式链路或显式 `subscribeOn(Schedulers.boundedElastic())`
- `TagImportSourceService`: 改为返回 `Mono<JsonNode>` 而非 `.block()`

---

### P0-5: ExecutionController 公开端点信任请求体 userId

**文件**: `web/ExecutionController.java:66-72`

**问题**: `behaviorTrigger` 端点完全无认证（SecurityConfig permitAll），且直接信任请求体中的 `req.getUserId()`：
```java
disruptorService.publish(req.getCanvasId(), req.getUserId(), ...);
```
而 `directCall` 端点通过 `currentUserId()` 从 JWT 取 userId（正确做法）。行为触发端点允许调用方伪造任意 userId 执行画布。

**影响**: 攻击者可伪造 userId 触发任意用户的画布执行 → 权限提升 / 数据越权

**修复**: 
1. 行为触发端点添加 HMAC 签名认证（与 EventReportAuthService 一致）
2. 或添加 API Key 认证
3. 至少校验 userId 非空且格式合法

---

### P0-6: CanvasExecutionService 1407 行 — God Class

**文件**: `engine/trigger/CanvasExecutionService.java`

**问题**: 1407 行，承担执行触发、去重、上下文初始化、DAG 调度、结果写入、DLQ、重试、恢复等全部职责。直接注入 15+ 依赖。

**影响**: 
- 任何修改都有高回归风险
- 测试困难（需 mock 15+ 依赖）
- 违反单一职责原则

**修复**: 拆分为：
- `ExecutionTriggerService` — 触发 + 去重
- `ExecutionContextInitializer` — 上下文加载/初始化
- `ExecutionResultWriter` — 结果写入 + 统计
- `ExecutionRecoveryService` — 恢复 + 重试

---

### P0-7: DagEngine 1539 行 — 核心引擎过度耦合

**文件**: `engine/scheduler/DagEngine.java`

**问题**: 1539 行，包含节点执行、特殊节点处理、LOGIC_RELATION、Wait/Hub 超时、DLQ 写入、trace 记录、circuit breaker 等全部逻辑。

**影响**: 引擎核心修改风险极高，任何 bug 都影响全部画布执行

**修复**: 提取 `SpecialNodeDispatcher`、`TraceRecorder`、`CircuitBreakerEvaluator` 等子组件

---

### P0-8: @Transactional + Redis 操作 — 事务回滚不一致

**文件**: `domain/canvas/CanvasService.java:86,132,445`, `CanvasTransactionService.java:46,92,118,140`

**问题**: 代码注释已指出此问题：
```java
// 混在 @Transactional 内，DB 回滚时 Redis 路由状态已改变，造成不可逆不一致。
```
`@Transactional` 方法内同时执行 DB 更新和 Redis 路由变更。DB 回滚时 Redis 已变更，导致 DB 与 Redis 状态不一致。

**影响**: 画布发布/下线失败后，Redis 路由指向不存在的版本 → 触发执行 404 或执行错误版本

**修复**: 
1. Redis 操作移到 `@Transactional` 外（`TransactionSynchronizationManager.registerSynchronization` + `afterCommit`）
2. 或引入 Saga 补偿模式

---

## P1 — HIGH

### P1-1: catch(Exception ignored) — 异常吞没 (10 处)

**文件**: `AuthController:127`, `CanvasExecutionManagementController:126`, `MetaController:449,479`, `EventDefinitionServiceImpl:178`, `CanvasRouteInitializer:76`, `MqTriggerConsumer:213,228`, `OutboundUrlValidator:97`, `TenantContextResolver:41`

**问题**: `catch (Exception ignored)` 完全吞没异常，无日志、无指标、无重抛。

**影响**: 关键错误被静默忽略 → 难以排查生产问题

**修复**: 至少添加 `log.debug()` 或 `log.trace()`，关键路径（Auth、MQ 消费）必须 `log.warn()`

---

### P1-2: InterruptedException 处理不当

**文件**: `CanvasRouteInitializer.java:76`

**问题**: 
```java
try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
```
吞没 `InterruptedException` 且不恢复中断标志。虚拟线程中此问题更严重——中断是取消信号。

**修复**: `catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }`

---

### P1-3: Thread.sleep 在 Reactor 应用中

**文件**: `TriggerRouteService.java:190` (50ms spin-wait), `CanvasRouteInitializer.java:76` (2s 启动等待), `AudienceComputeTaskRunner.java:216` (lock retry)

**问题**: 在 WebFlux 应用中使用 `Thread.sleep` 阻塞线程。`TriggerRouteService` 用 spin-wait 等待 Redis 锁，在高并发下浪费线程资源。

**修复**: 
- `TriggerRouteService`: 改用 Redis 分布式锁（Redisson `RLock.tryLock()`）
- `CanvasRouteInitializer`: 改用 `ApplicationListener<ContextRefreshedEvent>` 或 `@PostConstruct` 顺序保证
- `AudienceComputeTaskRunner`: 改用 `Mono.delay()` 响应式等待

---

### P1-4: fire-and-forget .subscribe() 无错误处理 (17 处)

**文件**: DagEngine (6处), CanvasSchedulerService (2处), CanvasDisruptorService (2处), WaitResumeService, ExecutionWatchdog, CanvasExecutionManagementController, KillSwitchSubscriber, CanvasTriggerHandler, TransferJourneyHandler, CanvasExecutionRequestExecutor

**问题**: `.subscribe()` 无 onError 回调。如果 Mono 链路出错，异常被 Reactor `Hooks.onErrorDropped` 吞掉（默认打 WARN 日志但不抛出）。

**影响**: DLQ 写入失败、触发执行失败等关键错误被静默丢弃

**修复**: 所有 `.subscribe()` 必须提供 onError 回调：`.subscribe(null, err -> log.error("...", err))`

---

### P1-5: SqlWhereGenerator 规则缓存无上限

**文件**: `engine/audience/SqlWhereGenerator.java:26`

**问题**: 
```java
private final ConcurrentMap<String, RuleGroup> ruleCache = new ConcurrentHashMap<>();
```
无 eviction 策略。每个不同 ruleJson 产生一个缓存条目，永不回收。

**影响**: 大量不同人群规则 → 内存泄漏 → OOM

**修复**: 改用 Caffeine 缓存 + 最大容量 + TTL

---

### P1-6: GroovyHandler shellPool.poll 超时后创建未池化 Shell

**文件**: `engine/handlers/GroovyHandler.java:156-157`

**问题**: 
```java
shell = shellPool.poll(100, TimeUnit.MILLISECONDS);
if (shell == null) shell = new GroovyShell(buildConfig());
```
池满时创建的 Shell 在 finally 中 `shellPool.offer(shell)`，但 `LinkedBlockingQueue` 已满会 offer 失败（静默丢弃）。这些 Shell 的安全配置与池中一致，但绕过了池容量限制。

**影响**: 高并发下池容量形同虚设 → 大量 Shell 对象创建 → GC 压力

**修复**: 池满时使用 `Mono.defer` + 排队等待，或改用信号量控制并发

---

### P1-7: ConditionEvaluator 静态创建 ObjectMapper

**文件**: `engine/handlers/ConditionEvaluator.java:19`

**问题**: 
```java
private static final RuleParser RULE_PARSER = new RuleParser(new ObjectMapper());
```
`ObjectMapper` 是重量级对象（线程安全但创建开销大），应复用 Spring 容器中的实例。静态创建绕过 Spring 管理，配置不一致（如日期格式、命名策略）。

**修复**: 注入 Spring 管理的 `ObjectMapper`

---

### P1-8: InFlightExecutionRegistry deregister 非原子操作

**文件**: `engine/trigger/InFlightExecutionRegistry.java:153-168`

**问题**: `deregister` 先移除本地条目再释放 Redis slot，两步非原子。如果 JVM 在两步之间崩溃，Redis slot 残留（虽然 TTL 自愈，但窗口期内并发计数偏高，可能拒绝合法请求）。

**影响**: 崩溃窗口期内并发计数虚高 → 合法执行被拒绝

**修复**: 可接受（TTL 自愈），但需文档说明此窗口期和影响

---

### P1-9: ExecutionContext nodeStatuses 使用 ConcurrentHashMap 但 NodeGate CAS 可能竞态

**文件**: `engine/context/ExecutionContext.java:187,275`

**问题**: `putIfAbsent` 和 `computeIfAbsent` 保证单次原子性，但 `DagEngine` 中 `handleLogicRelation` 的 CAS 模式（`gate.executing.compareAndSet(false, true)` + 业务逻辑 + `gate.executing.set(false)`）在异常时可能不释放锁。

**影响**: 如果 CAS 获胜后业务逻辑抛出未捕获异常，`executing` 永远为 true → 节点永远无法重新执行

**修复**: CAS 获胜后的业务逻辑必须 try-finally 确保 `executing.set(false)`

---

### P1-10: 前端 JWT 存储在 localStorage — XSS 可窃取

**文件**: `frontend/src/context/AuthContext.tsx:83-84`

**问题**: 
```typescript
localStorage.setItem('canvas_token', resp.token)
localStorage.setItem('canvas_user', JSON.stringify(resp))
```
localStorage 可被 XSS 读取。虽然当前无服务端渲染（XSS 风险较低），但存储型内容（画布名称/描述）若未转义，XSS 可窃取 token。

**修复**: 改用 `httpOnly` cookie 存储 JWT，或至少对 localStorage 值加密

---

### P1-11: 前端 62 处 `any` 类型 — 类型安全缺失

**文件**: `config-panel/index.tsx`, `audience-edit/index.tsx`, `canvas-editor/index.tsx`, `services/api.ts` 等

**问题**: 62 处 `any` 类型使用，包括 API 响应、表单值、规则序列化等关键路径。

**影响**: 运行时类型错误无法在编译期发现 → 生产 bug

**修复**: 为 API 响应定义 TypeScript interface，启用 `noImplicitAny` 严格模式

---

### P1-12: WebSocket 票据端点 permitAll — 票据重放风险

**文件**: `config/SecurityConfig.java:68`

**问题**: `/canvas/ws/notifications` permitAll，依赖一次性票据鉴权。但如果票据 TTL 过长或未正确验证，攻击者可重放票据建立 WebSocket 连接。

**修复**: 确认票据 TTL 足够短（当前 `TICKET_TTL_SECONDS`），且使用后立即删除

---

## P2 — MEDIUM

### P2-1: MapFieldKeys 691 行 — 常量类过大

**文件**: `common/MapFieldKeys.java`

**问题**: 691 行常量类，所有节点类型的字段 key 混在一起。

**修复**: 按节点类型拆分为 `MqTriggerKeys`、`WaitHandlerKeys` 等

---

### P2-2: 无 JaCoCo 覆盖率门控

**问题**: 无覆盖率工具，关键路径（DagEngine、CanvasExecutionService）无覆盖保证。

**修复**: 添加 JaCoCo + 80% 最低门槛（P0 路径 90%）

---

### P2-3: 无分布式追踪

**问题**: 无 Sleuth/Micrometer Tracing，跨节点执行链路无法追踪。

**修复**: 引入 Micrometer Tracing + Jaeger/Zipkin

---

### P2-4: 前端零组件测试

**问题**: 30 个测试文件全部测试纯函数，0 个组件渲染测试。canvas-editor、config-panel 等关键交互无测试。

**修复**: 引入 React Testing Library + MSW

---

### P2-5: 前端无 ErrorBoundary

**问题**: React 组件无 ErrorBoundary，任何渲染异常导致白屏。

**修复**: 在 App.tsx 和关键页面添加 ErrorBoundary

---

### P2-6: TriggerRouteService Redis 事务 + spin-wait

**文件**: `infrastructure/redis/TriggerRouteService.java:180-191`

**问题**: 用 `System.currentTimeMillis()` + `Thread.sleep(50)` 实现自旋锁等待 Redis 事务。在 WebFlux 环境中阻塞线程。

**修复**: 改用 Redisson `RLock` 或 `ReactiveRedisTemplate` + `Mono.retryWhen()`

---

### P2-7: CanvasRouteInitializer Thread.sleep(2000) 启动等待

**文件**: `infrastructure/redis/CanvasRouteInitializer.java:76`

**问题**: 硬编码 2 秒等待 Redis 就绪，不可靠且浪费启动时间。

**修复**: 改用 Redis `PING` 健康检查 + 重试

---

### P2-8: 前端 API 服务无统一错误处理

**文件**: `services/api.ts`

**问题**: API 错误在各页面分散处理（`catch (e: any) { message.error(...) }`），无统一拦截器。

**修复**: 添加 Axios interceptor 统一处理 401/403/500

---

### P2-9: run_token 作为执行请求认证凭证

**文件**: `dal/mapper/CanvasExecutionRequestMapper.java`

**问题**: `run_token` 用于执行请求的幂等校验和状态更新，但无加密/签名。如果 DB 泄露，攻击者可伪造 run_token 操控执行请求。

**修复**: run_token 应为加密随机值（UUID + HMAC），且传输时走 TLS

---

## P3 — LOW

### P3-1: 2 处 FIXME 注释

**文件**: `CanvasExecutionService.java:893,903`

```java
// FIXME：isResume 时 dedupTtl = globalTimeoutSec + 600
// FIXME: 过期时间会发生变化, 判断是否需要调整
```

**修复**: 排期解决或转为 Issue 跟踪

---

### P3-2: InAppNotifyHandler TODO

**文件**: `engine/handlers/InAppNotifyHandler.java:43`

```java
// TODO: 接入 MQTT 推送客户端
```

**修复**: 排期实现或标记为已知限制

---

### P3-3: BCryptPasswordEncoder 直接 new 而非注入

**文件**: `config/SecurityConfig.java:34`

**问题**: `new BCryptPasswordEncoder()` 使用默认 strength=10，未配置化。

**修复**: 改为 `new BCryptPasswordEncoder(strength)` + 配置项

---

### P3-4: 前端 window.location.reload() 硬刷新

**文件**: `pages/canvas-editor/index.tsx:1142`

**问题**: `window.location.reload()` 丢失 React 状态，用户体验差。

**修复**: 改为 React Router 导航或状态重置

---

## Cross-Cutting Concerns

### 并发安全矩阵

| 组件 | 并发原语 | 风险 | 评估 |
|------|---------|------|------|
| InFlightExecutionRegistry | ConcurrentHashMap + Redis Lua | Redis 原子, 本地非原子但 TTL 自愈 | **可接受** |
| ExecutionContext | ConcurrentHashMap + volatile + CAS | CAS 异常不释放锁 | **P1-9** |
| CanvasSchedulerService | synchronized(lifecycleLock) + ConcurrentHashMap | synchronized 在虚拟线程中 pin carrier | **P2** |
| DagEngine NodeGate | AtomicBoolean CAS | 异常路径不释放 | **P1-9** |
| TraceWriteBuffer | ConcurrentLinkedQueue + AtomicInteger | 单生产者-多消费者, 无锁 | **安全** |
| SqlWhereGenerator | ConcurrentHashMap (无上限) | 内存泄漏 | **P1-5** |
| GroovyHandler | BlockingQueue + VirtualThreadPerTaskExecutor | 池满绕过, 无 shutdown | **P0-3, P1-6** |

### Reactor 合规矩阵

| 模式 | 出现次数 | 严重度 |
|------|---------|--------|
| `.block()` 在生产代码 | 5 | P0-4 |
| `.subscribe()` 无 onError | 17 | P1-4 |
| `Thread.sleep()` | 3 | P1-3 |
| `Schedulers.boundedElastic()` 正确使用 | 30+ | 合规 |

### 文件大小超标

| 文件 | 行数 | 限制 | 严重度 |
|------|------|------|--------|
| DagEngine.java | 1539 | 500 | P0-7 |
| CanvasExecutionService.java | 1407 | 500 | P0-6 |
| MapFieldKeys.java | 691 | 500 | P2-1 |
| CanvasSchedulerService.java | 632 | 500 | P2 |
| CanvasService.java | 551 | 500 | P2 |
| MetaController.java | 523 | 500 | P2 |
| WaitHandler.java | 504 | 500 | P2 |

---

## Fix Priority Roadmap

### Week 1 — P0 Security & Data Safety
1. **P0-5**: ExecutionController 行为触发添加 HMAC/API Key 认证
2. **P0-2**: SqlWhereGenerator 输出白名单校验 + 注入测试
3. **P0-8**: @Transactional + Redis 操作改为 afterCommit
4. **P0-3**: GroovyHandler 添加 @PreDestroy shutdown

### Week 2 — P0 Reactor & Resource
5. **P0-1**: Audience DataSource 池化
6. **P0-4**: .block() 改为响应式或显式 boundedElastic
7. **P0-6**: CanvasExecutionService 拆分（Phase 1: 提取 ResultWriter）
8. **P0-7**: DagEngine 拆分（Phase 1: 提取 TraceRecorder）

### Week 3 — P1 Correctness
9. **P1-4**: 所有 .subscribe() 添加 onError
10. **P1-1**: catch(Exception ignored) → 至少 log.debug
11. **P1-9**: NodeGate CAS try-finally 保证
12. **P1-5**: SqlWhereGenerator 缓存改 Caffeine

### Week 4 — P1 Safety & P2 Quality
13. **P1-2/P1-3**: InterruptedException + Thread.sleep 修复
14. **P1-10/P1-11**: 前端 JWT 存储 + TypeScript strict
15. **P2-2/P2-3**: JaCoCo + 分布式追踪
16. **P2-4/P2-5**: React Testing Library + ErrorBoundary
