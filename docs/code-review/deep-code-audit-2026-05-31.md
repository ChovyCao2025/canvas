# Canvas 营销画布平台 — 深度代码审核报告

**审核日期**: 2026-05-31  
**审核方法**: BMAD Brownfield Full-Stack Workflow + PO Master Checklist  
**审核范围**: 后端(Java 21/Spring Boot/WebFlux) + 前端(React 18/TypeScript) + 缓存SDK + 基础设施  
**代码规模**: 29 Controllers, 64 NodeHandlers, 90 Flyway Migrations, 112 Backend Tests, 30 Frontend Tests  

---

## 总览

| 严重度 | 数量 | 说明 |
|--------|------|------|
| **P0 CRITICAL** | **69** | 生产环境必然失败或安全漏洞 |
| **P1 IMPORTANT** | **157** | 高风险，数据丢失/性能/合规问题 |
| **P2 SHOULD FIX** | **80** | 中等风险，技术债 |
| **P3 NICE TO HAVE** | **14** | 低优先级改进 |

**整体就绪度**: 8%
**最终决定**: **REJECTED** — 必须修复P0后方可推进任何生产部署  
**最终决定**: **REJECTED** — 必须修复P0后方可推进任何生产部署

---

## P0 CRITICAL（21项）

### 安全（7项）

| # | 文件 | 问题 | 影响 |
|---|------|------|------|
| S1 | SecurityConfig.java:62-66 | `/canvas/execute/direct/*`, `/canvas/trigger/behavior`, `/canvas/events/report` 完全无认证（permitAll），report-secret定义了但从未校验 | 任意网络可达客户端可触发画布执行，造成真实业务副作用 |
| S2 | SecurityConfig.java:70 | `/ops/**` permitAll 暴露缓存失效、模板管理、待审记录 | 无认证缓存失效=DoS武器，数据泄露 |
| S3 | ExecutionController.java:65-73 | behaviorTrigger 信任客户端传入的 userId，无校验 | 完整用户冒充，绕过配额/疲劳控制/审计 |
| S4 | WebConfig.java:28-49 | CORS默认 `*` + `allowCredentials=true`，无启动校验 | 任意恶意网站可发起带凭证的跨域请求 |
| S5 | AuthContext.tsx:73 | 生产代码中 `console.log('[AUTH] init token:', token?.slice(0,20))` | Token部分信息泄露到控制台 |
| S6 | api.ts:24,38-39 | JWT存储在 localStorage（XSS可窃取） | XSS攻击可劫持用户会话 |
| S7 | V71__data_source_config.sql:7 | data_source_config 密码明文存储 | DB被入侵后横向移动风险，合规违规 |

### 并发/竞态（5项）

| # | 文件 | 问题 | 影响 |
|---|------|------|------|
| R1 | CircuitBreakerRegistry.java:96-116 | checkState() TOCTOU：volatile读+写非原子，多线程可同时从OPEN→HALF_OPEN | 熔断器保护机制被绕过 |
| R2 | CircuitBreakerRegistry.java:118-144 | recordSuccess/recordFailure 状态转换竞态 | HALF_OPEN下成功/失败竞态导致状态不一致 |
| R3 | ExecutionContext.java:65,47 | callStack(ArrayList)和triggerPayload(HashMap)非线程安全，但被并发访问 | ConcurrentModificationException或数据损坏 |
| R4 | TraceWriteBuffer.java:45-56 | pending计数器与buffer.offer()之间存在竞态 | 计数器与实际队列大小失同步，可能变负 |
| R5 | ReachDeliveryService.java:70-93 | 幂等SELECT+INSERT的TOCTOU竞态 | 并发请求绕过幂等保护，用户收到重复消息 |

### 阻塞Reactor线程（4项）

| # | 文件 | 问题 | 影响 |
|---|------|------|------|
| B1 | CdpTagWriteHandler.java:63 | tagService.setTag() 阻塞DB调用未包装boundedElastic | 阻塞Netty事件循环，应用无响应 |
| B2 | UpdateProfileHandler.java:62-86 | profileMapper.selectOne/insert/updateById 直接在reactive路径执行 | 同上 |
| B3 | FrequencyCapHandler.java:62-63 | policyService.consumeFrequency() 未包装 | 同上 |
| B4 | SuppressionCheckHandler.java:56-63 | policyService.consentAllowed()/suppressionAllowed() 未包装 | 同上 |

### 数据正确性（3项）

| # | 文件 | 问题 | 影响 |
|---|------|------|------|
| D1 | AudienceBitmapStore.java:42-48 | murmur3_32哈希到32位整数，10M用户碰撞率99.9% | 错误用户被纳入受众，发送不该发的消息 |
| D2 | WaitResumeService.java:42-48 | WAIT恢复时trigger()经过完整pre-check路径，cooldown/配额被二次消耗 | Wait/Resume功能在有cooldown或per-user限制时完全失效 |
| D3 | CanvasStatsController.java:127-131 | 全表扫描加载所有execution到内存做聚合 | 百万级数据=OOM崩溃 |

### 缓存/基础设施（2项）

| # | 文件 | 问题 | 影响 |
|---|------|------|------|
| C1 | TieredCacheImpl.java:1069-1071 | l2Key()用String.valueOf(key)无类型区分，不同缓存实例key可碰撞 | 缓存数据损坏，ClassCastException |
| C2 | TieredCacheImpl.java:929-941 | freshL1IfPresent()版本检查非原子，L1/L2不一致窗口 | L1缓存可能返回过期数据 |

---

## P1 IMPORTANT（39项）

### 安全/认证（6项）

| # | 文件 | 问题 |
|---|------|------|
| P1-1 | GlobalExceptionHandler.java:84 | catch-all返回 `"系统错误: " + e.getMessage()` 泄露内部异常信息 |
| P1-2 | AuthController.java:56-89 | 登录限流TOCTOU：hasKey检查与increment非原子，并发可绕过 |
| P1-3 | CanvasExecutionManagementController.java:75-83 | reject的reason参数被丢弃，无审计记录 |
| P1-4 | CanvasExecutionManagementController.java:116-127 | approvers JSON解析失败时catch(Exception ignored)静默跳过校验 |
| P1-5 | 多个Controller | `defaultIfEmpty("system")` 在安全上下文缺失时创建幽灵超级用户 |
| P1-6 | 所有Controller DTO | @RequestBody无@Valid注解，DTO无Bean Validation约束 |

### 授权缺失（2项）

| # | 文件 | 问题 |
|---|------|------|
| P1-7 | DlqController, CanvasExecutionRequestManagementController | 任何认证用户可replay/delete DLQ和执行请求 |
| P1-8 | CanvasExecutionManagementController | 任何认证用户可approve/reject审批 |

### 并发/竞态（7项）

| # | 文件 | 问题 |
|---|------|------|
| P1-9 | InFlightExecutionRegistry.java:154-167 | deregister中isEmpty()+remove(canvasId)的check-then-act竞态 |
| P1-10 | CanvasDisruptorService.java:132-155 | fire-and-forget subscribe丢失结果，Mono挂起=slot永久泄漏 |
| P1-11 | MarketingPolicyService.java:159-169 | 非原子INCR+EXPIRE，JVM崩溃=永久僵尸Redis key |
| P1-12 | ApiCallHandler.java:271-286 | 限流increment+expire非原子，key可能永不过期 |
| P1-13 | AudienceBatchComputeService.java:369-375 | releaseLock不验证锁持有者，可删除他人锁 |
| P1-14 | LoopHandler.java:39 | ConcurrentHashMap.merge后立即读nextCount，并发下迭代计数不准 |
| P1-15 | LocalTaskScheduleRegistrar.java:134-147 | 一次性任务finally中remove可能删除新注册的同key任务 |

### 资源泄漏/性能（10项）

| # | 文件 | 问题 |
|---|------|------|
| P1-16 | DagEngine.java:636-638 | Mono.delay().subscribe()未存储Disposable，超时处理器变僵尸 |
| P1-17 | GroovyHandler.java:154-212 | shellPool耗尽时新建Shell也归还池，池大小无上限=内存泄漏 |
| P1-18 | CdpAudienceSourceService.java:82-121 | selectList(null)加载全表到内存，百万用户=OOM |
| P1-19 | AudienceUserResolver.java:41-80 | resolveJdbc收集所有userId到ArrayList，无上限 |
| P1-20 | CanvasSchedulerService.java:417-452 | WebClient .block()无超时，阻塞调度线程 |
| P1-21 | CdpUserDirectoryService.java:41-58 | N+1查询+无界IN子句+无分页 |
| P1-22 | CanvasUserQueryService.java:36-40 | 加载全部execution无LIMIT+N+1 ensureUser调用 |
| P1-23 | CanvasVersionCleanupJob.java:44 | selectList(null)加载所有画布，无分页 |
| P1-24 | CanvasExecutionTraceMapper.xml:20 | Funnel查询全表扫描最大表，缺复合索引 |
| P1-25 | ExecutionWatchdog.java:114-136 | scanApprovalTimeout每30秒全表扫描无LIMIT |

### 数据/逻辑（6项）

| # | 文件 | 问题 |
|---|------|------|
| P1-26 | CanvasExecutionStatsMapper.java:21 | VALUES()在MySQL 8.0.20+已废弃，8.4+将移除 |
| P1-27 | CdpTagService.java:95-109 | removeTag缺少@Transactional，tag更新与history写入可能不一致 |
| P1-28 | RuleAstEvaluator.java:102 | stringify(null)返回"null"字符串，EQ/NEQ对null字段行为不正确 |
| P1-29 | AudienceSchedulerService.java:52-59 | refreshAll()用空lambda替换真实计算任务，重启后受众数据永不刷新 |
| P1-30 | AudienceBatchComputeService.java:85-139 | Redis锁2小时TTL，JVM崩溃=受众计算阻塞2小时 |
| P1-31 | DagEngine.java:288,911 | handlerRegistry.get(node.getType())无null检查，未注册类型=NPE崩溃 |

### 缓存/Redis（4项）

| # | 文件 | 问题 |
|---|------|------|
| P1-32 | TieredCacheImpl.java:596-599 | 本地single-flight非分布式，多实例缓存未命中=缓存击穿 |
| P1-33 | TieredCacheManager.java:100-107 | Pub/Sub无ACK机制，消息丢失=L1永久过期 |
| P1-34 | TieredCacheImpl.java:741-755 | 分布式锁非可重入，嵌套缓存操作=死锁 |
| P1-35 | 多处(MarketingPolicyService,AuthController,JwtAuthFilter,AudienceBitmapStore) | 硬编码Redis key前缀绕过RedisKeyUtil，多环境部署key碰撞 |

### 前端（4项）

| # | 文件 | 问题 |
|---|------|------|
| P1-36 | api.ts:31-44 | 响应拦截器不校验code!==0，业务错误被当成功处理 |
| P1-37 | canvas-editor/index.tsx:500-507 | auto-save useEffect无依赖数组，每次渲染都触发 |
| P1-38 | canvas-editor/index.tsx:650-699 | 键盘事件处理器闭包捕获过期状态 |
| P1-39 | NotificationContext.tsx:171-208 | WebSocket连接卸载时异步操作可能更新已卸载组件状态 |

---

## P2 SHOULD FIX（18项）

| # | 类别 | 文件 | 问题 |
|---|------|------|------|
| P2-1 | RACE | ExecutionContext.java:135-143 | approxSizeBytes addAndGet+get非原子，上下文可能超1MB限制 |
| P2-2 | LEAK | CanvasDisruptorService.java:164 | Daemon线程，JVM关闭可能丢失ring buffer中未消费事件 |
| P2-3 | PERF | CanvasMetrics.java:57-61 | Gauge重复注册，lambda捕获首次count值，后续调用不更新 |
| P2-4 | LEAK | DagEngine.java:158-159 | SPECIAL_NODE_TIMEOUT_SCHEDULER未shutdown |
| P2-5 | ERROR | DagEngine.java:560-562 | DLQ写入fire-and-forget，失败仅log无重试/指标 |
| P2-6 | ERROR | DagEngine.java:811-857 | 超时分支执行错误仅log，无指标/DLQ |
| P2-7 | RACE | DagEngine.java:1501-1522 | markSkippedPath可能与并发执行竞态（主要是trace问题） |
| P2-8 | BUG | DagEngine.java:1102-1146 | tryPrioritySequentially递归，50+分支可能栈溢出 |
| P2-9 | NULL | MqTriggerHandler.java:27-28 | mqMessageDefinitionMapper @Autowired(required=false)可能null |
| P2-10 | ERROR | WaitHandler.java:244-254 | waitSubscriptionService调用无错误处理 |
| P2-11 | ERROR | CanvasTriggerHandler.java:121 | ASYNC模式fire-and-forget无错误日志 |
| P2-12 | ERROR | SubFlowRefHandler.java:105-117 | DB调用无try-catch |
| P2-13 | BUG | WeightedChoice.java:36 | 权重总和可能int溢出 |
| P2-14 | SECURITY | SecurityConfig.java:59-60 | Swagger/OpenAPI所有环境permitAll |
| P2-15 | SECURITY | notificationRealtime.ts:11 | WebSocket ticket在URL查询参数中，可能被日志记录 |
| P2-16 | DESIGN | canvas-editor/index.tsx | 15+处 `as any` 类型断言绕过类型安全 |
| P2-17 | DESIGN | 多个Handler | 10+个Handler重复private string()辅助方法 |
| P2-18 | PERF | RedisConfig.java | ReactiveRedisTemplate无连接池配置 |

---

## P3 NICE TO HAVE（5项）

| # | 问题 |
|---|------|
| P3-1 | DagEngine硬编码MAX_NODE_DEPTH=200和scheduler参数，无配置化 |
| P3-2 | NodeResult.traceStatus()中SUPPRESSED映射为1(success)语义不正确 |
| P3-3 | CircuitBreaker构造函数不校验参数（failureThreshold=0等） |
| P3-4 | HandlerRegistry.init()不检测重复typeKey注册 |
| P3-5 | 前端多处useCallback依赖数组不完整 |

---

## 按子系统分布

| 子系统 | P0 | P1 | P2 | 最关键问题 |
|--------|----|----|----|----|
| **安全/认证** | 7 | 8 | 1 | 无认证执行端点、CORS通配符、用户冒充 |
| **DAG引擎** | 7 | 12 | 5 | fire-and-forget泄漏×6、超时不取消执行、Gate竞态、递归栈溢出 |
| **NodeHandler** | 14 | 14 | 2 | 14个handler阻塞Reactor线程(系统性问题)+2处fire-and-forget |
| **执行上下文/通道** | 1 | 4 | 1 | callStack/triggerPayload非线程安全 |
| **投递/受众/规则** | 3 | 6 | 1 | 幂等竞态、位图碰撞、Wait恢复失效 |
| **数据库/持久化** | 1 | 6 | 0 | 全表扫描OOM、VALUES()废弃、明文密码 |
| **缓存/Redis** | 2 | 4 | 1 | Key碰撞、L1/L2不一致、缓存击穿 |
| **前端** | 6 | 12 | 3 | 无ErrorBoundary、401硬跳转、TENANT_ADMIN权限提升、auto-save失效 |
| **触发器/MQ** | 3 | 5 | 0 | 审批超时加载错误上下文、配额回滚非原子、调度线程阻塞 |
| **版本/发布/灰度** | 4 | 5 | 0 | 版本号竞态、灰度缺锁、回滚仅1版、缺租户隔离 |
| **Disruptor/通道** | 3 | 5 | 0 | 关闭丢事件、event.reset竞态、fire-and-forget无背压 |
| **前端画布编辑器** | 2 | 6 | 2 | undo/redo闭包过期、auto-save永不触发、拷贝浅复制 |
| **认证/JWT** | 3 | 7 | 4 | WebSocket permitAll、无Refresh Token、密码改后Token不过期、租户ID不刷新 |
| **MyBatis/SQL** | 0 | 2 | 3 | 无注入风险(正面)、高频API全表扫描、批量清理逐条UPDATE |
| **Spring配置** | 1 | 7 | 0 | 重复TaskScheduler Bean、缺安全头、Snowflake ID碰撞 |
| **前端通知/WebSocket** | 3 | 5 | 0 | closeSocket竞态、双重调度、无限重试、无心跳 |
| **Flyway迁移链** | 5 | 7 | 6 | 30+表缺tenant_id、全量缺FK、NOT NULL缺失、无分区 |
| **测试覆盖率** | 0 | 6 | 6 | 6个关键类零测试、50+handler无测试、DagEngine不完整 |
| **可观测性** | 5 | 6 | 0 | .subscribe()静默丢错、无trace ID、关键业务指标缺失、健康检查不完整 |
| **配置(application.yml)** | 5 | 14 | 0 | 无prod profile、硬编码凭证、CORS*、Redis无密码、Swagger默认开启 |

---

## 深挖发现（第二轮审计）

### 触发器/MQ子系统（3项P0 + 5项P1）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| T1 | P0 | ExecutionWatchdog.java:129,154 | 审批超时加载上下文用(canvasId,userId)做key，同用户多次执行=加载错误上下文 | 超时审批结果写入错误执行，路由不正确 |
| T2 | P0 | TriggerPreCheckService.java:219-229 | incrementWithinLimit超限后DECR回滚非原子，Redis故障=配额永久膨胀 | 合法触发被错误拒绝 |
| T3 | P0 | CanvasSchedulerService.java:411-458 | resolveUserIds中WebClient .block()无超时，阻塞调度线程池 | 所有定时画布停摆 |
| T4 | P1 | MqTriggerConsumer.java:41 | 多画布循环发布部分失败时异常传播导致RocketMQ重试，可能重复投递 | 重复执行（CAS防护但浪费资源） |
| T5 | P1 | OverflowRetryConsumer.java:143 | DLQ写入失败时rethrow→无限重试循环 | DB中断期间加剧系统不稳定 |
| T6 | P1 | ExecutionWatchdog.java:86-96 | scanZombieCtx先释放Redis dedup再更新DB，顺序颠倒=重复执行 | dedup已释放但DB未更新，watchdog再次释放 |
| T7 | P1 | CanvasSchedulerService.java:322-330 | parseTriggerTime无时区感知，服务器时区≠业务时区=触发时间偏移 | 全球部署时活动提前/延后8小时 |
| T8 | P1 | CanvasExecutionReplayRateLimiter.java:160-181 | Redis路径异常无fallback到本地限速，synchronized在Redis路径不必要 | Redis故障=重放请求直接崩溃 |

### 版本/发布/灰度子系统（4项P0 + 5项P1）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| V1 | P0 | CanvasTransactionService.java:160-166 | nextVersionNumber()读后写无锁，并发发布=版本号碰撞 | 版本记录损坏 |
| V2 | P0 | CanvasOpsService.java:126-146 | startCanary()无分布式锁，并发灰度=重复版本+状态不一致 | 灰度功能不可靠 |
| V3 | P0 | CanvasOpsService.java:189-198 | rollback()无锁，并发回滚=画布指向错误版本 | 运行中执行引用错误版本 |
| V4 | P0 | CanvasDO.java | 无tenantId字段，所有画布操作无租户隔离 | 跨租户数据访问漏洞 |
| V5 | P1 | CanvasOpsService.java:152-162 | promoteCanary()不更新触发路由/调度/缓存 | 灰度转正后新触发配置不生效 |
| V6 | P1 | CanvasVersionCleanupJob.java:60-88 | 清理不检查canaryVersionId，可能清除活跃灰度版本graphJson | 灰度执行崩溃 |
| V7 | P1 | CanvasOpsService.java:189-198 | 回滚仅支持上一版本，且publish()不设置previousVersionId | 常规发布后回滚永远失败 |
| V8 | P1 | CanvasOpsService.java:91-105 | Kill Switch: Pub/Sub不保证投递、Caffeine L1缓存延迟、GRACEFUL不取消运行中执行 | Kill后部分执行继续 |
| V9 | P1 | CanvasService.java:305-351 | 状态机转换无验证（offline不检查PUBLISHED状态、archive允许从任意状态） | 无效状态转换 |

### Disruptor/通道子系统（3项P0 + 5项P1）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| X1 | P0 | CanvasDisruptorService.java:276-279 | shutdown()不等待Reactor链完成，.subscribe()让Disruptor认为事件已消费 | 优雅关闭=丢失所有在途执行 |
| X2 | P0 | CanvasDisruptorService.java:140 | event.reset()在finally立即清空，.subscribe()错误回调读到null字段 | 所有Disruptor错误日志canvasId=null userId=null |
| X3 | P0 | CanvasDisruptorService.java:134-142 | fire-and-forget .subscribe()绕过Disruptor背压，消费者过快=环形缓冲区瞬间排空 | 突发溢出到RocketMQ，Redis/MQ过载 |
| X4 | P1 | CanvasDisruptorService.java:256-272 | publishRequest不清除上次canvas事件的残留字段 | 维护陷阱：新增request字段读取=脏数据 |
| X5 | P1 | InFlightExecutionRegistry.java:146 | 允许入场结果硬编码activeCount=0 | 监控看板显示错误并发利用率 |
| X6 | P1 | InFlightExecutionRegistry.java:163 | deregister本地map缺失时fallback到STANDARD通道 | HEAVY/LIGHT执行泄漏在错误通道ZSET |
| X7 | P1 | CanvasMetrics.java:42 | canvasId作为Micrometer标签=无界基数，OOM风险 | 监控系统内存泄漏 |
| X8 | P1 | ExecutionLaneResolver.java:31 | "MANUAL_APPROVAL_RESUME"字符串字面量可能不匹配运行时TriggerType | 审批恢复错误分类为STANDARD通道 |
| X9 | P1 | CanvasDisruptorService.java:110 | WorkerPool无每画布排序保证 | 同画布不同用户触发可能乱序执行 |

### 前端画布编辑器（2项P0 + 6项P1 + 2项P2）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| F1 | P0 | canvas-editor/index.tsx:283-317 | undo/redo快照捕获闭包中的nodes/edges，快速操作=快照过期 | undo恢复错误状态，用户丢失工作 |
| F2 | P0 | canvas-editor/index.tsx:500-507 | auto-save useEffect无依赖数组=每次渲染重置3秒计时器 | 编辑期间auto-save永不触发，离开丢数据 |
| F3 | P1 | canvas-editor/index.tsx:650-699 | 键盘快捷键effect无依赖数组+闭包捕获clipboard | 粘贴后立即Ctrl+V=粘贴旧内容 |
| F4 | P1 | canvas-editor/index.tsx:680-694 | 拷贝粘贴浅复制bizConfig，深层嵌套对象共享引用 | 编辑粘贴节点深层属性=静默修改原始节点 |
| F5 | P1 | canvas-editor/index.tsx:1443-1458 | deleteEdgeById在setEdges回调中嵌套setNodes，快速删除=孤儿引用 | 快速删线后bizConfig残留指向已删目标 |
| F6 | P1 | canvas-editor/index.tsx:917-946 | onNodesChangeWrapped删除和非删除变更同批时丢弃非删除变更 | 拖拽节点+删节点同时发生=拖拽回弹 |
| F7 | P1 | canvas-editor/index.tsx:1154-1189 | handleSaveCanvasName不传editVersion→并发覆盖+后续409 | 命名保存绕过乐观锁 |
| F8 | P1 | outletRouting.ts:213-268 | START/DIRECT_CALL多分支指向同目标=重复edgeId | 第二条边被静默丢弃 |
| F9 | P2 | config-panel/index.tsx:64-70 | 模块级Map缓存永不驱逐=长会话内存泄漏 | 编辑器长时间打开变慢 |
| F10 | P2 | canvas-editor/index.tsx:139-141 | buildSaveGraphJson的_placeholder过滤器是唯一防线但无类型级强制 | 脆弱防御：误传displayNodes=占位符泄漏到后端 |

---

## 更新后优先修复路线图

### 第一周：安全止血

1. **S1-S3**: 给permitAll端点添加report-secret校验或API Key认证
2. **S4**: CORS添加启动校验，非dev环境拒绝`*`
3. **S5**: 删除token的console.log
4. **P1-5**: defaultIfEmpty("system") → switchIfEmpty(Mono.error(UNAUTHORIZED))
5. **P1-6**: 给所有@RequestBody添加@Valid + DTO校验注解
6. **V4**: 画布操作添加租户隔离（tenant_id字段+过滤）

### 第二周：并发安全

7. **R1-R2**: CircuitBreaker状态转换加synchronized
8. **R3**: ExecutionContext.callStack→CopyOnWriteArrayList, triggerPayload→ConcurrentHashMap
9. **R4**: TraceWriteBuffer pending计数器重构
10. **R5**: ReachDeliveryService幂等改为INSERT ON DUPLICATE KEY + 唯一约束
11. **V1-V3**: 版本号生成/灰度/回滚添加分布式锁
12. **T1**: 审批超时上下文key加入executionId

### 第三周：Reactor合规 + Disruptor

13. **B1-B4**: 4个NodeHandler阻塞调用包装boundedElastic
14. **P1-16**: Mono.delay Disposable存入ExecutionContext
15. **X1-X2**: Disruptor WorkHandler提取event字段到局部变量+shutdown等待在途完成
16. **X3**: Disruptor消费者添加背压（.block()或限速器）
17. **T3**: CanvasSchedulerService WebClient添加.timeout()
18. **T2**: 配额回滚改用Lua脚本原子化

### 第四周：数据正确性 + 前端

19. **D1**: AudienceBitmapStore改用murmur3_128或用户-位图索引表
20. **D2**: WaitResumeService添加isResume标志跳过cooldown/配额
21. **D3**: CanvasStatsController改用canvas_execution_stats表聚合
22. **P1-26**: StatsMapper VALUES()替换为直接表达式
23. **F1-F2**: undo/redo改用ref捕获最新状态+auto-save添加依赖数组
24. **F4**: 拷贝粘贴bizConfig深克隆(structuredClone)
25. **V5**: promoteCanary添加触发路由/调度/缓存更新

### 第五周：补全

26. **V6**: 版本清理排除canaryVersionId
27. **V7**: publish()设置previousVersionId支持回滚
28. **V8**: Kill Switch增强（保证投递+L1缓存立即失效+可选FORCE取消运行中执行）
29. **X7**: CanvasMetrics移除canvasId高基数标签
30. **F6-F8**: 前端onNodesChangeWrapped+edgeId去重+handleSaveCanvasName传editVersion
31. **G1**: TaskScheduler Bean添加@Primary或合并为单一配置
32. **G2**: SecurityConfig添加安全头(X-Frame-Options/HSTS/CSP/X-Content-Type-Options)
33. **G3**: ThreadPoolTaskScheduler配置waitForTasksToCompleteOnShutdown+awaitTerminationSeconds
34. **G6**: SnowflakeConfig workerId改用环境变量或K8s pod ordinal
35. **N1-N3**: WebSocket closeSocket竞态修复+onerror/onclose去重+添加最大重试次数
36. **J3/G7**: WebSocket端点添加ticket验证或认证要求
37. **Q1**: HomeOverviewController添加WHERE status=PUBLISHED条件
38. **G4**: WebClient ConnectionProvider添加maxIdleTime/maxLifeTime/evictInBackground
39. **M1-M3**: DagEngine DLQ/超时/继续执行所有.subscribe()添加错误handler+存储Disposable
40. **M9**: 超时处理添加取消运行中节点执行的逻辑
41. **F1-F1**: 30+表添加tenant_id列(V78后续迁移)
42. **F2**: 关键关系添加FK+ON DELETE CASCADE
43. **F3**: 关键列添加NOT NULL约束
44. **O1-O2**: 所有.subscribe()添加错误handler+所有catch(Exception ignored)添加log.warn
45. **O3**: 添加trace ID传播(MDC+Reactor Context)
46. **O5**: HealthIndicator检查RocketMQ+外部服务+Disruptor状态
47. **T1**: 6个关键类添加单元测试(CircuitBreakerRegistry/TriggerPreCheck/CanvasOps/WeightedChoice/MarketingPolicy/ReachDelivery)

---

### 认证/JWT子系统（3项P0 + 7项P1 + 4项P2）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| J1 | P0 | SecurityConfig.java:70 | `/ops/**` permitAll，OpsController暴露缓存失效/模板管理/待审列表 | 无认证缓存失效=DoS武器，数据泄露（与S2重复确认） |
| J2 | P0 | SecurityConfig.java:63-66 | `/canvas/execute/direct/*`和`/canvas/trigger/behavior`完全无认证 | 外部攻击者可触发画布执行，造成真实业务副作用（与S1重复确认） |
| J3 | P0 | SecurityConfig.java:68 | `/canvas/ws/notifications` permitAll，WebSocket端点无安全层认证 | WebSocket连接劫持/资源耗尽攻击 |
| J4 | P1 | JwtUtil.java:57-71 | 无Refresh Token机制，24小时强制重登录 | 用户体验差，无法实现"记住我" |
| J5 | P1 | SysUserService.java:103-111 | 密码修改后不失效已有JWT Token | 密码泄露后攻击者Token仍有效直到过期 |
| J6 | P1 | JwtAuthFilter.java:74-95 | tenantId从JWT claims复制而非从数据库刷新，用户换租户后旧Token仍带旧tenantId | 跨租户数据访问 |
| J7 | P1 | AuthController.java:93-102 | 登录限流increment+expire非原子（与P1-2重复确认） | Redis故障=僵尸key |
| J8 | P1 | WebConfig.java:36-45 | CORS `addAllowedOriginPattern("*")` + `allowCredentials=true`（与S4重复确认） | 任意网站可发带凭证跨域请求 |
| J9 | P1 | SysUserService.java:136-140 | 密码强度仅检查长度≥6，无复杂度/黑名单/长度上限 | 弱密码+长密码DoS |
| J10 | P1 | AuthController.java:136-144 | Token黑名单哈希截断为16字节(128bit)，无jti claim | 理论碰撞风险，无法选择性撤销Token |
| J11 | P2 | JwtUtil.java | JWT无jti(JWT ID) claim，无法按Token ID追踪/撤销 | Token生命周期管理受限 |
| J12 | P2 | AuthController.java:127-129 | logout catch(Exception ignored)吞掉所有异常 | 安全问题无监控，攻击者可探测 |
| J13 | P2 | LoginReq.java | DTO无@NotBlank/@Size校验 | 空值/超长输入直达认证逻辑 |
| J14 | P2 | application.yml:55 | report-secret默认值`canvas-event-report-secret-2026!!` | 未配置环境变量=已知密钥可伪造事件 |

### MyBatis Mapper/SQL子系统（0项P0 + 2项P1 + 3项P2）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| Q1 | P1 | HomeOverviewController.java:65 | `selectList(null)`全表扫描后在Java过滤PUBLISHED状态 | 高频API全表扫描，内存溢出 |
| Q2 | P1 | CanvasVersionCleanupJob.java:44 | `selectList(null)`每日3AM全表扫描所有画布 | 数据量增长后锁表+内存压力 |
| Q3 | P2 | AudienceSchedulerService.java:53 | 启动时`selectList(null)`加载全量受众定义 | 启动变慢 |
| Q4 | P2 | CanvasExecutionTraceMapper.xml:20-38 | Funnel查询JOIN+GROUP BY缺复合索引 | 大画布查询慢 |
| Q5 | P2 | CanvasVersionCleanupJob.java:62-80 | 逐条UPDATE清理graphJson，应批量UPDATE | 100画布×20版本=2000+条UPDATE |

**正面发现**: 全部SQL使用`#{}`参数化查询，无`${}`注入风险；LIMIT使用规范；`.apply()`安全；ON DUPLICATE KEY UPDATE正确。

### Spring @Configuration子系统（1项P0 + 7项P1）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| G1 | P0 | SchedulerConfig + ScheduleRegistrarConfig | 两个TaskScheduler Bean定义缺@Primary，bean名不同致@ConditionalOnMissingBean失效 | @Scheduled方法使用未知调度器 |
| G2 | P1 | SecurityConfig.java | 无安全头配置(X-Frame-Options/X-Content-Type-Options/HSTS/CSP) | Clickjacking/MIME嗅探/XSS |
| G3 | P1 | SchedulerConfig + ScheduleRegistrarConfig | ThreadPoolTaskScheduler未配置waitForTasksToCompleteOnShutdown | 关闭时调度任务被截断，数据不一致 |
| G4 | P1 | WebClientConfig.java:36-40 | ConnectionProvider无maxIdleTime/maxLifeTime/evictInBackground | 连接池累积僵尸连接 |
| G5 | P1 | ConditionEvaluator.java:19 | ObjectMapper在Spring上下文外new创建，缺全局Jackson配置 | 序列化行为不一致+潜在反序列化漏洞 |
| G6 | P1 | SnowflakeConfig.java:27-30 | workerId从IP推导，容器环境多实例可能碰撞=ID重复 | 主键冲突+数据损坏 |
| G7 | P1 | SecurityConfig.java:68 | WebSocket端点permitAll（与J3重复确认） | 连接劫持 |
| G8 | P1 | application.yml:165-166 | Actuator health show-details: always暴露DB/Redis详情 | 信息泄露 |

### 前端通知/WebSocket子系统（3项P0 + 5项P1）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| N1 | P0 | NotificationContext.tsx:131-136,197-202 | closeSocket()触发onclose→scheduleReconnect，用户切换时两个WebSocket竞态 | 旧socket的onclose覆盖新socket引用，连接泄漏 |
| N2 | P0 | NotificationContext.tsx:193-201 | onerror+onclose双重调度fallbackPolling+reconnect | 连接不稳定时双倍服务器负载(HTTP轮询+WS重连) |
| N3 | P0 | NotificationContext.tsx:147-157 | WebSocket重连无最大重试次数，服务端永久故障=客户端无限重试 | 千级浏览器tab=每30秒一次HTTP POST+WS连接 |
| N4 | P1 | NotificationContext.tsx:172-208 | 无客户端PING心跳，网络切换/休眠后连接僵尸 | UI显示connected但无通知，需刷新页面 |
| N5 | P1 | NotificationContext.tsx:59-67 | refresh()未校验listRes.data/countRes.data是否undefined | 后端返回缺data字段=整个通知状态崩溃 |
| N6 | P1 | notificationRealtime.ts:25-37 | 去重仅用notificationId，忽略dedupKey字段 | 重复通知显示 |
| N7 | P1 | NotificationContext.tsx:172-208 | connectRealtime() await后未重检stoppedRef | 用户切换后旧handler更新新用户状态 |
| N8 | P1 | NotificationContext.tsx:197-201 | onclose置wsRef.current=null不检查是否为当前socket | 新连接被旧onclose孤儿化 |

---

### DagEngine行级审查（4项P0 + 9项P1，深化已有发现）

| # | 严重度 | 文件:行 | 问题 | 影响 |
|---|--------|---------|------|------|
| M1 | P0 | DagEngine.java:560-562 | DLQ写入fire-and-forget .subscribe()无Disposable（与P1-16/P2-5深化） | 高失败率下DLQ订阅无限累积=内存泄漏 |
| M2 | P0 | DagEngine.java:636,725,776,806 | 4处Mono.delay超时handler .subscribe()无错误handler+无Disposable | 超时调度失败=节点永远卡WAITING |
| M3 | P0 | DagEngine.java:843-856 | 超时继续执行fire-and-forget .subscribe()无Disposable | 继续执行挂起=无法检测/取消 |
| M4 | P0 | DagEngine.java:636-638 | Mono.delay .subscribe()仅有success回调，无error回调 | 调度器拒绝=超时任务静默丢失 |
| M5 | P1 | DagEngine.java:617-622 | LOGIC_RELATION/HUB竞态依赖超时恢复(600s)，无主动检测 | 并行分支卡死最长600秒 |
| M6 | P1 | DagEngine.java:58-61 | callStack(ArrayList)非线程安全（与R3深化确认） | 子画布调用并发修改异常 |
| M7 | P1 | DagEngine.java:330-338,476 | NodeGate锁释放所有权混淆：executeHandlerWithRepeat释放后onErrorResume可能再次释放 | 锁语义不清，未来修改可能引入死锁 |
| M8 | P1 | DagEngine.java:811-857 | 超时只标记状态但不取消运行中的节点执行 | 超时节点仍完成并写入context覆盖超时状态 |
| M9 | P1 | DagEngine.java:128-143 | putNodeOutput仅log警告不阻止写入，context无限增长 | 大DAG=OOM |
| M10 | P1 | DagEngine.java:1102-1145 | tryPrioritySequentially递归组装Mono，50+分支=栈溢出风险（与P2-8深化确认） | 大量优先级分支=栈溢出 |
| M11 | P1 | DagEngine.java:288,911 | handlerRegistry.get无null检查（与P1-31深化确认） | 未注册节点类型=NPE崩溃 |
| M12 | P1 | DagEngine.java:732-738 | Hub超时检查非原子(检查+状态更新间隔) + 定时器仍pending | Hub被标记失败后定时器仍触发 |
| M13 | P1 | DagEngine.java:636-638 | NodeGate锁错误路径双重释放(executing.set(false) x2) | 锁语义混淆 |

### Flyway迁移链审查（5项P0 + 7项P1 + 6项P2）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| F1 | P0 | V1-V75(30+表) | V78添加tenant_id到核心表但30+子表缺tenant_id | 多租户部署=跨租户数据泄露 |
| F2 | P0 | V1,V3,V4,V5,V46,V57,V62 | 所有canvas/execution关系缺FK+ON DELETE CASCADE | 删除画布后大量孤儿记录 |
| F3 | P0 | V1,V3,V4,V5,V46 | execution.status/user_id/trigger_type等关键列允许NULL | 数据完整性问题+NPE |
| F4 | P0 | V1,V3,V70 | execution_trace/execution_request缺复合索引 | 生产大表查询慢 |
| F5 | P0 | V4,V74 | error_msg VARCHAR(500)/tag_value VARCHAR(1000)可能截断长堆栈/JSON值 | 关键调试信息丢失 |
| F6 | P1 | V1-V75(大量表) | created_at/updated_at无DEFAULT CURRENT_TIMESTAMP | 代码忘记设置=INSERT失败或NULL |
| F7 | P1 | V1,V3,V4,V5(大量列) | ENUM-like VARCHAR列无CHECK约束 | 无效值插入导致应用错误 |
| F8 | P1 | V2,V9,V26,V27,V34等(15个迁移) | INSERT到node_type_registry无ON DUPLICATE KEY UPDATE | flyway repair后重跑=失败 |
| F9 | P1 | V1,V3,V6 | execution/trace/stats/event_log等高增长表无分区策略 | 表膨胀后查询变慢+维护困难 |
| F10 | P1 | V14 | V14是空操作(SELECT 1)，schema状态模糊 | 列是否存在不确定 |
| F11 | P1 | V6 | V6分区设计全部deferred(SELECT 1) | 开发和生产schema不一致 |
| F12 | P1 | V78 | tenant_id迁移UPDATE依赖subquery，tenant表空=迁移失败 | 空数据库首次启动=迁移崩溃 |
| F13 | P2 | 全部索引 | idx_/uk_前缀不一致 | 可读性差 |
| F14 | P2 | V3:21 | BCrypt密码哈希在VCS中 | 安全风险 |
| F15 | P2 | V41,V43,V71 | JDBC URL含硬编码root/root凭证 | 版本控制中泄露凭证 |
| F16 | P2 | V1-V62(大量子表) | 缺ON DELETE CASCADE | 孤儿数据累积 |
| F17 | P2 | V3-V75 | VARCHAR命名不一致 | 维护困难 |
| F18 | P2 | 全量表 | TIMESTAMP精度不一致(部分DATETIME部分TIMESTAMP) | 时区问题 |

### 测试覆盖率审查（6项P1 + 6项P2 + 2项P3）

| # | 严重度 | 类 | 缺失测试场景 | 影响 |
|---|--------|----|-------------|------|
| T1 | P1 | CircuitBreakerRegistry | 完全无测试：TOCTOU竞态/状态机转换/HALF_OPEN探针限制 | 熔断器不开或永不恢复=长时间停机 |
| T2 | P1 | TriggerPreCheckService | 完全无测试：配额竞态/Lua原子性/rollback部分失败/SCAN分页 | 配额绕过/Redis key泄漏 |
| T3 | P1 | CanvasOpsService | 完全无测试：FORCE kill事务/canary推广竞态/rollback版本交换 | 灰度/回滚功能不可靠 |
| T4 | P1 | WeightedChoice | 完全无测试：分布均匀性/边界(空列表/0权重) | A/B实验分布严重偏差 |
| T5 | P1 | MarketingPolicyService | 完全无测试：consent/suppression/frequency原子性 | 合规违规/疲劳控制绕过 |
| T6 | P1 | ReachDeliveryService | 完全无测试：幂等性/外部超时/记录持久化 | 重复消息发送 |
| T7 | P2 | DagEngine | 不完整：缺循环检测/并行执行/错误传播/重试backoff/DLQ/Gate竞态/超时恢复 | 核心路径无保障 |
| T8 | P2 | InFlightExecutionRegistry | 仅lane拒绝测试：缺Lua原子性/并发acquire/release/Kill Switch/crash恢复 | slot泄漏/过准入 |
| T9 | P2 | WaitResumeService | 仅event filter测试：缺幂等性/并发resume/阈值等待 | 重复执行/wait永不完成 |
| T10 | P2 | CanvasDisruptorService | 仅overflow测试：缺shutdown/消费者失败/背压 | 关闭丢事件/ring buffer卡死 |
| T11 | P2 | 50+NodeHandler | 50个handler无测试(含全部Send渠道/Policy/FlowControl) | 处理器bug=路由/数据错误 |
| T12 | P2 | SecurityConfig | 仅role常量测试：缺路由授权/JWT filter/认证入口点 | 未授权访问 |
| T13 | P3 | 前端组件 | 15+核心组件无测试(CanvasNode/AuthContext/全部page) | UI bug靠手工发现 |
| T14 | P3 | CanvasService | 无测试：publish/offline/archive/版本管理 | 管理操作无保障 |

### 可观测性/监控审查（5项P0 + 6项P1）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| O1 | P0 | DagEngine:560,636,725,776,806等 | .subscribe()无错误handler，4处+DLQ+3处超时+2处handler=静默丢错 | 生产调试不可能 |
| O2 | P0 | DagEngine:1237,MarketingPolicy:119,ConditionEvaluator:44,ScoringHandler:102等 | catch(Exception ignored)吞掉7处关键异常 | 时区/规则/评分/trace错误完全隐藏 |
| O3 | P0 | 全量引擎代码 | 无trace ID传播机制(MDC+Reactor Context)，GlobalExceptionHandler声明traceId但不填充 | 无法跨节点/跨画布/跨MQ关联日志 |
| O4 | P0 | CanvasMetrics.java | 8项关键业务指标缺失：投递成功率/投递延迟/画布完成率(定义但从未调用)/节点失败率/受众解析/CDP/配额/熔断器 | 无法设置告警阈值，oncall无法被page |
| O5 | P0 | application.yml:159-169 | Health endpoint仅检查DataSource+Redis基本ping，缺RocketMQ/外部服务/Disruptor的HealthIndicator | 应用报healthy但MQ/外部服务/Disruptor溢出时全部静默失败 |
| O6 | P1 | DagEngine:540+DlqController | DLQ计数器存在但无告警阈值+无dashboard | 失败累积千条无人知 |
| O7 | P1 | 全量引擎 | 日志缺executionId/canvasId/userId等结构化上下文(仅部分日志包含) | 无法grep特定执行的完整旅程 |
| O8 | P1 | 全量代码 | 无SLO/SLI定义(错误预算/延迟SLO/可用性SLO/燃烧率告警) | 无法衡量平台是否满足业务需求 |
| O9 | P1 | InFlightExecutionRegistry:109-112 | Redis失败时仅ERROR日志，无metric(无"redis_unavailable_rejections"计数器) | Redis故障=平台完全停摆但无自动告警 |
| O10 | P1 | ExecutionWatchdog:172-173 | watchdog恢复失败仅log.error，无metric+无告警 | zombie执行无限累积 |
| O11 | P1 | CanvasDisruptorService:233 | overflow计数器存在但无告警阈值+无自动扩缩触发 | ring buffer溢出丢触发直到客户投诉 |

### Handler深度审查（12项P0 + 9项P1）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| H1 | P0 | PointsOperationHandler.java:51-78 | 阻塞Reactor线程(selectOne+insert未包装boundedElastic) | 阻塞Netty事件循环 |
| H2 | P0 | TagOperationHandler.java:54-73 | 阻塞Reactor线程(delete+update+insert未包装) | 同上 |
| H3 | P0 | GoalCheckHandler.java:129-141 | 阻塞Reactor线程(selectCount未包装)+IllegalArgumentException未捕获 | 阻塞+异常传播崩溃整个DAG |
| H4 | P0 | TrackEventHandler.java:56-75 | 阻塞Reactor线程(insert未包装)+IllegalArgumentException未捕获 | 同上 |
| H5 | P0 | CreateTaskHandler.java:50-68 | 阻塞Reactor线程(insert未包装) | 同上 |
| H6 | P0 | ManualApprovalHandler.java:60-125 | 阻塞Reactor线程(insert+deleteById未包装) | 同上 |
| H7 | P0 | SubFlowRefHandler.java:89-153 | 阻塞Reactor线程(selectById+selectList未包装)+JSON解析CPU密集 | 同上 |
| H8 | P0 | CanvasTriggerHandler.java:72-137 | 阻塞Reactor线程(selectById未包装) | 同上 |
| H9 | P0 | ChannelAvailabilityHandler.java:49-61 | 阻塞Reactor线程(via policyService.channelAvailable→DB调用) | 同上 |
| H10 | P0 | QuietHoursHandler.java:49-69 | 阻塞Reactor线程(via policyService.quietHoursAllowed→DB调用) | 同上 |
| H11 | P0 | TransferJourneyHandler.java:65-74 | fire-and-forget .subscribe()无错误handler，触发失败静默丢弃 | 用户旅程转移在纸面上成功但目标画布从未执行 |
| H12 | P0 | CanvasTriggerHandler.java:121 | ASYNC模式fire-and-forget .subscribe()无错误handler | 子画布执行失败被静默丢弃 |
| H13 | P1 | ScoringHandler.java:86 | IN操作符逻辑反转：expected.contains(actual)做子串匹配而非集合包含 | 评分IN规则假阳性，用户被分到错误营销段 |
| H14 | P1 | CouponHandler.java:49 | couponTypeKey未校验null/blank直接发HTTP | null=发券请求含null参数=外部服务异常 |
| H15 | P1 | CouponHandler.java:63-75 | 外部服务响应Map字段无null检查，"发券失败: null" | 无帮助的错误消息 |
| H16 | P1 | WeightedChoice.java:36 | 权重总和使用int，大权重溢出变负数=永远选第一项（与P2-13深化） | A/B实验分布严重偏差 |
| H17 | P1 | AbSplitHandler.java:50 | Math.abs(Integer.MIN_VALUE)返回负数，取模=ArrayIndexOutOfBounds | 特定用户+实验组合=画布执行崩溃 |
| H18 | P1 | TagOperationHandler.java:85-103 | upsertTag check-then-act竞态，并行分支=重复tag记录 | 并行tag操作=重复/SQL异常 |
| H19 | P1 | PointsOperationHandler.java:54-75 | 积分幂等selectOne+insert非原子，并发=双倍积分 | 财务影响 |
| H20 | P1 | MqTriggerHandler.java:67-79 | resolveTopic()阻塞DB调用无自保护(依赖调用方恰好在boundedElastic) | 脆弱，重构=引入阻塞bug |
| H21 | P1 | TaggerOfflineHandler.java:46-52 | tagCodeKey未校验null/blank，null作为查询参数发送 | 静默错误行为 |

**阻塞Reactor线程统计**: 已知4个(B1-B4) + 本次新增10个(H1-H10) = **14个Handler阻塞Netty事件循环**，这是系统性问题。

### application.yml配置审查（5项P0 + 14项P1）

| # | 严重度 | 配置项 | 问题 | 影响 |
|---|--------|--------|------|------|
| Y1 | P0 | spring.datasource:7-9 | DB凭证硬编码(root/root)+useSSL=false+allowPublicKeyRetrieval=true | 凭证泄露+明文传输+MITM攻击 |
| Y2 | P0 | canvas.cors:56-59 | allowed-origins默认"*"+无application-prod.yml覆盖 | 任意网站跨域请求 |
| Y3 | P0 | 无application-prod.yml | 所有危险默认值(CORS*/硬编码凭证/localhost URL/调试日志/Swagger)无生产覆盖 | 部署到生产=全部安全漏洞生效 |
| Y4 | P0 | spring.data.redis:25-29 | Redis无密码+无生产覆盖 | 无保护Redis=读写缓存/操纵执行状态 |
| Y5 | P0 | canvas.events.report-secret:55 | 默认值`canvas-event-report-secret-2026!!`可预测 | 未配置环境变量=伪造事件报告 |
| Y6 | P1 | springdoc:143-148 | Swagger UI默认enabled+无生产覆盖 | 暴露全部API端点和schema |
| Y7 | P1 | management.endpoint.health:159-166 | show-details:always+无生产覆盖 | 暴露DB/Redis连接详情 |
| Y8 | P1 | canvas.integration:137-141 | 全部集成URL指向localhost:8099 WireMock | 生产环境外部服务调用全部失败 |
| Y9 | P1 | server.shutdown | 缺graceful shutdown配置 | 部署时在途请求/Disruptor事件/MQ消息丢失 |
| Y10 | P1 | 7个Handler | 各自new WebClient.builder()绕过连接池 | 每个独立连接池，绕过配置的限制(500max) |
| Y11 | P1 | hikari:11-18 | maximum-pool-size=33 vs max-concurrency=3000 | 高负载下DB连接等待超时 |
| Y12 | P1 | lettuce.pool:30-35 | max-active=64, max-wait=100ms vs 3000并发 | PoolExhaustedException 100ms即超时 |
| Y13 | P1 | SecurityConfig:62-66 | permitAll端点无限流 | DoS攻击可淹没Disruptor ring buffer |
| Y14 | P1 | canvas.groovy:127-129 | timeout-ms=5000对于高并发系统过长 | 长脚本累积耗尽虚拟线程 |
| Y15 | P1 | canvas.circuit-breaker:131-135 | failure-threshold=5过低 | 瞬态网络问题=熔断器打开30秒 |
| Y16 | P1 | MqTriggerConsumer:43 | consumeThreadNumber=20硬编码忽略yml配置 | 配置属性定义但未使用 |
| Y17 | P1 | flyway:19-23 | 缺clean-disabled:true | 误操作flyway clean=全库数据丢失 |
| Y18 | P1 | logback-spring.xml:36-41 | !prod profile启用DEBUG日志 | 未正确设置profile=生产DEBUG日志泄露敏感数据 |
| Y19 | P1 | server.ssl | 无HTTPS/SSL配置 | JWT/用户数据/事件payload明文传输 |

### 前端路由/状态/API审查（4项P0 + 8项P1）

| # | 严重度 | 文件 | 问题 | 影响 |
|---|--------|------|------|------|
| W1 | P0 | App.tsx全文件 | 零ErrorBoundary，任何组件异常=整个应用白屏 | 运行时JS错误=全站不可用，无恢复路径 |
| W2 | P0 | api.ts:35-43 | 401拦截器硬跳转window.location.href='/login'，不尝试refresh token | JWT过期=立即丢失所有未保存数据(画布编辑/表单) |
| W3 | P0 | AuthContext.tsx:98 + roles.ts:11-13 | isAdmin=canManageUsers()包含TENANT_ADMIN→租户管理员可访问超级管理员路由 | 权限提升：租户管理员可管理用户/修改系统配置 |
| W4 | P0 | canvas-list/index.tsx:127-148 | Modal.confirm中直接插值服务端canvas name，未经sanitization | 多租户平台XSS风险 |
| W5 | P1 | canvas-editor/index.tsx:500-507 | auto-save useEffect无依赖数组(与F2/P1-37深化确认) | 编辑期间auto-save永不触发 |
| W6 | P1 | canvas-editor/index.tsx:650-699 | 键盘快捷键effect无依赖数组(与F3/P1-38深化确认) | 每次渲染重注册+闭包过期 |
| W7 | P1 | canvas-editor/index.tsx:2063-2065 | 画布详情fetch无.catch()+无stale-request guard | 网络错误=无限spinner，无法恢复 |
| W8 | P1 | 多个页面(admin/tenant-admin/cdp-users等) | useEffect数据获取无错误处理 | API失败=空页面/无限spinner/未处理Promise拒绝 |
| W9 | P1 | AuthContext.tsx:73 | console.log泄露token前20字符(与S5深化确认) | 生产环境token信息泄露 |
| W10 | P1 | cdp-users/index.tsx:55-73 | 轮询effect依赖operations，每次数据更新重建interval | 轮询间隔不稳定，任务看起来"卡住" |
| W11 | P1 | canvas-list/index.tsx:66 | fetchList try/finally无catch=未处理Promise拒绝 | 错误=空表格无提示 |
| W12 | P1 | api.ts:20-44 | 无CSRF保护(如果后端同时接受cookie认证) | 跨站请求伪造风险 |

---

## 更新后子系统分布表
