# PRD-P2-54-OFFLINE归档悬空引用

> 本文档为营销画布平台 OFFLINE 归档悬空引用需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-54 |
| **需求名称** | OFFLINE归档悬空引用 |
| **优先级** | P2 |
| **所属类别** | 数据一致性 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

版本行无软删除保护。

### 1.2 痛点

- 执行历史记录引用的版本行可被删除，导致「版本行找不到 ERROR 404」

---

## 2. 功能需求

### 2.1 核心功能

1. **软删除保护**：
   ```sql
   -- 即物理删除前写入快照
   INSERT INTO canvas_version_snapshot
   (version_id, canvas_id, deleted_at, creator_id, created_at)
   VALUES (?, ?, NOW(), ?, NOW());

   DELETE FROM canvas_version
   WHERE id = ? AND deleted_at IS NULL;
   ```

2. **审计日志**：
   - 记录 `delete canvas_version { id: 123, snapshot_id: 456 }`
   - 用于数据恢复（从 snapshot 恢复）

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 软删除表快照 | 2 |
| 删除逻辑改造 | 2 |
| UI 审计日志 | 1 |
| 测试 + 文档 | 1 |
| **总计** | **6** |

---

## 3. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 数据一致性

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-54-OFFLINE归档悬空引用.md`）**
