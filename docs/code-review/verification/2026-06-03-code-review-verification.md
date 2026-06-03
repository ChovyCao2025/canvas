# docs/code-review 二次复核台账

复核日期：2026-06-03
复核范围：`docs/code-review` 下 20 份 Markdown 审查文档，以及当前仓库源码/配置/迁移/测试文件。
复核原则：保留原问题，不抽取、不合并删除；重复问题可以交叉引用，但每个原编号都给出判定。

## 判定标准

| 判定 | 含义 |
|---|---|
| 成立 | 当前源码/配置/迁移可静态证明问题存在。 |
| 部分成立 | 风险方向存在，但原报告的数量、文件、严重度、前提或描述有误。 |
| 已修复 | 原问题曾可能存在，但当前代码已有明确修复证据。 |
| 不成立 | 当前代码与原描述相反，或原描述所依赖事实错误。 |
| 需运行验证 | 只能通过压测、部署环境、真实生产配置、安全扫描数据库或外部系统行为验证，源码无法直接证明。 |

## 关键更正

- Java 包路径是 `org/chovy/canvas`，不是 `com/photon/canvas`。
- `events/report` 虽然在 Spring Security 中 `permitAll`，但当前 `EventDefinitionController.reportEvent()` 会调用 `EventReportAuthService.verify()` 校验 HMAC；“report-secret 定义但从未校验”已修复。默认密钥仍是已知值，问题仍成立。
- `DataSourceConfigDO.password` 当前已有 `@JsonProperty(access = WRITE_ONLY)`；“通过 JSON API 明文返回 password”不成立。但 DB 明文存储仍成立，且 `@Data.toString()` 日志泄露风险仍成立。
- WebSocket 端点 `permitAll` 不能直接等价为“无认证”：`/canvas/notifications/ws-ticket` 需认证，WS handler 使用 Redis 一次性 ticket 且 `getAndDelete()` 消费。无连接数限制仍成立。
- WAIT/GOAL 恢复路径当前已在 `CanvasExecutionService.isInternalContinuationTrigger()` 跳过 pre-check/cooldown/配额扣减；`WaitResumeService` 注释仍保留旧问题，需要清理。
- Handler 统计：`engine/handlers` 下 61 个 `*Handler.java`，60 个含 `@NodeHandlerType`。原报告“61 个 Handler 文件但仅 29 个注册”不成立。
- 前端测试不是零：`frontend/src` 下有 30 个 `*.test|*.spec` 文件。后端测试文件 112 个。
- `@Lazy` 实际为 7 处注解加 1 条注释；它能证明存在循环依赖/延迟依赖点，但不能直接证明“7 处循环依赖”全部成立。
- `@Transactional` 当前主代码有 16 处注解，仅 `TagImportService` 带 `rollbackFor = Exception.class`；原“15 个/19 个”计数不准，但裸事务问题成立。
- `CanvasService.publish()` 已把 DB 发布事务与 Redis 路由/调度/缓存/预编译拆开，原“发布事务内混 Redis 路由注册”已修复。裸事务 rollbackFor 仍另行成立。
- 依赖项：Maven 解析到 `jackson-databind:2.15.4`、`hutool-all:5.8.44`、`commons-validator:1.7`。但原报告把 `CVE-2024-29857` 指为 Jackson、把 `CVE-2023-25871` 指为 Hutool均不成立；NVD 显示二者分别不是这两个包的问题。`npm audit` 当前确认前端有 `vite/esbuild` moderate 与 `vitest` critical advisory。

## 证据索引

| 编号 | 证据 |
|---|---|
| E1 | `backend/canvas-engine/src/main/resources/application.yml`: MySQL `root/root`、`useSSL=false`、`allowPublicKeyRetrieval=true`；Redis password 注释；JWT secret 空默认；event report secret 默认 `canvas-event-report-secret-2026!!`；CORS `*`；Actuator `show-details: always`。 |
| E2 | `SecurityConfig.java`: CSRF disabled；`/canvas/events/report`、`/canvas/execute/direct/*`、`/canvas/trigger/behavior`、`/canvas/ws/notifications`、`/ops/**` permitAll。 |
| E3 | `WebConfig.java`: `allowedOrigins.contains("*")` 时 `addAllowedOriginPattern("*")`，同时 `setAllowCredentials(true)`。 |
| E4 | `EventReportAuthService` + `EventDefinitionController.reportEvent`: HMAC timestamp/signature 校验已接入。 |
| E5 | `ExecutionController`: direct call 使用当前安全上下文，匿名时落到 `"system"`；behavior trigger 仍使用请求体 `userId`。 |
| E6 | `DataSourceConfigDO`: `password` 为 Jackson WRITE_ONLY；V71 迁移仍以 `VARCHAR(500)` 明文存储，`application.yml` 和 demo 迁移含 root/root。 |
| E7 | `WebClientConfig`: 已有连接/响应/read/write timeout 与共享 `WebClient.Builder`；缺 `maxIdleTime/maxLifeTime/evictInBackground` 和 codec `maxInMemorySize`；多个组件绕过共享 builder。 |
| E8 | `GroovyHandler`: `newVirtualThreadPerTaskExecutor()` 无 `@PreDestroy`；`binding.setVariable("ctx", ctx)` 暴露完整 `ExecutionContext`；`shellPool` 是有界队列但高并发会阻塞/额外创建临时 shell。 |
| E9 | `ExecutionContext`: `triggerPayload = new HashMap<>()`、`callStack = new ArrayList<>()`；`putNodeOutput()` 分别写 `nodeOutputs` 与 `flatContext`。 |
| E10 | `CanvasDisruptorService`: worker 中 fire-and-forget `.subscribe()`，finally 立即 `event.reset()`；`shutdown()` 只 `disruptor.shutdown()`；`publishRequest()` 只设置 requestId。 |
| E11 | `CanvasExecutionService`: WAIT/GOAL 恢复已跳过 pre-check/配额；dedup TTL 仍有 FIXME；恢复 ctx 丢失时使用当前版本；完成统计每次创建虚拟线程。 |
| E12 | `MqTriggerConsumer`: `consumeThreadNumber = 20` 硬编码；onMessage 内同步 `requestService.enqueue()`；`source_msg_id` 只索引非唯一。 |
| E13 | `frontend/src/context/AuthContext.tsx`: JWT 存 localStorage，并打印 token 前缀。`api.ts` 401 硬跳 `/login`，响应拦截器未检查业务 `code`。 |
| E14 | `NotificationContext.tsx`: `refresh()` 假设 `data` 存在；`onerror`/`onclose` 都调度 fallback/reconnect；无最大重试；await ticket 后未重检 stopped；无客户端心跳。 |
| E15 | Docker/Compose: 主 Dockerfile 无 `USER`；`Dockerfile.perf` 有非 root；local compose MySQL root/root、Redis 无密码、RocketMQ 无 ACL/TLS；`brokerIP1=192.168.0.154`。 |
| E16 | 迁移：V78 只给 6 张表加 nullable tenant_id；无 `FOREIGN KEY`/`ON DELETE`；`canvas_audit_log` 表存在但未找到写入代码；V14/V6 有 `SELECT 1` 占位。 |
| E17 | 计数：DagEngine 1539 行，CanvasExecutionService 1407 行，MapFieldKeys 691 行，canvas-editor 2084 行；无 `application-prod.yml`、无 `.github/.gitlab-ci/Jenkinsfile`。 |
| E18 | 测试：后端 112 个测试文件；前端 30 个测试文件；未找到 `CircuitBreakerRegistry`、`TriggerPreCheckService`、`WeightedChoice`、`MarketingPolicyService`、`ReachDeliveryService` 的直接测试。 |
| E19 | NPM audit: `esbuild` GHSA-67mh-4wv8-2f99、`vite` GHSA-4w7w-66w2-5vf9、`vitest` GHSA-5xrq-8626-4rwp。 |
| E20 | NVD 复核：`CVE-2024-29857` 与 Jackson BigDecimal DoS 指认不匹配；`CVE-2023-25871` 与 Hutool XXE 指认不匹配。 |

## brownfield-service-workflow.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| TD-1 | DagEngine 1540 行上帝类 | 部分成立 | 实际 1539 行，超大类事实成立，行数差 1。见 E17。 |
| TD-2 | CanvasExecutionService 1407 行 + 50 方法 | 成立 | 行数 1407；职责过大成立。见 E17。 |
| TD-3 | 14 个 Handler 直接注入 Mapper | 部分成立 | Handler 阻塞/直连 Mapper 方向成立，但具体数量需按当前类重新统计。见 H 系列和 E12。 |
| TD-4 | 7 处 @Lazy 循环依赖 | 部分成立 | 当前 7 处注解加 1 条注释；只能证明延迟依赖点，不能逐一等同循环依赖。 |
| TD-5 | Domain -> Engine 反向依赖 | 成立 | `CanvasService` 直接 import `DagParser/GroovyHandler/CanvasSchedulerService/CanvasExecutionService` 等 engine 类。 |
| TD-6 | canvas-editor 2085 行 | 部分成立 | 实际 2084 行，巨型组件成立。 |
| TD-7 | 前端零 ErrorBoundary | 成立 | `App.tsx` 未发现全局 ErrorBoundary 包裹。 |
| TD-8 | 前端零组件测试 | 不成立 | 当前前端测试文件 30 个。见 E18。 |
| TD-9 | data_source_config 密码明文 | 成立 | DB 明文存储成立；API JSON 返回泄露已被 WRITE_ONLY 防护。见 E6。 |
| TD-10 | CORS wildcard | 成立 | `allowed-origins: "*"` 且 credentials=true。见 E1/E3。 |
| TD-11 | 无分布式追踪 | 部分成立 | 项目已有 OTel/Micrometer 相关传递依赖痕迹，但代码层无 MDC/traceId 贯通；运行链路需验证。 |
| TD-12 | 无 CI/CD 流水线 | 成立 | 未发现 `.github`、`.gitlab-ci.yml`、`Jenkinsfile`。见 E17。 |
| TD-13 | CanvasExecutionService FIXME x2 | 成立 | dedup TTL 附近 FIXME 仍在。见 E11。 |
| TD-14 | InAppNotifyHandler TODO | 成立 | MQTT 未实现 TODO 仍在。 |

## failed-config-check-report-2026-06-02.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| #1 | CORS 配置存在严重安全风险 | 成立 | 默认 `*` + `allowCredentials=true`。见 E1/E3。 |
| #2 | 数据库明文凭证风险 | 成立 | app 配置与 local compose 均含 root/root，数据源密码明文存储。见 E1/E6/E15。 |
| #3 | Redis 无密码 | 部分成立 | Redis password 注释、compose 无密码成立；报告中“已有超时保护缺失”不成立，app 有 timeout/pool max-wait。 |
| #4 | JWT Secret 配置安全 | 部分成立 | `JwtUtil` 会对空 secret fail-fast，但配置默认空仍依赖部署注入；不能评为完全安全。见 E1。 |
| #5 | 配置即代码良好 | 部分成立 | 集中配置属实，但缺 prod profile/危险默认值，不能作为上线安全结论。 |

## main-branch-review.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| 1 | CouponHandler 缺少安全审计字段 | 部分成立 | 缺 `couponTypeKey` 校验、响应 message 可为 null、绕过共享 WebClient 成立；KMS/审计字段要求缺少仓库内规范证据。 |
| 2 | Scheduler 资源泄漏风险 | 成立 | `DagEngine.SPECIAL_NODE_TIMEOUT_SCHEDULER` 静态 scheduler 无 dispose/shutdown 钩子。 |
| 3 | StartHandler 类型转换缺少异常处理 | 成立 | `(List<Map<String,Object>>) config.get(...)` 未校验类型，`branches.get(i)` 可能 NPE。 |
| 4 | 缺少幂等性配置验证 | 部分成立 | CommitAction 不强制用户配置幂等键；下游默认 `executionId:nodeId`，不是完全缺失。 |
| 5 | 敏感数据硬编码风险 | 部分成立 | 默认 event secret 确实硬编码，但原文定位到 `DagEngine.java` 错，应为 `application.yml`。 |
| 6 | 测试覆盖率不完整 | 部分成立 | CommitAction 有测试；边界/失败场景覆盖仍不足。 |

## api-design-standards-audit-2026-06-02.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| Good: Global Exception Handling | 统一异常处理良好 | 部分成立 | Handler 存在，但 catch-all 返回 `e.getMessage()` 且 `R` 无 traceId。 |
| Issue #17 | DTO 无 Bean Validation | 成立 | 主代码未发现 `@Valid/@Validated`；starter-validation 非直接声明，DTO 约束也缺失。 |
| Issue #18 | 无 API 版本策略 | 成立 | 路由无 `/api/v1` 或版本协商。 |
| Issue #19 | Transaction 注解使用不一致 | 成立 | 16 处 `@Transactional`，15 处无 rollbackFor。 |
| Good: Swagger/OpenAPI | Swagger 集成良好 | 部分成立 | Swagger 存在，但所有环境 permitAll/默认启用是安全风险。 |

## infrastructure-security-scan-2026-06-02.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| Issue 1 | Dockerfile 无非 root 用户 | 成立 | 主 Dockerfile 无 `USER`；`Dockerfile.perf` 已加 USER。见 E15。 |
| Issue 2 | 无容器 hardening | 部分成立 | compose 未设置 read-only/cap_drop/security_opt 等；生产编排未提供，需部署验证。 |
| Issue 3 | JDBC 连接池容量不足 | 需运行验证 | `maximum-pool-size=33` 与目标并发 3000 不匹配属实；是否不足需压测。 |
| Issue 4 | DB 密码明文 | 成立 | 见 E1/E6/E15。 |
| Issue 5 | Redis 连接池安全缺超时保护 | 部分成立 | app 有 timeout/max-wait；无密码/TLS/server hardening 成立。 |
| Issue 6 | RocketMQ 无权限隔离 | 部分成立 | local compose/broker 无 ACL/TLS 成立；生产网络隔离需部署验证。 |
| Health Checks Present | Docker healthcheck 存在 | 成立 | 主 Dockerfile 有 `/actuator/health` healthcheck；语义不足另见 Round7/O5。 |

## code-review-logic-bugs-2026-06.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| 1 | @Transactional 缺 rollbackFor | 成立 | 15/16 主代码事务注解无 rollbackFor。 |
| 2 | 事务内混 Redis 路由注册 | 已修复 | `CanvasService.publish()` 已调用 `publishDb()` 后再处理 Redis/调度/缓存。 |
| 3 | InFlightExecutionRegistry TOCTOU | 成立 | `deregister` 仍是 get/remove/isEmpty/remove 类 check-then-act。 |
| 4 | 去重 TTL 配置竞态 | 成立 | dedup TTL 使用运行时 `globalTimeoutSec + 600`，代码有 FIXME。 |
| 5 | GroovyHandler 废弃 Executor API | 部分成立 | 无 shutdown 成立；“JDK21 已废弃”未被本地源码证明，需 JDK 文档确认。 |
| 6 | 重试计数非原子 | 需运行验证 | 静态上缺消费者级幂等；突破上限需 MQ 重投/并发场景验证。 |
| 7 | DLQ fire-and-forget | 成立 | `.subscribe()` 不纳入主链，JVM shutdown/拒绝时无完成保证。 |
| 8 | GroovyShell 对象池阻塞 | 部分成立 | `poll(100ms)` 阻塞成立；“池大小无上限”不成立，队列有界。 |
| 9 | 每次完成创建新虚拟线程 | 成立 | `incrementStats()` 每次 `Thread.ofVirtual().start()`。 |
| 10 | ctx 恢复版本不匹配 | 部分成立 | ctx 丢失时用当前版本重建成立；正常 WAIT 订阅已保存 versionId。 |
| 11 | ExecutionContext 并发一致性 | 成立 | `nodeOutputs` 与 `flatContext` 分开写，`triggerPayload/callStack` 非线程安全。 |
| 12 | MQ 消费线程阻塞 MyBatis | 成立 | `MqTriggerConsumer` 同步 enqueue/insert。 |
| 13 | 外部 HTTP 缺超时 | 部分成立 | 共享 builder 有超时；多个绕过 builder 的客户端缺统一超时/池/体积限制。 |
| 14 | @Autowired 字段注入过时 | 部分成立 | 存在字段注入/optional 注入，但主要是可维护性问题。 |
| 15 | DAG 深度保护未检测循环 | 部分成立 | `DagParser` 有拓扑结构构建，但未确认发布期强制循环拒绝；需补测试。 |
| 16 | 熔断器降级日志不完整 | 部分成立 | 熔断状态转换非原子、指标缺失成立；日志是否足够需运行/告警策略确认。 |
| 17 | 缺少关键监控指标 | 成立 | 多个关键指标/告警缺失，见 E18 与 O 系列。 |

## deep-code-audit-round2.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | Audience JDBC 动态 DataSource 连接泄漏 | 已修复 | `AudienceBatchComputeService` 当前 finally close DataSource；`AudienceUserResolver` 仍需单独确认。 |
| P0-2 | SQL 注入：表/列拼接限制不足 | 部分成立 | JDBC 动态表/列拼接存在，规则生成有参数化；完整白名单需专项验证。 |
| P0-3 | GroovyHandler 虚拟线程池永不关闭 | 成立 | 无 `@PreDestroy` shutdown。 |
| P0-4 | Reactor 链路 `.block()` | 部分成立 | TagImportSourceService/CanvasSchedulerService 等仍有 `.block()`；是否在 Netty event loop 需运行验证。 |
| P0-5 | ExecutionController 公开端点信任 userId | 部分成立 | behavior trigger 信任请求体；direct call 已改用 security context 但匿名可落 system。 |
| P0-6 | CanvasExecutionService God Class | 成立 | 1407 行。 |
| P0-7 | DagEngine 过度耦合 | 成立 | 1539 行且含调度/路由/trace/DLQ 等多职责。 |
| P0-8 | @Transactional + Redis 不一致 | 已修复 | publish 主路径已拆；其他事务外副作用仍需逐项审计。 |
| P1-1 | catch(Exception ignored) | 成立 | 多处仍存在吞异常/弱日志。 |
| P1-2 | InterruptedException 处理不当 | 需运行验证 | 需逐处线程中断语义验证。 |
| P1-3 | Thread.sleep in Reactor app | 部分成立 | 启动等待/测试或初始化路径存在，影响需运行验证。 |
| P1-4 | fire-and-forget subscribe 无错误处理 | 部分成立 | 部分 subscribe 有 error handler，仍不纳入生命周期。 |
| P1-5 | SqlWhereGenerator 规则缓存无上限 | 需运行验证 | 需检查缓存容量与真实规则基数。 |
| P1-6 | Groovy shell 临时创建 | 部分成立 | 临时创建成立；归还有界队列，不是永久池膨胀。 |
| P1-7 | ConditionEvaluator 静态 ObjectMapper | 成立 | 静态 new parser/objectMapper 模式存在。 |
| P1-8 | InFlight deregister 非原子 | 成立 | 同逻辑 #3。 |
| P1-9 | NodeGate CAS 竞态 | 需运行验证 | 需要并发测试证明错误路径。 |
| P1-10 | JWT localStorage | 成立 | 见 E13。 |
| P1-11 | 前端 any 类型 | 成立 | TS 中仍有大量 `any/as any`。 |
| P1-12 | WS ticket endpoint permitAll | 不成立 | ticket endpoint 需认证；WS endpoint 通过一次性 ticket。 |
| P2-1 | MapFieldKeys 691 行 | 成立 | 见 E17。 |
| P2-2 | 无 JaCoCo 门控 | 成立 | 未发现覆盖率门控。 |
| P2-3 | 无分布式追踪 | 部分成立 | 同 TD-11。 |
| P2-4 | 前端零组件测试 | 不成立 | 30 个测试文件。 |
| P2-5 | 前端无 ErrorBoundary | 成立 | 同 TD-7。 |
| P2-6 | TriggerRouteService Redis 事务 + spin-wait | 部分成立 | 路由锁等待存在；影响需运行验证。 |
| P2-7 | CanvasRouteInitializer Thread.sleep | 需运行验证 | 需结合启动顺序验证实际风险。 |
| P2-8 | 前端 API 无统一错误处理 | 成立 | `api.ts` 未检查业务 code，页面 catch 不一致。 |
| P2-9 | run_token 作为执行请求认证凭证 | 部分成立 | runToken 用于内部请求状态迁移，外部暴露面需运行/接口验证。 |
| P3-1 | 2 处 FIXME | 成立 | 见 E11。 |
| P3-2 | InAppNotifyHandler TODO | 成立 | 见 TD-14。 |
| P3-3 | BCryptPasswordEncoder 直接 new | 部分成立 | 存在默认 strength=10；是否必须 Bean 注入是风格问题。 |
| P3-4 | 前端硬刷新 | 成立 | canvas-editor 多处 `window.location.reload()`。 |

## deep-code-audit-round3.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | 多租户隔离失效 | 成立 | V78 仅 6 表 nullable tenant_id；实体/查询未全量过滤。 |
| P0-2 | DataSource password API 明文返回 | 不成立 | WRITE_ONLY 防止 JSON 返回；DB 明文另行成立。 |
| P0-3 | 14 Handler 直接 Mapper | 部分成立 | 系统性阻塞/直连成立，数量需修正。 |
| P1-1 | CanvasDO 缺 tenantId 字段映射 | 成立 | V78 给 canvas 加列，`CanvasDO` 无 `tenantId`。 |
| P1-2 | canvas-editor 巨型组件 | 成立 | 2084 行。 |
| P1-3 | console.log 泄露 JWT | 成立 | `AuthContext.tsx:73`。 |
| P1-4 | 7 处 @Lazy 循环依赖 | 部分成立 | 见关键更正。 |
| P1-5 | DB 无外键约束 | 成立 | 迁移未发现 FK/ON DELETE。 |
| P1-6 | editVersion 无重试 | 部分成立 | CAS 存在；自动重试/冲突策略不足。 |
| P2-1 | StringRedisTemplate 直接注入 | 部分成立 | 直接注入存在；是否必须抽象层属架构选择。 |
| P2-2 | Handler 注入 ObjectMapper 非 Spring 管理 | 部分成立 | 部分静态/手工 ObjectMapper 成立，数量需修正。 |
| P2-3 | 90 个 Flyway 迁移无基线 | 需运行验证 | 迁移数量多成立；新环境耗时需实测。 |
| P2-4 | 前端 useEffect 数量多 | 部分成立 | 复杂度成立，数量需当前统计。 |
| P2-5 | V78 ALTER 无 IF NOT EXISTS | 成立 | 迁移重复执行/repair 兼容风险成立。 |
| P3-1 | TenantService 硬编码 QueryWrapper | 部分成立 | 代码风格问题，非 bug。 |
| P3-2 | 前端 API create/update any | 成立 | TS 类型弱化存在。 |

## deep-code-audit-round4.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | HMAC 默认密钥硬编码 | 成立 | 见 E1/E4。 |
| P0-2 | API 零 Bean Validation | 成立 | 见 API Issue #17；“211 个”数量需修正。 |
| P1-1 | MySQL useSSL=false + allowPublicKeyRetrieval=true | 成立 | 见 E1。 |
| P1-2 | DataSourceConfigDO @Data + WRITE_ONLY 矛盾 | 部分成立 | API 序列化安全；`@Data.toString/equals/hashCode` 含 password 风险成立。 |
| P1-3 | LocalDateTime.now 无时区感知 | 部分成立 | 多处存在；实际跨时区影响需部署验证。 |
| P1-4 | 无文件上传大小限制 | 部分成立 | Controller 行数/内容限制存在；全局请求体/Multipart 限制需补。 |
| P1-5 | 前端 axios 无 timeout | 成立 | `axios.create({ baseURL: '/' })` 无 timeout。 |
| P2-1 | SysUserDO @Data + @JsonIgnore toString 泄露 | 成立 | Lombok @Data 风险成立。 |
| P2-2 | TagImport 文件类型无校验 | 成立 | 需补 content-type/extension 校验。 |
| P2-3 | CdpTagOperationController limit 无上限 | 部分成立 | 需按当前 controller 重新确认具体参数名；分页上限类问题成立。 |
| P2-4 | 定时任务默认 Asia/Shanghai 硬编码 | 成立 | 默认时区假设存在，国际化需配置化。 |
| P3-1 | testConnection DataSource 未关闭 | 需运行验证 | 需检查 DataSourceBuilder 实例类型与连接池关闭行为。 |

## deep-code-audit-round5.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | WebClient 每次请求重建实例 | 部分成立 | `ApiCallHandler/TagImportSourceService` 每次 build；多个 handler 构造期自建，不是全部“每次请求”。 |
| P1-1 | ConnectionProvider 无 idle eviction | 成立 | 缺 maxIdleTime/maxLifeTime/evictInBackground。 |
| P1-2 | HikariCP 无 leak detection | 成立 | 配置未见 leak-detection-threshold。 |
| P1-3 | health show-details=always | 成立 | 见 E1。 |
| P1-4 | DataMaskingUtil 覆盖率低 | 部分成立 | 调用覆盖不全成立；“仅 3 处”需当前统计。 |
| P1-5 | 无应用层加密 | 成立 | data_source_config/CDP/event 等无加密。 |
| P2-1 | CSRF 全局禁用 | 部分成立 | JWT Bearer 架构下风险较低；需文档和 cookie 策略确认。 |
| P2-2 | ApiCallHandler 响应体无大小限制 | 成立 | `bodyToMono(String.class)`，无 codec 限制。 |
| P2-3 | 其他 WebClient 响应体无大小限制 | 成立 | Coupon/Reach/Tagger/ReachDelivery 等 `bodyToMono(Map.class)`。 |
| P2-4 | CircuitBreaker 无 per-node 配置 | 部分成立 | 按 nodeType 有实例，但阈值全局；per-node 配置缺失。 |
| P2-5 | response-timeout 3s 可能不够 | 需运行验证 | 超时值存在，是否不够取决于外部 API SLA。 |
| P3-1 | SafeUpdateReq 无 @Valid | 成立 | 无 Bean Validation。 |
| P3-2 | CanvasOpsService 乐观锁无重试 | 部分成立 | CAS 存在；是否自动重试需产品/并发策略确认。 |

## deep-code-audit-round6.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P1-1 | ExecutionContext triggerPayload/callStack 非线程安全 | 成立 | 见 E9。 |
| P1-2 | Map config 零类型安全 | 成立 | Handler 接口普遍 `Map<String,Object>`，运行时 cast 多。 |
| P1-3 | Long.parseLong 无 try-catch | 部分成立 | 部分 MQ 路由解析已 try-catch；需逐处修正数量。 |
| P2-1 | @SuppressWarnings unchecked | 成立 | 多处存在。 |
| P2-2 | 前端 index-as-key | 部分成立 | 需当前组件逐处统计；风险模式成立。 |
| P2-3 | eslint-disable hooks 依赖 | 部分成立 | 需当前文件确认；如存在则成立。 |
| P2-4 | canvas-editor 状态管理过载 | 成立 | 巨型组件、useState/effect 多。 |
| P2-5 | UUID + Snowflake 混用 | 部分成立 | 混用成立；是否必须统一属架构取舍。 |
| P3-1 | memoization 偏低 | 需运行验证 | 需要渲染性能 profile。 |
| P3-2 | data-source-config raw fetch/services 不一致 | 部分成立 | 页面用 http/service 混合；可维护性问题。 |

## deep-code-audit-round7.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | Dockerfile root | 成立 | 主 Dockerfile 无 USER。 |
| P0-2 | 公开端点/ops 无认证 | 部分成立 | events/report 有 HMAC，WS 有 ticket；direct/behavior/ops 仍高风险。 |
| P0-3 | @Transactional rollbackFor | 成立 | 见逻辑 #1。 |
| P0-4 | 零分布式追踪 | 部分成立 | 见 TD-11。 |
| P0-5 | 零 HealthIndicator | 部分成立 | Actuator 基础 health 有，缺业务自定义 health。 |
| P1-1 | bare subscribe | 成立 | 仍有 fire-and-forget 生命周期问题。 |
| P1-2 | catch ignored | 成立 | 多处存在。 |
| P1-3 | R 无 traceId | 成立 | `R` 只有 code/message/data。 |
| P1-4 | MDC 零使用 | 成立 | logback 提取 MDC，但代码未发现 `MDC.put`。 |
| P1-5 | 无 application-prod.yml | 成立 | 未发现。 |
| P1-6 | synchronized in WebFlux | 需运行验证 | 是否阻塞事件循环需请求线程路径验证。 |
| P1-7 | 静态 ObjectMapper | 成立 | ConditionEvaluator 静态 new。 |
| P1-8 | 无 CI/CD | 成立 | 未发现流水线。 |
| P2-1 | Redis password 注释 + MySQL root 明文 | 成立 | 见 E1/E15。 |
| P2-2 | JWT 空默认 + HMAC 默认 | 部分成立 | JWT 空默认由 JwtUtil fail-fast；HMAC 默认成立。 |
| P2-3 | 默认 profile 非 prod | 部分成立 | Dockerfile 设 prod；非 Docker 启动仍可能非 prod。 |
| P2-4 | 虚拟线程无统一管理 | 成立 | 多处 `Thread.ofVirtual`/executor 无统一生命周期。 |
| P2-5 | CanvasMetrics 缺关键指标 | 成立 | 关键指标/告警不足。 |
| P2-6 | TODO/FIXME | 成立 | 见 TD-13/TD-14。 |
| P3-1 | JwtUtil Date API | 成立 | 可维护性问题。 |
| P3-2 | Vitest environment=node | 成立 | `vite.config.ts` test env 为 node。 |
| P3-3 | ThreadLocalRandom 不可控 | 部分成立 | 存在随机源；测试可控性需逐类设计。 |

## deep-code-audit-round8.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | 44/50 表缺 tenant_id | 部分成立 | 方向成立；精确 44/50 需 schema 重建，V78 仅 6 表加列。 |
| P0-2 | canvas_audit_log 零写入 | 成立 | 表存在，代码未找到写入。 |
| P0-3 | data_source_config.password 明文 | 成立 | 见 E6。 |
| P1-1 | CORS wildcard + credentials | 成立 | 见 E1/E3。 |
| P1-2 | pageSize 无上限 | 成立 | 多个分页参数缺统一 max。 |
| P1-3 | 零 API 版本策略 | 成立 | 见 API Issue #18。 |
| P1-4 | 零数据保留/归档策略 | 成立 | 大表无分区/归档策略。 |
| P1-5 | V78 tenant_id nullable | 成立 | 6 表均 nullable。 |
| P2-1 | V71/V78 无回滚脚本 | 部分成立 | Flyway Community 无 undo，数据迁移需人工回滚方案。 |
| P2-2 | 异步任务表无 tenant_id | 成立 | V78 未覆盖。 |
| P2-3 | CDP 用户表无 tenant_id | 成立 | V78 未覆盖。 |
| P2-4 | event_log 无分区 | 成立 | 迁移未见分区策略。 |
| P3-1 | Flyway `$${}` 占位符兼容 | 部分成立 | 当前 `placeholder-replacement=false`，风险来自未来配置改动。 |

## deep-code-audit-round9.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | Groovy 暴露 ExecutionContext | 成立 | `binding.setVariable("ctx", ctx)`。 |
| P0-2 | JWT localStorage | 成立 | 见 E13。 |
| P1-1 | 零安全响应头 | 成立 | SecurityConfig 未配置 CSP/HSTS/frame 等安全头。 |
| P1-2 | Lua 脚本内联 | 成立 | 多个 Redis Lua 以内联字符串存在。 |
| P1-3 | Groovy sandbox 可绕过 | 部分成立 | `ctx` 对象链绕过风险成立；具体绕过需安全测试。 |
| P1-4 | 零前端 XSS 防护 | 部分成立 | React 默认转义；缺 CSP/DOMPurify/输入策略成立。 |
| P2-1 | GroovyScriptCache 64-bit hash | 部分成立 | 取 8 字节成立；实际碰撞概率风险较低。 |
| P2-2 | Lua PEXPIREAT +60000 | 成立 | 额外时间硬编码。 |
| P2-3 | TriggerRouteService 动态 Lua | 部分成立 | 当前脚本来源硬编码；模式上不宜动态传入。 |
| P3-1 | window.open noopener/noreferrer | 成立 | 正面发现成立。 |

## deep-code-audit-round10.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | 零静态数据加密 | 成立 | data_source_config/CDP/event/trace 等明文。 |
| P1-1 | Jackson 2.15.4 CVE-2024-29857 | 不成立 | 包版本存在，但 CVE 指认与 NVD 不匹配。见 E20。 |
| P1-2 | commons-validator 1.7 CVE-2023-35889 | 需运行验证 | 依赖存在且为传递依赖；CVE 编号/影响需安全数据库二次确认。 |
| P1-3 | Hutool 5.8.44 CVE-2023-25871 | 不成立 | 包版本存在，但 CVE 指认与 NVD 不匹配。见 E20。 |
| P1-4 | BCrypt strength=10 偏低 | 成立 | 默认 `new BCryptPasswordEncoder()`。 |
| P2-1 | DataMaskingUtil key 不完整 | 成立 | PII key 覆盖不足。 |
| P2-2 | JWT 24h + 无 refresh token | 成立 | 配置 24h，前端无 refresh flow。 |
| P2-3 | HMAC 无轮换 | 成立 | 单密钥验证，无 key version。 |
| P3-1 | JWT 黑名单依赖 Redis 单点 | 部分成立 | 黑名单在 Redis；Redis 故障策略需运行验证。 |

## deep-code-audit-round11.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | 9 Handler 阻塞 Netty | 成立 | 多个 handler 直接 Mapper/service 阻塞调用无 boundedElastic。 |
| P1-1 | SSRF DNS rebinding TOCTOU | 部分成立 | URL 校验与 WebClient 连接分离成立；利用需安全测试。 |
| P1-2 | config.get 无 null 检查 | 成立 | 系统性 Map/cast 运行时风险。 |
| P1-3 | Groovy ExecutorService 无 shutdown | 成立 | 见 E8。 |
| P2-1 | TagImportSourceService Object.class 反序列化 | 部分成立 | `Object.class` 弱类型成立；RCE 需 ObjectMapper default typing 前提。 |
| P2-2 | 61 Handler 仅 29 注解 | 不成立 | 61 文件，60 个有 `@NodeHandlerType`。 |
| P2-3 | Handler 配置验证分散 | 成立 | 发布期统一校验不足。 |
| P3-1 | 自定义 SQL 参数化安全 | 成立 | 正面发现成立。 |

## deep-code-audit-round12.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | DataSourceConfigDO.password API 返回 | 不成立 | WRITE_ONLY 当前已防 JSON 返回。 |
| P1-1 | 发布/下线无乐观锁 | 部分成立 | `edit_version` CAS 只覆盖部分保存路径；发布/灰度/回滚仍缺统一并发控制。 |
| P1-2 | 执行端点零限流 | 成立 | direct/behavior permitAll 且无 IP/global rate limit。 |
| P1-3 | CanvasDO 无 @Version | 部分成立 | 确无 `@Version`；已有自定义 editVersion CAS，覆盖不足。 |
| P2-1 | axios 1.7.2 需检查 CVE | 已修复 | lockfile 当前 `axios 1.16.0`；npm audit 未报 axios。 |
| P2-2 | ApiDefinitionDO.apiKey 暴露 | 部分成立 | `apiKey` 是内部引用键非密钥；是否对前端隐藏需产品/API 策略确认。 |
| P3-1 | version diff 无审计 | 部分成立 | diff 是只读；如含敏感配置需脱敏/访问审计。 |

## deep-code-audit-round13.md

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P1-1 | WebSocket 无连接数限制 | 成立 | `sessionsByUser` 无 per-user/global limit。 |
| P1-2 | MQ 消费者零幂等保护 | 部分成立 | 执行层有 dedup；消费者/持久请求层缺唯一 source_msg_id 幂等。 |
| P1-3 | CanvasVersionCleanupJob 全表扫描 + N+1 | 成立 | `selectList(null)` + per canvas cleanup。 |
| P2-1 | WS sessions 纯内存 | 成立 | JVM 内存存储；重启断线。 |
| P2-2 | MQ 去重仅执行层 | 成立 | 与 P1-2 同源。 |
| P2-3 | 画布导入无内容大小校验 | 部分成立 | 需确认导入端点当前实现；全局请求体/graph 节点上限缺失方向成立。 |
| P3-1 | ExecutionWatchdog 30s 扫描延迟 | 部分成立 | fixedDelay=30s 成立；是否漏检取决于超时配置。 |

## deep-code-audit-2026-05-31.md：基线清单

| 原编号 | 原问题短名 | 判定 | 复核说明 |
|---|---|---|---|
| S1 | 公开执行/events 端点无认证 | 部分成立 | events/report 已 HMAC；direct/behavior/ops 仍高风险。 |
| S2 | `/ops/**` permitAll | 成立 | 见 E2。 |
| S3 | behaviorTrigger 信任 userId | 成立 | 见 E5。 |
| S4 | CORS `*` + credentials | 成立 | 见 E1/E3。 |
| S5 | console.log token | 成立 | 见 E13。 |
| S6 | JWT localStorage | 成立 | 见 E13。 |
| S7 | data_source_config 明文密码 | 成立 | 见 E6。 |
| R1 | CircuitBreaker checkState TOCTOU | 成立 | volatile state + 非原子状态切换。 |
| R2 | CircuitBreaker success/failure 竞态 | 成立 | 同 R1。 |
| R3 | ExecutionContext 非线程安全字段 | 成立 | 见 E9。 |
| R4 | TraceWriteBuffer pending 变负 | 不成立 | `ConcurrentLinkedQueue.offer` 正常不失败，pending 与 poll 数量匹配；可视为近似 gauge，不足以证明变负。 |
| R5 | ReachDelivery SELECT+INSERT 幂等竞态 | 部分成立 | DB 有唯一键会抛重复，不是双插入；非原子/异常处理仍需修。 |
| B1 | CdpTagWrite 阻塞 | 成立 | 直接 service/DB 调用。 |
| B2 | UpdateProfile 阻塞 | 成立 | 直接 mapper。 |
| B3 | FrequencyCap 阻塞 | 成立 | policy service 阻塞路径。 |
| B4 | SuppressionCheck 阻塞 | 成立 | policy service 阻塞路径。 |
| D1 | AudienceBitmap 32-bit 碰撞 | 部分成立 | murmur3_32 静态成立；“99.9%”需数据规模模型验证。 |
| D2 | WAIT 恢复二次消耗配额 | 已修复 | `isInternalContinuationTrigger` 已跳过。 |
| D3 | CanvasStats 全表聚合 | 成立 | 存在内存聚合/范围查询风险。 |
| C1 | TieredCache l2Key 碰撞 | 部分成立 | 当前 key 含 prefix/version，但 `keyToString` 类型语义仍需专项测试。 |
| C2 | freshL1IfPresent 非原子 | 需运行验证 | 需并发缓存一致性测试。 |
| P1-1 | Exception message 泄露 | 成立 | `GlobalExceptionHandler` 返回 `e.getMessage()`。 |
| P1-2 | 登录限流 TOCTOU | 成立 | `hasKey`/`increment` 非原子。 |
| P1-3 | reject reason 丢弃 | 成立 | reason 未形成审计闭环。 |
| P1-4 | approvers JSON parse ignored | 成立 | catch ignored/弱处理存在。 |
| P1-5 | defaultIfEmpty("system") | 成立 | 多 controller 存在。 |
| P1-6 | @RequestBody 无 @Valid | 成立 | 见 API Issue #17。 |
| P1-7 | DLQ/request 管理授权不足 | 部分成立 | 路由需结合 SecurityConfig 角色验证；细粒度权限不足方向成立。 |
| P1-8 | 审批 approve/reject 授权不足 | 部分成立 | 需业务角色矩阵确认。 |
| P1-9 | InFlight deregister 竞态 | 成立 | 同逻辑 #3。 |
| P1-10 | Disruptor fire-and-forget | 成立 | 见 E10。 |
| P1-11 | MarketingPolicy INCR+EXPIRE 非原子 | 成立 | 源码静态成立。 |
| P1-12 | ApiCallHandler 限流 INCR+EXPIRE 非原子 | 成立 | 源码静态成立。 |
| P1-13 | AudienceBatch releaseLock 不验证持有者 | 成立 | lock release 需 token 校验。 |
| P1-14 | LoopHandler merge 后读计数 | 需运行验证 | 需并发 handler 测试。 |
| P1-15 | LocalTaskScheduleRegistrar remove 竞态 | 需运行验证 | 需调度并发测试。 |
| P1-16 | Mono.delay Disposable 未保存 | 成立 | 生命周期不可取消。 |
| P1-17 | Groovy shell pool 无上限 | 部分成立 | 临时 shell 创建成立；池本身有界。 |
| P1-18 | CdpAudienceSource 全量加载 | 成立 | `selectList`/集合聚合路径存在。 |
| P1-19 | AudienceUserResolver ArrayList 无上限 | 成立 | JDBC resolve 收集 userId 到 List。 |
| P1-20 | CanvasSchedulerService WebClient block | 部分成立 | `.block()` 存在；共享 timeout/运行线程需验证。 |
| P1-21 | CdpUserDirectory N+1/无分页 | 成立 | 查询模式需重构。 |
| P1-22 | CanvasUserQuery 全量 execution/N+1 | 成立 | 查询模式需重构。 |
| P1-23 | CanvasVersionCleanup selectList(null) | 成立 | 见 Round13 P1-3。 |
| P1-24 | Funnel 查询缺复合索引 | 成立 | mapper 查询与迁移索引不匹配。 |
| P1-25 | Watchdog 全表扫描无 LIMIT | 部分成立 | 当前 WAIT 扫描有批量上限，审批/zombie 路径需再测。 |
| P1-26 | MySQL VALUES() 废弃 | 成立 | mapper 使用 `VALUES()`。 |
| P1-27 | CdpTagService removeTag 缺事务 | 成立 | 静态成立。 |
| P1-28 | RuleAst null stringify | 部分成立 | 需用规则单测确认 null 语义。 |
| P1-29 | AudienceScheduler 空 lambda | 成立 | `refreshAll` 需真实任务。 |
| P1-30 | Audience lock 2h TTL | 成立 | 长 TTL 导致故障恢复慢。 |
| P1-31 | handlerRegistry null check | 已修复 | `HandlerRegistry.get` 当前会抛明确异常；需确认 DagEngine 调用路径。 |
| P1-32 | single-flight 非分布式 | 部分成立 | 多实例击穿需运行验证。 |
| P1-33 | Pub/Sub 无 ACK | 成立 | 失效消息可靠性不足。 |
| P1-34 | 分布式锁不可重入 | 需运行验证 | 需缓存嵌套场景证明。 |
| P1-35 | Redis key 硬编码绕过 RedisKeyUtil | 部分成立 | 部分 key 已统一，仍有 `canvas:` 硬编码。 |
| P1-36 | 前端响应不校验 code | 成立 | 见 E13。 |
| P1-37 | auto-save effect | 成立 | 前端保存/草稿逻辑需修。 |
| P1-38 | 键盘 handler 闭包过期 | 成立 | effect/依赖问题成立。 |
| P1-39 | Notification 卸载竞态 | 成立 | 见 E14。 |
| P2-1 | approxSizeBytes 非原子超限 | 部分成立 | addAndGet/get 有窗口；实际超过限制需并发测试。 |
| P2-2 | Disruptor daemon shutdown 丢事件 | 成立 | 见 E10。 |
| P2-3 | CanvasMetrics gauge 捕获值 | 部分成立 | 需逐 gauge 核实；指标质量问题成立。 |
| P2-4 | SPECIAL scheduler 未 shutdown | 成立 | 同 main #2。 |
| P2-5 | DLQ fire-and-forget | 成立 | 同逻辑 #7。 |
| P2-6 | 超时分支错误仅 log | 成立 | 指标/DLQ不足。 |
| P2-7 | markSkippedPath 并发竞态 | 需运行验证 | 需 DAG 并发测试。 |
| P2-8 | priority 递归栈溢出 | 成立 | 递归组装风险成立。 |
| P2-9 | MqTriggerHandler optional mapper null | 成立 | `@Autowired(required=false)` 风险。 |
| P2-10 | WaitHandler subscription 错误处理 | 部分成立 | 需逐路径补错误处理。 |
| P2-11 | CanvasTrigger ASYNC fire-and-forget | 成立 | 无生命周期保证。 |
| P2-12 | SubFlowRef DB try-catch | 部分成立 | 异常传播可控性不足。 |
| P2-13 | WeightedChoice int overflow | 成立 | int sum 可能溢出。 |
| P2-14 | Swagger 所有环境 permitAll | 成立 | 见 E2。 |
| P2-15 | WS ticket URL query | 成立 | query 参数可能进日志。 |
| P2-16 | canvas-editor as any | 成立 | 类型绕过存在。 |
| P2-17 | Handler 重复 string helper | 成立 | 维护性问题。 |
| P2-18 | ReactiveRedisTemplate 无连接池配置 | 部分成立 | Lettuce pool 配置存在于 `spring.data.redis.lettuce.pool`；Reactive 实际连接池需运行验证。 |
| P3-1 | DagEngine 参数硬编码 | 成立 | 可配置化不足。 |
| P3-2 | SUPPRESSED traceStatus 语义 | 需运行验证 | 需业务统计语义确认。 |
| P3-3 | CircuitBreaker 构造参数不校验 | 成立 | 无阈值边界校验。 |
| P3-4 | HandlerRegistry 重复 type 检测 | 成立 | 需启动期 fail-fast。 |
| P3-5 | useCallback 依赖不完整 | 部分成立 | 需 ESLint/测试逐项确认。 |

## deep-code-audit-2026-05-31.md：深挖追加清单

下表保留原深挖 ID。短标题沿用原问题，不再重复原文完整描述。

| 原编号 | 判定 | 复核说明 |
|---|---|---|
| T1 | 部分成立 | WaitSubscription 当前保存 executionId/versionId；旧按 canvasId/userId 恢复风险已缓解，但上下文 key 仍需专项测。 |
| T2 | 成立 | 配额回滚非原子风险成立。 |
| T3 | 部分成立 | Scheduler WebClient `.block()` 成立；无超时需结合共享 builder 确认。 |
| T4 | 部分成立 | 多画布循环失败会带来重试/重复风险；执行层 dedup 可部分缓解。 |
| T5 | 需运行验证 | DLQ 失败无限重试需 MQ/DB 故障演练。 |
| T6 | 需运行验证 | watchdog/dedup 顺序需并发和崩溃测试。 |
| T7 | 成立 | parse trigger time 时区假设存在。 |
| T8 | 部分成立 | replay limiter Redis 异常路径需补 fallback；synchronized 影响需运行验证。 |
| V1 | 成立 | version 号 nextVersionNumber 读后写竞态。 |
| V2 | 成立 | startCanary 缺统一分布式锁/乐观锁。 |
| V3 | 成立 | rollback 缺统一并发保护。 |
| V4 | 成立 | CanvasDO 无 tenantId。 |
| V5 | 成立 | promoteCanary 后路由/调度/缓存更新不足。 |
| V6 | 成立 | cleanup 未排除 canaryVersionId 风险。 |
| V7 | 部分成立 | previousVersionId 字段存在；发布/回滚语义仍不完整。 |
| V8 | 部分成立 | Kill Switch Pub/Sub/L1/运行中取消保证不足。 |
| V9 | 成立 | 状态机转换校验不足。 |
| X1 | 成立 | shutdown 不等待 Reactor 链。 |
| X2 | 成立 | event.reset 早于异步 error 回调。 |
| X3 | 成立 | subscribe 绕过 Disruptor 背压。 |
| X4 | 部分成立 | request 事件只设 requestId；reset 通常清理，维护风险成立。 |
| X5 | 成立 | activeCount 监控不准风险成立。 |
| X6 | 部分成立 | fallback 通道释放语义需并发测试。 |
| X7 | 部分成立 | 高基数标签需逐 `CanvasMetrics` 核实；监控 OOM 风险方向成立。 |
| X8 | 已修复 | `ExecutionLaneResolver` 当前使用 `TriggerType.WAIT_RESUME` 常量。 |
| X9 | 需运行验证 | WorkerPool 排序保证需业务顺序要求确认。 |
| FE1 | 成立 | undo/redo 快照闭包风险。 |
| FE2 | 成立 | auto-save effect 风险。 |
| FE3 | 成立 | 快捷键闭包风险。 |
| FE4 | 成立 | bizConfig 浅复制风险。 |
| FE5 | 成立 | 删除边/节点引用竞态风险。 |
| FE6 | 成立 | onNodesChange 批处理丢变更风险。 |
| FE7 | 成立 | save canvas name 乐观锁缺失风险。 |
| FE8 | 成立 | edgeId 去重/多分支同目标风险。 |
| FE9 | 成立 | 模块级 Map cache 无驱逐。 |
| FE10 | 部分成立 | placeholder 过滤防线脆弱；需前后端 schema 校验。 |
| J1 | 成立 | `/ops/**` permitAll。 |
| J2 | 部分成立 | direct/behavior 成立；events/report HMAC。 |
| J3 | 不成立 | WS endpoint 通过 ticket；无连接限制另行成立。 |
| J4 | 成立 | 无 refresh token。 |
| J5 | 成立 | 密码修改不吊销旧 JWT。 |
| J6 | 成立 | tenantId claim 不刷新风险。 |
| J7 | 成立 | 登录限流 INCR/EXPIRE 非原子。 |
| J8 | 成立 | CORS 风险。 |
| J9 | 成立 | 密码强度仅长度。 |
| J10 | 部分成立 | token hash 截断/无 jti；碰撞风险低但撤销粒度不足。 |
| J11 | 成立 | JWT 无 jti。 |
| J12 | 成立 | logout catch ignored。 |
| J13 | 成立 | LoginReq 无 Bean Validation。 |
| J14 | 成立 | report-secret 默认值。 |
| Q1 | 成立 | HomeOverview `selectList(new LambdaQueryWrapper<>()).stream()` 后过滤。 |
| Q2 | 成立 | cleanup 全表扫描。 |
| Q3 | 成立 | AudienceScheduler 启动 selectList(null)。 |
| Q4 | 成立 | Funnel 查询缺复合索引。 |
| Q5 | 成立 | 逐条 UPDATE 清理 graphJson。 |
| G1 | 部分成立 | 多 scheduler bean 风险需 Spring context 启动验证。 |
| G2 | 成立 | 安全响应头缺失。 |
| G3 | 部分成立 | scheduler shutdown 等待策略需逐 bean 验证。 |
| G4 | 成立 | WebClient pool 无 idle/life eviction。 |
| G5 | 成立 | ObjectMapper 手工 new。 |
| G6 | 部分成立 | Snowflake workerId IP 推导风险需多实例验证。 |
| G7 | 不成立 | WS ticket 认证存在；permitAll 本身仍需注释说明。 |
| G8 | 成立 | health show-details always。 |
| N1 | 成立 | closeSocket/onclose 重连竞态。 |
| N2 | 成立 | onerror/onclose 双调度。 |
| N3 | 成立 | 无最大重试。 |
| N4 | 成立 | 无客户端 ping 心跳。 |
| N5 | 成立 | refresh 假设 data。 |
| N6 | 部分成立 | 去重仅 notificationId 需结合 payload 字段确认。 |
| N7 | 成立 | await 后未重检 stopped。 |
| N8 | 成立 | onclose 未检查当前 socket。 |
| M1 | 成立 | DLQ subscribe 生命周期问题。 |
| M2 | 成立 | 多处 Mono.delay subscribe 生命周期问题。 |
| M3 | 成立 | timeout continue fire-and-forget。 |
| M4 | 成立 | Mono.delay 缺 error handler。 |
| M5 | 需运行验证 | LOGIC/HUB 卡死恢复需并发 DAG 测试。 |
| M6 | 成立 | callStack 非线程安全。 |
| M7 | 需运行验证 | NodeGate 锁释放语义需并发测试。 |
| M8 | 成立 | 超时不取消运行节点。 |
| M9 | 成立 | oversize 仅 warn 不阻止。 |
| M10 | 成立 | priority 递归风险。 |
| M11 | 已修复 | HandlerRegistry 当前 fail-fast 抛明确异常。 |
| M12 | 需运行验证 | Hub 超时非原子需并发测试。 |
| M13 | 需运行验证 | NodeGate 双重释放需并发测试。 |
| MIG1 | 成立 | 30+ 表缺 tenant_id。 |
| MIG2 | 成立 | 缺 FK/ON DELETE。 |
| MIG3 | 成立 | 关键列 NULL 约束不足。 |
| MIG4 | 成立 | trace/request 缺复合索引。 |
| MIG5 | 成立 | error_msg/tag_value 可能截断。 |
| MIG6 | 成立 | created_at/updated_at 默认不足。 |
| MIG7 | 成立 | enum-like 字段缺 CHECK。 |
| MIG8 | 部分成立 | 部分注册迁移已有 ON DUPLICATE，需逐 V 文件修正数量。 |
| MIG9 | 成立 | 高增长表无分区。 |
| MIG10 | 成立 | V14 SELECT 1 占位。 |
| MIG11 | 成立 | V6 SELECT 1/deferred。 |
| MIG12 | 不成立 | V78 先 upsert default tenant，再 UPDATE；“tenant 表空必失败”不成立。 |
| MIG13 | 成立 | 索引命名前缀不一致。 |
| MIG14 | 部分成立 | VCS 中 BCrypt hash 不是明文密码，但仍是测试凭证/敏感样本。 |
| MIG15 | 成立 | V41/V43/V71 root/root。 |
| MIG16 | 成立 | 缺 ON DELETE CASCADE。 |
| MIG17 | 成立 | VARCHAR 命名不一致。 |
| MIG18 | 部分成立 | TIMESTAMP/DATETIME 混用，实际时区影响需验证。 |
| TEST1 | 成立 | CircuitBreakerRegistry 缺直接测试。 |
| TEST2 | 成立 | TriggerPreCheckService 缺直接测试。 |
| TEST3 | 成立 | CanvasOpsService 覆盖不足。 |
| TEST4 | 成立 | WeightedChoice 缺直接测试。 |
| TEST5 | 成立 | MarketingPolicyService 缺直接测试。 |
| TEST6 | 成立 | ReachDeliveryService 缺直接测试。 |
| TEST7 | 部分成立 | DagEngine 有测试但覆盖不全。 |
| TEST8 | 部分成立 | InFlight 有部分测试但覆盖不全。 |
| TEST9 | 部分成立 | WaitResume 有测试但覆盖不全。 |
| TEST10 | 部分成立 | Disruptor 有测试但缺 shutdown/失败/背压。 |
| TEST11 | 部分成立 | Handler 测试不足；“50 个无测试”需当前统计。 |
| TEST12 | 部分成立 | SecurityConfig 有角色测试，缺路由授权/JWT。 |
| TEST13 | 部分成立 | 前端有测试，但核心组件覆盖不足。 |
| TEST14 | 成立 | CanvasService 覆盖不足。 |
| O1 | 成立 | subscribe 错误/生命周期问题。 |
| O2 | 成立 | catch ignored/弱日志。 |
| O3 | 成立 | trace ID/MDC 未贯通。 |
| O4 | 成立 | 关键业务指标缺失/未调用。 |
| O5 | 部分成立 | 基础 health 有；缺 MQ/外部服务/Disruptor health。 |
| O6 | 成立 | DLQ 告警/dashboard 不足。 |
| O7 | 成立 | 结构化上下文字段不完整。 |
| O8 | 成立 | SLO/SLI 未定义。 |
| O9 | 成立 | Redis failure metric 不足。 |
| O10 | 成立 | watchdog failure metric/alert 不足。 |
| O11 | 成立 | Disruptor overflow 告警不足。 |
| H1 | 成立 | PointsOperation 阻塞。 |
| H2 | 成立 | TagOperation 阻塞。 |
| H3 | 成立 | GoalCheck 阻塞/异常传播。 |
| H4 | 成立 | TrackEvent 阻塞/异常传播。 |
| H5 | 成立 | CreateTask 阻塞。 |
| H6 | 成立 | ManualApproval 阻塞。 |
| H7 | 成立 | SubFlowRef 阻塞/CPU JSON。 |
| H8 | 成立 | CanvasTrigger 阻塞。 |
| H9 | 成立 | ChannelAvailability 阻塞。 |
| H10 | 成立 | QuietHours 阻塞。 |
| H11 | 成立 | TransferJourney fire-and-forget。 |
| H12 | 成立 | CanvasTrigger async fire-and-forget。 |
| H13 | 成立 | Scoring IN 子串逻辑。 |
| H14 | 成立 | Coupon couponTypeKey 未校验。 |
| H15 | 成立 | Coupon response message null。 |
| H16 | 成立 | WeightedChoice int overflow。 |
| H17 | 成立 | AbSplit Math.abs(MIN_VALUE)。 |
| H18 | 部分成立 | Tag upsert 非原子；DB unique 会抛重复而非双记录。 |
| H19 | 部分成立 | Points 幂等非原子；DB unique 可防双插但异常处理不足。 |
| H20 | 部分成立 | MqTrigger resolveTopic 阻塞依赖调用方线程。 |
| H21 | 成立 | TaggerOffline tagCodeKey 未校验。 |
| Y1 | 成立 | DB root/root + SSL 风险。 |
| Y2 | 成立 | CORS default `*`。 |
| Y3 | 成立 | 无 application-prod.yml。 |
| Y4 | 成立 | Redis 无密码。 |
| Y5 | 成立 | report-secret 默认值。 |
| Y6 | 成立 | Swagger 默认启用/permitAll。 |
| Y7 | 成立 | health show-details always。 |
| Y8 | 成立 | 集成 URL 默认 WireMock/localhost。 |
| Y9 | 成立 | graceful shutdown 配置缺失。 |
| Y10 | 成立 | 多个 handler 自建 WebClient。 |
| Y11 | 需运行验证 | Hikari 33 vs 并发 3000 需压测。 |
| Y12 | 需运行验证 | Redis pool 64/100ms 是否不足需压测。 |
| Y13 | 成立 | permitAll 端点缺防护层限流。 |
| Y14 | 需运行验证 | Groovy 5s 是否过长需容量测试。 |
| Y15 | 需运行验证 | breaker 阈值 5 是否过低需故障注入。 |
| Y16 | 成立 | MQ consumeThreadNumber 硬编码。 |
| Y17 | 成立 | Flyway 缺 clean-disabled。 |
| Y18 | 部分成立 | `!prod` DEBUG 风险取决 profile；默认/启动脚本需固定。 |
| Y19 | 部分成立 | 应用无 SSL；若由网关终止 TLS 需部署验证。 |
| W1 | 成立 | 无 ErrorBoundary。 |
| W2 | 成立 | 401 硬跳登录/无 refresh。 |
| W3 | 部分成立 | TENANT_ADMIN 进入 admin 路由与后端权限一致；是否越权需产品角色矩阵确认。 |
| W4 | 部分成立 | React 默认转义；XSS 需确认 Modal/HTML 渲染路径。 |
| W5 | 成立 | auto-save 同 FE2。 |
| W6 | 成立 | 快捷键同 FE3。 |
| W7 | 成立 | fetch 缺 catch/stale guard 风险。 |
| W8 | 成立 | 多页面错误处理不一致。 |
| W9 | 成立 | token log。 |
| W10 | 成立 | polling effect 依赖导致 interval 重建风险。 |
| W11 | 成立 | canvas-list fetch 无 catch。 |
| W12 | 部分成立 | Bearer token 架构下 CSRF 较低；若 cookie 化需启用 CSRF。 |

## deep-code-audit-all-rounds-summary.md

该文件是汇总文档，不新增独立源码事实；仍按原汇总行保留判定。

| 原编号 | 原问题 | 判定 | 复核说明 |
|---|---|---|---|
| P0-1 | SecurityConfig 4 个公开端点无认证 | 部分成立 | events/report 和 WS 描述过度；direct/behavior/ops 成立。 |
| P0-2 | Dockerfile root | 成立 | 主 Dockerfile 成立。 |
| P0-3 | CORS wildcard | 成立 | 成立。 |
| P0-4 | JWT localStorage | 成立 | 成立。 |
| P0-5 | DB 凭证/Redis/useSSL | 成立 | 成立。 |
| P0-6 | TENANT_ADMIN 权限提升 | 部分成立 | 需产品角色矩阵确认。 |
| P0-7 | tenant_id/IDOR | 部分成立 | 多租户缺口成立，IDOR 数量需专项 API 授权测试。 |
| P0-8 | Groovy ctx 暴露 | 成立 | 成立。 |
| P0-9 | 14 Handler 阻塞 | 成立 | 系统性成立，数量需修正。 |
| P0-10 | 9 Handler 阻塞 | 成立 | 成立。 |
| P0-11 | triggerPayload 非线程安全 | 成立 | 成立。 |
| P0-12 | callStack 非线程安全 | 成立 | 成立。 |
| P0-13 | DagEngine fire-and-forget | 成立 | 成立。 |
| P0-14 | catch ignored | 成立 | 成立。 |
| P0-15 | AudienceBitmap 碰撞 | 部分成立 | 模型/概率需验证。 |
| P0-16 | Scoring IN 逻辑 | 成立 | 成立。 |
| P0-17 | 无 graceful shutdown | 部分成立 | 多组件成立，TraceWriteBuffer 有 PreDestroy。 |
| P0-18 | 虚拟线程无统一管理 | 成立 | 成立。 |
| P0-19 | 15/19 transactional | 部分成立 | 实际 15/16 主代码裸事务。 |
| P0-20 | tracing/MDC/R traceId | 成立 | 成立。 |
| P0-21 | 零 HealthIndicator | 部分成立 | 基础 actuator 有，自定义不足。 |
| P0-22 | 44/50 tenant_id | 部分成立 | 方向成立，精确数需 schema 统计。 |
| P0-23 | audit log 零写入 | 成立 | 成立。 |
| P0-24 | data_source_config 明文 | 成立 | 成立。 |
| P0-25 | 零静态加密 | 成立 | 成立。 |
| P0-26 | 零保留/归档 | 成立 | 成立。 |
| P0-27 | 无 application-prod.yml | 成立 | 成立。 |
| P0-28 | RequestBody 缺 Valid | 成立 | 成立。 |
| P0-29 | 无 CI/CD | 成立 | 成立。 |
| P0-30 | 零 ErrorBoundary | 成立 | 成立。 |
| P0-31 | prod 危险默认值 | 成立 | 成立。 |
| P0-32 | Redis 单点导致 paused 卡死 | 需运行验证 | 需要 Redis 故障演练。 |
| P0-33 | 5 bare subscribe | 成立 | 成立。 |

## 依赖扫描复核

| 项 | 判定 | 复核说明 |
|---|---|---|
| 后端 Jackson 2.15.4 | 需运行验证 | 版本存在；原 CVE 指认错误。需用 OWASP Dependency-Check/官方 advisory 重新扫描。 |
| 后端 commons-validator 1.7 | 需运行验证 | 传递依赖存在；原 CVE 需二次确认，建议统一升级或排除。 |
| 后端 Hutool 5.8.44 | 部分成立 | `hutool-all` 攻击面大成立；原 CVE 指认错误。 |
| 前端 axios | 已修复 | lockfile 为 1.16.0，npm audit 未报 axios。 |
| 前端 vite/esbuild/vitest | 成立 | npm audit 报 2 moderate + 1 critical。见 E19。 |

## 进入整改 spec 的范围

纳入 spec/plan：所有 `成立`、仍有修复价值的 `部分成立`、以及需要通过压测/部署验证来关闭风险的 `需运行验证`。
排除整改范围：已修复项、不成立项、纯正面发现。排除不代表删除，仍保留在本台账中。
