# PRD-P2-52-promoteCanary更新路由

> 本文档为营销画布平台 Canary 更新路由需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-52 |
| **需求名称** | Canary更新路由 |
| **优先级** | P2 |
| **所属类别** | 数据一致性 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

灰度配置变更后路由不可见。

### 1.2 痛点

- 轮转 Canary 画布时，新版本路由未生效，用户持续命中旧版本

---

## 2. 功能需求

### 2.1 核心功能

1. **Canary 路由分发**：
   ```yaml
   canvas.canary.strategy:
     weights:
       mode: "user-ids"
       weights:
         trammers_users: 10%       # 10% 用户切新版
         all_users: 90%            (新画布不支持旧版本)
   ```

2. **路由表刷新**：
   - 检测到 `promoteCanary` 触发后，强制刷新内存路由表（使用 `CountDownLatch` 确保刷新完成）

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| Router 路由刷新 | 3 |
| UI 分流配置 | 2 |
| 测试 + 文档 | 1 |
| **总计** | **6** |

---

## 3. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 数据一致性

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-52-promoteCanary更新路由.md`）**
