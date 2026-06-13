# AI Operator Contract

日期：2026-06-08

## Scope

AI Operator 是围绕营销旅程的生成、审计和解释能力，不是直接操作生产状态的自动代理。

## Operators

首版支持：

- Journey generation：自然语言生成 Canvas DSL 草稿。
- Risk audit：识别发布风险。
- Copy generation：生成触达文案和 A/B 版本。
- Trace explanation：解释执行失败。

## Requirements

- 必须支持 mock provider。
- 没有模型 key 也能完成 demo。
- AI 输出必须进入预览或草稿。
- AI 不得直接发布画布。
- AI 不得直接修改已发布版本。
- 发送到外部模型的数据必须先脱敏，并经过人工确认后启用。

## Risk Audit Minimum Rules

至少识别：

- 缺少频控。
- 缺少审批。
- 优惠券节点无上限。
- 触达过密。
- 无失败分支或退出路径。

## Tests

至少覆盖：

- mock provider 生成合法 DSL 草稿。
- 风险审计返回结构化风险。
- trace 解释基于失败节点和错误类型。
- AI 结果不会覆盖已发布画布。

## Backend Placement / Owner

Current allowed state:

- `DOCS_ONLY` for prompt examples, UX copy, mock responses, and release
  narratives before DDD APIs are stable.
- `CURRENT_ENGINE_BRIDGE` only when the worker packet names the old draft,
  risk, or trace service used, the final DDD owner, and the bridge removal gate.
- `DDD_FINAL_MODULE` after draft, risk, and trace APIs are explicit.

Final owner:

- Journey draft generation: `canvas-context-canvas` through DSL/draft APIs.
- Marketing risk audit rules: `canvas-context-marketing` when the rule is
  campaign/product specific.
- Risk decision checks: `canvas-context-risk`.
- Trace explanation input: `canvas-context-execution`.
- AI provider calls: adapter external package in the owning context.
- DDD-C07 journey draft boundary: `AiJourneyDraftProposal`.
- DDD-C07 trace input boundary: `ExecutionTraceView` with string
  `executionId`.

Allowed adapters:

- Mock provider by default.
- External model provider only after secrets, redaction, and human enablement
  are configured outside demo defaults.

Mirror documents:

- `docs/ddd-rewrite/task-packs/03-worker-marketing.md`
- `docs/ddd-rewrite/task-packs/07-worker-canvas.md`
- `docs/ddd-rewrite/task-packs/08-worker-execution.md`

Verification:

- AI output enters preview or draft only.
- AI never publishes or overwrites a published canvas.
- Trace explanation reads execution trace through an execution API, not direct
  persistence access.
- AI journey proposals may reference risk findings and trace IDs, but they must
  not carry published canvas mutation fields.
