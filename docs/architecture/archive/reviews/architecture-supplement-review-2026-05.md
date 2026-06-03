# 架构评审补充报告 — 已有审查未覆盖的 10 个维度

> 生成日期: 2026-05-31
> 作者: Winston (Architect)
> 前置文档:
>   - architecture-deep-review-2026-05.md (技术选型 + 设计缺陷)
>   - deep-code-audit-round6.md (6轮代码审核, 144项)
>   - architect-checklist-report.md (10章检查清单, 38%通过率)
>   - bmad-product-review-2026-05.md (产品缺项, ~208项)
>
> 本报告聚焦上述文档**未覆盖或覆盖不足**的架构维度。

---

## 执行摘要

| 维度 | 就绪度 | 优先级 | 核心风险 |
|------|--------|--------|----------|
| 1. 成本架构 | **5%** | P0 | 无成本模型，无法预测生产费用 |
| 2. 灾难恢复/业务连续性 | **10%** | P0 | Redis 单点状态丢失=执行永久卡死 |
| 3. 演进路线可行性 | **15%** | P1 | 路线图无资源估算、无回滚策略 |
| 4. 团队/组织架构适配 | **20%** | P1 | 上帝类修改需核心人员，知识集中 |
| 5. 供应商锁定风险 | **25%** | P2 | Redis/MQ/MySQL 深度绑定，无抽象层 |
| 6. 知识管理/文档体系 | **8%** | P2 | 零 ADR、零运维手册、零排查指南 |
| 7. 可测试性架构 | **15%** | P2 | 零集成测试、零组件测试、上帝类不可测 |
| 8. 性能容量模型 | **12%** | P2 | 无 SLA、无容量规划、无瓶颈预测 |
| 9. AI 辅助开发适配 | **35%** | P3 | 上帝类超出 AI 上下文窗口 |
| 10. 合规认证就绪度 | **10%** | P3 | 无认证路径、无隐私评估流程 |

**总体补充就绪度: 15%** — 10 个维度中 6 个低于 20%。

---

## 1. 成本架构 (5%)

### 1.1 当前资源消耗清单

| 资源 | 配置值 | 单实例内存估算 | 成本驱动因素 |
|------|--------|---------------|-------------|
| Disruptor Ring Buffer | 65,536 slots | ~16 MB | 固定，与流量无关 |
| TraceWriteBuffer | 50,000 条上限 | ~50 MB (50K × ~1KB) | 与执行吞吐正相关 |
| HikariCP 连接池 | 33 连接 | ~33 MB | 与并发 DB 查询正相关 |
| Redis Lettuce 连接池 | 64 active | 64 TCP 连接 | 固定 |
| WebClient 连接池 | 500 连接 | 500 TCP 连接 | 与外部 API 调用正相关 |
| Caffeine L1 缓存 | ~2,700 entries | ~5-10 MB | 与活跃画布数正相关 |
| 虚拟线程栈 | 每执行 ~1-3 个 | ~1-3 MB/执行 | 与并发执行数正相关 |
| ExecutionContext (Redis) | 每执行 1 份 | Redis 内存 | 与并发执行数 × 上下文大小 |

**单实例 JVM 堆估算**: 基础 ~120 MB + 3000 并发执行 × ~3 MB = **~9.1 GB** (不含 Groovy Metaspace)

### 1.2 Redis 内存成本模型

| Key 模式 | 单条大小 | 数量级 | 总内存估算 |
|----------|---------|--------|-----------|
| `canvas:{id}:user:{uid}` (ExecutionContext) | ~50-200 KB | 3000 并发 | 150-600 MB |
| `canvas:inflight:*` (3 ZSET) | ~100 bytes/entry | 3000 | ~1 MB |
| `canvas:quota:*` (配额键) | ~50 bytes | 画布数 × 日活用户 | 1000 画布 × 100K 用户 = ~5 GB |
| `canvas:trigger:*` (路由表) | ~1 KB | 画布数 | 1000 画布 = ~1 MB |
| `canvas:config:*` (L2 缓存) | ~10-100 KB | 活跃画布 | 500 = ~50 MB |
| `canvas:marketing:freq:*` (频控) | ~50 bytes | 画布 × 用户 × 时间桶 | 高基数，难估算 |

**Redis 内存估算**: 1000 画布 + 100K DAU + 3000 并发 → **6-8 GB Redis**

**关键发现**: `canvas:quota:*` 键是 Redis 内存最大消耗者。部分 `canvas:global_count:*` 和 `canvas:quota:total:*` **无 TTL**，仅靠画布下线/归档时清理。长期运行 Redis 内存只增不减。

### 1.3 MySQL 存储成本模型

| 表 | 行增长速率 | 单行大小 | 月增量 (3000 QPS 假设) |
|----|-----------|---------|----------------------|
| `canvas_execution` | ~3000/秒 × 86400 = 2.6 亿/天 | ~500 bytes | **~3.9 TB/月** |
| `canvas_execution_trace` | ~5× execution | ~1 KB | **~39 TB/月** |
| `canvas_execution_request` | ~1× execution | ~800 bytes | **~6.2 TB/月** |
| `canvas_execution_dlq` | 低 (异常时) | ~1 KB | ~1 GB/月 |

**关键发现**: 以 3000 QPS 持续运行，**月存储增量约 49 TB**。无分区（V6 是 no-op）、无归档策略、无保留期限。3 个月后单库超 150 TB，MySQL 无法承受。

**成本推算** (阿里云/腾讯云定价参考):
- 云 MySQL 8C32G: ~2000 元/月
- 云 Redis 8G: ~1500 元/月
- 云 RocketMQ 标准版: ~800 元/月
- 云磁盘 (ESSD PL1): ~1 元/GB/月 → 49 TB = **49,000 元/月**
- **总计: ~53,300 元/月** (存储占 92%)

### 1.4 缺失的成本管控能力

| 能力 | 状态 | 影响 |
|------|------|------|
| 按画布/租户成本分摊 | ❌ 无 | 无法核算单个画布运营成本 |
| 资源消耗热点分析 | ❌ 无 | 不知道哪些节点类型最耗资源 |
| 存储保留策略 | ❌ 无 | 数据无限增长 |
| 执行成本估算 | ❌ 无 | 发布前无法评估成本影响 |
| 成本告警 | ❌ 无 | 超预算无感知 |

### 1.5 建议

1. **P0**: 制定数据保留策略 — execution_trace 保留 7 天，execution 保留 30 天，超期归档/删除
2. **P0**: 实现 V6 分区 — 按月 RANGE 分区 execution/trace 表，支持 DROP PARTITION 快速清理
3. **P1**: 为 `canvas:quota:*` 和 `canvas:global_count:*` 添加 TTL，防止 Redis 内存泄漏
4. **P1**: 建立成本模型文档 — 按并发量/画布数/DAU 三个维度推算资源需求
5. **P2**: 添加按画布的执行计数指标，支持成本分摊

---

## 2. 灾难恢复/业务连续性 (10%)

### 2.1 当前恢复能力评估

| 故障场景 | 当前恢复能力 | RTO | RPO | 评级 |
|---------|-------------|-----|-----|------|
| **Redis 宕机** | ExecutionContext 全丢，PAUSED 执行永久卡死 | ∞ | 全丢 | ❌ CRITICAL |
| **Redis 主从切换** | Lettuce 自动重连，但 PAUSED 上下文可能丢失 | ~30s | 可能丢 | ⚠️ |
| **MySQL 宕机** | HikariCP 3s 超时，所有 DB 操作失败 | ~60s | 0 (ACID) | ⚠️ |
| **MySQL 主从切换** | 无配置，需手动 | ~5-30min | 0 | ❌ |
| **RocketMQ 宕机** | 新触发无法入队，已有 Disruptor 事件可继续 | ~60s | 队列消息丢 | ⚠️ |
| **单 JVM 实例宕机** | InFlightExecutionRegistry TTL 过期后其他实例可接管 | ~10min | PAUSED 上下文丢 | ⚠️ |
| **全机房不可用** | 无跨机房方案 | ∞ | 全丢 | ❌ CRITICAL |
| **Groovy Metaspace OOM** | JVM 崩溃，需重启 | ~5min | PAUSED 上下文丢 | ⚠️ |

### 2.2 Redis 单点分析 — 最关键风险

Redis 承载了 **7 类关键状态**，无任何替代方案：

| 状态类型 | Key 模式 | 恢复方式 | Redis 丢失后果 |
|---------|---------|---------|--------------|
| ExecutionContext | `canvas:{id}:user:{uid}` | ❌ 无 DB 回退 | PAUSED 执行永久卡死 |
| 消息去重 | `canvas:dedup:*` | DB 重新计算 (慢) | 消息重复处理 |
| 执行准入 | `canvas:inflight:*` ZSET | TTL 自动过期 (~10min) | 短期超并发 |
| 触发配额 | `canvas:quota:*` | DB 查询 (最终一致) | 配额重置，可能超发 |
| 路由表 | `canvas:trigger:*` | DB 重建 (CanvasRouteInitializer) | 触发短暂中断 |
| 缓存 L2 | `canvas:config:*` | DB 重加载 | 缓存击穿，DB 压力 |
| 熔断器状态 | JVM 本地 | 自动重置 | 短期保护失效 |

**最严重**: ExecutionContext 仅存 Redis。Redis flushall 后，所有 WAIT/HUB/GOAL_CHECK 节点的 PAUSED 执行**无法恢复**，用户永远收不到预期消息。

### 2.3 Wait 节点跨部署分析

| 组件 | 部署后行为 | 恢复机制 | 评级 |
|------|-----------|---------|------|
| `Mono.delay()` 定时器 | ❌ 丢失 | ExecutionWatchdog 30s 扫描补偿 | ⚠️ 延迟恢复 |
| `NodeGate.executing` (内存) | ❌ 丢失 | 重新从 ExecutionContext 重建 | ⚠️ 需上下文完整 |
| `scheduledHubTimeouts` (内存) | ❌ 丢失 | Watchdog 扫描 | ⚠️ |
| WaitSubscription (DB) | ✅ 持久化 | CAS 恢复 | ✅ |
| ExecutionContext (Redis) | ✅ 持久化 | load + resume | ✅ (Redis 可用时) |

**滚动部署影响**: 每次 JVM 重启，所有内存定时器丢失。ExecutionWatchdog 30s 周期扫描可补偿，但存在 **30s 恢复延迟窗口**。高频定时触发场景下，此窗口可能导致超时误判。

### 2.4 优雅关停分析

| 组件 | @PreDestroy | 关停行为 | 风险 |
|------|------------|---------|------|
| CanvasExecutionService | ❌ 无 | 继续接受触发 | 新执行可能写到一半 |
| MqTriggerConsumer | ❌ 无 | 继续消费消息 | 消息处理到一半 |
| Disruptor | ✅ shutdown() | 等待 WorkHandler 处理完 | 但 `.subscribe()` 异步链已脱离 |
| InFlightExecutionRegistry | ❌ 无 drain | Redis ZSET 残留 | 依赖 TTL 过期 (~10min) |
| TraceWriteBuffer | ❌ 无 | 缓冲区 trace 丢失 | 最多 50,000 条 |
| GroovyHandler | ❌ 无 | 虚拟线程不被等待 | 脚本执行中断 |

**`server.shutdown: graceful` 未配置**。Spring Boot 默认立即终止。

### 2.5 建议

1. **P0**: ExecutionContext 双写 — Redis (热) + MySQL (冷备)，Redis 不可用时从 DB 恢复
2. **P0**: 配置 `server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase: 30s`
3. **P0**: 添加 @PreDestroy — MqTriggerConsumer 暂停消费、InFlightExecutionRegistry drain、TraceWriteBuffer flush
4. **P1**: 定义 RTO/RPO 目标 — 建议 RTO=5min, RPO=0 (执行结果), RPO=30s (上下文)
5. **P1**: Redis Sentinel/Cluster 部署 — 最少 3 节点，自动主从切换
6. **P2**: 跨机房灾备方案 — MySQL 异步复制 + Redis AOF 持久化 + MQ 镜像队列

---

## 3. 演进路线可行性 (15%)

### 3.1 已有路线图回顾

| Phase | 内容 | 周期 | 人力估算 | 依赖 | 风险 |
|-------|------|------|---------|------|------|
| 1 止血 | XXL-Job + Wait 外部化 + 画布并发限制 | 1-2周 | 2人 | 无 | 低 |
| 2 解耦 | 拆分单体 + 投递队列 + 拆 MQ Topic | 2-4周 | 3-4人 | Phase 1 | 中 |
| 3 重构 | Spring MVC + 虚拟线程 + 命令式 DAG | 4-8周 | 4-6人 | Phase 2 | **高** |
| 4 数据基建 | Doris + Flink CDC + trace 迁移 | 4-6周 | 3-4人 | 可与 P3 并行 | 高 |
| 5 前端重构 | Zustand + Web Worker + Zod | 4-6周 | 2-3人 | 可与 P3/P4 并行 | 中 |

### 3.2 缺失的关键要素

| 要素 | 状态 | 影响 |
|------|------|------|
| **人力投入估算** | ❌ 无 | 不知道需要多少人、是否可行 |
| **回滚策略** | ❌ 无 | 迁移失败后无法回退 |
| **增量迁移路径** | ❌ 无 | "一次性消除根因"风险过高 |
| **里程碑验收标准** | ❌ 无 | 不知道"完成"的定义 |
| **并行度分析** | ❌ 无 | 不知道哪些可并行 |
| **技术预研** | ❌ 无 | Spring MVC 迁移是否可行未验证 |
| **兼容性矩阵** | ❌ 无 | 迁移期间新旧代码如何共存 |

### 3.3 Phase 3 风险深度分析 — 最关键迁移

**当前**: WebFlux + Reactor + 178 处 boundedElastic + 递归 Mono 链
**目标**: Spring MVC + 虚拟线程 + 命令式 DAG

| 迁移项 | 影响范围 | 难度 | 增量可行性 |
|--------|---------|------|-----------|
| Controller 返回类型 Mono→同步 | 29 个 Controller | 中 | ✅ 可逐个迁移 |
| Service 层 Mono→同步 | ~50 个 Service | 高 | ⚠️ 上下游联动 |
| DagEngine 递归 Mono→命令式 | 1540 行核心 | **极高** | ❌ 必须一次性 |
| Handler executeAsync→同步 | 64 个 Handler | 中 | ✅ 可逐个迁移 |
| InFlightExecutionRegistry | Redis 阻塞→可保留 | 低 | ✅ 无需改 |
| boundedElastic 移除 | 178 处 | 中 | ✅ 随 Controller/Service 迁移 |

**关键约束**: DagEngine 是所有执行的入口，其 Reactor 模型与 64 个 Handler 的 `Mono<NodeResult>` 签名深度耦合。**DagEngine 必须一次性重写**，无法增量迁移。

### 3.4 建议的增量迁移路径

```
Phase 0: 技术预研 (1周)
  └─ 证明: Spring MVC + 虚拟线程 + 命令式 DAG 可达到同等吞吐
  └─ 基准: 单画布 3000 QPS 压测对比

Phase 1: 止血 (1-2周, 2人)
  └─ XXL-Job 替换 Spring Scheduler
  └─ Wait 定时器外部化 (Redis Sorted Set + Watchdog)
  └─ 画布级并发限制
  └─ 验收: 定时触发可跨部署存活

Phase 2: 外围同步化 (2-3周, 2-3人)
  └─ 非 DagEngine 的 Controller/Service 逐个迁移到同步
  └─ 保留 DagEngine + Handler 的 Reactor 接口不变
  └─ 验收: 非 DAG 路径全部同步化

Phase 3: DAG 引擎重写 (4-6周, 3-4人)
  └─ 命令式 DagEngine (虚拟线程 + CountDownLatch 替代 NodeGate)
  └─ Handler 签名同步化 (execute → NodeResult)
  └─ 集成测试: 全部 64 Handler + 端到端
  └─ 验收: 压测达到同等吞吐 + 延迟不退化

Phase 4: 清理 (1-2周, 2人)
  └─ 移除 Reactor 依赖
  └─ 移除 boundedElastic
  └─ 验收: 零 Reactor import
```

### 3.5 回滚策略

| Phase | 回滚方式 | 成本 |
|-------|---------|------|
| Phase 1 | 关停 XXL-Job，恢复 Spring Scheduler | 低 (配置切换) |
| Phase 2 | Git revert 同步化改动 | 中 (逐个 revert) |
| Phase 3 | **无法回滚** — DagEngine 重写是单向门 | **极高** |
| Phase 4 | Git revert | 低 |

**Phase 3 无法回滚** — 一旦命令式 DagEngine 上线，所有 Handler 签名已变，回退需重写回 Reactor。建议：
- Phase 3 前必须完成 Phase 0 技术预研
- Phase 3 实施时保留 Reactor DagEngine 代码分支至少 2 周
- 灰度切换：先 10% 流量走新引擎，逐步放量

---

## 4. 团队/组织架构适配 (20%)

### 4.1 代码认知负荷分析

| 文件 | 行数 | 修改风险 | 需要的技能 | 预估上手时间 |
|------|------|---------|-----------|------------|
| DagEngine.java | 1540 | **极高** | Reactor + DAG + CAS + 虚拟线程 | 2-4 周 |
| CanvasExecutionService.java | 1407 | **极高** | Reactor + MQ + Redis + 调度 | 2-3 周 |
| TieredCacheImpl.java | 1556 | 高 | 缓存一致性 + Redis + Caffeine | 1-2 周 |
| canvas-editor/index.tsx | 2084 | 高 | React + xyflow + 状态管理 | 1-2 周 |
| config-panel/index.tsx | 1414 | 中 | React + Schema 驱动 | 1 周 |
| WaitHandler.java | 504 | 高 | Reactor + Wait 状态机 + Redis | 1-2 周 |

### 4.2 知识集中度 — "巴士因子"分析

| 核心组件 | 能安全修改的人数估算 | 巴士因子 |
|---------|-------------------|---------|
| DagEngine | 1-2 人 | **1** (极危险) |
| CanvasExecutionService | 1-2 人 | **1** |
| TieredCacheImpl | 1-2 人 | **1** |
| canvas-editor (前端) | 1-2 人 | **1** |
| Handler 生态 (单个) | 3-5 人 | **3** (较安全) |
| Flyway 迁移 | 3-5 人 | **3** |

**巴士因子 = 1**: 如果核心开发离职，DagEngine 和 CanvasExecutionService **无人能安全修改**。

### 4.3 技能需求矩阵

| 技能 | 必需程度 | 团队掌握难度 | 替代方案 |
|------|---------|-------------|---------|
| Reactor/WebFlux | **极高** (178处 boundedElastic) | 高 (学习曲线陡) | 迁移到 Spring MVC |
| 虚拟线程 (Java 21) | 高 | 中 (新特性) | 平台线程 (性能差) |
| Disruptor LMAX | 中 | 高 (小众) | ArrayBlockingQueue |
| MyBatis-Plus | 高 | 低 (主流) | — |
| React + xyflow | 高 | 中 | — |
| Redis Lua 脚本 | 中 | 中 | 移到 Java 逻辑 |
| Groovy 沙箱 | 中 | 高 (安全敏感) | 表达式引擎 (Aviator/MVEL) |

### 4.4 @Lazy 循环依赖 — 职责不清的信号

7 处 @Lazy 循环依赖说明组件职责边界模糊：

| 循环 | 涉及组件 | 根因 |
|------|---------|------|
| DagEngine ↔ CanvasExecutionService | 引擎 ↔ 触发服务 | 引擎需要触发子画布，触发需要引擎执行 |
| CanvasDisruptorService ↔ CanvasExecutionService ↔ CanvasExecutionRequestExecutor | 三向循环 | 分发、执行、请求处理职责交叉 |
| TaggerHandler ↔ CanvasExecutionService | Handler ↔ Service | Handler 需要触发重新执行 |
| SubFlowRefHandler ↔ DagEngine | Handler ↔ Engine | 子流程需要引擎执行子图 |
| CanvasTriggerHandler ↔ DagEngine | Handler ↔ Engine | 画布触发需要引擎执行子画布 |
| TransferJourneyHandler ↔ CanvasExecutionService | Handler ↔ Service | 转旅程需要触发新执行 |

### 4.5 建议

1. **P0**: 拆分 DagEngine — 至少拆为 DagExecutor + NodeScheduler + RepeatManager + GateManager 4 个类，降低认知负荷
2. **P0**: 编写 DagEngine 核心流程文档 — 6 阶段管道 + 状态机图 + 关键 CAS 机制
3. **P1**: 消除 @Lazy 循环 — 引入事件总线解耦 Handler → Engine 依赖
4. **P1**: 建立代码审查规则 — DagEngine/CanvasExecutionService 变更必须 2 人审查
5. **P2**: 新人培训路径 — Handler 开发 (1周) → Service 开发 (2周) → DagEngine 修改 (4周)

---

## 5. 供应商锁定风险 (25%)

### 5.1 锁定程度评估

| 供应商/技术 | 绑定深度 | 替代方案 | 迁移成本 | 锁定评级 |
|------------|---------|---------|---------|---------|
| **Redis** | 极深: 6 Lua 脚本 + Bitmap + Pub/Sub + ZSET + 分布式锁 | KeyDB/Dragonfly/Garnet | **极高** (Lua 脚本重写) | 🔴 |
| **RocketMQ** | 深: 3 Topic + ORDERLY 消费 + 延迟消息 | Kafka/Pulsar | 高 (消费模型不同) | 🟠 |
| **MySQL** | 深: 90 Flyway 迁移 + JSON 列 + MyBatis-Plus | PostgreSQL | 中-高 (DDL + ORM) | 🟠 |
| **MyBatis-Plus** | 深: 全部 Mapper + 代码生成 | JPA/JOOQ | 高 (重写所有 Mapper) | 🟠 |
| **Hutool** | 中: Snowflake + 工具类 | Guava + 自研 | 低 | 🟡 |
| **Disruptor** | 中: 单一入口点 | ArrayBlockingQueue | 低 | 🟡 |
| **Groovy** | 中: 脚本引擎 + 沙箱 | Aviator/MVEL/Janino | 中 (脚本迁移) | 🟠 |
| **React Flow** | 深: 2084 行编辑器深度耦合 | 自研/MaxGraph | 极高 | 🔴 |
| **Antd 5** | 中: 全局使用 | Arco Design/自研 | 高 | 🟠 |
| **Caffeine** | 低: 标准 JSR-107 缓存 | Guava/ExpiringMap | 低 | 🟢 |

### 5.2 Redis 锁定详情 — 最严重

Redis 使用了 **6 个 Lua 脚本** 实现原子操作：

| 脚本 | 位置 | 功能 | 替代难度 |
|------|------|------|---------|
| RESUME_LOCK_RELEASE | ContextPersistenceService:145 | 原子 check-then-del 锁释放 | 中 (可用事务模拟) |
| ACQUIRE_SCRIPT | InFlightExecutionRegistry:281 | 原子 ZSET 准入 (3 ZSET) | **极高** (3 ZSET 联合操作) |
| RELEASE_SCRIPT | InFlightExecutionRegistry:305 | 原子多 ZSET ZREM | 高 |
| INCR_WITH_TTL_SCRIPT | TriggerPreCheckService:236 | 原子 INCR + PEXPIRE | 低 (可用 INCR + EXPIRE 近似) |
| PUBLISH_LOCK_RELEASE | CanvasService:249 | 原子 check-then-del | 中 |
| TieredCache 分布式锁 | TieredCacheImpl:766 | 原子 check-then-del | 中 |

**InFlightExecutionRegistry 的 ACQUIRE_SCRIPT** 是最深的绑定 — 一次 Lua 调用操作 3 个 ZSET (canvas + lane + global)，这在其他 KV 存储中无法原子实现。

### 5.3 抽象层缺失分析

| 依赖 | 当前抽象层 | 理想抽象层 | 差距 |
|------|-----------|-----------|------|
| Redis | ❌ 直接注入 StringRedisTemplate | MessageQueue / DistributedLock / RateLimiter 接口 | 巨大 |
| RocketMQ | ❌ 直接使用 RocketMQ API | MessagePublisher / MessageConsumer 接口 | 大 |
| MySQL | ⚠️ MyBatis-Plus Mapper (部分抽象) | Repository 接口层 | 中 |
| Groovy | ❌ 直接 GroovyShell | ExpressionEngine 接口 | 大 |
| React Flow | ❌ 直接使用 API | GraphEditor 抽象 | 巨大 |

### 5.4 建议

1. **P1**: 引入 MQ 抽象接口 — `MessagePublisher` / `MessageConsumer`，RocketMQ 为默认实现
2. **P1**: 引入分布式锁抽象 — `DistributedLock` 接口，Redis Lua 为默认实现
3. **P2**: Groovy → Aviator 迁移评估 — Aviator 无 Metaspace 泄漏、更安全、性能更好
4. **P2**: Redis Lua 脚本文档化 — 每个 Lua 脚本记录语义、输入、输出、替代方案
5. **P3**: 评估 DragonflyDB 兼容性 — Redis 协议兼容，可能零代码迁移

---

## 6. 知识管理/文档体系 (8%)

### 6.1 当前文档资产盘点

| 文档类型 | 数量 | 覆盖度 | 评级 |
|---------|------|--------|------|
| 架构决策记录 (ADR) | **0** | 0% | ❌ |
| 架构图 (UML/Mermaid/Drawio) | **0** | 0% | ❌ |
| 运维手册/Runbook | **0** | 0% | ❌ |
| 故障排查指南 | **0** | 0% | ❌ |
| 新人培训材料 | **0** | 0% | ❌ |
| API 文档 (Swagger 注解) | **0** (@Operation/@Tag/@Schema) | 0% | ❌ |
| OpenAPI Spec 文件 | **0** | 0% | ❌ |
| Postman/Insomnia 集合 | **0** | 0% | ❌ |
| 贡献指南 (CONTRIBUTING.md) | **0** | 0% | ❌ |
| 开发指南 (DEVELOPMENT.md) | **0** | 0% | ❌ |
| CLAUDE.md (AI 上下文) | 1 | 部分 | ⚠️ |
| 优化/审查文档 | 10+ | 深度 | ✅ |
| Flyway 迁移 (隐式文档) | 90 | 部分 | ⚠️ |

### 6.2 关键知识缺口

| 知识缺口 | 影响 | 紧急度 |
|---------|------|--------|
| **DagEngine 执行流程图** | 无人能快速理解 6 阶段管道 | P0 |
| **Handler 开发指南** | 新人不知道如何正确实现 Handler | P0 |
| **Redis Key 全景图** | 运维不知道哪些 Key 可以安全清理 | P1 |
| **API 契约文档** | 前后端联调靠口口相传 | P1 |
| **生产部署手册** | 不知道如何安全部署/回滚 | P1 |
| **故障排查决策树** | 生产事故时靠经验猜测 | P1 |
| **配置参数参考** | 不知道每个配置的含义和影响 | P2 |
| **数据模型 ER 图** | 不知道表间关系 | P2 |

### 6.3 建议

1. **P0**: 建立 ADR 仓库 — `docs/adr/` 目录，至少补写 5 个关键决策 (WebFlux 选型、DagEngine Reactor 模型、Disruptor 选型、Groovy 选型、单 MQ Topic 设计)
2. **P0**: 绘制 DagEngine 执行流程图 — Mermaid 格式，覆盖 6 阶段管道 + 4 种触发入口 + 暂停/恢复路径
3. **P1**: 编写 Handler 开发指南 — 接口契约 + 幂等要求 + boundedElastic 规则 + 测试模板
4. **P1**: 生成 OpenAPI Spec — springdoc 已配置，补全 @Operation/@Tag 注解
5. **P2**: 编写运维 Runbook — 部署/回滚/扩容/故障排查/Redis Key 清理

---

## 7. 可测试性架构 (15%)

### 7.1 后端测试能力评估

| 测试类型 | 当前状态 | 覆盖率 | 评级 |
|---------|---------|--------|------|
| 纯单元测试 (Mockito) | 112 文件 | ~36% Handler | ⚠️ |
| Spring Boot 集成测试 | **0** | 0% | ❌ |
| Testcontainers | **0** | 0% | ❌ |
| H2 内存 DB | **0** | 0% | ❌ |
| Embedded Redis | **0** | 0% | ❌ |
| Reactor 测试 (StepVerifier) | 11 处 | 极低 | ❌ |
| 端到端测试 | **0** | 0% | ❌ |
| 契约测试 (Pact) | **0** | 0% | ❌ |
| 变异测试 (PITest) | **0** | 0% | ❌ |
| 性能测试 (CI) | **0** (仅手动) | 0% | ❌ |
| test resources | **0 文件** | 0% | ❌ |
| application-test.yml | **不存在** | — | ❌ |

### 7.2 不可测试的代码模式

| 模式 | 示例 | 影响 | 修复难度 |
|------|------|------|---------|
| **上帝类** | DagEngine 1540 行 | 无法为单个阶段写测试 | 高 (需先拆分) |
| **静态时间依赖** | `LocalDateTime.now()` 散布 | 时间相关逻辑不可控 | 低 (注入 Clock) |
| **new() 内部创建** | `Executors.newVirtualThreadPerTaskExecutor()` | 无法 mock 线程池 | 中 (注入 ExecutorService) |
| **ThreadLocal/MDC** | 无 MDC 集成 | 测试中无法追踪上下文 | 中 |
| **fire-and-forget** | `.subscribe(null, err -> ...)` | 异步结果无法断言 | 中 (改为返回 Mono) |
| **Redis 阻塞调用** | StringRedisTemplate 直接注入 | 单元测试需 mock 全部 Redis | 低 (已有 mock) |

### 7.3 DagEngine 测试缺口 — 最关键

| 功能 | 测试状态 | 风险 |
|------|---------|------|
| Repeat CAS 机制 (400-498 行) | **零** | 并发 repeat 信号丢失 |
| Hub/LogicRelation 并发汇聚 | 顺序设置 (非并发) | CAS 竞态未测 |
| Timeout fallback | **零** | 超时降级路径未验证 |
| Kill switch 取消 | **零** | 取消后孤儿执行 |
| Context persistence round-trip | 纯 mock | Redis 序列化/反序列化未验证 |
| Circuit breaker OPEN→HALF_OPEN→CLOSED | mock 掉 | 真实状态转换未验证 |
| 虚拟线程 + Reactor 交互 | **零** | future.get() 阻塞事件循环 |

### 7.4 前端测试能力评估

| 测试类型 | 当前状态 | 覆盖率 | 评级 |
|---------|---------|--------|------|
| 纯函数测试 (vitest) | 30 文件 | ~25% 文件级 | ⚠️ |
| 组件渲染测试 (@testing-library) | **0** | 0% | ❌ |
| E2E 测试 (Cypress/Playwright) | **0** | 0% | ❌ |
| Mock Service Worker (MSW) | **0** | 0% | ❌ |
| 视觉回归测试 | **0** | 0% | ❌ |

**关键缺失**: undo/redo 无测试、CanvasNode 无渲染测试、保存流程无测试、config-panel 无交互测试。

### 7.5 建议

1. **P0**: 引入 Testcontainers — Redis + MySQL + RocketMQ 真实集成测试
2. **P0**: 创建 application-test.yml — 测试专用配置 (H2/Embedded Redis)
3. **P1**: DagEngine 拆分后逐模块测试 — 每个阶段独立可测
4. **P1**: 前端引入 @testing-library/react — 至少覆盖 canvas-editor 核心交互
5. **P2**: 前端引入 MSW — API mock 层，支持组件测试
6. **P2**: 引入 PITest 变异测试 — 验证单元测试质量 (非假阳性)

---

## 8. 性能容量模型 (12%)

### 8.1 当前容量配置

| 维度 | 配置值 | 理论上限 | 瓶颈点 |
|------|--------|---------|--------|
| 全局并发执行 | 3000 | 3000 | Redis ZSET CAS |
| Light 车道 | 600 并发 + 2000 队列 | 2600 | 信号量 |
| Standard 车道 | 1800 并发 + 10000 队列 | 11800 | 信号量 |
| Heavy 车道 | 300 并发 + 1000 队列 | 1300 | 信号量 |
| Retry 车道 | 300 并发 + 3000 队列 | 3300 | 信号量 |
| HikariCP 连接池 | 33 | 33 | **DB 连接** |
| Disruptor Ring Buffer | 65536 | 65536 事件 | tryNext() 满时抛异常 |
| WebClient 连接池 | 500 | 500 | 外部 HTTP 调用 |
| MQ 消费线程 | 20 | 20 | MQ 消费速率 |

### 8.2 瓶颈分析

**最可能的性能瓶颈排序**:

| # | 瓶颈 | 原因 | 影响面 | 扩容方式 |
|---|------|------|--------|---------|
| 1 | **HikariCP 33 连接** | 178 处 boundedElastic 共享 33 连接 | 全局 | 增大池 (受 MySQL 限制) |
| 2 | **Netty 事件循环阻塞** | 10+ Handler 直接阻塞事件循环 | 全局 | 修复 boundedElastic 包装 |
| 3 | **Redis 单节点** | 所有 ZSET + Lua + Bitmap 走单节点 | 全局 | Redis Cluster |
| 4 | **TraceWriteBuffer 丢弃** | 50K 上限，3000 QPS × 5 trace = 15K/s | 可观测性 | 增大缓冲或异步持久化 |
| 5 | **MQ 消费线程 20** | 20 线程处理所有 MQ 触发 | 触发入口 | 增大消费线程 |
| 6 | **Disruptor YieldingWait** | CPU 自旋等待 | CPU 浪费 | 改 BlockingWait |

### 8.3 缺失的 SLA 和容量规划

| 缺失项 | 影响 |
|--------|------|
| **无 SLA 定义** | 不知道延迟/吞吐/可用性目标 |
| **无容量规划模型** | 不知道多少资源支撑多少 QPS |
| **无扩容决策点** | 不知道什么指标达到多少时扩容 |
| **无性能基线** | 不知道当前性能是多少 |
| **无性能回归检测** | 代码变更可能静默降低性能 |
| **无负载测试 CI** | 性能退化不可及时发现 |

### 8.4 单实例容量估算

基于配置和代码分析：

| 指标 | 估算值 | 约束因素 |
|------|--------|---------|
| 最大触发 QPS | ~500-1000 | HikariCP 33 连接 + Redis 延迟 |
| 最大并发执行 | 3000 | 配置上限 |
| 单执行平均延迟 | ~50-200ms | 节点数 × Handler 延迟 |
| P99 延迟 (简单画布) | ~500ms | Reactor 调度 + Redis + DB |
| P99 延迟 (复杂画布 20 节点) | ~2-5s | 串行节点 + 外部 API 调用 |
| 内存占用 (3000 并发) | ~9-12 GB | ExecutionContext + 线程栈 |

### 8.5 建议

1. **P0**: 定义 SLA — 触发延迟 P99 < 1s，执行成功率 > 99.5%，可用性 > 99.9%
2. **P0**: 建立性能基线 — 用 tools/perf/ 跑一次完整压测，记录基线数据
3. **P1**: HikariCP 池监控 — `HikariDataSource.getHikariPoolMXBean()` 指标注册
4. **P1**: 容量规划文档 — QPS → 资源映射 (1K QPS = ? 实例 + ? Redis + ? MySQL)
5. **P2**: CI 性能回归 — 每次合并到 main 跑一次基准压测对比

---

## 9. AI 辅助开发适配 (35%)

### 9.1 AI 友好度评估

| 代码区域 | AI 可独立完成 | AI 需辅助 | AI 不可靠 | 原因 |
|---------|-------------|-----------|----------|------|
| 新增 NodeHandler | ✅ | — | — | 注解驱动 + 注册表，模式清晰 |
| Flyway 迁移 | ✅ | — | — | 命名规范 + 模式清晰 |
| 新增前端页面 | — | ✅ | — | 模式清晰但无规范文档 |
| 修改 Service 层 | — | ✅ | — | 需理解 Reactor 链 |
| 修改 DagEngine | — | — | ✅ | 1540 行超出上下文窗口 |
| 修改 canvas-editor | — | — | ✅ | 2084 行 + 25 useState |
| 修改 TieredCacheImpl | — | — | ✅ | 1556 行 + 复杂缓存一致性 |
| 修改 ExecutionContext | — | ✅ | — | 需理解线程安全约束 |
| 修复并发 Bug | — | — | ✅ | CAS 竞态需深度推理 |

### 9.2 上下文窗口限制

| 文件 | 行数 | 典型 AI 上下文窗口 | 可完整加载? |
|------|------|-------------------|-----------|
| DagEngine.java | 1540 | 200-400 行 (对话中) | ❌ 需分段 |
| CanvasExecutionService.java | 1407 | 200-400 行 | ❌ 需分段 |
| TieredCacheImpl.java | 1556 | 200-400 行 | ❌ 需分段 |
| canvas-editor/index.tsx | 2084 | 200-400 行 | ❌ 需分段 |
| config-panel/index.tsx | 1414 | 200-400 行 | ❌ 需分段 |

**5 个核心文件超出 AI 上下文窗口**。AI 修改这些文件时只能看到局部，容易遗漏跨区域依赖。

### 9.3 AI 易引入的 Bug 模式

| 模式 | 场景 | 后果 | 防护 |
|------|------|------|------|
| 遗漏 boundedElastic | 新增 Handler 中的 DB 调用 | 阻塞 Netty 事件循环 | 代码审查规则 |
| 遗漏 NodeHandlerType 注解 | 新增 Handler | 运行时 Handler 未注册 | 启动时断言 |
| 遗漏 cleanRefs 字段 | 新增节点类型 | 删除节点后引用残留 | 自动化检测 |
| Reactor 链断裂 | 修改 Mono 链 | 错误静默吞没 | StepVerifier 测试 |
| 忘记 Redis Key 前缀 | 新增 Redis 操作 | Key 冲突 | RedisKeyUtil 强制使用 |

### 9.4 建议

1. **P1**: 拆分 5 个大文件 — 每个不超过 500 行，AI 可完整加载
2. **P1**: 编写 AI 编码指南 — Handler 开发 checklist + boundedElastic 规则 + Reactor 规则
3. **P2**: 新增 Handler 脚手架 — AI 可用模板生成，减少遗漏
4. **P2**: 启动时 Handler 注册校验 — 检查所有 @NodeHandlerType 注解是否在 Registry 中

---

## 10. 合规认证就绪度 (10%)

### 10.1 认证路径分析

| 认证/合规 | 当前就绪度 | 主要缺口 | 改造估算 |
|----------|-----------|---------|---------|
| **ISO 27001** | 5% | 无 ISMS、无风险评估、无资产清单、无访问控制策略 | 6-12 月 |
| **SOC 2 Type II** | 10% | 无审计日志代码、无变更管理、无事件响应流程 | 6-9 月 |
| **GDPR** | 15% | 无删除权实现、无 DPO、无 DPIA、无跨境传输评估 | 3-6 月 |
| **个保法 (PIPL)** | 15% | 同 GDPR + 无个人信息分类分级 + 无同意管理 | 3-6 月 |
| **PCI DSS** | 5% | 明文密码、无加密传输、无日志脱敏 | 6-12 月 |

### 10.2 关键合规缺口

| # | 缺口 | 涉及认证 | 当前状态 | 修复难度 |
|---|------|---------|---------|---------|
| 1 | **审计日志表存在但无代码写入** | ISO/SOC/GDPR | `canvas_audit_log` 表空 | 中 |
| 2 | **无数据删除权实现** | GDPR/PIPL | 硬删除 + 无级联清理 | 高 |
| 3 | **PII 明文存储** (userId 日志 + 密码) | GDPR/PIPL/PCI | DataMaskingUtil 存在但未调用 | 中 |
| 4 | **无同意管理** (Consent) | GDPR/PIPL | SuppressionCheck 存在但不完整 | 中 |
| 5 | **无数据保留策略** | ISO/SOC/GDPR | 数据无限增长 | 中 |
| 6 | **无数据分类分级** | PIPL/ISO | 无标准 | 低 (文档) |
| 7 | **无变更管理流程** | ISO/SOC | 无审批流 | 中 |
| 8 | **无安全事件响应流程** | ISO/SOC | 无文档 | 低 (文档) |
| 9 | **CORS * + CSRF 禁用** | PCI | 安全漏洞 | 低 (配置) |
| 10 | **无渗透测试记录** | ISO/SOC/PCI | 无 | 中 |

### 10.3 数据跨境传输

| 数据 | 当前存储 | 跨境风险 | 合规要求 |
|------|---------|---------|---------|
| 用户画像 (userId, tags) | MySQL 本地 | 如部署在海外 → PIPL 要求 | 数据本地化 + 安全评估 |
| 执行轨迹 | MySQL 本地 | 同上 | 同上 |
| 事件数据 | MySQL 本地 | 同上 | 同上 |
| Redis 缓存 | 本地 | 同上 | 同上 |

**如果服务部署在中国大陆以外**: PIPL 要求个人信息出境需通过安全评估。

### 10.4 建议

1. **P0**: 启用审计日志写入 — 所有 CanvasService/CanvasOpsService 操作写入 canvas_audit_log
2. **P0**: 实现 GDPR 删除权 — 按用户 ID 级联删除所有关联数据
3. **P1**: PII 脱敏 — 日志中 userId 使用 DataMaskingUtil，密码使用 Jasypt 加密
4. **P1**: 数据保留策略 — execution 30 天, trace 7 天, audit_log 1 年
5. **P2**: ISO 27001 差距分析 — 正式评估，制定改进计划
6. **P2**: 同意管理完善 — SuppressionCheck 扩展为完整 Consent Framework

---

## 综合建议优先级矩阵

| 优先级 | 维度 | 关键行动 | 预估工期 | 人力 |
|--------|------|---------|---------|------|
| **P0** | 灾难恢复 | ExecutionContext 双写 Redis+MySQL | 1 周 | 2 人 |
| **P0** | 灾难恢复 | 优雅关停 + @PreDestroy | 3 天 | 1 人 |
| **P0** | 成本架构 | 数据保留策略 + V6 分区实现 | 1 周 | 2 人 |
| **P0** | 知识管理 | ADR 仓库 + DagEngine 流程图 | 3 天 | 1 人 |
| **P0** | 可测试性 | Testcontainers + application-test.yml | 1 周 | 2 人 |
| **P0** | 合规 | 审计日志启用 + GDPR 删除权 | 2 周 | 2 人 |
| **P1** | 演进路线 | Phase 0 技术预研 | 1 周 | 2 人 |
| **P1** | 团队适配 | DagEngine 拆分 + 核心流程文档 | 2 周 | 2 人 |
| **P1** | 性能容量 | SLA 定义 + 性能基线 | 1 周 | 2 人 |
| **P1** | 供应商锁定 | MQ/锁抽象接口 | 2 周 | 2 人 |
| **P1** | 知识管理 | Handler 开发指南 + OpenAPI Spec | 1 周 | 1 人 |
| **P2** | 成本架构 | 成本模型文档 + 按画布计费指标 | 1 周 | 1 人 |
| **P2** | 供应商锁定 | Groovy→Aviator 评估 | 1 周 | 1 人 |
| **P2** | 可测试性 | 前端 @testing-library + MSW | 2 周 | 2 人 |
| **P2** | AI 适配 | 大文件拆分 + AI 编码指南 | 2 周 | 2 人 |
| **P3** | 合规 | ISO 27001 差距分析 | 2 周 | 1 人 |
| **P3** | 灾难恢复 | 跨机房灾备方案 | 4 周 | 3 人 |

**P0 总计: ~7.5 周 × 2 人** | **P1 总计: ~7 周 × 2 人** | **P2+P3: ~12 周 × 2 人**

---

## 与已有文档的补充关系

| 本报告新增 | 已有文档覆盖 |
|-----------|------------|
| 成本模型 (Redis/MySQL 存储估算) | 无 |
| RTO/RPO 定义 + Redis 单点分析 | Kill Switch (部分) |
| 迁移回滚策略 + 增量路径 | 迁移路线图 (有方向无细节) |
| 巴士因子 + 认知负荷分析 | 上帝类提及 (无深度) |
| 供应商锁定量化 + Lua 脚本清单 | 技术选型对比 (部分) |
| ADR 缺失 + 文档资产盘点 | 无 |
| 测试能力矩阵 + DagEngine 测试缺口 | 测试覆盖率 (表层) |
| SLA 定义 + 瓶颈排序 + 容量估算 | 车道配置 (无分析) |
| AI 上下文窗口限制 + 易错模式 | AI 适配评分 (表层) |
| ISO/SOC/GDPR 认证路径 | 安全漏洞 (部分) |
