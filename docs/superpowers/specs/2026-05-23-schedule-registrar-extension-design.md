# 调度注册扩展口设计

## 背景

当前项目的画布定时触发主要由 `CanvasSchedulerService` 在画布发布时把 `SCHEDULED_TRIGGER` 注册到 Spring `TaskScheduler`。任务句柄保存在当前 JVM 内存中，适合本地开发和单实例部署，但不适合作为严格生产级调度能力：

1. 应用重启会丢失 JVM 内的画布调度注册，除非重新发布画布。
2. 多副本部署时，每个实例都有可能注册同一个 cron，导致重复触发。
3. jitter 依赖内存 `Mono.delay`，实例重启会丢失尚未触发的延迟任务。
4. 业务代码直接绑定 Spring `TaskScheduler`，后续接入 DolphinScheduler、XXL-Job、Quartz 等外部调度器时会侵入核心流程。

本设计先解决“可替换注册位置”的架构问题：为调度注册预留稳定扩展口，让生产环境可以通过自定义 Bean 接入外部调度平台，默认实现继续支持本地开发。

## 目标

1. 引入一个小而清晰的 `ScheduleRegistrar` 抽象，隐藏具体调度器差异。
2. 默认提供 `LocalTaskScheduleRegistrar`，继续使用 Spring `TaskScheduler`，保证现有本地行为可用。
3. 默认实现必须明确注释：只适合本地开发、单实例、非严格生产场景。
4. 生产环境可以声明自己的 `ScheduleRegistrar` Bean 替换默认实现。
5. 不在本次实现中直接集成 DolphinScheduler，只保留接入边界和示例说明。
6. 不改变画布执行引擎、节点 Handler、`canvas_execution_request` 的已有职责。

## 非目标

1. 不搭建 DolphinScheduler、XXL-Job 或 Quartz。
2. 不实现 DolphinScheduler Open API 客户端。
3. 不重构调度触发后的用户级执行为持久化 request 队列。
4. 不解决全部生产级调度问题，例如 misfire 补偿、跨实例幂等 fire、持久化 jitter。
5. 不把外部调度器暴露给前端。

## DolphinScheduler 评估

DolphinScheduler 可以 Docker 部署，也有 API、可视化工作流、补数、失败重跑、告警和权限能力。它适合公司级统一调度中心，但对单个 Canvas 项目的 cron 来说偏重。

生产部署通常不是一个轻量进程，而是一组服务：API Server、Master、Worker、Alert Server，以及 PostgreSQL/MySQL 和 ZooKeeper 等依赖。它带来的收益是统一治理和运维可见性，代价是平台运维复杂度、资源占用和 API 适配成本。

因此本项目不应直接把 DolphinScheduler 写死在核心代码里。正确边界是：Canvas 只依赖 `ScheduleRegistrar`，生产环境可通过自定义 Bean 接入 DolphinScheduler；如果未来公司统一调度平台换成 XXL-Job 或 Quartz，同样只替换 Bean。

## 方案

### 核心接口

新增调度注册接口，表达“注册一个业务调度”和“注销一个业务调度”：

```java
public interface ScheduleRegistrar {
    void register(ScheduleRegistration registration);
    void unregister(ScheduleKey key);
}
```

`ScheduleRegistration` 包含：

- `ScheduleKey key`：业务唯一键，例如 `canvas:{canvasId}:{nodeId}`。
- `String cronExpression`：CRON 调度表达式，和一次性时间二选一。
- `LocalDateTime triggerTime`：ONCE 模式触发时间，和 cron 二选一。
- `String timezone`：默认 `Asia/Shanghai`。
- `Runnable callback`：本地实现触发时直接调用的回调。
- `Map<String, Object> metadata`：外部调度器注册所需的业务元数据，例如 `canvasId`、`nodeId`、`callbackUrl`。

`ScheduleKey` 只承载稳定业务标识：

```java
public record ScheduleKey(String namespace, String id) {}
```

画布定时可以使用：

```text
namespace = "canvas"
id = "{canvasId}:{nodeId}"
```

人群计算可以后续使用：

```text
namespace = "audience"
id = "{audienceId}"
```

### 默认本地实现

`LocalTaskScheduleRegistrar` 使用现有 Spring `TaskScheduler`：

- 内部维护 `Map<ScheduleKey, ScheduledFuture<?>>`。
- `register()` 先取消同 key 旧任务，再注册新任务。
- `unregister()` 取消并移除任务。
- `@PreDestroy` 取消全部本地任务。
- 使用 `@ConditionalOnMissingBean(ScheduleRegistrar.class)` 注册默认 Bean。

类注释必须直接说明限制：

```text
This implementation is for local development and single-instance deployments.
It stores schedule handles in the current JVM and is not a production-grade
distributed scheduler. Production deployments should provide their own
ScheduleRegistrar bean backed by DolphinScheduler, XXL-Job, Quartz JDBC cluster,
or another durable scheduler.
```

### CanvasSchedulerService 接入方式

`CanvasSchedulerService` 不再直接依赖 `TaskScheduler`，改为依赖 `ScheduleRegistrar`。

注册流程保持原语义：

1. 发布画布后解析 graph。
2. 找到 `SCHEDULED_TRIGGER` 节点。
3. 构造 `ScheduleRegistration`。
4. 把现有 `triggerForAllUsers(canvasId, nodeId, cfg, group)` 包装为 callback。
5. 调用 `scheduleRegistrar.register(registration)`。

注销流程：

1. 下线、归档或重新发布旧版本时解析旧 graph。
2. 根据 `canvasId + nodeId` 构造 `ScheduleKey`。
3. 调用 `scheduleRegistrar.unregister(key)`。

为了不扩大范围，`PendingJitterGroup` 可以暂时留在 `CanvasSchedulerService`，本地 callback 仍沿用现有 jitter 行为。外部调度器实现如果不在当前 JVM 调 callback，可以把 `metadata` 注册到外部平台，并由外部平台调用后续预留的 HTTP fire API。

### 外部调度器替换方式

生产环境或其他模块只需要声明自己的 Bean：

```java
@Bean
ScheduleRegistrar dolphinScheduleRegistrar(DolphinSchedulerClient client) {
    return new DolphinScheduleRegistrar(client);
}
```

由于默认本地实现使用 `@ConditionalOnMissingBean(ScheduleRegistrar.class)`，自定义 Bean 存在时本地实现不会生效。

DolphinScheduler 实现建议采用 upsert 语义：

1. 根据 `ScheduleKey` 查本地映射或外部 workflow。
2. 不存在则创建 project/workflow/task/schedule。
3. 已存在则更新 cron、timezone、metadata 和 callback 信息。
4. 禁用或删除时调用外部平台下线 schedule。
5. 外部注册失败时抛异常，让发布流程失败，不产生“画布已发布但定时未注册”的静默不一致。

本次不实现该客户端，只在注释或文档中给出替换 Bean 的方式。

## 错误处理

`ScheduleRegistrar.register()` 失败时应该抛出运行时异常。画布发布流程已经在注册触发路由后注册定时任务；如果定时注册失败，应让发布调用方看到失败，避免运营误以为定时已生效。

`ScheduleRegistrar.unregister()` 失败应记录错误并向调用方抛出异常。当前下线/归档路径有事务外清理动作，后续实现时应保持现有语义，不吞掉外部调度器注销失败。

默认本地实现的 `callback` 内异常不应杀死调度线程，应捕获日志并保持后续 cron 继续运行。现有 `triggerForAllUsers` 内部已有部分错误处理，注册层仍应做兜底保护。

## 测试策略

1. 为 `LocalTaskScheduleRegistrar` 写单元测试：
   - 同 key 重复注册会取消旧任务。
   - unregister 会取消任务。
   - `@PreDestroy` 会取消全部任务。
2. 为 `CanvasSchedulerService` 写协作测试：
   - 发布 graph 中的 `SCHEDULED_TRIGGER` 会调用 `ScheduleRegistrar.register()`。
   - 下线 graph 中的 `SCHEDULED_TRIGGER` 会调用 `ScheduleRegistrar.unregister()`。
   - 注册 metadata 包含 `canvasId`、`nodeId`、`timezone`、`cronExpression` 或 `triggerTime`。
3. 保留现有 jitter 测试，确保本地实现路径行为不退化。

## 后续生产化方向

这次只预留扩展口。真正生产化调度还需要后续独立设计：

1. 外部调度器到点后调用 Canvas 内部 fire API，而不是直接执行用户级 DAG。
2. fire API 使用确定性 `fireId` 做跨实例幂等。
3. 用户级触发写入 `canvas_execution_request`，`sourceMsgId` 使用 `schedule:{canvasId}:{nodeId}:{fireTime}:{userId}`。
4. jitter 改为持久化计划执行时间，不再依赖 JVM 内存 `Mono.delay`。
5. 人群计算定时也迁到同一 `ScheduleRegistrar` 边界，避免重启后空 job。

## 验收标准

1. 核心代码不再直接把调度注册能力绑定到 Spring `TaskScheduler`。
2. 默认不声明自定义 Bean 时，本地开发行为保持可用。
3. 声明自定义 `ScheduleRegistrar` Bean 后，默认本地实现不会生效。
4. 代码注释明确说明本地实现不是生产级分布式调度器。
5. 设计不把 DolphinScheduler 作为硬依赖，后续可替换为 XXL-Job、Quartz 或其他实现。
