# PRD-P2-50-GOTO迭代上限

> 本文档为营销画布平台 GOTO 迭代上限需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-50 |
| **需求名称** | GOTO迭代上限 |
| **优先级** | P2 |
| **所属类别** | 业务逻辑盲区 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

GOTO 节点可能无限循环（如 `A → GOTO A`）。

### 1.2 痛点

- GOTO 循环导致执行线程池耗尽，系统 DoS

---

## 2. 功能需求

### 2.1 核心功能

1. **循环检测**：
   ```java
   Stack<Long> visitedNodeIds = new Stack<>();
   public boolean canExecute(Long nodeId) {
       if (visitedNodeIds.contains(nodeId)) {
           throw new LoopDetectedException("Detected GOTO loop");
       }
       if (visitedNodeIds.size() > 1000) {  // 防止深度无限
           throw new VisitDepthExceededException("Too many GOTO visits");
       }
       visitedNodeIds.push(nodeId);
       // ... execute
       visitedNodeIds.pop();
       return true;
   }
   ```

2. **警告 UI**：
   - 编辑画布时检测到 GOTO 循环 → 弹出警告 `GOTO: A → A` → 标记红色

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 循环检测 | 2 |
| UI 警告检测 | 2 |
| 测试 + 文档 | 1 |
| **总计** | **5** |

---

## 3. 参考资料

-- BMAD 产品设计审查报告 (2026-05-31) - 业务逻辑盲区

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-50-GOTO迭代上限.md`）**
