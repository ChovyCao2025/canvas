# 架构实施状态台账

本文用于区分“文档已完成”和“工程已完成”。`archive/completed/` 中的 spec/plan 表示设计和实施计划已经整理归档，不表示对应代码已经全部落地。

## 状态口径

- `文档完成`：已有归档 spec/plan 或支撑产物，可作为实施入口。
- `部分完成`：代码中已有部分修复或基础能力，但仍有明确缺口。
- `待实现`：当前仓库证据仍显示问题存在，或只有设计/计划没有工程落地证据。
- `待决策`：需要产品、运维、团队容量或外部环境确认后才能实施。
- `已实现`：需要有代码合并、测试或验证证据；本台账只在证据明确时标记。

## 当前结论

截至本次整理，`docs/architecture` 的文档整理已经完成；但架构问题对应的工程实现没有全部完成。主要未完成项仍集中在 P0/P1 的安全、事务/线程、状态一致性、租户隔离、API 校验、可观测性，以及 P3 的服务拆分、数据中台、多数据源、K8s、WebFlux/MVC 等演进方向。

## P0 高优先级工程项

| 包 | 文档状态 | 工程状态 | 未完成依据 |
|---|---|---|---|
| [P0-01 安全加固](../../archive/completed/specs/P0-01-security-hardening-spec.md) | 文档完成 | 待实现/部分完成 | 默认 datasource 凭据、事件密钥弱默认值、CORS wildcard + credentials、部分接口 `permitAll()`、异常信息外泄仍在验证摘要中列为问题。 |
| [P0-02 响应式线程和事务](../../archive/completed/specs/P0-02-reactive-threading-and-transactions-spec.md) | 文档完成 | 待实现 | `.block()`、fire-and-forget `.subscribe()`、`Thread.sleep()`、WebFlux 与 `@Transactional` 边界共存仍存在。 |
| [P0-03 画布状态与数据一致性](../../archive/completed/specs/P0-03-canvas-state-data-consistency-spec.md) | 文档完成 | 待实现/部分完成 | KILLED canvas 仍可能被发布，已发布 canvas 仍可更新运行限制/有效期；Redis 一致性仍有 rollback、outbox、repair job 缺口。 |
| [P0-04 执行并发安全](../../archive/completed/specs/P0-04-execution-concurrency-safety-spec.md) | 文档完成 | 待实现 | `CircuitBreakerRegistry` 状态切换仍不是单一原子状态机，`CanvasSchedulerService.closed` 仍非 volatile。 |
| [P0-05 生产韧性与灾备](../../archive/completed/specs/P0-05-production-resilience-and-dr-spec.md) | 文档完成 | 待实现/部分完成 | 仍缺完整 in-flight drain、虚拟线程管理、`server.shutdown: graceful`；真实 RTO/RPO 需要外部确认。 |
| [P0-06 数据安全与租户隔离](../../archive/completed/specs/P0-06-data-security-and-tenant-isolation-spec.md) | 文档完成 | 待实现 | 数据源密码仍为明文列，demo migration 有 `root/root`，`tenant_id` 可空，核心 `CanvasDO` 没有 `tenantId`。 |

## P1 高优先级架构项

| 包 | 文档状态 | 工程状态 | 未完成依据 |
|---|---|---|---|
| [P1-01 DAG 引擎与 Handler 边界](../../archive/completed/specs/P1-01-dag-engine-and-handler-boundaries-spec.md) | 文档完成 | 待实现 | `DagEngine`、`CanvasExecutionService` 等仍过大；边界核查指出 handler 直接访问 mapper、跨上下文 import、共享 DAL 所有权等阻塞服务拆分。 |
| [P1-02 API 契约与校验](../../archive/completed/specs/P1-02-api-contract-and-validation-spec.md) | 文档完成 | 待实现 | 29 个 controller 缺少 `@Valid` / `@Validated`，异常响应和 DO 暴露问题仍需收敛。 |
| [P1-03 前端画布状态](../../archive/completed/specs/P1-03-frontend-canvas-state-spec.md) | 文档完成 | 待实现/部分完成 | autosave 旧竞态部分修复，但缺组件/浏览器回归覆盖；`frontend/src/pages/canvas-editor/index.tsx` 仍过大。 |
| [P1-04 可观测性与运维](../../archive/completed/specs/P1-04-observability-and-ops-spec.md) | 文档完成 | 待实现 | Logback 有 `traceId` 格式，但缺对应 MDC/tracing 实现；部分生产运维证据仍不足。 |
| [P1-05 发布部署治理](../../archive/completed/specs/P1-05-release-deployment-governance-spec.md) | 文档完成 | 待实现/部分完成 | 部署配置强制校验、生产拓扑、云部署加固等仍需要工程与环境证据。 |

## P2 基础能力项

| 包 | 文档状态 | 工程状态 | 未完成依据 |
|---|---|---|---|
| [P2-01 测试基础](../../archive/completed/specs/P2-01-testing-foundation-spec.md) | 文档完成 | 待实现/部分完成 | “零测试”已过期，但关键路径、集成测试、Testcontainers 风格 DB/Redis/RocketMQ 覆盖仍不足。 |
| [P2-02 容量、成本与保留策略](../../archive/completed/specs/P2-02-cost-capacity-and-retention-spec.md) | 文档完成 | 待实现/待验证 | 成本模型、容量基线、保留策略需要压测、生产指标和治理证据。 |
| [P2-03 文档、ADR 与 Runbook](../../archive/completed/specs/P2-03-documentation-adr-and-runbooks-spec.md) | 文档完成 | 部分完成 | 已补多份 ADR、runbook 和索引，但后续工程改造仍需持续更新决策记录和操作手册。 |
| [P2-04 依赖抽象与供应商锁定](../../archive/completed/specs/P2-04-dependency-abstraction-and-vendor-lock-in-spec.md) | 文档完成 | 待实现 | 依赖抽象仍是风险；`frontend/package.json` 仍缺显式 `dayjs` 依赖。 |
| [P2-05 合规与数据治理](../../archive/completed/specs/P2-05-compliance-data-governance-spec.md) | 文档完成 | 待实现/部分完成 | PII、密码列、租户传播、删除/保留、审计证据仍需工程闭环。 |
| [P2-06 前端可访问性与质量](../../archive/completed/specs/P2-06-frontend-accessibility-and-quality-spec.md) | 文档完成 | 待实现/部分完成 | 前端类型、状态流、组件规模和质量覆盖仍需继续收敛。 |

## P3 平台演进项

| 包 | 文档状态 | 工程状态 | 未完成依据 |
|---|---|---|---|
| [P3-00 架构边界评审](../../archive/completed/specs/P3-00-architecture-boundary-review-spec.md) | 文档完成 | 待实现 | 边界评审完成，但物理服务拆分尚未准备好。 |
| [P3-01 平台演进总览](../../archive/completed/specs/P3-01-platform-evolution-spec.md) | 文档完成 | 待决策 | 推进前必须完成 [平台演进推进检查表](../../decisions/work-products/p3-01-platform-evolution/platform-evolution-promotion-checklist.md)。 |
| [P3-02 服务拆分与领域边界](../../archive/completed/specs/P3-02-service-decomposition-and-domain-boundaries-spec.md) | 文档完成 | 待实现/待决策 | 七个 bounded context 已验证，但共享 DAL、跨上下文调用、租户传播等阻塞物理拆分。 |
| [P3-03 数据中台架构](../../archive/completed/specs/P3-03-data-platform-architecture-spec.md) | 文档完成 | 待实现/待决策 | 当前是 source inventory、POC plan、contract governance；完整数据中台尚未落地。 |
| [P3-04 多数据源隔离](../../archive/completed/specs/P3-04-multi-datasource-isolation-spec.md) | 文档完成 | 待实现/待决策 | 已有 ownership、transaction boundary、migration plan；仍需工程迁移和回滚验证。 |
| [P3-05 WebFlux 到 MVC 迁移](../../archive/completed/specs/P3-05-webflux-to-mvc-migration-spec.md) | 文档完成 | 待决策 | 需要先决定迁 MVC 还是强化当前 WebFlux 模型。 |
| [P3-06 K8s 部署平台](../../archive/completed/specs/P3-06-k8s-deployment-platform-spec.md) | 文档完成 | 待实现/待验证 | 需要真实部署拓扑、托管/自运维选择、SLO、secret、监控和 rollout/rollback 证据。 |
| [P3-07 生产平台组件](../../archive/completed/specs/P3-07-production-platform-components-spec.md) | 文档完成 | 待决策/待验证 | 组件矩阵和抽象计划已出，仍需 owner、故障模式、回滚、演练和签收证据。 |
| [P3-08 企微 SCRM 模块](../../archive/completed/specs/P3-08-wecom-scrm-module-spec.md) | 文档完成 | 待实现/待验证 | 实施切片、集成边界和测试计划已出，仍需模块工程实现与沙箱/回调/签名验证。 |
| [P3-09 身份、事件与租户平台](../../archive/completed/specs/P3-09-identity-event-and-tenant-platform-spec.md) | 文档完成 | 待实现/待决策 | OneID、事件 schema、租户契约、engine/web 边界仍是平台契约和治理设计，未证明完整落地。 |

## 待外部决策

这些不是简单归档问题，必须先有外部输入或 owner 决策：

- 真实生产 RTO/RPO 目标。
- Redis HA 拓扑和跨区域恢复策略。
- 服务拆分时间线。
- WebFlux 迁 MVC 还是先强化当前 WebFlux 模型。
- P3 平台演进项的 owner、成功指标、回滚、容量、安全、合规和验证路径。

## 如何判断下一步

1. 优先从 P0 表格开始，选择 `待实现` 且已有明确代码证据的问题。
2. 每个工程实现都应引用对应的 archived spec/plan，再补代码、测试和 evidence。
3. 完成工程实现后，更新本台账的工程状态，并把验证命令或证据链接写入对应 evidence 文件。
