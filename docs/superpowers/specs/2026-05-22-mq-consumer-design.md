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

## 补充设计：边界问题

### 1. 消费者启动时序竞态

`CanvasRouteInitializer`（`@PostConstruct`）与 RocketMQ Consumer 启动无顺序保证。若 Consumer 先就绪，路由表为空，消息被 ACK 后静默丢弃。

修复：`TriggerRouteService` 增加 `routeTableReady` 原子标志位，`CanvasRouteInitializer` 完成后置 `true`。Consumer 消费时若标志为 false，抛异常触发 `RECONSUME_LATER`：

```java
if (canvasIds.isEmpty()) {
    if (!routeService.isRouteTableReady()) {
        throw new IllegalStateException("路由表未就绪，RECONSUME_LATER");
    }
    log.warn("[MQ_CONSUMER] tag={} 无匹配画布，正常丢弃", tag);
    return; // 路由表已就绪但确实无匹配，ACK
}
```

### 2. SEND_MQ 循环触发防护

画布若在 MQ_TRIGGER 执行路径中又发 SEND_MQ 到同一 topic，会产生无限循环。

修复：消息体加 `executionDepth` 字段（默认 0），`SendMqHandler` 发送时递增，`MqTriggerConsumer` 传入执行上下文，`CanvasExecutionService.trigger()` 超过阈值（默认 5）时拒绝并写内部 DLQ：

```json
{ "userId": "u_123", "messageCode": "order_paid", "payload": {}, "executionDepth": 1 }
```

配置：`canvas.mq.max-execution-depth: 5`

### 3. RocketMQ DLQ 桥接

消息超过 16 次重试后进入 `%DLQ%GID_CANVAS_ENGINE`，若无监控则成黑洞。

修复：增加 `RocketMqDlqBridgeConsumer`，订阅 `%DLQ%GID_CANVAS_ENGINE`，将失败消息写入现有 `canvas_execution_dlq` 表，复用 `DlqController` 的查询和重放 UI。

### 4. 生产环境 Topic 预创建

`autoCreateTopicEnable=true` 仅用于开发，默认 4 队列。生产环境必须预创建 `CANVAS_MQ_TRIGGER`（8 队列）和 `CANVAS_TRIGGER_OVERFLOW`（4 队列），否则吞吐量受限且无法动态调整。运维必须在部署前执行 Topic 创建脚本。

---

## 不在范围内

- 顺序消息（Orderly）：画布触发不需要严格顺序
- 事务消息：SendMqHandler 仅发普通消息，事务由业务层保证
