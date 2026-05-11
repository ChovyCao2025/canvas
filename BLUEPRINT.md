# 营销画布 — 技术蓝图

> 基于 `marketing-canvas-design.md` v1.0 生成  
> 状态：待评审

---

## 项目总览

| 维度 | 说明 |
|------|------|
| 系统名称 | canvas-engine（营销画布） |
| 前端技术 | React + @antv/xflow + antd |
| 后端技术 | Spring Boot WebFlux（Reactor） |
| 存储 | MySQL（元数据/版本）+ Redis（上下文/路由表） |
| 核心模式 | 配置与执行分离、DAG、Schema 驱动、节点可插拔 |

---

## 目录结构规划

```
canvas/
├── backend/                    # Spring Boot 后端
│   ├── canvas-engine/          # 单模块（monolith，后期可拆）
│   │   ├── src/main/java/com/photon/canvas/
│   │   │   ├── config/         # Spring 配置
│   │   │   ├── controller/     # HTTP 入口（配置服务 API）
│   │   │   ├── domain/
│   │   │   │   ├── canvas/     # 画布领域（entity, repository, service）
│   │   │   │   ├── meta/       # 节点注册表、上下文字段注册表
│   │   │   │   └── execution/  # 执行记录领域
│   │   │   ├── engine/         # 执行引擎核心
│   │   │   │   ├── dag/        # DAG 解析、邻接表构建、Kahn环检测
│   │   │   │   ├── context/    # ExecutionContext 模型
│   │   │   │   ├── handler/    # NodeHandler 接口 + HandlerRegistry
│   │   │   │   ├── handlers/   # 通用 Handler 实现（IF/Groovy/Delay 等）
│   │   │   │   ├── scheduler/  # Reactor 调度、repeat 并发保护
│   │   │   │   └── trigger/    # 触发路由（MQ/行为/直调）
│   │   │   └── infra/
│   │   │       ├── redis/      # 上下文持久化、路由表、分布式锁
│   │   │       └── mq/         # MQ 消费者接入
│   │   └── src/main/resources/
│   │       ├── db/migration/   # Flyway SQL 脚本（建表 DDL）
│   │       └── application.yml
└── frontend/                   # React 前端
    ├── src/
    │   ├── pages/
    │   │   ├── canvas-list/    # 画布列表页
    │   │   └── canvas-editor/  # 画布编辑页（主页面）
    │   ├── components/
    │   │   ├── canvas/
    │   │   │   ├── CanvasNode/ # 节点卡片 React 组件
    │   │   │   ├── NodePanel/  # 左侧节点面板
    │   │   │   └── ConfigPanel/# 右侧配置面板
    │   │   └── form-controls/  # 自定义表单控件（条件规则/代码编辑器等）
    │   ├── services/           # 后端 API 调用封装
    │   └── types/              # TypeScript 类型定义
    └── package.json
```

---

## 开发阶段划分

### Phase 1 — 基础骨架（2周）

**后端**
- [ ] Spring Boot WebFlux 项目初始化（Gradle）
- [ ] MySQL 建表 DDL（Flyway 管理）：`canvas`、`canvas_version`、`canvas_execution`、`canvas_execution_trace`、`context_field`、`node_type_registry`
- [ ] 画布 CRUD API（`/canvas` 系列）
- [ ] 元数据 API（`/meta/node-types`、`/meta/context-fields`）
- [ ] 预置节点类型注册数据（INSERT 脚本）

**前端**
- [ ] React 项目初始化（Vite + TypeScript）
- [ ] 画布列表页（增删改查）
- [ ] xflow 画布编辑器框架接入（空白画布可渲染）
- [ ] 左侧节点面板（从 `/meta/node-types` 加载，支持分类折叠）

### Phase 2 — 核心画布功能（3周）

**后端**
- [ ] 发布流程：DAG 校验（Kahn 算法环检测）→ 版本快照 → 触发路由注册 Redis
- [ ] 历史版本 API（列表 + 指定版本获取）
- [ ] 下线 API（清理 Redis 路由表）
- [ ] 版本回滚支持

**前端**
- [ ] 节点卡片组件（统一 `CanvasNode`，按 category 渲染颜色）
- [ ] 右侧配置表单（Schema 驱动动态渲染，从 `/meta/node-types/:key/schema` 加载）
- [ ] 自定义表单控件：条件规则列表、参数列表、上下文字段选择器
- [ ] 画布保存（xflow 格式 → 后端节点中心格式转换）
- [ ] 发布/下线操作 + 错误高亮展示
- [ ] 版本历史面板

### Phase 3 — 执行引擎核心（4周）

**后端**
- [ ] `ExecutionContext` 模型（含 `flatContext` O(1) 查找优化）
- [ ] `NodeHandler` 接口 + `HandlerRegistry`（`@NodeHandler` 注解自动注册）
- [ ] `DagEngine`：DAG 解析、本地内存缓存、Reactor 异步调度
- [ ] 单节点执行 6 阶段流程（解析配置→逻辑关系检查→幂等→CAS 抢占→Handler→触发下游）
- [ ] repeat 并发保护机制（`AtomicBoolean.compareAndSet`）
- [ ] 通用节点 Handler 实现：
  - `IfConditionHandler`（IF 判断）
  - `SelectorHandler`（条件选择器，CONTAINS 操作符完整语义）
  - `LogicRelationHandler`（AND/OR，SKIPPED 节点处理）
  - `HubHandler`（集线器，等待所有上游完成）
  - `GroovyHandler`（Groovy 脚本，含 GroovyUtils 工具方法）
  - `DelayHandler`（延迟器）
  - `ApiCallHandler`（接口调用）
  - `PriorityHandler`（优先级）
  - `DirectCallHandler` / `DirectReturnHandler`（业务直调）
- [ ] 执行结果判定（防资损：`benefitGranted`/`userReached` 全局标志位）
- [ ] 执行记录写入（`canvas_execution` + `canvas_execution_trace`，含 SKIPPED 批量写入）

### Phase 4 — 触发机制（2周）

**后端**
- [ ] MQ 触发：消费者 → Redis 路由查找 → 批量触发画布执行
- [ ] 业务直调：`POST /canvas/execute/direct/{canvasId}` 同步 HTTP 接口
- [ ] 端内行为触发接口（`/canvas/trigger/behavior`）
- [ ] 服务重启后触发路由表全量重建（`@PostConstruct`）
- [ ] dedup key 幂等去重（Redis SETNX + 区分首次/恢复 TTL）

### Phase 5 — 多阶段执行（2周）

**后端**
- [ ] 挂起机制：LOGIC_RELATION 条件未满足 → 序列化 ctx → Redis（含 TTL 配置）
- [ ] 恢复机制：第二次触发 → 反序列化 ctx → 追加 payload → 继续执行
- [ ] resume-lock 分布式锁（防多实例并发恢复）
- [ ] 僵尸 ctx 清理：`canvas_execution.last_dedup_key` 字段 + Watchdog 定时扫描
- [ ] 超长挂起防重：`benefitGranted` 随 ctx 持久化到 Redis

### Phase 6 — 前端高级功能 + 集成节点（2周）

**后端（集成 Handler，Mock 实现，供前端联调）**
- [ ] `MqTriggerHandler`（stub）
- [ ] `CouponHandler`（含 idempotencyKey 发券，防资损算法）
- [ ] `InAppNotifyHandler`（MQTT 推送）
- [ ] `ReachPlatformHandler`（触达平台 MQ 发送）
- [ ] `TaggerOfflineHandler` / `TaggerRealtimeHandler`
- [ ] `AbSplitHandler`（Hash 确定性分流）
- [ ] 分布式锁（原生 Redis SETNX + 心跳续期，替代 lock-sedis）

**前端**
- [ ] 执行测试面板（发起直调执行，实时展示节点执行状态）
- [ ] 执行轨迹可视化（节点着色：成功/失败/跳过/进行中）
- [ ] Groovy 代码编辑器（Monaco Editor）
- [ ] 画布 AB 分流节点实验分组动态加载

---

## 核心接口契约（后端 → 前端）

### 配置服务

```
POST   /canvas                           创建画布
GET    /canvas/{id}                      获取画布（含最新草稿）
PUT    /canvas/{id}                      保存草稿
GET    /canvas/list?page=1&size=20       画布列表
POST   /canvas/{id}/publish              发布
POST   /canvas/{id}/offline              下线
GET    /canvas/{id}/versions             历史版本列表
GET    /canvas/{id}/versions/{versionId} 指定版本
```

### 元数据（Schema 驱动前端，不写 if-else）

```
GET    /meta/node-types                  节点类型列表（含 config_schema）
GET    /meta/node-types/{key}/schema     指定节点表单 Schema
GET    /meta/context-fields              上下文字段注册表
GET    /meta/mq-topics                   MQ topic 列表
GET    /meta/coupon-types                券类型列表
GET    /meta/reach-scenes                触达场景列表
GET    /meta/ab-experiments              AB 实验列表
GET    /meta/ab-experiments/{key}/groups 实验分组
GET    /meta/tagger-tags?type=realtime|offline  Tagger 标签
GET    /meta/biz-lines                   业务线列表
GET    /meta/biz-lines/{key}/apis        业务线接口列表
GET    /meta/behavior-strategy-types     行为策略类型
GET    /meta/message-codes?type=IN_APP|MQ 消息 code 列表
```

### 执行引擎

```
POST   /canvas/execute/direct/{canvasId} 业务直调（同步）
POST   /canvas/trigger/behavior          端内行为触发（异步）
```

---

## 关键技术决策

| 决策 | 方案 | 理由 |
|------|------|------|
| 异步框架 | Spring Reactor (WebFlux) | 异步非阻塞，单节点等待不占线程 |
| 并发保护（节点级） | AtomicBoolean.compareAndSet | 不绑定线程，适配 Reactor 协程切换 |
| 分布式锁 | 原生 Redis SETNX + 心跳续期 | lock-sedis 依赖 threadId，与 Reactor 不兼容 |
| 上下文传递 | Reactor Context API | 替代 ThreadLocal，保证跨线程切换正确 |
| 上下文查找性能 | flatContext（扁平 HashMap） | O(1) 替代 O(N) 节点遍历 |
| 画布存储 | 节点中心式（连线内嵌 config） | IF/Selector 分支信息天然属于节点 config |
| 环检测算法 | Kahn 算法（拓扑排序） | 检测环的同时产出拓扑顺序，可用于调度 |
| 节点注册 | @NodeHandler 注解 + Spring Bean | 新增节点类型无需改引擎，仅插入 DB 记录 |
| 前端表单驱动 | config_schema（JSON 数组）动态渲染 | 消除前端 if-else，后端驱动表单结构 |
| AB 分流 | Hash(userId:experimentKey) % 100 | 无状态确定性分流，相同用户永远同组 |
| 幂等去重 | Redis dedup key（首次 24h / 恢复 ~10min） | 区分 TTL 防止恢复场景 dedup 僵尸化 |

---

## 数据库 Schema 概览

| 表 | 用途 |
|----|------|
| `canvas` | 画布主表（name/status/published_version_id） |
| `canvas_version` | 版本快照（每次发布生成，graph_json 全量） |
| `canvas_execution` | 执行记录（executionId/status/result） |
| `canvas_execution_trace` | 节点执行轨迹（input/output/status） |
| `context_field` | 上下文字段注册表（field_key/data_type） |
| `node_type_registry` | 节点类型插件注册（handler_class/config_schema/output_schema） |

---

## 风险 & 注意事项

1. **xflow 版本锁定**：`@antv/xflow` API 变化频繁，在 Phase 1 确认使用版本并锁定 package-lock。
2. **Groovy 安全沙箱**：Groovy 节点执行用户代码，需配置 SecureASTCustomizer 或 SandboxTransformer，防止调用系统接口。
3. **多阶段 ctx Redis TTL**：TTL 配置为业务可配（Nacos），默认 3600s，不同画布可按需覆盖。
4. **同层节点同名字段**：发布校验需检测同层节点 output_schema 是否有 fieldKey 冲突，冲突时阻止发布。
5. **执行引擎本地缓存失效**：画布发布后需推送更新到所有执行引擎实例（可用 Redis Pub/Sub 广播）。
