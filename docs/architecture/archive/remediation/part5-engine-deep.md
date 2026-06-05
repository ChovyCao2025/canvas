# 架构整改方案 — engine-deep
> 详见 [README.md](./README.md) 获取完整索引


## 第五部分：深度结构设计问题（4大维度扫描）

> 以下问题来自对引擎核心、数据模型、API集成、前端架构四个维度的深度结构扫描。

---

### 问题二十一：DAG 引擎核心设计缺陷（19项）

#### 21.1 God Class — DagEngine（1539行，26+方法）

**文件**: `engine/scheduler/DagEngine.java`

DagEngine 混合了调度、分发、监控、重试、熔断、DLQ、Trace写入、配置解析、分支跳过、优先级、超时管理共6大关注点。构造函数注入8个依赖，其中 `@Lazy CanvasExecutionService` 本身也是 God Class（1407行），形成双向依赖。

方法分解：
- 9 个 `private Mono<Map<String, Object>>` 方法（执行分发）
- 13 个 `private void` 方法（副作用操作：tracing/metrics/DLQ/retry）
- 4 个 `private boolean` 方法（分支跳过/优先级检查）

**影响**: 新人无法理解执行流程，修改任一关注点有波及全部的风险

#### 21.2 `executeNode()` 方法复杂度爆炸（150行，6个阶段）

**文件**: `DagEngine.java:190-340`

单方法实现深度限制检查→配置解析→特殊节点路由→NodeGate CAS幂等→Handler执行+repeat→结果分发，每阶段各有分支逻辑。嵌套 `flatMap`/`onErrorResume` 链使控制流不透明。

#### 21.3 Handler 逻辑泄漏到 Scheduler

`handleLogicRelation()`（573-647行）、`handleHub()`（692-744行）、`handleAggregate()`（757-788行）都复制了各自Handler的逻辑。HubHandler/LogicRelationHandler暴露静态工具方法由DagEngine直接调用。结果：
- 新增收敛型节点必须同时修改Handler和DagEngine
- Handler接口契约被违反——Handler应是自包含的

#### 21.4 6个循环依赖被 @Lazy 掩盖

| 类 | 依赖 | 文件 |
|----|------|------|
| DagEngine | CanvasExecutionService | `scheduler/DagEngine.java:121` |
| CanvasDisruptorService | CanvasExecutionService | `disruptor/CanvasDisruptorService.java:48` |
| CanvasDisruptorService | CanvasExecutionRequestExecutor | `disruptor/CanvasDisruptorService.java:49` |
| SubFlowRefHandler | DagEngine | `handlers/SubFlowRefHandler.java` |
| TaggerHandler | CanvasExecutionService | `handlers/TaggerHandler.java` |
| CanvasTriggerHandler | DagEngine | `handlers/CanvasTriggerHandler.java` |
| TransferJourneyHandler | CanvasExecutionService | `handlers/TransferJourneyHandler.java` |

模式：Handler依赖Scheduler，Scheduler依赖ExecutionService，ExecutionService又触发Handler。`@Lazy`只是掩盖了循环依赖，依赖图仍然有环。

#### 21.5 ExecutionContext 上下文爆炸（15+ Mutable Maps）

`nodeOutputs`/`flatContext`/`nodeStatuses`/`hubStartTimes`/`loopIterations`/`jumpCounts`——每个新控制流特性都往ExecutionContext加Map。无"per-node execution state"抽象，只是临时拼凑。

#### 21.6 flatContext.putAll() Last-Writer-Wins 数据丢失

**文件**: `context/ExecutionContext.java:128-144`

两个节点产生相同key的output时，第二个覆盖第一个，无警告。下游节点读到非确定性值。

#### 21.7 Context 大小限制只警告不执行

`approxSizeBytes`（AtomicInteger）超过1MB限制时只log warn，不拒绝写入。恶意/缺陷节点可无限制膨胀context导致OOM。

#### 21.8 无内部事件模型

引擎组件间通信全部是直接的Reactive方法链（flatMap/then/onErrorResume）。无法观察/拦截阶段间流转，无法添加横切关注点。Disruptor仅用于ingress，不用于内部通信。

#### 21.9 Disruptor 背压仅 Fast-Fail

**文件**: `disruptor/CanvasDisruptorService.java:229-235`

`tryNext()` 在ring buffer满时抛 `InsufficientCapacityException`。无阻塞等待、无溢出队列、无优雅降级。持续负载下触发器直接丢弃。

#### 21.10 事件类型靠 Null Check 区分

**文件**: `disruptor/CanvasExecutionEvent.java`

事件类型（canvas执行 vs request执行）由 `event.requestId != null` 决定。无显式事件类型enum。若bug误设requestId，事件将分发到错误handler。

#### 21.11 TraceWriteBuffer 静默丢弃

**文件**: `scheduler/TraceWriteBuffer.java:49-51`

buffer超过50000条时静默丢弃新trace。无指标计数、无fallback存储、无告警。负载下执行trace消失，调试不可能。

#### 21.12 无类型化异常层次

所有错误用 `RuntimeException`/`IllegalStateException`。无法区分编程bug/域错误/瞬态失败/业务规则违反。全被同一 `onErrorResume` 捕获同等处理。

#### 21.13 7处吞异常（2处在关键路径）

| 文件 | 行 | 上下文 |
|------|---|--------|
| `DagEngine.java` | 1237 | `catch (Exception ignored) {}` writeTraceEnd — 节点永远显示"running" |
| `CanvasExecutionService.java` | 1379 | `catch (Exception ignored) {}` updateExecutionById — 执行记录永远不更新 |
| `CanvasSchedulerService.java` | 325 | 定时触发吞异常 |
| `CanvasExecutionRequestExecutor.java` | 488 | 请求执行吞异常 |
| `AudienceUserResolver.java` | 75 | 人群解析吞异常 |
| `ApiCallHandler.java` | 206 | API调用错误吞异常 |
| `MarketingPolicyService.java` | 119 | 策略检查吞异常 |

#### 21.14 `Map<String, Object>` 作为通用配置类型

Handler接口 `executeAsync(Map<String, Object> config, ExecutionContext ctx)` 意味着：
- 无编译期配置结构验证
- 无IDE自动补全
- 运行时 `ClassCastException`
- 唯一文档是源代码

#### 21.15 DagParser 硬编码边字段提取

**文件**: `dag/DagParser.java:86-140`

`extractTargets()` 硬编码 ~20个字段key + 6个列表字段。新增节点出口字段必须修改此方法。

#### 21.16 GroovyHandler 暴露完整上下文

**文件**: `handlers/GroovyHandler.java:164`

Groovy脚本binding接收完整 `ExecutionContext`。用户脚本可读取任何节点输出、修改flatContext、访问内部状态如 `benefitGranted`/`userReached`。无沙箱访问控制。

#### 21.17 NodeGate 无封装

**文件**: `context/NodeGate.java`

`executing`/`repeatPending` 暴露为public `AtomicBoolean`。任何代码可直接操纵这些标志，绕过CAS并发协议。

#### 21.18 HandlerRegistry 无生命周期

**文件**: `handler/HandlerRegistry.java`

`@PostConstruct` 扫描注册后不可变。无 `init()`/`destroy()`/`healthCheck()`/`pause()`/`resume()`。无热重载或动态注册能力。

#### 21.19 String Literal 代替 Enum

**文件**: `DagEngine.java:1456-1490`

`markNonTakenBranchesSkipped()` 用字符串 `"SELECTOR"` 而非 `NodeType.SELECTOR` 枚举常量。

#### 实施方案

**Step 1: 拆分DagEngine God Class（21.1/21.2/21.3）**

```
engine/scheduler/
├── DagEngine.java           (精简为编排入口，~300行)
├── NodeDispatcher.java      (节点分发：executeNode逻辑)
├── ConvergenceHandler.java  (Hub/LogicRelation/Aggregate收敛逻辑，从DagEngine提取)
├── EngineMonitor.java       (trace/metrics/DLQ写入)
└── ConfigResolver.java      (配置解析+context值注入)
```

**Step 2: 消除循环依赖（21.4）**

引入 `EngineEventPublisher` 接口解耦：
```java
// handlers 不再直接依赖 DagEngine/CanvasExecutionService
public interface EngineEventPublisher {
    Mono<Void> publishNodeCompletion(String canvasId, String nodeId, NodeResult result);
    Mono<Void> publishSubFlowExecution(String canvasId, String subFlowConfig);
}
```

**Step 3: ExecutionContext 重构（21.5/21.6/21.7）**

```java
public class ExecutionContext {
    // 按节点隔离输出，禁止全局flatContext putAll
    private final ConcurrentHashMap<String, NodeOutput> nodeOutputs; // key=nodeId
    // 只读的合并视图
    public Map<String, Object> getFlattenedOutput() { /* 延迟计算，key冲突加前缀 */ }

    // 大小限制强制执行
    public void putNodeOutput(String nodeId, Map<String, Object> output) {
        if (approxSizeBytes.get() + estimateSize(output) > MAX_CONTEXT_BYTES) {
            throw new ContextSizeExceededException(MAX_CONTEXT_BYTES);
        }
        // ...
    }
}
```

**Step 4: 异常层次（21.12/21.13）**

```java
public abstract class CanvasEngineException extends RuntimeException {
    private final String errorCode;
    protected CanvasEngineException(String errorCode, String message) { ... }
}

public class HandlerNotFoundException extends CanvasEngineException { ... }
public class ContextSizeExceededException extends CanvasEngineException { ... }
public class DagCycleDetectedException extends CanvasEngineException { ... }
public class ExecutionDepthExceededException extends CanvasEngineException { ... }
```

**Step 5: 类型化配置（21.14）**

```java
// 每种节点类型定义自己的Config record
public record IfNodeConfig(String condition, String successNodeId, String failNodeId) {}
public record DelayNodeConfig(long delaySeconds, String nextNodeId) {}

// Handler接口改为泛型
public interface NodeHandler<C> {
    Mono<NodeResult> executeAsync(C config, ExecutionContext ctx);
}
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | DagEngine拆分为4个类 | 16h |
| 2 | 消除循环依赖（EngineEventPublisher） | 8h |
| 3 | ExecutionContext重构 | 8h |
| 4 | 异常层次定义+替换 | 6h |
| 5 | Handler泛型化配置 | 12h |
| 6 | TraceWriteBuffer指标+限流 | 3h |
| 7 | GroovyHandler沙箱 | 4h |
| 8 | NodeGate封装 | 2h |
| 9 | 全量测试 | 8h |

**总工时**: ~67h

---

### 问题二十二：数据模型与持久化设计缺陷（17项）

#### 22.1 完全贫血的领域模型

全部49个DO类是纯 `@Data` POJO，零业务方法。所有业务规则散落在Service类中。

- **CanvasDO** — `status` 是裸 `Integer`，状态转换规则分散在 `CanvasService`/`CanvasOpsService`/`CanvasTransactionService`
- **CanvasVersionDO** — 已发布版本应是不可变快照，但有公开 `setGraphJson()` setter
- **CanvasExecutionDO** — 无状态转换校验，`markRunningExecutionsFailed()` 可直接设为FAILED绕过任何验证
- **CustomerPointsLedgerDO** — 无余额不变量，无聚合根确保 总贷方>=总借方
- **所有实体** — `@Data` 生成全部setter，任何Service可破坏不变量

#### 22.2 零外键约束

90个迁移文件无一 `FOREIGN KEY` 子句。孤儿记录可能产生。`CanvasOpsService.clone()` 分两条语句插入canvas和version——若version插入失败，canvas行无draft残留。

#### 22.3 Nullable tenant_id — 多租户是装饰性的

**文件**: `V78__saas_foundation.sql`

所有 `ADD COLUMN tenant_id BIGINT NULL`。无NOT NULL约束、无FK到tenant.id、无应用层拦截器自动填充。`CanvasDO.java` 甚至没有 `tenantId` 字段——列存在于DB但对ORM层不可见。

#### 22.4 graph_json MEDIUMTEXT — 无结构化DAG存储

整个DAG序列化为单个MEDIUMTEXT列。V88迁移直接在SQL中用 `JSON_REMOVE()` 修复域数据——无结构存储模型正在崩溃的明确信号。

#### 22.5 6个限界上下文共享单库

Canvas/CDP/Customer/Auth/Notification/Task 全部共享一个MySQL数据库，无schema隔离。CDP的批量查询可饿死Canvas执行写入。

#### 22.6 N+1 查询

**文件**: `CanvasUserQueryService.java:76`

`listUsers()` 加载全部执行记录，然后对每个用户调用 `userService.ensureUser()` + `tagService.listCurrentTags()`。10000次执行 → 4000+独立DB查询。

#### 22.7 无界胖查询 — 无LIMIT

`CanvasUserQueryService.listUsers()`、`CdpUserInsightService.getUserInsight()` 无分页限制。百万级执行记录将OOM。

#### 22.8 无 Saga/补偿模式

`CanvasService.publish()` 修改DB后注册Redis路由——若Redis注册失败，画布已PUBLISHED但无触发路由。代码注释承认："外部副作用失败不会回滚DB，路由/缓存最终会通过TTL或下次操作自愈"——这是"基于希望的一致性"。

#### 22.9 长事务混合DB和Redis

`CanvasService.create()` 是 `@Transactional`，内部调用 `triggerRouteService.registerEventRoutes()`（Redis调用）——Redis调用持有DB事务打开。

#### 22.10 虚拟线程在事务边界外执行

`CdpTagOperationService.run()` 在虚拟线程中执行批量标签操作，运行在创建方法的事务之外。若虚拟线程崩溃，操作记录永久卡在PROCESSING。

#### 22.11 ensureUser() 竞态条件

`CdpUserService.ensureUser()` 做 `selectById()` → if null → `insert()`。`ensureUserByIdentity()` 捕获 `DuplicateKeyException`，但 `ensureUser()` 不捕获——userId上的竞态未处理。

#### 22.12 幂等性是临时拼凑的

每个Service独立用 `DuplicateKeyException` catch 实现幂等：`CdpTagService.setTag()`、`NotificationService.create()`、`AsyncTaskService.createOrReuseRunning()`。无框架级幂等机制。

#### 22.13 跨上下文一致性未处理

画布KILL时，关联的CDP标签操作不取消/通知。用户删除时，关联的CDP/客户/通知记录全部残留为孤儿。

#### 22.14 统计表是死Schema

`canvas_execution_stats`/`canvas_node_funnel_stats`（V3创建）无代码读写。实时分析查询直接命中运营写表 `canvas_execution_trace`。

#### 22.15 Per-Table Mapper 而非 Per-Aggregate Repository

48个Mapper 1:1映射数据库表。`CanvasService` 注入7+个Mapper。无 `CanvasRepository` 封装Canvas聚合根（canvas+version+execution）。

#### 22.16 Duplicated Helper Methods

`nextVersionNumber()`/`latestDraft()` 在 `CanvasService`/`CanvasTransactionService`/`CanvasOpsService` 三处重复。

#### 22.17 V6 分区是空操作

仅含 `SELECT 1`，注释说DBA手动分区。Flyway历史误导——生产可能有也可能没有分区。

#### 实施方案

**Step 1: 富领域模型（22.1）**

```java
// CanvasDO → Canvas (聚合根)
public class Canvas extends BaseDO {
    private CanvasStatus status;
    private Long publishedVersionId;

    public void publish(Long versionId) {
        if (status != CanvasStatus.DRAFT) throw new InvalidStateTransitionException(status, CanvasStatus.PUBLISHED);
        this.status = CanvasStatus.PUBLISHED;
        this.publishedVersionId = versionId;
    }

    public void kill() {
        if (status != CanvasStatus.PUBLISHED && status != CanvasStatus.OFFLINE)
            throw new InvalidStateTransitionException(status, CanvasStatus.KILLED);
        this.status = CanvasStatus.KILLED;
    }
}
```

**Step 2: Outbox Pattern 替代希望一致性（22.8/22.9）**

```java
// 在 @Transactional 内写入 outbox 表
@Transactional
public void publish(Long canvasId) {
    // ... DB操作 ...
    outboxMapper.insert(new OutboxEvent("canvas.published", canvasId, payload));
}

// 独立线程轮询 outbox 并发送 Redis/MQ 事件
@Scheduled(fixedDelay = 1000)
public void processOutbox() {
    List<OutboxEvent> events = outboxMapper.selectPending();
    for (OutboxEvent e : events) {
        try {
            redis.convertAndSend(e.getTopic(), e.getPayload());
            outboxMapper.markProcessed(e.getId());
        } catch (Exception ex) {
            // 下次重试
        }
    }
}
```

**Step 3: 聚合Repository（22.15）**

```java
public class CanvasRepository {
    public CanvasAggregate load(Long canvasId) { /* 加载canvas+version+latestExecution */ }
    public void save(CanvasAggregate agg) { /* 在一个事务内持久化整个聚合 */ }
}
```

**Step 4: 查询优化（22.6/22.7）**

- `CanvasUserQueryService` 改为单SQL JOIN查询
- 添加分页参数，限制单次加载量
- `canvas_execution_trace` 添加 `(canvas_id, status, node_id)` 复合索引

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | Canvas/CanvasVersion 富领域模型 | 8h |
| 2 | Outbox Pattern 实现 | 12h |
| 3 | CanvasRepository 聚合封装 | 8h |
| 4 | N+1/无界查询修复 | 8h |
| 5 | tenant_id NOT NULL + ORM映射 | 4h |
| 6 | ensureUser 竞态修复 | 2h |
| 7 | 死Schema清理 + 索引补全 | 4h |
| 8 | 全量测试 | 6h |

**总工时**: ~52h

---

### 问题二十三：API层与集成设计缺陷（12项）

#### 23.1 无API版本化

无URL前缀、无 `Accept-Version` header、无 `@ApiVersion` 注解。30+个Controller全部暴露无版本路由。Breaking change无迁移路径。

#### 23.2 非RESTful资源命名

- `CanvasController` 用 `@GetMapping("/list")` 而非 `GET /canvas`
- `TenantController` 用 `@PutMapping("/{id}/disable")` 对状态变更用PUT
- `OpsController` 无类级 `@RequestMapping`，路由散落

#### 23.3 R.fail() 丢失所有错误码粒度

**文件**: `common/R.java`, `config/GlobalExceptionHandler.java`

- `R.fail()` 始终返回 `code = -1`，`ErrorCode` 常量（CANVAS_001-010等）嵌入message字符串
- 500 handler泄露内部异常信息：`R.fail("系统错误: " + e.getMessage())`
- GlobalExceptionHandler Javadoc声称响应包含 `traceId` 但R类实际不含

#### 23.4 DO对象直接暴露为API契约

5+个Controller接受/返回DO对象：`CanvasController`(CanvasDO)、`EventDefinitionController`(EventDefinitionDO)、`ApiDefinitionController`(ApiDefinitionDO)、`MqDefinitionController`(MqMessageDefinitionDO)。DB列增删改直接破坏API。

#### 23.5 关键开放端点无认证

**文件**: `config/SecurityConfig.java:63-66`

`/canvas/execute/direct/*` 和 `/canvas/trigger/behavior` 是 `permitAll()`。无HMAC/API Key/二次认证。`ExecutionController` 直接从request body取 `userId`，任何调用者可冒充任何用户。

#### 23.6 5处Controller内联授权检查

`AdminController.requireUserAdmin()`、`TenantController.requireSuperAdmin()`、`SystemOptionController.requireAdminContext()`、`AsyncTaskController.canView()`、`CanvasExecutionManagementController`审批人检查——与SecurityConfig形成分裂的授权模型。

#### 23.7 7处裸WebClient绕过连接池/超时/熔断

`MetaController:304`、`CanvasSchedulerService:83-84`、`ReachDeliveryService:44`、`ReachPlatformHandler:29`、`TaggerOfflineHandler:31`、`CouponHandler:33`、`AudienceBatchComputeService:236`——全用 `WebClient.builder().baseUrl(url).build()`，无连接池/超时/熔断。可无限挂起耗尽资源。

#### 23.8 无抗腐层

外部服务响应直接被领域代码消费。`MetaController` 直接转发tagger服务响应到前端。`TagImportSourceService` 直接转发HTTP响应。

#### 23.9 无Correlation ID / 分布式追踪

代码库零 `correlation`/`traceId`/`MDC` 模式。无端到端请求追踪能力。

#### 23.10 401处理用 window.location.href 而非 React Router

**文件**: `frontend/src/services/api.ts:38`

全页刷新销毁所有React状态。未保存的画布编辑丢失。

#### 23.11 Ops缓存失效端点无认证

**文件**: `SecurityConfig.java:70`

`/ops/**` 是 `permitAll()`。任何未认证调用者可反复失效缓存，构成DoS攻击向量。

#### 23.12 Route初始化锁未用Lua释放

**文件**: `CanvasRouteInitializer.java:117`

`redis.delete(REBUILD_LOCK)` 无条件删除锁key。另一个实例的TTL过期后可能删除其锁。`TriggerRouteService` 正确用了Lua脚本但此处没有。

#### 实施方案

**Step 1: API版本化（23.1）**

```java
// 方案：URL前缀版本化
@RestController
@RequestMapping("/v1/canvas")
public class CanvasController { ... }

// 或 Header 版本化
@GetMapping(value = "/canvas", headers = "Accept-Version=1")
```

**Step 2: R类改造（23.3）**

```java
public class R<T> {
    private String code;   // 改为 String，使用 ErrorCode 常量
    private String message;
    private String traceId; // 新增
    private T data;

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), TraceContext.currentTraceId(), null);
    }
}
```

**Step 3: DTO层引入（23.4）**

```java
// 不再暴露 DO
@RestController
@RequestMapping("/v1/canvas")
public class CanvasController {
    @GetMapping("/{id}")
    public R<CanvasDetailVO> getDetail(@PathVariable Long id) {
        Canvas canvas = canvasRepository.load(id);
        return R.ok(CanvasConverter.toVO(canvas));
    }
}
```

**Step 4: 统一WebClient（23.7）**

```java
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient webClient(ReactorClientHttpConnector connector) {
        return WebClient.builder()
            .clientConnector(connector)
            .filter(Resilience4jFilter.circuitBreaker("default"))
            .build();
    }
}

// 禁止：WebClient.builder().baseUrl(url).build()  → 编译期检查用 ArchUnit
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | API版本化（URL前缀） | 4h |
| 2 | R类改造 + ErrorCode集成 | 6h |
| 3 | DTO层创建（5个核心Controller） | 12h |
| 4 | 开放端点认证加固 | 4h |
| 5 | 统一WebClient + ArchUnit规则 | 6h |
| 6 | Correlation ID引入 | 4h |
| 7 | Ops端点认证 + 锁Lua化 | 2h |
| 8 | 全量测试 | 6h |

**总工时**: ~44h

---

### 问题二十四：前端架构深度缺陷（13项）

#### 24.1 画布编辑器God Component（2084行）

**文件**: `frontend/src/pages/canvas-editor/index.tsx`

`EditorInner` 组件（332-2053行）管理15+个独立状态：nodes/edges、canvasName/settings、selectedNodeId/insertContext、saving/isDirty/clipboard、testModal/canaryModal/historyDrawer/settingsModal。组合了渲染、事件处理、保存逻辑、自动保存、本地草稿、撤销重做、验证、多个弹窗。

#### 24.2 ConfigPanel单体（1415行）

**文件**: `frontend/src/components/config-panel/index.tsx`

11个子组件（ConditionRuleList/ContextValueList/ParamDefineList等）全部定义在单文件中。

#### 24.3 CanvasNodeData类型重复定义

`types/canvas.ts:133-154` 和 `components/canvas/constants.ts:134-148` 各定义一份，shape不同。组件用 `data as CanvasNodeData & { traceColor?: string }` 强转桥接——证明constants.ts版本不完整。

#### 24.4 模块级可变缓存永不失效

**文件**: `components/config-panel/index.tsx:64-70`

`schemaCache`/`rawCache`/`systemOptionCache`/`contextFieldsCache`/`canvasListCache` 是模块作用域可变单例。跨路由导航永不清除。用户在admin页修改节点类型schema后返回编辑器，会读到过期缓存。

#### 24.5 Outlet字段映射三重复制

1. `components/canvas/outletSchema.ts:9-34` — `OUTLET_TARGET_FIELDS`
2. `pages/canvas-editor/outletRouting.ts:11-36` — `FIELD_HANDLES`
3. `pages/canvas-editor/index.tsx:163-200` — `cleanRefs`

三处必须手动同步。新出口字段若只更新了一处，删除节点会留下悬挂引用。

#### 24.6 无节点类型插件/扩展模型

**文件**: `components/canvas/branchHandles.ts:32-161`

节点类型行为编码为巨型 `switch` 语句。新增节点类型需修改5+文件：branchHandles switch、DEFAULT_NAMES、CATEGORY_COLORS、cleanRefs、outletSchema。

#### 24.7 无服务端状态层

无React Query/SWR。`canvasApi.get(canvasId)` 只在mount时调用一次。其他用户发布画布后本地状态过期直到刷新。`useSystemOptions` 每个hook实例发独立请求，3个组件同时调用=3次相同请求。

#### 24.8 自动保存竞态

**文件**: `canvas-editor/index.tsx:500-507`

`useEffect` 无依赖数组，每次渲染执行。`handleSave` closure可能过期。`savingPromiseRef` 部分缓解但设计脆弱。

#### 24.9 API响应拦截器隐式契约

**文件**: `services/api.ts:33-34`

拦截器返回 `res.data`，调用方再 `.data` 获取payload。拦截器行为变化则所有调用方静默损坏。

#### 24.10 零React组件测试

30个测试文件全测纯函数。无组件测试、无Context Provider测试、无Hook测试、无撤销重做测试、无自动保存测试。

#### 24.11 测试环境是node而非jsdom

**文件**: `vite.config.ts:8`

`environment: 'node'` 意味着无DOM。任何React组件渲染测试都会失败。结构性障碍必须先修复。

#### 24.12 dayjs缺失依赖

`canvas-editor/index.tsx` import dayjs 但 package.json 无此依赖。靠antd传递依赖工作，antd升级可能破坏。

#### 24.13 全部内联样式

200+ inline `style={{}}` 对象。无样式复用、无主题化、无法CSS覆盖。每次渲染重新创建对象。

#### 实施方案

**Step 1: 画布编辑器拆分（24.1/24.2/24.8）**

```
canvas-editor/
├── index.tsx              (~200行主入口)
├── useCanvasEditor.ts     (状态编排)
├── useAutoSave.ts         (自动保存)
├── useKeyboardShortcuts.ts(快捷键)
├── useUndoHistory.ts      (撤销重做)
├── CanvasToolbar.tsx       (工具栏)
├── TestModal.tsx           (测试弹窗)
├── CanaryModal.tsx         (灰度弹窗)
├── VersionDrawer.tsx       (版本历史)
└── SettingsModal.tsx       (设置弹窗)

config-panel/
├── index.tsx              (主入口)
└── controls/              (11个子组件独立文件)
    ├── ConditionRuleList.tsx
    ├── BranchList.tsx
    └── ...
```

**Step 2: 节点类型注册表（24.5/24.6）**

```typescript
// nodeTypeRegistry.ts
interface NodeTypeDefinition {
  type: string;
  defaultName: string;
  category: string;
  color: string;
  outlets: OutletField[];
  branchHandles: (data: CanvasNodeData) => HandleConfig[];
  cleanRefs: (bizConfig: BizConfig) => Record<string, string | undefined>;
}

const registry = new Map<string, NodeTypeDefinition>();

export function registerNodeType(def: NodeTypeDefinition) { registry.set(def.type, def); }
export function getNodeTypeDefinition(type: string) { return registry.get(type); }
```

**Step 3: 引入 React Query（24.7）**

```typescript
// hooks/useCanvas.ts
export function useCanvas(id: string) {
  return useQuery({
    queryKey: ['canvas', id],
    queryFn: () => canvasApi.get(id),
    staleTime: 30_000,
  });
}
```

**Step 4: 测试基础设施（24.10/24.11）**

```typescript
// vite.config.ts
test: {
  environment: 'jsdom',
  setupFiles: ['./src/test/setup.ts'],
}

// 添加 @testing-library/react + @testing-library/jest-dom
```

| 步骤 | 任务 | 预估工时 |
|------|------|----------|
| 1 | 画布编辑器拆分 | 12h |
| 2 | ConfigPanel拆分 | 6h |
| 3 | 节点类型注册表 | 8h |
| 4 | React Query引入 | 6h |
| 5 | CanvasNodeData统一+类型修复 | 4h |
| 6 | 模块级缓存替换为React Query cache | 4h |
| 7 | 测试基础设施（jsdom+testing-library） | 4h |
| 8 | 核心组件测试 | 8h |
| 9 | 内联样式迁移CSS Modules | 8h |
| 10 | dayjs显式依赖 | 0.5h |

**总工时**: ~60.5h

---
