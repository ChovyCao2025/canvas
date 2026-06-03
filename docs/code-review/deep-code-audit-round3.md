# Deep Code Audit — Round 3

> 第三轮深度扫描：多租户隔离完整性、Handler→Mapper分层违规、DataSourceConfig密码暴露、前端巨型组件、数据库约束缺失
> 审计日期: 2026-05-31

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **P0 CRITICAL** | 3 | 多租户隔离失效、DataSourceConfig密码API泄露、Handler→Mapper违规(14处) |
| **P1 HIGH** | 6 | CanvasDO缺tenantId字段、前端巨型组件2084行、console.log泄露token、@Lazy循环依赖7处、DB无外键约束、无乐观锁重试 |
| **P2 MEDIUM** | 5 | 30+处直接StringRedisTemplate注入、7处Handler注入ObjectMapper(非Spring管理)、90个Flyway迁移无基线、前端91个useEffect无统一模式 |
| **P3 LOW** | 2 | V78迁移ALTER TABLE无IF NOT EXISTS保护、TenantService硬编码QueryWrapper |

---

## P0 — CRITICAL

### P0-1: 多租户隔离失效 — Canvas/Execution/Handler 层无 tenant_id 过滤

**问题**: V78 迁移已为 `canvas`、`canvas_execution`、`canvas_execution_trace` 等表添加 `tenant_id` 列，但：

1. **`CanvasDO`** 无 `tenantId` 字段 — MyBatis-Plus 实体未映射，所有查询自动忽略
2. **`CanvasService`** 无 tenant 过滤 — `list()`、`getById()` 不限租户
3. **`CanvasController`** 无 tenant 校验 — 任何登录用户可访问其他租户的画布
4. **`ExecutionController`** 无 tenant 校验 — 公开端点（direct/behavior）无租户上下文
5. **Engine 层** 完全无 tenant 概念 — `CanvasExecutionService`、`DagEngine`、所有 Handler 无 tenant_id 传递

**影响**: 
- 租户 A 可读取/修改/执行租户 B 的画布 → **数据越权**
- 公开端点无租户上下文 → 任意租户的画布可被触发执行

**修复**: 
1. `CanvasDO` 添加 `tenantId` 字段
2. `CanvasService` 所有查询添加 `eq(CanvasDO::getTenantId, currentTenantId)`
3. `ExecutionController` 公开端点添加 API Key + tenant 绑定
4. Engine 层 `ExecutionContext` 添加 `tenantId`，所有 DB 操作带租户过滤
5. 考虑 MyBatis-Plus `TenantLineInnerInterceptor` 自动注入

---

### P0-2: DataSourceConfig password 通过 API 明文返回

**文件**: `web/DataSourceConfigController.java:198`, `dal/dataobject/DataSourceConfigDO.java:36`

**问题**: 
1. `DataSourceConfigDO.password` 无 `@TableField(select = false)` — 查询时返回明文密码
2. `DataSourceConfigController` 的 `testConnection` 方法直接 `config.getPassword()` 读取明文
3. 更严重：如果 `GET /canvas/data-sources/{id}` 返回完整 DO，密码通过 JSON 暴露给前端

**影响**: 前端用户可通过 API 看到外部数据库密码 → 凭证泄露

**修复**: 
1. `DataSourceConfigDO.password` 添加 `@TableField(select = false)`
2. 密码列改用 Jasypt 加密存储
3. API 响应 DTO 不包含 password 字段
4. `testConnection` 改为从加密存储解密后使用

---

### P0-3: 14 个 Handler 直接注入 Mapper — 分层违规

**文件**: `engine/handlers/` 目录

| Handler | 注入的 Mapper | 应走 Service/Repository |
|---------|--------------|----------------------|
| SubFlowRefHandler | CanvasMapper, CanvasVersionMapper | CanvasService |
| TagOperationHandler | CustomerTagMapper | CustomerTagService |
| UpdateProfileHandler | CustomerProfileMapper | CustomerProfileService |
| SendMqHandler | MqMessageDefinitionMapper | MqDefinitionService |
| CreateTaskHandler | CustomerTaskRecordMapper | CustomerTaskService |
| ManualApprovalHandler | CanvasManualApprovalMapper | ManualApprovalService |
| GoalCheckHandler | EventLogMapper | EventLogService |
| TrackEventHandler | EventLogMapper | EventLogService |
| PointsOperationHandler | CustomerPointsLedgerMapper | PointsService |
| CanvasTriggerHandler | CanvasMapper | CanvasService |
| ApiCallHandler | StringRedisTemplate (直接) | RateLimitService |
| WaitHandler | ObjectMapper (非Spring) | — |
| SubFlowRefHandler | ObjectMapper (非Spring) | — |
| ManualApprovalHandler | ObjectMapper (非Spring) | — |

**影响**: 
- Handler 直接依赖数据层 → 无法独立测试（需 mock Mapper）
- 业务逻辑分散在 Handler 中 → 重复代码（如 TagOperationHandler 和 TagService 做同样的事）
- 违反依赖倒置原则 → 架构腐化

**修复**: 引入 Repository 接口层，Handler 仅依赖 Repository/Service 接口

---

## P1 — HIGH

### P1-1: CanvasDO 缺少 tenantId 字段映射

**文件**: `dal/dataobject/CanvasDO.java`

**问题**: V78 迁移已为 `canvas` 表添加 `tenant_id BIGINT` 列，但 `CanvasDO` 无对应字段。MyBatis-Plus 查询时自动忽略此列，写入时不填充。

**影响**: 新创建的画布 `tenant_id = NULL` → 租户隔离失效

**修复**: 添加 `private Long tenantId;` + `@TableField(fill = FieldFill.INSERT)`

---

### P1-2: 前端 canvas-editor 2084 行 — 巨型组件

**文件**: `frontend/src/pages/canvas-editor/index.tsx`

**问题**: 单文件 2084 行，包含：
- 画布编辑器主体
- 节点配置验证逻辑
- 发布/下线/克隆对话框
- 版本对比
- Auto-save
- 键盘快捷键
- 91 个 useEffect（整个前端）

**影响**: 任何修改都有高回归风险，渲染性能差，难以测试

**修复**: 
- 提取 `useCanvasEditor` hook（状态 + 逻辑）
- 提取 `CanvasPublishDialog`、`CanvasVersionDiff` 子组件
- 验证逻辑提取到 `canvasValidation.ts`

---

### P1-3: console.log 泄露 JWT Token

**文件**: `frontend/src/context/AuthContext.tsx:73`

```typescript
console.log('[AUTH] init token:', token?.slice(0,20), 'saved:', !!saved)
```

**问题**: 开发调试日志泄露 token 前 20 字符。生产环境不应输出任何 token 片段。

**修复**: 删除或改为 `process.env.NODE_ENV === 'development'` 条件日志

---

### P1-4: 7 处 @Lazy 循环依赖 — 架构设计问题

| 位置 | 循环链 |
|------|--------|
| DagEngine → CanvasExecutionService | DagEngine ↔ CanvasExecutionService |
| CanvasDisruptorService → CanvasExecutionService, CanvasExecutionRequestExecutor | Disruptor ↔ Execution |
| SubFlowRefHandler → DagEngine | Handler ↔ Engine |
| TaggerHandler → CanvasExecutionService | Handler ↔ Execution |
| CanvasTriggerHandler → DagEngine | Handler ↔ Engine |
| TransferJourneyHandler → CanvasExecutionService | Handler ↔ Execution |

**影响**: Spring 容器初始化顺序不确定 → 潜在 NPE；代码修改可能打破脆弱的初始化链

**修复**: 
- Handler → Engine: 通过事件总线解耦（Handler 发事件，Engine 订阅）
- DagEngine ↔ CanvasExecutionService: 提取 `ExecutionCallback` 接口

---

### P1-5: 数据库无外键约束 — 引用完整性无保障

**问题**: 检查 V1-V90 迁移，所有表使用 `BIGINT` 引用其他表但无 `FOREIGN KEY` 约束。例如：
- `canvas_execution.canvas_id` → `canvas.id` 无 FK
- `canvas_version.canvas_id` → `canvas.id` 无 FK
- `canvas_execution_trace.execution_id` → `canvas_execution.id` 无 FK

**影响**: 
- 删除画布时执行记录可能孤立
- 应用层必须手动保证一致性 → 容易遗漏

**修复**: 
- 考虑添加 FK（性能敏感表可延迟）
- 至少添加应用层级联删除逻辑

---

### P1-6: 乐观锁 editVersion 无重试机制

**文件**: `dal/dataobject/CanvasDO.java:91`

**问题**: `CanvasDO.editVersion` 用于乐观锁防并发编辑覆盖，但 `CanvasService` 的更新操作未实现乐观锁重试。并发更新时直接失败返回错误。

**影响**: 用户并发编辑画布时频繁收到错误 → 体验差

**修复**: 添加自动重试（最多 3 次），或前端提示"内容已被修改，请刷新后重试"

---

## P2 — MEDIUM

### P2-1: 18 处直接注入 StringRedisTemplate — 无 Redis 抽象层

**问题**: 18 个组件直接注入 `StringRedisTemplate`，Redis key 散落在各处。虽然已有 `RedisKeyUtil`，但 Redis 操作（INCR、ZADD、SET NX 等）无统一封装。

**影响**: 
- Redis 命令变更需改 18 处
- 无法统一切换 Redis 实现（如 Redisson）
- 无法统一添加监控/限流

**修复**: 引入 `RedisCommandService` 封装常用操作

---

### P2-2: 7 处 Handler 注入 ObjectMapper — 非 Spring 管理

**文件**: ApiCallHandler, SubFlowRefHandler, ManualApprovalHandler, UpdateProfileHandler, TrackEventHandler, GoalCheckHandler, WaitHandler

**问题**: 这些 Handler 注入 Spring 管理的 `ObjectMapper`（正确），但 `ConditionEvaluator` 静态创建 `new ObjectMapper()`（P1-7 Round2）。不一致的 ObjectMapper 配置可能导致序列化行为不同。

**修复**: 统一使用 Spring `ObjectMapper`，`ConditionEvaluator` 改为注入

---

### P2-3: 90 个 Flyway 迁移无基线 — 新环境迁移慢

**问题**: V1-V90 共 90 个迁移文件，新环境首次启动需顺序执行全部。无基线版本（`flyway:baseline`），开发/测试环境每次重建耗时。

**修复**: 
- 定期合并历史迁移为基线（如 V1-V70 合并为 V70__baseline.sql）
- CI 环境使用 Docker 镜像预置 DB

---

### P2-4: 前端 91 个 useEffect — 缺乏统一副作用管理

**问题**: 309 个 hooks（useState + useEffect），91 个 useEffect。无统一模式：
- 部分 useEffect 有 cleanup（NotificationContext、cdp-users）
- 部分无 cleanup（潜在内存泄漏）
- Auto-save、WebSocket、轮询等副作用分散在各组件

**修复**: 
- 提取 `useAutoSave`、`useWebSocket`、`usePolling` 自定义 hook
- 所有 useEffect 必须有 cleanup 或注释说明为何不需要

---

### P2-5: V78 迁移 ALTER TABLE 无 IF NOT EXISTS 保护

**文件**: `V78__saas_foundation.sql`

**问题**: 
```sql
ALTER TABLE canvas ADD COLUMN tenant_id BIGINT NULL AFTER id;
```
如果 V78 执行失败后重试，ALTER TABLE 会因列已存在而报错。Flyway checksum 校验会阻止重试，但手动 `flyway repair` 后可能重复执行。

**修复**: 使用存储过程或 `IF NOT EXISTS` 模式（MySQL 8.0 不原生支持，需用 `INFORMATION_SCHEMA` 检查）

---

## P3 — LOW

### P3-1: TenantService 硬编码 QueryWrapper

**文件**: `domain/tenant/TenantService.java:69-78`

**问题**: 
```java
new QueryWrapper<CanvasDO>().eq("tenant_id", tenantId)
```
使用字符串列名而非 Lambda 表达式，无编译期检查。

**修复**: 改为 `new LambdaQueryWrapper<CanvasDO>().eq(CanvasDO::getTenantId, tenantId)` （需先添加 CanvasDO.tenantId 字段）

---

### P3-2: 前端 API 服务 create/update 使用 any 类型

**文件**: `frontend/src/services/api.ts:370-372`

```typescript
create: (body: any) =>
update: (id: number, body: any) =>
```

**影响**: API 请求体无类型检查 → 可发送任意字段

**修复**: 定义 `CanvasCreateReq`、`CanvasUpdateReq` interface

---

## Multi-Tenant Isolation Audit

| Layer | tenant_id 过滤 | 评估 |
|-------|---------------|------|
| **DB Schema** | V78 已添加列 | ✅ 列存在 |
| **MyBatis-Plus Entity** | CanvasDO 无 tenantId 字段 | ❌ **P1-1** |
| **Domain Service** | CanvasService 无 tenant 过滤 | ❌ **P0-1** |
| **Controller** | CanvasController 无 tenant 校验 | ❌ **P0-1** |
| **ExecutionController** | 公开端点无 tenant | ❌ **P0-1** |
| **Engine Layer** | ExecutionContext 无 tenantId | ❌ **P0-1** |
| **Handler Layer** | 全部 Handler 无 tenant | ❌ **P0-1** |
| **Auth (SysUserService)** | 正确过滤 tenant_id | ✅ |
| **Admin (AdminController)** | 正确使用 TenantContext | ✅ |
| **Meta (MetaController)** | 正确使用 TenantContext | ✅ |

**结论**: 多租户隔离仅在 Auth/Admin/Meta 层实现，核心业务层（Canvas/Execution/Engine/Handler）**完全无隔离**。这是最严重的安全问题。

---

## Handler Layer Violation Matrix

| Handler | Mapper 直接注入 | Service 应替代 | ObjectMapper | StringRedisTemplate |
|---------|:---:|---|:---:|:---:|
| SubFlowRefHandler | CanvasMapper, CanvasVersionMapper | CanvasService | ✅ | — |
| TagOperationHandler | CustomerTagMapper | CustomerTagService | — | — |
| UpdateProfileHandler | CustomerProfileMapper | CustomerProfileService | ✅ | — |
| SendMqHandler | MqMessageDefinitionMapper | MqDefinitionService | — | — |
| CreateTaskHandler | CustomerTaskRecordMapper | CustomerTaskService | — | — |
| ManualApprovalHandler | CanvasManualApprovalMapper | ManualApprovalService | ✅ | — |
| GoalCheckHandler | EventLogMapper | EventLogService | ✅ | — |
| TrackEventHandler | EventLogMapper | EventLogService | ✅ | — |
| PointsOperationHandler | CustomerPointsLedgerMapper | PointsService | — | — |
| CanvasTriggerHandler | CanvasMapper | CanvasService | — | — |
| ApiCallHandler | — | — | ✅ | ✅ |

---

## Cumulative Findings (Round 1 + 2 + 3)

| Severity | Round 1 | Round 2 | Round 3 | **Total** |
|----------|---------|---------|---------|-----------|
| P0 CRITICAL | 21 | 8 | 3 | **32** |
| P1 HIGH | 39 | 12 | 6 | **57** |
| P2 MEDIUM | — | 9 | 5 | **14** |
| P3 LOW | — | 4 | 2 | **6** |

### Top 5 Most Critical Issues (All Rounds)

1. **P0-1 R3**: 多租户隔离失效 — Canvas/Engine/Handler 层无 tenant_id 过滤
2. **P0-2 R3**: DataSourceConfig password API 明文返回
3. **P0-5 R2**: ExecutionController 行为触发端点 userId 伪造
4. **P0-1 R2**: Audience JDBC 动态 DataSource 连接泄漏
5. **P0-2 R2**: SQL 注入风险 — 表名/列名拼接

### Recommended Fix Order (All Rounds)

1. **Week 1**: 多租户隔离 (P0-1 R3) + DataSourceConfig 密码 (P0-2 R3) + 行为触发认证 (P0-5 R2)
2. **Week 2**: SQL 注入防护 (P0-2 R2) + DataSource 池化 (P0-1 R2) + @Transactional+Redis (P0-8 R2)
3. **Week 3**: Handler→Repository 重构 (P0-3 R3) + God Class 拆分 (P0-6/7 R2) + .block() 修复 (P0-4 R2)
4. **Week 4**: 异常处理 (P1-1/4 R2) + 前端安全 (P1-10/11 R2) + 巨型组件拆分 (P1-2 R3)
