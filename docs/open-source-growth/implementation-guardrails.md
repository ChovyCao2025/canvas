# Open Source Growth Implementation Guardrails

日期：2026-06-08

## 1. 目的

本文件用于约束 Open Source Growth 专项的实际执行，尤其约束大模型或多人并行实施时的偏差。

专项设计文档定义方向，实施计划定义阶段。本文件定义硬边界：

- 哪些现有实现必须复用。
- 哪些事情半年内禁止做。
- 每个任务开始前必须检查什么。
- 每个 PR 必须如何切分。
- 什么情况必须暂停并要求人工决策。

## 2. 总执行原则

### 2.1 先承接现有实现，再新增抽象

执行任何任务前，必须先搜索现有类、表、API、前端组件和测试。

优先级：

1. 扩展现有实现。
2. 小范围重命名或补齐现有实现。
3. 新建适配层。
4. 新平行体系必须先有 coordinator decision，并明确是 temporary adapter
   还是 final DDD module。

禁止在没有审查现有实现的情况下新建同名或同职责模块。
禁止为了绕过 DDD ownership 新建 parallel registry/contract surfaces。
任何 registry、contract、DSL、template、plugin 或 AI backend surface 的新增
都必须在 `docs/program-coordination/subagent-worker-packets.md` 中有明确
worker packet、target backend state 和 final owner module。

### 2.2 一次只推进一个阶段

不得在一个 PR 中同时完成多个阶段。

例如：

- 不要在 README 改造 PR 中同时做插件注册表。
- 不要在插件注册表 PR 中同时做 DSL 和 AI。
- 不要在模板包 PR 中同时重构画布执行引擎。

每个 PR 必须有清晰边界、测试命令和回滚方式。

### 2.3 Demo-first，但不牺牲内核稳定性

demo 可以使用 mock provider 和示例数据，但不能绕过核心状态机、权限、执行记录和 trace。

允许：

- demo profile。
- mock provider。
- 示例租户和示例用户。
- 示例模板自动导入。

禁止：

- 为 demo 写一套绕过执行引擎的假流程。
- 为 demo 放宽生产 profile 安全限制。
- 为 demo 修改核心状态流转语义。

### 2.4 构建期插件优先

半年内插件体系只做构建期插件、Spring Bean 注册和配置启停。

禁止：

- 运行时 jar 热加载。
- 自定义 classloader。
- 在线插件安装。
- 插件市场交易。
- 任意脚本插件执行权限扩大。

## 3. 执行前强制检查

每个任务开始前必须执行对应搜索。

### 3.1 通用搜索

```bash
rg -n "<关键词>" backend frontend docs scripts tools
find backend/canvas-engine/src/main/java -type f | rg "<关键词>"
find frontend/src -type f | rg "<关键词>"
```

必须记录：

- 已有类名。
- 已有测试。
- 已有数据库迁移。
- 已有前端页面或组件。
- 是否存在同职责实现。

### 3.2 插件任务搜索

必须先检查：

```bash
rg -n "PluginRegistryService|JdbcPluginRepository|built_in_plugin_registry|PluginRegistryController" backend
sed -n '1,220p' backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java
sed -n '1,220p' backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/JdbcPluginRepository.java
```

已有事实：

- `PluginRegistryService` 已存在。
- `JdbcPluginRepository` 已存在。
- `built_in_plugin_registry` 表已存在。
- `PluginRegistryController` 已存在。

执行约束：

- 不要直接新建平行的 `CanvasPluginRegistry` 作为第二套注册中心。
- 首选做法是扩展 `PluginRegistryService`，增加 manifest、extension point、permissions、node metadata 等能力。
- 如果确实需要新类，必须说明它和 `PluginRegistryService` 的职责边界。

### 3.3 节点 handler 任务搜索

必须先检查：

```bash
sed -n '1,220p' backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/HandlerRegistry.java
sed -n '1,160p' backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/NodeHandlerType.java
find backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handlers -maxdepth 1 -type f | sort
```

已有事实：

- 节点处理器通过 Spring 注入 `List<NodeHandler>`。
- 节点类型通过 `@NodeHandlerType` 标识。
- `HandlerRegistry` 用 `typeKey -> NodeHandler` 映射。

执行约束：

- 插件节点必须兼容 `NodeHandler`。
- 插件节点必须有唯一 `@NodeHandlerType`。
- 插件启停必须和 `HandlerRegistry` 的可用性一致。
- 如果插件被禁用，依赖该插件的节点不能在发布校验中通过。
- 不得绕过 `HandlerRegistry` 直接从插件执行 handler。

### 3.4 模板任务搜索

必须先检查：

```bash
rg -n "TemplateRenderService|templateCatalog|canvas_template|CanvasExampleSeeder|Template" backend frontend docs/canvas-examples
sed -n '1,220p' backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplateRenderService.java
sed -n '1,220p' frontend/src/pages/canvas-list/templateCatalog.ts
```

已有事实：

- `TemplateRenderService` 已存在。
- 前端 `templateCatalog.ts` 已存在。
- 文档 `docs/canvas-examples/` 已存在。

执行约束：

- 模板包能力必须扩展现有 `engine.template` 包。
- 前端模板库必须承接 `templateCatalog.ts`，不要另起孤立模板入口。
- 模板导入必须复用现有画布创建、保存、版本机制。

### 3.5 AI 任务搜索

必须先检查：

```bash
find backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm -type f | sort
find backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai -type f | sort
find frontend/src/pages/ai-predictions -type f | sort
rg -n "AiLlmHandler|AiProvider|LLM|ai" backend/canvas-engine/src/main/java frontend/src
```

执行约束：

- AI 旅程生成必须支持 mock provider。
- 真实模型 key 不能作为 demo 必需条件。
- AI 生成结果必须进入预览或草稿，不得直接覆盖已发布画布。

## 4. 决策冻结区

以下事项没有人工确认前不得擅自决定。

### 4.1 License

`LICENSE` 必须由人工明确选择。

可选方向：

- Apache-2.0：更利于采用和生态。
- AGPL-3.0：更利于保护商业化边界。

大模型不得自行选择。

### 4.2 项目命名和英文品牌

不得擅自更改项目名、包名、组织名和 artifactId。

允许在 README 中使用产品定位文案，但不修改：

- Maven groupId。
- npm package name。
- Java package root。
- Docker image name。

### 4.3 商业版边界

不得在代码中新增商业版限制、license check、cloud-only 标记。

商业边界只能先写入文档。

### 4.4 真实外部服务

不得把真实短信、邮件、审批、AI、优惠券供应商接入设为默认路径。

默认路径必须是 mock 或 WireMock。

## 5. 阶段护栏

### 5.1 Month 1：开源入口和 Demo

允许：

- README 重写。
- 文档移动和整理。
- `docker-compose.demo.yml`。
- demo profile。
- mock provider。
- 示例画布初始化。

禁止：

- 改造插件注册表。
- 改造 DSL。
- 引入真实 AI provider。
- 重构执行引擎。

PR 大小限制：

- README 和社区文件一个 PR。
- demo compose 和 mock provider 一个 PR。
- 示例模板初始化一个 PR。

### 5.2 Month 2：轻量插件体系

允许：

- 扩展 `PluginRegistryService`。
- 扩展 `built_in_plugin_registry` 表，新增迁移。
- 增加 manifest 字段。
- 增加插件权限声明。
- 将插件启停接入发布校验。

禁止：

- 新建第二套插件注册中心取代 `PluginRegistryService`。
- classloader 热加载。
- 插件运行时安装。
- 插件执行任意脚本。

PR 大小限制：

- manifest 数据模型和迁移一个 PR。
- 启停和发布校验一个 PR。
- 官方插件 skeleton 一个 PR。

### 5.3 Month 3：模板库和配置面板

允许：

- 扩展 `templateCatalog.ts`。
- 新增 schema-driven 通用配置面板。
- 为简单插件节点使用 JSON Schema。
- 复杂节点保留自定义配置面板。

禁止：

- 一次性迁移所有节点配置面板。
- 删除现有复杂配置面板。
- 模板导入绕过画布保存服务。

PR 大小限制：

- SchemaConfigPanel 一个 PR。
- Template Pack 后端一个 PR。
- 10 个模板可以拆成多个 PR。

### 5.4 Month 4：Canvas DSL 和 CLI

允许：

- DSL v1。
- CLI validate/import/export/diff。
- DSL 到现有 graph JSON 的映射。

禁止：

- DSL 覆盖全部历史节点。
- DSL 成为运行时唯一真实格式。
- CLI 绕过后端 API 直接写数据库。

首版 DSL 只覆盖：

- webhook trigger。
- condition。
- message。
- coupon。
- approval。
- ai。
- risk-check。
- end。

### 5.5 Month 5：AI-native

允许：

- mock AI provider。
- 生成 DSL 草稿。
- 风险审计。
- trace 失败解释。
- 文案生成。

禁止：

- AI 直接发布画布。
- AI 自动修改已发布版本。
- demo 依赖真实模型 key。
- 把敏感用户数据发送到外部模型，除非明确脱敏且人工确认。

### 5.6 Month 6：Playground 和发布

允许：

- 本地 Playground。
- 公网部署文档。
- 英文文档。
- 竞品对比文章。

禁止：

- 为赶发布关闭测试。
- 将 demo 数据和生产 profile 混用。
- 公开默认弱密钥。

## 6. 任务拆分模板

每个可执行任务必须先写 mini-spec，格式如下：

```markdown
# <Task Name> Mini Spec

## Existing Code Checked

- `<path>`：发现了什么。
- `<path>`：决定复用还是修改。

## Scope

- 本任务做什么。
- 本任务不做什么。

## Files

- Create: `<path>`
- Modify: `<path>`
- Test: `<path>`

## Contracts

- API payload。
- Java interface。
- TypeScript type。
- DB migration。

## Tests

- 后端测试命令。
- 前端测试命令。
- 手工验证步骤。

## Rollback

- 如何回滚。
- 是否涉及迁移。
```

没有 mini-spec 不得开始实现。

## 7. PR 验收门禁

每个 PR 至少满足：

- 有清晰范围说明。
- 有测试命令和结果。
- 没有无关重构。
- 没有修改旧 Flyway 迁移。
- 没有提交 `target/`、`dist/`、`node_modules/`。
- 没有真实 secret。
- 没有把 demo mock 配置用于 production profile。

按变更类型运行：

```bash
cd backend && mvn -pl canvas-engine -Dtest=<FocusedTest> test
cd frontend && npm run test -- --run <focused-pattern>
cd frontend && npm run build
docker compose -f docker-compose.demo.yml config
```

## 8. 大模型执行偏差清单

执行过程中如果出现以下行为，必须停止并修正：

- 新建与已有服务同职责的平行类。
- 一次性修改超过一个阶段。
- 未搜索现有实现就新增模块。
- 修改已应用 Flyway 迁移。
- 为了测试通过删除或弱化校验。
- 为 demo 绕过真实执行引擎。
- 将 AI 输出直接写入已发布画布。
- 未经人工确认选择 license。
- 引入新大型框架但没有替代方案评估。
- 变更认证、租户、权限、安全策略但没有专项 spec。

## 9. 推荐执行流程

每个任务按以下顺序执行：

1. 阅读本 guardrails。
2. 阅读对应专项 spec 和 plan。
3. 搜索现有实现。
4. 写 mini-spec。
5. 写失败测试。
6. 实现最小代码。
7. 运行聚焦测试。
8. 运行必要构建。
9. 更新文档。
10. 提交或交付变更摘要。

## 10. 需要人工确认的问题清单

开始 Month 1 前需要确认：

- License 选择 Apache-2.0 还是 AGPL-3.0。
- 对外项目名是否继续使用 Canvas。
- 是否需要英文 README 作为主 README，中文作为 `README.zh-CN.md`。
- demo 是否允许默认账号 `admin / Admin@123` 对外展示。

开始 Month 2 前需要确认：

- 插件 manifest 是否存数据库、代码资源文件，还是两者兼容。
- 插件启停是全局级别还是租户级别。
- 官方插件是否仍放在主应用 module 内。

开始 Month 4 前需要确认：

- DSL 是否使用 YAML 作为主格式。
- DSL v1 是否允许表达坐标。
- CLI 是否发布为 npm package，还是仅作为 repo 内工具。
