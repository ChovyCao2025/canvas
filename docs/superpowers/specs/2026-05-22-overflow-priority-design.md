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

## 补充设计：边界问题修正

### 1. retryCount 字段设计修正（严重）

原设计用消息体的 `retryCount` 判断是否超限，但 RocketMQ 自动重试时消息体不变，`retryCount` 永远不递增，限制形同虚设。

**修正**：使用 `MessageExt.getReconsumeTimes()`（RocketMQ 内置重试计数）：

```java
// OverflowRetryConsumer.onMessage()
if (message.getReconsumeTimes() >= priorityConfig.getOverflowMaxRetry()) {
    log.warn("[OVERFLOW_RETRY] 超过最大重试 canvasId={}", msg.getCanvasId());
    dlqService.record(msg.getCanvasId(), msg.getUserId(), "overflow_max_retry");
    return; // ACK，不再重试
}
```

`OverflowRetryMessage` 中的 `retryCount` 字段**改为记录跨越 `sendOverflowRetry` 调用的累计次数**（解决下方无限循环问题），与 `getReconsumeTimes()` 相加共同判断：

```java
int totalRetry = message.getReconsumeTimes() + msg.getChainRetryCount();
if (totalRetry >= priorityConfig.getOverflowMaxRetry()) { ... }
```

### 2. 溢出 → 重试 → 再溢出无限循环修正（严重）

`OverflowRetryConsumer` 投入 Disruptor，Disruptor worker 再调 `trigger()`，若仍超限则再次 `sendOverflowRetry(retryCount=0)`，产生全新消息，`getReconsumeTimes()` 重置为 0，`maxRetry` 完全失效。

**修正**：`OverflowRetryMessage` 新增 `chainRetryCount` 字段，`CanvasExecutionService.sendOverflowRetry()` 从上下文读取并递增传入；`trigger()` 的 payload 中携带此字段：

```java
// trigger() 内溢出分支
int chainRetry = (int) payload.getOrDefault("__overflowChainRetry", 0);
sendOverflowRetry(canvasId, userId, triggerType, ..., chainRetry);

// sendOverflowRetry()
OverflowRetryMessage msg = new OverflowRetryMessage(..., chainRetryCount = chainRetry + 1);
```

### 3. HIGH 优先级上限修正

完全无限制的 HIGH 优先级在结算日批量支付回调场景下会打满引擎。修正为独立的 `highMaxConcurrencyRatio`：

```yaml
canvas.execution.priority:
  high-max-concurrency-ratio: 2.0  # HIGH 使用 maxConc × 2，而非真正无限制
```

```java
case HIGH:
    int highMax = (int)(maxConc * priorityConfig.getHighMaxConcurrencyRatio());
    if (active < highMax) break; // 未超 HIGH 上限，直接执行
    // 超 HIGH 上限：打 warn，仍执行（HIGH 不丢弃，但记录告警）
    log.error("[ENGINE] HIGH 优先级超并发上限 canvasId={} active={}/{}", canvasId, active, highMax);
```

### 4. 溢出超限写 DLQ 实现补全

原 Plan 里 `OverflowRetryConsumer` 只打 warn 日志，未实际写 DLQ 表。补全调用 `CanvasExecutionDlqService.record()` 写入 `canvas_execution_dlq`，与现有 DLQ 基础设施对接。

---

## 不在范围内

- Disruptor 内部优先级队列（复杂度高，当前方案已满足需求）
- 分布式优先级队列（跨实例排序）
- 可视化队列积压监控（Micrometer 指标预留）
