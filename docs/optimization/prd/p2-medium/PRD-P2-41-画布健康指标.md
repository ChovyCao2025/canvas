# PRD-P2-41-画布健康指标

> 本文档为营销画布平台画布健康指标需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-41 |
| **需求名称** | 画布健康指标 |
| **优先级** | P2 |
| **所属类别** | 其他扫描发现 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

仅有原始状态标签，无红黄绿灯综合评分。

### 1.2 痛点

- 无法快速评估画布的健康程度（比如「高风险」和「温和」）

---

## 2. 功能需求

### 2.1 核心功能

1. **健康评分算法**：
   ```yaml
   health_score:
     nodes:
       execution_timeout_rate: 0.3     # 执行超时率（>20% → 扣分）
       business_error_rate: 0.4       # 业务错误率（>15% → 扣分）
       history_success_rate: 0.3      # 历史成功率（<70% → 扣分）
     version:
       failed_in_last_24h: -10        # 最近24h失败 → 扣10分
     resource:
       infrequent_usage: -5           # 一周无使用 → 扣5分
   ```
   - 最终分数：{nodes_score} + {version_score} + {resource_score}

2. **红黄绿灯 UI 组件**：
   - 绿灯：health_score ≥ 80（健康）
   - 黄灯：60 ≤ health_score < 80（警告）
   - 红灯：health_score < 60（危险）

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 评分算法 | 3 |
| UI 红黄绿灯组件 | 2 |
| UI 探测看板 | 2 |
| 测试 + 文档 | 1 |
| **总计** | **8** |

---

## 3. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - 其他扫描发现

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-41-画布健康指标.md`）**
