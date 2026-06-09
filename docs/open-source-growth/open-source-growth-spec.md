# Open Source Growth Spec

日期：2026-06-08

## 1. 背景

当前项目已经具备较厚的工程基础：Java 21 + Spring Boot 后端、Vite + React 前端、React Flow 画布、Flyway 迁移、CI、K8s/Helm、压测脚本、运行手册、SDK、营销内容、审批、BI、CDP、数据仓库和多类运营能力。

但从开源项目视角看，当前仓库更像企业内部产品工程仓库，而不是一个外部开发者可以快速理解和参与的开源产品。主要问题是：

- README 更偏部署手册，首屏没有清晰说明“它替代谁、为什么用、如何 5 分钟看到价值”。
- 功能面过宽，CDP、BI、数据仓库、审批、内容中心、SCRM、广告投放等概念同时出现，外部用户难以抓住主线。
- 本地启动依赖较重，用户需要理解 MySQL、Redis、RocketMQ、WireMock、JWT 等基础设施后才能体验。
- 扩展点尚未被包装成稳定生态协议，外部开发者贡献新节点、新模板、新集成的路径不够短。
- 缺少开源社区基础设施和对外传播材料。

本专项将项目对外收敛成“开源、插件驱动、AI 辅助的营销自动化平台”，以 Journey Canvas 作为第一体验，以插件、模板、DSL、CLI 和 AI 能力作为生态入口。

## 2. 产品定位

### 2.1 品类定位

Canvas 对外定位为：

```text
Open-source Marketing Automation Platform
```

更具体地说：

```text
Plugin-driven Journey Canvas + MarketingOps Runtime + AI-assisted Campaign Operations
```

中文表述：

```text
开源、插件驱动、AI 辅助的营销自动化平台。
```

### 2.2 差异化

Canvas 不直接宣称自己是完整营销云，而是聚焦三件事：

- Journey Canvas：用可视化 DAG 编排客户旅程。
- Plugin Ecosystem：通过插件连接触达、审批、AI、优惠券、数据源和企业系统。
- Execution Governance：提供 dry-run、审批、灰度、回滚、审计、trace 和执行可观测。

### 2.3 推荐首屏文案

英文：

```text
Canvas: Open-source Marketing Automation Platform

Build customer journeys with a visual canvas, connect channels through plugins,
and run campaigns with governance, traceability, and AI assistance.
```

中文：

```text
Canvas：开源营销自动化平台

用可视化画布编排客户旅程，通过插件连接渠道，并以审批、审计、追踪和 AI 能力运行营销活动。
```

## 3. 成功目标

半年内完成以下可验证目标：

- 新用户可以通过一条命令启动完整 demo。
- README 首屏包含定位、截图/GIF、快速启动、核心能力、竞品对比和模板入口。
- 至少 6 个官方插件可用：webhook、email/sms mock、coupon、approval、ai-llm、message。
- 至少 10 个官方模板可用，并可从前端或 CLI 导入。
- Canvas DSL 支持主流旅程表达，并可导入/导出当前画布。
- CLI 支持 validate、import、export、diff、publish 的最小闭环。
- AI 能力支持生成旅程草稿、风险审计、文案生成和执行失败解释。
- Playground 或沙盒模式可以在无需外部真实系统的情况下演示完整路径。
- 开源基础文件齐全：LICENSE、CONTRIBUTING、CODE_OF_CONDUCT、SECURITY、issue/PR 模板、Roadmap。

## 4. 非目标

半年内不追求：

- 运行时 jar 热加载。
- 插件在线交易市场。
- 完整 SaaS 托管云。
- 完整 CDP、BI、广告投放产品化闭环。
- 完整微服务化。
- 全量 DDD 重构。
- 面向所有营销场景的完整功能覆盖。

## 5. 架构原则

### 5.1 稳定内核

以下能力属于 Core Kernel，不对插件开放破坏性写入：

- DAG 结构和画布版本。
- 执行上下文基础协议。
- 发布、回滚、审批和状态流转。
- 执行记录、trace、审计。
- 租户、认证、权限基础边界。
- 插件权限和启停治理。

### 5.2 扩展优先

以下能力通过 Extension Points 暴露：

- 节点处理器。
- 节点配置 schema。
- 外部 Provider。
- 模板包。
- Dashboard widget。
- 事件 sink。
- AI 操作器。

### 5.3 Contract-first

插件、DSL、CLI、模板和前端配置必须围绕稳定契约：

- Java interface：后端插件和 Provider。
- JSON Schema：节点配置和插件配置。
- TypeScript types：前端插件注册和配置渲染。
- YAML/JSON：Canvas DSL。
- OpenAPI：Headless API。

### 5.4 Demo-first

任何新增生态能力都必须能在 demo 环境中被体验：

- 不依赖真实短信、邮件、审批、优惠券供应商。
- 默认使用 mock provider。
- dry-run 能展示输入、节点决策、输出和 trace。
- 示例模板能一键导入并执行。

## 6. 目标架构

```text
Canvas Core
  Canvas Model
  DAG Engine
  Execution Runtime
  Governance
  Audit and Trace
  Plugin Registry

Extension Layer
  Node Plugins
  Provider Plugins
  Template Packs
  AI Operators
  Dashboard Widgets

Developer Layer
  Canvas DSL
  CLI
  Plugin SDK
  Template SDK
  Contract Tests

Experience Layer
  Web Console
  Demo Workspace
  Playground
  Documentation Site
```

## 6.1 Backend Path Authority

本 spec 中出现的 `backend/canvas-engine/**` 路径只表示当前能力来源或
temporary bridge only，不自动成为子代理可写范围。

后端写入权威来自：

```text
docs/program-coordination/subagent-worker-packets.md
docs/program-coordination/gate-verification-matrix.md
docs/program-coordination/progress-ledger.md
```

规则：

- 每个后端任务必须声明 `Target backend state`：`DOCS_ONLY`、
  `CURRENT_ENGINE_BRIDGE` 或 `DDD_FINAL_MODULE`。
- `CURRENT_ENGINE_BRIDGE` 必须声明 exact old service/API、exact old files、
  final DDD owner module、idempotency rule、bridge removal gate 和 rollback
  path。
- `DDD_FINAL_MODULE` 只能写 worker packet 中列出的最终 DDD module 路径。
- 如果本 spec 的建议路径和 worker packet 冲突，以 worker packet 为准。
- 没有 worker packet、bridge declaration 或 progress-ledger dispatch row 时，
  worker 必须返回 `NEEDS_CONTEXT`，不得写旧 `canvas-engine` 路径。

## 7. 模块设计

### 7.1 Demo Workspace

Demo Workspace 是用户首次体验入口。

能力：

- 使用 docker compose 启动所有依赖。
- 自动导入官方插件和模板。
- 默认账号可登录。
- 外部触达、审批、优惠券、AI 均使用 mock provider。
- 首页直接展示 3 到 5 个示例旅程。

建议路径：

- `docker-compose.local.yml`
- `wiremock/`
- `backend/canvas-engine/src/main/resources/db/migration/`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/demo/`
- `frontend/src/pages/home/`
- `frontend/src/pages/canvas-list/`

### 7.2 Plugin Registry

插件注册表负责发现、校验、启停和展示插件。

插件声明示例：

```json
{
  "id": "canvas-plugin-coupon",
  "name": "Coupon Plugin",
  "version": "0.1.0",
  "canvasCoreVersion": ">=0.1.0 <1.0.0",
  "extensionPoints": ["node-handler", "coupon-provider", "template-pack"],
  "permissions": ["coupon:grant", "execution:write-context"],
  "nodes": ["COUPON_GRANT"],
  "templates": ["coupon-approval-journey"],
  "configSchema": "config.schema.json"
}
```

第一阶段插件不做运行时 classloader 动态加载，而是：

- 随主应用构建。
- 通过 Spring Bean 注册。
- 通过 manifest 和配置启停。
- 前端通过统一 registry 展示和渲染配置。

建议路径：

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/plugin/`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/handler/`
- `frontend/src/components/config-panel/`
- `frontend/src/components/node-panel/`

### 7.3 Template Pack

模板包负责沉淀业务场景。

模板包包含：

- 画布 DSL 或 JSON。
- 依赖插件列表。
- 示例输入 payload。
- 预期 dry-run 结果。
- 截图或缩略图。
- 风险等级和使用说明。

模板优先级：

1. 新用户欢迎旅程。
2. 沉睡用户召回。
3. 优惠券审批发布。
4. AI 文案生成与人工审核。
5. 表单线索分配。
6. 生日权益触达。
7. 高价值用户维护。
8. A/B 实验触达。
9. 风险触达拦截。
10. 私域跟进旅程。

建议路径：

- `docs/canvas-examples/`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/`
- `frontend/src/pages/canvas-list/templateCatalog.ts`

### 7.4 Canvas DSL

Canvas DSL 让旅程可以用文本表达、版本管理、AI 生成和 CLI 操作。

首版 DSL 支持：

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
    - id: coupon
      type: coupon.grant
      config:
        couponKey: "NEW_USER_10"
    - id: message
      type: message.send
      config:
        channel: "mock_sms"
        template: "welcome_coupon"
  edges:
    - from: segment
      to: coupon
      when: true
    - from: coupon
      to: message
```

DSL 必须支持：

- validate：结构和插件依赖校验。
- import：导入为画布草稿。
- export：从现有画布导出 DSL。
- diff：比较两个 DSL 或两个环境。

建议路径：

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/template/`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/dto/canvas/`
- 新建 `tools/canvas-cli/` 或 `tools/marketingops-cli/`

### 7.5 CLI

CLI 作为开发者入口。

首版命令：

```bash
canvas validate journey.yaml
canvas import journey.yaml --tenant default
canvas export --canvas-id 123 --output journey.yaml
canvas diff journey-a.yaml journey-b.yaml
canvas publish --canvas-id 123 --env local
```

CLI 首版可以是 Node.js 脚本，复用当前仓库已有 `tools/*.mjs` 风格。

建议路径：

- `tools/canvas-cli/package.json`
- `tools/canvas-cli/src/index.mjs`
- `tools/canvas-cli/src/commands/*.mjs`
- `tools/canvas-cli/test/*.test.mjs`

### 7.6 AI-native Experience

AI 不是单个节点，而是一组围绕营销旅程的操作器。

首版能力：

- 根据自然语言生成 Journey DSL 草稿。
- 检查旅程风险：无频控、无审批、优惠券过量、触达过密、无退出路径。
- 生成触达文案和 A/B 版本。
- 根据执行 trace 解释失败原因。
- 根据当前节点推荐下一步节点。

AI 首版必须支持 mock 模式，避免 demo 依赖真实模型 key。

建议路径：

- `backend/canvas-engine/src/main/java/org/chovy/canvas/engine/llm/`
- `backend/canvas-engine/src/main/java/org/chovy/canvas/domain/ai/`
- `frontend/src/pages/ai-predictions/`
- `frontend/src/pages/canvas-editor/`

### 7.7 Playground

Playground 目标是降低首次体验成本。

可选实现路径：

- 本地沙盒模式：`docker compose -f docker-compose.demo.yml up`。
- 公开只读/临时沙盒：后续部署到公网。

首版必须具备：

- 模板浏览。
- 画布拖拽。
- dry-run。
- trace 查看。
- DSL 导出。
- AI mock 生成旅程。

## 8. 数据流

### 8.1 用户首次体验

```text
README -> docker compose up -> Demo Workspace -> 选择模板 -> dry-run -> 查看 trace -> 导出 DSL
```

### 8.2 插件开发

```text
create plugin -> 填写 plugin.json -> 实现 NodeHandler/Provider -> 添加 config schema -> 添加 mock -> 添加模板 -> 运行 contract test
```

### 8.3 DSL 工作流

```text
journey.yaml -> CLI validate -> import draft -> editor preview -> dry-run -> publish -> export/diff
```

### 8.4 AI 工作流

```text
自然语言目标 -> AI 生成 DSL -> 风险审计 -> 用户确认 -> 导入画布 -> dry-run -> 调整发布
```

## 9. 验收标准

### 9.1 开源入口

- 根目录存在 `LICENSE`、`CONTRIBUTING.md`、`CODE_OF_CONDUCT.md`、`SECURITY.md`。
- README 首屏包含定位、截图/GIF、快速启动、模板入口和插件开发入口。
- `.github/` 包含 issue 和 PR 模板。

### 9.2 Demo

- 新机器执行一条命令后可访问前端。
- 默认账号登录后能看到示例旅程。
- 至少 3 个旅程可以 dry-run 并展示 trace。

### 9.3 插件

- 插件 manifest 可被后端读取和校验。
- 插件可通过配置启停。
- 至少 6 个官方插件在 demo 中可用。
- 插件配置可以由 schema 渲染。

### 9.4 模板

- 至少 10 个模板可导入。
- 每个模板声明依赖插件。
- 每个模板有示例 payload 和 dry-run 预期。

### 9.5 DSL/CLI

- `canvas validate` 能校验合法和非法 DSL。
- `canvas import` 能生成画布草稿。
- `canvas export` 能从画布导出 DSL。
- `canvas diff` 能展示结构变化。

### 9.6 AI

- AI mock 模式不依赖外部 key。
- AI 能生成至少 3 类旅程草稿。
- AI 风险审计能识别至少 5 类风险。
- AI 失败解释能基于 trace 给出结构化原因。

## 10. 风险和约束

### 10.1 范围膨胀

风险：继续把 CDP、BI、数据仓库、广告投放等能力推到主叙事，导致外部用户难以理解。

控制：README 和 demo 只讲 Journey Canvas、插件、模板、DSL、AI。

### 10.2 插件体系过度设计

风险：过早做运行时热加载、复杂 marketplace 和 classloader 隔离。

控制：半年内只做构建期插件 + 配置启停 + manifest 校验。

### 10.3 Demo 依赖过重

风险：用户需要配置多个真实外部系统。

控制：所有外部系统默认 mock，真实 provider 作为高级配置。

### 10.4 AI 依赖不可控

风险：没有模型 key 时无法演示。

控制：AI 首版必须支持 mock provider，真实 provider 可选。

### 10.5 工程主线被重构拖慢

风险：完整 DDD、微服务拆分、运行时插件加载占用半年窗口。

控制：只做局部边界收敛，避免大规模重构。

## 11. 发布策略

建议以 3 个公开里程碑发布：

- `v0.1 Open Source Demo`：README、一键 demo、模板、基础插件。
- `v0.2 Plugin and Template SDK`：插件 manifest、schema 配置、模板包、插件脚手架。
- `v0.3 MarketingOps as Code`：Canvas DSL、CLI、AI 生成与审计、Playground。
