# MQ 削峰与优先级队列设计（优化点 #5）

## 现状问题

`CanvasExecutionService.trigger()` 在单画布并发超限时**静默丢弃**事件：

```java
// CanvasExecutionService.java L169-173
if (active >= maxConc) {
    return Map.of("overflow", "concurrency_limit_reached", ...);  // 事件永久丢失
}
```

Disruptor Ring Buffer（65536 槽）已作为本地队列吸收瞬间流量，但缺少**溢出承接**和**优先级分级**。

---

## 架构设计

```
触发源（行为/MQ/定时）
    ↓
TriggerPreCheckService（配额/冷却期门控）
    ↓
优先级路由
    ├─ HIGH  ─────────────────────────→ Disruptor（不受 maxConc 限制）
    ├─ NORMAL ─→ active < maxConc?─Yes→ Disruptor
    │             └─No→ CANVAS_TRIGGER_OVERFLOW（RocketMQ 延迟 5s 重试）
    └─ LOW  ───→ active < maxConc×0.5?─Yes→ Disruptor
                  └─No→ 直接丢弃（低优先级可接受损失）
```

### 优先级定义

| 级别 | 触发类型 | 并发限制 | 溢出策略 |
|------|---------|---------|---------|
| HIGH | `DIRECT_CALL`（支付回调等直调） | 不限（始终执行） | 无 |
| NORMAL | `MQ`、`BEHAVIOR`、`API_CALL` | `maxConc`（默认值） | 延迟 MQ 重试（最多 3 次） |
| LOW | `SCHEDULED`（批量定时触发） | `maxConc × 0.5` | 直接丢弃（已有 Jitter 均摊） |

优先级通过 `application.yml` 配置（可覆盖默认值）：

```yaml
canvas:
  execution:
    priority:
      HIGH:    [DIRECT_CALL]
      NORMAL:  [MQ, BEHAVIOR, EVENT_TRIGGER]
      LOW:     [SCHEDULED]
    overflow-retry-delay-ms: 5000   # 延迟重试间隔
    overflow-max-retry: 3           # 最大重试次数
```

---

## 溢出重试 Topic

| Topic | Tag | 用途 |
|-------|-----|------|
| `CANVAS_TRIGGER_OVERFLOW` | `{triggerType}` | 溢出事件延迟重试 |

消息体复用 `MqTriggerMessage`，额外携带 `retryCount` 字段。

**Consumer：** `OverflowRetryConsumer`，读取延迟消息，重新调用 `CanvasExecutionService.trigger()`。
若重试次数超过 `overflow-max-retry`，写入现有 `canvas_execution_dlq` 表，不再重试。

---

## 改动文件

| 文件 | 改动 |
|------|------|
| `CanvasExecutionService.java` | 溢出时改为发 RocketMQ delay 消息；HIGH 优先级跳过并发检查 |
| `infra/mq/OverflowRetryMessage.java` | 新建，携带重试次数 |
| `infra/mq/OverflowRetryConsumer.java` | 新建，消费延迟重试消息 |
| `application.yml` | 优先级映射配置 |

---

## 不在范围内

- Disruptor 内部优先级队列（实现复杂度高，当前 Ring Buffer + 优先级路由已满足需求）
- 分布式优先级队列（跨实例排序）
- 可视化队列积压监控（Micrometer 指标预留即可）
