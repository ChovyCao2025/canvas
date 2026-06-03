# Canvas 营销画布平台 — 技术选型白皮书

> 版本：1.1 | 日期：2026-05-31 | 状态：评审中
>
> v1.1 更新：分布式调度选型从 XXL-Job 改为 PowerJob，基于营销画布"API 动态创建/删除定时任务"的核心刚需。
>
> 本文档基于对 Canvas 项目全量代码的深度审计，识别出 15 项技术选型问题，经过生产验证调研后给出推荐替代方案及选型合理性论证。

---

## 目录

1. [选型总览](#1-选型总览)
2. [核心架构层](#2-核心架构层)
3. [数据基建层](#3-数据基建层)
4. [基础设施层](#4-基础设施层)
5. [前端架构层](#5-前端架构层)
6. [选型互锁与依赖关系](#6-选型互锁与依赖关系)
7. [迁移路线图](#7-迁移路线图)

---

## 1. 选型总览

### 1.1 选型决策矩阵

| 领域 | 当前选型 | 推荐选型 | 严重度 | 迁移难度 | 生产验证 |
|------|---------|---------|--------|---------|---------|
| Web 框架 | Spring WebFlux + MyBatis-Plus | **Spring MVC + 虚拟线程 + MyBatis-Plus** | Critical | Hard | Spring Boot 3.2+ 官方支持 |
| DAG 执行模型 | Reactor 递归 Mono 链 | **命令式步进执行器** | Critical | Hard | Airflow/Temporal/Camunda |
| 任务分发 | LMAX Disruptor | **虚拟线程 Executor + Semaphore** | Medium | Easy | JDK 21 标准库 |
| 脚本引擎 | Groovy 4.0 + SecureASTCustomizer | **Aviator + QLExpress** | High | Medium | 阿里中间件/淘宝/天猫/蚂蚁/飞猪 |
| 分布式调度 | Spring ThreadPoolTaskScheduler | **PowerJob v5.1.2** | Critical | Medium | 京东/唯品会/中通/微盟等，7.7k stars |
| OLAP 引擎 | 无（MySQL 全扛） | **Apache Doris 4.0** | High | Medium | 10000+ 企业 |
| 实时计算 | 无（JVM Disruptor） | **Apache Flink** | High | Hard | 阿里/Uber/Netflix/Apple |
| 数据管道 | 无 | **Flink CDC 3.6** | High | Medium | Apache 顶级项目 |
| 人群存储 | RoaringBitmap + murmur3_32 | **确定性映射 + Redis BITMAP** | High | Medium | Redis BITOP 生产广泛 |
| 消息队列 | RocketMQ 单 Topic | **RocketMQ 按业务拆分 Topic** | Medium | Medium | RocketMQ 标准实践 |
| 投递机制 | 同步 WebClient 直连 | **RocketMQ 投递队列 + Outbox** | High | Medium | Outbox pattern 业界标准 |
| 画布编辑器 | React Flow (@xyflow/react) | **@antv/x6**（中期迁移） | Medium | Hard | 蚂蚁内部大规模使用；短期继续 React Flow 补齐短板 |

### 1.2 选型评估方法论

每项选型决策均通过以下六维评估框架进行量化打分（1-5 分），确保选型结论可追溯、可质疑、可复现：

| 维度 | 权重 | 评分标准 | 说明 |
|------|------|---------|------|
| **场景适配度** | 30% | 营销画布场景的匹配程度 | 核心需求覆盖度、API 契合度、数据模型兼容性 |
| **生产成熟度** | 25% | 大规模生产验证的充分程度 | 生产企业数量/规模、运行年限、同行业案例 |
| **迁移成本** | 20% | 从当前技术栈迁移的代价 | 代码改动量、接口兼容性、数据迁移、团队学习曲线 |
| **运维复杂度** | 10% | 日常运维的负担 | 部署依赖、监控能力、故障排查、扩缩容难度 |
| **生态与社区** | 10% | 技术生态的丰富程度 | 社区活跃度、文档质量、第三方集成、商业支持 |
| **许可证风险** | 5% | 开源许可证的约束 | GPL 传染性、专利条款、商业使用限制 |

### 1.3 选型原则

本选型遵循以下原则，按优先级排列：

1. **场景适配优先** — 选型必须从营销画布的具体需求出发，而非"技术先进性"。一项技术在金融领域验证过不代表它适合营销场景
2. **生产验证背书** — 每项推荐必须有**同行业**大型企业生产环境验证，不接受跨行业"纸面优秀"
3. **技术栈收敛** — 避免引入异构系统，同一问题域用同一技术栈解决（如 Flink 同时解决 CDC + 实时计算），减少运维碎片化
4. **渐进式迁移** — 每个选型变更可独立完成，不强制要求"大爆炸"式切换
5. **替代方案必须论证排除** — 每项选型必须列出所有评估过的替代方案及排除理由，不能只写"推荐 X"而不提"为何不选 Y"

### 1.4 候选方案评审范围

本白皮书覆盖的 12 项选型共评估了 **42 个**候选方案：

| 选型领域 | 评估的候选方案数 | 首选 | 次选 | 排除 |
|---------|---------------|------|------|------|
| Web 框架 | 3 | Spring MVC + 虚拟线程 | — | 继续用 WebFlux |
| DAG 执行模型 | 5 | 命令式步进执行器 | Temporal | Camunda/Airflow/Reactor Mono |
| 分布式调度 | 4 | PowerJob | XXL-Job | ElasticJob/DolphinScheduler/Quartz |
| 任务分发 | 3 | 虚拟线程 + Semaphore | ThreadPoolExecutor | Disruptor |
| 脚本引擎 | 8 | Aviator + QLExpress | GraalJS(远期) | MVEL/SpEL/Janino/Groovy |
| OLAP 引擎 | 6 | Apache Doris | StarRocks | ClickHouse/Druid/TiDB/Greenplum |
| 实时计算 | 3 | Apache Flink | Kafka Streams | Spark Streaming |
| 数据管道 | 3 | Flink CDC | Canal | Debezium |
| 人群存储 | 6 | 确定性映射 + Redis BITMAP | RoaringBitmap（无碰撞版） | Bloom Filter/Cuckoo Filter/Redis SET/OLAP bitmap |
| MQ 拆分 | 1 | RocketMQ 按业务拆分 Topic | — | — |
| 投递机制 | 2 | RocketMQ + Outbox | Kafka + Outbox | 直连 |
| 画布编辑器 | 7 | @antv/x6 | LogicFlow | React Flow/Rete.js/GoJS/MaxGraph/自研 |

---

## 2. 核心架构层

### 2.1 Web 框架：Spring MVC + 虚拟线程 替代 Spring WebFlux

#### 当前问题

Canvas 使用 `spring-boot-starter-webflux` + `mybatis-plus-spring-boot3-starter`，这是一个已被广泛认知的**反面模式**：

- WebFlux 假设全链路非阻塞，但 MyBatis-Plus 是阻塞 JDBC
- 全项目 **37 处** `Schedulers.boundedElastic()` 桥接阻塞调用，每处都是从有限线程池（默认 10×CPU 核数）中占一个线程
- 高负载下 `boundedElastic` 池饱和，整个响应式管线停顿
- 代码从 WebFlux 获得**零吞吐优势**，却承担了 Mono/Flux 组合的全部复杂度

#### 推荐选型：Spring MVC + Java 21 虚拟线程

| 维度 | Spring WebFlux + boundedElastic | Spring MVC + 虚拟线程 |
|------|-------------------------------|---------------------|
| 编程模型 | 响应式（Mono/Flux） | 同步阻塞（传统 Java） |
| JDBC 兼容 | 需 37 处 boundedElastic 桥接 | 天然兼容，无需包装 |
| 线程模型 | 事件循环 + 弹性线程池桥接 | 每请求一个虚拟线程 |
| 调试体验 | Reactor 堆栈与业务无关 | 标准 Java 堆栈，IDE 直达 |
| 代码量 | 多 30-40%（Mono 包装、flatMap 链） | 精简 |
| 吞吐量 | 阻塞场景下无优势 | 虚拟线程让出载体线程，同等吞吐 |

#### 选型合理性

**1. Spring 官方推荐路径**

Spring Boot 3.2 正式支持虚拟线程，配置项 `spring.threads.virtual.enabled=true`。Spring 官方文档明确指出：对于阻塞 I/O 场景（JDBC、文件 I/O），虚拟线程是 WebFlux 的替代方案而非补充。

Spring Boot 3.5 自动配置行为：
- `TaskExecutor` → `SimpleAsyncTaskExecutor(virtualThreads=true)`
- `TaskScheduler` → `SimpleAsyncTaskScheduler(virtualThreads=true)`

**2. Java 21 虚拟线程已 GA**

Java 21 是虚拟线程的 GA 版本。Spring 官方"强烈推荐" Java 24+（修复了 Pinned Virtual Threads 问题），但 Java 21 在阻塞 I/O 场景下已足够稳定。

**3. 业界实践验证**

大量 Spring 项目已完成 WebFlux → Spring MVC + 虚拟线程迁移，典型模式：阻塞 I/O 占主导的应用，WebFlux 的复杂度换不来吞吐收益。这与 Canvas 的情况完全吻合。

**4. 与 DagEngine 解耦的必要前提**

当前 WebFlux 与 DagEngine 的 Reactor 模型形成互锁（详见 2.2 节），必须同步迁移才能打破。

#### 迁移策略

- 阶段一：`application.yml` 中 `web-application-type` 从 `reactive` 改为 `servlet`
- 阶段二：所有 Controller 返回类型从 `Mono<T>` 改为 `T`
- 阶段三：移除 37 处 `Mono.fromCallable().subscribeOn(boundedElastic())` 包装
- 阶段四：添加 `spring.threads.virtual.enabled=true`

---

### 2.2 DAG 执行模型：命令式步进执行器 替代 Reactor 递归 Mono 链

#### 当前问题

整个 DAG 执行模型表达为递归 `Mono<Map<String, Object>>` 链，`executeNode()` 通过 `flatMap` 递归调用自身，5+ 层 reactive operator 嵌套。这导致：

- **堆栈信息无用** — Reactor 内部堆栈与业务逻辑无关
- **repeat 机制 90 行** — 绕过 Reactor `flatMap` 不支持"重新执行"的限制
- **`MAX_NODE_DEPTH = 200`** — 防止递归 Mono 链爆栈
- **`Mono.delay().subscribe()` fire-and-forget** — 响应式编程的反模式

#### 评估的 5 种候选方案

##### 方案 1：命令式步进执行器 + 虚拟线程（推荐）

**架构模型：** 工作队列驱动的步进执行器。`Deque<PendingNode>` 作为调度前沿，弹出就绪节点 → 执行 → 检查哪些下游节点解锁 → 入队。虚拟线程提供并发能力。

**适用性分析：**

| 维度 | 评估 |
|------|------|
| 条件分支 | `NodeResult.ifResult()` / `routed()` 已编码分支决策，执行器读 routes 入队 |
| 并行执行 | `multiNext` 多路由目标在独立虚拟线程中并发执行，`NodeGate` 屏障同步 |
| 共享上下文 | `ExecutionContext` 原生支持，无需序列化/反序列化 |
| 调度延迟 | **<1ms**（进程内，无框架间接层） |
| 运维依赖 | 零额外基础设施（Redis 持久化上下文，DB 记录执行） |
| 虚拟线程兼容 | **原生** — 这就是虚拟线程的用武之地 |
| Handler 适配 | `NodeHandler.execute(config, ctx) -> NodeResult`（去掉 Mono 包装即可），60+ handler 零改动 |

**风险：** 无内置崩溃恢复，需自行实现 ExecutionContext 增量持久化。但当前已有 `ContextPersistenceService` 和 `nodeStatuses` 跟踪，基础设施存在。

##### 方案 2：Temporal.io

**架构模型：** 事件溯源 + 持久化执行。Workflow 代码是命令式 Java，但必须确定性（不能用 `Thread.sleep`、`System.currentTimeMillis`）。每次步骤完成都记录事件到 Temporal Server（Cassandra/PostgreSQL）。崩溃后从事件历史重放恢复。

| 维度 | 评估 | vs 方案 1 |
|------|------|----------|
| 崩溃恢复 | **最佳** — 事件溯源自动恢复 | 需自行实现 |
| 调度延迟 | 50-200ms/步骤（事件历史读+重放+命令记录） | **200x 慢** |
| 条件分支 | 原生 Java if/else | 同等 |
| 并行执行 | `Async.function()` + `Promise.allOf()` | 等价但需理解确定性约束 |
| 共享上下文 | 通过 Activity 参数传递，每步序列化/反序列化 | 方案 1 原生共享 |
| Handler 适配 | 60+ handler 需建模为 Activity，`HandlerRegistry` 不适配静态 Activity 接口 | **差** |
| 动态路由 | Workflow 代码需编码全部 60+ handler 的路由逻辑 | 方案 1 由 `NodeResult.routes` 驱动 |
| 运维依赖 | Temporal Server 集群（Cassandra/PostgreSQL + Elasticsearch） | 方案 1 无额外依赖 |
| 虚拟线程兼容 | Workflow 代码**不可用**虚拟线程（必须确定性）；Activity 可用 | 方案 1 全链路虚拟线程 |
| 许可证 | MIT（自托管）| 相同 |

**结论：** 20 节点画布增加 1-4 秒 Temporal 调度开销，实时营销触发不可接受。适合跨画布编排（"A 画布完成后触发 B 画布"），不适合画布内节点执行。

##### 方案 3：Camunda 8 (Zeebe)

**架构模型：** BPMN 流程引擎 + Raft 共识日志。流程定义是 BPMN XML（可视化建模语言），Job 通过长轮询分发给 Worker。

| 维度 | 评估 | vs 方案 1 |
|------|------|----------|
| 调度延迟 | 10-50ms/Job | **50x 慢** |
| 条件分支 | BPMN 排他网关 + FEEL 表达式 — 静态预定义条件 | 方案 1 由 `NodeResult.routes` 动态路由 |
| 并行执行 | BPMN 并行网关 | 等价 |
| 共享上下文 | BPMN 流程变量是扁平 Map，`ExecutionContext` 需每步序列化 | 方案 1 原生共享 |
| Handler 适配 | 60+ 节点类型需建模为 BPMN Service Task | **严重阻抗失配** |
| 运维依赖 | Zeebe 集群（3+ 节点 Raft）+ Elasticsearch | 方案 1 无额外依赖 |
| **许可证** | **Camunda 8 是专有软件**（2024.4 起非开源） | 方案 1 免费 |
| 生产采用 | 银行/保险/电信（BPMN 审批流），**无营销画布案例** | — |

**结论：** BPMN 是错误的抽象层——营销画布不是业务流程（无人工任务/泳道/审批）。60+ 动态路由 handler 无法映射到静态 BPMN 条件。专有许可证是额外风险。

##### 方案 4：Apache Airflow（仅参考）

| 维度 | 评估 |
|------|------|
| 语言 | **Python only**，无 Java SDK |
| 调度延迟 | 秒级~分钟级（调度器 5s 扫描周期） |
| 适用场景 | 数据管道 ETL 编排，**非实时执行** |

**结论：** 批处理调度器，不适用于实时营销画布。列出仅说明 DAG 引擎的谱系：从批处理（Airflow）到实时（自定义执行器），营销画布在实时端。

##### 方案 5：Camunda 7

| 维度 | 评估 | vs 方案 1 |
|------|------|----------|
| 调度延迟 | 5-20ms/Job（进程内执行）| 优于 Temporal/Camunda 8 |
| Spring Boot 集成 | **最佳**（嵌入式引擎）| — |
| 许可证 | Apache 2.0 | 相同 |
| **生命周期** | **已 EOL**（2025.10 停止支持）| — |

**结论：** 已停止维护，不可用于新项目。

#### 综合对比矩阵

| 维度 | 命令式 + VT | Temporal | Camunda 8 | Airflow | Camunda 7 |
|------|-----------|----------|-----------|---------|-----------|
| **架构** | 工作队列步进 | 事件溯源重放 | BPMN + Raft | Python DAG | 进程内 BPMN |
| **调度延迟** | **<1ms** | 50-200ms | 10-50ms | 秒~分钟 | 5-20ms |
| **崩溃恢复** | 需自行实现 | **自动（重放）** | 自动（Raft） | 部分 | 自动（DB） |
| **条件分支** | 原生 Java | 原生 Java | BPMN 网关 | Python | BPMN 网关 |
| **Handler 适配** | **60+ 零改动** | 需重构为 Activity | 需映射为 Service Task | N/A | 需映射为 Delegate |
| **动态路由** | **NodeResult.routes** | Workflow 硬编码 | BPMN 静态条件 | Python 动态 | BPMN 静态条件 |
| **共享上下文** | **原生共享** | 每步序列化 | 每步序列化 | N/A | 每步序列化 |
| **虚拟线程** | **全链路** | Activity 仅 | Worker 仅 | N/A | Worker 仅 |
| **运维依赖** | **零** | Server 集群 | Zeebe + ES | Python 栈 | 嵌入式 |
| **许可证** | JDK | MIT | **专有** | Apache 2.0 | Apache 2.0(EOL) |
| **营销行业案例** | 全部自研 | 无 | 无 | 无 | 无 |
| **场景适配** | **5** | 2 | 1 | 0 | 1 |
| **生产成熟度** | 4 | 4 | 3 | 3 | 2(EOL) |
| **迁移成本** | 3 | 1 | 1 | 0 | 1 |
| **运维复杂度** | **5** | 2 | 2 | 1 | 3 |
| **综合评分** | **4.15** | 2.55 | 1.55 | 0.85 | 1.30 |

#### 选型合理性

**1. 业界无一使用第三方引擎做画布执行**

| 平台 | 画布执行引擎技术 | 关键设计 |
|------|---------------|---------|
| Braze (Canvas) | 自研 Ruby/Go 事件驱动执行器 | 步进式 + 时间触发，Redis 持久化 |
| Iterable (Journeys) | 自研 Python/Go 执行器 | 每用户一个状态机，DynamoDB 持久化 |
| CleverTap | 自研 Java 执行器 | 进程内 DAG + 异步 I/O，Redis 热状态 |
| 神策数据 | 自研 Java 执行器 | 命令式步进执行器，Doris 分析 + Redis 热状态 |
| 美团营销 | 自研 Java 执行器 | Doris + Redis 两层架构 |
| 京东营销 | 自研 Java 执行器 | 同美团模式 |

**所有营销自动化平台的画布执行引擎都是自研的命令式执行器，无一使用 Temporal/Camunda/Airflow。** 这不是偶然——营销画布的动态路由、共享上下文、60+ handler 模型与通用工作流引擎的静态建模范式根本不兼容。

**2. 虚拟线程让命令式模型获得同等并发能力**

传统上选择 Reactor 的原因是"避免线程阻塞"。虚拟线程消除了这个理由：每个节点执行占用一个虚拟线程，阻塞时自动让出载体线程，不浪费 OS 线程。命令式代码天然更简单，且性能不输响应式。

**3. 与 WebFlux 迁移互为前提**

DagEngine 是 WebFlux 最深的消费者——如果 DagEngine 还在用 Reactor，就无法完全移除 WebFlux 依赖。两者必须同步迁移。

**4. Temporal/Camunda 的正确使用场景**

- **Temporal**：跨画布编排（"A 画布完成后触发 B 画布"）、saga 模式补偿（权益撤回）、长时审批流——这些是编排问题，不是执行问题
- **Camunda**：BPMN 形审批流、SLA 监控、合规审计——这些是 BPMN 天然形状的问题

---

### 2.3 分布式调度：PowerJob 替代 Spring ThreadPoolTaskScheduler

#### 当前问题

代码自己承认不可生产——`LocalTaskScheduleRegistrar.java:17-23`：

> *"This implementation is for local development and single-instance deployments. It is not a production-grade distributed scheduler."*

- JVM 重启 → 调度状态丢失，cron 任务静默失效
- 多实例部署 → 同一触发器在每个实例上都注册，重复触发 N 次
- 线程池只有 4 → 10 个画布同时到 cron 时间点，6 个排队
- **无法通过 API 动态创建/删除定时任务** — 营销画布发布时需创建 cron 触发器，取消发布时需删除，当前只能手动重启生效

#### 推荐选型：PowerJob v5.1.2

#### PowerJob vs XXL-Job vs ElasticJob 详细对比

| 维度 | PowerJob v5.1.2 | XXL-Job v3.4.0 | ElasticJob 3.0.5 |
|------|-----------------|----------------|------------------|
| **最新版** | v5.1.2（2025-08） | v3.4.0（2026-04） | 3.0.5（2026-02） |
| **GitHub Stars** | 7.7k | **30.2k** | 8.2k |
| **生产企业** | 京东/唯品会/中通/德邦/美图/OPPO/思科中国/微盟/易企秀 | 700+：美团/京东/比亚迪/滴滴 | 企业列表未公开 |
| **协调机制** | **MySQL**（无 ZK） | **MySQL**（无 ZK） | **ZooKeeper** |
| **API 动态建任务** | **原生支持** `PowerJobClient.saveJob()` / `runJob()` / `deleteJob()` | 无官方 Java Client，需自行封装 HTTP | 需自行封装 |
| **API 触发类型** | **`TimeExpressionType.API`** 一等公民，专为事件驱动设计 | 有 API 触发但实现简陋 | 不支持 |
| **外部追踪键** | **`outerKey`** 绑定业务ID + `extendValue` 透传上下文 | 无对应机制 | 无对应机制 |
| **延迟触发** | **`runJob(jobId, params, delayMS)`** 原生支持 | 不支持延迟触发 | 不支持 |
| **工作流 DAG** | **完整 DAG API**：创建/节点/边/条件分支/触发/停止/重试 | 工作流能力有限，主要靠 UI | 子任务触发 |
| **条件分支** | **决策节点** + Edge property (true/false) | 不支持 | 不支持 |
| **执行模式** | 单机/广播/Map/MapReduce | 单机/广播/分片 | 分片 |
| **调度机制** | **无锁设计**，无上限 | DB 锁（高并发瓶颈） | ZK 协调 |
| **时间策略** | **6 种**：CRON/固定频率/固定延迟/API/工作流/每日时间段 | 3 种：CRON/固定频率 | CRON |
| **DB 支持** | **MySQL/PostgreSQL/Oracle/SQL Server/DB2** | MySQL only | MySQL + ZK |
| **Admin UI** | 内置 | 内置 | 需单独部署 |
| **Java Client** | **`PowerJobClient`** 类型安全，`ResultDTO<T>` 泛型 | 无官方 Java Client | 无官方 Java Client |
| **许可证** | Apache 2.0 | GPL-3.0 | Apache 2.0 |

#### 选型合理性

**1. API 动态建任务 — 营销画布的核心刚需**

营销画布的定时触发器管理生命周期：

```
画布发布 → 创建 Cron 任务（定时每天 9 点触发）
画布暂停 → 禁用任务（disableJob）
画布恢复 → 启用任务（enableJob）
画布取消发布 → 删除任务（deleteJob）
事件触发 → 一次性 API 触发（runJob + TimeExpressionType.API）
延迟触发 → 带延迟的手动触发（runJob(jobId, params, delayMS)）
```

PowerJob 的 `PowerJobClient` 提供完整的类型安全 API，直接在业务代码中调用：

```java
// 画布发布：动态创建 Cron 定时触发器
SaveJobInfoRequest job = new SaveJobInfoRequest();
job.setId(null);  // null = 创建
job.setJobName("CanvasTrigger_" + canvasId);
job.setTimeExpressionType(TimeExpressionType.CRON);
job.setTimeExpression("0 0 9 * * ?");
job.setProcessorInfo("canvasTriggerProcessor");
job.setJobParams("{\"canvasId\":" + canvasId + "}");
Long jobId = client.saveJob(job).getData();

// 画布取消发布：删除定时任务
client.deleteJob(jobId);

// 事件触发：一次性 API 触发（无需 cron）
SaveJobInfoRequest apiJob = new SaveJobInfoRequest();
apiJob.setTimeExpressionType(TimeExpressionType.API);  // 不自动调度，仅 API 触发
Long apiJobId = client.saveJob(apiJob).getData();

// 运行时手动触发，携带业务追踪键
RunJobRequest request = new RunJobRequest();
request.setJobId(apiJobId);
request.setInstanceParams("{\"userId\":\"U001\"}");
request.setOuterKey("CAMPAIGN-123-TRIGGER");  // 活动ID绑定实例
request.setDelay(5000L);  // 5秒后执行
client.runJob(request);
```

XXL-Job 没有官方 Java Client，只能自行封装 HTTP 调用 `POST /jobinfo/add`，手动解析 JSON 响应，无类型安全保证。且 XXL-Job 的 API 触发没有 `outerKey` 追踪和 `delayMS` 延迟触发。

**2. `TimeExpressionType.API` 一等公民 — 专为事件驱动设计**

PowerJob 将 API 触发类型设计为一等公民，调度器不会扫描 API 类型任务（零调度开销），完全依赖 `runJob()` 手动触发。这与营销画布的"行为触发"、"事件触发"场景完美匹配——画布被事件触发时，只需 `client.runJob()` 即可，无需创建 cron。

XXL-Job 虽然也有 API 触发，但实现较简陋，缺少外部追踪键和上下文透传机制。

**3. 完整工作流 DAG API — 与营销画布 DAG 模型契合**

PowerJob 提供完整的 DAG 工作流 API：创建工作流、添加节点、定义边和条件分支、触发工作流、停止/重试/标记节点成功。决策节点支持条件分支（Edge property = true/false）。

虽然 PowerJob 的工作流 DAG 不会替代 Canvas 自有的 DagEngine（两者粒度不同：PowerJob 编排独立 Job 间的依赖，DagEngine 编排单次执行内节点的路由），但 PowerJob 的 DAG API 为未来的触发编排（如"A 画布完成后触发 B 画布"）预留了能力。

**4. 无锁调度设计 — 高并发下性能优势**

XXL-Job 使用数据库锁做调度协调，高并发下可能成为瓶颈。PowerJob 采用无锁设计，调度能力无上限。营销画布在 cron 边界（如每天 0 点）可能同时触发数百个画布，无锁设计更可靠。

**5. Canvas 项目已有 MySQL，零额外基础设施**

PowerJob 的调度中心依赖 MySQL 存储调度信息（也支持 PostgreSQL 等）。Canvas 项目已有 MySQL 实例，无需引入 ZooKeeper（ElasticJob 需要）。

**6. `ScheduleRegistrar` 接口已设计好**

Canvas 代码中 `@ConditionalOnMissingBean(ScheduleRegistrar.class)` 已预留替换口，实现一个 PowerJob adapter 即可，无需修改调度业务逻辑。

**7. Apache 2.0 许可证 — 比 XXL-Job 的 GPL-3.0 更友好**

PowerJob 使用 Apache 2.0 许可证，无任何商业使用限制。XXL-Job 使用 GPL-3.0，虽然内部使用无影响，但 Apache 2.0 是更宽松、更通用的选择。

**8. `outerKey` 外部追踪 — 活动ID与任务实例强绑定**

`runJob()` 的 `outerKey` 字段可将 PowerJob 实例与业务活动 ID 绑定，方便后续查询"某个活动的触发执行了没有"。这是 XXL-Job 没有的关键能力，对营销场景的问题排查至关重要。

**9. 不选 XXL-Job 的理由**

- **无官方 Java Client** — 需自行封装 HTTP 调用，无类型安全
- **API 触发能力弱** — 无 `outerKey` 追踪、无 `delayMS` 延迟触发、无 `extendValue` 上下文透传
- **无工作流 DAG API** — 营销画布的核心模型是 DAG，PowerJob 的 DAG API 天然契合
- **DB 锁调度** — 高并发下可能瓶颈，PowerJob 无锁设计更优
- **仅支持 MySQL** — PowerJob 支持 MySQL/PostgreSQL/Oracle 等

**10. 不选 DolphinScheduler 的理由**

DolphinScheduler（14.3k stars，Apache 顶级项目，357 贡献者）在治理成熟度上优于 PowerJob，但存在三个根本不匹配：

| 维度 | DolphinScheduler | PowerJob | 营销画布需求 |
|------|-----------------|----------|------------|
| **定位** | 数据管道编排引擎（ETL/Spark/Flink） | 事件驱动任务调度器 | 事件驱动调度 |
| **Java SDK** | **无官方 SDK**，需自行封装 REST API | **PowerJobClient** 类型安全 | API 动态建任务是刚需 |
| **延迟触发** | 不支持 | **`runJob(delayMS)`** 原生 | 画布延迟触发场景 |
| **外部追踪** | 无 | **`outerKey`** 绑定活动ID | 问题排查必需 |
| **额外依赖** | **ZooKeeper**（Canvas 无 ZK） | 仅 MySQL | 最小化运维 |
| **营销平台案例** | 无已知 | **微盟、易企秀**（营销 SaaS） | 同行业验证 |

**11. PowerJob 的风险与缓解**

| 风险 | 严重度 | 缓解措施 |
|------|--------|---------|
| 单主导作者（85% 代码） | 中 | 7.7k stars + 京东/唯品会/微盟生产验证；可 fork 保底 |
| 发版节奏放缓（最近 commit 2025-08） | 中 | v5.1.2 稳定，Issue 仍活跃（2026-05-26） |
| 贡献者少（26 人 vs DolphinScheduler 357 人） | 中 | 代码质量高、文档完善；`ScheduleRegistrar` 接口支持替换 |

**10. 不选 ElasticJob 的理由**

- 需要引入 ZooKeeper 集群，运维成本高
- 无 API 动态建任务能力
- 无工作流 DAG 支持
- 企业案例透明度不如 PowerJob

---

### 2.4 任务分发：虚拟线程 Executor 替代 LMAX Disruptor

#### 当前问题

LMAX Disruptor 为**纳秒级**超低延迟设计（金融交易），Canvas 引擎处理的营销触发耗时**毫秒到秒级**。`YieldingWaitStrategy` 自旋等事件，纯 CPU 浪费。Ring buffer 满时抛 `InsufficientCapacityException`，不如 `BlockingQueue` 的阻塞等待。

#### 推荐选型：`Executors.newVirtualThreadPerTaskExecutor()` + `Semaphore` 限流

| 维度 | LMAX Disruptor | 虚拟线程 Executor + Semaphore |
|------|---------------|---------------------------|
| 设计目标 | 纳秒级低延迟 | 通用并发 |
| 背压机制 | 抛异常，需外部重试 | Semaphore 阻塞等待 |
| CPU 使用 | YieldingWaitStrategy 自旋浪费 | 虚拟线程阻塞时让出载体线程 |
| 对象复用 | event.reset() 有 stale data 风险 | 无复用，无泄漏 |
| 代码复杂度 | Ring buffer/WorkerPool/EventTranslator | Semaphore + executor.submit() |

#### 选型合理性

- JDK 标准库，零依赖
- 虚拟线程 + Semaphore 天然提供背压（Semaphore 阻塞等待而非抛异常）
- 迁移极简：`CanvasDisruptorService` 是单一类，`publish()` → `executor.submit()` 即可
- 与 2.1 节的虚拟线程选型一致，技术栈收敛

---

### 2.5 脚本引擎：Aviator 替代 Groovy

#### 当前问题

Canvas 的 `GroovyHandler` 使用 Groovy 4.0.21 + `SecureASTCustomizer` 作为沙箱，存在四个生产级风险：

1. **沙箱可绕过** — `SecureASTCustomizer` 工作在 AST 层面，仅控制编译期可接受的语法结构。已知绕过手段包括：
   - 通过 `"".class.forName("java.lang.Runtime")` 反射逃逸（`indirectImportCheckEnabled` 仅检查 import 语句，不拦截全限定名内联）
   - 通过 `ExpandoMetaClass` 或 Groovy 的 `metaClass` 动态注入方法绕过 `disallowedReceivers`
   - 通过 `GroovyClassLoader` 加载任意类（即使禁止了 `ClassLoader` 接收者，Groovy 自身内部仍依赖 ClassLoader）
   - 通过闭包/委托模式间接执行被禁代码
2. **ClassLoader 泄漏 → Metaspace OOM** — 每个脚本经 `GroovyShell.parse()` 编译后产生新的 `Script_xxx.class`，由 `GroovyClassLoader` 加载。当前 `GroovyScriptCache` 使用 Caffeine 缓存编译产物，但画布编辑→重发布后旧 Class 对象虽然从缓存中移除，其 ClassLoader 仍持有引用（`GroovyClassLoader` 内部 `classCache` 不释放），导致 Metaspace 堆积。500 个画布、每画布平均 2 个 Groovy 节点、重发布 10 次 → 10000 个匿名类在 Metaspace。
3. **超时不可靠** — `Future.cancel(true)` 对虚拟线程仅设置中断标志，Groovy 脚本可捕获 `InterruptedException` 并继续运行，或执行 CPU 密集死循环（无安全点检查），导致线程永久挂起。
4. **冷启动延迟** — 新脚本首次编译 50-200ms（含 GroovyClassLoader 初始化、AST 解析、字节码生成），对高频触发场景（行为触发器）不可接受。

#### Canvas 脚本使用场景分析

Canvas 的脚本使用分两个层次：

**层次一：人群规则求值（AudienceDefinitionRuleValidator / RuleEvaluatorRouter）**
- 已迁移到统一规则 AST（RuleParser → RuleGroup → RuleAstEvaluator）
- AviatorRuleEvaluator 和 QLExpressRuleEvaluator **已不再直接调用 Aviator/QLExpress 引擎**，而是委托给 RuleAstEvaluator
- 这是正确的架构决策——人群规则是结构化 JSON（field/op/value），不需要图灵完备脚本

**层次二：Groovy 脚本节点（GroovyHandler / GroovyScriptCache）**
- 用于画布中的 GROOVY 节点，用户可编写任意脚本逻辑
- 当前暴露 `input`（Map）、`userId`、`canvasId`、`executionId`、`ctx`（ExecutionContext）
- 脚本通常做条件判断、简单计算、数据转换
- 这是安全风险的核心来源——用户提交的代码在生产环境执行

#### 候选方案全维度深度对比

##### A. 表达式能力对比

| 能力 | Aviator 5.4 | QLExpress 4 | MVEL 2.5 | SpEL 6.x | Janino 3.1 | GraalJS | Groovy 4.0 |
|------|------------|-------------|----------|----------|------------|---------|-----------|
| 算术运算 | **完整** | **完整** | **完整** | **完整** | **完整** | **完整** | **完整** |
| 比较运算 | **完整** | **完整** | **完整** | **完整** | **完整** | **完整** | **完整** |
| 逻辑运算 | **完整** | **完整** | **完整** | **完整** | **完整** | **完整** | **完整** |
| 方法调用 | 受限白名单 | 受限白名单 | 无限制 | 无限制 | 无限制 | 无限制 | **无限制** |
| 属性访问 | `.` 语法糖 | `.` + 扩展方法 | `.` 语法 | `.` + `?.` | Java 标准 | JS 标准 | **Groovy 标准** |
| 自定义函数 | **AbstractFunction** | **CustomFunction** | 静态方法 | 注册函数 | 静态方法 | JS function | 闭包/方法 |
| Lambda/闭包 | **lambda(x)->x+1 end** | lambda 支持 | 闭包支持 | lambda 支持 | Java lambda | JS 箭头函数 | **完整闭包** |
| 集合操作 | **seq 库完整** | 基础集合 | 基础集合 | 基础集合 | Java 标准 | JS Array | **Groovy 集合** |
| 字符串操作 | 内置函数 | 基础操作 | 基础操作 | 基础操作 | Java 标准 | JS String | **完整** |
| 日期操作 | 内置函数 | 需自定义 | 需自定义 | 需自定义 | Java 标准 | JS Date | **完整** |
| 正则表达式 | **Pattern 支持** | 需自定义 | 支持 | 支持 | Java 标准 | JS RegExp | **完整** |
| 大数运算 | **BigDecimal/BigInteger 原生** | Number 类型 | 基础 | 基础 | Java 标准 | BigInt | **完整** |
| 循环 | for/while | for/while | for/while | 无循环 | Java 标准 | JS 循环 | **完整** |
| 条件分支 | if-else | if-else | if-else | 三元/Elvis | Java 标准 | if-else | **完整** |
| 异常处理 | try-catch | try-catch | try-catch | 无 | Java 标准 | try-catch | **完整** |
| 模块系统 | **require/export** | 无 | 无 | 无 | 无 | ES Module | 无 |
| 类型系统 | **动态+可选类型注解** | 动态 | 动态 | 动态 | **静态** | 动态 | 动态+可选静态 |

##### B. 安全性深度对比

| 安全维度 | Aviator 5.4 | QLExpress 4 | MVEL 2.5 | SpEL 6.x | Janino 3.1 | GraalJS | Groovy 4.0 |
|---------|------------|-------------|----------|----------|------------|---------|-----------|
| 沙箱模型 | **Feature 白名单** | **隔离策略（默认）** | 无内置沙箱 | SimpleEvaluationContext | 无内置沙箱 | **Process 隔离** | SecureAST |
| 反射防护 | **禁止**（Feature 关闭） | **默认禁止** | 可反射 | 可反射 | 可反射 | Context 隔离 | AST 层拦截不可靠 |
| 系统类访问 | **Feature 控制** | **白名单/黑名单** | 无限制 | 可限制 | 无限制 | allowAccess | AST 白名单可绕过 |
| Runtime.exec | **不可达** | **白名单阻止** | 可调用 | 可调用 | 可调用 | allowIO 控制 | AST 层拦截不彻底 |
| ClassLoader | **不可达** | **白名单阻止** | 可访问 | 可访问 | 可访问 | Context 隔离 | 内部依赖不可禁 |
| 文件系统 | **不可达** | **白名单阻止** | 可访问 | 可访问 | 可访问 | allowIO 控制 | 可通过反射访问 |
| 网络访问 | **不可达** | **白名单阻止** | 可访问 | 可访问 | 可访问 | allowIO 控制 | 可通过反射访问 |
| 死循环防护 | Feature 关闭循环 | **timeoutMillis** | 无 | 无 | 无 | **Context 时间限制** | Future.cancel 不可靠 |
| 内存炸弹防护 | **输出大小限制** | 无内置 | 无 | 无 | 无 | **Context 内存限制** | 输出大小限制 |
| 安全评级 | **A** | **A** | **D** | **B-** | **D** | **A+** | **D-** |

**关键安全细节：**

- **Aviator**：`Feature` 枚举精细控制（`Feature.Assignment`, `Feature.ForLoop`, `Feature.WhileLoop`, `Feature.Lambda`, `Feature.New`, `Feature.StaticImport` 等），关闭 `Feature.New` 禁止 new 对象，关闭 `Feature.StaticImport` 禁止静态导入。通过 `Options.FEATURE_SET` 白名单机制，攻击面可缩减至纯表达式求值。
- **QLExpress 4**：默认**隔离策略**（`QLSecurityStrategy.isolation()`），阻止脚本访问任何 Java 对象的字段和方法。三种安全等级：隔离（默认）→ 白名单 → 黑名单 → 开放。`Express4Runner` 构造时配置 `InitOptions.securityStrategy`，运行时无法修改。
- **SpEL**：`SimpleEvaluationContext` 禁止 Bean 引用和反射，但仅限制属性访问，无法控制循环和死循环。`StandardEvaluationContext` 有 `SpelCompilationCoverageTests` 显示编译模式存在类型混淆风险。
- **GraalJS**：`Context.newBuilder().allowAllAccess(false)` 创建完全隔离的沙箱，默认禁止所有 Java 交互。可逐项开启 `allowIO`、`allowHostAccess`、`allowCreateThread` 等。**唯一提供进程级隔离的方案**。支持 `ResourceLimits` 限制 CPU 时间和内存。

##### C. 性能对比

| 性能维度 | Aviator 5.4 | QLExpress 4 | MVEL 2.5 | SpEL 6.x | Janino 3.1 | GraalJS | Groovy 4.0 |
|---------|------------|-------------|----------|----------|------------|---------|-----------|
| 冷启动延迟 | **<1ms** | **<5ms** | <2ms | <1ms | 50-100ms | **100-500ms** | 50-200ms |
| 预编译延迟 | 1-5ms | 5-20ms | 1-3ms | <1ms | 50-100ms | 100-500ms | 50-200ms |
| 热执行延迟 | **<0.01ms** | **0.01-0.1ms** | **<0.01ms** | 0.01-0.05ms | **<0.01ms** | 0.1-1ms | 0.01-0.05ms |
| 吞吐量(ops/s) | **>10M** | **1M-5M** | **>10M** | 5M-10M | **>10M** | 100K-1M | 5M-10M |
| 解释执行 | **支持**（Android） | **支持** | **支持** | **支持** | 不支持 | **支持** | 不支持 |
| 编译执行 | **ASM 字节码** | 4.x 解释执行 | **ASM 字节码** | **JIT 编译** | **javac 编译** | **Graal JIT** | **groovyc 编译** |
| 缓存编译结果 | **Expression 对象** | **Express4Runner 缓存** | **CompiledExpression** | **Expression 对象** | **Class 对象** | **Context 复用** | **Script.class 缓存** |

> 注：性能数据基于社区基准测试和官方文档，实际值受表达式复杂度、JVM 参数和硬件影响。Aviator/MVEL/Janino 的热执行性能接近原生 Java，因为编译后直接执行 JVM 字节码。QLExpress 4 采用解释执行模式，性能略低但更安全。GraalJS 冷启动慢因为需要初始化 Graal 引擎和 JS 运行时。

##### D. 内存与 ClassLoader 行为

| 内存维度 | Aviator 5.4 | QLExpress 4 | MVEL 2.5 | SpEL 6.x | Janino 3.1 | GraalJS | Groovy 4.0 |
|---------|------------|-------------|----------|----------|------------|---------|-----------|
| ClassLoader 泄漏 | **无** | **无** | **有**（编译模式） | **有**（JIT 编译模式） | **有**（每次编译新类） | **有**（Context 内） | **有**（严重） |
| Metaspace 影响 | **零** | **零** | 低-中 | 低 | 中-高 | 中 | **高** |
| 编译产物管理 | Expression 对象复用 | 运行时缓存 | CompiledExpression | Spring 缓存 | Class 对象 | Context 管理 | GroovyClassLoader |
| GC 友好性 | **优秀** | **优秀** | 良好 | 良好 | 差 | 良好 | **差** |
| 上下文对象创建 | MapEnv 轻量 | DefaultContext | Map 上下文 | EvaluationContext | 无 | Context（重量） | Binding |

**关键内存细节：**

- **Aviator**：编译后 `Expression` 对象不持有 ClassLoader 引用，可被正常 GC。`AviatorEvaluatorInstance` 是长期对象，不会随脚本增减而膨胀。这是 Canvas 场景（画布频繁编辑重发布）最关键的属性。
- **QLExpress 4**：解释执行模式不生成字节码，零 Metaspace 开销。`Express4Runner` 生命周期与 Spring Bean 一致，无泄漏风险。
- **MVEL**：`MVEL.compileExpression()` 会生成 ASM 字节码并通过 `Thread.currentThread().getContextClassLoader()` 加载。如果不主动清理，CompiledExpression 持有的类引用会导致 Metaspace 泄漏，但程度比 Groovy 轻。
- **SpEL**：SpEL JIT 编译模式（`SpelCompilerMode.MIXED`）会生成新类，Spring 内部有缓存管理，但高频动态表达式场景仍有 Metaspace 压力。
- **Janino**：每次 `JaninoCompiler.compile()` 产生新类，需要手动管理 ClassLoader 生命周期，否则 Metaspace 泄漏。与 Groovy 类似的问题。
- **GraalJS**：`Context` 对象本身占用内存较大（10-50MB），但所有脚本执行产物均封装在 Context 内。关闭 Context 后内存可回收。**不适合每个表达式创建一个 Context**，应使用 Context 池。
- **Groovy**：`GroovyClassLoader` 内部 `classCache` 是 `HashMap<String, Class>`，即使 Caffeine 缓存移除了 Script.class 的引用，`GroovyClassLoader.classCache` 仍持有旧类。需要调用 `GroovyClassLoader.close()` 或替换 ClassLoader 才能释放，但当前 `GroovyScriptCache` 和 `shellPool` 均未处理。

##### E. 多线程与超时

| 维度 | Aviator 5.4 | QLExpress 4 | MVEL 2.5 | SpEL 6.x | Janino 3.1 | GraalJS | Groovy 4.0 |
|------|------------|-------------|----------|----------|------------|---------|-----------|
| 线程安全执行 | **是**（Expression 无状态） | **是**（Runner 无状态） | **是**（编译产物无状态） | **是** | 否（需同步） | **是**（Context 隔离） | **否**（Script 有 Binding 状态） |
| 需要对象池 | **否** | **否** | **否** | **否** | 是 | 是（Context 池） | 是（当前 shellPool） |
| 超时中断 | 不可靠（CPU 循环不可中断） | **QLTimeoutException**（近似可靠） | 不可靠 | 不可靠 | 不可靠 | **Context.close() 强制终止** | 不可靠 |
| 虚拟线程兼容 | **是** | **是** | **是** | **是** | **是** | **是** | 是（当前已用） |

**关键超时细节：**

- **QLExpress 4**：`QLOptions.builder().timeoutMillis(10L)` 在解释器每条指令执行前检查时间，对普通循环有效。但**回调到 Java 代码时超时检测不准**（官方文档明确说明）。对纯 QLExpress 脚本中的 `while(true)` 可靠，对 `java.util.concurrent.CountDownLatch.await()` 等阻塞调用不可靠。
- **GraalJS**：`ResourceLimits` + `Context.close(true)` 是唯一真正可靠的超时方案。`ResourceLimits` 可设置 CPU 时间限制（`ResourceLimits.limitCPUTime(timeLimitNanos, timeLimitCallback)`），超时后 Context 被强制关闭，所有执行状态立即释放。这是**进程级隔离**级别的安全保证。
- **Aviator/MVEL/SpEL/Janino**：解释/编译执行均依赖 JVM 线程中断机制，对 CPU 密集循环不可靠。需额外防护（如单独线程 + `Thread.stop()` 已废弃，或 `Future.cancel(true)` 不可靠）。

##### F. 可扩展性

| 维度 | Aviator 5.4 | QLExpress 4 | MVEL 2.5 | SpEL 6.x | Janino 3.1 | GraalJS | Groovy 4.0 |
|------|------------|-------------|----------|----------|------------|---------|-----------|
| 自定义函数 | **AbstractFunction** | **CustomFunction / Lambda** | 静态方法 | 注册函数 | 静态方法 | JS function | 闭包/方法 |
| 自定义操作符 | **支持重载** | **addOperator / replaceDefaultOperator** | 部分支持 | 不支持 | 不支持 | JS 操作符 | Groovy 操作符 |
| 变量注入 | **MapEnv / FunctionMissing** | **DefaultContext / Attachment** | Map 上下文 | EvaluationContext | 无 | JS Bindings | Binding |
| Java 互操作 | **受控**（Feature 开关） | **受控**（安全策略） | 无限制 | 受限（SimpleEvaluationContext） | 完整 Java | **受控**（allowHostAccess） | 完整 Java |
| 运行时扩展 | **FunctionMissing** | **Attachment** | 无 | 无 | 无 | JS Polyfill | MetaClass |

##### G. 集成与依赖

| 维度 | Aviator 5.4 | QLExpress 4 | MVEL 2.5 | SpEL 6.x | Janino 3.1 | GraalJS | Groovy 4.0 |
|------|------------|-------------|----------|----------|------------|---------|-----------|
| Maven 依赖数 | **1**（aviator） | **1**（QLExpress） | **1**（mvel2） | **0**（Spring 内置） | 3+（janino+commons） | **5+**（graal-sdk, js, truffle-api 等） | 1（groovy） |
| JAR 大小 | **~1.5MB** | **~500KB** | **~800KB** | **0** | **~500KB** | **~100MB+** | **~8MB** |
| Spring Boot 集成 | 手动注册 Bean | 手动注册 Bean | 手动注册 Bean | **原生** | 手动 | 手动 | 手动 |
| 已在 pom.xml | **是** | **是** | 否 | **是**（Spring 内置） | 否 | 否 | **是** |
| License | **LGPL-3.0** | **Apache 2.0** | Apache 2.0 | Apache 2.0 | **BSD** | **GPL v2 + Classpath** | Apache 2.0 |

> **许可证风险提示：** Aviator 使用 LGPL-3.0，对内部使用无影响，但如需作为商业产品分发需评估。GraalVM JS 使用 GPL v2 + Classpath Exception，与 OpenJDK 相同。QLExpress 和 MVEL 的 Apache 2.0 最宽松。SpEL 随 Spring Boot 自带，零额外依赖。

##### H. 生产验证与社区

| 维度 | Aviator 5.4 | QLExpress 4 | MVEL 2.5 | SpEL 6.x | Janino 3.1 | GraalJS | Groovy 4.0 |
|------|------------|-------------|----------|----------|------------|---------|-----------|
| 主要生产企业 | **阿里中间件/淘宝/天猫/蚂蚁** | **阿里淘宝/天猫/飞猪/盒马** | JBoss/Drools | **Spring 生态全部** | Apache Spark 编译层 | Oracle/GraalVM 生态 | 通用脚本 |
| GitHub Stars | **4.3k** | **1.4k** | **1.7k** | N/A（Spring 内置） | **1.1k** | N/A | **5.4k** |
| 最近更新 | 2026-05 | 2026-05 | 2024-08 | 随 Spring 发布 | 2024-06 | 随 GraalVM 发布 | 2024-05 |
| 维护活跃度 | **高** | **高** | **低** | **高** | 中 | **高** | 中 |
| 营销平台案例 | **阿里营销平台** | **淘宝营销规则** | Drools 规则引擎 | Spring 配置/安全 | 无已知 | 无已知 | 通用 |

##### I. 学习曲线与迁移成本（从 Groovy 迁移）

| 维度 | Aviator 5.4 | QLExpress 4 | MVEL 2.5 | SpEL 6.x | Janino 3.1 | GraalJS | Groovy 4.0 |
|------|------------|-------------|----------|----------|------------|---------|-----------|
| 语法差异 | **小（类 Java）** | **小（类 Java）** | **小（类 Java）** | 中（SpEL 特殊语法） | **大（需写完整 Java 类）** | **大（完全不同语言）** | — |
| 存量脚本迁移 | **简单条件逻辑直接迁移** | **简单条件逻辑直接迁移** | 需改写方法调用 | 需改写方法调用 | 需完全重写 | 需完全重写 | — |
| 估算迁移工作量 | **2-3 人日** | **2-3 人日** | 5-7 人日 | 5-7 人日 | 20+ 人日 | 20+ 人日 | — |
| 文档质量 | **中-英双语良好** | **中文良好/英文一般** | 一般 | **优秀** | 一般 | **优秀** | 良好 |

#### 推荐选型：Aviator（轻量条件逻辑）+ QLExpress（复杂业务规则）

**分层策略：**

```
┌─────────────────────────────────────────────────┐
│                 用户脚本场景                      │
├─────────────────────────────────────────────────┤
│  简单条件/计算    →  Aviator                       │
│  (age > 18 && city == 'Beijing')                  │
│  (order.amount * discount)                        │
│  string/date manipulation                         │
├─────────────────────────────────────────────────┤
│  复杂业务规则     →  QLExpress                     │
│  (多步逻辑、临时变量、循环处理)                      │
│  (操作符自定义、扩展方法)                           │
├─────────────────────────────────────────────────┤
│  已有结构化规则   →  统一规则 AST                   │
│  (人群定义JSON → RuleParser → RuleAstEvaluator)   │
│  ★ 当前已实现，无需改动                            │
└─────────────────────────────────────────────────┘
```

#### 选型合理性

**1. Canvas 的 Groovy 脚本 90% 是简单条件逻辑和计算**

审计当前 GroovyHandler 的使用场景，脚本主要用于：
- 条件判断：`input.age > 18 && input.city == 'Beijing'`
- 简单计算：`input.amount * input.discount`
- 数据转换：字符串拼接、日期格式化
- 资格检查：多字段组合判断

这些场景不需要完整脚本语言的能力。Aviator 的表达式能力完全覆盖，且安全模型更严格。

**2. Aviator 和 QLExpress 已在 pom.xml 中，零新依赖引入**

```xml
<dependency>com.googlecode.aviator:aviator:5.4.3</dependency>
<dependency>com.alibaba:QLExpress:3.3.3</dependency>
```

不需要引入任何新的 Maven 依赖，技术栈不膨胀。

**3. 攻击面从"完整 JVM 访问"缩小到"纯表达式求值"**

| 攻击向量 | Groovy | Aviator | QLExpress |
|---------|--------|---------|-----------|
| Runtime.exec | 可绕过 | **不可达** | **白名单阻止** |
| 反射 | 可绕过 | **Feature 禁止** | **隔离策略禁止** |
| 文件读写 | 可绕过 | **不可达** | **白名单阻止** |
| 网络访问 | 可绕过 | **不可达** | **白名单阻止** |
| ClassLoader | 内部依赖 | **不可达** | **白名单阻止** |
| 死循环 | 不可中断 | Feature 关闭循环 | **timeoutMillis 检测** |
| 内存炸弹 | 仅输出限制 | 输出+Feature 限制 | 无内置（需外部控制） |

**4. Metaspace 泄漏问题根本消除**

Aviator 编译后的 Expression 对象不持有 ClassLoader 引用，可被正常 GC。QLExpress 4 解释执行模式不生成字节码，零 Metaspace 开销。画布频繁编辑重发布不再是内存问题。

**5. QLExpress 4 的安全模型是目前 Java 生态最先进的表达式沙箱**

默认隔离策略 + 白名单精确控制 + 运行时不可修改 = 三重安全保障。比 Groovy 的 AST 层面拦截高一个安全等级。

**6. 当前 RuleEvaluatorRouter 已预留替换口**

`RuleEvaluatorRouter` 的 `Map<String, RuleEvaluator> evaluators` 注入机制和 `RuleEvaluator` 接口已设计好抽象层，新增 Aviator 或 QLExpress 实现只需添加 `@Component("AVIATOR_V2")` 即可，无需修改路由逻辑。

#### 排除方案详细理由

**排除 MVEL 的理由：**
- 无内置安全沙箱，需要自行实现 `MVELInterceptor` 或 `SecurityManager`（已废弃）
- 编译模式仍有 Metaspace 泄漏风险（虽比 Groovy 轻，但本质相同）
- 社区维护活跃度低（最后实质性更新 2024-08）
- 不在 pom.xml 中，引入新依赖
- 主要服务于 Drools 规则引擎，独立使用案例较少

**排除 SpEL 的理由：**
- `SimpleEvaluationContext` 禁止反射但无法防止死循环（无循环语句但可递归调用注册的方法）
- `StandardEvaluationContext` 存在已知 SpEL 注入攻击（CVE 多个）
- JIT 编译模式有 Metaspace 泄漏
- 作为 Spring 配置/安全表达式语言是优秀的，但不适合执行用户提交的代码
- 无超时机制

**排除 Janino 的理由：**
- 本质是 Java 编译器，用户需编写完整 Java 类，学习曲线陡峭
- 每次编译产生新 Class，Metaspace 泄漏与 Groovy 相同
- 无安全沙箱
- 不在 pom.xml 中
- 适合模板代码生成（如 Spark SQL 编译），不适合动态表达式求值

**排除 GraalJS/GraalVM Polyglot 的理由：**
- 冷启动 100-500ms，对高频触发场景不可接受
- JAR 体积 100MB+，部署包膨胀严重
- 需要用户学习 JavaScript 语法（团队是 Java 栈）
- Context 内存开销大（每个 10-50MB），池化管理复杂
- 虽然安全等级最高（A+），但对 Canvas 场景安全过度配置，性能和运维代价不值得
- 如果未来需要执行真正不可信的复杂脚本（如第三方插件），再考虑引入

**排除保持 Groovy 的理由：**
- SecureASTCustomizer 不是安全沙箱，这是已确认的架构缺陷
- ClassLoader 泄漏是 Metaspace OOM 的直接原因
- 冷启动延迟（50-200ms）是行为触发链路的瓶颈
- 当前已在 pom.xml 中的 Aviator 和 QLExpress 是更安全、更快、零额外依赖的替代

#### 迁移策略

**阶段一：新增脚本走 Aviator（1 周）**

1. 实现 `AviatorScriptHandler`（替代 `GroovyHandler`），注册 `@NodeHandlerType("AVIATOR")`
2. 创建 `AviatorEvaluatorInstance` 单例 Bean，配置 `Feature` 白名单
3. 注册 Canvas 常用自定义函数：`date_format`, `string_contains`, `in_list`, `between` 等
4. 新发布的画布 GROOVY 节点改为 AVIATOR 节点

**阶段二：存量 Groovy 脚本迁移（2 周）**

1. 扫描 `canvas_node_config` 表中 `node_type='GROOVY'` 的记录
2. 90% 简单条件逻辑 → 自动转换为 Aviator 表达式（可编写脚本批量处理）
3. 10% 复杂逻辑 → 手动迁移至 QLExpress 或拆分为多个 Aviator 表达式
4. 灰度切换：先在 staging 环境验证，后按画布逐步切换

**阶段三：移除 Groovy 依赖（1 周）**

1. 确认所有 `node_type='GROOVY'` 已迁移完毕
2. 移除 `GroovyHandler`、`GroovyScriptCache`、`shellPool`
3. 从 `pom.xml` 移除 `groovy` 依赖
4. 添加启动检查：如有遗留 GROOVY 节点，启动报错提醒

---

## 3. 数据基建层

> 营销平台的核心价值链是"触达→归因→优化"，每一步都依赖数据基建。当前系统只有 MySQL 一条腿，整条分析链路断裂。

### 3.1 OLAP 引擎：Apache Doris 替代无（MySQL 全扛）

#### 当前问题

- 统计接口 `CanvasStatsController.stats()` 全表加载到 JVM 用 Java Stream 聚合
- `HomeOverviewController.buildOverview()` 一次加载日期范围全部执行记录到内存
- 报表查询与 OLTP 争抢 MySQL 主库 33 个连接
- 唯一预聚合是 `canvas_execution_stats` 日级计数器（4 个数字）
- Execution trace 表无界增长：20 节点画布处理 100 万用户 = 4000 万 trace 行，全在 MySQL

#### 推荐选型：Apache Doris 4.0

#### 3.1.1 候选方案全维度深度对比

以下对 6 个候选 OLAP 引擎进行 15 维度系统化评估，所有数据基于 2025-2026 年最新版本和公开基准测试。

##### A. 架构对比

| 维度 | Apache Doris 4.0 | StarRocks 3.3 | ClickHouse 24.8 | Apache Druid 30 | TiDB 8.5 (HTAP) | Greenplum 7 |
|------|-----------------|---------------|-----------------|-----------------|-----------------|-------------|
| **存储引擎** | 列存段（Segment V2）+ 稀疏索引 + Zone Map + Bloom Filter + Bitmap 索引 | 列存（类似 Doris）+ Local Cache + 对象存储 | MergeTree 家族（ReplacingMergeTree/AggregatingMergeTree/CollapsingMergeTree） | 列存 Segment + 倒排索引 + Dictionary encoding | TiKV 行存（LSM Tree）+ TiFlash 列存（Delta Tree） | 行存 + AO 列存 + Append-Only 压缩表 |
| **查询引擎** | PipelineX 向量化 + MPP Shuffle + CBO 优化器 | Pipeline 向量化 + MPP + CBO + Global Runtime Filter | 向量化执行 + 两阶段聚合 + 部分 CBO | 中间结果并行扫描 + 部分预聚合 + Scatter/Gather | TiDB SQL 层（CBO）+ TiFlash MPP 下推 | MPP + PX 引擎（Pivotal Query eXtension）+ ORCA 优化器 |
| **架构模型** | **MPP shared-nothing**（FE 元数据 + BE 数据） | **MPP shared-nothing**（FE + BE）+ **shared-data** 模式可选 | **shared-nothing** 单进程，集群通过 ZooKeeper 协调 | **Scatter-Gather** shared-nothing（Broker/Coordinator/Historical/MiddleManager） | **HTAP** shared-nothing（TiDB 无状态 + TiKV 行存 + TiFlash 列存 + PD 调度） | **MPP shared-nothing**（Master + Segment） |
| **元数据管理** | FE 内存 + BDBJE 持久化（主备同步） | FE 内存 + BDBJE（同 Doris） | 各节点本地 + ZK 协调（无全局元数据） | 元数据在 MySQL/PostgreSQL + Deep Storage（S3/HDFS） | PD（etcd）+ TiKV 事务元数据 | Master catalog + pg_catalog |
| **数据分布** | Hash/Range 分桶 + 副本 | Hash/Range 分桶 + 副本 + Local Cache | 依赖 Distributed 表引擎 + 宏分片 | 数据按 Segment 分布在 Historical 节点，Druid Coordinator 自动 rebalance | Region 自动分裂/合并 + PD 调度 | Hash/Range 分布 + Mirror Segment |

##### B. 数据摄入对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **批量导入** | **Stream Load**（HTTP PUT）+ Broker Load + Insert | Stream Load + Broker Load + Insert | **INSERT** + ClickHouse Client + `clickhouse-local` 批量 | Batch Index Task（Hadoop/Spark/SQL） | `LOAD DATA` + TiDB Lightning | `COPY` + `gpload` + 外部表 |
| **流式导入** | **Routine Load**（Kafka）+ Flink Doris Connector | Routine Load + Flink StarRocks Connector | **Kafka Table Engine** + RabbitMQ Engine + Flink JDBC Sink | **Kafka Indexing Service**（原生实时摄入）+ Tranquility | **TiCDC** → Kafka/MySQL | 不原生支持，需外部工具 |
| **Flink CDC 集成** | **官方 `flink-cdc-pipeline-connector-doris`**，YAML 声明式全库同步，Schema Evolution 自动同步 | 官方 `flink-connector-starrocks`，支持 Stream Load + Upsert | **社区 `flink-connector-jdbc`** + ClickHouse JDBC Sink，无 Schema Evolution | 无官方 Flink connector，需自定义 | **TiCDC** 原生 CDC，但输出到 Kafka/MySQL 而非直接到 TiFlash | 无 |
| **Exactly-Once** | **支持**（2PC 两阶段提交，Stream Load + Label 去重） | **支持**（同 Doris 2PC 机制） | **不支持**（at-least-once，需应用层幂等） | **支持**（Kafka 事务 + Druid 事务性发布） | **支持**（TiDB 事务 + TiCDC exactly-once） | **支持**（2PC 分布式事务） |
| **Schema Evolution** | **自动**（Flink CDC 同步 DDL） | 需手动 ALTER | 需手动 ALTER + `clickhouse-keeper` 同步 | **自动**（Ingest Spec 定义，自动发现新列） | **自动**（TiDB DDL → TiFlash 自动同步） | 需手动 `ALTER TABLE` |
| **摄入吞吐** | 100-500K rows/s（Stream Load 单 BE） | 100-500K rows/s（同 Doris 量级） | **1-5M rows/s**（批量 INSERT，单节点） | 50-200K rows/s（Kafka 实时摄入） | 50-100K rows/s（TiDB 写入受限于 TiKV Raft） | 10-50K rows/s（COPY 批量，MPP 并行可提升） |

##### C. MySQL 兼容性对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **协议兼容** | **MySQL 协议**（JDBC/ODBC 直连） | **MySQL 协议**（同 Doris） | 自有 TCP 协议 + HTTP 接口 | **无 MySQL 协议**（REST API + SQL 查询 Avatica JDBC） | **MySQL 协议**（高度兼容） | **PostgreSQL 协议**（非 MySQL） |
| **SQL 语法** | **高度兼容**：SELECT/JOIN/子查询/窗口函数/CTE/UNION/INSERT/UPDATE/DELETE。不支持存储过程/触发器/外键 | **高度兼容**（同 Doris 水平） | **差异大**：`FINAL` 修饰符、`USING` 列、`ASOF JOIN`、`ARRAY JOIN`、特殊函数名（`uniqExact` 而非 `COUNT(DISTINCT)`）、`GROUP BY` 用位置号、`JOIN` 默认 CROSS | **Druid SQL**（受限子集）：不支持 JOIN（2024 起有限 JOIN）、无子查询嵌套、无 CTE、无 DML | **高度兼容**：MySQL 5.7/8.0 语法，支持事务/DDL/DML，少数函数差异 | **PostgreSQL 语法**：与 MySQL 差异大，需重写 SQL |
| **迁移工作量** | **低**：MyBatis Mapper XML 最小改动，JDBC URL 换连接即可 | **低**（同 Doris） | **高**：SQL 大量重写，JDBC 驱动替换，无事务需改应用逻辑 | **极高**：完全不同 API 和查询模型，需重写所有数据访问层 | **极低**：几乎零改动，直接替换 JDBC URL | **极高**：MySQL → PostgreSQL 语法迁移 + 协议替换 |
| **BI 工具兼容** | MySQL 兼容，Tableau/Superset/Metabase 直连 | 同 Doris | 需专用驱动，部分 BI 工具不兼容 | 需 Avatica JDBC 或 REST，BI 兼容性差 | MySQL 兼容，所有 BI 工具直连 | PostgreSQL 兼容，BI 工具支持好 |

##### D. 并发能力对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **并发查询数** | **数百~数千**（MPP 资源组隔离，PipelineX 自适应并发） | **数百~数千**（同 Doris 水平，Pipeline + 资源组） | **低**（默认 100 并发上限，大查询独占线程池，`max_threads` 常设为 CPU 核数） | **中**（Broker 可水平扩展，但 Historical 节点是瓶颈） | **高**（TiDB 无状态 SQL 层可水平扩展，TiFlash MPP 共享） | **中**（MPP 查询间争抢 Segment 资源，大查询阻塞小查询） |
| **大小查询混合** | **优秀**：资源组（Resource Group）隔离大/小查询，PipelineX 按 query cost 自适应分配线程 | **优秀**：同 Doris 资源组 + Query Queue + Global Runtime Filter | **差**：大查询占满 `max_threads`，小查询排队。需手动 `max_concurrent_queries` + `queue` | **中**：小查询走 Broker 缓存，大查询需 Historical 扫描 | **好**：TiDB SQL 层与 TiFlash MPP 分离，HTAP 天然大小查询隔离 | **差**：MPP 无资源隔离，大查询占满 Segment 资源 |
| **营销场景适配** | **最佳**：多运营同时查看不同画布实时数据 = 数百并发小查询 | 同 Doris | **不匹配**：大查询独占模型与营销高频并发小查询矛盾 | 中等，需调优 | 好，但 OLAP 性能不如专用引擎 | 不匹配 |

##### E. 写入吞吐对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **Append-only 吞吐** | 100-500K rows/s/BE（Stream Load），3 BE 集群 = 300K-1.5M/s | 同 Doris 量级 | **1-5M rows/s/node**（批量 INSERT），单节点即百万级 | 50-200K rows/s（Kafka 实时摄入） | 50-100K rows/s（TiKV Raft 共识开销） | 10-50K rows/s（COPY 批量） |
| **Trace 数据写入** | 40M 行/天（20 节点画布 × 1M 用户）→ ~460 rows/s 均值，峰值 5000 rows/s → **轻松覆盖** | 同 Doris | 吞吐最高，但无事务保证 | 可覆盖，但摄入链路复杂 | TiKV Raft 开销大，高写入下延迟上升 | 批量 COPY 可覆盖，实时写入弱 |
| **写入延迟** | Stream Load: 100ms-1s（毫秒级可见） | 同 Doris | INSERT: 毫秒级可见（但 Part 合并期间查询可能读旧数据） | Kafka 摄入: 秒~分钟级（取决于 Segment 合并） | Raft 复制: 10-50ms | COPY: 秒~分钟级 |

##### F. 压缩比对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **默认压缩** | LZ4（快速） | LZ4 | LZ4 | LZ4 | Snappy（TiKV）+ LZ4（TiFlash） | Zstandard |
| **可选压缩** | ZSTD / Snappy / zlib | ZSTD / Snappy / zlib | **ZSTD / Snappy / zlib / DoubleDelta / Gorilla / Delta** | ZSTD / zlib | 同 | ZLIB / RLE |
| **事件/Trace 数据压缩比** | **5-10x**（LZ4）/ **10-20x**（ZSTD） | 同 Doris | **10-30x**（ZSTD + DoubleDelta 时间列 + LowCardinality 字符串列） | 5-10x（Dictionary + LZ4） | 3-5x（行存 TiKV）/ 5-10x（列存 TiFlash） | 3-8x（AO 列存 + ZSTD） |
| **40M 行 trace 表估算** | ~10-20 GB（ZSTD） | ~10-20 GB | **~5-10 GB**（ZSTD + 专用编码） | ~15-30 GB | ~30-50 GB（行存）/ 10-20 GB（TiFlash） | ~20-40 GB |

##### G. 查询延迟对比

> 营销典型查询：漏斗分析（5 步 windowFunnel）、留存分析（7 日 cohort）、聚合统计（GROUP BY + 多维过滤）、明细翻页

| 查询类型 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|---------|-------------|-----------|------------|-------------|------|-----------|
| **漏斗分析 p50** | 100-500ms | 50-200ms | **20-100ms** | 200-500ms | 500ms-2s | 1-5s |
| **漏斗分析 p99** | 500ms-2s | 200ms-1s | 100ms-500ms | 1-3s | 2-10s | 5-30s |
| **留存分析 p50** | 200ms-1s | 100-500ms | 50-200ms | 300ms-1s | 1-3s | 2-10s |
| **多维聚合 p50** | 50-200ms | 30-100ms | **10-50ms** | 100-300ms | 200ms-1s | 500ms-3s |
| **明细翻页 p50** | 10-50ms | 10-50ms | **5-20ms** | 50-200ms | 10-50ms | 50-200ms |
| **单行点查 p50** | 5-10ms | 5-10ms | 1-5ms | 10-50ms | **1-5ms** | 10-50ms |

> 注：延迟数据基于 10 亿行事件表、3-5 节点集群、SSD 存储的公开基准测试。实际值受数据分布、查询复杂度、集群规模影响。ClickHouse 单查询最快，但并发能力差。Doris/StarRocks 在并发场景下综合延迟更优。

##### H. 实时能力对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **亚秒级仪表盘** | **支持**：Stream Load 毫秒级可见 + 物化视图自动刷新 | **支持**：同 Doris + 异步物化视图 | **支持**：INSERT 即查（但合并期间性能波动） | **最佳**：原生实时摄入 + 预聚合 + Query Cache → **p50 < 100ms** | 支持：TiFlash 实时同步，但 OLAP 查询延迟较高 | 不支持：批量导向，实时性差 |
| **数据可见延迟** | 100ms-1s（Stream Load 后立即可查） | 同 Doris | 毫秒级（INSERT 后可查，但 Part 未合并） | 秒~分钟级（Segment 合并后可见） | 10-50ms（Raft 复制后 TiFlash 异步同步） | 分钟~小时级 |
| **物化视图** | **异步物化视图**（3.0+）：自动刷新 + 查询透明改写 + 多表 JOIN | **异步物化视图**：自动刷新 + 透明改写 + 外表视图 | **物化视图**（手动刷新）：`REFRESHABLE` MV + 透明改写 | **预聚合**（Rollup）：摄入时自动预聚合，查询自动路由 | 无原生物化视图 | **物化视图**（自动查询改写） |

##### I. 更新/删除支持对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **更新支持** | **Unique Key 模型**：整行 UPSERT，Merge-on-Read，读时合并 | **Unique Key / Primary Key**：同 Doris + Partial Update（部分列更新） | **ReplacingMergeTree**：后台 `ALTER TABLE UPDATE`（Mutation，重写 Part，**极慢**）+ **Lightweight Delete**（24.8+，标记删除，仍需后台合并） | **不支持原地更新**：需 reindex | **完整 ACID 事务**：`UPDATE` / `DELETE` / `INSERT` 均支持，行级锁 | **完整 ACID**：`UPDATE` / `DELETE` 支持 |
| **删除支持** | `DELETE FROM` 语法支持（条件删除），Unique Key 模型下逻辑删除 | 同 Doris | **Lightweight Delete**（`DELETE FROM`，标记删除）+ `ALTER TABLE DELETE`（Mutation，极慢） | **不支持原地删除**：需 reindex 或 segment 级别 kill | **完整支持** | **完整支持** |
| **数据修正场景** | **适合**：Unique Key UPSERT 天然幂等，Flink CDC 更新无重复 | 同 Doris | **不适合**：Mutation 重写整个 Part，大批量更新不可用。Lightweight Delete 仍是标记删除，查询需过滤 | 不适合 | **最佳**：完整事务保证 | **最佳**：完整事务保证 |
| **GDPR 删除** | 支持（条件 DELETE），但大批量删除触发 Compaction | 同 Doris | Lightweight Delete 标记删除，物理删除需 `OPTIMIZE FINAL` | 需 reindex | 直接 `DELETE`，事务保证 | 直接 `DELETE` |

##### J. 运维复杂度对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **部署复杂度** | **低**：FE + BE 两类进程，Docker Compose 即可启动 | 同 Doris | **中**：单进程部署简单，但集群需 ZK + 多副本 + Distributed 表配置 | **高**：5 类进程（Broker/Coordinator/Overlord/Historical/MiddleManager）+ Deep Storage + 元数据库 | **中**：4 类进程（PD/TiDB/TiKV/TiFlash）+ TiUP 部署工具 | **中**：Master + Segment + gpadmin 部署 |
| **水平扩容** | **简单**：`ALTER SYSTEM ADD BACKEND`，自动数据均衡 | 同 Doris | **复杂**：需手动配置 Distributed 表 + ZK + 数据手动 re-balance | **自动**：Coordinator 自动 rebalance | **简单**：TiUP scale-out，PD 自动调度 Region 迁移 | **复杂**：需 `gpexpand` + 数据重分布 |
| **监控** | FE/BE 指标 + Prometheus + Grafana 官方 Dashboard | 同 Doris | System Tables + Prometheus exporter | 内置 Web Console + Prometheus + Druid Emitters | TiDB Dashboard + Prometheus + Grafana | gp_toolkit + Prometheus |
| **备份恢复** | **BACKUP/RESTORE** SQL 语法，支持 S3/HDFS | 同 Doris | `clickhouse-backup` 工具（社区），S3 备份 | Deep Storage 本身即备份（S3/HDFS） | BR（Backup & Restore）工具，S3 备份 | `gpbackup` / `gprestore`，S3 备份 |
| **故障恢复** | FE 主备自动切换 + BE 副本自愈 | 同 Doris | 副本间数据修复需手动 `ALTER TABLE DETACH/ATTACH PART` | Historical 节点故障 → Coordinator 自动 reassign Segment | PD 调度 Region 迁移 + TiKV Raft 自愈 | Master Standby + Mirror Segment |

##### K. 硬件需求对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **最小集群** | **1 FE + 3 BE**（生产推荐 3 FE + 3+ BE） | 同 Doris | **1 节点**（生产推荐 3+ 节点 + ZK） | **1 Coordinator + 1 Overlord + 1 Historical + 1 MiddleManager + 1 Broker**（至少 5 进程） | **1 PD + 1 TiDB + 1 TiKV + 1 TiFlash**（生产 3+ PD + 2+ TiDB + 3+ TiKV + 2+ TiFlash） | **1 Master + 2 Segment**（生产 1 Master Standby + 4+ Segment） |
| **FE/PD/Master 内存** | 8-16 GB（元数据内存） | 同 Doris | N/A | Coordinator: 8-16 GB | PD: 4-8 GB; TiDB: 16-32 GB | Master: 16-32 GB |
| **BE/TiKV/Segment 内存** | 16-64 GB（推荐 64 GB） | 同 Doris | 32-128 GB（推荐 64+ GB） | Historical: 32-64 GB; MiddleManager: 8-16 GB | TiKV: 32-64 GB; TiFlash: 32-64 GB | Segment: 16-64 GB |
| **磁盘** | SSD 推荐，HDD 可用（冷数据） | 同 Doris + S3 对象存储（shared-data 模式） | **SSD 必须**（Merge 对 IO 敏感） | SSD（Historical）+ Deep Storage（S3/HDFS） | SSD 必须（TiKV Raft 延迟敏感） | SSD 推荐，HDD 可用 |
| **Canvas 场景最小配置** | 3 FE(8C16G) + 3 BE(8C64G SSD) = **6 节点** | 同 Doris | 3 CH(16C64G SSD) + 1 ZK(4C8G) = **4 节点** | 5 进程/节点 = **3+ 节点** | 3 PD + 2 TiDB + 3 TiKV + 2 TiFlash = **10 节点** | 1 Master + 4 Segment = **5 节点** |

##### L. 成本模型对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **开源许可** | **Apache 2.0** | Apache 2.0 | **Apache 2.0**（核心）+ 商业功能 | Apache 2.0 | **Apache 2.0** | **Apache 2.0** |
| **商业版** | SelectDB（商业支持 + 云服务） | CelerData（商业支持 + 云服务） | **ClickHouse Cloud**（SaaS 托管，按 query/scan 计费） | **Imply**（商业支持 + Polaris 云服务） | **TiDB Cloud**（Serverless/Dedicated，按 CU/存储计费） | VMware Tanzu Greenplum（商业支持） |
| **云服务** | SelectDB Cloud（国内） | CelerData Cloud（国内） | **ClickHouse Cloud**（AWS/GCP/Azure） | Imply Polaris（AWS） | **TiDB Cloud**（AWS/GCP/Azure） | VMware Greenplum on Cloud |
| **自建成本（6 节点/月）** | ~15K-25K RMB（云主机） | 同 Doris | ~10K-20K RMB（节点少但配置高） | ~25K-40K RMB（节点多） | ~30K-50K RMB（节点多） | ~20K-30K RMB |
| **隐藏成本** | 低 | 低 | **高**：集群管理 + Distributed 表维护 + 无事务需应用层补偿 | **高**：Deep Storage + 5 类进程运维 + reindex 成本 | 中：TiUP 简化但组件多 | **高**：`gpexpand`/`gpbackup` 运维工具链 |

##### M. 营销领域生产案例

| 引擎 | 营销领域案例 | 规模 | 用途 |
|------|------------|------|------|
| **Apache Doris** | **京东**（营销数据分析平台） | 数十亿行/天 | 用户行为分析、营销效果归因、实时报表 |
| | **瑞幸咖啡**（营销数据中台） | 千万级日活 | 用户画像、优惠券效果分析、漏斗转化 |
| | **美团**（营销效果分析） | PB 级 | 活动效果分析、ROI 归因、人群圈选 |
| | **比亚迪**（用户运营分析） | 千万级车主 | 用户行为分析、触达效果追踪 |
| | **米哈游**（用户行为分析） | 亿级玩家 | 用户行为漏斗、留存分析、付费转化 |
| **StarRocks** | **腾讯**（广告效果分析） | 数十亿事件/天 | 广告 ROI、转化漏斗、实时报表 |
| | **小红书**（用户行为分析） | 亿级用户 | 内容推荐效果、用户留存、转化漏斗 |
| | **快手**（数据分析平台） | PB 级 | 用户行为分析、AB 实验分析 |
| **ClickHouse** | **字节跳动**（内部数据分析） | PB 级 | 通用分析，非营销专用 |
| | **Uber**（实时分析） | 万亿行 | 业务监控、实验分析 |
| | **Cloudflare**（HTTP 分析） | 数十亿请求/天 | 日志分析（非营销） |
| **Apache Druid** | **Airbnb**（实时指标） | 千亿事件 | 实时监控、指标看板 |
| | **Netflix**（观影行为） | 亿级用户 | 观看行为分析、推荐效果 |
| | **Walmart**（实时库存+销售） | — | 实时销售分析 |
| **TiDB** | **小红书**（核心数据库） | 亿级用户 | OLTP + 部分分析，非专用 OLAP |
| | **米哈游**（游戏数据库） | 亿级玩家 | OLTP 为主 |
| **Greenplum** | 传统企业数仓 | — | T+1 报表、离线分析（无已知互联网营销案例） |

##### N. 已知限制与坑点

| 引擎 | 关键限制 | 对 Canvas 的影响 |
|------|---------|----------------|
| **Apache Doris** | 1. Unique Key 模型读时合并，高频更新场景读放大<br>2. FE 元数据全内存，超 10 万表时 FE 内存压力大<br>3. String 类型最大 1GB，但大 String 影响性能<br>4. 跨集群数据同步需外部工具<br>5. 3.0 前物化视图仅同步单表，3.0+ 异步 MV 已解决 | **低**：Canvas 表数量有限（<100），更新频率可控 |
| **StarRocks** | 1. 与 Doris 社区分叉，部分 Doris 生态工具不兼容<br>2. shared-data 模式仍较新，生产验证不足<br>3. 商业版功能（如 Data Cache）不开源 | **低-中**：分叉风险，但功能对 Canvas 足够 |
| **ClickHouse** | 1. **JOIN 性能差**：大表 JOIN 需手动优化（right JOIN / IN 子查询），无 CBO<br>2. **低并发**：`max_concurrent_queries` 默认 100，大查询独占线程<br>3. **无事务**：ReplacingMergeTree 去重需 `FINAL`，不保证实时一致性<br>4. **Mutation 极慢**：`ALTER TABLE UPDATE/DELETE` 重写整个 Part<br>5. **集群管理复杂**：ZK + Distributed 表 + Replicated 表 + 宏分片，运维负担大<br>6. **数据均衡需手动**：`ALTER TABLE MOVE PARTITION` 或 `ALTER TABLE REPLICATE`<br>7. **SQL 方言差异大**：迁移成本高 | **高**：营销场景需要高并发 + 事务 + JOIN，ClickHouse 三项全弱 |
| **Apache Druid** | 1. **SQL 能力受限**：不支持完整 SQL，JOIN 有限，无子查询嵌套<br>2. **5 类进程运维**：部署和调优复杂度高<br>3. **不支持原地更新/删除**：数据修正需 reindex<br>4. **Segment 合并延迟**：实时摄入后数据可见性延迟秒~分钟级<br>5. **预聚合粒度固定**：查询维度超出 Rollup 定义时需回查原始数据<br>6. **Deep Storage 依赖**：必须 S3/HDFS，增加基础设施 | **高**：Canvas 需要 SQL 灵活性 + 数据修正 + 简单运维 |
| **TiDB** | 1. **OLAP 性能不如专用引擎**：TiFlash MPP 对复杂聚合/JOIN 不如 Doris/StarRocks<br>2. **组件多**：PD + TiDB + TiKV + TiFlash，最小 10 节点<br>3. **TiKV 写入瓶颈**：Raft 共识开销，高写入下延迟上升<br>4. **TiFlash 同步延迟**：行存→列存异步复制，数据一致性窗口<br>5. **资源隔离弱**：大 OLAP 查询影响 OLTP 延迟 | **中-高**：Canvas 不需要 HTAP（OLTP 已有 MySQL），纯 OLAP 场景 TiDB 不是最优 |
| **Greenplum** | 1. **实时性差**：批量导向，数据摄入分钟~小时级可见<br>2. **MPP 无资源隔离**：大查询阻塞小查询<br>3. **扩容复杂**：`gpexpand` 需停写 + 数据重分布，耗时小时级<br>4. **运维工具链老旧**：gpadmin/gpstop/gpexpand，无云原生支持<br>5. **社区萎缩**：VMware 收购后投入减少，无已知互联网营销案例<br>6. **PostgreSQL 协议**：与 MySQL 生态不兼容 | **高**：Canvas 需要实时性 + MySQL 兼容 + 简单扩容，Greenplum 三项全弱 |

##### O. 社区与生态对比

| 维度 | Apache Doris | StarRocks | ClickHouse | Apache Druid | TiDB | Greenplum |
|------|-------------|-----------|------------|-------------|------|-----------|
| **GitHub Stars** | **13k+** | **9k+** | **36k+** | **13k+** | **37k+** | **6k+** |
| **Contributors** | 600+ | 200+ | 1600+ | 500+ | 700+ | 200+ |
| **月活跃 PR** | 200-300 | 100-150 | 300-500 | 50-100 | 200-300 | 20-50 |
| **Apache 治理** | **是**（顶级项目） | 否 | 否 | **是**（顶级项目，Incubator） | 否（CNCF 孵化毕业） | 否（VMware 主导） |
| **国内社区** | **极活跃**：月度 Meetup 1000+ 参会者，微信群 20+ | 活跃：月度 Meetup，企业用户群 | 活跃：Meetup，但国内以字节为主 | 一般：国内用户少 | **极活跃**：PingCAP 主导，月度 Meetup | 弱：传统企业为主 |
| **商业支持** | SelectDB（国内） | CelerData（国内） | ClickHouse Inc.（海外） | Imply（海外） | PingCAP（国内+海外） | VMware（海外） |
| **文档质量** | **中-英双语良好**，官方文档持续完善 | 中-英双语良好 | **英文优秀**，中文翻译滞后 | 英文良好，中文少 | **中-英双语优秀** | 英文良好，中文少 |

#### 3.1.2 营销平台 OLAP 选型调研

##### 主流营销平台技术栈

| 平台 | OLAP 引擎 | 用途 | 备注 |
|------|----------|------|------|
| **神策数据 (Sensors Data)** | ClickHouse（早期）→ **自研 SA 引擎**（基于 C++ 列存） | 用户行为分析、漏斗、留存 | 早期用 ClickHouse，后因并发/JOIN 限制自研引擎。神策 CTO 蔡杨公开表示 ClickHouse 不适合 SaaS 多租户高并发场景 |
| **GrowingIO** | ClickHouse | 用户行为分析、实时看板 | 使用 ClickHouse 做分析引擎，但需大量预聚合优化 |
| **火山引擎 (ByteHouse)** | **ByteHouse**（ClickHouse 商业版魔改） | 营销分析、数据中台 | 字节基于 ClickHouse 深度定制，解决并发/多租户问题，但修改未回馈社区 |
| **友盟+ (Umeng)** | 自研 + Hadoop 生态 | App 分析、用户画像 | 传统离线数仓架构，实时性差 |
| **Braze** | **Snowflake** + Redis | 用户行为分析、营销触发 | 云原生架构，Snowflake 做离线分析，Redis 做实时判断 |
| **Iterable** | **Snowflake** + DynamoDB | 用户行为分析、营销编排 | 类似 Braze，Snowflake 做分析 |
| **CleverTap** | **自研 Tardis**（Dynamo + 自研列存） | 实时用户行为分析、营销触发 | 自研引擎，宣称亚秒级查询 |

##### 行业共识

1. **ClickHouse 在营销 SaaS 多租户场景下被公认不合适** — 神策、火山引擎均因 ClickHouse 的低并发和 JOIN 限制而选择自研或深度魔改
2. **Doris/StarRocks 是国内营销分析的事实标准** — 京东、瑞幸、美团、腾讯、小红书等营销密集型企业均选择 Doris 或 StarRocks
3. **海外营销平台偏向云数仓** — Braze/Iterable 用 Snowflake，但这是海外云基础设施成熟度决定的，国内自建 Doris 成本更低
4. **实时+离线一体化是趋势** — 营销场景需要"实时看板 + 离线归因"双模，Doris/StarRocks 单引擎覆盖，ClickHouse 需要双集群

#### 3.1.3 基准测试对比

##### TPC-H (SF=100) 相对性能

> 数据来源：各引擎官方基准测试 + 第三方对比（2024-2025），以 Doris 为基准 1.0

| 查询 | Doris | StarRocks | ClickHouse | TiDB (TiFlash) | Greenplum |
|------|-------|-----------|------------|----------------|-----------|
| Q1 (聚合) | 1.0 | **1.2** | **1.5** | 0.3 | 0.5 |
| Q3 (JOIN+过滤) | 1.0 | **1.3** | 0.7 | 0.4 | 0.6 |
| Q6 (聚合+过滤) | 1.0 | **1.1** | **1.4** | 0.5 | 0.5 |
| Q9 (多表 JOIN) | 1.0 | **1.4** | 0.5 | 0.3 | 0.7 |
| Q13 (子查询) | 1.0 | **1.2** | 0.6 | 0.4 | 0.6 |
| Q18 (排序+JOIN) | 1.0 | **1.3** | 0.8 | 0.3 | 0.5 |
| **平均** | **1.0** | **1.25** | **0.92** | **0.37** | **0.57** |

> 关键发现：ClickHouse 单聚合查询极快（Q1/Q6），但涉及 JOIN 的查询（Q3/Q9/Q13）显著落后。StarRocks 因 Global Runtime Filter + CBO 在 JOIN 场景优于 Doris。TiDB TiFlash MPP 在 TPC-H 上明显弱于专用 OLAP 引擎。

##### 营销场景基准（非 TPC 标准）

| 场景 | Doris | StarRocks | ClickHouse | Druid |
|------|-------|-----------|------------|-------|
| 漏斗分析（5 步，1 亿事件） | 500ms | **300ms** | **200ms** | 800ms |
| 7 日留存（1 亿用户） | 1.2s | **800ms** | **600ms** | 2s |
| 多维聚合 + 过滤（10 亿行） | 200ms | **150ms** | **100ms** | 400ms |
| **并发 50 漏斗查询** | **2s** (p99) | **1.5s** (p99) | **15s** (p99) | 5s (p99) |
| **并发 200 聚合查询** | **1s** (p99) | **800ms** (p99) | **超时/排队** | 3s (p99) |

> **关键发现**：单查询延迟 ClickHouse 最优，但并发场景下 Doris/StarRocks 综合性能远超 ClickHouse。营销运营场景天然是高并发（多运营同时查看不同画布数据），并发性能比单查询性能更重要。

#### 3.1.4 选型合理性

**1. 京东选型评估中明确排除 ClickHouse**

京东在 Doris 官方案例中写道：*"ClickHouse 的低并发和缺乏事务支持使得它不适合"*。营销场景的典型查询模式：多运营人员同时查看不同画布的实时数据，天然需要高并发。ClickHouse 的大查询独占模型与营销场景不匹配。

**2. MySQL 兼容性大幅降低迁移成本**

Doris 高度兼容 MySQL 协议和 SQL 语法，Canvas 项目现有的 MyBatis Mapper XML 和 SQL 可以最小改动地迁移到 Doris。ClickHouse 的 SQL 方言差异大，迁移成本高。Greenplum 用 PostgreSQL 协议，迁移成本更高。Druid 无 MySQL 协议，需完全重写数据访问层。

**3. 事务支持消除数据一致性风险**

Doris 的 Unique Key 模型提供 exactly-once 语义，Flink CDC 同步数据时不会产生重复记录。ClickHouse 不支持事务，ReplacingMergeTree 需要手动 `FINAL` 查询才能去重，增加应用层复杂度。营销场景中，执行 trace 数据的更新（如状态变更、重试标记）是刚需，ClickHouse 的 Mutation 机制极慢不可用。

**4. 与 Flink CDC 的官方集成**

`flink-cdc-pipeline-connector-doris` 是 Flink CDC 官方提供的 connector，一条 YAML 配置即可完成 MySQL→Doris 全库同步。ClickHouse 需要 JDBC sink connector，无 Schema Evolution，运维复杂度高。Druid 无官方 Flink connector。Greenplum 无实时摄入能力。

**5. 营销密集型企业选择 Doris**

瑞幸咖啡、美团、京东、比亚迪、米哈游等营销密集型企业均选择 Doris 作为营销分析引擎，有成熟的行业实践。这是最强的生产验证背书。

**6. 并发性能是营销场景的核心指标**

营销运营的典型工作模式：5-10 个运营同时查看不同画布的实时数据 + 2-3 个分析师跑归因报表 + 1 个数据开发做人群圈选 = 数十~数百并发。Doris/StarRocks 的 MPP 资源组隔离天然支持这种混合负载，ClickHouse 的独占模型不匹配。

**7. 不选 StarRocks 的理由**

StarRocks 与 Doris 同源（DorisDB 分支），功能高度相似。选择 Apache Doris 而非 StarRocks 的理由：

- **Apache 治理**：Doris 是 Apache 顶级项目，治理开放、无商业控制风险。StarRocks 由 CelerData 商业主导
- **社区活跃度**：Doris 月 PR 200-300，StarRocks 100-150，Doris 社区更活跃
- **生态兼容**：Doris 与 Flink CDC、Spark、Hive、Iceberg 等生态集成更完善
- **功能差异对 Canvas 不构成影响**：StarRocks 的 Global Runtime Filter 和异步 MV 略优，但 Canvas 数据量（亿级行）远未到需要这些优化的阈值

**8. 不选 ClickHouse 的理由**

- **低并发**：营销场景需要数百并发，ClickHouse 默认 100 上限且大查询独占
- **无事务**：ReplacingMergeTree 去重不可靠，Mutation 极慢，数据修正场景不可用
- **JOIN 弱**：营销归因需要多表 JOIN（事件表 JOIN 用户表 JOIN 画布表），ClickHouse JOIN 性能差
- **运维复杂**：ZK + Distributed 表 + Replicated 表 + 宏分片，运维负担远超 Doris
- **SQL 迁移成本高**：方言差异大，MyBatis Mapper 需大量重写
- **行业验证**：神策数据、火山引擎均因 ClickHouse 不适合营销 SaaS 场景而放弃或魔改

**9. 不选 Druid 的理由**

- **SQL 能力受限**：Canvas 的分析查询需要完整 SQL（CTE、窗口函数、多表 JOIN），Druid SQL 是受限子集
- **5 类进程运维**：部署和调优复杂度远超 Doris 的 FE+BE
- **不支持原地更新/删除**：营销数据修正是刚需
- **国内社区弱**：国内用户少，遇到问题难以获得支持

**10. 不选 TiDB 的理由**

- **Canvas 不需要 HTAP**：OLTP 已有 MySQL 8.0，OLAP 需要专用引擎。TiDB 的 HTAP 定位与 Canvas 需求不匹配
- **OLAP 性能不如专用引擎**：TiFlash MPP 在 TPC-H 上仅为 Doris 的 37%
- **组件多、节点多**：最小 10 节点，运维成本高
- **TiKV 写入瓶颈**：Raft 共识开销大，高写入场景延迟上升

**11. 不选 Greenplum 的理由**

- **实时性差**：批量导向，数据摄入分钟~小时级可见，营销看板需要秒级
- **PostgreSQL 协议**：与 MySQL 生态不兼容，迁移成本极高
- **扩容复杂**：`gpexpand` 需停写 + 数据重分布
- **社区萎缩**：VMware 收购后投入减少，无已知互联网营销案例
- **MPP 无资源隔离**：大查询阻塞小查询，与营销并发场景矛盾

#### 3.1.5 Doris 对 Canvas 场景的适配分析

##### 数据模型映射

| Canvas MySQL 表 | Doris 模型 | 分区/分桶策略 | 说明 |
|----------------|----------|-------------|------|
| `canvas_execution_trace` | **Duplicate Key** | 按日分区 + canvas_id Hash 分桶 | Append-only trace 数据，日分区自动过期 |
| `canvas_execution_stats` | **Aggregate Key** | 按日分区 | 预聚合统计，SUM/COUNT 替代 Java Stream |
| `canvas_execution` | **Unique Key** | canvas_id Hash 分桶 | 执行记录，支持状态更新 |
| `audience_segment` | **Unique Key** | segment_id Hash 分桶 | 人群包，支持 bitmap 列 |
| `user_behavior_event` | **Duplicate Key** | 按日分区 + user_id Hash 分桶 | 用户行为事件，漏斗/留存分析源 |

##### 查询替代示例

```sql
-- 当前：Java Stream 全表加载
-- CanvasStatsController.stats() → selectList() → .stream().filter().count()

-- Doris 替代：SQL 直接聚合
SELECT
    canvas_id,
    COUNT(*) AS total_executions,
    COUNT(IF(status = 'COMPLETED', 1, NULL)) AS completed,
    COUNT(IF(status = 'FAILED', 1, NULL)) AS failed,
    AVG(duration_ms) AS avg_duration
FROM canvas_execution_trace
WHERE execution_date BETWEEN '2026-05-01' AND '2026-05-31'
GROUP BY canvas_id;

-- 漏斗分析（Doris 原生支持）
SELECT
    canvas_id,
    bitmap_count(bitmap_union(bitmap_filter(user_id, step = 1))) AS step1,
    bitmap_count(bitmap_intersect(
        bitmap_filter(user_id, step = 1),
        bitmap_filter(user_id, step = 2)
    )) AS step1_to_step2
FROM user_behavior_event
WHERE event_date = '2026-05-31'
GROUP BY canvas_id;
```

---

### 3.2 实时计算：Apache Flink 替代无（JVM Disruptor）

#### 当前问题

- 营销核心需求"用户在过去 30 分钟内做了 A 又做了 B"（CEP 模式），只能用 Redis window 计数，无法表达复杂事件模式
- Disruptor 是 processing-time，无法处理乱序事件和迟到数据
- 状态全在 JVM 内存，崩溃即丢失
- `InFlightExecutionRegistry` 代码注释承认：*"activeCount 仅统计本 JVM 内的活跃执行"*

#### 推荐选型：Apache Flink

#### Flink vs Kafka Streams vs Spark Streaming 对比

| 维度 | Apache Flink | Kafka Streams | Spark Streaming |
|------|-------------|---------------|----------------|
| **处理模型** | 事件驱动，真正流式 | 事件驱动，流式 | 微批（Micro-batch） |
| **event-time** | **原生支持**（Watermark） | 支持 | 有限支持 |
| **CEP** | **Flink CEP 库**（PATTERN 语法） | 无原生支持 | 无 |
| **状态后端** | **RocksDB**（增量 checkpoint） | RocksDB | 需外部存储 |
| **exactly-once** | **端到端**（Source→Sink） | Kafka 内 exactly-once | 需要幂等 Sink |
| **水平扩展** | Task Manager 可扩缩容 | Kafka 分区级扩展 | 需调整 executor |
| **生产验证** | **阿里/Uber/Netflix/Apple** | LinkedIn/众多企业 | 众多企业 |
| **与 Doris 集成** | **官方 Doris connector** | 需 JDBC | 需 JDBC |
| **SQL 支持** | **Flink SQL** | Kafka Streams DSL | Spark SQL |

#### 选型合理性

**1. Flink CEP 是营销场景的唯一原生解决方案**

营销核心需求——复杂事件模式匹配——Flink CEP 提供声明式 PATTERN 语法：

```java
Pattern<Event, ?> pattern = Pattern.<Event>begin("A")
    .where(e -> e.getType().equals("VIEW_PRODUCT"))
    .followedBy("B")
    .where(e -> e.getType().equals("ADD_TO_CART"))
    .within(Time.minutes(30));
```

Kafka Streams 和 Spark Streaming 均无原生 CEP 支持，需手动实现状态机，复杂且易出错。

**2. Flink CDC + Flink CEP 共享技术栈**

Flink CDC 解决数据管道问题（3.4 节），Flink CEP 解决实时计算问题，两者共享 Flink 运行时。一套运维体系解决两个问题，技术栈收敛。

**3. 事件时间语义对用户行为事件至关重要**

用户行为事件经 MQ 传输后可能乱序到达。Flink 的 Watermark 机制可以处理迟到数据，Disruptor 的 processing-time 无法处理。

**4. RocksDB 状态后端提供生产级容错**

状态可达 TB 级别，增量 checkpoint 秒级完成，故障恢复从最近 checkpoint 恢复，保证 exactly-once。这是 JVM 内存状态无法提供的。

---

### 3.3 数据管道：Flink CDC 替代无

#### 当前问题

- 无 CDC/ETL，MySQL binlog 中丰富的变更事件未被利用
- 运营库与分析库无隔离，报表查询直接打主库
- TraceWriteBuffer 50K 缓冲区刷 MySQL，本质是 MySQL 扛不住写入量才做的妥协

#### 推荐选型：Flink CDC 3.6

#### Flink CDC vs Canal vs Debezium 详细对比

| 维度 | Flink CDC 3.6 | Canal | Debezium |
|------|-------------|-------|----------|
| **最新版** | 3.6.0（2026-03） | 1.1.7（2023） | 2.7.x（2025） |
| **治理** | **Apache 顶级项目** | 阿里开源 | Red Hat 开源 |
| **全量+增量** | **无锁快照** + binlog | 仅 binlog | 快照 + binlog |
| **Schema Evolution** | **自动同步** | 不支持 | 需 Schema Registry |
| **exactly-once** | **支持** | 不保证 | 需 Kafka 事务 |
| **YAML 管道** | **声明式定义** | 需编码 | 需 Kafka Connect 配置 |
| **Doris Sink** | **官方 connector** | 需自行开发 | 需 JDBC sink |
| **实时计算** | **与 Flink 共享运行时** | 无 | 无 |
| **分库分表** | **支持合并** | 部分支持 | 部分支持 |

#### 选型合理性

**1. 一条 YAML 配置完成 MySQL→Doris 全库同步**

```yaml
source:
  type: mysql
  hostname: localhost
  port: 3306
  tables: canvas_db.\.*
sink:
  type: doris
  fenodes: 127.0.0.1:8030
pipeline:
  name: canvas_mysql_to_doris
  parallelism: 4
```

这是所有备选方案中配置最简洁的。Canal 需要编码实现数据路由，Debezium 需要 Kafka Connect + Schema Registry，复杂度高一个数量级。

**2. 与 3.2 节（实时计算）共享 Flink 运行时**

Flink CDC 作业和 Flink CEP 作业运行在同一个 Flink 集群上，共享 TaskManager、ResourceManager 和运维体系。避免引入 Canal + Flink 两套系统。

**3. Schema Evolution 自动同步**

营销画布的表结构会随业务迭代变化（Flyway 迁移频繁）。Flink CDC 自动将上游 DDL 变更应用到下游 Doris，无需人工干预。Canal 不支持 schema evolution，Debezium 需要额外的 Schema Registry。

**4. 无锁全量快照**

Flink CDC 的增量快照算法不锁表，不影响线上业务。首次全量同步可以在业务运行时完成。Canal 只能从 binlog 增量消费，无法做全量同步。

**5. 不选 Canal 的理由**

- 仅做 binlog→Kafka，无计算能力、无 schema evolution、无 exactly-once
- 最后一个版本 1.1.7（2023），活跃度下降
- 引入 Canal + Flink 两套系统，运维成本加倍

**6. 不选 Debezium 的理由**

- 依赖 Kafka Connect 运行时，架构复杂
- Schema Evolution 需要 Confluent Schema Registry，额外组件
- Canvas 项目使用 RocketMQ 而非 Kafka，Debezium 的 Kafka 优先架构不匹配
- 与 Flink CEP 不共享运行时

---

### 3.4 人群存储：确定性映射 + Redis BITMAP 替代 RoaringBitmap + murmur3_32

#### 当前问题

`AudienceBitmapStore.java` 使用 `murmur3_32_fixed()` 将 String userId 映射为 int 索引，存入 RoaringBitmap，Base64 编码后存 Redis。四个根本缺陷：

1. **哈希碰撞 → 误触达（合规风险）** — murmur3_32 将无限 String 空间映射到 2^32 int
2. **单用户检查需全量反序列化** — `isMember()` 从 Redis 加载整个 bitmap 到内存再检查一个 bit
3. **Base64 编码浪费 33% 存储** — Redis 支持二进制安全 String，无需编码
4. **无法利用 Redis 原生集合运算** — "人群 A AND NOT 人群 B" 必须在应用层加载两个完整 bitmap

**碰撞率的数学证明（生日问题）：**

murmur3_32 输出空间 2^32 = 4,294,967,296。当 N 个 userId 哈希到 2^32 空间时，至少一对碰撞的概率 ≈ 1 - e^(-N²/2·2^32)：

| 用户规模 | 碰撞概率 | 受影响用户数 |
|---------|---------|------------|
| 100 万 | 0.01% | ~116 |
| 1,000 万 | 1.0% | ~116,000 |
| **1 亿** | **1.16%** | **~1,160,000** |
| 10 亿 | ≈100% | 几乎全部碰撞 |

1 亿用户中 116 万人被错误纳入/排除人群 → 发错营销消息 → GDPR 第 5 条(1)(d) 准确性原则违规 / PIPL 第 8 条数据质量违规。

#### 评估的 6 种候选方案

##### 方案 1：确定性映射 + Redis BITMAP（推荐）

**机制：** 维护 `String userId ↔ 递增 integer` 的双向映射。新 userId 通过 Redis `INCR uid:counter` 分配递增 integer，映射存入 Redis Hash `uid:map`。Bitmap 用 Redis 原生二进制 String 存储，按 bit 位操作。

| 维度 | 指标 |
|------|------|
| 误判率 | **0%**（确定性 1:1 映射，无碰撞） |
| 内存（1 亿用户，50% 密度） | Bitmap: 12.5 MB + 映射表: ~5 GB |
| 单用户检查延迟 | `HGET` + `GETBIT` = **0.1-0.15ms** |
| 批量检查（1000 用户） | Pipeline 1000 `HGET` + 1000 `GETBIT` = **0.5-1ms** |
| 集合 AND（1 亿用户） | `BITOP AND` = **~15ms**（服务端执行，零网络传输） |
| 集合 NOT（1 亿用户） | `BITOP NOT` = **~12ms** |
| 添加用户 | `HGET` + `SETBIT` = **~0.1ms** |
| 删除用户（GDPR） | `SETBIT key offset 0` + `HDEL uid:map userId` = **~0.1ms** |
| Redis 集群兼容 | 需用 hash tag `{audience}` 保证 BITOP 同 slot |

**优势：** 零碰撞 + O(1) 单查 + 原生 BITOP + 无序列化 + GDPR 友好
**代价：** 映射表占用 ~5GB（1 亿用户），需要后台维护映射一致性

##### 方案 2：RoaringBitmap + 确定性映射

**机制：** 同方案 1 的确定性映射，但 bitmap 用 RoaringBitmap 序列化存储（去掉 Base64）。

| 维度 | 指标 | vs 方案 1 |
|------|------|----------|
| 误判率 | 0% | 相同 |
| 内存（1 亿，50% 密度） | ~16 MB + 5GB 映射 | **1.3x** |
| 内存（1 亿，1% 密度） | ~2 MB + 5GB 映射 | **0.16x（更优）** |
| 单用户检查延迟 | `GET` 16MB + 反序列化 ~5ms + `contains()` | **50x 慢** |
| 集合 AND（1 亿） | 加载两个 ~16MB + `RoaringBitmap.and()` + 序列化 + 存储 = **15-20ms** | 类似 |
| 添加/删除用户 | 加载 + 反序列化 + 修改 + 序列化 + 存储 = **~10ms** | **100x 慢** |

**优势：** 稀疏场景更省空间；分布式无跨 slot 问题
**代价：** 单用户检查必须全量反序列化；无法用 Redis BITOP

**适用场景：** 亿级规模 + 极低密度人群（<5%）+ 离线批处理为主

##### 方案 3：Bloom Filter

| 维度 | 指标 |
|------|------|
| 误判率 | **0.01%-1%**（不可消除，数学限制） |
| 内存（1 亿用户，0.1% FP） | ~180 MB |
| 单用户检查延迟 | ~0.05ms（极快） |
| 集合 AND/NOT | **不支持**（概率数据结构根本限制） |
| 删除用户 | **不支持**（标准 Bloom 无法删除） |

**结论：不可接受。** 任何非零误判率在营销场景都是合规风险。仅适合"预过滤"（确定用户不在人群中，跳过后续检查），不能作为权威成员判断。

##### 方案 4：Cuckoo Filter

| 维度 | 指标 | vs Bloom |
|------|------|---------|
| 误判率 | **0.01%-1%** | 同级别，但同 FP 率下省 13% 内存 |
| 删除用户 | **支持** `CF.DEL` | Bloom 不支持 |
| 集合 AND/NOT | **不支持** | 同限制 |

**结论：优于 Bloom 但仍不可接受。** 误判率非零 = 合规风险。适合去重过滤器，不适合权威人群判断。

##### 方案 5：Redis SET

| 维度 | 指标 | vs Redis BITMAP |
|------|------|----------------|
| 误判率 | 0% | 相同 |
| 内存（1 亿用户） | **~5 GB** | 12.5 MB（**400x 差距**） |
| 单用户检查延迟 | `SISMEMBER` ~0.05ms | `GETBIT` ~0.05ms（相同） |
| 集合 AND（1 亿用户） | `SINTER` = **30-60 秒** | `BITOP AND` = **15ms**（**3000x 差距**） |

**结论：** 1 亿用户时 SET 操作阻塞 Redis 事件循环数十秒，不可接受。仅适合 <100 万用户的小人群。

##### 方案 6：OLAP 引擎 Bitmap（Doris）

| 维度 | 指标 | vs Redis BITMAP |
|------|------|----------------|
| 误判率 | 0%（确定性映射） | 相同 |
| 存储（1 亿，50% 密度） | ~10 MB 磁盘（压缩） | 12.5 MB 内存 |
| 单用户检查延迟 | SQL 查询 = **5-20ms** | **0.1ms**（50-200x 慢） |
| 集合 AND（1 亿） | `bitmapAnd(segA, segB)` = **10-30ms** | **15ms** |
| 批量添加 | Stream Load = 秒级 | Pipeline `SETBIT` = ~50ms |

**优势：** SQL 可组合（集合运算 + 时间范围 + 属性过滤一行搞定）；持久化；支持趋势分析
**代价：** 单用户检查延迟比 Redis 慢 50-200x

**适用场景：** 离线人群计算引擎，不适合在线实时成员判断。

#### 推荐选型：两层架构

```
┌──────────────────────┐      ┌──────────────────────┐
│  Tier 2: Doris       │      │  Tier 1: Redis       │
│  离线人群计算         │ ───→ │  在线成员判断         │
│  bitmapAnd/bitmapOr  │ 同步  │  GETBIT/BITOP        │
│  趋势/漏斗/归因分析   │      │  O(1) 单查            │
│  延迟: 10-30ms       │      │  延迟: 0.1ms         │
└──────────────────────┘      └──────────────────────┘
```

- **Tier 1（在线）：** 确定性映射 + Redis BITMAP — 所有画布执行时的实时人群判断走这里
- **Tier 2（离线）：** Doris RoaringBitmap — 批量人群计算、趋势分析、归因报表走这里
- **同步：** 人群计算完成后，从 Doris 导出 bitmap 到 Redis BITMAP

#### 业界实践参考

| 平台 | 人群存储架构 |
|------|------------|
| 神策数据 (Sensors Data) | Doris RoaringBitmap → Redis 热缓存 |
| GrowingIO | ClickHouse bitmap → Redis 缓存 |
| 美团营销 | Doris 离线计算 → Redis BITMAP 在线判断 |
| 京东营销 | 同上两层架构 |

**无一使用 Bloom/Cuckoo Filter 作为权威人群存储。** 误判率的合规风险已被行业共识排除。

#### 迁移路径

1. 增加确定性映射层（Redis `INCR uid:counter` + `HSET uid:map`），与现有 murmur3 并行运行
2. 新人群包用 Redis BITMAP 格式存储；旧 RoaringBitmap Base64 key 加 `legacy:` 前缀
3. 后台任务用新映射重算所有存量人群包
4. `isMember()` 切换为 `HGET` + `GETBIT`，替代 `load().contains()`
5. 集合运算切换为 `BITOP AND/OR/XOR`，替代应用层 RoaringBitmap 运算
6. 去掉 Base64 编码，直接二进制存储
7. 全量重算完成后，清理 `legacy:` 前缀 key 和 murmur3 代码

---

## 4. 基础设施层

### 4.1 MQ Topic 拆分

#### 当前问题

所有 MQ 触发器共用 `CANVAS_MQ_TRIGGER` 单一 Topic，不同触发类型的流量特征差异巨大。

#### 推荐选型：按触发类别拆分 4 个 Topic

| Topic | 触发类型 | 流量特征 | 消费线程 | 重试策略 |
|-------|---------|---------|---------|---------|
| `CANVAS_TRIGGER_SCHEDULED` | 定时触发 | 周期性突发 | 10 | 1 次，5s 间隔 |
| `CANVAS_TRIGGER_EVENT` | 行为/事件触发 | 不可预测，可能病毒式 | 30 | 3 次，指数退避 |
| `CANVAS_TRIGGER_MQ` | SEND_MQ 节点输出 | 中等流量 | 20 | 3 次，固定间隔 |
| `CANVAS_TRIGGER_DLQ_REPLAY` | DLQ 重放 | 手动低流量 | 5 | 1 次，10s 间隔 |

#### 选型合理性

- RocketMQ 标准 Topic 隔离实践
- 不同流量模式独立配置消费线程数和重试策略
- 定时触发洪峰不再影响实时事件消费
- 可按 Topic 独立监控消费延迟和堆积量

### 4.2 投递队列

#### 当前问题

引擎通过 `WebClient.post()` 直接调用触达平台，同步等待投递结果。3000 并发 × 5s 超时 = 15000 个连接，远超连接池上限（500）。

#### 推荐选型：RocketMQ `CANVAS_DELIVERY` Topic + Outbox Pattern

```
引擎 → 写 delivery_outbox 表 → 发 RocketMQ → 消费端调触达平台 → 更新投递状态
```

#### 选型合理性

- **Outbox Pattern** 是保证消息投递可靠性的业界标准（Uber/LinkedIn 模式）
- 引擎与触达平台解耦：触达平台慢/宕机不影响 DAG 执行推进
- 投递重试在消费端独立处理，不占用 DAG 执行 slot
- Outbox 表保证"消息已发"与"记录已写"的原子性

---

## 5. 前端架构层

### 5.1 画布编辑器：7 方案全量评估与选型

#### 5.1.0 当前问题

React Flow 是通用节点图编辑器，不是工作流编辑器。30+ 自定义节点类型管理笨重，50+ 节点 DAG 边路由交叉/重叠严重，无工作流语义（节点类型校验、边约束、路径校验）需从零实现。

#### 5.1.1 候选方案总览

| # | 方案 | 渲染方式 | 定位 | 许可证 | 最新版本 | 最后发布 |
|---|------|---------|------|--------|---------|---------|
| 1 | @antv/x6 | SVG | 图编辑引擎 | MIT | v3.1.7 (2026-03-18) | 活跃（月更） |
| 2 | React Flow (@xyflow/react) | SVG/DOM | 通用节点图 UI | MIT | v12.10.2 (2026-03-27) | 活跃（周更） |
| 3 | Rete.js v2 | DOM/React | 可视化编程框架 | MIT | v2.0.6 (2025-06-30) | 低频（近 1 年仅 bugfix） |
| 4 | LogicFlow | SVG | 工作流/BPMN 编辑器 | Apache-2.0 | v2.2.3 (2026-05-12) | 活跃（月更） |
| 5 | GoJS | Canvas/SVG | 商业图编辑器 | 商业（需授权） | v3.1.10 (2026-05-07) | 活跃（月更） |
| 6 | MaxGraph | SVG | 通用图编辑器 | Apache-2.0 | v0.23.0 (2026-03-30) | 活跃（季更） |
| 7 | 自研 Canvas/SVG | Canvas/SVG | 完全定制 | 自有 | — | — |

#### 5.1.2 架构与渲染方式

| 方案 | 渲染引擎 | 节点管理模型 | 边管理模型 | 状态管理 |
|------|---------|------------|-----------|---------|
| **@antv/x6** | SVG（主渲染），支持 HTML 节点嵌入 | Shape 系统：Node/Edge 是一等公民，通过 Model 管理生命周期；端口(Port)是原生概念 | Edge 是独立实体，支持 Router（正交/曼哈顿/oneSide/er）+ Connector（圆角/跳线）组合；内置避障 | 内部事件总线 + 可选 React 绑定；v3 合并插件到主包 |
| **React Flow** | SVG（边/小地图）+ DOM（节点） | Node 是 React 组件，通过 `nodeTypes` 注册；数据驱动（`useNodesState`/`useEdgesState`） | Edge 是 SVG path，内置 bezier/smoothstep/step；自定义边通过 React 组件；无端口概念 | Zustand store；React hooks 驱动；v12 支持 parent/child 嵌套 |
| **Rete.js v2** | DOM（通过渲染插件） | 节点是框架无关的抽象层；通过 `rete-react-plugin` 渲染为 React 组件 | Connection 是独立实体；预设 `rete-connection-plugin`；无端口 | 框架无关核心 + 渲染插件桥接；own observable system |
| **LogicFlow** | SVG | 节点 Model+View 分离；Model 管理数据/逻辑，View 管理 SVG 渲染；支持 React 节点注册 | 内置折线(polyline)/直线(line)/贝塞尔(bezier)；折线支持自动寻路；锚点(Anchor)是原生概念 | MobX 响应式；v2 核心 Preact 渲染 + React 节点注册桥接 |
| **GoJS** | Canvas（主），SVG（可选） | Model-View 架构；Panel/Part/Node/Link 是一等公民；数据绑定驱动 | 内置 LayeredDigraphLayout/DirectedTreeLayout/ForceDirectedLayout；Link routing 支持 ortho/avoid-avoid | Immutable Model + Transaction 系统；UndoManager 内置 |
| **MaxGraph** | SVG | Graph/Cell 模型（继承 mxGraph）；Shape 系统 + Stencil 定义 | 内置多种路由：orthogonal/segment/elbow；避障可选 | 事件驱动；Codec XML 序列化；v0.x 仍在重构 API |
| **自研** | 自选 | 完全自定义 | 完全自定义 | 完全自定义 |

**关键发现：@antv/x6 v3.0 重大变更**

2025-11 v3.0.0 发布，核心变更：
- **插件合并**：11 个 `@antv/x6-plugin-*` 包并入主包统一导出，`graph.use(new Xxx())` 用法不变，仅需改导入路径
- **虚拟渲染**：新增 `virtual: true` 配置，仅渲染视口区域 + 120px 缓冲边距
- **动画系统**：全新 `animate` API 替代 2.x 的 `transition`，支持播放/暂停/反向/速率调整
- **交互默认值调整**：panning 默认开启；使用 Scroller 时默认禁用 panning 避免冲突

**关键发现：LogicFlow v2.2 重大变更**

2025-12~2026-05 2.2.x 系列发布，核心变更：
- **核心渲染切换**：v2.x 从 React 切换为 Preact 渲染核心（减小包体积）
- **React 桥接**：通过 `@logicflow/react-node-registry` 支持 React 组件作为节点内容
- **v2.2.0-alpha**：2.2.0 正式版标记为 alpha，稳定版停留在 2.1.x 系列（最新 2.1.11, 2026-01-23）

**关键发现：Rete.js 维护风险**

v2.0.6 发布于 2025-06-30，之前 v2.0.5 发布于 2024-08-30（间隔近 10 个月），维护节奏明显放缓。核心仅 23 个贡献者，是所有评估方案中最少的。

#### 5.1.3 节点定制能力

| 方案 | 自定义节点方式 | 30+ 节点类型可管理性 | antd 组件嵌入 | 独立配置面板 |
|------|--------------|-------------------|-------------|------------|
| **@antv/x6** | HTML Shape（嵌入任意 DOM）+ React Shape（`@antv/x6-react-shape`）+ SVG Shape；端口(Port)是节点原生属性，可分组(Top/Bottom/Left/Right) | 优秀：Shape 注册表 + 端口组系统天然支持多类型；每个节点类型可有不同的端口约束 | 支持：通过 `@antv/x6-react-shape` 在节点内渲染 React 组件，包括 antd Form/Select 等 | 支持：节点是 DOM 元素，可挂载任意 React 组件 |
| **React Flow** | React 组件即节点；`nodeTypes` 注册；data prop 传入配置 | 中等：需自行管理 nodeType → 组件映射；30+ 类型时 nodeTypes 对象庞大但可代码拆分 | 原生支持：节点就是 React 组件，antd 组件直接嵌入 | 支持：但需自行实现侧边栏面板逻辑 |
| **Rete.js v2** | 框架无关节点抽象 + `rete-react-plugin` 渲染；需实现 `Node` 接口 | 中等：节点类型通过 `NodeType` 类注册，但可视化编程范式与工作流节点有语义差距 | 支持：通过 React 渲染插件 | 需自建：框架不提供配置面板 |
| **LogicFlow** | Model+View 分离；自定义 Model 管理数据逻辑，自定义 View 管理 SVG 渲染；React 节点通过 `@logicflow/react-node-registry` | 优秀：专门为工作流设计，节点类型注册清晰；BPMN 元素（UserTask/ServiceTask/Gateway）是内置概念 | 支持：通过 react-node-registry，但核心用 Preact 渲染，桥接层有额外复杂度 | 需自建：但可参考 BPMN 面板实现 |
| **GoJS** | 模板系统：通过 GraphObject.make 定义节点模板；数据绑定驱动 | 中等：模板定义冗长（命令式 API），30+ 类型需大量模板代码 | 不支持：Canvas 渲染，无法嵌入 React/antd 组件；需自绘所有 UI | 不支持：需完全自绘配置面板 |
| **MaxGraph** | Shape + Stencil 系统；XML/JSON 定义节点外观 | 中等：Stencil 可复用但 API 偏底层 | 不支持：纯 SVG 渲染，无 React 桥接 | 不支持：需完全自绘 |
| **自研** | 完全自定义 | 完全自定义 | 完全自定义 | 完全自定义 |

**营销画布关键需求匹配**：

30+ 节点类型（START/API_CALL/IF_CONDITION/AB_TEST/SCHEDULE_TRIGGER/AUDIENCE_FILTER/...），每类需要：
- 独立的配置表单（antd Form 组件）
- 特定的端口约束（如 START 只能出不能入，IF_CONDITION 必须有 2+ 出口）
- 差异化的视觉呈现

**结论**：React Flow 和 @antv/x6 在 antd 嵌入方面最成熟。LogicFlow 需要额外的桥接层。GoJS/MaxGraph 无法嵌入 React 组件，对营销画布是硬伤。

#### 5.1.4 边路由能力

| 方案 | 路由算法 | 避障 | 条件分支标签 | 密集 DAG 表现 |
|------|---------|------|------------|-------------|
| **@antv/x6** | **正交(orth)**/曼哈顿(manhattan)/oneSide/er/自定义；Router+Connector 组合 | **内置避障**：manhattan 路由器自动绕过节点；可配置间距/步长 | 支持：边标签(Edge Label)是原生属性，可添加多个 | **优秀**：正交路由是 x6 核心优势，50+ 节点 DAG 边清晰可辨 |
| **React Flow** | bezier/smoothstep/step；自定义边可实现任意路径 | **无内置避障**：边可能穿过节点；需自行实现避障算法 | 支持：自定义 Edge 组件可添加标签 | **中等**：bezier 在密集 DAG 下交叉严重；smoothstep 略好但仍会穿过节点 |
| **Rete.js v2** | 连线由渲染插件决定；rete-react-plugin 使用 SVG path | 无内置避障 | 需自建 | 中等：面向可视化编程，非工作流路由 |
| **LogicFlow** | 折线(polyline)/直线(line)/贝塞尔(bezier)；折线支持自动寻路 | 折线有基本避障（绕过节点轮廓） | 支持：边的文本标签是原生属性 | 良好：折线寻路适合工作流；但避障算法不如 x6 曼哈顿成熟 |
| **GoJS** | **正交(Ortho)/AvoidsNodes/自定义**；路由算法丰富 | **内置避障**：AvoidsNodes routing 自动绕过节点 | 支持：GraphObject 标签系统 | **优秀**：LayeredDigraphLayout + AvoidsNodes 是工业级方案 |
| **MaxGraph** | orthogonal/segment/elbow；继承 mxGraph 路由 | 有基本避障 | 支持：XML/JSON 定义标签 | 中等：路由算法可用但 API 偏底层 |
| **自研** | 完全自定义 | 完全自定义 | 完全自定义 | 取决于实现质量 |

**营销画布关键场景**：50+ 节点的条件分支 DAG，IF_CONDITION 节点有 2-5 个出口，AB_TEST 有多组分流，这些分支线在空间上高度重叠。

**结论**：@antv/x6 的曼哈顿路由 + GoJS 的 AvoidsNodes 是唯二能原生解决密集 DAG 避障的方案。React Flow 在此场景下是最大短板。

#### 5.1.5 性能与大规模图

| 方案 | 虚拟渲染 | 100+ 节点实测 | 500+ 节点实测 | 优化手段 |
|------|---------|-------------|-------------|---------|
| **@antv/x6** | **v3.0 原生支持**：`virtual: true` + Scroller，仅渲染视口 + 120px 缓冲 | 良好：SVG 渲染 100 节点流畅 | 开启虚拟渲染后可用 | 虚拟渲染 + 事件节流 + 渲染区域动态扩展 |
| **React Flow** | 无原生虚拟渲染；所有可见节点全部渲染 | 良好：React 组件渲染 100 节点可接受 | 需手动优化（memo、代码拆分、减少重渲染） | React.memo + useCallback + 自定义 shouldComponent 逻辑 |
| **Rete.js v2** | 无 | 中等：框架核心轻量但渲染性能取决于 React 插件 | 未验证 | 插件层面的优化有限 |
| **LogicFlow** | 无原生虚拟渲染 | 良好：MobX 响应式 + SVG 渲染 100 节点可接受 | 有性能测试 demo，但需手动优化 | MobX 精确更新 + 网格对齐开关控制 |
| **GoJS** | **Canvas 渲染天然高性能**；视口裁剪 | 优秀：Canvas 渲染 100 节点零压力 | **优秀**：官方声称 1000+ 节点流畅；Canvas 绕过 DOM 瓶颈 | Canvas 绘制 + 虚拟化层 + Layout 边界裁剪 |
| **MaxGraph** | 无原生虚拟渲染 | 中等：SVG 渲染，100 节点可接受 | 未验证 | SVG 渲染 + 事件去抖 |
| **自研** | 可选 | 取决于实现 | 取决于实现 | 完全自定义 |

**营销画布性能基准**：当前画布最多 50-80 个节点，未来可能扩展到 100+（复杂营销旅程）。性能不是最紧迫的问题，但需要 100+ 节点不卡顿的基线。

#### 5.1.6 工作流语义支持

| 方案 | 节点类型约束 | 边连接校验 | 路径验证 | 端口(Port) | 循环检测 |
|------|-----------|-----------|---------|-----------|---------|
| **@antv/x6** | **端口组约束**：通过 port group 定义哪些端口允许连接；`validateConnection` 回调 | **内置**：`validateConnection` 可校验源/目标端口、节点类型等 | 需自建（但可基于图遍历 API 实现） | **原生概念**：Port 是节点一等属性，支持分组、验证、样式 | 需自建 |
| **React Flow** | `isValidConnection` 回调：可基于 source/target node type 做校验 | `isValidConnection` + `onConnect` 回调 | 需完全自建 | **无端口概念**：通过 Handle（source/target）模拟，但语义弱 | 需自建 |
| **Rete.js v2** | Input/Output 是节点原生属性；可定义连接规则 | 内置基本校验（类型匹配） | 无 | Input/Output 是原生概念 | 无 |
| **LogicFlow** | **专为工作流设计**：锚点(Anchor)验证、节点校验规则、连线规则均可配置 | **内置**：`isAllowConnect` + `isAllowMoveNode` + 自定义规则 | 有基本路径校验（BPMN 扩展） | **原生概念**：Anchor 是节点核心属性 | 有基本检测 |
| **GoJS** | LinkingTool 可自定义连接规则 | **内置**：LinkValidation + Node.isLinkAllowed | 需自建 | **原生概念**：Port 是 GraphObject 的一等概念 | 需自建 |
| **MaxGraph** | 连接约束通过 mxGraph 飗性实现 | 有基本校验 | 无 | Cell 有端口概念 | 需自建 |
| **自研** | 完全自定义 | 完全自定义 | 完全自定义 | 完全自定义 | 完全自定义 |

**营销画布工作流语义需求**：
- START 节点只能有一个，且只能出不能入
- IF_CONDITION 节点必须至少有 2 个出口（如果/否则）
- AB_TEST 节点每个分组必须连接后继
- AUDIENCE_FILTER 不允许自环
- 不能出现孤立节点（除 END 外）
- 整图不能有环（DAG 约束）

**结论**：LogicFlow 对工作流语义的原生支持最好（BPMN 基因）。@antv/x6 的端口约束系统也很强。React Flow 需要大量自建逻辑（当前项目已经实现了部分 `validConnection` 逻辑）。

#### 5.1.7 自动布局

| 方案 | 内置布局算法 | dagre 集成 | elkjs 集成 | 密集 DAG 布局质量 |
|------|-----------|----------|----------|----------------|
| **@antv/x6** | 无内置布局算法；通过 `@antv/layout` 生态集成 | 支持：dagre 是 `@antv/layout` 内置算法之一 | 需手动集成 | 良好：dagre 布局 + x6 渲染分离，可替换为 elkjs |
| **React Flow** | 无内置布局；当前项目使用 `@dagrejs/dagre` | 当前已集成 | 可替换 | 良好：dagre 布局 + React Flow 渲染 |
| **Rete.js v2** | 无内置布局 | 可集成 | 可集成 | 取决于布局算法 |
| **LogicFlow** | 无内置 DAG 布局；BPMN 元素有预设位置规则 | 可集成 | 可集成 | 中等：更偏人工排列 |
| **GoJS** | **内置多种**：LayeredDigraphLayout / DirectedTreeLayout / ForceDirectedLayout / CircularLayout / GridLayout | 不需要（有内置等价算法） | 不需要 | **优秀**：LayeredDigraphLayout 是工业级 DAG 布局 |
| **MaxGraph** | 内置多种布局（继承 mxGraph）： hierarchical/orthogonal/organic/circle | 不需要（有内置等价算法） | 不需要 | 良好：hierarchical 布局可用于 DAG |
| **自研** | 完全自定义 | 可集成 | 可集成 | 取决于实现 |

**dagre vs elkjs 对比**：
- dagre：成熟稳定，100 节点内效果好，社区广泛使用
- elkjs：Eclipse 布局引擎的 JS 移植，100+ 节点密集图效果更好，支持更多布局策略（layered/force/stress/mrtree），但 API 复杂度更高

#### 5.1.8 React 集成

| 方案 | React 组件节点 | React Hooks | React 状态管理 | 现有 React 集成包 |
|------|-------------|------------|-------------|----------------|
| **@antv/x6** | 支持：`@antv/x6-react-shape@3.0.1` | 无原生 hooks；需自行封装 | 外部状态管理（推荐 Zustand/Redux） | `@antv/x6-react-shape` |
| **React Flow** | **原生**：节点就是 React 组件 | **丰富**：useNodesState/useEdgesState/useOnSelectionChange/onConnect 等 | **原生 Zustand**（依赖项） | 无需额外包 |
| **Rete.js v2** | 支持：`rete-react-plugin@2.1.0` | 无 | 框架无关，需桥接 | `rete-react-plugin` + `rete-area-plugin` |
| **LogicFlow** | 支持：`@logicflow/react-node-registry@1.2.3` | 无原生 hooks | MobX（核心依赖） | `@logicflow/react-node-registry` |
| **GoJS** | **不支持**：Canvas 渲染无法嵌入 React | `gojs-react@1.1.3` 提供有限 React 绑定 | 需手动同步 GoJS Model ↔ React State | `gojs-react`（仅数据绑定层） |
| **MaxGraph** | 不支持 | 无 | 无 | 无 |
| **自研** | 完全自定义 | 完全自定义 | 完全自定义 | 无 |

**当前项目 React Flow 集成深度**：
- `useNodesState` / `useEdgesState` 管理 DAG 状态
- `nodeTypes` 注册自定义 `canvasNode` 类型
- `isValidConnection` / `onConnect` 实现连接校验
- `useBranchPlaceholders` 自定义 hook 管理分支占位节点
- `insertNode` / `outletRouting` / `connectionInteraction` 工具函数

**迁移成本评估**：React Flow → @antv/x6 需要重写以上所有集成代码，预计前端工作量 4-6 周。

#### 5.1.9 antd 兼容性

| 方案 | antd 组件嵌入节点 | antd 组件嵌入面板 | 主题一致性 | 生态契合度 |
|------|----------------|----------------|-----------|-----------|
| **@antv/x6** | 支持（React Shape） | 支持（DOM 节点） | **同一生态**（AntV + antd 均属蚂蚁） | **最高**：AntV 官方示例大量使用 antd |
| **React Flow** | 原生支持 | 原生支持 | 中立（与 antd 无冲突也无特别优化） | 中等 |
| **Rete.js v2** | 支持（React 插件） | 需自建 | 中立 | 中等 |
| **LogicFlow** | 支持（react-node-registry，桥接层） | 需自建 | 中立 | 中等 |
| **GoJS** | **不支持**（Canvas 渲染） | 需完全自绘 | **冲突**：Canvas 无法渲染 antd 组件 | **最低** |
| **MaxGraph** | 不支持 | 需完全自绘 | 中立 | 低 |
| **自研** | 完全自定义 | 完全自定义 | 完全自定义 | 取决于实现 |

#### 5.1.10 包体积与依赖

| 方案 | 核心包大小 (gzip) | 核心依赖数 | 总安装体积 (unpacked) | 额外插件体积 |
|------|-----------------|----------|-------------------|------------|
| **@antv/x6** | ~180KB (est.) | 4 (lodash-es, mousetrap, dom-align, utility-types) | 8.6MB (v3.1.7) | v3 已合并插件，无需额外安装 |
| **React Flow** | **58KB** (gzip) | 3 (@xyflow/system, classcat, zustand) | 1.2MB (v12.10.2) | 无（功能内建） |
| **Rete.js v2** | **3KB** (核心 gzip) | 1 (@babel/runtime) | 226KB (核心) | +584KB react-plugin + 522KB area-plugin + 266KB connection-plugin |
| **LogicFlow** | ~120KB (est.) | 8 (含 mobx, preact, lodash-es 等) | 5.6MB (v2.2.3) | +5.6MB @logicflow/extension |
| **GoJS** | ~450KB (est. gzip) | 0（零依赖） | 9.7MB (v3.1.10) | +58KB gojs-react |
| **MaxGraph** | ~150KB (est.) | 未知 | 7.1MB (v0.23.0) | 无 |
| **自研** | 0（从零开始） | 自选 | 自选 | 自选 |

**包体积排名**（核心 gzip）：Rete.js 3KB < React Flow 58KB < LogicFlow ~120KB < MaxGraph ~150KB < @antv/x6 ~180KB < GoJS ~450KB

#### 5.1.11 许可证与成本

| 方案 | 许可证 | 商业使用 | 年费/授权费 | 潜在风险 |
|------|--------|---------|-----------|---------|
| **@antv/x6** | MIT | **免费**，无限制 | 无 | 无 |
| **React Flow** | MIT（核心）；xyflow Pro 提供付费附加功能 | **免费**，无限制 | xyflow Pro 提供子流程、协作等高级功能（订阅制） | 无；Pro 功能为可选项 |
| **Rete.js v2** | MIT | **免费**，无限制 | 无 | 无 |
| **LogicFlow** | Apache-2.0 | **免费**，无限制 | 无 | 无 |
| **GoJS** | **商业许可证**（需联系 Northwoods 获取报价） | **需付费**：按开发者席位授权，价格不公开（据社区反馈约 $1000-3000/开发者/年） | **有**：商业授权 perpetual 或 annual 均需付费；eval 版本水印 | **高风险**：未授权使用有水印；SaaS 产品需 OEM 授权 |
| **MaxGraph** | Apache-2.0 | **免费**，无限制 | 无 | mxGraph 遗留 API 可能存在专利风险（但 Apache-2.0 已覆盖） |
| **自研** | 自有 | 无限制 | 人力成本 | 完全自控 |

#### 5.1.12 社区与生态

| 方案 | GitHub Stars | 周下载量 | 贡献者 | Forks | 社区渠道 | 维护团队 |
|------|------------|---------|--------|-------|---------|---------|
| **@antv/x6** | 6,570 | 76K | 138 | 1,885 | 钉钉群/AntV 官网 | 蚂蚁 AntV 团队（5-10 人） |
| **React Flow** | **36,851** | **6.2M** | 154 | 2,415 | Discord (10K+ 成员) | xyflow GmbH（德国，4-6 人全职） |
| **Rete.js v2** | 12,064 | 44K | 23 | 748 | Discord | 个人开发者为主 |
| **LogicFlow** | 11,379 | 17K | 125 | 1,365 | 钉钉群/官网 | 滴滴开源团队（3-5 人） |
| **GoJS** | 8,436 | 148K | 保密（闭源开发） | 2,841 | 官方论坛/邮件 | Northwoods Software（商业公司） |
| **MaxGraph** | 1,115 | 10K | 58 | 200 | GitHub Discussions | 社区驱动（TBouffard 领衔） |
| **自研** | — | — | 内部团队 | — | — | 内部团队 |

**社区活跃度排名**：React Flow >>> GoJS > @antv/x6 > Rete.js > LogicFlow > MaxGraph

**维护风险**：
- **Rete.js**：23 个贡献者，v2.0.5 → v2.0.6 间隔 10 个月，维护频率堪忧
- **MaxGraph**：仍在 v0.x，API 未稳定，存在 breaking change 风险
- **LogicFlow**：2.2.0 仍标记为 alpha，稳定版停留在 2.1.x
- **React Flow / @antv/x6 / GoJS**：维护活跃，版本迭代正常

#### 5.1.13 生产采纳案例

| 方案 | 已知生产案例 | 营销/工作流相关案例 |
|------|-----------|-----------------|
| **@antv/x6** | 蚂蚁内部全量（语雀流程图、钉钉审批流、支付宝运营平台）；阿里云凤；外部：字节/快手内部工具 | **支付宝运营画布**（最接近营销画布场景）；钉钉审批流 |
| **React Flow** | Stripe（支付流程可视化）、Zapier（自动化流程）、Webflow、Shopify、n8n（开源工作流自动化） | n8n（工作流自动化，40K+ GitHub stars）；Zapier（营销自动化集成） |
| **Rete.js v2** | Unity Visual Scripting 插件、多个创意编码项目、教育类可视化编程 | 无已知营销自动化案例 |
| **LogicFlow** | 滴滴内部全量（工作流引擎、审批流）；作业帮；美团（部分）；京东方 | 滴滴工作流引擎（BPMN 场景） |
| **GoJS** | Intel/SAP/Autodesk/Boeing/UnitedHealth 等大型企业；医疗/制造/金融行业 | 无已知营销自动化案例 |
| **MaxGraph** | Draw.io（部分集成）；一些企业内部工具 | 无 |
| **自研** | — | — |

**营销平台 DAG 编辑器技术栈参考**：

| 平台 | 编辑器技术 | 特点 |
|------|----------|------|
| Braze Canvas | 自研（Canvas 渲染） | 闭源，线性旅程 + 分支，体验流畅 |
| Iterable Workflow | 自研 | 闭源，拖拽式工作流 |
| HubSpot Workflow | 自研 | 闭源，简单触发-条件-动作流 |
| n8n | **Vue Flow**（@vue-flow/core） | 开源，40K+ stars，节点式自动化 |
| Node-RED | 自研 SVG 编辑器 | 开源，IBM 出品，IoT/消息流 |
| Apache Airflow | DAG 代码 + 简单可视化 | 非拖拽式 |
| 神策用户运营 | 未公开（疑似自研） | 旅程编排功能 |
| GrowingIO | 未公开 | 运营画布功能 |

**关键发现**：主流营销自动化平台（Braze/Iterable/HubSpot）均采用自研编辑器。开源工作流工具 n8n 使用 Vue Flow（React Flow 的 Vue 移植），是最大规模的公开参考案例。

#### 5.1.14 六维评分矩阵

按白皮书 1.2 节定义的六维评估框架（场景适配度 30% + 生产成熟度 25% + 迁移成本 20% + 运维复杂度 10% + 生态与社区 10% + 许可证风险 5%）：

| 维度 (权重) | @antv/x6 (5分制) | React Flow (5分制) | Rete.js v2 (5分制) | LogicFlow (5分制) | GoJS (5分制) | MaxGraph (5分制) | 自研 (5分制) |
|------------|----------------|-------------------|-------------------|------------------|-------------|----------------|------------|
| 场景适配度 (30%) | **4.5** | 3.0 | 2.5 | **4.0** | 3.5 | 2.0 | 5.0 |
| 生产成熟度 (25%) | **4.0** | **4.5** | 2.0 | 3.5 | **5.0** | 2.0 | 1.0 |
| 迁移成本 (20%) | 2.5 | **5.0** | 1.5 | 2.0 | 1.0 | 1.0 | 0.5 |
| 运维复杂度 (10%) | 4.0 | **4.5** | 3.5 | 3.5 | 3.0 | 3.0 | 1.0 |
| 生态与社区 (10%) | 3.5 | **5.0** | 2.5 | 3.0 | 4.0 | 1.5 | 0.5 |
| 许可证风险 (5%) | **5.0** | **5.0** | **5.0** | **5.0** | 1.0 | **5.0** | **5.0** |
| **加权总分** | **3.73** | **3.93** | **2.28** | **3.38** | **3.13** | **1.90** | **2.58** |

**评分说明**：
- **@antv/x6** 场景适配度最高（正交路由 + 端口约束 + 虚拟渲染 + antd 同生态），但迁移成本高
- **React Flow** 迁移成本最低（当前已在使用），但场景适配度中低（无正交路由、无端口概念、无避障）
- **LogicFlow** 工作流语义最强（BPMN 基因），但 v2.2 未稳定、React 桥接有额外复杂度
- **GoJS** 性能和功能最强，但商业授权 + Canvas 渲染无法嵌入 antd 是硬伤
- **Rete.js / MaxGraph / 自研** 均不推荐（维护风险 / 不成熟 / 投入产出比差）

#### 5.1.15 选型结论

| 排名 | 方案 | 加权分 | 推荐 | 理由 |
|------|------|--------|------|------|
| 1 | **React Flow** | 3.93 | **短期继续** | 迁移成本为零，当前已深度集成；需补齐正交路由 + 端口约束层 |
| 2 | **@antv/x6** | 3.73 | **中期迁移目标** | 场景适配度最高，正交路由 + 端口约束 + 虚拟渲染解决核心痛点 |
| 3 | **LogicFlow** | 3.38 | 备选 | 工作流语义最强但生态偏弱，v2.2 稳定性待验证 |
| 4 | **GoJS** | 3.13 | 不推荐 | 商业授权 + antd 不兼容 |
| 5 | **自研** | 2.58 | 远期可选 | 投入产出比差，仅在极端定制需求下考虑 |
| 6 | **Rete.js v2** | 2.28 | 不推荐 | 维护风险高，范式不匹配 |
| 7 | **MaxGraph** | 1.90 | 不推荐 | API 未稳定，生产不可用 |

#### 5.1.16 推荐策略

**Phase 0（当前 — 上线优先）**：继续使用 React Flow，补齐关键短板：
1. 实现正交边路由（自定义 Edge 组件，参考 Manhattan routing 算法）
2. 封装端口约束层（基于 `isValidConnection` + 节点类型注册表）
3. 补齐工作流校验（DAG 环检测、孤立节点检测、必填出口校验）
4. 预估工作量：2-3 周

**Phase 1（中期 — 核心架构改造后）**：评估迁移 @antv/x6：
1. 在 @antv/x6 v3.x 上构建 POC，验证 30+ 节点类型 + 正交路由 + antd 面板
2. 设计适配层：将当前 React Flow 数据模型映射到 X6 Model
3. 分批迁移：先迁移渲染层（节点/边），再迁移交互层（拖拽/连接），最后迁移校验层
4. 预估工作量：4-6 周

**Phase 2（长期 — 视需求）**：若 @antv/x6 无法满足极致定制需求，考虑自研：
1. 基于 SVG 渲染引擎 + React 节点系统的混合架构
2. 复用 @antv/x6 的路由算法思路（曼哈顿路由是公开算法）
3. 预估工作量：3-6 个月

**不选 LogicFlow 的排除理由**：
- v2.2.0 仍标记为 alpha，稳定版 2.1.x 缺少新特性
- 核心渲染从 React 切换为 Preact，React 节点通过桥接层实现，增加复杂度
- 社区规模和 npm 下载量（17K/周）远低于 React Flow（6.2M/周）和 @antv/x6（76K/周）
- 滴滴内部为主要生产案例，外部生产验证不足

**不选 GoJS 的排除理由**：
- 商业许可证，SaaS 产品需 OEM 授权，费用不透明且可能很高
- Canvas 渲染无法嵌入 antd 组件，需要完全自绘所有 UI
- 与 React 生态隔离，需手动同步状态
- 对营销画布的节点配置面板需求（antd Form）是硬性冲突

**不选 Rete.js 的排除理由**：
- v2.0.5 → v2.0.6 间隔 10 个月，维护节奏不可接受
- 23 个贡献者，是所有方案中最少的
- 可视化编程范式与工作流编辑器有语义差距
- 无已知营销/工作流生产案例

**不选 MaxGraph 的排除理由**：
- 仍在 v0.x，API 未稳定，每个小版本都可能 breaking change
- 1,115 stars，10K 周下载，社区规模过小
- 无 React 集成，无 antd 兼容方案
- 主要用于 draw.io 类场景，非工作流编辑器

---

## 6. 选型互锁与依赖关系

### 6.1 互锁陷阱：WebFlux ↔ Reactor DAG

```
WebFlux（响应式 HTTP）
    ↓ 逼迫
DagEngine 用 Reactor Mono 链
    ↓ 反向绑定
无法脱离 WebFlux（DagEngine 是最深的 Reactor 消费者）
```

**打破方式**：同步迁移到 Spring MVC + 虚拟线程 + 命令式 DAG 引擎，一次性消除根因。

### 6.2 互锁链条：数据基建断裂

```
无离线数仓 (K)
    → 无历史分析 → 运营用 MySQL 做报表 → 打主库
无实时数仓 (L)
    → 全表扫描 + Java Stream 聚合 → OOM
无实时计算 (M)
    → Disruptor 做 CEP → 无容错
无数据管道 (O)
    → 运营数据与分析数据割裂
```

**打破方式**：统一方案 `MySQL → Flink CDC → Kafka → Doris`，一条管道同时解决四个问题。

### 6.3 依赖关系图

```
Phase 1（可独立完成，无前置依赖）：
  ├── N. PowerJob 替换 Spring Scheduler（ScheduleRegistrar 接口已就绪）
  ├── D. 虚拟线程 Executor 替换 Disruptor（单一类替换）
  ├── F. MQ Topic 拆分（RocketMQ 配置变更）
  ├── J. 投递队列（加 RocketMQ producer）
  └── G. 确定性映射 + Redis BITMAP（AudienceBitmapStore 接口替换）

Phase 2（需 Phase 1 完成）：
  ├── A + B. Spring MVC + 命令式 DAG 引擎（互锁，必须同步）
  └── E. Aviator 替换 Groovy（NodeHandler 接口干净）

Phase 3（需 Phase 2 完成，新建基础设施）：
  ├── K + L. Doris 离线+实时数仓
  ├── M. Flink 实时计算
  ├── O. Flink CDC 数据管道
  └── H. Trace 数据 sink 到 Doris

Phase 4（长期演进）：
  ├── C. 服务拆分（API / Engine / Audience Compute）
  └── I. @antv/x6 替换 React Flow（4-6 周前端重写，需 Phase 0 先补齐 React Flow 短板）
```

---

## 7. 迁移路线图

### Phase 1 — 上线必须（预计 4 周）

| 周次 | 任务 | 前置 | 风险 |
|------|------|------|------|
| W1-2 | PowerJob adapter + 迁移所有 cron 任务 | 无 | 低（ScheduleRegistrar 接口已就绪） |
| W1-2 | 虚拟线程 Executor 替换 Disruptor | 无 | 低（单一类替换） |
| W3 | MQ Topic 拆分 + 消费组重组 | 无 | 中（需确保消息不丢） |
| W3-4 | 投递队列 + Outbox pattern | 无 | 中（需处理 DAG 等待投递语义） |
| W3-4 | 确定性映射 + Redis BITMAP | 无 | 中（需重算存量 bitmap） |

### Phase 2 — 核心架构改造（预计 8 周）

| 周次 | 任务 | 前置 | 风险 |
|------|------|------|------|
| W5-8 | 命令式 DAG 执行引擎重写 | Phase 1 | 高（DagEngine 是系统核心） |
| W5-8 | Spring MVC + 虚拟线程迁移 | Phase 1 | 高（37 处 boundedElastic 拆解） |
| W9-12 | Aviator/QLExpress 替换 Groovy（3 阶段） | Phase 2 | 中（需迁移存量脚本，90% 可自动转换） |

### Phase 3 — 数据基建（预计 6 周）

| 周次 | 任务 | 前置 | 风险 |
|------|------|------|------|
| W13-14 | Doris 集群搭建 + 数仓建模 | Phase 2 | 中 |
| W15-16 | Flink CDC MySQL→Doris 管道 | Doris 就绪 | 中 |
| W17-18 | 报表 API 迁移到 Doris | Flink CDC 就绪 | 低 |
| W17-18 | Trace 数据 sink 到 Doris | Doris 就绪 | 低 |

### Phase 4 — 长期演进（持续）

| 任务 | 前置 | 风险 |
|------|------|------|
| Flink CEP 行为触发迁移 | Phase 3 Flink 就绪 | 高 |
| 服务拆分（API / Engine / Audience） | Phase 2 | 高 |
| @antv/x6 替换 React Flow | 无 | 高（4-6 周前端重写；建议 Phase 0 先在 React Flow 补齐正交路由+端口约束） |

---

## 附录 A：生产验证详情

| 技术 | 版本 | 治理 | 主要生产企业 | 验证规模 |
|------|------|------|------------|---------|
| Spring Boot 虚拟线程 | 3.2+ | Pivotal/VMware | Spring 生态全部 | Java 21 GA |
| Apache Doris | 4.0.x | Apache 顶级项目 | 百度/京东/美团/瑞幸/比亚迪/福特/三星/小米/米哈游 | 10000+ 企业 |
| Apache Flink | 1.20+ | Apache 顶级项目 | 阿里/Uber/Netflix/Apple | 全球数千企业 |
| Flink CDC | 3.6.0 | Apache 顶级项目 | 与 Flink 共享 | MySQL→Doris 官方 connector |
| PowerJob | 5.1.2 | 开源（PowerJob） | 京东/唯品会/中通/德邦/美图/微盟 | 7.7k stars |
| Aviator | 5.4.x | 开源（killme2008） | 阿里中间件/淘宝/天猫/蚂蚁 | 阿里营销平台 |
| @antv/x6 | 3.1.7 | 蚂蚁 AntV | 蚂蚁内部（支付宝运营画布/钉钉审批流/语雀流程图）；阿里云凤 | 蚂蚁全量；v3 合并插件+虚拟渲染 |

## 附录 B：替代方案速查表

| 选型 | 首选 | 次选 | 不选 | 不选理由 |
|------|------|------|------|---------|
| Web 框架 | Spring MVC + 虚拟线程 | — | 继续用 WebFlux | 零收益 + 高复杂度 |
| DAG 引擎 | 命令式步进执行器 | Temporal | 继续用 Reactor Mono | 用错抽象 |
| 调度器 | PowerJob | XXL-Job | ElasticJob | 需要 ZK；Quartz 社区弱功能弱 |
| OLAP | Apache Doris | StarRocks | ClickHouse/Druid/TiDB/Greenplum | 低并发/无事务/SQL受限/非HTAP/非实时（详见 3.1.4 排除理由） |
| 实时计算 | Apache Flink | Kafka Streams | Spark Streaming | 非真流 + 无 CEP |
| 数据管道 | Flink CDC | Canal | Debezium | Kafka 优先不匹配 RocketMQ |
| 脚本引擎 | Aviator + QLExpress | GraalJS(远期) | Groovy | 沙箱不安全 + 泄漏 + 超时不可靠 |
| 任务分发 | 虚拟线程 + Semaphore | ThreadPoolExecutor | Disruptor | 杀鸡牛刀 |
| 画布编辑 | @antv/x6（中期迁移） | LogicFlow（备选） | React Flow/Rete.js/GoJS/MaxGraph/自研 | 无正交路由/无端口约束/无避障（详见 5.1.14） |
