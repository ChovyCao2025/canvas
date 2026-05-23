# 标签中心与历史标签导入设计

Spec ID: `TAG-CENTER-IMPORT`

## 背景

当前项目已经支持配置 Tagger 标签，但能力停留在 `tag_definition` 级别：只能维护标签编码、名称、类型和启停状态，不能维护标签值字典。画布 `TAGGER` 节点选择标签后，也不能继续配置标签值。

本次需求要补齐三块能力：

1. 标签定义下可维护标签值。
2. API 推送、后台主动拉取、Excel 导入都能导入历史标签数据，并写入库中。
3. 导入数据不锁死在单一 `userId` 字段上，需要参考 Quick Audience 的 ID 类型管理思路，提供独立 ID 类型配置，让接口文档和导入模板使用 `idType + idValue`。

项目尚未上线，本设计不做旧数据兼容迁就。可以直接调整现有标签模型、节点配置 schema 和管理页面结构。

## 目标

1. 新增独立“ID 类型配置”页面，维护导入数据可使用的身份字段。
2. 将现有“标签配置”升级为标签中心，支持标签定义和标签值字典管理。
3. 提供统一历史标签导入管道，覆盖 API 推送、外部 API 拉取、Excel 导入。
4. 将导入结果落库为可查询的用户标签当前值，并保留导入批次、导入来源、失败行原因。
5. 画布 `TAGGER` 节点在选择标签后，可以联动选择标签值。
6. 接口文档、Excel 模板和拉取配置使用统一字段契约：`idType`、`idValue`、`tagCode`、`tagValue`。

## 非目标

1. 本次不实现完整 ID Mapping 合并引擎，不把手机号、OpenID、会员号自动归并成 OneID。
2. 本次不做旧库数据迁移保护；Flyway 可以直接新增新表并调整现有 seed/schema。
3. 本次不实现大型离线任务中心，只提供导入批次状态和错误行查询。
4. 本次不接入真实外部 Tagger 服务认证协议，拉取接口先支持通用 HTTP 配置。

## 方案选型

### 方案 A：标签中心 + ID 类型配置 + 统一导入管道

做法：
- 新增 `identity_type`、`tag_value_definition`、`user_tag_current`、`tag_import_batch`、`tag_import_error`。
- 三种导入入口都先转换成统一 `TagImportRow`，再由同一个服务校验和落库。
- `TAGGER` 节点 schema 增加 `tagValue` 字段，前端按 `tagCodeKey` 联动加载标签值。

优点：
- 模型边界清楚，后续扩展 ID Mapping、审计、任务调度都顺。
- API、Excel、拉取导入行为一致。
- 能完整解决“能配标签但不能配标签值”的问题。

缺点：
- 表、接口、前端页面和节点 schema 都要调整。

### 方案 B：在现有 `tag_definition` 上快速扩列

做法：
- 在现有表上加值相关字段，再补一个用户标签表。
- 三种导入入口各自写入。

优点：
- 短期改动少。

缺点：
- 导入校验、错误处理和来源追踪容易分散。
- ID 类型配置会被迫混进标签配置页，边界不清。

### 方案 C：只做导入落库，不改节点配置

做法：
- 只新增历史标签数据导入和查询。
- 画布节点继续只选标签，不选标签值。

优点：
- 范围最小。

缺点：
- 不解决原始诉求，业务配置闭环不完整。

### 结论

采用方案 A。项目未上线，可以直接把标签能力整理成清晰的数据接入和标签中心模型，而不是在旧单表上继续补丁式扩展。

## 信息架构

### ID 类型配置页

新增独立页面，不放在标签配置页中。它属于数据接入基础配置，供标签导入、人群导入和后续 ID Mapping 复用。

字段：
- `code`：身份类型编码，如 `user_id`、`mobile`、`open_id`、`email`、`member_no`
- `name`：显示名称
- `description`：说明
- `enabled`：是否启用
- `allowImport`：是否允许作为导入身份字段
- `multiValue`：同一用户是否允许多个该类型 ID
- `priority`：后续映射优先级
- `participateMapping`：是否参与后续 ID Mapping，先预留

导入时只能使用 `enabled = 1` 且 `allowImport = 1` 的 ID 类型。接口文档和 Excel 模板都引用这里维护的 `code`。

### 标签配置页

现有“标签配置”升级为“标签中心”，仍作为一个页面入口，内部按标签和标签值组织。

标签定义字段：
- `name`
- `tagCode`
- `tagType`：`offline`、`realtime`
- `valueType`：`STRING`、`NUMBER`、`BOOLEAN`
- `description`
- `enabled`

标签值字段：
- `tagCode`
- `value`
- `label`
- `sortOrder`
- `enabled`
- `description`

标签值可以在标签编辑抽屉中维护，也可以通过导入历史标签时自动补齐。自动补齐的值默认启用，并标记来源为 `IMPORT_AUTO_DISCOVERED`。

### 历史标签导入页

新增“标签导入”页面，负责三类入口：

1. API 推送文档：展示请求格式、字段说明和示例。
2. 外部 API 拉取配置：配置远端 URL、方法、分页参数、字段映射和定时/手动拉取。
3. Excel 导入：下载模板、上传文件、查看批次结果。

导入页不直接维护标签定义和 ID 类型，只引用已启用配置。

## 数据模型设计

### `identity_type`

```sql
CREATE TABLE identity_type (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  code                VARCHAR(64) NOT NULL,
  name                VARCHAR(100) NOT NULL,
  description         VARCHAR(500) NULL,
  enabled             TINYINT NOT NULL DEFAULT 1,
  allow_import        TINYINT NOT NULL DEFAULT 1,
  multi_value         TINYINT NOT NULL DEFAULT 0,
  priority            INT NOT NULL DEFAULT 100,
  participate_mapping TINYINT NOT NULL DEFAULT 0,
  created_by          VARCHAR(64) NULL,
  created_at          DATETIME NULL,
  updated_at          DATETIME NULL,
  UNIQUE KEY uk_identity_type_code (code),
  INDEX idx_identity_type_enabled (enabled, allow_import)
);
```

初始 seed：
- `user_id`
- `mobile`
- `open_id`
- `email`
- `member_no`

### `tag_definition`

重塑现有表，保留表名：

```sql
CREATE TABLE tag_definition (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100) NOT NULL,
  tag_code    VARCHAR(64) NOT NULL,
  tag_type    VARCHAR(16) NOT NULL DEFAULT 'offline',
  value_type  VARCHAR(16) NOT NULL DEFAULT 'STRING',
  description VARCHAR(500) NULL,
  enabled     TINYINT NOT NULL DEFAULT 1,
  created_by  VARCHAR(64) NULL,
  created_at  DATETIME NULL,
  updated_at  DATETIME NULL,
  UNIQUE KEY uk_tag_code (tag_code),
  INDEX idx_tag_type_enabled (tag_type, enabled)
);
```

`tagCode` 在全局唯一，不再按 `tagType` 复合唯一。画布配置和历史标签数据都通过同一个 `tagCode` 引用标签。

### `tag_value_definition`

```sql
CREATE TABLE tag_value_definition (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  tag_code    VARCHAR(64) NOT NULL,
  value       VARCHAR(255) NOT NULL,
  label       VARCHAR(255) NOT NULL,
  sort_order  INT NOT NULL DEFAULT 0,
  enabled     TINYINT NOT NULL DEFAULT 1,
  source      VARCHAR(32) NOT NULL DEFAULT 'MANUAL',
  description VARCHAR(500) NULL,
  created_at  DATETIME NULL,
  updated_at  DATETIME NULL,
  UNIQUE KEY uk_tag_value (tag_code, value),
  INDEX idx_tag_value_enabled (tag_code, enabled, sort_order)
);
```

`source` 取值：
- `MANUAL`
- `EXCEL_IMPORT`
- `API_PUSH`
- `API_PULL`
- `IMPORT_AUTO_DISCOVERED`

### `user_tag_current`

```sql
CREATE TABLE user_tag_current (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  id_type          VARCHAR(64) NOT NULL,
  id_value         VARCHAR(255) NOT NULL,
  tag_code         VARCHAR(64) NOT NULL,
  tag_value        VARCHAR(255) NOT NULL,
  tag_time         DATETIME NULL,
  source_type      VARCHAR(32) NOT NULL,
  source_batch_id  BIGINT NULL,
  updated_at       DATETIME NULL,
  UNIQUE KEY uk_user_tag_current (id_type, id_value, tag_code),
  INDEX idx_user_tag_tag (tag_code, tag_value),
  INDEX idx_user_tag_identity (id_type, id_value)
);
```

重复导入同一 `idType + idValue + tagCode` 时覆盖当前值。因为项目未上线，本次不保留完整变更历史表；导入批次和错误行足够支撑排查。后续若需要审计，可新增 `user_tag_history`。

### `tag_import_batch`

```sql
CREATE TABLE tag_import_batch (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  source_type      VARCHAR(32) NOT NULL,
  status           VARCHAR(32) NOT NULL,
  file_name        VARCHAR(255) NULL,
  external_url     VARCHAR(1000) NULL,
  total_rows       INT NOT NULL DEFAULT 0,
  success_rows     INT NOT NULL DEFAULT 0,
  failed_rows      INT NOT NULL DEFAULT 0,
  created_by       VARCHAR(64) NULL,
  started_at       DATETIME NULL,
  finished_at      DATETIME NULL,
  error_message    VARCHAR(1000) NULL,
  created_at       DATETIME NULL,
  updated_at       DATETIME NULL,
  INDEX idx_import_batch_status (status, created_at)
);
```

`sourceType` 取值：
- `API_PUSH`
- `API_PULL`
- `EXCEL_IMPORT`

`status` 取值：
- `PENDING`
- `RUNNING`
- `SUCCESS`
- `PARTIAL_SUCCESS`
- `FAILED`

### `tag_import_error`

```sql
CREATE TABLE tag_import_error (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  batch_id    BIGINT NOT NULL,
  row_no      INT NOT NULL,
  raw_payload  TEXT NULL,
  error_code  VARCHAR(64) NOT NULL,
  error_msg   VARCHAR(1000) NOT NULL,
  created_at  DATETIME NULL,
  INDEX idx_import_error_batch (batch_id, row_no)
);
```

### `tag_import_source`

```sql
CREATE TABLE tag_import_source (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  name           VARCHAR(100) NOT NULL,
  url            VARCHAR(1000) NOT NULL,
  method         VARCHAR(16) NOT NULL DEFAULT 'GET',
  headers_json   TEXT NULL,
  body_template  TEXT NULL,
  page_param     VARCHAR(64) NULL,
  page_size_param VARCHAR(64) NULL,
  page_size      INT NOT NULL DEFAULT 500,
  records_path   VARCHAR(255) NOT NULL DEFAULT '$',
  field_mapping  TEXT NOT NULL,
  enabled        TINYINT NOT NULL DEFAULT 1,
  created_by     VARCHAR(64) NULL,
  created_at     DATETIME NULL,
  updated_at     DATETIME NULL,
  INDEX idx_tag_import_source_enabled (enabled, created_at)
);
```

`field_mapping` 使用 JSON 存储，描述远端字段到统一导入契约的映射关系。

## 统一导入契约

所有导入入口统一成以下字段：

```json
{
  "idType": "mobile",
  "idValue": "13800000000",
  "tagCode": "user_level",
  "tagValue": "VIP",
  "tagTime": "2026-05-23 10:30:00"
}
```

字段规则：
- `idType` 必填，必须存在于 `identity_type` 且允许导入。
- `idValue` 必填，按字符串存储，后续脱敏展示由页面负责。
- `tagCode` 必填，必须存在且启用。
- `tagValue` 必填，按标签的 `valueType` 做基础格式校验。
- `tagTime` 可选；为空时使用导入处理时间。

导入时如果 `tagValue` 不在 `tag_value_definition` 中，自动补齐标签值字典，`label = value`。

同一批次内出现重复 `idType + idValue + tagCode` 时，后续重复行记为 `DUPLICATE_ROW`，第一条合法行入库。不同批次再次导入同一身份同一标签时覆盖 `user_tag_current`。

## 后端接口设计

### ID 类型配置

- `GET /canvas/identity-types`
- `POST /canvas/identity-types`
- `PUT /canvas/identity-types/{id}`
- `DELETE /canvas/identity-types/{id}`
- `GET /meta/identity-types?allowImport=1`

删除保护：
- 已被 `user_tag_current` 使用的 ID 类型不能删除，只能停用。

### 标签中心

保留并扩展现有 `/canvas/tag-definitions`：

- `GET /canvas/tag-definitions`
- `POST /canvas/tag-definitions`
- `PUT /canvas/tag-definitions/{id}`
- `DELETE /canvas/tag-definitions/{id}`
- `GET /canvas/tag-definitions/{tagCode}/values`
- `POST /canvas/tag-definitions/{tagCode}/values`
- `PUT /canvas/tag-values/{id}`
- `DELETE /canvas/tag-values/{id}`
- `GET /meta/tagger-tags?type=offline`
- `GET /meta/tagger-tag-values?tagCode=user_level`

删除保护：
- 标签已被 `user_tag_current` 或画布节点引用时不能删除，只能停用。
- 标签值已被 `user_tag_current` 使用时不能硬删，只能停用。

### API 推送导入

```http
POST /canvas/tag-imports/api-push
Content-Type: application/json

{
  "rows": [
    {
      "idType": "mobile",
      "idValue": "13800000000",
      "tagCode": "user_level",
      "tagValue": "VIP",
      "tagTime": "2026-05-23 10:30:00"
    }
  ]
}
```

返回：

```json
{
  "batchId": 1001,
  "status": "SUCCESS",
  "totalRows": 1,
  "successRows": 1,
  "failedRows": 0
}
```

### Excel 导入

- `GET /canvas/tag-imports/excel-template`
- `POST /canvas/tag-imports/excel`

模板列：
- `idType`
- `idValue`
- `tagCode`
- `tagValue`
- `tagTime`

Excel 解析使用 Hutool 的 Excel 工具，避免额外引入重依赖。单次导入默认限制 20,000 行，超出时拒绝并提示拆分文件。

### 外部 API 拉取

新增拉取配置接口：

- `GET /canvas/tag-import-sources`
- `POST /canvas/tag-import-sources`
- `PUT /canvas/tag-import-sources/{id}`
- `DELETE /canvas/tag-import-sources/{id}`
- `POST /canvas/tag-import-sources/{id}/run`

拉取配置字段：
- `name`
- `url`
- `method`
- `headersJson`
- `bodyTemplate`
- `pageParam`
- `pageSizeParam`
- `pageSize`
- `recordsPath`
- `fieldMapping`
- `enabled`

`fieldMapping` 示例：

```json
{
  "idType": "identity_type",
  "idValue": "identity_value",
  "tagCode": "tag_code",
  "tagValue": "tag_value",
  "tagTime": "tag_time"
}
```

第一阶段只要求支持手动触发 `run`。定时拉取可以在同一模型上后续加 `cronExpression`。

### 批次查询

- `GET /canvas/tag-import-batches`
- `GET /canvas/tag-import-batches/{id}`
- `GET /canvas/tag-import-batches/{id}/errors`

## 前端设计

### 导航

在配置区新增：
- `ID 类型配置`
- `标签中心`
- `标签导入`

### ID 类型配置页

列表列：
- 编码
- 名称
- 启用
- 允许导入
- 单值/多值
- 优先级
- 参与映射
- 更新时间

表单支持新增、编辑、停用、删除。

### 标签中心页

列表展示标签定义。编辑标签时打开抽屉，抽屉上半部分维护标签基础信息，下半部分维护标签值表格。

标签值表格支持：
- 新增值
- 编辑 label、排序、说明
- 启停
- 删除未使用值

### 标签导入页

页面分三个 tab：

1. API 推送
   - 展示接口地址、字段说明、JSON 示例。
   - 提供复制示例按钮。
2. API 拉取
   - 管理外部拉取源。
   - 支持手动运行并跳转批次详情。
3. Excel 导入
   - 下载模板。
   - 上传文件。
   - 展示导入结果和错误行。

底部或独立列表展示最近导入批次。

### 画布节点配置

`TAGGER` 节点 schema 增加标签值字段：

```json
[
  {
    "key": "mode",
    "label": "标签模式",
    "type": "radio",
    "required": true,
    "options": [
      { "label": "实时触发（监听 MQ 事件）", "value": "realtime" },
      { "label": "离线打标（流程内执行）", "value": "offline" },
      { "label": "人群圈选", "value": "audience" }
    ]
  },
  {
    "key": "tagCodeKey",
    "label": "标签",
    "type": "select",
    "dataSource": "/meta/tagger-tags",
    "required": true,
    "showWhen": "mode!=audience"
  },
  {
    "key": "tagValue",
    "label": "标签值",
    "type": "select",
    "dataSource": "/meta/tagger-tag-values?tagCode={tagCodeKey}",
    "required": false,
    "showWhen": "mode==offline"
  }
]
```

前端 `ConfigPanel` 需要支持 `dataSource` 中的 `{fieldKey}` 占位符。当依赖字段变化时，清空当前字段值并重新加载选项。

## 后端服务边界

### `IdentityTypeService`

负责：
- ID 类型 CRUD。
- 导入时校验 ID 类型是否可用。
- 提供 `/meta/identity-types` 下拉数据。

### `TagDefinitionService`

负责：
- 标签定义 CRUD。
- 标签值 CRUD。
- 导入时自动补齐标签值。
- 提供 `/meta/tagger-tags` 和 `/meta/tagger-tag-values`。

### `TagImportService`

负责：
- 创建导入批次。
- 校验 `TagImportRow`。
- 批量 upsert `user_tag_current`。
- 写入错误行。
- 汇总批次状态。

### `TagImportSourceService`

负责：
- 外部 API 拉取源 CRUD。
- 运行拉取源。
- 将远端响应按 `fieldMapping` 转换为 `TagImportRow`。
- 委托 `TagImportService` 入库。

## 数据流

### API 推送

1. 外部系统调用 `/canvas/tag-imports/api-push`。
2. Controller 创建 `API_PUSH` 批次。
3. `TagImportService` 校验每行。
4. 合法行 upsert 到 `user_tag_current`，必要时自动补齐标签值字典。
5. 非法行写入 `tag_import_error`。
6. 更新批次状态并返回统计结果。

### Excel 导入

1. 用户下载模板并填写。
2. 前端上传 Excel。
3. 后端解析 Excel 为 `TagImportRow`。
4. 复用 `TagImportService` 入库。
5. 前端展示批次结果和错误行。

### API 拉取

1. 用户配置外部 API 拉取源。
2. 用户手动点击运行。
3. 后端请求外部 API。
4. 按 `recordsPath` 提取数组，按 `fieldMapping` 映射字段。
5. 复用 `TagImportService` 入库。

## 错误处理

行级错误不导致整个批次失败。只要存在成功行，批次状态为 `PARTIAL_SUCCESS`。

常见错误码：
- `IDENTITY_TYPE_NOT_FOUND`
- `IDENTITY_TYPE_NOT_IMPORTABLE`
- `TAG_NOT_FOUND`
- `TAG_DISABLED`
- `TAG_VALUE_INVALID`
- `REQUIRED_FIELD_MISSING`
- `DUPLICATE_ROW`
- `EXCEL_PARSE_FAILED`
- `API_PULL_FAILED`
- `FIELD_MAPPING_FAILED`

整批错误：
- Excel 文件格式无法解析。
- 外部 API 无法访问。
- 请求体超过限制。
- 数据库写入出现不可恢复异常。

## 安全与限制

1. Excel 单次导入默认 20,000 行。
2. API 推送单次默认 5,000 行。
3. `idValue` 页面展示时按 ID 类型做脱敏：手机号、邮箱默认脱敏。
4. 外部 API 拉取 headers/body 不在列表页明文展示。
5. 导入接口需要登录态；权限沿用现有管理配置权限模型。

## 测试策略

后端：
- `TagImportServiceTest`
  - 成功导入。
  - 自动补齐标签值。
  - 重复导入覆盖当前值。
  - 行级错误写入 `tag_import_error`。
- `IdentityTypeControllerTest`
  - CRUD。
  - 已使用 ID 类型删除保护。
- `TagDefinitionControllerTest`
  - 标签值 CRUD。
  - 已使用标签值删除保护。
- `TagImportSourceServiceTest`
  - 字段映射。
  - 外部 API 拉取失败。

前端：
- 标签中心页面测试标签值新增、编辑、启停。
- 标签导入页面测试 Excel 上传成功/失败展示。
- ConfigPanel 测试 `{tagCodeKey}` 联动数据源加载和依赖字段变更清空。

迁移：
- 新增 Flyway migration 创建新表。
- 新增后续 Flyway migration，将 `TAGGER` schema 更新为支持 `tagValue`。

## 开放给后续的能力

1. 完整 ID Mapping：把多个 `idType/idValue` 合并到统一 OneID。
2. 用户标签历史审计表：保留每次导入前后的标签值变化。
3. 定时 API 拉取：在 `tag_import_source` 上启用 `cronExpression`。
4. 标签导入异步化：接入已有异步任务通知能力。
5. 标签值统计：按标签值统计用户数，支撑配置页预览。
