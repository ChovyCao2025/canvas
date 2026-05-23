# 完整 CDP 能力设计

## 背景

当前系统已经具备营销画布、执行轨迹、标签定义、人群圈选和 TAGGER 人群分支能力：

- 画布执行数据在 `canvas_execution` 和 `canvas_execution_trace` 中沉淀。
- 标签元数据在 `tag_definition` 中维护，前端已有 `/tag-config`。
- 人群定义在 `audience_definition` 中维护，前端已有 `/audiences` 和 `/audiences/:id/edit`。
- TAGGER 节点已经支持 `offline`、`realtime`、`audience` 模式，其中 `audience` 模式可以基于人群 bitmap 分支。
- 画布统计页已有总量、趋势、节点漏斗和执行轨迹查看能力。

缺口是：这些能力还没有形成 CDP 的统一用户域。系统能执行画布，也能维护标签定义和人群定义，但缺少用户档案、用户标签实例、标签变更历史、画布用户明细、用户画像页，以及从画布对用户写标签的闭环。

## 目标

1. 建立 CDP 用户域基础模型：用户档案、身份映射、用户属性、用户标签实例、标签历史、事件记录。
2. 上线画布用户数据展示：按画布查看进入用户、执行状态、执行次数、最近节点轨迹、上下文输出和用户标签。
3. 上线针对用户打标签能力：支持用户详情手动打标签、批量打标签、画布节点自动打标签。
4. 打通现有标签定义、人群定义和画布执行记录，避免另建一套孤立系统。
5. 保持现有画布、人群、TAGGER 配置兼容，已有流程可以继续运行。
6. 将完整 CDP 拆成阶段交付，第一批上线形成可用闭环，后续逐步补齐事件接入、画像洞察、治理审计。

## 非目标

1. 第一批不接入外部实时埋点 SDK。
2. 第一批不做复杂 ID 合并审批和冲突裁决 UI。
3. 第一批不重写现有 `audience_definition` 计算引擎。
4. 第一批不替换现有外部 Tagger 服务；CDP 标签实例作为本系统内的用户标签事实表。
5. 第一批不建设完整数据血缘平台，只保留来源字段和操作历史，供后续治理扩展。

## 方案选型

### 方案 A：只在画布统计页追加用户列表

做法：
- 直接基于 `canvas_execution` 查询画布用户列表。
- 在用户行上提供简单手动打标签按钮。
- 不新增统一用户域，只把标签结果存成画布局部数据。

优点：
- 改动最小。
- 最快能看到“画布用户数据”页面。

缺点：
- 不能形成 CDP 用户中心。
- 标签只服务某个画布，无法复用于人群、画像和其他旅程。
- 后续补用户档案时需要迁移临时数据。

### 方案 B：先做用户中心和标签实例，再接画布

做法：
- 新建 CDP 用户域表和服务。
- 先上线 `/cdp/users`、用户详情、手动打标签。
- 后续再接画布用户展示和画布打标签节点。

优点：
- 用户域边界清楚。
- 后续扩展稳定。

缺点：
- 用户短期看不到画布闭环。
- 已有执行数据不能第一时间利用起来。

### 方案 C：完整 CDP 分阶段落地

做法：
- 建立完整 CDP 蓝图。
- 第一批同时做用户域地基、画布用户展示、手动和画布自动打标签闭环。
- 后续阶段补齐事件接入、画像洞察、规则标签、权限治理。

优点：
- 满足“完整 CDP”的方向。
- 第一批上线就能形成业务闭环。
- 保持与现有画布、人群、标签模块的兼容。

缺点：
- 第一批比纯画布列表改动更大。
- 需要清晰划分数据模型和服务边界。

### 结论

采用方案 C。完整 CDP 作为总体设计，第一批实现“用户域 + 画布用户数据展示 + 用户打标签闭环”。后续能力按依赖顺序继续扩展，不把所有平台化能力塞进一次上线。

## 总体信息架构

新增 CDP 一级模块，和现有“旅程管理”“人群管理”“标签配置”并列：

- 用户中心
- 用户详情
- 标签实例和标签历史
- 画布用户数据
- 标签动作节点
- 后续阶段的事件、画像、治理页面

推荐路由：

- `/cdp/users`
- `/cdp/users/:userId`
- `/canvas/:id/users`
- `/canvas/:id/users/:userId`

现有路由保留：

- `/tag-config` 继续维护标签定义。
- `/audiences` 继续维护人群定义。
- `/canvas/:id/stats` 继续维护活动效果统计。

## 阶段划分

### 第一批：CDP 核心闭环

第一批必须上线：

1. 用户档案表和用户服务。
2. 用户标签实例表和标签历史表。
3. 画布用户列表和画布用户详情。
4. 用户详情中的手动打标签、移除标签。
5. 批量打标签任务接口。
6. 新增“写用户标签”画布节点。
7. 标签定义扩展字段，用于约束标签值类型、是否允许手动打标、默认有效期。

第一批上线后，运营能完成：

1. 从画布进入用户数据页。
2. 查看某个用户在该画布中的执行历史和节点轨迹。
3. 查看用户当前标签。
4. 对单个用户或一批用户打标签。
5. 在画布中配置节点，当用户经过节点时自动写入 CDP 标签。
6. 后续在人群规则中复用标签数据。

### 第二批：事件和属性接入

1. 统一事件模型 `cdp_event`。
2. API/MQ/批量导入三种接入方式。
3. 用户属性快照和属性历史。
4. 事件驱动的用户 `first_seen_at`、`last_seen_at` 更新。

### 第三批：画像和洞察

1. 用户 360 画像页。
2. 标签分布、事件时间线、旅程记录、归属人群。
3. 画布和人群层面的用户洞察指标。

### 第四批：规则标签和人群增强

1. 基于事件、属性、标签的规则标签计算。
2. 标签和属性参与人群圈选。
3. 人群预估、交并差、版本化和发布。

### 第五批：治理和平台化

1. 标签审批、权限、审计。
2. 敏感字段脱敏和访问控制。
3. 标签血缘、来源统计、过期清理。
4. 数据质量监控和告警。

## 数据模型设计

### 扩展表：`tag_definition`

当前 `tag_definition` 是标签元数据。本次扩展，不改变已有字段含义。

新增字段：

```sql
ALTER TABLE tag_definition
    ADD COLUMN value_type VARCHAR(20) NOT NULL DEFAULT 'STRING' COMMENT 'STRING/NUMBER/BOOLEAN/JSON',
    ADD COLUMN manual_enabled TINYINT NOT NULL DEFAULT 1 COMMENT '是否允许人工打标',
    ADD COLUMN default_ttl_days INT NULL COMMENT '默认有效期天数，null=长期有效',
    ADD COLUMN category VARCHAR(64) NULL COMMENT '标签分类',
    ADD COLUMN owner VARCHAR(64) NULL COMMENT '负责人',
    ADD COLUMN write_policy VARCHAR(20) NOT NULL DEFAULT 'UPSERT' COMMENT '第一批仅启用UPSERT，APPEND为后续多值标签预留';
```

约束：

- `tag_code + tag_type` 继续唯一。
- 第一批写入 CDP 标签时，使用 `tag_code` 作为业务主键。
- `manual_enabled = 0` 的标签不能在用户详情中人工写入，但画布节点和系统任务仍可按权限写入。
- 第一批统一按 `UPSERT` 写入当前态；`APPEND` 只作为后续多值标签能力的兼容字段，不在第一批开放配置。

### 新增表：`cdp_user_profile`

```sql
CREATE TABLE cdp_user_profile (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL COMMENT '系统内统一用户ID',
    display_name     VARCHAR(128) NULL,
    phone            VARCHAR(128) NULL,
    email            VARCHAR(256) NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    properties_json  JSON NULL COMMENT '轻量扩展属性',
    first_seen_at    DATETIME NULL,
    last_seen_at     DATETIME NULL,
    created_by       VARCHAR(64) NULL,
    created_at       DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_cdp_user_id (user_id),
    INDEX idx_last_seen (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户档案';
```

说明：

- `user_id` 先沿用画布执行中的 `canvas_execution.user_id`。
- `phone`、`email` 返回前端时必须脱敏。
- 第一批允许只存在 `user_id`，其他属性后续由事件和导入补齐。

### 新增表：`cdp_user_identity`

```sql
CREATE TABLE cdp_user_identity (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL,
    identity_type    VARCHAR(32) NOT NULL COMMENT 'USER_ID/PHONE/EMAIL/DEVICE_ID/OPEN_ID',
    identity_value   VARCHAR(256) NOT NULL,
    source_type      VARCHAR(32) NULL,
    source_ref_id    VARCHAR(128) NULL,
    verified         TINYINT NOT NULL DEFAULT 0,
    created_at       DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_identity (identity_type, identity_value),
    INDEX idx_user_identity (user_id, identity_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户身份映射';
```

说明：

- 第一批只自动写入 `USER_ID` 身份。
- 后续导入手机号、邮箱、设备 ID 时复用该表。

### 新增表：`cdp_user_tag`

```sql
CREATE TABLE cdp_user_tag (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL,
    tag_code         VARCHAR(64) NOT NULL,
    tag_value        VARCHAR(1000) NULL,
    value_type       VARCHAR(20) NOT NULL DEFAULT 'STRING',
    source_type      VARCHAR(32) NOT NULL COMMENT 'MANUAL/CANVAS/BATCH/RULE/API/IMPORT',
    source_ref_id    VARCHAR(128) NULL COMMENT 'executionId/nodeId/jobId等来源引用',
    status           VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    effective_at     DATETIME NULL,
    expires_at       DATETIME NULL,
    created_by       VARCHAR(64) NULL,
    created_at       DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_user_tag (user_id, tag_code),
    INDEX idx_tag_user (tag_code, user_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户当前标签';
```

说明：

- 第一批采用当前态表模型，一个用户同一个 `tag_code` 只有一条当前记录。
- 标签值变更通过 upsert 更新当前态，并写历史。
- `status = EXPIRED` 或 `REMOVED` 的记录不参与用户当前标签展示和人群圈选。

### 新增表：`cdp_user_tag_history`

```sql
CREATE TABLE cdp_user_tag_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL,
    tag_code         VARCHAR(64) NOT NULL,
    old_value        VARCHAR(1000) NULL,
    new_value        VARCHAR(1000) NULL,
    operation        VARCHAR(20) NOT NULL COMMENT 'SET/REMOVE/EXPIRE',
    source_type      VARCHAR(32) NOT NULL,
    source_ref_id    VARCHAR(128) NULL,
    idempotency_key  VARCHAR(256) NULL COMMENT '幂等键，画布来源使用executionId:nodeId:userId:tagCode',
    reason           VARCHAR(500) NULL,
    operator         VARCHAR(64) NULL,
    operated_at      DATETIME NULL,
    UNIQUE KEY uk_tag_history_idempotency (idempotency_key),
    INDEX idx_user_tag_history (user_id, tag_code, operated_at),
    INDEX idx_source_history (source_type, source_ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户标签历史';
```

### 新增表：`cdp_tag_operation`

```sql
CREATE TABLE cdp_tag_operation (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_type  VARCHAR(20) NOT NULL COMMENT 'BATCH_SET/BATCH_REMOVE',
    tag_code        VARCHAR(64) NOT NULL,
    tag_value       VARCHAR(1000) NULL,
    total_count     INT NOT NULL DEFAULT 0,
    success_count   INT NOT NULL DEFAULT 0,
    fail_count      INT NOT NULL DEFAULT 0,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_msg       VARCHAR(1000) NULL,
    created_by      VARCHAR(64) NULL,
    created_at      DATETIME NULL,
    updated_at      DATETIME NULL,
    INDEX idx_tag_operation_status (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP批量标签任务';
```

说明：

- 第一批批量打标输入用户 ID 列表。
- 后续可扩展为上传文件、从人群选择、从 SQL 结果写入。

### 后续表：`cdp_event`

第二批新增事件表：

```sql
CREATE TABLE cdp_event (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    event_id         VARCHAR(128) NOT NULL,
    user_id          VARCHAR(128) NOT NULL,
    event_code       VARCHAR(64) NOT NULL,
    event_time       DATETIME NOT NULL,
    properties_json  JSON NULL,
    source_type      VARCHAR(32) NOT NULL,
    source_ref_id    VARCHAR(128) NULL,
    received_at      DATETIME NULL,
    UNIQUE KEY uk_event_id (event_id),
    INDEX idx_user_event_time (user_id, event_time),
    INDEX idx_event_code_time (event_code, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP事件明细';
```

## 后端服务边界

### `CdpUserService`

职责：

- `ensureUser(userId, sourceType, sourceRefId)`：如果用户不存在则创建档案和 `USER_ID` 身份。
- 查询用户档案、身份、属性摘要。
- 更新 `first_seen_at`、`last_seen_at`。
- 对外返回脱敏后的用户信息。

依赖：

- `CdpUserProfileMapper`
- `CdpUserIdentityMapper`
- `DataMaskingUtil`

### `CdpTagService`

职责：

- 校验标签定义是否存在、启用、允许当前来源写入。
- 对单个用户 set/remove 标签。
- 写入当前态 `cdp_user_tag`。
- 写入历史 `cdp_user_tag_history`。
- 支持基于 `executionId + nodeId + userId + tagCode` 的幂等写入。

依赖：

- `TagDefinitionMapper`
- `CdpUserTagMapper`
- `CdpUserTagHistoryMapper`
- `CdpUserService`

### `CdpTagOperationService`

职责：

- 创建批量打标任务。
- 异步执行批量 set/remove。
- 记录成功、失败和任务状态。
- 第一批复用已有异步任务模式；如 `AsyncTaskService` 可用，则统一接入。

### `CanvasUserQueryService`

职责：

- 基于 `canvas_execution` 查询画布用户列表。
- 聚合每个用户在画布中的执行次数、成功次数、失败次数、最近执行时间、最近状态。
- 查询用户在某画布中的执行记录和最近轨迹。
- 关联 CDP 用户档案和当前标签。

说明：

- 第一批不新增画布用户汇总表，直接利用已有索引 `idx_canvas_user` 查询。
- 如果线上数据量大，后续增加 `canvas_user_journey_summary` 物化汇总表。

### `CdpEventService`

第二批引入，负责事件写入、去重、属性更新和事件查询。

## 后端接口设计

### 用户中心

```http
GET /cdp/users?page=1&size=20&keyword=&tagCode=&status=
GET /cdp/users/{userId}
GET /cdp/users/{userId}/tags
GET /cdp/users/{userId}/tag-history?page=1&size=20
POST /cdp/users/{userId}/tags
DELETE /cdp/users/{userId}/tags/{tagCode}
```

`POST /cdp/users/{userId}/tags` 请求：

```json
{
  "tagCode": "high_value",
  "tagValue": "true",
  "reason": "运营人工标记",
  "expiresAt": "2026-12-31T23:59:59"
}
```

### 批量打标签

```http
POST /cdp/tag-operations
GET /cdp/tag-operations/{id}
GET /cdp/tag-operations?page=1&size=20
```

`POST /cdp/tag-operations` 请求：

```json
{
  "operationType": "BATCH_SET",
  "tagCode": "campaign_target",
  "tagValue": "summer_2026",
  "userIds": ["u001", "u002", "u003"],
  "reason": "夏促目标用户"
}
```

### 画布用户数据

```http
GET /canvas/{id}/users?page=1&size=20&keyword=&status=&since=&until=&tagCode=
GET /canvas/{id}/users/{userId}
GET /canvas/{id}/users/{userId}/executions?page=1&size=20
GET /canvas/{id}/users/{userId}/latest-trace
```

列表返回核心字段：

```json
{
  "userId": "user_test_001",
  "displayName": "user_test_001",
  "executionCount": 6,
  "successCount": 5,
  "failedCount": 1,
  "latestStatus": "SUCCESS",
  "firstEnteredAt": "2026-05-01T10:00:00",
  "lastEnteredAt": "2026-05-23T15:00:00",
  "tags": [
    { "tagCode": "high_value", "tagName": "高价值用户", "tagValue": "true" }
  ]
}
```

### 标签定义扩展

现有接口保留：

```http
GET /canvas/tag-definitions
POST /canvas/tag-definitions
PUT /canvas/tag-definitions/{id}
DELETE /canvas/tag-definitions/{id}
```

请求和返回增加：

- `valueType`
- `manualEnabled`
- `defaultTtlDays`
- `category`
- `owner`
- `writePolicy`

## 画布节点设计

新增节点类型：`CDP_TAG_WRITE`

分类建议：`行为策略` 或 `用户运营`

节点配置：

```json
[
  {
    "key": "tagCode",
    "label": "标签",
    "type": "select",
    "dataSource": "/meta/tagger-tags",
    "required": true
  },
  {
    "key": "valueMode",
    "label": "标签值来源",
    "type": "radio",
    "required": true,
    "options": [
      { "label": "固定值", "value": "fixed" },
      { "label": "上下文字段", "value": "context" }
    ]
  },
  {
    "key": "tagValue",
    "label": "标签值",
    "type": "text",
    "showWhen": "valueMode==fixed"
  },
  {
    "key": "tagValueField",
    "label": "上下文字段",
    "type": "select",
    "dataSource": "/meta/context-fields",
    "showWhen": "valueMode==context"
  },
  {
    "key": "reason",
    "label": "原因",
    "type": "text"
  },
  {
    "key": "nextNodeId",
    "label": "下一节点",
    "type": "node-select"
  }
]
```

执行行为：

1. 从 `ExecutionContext.userId` 获取用户。
2. 调用 `CdpUserService.ensureUser`。
3. 根据固定值或上下文字段解析标签值。
4. 调用 `CdpTagService.setTag`，来源为 `CANVAS`。
5. `source_ref_id` 使用 `executionId:nodeId`。
6. `idempotency_key` 使用 `executionId:nodeId:userId:tagCode`。
7. 输出 `tagCode`、`tagValue`、`tagWriteStatus`。
8. 继续走 `nextNodeId`。

错误处理：

- 标签定义不存在或禁用：节点失败。
- 标签值类型不匹配：节点失败并写入 trace 错误。
- 重复执行同一 `executionId + nodeId + userId + tagCode`：视为幂等成功。

## 前端设计

### 侧边栏

新增“CDP 用户中心”入口。建议位于“自动化营销”分组下，而不是“系统设置”下，因为用户数据是运营工作台能力。

保留：

- 人群管理
- 标签配置
- 旅程管理

### 用户中心页 `/cdp/users`

页面结构：

- 顶部筛选：关键词、标签、状态、最近活跃时间。
- KPI 摘要：总用户数、活跃用户数、带标签用户数、最近新增。
- 用户表格：用户 ID、手机号/邮箱脱敏、标签数、最近活跃、最近旅程、状态。
- 操作：查看详情、打标签。

交互：

- 点击用户进入用户详情。
- 支持从用户列表批量选择用户并发起批量打标签。

### 用户详情页 `/cdp/users/:userId`

页面结构：

- 顶部用户摘要：用户 ID、身份、状态、首次出现、最近活跃。
- 标签面板：当前标签、标签值、来源、有效期、更新时间。
- 标签历史：操作、来源、操作者、前后值、时间。
- 旅程记录：进入过的画布、执行状态、最近执行时间。
- 后续阶段追加事件时间线、属性、归属人群。

操作：

- 添加标签。
- 移除标签。
- 查看某次画布执行轨迹。

### 画布用户数据页 `/canvas/:id/users`

入口：

- 画布列表页操作区增加“用户数据”。
- 画布编辑器工具栏增加“用户”按钮。
- 画布统计页增加“用户明细”入口。

页面结构：

- 顶部返回和画布名称。
- 日期范围、执行状态、标签、关键词筛选。
- KPI：进入用户数、成功用户数、失败用户数、平均执行次数。
- 表格：用户 ID、当前标签、执行次数、成功次数、失败次数、最近状态、最近进入时间。
- 右侧抽屉：用户在该画布下的执行记录、最近轨迹、节点输出、打标签操作。

### 标签配置页 `/tag-config`

扩展字段：

- 标签值类型。
- 是否允许人工打标。
- 默认有效期。
- 标签分类。
- 负责人。
- 写入策略。

列表新增列：

- 值类型。
- 人工打标。
- 默认有效期。
- 当前用户数。

`当前用户数` 可第一批通过 `cdp_user_tag` 按 `tag_code` 聚合查询，后续可物化。

## 数据流

### 画布执行进入 CDP

1. 画布触发时已有 `userId`。
2. 执行开始时调用 `CdpUserService.ensureUser(userId, "CANVAS_EXECUTION", executionId)`。
3. `canvas_execution` 按现有流程写入。
4. 画布用户页从 `canvas_execution` 聚合用户执行情况。
5. 用户详情页通过 `userId` 关联 CDP 标签和画布执行历史。

### 手动打标签

1. 前端在用户详情或画布用户抽屉提交打标签请求。
2. 后端校验用户、标签定义、权限和值类型。
3. `cdp_user_tag` upsert 当前态。
4. `cdp_user_tag_history` 写入历史。
5. 前端刷新用户当前标签和历史。

### 画布自动打标签

1. 用户执行到 `CDP_TAG_WRITE` 节点。
2. 节点从配置和上下文解析标签值。
3. 后端写入用户标签。
4. 节点 trace 记录输出。
5. 后续节点可继续执行。

### 人群联动

第一批保留现有 `audience_definition` 结构。后续在人群编辑页的数据源里增加 `CDP_TAG` 或 `CDP_USER` 类型：

- 基于 `cdp_user_tag` 做标签圈选。
- 基于 `cdp_user_profile.properties_json` 做属性圈选。
- 计算结果仍写入现有 `audience_stat` 和 Redis bitmap。

## 权限和审计

角色策略：

- `OPERATOR`：查看用户中心、画布用户数据、用户详情。
- `ADMIN`：维护标签定义、手动打标签、批量打标签、配置画布打标签节点。

审计：

- 所有标签变更必须写入 `cdp_user_tag_history`。
- 标签历史记录操作者、来源、原因和前后值。
- 批量任务记录创建人、任务状态、成功失败数量。

隐私：

- 手机号、邮箱等敏感字段通过 `DataMaskingUtil` 返回脱敏值。
- 第一批不在前端展示未脱敏敏感字段。
- 后续治理阶段再增加字段级权限。

## 错误处理

1. 用户不存在
   - 读详情返回 404。
   - 打标签时可根据 `userId` 自动 `ensureUser`，但返回中标注该用户为新建档案。

2. 标签不存在或禁用
   - 返回业务错误，不写入当前态和历史。

3. 标签值类型错误
   - 后端按 `value_type` 校验，返回明确错误。

4. 批量任务部分失败
   - 任务整体完成但 `fail_count > 0`。
   - 每个失败用户的错误进入任务明细日志；第一批可存入 `error_msg` 汇总，后续拆任务明细表。

5. 画布节点写标签失败
   - 节点失败，trace 写错误。
   - 整体画布按现有 DAG 错误语义处理。

6. 旧执行记录没有用户档案
   - 画布用户列表仍展示 `canvas_execution.user_id`。
   - 打开详情或执行 backfill 后创建档案。

## 性能设计

第一批依赖以下索引：

- `canvas_execution(canvas_id, user_id, created_at)` 已存在。
- `cdp_user_profile(user_id)`。
- `cdp_user_identity(identity_type, identity_value)`。
- `cdp_user_tag(user_id, tag_code)`。
- `cdp_user_tag(tag_code, user_id)`。
- `cdp_user_tag_history(user_id, tag_code, operated_at)`。
- `cdp_user_tag_history(idempotency_key)`。

查询策略：

- 画布用户列表只聚合 `canvas_execution`，不展开 trace。
- 用户详情按需查询某次执行轨迹。
- 当前标签批量查询时按 userId 集合一次性加载。
- 标签历史分页查询。

后续优化：

- 当画布执行量增长到列表查询明显变慢时，增加 `canvas_user_journey_summary`。
- 当标签计数变慢时，增加标签统计表或异步聚合任务。

## 迁移和兼容

Flyway 迁移：

1. 扩展 `tag_definition`。
2. 新建 `cdp_user_profile`。
3. 新建 `cdp_user_identity`。
4. 新建 `cdp_user_tag`。
5. 新建 `cdp_user_tag_history`。
6. 新建 `cdp_tag_operation`。
7. 新增 `CDP_TAG_WRITE` 节点类型。

数据回填：

- 从历史 `canvas_execution.user_id` 中去重生成 `cdp_user_profile`。
- 为每个用户生成 `USER_ID` 身份。
- 示例数据可从现有 demo execution 和 demo audience 生成少量 CDP 用户。

兼容要求：

- 不删除或重命名现有接口。
- `tag_definition` 旧数据默认 `value_type = STRING`、`manual_enabled = 1`、`write_policy = UPSERT`。
- 现有 `TAGGER` 节点语义不变。
- 现有 `/canvas/audiences/ready` 继续服务 TAGGER audience 模式。

## 测试计划

### 后端单元测试

- `CdpUserServiceTest`
  - `ensureUser` 首次创建。
  - `ensureUser` 重复调用幂等。
  - 敏感字段脱敏。

- `CdpTagServiceTest`
  - 手动 set 标签。
  - remove 标签。
  - 标签值类型校验。
  - 禁用标签拒绝写入。
  - upsert 当前态并写历史。
  - canvas 来源幂等写入。

- `CanvasUserQueryServiceTest`
  - 按画布聚合用户执行次数。
  - 状态、时间、关键词过滤。
  - 批量关联当前标签。

- `CdpTagWriteHandlerTest`
  - 固定值写标签。
  - 上下文字段写标签。
  - 标签不存在时节点失败。
  - 写入成功后返回 nextNodeId。

### 后端控制器测试

- `CdpUserControllerTest`
- `CdpTagOperationControllerTest`
- `CanvasUserControllerTest`
- `TagDefinitionControllerTest` 扩展字段兼容。

### 前端测试

- 用户中心列表渲染和筛选。
- 用户详情标签新增、移除交互。
- 画布用户数据表格和抽屉。
- 标签配置扩展字段。
- `CDP_TAG_WRITE` 节点配置 schema 展示和保存。

### 验证命令

后端：

```bash
cd backend/canvas-engine
mvn test
```

前端：

```bash
cd frontend
npm test
npm run build
```

## 上线顺序

1. 合入数据库迁移和后端 CDP domain/service/mapper。
2. 合入用户中心和标签实例接口。
3. 合入画布用户数据接口。
4. 合入前端用户中心、用户详情、画布用户数据页。
5. 合入标签配置扩展字段。
6. 合入 `CDP_TAG_WRITE` 节点类型和 handler。
7. 回填历史执行用户。
8. 执行回归测试。

## 风险和约束

1. 当前工作区已有未解决冲突，实施前必须先恢复到可构建状态。
2. 第一批直接聚合 `canvas_execution`，大数据量下可能需要物化汇总。
3. 标签当前态采用 `user_id + tag_code` 唯一，暂不支持同一标签多值并存。
4. 第一批 ID Mapping 只做存储，不做复杂合并规则。
5. 敏感字段治理第一批只做脱敏展示，字段级权限后续补齐。

## 成功标准

第一批上线完成后：

1. 可以在 CDP 用户中心搜索和查看用户。
2. 可以在画布下查看进入过的用户、执行次数、最近状态和当前标签。
3. 可以查看单个用户在画布下的执行记录和节点轨迹。
4. 可以对单个用户手动打标签并查看历史。
5. 可以对一批用户提交批量打标签任务。
6. 可以在画布中配置节点自动给经过的用户写入标签。
7. 现有画布、人群、标签定义、TAGGER audience 模式不发生回归。
