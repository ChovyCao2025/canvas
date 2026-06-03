# Docs 导航索引

> 本文件为 AI/大模型提供快速定位能力。每个条目包含文件路径 + 一句话摘要，按主题分类。

---

## 一、核心设计文档

- [marketing-canvas-design.md](architecture/marketing-canvas-design.md) — 营销画布核心技术设计文档（251KB，最完整的系统设计说明）
- [tech-selection-whitepaper.md](product-evolution/tech-selection-whitepaper.md) — 技术选型白皮书（134KB，中间件/框架对比与决策依据）

## 二、架构文档

### 基础架构
- [architecture/index.md](architecture/index.md) — 架构文档目录索引
- [architecture/backend-architecture.md](architecture/backend-architecture.md) — 后端架构：单体 WebFlux + MyBatis-Plus + Disruptor
- [architecture/frontend-architecture.md](architecture/frontend-architecture.md) — 前端架构：React 18 + Vite + React Flow 画布编辑器
- [architecture/database-schema.md](architecture/database-schema.md) — 数据库 Schema：MySQL 8.0 + 89 个 Flyway 迁移
- [architecture/tech-stack.md](architecture/tech-stack.md) — 技术栈全览与版本约束
- [architecture/api-spec-summary.md](architecture/api-spec-summary.md) — API 规范摘要：29 个 Controller，173 个端点
- [architecture/deployment-guide.md](architecture/deployment-guide.md) — 部署指南：本地开发 + Docker Compose
- [architecture/security-considerations.md](architecture/security-considerations.md) — 安全考量：3 个 CRITICAL 级别缺口
- [architecture/testing-strategy.md](architecture/testing-strategy.md) — 测试策略：后端 112 文件，前端 30 文件
- [architecture/coding-standards.md](architecture/coding-standards.md) — 编码规范：Java + TypeScript 命名约定

### 架构审查与评估
- [architecture/architecture-deep-review-2026-05.md](architecture/architecture-deep-review-2026-05.md) — 架构深度审查（48KB）：代码实证 + 数据实证 + 前端/基础设施补充
- [architecture/architecture-supplement-review-2026-05.md](architecture/architecture-supplement-review-2026-05.md) — 架构评审补充：10 个未覆盖维度
- [architecture/architecture-constraints-risks-2026-06-02.md](architecture/architecture-constraints-risks-2026-06-02.md) — 架构约束与风险评估
- [architecture/architect-checklist-report.md](architecture/architect-checklist-report.md) — 架构师检查清单综合验证报告
- [architecture/brownfield-architecture.md](architecture/brownfield-architecture.md) — 棕地架构增强文档：基于现有代码库的系统性蓝图
- [architecture/production-deployment-checklist-2026-06-02.md](architecture/production-deployment-checklist-2026-06-02.md) — 生产部署检查清单

### 架构演进方案
- [architecture/evolution/target-architecture-overview.md](architecture/evolution/target-architecture-overview.md) — 目标架构总览：单体 → 模块化私域运营中台
- [architecture/evolution/service-architecture-design.md](architecture/evolution/service-architecture-design.md) — 服务划分方案：哪些拆独立服务、服务间通信
- [architecture/evolution/architecture-evolution-roadmap.md](architecture/evolution/architecture-evolution-roadmap.md) — 架构演进路线图：4 阶段 12 个月
- [architecture/evolution/multi-datasource-isolation.md](architecture/evolution/multi-datasource-isolation.md) — 多数据源隔离方案：65 表 → 3 库
- [architecture/evolution/webflux-to-mvc-migration.md](architecture/evolution/webflux-to-mvc-migration.md) — WebFlux → Spring MVC + Virtual Threads 迁移方案
- [architecture/evolution/data-platform-architecture.md](architecture/evolution/data-platform-architecture.md) — 数据中台架构方案
- [architecture/evolution/wecom-scrm-module-design.md](architecture/evolution/wecom-scrm-module-design.md) — 企微 SCRM 模块详细设计
- [architecture/evolution/k8s-deployment-plan.md](architecture/evolution/k8s-deployment-plan.md) — K8s 部署方案
- [architecture/evolution/production-practice-review.md](architecture/evolution/production-practice-review.md) — 生产实践补审：技术选型与开源组件对标
- [architecture/evolution/architect-critical-review.md](architecture/evolution/architect-critical-review.md) — 架构师审查：8 个上线前必须解决的关键缺失

### 架构整改方案
- [architecture/architecture-remediation-plan-2026-05.md](architecture/architecture-remediation-plan-2026-05.md) — 架构问题整改总索引（已拆分为 7 个分册）
- [architecture/remediation/README.md](architecture/remediation/README.md) — 整改方案总览：35 类问题、430+ 项缺陷
- [architecture/remediation/part1-structure.md](architecture/remediation/part1-structure.md) — 整改一：单体巨石无服务边界
- [architecture/remediation/part2-security-concurrency.md](architecture/remediation/part2-security-concurrency.md) — 整改二：安全与并发（31 项）
- [architecture/remediation/part3-frontend.md](architecture/remediation/part3-frontend.md) — 整改三：前端架构（23 项）
- [architecture/remediation/part4-ops.md](architecture/remediation/part4-ops.md) — 整改四：运维与基础设施（19 项）
- [architecture/remediation/part5-engine-deep.md](architecture/remediation/part5-engine-deep.md) — 整改五：深度结构设计（4 大维度）
- [architecture/remediation/part6-logic-testing.md](architecture/remediation/part6-logic-testing.md) — 整改六：业务逻辑正确性 + 测试
- [architecture/remediation/part7-resilience.md](architecture/remediation/part7-resilience.md) — 整改七：韧性/容错/可观测性

## 三、产品演进

### 产品战略
- [product-evolution/product-strategy-dual-track-2026-05-31.md](product-evolution/product-strategy-dual-track-2026-05-31.md) — 产品战略方案（163KB）：双轨并行，稳定 60% + 创新 40%，5 阶段 12 个月
- [product-evolution/product-strategy-supplementary-dimensions-2026-05-31.md](product-evolution/product-strategy-supplementary-dimensions-2026-05-31.md) — 产品战略补充维度（F-O）：8 个补充维度快速参考

### 产品演进方向
- [product-evolution/product-evolution-directions-2026-05-31.md](product-evolution/product-evolution-directions-2026-05-31.md) — 10 大演进方向深度方案（45KB）
- [product-evolution/product-evolution-directions-ext-2026-05-31.md](product-evolution/product-evolution-directions-ext-2026-05-31.md) — 演进方向扩展（11-15）：生态合作/客户旅程/运营知识/体验设计/数据驱动
- [product-evolution/product-interaction-directions-2026-06-01.md](product-evolution/product-interaction-directions-2026-06-01.md) — 交互设计演进（16-22）：画布交互/协作/信息架构/微文案/移动端
- [product-evolution/product-interaction-directions-2026-06-02.md](product-evolution/product-interaction-directions-2026-06-02.md) — 交互设计演进（23-29）：性能/无障碍/动效/错误处理/表单/表格/可视化

### 产品能力评估
- [product-evolution/product-audit-report-2026-05-31.md](product-evolution/product-audit-report-2026-05-31.md) — 产品审核报告：前端 UX + 后端 API + 竞品差距
- [product-evolution/product-best-practice-roadmap-2026-05-31.md](product-evolution/product-best-practice-roadmap-2026-05-31.md) — 产品最佳实践路线图：15 项竞品实践全量采纳
- [product-evolution/production-design-gaps.md](product-evolution/production-design-gaps.md) — 生产级设计问题清单：架构与设计层面的生产环境差距
- [product-evolution/production-readiness-checklist.md](product-evolution/production-readiness-checklist.md) — 生产级优化清单：按优先级分层

### 竞品对标
- [product-evolution/mautic-comparison-2026-06.md](product-evolution/mautic-comparison-2026-06.md) — Mautic 平台对比分析报告
- [product-evolution/mautic-capabilities-to-adopt.md](product-evolution/mautic-capabilities-to-adopt.md) — Mautic 值得借鉴能力清单（P0-P2）
- [product-evolution/mautic-plugin-feasibility-analysis.md](product-evolution/mautic-plugin-feasibility-analysis.md) — Mautic 插件体系可行性分析
- [optimizer/n8n-mica-comparison-2026-06.md](optimizer/n8n-mica-comparison-2026-06.md) — 对标分析：n8n + MiCA OSS 借鉴能力
- [product-evolution/plugin-candidate-list.md](product-evolution/plugin-candidate-list.md) — Canvas 官方插件候选清单

### 产品文档
- [product-docs/01-p0-gap-priority.md](product-docs/01-p0-gap-priority.md) — 产品缺口优先级分析报告
- [product-docs/02-product-roadmap.md](product-docs/02-product-roadmap.md) — 产品演进路线图

## 四、代码审查与审计

### 深度代码审计（13 轮）
- [code-review/deep-code-audit-all-rounds-summary.md](code-review/deep-code-audit-all-rounds-summary.md) — 13 轮完整汇总：覆盖 7 大维度
- [code-review/deep-code-audit-round2.md](code-review/deep-code-audit-round2.md) — Round 2：并发安全、Reactor 合规、数据正确性、异常处理、资源泄漏
- [code-review/deep-code-audit-round3.md](code-review/deep-code-audit-round3.md) — Round 3：多租户隔离、Handler→Mapper 分层违规、DataSourceConfig 密码暴露
- [code-review/deep-code-audit-round4.md](code-review/deep-code-audit-round4.md) — Round 4：配置安全、输入验证、分页/导出安全、时区处理
- [code-review/deep-code-audit-round5.md](code-review/deep-code-audit-round5.md) — Round 5：HTTP 客户端安全、连接池管理、Actuator 暴露、数据脱敏
- [code-review/deep-code-audit-round6.md](code-review/deep-code-audit-round6.md) — Round 6：类型安全、ExecutionContext 线程安全、前端性能
- [code-review/deep-code-audit-round7.md](code-review/deep-code-audit-round7.md) — Round 7：架构配置/安全/运维/事务/可观测性
- [code-review/deep-code-audit-round8.md](code-review/deep-code-audit-round8.md) — Round 8：Flyway 迁移安全、API 版本策略、数据保留、CORS
- [code-review/deep-code-audit-round9.md](code-review/deep-code-audit-round9.md) — Round 9：前端安全、Groovy 沙箱逃逸、Redis Lua 脚本正确性
- [code-review/deep-code-audit-round10.md](code-review/deep-code-audit-round10.md) — Round 10：依赖漏洞扫描、配置加密、密钥轮换、日志脱敏
- [code-review/deep-code-audit-round11.md](code-review/deep-code-audit-round11.md) — Round 11：Handler 边缘条件、资源泄漏、SSRF、线程池生命周期
- [code-review/deep-code-audit-round12.md](code-review/deep-code-audit-round12.md) — Round 12：API 响应泄露、前端依赖安全、乐观锁覆盖
- [code-review/deep-code-audit-round13.md](code-review/deep-code-audit-round13.md) — Round 13：WebSocket 安全、MQ 消费者幂等性、定时任务并发

### 综合审计报告
- [code-review/deep-code-audit-2026-05-31.md](code-review/deep-code-audit-2026-05-31.md) — 深度代码审核总报告（45KB）：21 项 P0 + 39 项 P1
- [code-review/code-review-logic-bugs-2026-06.md](code-review/code-review-logic-bugs-2026-06.md) — 代码审查：逻辑错误与隐藏 Bug（并发/事务/Reactor/幂等性）

### 专项审计
- [code-review/api-design-standards-audit-2026-06-02.md](code-review/api-design-standards-audit-2026-06-02.md) — API 设计与错误处理规范审计
- [code-review/failed-config-check-report-2026-06-02.md](code-review/failed-config-check-report-2026-06-02.md) — 配置安全检查报告
- [code-review/infrastructure-security-scan-2026-06-02.md](code-review/infrastructure-security-scan-2026-06-02.md) — 基础设施与部署安全分析
- [code-review/main-branch-review.md](code-review/main-branch-review.md) — main 分支代码审查（2026-06-02）
- [code-review/brownfield-service-workflow.md](code-review/brownfield-service-workflow.md) — 棕地服务增强工作流执行报告
- [optimization/bmad-product-review-2026-05.md](optimization/bmad-product-review-2026-05.md) — BMAD 产品设计合理性审查：PM Checklist + 竞品 + UI/UX

## 五、优化清单

- [optimization/optimization_list_v7.md](optimization/optimization_list_v7.md) — 当前活跃优化清单（疲劳度控制等）
- [optimization/archive/optimization_list_v1.md](optimization/archive/optimization_list_v1.md) — 优化清单 v1（画布撤销/事件配置）
- [optimization/archive/optimization_list_v2.md](optimization/archive/optimization_list_v2.md) — 优化清单 v2（事件配置概念）
- [optimization/archive/optimization_list_v3.md](optimization/archive/optimization_list_v3.md) — 优化清单 v3（标签配置化）
- [optimization/archive/optimization_list_v4.md](optimization/archive/optimization_list_v4.md) — 优化清单 v4（画布删除功能）
- [optimization/archive/optimization_list_v5.md](optimization/archive/optimization_list_v5.md) — 优化清单 v5（Service 接口规范化）
- [optimization/archive/optimization_list_v6.md](optimization/archive/optimization_list_v6.md) — 优化清单 v6（旅程运行平台能力补强）
- [optimization/archive/2000并发目标专项-plan.md](optimization/archive/2000并发目标专项-plan.md) — 2000+ 并发目标专项实施计划
- [optimization/archive/2000并发目标专项-spec.md](optimization/archive/2000并发目标专项-spec.md) — 2000+ 并发目标专项规格说明
- [optimization/archive/3000-concurrency-hardening-checklist.md](optimization/archive/3000-concurrency-hardening-checklist.md) — 3000 并发加固检查清单
- [optimization/archive/4000-concurrency-readiness-checklist.md](optimization/archive/4000-concurrency-readiness-checklist.md) — 4000 并发就绪检查清单

## 六、待办与路线图

### 路线图
- [optimization/todo/2026-05-30-marketing-platform-roadmap.md](optimization/todo/2026-05-30-marketing-platform-roadmap.md) — 营销平台 35 项能力缺项实施路线图
- [optimization/todo/2026-05-30-cdp-roadmap.md](optimization/todo/2026-05-30-cdp-roadmap.md) — CDP 能力补齐实施路线图
- [optimization/todo/2026-05-31-ai-capability-roadmap.md](optimization/todo/2026-05-31-ai-capability-roadmap.md) — AI 能力追赶路线图（3 阶段）
- [optimization/todo/2026-05-31-evolution-directions.md](optimization/todo/2026-05-31-evolution-directions.md) — 项目演进方向分析

### 差距分析
- [optimization/todo/marketing_platform_gap_analysis.md](optimization/todo/marketing_platform_gap_analysis.md) — 营销平台能力缺项全景：35 项能力缺项（140KB）
- [optimization/todo/cdp_gap_analysis.md](optimization/todo/cdp_gap_analysis.md) — CDP 能力缺项全景：20 项缺项
- [optimization/todo/competitor-analysis-report.md](optimization/todo/competitor-analysis-report.md) — 竞品分析报告：11 家竞品 × 10 个维度
- [optimization/todo/market-research-report.md](optimization/todo/market-research-report.md) — 市场研究报告
- [optimization/todo/plan-review-findings.md](optimization/todo/plan-review-findings.md) — 28 个 spec+plan 二次审查问题清单

### 设计规格
- [optimization/todo/2026-05-30-cdp-sdk-design.md](optimization/todo/2026-05-30-cdp-sdk-design.md) — CDP 埋点数据采集 SDK 设计规格（Web/iOS/Android）

### 实施计划（plans）
- [optimization/todo/plans/](optimization/todo/plans/) — 29 个技术改进 plan（含单体拆分、WebFlux 迁移、Groovy 替换等）
- [optimization/todo/plans/evolution-index/](optimization/todo/plans/evolution-index/) — 50 个产品演进方向 plan（direction-1 ~ direction-50）

### 技术规格（specs）
- [optimization/todo/specs/](optimization/todo/specs/) — 28 个技术改进 spec + 2 个产品演进 spec

## 七、历史规划（superpowers）

- [superpowers/specs/INDEX.md](superpowers/specs/INDEX.md) — Superpowers Specs 索引
- [superpowers/plans/archive/](superpowers/plans/archive/) — 已归档 plan（44 个，2026-05-16 ~ 2026-05-27）
- [superpowers/plans/todo/](superpowers/plans/todo/) — 待处理 plan（1 个：AI LLM 节点）
- [superpowers/specs/archive/](superpowers/specs/archive/) — 已归档 spec（44 个，2026-05-16 ~ 2026-05-28）
- [superpowers/specs/todo/](superpowers/specs/todo/) — 待处理 spec（1 个：AI LLM 节点设计）

## 八、示例与参考

- [canvas-examples/CanvasUse.md](canvas-examples/CanvasUse.md) — 画布使用说明
- [canvas-examples/scenarios.md](canvas-examples/scenarios.md) — 画布场景示例
- [canvas-examples/combinations.md](canvas-examples/combinations.md) — 节点组合示例
- [canvas-examples/components.md](canvas-examples/components.md) — 组件使用示例
- [canvas-examples/README.md](canvas-examples/README.md) — 示例目录说明

## 九、压测

- [stressTest/2026-05-27-local-container-capacity-testing-design.md](stressTest/2026-05-27-local-container-capacity-testing-design.md) — 本地容器容量测试设计
- [stressTest/并发量评估报告.md](stressTest/并发量评估报告.md) — 并发量评估报告
- [stressTest/老板汇报版-并发评估摘要.md](stressTest/老板汇报版-并发评估摘要.md) — 并发评估摘要（管理层版）

## 十、图片资源

- [pics/](pics/) — 文档引用图片（img.png ~ img15.png）

---

## 快速导航：按问题域查找

| 你想了解... | 先看这些文件 |
|------------|-------------|
| 系统整体设计 | `architecture/marketing-canvas-design.md` |
| 技术选型理由 | `product-evolution/tech-selection-whitepaper.md` |
| 当前架构问题 | `architecture/architecture-deep-review-2026-05.md` |
| 架构如何演进 | `architecture/evolution/target-architecture-overview.md` |
| 代码质量问题 | `code-review/deep-code-audit-all-rounds-summary.md` |
| 安全漏洞 | `code-review/deep-code-audit-round9.md` + `round10.md` |
| 产品战略方向 | `product-evolution/product-strategy-dual-track-2026-05-31.md` |
| 产品功能缺什么 | `optimization/todo/marketing_platform_gap_analysis.md` |
| 竞品对比 | `optimization/todo/competitor-analysis-report.md` |
| 并发/性能 | `optimization/archive/2000并发目标专项-plan.md` |
| 部署上线 | `architecture/production-deployment-checklist-2026-06-02.md` |
| 生产环境差距 | `product-evolution/production-design-gaps.md` |
| 实施方案 | `optimization/todo/plans/` 目录 |
| 前端架构 | `architecture/frontend-architecture.md` |
| 后端架构 | `architecture/backend-architecture.md` |
| 数据库设计 | `architecture/database-schema.md` |
