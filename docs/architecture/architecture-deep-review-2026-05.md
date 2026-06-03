# 架构深度审查报告

> 基于 2026-05-31 全量代码深度探索，补充 tech-selection-whitepaper.md 之外的代码级发现。
> 已有文档覆盖：选型对比、迁移路线图。本文聚焦：代码实证、数据实证、前端+基础设施补充。

---

## 1. 代码实证：选型问题的精确量化

### 1.1 WebFlux + MyBatis-Plus 的真实规模

| 指标 | 之前估计 | 实际审计 |
|------|---------|---------|
| `boundedElastic()` 调用次数 | 37 处 | **178 处** |
| Controller 文件数 | — | **29 个**，全部返回 `Mono<R<...>>` |
| 非 Mono Controller 端点 | — | **0 个** |

178 处 `boundedElastic()` 说明阻塞-桥接是**系统性模式**，不是局部问题。每个 DB 访问的 Controller、Service、甚至 JwtAuthFilter 都需要桥接。

### 1.2 DagEngine 的真实复杂度

| 指标 | 数值 |
|------|------|
| DagEngine.java 总行数 | **1540 行** |
| 节点执行管道阶段数 | **6 阶段**（resolveConfig → specialHandling → idempotentCheck → casLock → executeHandler → triggerDownstream） |
| Reactor 嵌套深度 | **3-4 层**（executeNode → Mono.defer → executeHandlerWithRepeat → flatMap → triggerDownstream → Flux.fromIterable.flatMap(executeNode)） |
| repeat 机制行数 | **90 行**（413-501），纯粹绕过 Reactor flatMap 不支持"重新执行" |
| MAX_NODE_DEPTH | **200**（防止递归 Mono 链爆 Reactor 内部栈） |
| NodeGate + Hub 超时 | **代码承认**："timeouts are not optional safety nets but necessary for correctness"（608-624 行） |

### 1.3 Disruptor 的真实使用模式

| 指标 | 数值 |
|------|------|
| Ring buffer 大小 | **65536**（2^16，约 16MB） |
| WaitStrategy | **YieldingWaitStrategy**（自旋，CPU 浪费） |
| 消费者数 | 默认 CPU 核数 |
| 背压机制 | `tryNext()` 非阻塞，满时抛 `InsufficientCapacityException` |
| fire-and-forget | `handleCanvasEvent` 第 132 行 `.subscribe(null, e -> ...)` |

### 1.4 Groovy 沙箱的真实配置

| 指标 | 数值 |
|------|------|
| Shell 池大小 | **CPU 核数 × 2** |
| 脚本缓存 | Caffeine max 500，key = `{canvasId}:{nodeId}:{SHA-256[:16]}` |
| 缓存值 | `Class<?>`（线程安全），每次执行 `newInstance()` |
| 评估方法 | `evaluateExpression()` 每次创建新 `GroovyShell`（绕过池） |
| 超时实现 | `future.get(timeoutMs, MILLISECONDS)` + `cancel(true)` |

---

## 2. CRITICAL 设计缺陷的代码级证据

### 2.1 DAG 执行状态无增量持久化

**ContextPersistenceService.java** 分析：

| 方法 | 调用时机 | 持久化内容 |
|------|---------|-----------|
| `save()` | 节点暂停(WAIT/HUB/GOAL_CHECK)、执行错误、超时 | 全量 ExecutionContext JSON |
| `delete()` | 执行完成/失败/超时终态 | 删除 Redis key |

**未持久化的关键状态**：
- `NodeGate.executing`（AtomicBoolean，@JsonIgnore）
- `Mono.delay()` 超时定时器
- `scheduledHubTimeouts` 集合
- 节点间中间输出（仅在暂停/完成时才 save）

**恢复锁问题**：
- `acquireResumeLock()` TTL = `globalTimeoutSec`（600s），Wait 节点可能等数小时/数天
- `releaseResumeLock()` 有 Lua 脚本保证原子性，但**有 fallback 到 plain DEL**（139 行），Redis 错误时可能删别人的锁

### 2.2 受众用户全量加载

**AudienceUserResolver.java**：
- `toUid()` 通过 JDBC 查询将所有匹配用户 ID 加载进 `ArrayList<String>`
- JDBC 路径（41 行）：每次创建新 DataSource，`fetchSize=1000`
- CDP 路径：同样返回 `List<String>`
- **无分页、无流式处理**

**AudienceBitmapStore.java**：
- `isMember()`（78 行）：每次从 Redis 加载**整个 bitmap** 到内存再检查一个 bit
- 大人群（百万级）= 每次检查加载几 MB 数据

### 2.3 Wait 节点跨部署问题

**WaitSubscriptionService.java**：
- 三种 wait 类型：UNTIL_EVENT、GOAL_CHECK、time-based
- `findActiveEventWaits()` 有 **LIMIT 100 上限**（158 行）
- CAS 完成用 `WHERE status='ACTIVE'` 防并发

**WaitResumeService.java**：
- 文档承认的 bug（42-49 行）：WAIT 恢复走完整 trigger 路径（含额度检查），导致：
  - cooldown 可拒绝合法恢复
  - per-user total limit 双重计数
- 超时恢复：`resumeDueWaits()` 扫描过期 ACTIVE 记录
- 但超时定时器本身是内存 `Mono.delay()`，部署后丢失

### 2.4 熔断器 JVM 本地

**CircuitBreakerRegistry.java**（167 行）：
- 状态机：CLOSED → OPEN → HALF_OPEN → CLOSED
- `checkState()` 非原子：多线程可能同时从 OPEN → HALF_OPEN，超过 `halfOpenAttempts`
- 每个节点类型独立熔断实例，存 `ConcurrentHashMap`
- **多实例部署下熔断形同虚设**

### 2.5 Handler 幂等问题

**ReachDeliveryService.java**：
- `prepareRecord()`（70 行）：SELECT + INSERT 无分布式锁
- 并发重试时，两个线程可能都发现无记录，都插入 PENDING 行
- 依赖外部平台的幂等检查作为最后防线

### 2.6 触发器版本未锁定

- 触发时从 Caffeine L1 缓存获取画布图结构
- 版本解析和图加载之间**无锁**
- 画布编辑/重新发布可能导致版本切换
- Wait 恢复时如原版本被删除，缓存返回 null → NPE

---

## 3. 前端架构深度发现

### 3.1 Canvas Editor 组件精确分析

**文件**：`frontend/src/pages/canvas-editor/index.tsx`（2085 行）

| 指标 | 数值 |
|------|------|
| useState hooks | **~25 个** |
| useRef | 6 个 |
| Form.useWatch | 7 个 |
| 外部状态管理 | **无**（Zustand/Jotai/Redux 均无） |

**关键函数**：
- `cleanRefs()`（163-200 行）：**手动列出 22 个 nodeId 字段**清理删除节点引用——新增节点类型时极易遗漏
- `buildDefaultBizConfig()`（452-497 行）：为每种节点类型硬编码默认值
- `validateBeforePublish()`（988-1066 行）：客户端发布前校验

### 3.2 边数据双源真相机制

**outletRouting.ts**（327 行）：
- `deriveEdges()`（213-268 行）：从 bizConfig 派生 React Flow 边
- `patchBizConfig()`（271-293 行）：用户画线时写回 bizConfig
- `clearEdgeRef()`（296-326 行）：删边时清除 bizConfig 引用

**动态 Outlet 模式**（104-155 行）：`branch-N`、`priority-N`、`group-KEY`、`path-KEY`、`variant-KEY`、`band-KEY`

**固定映射**（11-36 行）：24 个 `FIELD_HANDLES` handle-to-field 映射

### 3.3 保存流程 while(true) 问题

**canvas-editor/index.tsx:1084-1128**：
- 保存完成后检查 `latestSaveSnapshotRef`，如果期间快照变化则重新保存
- `while(true)` 循环无最大迭代次数
- 自动保存失败仅 `message.error`，不重试
- 409 冲突处理：弹窗让用户选择强制刷新（丢失本地编辑）

### 3.4 API 层精确问题

**services/api.ts**（463 行）：

| 特性 | 状态 |
|------|------|
| 请求拦截器 | 有（JWT 注入，23-28 行） |
| 响应拦截器 | 有（解包 R<T>，401 处理，31-44 行） |
| AbortController | **无** |
| 请求去重 | **无** |
| 重试逻辑 | **无** |
| `any` 类型 | `apiDefinitionApi`/`abExperimentApi`/`tagDefinitionApi` 用 `any`（368-411 行） |

### 3.5 Config Panel 模块级缓存问题

**config-panel/index.tsx**（1415 行）：
- `schemaCache`（Map，64 行）、`rawCache`（Map，66 行）、`systemOptionCache`（Map，68 行）——有 Map 但无 TTL/淘汰
- `contextFieldsCache`（70 行）、`canvasListCache`（1152 行）——**let 变量，永远不失效**

### 3.6 类型安全精确问题

**types/canvas.ts**（164 行）：
- `Branch`：`[k: string]: unknown`（44 行）
- `Priority`：`[k: string]: unknown`（56 行）
- `AbGroup`：`[k: string]: unknown`（68 行）
- `BizConfig`：22 个可选 `*NodeId` 字段 + `[key: string]: unknown`（128 行）
- `CanvasNodeData`：`[key: string]: unknown`（153 行）

**types/index.ts**：
- `BackendNode.config`：`Record<string, unknown>`（27 行）
- 无运行时校验库（Zod/Yup/io-ts 均无）

---

## 4. 基础设施精确发现

### 4.1 Docker 配置

**docker-compose.local.yml**（仅本地开发，无生产 compose）：

| 问题 | 详情 |
|------|------|
| MySQL 凭证 | root/root 硬编码 |
| Redis | 无密码 |
| 容器资源限制 | 无（memory/CPU） |
| Broker heap | 512m（生产不够） |
| rocketmq-dashboard | `:latest` tag（非确定性构建） |

**Dockerfile**：多阶段构建，`eclipse-temurin:21-jre-alpine`，ZGC + Generational，75% MaxRAMPercentage，HEALTHCHECK 每 30s。

### 4.2 HikariCP 连接池

```yaml
hikari:
  maximum-pool-size: 33  # 非常规数字
  minimum-idle: 8
  connection-timeout: 3000
  validation-timeout: 1000
  idle-timeout: 600000
  max-lifetime: 1800000
  keepalive-time: 60000   # 60s，较激进
```

33 是非常规数字，可能为特定部署拓扑计算。

### 4.3 线程池配置

| 线程池 | 大小 | 可配置 |
|--------|------|--------|
| canvas-scheduler | **4**（硬编码） | 否 |
| MqTriggerConsumer | 20 | 是（application.yml） |
| OverflowRetryConsumer | 5 | 是 |
| CacheInvalidationConsumer | 2 | 是 |
| Disruptor WorkerPool | CPU 核数 | 是 |
| WebClient connection pool | 500 | 是 |

**注意**：有两个 TaskScheduler bean 定义（SchedulerConfig + ScheduleRegistrarConfig），`@ConditionalOnMissingBean` 应该防冲突，但存在意味着合并痕迹。

### 4.4 安全配置问题

| 问题 | 位置 | 影响 |
|------|------|------|
| CORS `*` + credentials | application.yml:57-58 | 生产风险（代码注释已警告） |
| `/ops/**` 公开 | SecurityConfig:70 | 运维端点无认证 |
| health show-details: always | application.yml | 暴露 DB/Redis/MQ 状态 |
| 事件报告密钥硬编码 | application.yml | `canvas-event-report-secret-2026!!` |
| Redis 无密码 | application.yml | 本地开发可，生产需配置 |
| Snowflake workerId 基于 IP | SnowflakeConfig:28 | 容器环境可能冲突 |
| releaseResumeLock fallback | ContextPersistenceService:139 | Redis 错误时破坏互斥 |

### 4.5 Flyway 迁移

- 90 个迁移文件（V1-V90）
- **V6 分区是 no-op**（`SELECT 1;`），Flyway 记录了 checksum 但无实际 DDL
- V88/V89 包含 `UPDATE` + `JSON_REMOVE`/`JSON_SET` 数据修复迁移，大表上可能慢且难回滚

---

## 5. Trace/Stats 系统问题

### 5.1 TraceWriteBuffer

| 指标 | 数值 |
|------|------|
| 缓冲区容量 | 50,000 条 |
| 刷新周期 | 500ms |
| 每周期处理上限 | 4,000 条（20 批 × 200） |
| 写入失败策略 | 仅 log，不重试 |
| 调度机制 | Spring `@Scheduled`（共享单线程调度器） |

**生产现实**：3000 并发 × 5 条 trace/执行 = 15,000 条/秒。缓冲区秒级填满，大多数 trace 被静默丢弃。

### 5.2 CanvasStatsController

- `stats()`（117-152 行）：`executionMapper.selectList()` 全量加载，然后 Java Stream `.filter().count()`
- `trend()`（175-209 行）：同样全量加载后 Java GROUP BY
- `HomeOverviewController.buildOverview()`：一次加载日期范围内所有执行记录到内存
- O(n) 内存复杂度，10 万条记录时已慢，100 万条时 OOM

---

## 6. 缓存系统补充

### 6.1 TieredCacheImpl 关键方法

**safeWrite()**（456 行起）：双删模式
1. 失效 L1 本地
2. 删除 L2 Redis
3. 发布失效广播 + version bump
4. 执行写操作（通常是 DB 写）
5. Thread.sleep(delayMs)（阻塞调用线程！）
6. 再次失效 L1
7. 再次删除 L2
8. 再次发布失效广播

**waitAndRetryL2()**（789 行）：
- `Thread.sleep(50)` 循环轮询 Redis，最多 10 次
- 高并发下多线程可能堆积在此轮询中

**l1Versions map**（117 行）：
- 增长上限 = `l1MaxSize * 2`，超过时 cleanupDetachedLocalVersions()

---

## 7. 与已有文档的补充关系

| 本报告新增 | 已有文档覆盖 |
|-----------|------------|
| 178 处 boundedElastic（vs 37 处估计） | 选型对比 |
| DagEngine 6 阶段管道 + 3-4 层嵌套 | 选型合理性 |
| Wait 节点 LIMIT 100 上限 | 设计缺陷描述 |
| 熔断器 checkState 非原子 | 多实例行为不一致 |
| 前端 25 useState + 22 nodeId 字段 | 前端问题概述 |
| API 层 368-411 行 `any` 类型 | 类型安全概述 |
| HikariCP 33 连接 + keepalive 60s | — |
| Snowflake workerId 基于 IP | — |
| V88/V89 数据修复迁移风险 | V6 no-op |
| TraceWriteBuffer 吞吐量化分析 | Trace 缓冲区溢出 |

---

## 8. Handler 层深度分析

### 8.1 Handler 生态全景

**总计 66 Java 文件**（64 handlers + 2 utilities + 1 abstract base + 1 helper + 1 enum）

| 类别 | 数量 | 代表性 Handler |
|------|------|----------------|
| Trigger | 7 | API_TRIGGER, MQ_TRIGGER, SCHEDULED_TRIGGER, EVENT_TRIGGER, AUDIENCE_TRIGGER, CANVAS_TRIGGER, START |
| Condition/Evaluation | 10 | IF_CONDITION, LOGIC_RELATION, SCORING, THRESHOLD, SUPPRESSION_CHECK, FREQUENCY_CAP, CHANNEL_AVAILABILITY, TAGGER |
| Action/Data | 17 | API_CALL, UPDATE_PROFILE, TAG_OPERATION, TRACK_EVENT, POINTS_OPERATION, COUPON, COMMIT_ACTION, GROOVY, AGGREGATE |
| Delivery/Messaging | 8 | SendEmail, SendPush, SendSms, SendWechat, SendInApp, REACH_PLATFORM, SEND_MQ, IN_APP_NOTIFY |
| Flow-Control | 12+ | WAIT, HUB, AGGREGATE, GOTO, LOOP, MERGE, GROUP, AB_SPLIT, RANDOM_SPLIT, PRIORITY, DELAY, SUBFLOW |

### 8.2 有外部副作用的 Handler（资损风险点）

| Handler | 外部系统 | 副作用类型 | Idempotency |
|---------|---------|-----------|-------------|
| SendEmail/Push/Sms/Wechat/InApp | 触达平台 (via ReachDeliveryService) | 发送消息 | **有** (`executionId:nodeId:channel`) |
| REACH_PLATFORM | 触达平台 HTTP `/send` | 发送消息 | **无** |
| COUPON | 优惠券服务 `/issue` | 发券 | **有** (`executionId:nodeId`) |
| POINTS_OPERATION | MySQL (CustomerPointsLedgerMapper) | 积分记账 | **有** (DB 查重) |
| CDP_TAG_WRITE | CDP Tag Service | 写标签 | **有** (`sourceRefId:...:userId:tagCode`) |
| SEND_MQ | RocketMQ `CANVAS_MQ_TRIGGER` | 发 MQ 消息 | **无** |
| API_CALL | 任意外部 HTTP API | 调外部接口 | **无** |
| TRACK_EVENT | MySQL (EventLogMapper) | 插事件日志 | **无** |
| UPDATE_PROFILE | MySQL (CustomerProfileMapper) | 读+写用户画像 | **无** |
| TAG_OPERATION | MySQL (CustomerTagMapper) | 加/删标签 | **upsert 模式** |
| CREATE_TASK | MySQL (CustomerTaskRecordMapper) | 创建 CRM 任务 | **无** |
| MANUAL_APPROVAL | MySQL + NotificationService | 创建审批+通知 | **无** |
| TRANSFER_JOURNEY | CanvasExecutionService | 触发新画布执行 | **无** |

**关键发现**：13 个有副作用的 Handler 中，仅 4 个实现了显式幂等。其余 9 个**依赖引擎级执行去重**——但引擎级去重仅在入口保证不重复触发同一 (canvasId, userId, msgId)，不保证 Handler 内部操作不重复执行。

### 8.3 阻塞调用在 Netty 事件循环上——无 boundedElastic 保护

| Handler | 阻塞操作 | 文件:行 |
|---------|---------|---------|
| PointsOperationHandler | `ledgerMapper.selectOne()` + `ledgerMapper.insert()` | :54, :75 |
| UpdateProfileHandler | `profileMapper.selectOne()` + `profileMapper.updateById()`/`insert()` | :62, :83-86 |
| TrackEventHandler | `eventLogMapper.insert()` | :71 |
| TagOperationHandler | `tagMapper.delete()` + `tagMapper.update()` + `tagMapper.insert()` | :64, :95-101 |
| CreateTaskHandler | `taskMapper.insert()` | :66 |
| GoalCheckHandler | `eventLogMapper.selectCount()` + `waitSubscriptionService.createGoalWait()` | :135, :171 |
| WaitHandler | `waitSubscriptionService.createTimeWait()`/`createEventWait()` | :244, :292 |
| ManualApprovalHandler | `approvalMapper.insert()` + `notificationEventService.approvalPending()` | :101, :111 |
| CdpTagWriteHandler | `tagService.setTag()`（外部 CDP HTTP 调用） | :63 |
| SuppressionCheckHandler | `policyService.consentAllowed()` + `policyService.suppressionAllowed()` | :56, :63 |
| ChannelAvailabilityHandler | `policyService.channelAvailable()` | :54 |
| FrequencyCapHandler | `policyService.consumeFrequency()` | :62 |

**10+ Handler 直接在 Netty 事件循环线程上执行阻塞 JDBC/HTTP 调用**。这是比 Controller 层 178 处 `boundedElastic` 更严重的风险——事件循环线程数固定（通常 = CPU 核数），任何一个被阻塞就减少一个处理能力。

### 8.4 Handler 测试覆盖率

**覆盖率 ~36%**（23/64 handler 文件有测试）

| 类别 | 有测试 | 无测试 |
|------|--------|--------|
| 副作用 Handler | 4 (Coupon, CdpTagWrite, SendMq, GoalCheck) | **9** (TrackEvent, UpdateProfile, TagOperation, Points, CreateTask, ManualApproval, TransferJourney, ReachPlatform, InAppNotify) |
| 流控 Handler | 3 (Hub via LogicRelation, Wait, DirectCall) | 8 (Goto, Loop, Merge, Group, Subflow, SubFlowRef, Priority, QuietHours) |
| 条件 Handler | 4 (IfCondition, LogicRelation, Tagger, Selector) | 5 (Scoring, Threshold, SuppressionCheck, FrequencyCap, ChannelAvailability) |

**最关键的缺失**：所有发送类 Handler（SendEmail/Push/Sms/Wechat）继承自 AbstractSendMessageHandler，但**无任何子类测试**。

---

## 9. 执行流程端到端追踪

### 9.1 四种触发入口路径

| 入口 | 代码路径 | 同步/异步 | 去重机制 |
|------|---------|----------|---------|
| MQ 触发 | MqTriggerConsumer → RequestService.enqueue → Disruptor → RequestExecutor → triggerInternal → DagEngine | 异步 | SHA-256 hash `{triggerType}:{canvasId}:{sourceMsgId}` insertIgnore |
| 定时触发 | SchedulerService → triggerForAllUsers (with jitter) → trigger → ... → DagEngine | 异步 | uuid-based msgId |
| 行为触发 | ExecutionController.behaviorTrigger → Disruptor.publish → triggerFromDisruptor → ... → DagEngine | 异步 | eventId-based msgId |
| Direct Call | ExecutionController.directCall → trigger → triggerInternal → DagEngine | **同步** | caller-supplied dedupKey |

### 9.2 ExecutionRequest 状态机

```
PENDING → RUNNING → SUCCEEDED
                → RETRY → PENDING (循环，最多 5 次)
                → FAILED
```

**关键机制**：
- `markRunning`：乐观锁 + stale window + runToken（防止僵尸 RUNNING 占位）
- `startHeartbeat`：每 60s `touchRunning`（Flux.interval），执行完成后 dispose
- `retryOrFail`：指数退避延迟计算，`nextRetryAt` 写入 DB，下次 dispatch 周期拾取

### 9.3 Lane 路由与溢出处理

**三层准入**：canvas ZSET → lane ZSET → global ZSET（Lua 脚本原子操作）

**溢出策略**：
- HIGH priority：**永不拒绝**，仅 warn
- NORMAL priority：发 RocketMQ `CANVAS_TRIGGER_OVERFLOW` delay queue，超过 `overflowMaxRetry` 写 DLQ
- LOW priority：**直接丢弃**

### 9.4 执行完成路径

```
DagEngine.execute() → doOnTerminate → writeSkippedNodesIfComplete
                     → doOnError → ctxStore.save(ctx)
                     → handleSuccess → isPaused?
                         YES → ctxStore.save, PAUSED stats, release locks
                         NO  → ctxStore.delete, SUCCESS stats, release locks
                     → handleError → isPaused?
                         YES → ctxStore.save, PAUSED stats
                         NO  → ctxStore.delete, FAILED stats
                     → doFinally → executionRegistry.deregister (release Redis ZSET slots)
```

### 9.5 暂停/恢复机制

**暂停触发**：WAIT → createSubscription → WAITING → PAUSED → ctxStore.save → 释放 resumeLock

**恢复路径**：
- **事件恢复**：WaitResumeService.resumeEventWaits → CAS complete → trigger(canvasId, userId, WAIT_RESUME, ...) → acquireResumeLock → load ctx from Redis → DagEngine.execute (isResume=true)
- **超时恢复**：ExecutionWatchdog 每 30s scan → findExpiredActiveWaits → trigger(WAIT_TIMEOUT/GOAL_CHECK_TIMEOUT)
- **Hub/LogicRelation/Aggregate 超时**：Mono.delay 定时器 → ctxStore.save → executeNode(timeoutTarget)
- **人工审批恢复**：HTTP approve/reject → CAS update → trigger(MANUAL_APPROVAL_RESUME)

**已知 Bug**：Wait 恢复走完整 trigger 路径（含额度检查），导致 cooldown 拒绝合法恢复 + quota 双重计数。

### 9.6 DLQ 流程

**进入 DLQ 的条件**：
1. Handler 重试耗尽 → DagEngine:457 `writeDlq`
2. Overflow chain retry 超限 → CanvasExecutionService:967
3. OverflowRetryConsumer 重试超限 → OverflowRetryConsumer:75

**恢复**：`POST /canvas/dlq/{id}/replay` → 重新触发完整 triggerInternal 链（不自动删除 DLQ 记录）

### 9.7 Kill Switch

**双模式**：
- FORCE：Redis Pub/Sub `canvas:kill:{canvasId}` → InFlightExecutionRegistry.cancelAll → dispose 所有 Reactor Subscription → release Redis ZSET slots
- GRACEFUL：仅设画布 status=KILLED，依赖 TriggerPreCheck 拒绝新触发，运行中执行自然完成

---

## 10. 前端组件架构精确数据

### 10.1 组件树规模

| 组件 | 行数 | 状态管理 | 关键问题 |
|------|------|---------|---------|
| canvas-editor/index.tsx | **2084** | 25 useState, 6 useRef, 7 Form.useWatch | 单体巨组件，无外部 Store |
| config-panel/index.tsx | **1414** | 模块级 Map 缓存 | 12+ 内联子组件，缓存无 TTL/淘汰 |
| NotificationBell.tsx | 344 | NotificationContext | WebSocket + HTTP polling fallback |
| CanvasNode.tsx | 217 | memo() | 性能优化有限 |

### 10.2 Undo/Redo 实现

**数据结构**：`Snapshot { nodes, edges, actionName }`，history stack max 50

**关键缺陷**：
- 快照是**浅拷贝** (`[...nodes]`)，节点对象仍是同一引用
- 修改 `node.data.bizConfig` 会**静默篡改历史快照**
- 无专门的 undo/redo 测试文件

### 10.3 WebSocket 通知

**事件类型**：SYNC（全量替换）、NOTIFICATION_CREATED（增量追加）、NOTIFICATION_UPDATED（原地更新）、PONG（心跳）

**重连机制**：指数退避 `min(30000, 1000 * 2^min(attempt, 5))`，30s HTTP polling fallback

**注意**：这是**唯一 WebSocket**，仅用于通知，**不用于画布编辑器实时协作**

### 10.4 Config Panel Schema 驱动渲染

**支持 20+ 字段类型**：select, number, toggle, radio, code-editor, datetime, cron, condition-rule-list, context-value-list, param-define-list, branch-list, broadcast-branch-list, ab-group-list, priority-list, key-value, canvas-select, node-select, api-input-params, delay-input, event-attr-preview, edge-hint

**可见性控制**：`evaluateVisible()` 仅支持 `==` 和 `!=`，不支持更复杂的条件表达式

**模块级缓存问题**：
- `schemaCache`/`rawCache`/`systemOptionCache`：Map 无 TTL/淘汰
- `contextFieldsCache`/`canvasListCache`：**let 变量，永不失效**

### 10.5 测试模式

**所有测试 = 纯函数测试**，无组件渲染测试（无 @testing-library/react）

| 覆盖区域 | 测试文件数 | 深度 |
|---------|-----------|------|
| Canvas branch handles | 1 | Good |
| Outlet schema | 1 | Moderate |
| Config panel | 6 | Moderate |
| Graph hydration | 1 | Good |
| Canvas editor 纯函数 | 7 | Moderate |
| Hooks | 2 | Moderate |
| Notifications | 2 | Moderate |

**关键缺失**：undo/redo 无测试、CanvasNode 组件无渲染测试、保存流程无测试

---

## 11. 响应式反模式精确审计

### 11.1 Mono.subscribe() 火后不理——7 处资源泄漏

| 位置 | 代码 | 后果 |
|------|------|------|
| KillSwitchSubscriber:73 | `container.receive(...).subscribe()` | 无法优雅关闭，Redis 重连可能重复订阅 |
| CanvasTriggerHandler:121 | `dagEngine.execute(childGraph, ...).subscribe()` | 子画布执行失败静默丢弃，父流程无感知 |
| TransferJourneyHandler:74 | `executionService.trigger(...).subscribe()` | 转旅程失败静默丢弃，用户留在旧旅程 |
| DagEngine:562 | `dlqWriter.write(...).subscribe(null, err -> log.error(...))` | 关停时 DLQ 写入半途废弃 |
| DagEngine:837,855 | `fallbackExecutor.execute(...).subscribe(null, ...)` | 超时降级执行半途废弃 |
| GroovyHandler:58 | `Executors.newVirtualThreadPerTaskExecutor()` 无 @PreDestroy | 关停时虚拟线程不被等待/中断 |

**CanvasTriggerHandler:121 的危害**：Kill Switch 强制取消画布时，子画布的 `.subscribe()` 独立于父 DAG 生命周期，**无法被取消**，产生孤儿执行。

### 11.2 响应式链错误静默吞没

| 位置 | 模式 | 后果 |
|------|------|------|
| CanvasExecutionRequestExecutor:286 | `Flux.interval(...).flatMap(...).onErrorResume(e -> Mono.empty()).subscribe()` | 心跳失败**完全静默**——无日志、无指标、无告警。DB 长时间故障导致所有 RUNNING 执行过期 |
| DagEngine:956 | `.onErrorResume(e -> { ... if (!shouldRethrow) return NodeResult.fail(...) })` | 基础设施异常（序列化失败、超时）被转为业务失败而非快速传播 |

### 11.3 Netty 事件循环上的阻塞调用——15+ 处

**除了 Handler 层的 10+ 处，还有更严重的系统性阻塞**：

| 服务 | 阻塞操作 | 影响面 |
|------|---------|--------|
| **InFlightExecutionRegistry** | 全部使用 `StringRedisTemplate`（blocking），`tryAcquire()`/`release()`/`getActiveCount()` | 每次触发执行都调用，**每次 DAG 执行至少 2 次 Redis 阻塞调用** |
| **TriggerPreCheckService** | 9+ 处 `StringRedisTemplate` 调用（get/setIfAbsent/ZSet add/score/rangeByScore） | 每次触发执行前的准入检查 |
| **AudienceBitmapStore** | `StringRedisTemplate` bitmap 操作 | 人群命中判断 |
| **CanvasExecutionReplayRateLimiter:198** | `redis.opsForValue().increment(key)` | DLQ 重放限流 |
| **NotificationRealtimeService:130** | `redis.convertAndSend(channel, payload)` | 通知发布 |
| CanvasTriggerHandler:90 | `canvasMapper.selectById()` | 子画布触发时查画布 |
| SubFlowRefHandler:105,116 | `canvasMapper.selectById()` + `canvasVersionMapper.selectById()` | 子流引用时查版本 |
| AudienceController:112,119 | `Mono.fromCallable(...)` **无** `subscribeOn(boundedElastic)` | API 层也漏了 |

**项目已配置 `ReactiveStringRedisTemplate`**（RedisConfig.java:23），但引擎服务**全部使用阻塞版 StringRedisTemplate**。这意味着 178 处 `boundedElastic` 之外的系统核心路径（准入、限流、配额）直接阻塞 Netty 线程。

### 11.4 虚拟线程 + Reactor 交互——2 处已知问题组合

| 位置 | 问题 |
|------|------|
| GroovyHandler:58,172-180 | `vte.submit(...)` + `future.get(timeoutMs)` —— **future.get() 阻塞 Netty 事件循环线程**。虚拟线程释放了载体线程，但 Netty 线程被阻塞等待。正确模式：`Mono.fromFuture(() -> vte.submit(...)).timeout(...)` |
| AudienceController:174-176 | `Thread.ofVirtual().start(...)` 从 Mono.fromRunnable 中启动 —— (1) 虚拟线程脱离 Reactor 生命周期，Mono 取消时虚拟线程继续运行；(2) 异常在虚拟线程上抛出，永远不进入 Mono error channel；(3) 无法追踪或取消 |

### 11.5 上下文传播断裂——系统性问题

**全项目无 MDC 集成**：没有 `Hooks.onEachOperator()`、没有 `contextCapture()`、没有 Reactor Context 到 MDC 的桥接。

后果：
- 每次线程切换（`subscribeOn`、`publishOn`）**丢失 MDC 上下文**
- 每个虚拟线程（GroovyHandler、AudienceController）**空 MDC**
- `boundedElastic()` 线程上的日志**无 traceId/spanId**
- DAG 执行的分布式追踪**不可能**

---

## 12. 并发与分布式系统 Bug 精确审计

### 12.1 CAS 竞态条件

**DagEngine.java 483-490 行——Repeat 信号丢失**：
```
Thread A: needsRepeat = repeatPending.getAndSet(false)  → false
Thread A: executing.set(false)                          → 释放锁
Thread B: CAS 成功，设置 repeatPending = true           → 新信号
Thread A: repeatPending.getAndSet(false)                → 清除了 Thread B 的信号！
```
后果：Thread B 的 repeat 请求丢失，DAG 执行可能不完整。

**DagEngine.java 593-597 行——LOGIC_RELATION 释放无 repeatPending 检查**：CAS 后无条件释放，忽略其他线程设置的 repeatPending 信号。

### 12.2 ConcurrentHashMap 复合操作非原子

| 位置 | 操作 | 竞态场景 |
|------|------|---------|
| InFlightExecutionRegistry:154-158 | `map.remove(id); if (map.isEmpty()) localRegistry.remove(canvasId)` | isEmpty=true 后、remove 前，新执行注册到同一 canvasId → 新条目被删 → 内存泄漏 |
| InFlightExecutionRegistry:175 | `localRegistry.remove(canvasId)` 然后 `map.forEach(swap::dispose)` | remove 后新注册的执行不会被 cancel |
| NotificationRealtimeService:179-191 | `sessions.remove(id); if (sessions.isEmpty()) sessionsByUser.remove(userId)` | 同 InFlightExecutionRegistry 模式 |
| ExecutionContext:128-144 | `nodeOutputs.put(nodeId, output); flatContext.putAll(output)` | 两步非原子——其他线程可见 nodeOutputs 但 flatContext 不完整 |

### 12.3 分布式锁缺陷

| 位置 | 锁机制 | 缺陷 |
|------|--------|------|
| CanvasRouteInitializer:70-118 | SETNX + UUID token，但 **plain DEL** 释放 | 锁过期后，其他实例获取锁，原实例 DEL 删了别人的锁 |
| AudienceBatchComputeService:87 | SETNX + 固定值 `"1"`，TTL 2 小时 | 无 Lua check-and-del，崩溃后锁 2 小时不释放，无法区分持有者 |
| ContextPersistenceService:132-140 | Lua check-and-del，但有 **fallback 到 plain DEL** | Redis 错误时破坏互斥 |
| TriggerRouteService:182-195 | SETNX + 30s TTL，**无续期** | 路由重建超 30s 锁过期，并发重建 |

### 12.4 双写断裂——6 处一致性风险

| 位置 | 操作 | 崩溃后果 |
|------|------|---------|
| CanvasService:227-238 | DB publish → Redis routes → Quartz schedule → cache invalidate | 画布已发布但无路由/无调度/缓存脏 |
| CanvasService:306-318 | DB offline → Redis cleanup → Quartz cleanup | 画布已下线但仍有路由/调度 |
| CanvasService:328-351 | DB archive → 同上 | 同上 |
| TriggerPreCheckService:197 | Redis quota INCR → async DB update | Redis 已扣配额但 DB last_trigger_at 未更新 → 冷启动后配额不一致 |
| EventDefinitionServiceImpl:183-191 | DB eventLog insert → waitResumeService.resumeWaits | 事件已记录但 Wait 节点不恢复 → DAG 卡 WAITING |
| CanvasExecutionService:1328 | DB insertExecution(RUNNING) → dagEngine.execute | 执行记录卡 RUNNING 永不更新 |

### 12.5 写后读过时

| 位置 | 问题 |
|------|------|
| TriggerPreCheckService:108-114 | 读取 `canvasMapper.selectLastTriggerAt()`，但由 `updateQuotaAsync()` 通过虚拟线程异步写入。高并发下读到过时值 → cooldown 检查失效 |
| CanvasExecutionManagementController:147-158 | `ctxStore.load()` → 修改 flatContext → `ctxStore.save()`。无版本控制，并发审批丢失更新 |

### 12.6 ExecutionContext 线程安全

| 字段 | 类型 | 问题 |
|------|------|------|
| `triggerPayload` | **HashMap**（非线程安全） | 并发节点访问可能 ConcurrentModificationException |
| `callStack` | **ArrayList**（非线程安全） | 并发节点修改栈可能异常 |
| `flatContext` | ConcurrentHashMap，但 `putAll()` **非原子** | 其他线程可见部分更新 |
| `approxSizeBytes` | AtomicLong，但 check-then-act **非原子** | 大小限制可被并发写突破 |
| `benefitGranted`/`userReached` | volatile boolean，复合操作 **非原子** | 两个线程都可能看到 false 然后都 grant |

### 12.7 非原子 Redis 操作

CanvasExecutionReplayRateLimiter:198-202：
```java
redis.opsForValue().increment(key);   // INCR
redis.expire(key, Duration.ofMinutes(5)); // EXPIRE
```
崩溃在两操作之间 → key 永不过期 → 永久限流。应使用 `INCR + EXPIRE` 原子脚本。

---

## 13. 数据模型与 API 设计问题

### 13.1 God Table——CanvasDO 承载 4 个职责域

| 域 | 字段 |
|----|------|
| 元数据 | id, name, description, status, createdBy, isExample, sourceTemplateKey |
| 执行约束 | validStart, validEnd, perUserTotalLimit, perUserDailyLimit, cooldownSeconds, maxTotalExecutions |
| 灰度发布 | canaryVersionId, canaryPercent, previousVersionId |
| 触发配置 | editVersion, triggerType, cronExpression |

建议拆分：`canvas` + `canvas_execution_policy` + `canvas_canary_config` + `canvas_trigger_config`（1:1 关系）。

### 13.2 缺失的复合索引

| 表 | 缺失索引 | 影响查询 |
|----|---------|---------|
| `canvas_execution` | `(canvas_id, status, created_at)` | Stats/Trend 端点全表扫描 |
| `canvas` | `(status)` 单列索引 | HomeOverview 加载全部画布后 Java 过滤 |
| `canvas_execution_trace` | `(execution_id, started_at)` | Trace 端点按时间排序 |
| `message_send_record` | 无任何复合索引 | 发送记录查询 |

V80-V90 迁移**零索引新增**。V78 的 `idx_execution_tenant_canvas_created` 以 tenant_id 开头，不带 tenant 上下文时无用。

### 13.3 JSON 反模式

`tenant.quota_json` 使用 MySQL JSON 列类型，但运行时**从未用 JSON 函数查询**——映射为 Java String，当普通文本用。JSON 类型徒增验证开销。应为 `VARCHAR(512)` 或提取结构化列。

### 13.4 API 设计不一致

**REST 违规——URL 中嵌入动词**（16 处）：
- `POST /canvas/{id}/publish`、`/offline`、`/archive`、`/kill`、`/clone`
- `POST /canvas/{id}/canary`、`/promote-canary`、`/rollback-canary`
- `POST /canvas/execution/{id}/approve`、`/reject`
- `POST /canvas/dlq/{id}/replay`
- `POST /canvas/execute/direct/{id}`
- `POST /canvas/trigger/behavior`

**分页不一致**：`AdminController.list` 返回 `R<List<SysUserDO>>` 无分页；`AudienceController.listReady` 同样无分页。

**DO 对象直接暴露为 Request Body**：EventDefinitionController.create 和 AudienceController.create 直接接受 DO 对象——暴露了 DB 结构。

**错误码不一致**：部分用 `"AUTH_003"`/`"CANVAS_010"`，部分用纯文本 `"画布不存在"`，部分 throw `IllegalArgumentException` 无错误码。

### 13.5 N+1 查询 / Java 内存聚合

| 位置 | 问题 | 修复 |
|------|------|------|
| HomeOverviewController:65-67 | `selectList()` 加载全部 canvas → Java `.filter(PUBLISHED)` | SQL `WHERE status = 'PUBLISHED'` |
| CanvasStatsController:127-150 | 全量加载 execution → Java `.stream().count()` | SQL `GROUP BY status` |
| CanvasStatsController:185-207 | 全量加载 execution → Java 按日期 group | SQL `GROUP BY DATE(created_at)` |
| DlqController:81,100 | 同一请求中 `selectById(id)` 两次 | 缓存第一次结果 |

### 13.6 硬删除 vs 软删除不一致

**全项目无 `@TableLogic` 注解**——所有删除都是硬删除（13 处）。

**风险**：EventDefinition、ApiDefinition、MqDefinition 硬删除后，已发布的画布引用它们时运行时崩溃。**删除前无引用完整性检查**。

唯一例外：CdpUserTagDO 用 `status = DELETED` 做隐式软删除。

### 13.7 金融级数据用 Integer

CustomerPointsLedgerDO.points 使用 `Integer`：
- 最大值 2,147,483,647，如积分=分的 100 倍，单笔上限约 21 万货币单位
- 不支持小数积分（如百分比累积）
- 金融账本应使用 `BigDecimal` + DB `DECIMAL`

### 13.8 审计列覆盖不一致

| 审计字段 | 覆盖率 | 关键缺失 |
|---------|--------|---------|
| `createdBy` | 18/42 (43%) | CanvasExecutionDO、CanvasExecutionRequestDO、MessageSendRecordDO |
| `updatedBy` | **1/42 (2%)** | 仅 TenantDO 有 |
| `updatedAt` | 29/42 (69%) | CanvasVersionDO、NotificationDO、CanvasManualApprovalDO 等 13 个缺失 |
| `createdAt` | 37/42 (88%) | AudienceStatDO、CanvasExecutionStatsDO 等 5 个缺失 |

**最关键**：CanvasExecutionDO 和 CanvasExecutionRequestDO 缺 `createdBy`，无法审计谁触发/修改了执行。

---

## 14. 安全漏洞精确审计

### 14.1 IDOR——无租户/所有权隔离（CRITICAL）

| Controller | 端点 | 问题 |
|-----------|------|------|
| CanvasController | getById, update, getVersions, diff, publish, offline, kill, clone | **零 tenantId/ownership 检查**，任何认证用户可读/改任意画布 |
| AudienceController | get, delete, stat | 同上，任何认证用户可操作任意人群定义 |
| ExecutionController | directCall, dryRun | 同上，任何认证用户可执行任意画布 |
| ExecutionController:66 | behaviorTrigger (`permitAll`) | **无认证** + 从 request body 接收 userId → 攻击者可冒充任意用户触发行为 |

**数据库层面**：`canvas`、`canvas_version`、`canvas_execution` 表**无 tenant_id 列**（V1 init schema 确认）。租户隔离从数据层到 API 层**完全缺失**。

**TenantContextResolver 仅在 AdminController/SystemOptionController 使用**——核心业务 Controller 不用。

### 14.2 CORS * + CSRF Disabled → 跨域请求伪造（CRITICAL）

```java
// WebConfig.java:37-45
config.addAllowedOriginPattern("*");
config.setAllowCredentials(true);

// SecurityConfig.java:44
.csrf(ServerHttpSecurity.CsrfSpec::disable);
```

**攻击场景**：
1. `evil.com` 页面用 JavaScript `POST /canvas/{id}/publish`，带受害者 localStorage 中的 JWT
2. CORS `*` 允许浏览器发送 Authorization header
3. CSRF 禁用 → 无 token 校验
4. 攻击者发布/修改/删除受害者画布

### 14.3 Mass Assignment——12+ 端点接受原始 DO 对象（HIGH）

| Controller | 端点 | 接受的 Entity |
|-----------|------|-------------|
| AudienceController:125,138 | create, update | AudienceDefinitionDO |
| EventDefinitionController:73,92 | create, update | EventDefinitionDO |
| IdentityTypeController:45,60 | create, update | IdentityTypeDO |
| DataSourceConfigController:90,108 | create, update | **DataSourceConfigDO**（含 JDBC URL → SSRF） |
| SystemOptionController:66 | update | SystemOptionDO |

**攻击场景**：POST 创建 AudienceDefinitionDO 带 `"id": 999` 或 `"tenantId": 1"`（其他租户），覆写/劫持记录。DataSourceConfigDO 的 JDBC URL 可指向内部服务 → SSRF。

### 14.4 Rate Limiting 完全缺失（HIGH）

| 端点 | 问题 |
|------|------|
| `/auth/login` | 5 次错误锁定账户，但**无端点级限流** → 攻击者可遍历所有用户名逐个锁定 → 批量账户 DoS |
| 所有 write 端点 | 无限流（画布创建、人群创建、执行触发等） |
| `/canvas/events/report` (permitAll) | HMAC 认证但无限流 → 共享密钥泄露后可洪水式事件注入 |
| `/canvas/execute/direct/*` (permitAll) | 无限流 → 执行洪水 |
| `/canvas/trigger/behavior` (permitAll) | 无限流 → 行为触发洪水 |

唯一存在的限流器：`CanvasExecutionReplayRateLimiter`（仅覆盖 DLQ 重放）。

### 14.5 Groovy 沙箱逃逸向量（MEDIUM）

**Object.wait() 绕过 Thread.sleep 阻塞**：
```groovy
(1 as Object).wait(600000)  // Object 不在 disallowedReceivers 列表
```
多个恶意脚本可耗尽虚拟线程池 → 拒绝服务。

**evaluateExpression() 绕过 Shell 池**（GroovyHandler:220-223）：
每次调用创建新 GroovyShell，可能未应用相同的 SecureASTCustomizer → 沙箱绕过。

### 14.6 SQL Injection——无风险

全项目 Mapper XML **零 `${}` 插值**，全部使用 `#{}` 参数化占位符。

### 14.7 JWT 安全评估

| 维度 | 评估 |
|------|------|
| Key length | **SAFE** — 32 byte minimum enforced (JwtUtil:49-53) |
| alg:none attack | **SAFE** — JJWT 0.12 锁定 algorithm to HMAC key type |
| Key rotation | **VULNERABLE** — 单一 secret，无多 key 支持，旋转需重启全部实例并强制所有用户重新登录 |
| Blacklist race | **LOW** — `redis.hasKey()` 是只读操作，但 logout 用非原子 `set()` |

### 14.8 WebSocket Ticket——基本安全

- TTL 60s + `getAndDelete()` 原子消费 → 单次使用
- Redis 故障时可能异常导致连接开放（minor）

### 14.9 Flyway Migration 安全

- 无审查强制——任何开发者添加 SQL 文件即自动执行
- `baseline-on-migrate: true` → 不验证现有 schema 是否匹配迁移脚本
- V1 schema 无 tenant_id → 后期添加需大规模数据迁移

---

## 15. 可观测性与运维能力缺项

### 15.1 Logging——MDC 字段声明但永不填充

logback-spring.xml 声明了 `traceId`、`canvasId`、`nodeId`、`userId` MDC 字段，但**全项目零 `MDC.put()` 调用**。

后果：生产事故中无法按 canvasId/traceId 过滤日志重建单条执行路径。

### 15.2 PII 明文日志

CanvasDisruptorService:140-141 和 CanvasExecutionService:584,908,967,1141,1250 等处直接 log userId。`DataMaskingUtil` 存在但**从未在任何日志语句中调用**。

### 15.3 MQ Consumer 日志洪水

MqTriggerConsumer:78 每条消息 INFO 级别日志。1000 msg/s → 86M 行/天。无限流/采样。

### 15.4 Metrics——高基数 canvasId 标签将炸 Prometheus

CanvasMetrics 7 个方法用 `canvasId` 作 Micrometer tag。10000 画布 × 每 15s scrape → O(10K × scrape_count) 时间序列 → Prometheus 内存爆。

### 15.5 缺失的基础设施指标

| 缺失指标 | 影响 |
|---------|------|
| HikariCP pool (active/idle/pending/timeout) | 连接池耗尽 → 静默 3s 超时无指标信号 |
| Redis Lettuce pool | 池饱和阻塞 Lua 脚本 |
| RocketMQ consumer lag | 背压不可见 |
| Disruptor ring buffer utilization | 仅计数 overflow，无当前填充率 |
| WebClient connection pool | 500 连接池无利用率 Gauge |
| TraceWriteBuffer backlog | `pendingCount()` 存在但**未注册为 Gauge** |
| Circuit breaker state | OPEN 状态不可见 |
| In-flight execution count | 容量压力不可见 |
| Thread pool metrics | Disruptor worker/scheduled pool 无指标 |
| GC/heap | 无 JVM 指标 |

### 15.6 Health Check——无自定义健康指示器

零 `HealthIndicator` 实现。仅 Spring Boot 自动配置的 DB/Redis ping。

**缺失的健康检查**：
- Disruptor ring buffer 填充率 > 90% = DOWN
- Circuit breaker OPEN = DOWN
- TraceWriteBuffer backlog > 80% = WARN
- In-flight execution 接近车道上限 = WARN
- RocketMQ consumer 运行状态

### 15.7 告警规则——零

全项目无任何 Prometheus alerting rule 文件。

**应存在的关键告警**：

| 告警 | 条件 | 严重度 |
|------|------|--------|
| DisruptorRingBufferNearFull | remaining < 10% | critical |
| ExecutionLaneExhausted | semaphore available = 0 for > 2m | warning |
| CircuitBreakerOpen | any breaker state = OPEN | warning |
| MqConsumerLag | offset lag > 10000 | warning |
| TraceBufferDropping | pending > 40000 | warning |
| HikariPoolExhaustion | active = max-pool for > 1m | critical |
| HighExecutionFailureRate | failure rate > 10% over 5m | warning |

### 15.8 Dashboard——零

无任何 Grafana dashboard JSON。

**最低需要 3 个仪表盘**：
1. Canvas Engine Overview（吞吐/失败率/车道利用率/Disruptor 填充率）
2. Infrastructure Health（HikariCP/Redis/MQ/WebClient/GC）
3. Trigger & Node Deep-Dive（每触发类型吞吐/每节点类型耗时/熔断器状态/DLQ 大小）

### 15.9 分布式追踪——零

pom.xml 无 Sleuth/Brave/OTel 依赖。全项目无 trace context 传播。

**追踪断裂边界**：

| 边界 | 发生什么 |
|------|---------|
| MQ → Disruptor | MqTriggerConsumer 不提取 trace headers |
| Disruptor → ExecutionService | Worker 线程无 producer 线程 context |
| ExecutionService → WebClient | HTTP 出站无 trace headers |
| Scheduled triggers | Scheduler 线程无 trace context |

### 15.10 错误分类——无 retryable/permanent 区分

ErrorCode 是纯字符串枚举，无 `boolean retryable()` 或 `Duration retryAfter()` 字段。重试机制无法区分：
- `QUOTA_003`(并发限) → **Transient**，可重试
- `AUTH_001`(未授权) → **Permanent**，重试无意义

R.fail() 总设 code = -1，ErrorCode 嵌入 message 字段前缀 → 客户端**无法程序化区分错误类型**。

### 15.11 GlobalExceptionHandler 泄露内部信息

GlobalExceptionHandler:84 的 catch-all 返回 `e.getMessage()` → 可能包含 stack trace、SQL 片段、内部类名。

### 15.12 配置验证——部分缺失

| 配置 | 当前验证 | 应添加 |
|------|---------|--------|
| Disruptor ring-buffer-size | **无**（非 2 的幂启动时 Cryptic 报错） | `Integer.bitCount() == 1` 校验 |
| ExecutionLaneProperties | lane > 0, 总量 <= global | 各 lane 不超 global、queue > 0 |
| TriggerPriorityConfig | **零** | lowRatio [0,1]、overflowMaxRetry [1,10] |
| WebClient connection pool | **零** | max-connections > 0 |

### 15.13 优雅关停——完全缺失

**缺失**：`server.shutdown: graceful` + `spring.lifecycle.timeout-per-shutdown-phase`

**关停序列无协调**：
1. CanvasExecutionService **无 @PreDestroy** → 继续接受触发
2. MqTriggerConsumer **无 @PreDestroy** → 继续消费消息
3. Disruptor.shutdown() 等待 WorkHandler 处理完，但 `.subscribe()` 异步链已脱离 → 执行仍在跑
4. InFlightExecutionRegistry **无 drain/await** → 执行中途被杀
5. Redis ZSET entries **无清理** → 依赖 TTL 过期

**Disruptor.shutdown() 不等异步链完成**：`handleCanvasEvent:132` 的 `.subscribe()` 是 fire-and-forget，shutdown 只等 WorkHandler 处理完事件对象，不等执行完成。

---

## 16. 测试与 CI/CD 缺项

### 16.1 后端测试结构

**112 test files**，全部纯单元测试（Mockito mocks）。**零 @SpringBootTest**。**零集成测试**。

`src/test/resources/` **完全空**——无 test YAML、无 seed SQL、无 fixture。

### 16.2 DagEngine 关键测试缺失

| 功能 | 测试状态 |
|------|---------|
| Repeat CAS 机制 (lines 400-498) | **零** — grep `repeatPending`/`NodeGate` 返回零结果 |
| Hub/LogicRelation 并发上游汇聚 | 顺序设置上游状态，**非并发** |
| Timeout fallback (handleSpecialNodeTimeout) | **零** |
| Kill switch cancellation | **零** — grep `KillSwitch` 返回零 |
| Context persistence/resume Redis round-trip | 纯 mock，**无真实 Redis 验证** |
| Circuit breaker OPEN 阻断 + HALF_OPEN 恢复 | **零** — 每个 test 都 mock(CircuitBreakerRegistry) |

### 16.3 集成测试基础设施

- **Testcontainers**：零
- **H2 内存 DB**：零
- **Embedded Redis**：零
- **application-test.yml**：不存在

docker-compose.local.yml + WireMock 仅用于手动本地测试，**无 CI 集成**。

### 16.4 前端测试

- **30 单元测试文件**（vitest），117 source files → ~25% 文件级覆盖率
- **零 E2E 测试**（无 Cypress/Playwright）
- **零组件渲染测试**（无 @testing-library/react）
- 所有测试 = 纯函数测试，不渲染组件

### 16.5 CI/CD Pipeline——零

- **无 .github/workflows/** 目录
- **无 Jenkinsfile / .gitlab-ci.yml**
- **Dockerfile 用 `-DskipTests` 打包** — Docker 构建明确跳过测试
- **无 JaCoCo / SonarQube / 代码覆盖率门槛**
- **无安全扫描**（Snyk/OWASP Dependency-Check）

### 16.6 性能测试——仅手动

`tools/perf/` 有完整性能测试套件（perf-runner, threshold-runner, verifier, capacity-report），但需要：
- 手动启动 backend + docker-compose
- 手动创建 PERF_RUN_ID
- 手动执行和验证

**无 CI 性能回归检测**。

### 16.7 Contract Testing——零

无 Pact/Spring Cloud Contract。前端 axios 调用后端 API 但**无契约验证**。API 变更可能静默破坏前端。

### 16.8 Mutation Testing——零

无 PITest。纯 mock 单元测试尤其容易产生**假阳性覆盖率**——调用 mock 达到高行覆盖率但不验证实际逻辑。
