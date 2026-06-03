# PRD-P2-27-InAppNotify实现

> 本文档为营销画布平台移动端 In-App 通知实现需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-27 |
| **需求名称** | InAppNotify 实现 |
| **优先级** | P2 |
| **所属类别** | 通知协作 P1/P2 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

InAppNotify 仅有 stub，标记 TODO: 接入 MQTT 推送客户端。

### 1.2 目标

实现移动端即时推送通知（飞书 App、钉钉 App）：

- 接收企步画布变更通知
- 接收执行任务调度通知
- 接收失败告警推送

---

## 2. 功能需求

### 2.1 架构

1. **推送网关**：
   - 飞书 SMTP/SMTP SDK
   - 钉钉 Webhook
   - MQTT（适用于轻量级推送到 WeChat JS）

2. **推送队列**（消息总线）：
   - `message_queue: { userId, channel, payload, timestamp }`

3. **推送调度器**：
   - 消费队列，批量推送（减少 API 调用成本）

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 推送网关 | 3 |
| MQTT 推送 | 2 |
| 调度器 | 3 |
| UI 消息中心 | 2 |
| 测试 + 文档 | 2 |
| **总计** | **12** |

---

## 3. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 通知协作 P1/P2

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-27-InAppNotify实现.md`）**
