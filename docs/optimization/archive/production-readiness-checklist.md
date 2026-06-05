# 生产级优化清单

> 基于全项目深度扫描，排除安全相关（安全专项单独文档），按优先级分层。
> 扫描日期：2026-05-30

---

## P0 — 上线前必须解决（不修会出事故）

### 1. 前端致命 Bug

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 1.1 | useEffect 无依赖数组，每次渲染执行自动保存 | `frontend/src/pages/canvas-editor/index.tsx:500-507` | 自动保存无限循环，大量无效 API 请求 |
| 1.2 | 键盘监听 useEffect 无依赖数组 | `frontend/src/pages/canvas-editor/index.tsx:650-699` | 每次渲染 add/remove listener，内存泄漏 + 事件重复绑定 |
| 1.3 | 零 ErrorBoundary | `frontend/src/App.tsx` | 任何组件未捕获异常 = 整个应用白屏 |
| 1.4 | 5 处 `window.location.reload()` | `frontend/src/pages/canvas-editor/index.tsx:1133,1240,1256,1275,1312` | 丢失 React 状态，用户体验极差 |
| 1.5 | console.log 泄露 token 前缀 | `frontend/src/context/AuthContext.tsx:73` | 浏览器控制台可见认证信息 |
| 1.6 | 多处 `.catch(() => undefined)` 吞错误 | `frontend/src/context/NotificationContext.tsx:142,154,225,226` | 错误完全静默，无法排查问题 |

### 2. 后端并发安全

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 2.1 | CircuitBreakerRegistry 竞态条件 | `backend/.../engine/scheduler/CircuitBreakerRegistry.java:97-143` | volatile 读 + 非原子写，多线程可能同时触发状态转换，熔断失效 |
| 2.2 | ExecutionContext 非原子 putAll | `backend/.../engine/context/ExecutionContext.java:131` | 并发 `putNodeOutput` 调用下 `flatContext.putAll(output)` 数据不一致 |
| 2.3 | JwtAuthFilter 每次请求查库 | `backend/.../config/JwtAuthFilter.java:74-76` | N+1 问题，高并发下 DB 压力巨大 |
| 2.4 | DB 连接池 vs 并发量严重不匹配 | `application.yml:12` (HikariCP max=33) vs 执行车道总并发 3000+ | 高负载下连接池耗尽，请求排队超时 |

---

## P1 — 上线后第一迭代必须补齐

### 3. 数据库

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 3.1 | `canvas_execution_trace` 缺 `(node_id, status)` 索引 | `V1__init_schema.sql:51-65` | 漏斗统计查询全表扫描，数据量大时严重变慢 |
| 3.2 | Flyway 启动时运行迁移 | `application.yml:19-23` | 迁移失败 = 应用 CrashLoopBackOff，阻塞所有部署 |
| 3.3 | 无数据保留/归档策略 | 无相关配置 | execution_trace 无限增长，磁盘耗尽 |
| 3.4 | 无只读副本配置 | `application.yml` 单数据源 | 报表/统计查询与执行流量争抢主库连接 |
| 3.5 | 数据库用户为 root（全权限） | `application.yml:8` | 运维风险，误操作无边界 |

### 4. 可观测性

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 4.1 | 有 Prometheus 指标但无 Grafana Dashboard | 无 `grafana/` 目录 | 指标存在但不可视化，等于没有 |
| 4.2 | 无告警规则 | 无 `*.rules.yml` 或 alertmanager 配置 | 错误率/延迟/队列深度异常无法及时发现 |
| 4.3 | 无日志聚合管道 | 无 Fluentd/Fluent Bit/Loki 配置 | JSON 日志输出到 stdout，无法跨实例搜索/关联 |
| 4.4 | 无分布式追踪 | 无 OpenTelemetry/Jaeger/Zipkin 集成 | MDC 有 traceId 但不跨服务传播，无法追踪全链路 |

### 5. 基础设施

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 5.1 | 零 CI/CD | 无 `.github/workflows/` 或任何 CI 配置 | 构建/测试/部署全手动，无质量门禁 |
| 5.2 | 无 `.dockerignore` | 项目根目录 | Docker 构建上下文包含 .git/node_modules/target，又大又慢 |
| 5.3 | 主 Dockerfile 以 root 运行 | `backend/canvas-engine/Dockerfile` | 容器逃逸 = 宿主机 root 权限 |
| 5.4 | 无 K8s manifest | 无 `k8s/` 目录 | 无 HPA/PDB/ConfigMap/Secret，无法编排生产部署 |
| 5.5 | 前端无 Dockerfile / nginx.conf | `frontend/` 目录 | 前端无容器化部署方案 |
| 5.6 | 前端 API 地址硬编码 localhost | `frontend/vite.config.ts:17-37` | 无法按环境切换后端地址 |
| 5.7 | 无生产 docker-compose | 仅有 `docker-compose.local.yml` | 生产部署无标准参考 |
| 5.8 | `package-lock.json` 被 gitignore | `frontend/.gitignore` | 前端构建不可复现，依赖版本漂移 |

### 6. 前端工程化

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 6.1 | 2076 行巨型组件 | `frontend/src/pages/canvas-editor/index.tsx` | 无法有效优化、测试、维护 |
| 6.2 | 无 E2E 测试 | 无 Playwright/Cypress 配置 | 关键路径（登录、画布编辑、发布）无端到端验证 |
| 6.3 | 无环境变量配置 | 无 `.env` / `.env.example` | 环境切换靠手动改代码 |
| 6.4 | useHistory hook 闭包陈旧风险 | `canvas-editor/index.tsx:283-318` | nodes/edges 在闭包中可能过期 |
| 6.5 | AuthContext loading 状态硬编码 false | `frontend/src/context/AuthContext.tsx:104` | 异步认证状态无法正确反映 |
| 6.6 | 401 时硬跳转而非 React 导航 | `frontend/src/services/api.ts:40` | 丢失当前路由状态 |
| 6.7 | 无 ARIA 属性 / 键盘导航 | 全局 | 无障碍合规缺失 |

---

## P2 — 持续优化（技术债）

### 7. 架构演进

| # | 方向 | 当前状态 | 目标 |
|---|------|---------|------|
| 7.1 | K8s 部署 | 无任何 manifest | Deployment + HPA + PDB + ConfigMap/Secret |
| 7.2 | 配置中心 | 注释中提到 Nacos 但未接入 | 接入 Nacos/Apollo 动态配置 |
| 7.3 | API 网关 | 直连后端 | 加 API Gateway 统一鉴权/限流/路由 |
| 7.4 | 读写分离 | 单主库 | 主写从读，报表走只读副本 |
| 7.5 | 灰度发布 | 无 | 金丝雀/蓝绿部署能力 |
| 7.6 | MQ Topic 拆分 | 入口/出口共用同一 Topic | 按业务拆分，避免单 Topic 流量瓶颈 |

### 8. 后端技术债

| # | 问题 | 位置 | 影响 |
|---|------|------|------|
| 8.1 | GlobalExceptionHandler 缺少 WebExchangeBindException/DataAccessException 处理 | `GlobalExceptionHandler.java` | 验证错误和数据库异常走通用 handler，响应不友好 |
| 8.2 | 通用 Exception handler 泄露 e.getMessage() | `GlobalExceptionHandler.java:84` | 可能暴露内部堆栈信息 |
| 8.3 | DagEngine catch(Exception ignored) 吞掉 trace 序列化错误 | `DagEngine.java:1235` | 审计数据丢失无感知 |
| 8.4 | 无集成测试 | `src/test/` 无 @SpringBootTest | 关键 DAG 执行路径无端到端验证 |
| 8.5 | 缺少 application-prod.yml / application-staging.yml | `src/main/resources/` | 无环境特定配置，全靠环境变量覆盖 |

### 9. 运维文档

| # | 缺失项 | 影响 |
|---|--------|------|
| 9.1 | 无 Runbook / 事故响应流程 | MTTR 长，靠人肉排查 |
| 9.2 | 无 ADR（架构决策记录） | 团队人员变动后知识流失 |
| 9.3 | 无 SLO/SLA 定义 | 无可用性和延迟目标 |
| 9.4 | 无容量规划指南 | 不知硬件需求，扩容无依据 |
| 9.5 | 无灾备恢复方案 | 数据中心故障无应对 |

---

## 建议执行顺序

1. **P0 前端致命 Bug**（1 周）— useEffect 依赖数组、ErrorBoundary、reload 替换
2. **P0 后端并发安全**（1 周）— CircuitBreaker 原子化、ExecutionContext 同步、AuthFilter 缓存、连接池调优
3. **P1 基础设施**（1-2 周）— CI/CD、Dockerfile 修复、.dockerignore、前端容器化
4. **P1 数据库 + 可观测性**（2 周）— 索引、Flyway 拆分、Grafana Dashboard、告警规则
5. **P1 前端工程化**（1-2 周）— 组件拆分、E2E 测试、环境配置
6. **P2 持续还债**— 按业务压力排优先级
