# PRD-P2-09-数据保留自动清理策略

> 本文档为营销画布平台消费者数据自动清理策略需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-09 |
| **需求名称** | 数据保留自动清理策略 |
| **优先级** | P2 |
| **所属类别** | 安全合规 HIGH 转 P2 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

执行日志 (`execution_log`)、节点执行结果 (`node_execution_result`) 无自动清理策略，可能无限堆积。

### 1.2 法规依据

- **GDPR Art.5(1)(e)**：数据主体有权在合适的情况下被及时删除，应有保存期限规定
- **PIPL Art.6**：处理个人信息的保存期限一般不得超过必要期限

### 1.3 竞品对标

| 竞品 | 策略 |
|------|------|
| Braze | 支持自定义 Data Retention Policy |
| Iterable | 7/30/90/180 天自动删除策略 |

---

## 2. 功能需求

### 2.1 核心功能

1. **自动清理调度器**：Cron 任务定期清理过期数据
2. **数据保留策略配置**：为每张表（或全局策略）配置保留天数
3. **软删除 + 硬删除**：历史数据软删除 30 天后硬删除
4. **清理审计**：记录每次清理操作的 `WHERE time < {CUTOFF_DATE}`

### 2.2 实现要点

```sql
-- 软删除（保留 30 天）
DELETE FROM execution_log
WHERE created_at < DATE_SUB(NOW(), INTERVAL 30 DAY) AND deleted_at IS NULL;

-- 硬删除（超过 90 天的软删除数据）
DELETE FROM execution_log
WHERE deleted_at < DATE_SUB(NOW(), INTERVAL 60 DAY);
```

---

## 3. 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 清理任务框架 | 2 |
| 数据保留策略配置 UI | 2 |
| 清理审计日志 | 1 |
| 测试 + 文档 | 1 |
| **总计** | **6** |

---

## 4. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 安全合规 HIGH 转 P2
- GDPR Art.5(1)(e)

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-09-数据保留自动清理策略.md`）**
