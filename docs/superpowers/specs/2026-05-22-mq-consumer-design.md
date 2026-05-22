# MQ_TRIGGER 消费入口设计（优化点 #6）

## 背景

`MqTriggerHandler`（节点处理逻辑）和路由表（`TriggerRouteService`）已完整实现，缺少：
1. **生产者**：`SendMqHandler` 有 TODO，未实际发送消息
2. **消费者**：无任何组件从 RocketMQ 读取消息并触发画布

## 技术选型：RocketMQ 5.x

使用 `rocketmq-spring-boot-starter 2.3.x`（官方 Spring Boot 3 适配版），通过 gRPC Proxy 接入 RocketMQ 5.x。

---

## Topic / 分区设计

| 维度 | 设计值 | 说明 |
|------|-------|------|
| Topic | `CANVAS_MQ_TRIGGER` | 单 Topic，所有 MQ 触发消息共用 |
| Tag | `MqMessageDefinition.topic` | 服务端过滤，精准路由到对应画布 |
| Queue 数 | 8（可调） | 8 路并行消费，横向扩容加实例即可 |
| Consumer Group | `GID_CANVAS_ENGINE` | CLUSTERING 模式，多实例自动分区 |
| 消费模式 | CONCURRENTLY | 各消息独立并发处理，互不阻塞 |

**慢消费隔离：** Consumer 只做 Disruptor.publish()（非阻塞），实际画布执行在 WorkerPool 异步进行。一个慢画布不阻塞任何消息消费。

---

## 消息体格式

```json
{
  "userId":      "u_123456",
  "messageCode": "order_paid",
  "payload": {
    "orderId": "9527",
    "amount":  100.00
  }
}
```

---

## 生产者（SendMqHandler）

发送目标：`CANVAS_MQ_TRIGGER:{tag}`（rocketmq-spring 的 `topic:tag` 格式）

```java
rocketMQTemplate.syncSend("CANVAS_MQ_TRIGGER:" + def.getTopic(), message);
```

---

## 消费者（MqTriggerConsumer）

```java
@RocketMQMessageListener(
    topic = "CANVAS_MQ_TRIGGER",
    consumerGroup = "GID_CANVAS_ENGINE",
    selectorType = SelectorType.TAG,
    selectorExpression = "*",
    consumeMode = ConsumeMode.CONCURRENTLY,
    messageModel = MessageModel.CLUSTERING,
    consumeThreadNumber = 20              // 可通过配置覆盖
)
```

**消费逻辑：**
1. 从 Tag 获取 routingKey（= `MqMessageDefinition.topic`）
2. `TriggerRouteService.getCanvasByMqTopic(tag)` 查路由
3. 对每个匹配的 canvasId 调用 `CanvasDisruptorService.publish()`
4. 正常返回 → RocketMQ 自动 ACK
5. Ring Buffer 满 / 路由查询异常 → 抛异常 → `RECONSUME_LATER`（最多重试 16 次）
6. 超过重试次数 → RocketMQ 自动投入 `%DLQ%GID_CANVAS_ENGINE`

---

## 背压链路

```
RocketMQ → MqTriggerConsumer.onMessage()
              ↓ publish()（微秒级）
           Disruptor Ring Buffer（65536 槽）
              ↓ 
           WorkerPool（CPU 核数线程）
              ↓
           CanvasExecutionService.trigger()（异步 Reactor）
```

Ring Buffer 满 → `publish()` 自旋 → Consumer 超时 → 抛异常 → `RECONSUME_LATER`，形成端到端背压。

---

## 配置（application.yml）

```yaml
rocketmq:
  name-server: ${ROCKETMQ_NAME_SERVER:localhost:9876}
  producer:
    group: PID_CANVAS_ENGINE
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
  consumer:
    group: GID_CANVAS_ENGINE

canvas:
  mq-consumer:
    consume-thread-number: 20  # @RocketMQMessageListener consumeThreadNumber
```

---

## 不在范围内

- 顺序消息（Orderly）：画布触发不需要严格顺序
- 事务消息：SendMqHandler 仅发普通消息，事务由业务层保证
- DLQ 手动重放 UI（现有 DlqController 架构可扩展）
