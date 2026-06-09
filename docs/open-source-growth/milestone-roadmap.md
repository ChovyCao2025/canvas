# Open Source Growth Milestone Roadmap

日期：2026-06-08

## 总周期

周期：2026-06-08 到 2026-12-08。

目标：在半年内把 Canvas 打造成可对外发布的开源营销自动化平台。

## Month 1：开源入口和 Demo

### 目标

让新用户 5 分钟看到价值。

### 交付物

- README 首屏重写。
- `LICENSE`、`CONTRIBUTING.md`、`CODE_OF_CONDUCT.md`、`SECURITY.md`。
- Issue 和 PR 模板。
- `docker-compose.demo.yml`。
- demo profile。
- 3 个示例旅程。
- quickstart 文档。

### 退出条件

- 新用户只看 README 就知道项目定位。
- demo compose 可以启动。
- 默认账号登录后能看到示例旅程。
- 示例旅程可 dry-run。

### 主要风险

- 启动依赖过重。
- README 继续保留过多部署细节。
- 示例旅程依赖真实外部系统。

### 控制策略

- 外部系统全部 mock。
- 部署细节下沉到 docs。
- README 聚焦定位、demo、截图、模板和插件。

## Month 2：轻量插件体系

### 目标

建立扩展点，让外部开发者知道如何贡献节点和 Provider。

### 交付物

- 插件 manifest。
- `CanvasPlugin` 接口。
- 扩展现有 `PluginRegistryService`，形成插件注册能力。
- 插件权限声明。
- HandlerRegistry 插件接入。
- 2 个官方插件可用：webhook、message。

### 退出条件

- 插件可发现、可启停、可校验。
- 重复 node type 会启动失败。
- demo 中能使用插件节点。

### 主要风险

- 过早做运行时热加载。
- 插件权限模型过度复杂。

### 控制策略

- 只做构建期插件。
- 权限只做声明和校验，不做复杂沙箱。

### DDD Coordination Gate

插件后端实现必须等待 `OSG-C07` 和 G10 public extension/API stability gate。
roadmap 交付物不是后端写权限；实际写范围以
`docs/program-coordination/subagent-worker-packets.md` 和
`docs/program-coordination/progress-ledger.md` 的 dispatch row 为准。

## Month 3：模板库和配置面板

### 目标

让业务模板成为对外传播入口，让插件节点可低成本配置。

### 交付物

- Schema-driven 配置面板。
- Template Pack 模型。
- TemplatePackRegistry。
- TemplateImportService。
- 10 个官方模板。
- 模板库前端展示。

### 退出条件

- 简单插件节点只靠 schema 可配置。
- 10 个模板可展示。
- 至少 3 个模板可导入并 dry-run。

### 主要风险

- 模板质量低，只是 JSON 样例。
- 配置面板抽象过度，影响已有复杂节点。

### 控制策略

- 每个模板必须有业务说明、依赖插件、示例 payload、预期 trace。
- 复杂节点保留自定义配置面板。

### DDD Coordination Gate

模板内容、文档和前端 catalog 可以先做。`TemplateImportService` 后端导入、
dry-run 以及执行依赖校验必须等待 canvas/execution API 通过 G10，并且必须
声明 final owner module 或完整 `CURRENT_ENGINE_BRIDGE`。

## Month 4：Canvas DSL 和 CLI

### 目标

建立 MarketingOps as Code 的开发者入口。

### 交付物

- Canvas DSL v1。
- DSL validate/import/export/diff API。
- `tools/canvas-cli`。
- CLI 支持 validate/import/export/diff/publish。
- DSL 文档和示例。

### 退出条件

- YAML 可导入为画布草稿。
- 画布可导出为 YAML。
- CLI 能在 demo 环境跑通。
- DSL diff 能识别节点和边变化。

### 主要风险

- DSL 试图覆盖所有复杂节点。
- CLI 和后端 API 契约不稳定。

### 控制策略

- DSL 首版只覆盖 trigger、condition、message、coupon、approval、ai、webhook。
- API 和 DSL 都标记 `canvas/v1`。

### DDD Coordination Gate

DSL/CLI 的本地 validate/diff 可以先做。CLI import/export/publish 和 DSL
backend API 必须等待 `canvas-context-canvas`、`canvas-context-execution` 和
`canvas-web` 的公共 API 通过 G10；禁止直接调用内部 module class 或旧
`canvas-engine` service。

## Month 5：AI-native 旅程体验

### 目标

让 AI 成为项目差异化，而不是普通节点。

### 交付物

- AI mock provider。
- 自然语言生成 Journey DSL 草稿。
- 旅程风险审计。
- 文案生成和 A/B 变体。
- Trace 失败解释。
- 前端 AI 助手。

### 退出条件

- 没有真实模型 key 也能演示。
- AI 能生成 3 类常见旅程草稿。
- 风险审计能识别 5 类风险。
- Trace 失败解释能输出结构化结果。

### 主要风险

- AI 输出不可控。
- 真实模型依赖影响 demo。

### 控制策略

- 先 mock，再接真实 provider。
- AI 生成结果必须进入预览，不直接写入生产画布。

## Month 6：Playground 和发布包装

### 目标

完成对外发布准备。

### 交付物

- 本地 Playground 模式。
- Playground 部署文档。
- 英文 quickstart。
- 英文插件开发指南。
- 英文模板指南。
- Canvas vs Mautic vs n8n 对比文章。
- 3 篇发布文章草稿。

### 退出条件

- 完成一条公开演示路径：模板 -> dry-run -> trace -> export DSL -> AI 审计。
- 英文文档可以支撑外部开发者试用。
- 对外发布材料齐全。

### 主要风险

- 最后一个月才发现 demo 不稳定。
- 文档和真实体验不一致。

### 控制策略

- 每个月都跑 demo 验收。
- 文档命令必须在 demo profile 验证。

## 半年后版本建议

### v0.1 Open Source Demo

发布时间：Month 1 结束。

包含：

- README。
- 一键 demo。
- 3 个模板。
- mock provider。

### v0.2 Plugin and Template Ecosystem

发布时间：Month 3 结束。

包含：

- 插件 manifest。
- 官方插件。
- schema 配置面板。
- 10 个模板。

### v0.3 MarketingOps as Code and AI

发布时间：Month 6 结束。

包含：

- Canvas DSL。
- CLI。
- AI 生成和审计。
- Playground。
- 英文文档。
