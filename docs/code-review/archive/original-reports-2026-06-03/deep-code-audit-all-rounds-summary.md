# Deep Code Audit — 13 轮完整汇总

> 审计日期: 2026-05-31
> 审计人: Winston (Architect)
> 累计 13 轮扫描，覆盖代码级 → 架构配置 → 数据/多租户 → 安全 → 依赖/运维 → 边缘条件 → WS/MQ/定时任务 7 大维度

---

## 总览

| Severity | 总数 | 占比 |
|----------|------|------|
| **P0 CRITICAL** | 48 | 22% |
| **P1 HIGH** | 100 | 45% |
| **P2 MEDIUM** | 52 | 24% |
| **P3 LOW** | 20 | 9% |
| **Total** | **220** | 100% |

---

## 各轮发现趋势

| 轮次 | 聚焦维度 | P0 | P1 | P2 | P3 | 总计 | 变化 |
|------|----------|----|----|----|----|------|------|
| R1 | 安全/并发/Reactor/配置 | 21 | 39 | — | — | 60 | 基线 |
| R2 | 深入扫描 | 8 | 12 | 9 | 4 | 33 | -45% |
| R3 | 触发器/版本/缓存 | 3 | 6 | 5 | 2 | 16 | -52% |
| R4 | JWT/前端/可观测性 | 2 | 5 | 4 | 1 | 12 | -25% |
| R5 | Flyway/配置/通知 | 1 | 5 | 5 | 2 | 13 | +8% |
| R6 | 类型安全/线程安全 | 0 | 3 | 5 | 2 | 10 | -23% |
| R7 | 架构配置/事务/Docker | 5 | 8 | 6 | 3 | 22 | +120% |
| R8 | 多租户/数据保留/迁移 | 3 | 5 | 4 | 1 | 13 | -41% |
| R9 | Groovy沙箱/前端安全/安全头 | 2 | 4 | 3 | 1 | 10 | -23% |
| R10 | 依赖CVE/加密/密钥/脱敏 | 1 | 4 | 3 | 1 | 9 | -10% |
| R11 | Handler边缘/SSRF/资源泄漏 | 1 | 3 | 3 | 1 | 8 | -11% |
| R12 | API响应泄露/乐观锁/限流 | 1 | 3 | 2 | 1 | 7 | -13% |
| R13 | WS安全/MQ幂等/定时任务 | 0 | 3 | 3 | 1 | 7 | 0% |
| **R7** | **架构配置/事务/Docker** | **5** | **8** | **6** | **3** | **22** | **+120%** |
| R8 | 多租户/数据保留/迁移 | 3 | 5 | 4 | 1 | 13 | -41% |
| R9 | Groovy沙箱/前端安全/安全头 | 2 | 4 | 3 | 1 | 10 | -23% |
| R10 | 依赖CVE/加密/密钥/脱敏 | 1 | 4 | 3 | 1 | 9 | -10% |
| R11 | Handler边缘/SSRF/资源泄漏 | 1 | 3 | 3 | 1 | 8 | -11% |

**收敛拐点**: R7 因切换到架构配置维度导致反弹，此后持续下降至 R11 的 8 项。

---

## P0 CRITICAL 完整清单 (47 项)

### 安全认证 (8 项)

| # | 问题 | 轮次 | 文件 |
|---|------|------|------|
| 1 | SecurityConfig 4个公开端点无认证(直调/行为触发/ops/WS) | R1,R7 | SecurityConfig.java |
| 2 | Dockerfile 以 root 运行 | R7 | Dockerfile |
| 3 | CORS wildcard + allowCredentials | R8 | WebConfig.java |
| 4 | JWT Token 存 localStorage | R9 | AuthContext.tsx |
| 5 | DB凭证硬编码+Redis无密码+useSSL=false | R1 | application.yml |
| 6 | 前端 TENANT_ADMIN 权限提升 | R1 | 前端路由 |
| 7 | 30+表缺tenant_id + 40+端点IDOR | R1 | 多处 |
| 8 | Groovy沙箱暴露ExecutionContext给脚本 | R9 | GroovyHandler.java:164 |

### 并发/Reactor (10 项)

| # | 问题 | 轮次 | 文件 |
|---|------|------|------|
| 9 | 14个Handler阻塞Reactor线程(注入Mapper) | R1 | 14个Handler |
| 10 | 9个Handler直接调用Mapper无boundedElastic | R11 | 9个Handler |
| 11 | ExecutionContext triggerPayload HashMap非线程安全 | R6 | ExecutionContext.java:46 |
| 12 | ExecutionContext callStack ArrayList非线程安全 | R6 | ExecutionContext.java:64 |
| 13 | DagEngine 6处fire-and-forget无错误handler | R1 | DagEngine.java |
| 14 | 7处catch(Exception ignored)吞异常 | R7 | 多处 |
| 15 | AudienceBitmapStore哈希碰撞99.9% | R1 | AudienceBitmapStore.java |
| 16 | ScoringHandler IN操作符逻辑反转 | R1 | ScoringHandler.java |
| 17 | 无graceful shutdown(关键组件无@PreDestroy) | R1 | 多处 |
| 18 | 虚拟线程无统一管理 | R7 | 8处Thread.ofVirtual() |

### 事务/数据 (8 项)

| # | 问题 | 轮次 | 文件 |
|---|------|------|------|
| 19 | 15/19 @Transactional缺rollbackFor | R7 | CanvasTransactionService等 |
| 20 | 零分布式追踪+MDC零使用+R类无traceId | R7 | 全项目 |
| 21 | 零自定义HealthIndicator | R7 | — |
| 22 | 44/50表缺tenant_id(86%) | R8 | V78及后续迁移 |
| 23 | canvas_audit_log表存在但零代码写入 | R8 | — |
| 24 | data_source_config.password VARCHAR(500)明文存储 | R8 | V71 |
| 25 | 零数据加密静态存储(用户画像/标签/事件日志全部明文) | R10 | 多表 |
| 26 | 零数据保留/归档策略(execution_trace无限增长) | R8 | — |

### 配置/部署 (7 项)

| # | 问题 | 轮次 | 文件 |
|---|------|------|------|
| 27 | 无application-prod.yml | R1,R7 | — |
| 28 | 全部40+个@RequestBody缺@Valid | R1 | Controller层 |
| 29 | 无CI/CD流水线 | R7 | — |
| 30 | 前端零ErrorBoundary | R1 | — |
| 31 | 无application-prod.yml危险默认值在生产生效 | R7 | — |
| 32 | Redis单点=PAUSED执行永久卡死 | R7 | ExecutionContext持久化 |
| 33 | 5个bare .subscribe()无错误handler | R7 | 多处 |

### 其他 (14 项来自R1-R5，已在各轮报告中详述)

R1-R5 的 14 项 P0 包括：SecurityConfig执行端点无认证、MQ消费失败消息丢失、前端401硬跳转、无trace ID传播、CanvasMetrics缺失、Flyway配置问题等。

---

## P0 修复优先级 Top 15

| # | 问题 | 修复工作量 | 紧急度 |
|---|------|-----------|--------|
| 1 | SecurityConfig 4个公开端点+ops无认证 | 2d | 立即 |
| 2 | 9+14=23个Handler阻塞Reactor线程 | 3d | 立即 |
| 3 | Dockerfile跑root → 添加USER指令 | 0.5d | 立即 |
| 4 | data_source_config.password明文 → Jasypt加密 | 2d | 1周内 |
| 5 | Groovy沙箱暴露ExecutionContext → 只读视图 | 3d | 1周内 |
| 6 | 15个@Transactional缺rollbackFor | 1d | 1周内 |
| 7 | 零分布式追踪+MDC → micrometer-tracing | 5d | 2周内 |
| 8 | canvas_audit_log零写入 → 审计日志服务 | 3d | 2周内 |
| 9 | JWT存localStorage → httpOnly cookie | 3d | 2周内 |
| 10 | 44/50表缺tenant_id → V91+迁移 | 10d | 3周内 |
| 11 | 零HealthIndicator → CanvasEngineHealthIndicator | 2d | 3周内 |
| 12 | 零数据加密静态存储 | 5d | 4周内 |
| 13 | 零数据保留策略 → 分区+归档 | 5d | 4周内 |
| 14 | 零安全响应头(CSP/HSTS/X-Frame-Options) | 1d | 2周内 |
| 15 | Redis单点=PAUSED执行永久卡死 → 双写MySQL | 5d | 4周内 |

**P0 修复总估算**: ~50.5 人天 (2人 × 5周)

---

## P1 HIGH 按维度汇总 (94 项)

| 维度 | 数量 | 关键问题 |
|------|------|----------|
| 安全/认证 | 15 | SSRF DNS rebinding、JWT 24h无refresh、HMAC无轮换、零API版本策略 |
| 并发/Reactor | 12 | synchronized在WebFlux、5个bare subscribe()、11个catch(Exception ignored) |
| 数据/事务 | 10 | 6个tenant_id可NULL、零数据保留策略、pageSize无上限 |
| 可观测性 | 8 | MDC零使用、R类无traceId、CanvasMetrics缺关键指标、零集成测试 |
| 配置/部署 | 10 | 无CI/CD、无application-prod.yml、BCrypt strength=10 |
| 依赖CVE | 4 | Jackson 2.15.4、commons-validator 1.7、Hutool 5.8.44 |
| Handler | 12 | 92处config.get无null检查、ObjectMapper静态实例、ExecutorService无shutdown |
| 前端 | 8 | 零ErrorBoundary、零组件测试、9处index-as-key、26 useState单组件 |
| 架构 | 15 | 7处@Lazy循环依赖、DagEngine 1540行上帝类、14 Handler注入Mapper |

---

## 审计覆盖维度

| 维度 | 覆盖轮次 | 就绪度 |
|------|----------|--------|
| 安全认证 | R1,R7,R9,R10,R11 | 15% |
| 并发/Reactor合规 | R1,R6,R7,R11 | 20% |
| 数据模型/多租户 | R8 | 12% |
| 事务语义 | R7 | 25% |
| 可观测性 | R1,R7 | 15% |
| 配置/部署 | R1,R7,R10 | 10% |
| 前端安全 | R9 | 25% |
| 依赖/CVE | R10 | 40% |
| 日志脱敏 | R10 | 50% |
| Handler边缘条件 | R11 | 30% |
| SSRF/XSS | R9,R11 | 60% |
| 成本架构 | supplement | 5% |
| 灾难恢复 | supplement | 10% |
| AI适配 | supplement | 35% |
| 合规认证 | supplement | 10% |

---

## 相关文档索引

| 文档 | 路径 | 内容 |
|------|------|------|
| R1-R6 合并报告 | docs/optimization/deep-code-audit-2026-05-31.md | 69 P0 + 157 P1 |
| R6 独立报告 | docs/deep-code-audit-round6.md | 类型安全/线程安全 |
| R7 | docs/deep-code-audit-round7.md | 架构配置/事务/Docker/HealthIndicator |
| R8 | docs/deep-code-audit-round8.md | 多租户/数据保留/迁移 |
| R9 | docs/deep-code-audit-round9.md | Groovy沙箱/前端安全/安全头/Lua |
| R10 | docs/deep-code-audit-round10.md | 依赖CVE/加密/密钥/脱敏 |
| R12 | docs/deep-code-audit-round12.md | API响应泄露/乐观锁/限流 |
| R13 | docs/deep-code-audit-round13.md | WS安全/MQ幂等/定时任务 |
| 架构深度审查 | docs/optimization/architecture-deep-review-2026-05.md | 15技术选型+15设计缺陷 |
| 补充架构审查 | docs/optimization/architecture-supplement-review-2026-05.md | 10维度补充就绪度15% |
| 架构师检查清单 | docs/architect-checklist-report.md | 10章38%通过率 |
| 产品审查 | docs/optimization/bmad-product-review-2026-05.md | ~208项产品缺项 |
