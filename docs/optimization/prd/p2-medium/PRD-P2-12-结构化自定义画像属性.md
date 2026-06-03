# PRD-P2-12-结构化自定义画像属性

> 本文档为营销画布平台结构化画像属性管理需求文档。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P2-12 |
| **需求名称** | 结构化自定义画像属性 |
| **优先级** | P2 |
| **所属类别** | CDP/人群管理 MEDIUM |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |

---

## 1. 问题描述

### 1.1 现状

用户画像使用自由 JSON 存储，字段无约束，无法在前端快速管理。

### 1.2 痛点

- 无预制 UI 面板管理画像字段（无法拖拽添加/删除属性）
- 字段定义不一致（如同时使用 `gender: 'male'` 和 `gender: '男'`）
- 无法通过查询构建人群（如 `WHERE customer_type = 'vip'`）

### 1.3 竞品对标

| 竞品 | 能力 |
|------|------|
| Braze | PropSync 支持自定义属性类型（枚举/标签/数字/时间） |
| Iterable | Audience Builder 支持 Profile Schema |

---

## 2. 功能需求

### 2.1 核心功能

1. **画像字段定义表** (`user_profile_schema`)：
   - `field_key`: 客户属性标识（如 `customer_type`）
   - `field_name`: 显示名称（如「客户类型」）
   - `field_type`: 枚举（text/enum/multi-select/number/date）
   - `options`: 枚举字段的选项（如 `{vip, normal, gray}`）
   - `is_required`: 是否必填

2. **前端字段管理 UI**：
   - 可在「画布编辑器侧边栏」添加画像字段
   - 支持拖拽排序、编辑选项

3. **原生查询支持**：
   - `WHERE customer_type = 'vip'`
   - `WHERE order_count > 10`
   - `WHERE registration_date BETWEEN '2026-01-01' AND '2026-06-01'`

### 2.2 工作量

| 任务 | 工作量（人天） |
|------|--------------|
| 字段定义表开发 | 3 |
| 查询构建 UI 开发 | 4 |
| 字段管理 UI 开发 | 3 |
| 文档 + 测试 | 2 |
| **总计** | **12** |

---

## 3. 参考资料

- BMAD 产品设计审查报告 (2026-05-31) - CDP/人群管理 MEDIUM
- Braze PropSync 能力

---

**（文档已保存到 `/Users/photonpay/project/canvas/docs/optimization/prd/p2-medium/PRD-P2-12-结构化自定义画像属性.md`）**
