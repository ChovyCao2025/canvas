# 优化点六：旅程运行平台能力补强记录

> 记录日期：2026-05-23
> 状态：仅记录，暂不实现。其他分支可能已实现其中部分内容，后续合并前逐项核对。

## 背景

当前项目已经具备画布版本快照、节点注册表、执行 trace、DLQ、RocketMQ、限流、受众、灰度、缓存 SDK 等基础能力。下面记录的是从营销旅程/工作流调度系统架构中可借鉴、但需要结合当前代码和其他分支实际状态再决定是否落地的补强项。

## 待核对清单

| 优先级 | 优化项 | 建议方向 | 核对状态 |
|---|---|---|---|
| P0 | Delay/Wait 持久化调度 | 当前短延迟可用内存等待，但长等待应落地为 `canvas_timer` 表、RocketMQ 定时消息或 Redis ZSET 调度。需支持 `resume_at`、租约、幂等唤醒、服务重启恢复。 | 待核对 |
| P0 | 旅程实例与节点游标模型 | 在现有 `canvas_execution`/`ExecutionContext` 基础上，评估是否补 `canvas_instance`、`canvas_step_run`、`canvas_timer`、`canvas_event_subscription`，支撑长生命周期、多等待、多事件唤醒。 | 待核对 |
| P0 | 统一业务幂等模型 | 对触达、发券、MQ 外发等副作用节点统一幂等键：`canvasId + versionId + executionId + nodeId + userId`。`retry_count` 只做 attempt 记录，不进入业务幂等 key。 | 待核对 |
| P1 | 发布期 Compile Plan | 发布时除保存 `graph_json` 外，生成运行态 `compiled_plan_json`，包含 `nodesById`、`edgesBySource`、`triggerIndex`、`waitEventIndex`、上下文字段 schema、资产快照等，降低运行时解释成本。 | 待核对 |
| P1 | 标准事件 Envelope | 统一 MQ/行为/API 触发事件结构：`eventId`、`tenantId`、`subjectId`、`eventType`、`occurredAt`、`receivedAt`、`schemaVersion`、`payload`，为去重、乱序处理、归因和等待事件唤醒打基础。 | 待核对 |
| P1 | Analytics 旁路事件流 | MySQL `canvas_execution_trace` 继续作为调试回放数据；大规模节点指标建议旁路写 MQ，再落 ClickHouse 或小时级聚合表，避免 trace 表承担 OLAP 压力。 | 待核对 |
| P2 | 版本治理细化 | 已有版本快照和灰度能力，后续可补稳定 `node_key`、资产/模板快照、版本 retired 策略、跨版本统计聚合策略。 | 待核对 |
| P2 | Pause/Resume 语义拆分 | 明确暂停是停止新用户进入、冻结运行中实例、允许老实例跑完，还是仅暂停定时唤醒；不同语义需要不同执行和 timer 处理。 | 待核对 |

## 暂不建议优先投入

- 不建议当前阶段直接拆成完整微服务。更适合先保持模块化单体边界，等执行量、故障域和团队职责明确后再拆。
- 不建议立即引入 Temporal/Camunda 等完整工作流平台。当前已有自研 DAG 引擎和节点 Handler 体系，应先补齐持久化调度、幂等和实例模型。
- 不建议把实时协作、模板市场、导出、AI 等“策略协作画布”能力混入执行引擎主线。这些可作为 Studio 能力后续规划。

## 建议后续处理方式

1. 合并其他分支后，逐项搜索现有实现并把“待核对”改为“已实现 / 部分实现 / 未实现”。
2. 先处理 P0 项，尤其是 Delay/Wait 持久化调度和副作用节点幂等。
3. 对已经实现的项补测试和设计文档链接；对未实现的项再单独拆 spec 和 plan。
