# Open Source Growth Traceability Matrix

日期：2026-06-08

## 目的

本矩阵把专项目标映射到实施任务、验证证据和阶段门禁。任何 Open Source Growth 相关 PR 都必须引用至少一个 `OSG-*` 编号。没有编号映射的改动默认不纳入本专项。

## 使用规则

- PR 描述必须包含 `OSG-*` 编号。
- 每个编号必须能追溯到 spec、plan、测试和手工 demo 证据。
- 如果实现过程中新增范围，必须先新增矩阵条目，再实施。
- 如果某项被延期，必须在 [decision-log.md](./decision-log.md) 记录原因和恢复条件。

后端相关 `OSG-*` 条目还必须在 PR 或 evidence 中记录：

```text
Target backend state:
worker packet ID:
final owner module:
bridge removal gate:
progress-ledger.md dispatch row:
```

如果 `Target backend state` 是 `CURRENT_ENGINE_BRIDGE`，还必须记录 exact old
service/API、exact old files、idempotency rule 和 rollback path。只满足旧
`canvas-engine` diff 或单个测试，不足以通过本矩阵。

## Matrix

| ID | Requirement | Spec Source | Plan Source | Required Evidence | Phase Gate |
|---|---|---|---|---|---|
| OSG-ENTRY-001 | README 首屏说明项目定位、快速启动、插件、模板和 demo 入口 | `open-source-growth-spec.md` 3, 9.1 | Task 1 | `README.md` 前 120 行；`docs/open-source/quickstart.md`；链接检查 | Month 1 |
| OSG-ENTRY-002 | 开源社区基础文件齐全 | `open-source-growth-spec.md` 3, 9.1 | Task 1 | `LICENSE`、`CONTRIBUTING.md`、`CODE_OF_CONDUCT.md`、`SECURITY.md`、issue/PR 模板 | Month 1 |
| OSG-DEMO-001 | 一键 demo 可启动并登录 | `open-source-growth-spec.md` 7.1, 9.2 | Task 2 | `docker-compose.demo.yml config`；demo profile；默认账号登录验证 | Month 1 |
| OSG-DEMO-002 | 示例旅程可 dry-run 并展示 trace | `open-source-growth-spec.md` 5.4, 9.2 | Task 2, Task 6 | 至少 3 个模板 dry-run 记录；执行 trace 截图或测试证据 | Month 1, Month 3 |
| OSG-PLUGIN-001 | 插件注册必须承接现有 `PluginRegistryService` 语义并落到 OSG-C07 final owner split | `implementation-guardrails.md` 3.2 | Task 3 | `canvas-platform` registry metadata/enablement evidence；old `PluginRegistryService` bridge or migration declaration；无平行注册中心；G10 plugin tests | Month 2 |
| OSG-PLUGIN-002 | 插件 handler 必须兼容 `NodeHandler` 和 `@NodeHandlerType` 并通过 execution-owned registry 绑定 | `implementation-guardrails.md` 3.3 | Task 3, Task 4 | `NodeHandlerRegistry`/bridge 兼容测试；禁用插件后发布校验失败 | Month 2 |
| OSG-PLUGIN-003 | 插件 manifest 有权限、版本、扩展点和节点声明 | `open-source-growth-spec.md` 7.2 | Task 3 | `plugin-manifest-v1.md`；manifest 校验测试；迁移或内置数据 | Month 2 |
| OSG-PLUGIN-004 | 6 个官方插件在 demo profile 可用 | `open-source-growth-spec.md` 3, 9.3 | Task 4 | 插件列表 API；每个插件文档；每个插件至少一个测试 | Month 2, Month 3 |
| OSG-TEMPLATE-001 | Template Pack 扩展现有模板能力 | `open-source-growth-spec.md` 7.3 | Task 6 | `TemplateRenderService` 复用说明；`TemplateImportServiceTest` | Month 3 |
| OSG-TEMPLATE-002 | 10 个官方模板可展示和导入 | `open-source-growth-spec.md` 7.3, 9.4 | Task 6 | `templateCatalog.ts`；模板依赖校验；导入测试 | Month 3 |
| OSG-CONFIG-001 | 简单插件节点支持 schema-driven 配置 | `open-source-growth-spec.md` 5.3, 7.2 | Task 5 | `SchemaConfigPanel` 测试；复杂节点保留自定义面板 | Month 3 |
| OSG-DSL-001 | Canvas DSL v1 支持 validate/import/export/diff | `open-source-growth-spec.md` 7.4, 9.5 | Task 7 | DSL contract；`CanvasDslValidatorTest`；`CanvasDslMapperTest` | Month 4 |
| OSG-CLI-001 | CLI 支持 validate/import/export/diff/publish | `open-source-growth-spec.md` 7.5 | Task 8 | `tools/canvas-cli` 测试；`canvas --help`；demo API 联调 | Month 4 |
| OSG-AI-001 | AI mock 支持旅程生成、风险审计、失败解释 | `open-source-growth-spec.md` 7.6, 9.6 | Task 9 | mock provider 测试；`JourneyRiskAuditServiceTest`；前端 AI 助手测试 | Month 5 |
| OSG-AI-002 | AI 输出只进入预览或草稿，不直接修改已发布画布 | `implementation-guardrails.md` 5.5 | Task 9 | Controller/service 测试；前端预览确认流 | Month 5 |
| OSG-PLAYGROUND-001 | Playground 跑通模板、dry-run、trace、DSL 导出、AI 审计 | `open-source-growth-spec.md` 7.7 | Task 10 | Golden Path 记录；Playground 文档；demo compose 验证 | Month 6 |
| OSG-DOCS-001 | 英文文档和发布材料可支撑外部试用 | `open-source-growth-spec.md` 11 | Task 11 | 英文 quickstart、插件指南、模板指南、竞品对比文章 | Month 6 |
| OSG-GUARD-001 | 防偏护栏自动化检查可运行 | `implementation-guardrails.md` 7, 8 | This guardrail extension | `node --test tools/open-source-growth/guardrail-verifier.test.mjs`；`node tools/open-source-growth/guardrail-verifier.mjs` | All Months |

## PR Mapping Template

```markdown
Traceability:
- Requirement: OSG-PLUGIN-001
- Spec: docs/open-source-growth/open-source-growth-spec.md
- Plan: docs/open-source-growth/open-source-growth-plan.md Task 3
- Gate: docs/open-source-growth/phase-gates.md Month 2
- Target backend state: DDD_FINAL_MODULE or CURRENT_ENGINE_BRIDGE
- Worker packet ID: OSG-W07A
- Final owner module: canvas-platform for registry metadata/enablement; canvas-context-execution for handler binding/node metadata/runtime validation
- Bridge removal gate: G12 if CURRENT_ENGINE_BRIDGE is used
- Progress ledger row: docs/program-coordination/progress-ledger.md dispatch id
- Evidence: focused test command and demo step
```

## Completion Rule

专项不能只凭“文档写完”或“测试通过”宣称完成。每个 `OSG-*` 条目必须同时满足：

- 对应代码或文档存在。
- 对应测试或手工证据存在。
- 对应 phase gate 已通过。
- 对应 contract 没有被破坏。
