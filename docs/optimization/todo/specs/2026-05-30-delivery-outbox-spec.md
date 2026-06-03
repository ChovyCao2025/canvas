# Spec: 投递 Outbox Pattern + PENDING Reconciliation

> **编号:** #7 | **严重度:** High | **类别:** 设计缺陷

## Problem

ReachDeliveryService 调用触达平台，失败后标记 FAILED。PENDING 记录在进程崩溃后无 reconciliation 机制扫描。

**核心问题：**
- 触达平台宕机 → 投递立即失败 → 重试 3 次仍失败 → DLQ → 无人处理
- PENDING 记录可能永久停留（进程崩溃在调用成功后、标记 SENT 前）
- 无 outbound queue/outbox pattern 缓冲

## Goal

引入 outbox pattern，投递请求先写 DB 再异步发送；PENDING 记录定时 reconciliation 扫描；DLQ 需要人工/自动 re-delivery 机制。

## Scope

### In Scope
- Outbox 表设计（投递请求先写 DB）
- 异步发送消费者
- PENDING 记录定时扫描（每分钟）
- DLQ re-delivery 机制（手动/自动）

### Out of Scope
- 投递队列（问题 J，但有关联，可合并）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `ReachDeliveryService.java` | Modify | outbox pattern |
| `DeliveryOutboxDO.java` | Create | outbox 表实体 |
| `DeliveryReconciliationJob.java` | Create | PENDING 扫描定时任务 |
| `DlqRedeliveryService.java` | Create | DLQ 重投递服务 |

## Success Criteria

1. 投递请求先写 DB，再异步发送
2. PENDING 记录有定时扫描
3. DLQ 有重投递机制
4. 进程崩溃不丢投递请求
