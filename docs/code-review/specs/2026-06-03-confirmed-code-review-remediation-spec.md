# Confirmed Code Review Remediation Spec

日期：2026-06-03
来源：`docs/code-review/verification/2026-06-03-code-review-verification.md`

## 目标

把二次复核中判定为 `成立`、仍有整改价值的 `部分成立`、以及必须通过运行手段关闭的 `需运行验证` 项转成可执行整改范围。`已修复`、`不成立`、纯正面发现不进入实现范围，但保留在 verification 台账中。

## 非目标

- 不把所有历史报告原文重写成新报告。
- 不在本 spec 中实现代码修复。
- 不把 `已修复` / `不成立` 条目重新当成缺陷修复。
- 不引入与当前技术栈不兼容的大型平台改造，除非作为后续阶段计划明确验证。

## 需求

### R1. 公开入口与生产配置安全

覆盖原问题：S1-S4、J1-J3、J8、J14、Y1-Y8、Y13、Y17-Y19、failed-config #1-#5、Round7 P0-1/P0-2/P1-5/P2-1/P2-2/P2-3、infra Issue 1-6。

要求：
- 生产环境禁止默认 `allowed-origins: "*"` 与 `allowCredentials=true` 组合。
- `CANVAS_EVENT_REPORT_SECRET` 不得有可预测默认值，启动时必须拒绝默认/空/低强度密钥。
- `/canvas/execute/direct/*`、`/canvas/trigger/behavior`、`/ops/**` 必须有明确认证与限流策略。
- Swagger/OpenAPI、Actuator detail、ops 路径必须按 profile/role 限制。
- 主 Dockerfile 必须非 root 运行，并补足容器最小权限建议。
- MySQL/Redis/RocketMQ 的生产凭证、TLS/ACL/network isolation 需要配置方案和验证脚本。

验收：
- prod profile 启动时，CORS wildcard、默认 HMAC secret、空 JWT secret、DB root/root 任一存在则 fail-fast。
- `curl` 从恶意 Origin 访问不返回通配 CORS。
- 未认证请求不能调用 direct/behavior/ops。
- Docker image 最终 runtime 用户不是 root。

### R2. 租户隔离、敏感数据与审计

覆盖原问题：TD-9、P0-1/P0-3/P1-1/P1-5/P1-15/P2-2/P2-3/P2-4 in Round8、V4、MIG1-MIG18、R10 P0-1/P2-1、S7、Y1、Y4、Y5、P0-23/P0-25/P0-26 summary。

要求：
- 所有业务表明确租户归属；核心查询必须带 tenant 过滤。
- `CanvasDO` 等实体与 V78 后续迁移保持一致，不允许 DB 有 tenant_id 而实体/查询无感。
- `data_source_config.password` 加密存储；日志、toString、equals/hashCode 不输出/比较明文密码。
- CDP 用户画像、事件日志、执行 trace、消息记录等敏感字段至少完成脱敏策略；强加密字段分阶段处理。
- `canvas_audit_log` 或新审计服务必须覆盖发布、下线、kill、灰度、回滚、审批、DLQ 重放、数据源变更等关键操作。
- 大表必须有保留、分区、归档或清理策略。

验收：
- 新增迁移为核心业务表补 tenant_id、索引、NOT NULL 路径，含回填和回滚说明。
- 数据源列表/详情响应不含明文 password；数据库列不再存可直接使用的明文密码。
- 关键管理动作可查询到审计记录。

### R3. 执行引擎并发、生命周期与幂等

覆盖原问题：R1-R5、P1-9/P1-10/P1-15/P1-16/P2-1/P2-2/P2-4/P2-5/P2-6/P2-7/P2-8、T1-T8、V1-V9、X1-X9、M1-M13、Round13 P1-2/P2-2、logic #1-#12。

要求：
- `InFlightExecutionRegistry.deregister` 原子化。
- `CircuitBreakerRegistry` 状态转换必须线程安全，构造参数必须校验。
- `ExecutionContext` 内部并发容器一致化，`putNodeOutput` 提供一致写入语义或明确快照策略。
- Disruptor 消费不能 fire-and-forget 后立即 reset event；shutdown 必须等待在途执行或有明确丢弃语义。
- WAIT/GOAL 恢复的已修复代码需补回归测试并清理过期注释。
- MQ 重投必须在持久执行请求层保持幂等。当前代码已经基于 `canvasId + triggerType + sourceMsgId` 生成确定性 `requestId`，并通过主键 + `INSERT IGNORE` 提供幂等；整改重点是补回归测试和状态机证明。只有产品要求按 `source_msg_id` 全局去重时，才新增 schema 设计。
- `@Transactional` 统一 rollbackFor 策略。
- 虚拟线程、Groovy executor、special scheduler 必须有统一生命周期和 shutdown。

验收：
- 并发单测覆盖 InFlight deregister、CircuitBreaker HALF_OPEN、ExecutionContext 并发写、WAIT resume quota bypass。
- Disruptor 测试能证明错误日志仍有 canvasId/userId，shutdown 不丢已接收事件或能显式拒绝新事件。
- MQ 重投相同 msgId 时，同一画布只生成同一个持久执行请求；重复发布同一 `requestId` 不会让终态请求再次执行。

### R4. Reactor/HTTP/Handler 安全与配置验证

覆盖原问题：B1-B4、H1-H21、Round11 P0-1/P1-1/P1-2/P2-1/P2-3、Round5 P0-1/P1-1/P2-2/P2-3/P2-5、main #1/#3/#4/#6、logic #13/#14/#15/#16。

要求：
- 所有阻塞 Mapper/Service 调用必须离开 Netty event loop，统一 `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())` 或 repository 封装。
- 出站 HTTP 必须使用受控 WebClient builder/bean，统一连接池、超时、响应大小限制。
- URL 校验和实际连接之间的 DNS rebinding 风险需缓解或至少在 SSRF 防护测试中覆盖。
- Handler 配置必须在发布期统一校验；运行期仍要 fail-fast 返回 `NodeResult.fail`，不能 NPE/ClassCastException。
- Benefit/Commit 类操作必须强制幂等策略，积分/券/标签 upsert 使用 DB 原子写或唯一键冲突可控处理。

验收：
- handler 测试覆盖缺字段、错类型、外部 HTTP 超时、响应过大、重复幂等键。
- `rg "WebClient.builder"` 只允许在配置类或显式例外中出现。
- `bodyToMono(String|Map|JsonNode)` 路径具备最大响应体限制。

### R5. 前端认证、通知、编辑器可靠性

覆盖原问题：TD-6/TD-7/TD-8、S5/S6、P1-36/P1-37/P1-38/P1-39、FE1-FE10、N1-N8、W1-W12、Round7 P3-2、Round12 P2-1。

要求：
- 去掉 token console.log。
- API response interceptor 必须处理业务 `code !== 0`，并返回稳定错误对象。
- 401 不得直接丢失编辑状态；需 refresh token 或保存草稿后跳转策略。
- 添加全局 ErrorBoundary 和编辑器局部 ErrorBoundary。
- Notification WebSocket 客户端要防止旧 socket onclose 覆盖新 socket，限制重连次数，合并 onerror/onclose fallback，补心跳/超时。
- canvas-editor 的 auto-save、undo/redo、快捷键、复制粘贴、删除边/节点引用必须补测试。
- 前端 Vite/Vitest advisory 要升级或记录可接受风险。

验收：
- 前端测试覆盖 Notification reconnect race、API business error、AuthContext token logging、canvas-editor auto-save/undo/keyboard。
- `npm audit --json` 无 critical；如 Vite major upgrade 暂缓，必须有替代风险接受记录。

### R6. 可观测性、运行验证与 CI

覆盖原问题：TD-11/TD-12、Round7 P0-4/P0-5/P1-3/P1-4/P1-8/P2-5、O1-O11、TEST1-TEST14、infra capacity issues。

要求：
- 建立 traceId/MDC/Reactor Context 贯通策略，`R` 响应含 traceId 或错误响应头含 traceId。
- 增加 Disruptor overflow、DLQ size、MQ lag、Redis unavailable、CircuitBreaker state、lane depth、handler latency、external HTTP latency 指标。
- 自定义 HealthIndicator 覆盖 RocketMQ、Redis、DB pool、Disruptor backlog、TraceWriteBuffer。
- 补 CI：后端 test、前端 test/build、npm audit、基础静态扫描。
- 对 `需运行验证` 项建立压测/故障注入脚本：DB pool、Redis pool、RocketMQ 重投、Disruptor shutdown、WebSocket 连接上限。

验收：
- `mvn test`、`npm test`、`npm run build` 纳入流水线。
- 有可运行脚本或文档验证 capacity 类问题。
- Prometheus/Actuator 暴露新增指标，至少有本地集成测试或 smoke test。

## 优先级

| 优先级 | 范围 |
|---|---|
| P0 | R1 公开入口/生产配置、R2 数据源密码/租户隔离最小闭环、R3 幂等/Disruptor/WAIT 回归、R4 阻塞 Handler 与 HTTP 限制。 |
| P1 | R5 前端可靠性、R6 追踪/指标/health/CI、R2 审计与归档。 |
| P2 | 架构瘦身、状态管理重构、长期 ID 策略、缓存一致性增强、国际化时区策略。 |

## 开放问题

- TENANT_ADMIN 是否应访问 `/admin/users` 和系统配置，需要产品角色矩阵确认。
- 生产 TLS 是否由应用直接终止，还是由网关/Ingress 终止，需要部署方案确认。
- Vite/Vitest major upgrade 是否可接受破坏性升级，需要前端兼容性评估。
