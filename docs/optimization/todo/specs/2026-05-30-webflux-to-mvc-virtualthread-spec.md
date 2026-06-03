# Spec: WebFlux → Spring MVC + Virtual Threads + Imperative DAG Engine

> **编号:** A+B | **严重度:** Critical | **迁移难度:** Hard
> **互锁陷阱:** A 和 B 必须同步迁移，不可单独解决

## Problem

当前系统使用 `spring-boot-starter-webflux`（响应式 HTTP）+ `mybatis-plus-spring-boot3-starter`（阻塞 JDBC），全项目 37 处用 `Schedulers.boundedElastic()` 桥接阻塞 DB 调用到响应式链。DagEngine 整个执行模型表达为递归 `Mono<Map<String, Object>>` 链。

**核心矛盾：**
1. WebFlux 事件循环假设非阻塞 I/O，但 JDBC 是阻塞的 → `boundedElastic` 线程池（默认 10×CPU）成为真正瓶颈
2. HikariCP 33 连接远小于 `boundedElastic` 池容量，高负载下管线停顿
3. 代码从 WebFlux 获零吞吐优势，却承担 Mono/Flux 全部复杂度
4. 递归 Mono 链表达 DAG 状态机导致：堆栈信息无用、调试极难、repeat 机制 90 行绕过、MAX_NODE_DEPTH=200 防爆栈
5. `Mono.delay().subscribe()` fire-and-forget 是响应式编程反模式

## Goal

将后端从 WebFlux + Reactor Mono 迁移到 Spring MVC + Java 21 虚拟线程 + 命令式步进 DAG 执行器，一次性消除 A+B 互锁根因。

## Target Architecture

### Spring MVC + Virtual Threads
- `spring-boot-starter-webflux` → `spring-boot-starter-web`
- `spring.threads.virtual.enabled=true`
- 每个 HTTP 请求运行在虚拟线程上，阻塞 JDBC 天然免费
- 所有 Controller 返回类型 `Mono<T>` → `T`
- 所有 `Mono.fromCallable().subscribeOn(boundedElastic)` 拆解为直接调用

### Imperative DAG Engine
- 递归 Mono 链 → `Deque<PendingNode>` 工作队列迭代式处理
- `NodeHandler.executeAsync()` → `NodeHandler.execute()` 返回 `NodeResult`
- repeat 机制从 Reactor operator → 简单循环控制
- `MAX_NODE_DEPTH=200` 限制可移除
- `Mono.delay().subscribe()` → `ScheduledExecutor.schedule()`

### Expected Outcomes
- 代码量减少 30-40%
- DagEngine 从 ~1539 行缩至 ~400 行可读命令式代码
- 调试堆栈信息与业务逻辑对应
- 无需 `boundedElastic` 桥接

## Scope

### In Scope
- Spring Boot starter 替换（webflux → web）
- 虚拟线程配置启用
- 所有 Controller 返回类型迁移
- 所有 Service 层 Mono/Flux 拆解
- DagEngine 完全重写为命令式
- NodeHandler 接口变更
- repeat/NodeGate/retry 机制重写
- 所有相关测试重写

### Out of Scope
- 单体服务拆分（问题 C，独立 TODO）
- Disruptor 替换（问题 D，独立 TODO）
- 数据基建（问题 K/L/M/O，独立 TODO）
- 前端变更（无前端影响）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `pom.xml` | Modify | webflux → web, 移除 reactor 依赖 |
| `application.yml` | Modify | 添加虚拟线程配置 |
| `DagEngine.java` | Rewrite | 递归 Mono → 命令式步进执行器 |
| `ExecutionContext.java` | Modify | 移除 Reactor 相关类型 |
| `NodeHandler.java` | Modify | executeAsync → execute |
| All 60+ handlers | Modify | 返回类型 Mono<NodeResult> → NodeResult |
| All 29 Controllers | Modify | Mono<T> → T |
| `CanvasExecutionService.java` | Modify | 移除所有 boundedElastic 包装 |
| `ContextPersistenceService.java` | Modify | Mono → 同步调用 |
| All Service classes | Modify | 移除 Reactor 类型 |

## Dependencies

- Java 21（已满足）
- Spring Boot 3.2+（需确认当前版本）
- 无其他外部依赖新增

## Risk Assessment

- **高风险：** DagEngine 是系统核心，重写影响所有 handler、测试、调用者
- **缓解策略：** 
  1. 先迁移 Spring MVC + 虚拟线程（Controller/Service 层），保持 DagEngine 暂时用 Reactor
  2. 再重写 DagEngine 为命令式
  3. 两步之间系统可运行，降低爆炸半径

## Success Criteria

1. 所有 `Schedulers.boundedElastic()` 调用消失
2. 所有 Controller 返回同步类型
3. DagEngine 无 Reactor 依赖
4. 全部现有测试通过（行为等价）
5. 代码行数显著减少（DagEngine 目标 < 500 行）
