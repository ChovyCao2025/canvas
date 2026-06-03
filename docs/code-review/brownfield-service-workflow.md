# Brownfield Service Enhancement — 工作流执行报告

**工作流:** brownfield-service (复杂增强序列)
**日期:** 2026-05-31

---

## Scope Assessment

本次评估判定为 **复杂增强 (Complex Enhancement)**，理由：

- 需要多个协调 Story（6个Epic，20+ Story）
- 涉及架构分层变更（Handler → Repository 抽象）
- 存在安全漏洞需修复（CRITICAL 级别）
- 多个集成点受影响（65+ Handler、29个 Controller、前端编辑器）

---

## Service Analysis (现有服务分析)

### 1. 服务概览

| 维度 | 数据 |
|------|------|
| 代码规模 | 409 Java 源文件 + 383 测试文件 (后端)；~20,000 行 TS/TSX (前端) |
| API 端点 | 28 个 Controller，173 个端点 |
| 核心引擎 | DagEngine (1540行) + CanvasExecutionService (1407行) + 65 个 NodeHandler |
| 数据库 | 89 个 Flyway 迁移，49 个 DO/Mapper |
| 基础设施 | MySQL + Redis + RocketMQ + Disruptor + Caffeine |

### 2. 核心服务依赖图

```
Controller Layer (29 controllers, 173 endpoints)
    ↓
Domain Service Layer (CanvasService, CdpUserService, NotificationService, ...)
    ↓ [VIOLATION: CanvasService 直接依赖 Engine 层 7 个类]
Engine Layer
    ├── DagEngine ← → CanvasExecutionService [CIRCULAR: @Lazy]
    ├── HandlerRegistry → 65 NodeHandler implementations
    ├── CanvasDisruptorService ← → CanvasExecutionService [CIRCULAR: @Lazy]
    └── 14 Handlers → MyBatis-Plus Mapper [LAYER VIOLATION]
    ↓
Data Access Layer (49 Mapper interfaces)
    ↓
MySQL (canvas_db)
```

### 3. 性能基线 (从配置推断)

| 指标 | 当前值 | 来源 |
|------|--------|------|
| 最大并发执行 | 3,000 | canvas.execution.max-concurrency |
| 全局超时 | 600s | canvas.execution.global-timeout-sec |
| 节点超时 | 3s | canvas.node.timeout-ms |
| DB 连接池 | 33 | hikari.max-pool-size |
| Redis 连接池 | 64 | lettuce.pool.max-active |
| HTTP 连接池 | 500 | canvas.http-client.max-connections |
| Disruptor ring buffer | 65,536 | canvas.disruptor.ring-buffer-size |

### 4. 集成依赖

| 依赖 | 类型 | 降级策略 |
|------|------|----------|
| MySQL | 数据库 | 无 — 单点故障 |
| Redis | 缓存+锁+黑名单 | L1 (Caffeine) 降级，有限 TTL |
| RocketMQ | 消息队列 | 溢出重试 → DLQ |
| WireMock (dev) | HTTP 模拟 | 仅开发环境 |

### 5. 已知技术债务清单

| ID | 债务 | 位置 | 优先级 |
|----|------|------|--------|
| TD-1 | DagEngine 1540行上帝类 | engine/scheduler/DagEngine.java | P1 |
| TD-2 | CanvasExecutionService 1407行+50方法 | engine/trigger/CanvasExecutionService.java | P1 |
| TD-3 | 14个Handler直接注入Mapper | engine/handlers/*.java | P0 |
| TD-4 | 7处@Lazy循环依赖 | 多处 | P1 |
| TD-5 | Domain→Engine反向依赖 | domain/canvas/CanvasService.java | P1 |
| TD-6 | 前端canvas-editor 2085行 | src/pages/canvas-editor/index.tsx | P2 |
| TD-7 | 前端零ErrorBoundary | 全局 | P1 |
| TD-8 | 前端零组件测试 | 全局 | P2 |
| TD-9 | data_source_config密码明文 | dal/dataobject + V71迁移 | P0 |
| TD-10 | CORS wildcard | application.yml | P0 |
| TD-11 | 无分布式追踪 | 全局 | P1 |
| TD-12 | 无CI/CD流水线 | 全局 | P2 |
| TD-13 | CanvasExecutionService FIXME x2 | line 893, 903 | P2 |
| TD-14 | InAppNotifyHandler TODO (MQTT未实现) | line 43 | P2 |

---

## Enhancement Sequence (增强序列)

### 复杂增强流程执行

```
[analyst: 现有服务分析] ✅ 完成 → 本文档
    ↓
[pm: brownfield-prd.md] ⏭ 跳过 — 已有148项缺项分析 + 架构审查报告
    ↓
[architect: brownfield-architecture.md] ✅ 完成 → docs/brownfield-architecture.md
    ↓
[po: validate with po-master-checklist] → 下一步
```

### PO Master Checklist (产品负责人验证)

#### 文档完整性

| 交付物 | 状态 | 位置 |
|--------|------|------|
| 现有服务分析 | ✅ | 本文档 |
| 架构增强文档 | ✅ | docs/brownfield-architecture.md |
| 架构检查清单报告 | ✅ | docs/architect-checklist-report.md |
| PRD | ⚠️ 跳过 | 替代：docs/optimization/marketing_platform_gap_analysis.md (148项) |

#### 集成安全性验证

| 检查项 | 状态 | 说明 |
|--------|------|------|
| API 兼容性 | ✅ | 所有端点签名不变 |
| 数据库兼容性 | ✅ | 仅增量迁移，现有列保留 |
| 向后兼容 | ✅ | Repository抽象通过委托模式，DagEngine拆分通过Facade模式 |
| 安全回归 | ⚠️ | 公开端点添加认证需确保不影响现有集成方 |
| 性能回归 | ⚠️ | Repository间接层需压测验证无显著延迟增加 |

#### 风险缓解验证

| 风险 | 缓解策略 | 验证方式 |
|------|----------|----------|
| DagEngine拆分回归 | Facade模式渐进切换 | 现有DagEngine*Test全部通过 |
| Handler Repository性能损耗 | 委托模式零逻辑 | Handler*Test基准对比 |
| 安全修复影响集成 | API Key向后兼容 | Swagger文档更新+集成方通知 |

---

## Epic & Story Planning

### Epic 1: 安全加固 (P0, 1-2周)

| Story | 描述 | 验收标准 | 预估 |
|-------|------|----------|------|
| 1.1 | data_source_config密码加密迁移 | 新增data_source_credential表；现有记录批量加密；读取优先解密列 | 3d |
| 1.2 | 公开端点添加API Key认证 | /canvas/events/report, /canvas/execute/direct/*, /canvas/trigger/behavior 添加API Key或HMAC验证 | 2d |
| 1.3 | CORS收紧+Redis/MySQL密码配置 | application.yml移除wildcard；docker-compose添加密码；环境变量覆盖 | 1d |

### Epic 2: Handler分层重构 (P0, 2-3周)

| Story | 描述 | 验收标准 | 预估 |
|-------|------|----------|------|
| 2.1 | 创建NodeExecutionRepository+NodeBusinessRepository接口 | 接口定义+实现类(委托Mapper)；所有方法返回Mono | 2d |
| 2.2 | 迁移14个Handler从Mapper到Repository | 每个Handler替换Mapper注入为Repository注入；现有测试通过 | 5d |
| 2.3 | DagEngine移除dlqMapper直接依赖 | DagEngine通过NodeExecutionRepository写入DLQ | 1d |
| 2.4 | 消除Domain→Engine反向依赖 | CanvasService不直接导入Engine类；通过接口或事件解耦 | 3d |

### Epic 3: DagEngine拆分 (P1, 3-4周)

| Story | 描述 | 验收标准 | 预估 |
|-------|------|----------|------|
| 3.1 | 抽取DagTraceWriter | 追踪写入+DLQ写入+批量缓冲独立为类；DagEngine委托调用 | 3d |
| 3.2 | 抽取DagRouter | 下游触发+优先级路由+分支跳过标记独立为类 | 4d |
| 3.3 | 抽取DagExecutor | 节点执行6阶段流水线+Handler调度独立为类 | 5d |
| 3.4 | DagEngine瘦身为Facade | DagEngine仅保留execute()入口，委托到Parser+Executor+Router+Tracer | 2d |

### Epic 4: 可观测性增强 (P1, 1-2周)

| Story | 描述 | 验收标准 | 预估 |
|-------|------|----------|------|
| 4.1 | 引入Micrometer Tracing + Jaeger | pom.xml添加依赖；application.yml配置；docker-compose添加Jaeger容器 | 2d |
| 4.2 | Repository层指标埋点 | 每个Repository方法添加Timer指标；p50/p95/p99可查 | 1d |
| 4.3 | 前端ErrorBoundary+错误上报 | GlobalErrorBoundary包裹路由；CanvasEditorErrorBoundary包裹编辑器；错误上报API | 2d |

### Epic 5: 前端重构 (P2, 2-3周)

| Story | 描述 | 验收标准 | 预估 |
|-------|------|----------|------|
| 5.1 | 引入Zustand + canvasEditorStore | 安装Zustand；创建store含图状态+保存状态+undo/redo | 2d |
| 5.2 | canvas-editor拆分为hooks+子组件 | 提取useCanvasGraph, useCanvasSave, useCanvasHistory hooks | 3d |
| 5.3 | config-panel拆分 | 1414行拆分为InspectorHeader, ConfigSection, BranchRouteCard等子组件 | 3d |

### Epic 6: DevOps基础建设 (P2, 1-2周)

| Story | 描述 | 验收标准 | 预估 |
|-------|------|----------|------|
| 6.1 | GitHub Actions CI流水线 | push/PR自动触发mvn verify + npm test + npm build | 2d |
| 6.2 | Staging环境 | docker-compose.staging.yml含生产级配置 | 1d |
| 6.3 | 生产部署文档 | 写入docs/deployment-guide.md | 1d |

---

## Handoff to Development

### 关键集成要求

1. **Repository抽象必须先于Handler迁移完成** — Epic 2.1 是 2.2-2.4 的前置
2. **DagEngine拆分必须保持Facade** — Epic 3.4 依赖 3.1-3.3，且外部调用方无需修改
3. **安全修复独立于架构重构** — Epic 1 可并行执行，不依赖其他Epic
4. **前端重构不影响后端** — Epic 5 可独立进行

### 实现顺序建议

```
Week 1-2: Epic 1 (安全加固) — 独立执行
Week 2-4: Epic 2 (Handler分层) — Epic 2.1 先行
Week 4-7: Epic 3 (DagEngine拆分) — 依赖 Epic 2 完成
Week 2-3: Epic 4 (可观测性) — 可与 Epic 2 并行
Week 5-7: Epic 5 (前端重构) — 可与 Epic 3 并行
Week 7-8: Epic 6 (DevOps) — 最后执行
```
