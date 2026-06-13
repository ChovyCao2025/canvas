# Open Source Growth Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在半年内把当前 Canvas 项目改造成对外可理解、可试用、可扩展、可贡献的开源营销自动化平台。

**Architecture:** 采用稳定内核 + 构建期插件 + 模板包 + Canvas DSL/CLI + AI 操作器 + Demo Workspace 的渐进式架构。核心执行、画布版本、权限、审计和租户边界保持内核化；节点、Provider、模板和 AI 操作通过清晰 extension points 扩展。

**Tech Stack:** Java 21, Spring Boot, MyBatis-Plus, Flyway, React 18, Vite, TypeScript, Ant Design, React Flow, Vitest, JUnit 5, Node.js CLI scripts, Docker Compose, WireMock.

---

## 0. Execution Control

实施任何任务前必须先阅读并遵守：

- [../program-coordination/README.md](../program-coordination/README.md)
- [../program-coordination/execution-readiness-audit.md](../program-coordination/execution-readiness-audit.md)
- [../program-coordination/gate-verification-matrix.md](../program-coordination/gate-verification-matrix.md)
- [../program-coordination/max-parallel-subagent-execution-plan.md](../program-coordination/max-parallel-subagent-execution-plan.md)
- [implementation-guardrails.md](./implementation-guardrails.md)
- [traceability-matrix.md](./traceability-matrix.md)
- [phase-gates.md](./phase-gates.md)
- [decision-log.md](./decision-log.md)

每个可执行任务开始前必须写 mini-spec，明确：

- 复用的现有代码。
- 本任务对应的 `OSG-*` 追踪编号。
- 修改文件、测试文件和回滚方式。
- 是否涉及人工决策。
- 当前 readiness level。
- `Target backend state`：`DOCS_ONLY`、`CURRENT_ENGINE_BRIDGE` 或 `DDD_FINAL_MODULE`。
- 如为 `CURRENT_ENGINE_BRIDGE`，必须写明最终 DDD owner module 和迁移退出条件。

没有 mini-spec 不得开始实现。每个 PR 必须通过对应 Month phase gate 的当前适用检查。

### 0.1 DDD Coordination Gate

本计划的月份顺序是产品目标顺序，不是后端代码可以无条件执行的顺序。

以下任务的后端实现必须等待
`docs/program-coordination/execution-readiness-audit.md` 中的 readiness gate：

- Task 2 的 demo seed service：只能作为 `CURRENT_ENGINE_BRIDGE`，且必须使用
  `application-demo.yml` 或 demo-only 配置，不得修改 production/staging 默认语义。
- Task 3 插件 registry backend：等待 OSG-C07 和 G10；必须承接旧
  `PluginRegistryService` 语义并迁移/桥接到 DDD final owner，不得新建第二套
  registry。
- Task 4 官方插件 runtime：等待 execution extension points 编译通过。
- Task 6 TemplateImportService：等待 canvas draft/version API 明确。
- Task 7 Canvas DSL backend import/export：等待 canvas API 和 web API 明确。
- Task 8 CLI import/export/publish：等待 G10 public extension/API stability
  gate；本地 validate/diff 可先做。
- Task 9 AI journey backend 和 trace explanation：等待 draft、risk、trace API 明确。

如果 readiness gate 未满足，这些任务只能执行 docs、contract、frontend mock、
local CLI validation 或 example 工作。

### 0.2 Backend Path Authority

本计划中 `backend/canvas-engine/**` 路径表示产品能力在当前代码中的来源或
临时 bridge 位置，不自动成为子代理的可写范围。只要 DDD modular rewrite
处于并行执行中，后端子代理必须以
`docs/program-coordination/subagent-worker-packets.md` 的具体 worker packet
为唯一写范围来源。

规则：

- `DDD_FINAL_MODULE` worker 只能写 worker packet 中列出的
  `backend/canvas-context-*`、`backend/canvas-platform`、`backend/canvas-web`
  或 `backend/canvas-boot` 路径。
- `CURRENT_ENGINE_BRIDGE` worker 必须在 mini-spec 中写明旧服务名、最终 DDD
  owner module、迁移退出条件和回滚路径。
- 如果本文件的 `Files` 列表和 worker packet 冲突，以 worker packet 为准。
- 如果没有对应 worker packet，停止并返回 `NEEDS_CONTEXT`，不得自行新增后端
  包或第二套 registry。

## 1. 文件结构规划

### 1.1 文档和开源入口

- Modify `README.md`：改为开源产品首页，保留部署细节并下沉到 docs。
- Create `LICENSE`：选择 Apache-2.0 或 AGPL-3.0。
- Create `CONTRIBUTING.md`：贡献流程、分支策略、测试命令、插件贡献入口。
- Create `CODE_OF_CONDUCT.md`：社区行为准则。
- Create `SECURITY.md`：漏洞报告、支持版本、敏感信息规则。
- Create `.github/ISSUE_TEMPLATE/bug_report.yml`：问题报告模板。
- Create `.github/ISSUE_TEMPLATE/feature_request.yml`：功能建议模板。
- Create `.github/pull_request_template.md`：PR 检查清单。
- Create `docs/open-source/quickstart.md`：5 分钟 demo。
- Create `docs/open-source/plugin-development.md`：插件开发指南。
- Create `docs/open-source/template-pack-development.md`：模板包指南。
- Create `docs/open-source/marketingops-as-code.md`：DSL/CLI 使用指南。

### 1.2 Demo Workspace

- Modify `docker-compose.local.yml`：确保 demo 所需 MySQL、Redis、RocketMQ、WireMock 一次启动。
- Create `docker-compose.demo.yml`：对外最小 demo compose。
- Modify `wiremock/mappings/mock-responses.json`：补齐短信、邮件、审批、优惠券、AI mock。
- Create `backend/canvas-engine/src/main/resources/application-demo.yml`：增加 demo-only profile 默认配置。
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoWorkspaceService.java`：demo 初始化编排，必须声明 `CURRENT_ENGINE_BRIDGE` 和最终 DDD owner。
- Modify `frontend/src/pages/home/index.tsx`：demo 首屏入口。
- Modify `frontend/src/pages/canvas-list/templateCatalog.ts`：展示官方模板。

### 1.3 Plugin Registry

本节后端文件是 Month 2 产品目标，不是无条件立即可写范围。OSG-C07 已决策
最终 owner：`canvas-platform` 承接 registry metadata、manifest、permissions、
compatibility、persistence 和 enablement；`canvas-context-execution` 承接 handler
binding、node metadata、runtime validation 和 trace failure path。执行前仍必须通过
G10，并在 worker packet 中声明 `DDD_FINAL_MODULE` 或 `CURRENT_ENGINE_BRIDGE`。

- Create final `backend/canvas-platform/**` plugin manifest/registry/permission
  model and services：插件接口、manifest 模型、权限枚举、manifest 校验和启停
  metadata。
- Modify final `backend/canvas-platform/**` persistence adapters：承接
  `PluginRegistryService`、`JdbcPluginRepository` 和 `built_in_plugin_registry`
  语义；如字段不足，新增 Flyway migration，不修改旧迁移。
- Modify final `backend/canvas-context-execution/**` plugin adapter/API：
  handler binding、node metadata、publish/dry-run validation 和
  `PluginEnablementView` consumption。
- Legacy bridge only: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java`。
- Legacy bridge only: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/JdbcPluginRepository.java`。
- Legacy bridge only: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/HandlerRegistry.java`。
- Create `frontend/src/plugins/pluginRegistry.ts`：前端插件注册入口。
- Create `frontend/src/plugins/pluginManifest.ts`：前端 manifest 类型。
- Modify `frontend/src/components/node-panel/nodeLibrary.ts`：合并后端节点元数据和前端插件展示信息。
- Modify `frontend/src/components/config-panel/index.tsx`：支持 schema-driven 配置。

### 1.4 官方插件

本节 backend runtime 行为必须等待 execution extension points 编译通过。等待期间
只允许写 docs、manifest examples、frontend mock 展示和 plugin skeleton。

- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/official/webhook/`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/official/message/`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/official/coupon/`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/official/approval/`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/official/ai/`
- Create `frontend/src/plugins/official/`
- Create `docs/open-source/plugins/official/*.md`

### 1.5 Template Packs

Template docs 和 catalog 可以先做；TemplateImportService 后端必须等待 canvas
draft/version API 和 execution dry-run contract 明确。

- Create `docs/open-source/templates/`
- Modify `docs/canvas-examples/README.md`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplatePack.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplatePackRegistry.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplateImportService.java`
- Modify `frontend/src/pages/canvas-list/templateCatalog.ts`

### 1.6 Canvas DSL and CLI

DSL contract、local parser/validate 和 CLI local diff 可以先做。后端
import/export/publish API 以及 CLI API write commands 必须等待 DDD canvas/web API
明确。

- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/dsl/CanvasDslDocument.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/dsl/CanvasDslParser.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/dsl/CanvasDslValidator.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/dsl/CanvasDslMapper.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasDslController.java`
- Create `tools/canvas-cli/package.json`
- Create `tools/canvas-cli/src/index.mjs`
- Create `tools/canvas-cli/src/commands/validate.mjs`
- Create `tools/canvas-cli/src/commands/import.mjs`
- Create `tools/canvas-cli/src/commands/export.mjs`
- Create `tools/canvas-cli/src/commands/diff.mjs`
- Create `tools/canvas-cli/test/*.test.mjs`

### 1.7 AI-native Experience

AI prompts、frontend preview 和 mock examples 可以先做。JourneyGenerationService、
JourneyRiskAuditService、TraceExplanationService 的后端最终实现必须等待 draft、
risk、trace API 明确。

- Modify `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/JourneyGenerationService.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/JourneyRiskAuditService.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/TraceExplanationService.java`
- Create `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiJourneyController.java`
- Modify `frontend/src/pages/canvas-editor/index.tsx`
- Create `frontend/src/pages/canvas-editor/AiJourneyAssistant.tsx`

## 2. 阶段任务

### Task 1: 开源入口和 README 改造

**目标：** 让第一次访问仓库的人在 60 秒内理解项目价值，在 5 分钟内启动 demo。

**Files:**

- Modify: `README.md`
- Create: `docs/open-source/quickstart.md`
- Create: `docs/open-source/positioning.md`
- Create: `LICENSE`
- Create: `CONTRIBUTING.md`
- Create: `CODE_OF_CONDUCT.md`
- Create: `SECURITY.md`
- Create: `.github/ISSUE_TEMPLATE/bug_report.yml`
- Create: `.github/ISSUE_TEMPLATE/feature_request.yml`
- Create: `.github/pull_request_template.md`

**Steps:**

- [ ] 将 `README.md` 首屏改成产品介绍，而不是部署手册。
- [ ] 在 `README.md` 顶部加入 slogan、截图占位、快速启动命令、核心能力、插件和模板入口。
- [ ] 把当前部署细节迁移或引用到 `docs/open-source/quickstart.md`。
- [ ] 添加开源社区基础文件。
- [ ] 添加 issue 和 PR 模板。
- [ ] 运行 Markdown 链接检查或手工检查所有相对链接。

**验收：**

- `README.md` 前 120 行包含定位、快速启动、核心截图/GIF 入口、模板入口、插件开发入口。
- 新用户无需阅读长部署手册即可找到 demo 命令。
- GitHub Community Profile 基础项齐全。

**验证命令：**

```bash
test -f LICENSE
test -f CONTRIBUTING.md
test -f CODE_OF_CONDUCT.md
test -f SECURITY.md
test -f .github/pull_request_template.md
```

### Task 2: Demo Workspace 和一键启动

**目标：** 提供对外最小体验环境，隐藏复杂依赖和真实外部系统。

**Backend Gate：** demo compose、WireMock 和 docs 可先做。`DemoWorkspaceService`
必须声明 `CURRENT_ENGINE_BRIDGE`，最终 owner 为 `canvas-boot` 或
`canvas-context-canvas`，并且只能使用 demo-only 配置。

**Files:**

- Create: `docker-compose.demo.yml`
- Modify: `docker-compose.local.yml`
- Modify: `wiremock/mappings/mock-responses.json`
- Modify: `backend/canvas-engine/src/main/resources/application.yml`
- Create: `backend/canvas-engine/src/main/resources/application-demo.yml`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/DemoWorkspaceService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/demo/DemoWorkspaceServiceTest.java`
- Modify: `frontend/src/pages/home/index.tsx`
- Modify: `frontend/src/pages/canvas-list/index.tsx`

**Steps:**

- [ ] 定义 demo profile：默认启用 mock provider、官方插件、官方模板。
- [ ] 创建 `docker-compose.demo.yml`，只暴露前端和后端必要端口。
- [ ] WireMock 补齐 email、sms、coupon、approval、ai 的 mock 响应。
- [ ] `DemoWorkspaceService` 启动时确保示例画布和模板存在，使用幂等 key。
- [ ] 前端首页增加 demo 快捷入口：模板库、最近示例旅程、dry-run 引导。
- [ ] 添加测试，验证 demo 初始化不会重复创建模板。

**验收：**

- `docker compose -f docker-compose.demo.yml up` 后能访问 `http://localhost:3000`。
- 默认账号可登录。
- 至少 3 个示例旅程存在。
- 示例旅程可以 dry-run，不依赖真实外部服务。

**验证命令：**

```bash
docker compose -f docker-compose.demo.yml config
cd backend && mvn -pl canvas-engine -Dtest=DemoWorkspaceServiceTest test
cd frontend && npm run build
```

### Task 3: 插件 manifest 和注册表

**目标：** 建立轻量插件体系，支持构建期插件 + 配置启停 + manifest 校验。

**Backend Gate：** 未通过 OSG-C07 前只能做 contract/docs/examples。后端实现
必须声明 `DDD_FINAL_MODULE` 或 `CURRENT_ENGINE_BRIDGE`，且 G10 通过前不得启动
backend ecosystem code-writing worker。旧 `canvas-engine` 路径只能作为 source rows
或临时 bridge，不得形成最终实现。

**Files:**

- Create/modify: `backend/canvas-platform/**` plugin registry metadata,
  manifest, permission, validation, compatibility, persistence, and enablement
  files.
- Create/modify: `backend/canvas-context-execution/**` plugin handler binding,
  node metadata, publish/dry-run validation, trace integration, and
  `PluginEnablementView` adapter files.
- Legacy bridge only with complete declaration:
  `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/PluginRegistryService.java`
- Legacy bridge only with complete declaration:
  `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/JdbcPluginRepository.java`
- Legacy bridge only with complete declaration:
  `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/HandlerRegistry.java`

**Steps:**

- [ ] 在 final owner 模块定义插件接口/manifest 模型，包含 `manifest()`、`nodeHandlers()`、`providers()`、`templatePacks()`。
- [ ] 定义 `CanvasPluginManifest`，字段包括 id、name、version、coreVersion、extensionPoints、permissions、nodes、templates。
- [ ] 定义插件权限枚举，覆盖外部 HTTP、写 context、发消息、发优惠券、注册 webhook、读用户属性。
- [ ] 在 `canvas-platform` 承接旧 `PluginRegistryService` 语义，从 Spring Bean 收集构建期插件并按配置启停。
- [ ] 在 `canvas-platform` persistence adapter 承接旧 `JdbcPluginRepository` 和
  `built_in_plugin_registry` 语义，让内置插件注册数据可以承载 manifest、
  extension point、permissions 和 node metadata。
- [ ] 实现 manifest 校验：id 格式、版本格式、节点 type 唯一、权限声明完整。
- [ ] 在 `canvas-context-execution` 的 `NodeHandlerRegistry` 路径合并核心 handler 和插件 handler，冲突时启动失败；旧 `HandlerRegistry` 只用于 bridge。
- [ ] 添加单元测试覆盖启停、冲突、非法 manifest。

**验收：**

- 插件可以被发现。
- 插件可以被配置禁用。
- 重复 node type 会失败。
- 缺失权限声明会失败。

**验证命令：**

```bash
cd backend && mvn test -pl canvas-platform,canvas-context-execution -Dtest='*Plugin*Test'
```

### Task 4: 官方插件最小闭环

**目标：** 提供能讲清楚平台价值的 6 个官方插件。

**Backend Gate：** runtime handler 行为必须等待 execution extension points
编译通过。等待期间只允许 docs、manifest examples、frontend mock 和 skeleton。

**Files:**

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/official/webhook/`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/official/message/`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/official/coupon/`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/official/approval/`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/official/ai/`
- Create: `frontend/src/plugins/official/`
- Create: `docs/open-source/plugins/official/webhook.md`
- Create: `docs/open-source/plugins/official/message.md`
- Create: `docs/open-source/plugins/official/coupon.md`
- Create: `docs/open-source/plugins/official/approval.md`
- Create: `docs/open-source/plugins/official/ai.md`

**Official plugins:**

- `webhook-plugin`：接收外部事件，适配通用 webhook payload。
- `message-plugin`：mock email/sms/message 触达。
- `coupon-plugin`：mock 优惠券发放和幂等检查。
- `approval-plugin`：mock 发布审批或内容审批。
- `ai-llm-plugin`：mock 文案生成和节点建议。
- `risk-check-plugin`：旅程发布前风险检查。

**Steps:**

- [ ] 每个插件提供 manifest。
- [ ] 每个插件至少贡献 1 个节点类型。
- [ ] 每个插件提供 JSON Schema 配置。
- [ ] 每个插件提供 dry-run mock 行为。
- [ ] 每个插件提供文档和示例 payload。
- [ ] 前端节点库按插件分组展示。

**验收：**

- 6 个插件默认在 demo profile 启用。
- 禁用任一插件后，依赖该插件的模板不可导入并展示明确错误。
- 每个插件至少有一个后端测试和一个前端展示/配置测试。

**验证命令：**

```bash
cd backend && mvn -pl canvas-engine -Dtest='*Plugin*Test' test
cd frontend && npm run test -- --run
```

### Task 5: Schema-driven 节点配置面板

**目标：** 新插件节点不需要改核心前端即可完成基础配置。

**Files:**

- Modify: `frontend/src/components/config-panel/index.tsx`
- Modify: `frontend/src/components/config-panel/formValues.ts`
- Modify: `frontend/src/components/config-panel/displayValues.ts`
- Create: `frontend/src/components/config-panel/SchemaConfigPanel.tsx`
- Create: `frontend/src/components/config-panel/schemaConfigPanel.test.tsx`
- Create: `frontend/src/plugins/pluginManifest.ts`
- Create: `frontend/src/plugins/pluginRegistry.ts`

**Steps:**

- [ ] 定义前端插件 manifest 类型。
- [ ] 实现 schema 到 Ant Design 表单控件的最小映射：string、number、boolean、enum、array、object。
- [ ] 支持插件自定义 display metadata：label、description、placeholder、secret、required。
- [ ] 对已有节点逐步接入 schema 配置，保留复杂节点的自定义面板。
- [ ] 测试 schema 表单渲染、默认值、提交值、必填校验。

**验收：**

- 简单插件节点只靠 schema 就能配置。
- 复杂节点仍可使用自定义 React 面板。
- 表单值结构和后端 config schema 保持一致。

**验证命令：**

```bash
cd frontend && npm run test -- --run schemaConfigPanel
cd frontend && npm run build
```

### Task 6: Template Pack 和 10 个官方模板

**目标：** 让业务模板成为项目增长入口。

**Backend Gate：** docs、sample payload、expected trace 和前端 catalog 可先做。
`TemplateImportService` 必须等待 canvas draft/version API 和 execution dry-run
contract 明确。
实际后端实现使用 `OSG-W09`，默认目标为 `canvas-context-canvas`，dry-run
校验只通过 `canvas-context-execution` API。

**Files:**

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplatePack.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplatePackRegistry.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/TemplateImportService.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/template/TemplateImportServiceTest.java`
- Modify: `frontend/src/pages/canvas-list/templateCatalog.ts`
- Create: `docs/open-source/templates/*.md`
- Modify: `docs/canvas-examples/README.md`

**Templates:**

- `new-user-welcome`
- `dormant-user-winback`
- `coupon-approval-release`
- `ai-copy-review-publish`
- `lead-capture-assignment`
- `birthday-benefit`
- `vip-retention`
- `ab-message-experiment`
- `risk-blocked-outreach`
- `private-domain-follow-up`

**Steps:**

- [ ] 定义模板包结构：metadata、dependencies、canvas、samplePayload、expectedTrace、docs。
- [ ] 实现依赖插件校验。
- [ ] 实现模板导入为草稿画布。
- [ ] 实现重复导入幂等策略。
- [ ] 前端模板库展示模板分类、依赖、风险等级和导入按钮。
- [ ] 每个模板添加文档和示例 dry-run payload。

**验收：**

- 10 个模板能导入。
- 模板依赖缺失时阻止导入。
- 至少 3 个模板可 dry-run 并生成 trace。

**验证命令：**

```bash
cd backend && mvn -pl canvas-engine -Dtest=TemplateImportServiceTest test
cd frontend && npm run test -- --run template
```

### Task 7: Canvas DSL 后端能力

**目标：** 支持 YAML/JSON 旅程定义的校验、导入、导出和 diff。

**Backend Gate：** DSL docs、examples、local parser/validate 可先做。后端
import/export API 必须等待 `canvas-context-canvas` 和 `canvas-web` API 明确。
实际后端实现使用 `OSG-W10`，默认目标为 `canvas-context-canvas` 与
`canvas-web`，不得绑定旧 `CanvasService` 内部实现。

**Files:**

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/dsl/CanvasDslDocument.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/dsl/CanvasDslParser.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/dsl/CanvasDslValidator.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/dsl/CanvasDslMapper.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/dsl/CanvasDslDiffService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/CanvasDslController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/template/dsl/CanvasDslValidatorTest.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/engine/template/dsl/CanvasDslMapperTest.java`

**Steps:**

- [ ] 定义 DSL v1 document 模型。
- [ ] 实现 YAML/JSON parser。
- [ ] 实现结构校验：apiVersion、kind、metadata、trigger、nodes、edges。
- [ ] 实现插件依赖校验：node type 必须由核心或启用插件提供。
- [ ] 实现 DSL 到现有 graph JSON 的映射。
- [ ] 实现 graph JSON 到 DSL 的导出。
- [ ] 实现基础 diff：新增节点、删除节点、节点配置变化、边变化。
- [ ] 暴露 API：validate、import、export、diff。

**验收：**

- 合法 DSL 通过校验。
- 缺失节点、环、未知 node type、缺失插件会失败。
- 现有画布可导出 DSL。
- DSL 可导入为草稿画布。

**验证命令：**

```bash
cd backend && mvn -pl canvas-engine -Dtest=CanvasDslValidatorTest,CanvasDslMapperTest test
```

### Task 8: Canvas CLI

**目标：** 提供 MarketingOps as Code 的开发者入口。

**Backend Gate：** `validate` 和本地文件 `diff` 可先做。`import`、`export`、
`publish` 必须等待 G10 public extension/API stability gate，不得直接访问数据库或
内部类。

**Files:**

- Create: `tools/canvas-cli/package.json`
- Create: `tools/canvas-cli/src/index.mjs`
- Create: `tools/canvas-cli/src/apiClient.mjs`
- Create: `tools/canvas-cli/src/commands/validate.mjs`
- Create: `tools/canvas-cli/src/commands/import.mjs`
- Create: `tools/canvas-cli/src/commands/export.mjs`
- Create: `tools/canvas-cli/src/commands/diff.mjs`
- Create: `tools/canvas-cli/src/commands/publish.mjs`
- Create: `tools/canvas-cli/test/validate.test.mjs`
- Create: `tools/canvas-cli/test/diff.test.mjs`
- Create: `docs/open-source/marketingops-as-code.md`

**Steps:**

- [ ] 初始化 Node.js CLI package。
- [ ] 实现 `canvas validate journey.yaml`。
- [ ] 实现 `canvas import journey.yaml --base-url http://localhost:8080 --token ...`。
- [ ] 实现 `canvas export --canvas-id 123 --output journey.yaml`。
- [ ] 实现 `canvas diff a.yaml b.yaml`。
- [ ] 实现 `canvas publish --canvas-id 123`，调用现有发布接口。
- [ ] 添加 CLI 文档和 examples。

**验收：**

- CLI 可在本地 demo 环境操作。
- CLI 错误输出包含清晰的字段路径和修复建议。
- CLI 测试覆盖 validate 和 diff。

**验证命令：**

```bash
cd tools/canvas-cli && npm test
cd tools/canvas-cli && node src/index.mjs --help
```

### Task 9: AI-native 旅程生成和审计

**目标：** 让 AI 成为项目差异化主路径。

**Backend Gate：** prompt、mock response、frontend preview 可先做。journey
generation、risk audit、trace explanation 后端实现必须等待 draft、risk、trace
API 明确。
实际后端实现使用 `OSG-W12`：草稿生成属于 `canvas-context-canvas`，营销风险
审计属于 `canvas-context-marketing`，trace 解释只通过
`canvas-context-execution` API。

**Files:**

- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/JourneyGenerationService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/JourneyRiskAuditService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/TraceExplanationService.java`
- Create: `backend/canvas-engine/src/main/java/org/chovy/canvas/web/AiJourneyController.java`
- Create: `backend/canvas-engine/src/test/java/org/chovy/canvas/domain/ai/JourneyRiskAuditServiceTest.java`
- Create: `frontend/src/pages/canvas-editor/AiJourneyAssistant.tsx`
- Create: `frontend/src/pages/canvas-editor/aiJourneyAssistant.test.tsx`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

**Steps:**

- [ ] 实现 mock AI provider，保证没有外部 key 也能演示。
- [ ] 实现自然语言到 DSL 草稿生成。
- [ ] 实现风险审计规则：缺频控、缺审批、优惠券节点无上限、触达过密、无失败分支。
- [ ] 实现 trace 失败解释：按失败节点、错误类型、重试状态、外部响应生成摘要。
- [ ] 前端在画布编辑器提供 AI 助手入口。
- [ ] AI 生成结果先进入预览，不直接覆盖当前画布。

**验收：**

- 输入“给新用户发欢迎优惠券并短信通知”能生成 DSL 草稿。
- 风险审计能返回结构化风险列表。
- 失败 trace 能生成可读解释。
- mock 模式不需要模型 key。

**验证命令：**

```bash
cd backend && mvn -pl canvas-engine -Dtest=JourneyRiskAuditServiceTest test
cd frontend && npm run test -- --run aiJourneyAssistant
```

### Task 10: Playground / 沙盒体验

**目标：** 对外提供低门槛试用入口。

**Files:**

- Create: `docs/open-source/playground.md`
- Modify: `docker-compose.demo.yml`
- Modify: `frontend/src/pages/home/index.tsx`
- Modify: `frontend/src/pages/canvas-list/index.tsx`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

**Steps:**

- [ ] 先实现本地 Playground 模式：demo profile + mock provider + 模板库 + AI mock。
- [ ] 为未来公网部署记录环境变量、只读/临时租户策略、数据清理策略。
- [ ] 前端给 demo 旅程增加“Try dry-run”和“Export DSL”按钮。
- [ ] 首页加入“Start from template”和“Generate with AI”两个主入口。

**验收：**

- 本地 Playground 完成一条完整路径：模板导入 -> dry-run -> trace -> export DSL。
- 文档说明如何部署公网沙盒。

**验证命令：**

```bash
docker compose -f docker-compose.demo.yml config
cd frontend && npm run build
```

### Task 11: 英文文档和传播包

**目标：** 支持对外发布和社区传播。

**Files:**

- Create: `docs/open-source/en/quickstart.md`
- Create: `docs/open-source/en/plugin-development.md`
- Create: `docs/open-source/en/templates.md`
- Create: `docs/open-source/en/marketingops-as-code.md`
- Create: `docs/open-source/en/canvas-vs-mautic-vs-n8n.md`
- Create: `docs/open-source/release-posts/`

**Steps:**

- [ ] 写英文 quickstart。
- [ ] 写插件开发英文版。
- [ ] 写模板库英文版。
- [ ] 写 Canvas vs Mautic vs n8n 对比，强调定位差异，不贬低竞品。
- [ ] 写 3 篇发布文章草稿：项目介绍、AI Journey Canvas、开发一个短信插件。

**验收：**

- 英文文档能支撑 GitHub 首次访问。
- 对外发布文章至少 3 篇。

## 3. 推荐执行顺序

```text
Month 1: README + community files + demo workspace
Month 2: plugin registry + official plugin skeletons
Month 3: schema config panel + template packs
Month 4: Canvas DSL + CLI
Month 5: AI-native journey generation and audit
Month 6: Playground + English docs + release content
```

## 4. 总体验证

半年专项结束时运行：

```bash
docker compose -f docker-compose.demo.yml config
cd backend && mvn test
cd frontend && npm run test -- --run
cd frontend && npm run build
cd tools/canvas-cli && npm test
```

手工验收：

- 新用户能按 README 启动 demo。
- 首页能进入模板库。
- 至少 3 个模板可 dry-run。
- 一个模板可导出 DSL。
- CLI 能 validate/export/diff。
- AI mock 能生成旅程并审计风险。
- 禁用插件后依赖模板不可导入。

## 5. 发布检查表

- [ ] README 首屏已替换为开源产品叙事。
- [ ] License 和贡献文档已补齐。
- [ ] demo compose 可启动。
- [ ] 官方插件文档齐全。
- [ ] 官方模板至少 10 个。
- [ ] DSL 示例至少 5 个。
- [ ] CLI 有 README 和 help 输出。
- [ ] AI mock 不依赖真实 key。
- [ ] 英文 quickstart 可用。
- [ ] 发布文章草稿完成。
