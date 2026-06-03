# PRD-P2-20-活动feed及审计日志

> 本文档为营销画布平台活动 Feed 及审计日志需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-20 |
| **需求名称** | 活动feed及审计日志 |
| **优先级** | P2 |
| **所属类别** | 通知协作 P1/P2 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

无活动 Feed 和审计日志。

### 1.2 痛点

- 无法回答「谁在什么时候改了这个画布」
- 画布变更无可追溯，问题排查困难

---

## 2. 功能需求

### 2.1 核心功能

1. **画布变更审计日志表** (`canvas_audit_log`)：
   - `action`: CREATE/UPDATE/DELETE
   - `before_content`: JSON_old
   - `after_content`: JSON_new
   - `changed_by_user`: userId
   - `changed_at`: timestamp

2. **Activity Feed UI**：
   - 右侧侧边栏显示该画布所有变更记录
   - 点击记录可查看前后对比（Diff view）

3. **核心操作审计**：
   - 节点创建/删除/修改
   - 画布发布
   - 画布分享

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 审计日志表开发 | 2 |
| 日志拦截器开发 | 2 |
| Feed UI 开发 | 3 |
| Diff 视图开发 | 2 |
| 测试 + 文档 | 2 |
| **总计** | **11** |

---

## 3. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 通知协作 P1/P2

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-20-活动feed及审计日志.md`）**
