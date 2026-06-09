# Open Source Growth Decision Log

日期：2026-06-08

## 目的

本文件记录 Open Source Growth 专项中会影响方向、范围或架构边界的决策。大模型或多人执行时不得推翻已记录决策；如需修改，必须新增 superseding decision，并说明原因。

## Decision Format

```text
ID:
Status:
Decision:
Reason:
Rejected:
Applies To:
Review:
```

## Decisions

### DEC-OSG-001：半年内只做构建期插件

Status: Accepted

Decision: 半年内插件体系只支持随主应用构建、Spring Bean 注册、manifest 校验和配置启停。

Reason: 当前目标是降低开源上手和贡献门槛，不是构建复杂运行时插件平台。运行时 jar 热加载会引入 classloader、安全、版本、回滚和隔离风险。

Rejected: 运行时 jar 热加载、PF4J、自定义 classloader、在线安装插件。

Applies To: Month 2, Month 3, Month 4。

Review: v0.3 发布后重新评估。

### DEC-OSG-002：插件注册必须承接现有 PluginRegistryService

Status: Accepted

Decision: 插件注册能力必须优先扩展 `PluginRegistryService`、`JdbcPluginRepository` 和 `built_in_plugin_registry`，不得直接新建平行注册中心。

Reason: 当前仓库已经存在插件注册服务、数据库表和 controller。重复建设会导致启停状态、API、UI 和测试分裂。

Rejected: 在 `engine.plugin` 下新增第二套同职责 registry 并让新插件只接入新 registry。

Applies To: Month 2。

Review: 如果旧服务被明确废弃并完成迁移计划，可以用新决策替代。

### DEC-OSG-003：DSL v1 只覆盖 Golden Path 节点集合

Status: Accepted

Decision: Canvas DSL v1 只覆盖 webhook trigger、condition、message、coupon、approval、ai、risk-check、end。

Reason: DSL 首版目标是支持 demo、模板、CLI 和 AI 草稿，不是表达全部历史节点。过度覆盖会拖慢半年目标并增加契约不稳定性。

Rejected: 一次性覆盖所有 `node_type_registry` 历史节点。

Applies To: Month 4。

Review: v0.3 后按模板使用量扩展节点集合。

### DEC-OSG-004：AI 必须 mock-first

Status: Accepted

Decision: AI 旅程生成、风险审计和 trace 解释必须先支持 mock provider；真实模型 provider 只能作为可选增强。

Reason: 开源 demo 不能要求用户先配置模型 key。mock-first 能保证 README、Playground 和 Golden Path 稳定。

Rejected: demo 依赖真实 OpenAI、Claude、通义、豆包或其他模型 key。

Applies To: Month 5。

Review: 有稳定模型配置和脱敏策略后再扩展真实 provider。

### DEC-OSG-005：License 需要人工确认

Status: Requires Human Decision

Decision: `LICENSE` 不能由大模型擅自选择。

Reason: Apache-2.0 和 AGPL-3.0 对采用、二次开发、商业化和社区贡献影响很大，属于项目治理决策。

Rejected: 由执行 agent 自动选择 license。

Applies To: Month 1。

Review: 开始 Month 1 前确认。

### DEC-OSG-006：主叙事收敛为营销自动化平台

Status: Accepted

Decision: 对外主叙事是“开源、插件驱动、AI 辅助的营销自动化平台”，核心抓手是 Journey Canvas、插件生态、执行治理、MarketingOps as Code。

Reason: “完整营销云”范围过大，会让 README、demo 和模板焦点发散。

Rejected: 将 CDP、BI、广告投放、数据仓库作为首屏主卖点。

Applies To: README、quickstart、Playground、英文文档。

Review: v0.3 发布后根据用户反馈调整。

