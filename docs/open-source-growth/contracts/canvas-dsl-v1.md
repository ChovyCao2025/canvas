# Canvas DSL v1 Contract

日期：2026-06-08

## Scope

Canvas DSL v1 用于 demo、模板、CLI 和 AI 草稿，不取代当前运行时 graph JSON 存储。

## Supported Node Set

首版只支持：

- `webhook`
- `condition`
- `message`
- `coupon`
- `approval`
- `ai`
- `risk-check`
- `end`

其他历史节点不在 v1 范围内。

## YAML Shape

```yaml
apiVersion: canvas/v1
kind: Journey
metadata:
  name: new-user-welcome
  title: 新用户欢迎旅程
spec:
  trigger:
    type: webhook
    event: user.registered
  nodes:
    - id: segment
      type: condition
      config:
        expression: "user.level == 'new'"
  edges:
    - from: segment
      to: end
```

## Required Semantics

- `apiVersion` 必须为 `canvas/v1`。
- `kind` 必须为 `Journey`。
- `metadata.name` 必须稳定、可用于模板 key。
- `nodes[].id` 必须唯一。
- `edges[].from` 和 `edges[].to` 必须引用已定义节点。
- 导入时必须执行 DAG 校验。
- 导出时可以包含坐标，但坐标不是 v1 必填语义。

## CLI Operations

- `validate`
- `import`
- `export`
- `diff`
- `publish`

CLI 必须调用后端 API，不得直接写数据库。

## Compatibility

DSL v1 可以表达 demo 和模板的 Golden Path。不能表达的节点必须保留在 graph JSON 中，不得强制降级。

## Backend Placement / Owner

Current allowed state:

- `DOCS_ONLY` for DSL field definitions, examples, and local validation rules.
- `CURRENT_ENGINE_BRIDGE` only when the worker packet names the old canvas API
  used as a temporary adapter, the final DDD owner, and the bridge removal gate.
- `DDD_FINAL_MODULE` after `canvas-context-canvas` owns import/export mapping
  and `canvas-web` exposes stable HTTP endpoints.

Final owner:

- DSL document model and mapping to draft canvas: `canvas-context-canvas`.
- Runtime validation for enabled node types and dry-run: `canvas-context-execution`.
- HTTP endpoints: `canvas-web`.
- CLI package: `tools/canvas-cli`.
- DDD-C07 canvas publication boundary: `PublishedCanvasDefinition`,
  `PublishedCanvasNodeDefinition`, and `PublishedCanvasEdgeDefinition`.
- DDD-C07 execution validation boundary: `NodeMetadataView`,
  `PluginEnablementView`, and `ExecutionDryRunFacade`.

Allowed adapters:

- Canvas application/API facade for import/export.
- Execution API facade for node type and dependency validation.
- `canvas-web` controller for CLI-facing endpoints.

Mirror documents:

- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`
- `docs/ddd-rewrite/task-packs/07-worker-canvas.md`
- `docs/ddd-rewrite/task-packs/08-worker-execution.md`
- `docs/ddd-rewrite/task-packs/09-coordinator-web-boot-cutover.md`

Verification:

- CLI does not write database tables directly.
- Backend import/export does not bind final implementation to old
  `CanvasService` internals.
- DSL v1 unsupported nodes remain in graph JSON and are not downgraded.
- DSL import/export maps to the existing `graphJson` shape and does not replace
  full graph JSON semantics in the first public contract.
