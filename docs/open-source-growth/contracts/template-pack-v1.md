# Template Pack v1 Contract

日期：2026-06-08

## Scope

Template Pack v1 描述可导入的营销旅程模板。

## Shape

```json
{
  "key": "new-user-welcome",
  "title": "新用户欢迎旅程",
  "category": "Lifecycle",
  "riskLevel": "LOW",
  "requiredPlugins": ["webhook-plugin", "message-plugin", "coupon-plugin"],
  "canvas": {},
  "samplePayload": {},
  "expectedTrace": [],
  "docs": "docs/open-source/templates/new-user-welcome.md"
}
```

## Required Fields

- `key`
- `title`
- `category`
- `riskLevel`
- `requiredPlugins`
- `canvas`
- `samplePayload`
- `expectedTrace`
- `docs`

## Import Rules

- 导入前必须检查 required plugins。
- 插件缺失或禁用时，导入失败并返回明确错误。
- 导入必须创建草稿画布。
- 导入必须复用现有画布创建、保存和版本机制。
- 重复导入必须幂等或生成明确 clone。

## Frontend Rules

- 模板库必须承接 `frontend/src/pages/canvas-list/templateCatalog.ts`。
- 每个模板显示分类、依赖插件、风险等级、说明和导入按钮。
- 模板缺依赖时导入按钮不可用或展示明确阻断原因。

## Tests

至少覆盖：

- 依赖插件启用时导入成功。
- 依赖插件禁用时导入失败。
- 重复导入不会破坏已有画布。
- sample payload dry-run 可产生 expected trace 的关键节点。

## Backend Placement / Owner

Current allowed state:

- `DOCS_ONLY` for template docs, sample payloads, expected traces, and catalog
  content before canvas/execution APIs are stable.
- `CURRENT_ENGINE_BRIDGE` only when the worker packet names the current canvas
  service used, the final DDD owner, idempotency behavior, and bridge removal
  gate.
- `DDD_FINAL_MODULE` after `canvas-context-canvas` exposes draft/version import
  APIs and `canvas-context-execution` exposes dry-run validation APIs.

Final owner:

- Template import and draft creation: `canvas-context-canvas`.
- Plugin dependency and dry-run validation: `canvas-context-execution`.
- Public template docs: `docs/open-source/templates/**`.
- DDD-C07 template boundary: `TemplateValidationPort`.
- DDD-C07 publish definition boundary: `PublishedCanvasDefinition`.
- DDD-C07 dry-run boundary: `ExecutionDryRunFacade`.

Allowed adapters:

- Canvas application service or API facade for draft creation.
- Execution API facade for dry-run and trace validation.

Mirror documents:

- `docs/ddd-rewrite/child-specs/canvas-execution-contract-spec.md`
- `docs/ddd-rewrite/task-packs/07-worker-canvas.md`
- `docs/ddd-rewrite/task-packs/08-worker-execution.md`

Verification:

- Import never writes database tables directly.
- Missing or disabled plugins block import before draft creation.
- At least three templates dry-run through the execution API.
- Template validation checks dependencies before draft creation and calls
  execution only through public validation/dry-run APIs, never adapters.
