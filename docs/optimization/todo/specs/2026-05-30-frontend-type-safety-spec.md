# Spec: 前端类型安全 — Zod 运行时校验

> **编号:** #15 | **严重度:** Medium | **类别:** 前端架构

## Problem

`BizConfig` 有 25+ 可选 `*NodeId` 字段 + `[key: string]: unknown` 索引签名。无运行时校验库。后端 `config` 是 `Record<string, unknown>` 而前端是 `BizConfig`。

**核心问题：**
- 后端 schema 变更 → 前端 `undefined` 访问无感知 → 运行时崩溃
- `bizConfig.sucessNodeId`（typo）编译通过 → 静默 bug
- 无前后端类型契约 → 改一端另一端必破

## Goal

引入 Zod 做 API 响应运行时校验；从 OpenAPI spec 自动生成类型；去除 index signature，用 discriminated union 替代。

## Scope

### In Scope
- Zod schema 定义（BizConfig、CanvasNodeData 等）
- API 响应拦截器中加入 Zod 校验
- 去除 `[key: string]: unknown` index signature
- discriminated union 替代可选字段

### Out of Scope
- OpenAPI 自动生成类型（后续自动化）
- 后端 API 规范化

## Key Files

| File | Change Type | Description |
|------|-------------|-------------|
| `types/canvas.ts` | Modify | Zod schema 替代 type 定义 |
| `types/index.ts` | Modify | discriminated union |
| `services/api.ts` | Modify | 响应拦截器 Zod 校验 |

## Success Criteria

1. API 响应有运行时校验
2. index signature 消除
3. typo 如 `sucessNodeId` 编译期报错
4. 后端 schema 变更有明确错误提示