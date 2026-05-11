# 营销画布 — 技术蓝图（完整版）

> 基于 `marketing-canvas-design.md` 全文通读后生成（6712 行，27 章 + 5 附录）
> 最后更新：2026-05-11
> 状态：已确认，可开发

---

## 项目技术栈

| 维度 | 决策 |
|------|------|
| Java | **Java 21**（含虚拟线程 Project Loom） |
| 后端框架 | Spring Boot 3.x + WebFlux (Reactor) |
| ORM | MyBatis-Plus 3.5.x，blocking 调用包在 `Schedulers.boundedElastic()` |
| 数据库 | MySQL 8.0 + Flyway 管理迁移 |
| 缓存 | Redis（上下文/路由表/分布式锁）+ Caffeine（JVM 本地缓存） |
| MQ | RocketMQ（触发消费 + 触达平台发送 + DLQ） |
| 前端 | React 18 + Vite + TypeScript + antd 5 |
| 画布编辑器 | **React Flow**（@xyflow/react）+ @dagrejs/dagre |
| 认证 | 自建 JWT + RBAC（ADMIN / OPERATOR） |
| 节点坐标 | 内嵌在 `graph_json` 每个节点的 `x/y` 字段 |
| Groovy 沙箱 | SecureASTCustomizer 白名单 + 执行超时 |
| 集成层节点 | 完整 Handler 实现，外部 URL 配置化 |
| 配置中心 | Nacos（动态推送执行参数、Groovy 白名单、用量限制等） |
| 可观测 | Micrometer + Prometheus + 全链路 traceId |
| API 文档 | springdoc-openapi（生产禁用） |

---

## 数据库表全集（含补充字段）

### 核心表

| 表 | 说明 | 状态 |
|----|------|------|
| `canvas` | 画布主表 | ✅ V1 已建，**需补充 12 个字段** |
| `canvas_version` | 版本快照 | ✅ V1 已建 |
| `canvas_execution` | 执行记录 | ✅ V1 已建 |
| `canvas_execution_trace` | 节点执行轨迹 | ✅ V1 已建 |
| `context_field` | 上下文字段注册表 | ✅ V1 已建 |
| `node_type_registry` | 节点类型注册（含 config_schema） | ✅ V1 已建 |
| `sys_user` | 用户表（JWT 登录） | 待建 V3 |

### canvas 表需补充的字段（V3 迁移追加）

```sql
ALTER TABLE canvas
  ADD COLUMN valid_start           DATETIME  COMMENT '活动开始时间，null=立即生效',
  ADD COLUMN valid_end             DATETIME  COMMENT '活动结束时间，null=永不过期',
  ADD COLUMN per_user_total_limit  INT       COMMENT '单用户总触发上限',
  ADD COLUMN per_user_daily_limit  INT       COMMENT '单用户每日触发上限',
  ADD COLUMN cooldown_seconds      INT       COMMENT '同用户两次触发最短间隔(秒)',
  ADD COLUMN max_total_executions  INT       COMMENT '全局总触发量上限',
  ADD COLUMN canary_version_id     BIGINT    COMMENT '灰度版本ID',
  ADD COLUMN canary_percent        INT       COMMENT '灰度流量比例 0~100',
  ADD COLUMN previous_version_id   BIGINT    COMMENT '上一个稳定版本（用于回滚）',
  ADD COLUMN edit_version          INT       NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';
```

### 补充表（V3 迁移新建）

| 表 | 说明 | 状态 |
|----|------|------|
| `canvas_audit_log` | 操作日志（已在设计文档 17.3 定义） | 待建 V3 |
| `canvas_user_quota` | 用户级执行限流（已在设计文档 15.1 定义） | 待建 V3 |
| `canvas_execution_stats` | 执行统计（已在设计文档 21.1 定义，含 p99、unique_users） | 待建 V3 |
| `canvas_node_funnel_stats` | 节点漏斗统计（设计文档 21.1） | 待建 V3 |
| `canvas_template` | 画布模板（设计文档 23.1） | 待建 V3 |

---

## 开发阶段划分（完整版）

---

### Phase 1 — 基础骨架 ✅ 已提交，需修正

**修正项：**
- [ ] `frontend/package.json`：换成 `@xyflow/react` + `@dagrejs/dagre`（当前用了 xflow 占位）
- [ ] V3 SQL 迁移：`sys_user` + `canvas` 补充字段 + 5 张补充表
- [ ] 后端补充 `springdoc-openapi` 依赖

---

### Phase 2 — JWT 认证 + 权限

**后端**
- [ ] `sys_user` 表 + BCrypt 密码加密
- [ ] `POST /auth/login`：验证密码，签发 JWT（payload 含 userId/role）
- [ ] `GET /auth/me`：返回当前用户信息
- [ ] `POST /auth/logout`（前端清 token，后端可加 Redis 黑名单）
- [ ] Spring Security WebFlux 配置：JWT 过滤器 + RBAC 路由保护
- [ ] 用户管理 API：`GET/POST/PUT /admin/users`，禁用用户
- [ ] Controller 层注入当前用户（从 SecurityContext 获取 operator）

**前端**
- [ ] 登录页（`/login`），表单校验
- [ ] localStorage 存 `canvas_token`
- [ ] axios 拦截器：请求带 `Authorization: Bearer xxx`，401 跳转登录
- [ ] React Context 存 `role`，用于按钮级权限控制（OPERATOR 隐藏发布/下线/Kill）
- [ ] 路由守卫：未登录重定向到 `/login`

---

### Phase 3 — 画布编辑器（React Flow 接入）

**后端**
- [ ] DAG 校验（Kahn 算法环检测）→ 发布校验（触发器节点存在、必填完整、同层字段冲突检测）
- [ ] 发布流程：校验 → 版本快照 → Redis 触发路由注册 → 广播缓存失效（Redis Pub/Sub）
- [ ] `canvas_audit_log` 写入（CREATED/EDITED/PUBLISHED/OFFLINE 事件）
- [ ] 并发编辑保护：`PUT /canvas/{id}` 携带 `editVersion`，CAS 检查，冲突返回 409
- [ ] 历史版本列表 + 指定版本获取 API
- [ ] 版本对比 API：`GET /canvas/{id}/versions/{v1}/diff/{v2}`

**前端**
- [ ] React Flow 接入：`@xyflow/react` 初始化、`ReactFlowProvider`
- [ ] 自定义节点卡片组件（`CanvasNode`）：按 category 颜色，含 Handle 连接桩
- [ ] 左侧节点面板（按类别分组，从 `/meta/node-types` 加载，HTML5 draggable 拖拽）
- [ ] 拖拽入画布（`onDrop` + `screenToFlowPosition`）
- [ ] 右侧配置面板（点击节点后 → `GET /meta/node-types/{type}/schema` → antd Form 动态渲染）
- [ ] 自定义表单控件：`ConditionRuleList`、`ParamList`、`ValueInput`（CUSTOM/CONTEXT 双模）、`NodeSelector`
- [ ] `onConnect`：更新源节点 `bizConfig.nextNodeId`，同步 edges 状态
- [ ] 连线规则约束（`isValidConnection`：触发器无入边、直调返回无出边、禁止自环）
- [ ] 画布保存：getNodes()/getEdges() → 后端格式（x/y 坐标内嵌）→ `PUT /canvas/{id}`
- [ ] 画布加载：后端 JSON → React Flow Node[] + Edge[]（deriveEdgesFromNodes 推导边）
- [ ] 自动布局（Dagre）：顶部工具栏"整理画布"按钮
- [ ] 撤销/重做（快照 50 步，Ctrl+Z / Ctrl+Shift+Z）
- [ ] 发布前前端校验：必有触发器、必填字段、IF判断两侧分支
- [ ] 版本历史面板（列表 + 版本对比 diff 视图：节点新增/删除/修改颜色区分）
- [ ] 代码编辑器（Groovy 节点）：引入 CodeMirror 或 Monaco Editor

---

### Phase 4 — 运营管控功能

**后端**
- [ ] 灰度发布：`POST /canvas/{id}/canary?percent=N`，设置 `canary_version_id` + `canary_percent`
- [ ] 灰度全量：`POST /canvas/{id}/promote-canary`，升级 `published_version_id`
- [ ] 灰度回退：`POST /canvas/{id}/rollback-canary`，清除灰度字段
- [ ] 版本快速回滚：`POST /canvas/{id}/rollback`，交换 `published/previous_version_id`
- [ ] 干运行：`POST /canvas/{id}/dry-run`，dryRun=true 时 Handler 返回 Mock 响应
- [ ] 紧急 Kill：`POST /canvas/{id}/kill`（GRACEFUL / FORCE 模式）→ Redis Pub/Sub 广播
- [ ] 画布克隆：`POST /canvas/{id}/clone`，复制草稿 JSON，重置状态和用量限制
- [ ] 画布导出：`GET /canvas/{id}/export`，含元信息 JSON
- [ ] 画布导入：`POST /canvas/import`，检测依赖节点类型
- [ ] 发布审批流：`submit-review / approve / reject` API，风险规则触发（大额券、无上限、含 Groovy）
- [ ] `canvas_audit_log` 完整写入（所有操作事件）

**前端**
- [ ] 顶部工具栏补充：灰度发布、回滚、Kill 按钮（ADMIN 可见）
- [ ] 干运行面板：输入测试数据，查看节点执行轨迹（Mock 高亮）
- [ ] 版本历史面板新增：回滚按钮、灰度进度显示
- [ ] 审批状态显示（待审批状态徽标）

---

### Phase 5 — 执行引擎核心

**后端**
- [ ] `ExecutionContext` 模型（flatContext O(1)、nodeStatus、benefitGranted、userReached、callStack）
- [ ] `NodeHandler` 接口 + `@NodeHandler` 注解 + `HandlerRegistry` 自动注册
- [ ] `DagEngine`：DAG 解析（邻接表 + 反向邻接表）、本地内存缓存（含版本号 key）、Reactor 调度
- [ ] 单节点 6 阶段执行（配置解析→逻辑关系检查→幂等→CAS→Handler→触发下游）
- [ ] repeat 并发保护（`AtomicBoolean.compareAndSet`）
- [ ] 执行版本锁定（触发时快照 versionId，全程不变）
- [ ] 通用节点 Handler（全部完整实现）：
  - `IfConditionHandler`（CONTAINS IN语义 + BigDecimal数值比较）
  - `SelectorHandler`（多分支按序匹配，无命中走 else 或自然结束）
  - `LogicRelationHandler`（AND/OR，SKIPPED处理，立即失败逻辑）
  - `HubHandler`（等待所有上游完成，**timeout 超时强制 FAILED**，延迟任务调度）
  - `GroovyHandler`（**标准安全沙箱** SecureASTCustomizer，GroovyUtils 工具，对象池复用，输出 64KB 限制，预编译缓存）
  - `DelayHandler`（虚拟线程 sleep，不占 OS 线程）
  - `ApiCallHandler`（HTTP 调用，validateResult 校验，不可重试业务异常）
  - `PriorityHandler`（按序尝试，成功即止，全失败走 nextNodeId 或 FAILED）
  - `DirectCallHandler` / `DirectReturnHandler`（业务直调同步返回）
- [ ] 防资损判定（`benefitGranted`/`userReached`，全局标志位，随 ctx 持久化）
- [ ] SKIPPED 批量写入（执行结束时扫描未出现在 nodeStatuses 的节点）
- [ ] 熔断器（Circuit Breaker）：每个集成节点类型独立，Nacos 配置 failure-threshold/timeout-ms
- [ ] 节点级超时（`Mono.timeout()`）+ 全局超时 + Watchdog 30s 扫描兜底
- [ ] 指数退避重试（maxRetry=3，1s→2s→4s，可重试/不可重试分类）
- [ ] 死信队列：重试耗尽 → 发 RocketMQ DLQ Topic → 钉钉告警
- [ ] 死信重放 API：`POST /canvas/execution/replay`（skipSuccessNodes）
- [ ] 执行记录写入（异步 Ring Buffer + 批量刷盘，不占主链路）
- [ ] 结构化日志（traceId/canvasId/nodeId/event 等字段）

---

### Phase 6 — 触发机制

**后端**
- [ ] 触发前置检查：canvas status / 有效期 / 全局上限 / 用户每日限 / 用户总限 / 冷却期
- [ ] Redis 原子 INCR 扣减 + 超配回滚（防并发超发）
- [ ] dedup key 幂等去重（MQ 24h TTL / 行为 10min 时间窗口桶 / 直调 1h）
- [ ] RocketMQ 消费者 → Redis 路由查找 → 批量触发画布执行
- [ ] 灰度路由（触发时按 canary_percent 随机分流版本）
- [ ] 业务直调：`POST /canvas/execute/direct/{canvasId}` 同步返回
- [ ] 端内行为触发：`POST /canvas/trigger/behavior`
- [ ] 服务重启路由表全量重建（`@PostConstruct`）
- [ ] MQ ACK 策略：写入 dedup key 后立即 ACK，防止消息堆积
- [ ] 启动依赖检查：MySQL/Redis 不可用拒绝启动，MQ 不可用降级（仅直调可用）

---

### Phase 7 — 多阶段执行

**后端**
- [ ] 挂起机制：LOGIC_RELATION 条件未满足 → 序列化 ctx（含 benefitGranted）→ Redis TTL
- [ ] 恢复机制：第二次触发 → 反序列化 → 追加 payload → 继续执行
- [ ] resume-lock 分布式锁（原生 Redis SETNX + 心跳续期，UUID 替代 threadId）
- [ ] 僵尸 ctx 清理：`last_dedup_key` 字段 + Watchdog + 短 TTL 兜底（恢复场景 ~620s）
- [ ] 超长挂起（7天）期间 benefitGranted 防重

---

### Phase 8 — 集成层节点（完整实现）

所有 Handler 为完整 HTTP/MQ 调用，URL 配置化（`canvas.integration.*`）：

- [ ] `MqTriggerHandler`：消费 RocketMQ，校验 payload validateRules
- [ ] `CouponHandler`：HTTP 调券系统，idempotencyKey={executionId}:{nodeId}，设 benefitGranted
- [ ] `InAppNotifyHandler`：MQTT 推送，设 userReached
- [ ] `ReachPlatformHandler`：发 RocketMQ 至触达平台，设 userReached
- [ ] `TaggerOfflineHandler`：HTTP 调 Tagger，标签为空拦截
- [ ] `TaggerRealtimeHandler`：监听 Tagger 实时标签 MQ
- [ ] `AbSplitHandler`：Hash(userId:experimentKey) % 100 确定性分流
- [ ] `SendMqHandler`：RocketMQ 生产者，发消息至配置的 Topic
- [ ] `BehaviorInAppHandler`：接收行为策略系统上报，评估 AND/OR 策略组合

---

### Phase 9 — 高级节点

**后端**
- [ ] `ScheduledTriggerHandler`：Cron/ONCE 模式，集成 XXL-Job 或 Elastic-Job；分页拉取 userSource（Tagger 标签组 / 静态列表 / 自定义 API），并发控制批量触发
- [ ] `ManualApprovalHandler`：挂起流程，发审批通知（钉钉/邮件），等待 approve/reject；`onTimeout` 三种模式（REJECT/APPROVE/KEEP_WAITING），`KEEP_WAITING` 时 Watchdog 定期续期 ctx
- [ ] `CanvasTriggerHandler`：触发子画布（SYNC 同步等待/ASYNC 异步），防循环（callStack），输出带 outputPrefix 写回父 ctx，Timeout 级联分配
- [ ] `SubFlowRefHandler`：策略表格（多因子精确匹配）+ 数据表格（列查找）+ 工作流子流程
- [ ] 相关 API：`POST /canvas/execution/{id}/approve`、`POST /canvas/execution/{id}/reject`

**数据库**
- [ ] `SCHEDULED_TRIGGER` 注册到 `node_type_registry`
- [ ] `MANUAL_APPROVAL` 注册到 `node_type_registry`
- [ ] `CANVAS_TRIGGER` 注册到 `node_type_registry`
- [ ] `SUB_FLOW_REF` 注册到 `node_type_registry`

---

### Phase 10 — 活动生命周期与用量控制

**后端**
- [ ] `canvas_user_quota` 表建立 + Redis 计数（`canvas:quota:{canvasId}:{userId}:{date}`）
- [ ] 有效期判断（valid_start/valid_end）
- [ ] 全局计数（`canvas:global_count:{canvasId}` Redis INCR）
- [ ] 用户每日/总量限制（Redis INCR 原子扣减 + 超配回滚）
- [ ] 冷却期（last_trigger_at 间隔检查）
- [ ] MySQL 异步写入（Write-Behind，批量刷盘）
- [ ] Redis 崩溃恢复（MySQL 回填 → Write-Through）
- [ ] Nacos 全局默认配额配置（`canvas-quota.yaml`）

---

### Phase 11 — 高并发与性能优化

**后端**
- [ ] 多级缓存：L1 Caffeine（500画布，LRU）→ L2 Redis → L3 MySQL；发布时 Redis Pub/Sub 广播 L1 失效
- [ ] Disruptor Ring Buffer（65536 大小，单生产者多消费者）替换 BlockingQueue 用于 MQ 分发
- [ ] Groovy 脚本发布时预编译缓存（key = canvasId:nodeId:scriptHash）
- [ ] 执行记录异步写入（Disruptor Ring Buffer，积满 200 条或 500ms 批量刷盘）
- [ ] 节点统计事件异步写入（复用 Disruptor，10s 批量更新统计表）
- [ ] 虚拟线程调度器（`Executors.newVirtualThreadPerTaskExecutor()`）用于 Handler I/O 和 Groovy 执行
- [ ] MQ 背压控制：单次拉取 N 条，最大并发执行数限制（Nacos 配置）
- [ ] 内部执行 Topic（外部 MQ → 内部 RocketMQ → 执行引擎，平滑削峰）
- [ ] 单画布并发执行上限（超出排队，队满丢弃记 overflow 日志）
- [ ] Nacos 动态配置：`canvas-engine.yaml`（超时、并发数、Groovy 限制等）

---

### Phase 12 — 可观测性

**后端**
- [ ] Micrometer 指标：执行总数/成功率/耗时分布/节点重试数/MQ 积压/DLQ 大小/缓存命中率
- [ ] 节点漏斗统计：`canvas_node_funnel_stats`（每日汇总 entered/success/failed/skipped）
- [ ] 全量重算 Job（每日凌晨 02:00，修正实时统计偏差）
- [ ] 执行统计 API：`GET /canvas/{id}/stats`、`/funnel`、`/trend`
- [ ] 全链路 traceId 传播（`X-Trace-Id` header 透传到所有下游系统）
- [ ] 结构化 JSON 日志（event 枚举：EXECUTION_STARTED/NODE_COMPLETED/TRIGGER_DEDUPLICATED 等）
- [ ] 告警规则配置（失败率 > 5% → P1 电话，DLQ > 10 条 → P1 钉钉，等）
- [ ] 数据脱敏（手机号/身份证 → DataMaskingUtil，写 trace 和日志前过滤）

**前端**
- [ ] 活动效果看板页（核心指标 KPI、节点漏斗、每日趋势折线图）
- [ ] 执行轨迹可视化（画布节点着色：绿/红/黄/灰 = SUCCESS/FAILED/RUNNING/SKIPPED）
- [ ] 操作日志面板（`canvas_audit_log` 展示）

---

### Phase 13 — 运营工具

**后端**
- [ ] 画布模板：`canvas_template` 表 + `GET /canvas/templates`（按 category 过滤）
- [ ] 另存为模板：`POST /canvas/{id}/save-as-template`
- [ ] 基于模板创建：`POST /canvas/from-template/{templateId}`
- [ ] 发布审批流（高风险识别 + submit-review / approve / reject API）
- [ ] 审批通过后自动发布，记录 `canvas_audit_log`

**前端**
- [ ] 新建画布支持选择模板（模板预览缩略图、官方标注）
- [ ] 审批状态展示（列表页显示待审批徽标）

---

### Phase 14 — 数据治理

**后端**
- [ ] MySQL 按月分区（`canvas_execution`、`canvas_execution_trace`）
- [ ] 分区自动创建 Job（每月 1 日凌晨，幂等设计，失败发 P1 告警）
- [ ] 冷数据归档策略（3个月热 → 12个月温 → OSS 冷 → 2年删除）
- [ ] Watchdog 每日巡检：清理 TTL 过期挂起 ctx，状态更新为 EXPIRED

---

### Phase 15 — 部署与基础设施

**后端**
- [ ] springdoc-openapi 集成（本地/测试开放，生产禁用）
- [ ] 统一错误响应格式（`{"code":"CANVAS_001","message":"...","traceId":"..."}`）
- [ ] 完整错误码表（CANVAS_xxx / EXEC_xxx / NODE_xxx / QUOTA_xxx 分类）
- [ ] Nacos 配置中心接入（`canvas-engine.yaml`、`canvas-groovy-security.yaml`、`canvas-quota.yaml`）
- [ ] 启动依赖检查（MySQL/Redis 必须，MQ 降级）
- [ ] Docker 镜像打包（多阶段构建，JVM 参数调优 for Java 21）
- [ ] 健康检查端点（`/actuator/health`）

**前端**
- [ ] 生产构建优化（代码分割、懒加载）
- [ ] 环境变量管理（API 地址、Sentry 等）

---

## API 完整列表

### 认证

| 方法 | 路径 | 权限 |
|------|------|------|
| POST | `/auth/login` | 公开 |
| POST | `/auth/logout` | 已登录 |
| GET | `/auth/me` | 已登录 |
| GET/POST/PUT | `/admin/users` | ADMIN |
| PUT | `/admin/users/{id}/disable` | ADMIN |

### 画布管理

| 方法 | 路径 | 权限 |
|------|------|------|
| POST | `/canvas` | 已登录 |
| GET | `/canvas/{id}` | 已登录 |
| PUT | `/canvas/{id}` | 已登录（含 editVersion 乐观锁） |
| GET | `/canvas/list` | 已登录 |
| POST | `/canvas/{id}/publish` | ADMIN |
| POST | `/canvas/{id}/offline` | ADMIN |
| POST | `/canvas/{id}/submit-review` | 已登录 |
| POST | `/canvas/{id}/approve` | ADMIN |
| POST | `/canvas/{id}/reject` | ADMIN |
| POST | `/canvas/{id}/kill` | ADMIN |
| POST | `/canvas/{id}/canary?percent=N` | ADMIN |
| POST | `/canvas/{id}/promote-canary` | ADMIN |
| POST | `/canvas/{id}/rollback-canary` | ADMIN |
| POST | `/canvas/{id}/rollback` | ADMIN |
| POST | `/canvas/{id}/dry-run` | 已登录 |
| POST | `/canvas/{id}/clone` | 已登录 |
| GET | `/canvas/{id}/export` | 已登录 |
| POST | `/canvas/import` | ADMIN |
| GET | `/canvas/{id}/versions` | 已登录 |
| GET | `/canvas/{id}/versions/{versionId}` | 已登录 |
| GET | `/canvas/{id}/versions/{v1}/diff/{v2}` | 已登录 |
| GET | `/canvas/templates` | 已登录 |
| POST | `/canvas/{id}/save-as-template` | 已登录 |
| POST | `/canvas/from-template/{id}` | 已登录 |

### 效果分析

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/canvas/{id}/stats` | 整体执行统计 |
| GET | `/canvas/{id}/funnel` | 节点漏斗 |
| GET | `/canvas/{id}/trend` | 每日趋势 |

### 执行引擎

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/canvas/execute/direct/{canvasId}` | 业务直调（同步） |
| POST | `/canvas/trigger/behavior` | 行为触发（异步） |
| POST | `/canvas/execution/replay` | 死信重放 |
| POST | `/canvas/execution/{id}/approve` | 人工审批通过 |
| POST | `/canvas/execution/{id}/reject` | 人工审批拒绝 |

### 元数据

| 方法 | 路径 |
|------|------|
| GET | `/meta/node-types` |
| GET | `/meta/node-types/{key}/schema` |
| GET | `/meta/context-fields` |
| GET | `/meta/mq-topics` |
| GET | `/meta/coupon-types` |
| GET | `/meta/reach-scenes` |
| GET | `/meta/ab-experiments` |
| GET | `/meta/ab-experiments/{key}/groups` |
| GET | `/meta/tagger-tags?type=realtime\|offline` |
| GET | `/meta/biz-lines` |
| GET | `/meta/biz-lines/{key}/apis` |
| GET | `/meta/behavior-strategy-types` |
| GET | `/meta/message-codes?type=IN_APP\|MQ` |

---

## 关键技术决策

| 决策 | 方案 | 理由 |
|------|------|------|
| 画布编辑器 | React Flow | xflow 官方停止维护 |
| MQ | RocketMQ | greenfield 自选，DLQ 原生支持好 |
| 并发保护（节点级） | AtomicBoolean.CAS | 不绑定线程，适配 Reactor |
| 分布式锁 | 原生 Redis SETNX + 心跳续期 | threadId 锁与 Reactor 不兼容 |
| 上下文传递 | Reactor Context API | 替代 ThreadLocal |
| 上下文查找 | flatContext HashMap | O(1) 替代 O(N) 遍历 |
| 画布存储 | 节点中心式（连线内嵌 config） | IF/Selector 分支信息属于节点 |
| 坐标存储 | 内嵌 graph_json x/y | 多端一致，无 localStorage 问题 |
| 环检测 | Kahn 算法 | 产出拓扑顺序，可用于调度 |
| 节点注册 | @NodeHandler 注解 | 新增节点无需改引擎 |
| AB 分流 | Hash(userId:experimentKey) % 100 | 无状态确定性分流 |
| 幂等去重 | Redis dedup key 分 TTL | 首次 24h / 恢复 ~620s，防僵尸 ctx |
| Hub 超时 | timeout 字段（默认 600s） | 防并行分支永久阻塞 |
| Groovy 安全 | SecureASTCustomizer 白名单 | 禁文件/系统命令/网络/反射 |
| 认证 | JWT 自建，RBAC（ADMIN/OPERATOR） | 独立服务，无需 SSO |
| 缓存 | 三级（Caffeine→Redis→MySQL） | 极热数据 JVM 本地，~0ms |
| 异步写入 | Disruptor Ring Buffer | 无锁，执行记录不占主链路 |
| 虚拟线程 | Java 21 Loom | 简化异步代码，节省 OS 线程 |

---

## 风险 & 注意事项

1. **React Flow 格式转换**：编辑态 nodes+edges ↔ 后端节点中心式，双向转换是主要复杂点
2. **Groovy 沙箱**：SecureASTCustomizer + 白名单 + 对象池 + 输出限制，层层防护
3. **多阶段 ctx TTL**：Nacos 配置 `canvas.execution.context-ttl-seconds`，默认 86400s（24h）
4. **同层节点字段冲突**：发布校验阻止发布，前端高亮显示冲突节点
5. **缓存失效广播**：Redis Pub/Sub 通知所有实例刷新 Caffeine，极短时间内缓存不一致可接受
6. **集成层 URL 配置**：每个外部系统独立配置项，dev 指向 WireMock
7. **分区 Job 失败**：P1 告警 + 运维手动补执行 SQL，Job 本身幂等
8. **审批流复杂度**：先实现简单审批（ADMIN 直接 approve/reject），后期可接飞书审批 API
