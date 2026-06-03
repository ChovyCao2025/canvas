# Marketing Canvas — Architecture Index

本目录包含 Marketing Canvas 营销画布平台的架构文档体系。

## 核心架构文档

| 文档 | 说明 |
|------|------|
| [brownfield-architecture.md](brownfield-architecture.md) | 棕地架构增强文档 — 技术栈对齐、组件架构、安全集成、风险评估 |
| [brownfield-service-workflow.md](../code%20review/brownfield-service-workflow.md) | 棕地服务工作流执行报告 — Epic/Story规划、实现顺序 |
| [architect-checklist-report.md](architect-checklist-report.md) | 架构师检查清单验证报告 — 10章节通过率、Top 8风险 |

## 架构子文档

| 文档 | 说明 |
|------|------|
| [tech-stack.md](tech-stack.md) | 技术栈与版本约束 |
| [backend-architecture.md](backend-architecture.md) | 后端架构模式与结构 |
| [frontend-architecture.md](frontend-architecture.md) | 前端架构模式与结构 |
| [database-schema.md](database-schema.md) | 数据库设计与关键表 |
| [coding-standards.md](coding-standards.md) | 编码规范与约定 |
| [testing-strategy.md](testing-strategy.md) | 测试策略与要求 |
| [security-considerations.md](security-considerations.md) | 安全模式与要求 |
| [api-spec-summary.md](api-spec-summary.md) | API端点概览 |
| [deployment-guide.md](deployment-guide.md) | 部署与运维指南 |

## 阅读顺序

1. **新人入门:** tech-stack → backend-architecture → frontend-architecture → coding-standards
2. **安全审计:** security-considerations → architect-checklist-report → brownfield-architecture (安全章节)
3. **架构重构:** architect-checklist-report → brownfield-architecture → brownfield-service-workflow → backend-architecture
4. **Bug排查:** database-schema → api-spec-summary → deployment-guide

## 外部参考

| 文档 | 位置 |
|------|------|
| 营销平台能力缺项(148项) | docs/optimization/marketing_platform_gap_analysis.md |
| 架构深度审查(15个技术选型问题) | docs/optimization/architecture-deep-review-2026-05.md |
| 技术选型白皮书 | docs/optimization/tech-selection-whitepaper.md |
| 148项优先级审核 | docs/optimization/todo/marketing_platform_gap_analysis.md |