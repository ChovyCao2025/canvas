# Spec: 引入投递队列（Outbox Pattern）

> **编号:** J | **严重度:** High | **迁移难度:** Medium

## Problem

引擎通过 `WebClient.post()` 直接调用触达平台 HTTP 接口，DAG 执行同步等待投递结果。

**核心问题：**
1. 投递无保障 — 引擎崩溃在触达平台接受请求后、DB 更新前 → 消息已发但未记录
2. 同步耦合 — 触达平台慢（5s 超时）→ 整个 DAG 执行阻塞
3. 重试不隔离 — 触达平台宕机 → retry 占用执行 slot → 其他画布被挤占
4. 无送达回执 — 只记录"已发/失败"，不追踪实际送达状态

## Goal

引入投递队列（RocketMQ Topic `CANVAS_DELIVERY`）。引擎发布投递请求到队列后立即推进到下一个 DAG 节点。独立消费端处理实际 HTTP 调用、重试和回执。

## Scope

### In Scope
- RocketMQ `CANVAS_DELIVERY` Topic 创建
- `ReachDeliveryService` 改为异步投递
- 投递消费者实现（独立重试、回执处理）
- DAG 中"等待投递结果再继续"语义处理
- PENDING 记录 reconciliation 机制

### Out of Scope
- DAG 引擎重构（问题 A+B）
- 服务拆分（问题 C）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `ReachDeliveryService.java` | Modify | 同步调用 → MQ 异步投递 |
| `DeliveryConsumer.java` | Create | 投递消费者 |
| `DagEngine.java` | Modify | 投递节点异步推进逻辑 |
| `application.yml` | Modify | CANVAS_DELIVERY Topic 配置 |

## Success Criteria

1. 引擎投递后立即推进 DAG，不等待触达平台响应
2. 投递失败有独立重试，不影响 DAG 执行 slot
3. 引擎崩溃不丢投递请求（MQ 持久化）
4. PENDING 记录有 reconciliation 扫描
