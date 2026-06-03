# Canvas Engine 代码审查报告 — 逻辑错误与隐藏 Bug

> 审查日期：2026-06-01 ~ 2026-06-02
> 审查范围：`backend/canvas-engine/src/main/java/` 全量
> 审查维度：并发安全、事务管理、Reactor 合规、空指针、边界条件、资源泄露、幂等性

---

## 审查概要

| 严重级别 | 数量 | 说明 |
|---------|------|------|
| P0 (严重) | 6 | 必须立即修复，影响数据正确性和系统稳定性 |
| P1 (重要) | 7 | 应尽快修复，存在性能瓶颈或边缘场景风险 |
| P2 (一般) | 4 | 建议修复，代码质量和可维护性 |

---

## P0 (严重)

### 1. @Transactional 缺少 rollbackFor = Exception.class

- **文件**: `domain/canvas/CanvasTransactionService.java`
- **行号**: 46, 92, 118, 141
- **问题**: 4 个事务方法 (`publishDb`, `offlineDb`, `killDb`, `archiveDb`) 使用裸 `@Transactional`，默认只回滚 `RuntimeException`。MyBatis 的 `PersistenceException` 等 checked exception 不会触发回滚，导致状态不一致。
- **影响**: 画布发布/下线/终止操作中途失败时，数据库状态部分更新，产生僵尸记录。
- **修复**: 全部改为 `@Transactional(rollbackFor = Exception.class)`

### 2. 事务内混入 Redis 路由注册——不可逆不一致

- **文件**: `domain/canvas/CanvasService.java`
- **行号**: ~189 及 publish 方法附近
- **问题**: 代码注释已承认：`"混在 @Transactional 内：若 DB 事务回滚，Redis 路由已变更，导致不可逆不一致"`，但未修复。publish 方法中 DB 事务和 Redis 路由注册/清理在同一事务边界内，DB 回滚后 Redis 状态无法回滚。
- **影响**: 画布发布失败后，路由表指向已回滚的版本，其他实例无法感知，执行静默失败。
- **修复**: 拆分事务——先完成 DB 事务（publishDb），提交后再注册 Redis 路由，失败时做补偿清理。

### 3. InFlightExecutionRegistry TOCTOU 竞态

- **文件**: `engine/trigger/InFlightExecutionRegistry.java`
- **行号**: 153-167
- **问题**: `deregister` 方法先 `map.isEmpty()` 检查再 `localRegistry.remove(canvasId)`，两步非原子。在检查和删除之间，另一线程 `tryAcquire` 可能插入新 entry，导致新注册的执行 `Disposable.Swap` 被误删。
- **影响**: Kill Switch (`cancelAll`) 对刚注册的执行失效，React 订阅无法被取消，资源泄露。
- **修复**: 使用 `localRegistry.computeIfPresent` 原子操作：
  ```java
  localRegistry.computeIfPresent(canvasId, (id, map) -> {
      map.remove(executionId);
      return map.isEmpty() ? null : map;
  });
  ```

### 4. 去重 TTL 配置竞态

- **文件**: `engine/trigger/CanvasExecutionService.java`
- **行号**: ~903-906
- **问题**: 代码有 `FIXME` 注释。恢复执行时去重 TTL 为 `globalTimeoutSec + 600`，但 `globalTimeoutSec` 从实时配置 `@Value` 读取。配置热更新后，不同时刻启动的执行使用不同 TTL，旧的短 TTL 可能提前过期，导致同一条 MQ 消息被重复执行。
- **影响**: DAG 路径重复执行，可能重复发送消息/调用外部接口。
- **修复**: 去重 TTL 应在执行创建时固定，存入 ExecutionContext 或从执行记录的 `globalTimeoutSec` 读取。

### 5. GroovyHandler 使用已废弃的 Executor API

- **文件**: `engine/handlers/GroovyHandler.java`
- **行号**: 58
- **问题**: `Executors.newVirtualThreadPerTaskExecutor()` 在 JDK 21+ 已标记废弃，应使用 `Executors.newUnboundedThreadPool()` 或 Spring 的 `ThreadPoolTaskExecutor`。
- **影响**: 未来 JDK 升级时可能移除该方法，编译失败。

### 6. 重试计数非原子——可能突破重试上限

- **文件**: `infrastructure/mq/OverflowRetryConsumer.java`
- **行号**: 75-80
- **问题**: `totalRetry = msg.getChainRetryCount() + message.getReconsumeTimes()` 计算后，该值直接传给 `disruptor.publishOverflowRetry()`。多线程并发消费同一条消息时，可能多个线程同时通过 `>= overflowMaxRetry` 检查后仍投递重试，突破重试上限约定。
- **影响**: 超过 maxRetry 的消息仍被重试，DLQ 写入与 Disruptor 投递不一致。
- **修复**: 使用 Redis `setIfAbsent` 做消息级去重，或数据库乐观锁。

---

## P1 (重要)

### 7. DLQ 写入 fire-and-forget——无完成保证

- **文件**: `engine/scheduler/DagEngine.java`
- **行号**: 560-562
- **问题**:
  ```java
  Mono.fromRunnable(() -> dlqMapper.insert(dlq))
      .subscribeOn(Schedulers.boundedElastic())
      .subscribe(null, e -> log.error(...));
  ```
  DLQ 写入是独立的 `subscribe()`，不链接到主 Reactive 链。boundedElastic 拒绝任务或 JVM 关闭时，DLQ 记录静默丢失。
- **影响**: 重试耗尽后失败记录丢失，无法手动回放。
- **修复**: 返回 Disposable 或 Mono，组合到主链（`Mono.when()` / `.then()`），或加 shutdown hook 排空。

### 8. GroovyShell 对象池阻塞——并发瓶颈

- **文件**: `engine/handlers/GroovyHandler.java`
- **行号**: 156
- **问题**: `shellPool.poll(100, TimeUnit.MILLISECONDS)` 在 boundedElastic 线程上阻塞等待。池大小仅 `availableProcessors() * 2`（通常 16-32），高并发 Groovy 节点执行时成为瓶颈。
- **影响**: 多个 boundedElastic 线程同时阻塞等 shell，降低所有 boundedElastic 工作并发度。
- **修复**: 增大 POOL_SIZE、改用非阻塞 take + CompletableFuture、或按需懒创建 GroovyShell。

### 9. 每次执行完成创建新虚拟线程——GC 压力

- **文件**: `engine/trigger/CanvasExecutionService.java`
- **行号**: ~1398
- **问题**:
  ```java
  Thread.ofVirtual().start(() -> {
      statsMapper.upsertDailyIncrement(...);
  });
  ```
  每次执行完成（SUCCESS/FAILED/PAUSED）都 spawn 新的虚拟线程。高吞吐场景（数千次/分钟）产生大量对象分配和 GC 压力。且无 shutdown guard。
- **修复**: 使用共享的 virtual-thread-backed ExecutorService，或批量累积后定时刷入。

### 10. ctx 恢复时版本不匹配

- **文件**: `engine/trigger/CanvasExecutionService.java`
- **行号**: ~645-648
- **问题**: 暂停执行恢复时，若 Redis ctx 过期，用当前发布版本重建 ExecutionContext。但画布可能已重新发布为新版本（节点/拓扑已变），导致 `findTriggerNode()` 失败或执行了错误的 DAG 拓扑。
- **影响**: 恢复执行失败或产生不正确的结果。
- **修复**: 将 versionId 持久化到单独的 Redis key（更长 TTL）或从执行记录的 DB 字段读取。

### 11. ExecutionContext 并发一致性

- **文件**: `engine/context/ExecutionContext.java`
- **行号**: 49-55
- **问题**: `flatContext` 和 `nodeOutputs` 虽然使用 `ConcurrentHashMap`，但多线程 `putNodeOutput` 时两者分开更新，存在最终一致性窗口。线程 A 更新了 `flatContext` 但尚未更新 `nodeOutputs`，线程 B 可能读到不一致的状态。
- **影响**: 分支汇聚节点可能读到不完整的上下文，导致逻辑判断错误。
- **修复**: 使用分段锁或 `AtomicReference` + 不可变 Map 快照。

### 12. MQ 消费线程内直接调 MyBatis 阻塞操作

- **文件**: `infrastructure/mq/MqTriggerConsumer.java`
- **行号**: 90-103
- **问题**: RocketMQ 消费线程（仅 20 个，ORDERLY 模式串行）内直接调用 `requestService.enqueue()` → MyBatis insert，阻塞 MQ 消费。20 线程全部阻塞在 DB 时，整个 MQ 消费停滞。
- **修复**: 将 DB 操作包装在 `subscribeOn(Schedulers.boundedElastic())` 中。

### 13. 外部 HTTP 调用缺少超时配置

- **文件**: 多个 Handler（ApiCallHandler 等）
- **问题**: Handler 中 Feign/WebClient 调用未显式设置 connectTimeout 和 readTimeout。
- **影响**: 外部服务故障时阻塞整条执行链路，直至全局超时（600s）。
- **修复**: WebClient 加 `.timeout(Duration.ofSeconds(30))`，Feign 配置 `Request.Options(5, TimeUnit.SECONDS, 30, TimeUnit.SECONDS)`。

---

## P2 (一般)

### 14. @Autowired 字段注入过时

- **文件**: `engine/handlers/MqTriggerHandler.java:27` 等多处
- **问题**: 使用 `@Autowired` 字段注入而非构造器注入，无法用 final 修饰，不利于单元测试。
- **修复**: 改用 `@RequiredArgsConstructor` + `private final` 构造器注入。

### 15. DAG 深度保护未检测循环引用

- **文件**: `engine/scheduler/DagEngine.java`
- **行号**: ~192
- **问题**: 有 `MAX_NODE_DEPTH = 200` 深度保护，但未检测 DAG 中是否存在循环引用。
- **影响**: 隐式循环的 DAG 会跑到深度上限才被拦截，浪费资源。
- **修复**: 拓扑排序或 BFS 遍历时维护 visited 集合检测循环。

### 16. 熔断器降级日志不完整

- **文件**: `engine/scheduler/DagEngine.java`
- **行号**: ~419-431
- **问题**: 熔断器 OPEN 时仅返回 `NodeResult.fail(e.getMessage())`，缺少降级策略记录和监控指标。
- **修复**: 添加 `metrics.recordCircuitBreakerOpen(nodeType)` 和结构化日志。

### 17. 全局缺少关键监控指标

- **问题**: Disruptor 溢出率、虚拟线程积压量、熔断器打开率、MQ 消息延迟等无 Prometheus 指标导出。
- **修复**: 接入 Micrometer，添加 `@Timed` 注解和 Gauge 指标。

---

## 修复路线图

| 阶段 | 范围 | 预估工时 |
|------|------|---------|
| 第一周 | P0 #1-#6 全部修复 | 2-3 人天 |
| 第二周 | P1 #7-#13 全部修复 | 3-5 人天 |
| 持续优化 | P2 #14-#17 | 2-3 人天 |

**总计**: 约 7-11 人天完成全部修复。
