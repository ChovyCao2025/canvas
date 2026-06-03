# PRD-P1-02-画布搜索增强

> 本文档为营销画布平台产品需求文档标准模板，每项缺项对应一份独立 PRD。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P1-02 |
| **需求名称** | 画布搜索增强 |
| **优先级** | P1 |
| **所属类别** | API设计 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | Iterable 维度: 支持按发布时间、创建者、标签筛选,SendinBlue 按状态/标签/限定范围搜索 |

---

## 1. 问题描述

### 1.1 现状

- 画布列表仅支持按名称和状态模糊搜索
- 支持的筛选条件: `canvas.name LIKE %keyword%` AND `canvas.status IN (...)`
- 不支持按创建时间、创建者、业务线、标签、状态聚合等维度筛选

### 1.2 痛点

- 当画布数量超过 30 个后,运营人员难以快速定位目标画布
- 无法按"本周发布的画布"或"某业务线"筛选,导致操作效率低下
- 测试/发布历史追溯困难,依赖手动翻页或记忆

### 1.3 竞品对标

| 竞品 | 能力 |
|------|------|
| Iterable | Campaign 列表支持: created_before/after、status、owner_id、段落筛选 |
| Braze | Audiencelist 筛选支持: created_at (RFC3339)、owner、tagging |
| SendinBlue | Campaign Filter: 名称、状态、创建者、带验证的 limited_scope 范围 |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为 运营人员,我希望 在画布搜索框下提供多维度筛选控件并支持高级搜索,以便 快速找到特定历史画布或业务线相关画布,提升日常操作效率。

### 2.2 成功指标

- 页面加载时间 < 1s (含筛选聚合)
- 筛选响应时间 < 300ms (90th percentile)
- 用户找到目标画布的平均点击次数减少 50%

### 2.3 不做会怎样

- 巨量画布场景下列表浏览歧义率高,易误操作
- 数据治理与审计溯源困难
- 团队协作成灰盒状态,无法按权限/业务归属检索

---

## 3. 功能需求

### 3.1 核心功能

1. **基础搜索** — 名称模糊匹配 + 状态过滤(保持现有功能)
2. **高级筛选** — 新增按创建时间、创建者、业务线、标签、分层/归档可选过滤
3. **范围限定** — 支持按"项目""空间""时间维度"限定搜索范围
4. **历史快照** — 支持按发布日期归档与筛选(归档画布仍可检索)

### 3.2 详细描述

- UI 布局: 在搜索框下方展开「高级筛选」,默认折叠
- 筛选项:
  - 创建时间(开始/结束选择器,示例: 2026-01-01 ~ 2026-06-01)
  - 创建者(user.UUID 前端可选配置为 user_id/open_id/union_id)
  - 业务线(下拉多选或自定义标签)
  - 标签(Intent-based 分类)
  - 私有/Public 权限范围
  - 归档状态(DRAFT/ARCHIVED)
- 端点新增: `GET /api/v3/canvases?status={status}&name={keyword}&createdAt_start={epoch}&createdAt_end={epoch}&owner_id={uuid}&business_line={tags}&limit={limit}&offset={offset}&page_token={page_token}`
- 前端筛选: 表单控件触发 query 更新,状态 persistence via URL query params

### 3.3 交互流程

1. 在画布列表页展开「高级筛选」
2. 选择创建时间(近7天/近30天快捷按钮或自定义范围)
3. 多选创建者(支持搜索)
4. 多选业务线标签(前端从 `/api/v3/teams` 或空间元数据映射到 business_line)
5. 筛选结果分页加载,每页 20 条,支持保存筛选模板快速调用

---

## 4. 非功能需求

- **性能要求**: 聚合筛选聚合(如按 business_line 分组计数)不在搜索延迟主路径上
- **安全要求**: 筛选条件不暴露敏感数据,owner_id 场景下只返回创建者的 ID
- **可用性要求**: 筛选状态可通过 URL 字符串 share,团队成员基于同一粘性参数可获得有限视图

---

## 5. 验收标准

- [ ] 创建时间范围选择器 UI 存在且工作正常
- [ ] 选择"过去 30 天"后默认聚合为 30 条结果
- [ ] owner_id 在前端展示为用户名,API 返回枚举值: [userId, openId, unionId]
- [ ] 筛选可保存为模板,下次切到该模板一键应用
- [ ] 未授权用户访问 key 与 owner_id 集成时只能查看自己的创建项

---

## 6. 技术建议

### 6.1 涉及模块

- 前端: `src/pages/canvas/canvas-list.tsx`
- 后端 API: 新增 `CanvasQueryVO` DTO,扩展现有 query parameters
- 权限校验: 利用现有 RBAC 模块

### 6.2 技术要点

- name/owner_id/business_line 与 canvas 表关联时使用 MyBatis-Plus `Wrapper<LambdaQueryWrapper<Canvas>>`
- 时间范围筛选: 数据库层面按 `created_at` 索引,HikariCP + MySQL
- owner_id 字段对 creator 字段的正交字段映射
- 分页使用 MySQL Server eller `limit={limit}&offset={offset}`,前端 page_token 传递避免深翻页性能下降

---

## 7. 依赖与风险

### 7.1 前置依赖

- 前端技术栈: Ant Design 表单组件
- 后端基础 API 层已支持钻取滑动窗口

### 7.2 风险

- 不完全设计下 owner_id/user_id 与 user 表关联可能导致线上问题
- 复合索引设计未尽早裁决可能引起执行计划波动
- 用户期望 vs 数据质量语义对齐有偏差

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31)
- Iterable 竞品 Campaign 列表搜索实现
- 当前 canvas-list 前端实现
