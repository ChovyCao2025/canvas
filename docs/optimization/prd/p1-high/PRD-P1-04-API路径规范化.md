# PRD-P1-04-API路径规范化

> 本文档为营销画布平台产品需求文档标准模板，每项缺项对应一份独立 PRD。

---

## 基本信息

| 字段 | 内容 |
|------|------|
| **PRD编号** | PRD-P1-04 |
| **需求名称** | API路径规范化 |
| **优先级** | P1 |
| **所属类别** | API设计 |
| **提出日期** | 2026-05-31 |
| **来源** | BMAD 产品设计审查报告 |
| **竞品对标** | Iterable 标准: `/api/v3/campaigns`, `/api/v3/segments`; Braze: `/kg/v1/events`

---

## 1. 问题描述

### 1.1 现状

- 当前存在 5 种不同路径前缀: `/canvas/`, `/admin/`, `/auth/`, `/meta/`, `/ops/`
- 部分端点归属模糊(如 `/meta/graph` 涉及核心但位于 meta)
- 前端代理配置复杂,包含多个前缀的 bypass 逻辑

### 1.2 痛点

- API 使用者需要记忆不同前缀规则,学习成本高
- Swagger/OpenAPI 文档中路径不统一,编排SDK生成困难
- 第三方集成需要分维护多套调用规范

### 1.3 竞品对标

| 竞品 | 能力 |
|------|------|
| Iterable | REST 严格: `/api/v3/{resource}`, Event: `/kg/v1/{resource}` |
| Braze | HTTP API: `{app_id}/kg/v2/{action}`, 但支持中间代理封装 |
| Stripe | 完全统一前缀: `/v1/{resource}` |

---

## 2. 目标与价值

### 2.1 用户故事

> 作为 API 开发者/集成方，我希望 所有 API 端点遵循统一的前缀规范，以便 减少学习成本，统一编排SDK，降低集成复杂度。

### 2.2 成功指标

- 所有 API 端点迁移完成，前缀统一为 `/api/v3/{module}/{resource}`
- 前端代理 bypass 规则简化，不再依赖 `canvas` 前缀的复杂匹配
- API 访问文档化更新，路径规则统一

### 2.3 不做会怎样

- 集成方需维护多套路径映射，易出不一致
- 路由冗余增加理解和扩展难度
- 前端代理复杂且可能引起路由冲突

---

## 3. 功能需求

### 3.1 核心功能

1. **统一前缀** — 所有 REST API 统一为 `/api/v3/{module}/{resource}`
2. **模块分组** — 按 resource 分组: canvas, execution, event, admin, template
3. **版本管理** — 前缀包含 `/api/v3`，保持版本隔离
4. **向后兼容** — 提供 30 天的 deprecated 路径重定向支持（若需要）

### 3.2 详细描述

- 新前缀规则:
  - `/api/v3/canvases/{id}` → 原 `/canvas/{id}` 或原 `/canvas/api/v1/{id}`?（注：问卷中是前缀混乱，请具体指定）
  - `/api/v3/executions/{id}` → 原 `/canvas/{id}`
  - `/api/v3/events/{action}` → 原 `/events/{action}` 或 `/meta/{action}`
  - `/api/v3/templates/{id}` → 原 `/templates/{id}`
  - `/api/v3/admin/users/{id}` → 原 `/admin/users/{id}`
  - `/api/v3/meta/graph` → 原 `/meta/graph`
  - `/api/v3/ops/monitoring` → 原 `/ops/...`

- Swagger/OpenAPI: 所有 paths 采用统一前缀，带 `x-deprecated` 标记旧端点（可选）

### 3.3 交互流程

1. API 端点迁移时，更新 Spring Boot `@RequestMapping` 的 value 路径
2. 前端 vite.config.ts 中的 bypass 规则简化：删除 `/canvas/*` 等特定映射
3. 集成方接入新端点，文档中列出路径迁移对照表

---

## 4. 非功能需求

- **性能要求**: 路由不引入额外开销，Spring DispatcherServlet 正常执行
- **安全要求**: 确保迁移规则不被用作越权入口，URL 重定向安全返回 301
- **可用性要求**: 路径变更不影响当前线上集成（通过提供约 30 天的兼容层）

---

## 5. 验收标准

- [ ] Swagger 文档中所有 paths 统一使用 `/api/v3/{module}/{resource}` 前缀
- [ ] 前端 vite.config.ts 中不再有 `/canvas/*` 或 `/meta/*` 等分散 bypass 匹配
- [ ] 旧端点标记为 deprecated（可选）
- [ ] 端点映射对照表已发布给集成方

---

## 6. 技术建议

### 6.1 涉及模块

- 后端 API (`application-api`)：更新 `@RequestMapping` 路径
- 前端：vite.config.ts（简化 proxy bypass 规则）
- 文档：Swagger UI/OpenAPI 合并更新

### 6.2 技术要点

- Spring Boot 已通过 `@RequestMapping` 统一编排路由，只需调整各 Controller 的 value
- 路径迁移不影响内部服务接口，仅对外暴露层变更
- 保留旧路径时可选 `@Deprecated`，避免嵌套过多 rewrite 用于兼容（防止升级震荡）

### 6.3 预估工作量

- 端点路径迁移：2 人天
- 文档与迁移说明：1 人天
- 前端代理简化：1 人天

---

## 7. 依赖与风险

### 7.1 前置依赖

- 前端 vite.config.ts 当前路径代理配置熟悉程度
- 主 call flow 集成方对路径使用有完整依赖链掌握

### 7.2 风险

- 路径重命名可能引起集成方未及时迁移的接入失败
- 前端误删部分代理规则导致单页路由（SPA）或 SSR 跳转有问题（但在当前仓库中，前端生产模式下仅 HTML 请求走 index.html 回退）

---

## 8. 参考资料

- BMAD 产品设计审查报告 (2026-05-31)
- 竞品 API 路径设计模式对比
- 当前 application-api Controller 与前端 vite.config.ts 代理配置
