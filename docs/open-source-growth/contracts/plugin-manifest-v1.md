# Plugin Manifest v1 Contract

日期：2026-06-08

## Scope

Plugin Manifest v1 描述构建期插件的身份、兼容性、扩展点、权限、节点和模板。

## JSON Shape

```json
{
  "id": "canvas-plugin-coupon",
  "name": "Coupon Plugin",
  "version": "0.1.0",
  "canvasCoreVersion": ">=0.1.0 <1.0.0",
  "extensionPoints": ["node-handler", "coupon-provider", "template-pack"],
  "permissions": ["coupon:grant", "execution:write-context"],
  "nodes": ["COUPON_GRANT"],
  "templates": ["coupon-approval-release"],
  "configSchema": {
    "type": "object",
    "properties": {}
  }
}
```

## Required Fields

- `id`：小写字母、数字和中划线，必须全局唯一。
- `name`：展示名称。
- `version`：语义化版本。
- `canvasCoreVersion`：兼容的 Canvas core 版本范围。
- `extensionPoints`：插件使用的扩展点。
- `permissions`：插件声明的能力权限。
- `nodes`：插件贡献的节点类型。
- `templates`：插件贡献或依赖的模板 key。

## Permission Vocabulary

首版权限：

- `http:external-call`
- `execution:write-context`
- `message:send`
- `coupon:grant`
- `approval:create`
- `webhook:register`
- `profile:read`
- `ai:generate`

## Validation Rules

- `id` 必须唯一。
- `nodes` 中的节点类型必须唯一。
- 所有权限必须来自权限词表。
- 插件声明的节点必须有对应 handler 或 schema。
- 插件禁用后，依赖插件的模板不能导入。

## Storage

首版优先承接：

- `PluginRegistryService`
- `JdbcPluginRepository`
- `built_in_plugin_registry`

manifest 可先映射进现有表字段；如果字段不足，通过新 Flyway migration 扩展表，不修改旧迁移。

## Backend Placement / Owner

Current allowed state:

- `DOCS_ONLY` for manifest examples, permission vocabulary, and compatibility
  documentation before DDD execution/platform ownership is ready.
- `CURRENT_ENGINE_BRIDGE` only when the worker packet explicitly names
  `PluginRegistryService`, `JdbcPluginRepository`, `HandlerRegistry`, the final
  DDD owner, and the bridge removal gate.
- `DDD_FINAL_MODULE` after `canvas-platform` owns plugin registry metadata and
  `canvas-context-execution` owns handler binding and node metadata.

Final owner:

- Registry metadata and enablement: `canvas-platform`.
- Handler binding and node metadata: `canvas-context-execution`.

Allowed adapters:

- Persistence adapter for registry storage.
- Execution adapter/config for Spring Bean discovery and handler binding.

Mirror documents:

- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`
- `docs/ddd-rewrite/task-packs/01-worker-platform.md`
- `docs/ddd-rewrite/task-packs/08-worker-execution.md`

Verification:

- No second plugin registry exists.
- `PluginRegistryService` is extended or replaced only through a named DDD
  final module decision.
- Disabled plugins block dependent template import or publish validation.
