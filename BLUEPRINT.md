# 营销画布 — 技术蓝图

> 基于 `marketing-canvas-design.md` v1.0 生成
> 最后更新：2026-05-11（澄清会议后）
> 状态：已确认，可开发

---

## 项目总览

| 维度 | 决策 |
|------|------|
| 系统名称 | canvas-engine（营销画布） |
| 项目性质 | 独立新服务 + 独立前端（greenfield，不依赖 union-boot-starter） |
| Java | **Java 21** |
| 后端框架 | **Spring Boot 3.x + WebFlux（Reactor）** |
| ORM | **MyBatis-Plus 3.5.x**，blocking 调用包在 `Schedulers.boundedElastic()` |
| 数据库 | **MySQL 8.0**（Flyway 管理迁移） |
| 缓存/路由 | **Redis**（上下文持久化、触发路由表、分布式锁） |
| MQ | **RocketMQ**（MQ触发节点消费者、触达平台发消息） |
| 前端 | **React 18 + Vite + TypeScript + antd 5** |
| 画布编辑器 | **React Flow**（xflow 官方已停止维护，弃用） |
| 认证 | **自建 JWT 登录**，RBAC 两角色：ADMIN / OPERATOR |
| 节点坐标 | 内嵌在 `graph_json` 每个节点的 `x/y` 字段 |
| Groovy 沙箱 | **标准安全沙箱**（禁文件/系统命令/网络访问） |
| 集成层节点 | **完整 Handler 实现**，外部 URL 通过配置注入（非 stub 逻辑） |

---

## 权限模型（RBAC）

| 角色 | 能力 |
|------|------|
| **ADMIN** | 全部操作：创建、编辑、发布、下线、查看所有人的画布 |
| **OPERATOR** | 创建、编辑自己的画布；只能查看不能发布/下线 |

发布/下线接口需校验 ADMIN 角色，否则返回 403。

---

## 目录结构规划

```
canvas/
├── backend/
│   └── canvas-engine/
│       ├── src/main/java/com/photon/canvas/
│       │   ├── auth/           # JWT 登录、过滤器、用户管理
│       │   ├── config/         # Spring 配置（Security、MyBatis、CORS 等）
│       │   ├── common/         # R<T>、PageResult、异常
│       │   ├── controller/     # HTTP 入口
│       │   ├── domain/
│       │   │   ├── canvas/     # 画布领域（Canvas、CanvasVersion、Service）
│       │   │   ├── meta/       # 节点注册表、上下文字段、外部元数据接口
│       │   │   ├── execution/  # 执行记录、执行轨迹
│       │   │   └── audit/      # 操作日志
│       │   ├── engine/         # 执行引擎核心
│       │   │   ├── dag/        # DAG 解析、Kahn 环检测、邻接表
│       │   │   ├── context/    # ExecutionContext（flatContext O(1)）
│       │   │   ├── handler/    # NodeHandler 接口 + HandlerRegistry
│       │   │   ├── handlers/   # 通用节点 Handler（IF/Groovy/Delay 等）
│       │   │   ├── scheduler/  # Reactor 调度、repeat 并发保护
│       │   │   └── trigger/    # 触发路由（MQ/行为/直调）
│       │   └── infra/
│       │       ├── redis/      # 上下文持久化、路由表、分布式锁
│       │       ├── rocketmq/   # RocketMQ 消费者/生产者
│       │       └── groovy/     # Groovy 沙箱执行器
│       └── src/main/resources/
│           ├── db/migration/   # Flyway 迁移脚本
│           └── application.yml
└── frontend/
    └── src/
        ├── auth/               # 登录页、JWT 存储、axios 拦截器
        ├── pages/
        │   ├── login/
        │   ├── canvas-list/
        │   └── canvas-editor/
        ├── components/
        │   ├── canvas/         # React Flow 封装、节点卡片
        │   ├── node-panel/     # 左侧节点拖拽面板
        │   ├── config-panel/   # 右侧 Schema 驱动表单
        │   └── form-controls/  # 自定义控件（条件规则、代码编辑器等）
        └── services/           # API 封装 + types
```

---

## 数据库 Schema 概览（含补充表）

| 表 | 用途 | 状态 |
|----|------|------|
| `canvas` | 画布主表 | ✅ V1 已建 |
| `canvas_version` | 版本快照 | ✅ V1 已建 |
| `canvas_execution` | 执行记录 | ✅ V1 已建 |
| `canvas_execution_trace` | 节点执行轨迹 | ✅ V1 已建 |
| `context_field` | 上下文字段注册表 | ✅ V1 已建 |
| `node_type_registry` | 节点类型注册（含 config_schema） | ✅ V1 已建 |
| `sys_user` | 用户表（JWT 登录） | 待建（V3） |
| `canvas_audit_log` | 操作日志（创建/发布/下线） | 待建（V3） |
| `canvas_user_quota` | 用户级执行限流 | 待建（V3） |
| `canvas_execution_stats` | 执行统计（按画布/日汇总） | 待建（V3） |

**三张补充表推导字段设计（待 V3 迁移实现）：**

`canvas_audit_log`：
```sql
id, canvas_id, operator, operator_role,
action ENUM('CREATE','PUBLISH','OFFLINE','UPDATE','DELETE'),
detail TEXT,  -- 操作摘要（如发布的 version_id）
created_at DATETIME
```

`canvas_user_quota`：
```sql
id, user_id, canvas_id,
quota_date DATE,                 -- 日期（按天限流）
execution_count INT DEFAULT 0,   -- 当日执行次数
daily_limit INT DEFAULT 1000,    -- 每日上限（可按画布配置）
PRIMARY KEY (user_id, canvas_id, quota_date)
```

`canvas_execution_stats`：
```sql
id, canvas_id,
stat_date DATE,
total_count INT DEFAULT 0,
success_count INT DEFAULT 0,
fail_count INT DEFAULT 0,
paused_count INT DEFAULT 0,
avg_duration_ms BIGINT,          -- 平均执行耗时
PRIMARY KEY (canvas_id, stat_date)
```

---

## 开发阶段划分（修订版）

### Phase 1 — 基础骨架 ✅ 已完成（需修正）

**已完成但需修正：**
- 前端 package.json 换 `reactflow`（当前是 xflow 占位）
- SQL 补充 V3 迁移（sys_user + 3 张补充表）
- 后端补充 JWT 认证模块

### Phase 2 — 画布编辑器（React Flow 接入）

**后端**
- DAG 校验（Kahn 算法）→ 发布流程完整实现
- 历史版本 API、版本回滚
- 触发路由注册 Redis（发布/下线时）

**前端**
- React Flow 接入：自定义节点卡片、按 category 渲染颜色
- 左侧节点面板（从 `/meta/node-types` 加载，分类展示，支持拖入画布）
- 右侧配置面板：Schema 驱动动态渲染表单
- 自定义表单控件：条件规则列表、参数列表、上下文字段选择器、代码编辑器
- graph_json ↔ React Flow nodes/edges 格式互转
- 发布校验错误高亮展示

### Phase 3 — 执行引擎核心

- `ExecutionContext`（flatContext O(1)）
- `NodeHandler` 接口 + `@NodeHandler` 注解自动注册
- `DagEngine`：DAG 解析、本地内存缓存、Reactor 异步调度
- 单节点执行 6 阶段（配置解析→逻辑关系检查→幂等→CAS→Handler→触发下游）
- repeat 并发保护（`AtomicBoolean.compareAndSet`）
- 通用节点 Handler（全部完整实现）：
  - `IfConditionHandler`（含 CONTAINS IN 语义、BigDecimal 数值比较）
  - `SelectorHandler`（多分支按序匹配）
  - `LogicRelationHandler`（AND/OR，SKIPPED 节点处理）
  - `HubHandler`（等待所有上游完成，**timeout 超时强制 FAILED**）
  - `GroovyHandler`（**标准安全沙箱**，GroovyUtils 工具方法）
  - `DelayHandler`、`ApiCallHandler`、`PriorityHandler`
  - `DirectCallHandler` / `DirectReturnHandler`
- 防资损判定（`benefitGranted`/`userReached`）
- 执行记录 + SKIPPED 批量写入

### Phase 4 — 触发机制

- **RocketMQ** 消费者 → Redis 路由查找 → 批量触发
- 业务直调 `POST /canvas/execute/direct/{canvasId}`
- 端内行为触发 `POST /canvas/trigger/behavior`
- 服务重启路由表全量重建（`@PostConstruct`）
- dedup key 幂等去重（首次 24h TTL / 恢复 ~10min TTL）

### Phase 5 — 多阶段执行

- 挂起/恢复机制（Redis 序列化 ctx，TTL 可配）
- resume-lock 分布式锁（原生 Redis SETNX + 心跳续期）
- 僵尸 ctx 清理（Watchdog + last_dedup_key）
- benefitGranted 随 ctx 持久化

### Phase 6 — 集成层节点（完整实现）

**所有 Handler 均为完整实现，外部 URL 通过 application.yml 配置：**
- `MqTriggerHandler`（消费 RocketMQ，校验 payload）
- `CouponHandler`（HTTP 调用券系统，idempotencyKey 防重，benefitGranted 设标志）
- `InAppNotifyHandler`（MQTT 推送）
- `ReachPlatformHandler`（发 RocketMQ 至触达平台）
- `TaggerOfflineHandler` / `TaggerRealtimeHandler`（HTTP 调用 Tagger）
- `AbSplitHandler`（Hash(userId:experimentKey) % 100 确定性分流）
- `SendMqHandler`（RocketMQ 生产者）

**前端**
- 执行测试面板（发起直调，实时轮询执行状态）
- 执行轨迹可视化（节点着色：成功/失败/跳过/进行中）
- 操作日志面板

---

## 关键技术决策

| 决策 | 方案 | 理由 |
|------|------|------|
| 画布编辑器 | **React Flow** | xflow 官方停止维护，React Flow 社区活跃文档完整 |
| MQ | **RocketMQ** | greenfield 项目自选，与执行引擎触发机制匹配 |
| 异步框架 | Spring Reactor (WebFlux) | 异步非阻塞，节点等待不占线程 |
| 并发保护 | AtomicBoolean.compareAndSet | 不绑定线程，适配 Reactor 协程切换 |
| 分布式锁 | 原生 Redis SETNX + 心跳续期 | threadId 绑定锁与 Reactor 不兼容 |
| 上下文传递 | Reactor Context API | 替代 ThreadLocal |
| 上下文查找 | flatContext（HashMap） | O(1) 替代 O(N) 遍历 |
| 画布存储 | 节点中心式（连线内嵌 config） | IF/Selector 分支信息天然属于节点 |
| 环检测 | Kahn 算法（拓扑排序） | 检测环同时产出拓扑顺序 |
| 节点注册 | @NodeHandler 注解 + Spring Bean | 新增节点无需改引擎 |
| 前端表单 | config_schema JSON 动态渲染 | 消除前端 if-else |
| AB 分流 | Hash(userId:experimentKey) % 100 | 无状态确定性分流 |
| 幂等去重 | Redis dedup key 分 TTL | 首次 24h / 恢复 ~10min，防僵尸 ctx |
| Hub 超时 | timeout 字段（默认 600s） | 防并行分支永久阻塞 |
| Groovy 安全 | 标准安全沙箱 | 禁文件/系统命令/网络，内部可信用户 |
| 认证 | JWT 自建，RBAC（ADMIN/OPERATOR） | 独立服务，无需 SSO |

---

## 风险 & 注意事项

1. **React Flow 格式转换**：前端编辑态用 `nodes[]`+`edges[]`，存库用节点中心式 JSON，需维护双向转换逻辑，是主要复杂点。
2. **Groovy 沙箱**：使用 `GroovyShell` + `SecureASTCustomizer`，禁止 `System`/`Runtime`/`File` 等类访问。
3. **多阶段 ctx TTL**：配置化（`canvas.execution.context-ttl-seconds`），默认 3600s。
4. **同层节点字段冲突**：发布校验检测同层节点 output_schema fieldKey 重复，重复则阻止发布。
5. **缓存失效广播**：画布发布后通过 Redis Pub/Sub 通知所有引擎实例刷新本地缓存。
6. **集成层 URL 配置**：每个外部系统 URL 独立配置项，dev 环境可指向 WireMock。
