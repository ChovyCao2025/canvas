# Open Source Growth Phase Gates

日期：2026-06-08

## 目的

本文件定义每个月结束前必须通过的门禁。未通过当前 Month gate，不得进入下一个 Month 的实现。门禁失败时只能修复当前阶段，不能通过扩大范围来绕过失败。

## 通用门禁

每个阶段都必须满足：

- PR 引用了 [traceability-matrix.md](./traceability-matrix.md) 中的 `OSG-*` 编号。
- 执行前写过 mini-spec，且说明复用的现有实现。
- 未修改已应用 Flyway 迁移。
- 未提交 `target/`、`dist/`、`node_modules/`。
- 未引入真实 secret。
- 未把 demo mock 配置混入 production profile。
- 运行 `bash docs/program-coordination/checks/program-coordination-checks.sh .` 通过。
- 运行 `node tools/open-source-growth/guardrail-verifier.mjs` 通过。
- 涉及 backend 的任务必须在 mini-spec 中声明 `Target backend state`：
  `DOCS_ONLY`、`CURRENT_ENGINE_BRIDGE` 或 `DDD_FINAL_MODULE`。
- 若为 `CURRENT_ENGINE_BRIDGE`，必须声明最终 DDD owner module 和迁移退出条件。

## Month 1 Gate：Open Source Entry and Demo

### Scope

- README 和开源入口。
- 社区基础文件。
- demo compose 和 demo profile。
- 3 个示例旅程。

### Required Evidence

- `README.md` 前 120 行说明定位、快速启动、插件、模板和 demo。
- `LICENSE` 存在，且 license 类型经过人工确认。
- `CONTRIBUTING.md`、`CODE_OF_CONDUCT.md`、`SECURITY.md` 存在。
- `.github/ISSUE_TEMPLATE/` 和 `.github/pull_request_template.md` 存在。
- `docker compose -f docker-compose.demo.yml config` 通过。
- 默认账号可登录 demo。
- 至少 3 个示例旅程可 dry-run。

### Stop Conditions

- README 仍以部署手册为首屏。
- demo 依赖真实短信、邮件、审批、优惠券或 AI key。
- 为 demo 绕过真实执行引擎。

## Month 2 Gate：Plugin Registry

### Scope

- 插件 manifest。
- 插件权限声明。
- 插件启停。
- 插件 handler 与 `HandlerRegistry` 兼容。
- 至少 2 个官方插件 skeleton。

### Required Evidence

- OSG-C07 owner split 已被实现或具名桥接：`canvas-platform` owns registry
  metadata/manifest/permissions/enablement，`canvas-context-execution` owns
  handler binding/node metadata/runtime validation。
- 旧 `PluginRegistryService` 语义被复用、迁移或通过完整
  `CURRENT_ENGINE_BRIDGE` declaration 保留。
- 没有新建第二套平行插件注册中心。
- 插件 manifest contract 已更新。
- 禁用插件后，依赖该插件的模板导入或发布校验失败。
- 插件 handler 仍通过 `NodeHandler`、`@NodeHandlerType` 和 execution-owned
  handler registry 接入。
- G10 聚焦后端测试通过。

### Stop Conditions

- 引入 `URLClassLoader`、PF4J 或运行时 jar 热加载。
- 插件绕过 `HandlerRegistry` 执行。
- 插件启停只影响 UI，不影响后端校验。

## Month 3 Gate：Template Pack and Schema Config

### Scope

- Template Pack。
- 10 个官方模板。
- Schema-driven 配置面板。
- 前端模板库。

### Required Evidence

- 模板导入复用现有画布创建、保存、版本机制。
- `templateCatalog.ts` 被承接而不是被绕开。
- 每个模板声明依赖插件、示例 payload 和风险等级。
- 至少 10 个模板可展示。
- 至少 6 个模板可导入。
- 至少 3 个模板可 dry-run 并展示 trace。
- 简单插件节点可以只靠 JSON Schema 配置。

### Stop Conditions

- 一次性迁移全部复杂节点配置面板。
- 删除复杂节点现有自定义面板。
- 模板导入直接写数据库绕过服务。

## Month 4 Gate：Canvas DSL and CLI

### Scope

- Canvas DSL v1。
- validate/import/export/diff。
- CLI 本地工具。

### Required Evidence

- [contracts/canvas-dsl-v1.md](./contracts/canvas-dsl-v1.md) 已冻结首版字段。
- DSL v1 只覆盖首版节点集合。
- 合法 DSL 可以导入为草稿画布。
- 现有画布可以导出 DSL。
- CLI 不直接写数据库，只调用后端 API。
- CLI validate 和 diff 测试通过。

### Stop Conditions

- DSL 试图覆盖所有历史节点。
- DSL 取代运行时真实 graph 存储。
- CLI 绕过 API 直接访问数据库。

## Month 5 Gate：AI-native Journey Operations

### Scope

- AI mock provider。
- 自然语言生成 DSL 草稿。
- 风险审计。
- 文案生成。
- Trace 失败解释。

### Required Evidence

- 没有模型 key 也能演示。
- AI 生成结果进入预览或草稿。
- AI 不直接修改已发布画布。
- 风险审计至少覆盖 5 类风险。
- Trace 失败解释基于真实 execution trace。

### Stop Conditions

- AI 自动发布画布。
- AI 自动覆盖已发布版本。
- 未脱敏用户数据发送给外部模型。

## Month 6 Gate：Playground and Release Package

### Scope

- 本地 Playground。
- 公网 Playground 部署文档。
- 英文文档。
- 竞品对比文章。
- 发布内容包。

### Required Evidence

- Golden Path 可跑通：启动 demo、登录、导入模板、dry-run、查看 trace、导出 DSL、CLI validate、AI 审计。
- 英文 quickstart 可用。
- 插件开发英文指南可用。
- 模板英文指南可用。
- 竞品对比内容客观、可维护。

### Stop Conditions

- 为赶发布关闭测试。
- 公开默认弱密钥或生产 secret。
- demo profile 和 production profile 混用。
