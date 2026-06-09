# Open Source Growth 专项

日期：2026-06-08

## 目标

本专项用于把当前 Marketing Canvas 项目从“功能厚重的企业内部平台仓库”改造成“对外可理解、可试用、可扩展、可贡献的开源营销自动化平台”。

半年目标不是做完完整营销云，而是形成一个清晰的开源产品闭环：

```text
一键 demo -> 可视化 Journey Canvas -> 插件节点 -> 模板库 -> Canvas DSL/CLI -> AI 生成与审计 -> Playground/文档传播
```

## 文件说明

- [open-source-growth-spec.md](./open-source-growth-spec.md)：专项设计规格，定义定位、范围、架构边界、交付物和验收标准。
- [open-source-growth-plan.md](./open-source-growth-plan.md)：半年实施计划，按阶段拆解到现有代码目录、测试和文档工作。
- [implementation-guardrails.md](./implementation-guardrails.md)：实施护栏，约束大模型或多人执行时的范围、复用、禁止事项和人工决策点。
- [traceability-matrix.md](./traceability-matrix.md)：需求到 plan、PR、测试和 demo 证据的追踪矩阵。
- [phase-gates.md](./phase-gates.md)：每个月必须通过的阶段门禁和停止条件。
- [decision-log.md](./decision-log.md)：方向性决策记录，避免后续任务推翻已确认边界。
- [contracts/README.md](./contracts/README.md)：插件、节点、模板、DSL、demo 和 AI 的契约冻结入口。
- [milestone-roadmap.md](./milestone-roadmap.md)：按 6 个月排期的里程碑、退出条件和风险控制。
- [success-metrics.md](./success-metrics.md)：成功指标、埋点、社区健康度和发布检查表。

## 相关专项协调

- [Program Coordination](../program-coordination/README.md)：协调本专项与 DDD 模块化重写的先后顺序、冲突矩阵、共享契约和并行执行规则。

## 推荐定位

中文：

> Canvas 是一个开源、插件驱动、AI 辅助的营销自动化平台，用可视化 Journey Canvas 和 MarketingOps as Code 编排客户旅程、触达、审批、权益和增长实验。

英文：

> Canvas is an open-source, plugin-driven marketing automation platform for building customer journeys with a visual canvas, MarketingOps as Code, and AI-assisted campaign operations.

## 半年主线

1. 开源产品化入口：README、license、贡献指南、一键 demo、截图/GIF。
2. 轻量插件体系：节点插件、Provider 插件、模板包、插件 manifest。
3. 模板库：10 个可演示营销旅程，覆盖新客、召回、权益、审批、AI 文案。
4. Canvas DSL 和 CLI：让旅程可用 YAML 管理、校验、导入、导出、diff。
5. AI-native 体验：自然语言生成旅程、风险审计、文案生成、失败解释。
6. Playground 和生态包装：沙盒体验、英文文档、插件开发脚手架、传播内容。

## 不做事项

半年内不把以下事项作为主目标：

- 真正运行时 jar 热加载。
- 大规模微服务拆分。
- 完整 DDD 重构。
- 插件市场交易系统。
- 全量 CDP、BI、广告投放闭环。
- 托管云商业化完整闭环。
