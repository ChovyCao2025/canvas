# Spec: React Flow → AntV X6 工作流编辑器迁移

> **编号:** I | **严重度:** Medium | **迁移难度:** Hard

## Problem

React Flow 是通用节点图编辑器，不是工作流编辑器。

**核心问题：**
1. 30+ 自定义节点类型各有独立配置面板、校验规则、视觉表达，在 React Flow 节点内管理笨重
2. 50+ 节点条件分支、循环、跳转边，React Flow 默认边路由处理不了密集 DAG 边交叉/重叠
3. 100+ 节点时 UI 卡顿（任何变更重渲染所有可见节点）
4. 无工作流语义（节点类型校验、边约束、工作流级校验需从零实现）

## Goal

迁移到 `@antv/x6`（与 antd 生态一致）或基于 HTML5 Canvas/SVG 自研。

## Scope

### In Scope
- @antv/x6 集成和评估
- 自定义节点渲染迁移
- 边路由和布局引擎迁移
- 工作流校验框架

### Out of Scope
- 前端状态管理重构（问题 #13）
- 边双源真相修复（问题 #10）

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| 全部 React Flow 组件 | Rewrite | X6 组件 |
| `package.json` | Modify | 替换依赖 |

## Success Criteria

1. 30+ 自定义节点类型全部迁移
2. 50+ 节点 DAG 边路由清晰无重叠
3. 100+ 节点画布无卡顿
4. 工作流级校验内置