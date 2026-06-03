# Backend Architecture

## Overview

后端是单体 Spring Boot WebFlux 应用 (canvas-engine)，依赖 canvas-cache-sdk 模块提供分层缓存。

## Module Structure

```
backend/
├── canvas-engine/          # 主应用：所有业务逻辑、API、引擎、DAL
│   └── pom.xml             # 依赖 canvas-cache-sdk
├── canvas-cache-sdk/       # 独立缓存SDK：Caffeine L1 + Redis L2
│   └── pom.xml             # 无业务依赖
└── pom.xml                 # 父POM：Spring Boot 3.2.5, Java 21
```

## Package Structure (canvas-engine)

```
org.chovy.canvas/
├── auth/                    # 认证：SysUserService, JwtUtil, LoginReq/Resp
├── common/                  # 通用：NodeType(47值), TriggerType(17值), DataMaskingUtil, MapFieldKeys
├── config/                  # 配置：SecurityConfig(15类), JwtAuthFilter, CacheConfig, ExecutionLaneProperties
├── dal/                     # 数据访问层
│   ├── dataobject/          # 49 MyBatis-Plus DO (@TableName)
│   └── mapper/              # 49 Mapper interfaces
├── domain/                  # 领域服务 (Canvas, Cdp, Notification, Tenant, Task, Audience, Execution, Meta)
├── dto/                     # DTO (audience, cdp, notification, task, tenant)
├── engine/                  # 核心引擎
│   ├── scheduler/           # DagEngine (1540行) — 核心调度器
│   ├── handler/             # NodeHandler接口, HandlerRegistry, NodeResult, NodeRouteResolver
│   ├── handlers/            # 65+ Handler实现 (@NodeHandlerType)
│   ├── context/             # ExecutionContext (per-execution state), NodeGate (CAS lock)
│   ├── dag/                 # DagParser, DagGraph (图解析+拓扑排序)
│   ├── lane/                # ExecutionLane (4车道: light/standard/heavy/retry)
│   ├── trigger/             # CanvasExecutionService (1407行), CanvasSchedulerService
│   ├── disruptor/           # CanvasDisruptorService (LMAX Disruptor ring buffer)
│   ├── delivery/            # ReachDeliveryService (消息投递)
│   ├── audience/            # Audience compute runner, rule evaluator
│   ├── rule/                # Rule engine AST, SQL compiler, graph validator
│   ├── schedule/            # Cron-based trigger management
│   ├── wait/                # WaitResumeService, WaitSubscriptionService
│   ├── policy/              # Circuit breaker, timeout
│   └── request/             # Execution request backlog + batch dispatch
├── infrastructure/          # 基础设施
│   ├── cache/               # RocketMQ cache invalidation
│   ├── mq/                  # MqTriggerConsumer, OverflowRetryConsumer
│   └── redis/               # ContextPersistenceService, TriggerRouteService
├── service/                 # 应用服务层
├── web/                     # 29 REST controllers, 173 endpoints
├── query/                   # 查询对象
├── perf/                    # 性能测试
└── dto/                     # DTO classes
```

## Core Engine: DAG Execution Model

### DagEngine 6-Stage Pipeline

每个节点执行经过6个阶段：

1. **Config Resolution** — 替换 `${field}` 占位符，解析灰度版本
2. **Special Node Handling** — LOGIC_RELATION/HUB/AGGREGATE/THRESHOLD 有自定义路径
3. **Idempotent Check** — 防止重复执行（retry/resume场景）
4. **CAS Lock Acquisition** — NodeGate.executing 确保单线程处理
5. **Handler Execution** — 通过 HandlerRegistry 调用 NodeHandler.executeAsync()
6. **Repeat + Downstream Trigger** — THRESHOLD 等节点可重复触发；否则推下游

### NodeHandler Pattern

```java
public interface NodeHandler {
    Mono<NodeResult> executeAsync(Map<String, Object> config, ExecutionContext ctx);
    boolean isBenefitNode();   // default false — 权益节点，防丢失保护
    boolean isReachNode();     // default false — 触达节点，防丢失保护
}
```

- 注册: `@NodeHandlerType("TYPE_KEY")` → HandlerRegistry ConcurrentHashMap
- 返回: `NodeResult` 工厂方法 (ok/terminal/ifResult/fail/multiNext/waiting/suppressed/timeout/skipped/routed/pending)

### NodeResult Factory Methods

| Method | 用途 | 路由 |
|--------|------|------|
| ok() | 正常成功 | → nextNodeId |
| terminal() | 终止路径 | 无下游 |
| ifResult() | 条件分支 | → successNodeId / failNodeId / elseNodeId |
| fail() | 错误 | → 错误处理 |
| multiNext() | 扇出 | → 多个下游 |
| waiting() | 等待恢复 | 暂停执行 (WAIT, MANUAL_APPROVAL) |
| suppressed() | 业务抑制 | 跳过 (frequency cap, quiet hours) |
| routed() | 命名路由 | → branch with output |
| skipped() | 主动跳过 | 不执行 |
| timeout() | 超时 | → 错误处理 |

### ExecutionContext (Per-Execution State)

- `nodeOutputs` (ConcurrentHashMap) — 各节点输出累积
- `flatContext` (ConcurrentHashMap) — 扁平化上下文值 (${field} 替换源)
- `nodeStatuses` (ConcurrentHashMap) — 节点执行状态
- `NodeGate` (CAS locks) — 节点并发保护 + repeat机制
- 1MB size cap, 512KB warning threshold
- `benefitGranted` / `userReached` — 防丢失保护标志
- `callStack` — 子流程循环检测 (防无限递归)

### Execution Lane System

| Lane | Max Concurrency | Queue Limit | 适用场景 |
|------|----------------|-------------|----------|
| LIGHT | 600 | 2,000 | DIRECT_CALL, WAIT, GOAL_CHECK, HUB, AGGREGATE |
| STANDARD | 1,800 | 10,000 | 默认：大多数触发和业务节点 |
| HEAVY | 300 | 1,000 | SCHEDULED, GROOVY, TAGGER_OFFLINE, DLQ_REPLAY |
| RETRY | 300 | 3,000 | 溢出重试，失败重试 |

### Trigger Priority Admission

| Priority | 类型 | 行为 |
|----------|------|------|
| HIGH | DIRECT_CALL | 总是允许，溢出仅警告 |
| NORMAL | MQ, EVENT, API | 溢出到 RocketMQ 延迟队列 |
| LOW | SCHEDULED | 溢出时50%丢弃 |

## API Design

### Standard Response Wrapper

```java
R<T> { code: int, message: String, data: T }
```

- code=0 表示成功，非0表示错误
- 所有 Controller 返回 `Mono<R<T>>`
- 阻塞 DB 操作必须包装在 `Mono.fromCallable().subscribeOn(Schedulers.boundedElastic())`

### Controller Distribution (Top 5)

| Controller | Endpoints | Primary Function |
|-----------|-----------|-----------------|
| MetaController | 22 | Node type registry, MQ topics, tags, experiments |
| CanvasController | 19 | Canvas CRUD, publish, offline, canary, rollback, clone, diff |
| AudienceController | 11 | Audience compute, statistics |
| TagDefinitionController | 9 | Tag definition CRUD |
| AbExperimentController | 9 | A/B experiment CRUD |

## Authentication & Authorization

### JWT Flow

1. `POST /auth/login` → BCrypt 验证 → Redis 暴力破解检查 → JWT 生成
2. JwtAuthFilter: Bearer token → HMAC-SHA256 校验 → Redis 黑名单检查 → DB 重新加载用户
3. Logout: token hash → Redis `canvas:jwt:revoked:<hash>` (黑名单)

### RBAC Roles

| Role | Scope |
|------|-------|
| SUPER_ADMIN | 全系统 + 租户管理 |
| TENANT_ADMIN | 租户内 + 画布生命周期 + 用户管理 |
| OPERATOR | 标准操作权限 |
| ADMIN (legacy) | 等同 SUPER_ADMIN |

## Known Architecture Issues

1. **DagEngine 1540行上帝类** — 52方法，需拆分
2. **CanvasExecutionService 1407行** — 50方法，5个Mapper注入
3. **14个Handler直接注入Mapper** — 违反引擎层纯净性
4. **7处@Lazy循环依赖** — DagEngine↔CanvasExecutionService等
5. **Domain→Engine反向依赖** — CanvasService导入7个Engine类
6. **WebFlux + MyBatis-Plus互锁** — boundedElastic池耗尽风险