# Spec: 单 MQ Topic 拆分为多 Topic

> **编号:** F | **严重度:** Medium | **迁移难度:** Medium

## Problem

所有 MQ 触发器共用 `CANVAS_MQ_TRIGGER` 单一 Topic，用 tag 区分。不同触发类型流量特征差异巨大。

**核心问题：**
1. 定时触发洪峰填满消费线程池（20 线程）→ 延迟实时事件
2. 无法按触发类型设不同重试策略
3. 无法独立监控/消费某种触发类型
4. `ConsumeMode.ORDERLY` 对所有消息生效，包括不需要顺序的消息，降低吞吐

## Goal

按触发类别拆分 Topic：`CANVAS_TRIGGER_SCHEDULED`、`CANVAS_TRIGGER_EVENT`、`CANVAS_TRIGGER_MQ`、`CANVAS_TRIGGER_DLQ_REPLAY`。

## Scope

### In Scope
- 创建 4 个独立 Topic
- 重组消费组（独立线程数、重试策略）
- Consumer 和 Handler 更新
- 监控指标独立化

### Out of Scope
- 投递队列（问题 J）
- 实时计算引擎（问题 M）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `MqTriggerConsumer.java` | Modify | 拆分为多个 Consumer |
| `application.yml` | Modify | 多 Topic 配置 |
| RocketMQ Admin | Create | 创建 Topic 和消费组 |

## Success Criteria

1. 4 个独立 Topic 各自消费
2. 各 Topic 独立线程数和重试策略
3. 实时事件不受定时洪峰影响
4. 各 Topic 独立监控
