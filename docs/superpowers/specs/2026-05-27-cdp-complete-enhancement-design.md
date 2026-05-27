# CDP 完整化增强设计

## 背景

当前系统已经完成 CDP 第一批核心闭环：

- 用户中心、用户详情、画布用户数据页已经存在。
- `cdp_user_profile`、`cdp_user_identity`、`cdp_user_tag`、`cdp_user_tag_history`、`cdp_tag_operation` 已经落库。
- 手动打标、批量打标、标签导入、画布 `CDP_TAG_WRITE` 节点已经具备基础能力。
- 人群计算已有 `JDBC` 和 `TAGGER_API` 路径，结果写入 Redis bitmap 和 `audience_stat`，画布 TAGGER audience 模式可复用。

当前缺口是：CDP 仍偏向“用户标签台账 + 画布用户明细”，还没有成为营销画布和人群圈选的统一用户事实层。标签能写入 CDP，但 CDP 标签、属性和身份尚不能直接驱动人群；用户详情还不是完整 360 画像；批量任务、权限、审计和性能能力也需要补齐。

## 目标

1. 打通 CDP 数据驱动人群和画布的运营闭环。
2. 将用户详情升级为用户 360 画像，展示身份、属性、标签、事件、人群和旅程。
3. 增强标签治理、批量任务可靠性、操作审计和权限边界。
4. 提升用户列表、画布用户列表和标签统计在数据量增长后的查询性能。
5. 保持现有画布、TAGGER audience、人群 bitmap、标签中心和导入能力兼容。
6. 后续实施必须使用独立 git worktree，避免污染当前工作区已有改动。

## 非目标

1. 本轮不接入外部实时埋点 SDK。
2. 本轮不做复杂 ID 合并审批流和冲突裁决 UI。
3. 本轮不建设完整数据血缘平台。
4. 本轮不替换现有 `JDBC`、`TAGGER_API` 人群计算路径。
5. 本轮不重写画布 DAG 执行引擎。

## 方案选型

### 方案 A：一次性全量实现

一次性完成 CDP 人群、画像、治理、权限、性能和洞察。

优点：

- 目标完整。
- 所有模块可以一次性按最终形态设计。

缺点：

- 改动面过大。
- 测试和回归成本高。
- 业务价值要等全部完成后才能验证。

### 方案 B：一份总设计，四个可交付阶段

先形成完整 CDP 目标设计，再按依赖顺序拆成四个阶段：

1. CDP 人群闭环。
2. 用户 360 和事件属性。
3. 治理和任务可靠性。
4. 性能和洞察。

优点：

- 每阶段都有可验证产出。
- 变更面可控。
- 可以优先补齐“用 CDP 数据驱动画布”的核心价值。
- 容易通过 git worktree 分支独立实施和评审。

缺点：

- 需要阶段间保持数据模型和接口兼容。
- 部分页面会经历渐进增强。

### 方案 C：先治理后业务

先补权限、审计、任务可靠性，再做人群和画像。

优点：

- 风险最低。
- 平台基础更稳。

缺点：

- 短期运营价值不明显。
- CDP 仍然停留在数据查看和打标层。

### 结论

采用方案 B。当前系统已经能写标签和看用户，最关键的下一步是让 CDP 标签、属性和身份进入人群计算，再复用现有 bitmap 和 TAGGER audience 模式驱动画布。

## 总体架构

CDP 完整化拆成四个子域。

### CDP 数据域

继续以现有表为核心：

- `cdp_user_profile`
- `cdp_user_identity`
- `cdp_user_tag`
- `cdp_user_tag_history`
- `tag_definition`
- `tag_value_definition`

新增事件、属性历史、批量任务明细和操作审计。CDP 数据域负责沉淀用户事实，不直接承载画布执行逻辑。

### 人群计算域

在现有 `AudienceBatchComputeService` 上扩展 CDP 数据源：

- `CDP_TAG`
- `CDP_PROFILE`
- `CDP_IDENTITY`

计算结果仍写入 `AudienceBitmapStore` 和 `audience_stat`。现有 TAGGER audience 节点继续读取 Redis bitmap，不需要重写。

### 画像查询域

新增聚合服务，统一返回用户 360 画像：

- 基础档案
- 身份列表
- 当前标签
- 标签历史
- 属性快照
- 属性历史
- 事件时间线
- 归属人群
- 画布旅程
- 操作审计

前端不直接拼接多个低层接口，避免页面逻辑扩散。

### 治理与任务域

批量打标从“任务主表 + 字符串错误摘要”升级为“任务主表 + 任务明细表”。人工打标、批量打标、导入、画布写标签和删除标签统一写审计记录。

## 阶段划分

### 阶段 1：CDP 人群闭环

目标：运营能直接用 CDP 标签、属性、身份圈选人群，并驱动画布。

范围：

- 新增 `CdpAudienceSourceService`。
- 扩展 `AudienceBatchComputeService` 支持 `CDP_TAG`、`CDP_PROFILE`、`CDP_IDENTITY`。
- 人群编辑页新增 CDP 数据源。
- 人群规则字段来自标签中心、用户属性和身份类型。
- 增加人群预览接口。
- 继续写现有 Redis bitmap 和 `audience_stat`。

验收：

- 用 `cdp_user_tag` 可以计算 READY 人群。
- TAGGER audience 模式可以消费该人群。
- 现有 `JDBC` 和 `TAGGER_API` 人群不受影响。

### 阶段 2：用户 360 和事件属性

目标：用户详情成为完整画像页。

范围：

- 新增 `cdp_event`。
- 新增 `cdp_user_property_history`。
- 新增 `CdpEventService`。
- 新增 `CdpUserProfileViewService`。
- 用户详情页升级为多 Tab：概览、标签、事件、属性、人群、旅程、审计。
- 事件写入更新用户 `first_seen_at`、`last_seen_at`，并按需更新 `properties_json`。

验收：

- 用户事件可以幂等写入。
- 用户画像页可以展示事件时间线和属性变更。
- 无事件或无属性时页面显示空态，不影响已有标签和旅程展示。

### 阶段 3：治理和任务可靠性

目标：批量打标、审计、权限和标签值校验达到可运营水平。

范围：

- 新增 `cdp_tag_operation_item`。
- 升级 `CdpTagOperationService`，按用户记录处理结果。
- 支持失败明细分页、失败重试和失败用户导出。
- 新增 `cdp_operation_audit`。
- 新增 `CdpAuditService`。
- 标签写入补充 JSON 严格校验。
- 打标弹窗联动标签中心和标签值字典。
- 写操作接入角色权限，敏感字段继续脱敏展示。

验收：

- 批量任务部分失败后可以看到每个失败用户和原因。
- 重试只处理失败用户。
- 人工、批量、导入、画布写标签都有审计记录。
- JSON 标签值非法时后端拒绝写入。

### 阶段 4：性能和洞察

目标：支撑更大的用户和执行数据量，并提供运营洞察。

范围：

- `/cdp/users` 支持分页和筛选：关键词、标签、状态、最近活跃时间。
- 当前标签批量加载，避免用户列表逐行查询。
- 标签中心展示当前用户数和近 7 天新增打标数。
- 画布用户页增加 KPI 和筛选。
- 可选新增 `canvas_user_journey_summary` 物化表。
- 用户中心增加总用户数、活跃用户、带标签用户、新增用户摘要。

验收：

- 用户中心不再全量拉取。
- 画布用户列表可以按状态、标签和时间过滤。
- 标签中心可查看覆盖人数。
- 大数据量下可以切换到物化汇总表。

## 数据模型设计

### 新增表：`cdp_event`

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
    UNIQUE KEY uk_cdp_event_id (event_id),
    INDEX idx_cdp_event_user_time (user_id, event_time),
    INDEX idx_cdp_event_code_time (event_code, event_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP事件明细';
```

说明：

- `event_id` 用于幂等。
- `event_time` 是业务发生时间。
- `received_at` 是系统接收时间。

### 新增表：`cdp_user_property_history`

```sql
CREATE TABLE cdp_user_property_history (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id          VARCHAR(128) NOT NULL,
    property_key     VARCHAR(128) NOT NULL,
    old_value        VARCHAR(2000) NULL,
    new_value        VARCHAR(2000) NULL,
    source_type      VARCHAR(32) NOT NULL,
    source_ref_id    VARCHAR(128) NULL,
    changed_at       DATETIME NOT NULL,
    INDEX idx_cdp_property_user_key_time (user_id, property_key, changed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP用户属性历史';
```

说明：

- 当前属性继续存 `cdp_user_profile.properties_json`。
- 历史变化单独追踪。

### 新增表：`cdp_tag_operation_item`

```sql
CREATE TABLE cdp_tag_operation_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    operation_id    BIGINT NOT NULL,
    user_id          VARCHAR(128) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_msg        VARCHAR(1000) NULL,
    processed_at     DATETIME NULL,
    created_at       DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_cdp_tag_op_item_user (operation_id, user_id),
    INDEX idx_cdp_tag_op_item_status (operation_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP批量标签任务明细';
```

说明：

- `cdp_tag_operation.error_msg` 保留兼容。
- 新失败写入明细表。
- 重试失败用户从明细表读取。

### 新增表：`cdp_operation_audit`

```sql
CREATE TABLE cdp_operation_audit (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_type         VARCHAR(64) NOT NULL,
    biz_id           VARCHAR(128) NULL,
    operation        VARCHAR(64) NOT NULL,
    operator         VARCHAR(64) NULL,
    source_type      VARCHAR(32) NULL,
    source_ref_id    VARCHAR(128) NULL,
    before_json      JSON NULL,
    after_json       JSON NULL,
    reason           VARCHAR(500) NULL,
    created_at       DATETIME NULL,
    INDEX idx_cdp_audit_biz (biz_type, biz_id, created_at),
    INDEX idx_cdp_audit_operator (operator, created_at),
    INDEX idx_cdp_audit_source (source_type, source_ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='CDP操作审计';
```

说明：

- 审计表记录业务变更，不替代 `cdp_user_tag_history`。
- 标签历史仍是用户标签事实变更历史。

### 扩展语义：`audience_definition`

优先不新增字段，通过 `data_source_type` 和 `rule_json` 增加 CDP 语义。

新增 `data_source_type` 值：

- `CDP_TAG`
- `CDP_PROFILE`
- `CDP_IDENTITY`

示例：

```json
{
  "logic": "AND",
  "conditions": [
    { "field": "high_value", "op": "=", "value": "VIP" },
    { "field": "churn_risk", "op": "IN", "value": ["HIGH", "MEDIUM"] }
  ]
}
```

### 可选物化表：`canvas_user_journey_summary`

第四阶段按性能需要引入。

```sql
CREATE TABLE canvas_user_journey_summary (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    canvas_id        BIGINT NOT NULL,
    user_id          VARCHAR(128) NOT NULL,
    execution_count  BIGINT NOT NULL DEFAULT 0,
    success_count    BIGINT NOT NULL DEFAULT 0,
    failed_count     BIGINT NOT NULL DEFAULT 0,
    latest_status    VARCHAR(32) NULL,
    first_entered_at DATETIME NULL,
    last_entered_at  DATETIME NULL,
    updated_at       DATETIME NULL,
    UNIQUE KEY uk_canvas_user_summary (canvas_id, user_id),
    INDEX idx_canvas_user_summary_last (canvas_id, last_entered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='画布用户旅程汇总';
```

## 后端服务设计

### `CdpAudienceSourceService`

职责：

- 解析 CDP 人群规则。
- 从 `cdp_user_tag`、`cdp_user_profile`、`cdp_user_identity` 查询命中用户。
- 输出用户 ID 批次给人群 bitmap 写入逻辑。
- 为前端提供可选字段元数据。

依赖：

- `CdpUserTagMapper`
- `CdpUserProfileMapper`
- `CdpUserIdentityMapper`
- `TagDefinitionMapper`
- `IdentityTypeMapper`

### `AudienceBatchComputeService`

调整：

- 保留 `JDBC` 和 `TAGGER_API`。
- 增加 `CDP_TAG`、`CDP_PROFILE`、`CDP_IDENTITY`。
- CDP 数据源计算完成后仍调用 `AudienceBitmapStore.save`。
- 失败时继续更新 `audience_stat` 为 `FAILED`。

### `CdpUserProfileViewService`

职责：

- 聚合用户 360 画像。
- 返回前端详情页需要的完整视图。
- 对敏感字段只返回脱敏值。

### `CdpEventService`

职责：

- 单条和批量写入事件。
- 基于 `event_id` 幂等。
- 确保用户档案存在。
- 更新 `first_seen_at`、`last_seen_at`。
- 按配置更新 `properties_json`。
- 写入属性历史。

### `CdpTagOperationService`

调整：

- 创建任务时生成 `cdp_tag_operation_item`。
- 执行时逐条更新 item 状态。
- 任务主表汇总总数、成功数、失败数和最终状态。
- `retryFailed` 只读取失败 item。

### `CdpAuditService`

职责：

- 为 CDP 写操作提供统一审计入口。
- 支持标签写入、标签移除、批量任务、标签导入、画布写标签、敏感操作。
- 审计失败不应吞掉主业务异常；如果审计失败，要记录日志并按配置决定是否阻断。

## 后端接口设计

### 用户中心

```http
GET /cdp/users?page=1&size=20&keyword=&tagCode=&status=&activeSince=&activeUntil=
GET /cdp/users/{userId}/profile-view
GET /cdp/users/{userId}/events?page=1&size=20&eventCode=
GET /cdp/users/{userId}/properties/history
```

### 事件

```http
POST /cdp/events
POST /cdp/events/batch
```

单条事件请求：

```json
{
  "eventId": "evt-001",
  "userId": "u001",
  "eventCode": "purchase",
  "eventTime": "2026-05-27T10:00:00",
  "properties": {
    "amount": 99.9,
    "city": "Shanghai"
  },
  "sourceType": "API"
}
```

### 批量打标任务

```http
POST /cdp/tag-operations
GET /cdp/tag-operations?page=1&size=20&status=
GET /cdp/tag-operations/{id}
GET /cdp/tag-operations/{id}/items?page=1&size=50&status=FAILED
POST /cdp/tag-operations/{id}/retry-failed
```

### 审计

```http
GET /cdp/audits?page=1&size=20&bizType=&operator=&sourceType=
```

### 人群

```http
GET /canvas/audiences/source-fields?dataSourceType=CDP_TAG
GET /canvas/audiences/source-fields?dataSourceType=CDP_PROFILE
GET /canvas/audiences/source-fields?dataSourceType=CDP_IDENTITY
POST /canvas/audiences/{id}/preview
```

预览返回：

```json
{
  "estimatedSize": 1234,
  "sampleUserIds": ["u001", "u002", "u003"]
}
```

## 前端设计

### CDP 用户中心 `/cdp/users`

增强：

- 分页。
- 关键词、标签、状态、最近活跃时间筛选。
- 摘要指标：总用户数、近 7 天活跃、带标签用户数、新增用户。
- 后端聚合返回标签，避免前端逐用户散查。
- 支持勾选用户后批量打标。

### 用户 360 `/cdp/users/:userId`

页面改为多 Tab：

- `概览`：基础档案、身份、关键属性、当前标签、最近事件、最近旅程。
- `标签`：当前标签、来源、有效期、更新时间；支持打标和移除。
- `事件`：事件时间线，支持按事件编码筛选。
- `属性`：属性快照和属性变更历史。
- `人群`：当前归属人群、最近计算时间、规模。
- `旅程`：进入过的画布、执行记录、节点轨迹入口。
- `审计`：用户相关操作记录。

### 人群编辑页

新增数据源：

- CDP 标签
- CDP 用户属性
- CDP 身份

交互：

- 选择 CDP 标签时，字段来自标签中心。
- 标签值优先使用标签值字典。
- 选择 CDP 用户属性时，字段来自已知属性 key。
- 选择 CDP 身份时，字段来自身份类型配置。
- 增加预估或预览按钮。

### 批量打标任务

增强：

- 显示任务进度、成功数、失败数。
- 失败明细分页表。
- 只重试失败用户。
- 复制或导出失败用户 ID。
- 标签编码使用下拉选择。
- 标签值使用字典选择或输入。

### 标签中心 `/tag-config`

增强：

- 展示当前用户数。
- 展示近 7 天新增打标数。
- JSON 类型标签增加格式提示。
- 标签值字典用于打标弹窗、人群规则值选择和导入校验。

### 画布用户数据 `/canvas/:id/users`

增强：

- 执行状态、标签、进入时间筛选。
- KPI：进入用户数、成功用户数、失败用户数、平均执行次数。
- 抽屉展示最近节点轨迹、上下文输出、用户当前标签和快捷打标。
- 支持跳转用户 360。

## 错误处理

1. CDP 人群规则字段不存在：
   - 保存或预览时报配置错误。
   - 计算时报 `FAILED` 并写入 `audience_stat.error_msg`。

2. 事件重复：
   - 基于 `event_id` 幂等忽略。

3. 标签值类型错误：
   - `STRING` 允许普通文本。
   - `NUMBER` 必须可解析为数字。
   - `BOOLEAN` 只允许 `true` 或 `false`。
   - `JSON` 必须通过 JSON 解析。

4. 批量任务部分失败：
   - 主任务状态为 `PARTIAL_FAILED`。
   - 每个失败用户写入 `cdp_tag_operation_item.error_msg`。

5. 权限不足：
   - 写操作拒绝。
   - 读敏感字段继续脱敏。

6. 审计失败：
   - 默认记录日志并继续主流程。
   - 后续可通过配置切换为强一致阻断。

## 权限和审计

角色建议：

- `OPERATOR`：查看用户中心、画像、画布用户数据、人群结果。
- `ADMIN`：维护标签定义、打标、批量打标、配置 CDP 写标签节点、查看审计。

审计要求：

- 所有标签写入和移除必须写 `cdp_user_tag_history`。
- 人工、批量、导入、画布写标签额外写 `cdp_operation_audit`。
- 批量任务记录创建人、任务状态、成功失败数量和失败明细。

隐私要求：

- 手机号、邮箱等敏感字段继续脱敏返回。
- 第一阶段不展示未脱敏敏感字段。
- 字段级权限作为治理阶段内容。

## 性能设计

第一阶段优先避免引入物化表，沿用现有查询和索引。第四阶段再按数据量引入物化。

策略：

- `/cdp/users` 必须分页。
- 用户列表当前标签由后端批量查询。
- 标签覆盖人数可先按 `cdp_user_tag` 聚合，后续物化。
- 画布用户页短期继续聚合 `canvas_execution`。
- 大数据量下新增 `canvas_user_journey_summary`。

关键索引：

- `cdp_user_tag(tag_code, user_id)`
- `cdp_user_tag(user_id, status)`
- `cdp_user_identity(identity_type, identity_value)`
- `cdp_event(user_id, event_time)`
- `cdp_event(event_code, event_time)`
- `cdp_tag_operation_item(operation_id, status)`

## 迁移计划

按阶段新增 Flyway migration。

### `V77__cdp_event_and_property_history.sql`

- `cdp_event`
- `cdp_user_property_history`

### `V78__cdp_tag_operation_items.sql`

- `cdp_tag_operation_item`
- `cdp_tag_operation` 必要索引

### `V79__cdp_operation_audit.sql`

- `cdp_operation_audit`

### `V80__canvas_user_journey_summary.sql`

- `canvas_user_journey_summary`
- 第四阶段按性能需要落地

迁移约束：

- 不删除现有表字段。
- `cdp_tag_operation.error_msg` 保留兼容。
- 新失败明细写入 `cdp_tag_operation_item`。
- 画像聚合读取现有数据，事件和属性为空时返回空列表。

## 测试计划

### 后端单元测试

- `CdpAudienceSourceServiceTest`
- `AudienceBatchComputeServiceCdpSourceTest`
- `CdpEventServiceTest`
- `CdpUserProfileViewServiceTest`
- `CdpTagOperationItemServiceTest`
- `CdpAuditServiceTest`
- `CdpTagServiceJsonValidationTest`

### 后端接口测试

- `/cdp/users` 分页和筛选。
- `/cdp/users/{userId}/profile-view` 聚合结果。
- `/cdp/events` 和 `/cdp/events/batch` 幂等写入。
- `/cdp/tag-operations/{id}/items` 失败明细。
- `/canvas/audiences/{id}/preview`。
- CDP 人群计算结果生成 bitmap。

### 前端测试

- 用户中心筛选参数构造。
- 用户 360 展示 helper。
- 人群规则 builder 的 CDP 字段映射。
- 批量任务进度和失败明细展示。
- 标签值字典在打标弹窗中的 payload 转换。

### 回归测试

- 现有 `JDBC` 人群计算。
- 现有 `TAGGER_API` 人群计算。
- 现有 TAGGER audience 节点。
- 现有手动打标。
- 现有标签导入。
- 现有画布 `CDP_TAG_WRITE`。
- 画布执行入口 `ensureUser`。

## git worktree 实施策略

后续实施必须使用独立 git worktree，避免污染当前工作区已有未提交改动。

建议命令：

```bash
git worktree add ../canvas-cdp-complete -b feat/cdp-complete
```

实施要求：

- 在 `../canvas-cdp-complete` 内执行实现。
- 每个阶段可以拆独立提交。
- 当前工作区已有未提交改动不得回退、覆盖或纳入阶段实现提交。
- 如果 worktree 创建时目标分支已存在，改用 `git worktree add ../canvas-cdp-complete feat/cdp-complete`。

## 验收标准

完整 CDP 增强完成后应满足：

1. 运营可以用 CDP 标签、属性、身份创建人群。
2. CDP 人群可以生成 READY bitmap，并被画布 TAGGER audience 模式消费。
3. 用户 360 展示档案、身份、标签、事件、属性、人群、旅程和审计。
4. 批量打标有明细、有失败重试、有失败用户导出。
5. 所有标签写入路径都有历史和审计。
6. 用户中心和画布用户页支持分页筛选。
7. 现有画布、人群、标签导入和 CDP 写标签节点兼容。

## 风险和缓解

1. CDP 人群查询性能不足：
   - 第一阶段限制预览样本和最大扫描量。
   - 第四阶段引入物化和统计表。

2. JSON 属性规则复杂：
   - 第一阶段只支持简单 key 和基础操作符。
   - 复杂 JSONPath 规则后续扩展。

3. 批量任务和审计增加写放大：
   - 明细表按任务和状态建索引。
   - 审计失败默认不阻断主流程。

4. 前端页面一次性变复杂：
   - 用户 360 按 Tab 渐进加载。
   - 先实现核心概览和标签，再补事件、属性、人群、审计。

5. 当前仓库测试存在非 CDP 构造器编译问题：
   - 实施阶段先在 worktree 修复或隔离相关测试，再验证 CDP 新增测试。
