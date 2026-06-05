# Marketing Canvas 架构文档

这个目录现在只保留四个顶层分类：

- [active/](./active/README.md)：当前判断入口，包含实施状态、spec/plan 导航和已审核来源包。
- [decisions/](./decisions/README.md)：架构决策，包含 ADR、P3 决策产物和当前参考资料。
- [evidence/](./evidence/README.md)：验证证据，包含审计证据、容量/合规/依赖/前端/测试证据、迁移证据、runbook 和指南。
- [archive/](./archive/README.md)：历史材料和已完成的 spec/plan 归档。

判断文档状态时，以 `archive/completed/` 作为“已完成 spec/plan”的正式记录；以 `active/status/implementation-status.md` 判断工程是否落地；以 `active/reviewed-packages/` 作为“已审核来源包”的追溯记录；以 `decisions/work-products/` 存放支撑性的决策、清单、矩阵和检查表。旧的评审和整改文档已经抽取主要问题并重新核实，原文保留在归档目录中。

## 状态说明

- 已完成的文档包：`archive/completed/specs/` 和 `archive/completed/plans/`。
- 工程实施状态：`active/status/implementation-status.md`。
- 已完成 P3 包的支撑产物：`decisions/work-products/`。
- 已审核但仅用于追溯的来源包：`active/reviewed-packages/`。
- 尚未采纳、过期或依赖外部决策的问题：仅在 `active/reviewed-packages/needs-review/`。

## 已审核来源包

| 优先级 | 关注范围 |
|---|---|
| [P0](./active/reviewed-packages/p0) | 安全、正确性、生产可用性、并发和数据隔离 |
| [P1](./active/reviewed-packages/p1) | API 契约、DAG 边界、前端画布状态、可观测性和运维 |
| [P2](./active/reviewed-packages/p2) | 测试基础、容量与成本、文档、依赖抽象、合规和前端质量 |
| [P3](./active/reviewed-packages/p3) | 平台演进决策包和推进门禁 |
| [needs-review](./active/reviewed-packages/needs-review) | 过期、重复或依赖外部决策的问题 |

## 包索引

P0：
- [安全加固](./active/reviewed-packages/p0/security-hardening/spec.md)
- [响应式线程和事务](./active/reviewed-packages/p0/reactive-threading-and-transactions/spec.md)
- [画布状态与数据一致性](./active/reviewed-packages/p0/canvas-state-data-consistency/spec.md)
- [执行并发安全](./active/reviewed-packages/p0/execution-concurrency-safety/spec.md)
- [生产韧性与灾备](./active/reviewed-packages/p0/production-resilience-and-dr/spec.md)
- [数据安全与租户隔离](./active/reviewed-packages/p0/data-security-and-tenant-isolation/spec.md)

P1：
- [DAG 引擎与 Handler 边界](./active/reviewed-packages/p1/dag-engine-and-handler-boundaries/spec.md)
- [API 契约与校验](./active/reviewed-packages/p1/api-contract-and-validation/spec.md)
- [前端画布状态](./active/reviewed-packages/p1/frontend-canvas-state/spec.md)
- [可观测性与运维](./active/reviewed-packages/p1/observability-and-ops/spec.md)
- [发布部署治理](./active/reviewed-packages/p1/release-deployment-governance/spec.md)

P2：
- [测试基础](./active/reviewed-packages/p2/testing-foundation/spec.md)
- [容量、成本与保留策略](./active/reviewed-packages/p2/cost-capacity-and-retention/spec.md)
- [文档、ADR 与 Runbook](./active/reviewed-packages/p2/documentation-adr-and-runbooks/spec.md)
- [依赖抽象与供应商锁定](./active/reviewed-packages/p2/dependency-abstraction-and-vendor-lock-in/spec.md)
- [合规与数据治理](./active/reviewed-packages/p2/compliance-data-governance/spec.md)
- [前端可访问性与质量](./active/reviewed-packages/p2/frontend-accessibility-and-quality/spec.md)

P3：
- [平台演进来源包](./active/reviewed-packages/p3/platform-evolution/spec.md)
- [平台演进推进检查表](./decisions/work-products/p3-01-platform-evolution/platform-evolution-promotion-checklist.md)
- [P3-00 架构边界评审](./archive/completed/specs/P3-00-architecture-boundary-review-spec.md)
- [P3-01 平台演进总览](./archive/completed/specs/P3-01-platform-evolution-spec.md)
- [P3-02 服务拆分与领域边界](./archive/completed/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md)
- [P3-03 数据中台架构](./archive/completed/specs/P3-03-data-platform-architecture-spec.md)
- [P3-04 多数据源隔离](./archive/completed/specs/P3-04-multi-datasource-isolation-spec.md)
- [P3-05 WebFlux 到 MVC 迁移](./archive/completed/specs/P3-05-webflux-to-mvc-migration-spec.md)
- [P3-06 K8s 部署平台](./archive/completed/specs/P3-06-k8s-deployment-platform-spec.md)
- [P3-07 生产平台组件](./archive/completed/specs/P3-07-production-platform-components-spec.md)
- [P3-08 企微 SCRM 模块](./archive/completed/specs/P3-08-wecom-scrm-module-spec.md)
- [P3-09 身份、事件与租户平台](./archive/completed/specs/P3-09-identity-event-and-tenant-platform-spec.md)

## 验证说明

工程实施状态见 [status/implementation-status.md](./active/status/implementation-status.md)，证据汇总见 [reviewed-packages/verification-summary.md](./active/reviewed-packages/verification-summary.md)，来源到包的覆盖关系见 [reviewed-packages/coverage-matrix.md](./active/reviewed-packages/coverage-matrix.md)，代码级架构边界核查见 [archive/completed/specs/P3-00-architecture-boundary-code-verification.md](./archive/completed/specs/P3-00-architecture-boundary-code-verification.md)。关键修正包括：

- JWT secret 启动校验已经存在，剩余问题是部署配置强制校验。
- 测试已经存在，剩余问题是关键路径和集成覆盖不足。
- 部分 Redis 路由一致性问题已有修复，剩余工作由状态/数据一致性与生产韧性包覆盖。

## 归档分类

- `archive/reference/`：稳定的架构参考文档。
- `archive/reviews/`：历史评审报告和检查清单。
- `archive/remediation/`：原始整改索引和问题分篇。
- `archive/evolution/`：长期架构演进材料。
- `archive/completed/`：已完成的优先级 spec 和 plan。
