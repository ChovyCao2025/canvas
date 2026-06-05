# 生产级设计问题清单

> 聚焦架构与设计层面：当前设计假设稳定的单进程 JVM，与生产环境（崩溃、滚动部署、资源争抢、多实例）的现实严重脱节。
> 排除安全相关，安全专项单独文档。
> 审查日期：2026-05-30

---

## ARCHITECTURAL — 技术栈选型与架构决策问题

### A. WebFlux + MyBatis-Plus：响应式与阻塞的根本矛盾 — CRITICAL

**当前选型：** `spring-boot-starter-webflux`（响应式 HTTP）+ `mybatis-plus-spring-boot3-starter`（阻塞 JDBC）。全项目 37 处用 `Schedulers.boundedElastic()` 桥接阻塞 DB 调用到响应式链。

**为什么不合适：** WebFlux 的事件循环模型假设非阻塞 I/O。每个 `boundedElastic` 包装意味着从有限线程池（默认 10×CPU 核数）中占一个线程执行 JDBC 调用。高负载下该线程池饱和，整个响应式管线停顿。HikariCP 最大 33 连接，但 `boundedElastic` 池才是真正的瓶颈。代码从 WebFlux 获得零吞吐优势，却承担了 Mono/Flux 组合的全部复杂度。

**应该选：** Spring MVC + Java 21 虚拟线程。每个 HTTP 请求运行在虚拟线程上，阻塞 JDBC 调用天然免费，无需 `boundedElastic` 包装。代码量预计减少 30-40%，调试难度大幅降低。

**迁移难度：** Hard — 每个 `Mono.fromCallable().subscribeOn(boundedElastic)` 需要拆解，所有 Controller 返回类型从 `Mono<T>` 改为 `T`，且与问题 B（DagEngine Reactor 模型）深度纠缠。

---

### B. Reactor/Mono 编排 DAG 执行：用错了抽象 — CRITICAL

**当前选型：** 整个 DAG 执行模型表达为递归 `Mono<Map<String, Object>>` 链。`executeNode()` 通过 `flatMap` 递归调用自身，5+ 层 reactive operator 嵌套。

**为什么不合适：** DAG 调度器本质是命令式状态机："节点 A 完成 → 检查 B 的依赖 → 调度 B"。用递归 Mono 链表达导致：
1. **堆栈信息无用** — Reactor 内部堆栈与业务逻辑无关
2. **调试需理解 Reactor 订阅机制** — 新人基本无法调试
3. **repeat 机制 90 行**（DagEngine:413-501）— 纯粹是为了绕过 Reactor 的 `flatMap` 不天然支持"重新执行"的问题
4. **`MAX_NODE_DEPTH = 200`** — 因为递归 Mono 链可能爆 Reactor 内部栈
5. **`Mono.delay().subscribe()` 的 fire-and-forget** — 响应式编程中的反模式，生命周期管理脆弱

**应该选：** 显式状态机或步进式执行器。维护 `Deque<PendingNode>` 工作队列，迭代式处理：弹出就绪节点 → 执行 → 检查哪些下游节点解锁 → 入队。Airflow、Temporal、Camunda 都是这样工作的。配合虚拟线程，每个节点执行就是简单的阻塞调用。1539 行的 DagEngine 可以缩到 ~400 行可读的命令式代码。

**迁移难度：** Hard — DagEngine 是系统核心，所有 handler、测试、调用者都绑定 `Mono<NodeResult>` 类型。与问题 A 深度耦合，必须同步迁移。

> **A 和 B 形成互锁陷阱**：WebFlux 逼迫 DagEngine 用 Reactor，Reactor 模型又让脱离 WebFlux 变得不可能。推荐路径：**同步迁移到 Spring MVC + 虚拟线程 + 命令式 DAG 引擎**，一次性消除根因。

---

### C. 单体服务：流量模式根本不同却共享 JVM — HIGH

**当前选型：** 一个 `canvas-engine` 服务承担：29 个 API Controller、触发器解析、DAG 执行、Handler 分发、投递、人群计算、Wait 管理、版本管理、MQ 消费、调度、WebSocket 通知、DLQ 管理。

**为什么不合适：** 不同模块的流量特征截然不同：
- **API 服务**：低 QPS，用户面向，需要低延迟
- **MQ 消费**：高 QPS，突发，需要吞吐量
- **定时触发**：周期性，批处理，可长时间运行
- **人群计算**：CPU 密集型，批处理，可能耗时数分钟，还创建临时 DataSource 做全表扫描
- **投递**：外部 HTTP 调用，延迟不确定，需要重试隔离

人群批量计算（`AudienceBatchComputeService.java:163-197` 创建临时 DataSource 做全表扫描）与毫秒级实时事件触发跑在同一个 JVM 里，是资源争抢风险。一个慢的人群计算可以饿死 `boundedElastic` 线程池，拖停所有 DAG 执行。

**应该选：** 至少拆为 (1) Canvas API Service（CRUD + 版本管理）、(2) Canvas Engine Service（DAG 执行 + 触发 + 投递）、(3) Audience Compute Service（批量人群计算）。MQ 消费者可随 Engine 或独立。

**迁移难度：** Medium — 代码已按 package 组织良好，主要挑战是解耦共享 DB 访问和 `CanvasExecutionService` 依赖图。

---

### D. LMAX Disruptor 做任务分发：杀鸡用牛刀 — MEDIUM

**当前选型：** LMAX Disruptor，65536 slot ring buffer，`YieldingWaitStrategy`，WorkerPool 消费者。

**为什么不合适：** Disruptor 为纳秒级超低延迟事件处理设计（如金融交易）。Canvas 引擎处理的营销触发每次耗时毫秒到秒级。`YieldingWaitStrategy` 自旋等事件，纯 CPU 浪费。Ring buffer 的"背压"（满时抛 `InsufficientCapacityException`）还不如 `BlockingQueue` 的阻塞等待——调用方必须自己实现外部重试。事件对象复用（`event.reset()`）引入了 stale data 泄漏到下一个事件的风险。

**应该选：** `ThreadPoolExecutor` + bounded `LinkedBlockingQueue`，或 Java 21 虚拟线程 `Executors.newVirtualThreadPerTaskExecutor()` + `Semaphore` 限流。天然背压，无对象复用 bug，代码简单。

**迁移难度：** Easy — `CanvasDisruptorService` 是单一类，接口清晰，替换 `publish()` 为 `executor.submit()` 即可。

---

### E. Groovy 做生产脚本引擎：沙箱不安全 + Metaspace 泄漏 — HIGH

**当前选型：** Groovy 4.0.21 + `SecureASTCustomizer` 沙箱，`GroovyShell` 对象池，虚拟线程执行，5 秒超时。

**为什么不合适：**
1. **`SecureASTCustomizer` 不是安全沙箱** — 工作在 AST 层面，有已知绕过手段。阻挡 `Runtime.exec()` 但无法阻止反射逃逸、通过允许的类操作 ClassLoader、间接路径访问 `java.lang.System`。`setIndirectImportCheckEnabled(true)` 本身就是承认基础白名单不够。
2. **ClassLoader 泄漏** — 每个 `GroovyShell` 编译产生新类。即使有缓存和池，脚本编辑重发布后旧类堆积在 Metaspace。`evictCache()` 只清缓存 Map，无法卸载已加载的类。长期运行 → Metaspace OOM。
3. **超时不可靠** — `Future.cancel(true)` 对虚拟线程发中断，但 Groovy 脚本可捕获 `InterruptedException` 或跑 CPU 死循环不检查中断标志。
4. **冷启动延迟** — 新脚本首次编译 50-200ms。

**应该选：** 简单条件逻辑用已有的 `QLExpress` 或 `Aviator`（pom.xml 已有依赖），两者是表达式求值器而非完整脚本语言，攻击面小得多。复杂逻辑考虑 GraalVM polyglot 隔离进程，或迁移到可配置规则引擎。

**迁移难度：** Medium — `NodeHandler` 接口干净，替换 GroovyHandler 不难。难点是迁移存量用户脚本。

---

### F. 单 RocketMQ Topic 承载所有触发 — MEDIUM

**当前选型：** 所有 MQ 触发器共用 `CANVAS_MQ_TRIGGER` 单一 Topic，用 tag 区分。

**为什么不合适：** 不同触发类型的流量特征差异巨大：
- **定时触发**：周期性、可预测、cron 边界突发
- **实时事件触发**：不可预测、可能病毒式传播（如秒杀活动）
- **SEND_MQ 节点输出**：业务发起、中等流量
- **DLQ 重放**：手动、低流量、不应与生产流量竞争

共享 Topic 导致：定时触发洪峰填满消费线程池（20 线程）→ 延迟实时事件；无法按触发类型设不同重试策略；无法独立监控/消费某种触发类型；`ConsumeMode.ORDERLY` 对所有消息生效，包括不需要顺序的消息，降低吞吐。

**应该选：** 按触发类别拆分 Topic：`CANVAS_TRIGGER_SCHEDULED`、`CANVAS_TRIGGER_EVENT`、`CANVAS_TRIGGER_MQ`、`CANVAS_TRIGGER_DLQ_REPLAY`。各自独立的消费组、线程数、重试策略和监控。

**迁移难度：** Medium — 需创建 Topic、重组消费组、更新 Consumer 和 Handler，无架构变更。

---

### G. RoaringBitmap 人群包：哈希碰撞误触达 — HIGH

**当前选型：** RoaringBitmap 存 Redis（Base64 编码），用 murmur3_32 将 String userId 映射为 int 索引。

**为什么不合适：**
1. **哈希碰撞 → 误触达** — murmur3_32 将无限 String 空间映射到 2^32 int。1 亿用户下碰撞率 ~1.16%，意味着错误地将不属该人群的用户纳入 → 发错营销消息（合规风险）。
2. **单用户检查需全量反序列化** — `isMember()` 从 Redis 加载整个 bitmap 到内存再检查一个 bit。大人群（百万级）= 每次检查加载数 MB 数据。
3. **Base64 编码浪费 33% 存储** — Redis 支持二进制安全 String，无需 Base64。
4. **不支持运行时集合运算** — "人群 A AND NOT 人群 B" 需要加载两个完整 bitmap 做 AND，无法利用 Redis 原生 BITOP。

**应该选：** 用确定性 userId-to-integer 映射（如 Redis INCR 自增或 DB 序列）替代哈希，消除碰撞。单用户检查用 Redis `GETBIT`（O(1)，零反序列化）。集合运算用 Redis `BITOP AND/OR/XOR`，无需在应用层反序列化。

**迁移难度：** Medium — `AudienceBitmapStore` 接口干净，但改映射方式需重算所有存量 bitmap。

---

### H. MySQL 存执行轨迹：OLTP 库扛时序数据 — MEDIUM

**当前选型：** MySQL 8.0 存储所有数据：画布定义、执行轨迹、DLQ、人群定义、发送记录等。

**为什么不合适：** 执行轨迹数据是高吞吐追加写入的时序数据。一个 20 节点画布处理 100 万用户 → 4000 万条 trace。MySQL 优化于 OLTP，不适合高吞吐追加写入：
1. 表膨胀 → 索引退化 → 查询变慢
2. trace 查询与 OLTP 操作（画布 CRUD、执行状态更新）共享同一个 DB → 互相影响
3. `TraceWriteBuffer` 的存在本身就是症状 — 因为 MySQL 扛不住写入量才做了 50K 缓冲 + 丢数据降级

**应该选：** 执行轨迹用 ClickHouse 或 TimescaleDB，10-100x 写入吞吐 + 压缩比。MySQL 保留给 OLTP 数据。`TraceWriteBuffer` 已经是抽象层，加一个 sink 不难。

**迁移难度：** Medium — TraceWriteBuffer 已有抽象，加数据 sink 简单。难点是迁移历史数据和更新 trace 查询 API。

---

### I. React Flow 做工作流编辑器：能画图但不懂工作流 — MEDIUM

**当前选型：** `@xyflow/react` v12.3.6 + `@dagrejs/dagre` 自动布局。

**为什么不合适：** React Flow 是通用节点图编辑器，不是工作流编辑器。营销画布需要的：
1. **30+ 自定义节点类型** — 各有独立配置面板、校验规则、视觉表达，在 React Flow 节点内管理很笨重
2. **复杂 DAG 边路由** — 50+ 节点的条件分支、循环、跳转边，React Flow 默认边路由（bezier/step）处理不了密集 DAG 的边交叉/重叠
3. **大画布性能** — React Flow 任何变更都重渲染所有可见节点，100+ 节点时 UI 卡顿
4. **无工作流语义** — 无内置节点类型校验（"触发节点必须是第一个"）、边约束（"条件节点必须有两条出边"）、工作流级校验（"每条路径必须到达终止节点"），全部需从零实现

**应该选：** 考虑 `@antv/x6`（与 antd 生态一致，复杂边路由和节点定制能力更强），或基于 HTML5 Canvas/SVG 自研以获得最大控制力。

**迁移难度：** Hard — 前端 DAG 编辑器与 React Flow API 深度绑定，迁移需重写整个画布编辑器组件。

---

### J. 无投递队列：引擎直连触达平台 — HIGH

**当前选型：** 引擎通过 `WebClient.post()` 直接调用触达平台 HTTP 接口。DAG 执行同步等待投递结果才继续下一个节点。

**为什么不合适：**
1. **投递无保障** — 引擎崩溃在触达平台接受请求后、DB 更新前 → 消息已发但未记录
2. **同步耦合** — 触达平台慢（5s 超时）→ 整个 DAG 执行阻塞。3000 并发 × 5s = 15000 个 WebClient 连接，远超连接池上限（500）
3. **重试不隔离** — 触达平台宕机 → DAG 引擎 retry → 占用执行 slot → 其他画布被挤占
4. **无送达回执** — `MessageSendRecordDO` 只记录"已发/失败"，不追踪实际送达状态（SMS 已送达、Push 已打开等）

**应该选：** 引入投递队列（RocketMQ Topic `CANVAS_DELIVERY`）。引擎发布投递请求到队列后立即推进到下一个 DAG 节点。独立消费端处理实际 HTTP 调用、重试和回执。解耦引擎与触达平台的延迟和可用性。

**迁移难度：** Medium — `ReachDeliveryService` 已是独立组件，加 RocketMQ producer 简单。难点是处理 DAG 中"等待投递结果再继续"的语义。

---

## CRITICAL — 设计缺陷，不修则生产不可用

### 1. DAG 执行状态无增量持久化 — 崩溃即丢失

**当前设计：** DagEngine 是纯内存 Reactor Mono 驱动的执行引擎。ExecutionContext 全程驻留 JVM 内存，仅在执行完成或暂停时写入 Redis（`ctxStore.save(ctx)`）。NodeGate 锁（`AtomicBoolean`）、Hub/LogicRelation 超时定时器（`Mono.delay()`）、`scheduledHubTimeouts` 集合均为内存对象，`@JsonIgnore` 不参与序列化。

**生产现实：** 进程崩溃/重启/滚动部署时，两个节点之间所有中间状态丢失。Redis 中的 context 反映的是上次暂停点的快照，不是崩溃点。恢复后：
- 已完成但有副作用的节点（发券、发短信）可能被重新执行 → 资损
- NodeGate 全部重建为 `false` → 正在执行的节点被视为"未执行"，触发重复执行
- 超时定时器消失 → 依赖超时恢复的 Hub/Wait 节点永久 WAITING

**修复方向：** 每个节点执行完成后，将 `nodeStatuses` + `nodeOutputs` 增量持久化到 Redis；NodeGate 和超时定时器需要可序列化的替代方案（如 Redis sorted set 做延迟队列）。

**涉及文件：** `DagEngine.java:169-866`, `ExecutionContext.java:83,102`, `ContextPersistenceService.java`

---

### 2. 受众用户全量加载进内存 — 大人群 OOM

**当前设计：** AudienceUserResolver 的 `toUid()` 方法通过 JDBC 查询将所有匹配用户 ID 加载进 `ArrayList<String>`。CDP 路径同样返回 `List<String>`。无分页、无流式处理。

**生产现实：** 500 万用户的人群包 → ArrayList 占 ~200MB+ 堆内存。3000 并发执行上限下，多个大人群包同时解析可轻松耗尽 JVM 堆。而且 `toUid()` 用 murmur3_32 将 userId 映射为 32 位 int，500 万用户下碰撞率约 0.5%，导致人群包错乱。

**修复方向：** 人群解析改为流式/分页，直接走 RoaringBitmap 运算而非中间 List；fan-out 触发也需分批限流，避免瞬间打满 Disruptor ring buffer。

**涉及文件：** `AudienceUserResolver.java:42-47,61-68`, `AudienceBatchComputeService.java:210-221`

---

### 3. Wait 节点状态无法跨部署存活 — 滚动部署=丢失等待

**当前设计：** Wait 节点的订阅状态持久化到 MySQL（`CanvasWaitSubscriptionDO`），但超时定时器是 DagEngine 内存的 `Mono.delay()` 订阅。恢复锁 TTL = `globalTimeoutSec`（默认 600s），而 Wait 节点可能等待数小时甚至数天。

**生产现实：**
- 滚动部署时，内存中的超时定时器全部消失。新实例没有机制重建这些定时器。
- 恢复锁 10 分钟过期，Wait 节点等 3 天 → 锁过期后同一用户可触发第二次执行，产生重复触达。
- WAIT 恢复仍走全量 trigger 路径（含额度检查），代码注释承认是已知问题。

**修复方向：** 所有定时器外部化到 Redis sorted set 或 DB + polling；恢复锁 TTL 需绑定 Wait 实际过期时间；WAIT 恢复需短路跳过额度/去重检查。

**涉及文件：** `DagEngine.java:634-636,723-724,804`, `WaitSubscriptionService.java:162-194`, `WaitResumeService.java:270-292`, `ContextPersistenceService.java:119`

---

## HIGH — 设计与生产现实有显著差距

### 4. 执行上下文内存无强制上限 — 无背压

**当前设计：** `ExecutionContext` 有 1MB 的 `approxSizeBytes` 估算和 `isOversized()` 方法，但 DagEngine 从不检查 `isOversized()`。`putNodeOutput()` 始终接受数据。大小估算用 `k.length() + v.toString().length()`，未考虑嵌套对象开销。`flatContext` 的 key 无命名空间隔离，后执行的节点输出会静默覆盖同名 key。

**生产现实：** 1MB 限制形同虚设。3000 并发 × 超 1MB context = 3GB+ JVM 内存仅用于 ExecutionContext。API 调用返回大 JSON、人群包列表等场景可轻松突破限制。key 碰撞导致节点间数据污染。

**修复方向：** `isOversized()` 必须在 `putNodeOutput` 入口强制检查并拒绝/降级；key 加命名空间前缀（如 `nodeId.keyName`）；大小估算需考虑序列化后实际大小。

**涉及文件：** `ExecutionContext.java:106-147`

---

### 5. Handler 无系统级幂等框架 — 重试=重复副效果

**当前设计：** DagEngine 的 repeat 机制可让同一 handler 执行两次，retry 机制可让 handler 执行 maxRetry+1 次。但无系统级幂等 key 或 token 传递给 handler。各 handler 需独立实现幂等（如 ReachDeliveryService 用 idempotencyKey），但无统一保证。handler 执行失败后可能已部分写入 ctx（如 `ctx.putNodeOutput` 在 handler 内部），污染下游判断逻辑。

**生产现实：** 未实现幂等的 handler（如发券、发 MQ、调 API）在 retry/repeat 时产生重复副效果。部分失败 + ctx 污染可能导致"防资损"逻辑误判。

**修复方向：** 引入系统级 `idempotencyKey = executionId + nodeId + attemptCount`，在 handler 执行前持久化，执行后标记完成；ctx 写入应在 handler 成功后原子性提交，而非边执行边写。

**涉及文件：** `DagEngine.java:424,446-453,470-498`, 各 handler 实现

---

### 6. 熔断器 JVM 本地 — 多实例行为不一致

**当前设计：** `CircuitBreakerRegistry` 是 JVM 本地 ConcurrentHashMap。实例 A 的熔断器可能 OPEN（拒绝请求），实例 B 的仍 CLOSED（放行请求）。同一用户连续两次请求可能走不同实例，得到不同结果。

**生产现实：** 多实例部署下熔断形同虚设。一个实例因外部服务故障打开熔断，流量自动转到其他实例，其他实例未熔断继续放行，反而加重外部服务压力——熔断保护失效。

**修复方向：** 熔断状态存 Redis，用 Lua 脚本保证原子性；或接受 JVM 本地熔断 + 全局限流兜底的组合策略。

**涉及文件：** `CircuitBreakerRegistry.java:36,97-143`

---

### 7. 投递无出站队列 — 渠道不可达=消息丢失

**当前设计：** ReachDeliveryService 调用触达平台，失败后标记 FAILED。DagEngine 级别的 retry 会重试整个节点（非仅投递），DLQ 捕获整个执行失败而非单条投递。PENDING 记录在进程崩溃后无 reconciliation 机制扫描。

**生产现实：** 触达平台宕机 → 投递立即失败 → 重试 3 次仍失败 → DLQ → 无人处理。PENDING 记录可能永久停留（进程崩溃在调用成功后、标记 SENT 前）。无 outbound queue/outbox pattern 缓冲消息。

**修复方向：** 引入 outbox pattern，投递请求先写 DB 再异步发送；PENDING 记录需定时 reconciliation 扫描；DLQ 需要人工/自动 re-delivery 机制。

**涉及文件：** `ReachDeliveryService.java:86-136`, `DagEngine.java:537-565`

---

### 8. 触发器版本未锁定 — 编辑/发布窗口期执行不一致

**当前设计：** 触发时从 Caffeine L1 缓存获取画布图结构（`configCache.get(canvasId, ctx.getVersionId())`），但版本解析和图加载之间无锁。画布编辑/重新发布可能导致版本切换、缓存淘汰。Wait 恢复时如原版本被删除，缓存返回 null 导致空指针。

**生产现实：** 重新发布画布时，正在执行/等待恢复的旧版本图可能被淘汰。恢复执行加载到新版本图结构 → DAG 拓扑变化 → 路由逻辑变化 → 执行结果不可预测。

**修复方向：** 触发时锁定版本，执行全程使用已锁定的版本；旧版本图需保留至所有引用它的执行完成；Wait 恢复需校验版本一致性。

**涉及文件：** `CanvasExecutionService.java:558,663,1119-1147`

---

### 9. 单画布可饿死所有车道 — 无租户隔离

**当前设计：** 车道限制按类型配置（light/standard/heavy/retry），但无画布维度的准入配额。热门画布（如秒杀活动）可占满 STANDARD 车道所有 slot，阻塞其他所有画布。Disruptor ring buffer 是全局共享的，一种触发类型洪峰会挤占全部 buffer。

**生产现实：** 一个爆款活动画布的流量尖峰可以导致整个引擎不可用，所有其他画布的执行请求排队超时。overflow retry 链在持续过载下本身也会成为瓶颈。

**修复方向：** 增加画布维度 admission rate limit；Disruptor 按优先级或类型分 ring buffer；overflow retry 需要总次数上限 + 终极 DLQ 保证不丢。

**涉及文件：** `CanvasDisruptorService.java:55,57`, `CanvasExecutionService.java:979-1262`, `InFlightExecutionRegistry.java:73-76`

---

## HIGH — 前端架构设计问题

### 10. 边数据双源真相 — React Flow Edges vs bizConfig

**当前设计：** 边同时存在于两个地方：React Flow 的 `useEdgesState` 管理的边数组，以及每个节点的 `bizConfig` 中的出口引用字段（如 `yesNodeId`、`noNodeId`）。`deriveEdges()` 在加载时从后端节点重建边，之后每次修改需手动保持两处同步。

**生产现实：** 双源真相是 bug 温床。任何修改边的操作（连接、断开、条件分支变更）都需同时更新两处，遗漏任何一处即产生界面与数据不一致。当前 undo/redo 用浅拷贝 `[...nodes]`，节点对象仍是同一引用 — 修改 `node.data.bizConfig` 会静默篡改历史快照。

**修复方向：** 以 `bizConfig` 为唯一真相源，React Flow 的边通过 `useMemo` 从 bizConfig 派生；undo/redo 需深拷贝或使用 immutable 数据结构。

**涉及文件：** `canvas-editor/index.tsx:283-318`, `outletRouting.ts:213`, 各处边同步代码

---

### 11. 编辑器无实时更新 — 后端变更不可见

**当前设计：** 画布编辑器仅在挂载时从服务端加载一次数据（`canvasApi.get(canvasId)`），之后无任何 WebSocket/SSE/轮询机制。执行状态变化、金丝雀部署结果、其他人编辑 — 全部不可见，必须手动刷新页面。

**生产现实：** 发布画布后无法看到执行状态更新；两人同时编辑画布只有 409 冲突时才知道；金丝雀推广/回滚后需 `window.location.reload()` 丢失全部内存状态。

**修复方向：** 增加执行事件的 SSE/WebSocket 推送通道；并发编辑至少需要 operational transform 或 CRDT 意识；409 冲突改为 diff/merge UI 而非强制刷新。

**涉及文件：** `canvas-editor/index.tsx:536-647`, `App.tsx:86`

---

### 12. 保存流程有潜在无限循环 — 无重试、有 while(true)

**当前设计：** `handleSave` 用 `savingPromiseRef` 合并并发保存，核心逻辑是 `while(true)` 循环：如果保存期间快照变化则重新保存。自动保存失败仅弹 `message.error`，不重试。冲突时弹窗让用户选择强制刷新（丢失本地编辑）。

**生产现实：** 用户持续快速编辑快于保存速度 → while 循环无限运行。网络故障 → 自动保存失败 → 仅提示用户 → 3 秒后重试 → 又失败 → 中间编辑仅存内存。409 冲突的处理是破坏性的（丢失一方全部工作）。

**修复方向：** `while(true)` 循环加最大迭代次数；保存失败加入指数退避重试；409 冲突提供 diff/merge UI 而非二选一。

**涉及文件：** `canvas-editor/index.tsx:1060-1142`

---

### 13. 前端状态架构 — 无外部 Store、组件不可测试

**当前设计：** `EditorInner` 组件 2075 行，包含 26 个 `useState`，所有状态/逻辑/UI 混在一起。无外部状态管理（Zustand/Jotai/Redux）。仅抽取了纯函数文件，组件本身无法测试。

**生产现实：** 任何状态变更触发整个编辑器重渲染（包括所有内联的 Modal/Drawer）。选一个节点 → `setSelectedNodeId` → 全部 26 个 useState 重渲染 → 所有节点组件重渲染（`displayNodes` 每次 `.filter()` 生成新引用，破坏 `memo()`）。Dagre 布局同步执行在主线程，100+ 节点画布卡顿数百毫秒。

**修复方向：** 提取 `useCanvasEditor` hook 封装状态+逻辑；节点选择等高频状态用 Zustand 细粒度订阅；Dagre 布局移入 Web Worker；Modal/Drawer 懒加载。

**涉及文件：** `canvas-editor/index.tsx` 全文, `CanvasNode.tsx:76`

---

### 14. API 层缺请求取消、去重、重试

**当前设计：** `api.ts` 用裸 `axios.create({ baseURL: '/' })`，无 AbortController、无 cancel token、无请求去重、无重试。每次请求拦截器从 localStorage 读 token。config-panel 模块级用 `Map` 做缓存，无 TTL、无淘汰、无失效。

**生产现实：** 组件卸载后 API 回调仍更新状态 → 内存泄漏/警告。同一数据（如 `metaApi.getNodeTypes()`）被多个组件并发调用 → 重复网络请求。瞬时网络抖动 → 自动保存失败 → 用户工作丢失。后端 schema 变化 → config-panel 缓存返回过期数据。

**修复方向：** useEffect 清理函数中 abort 请求；引入请求去重（相同 key 的并发请求合并）；自动保存加入 retry with backoff；config-panel 缓存加 TTL 和版本化失效。

**涉及文件：** `services/api.ts`, `config-panel/index.tsx:64-68`

---

### 15. 类型安全形同虚设 — 零运行时校验

**当前设计：** `BizConfig` 有 25+ 可选 `*NodeId` 字段 + `[key: string]: unknown` 索引签名。`CanvasNodeData` 同样有 `[key: string]: unknown`。无运行时校验库（Zod/Yup/io-ts）。后端 `config` 是 `Record<string, unknown>` 而前端是 `BizConfig`，命名不一致且需手动转换。`JSON.parse(graphJson || '{"nodes":[]}').nodes` 无类型断言。

**生产现实：** 后端 schema 变更 → 前端 `undefined` 访问无感知 → 运行时崩溃。`bizConfig.sucessNodeId`（typo）编译通过 → 静默 bug。无前后端类型契约 → 改一端另一端必破。

**修复方向：** 引入 Zod 做 API 响应运行时校验；从 OpenAPI spec 自动生成类型；去除 index signature，用 discriminated union 替代。

**涉及文件：** `types/canvas.ts:128,153`, `types/index.ts:190`, `canvas-editor/index.tsx:548,575`

---

## MEDIUM — 缓存与审计设计不足

### 16. 缓存失效不保证 — L1 可能长期脏

**当前设计：** TieredCacheImpl 的 L1 失效依赖 MQ 异步发布事件。MQ 临时不可用 → 失效事件丢失 → 其他实例 L1 缓存脏数据直到 Caffeine TTL 过期。`safeWrite` 用 `Thread.sleep` 做双删延迟，阻塞调用线程。`staleValues` 是 JVM 本地 ConcurrentHashMap，各实例降级结果不一致。

**修复方向：** MQ 不可用时降级为同步 Redis 版本校验（已有 version 机制但每次 L1 hit 都多一次 Redis RT）；`safeWrite` 改用异步延迟任务；stale value 可考虑接受（降级一致性）。

**涉及文件：** `TieredCacheImpl.java:456-471,929-941,1044-1048,1362-1369`

---

### 17. 审计轨迹高负载不可靠 — Trace 缓冲区溢出丢数据

**当前设计：** TraceWriteBuffer 50,000 条容量，500ms 刷新周期，每周期最多处理 4000 条（20 批 × 200）。batch insert 失败仅 log，不重试。Spring `@Scheduled` 共享单线程调度器，可能被其他任务延迟。

**生产现实：** 3000 并发 × 5 条 trace/执行 = 15,000 条/秒。缓冲区秒级填满，大多数 trace 被静默丢弃。审计数据不可靠，故障排查无据可依。Context 保存/恢复等关键事件完全未 trace。

**修复方向：** 刷新线程独立化，不依赖 Spring scheduler；增加 backpressure 机制（阻塞或降级为采样）；关键事件（ctx save/resume）必须 trace；考虑异步 MQ 写入替代直接 DB 写。

**涉及文件：** `TraceWriteBuffer.java:32,45-56,59-69,119-127`

---

## 根因总结

| 根因 | 影响范围 | 核心修复 |
|------|---------|---------|
| **关键状态过度驻留内存** | #1, #3, #5, #6, #7, #8 | 增量持久化 + 外部化定时器 |
| **无租户/画布级隔离** | #2, #9 | 画布级 rate limit + 分 ring buffer |
| **前端双源真相 + 无 Store** | #10, #11, #12, #13 | bizConfig 为唯一源 + Zustand |
| **前后端无类型契约** | #14, #15 | Zod 校验 + OpenAPI 类型生成 |

> 一句话：当前系统假设"进程永生、单实例运行、内存无限"，这三条在生产环境全部不成立。

---

## 技术栈选型问题汇总

| # | 问题 | 当前选型 | 严重度 | 迁移难度 |
|---|------|---------|--------|---------|
| A | WebFlux + MyBatis-Plus 互斥 | 响应式 HTTP + 阻塞 JDBC + 37 处 boundedElastic 桥接 | **Critical** | Hard |
| B | Reactor 编排 DAG 执行 | 递归 Mono 链表达状态机 | **Critical** | Hard |
| C | 单体服务扛异质流量 | API + 引擎 + 人群 + 投递 共享 JVM | **High** | Medium |
| D | Disruptor 做任务分发 | 纳金融级组件用于毫秒级营销触发 | **Medium** | Easy |
| E | Groovy 做脚本引擎 | SecureASTCustomizer 非真沙箱 + Metaspace 泄漏 | **High** | Medium |
| F | 单 MQ Topic 承全触发 | 定时/事件/投递/DLQ 共享 Topic | **Medium** | Medium |
| G | RoaringBitmap 哈希碰撞 | murmur3_32 → 1 亿用户 1.16% 误触达率 | **High** | Medium |
| H | MySQL 存时序轨迹 | OLTP 库扛高吞吐追加写入 | **Medium** | Medium |
| I | React Flow 做工作流编辑 | 通用图编辑器 ≠ 工作流编辑器 | **Medium** | Hard |
| J | 无投递队列 | 引擎直连触达平台，同步等待 | **High** | Medium |

> **A 和 B 形成互锁陷阱**：WebFlux 逼迫 DagEngine 用 Reactor，Reactor 模型又让脱离 WebFlux 变得不可能。推荐路径：**同步迁移到 Spring MVC + 虚拟线程 + 命令式 DAG 引擎**，一次性消除根因。