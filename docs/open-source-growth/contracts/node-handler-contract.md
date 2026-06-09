# Node Handler Contract

日期：2026-06-08

## Scope

本契约定义插件节点如何接入现有执行引擎。

## Existing Integration Point

当前执行引擎通过以下机制发现节点处理器：

- `NodeHandler`
- `@NodeHandlerType`
- `HandlerRegistry`
- `node_type_registry`

插件节点必须承接该机制。

## Requirements

- 插件节点必须实现 `NodeHandler`。
- 插件节点必须声明唯一 `@NodeHandlerType`。
- 插件节点 type 必须与 `node_type_registry.type_key` 或插件 manifest 中的节点声明一致。
- 插件节点必须通过 Spring Bean 注册。
- `HandlerRegistry` 是唯一运行时 handler 获取入口。
- 插件被禁用时，依赖该插件的节点不能通过发布校验。
- 插件被禁用时，已有草稿可以保留，但不能发布或 dry-run。

## Forbidden

- 绕过 `HandlerRegistry` 直接执行插件 handler。
- 在插件中直接修改画布版本状态。
- 在插件中绕过执行 trace 写入。
- 在插件中绕过权限或租户上下文。
- 运行时 jar 热加载。

## Tests

至少覆盖：

- 重复 node type 启动失败。
- 未启用插件的节点发布失败。
- 启用插件的节点能被 `HandlerRegistry` 获取。
- 插件 handler 异常能进入现有执行失败路径。

## Backend Placement / Owner

Current allowed state:

- `DOCS_ONLY` for handler contract examples and plugin development
  documentation before execution extension points compile.
- `CURRENT_ENGINE_BRIDGE` only when the worker packet names existing
  `NodeHandler`, `NodeHandlerType`, `HandlerRegistry`, the final DDD owner, and
  the bridge removal gate.
- `DDD_FINAL_MODULE` after `canvas-context-execution` owns handler discovery,
  handler enablement, node metadata, dry-run validation, and trace integration.

Final owner:

- Runtime handler registry and dispatch: `canvas-context-execution`.
- Plugin enablement metadata: `canvas-platform` for registry state, consumed by
  `canvas-context-execution`.
- Public HTTP exposure of node metadata: `canvas-web`.

Allowed adapters:

- Execution config/adapter package for Spring Bean discovery.
- Execution application/API facade for publish validation and dry-run.
- No direct plugin handler access from canvas, web, or template code.

Mirror documents:

- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`
- `docs/ddd-rewrite/task-packs/08-worker-execution.md`
- `docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md`

Verification:

- `HandlerRegistry` remains the only runtime lookup path.
- Disabled plugin nodes cannot publish or dry-run.
- Handler failures enter execution trace through execution-owned failure paths.
