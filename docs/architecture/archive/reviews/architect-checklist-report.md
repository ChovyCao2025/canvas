# 架构师检查清单 — 综合验证报告

**项目:** Marketing Canvas (营销画布)
**日期:** 2026-05-31
**项目类型:** 全栈 (Java 21 + React 18)
**评估范围:** 后端 + 前端 + 基础设施 + 安全

---

## 1. 执行摘要

| 维度 | 评估 |
|------|------|
| **总体架构就绪度** | **中等偏低 (38%)** |
| **关键风险** | 3 CRITICAL + 5 HIGH |
| **核心优势** | DAG引擎插件化设计、NodeHandler注解驱动、分层缓存体系完善、SSRF防护优秀 |
| **评估章节** | 全部10章（含前端） |

---

## 2. 逐章节通过率

### 2.1 需求对齐 (Requirements Alignment) — 40%

| 子项 | 状态 | 说明 |
|------|------|------|
| 功能需求覆盖 | ❌ | 无正式PRD，无法验证覆盖度。已有分析指出148项能力缺项 |
| 非功能需求对齐 | ⚠️ | 性能/可扩展性有方案，合规(GDPR/个保法)缺少技术实现 |
| 技术约束遵守 | ✅ | Java 21、WebFlux、MyBatis-Plus 均已落地 |

### 2.2 架构基础 (Architecture Fundamentals) — 55%

| 子项 | 状态 | 说明 |
|------|------|------|
| 架构清晰度 | ⚠️ | CLAUDE.md 有架构描述，但无正式架构文档、无架构图 |
| 关注点分离 | ❌ | DagEngine 1540行上帝类，canvas-editor 2085行上帝组件，14个Handler直接注入Mapper |
| 设计模式 | ✅ | NodeHandler插件模式、HandlerRegistry注解驱动、NodeResult工厂方法 — 设计优秀 |
| 模块化/可维护性 | ⚠️ | 后端30+包但层违规(domain→engine反向依赖)，前端无模块边界 |

### 2.3 技术栈与决策 (Technical Stack & Decisions) — 50%

| 子项 | 状态 | 说明 |
|------|------|------|
| 技术选型合理性 | ❌ | WebFlux(非阻塞) + MyBatis-Plus(阻塞) 混用是架构矛盾，需boundedElastic桥接 |
| 技术版本定义 | ✅ | 版本明确 (Spring Boot 3.2.5, Java 21, React 18.3.1) |
| 选型理由文档 | ❌ | 无技术选型白皮书，决策不可追溯 |
| 栈组件兼容性 | ❌ | Reactive + Blocking混用、Disruptor + WebFlux事件循环 — 存在互锁风险 |

**CRITICAL**: WebFlux + MyBatis-Plus + Disruptor 三者互锁。

### 2.4 前端设计 (Frontend Design) — 35%

| 子项 | 状态 | 说明 |
|------|------|------|
| 框架与库选择 | ✅ | React 18 + antd 5 + @xyflow/react — 选型合理 |
| 状态管理 | ⚠️ | 无外部状态管理库，canvas-editor 20+ useState + useRef，已达管理极限 |
| 组件架构 | ❌ | 2085行单体组件、无共享组件库、无CSS Modules、0个ErrorBoundary |
| 响应式/适配 | ❌ | 无响应式设计，纯固定布局 |
| 构建打包策略 | ✅ | Vite + React.lazy + 代码分割 — 良好 |

### 2.5 弹性与运维 (Resilience & Operational Readiness) — 55%

| 子项 | 状态 | 说明 |
|------|------|------|
| 错误处理 | ✅ | 自定义熔断器(CLOSED→OPEN→HALF_OPEN)、DLQ、NodeGate CAS — 完善 |
| 重试策略 | ✅ | 三层重试(节点级/执行级/溢出级) + 优先级准入(HIGH/NORMAL/LOW) — 设计优秀 |
| 监控可观测 | ⚠️ | Micrometer指标丰富(Counter/Timer/Gauge)，但无分布式追踪、无链路关联 |
| 性能/扩展 | ⚠️ | 4车道隔离、优先级准入 — 良好，但无水平扩展方案 |
| 部署/DevOps | ❌ | 无CI/CD、无IaC、无环境策略 |

### 2.6 安全与合规 (Security & Compliance) — 45%

| 子项 | 状态 | 说明 |
|------|------|------|
| 认证授权 | ✅ | JWT + RBAC(4角色) + 多租户 + Redis黑名单 + HMAC事件上报签名 — 完善 |
| 数据安全 | ❌ | `data_source_config.password`明文存储、无加密静态数据、CORS wildcard+allowCredentials |
| API安全 | ⚠️ | SSRF防护优秀(OutboundUrlValidator含DNS重绑定防护)，但4个公开端点需审视 |
| 基础设施安全 | ❌ | Redis/MySQL无密码、无网络隔离、Groovy沙箱虽有但沙箱逃逸风险需持续关注 |

### 2.7 实现指导 (Implementation Guidance) — 30%

| 子项 | 状态 | 说明 |
|------|------|------|
| 编码标准 | ⚠️ | CLAUDE.md有部分约定，无正式编码规范文档 |
| 测试策略 | ⚠️ | 后端112测试(合理)、前端30测试(仅纯函数，0组件测试) |
| 开发环境 | ✅ | docker-compose + Flyway自启动 — 开发体验好 |
| 技术文档 | ❌ | 无正式技术文档体系 |

### 2.8 依赖与集成管理 (Dependency & Integration) — 50%

| 子项 | 状态 | 说明 |
|------|------|------|
| 外部依赖识别 | ⚠️ | RocketMQ/Redis/MySQL已识别，但无降级策略文档 |
| 内部依赖映射 | ❌ | 7处@Lazy循环依赖(DagEngine↔CanvasExecutionService等)，架构倒置(domain→engine) |
| 第三方集成 | ⚠️ | WireMock做本地模拟，但无集成测试环境 |

### 2.9 AI代理实现适用性 (AI Agent Implementation Suitability) — 60%

| 维度 | 评分 | 说明 |
|------|------|------|
| 新增NodeHandler | ⭐⭐⭐⭐⭐ | 注解驱动+注册表，AI可直接创建新handler |
| 修改DagEngine | ⭐⭐ | 1540行+6阶段模型，AI修改风险极高 |
| 新增前端页面 | ⭐⭐⭐ | 模式清晰但无规范文档 |
| 修改Canvas Editor | ⭐⭐ | 2085行+20+状态变量，AI易引入bug |
| 数据库迁移 | ⭐⭐⭐⭐ | Flyway模式清晰，命名规范 |

### 2.10 无障碍 (Accessibility) — 0%

未实现，未测试。

---

## 3. 风险评估 — Top 8

| # | 风险 | 严重度 | 影响 | 缓解建议 |
|---|------|--------|------|----------|
| 1 | **WebFlux + MyBatis-Plus + Disruptor 互锁** | CRITICAL | 线程模型冲突，boundedElastic池耗尽致全链路阻塞 | 评估迁移至Spring MVC或R2DBC；短期增加池监控告警 |
| 2 | **14个Handler直接注入Mapper + 架构倒置** | CRITICAL | 引擎层与持久化层耦合，Handler中DB调用可能阻塞事件循环 | 引入Repository抽象层，Handler仅依赖接口 |
| 3 | **data_source_config.password 明文存储** | CRITICAL | 违反个保法/GDPR，数据泄露直接暴露数据库凭证 | 引入Jasypt加密或Vault凭证管理 |
| 4 | **7处@Lazy循环依赖** | HIGH | DagEngine↔CanvasExecutionService等循环，职责划分不清 | 通过分层重构消除循环 |
| 5 | **DagEngine(1540行) + CanvasExecutionService(1407行) 双上帝类** | HIGH | 修改风险高、测试困难、新人上手慢 | 拆分为职责单一的组件 |
| 6 | **无分布式追踪** | HIGH | 生产故障排查耗时从分钟级变小时级 | 引入Micrometer Tracing + Zipkin/Jaeger |
| 7 | **前端零ErrorBoundary** | HIGH | 渲染错误白屏崩溃整个应用 | 添加GlobalErrorBoundary + CanvasEditorErrorBoundary |
| 8 | **CORS wildcard + allowCredentials** | MEDIUM | 安全漏洞，允许任意域携带凭证跨域请求 | 收紧为具体域名白名单 |

---

## 4. 深度扫描补充发现

### 发现 #1: 14个Handler直接注入Mapper

ApiCallHandler, CanvasTriggerHandler, ConditionEvaluator, CreateTaskHandler, ManualApprovalHandler, PointsOperationHandler, GoalCheckHandler, MqTriggerHandler, SubFlowRefHandler, SendMqHandler, TrackEventHandler, TagOperationHandler, UpdateProfileHandler, WaitHandler — 全部直接注入MyBatis-Plus Mapper。

### 发现 #2: Domain层反向依赖Engine层

`domain/canvas/CanvasService` 直接导入7个engine包类 (TriggerPreCheckService, DagGraph, DagParser, GroovyHandler, CanvasRuleGraphValidator, CanvasSchedulerService, CanvasExecutionService)。

### 发现 #3: 7处@Lazy循环依赖

DagEngine ↔ CanvasExecutionService, CanvasDisruptorService ↔ CanvasExecutionService ↔ CanvasExecutionRequestExecutor (三向), TaggerHandler ↔ CanvasExecutionService, SubFlowRefHandler ↔ DagEngine, CanvasTriggerHandler ↔ DagEngine, TransferJourneyHandler ↔ CanvasExecutionService。

### 发现 #4: EventReportAuthService 实现有HMAC签名

之前报告"公开端点无认证"不准确。EventReportAuthService 实现了 HMAC-SHA256 + 时间窗口防重放 + 常量时间比较。但需确保 `canvas.events.report-secret` 生产环境已配置。

### 发现 #5: CanvasExecutionService 准上帝类

1407行 + 50方法 + 5个Mapper直接注入，承担了触发入口、Resume/Wake、去重、上下文持久化、灰度解析、DLQ写入、统计更新7项职责。

### 发现 #6: 前端零ErrorBoundary + 零TODO/FIXME

0个ErrorBoundary = 白屏崩溃风险；0个TODO/FIXME = 技术债务未被标记。

### 发现 #7: 虚拟线程无统一管理

4处 `Thread.ofVirtual().start()` 直接启动，无统一池管理或监控。如果虚拟线程内部有阻塞DB调用，可能无法被现有指标追踪。

### 发现 #8: ExecutionController 可能阻塞事件循环

7处DB依赖但无显式 boundedElastic 包装。需验证 CanvasExecutionService 内部是否已做响应式包装。

---

## 5. 建议

### 必须修复 (Must-fix)

1. 加密 `data_source_config.password` — Jasypt 或 Vault
2. 公开端点添加 API Key / HMAC 签名认证
3. CORS 配置收紧，禁止 wildcard + allowCredentials
4. 限制 Redis/MySQL 生产环境密码
5. 引入 Handler Repository 抽象层，消除 Mapper 直接注入

### 应当修复 (Should-fix)

1. 拆分 DagEngine 为 4-5 个职责单一的类
2. 拆分前端 canvas-editor 2085行组件
3. 引入分布式追踪 (Micrometer Tracing)
4. 建立 CI/CD 流水线
5. 前端添加 ErrorBoundary 和组件测试
6. 消除 @Lazy 循环依赖

### 锦上添花 (Nice-to-have)

1. 技术选型白皮书
2. 正式PRD文档
3. 前端状态管理迁移 (Zustand)
4. 响应式/适配设计
5. 无障碍支持
6. 虚拟线程统一管理框架

---

## 6. 通过率汇总

| 章节 | 通过率 | 评级 |
|------|--------|------|
| 1. 需求对齐 | 40% | ⚠️ |
| 2. 架构基础 | 55% | ⚠️ |
| 3. 技术栈与决策 | 50% | ⚠️ |
| 4. 前端设计 | 35% | ❌ |
| 5. 弹性与运维 | 55% | ⚠️ |
| 6. 安全与合规 | 45% | ❌ |
| 7. 实现指导 | 30% | ❌ |
| 8. 依赖与集成 | 50% | ⚠️ |
| 9. AI代理适用性 | 60% | ⚠️ |
| 10. 无障碍 | 0% | ❌ |
| **总体** | **38%** | **中等偏低** |
