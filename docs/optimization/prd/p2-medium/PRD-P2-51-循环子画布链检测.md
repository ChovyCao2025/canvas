# PRD-P2-51-循环子画布链检测

> 本文档为营销画布平台循环子画布检测需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-51 |
| **需求名称** | 循环子画布链检测 |
| **优先级** | P2 |
| **所属类别** | 业务逻辑盲区 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

不检测传递环（如 `大画布 → 子画布A → 子画布A`）。

### 1.2 痛点

- 父画布 → 子画布 A（内部有 GOTO 回到父画布）导致无限循环

---

## 2. 功能需求

### 2.1 核心功能

1. **子画布依赖图**：
   ```java
   Graph<Long> canvasDependencyGraph = new DefaultDirectedGraph<>();
   canvasDependencyGraph.addVertex(rootCanvasId);
   canvasDependencyGraph.addVertex(childAId);
   canvasDependencyGraph.addEdge(rootCanvasId, childAId);

   if (Graphs.hasCycle(canvasDependencyGraph)) {
       throw new CycleDetectedException("Recursive sub-canvas detected");
   }
   ```

2. **UI 依赖图展示**：
   - 画布设计器右侧显示「依赖图」，红色边表示循环依赖

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 依赖图算法 | 2 |
| UI 依赖图展示 | 3 |
| 测试 + 文档 | 1 |
| **总计** | **6** |

---

## 3. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 业务逻辑盲区

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-51-循环子画布链检测.md`）**
