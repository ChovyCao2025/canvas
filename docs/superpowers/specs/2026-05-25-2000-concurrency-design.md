# 3000 Execution 并发目标 Design Spec

> 文件名保留 `2000-concurrency` 是为了沿用既有规划链路；本文档的正式目标已升级为 **3000 stable execution concurrency**。

## 1. 背景

当前 Canvas 执行引擎已经具备继续上探的基础：
- WebFlux 入口层可以承接较高连接压力。
- RocketMQ + Disruptor 已经具备削峰和异步分发能力。
- `InFlightExecutionRegistry` 已使用 Redis ZSET 做跨实例 in-flight 注册，具备全局执行槽位的基础能力。
- `CanvasExecutionService` 已有 dedup、resume lock、overflow retry、request retry、DLQ 等执行治理机制。

但 3000 execution 并发不是把 `canvas.execution.max-concurrency` 改成 3000 就能稳定达成。3000 目标下，系统瓶颈会从单点资源不足转为平台边界不清：
- 轻任务、普通任务、重任务、重试流量共享执行预算，容量不可预测。
- retry / overflow 在高峰时可能形成自激增，反向冲击正常流量。
- Audience、Groovy、大 replay、大批量外部调用会拖慢主链路。
- Redis 同时承担 ctx、dedup、lock、quota、route、rate limit、in-flight 等职责，热点互相影响。
- MySQL 写入、执行记录、统计写入、审计写入需要进一步区分在线强依赖和弱在线数据。
- 下游长尾会在 3000 并发下放大，必须有连接池、超时、熔断、限流和容量契约。

因此，本设计采用 **Global Budget + Lane Budget + Queue Isolation** 的分层演进方案：保留现有 WebFlux + RocketMQ + Disruptor + DAG 主结构，优先把代码可控制的执行准入、lane 隔离、重试隔离、重任务隔离和指标补齐，再用压测与基础设施门槛证明 3000 稳定能力。

---

## 2. 目标定义

### 2.1 总目标

建设一套支持 **3000 stable execution concurrency** 的 Canvas 执行平台能力。

### 2.2 并发口径

本文档中的 3000 并发指：
- 集群级同时执行中的画布 execution 数。
- 不是单机 execution 并发。
- 不是 HTTP 连接数。
- 不是 MQ 堆积数。
- 不是总客户数或 DAU。
- 不是离线任务吞吐量。

### 2.3 目标边界

代码侧必须先完成：
- 全局并发预算。
- lane 级并发预算。
- Redis ZSET 原子准入扩展。
- normal / retry / heavy 队列隔离。
- retry backoff 和 DLQ 上限。
- heavy path 隔离。
- lane 级指标和压测门槛。

基础设施侧必须同步满足：
- MySQL 连接数、慢 SQL、热点表写入能力满足压测门槛。
- Redis 延迟、连接数、热点 key、内存、CPU 满足压测门槛。
- RocketMQ broker、consumer group、topic backlog 满足压测门槛。
- 下游服务能承受 3000 并发传导或具备明确限流降级。
- 应用实例数、JVM 参数、容器资源经过压测确认。

### 2.4 成功标准

1. 在约定链路模型下达到 3000 execution 并发，并稳定运行完整观察窗口。
2. `LIGHT` / `STANDARD` 主链路不会被 `HEAVY` / `RETRY` 拖垮。
3. Redis / MySQL / RocketMQ / 下游没有持续性饱和。
4. backlog 不持续增长，retry 不形成自激增。
5. admission reject、overflow、DLQ、timeout 都在预设阈值内。
6. 上线过程有明确灰度、回滚和降级条件。

---

## 3. 当前边界

### 3.1 已具备的基础能力

当前系统不是只能做到 500 或 1000，已有基础包括：
- 入口层是 WebFlux。
- 异步触发通过 RocketMQ 和 Disruptor 削峰。
- 执行服务已有 dedup、resume lock、overflow retry、persistent request retry。
- `InFlightExecutionRegistry` 已经使用 Redis ZSET 做 canvas 维度和 global 维度的原子槽位获取。
- 全局并发配置已有启动一致性校验，避免多实例配置不一致。
- 执行请求表已有 running heartbeat、stale running 回收和重试退避。

这些能力可以作为 3000 改造的基础，而不是推翻重建。

### 3.2 代码侧缺口

要稳定承担 3000，代码侧还缺：
- `ExecutionLane` 只有规划，没有成为执行准入的核心维度。
- `InFlightExecutionRegistry` 目前只有 canvas + global 卡口，缺少 lane + global 的原子双层预算。
- `CanvasExecutionService` 仍以单一 `globalMaxConcurrency` 为核心准入参数，缺少 per-lane 行为。
- `RETRY` 不是独立 execution lane，重试洪峰仍可能回流主链路。
- `HEAVY` 流量没有硬性预算，Audience / Groovy / replay 容易和主链路互相影响。
- MQ topic 和 Disruptor event 尚未完整携带 lane 语义。
- 指标还不能回答“哪个 lane 正在压垮系统”。

### 3.3 运维侧缺口

运维与基础设施不能由代码自动解决，但必须进入 3000 验收：
- MySQL 是否具备足够连接池、IO、慢 SQL 治理和热表写能力。
- Redis 是否需要按 execution-state、route-cache、bitmap / large-object 分层。
- RocketMQ topic、broker、consumer thread、消费堆积是否能支撑隔离后的流量。
- 下游服务是否有独立容量、超时、熔断和限流契约。
- 应用实例数是否能在单实例合理并发下组成 3000 集群能力。

---

## 4. 方案比较

### 4.1 方案 A：Global Budget + Lane Budget + Queue Isolation（推荐）

保留现有主架构，在执行平台内部补齐：
- 全局 3000 硬预算。
- `LIGHT` / `STANDARD` / `HEAVY` / `RETRY` lane 预算。
- Redis ZSET lane + global 原子准入。
- normal / retry / heavy topic 隔离。
- heavy path 与 retry path 隔离。
- 分阶段压测和验收门槛。

优点：
- 与当前代码结构最匹配。
- 先处理代码能控制的容量风险。
- 不需要立即拆成多服务。
- 可以分阶段上线和回滚。

缺点：
- 执行模型从单通道变成多通道，配置和监控复杂度会上升。

### 4.2 方案 B：纯基础设施放大型

尽量不改代码，主要扩应用实例、MySQL、Redis、RocketMQ 和下游。

优点：
- 短期改动少。

缺点：
- 重任务、retry、热点 key、下游长尾不会被根治。
- 3000 高峰下容量不可预测。
- 更依赖堆资源，故障边界不清。

### 4.3 方案 C：多服务彻底拆分型

把实时执行、重任务、状态服务、调度服务、审计服务拆成独立服务。

优点：
- 长期上限最高。

缺点：
- 当前阶段成本过高。
- 迁移风险大。
- 不适合作为 3000 目标的第一步。

### 4.4 推荐结论

采用方案 A。3000 是正式稳定目标，2000 是中间验收门槛，1000 是基线门槛。代码改造先聚焦执行准入、lane 隔离、重试隔离、重任务隔离和指标，基础设施作为上线前置条件进入验收。

---

## 5. 架构演进目标

### 5.1 全局预算

`canvas.execution.max-concurrency` 升级为集群级硬上限，目标值为 3000。该值仍需要启动一致性校验，避免多实例配置不一致。

全局预算只回答“集群当前是否还能接受新的 execution”，不回答“哪类 execution 可以进来”。后者由 lane 预算决定。

### 5.2 Lane 预算

引入 4 类 execution lane：

1. `LIGHT`
   - 轻量直调。
   - 短 DAG。
   - 少量 Redis / DB / 下游调用。
   - 目标是低延迟和快速反馈。

2. `STANDARD`
   - 普通营销画布。
   - 正常 MQ / 行为 / 事件触发。
   - 是主执行通道。

3. `HEAVY`
   - Audience batch compute。
   - Groovy heavy script。
   - 大规模 replay。
   - 大对象或大批量外部调用。

4. `RETRY`
   - overflow retry。
   - execution request retry。
   - retry topic 消费。
   - 目标是把重试洪峰限制在独立预算内。

推荐初始预算：
- `LIGHT`: 600
- `STANDARD`: 1800
- `HEAVY`: 300
- `RETRY`: 300
- global: 3000

预算必须配置化，并在启动时校验 lane 总和不能超过 global。

### 5.3 分布式准入

`InFlightExecutionRegistry` 从 canvas + global 扩展为 canvas + lane + global 原子准入。

准入顺序：
1. `ExecutionLaneResolver` 解析 lane。
2. 读取 lane limit、canvas limit、global limit。
3. Redis Lua 脚本一次性清理过期 slot。
4. 原子检查 canvas active、lane active、global active。
5. 三层都未超限时写入 canvas ZSET、lane ZSET、global ZSET。
6. 返回成功 slot 或明确失败原因。

失败原因必须可观测：
- `CANVAS_LIMIT`
- `LANE_LIMIT`
- `GLOBAL_LIMIT`
- `REGISTRY_UNAVAILABLE`

### 5.4 队列隔离

MQ 流量按用途拆分：
- normal topic：标准主链路。
- retry topic：overflow / request retry。
- heavy topic：Audience、Groovy、大 replay、大对象任务。
- light topic：可选，第一阶段可以先通过 lane 标记保留在 normal topic。

Disruptor 第一阶段可以保持单 ring buffer，但 event 必须携带 lane。第二阶段视压测结果决定是否升级为 per-lane ring buffer 或独立 worker pool。

### 5.5 重试治理

`RETRY` lane 必须具备独立预算和独立退避策略：
- retry 超限时指数退避。
- retry 超过最大次数进入 DLQ。
- retry backlog 超阈值时触发降级或暂停低优先级 retry。
- retry 不允许绕过 global budget。

### 5.6 重任务治理

以下路径默认进入 `HEAVY`：
- Audience batch compute。
- Groovy heavy script。
- 批量 replay。
- 大对象 bitmap / large result 操作。
- 大批量外部调用。

`HEAVY` 超限时只能排队、延后或降级，不允许抢占 `LIGHT` / `STANDARD` 预算。

### 5.7 状态和 Redis 分层

状态分为：
- 强在线状态：执行必须依赖，保留在主执行路径。
- 弱在线状态：控制台秒级可见即可，允许异步写入。
- 审计和分析状态：不能和在线执行热表竞争。

Redis 分为：
- execution-state：ctx、dedup、lock、quota、in-flight。
- route-cache：route、config cache、pubsub。
- bitmap / large-object：Audience bitmap、大中间结果。

第一阶段可以先用客户端抽象隔离角色，第二阶段按压测结果拆实例或集群。

### 5.8 下游隔离

外部调用必须按 lane 和依赖类型应用不同策略：
- 独立连接池。
- 独立超时。
- 熔断。
- 限流。
- 最大 pending acquire。
- 下游容量契约。

3000 验收时，下游超时不能传染到主执行 lane。

---

## 6. 分阶段实施路径

### Phase 1：冻结 1000 基线

目标：
- 证明当前架构在 1000 execution 并发下的真实边界。
- 补齐现有指标缺口。
- 明确 MySQL、Redis、RocketMQ、下游的资源基线。

通过条件：
- 1000 并发完整观察窗口内 backlog 不持续增长。
- Redis / MySQL / RocketMQ / 下游没有持续饱和。
- overflow 和 retry 可解释且有上限。

### Phase 2：代码侧 3000 readiness

目标：
- 完成 `ExecutionLane`、lane 配置、lane resolver。
- 扩展 `InFlightExecutionRegistry` 为 lane-aware 原子准入。
- `CanvasExecutionService` 接入 lane 准入。
- 拆 retry / heavy topic 路由。
- 增加 lane 级指标。

通过条件：
- 单元测试和集成测试证明 global、canvas、lane 三层准入有效。
- retry / heavy 超限不会占用 normal 预算。
- 指标能定位每个 lane 的 active、reject、backlog。

### Phase 3：2000 中间验收

目标：
- 在新准入模型下跑稳 2000。
- 验证 3000 前的主要风险。

通过条件：
- 2000 并发稳定。
- `LIGHT` / `STANDARD` RT 可控。
- `HEAVY` / `RETRY` 超限不会拖垮主链路。
- backlog 不持续增长。

### Phase 4：3000 正式验收

目标：
- 达到 3000 stable execution concurrency。
- 完成上线与回滚策略验证。

通过条件：
- 3000 并发完整观察窗口稳定。
- global active 接近目标时系统通过 admission reject / retry / DLQ 有界保护。
- Redis / MySQL / RocketMQ / 下游指标满足阈值。
- 任何一个 lane 超限都不会引发全局雪崩。

---

## 7. 验收标准

### 7.1 性能指标

- 集群 execution 并发稳定达到 3000。
- `LIGHT` p95 / p99 RT 满足低延迟目标。
- `STANDARD` p95 / p99 RT 满足主链路目标。
- `HEAVY` 和 `RETRY` 可以变慢，但不能拖垮 `LIGHT` / `STANDARD`。
- 错误率、超时率、DLQ 比例低于压测阈值。

### 7.2 容量指标

- `canvas.execution.active.global` 不超过 3000。
- `canvas.execution.active.lane{lane}` 不超过 lane 预算。
- `canvas.execution.admission.rejected{lane,reason}` 可解释且有界。
- `canvas.execution.retry.backlog` 不持续增长。
- `canvas.disruptor.overflow.total` 不持续快速增长。
- `canvas.mq.topic.backlog{topic}` 不持续增长。

### 7.3 基础设施指标

- MySQL active connections 不持续打满。
- MySQL 慢 SQL 和锁等待在阈值内。
- Redis p95 / p99 延迟在阈值内。
- Redis CPU、连接数、内存、热点 key 在阈值内。
- RocketMQ consumer lag 不持续增长。
- 下游超时率和熔断比例在阈值内。

### 7.4 架构指标

- `LIGHT` / `STANDARD` / `HEAVY` / `RETRY` 已有独立预算。
- registry 原子准入覆盖 canvas、lane、global。
- retry 和 heavy 已有独立 topic 或等价隔离路径。
- heavy path 已默认进入 `HEAVY`。
- retry path 已默认进入 `RETRY`。
- 指标可以按 lane 定位瓶颈。

---

## 8. 风险与非目标

### 8.1 风险

- 多 lane 模型提升配置和运维复杂度。
- Redis ZSET 原子准入成为更关键的热路径，需要压测 Lua 脚本耗时。
- lane 初始预算可能需要多轮压测调参。
- retry 隔离后，失败恢复速度可能下降，但这是保护主链路的必要代价。
- 下游容量不足时，代码只能限流和降级，不能凭空提升下游吞吐。

### 8.2 非目标

本设计不要求：
- 单机承担 3000 execution 并发。
- 立即拆成多服务架构。
- 立即把所有 Redis 角色物理拆集群。
- 在未完成 1000 / 2000 / 3000 分级压测前直接承诺生产稳定。
- 让代码单独解决 MySQL、Redis、RocketMQ、下游规格不足的问题。

---

## 9. 输出物

该专项最终应输出：
1. 3000 execution 并发架构方案。
2. 分阶段实施计划。
3. 1000 / 2000 / 3000 压测与验收基线。
4. lane 预算配置和调参说明。
5. 上线、灰度、回滚和降级策略。
6. 容量口径与汇报口径统一说明。
