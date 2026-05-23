# 人群数据源外提设计

## 背景

当前人群编辑页在选择 `JDBC` 数据源时，要求每次重复填写完整数据库连接配置，包括 `url`、`username`、`password`、`driverClassName`、`baseTable`、`userIdColumn`、`maxRows`。这带来三个问题：

1. 同一数据库连接在多个人群中重复录入，配置成本高。
2. 数据库连接变更时，需要逐个人群修改，容易遗漏。
3. 连接级配置和人群级查询配置混在一起，模型边界不清晰。

本次目标是将 JDBC 连接配置从人群定义中抽离，提升为人群模块内的独立配置对象，供多个 JDBC 人群复用。

## 目标

1. 新增“人群数据源”一级配置对象，统一维护 JDBC 连接信息。
2. 人群编辑页在使用 `JDBC` 时只选择数据源，不再重复填写连接信息。
3. `baseTable`、`userIdColumn`、`maxRows` 继续保留为人群级配置。
4. 通过一次性迁移将历史 JDBC 人群映射到新的数据源模型。
5. 保持 `TAGGER_API` 路径不受影响。

## 非目标

1. 不将人群数据源提升为全局系统配置中心能力。
2. 本次不扩展非 JDBC 类型的人群外部数据源。
3. 本次不强制实现“测试连接”能力。
4. 本次不调整人群规则构建器或计算引擎本身。

## 方案选型

### 方案 A：继续复用 `audience_definition.data_source_config`

做法：
- 新增数据源表，但人群定义不新增显式 `dataSourceId`
- 在 `data_source_config` 中混合存储引用关系和人群级参数

优点：
- 表结构改动较少

缺点：
- `data_source_config` 语义继续混杂
- 数据源作为独立业务对象的边界仍然不清晰
- 后续扩展测试连接、停用、引用统计会更别扭

### 方案 B：新增独立 `audience_data_source` 模型

做法：
- 新增 `audience_data_source` 表
- `audience_definition` 新增 `data_source_id`
- 人群定义只保留数据源引用和人群级 JDBC 参数

优点：
- 模型边界最清楚
- 前后端接口更易维护
- 后续支持多数据源、停用、引用统计、测试连接更顺畅

缺点：
- 需要 migration、接口、前端页面同步调整

### 方案 C：仅做前端默认模板

做法：
- 不改后端模型
- 前端通过本地默认值或最近使用减少重复填写

优点：
- 改动最小

缺点：
- 不能统一管理数据库连接
- 不能解决连接变更时的重复维护问题

### 结论

采用方案 B。用户诉求不是单纯减少表单输入，而是将“人群数据源”提升为独立业务配置对象。该方案能从数据模型上解决重复配置问题。

## 数据模型设计

### 新增表：`audience_data_source`

建议字段：

```sql
CREATE TABLE audience_data_source (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(100) NOT NULL COMMENT '数据源名称',
    description       VARCHAR(500) NULL COMMENT '备注',
    url               VARCHAR(1000) NOT NULL COMMENT 'JDBC URL',
    username          VARCHAR(200) NOT NULL COMMENT '数据库用户名',
    password          VARCHAR(500) NOT NULL COMMENT '数据库密码',
    driver_class_name VARCHAR(255) NULL COMMENT 'JDBC Driver 类名',
    enabled           TINYINT NOT NULL DEFAULT 1 COMMENT '1=启用 0=停用',
    created_by        VARCHAR(100) NULL,
    created_at        DATETIME NULL,
    updated_at        DATETIME NULL
);
```

说明：
- `password` 仍按当前项目风格存储；本次不额外引入密钥托管方案。
- `enabled` 用于控制是否可被新的人群继续选择。

### 调整表：`audience_definition`

新增字段：

```sql
ALTER TABLE audience_definition
    ADD COLUMN data_source_id BIGINT NULL COMMENT '人群数据源ID';
```

保留字段：
- `data_source_type`
- `data_source_config`

收敛后的 `data_source_config` 语义：

```json
{
  "baseTable": "audience_demo_user",
  "userIdColumn": "user_id",
  "maxRows": 10000
}
```

约束：
- `data_source_type = 'JDBC'` 时，`data_source_id` 必填。
- `data_source_type = 'TAGGER_API'` 时，`data_source_id` 为空，`data_source_config` 维持现有格式。

## 领域职责边界

### 人群数据源

负责维护 JDBC 连接级配置：
- 连哪个库
- 用什么账号
- 用什么驱动
- 当前是否允许被新的人群引用

### 人群定义

负责维护人群自身逻辑：
- 用哪个数据源
- 查哪张表
- 用户 ID 列是哪一列
- 最多扫描多少行
- 圈选规则和计算策略是什么

该拆分避免了“连接”和“查询对象”绑定死在一起，允许一个数据库连接承载多个人群配置。

## 后端接口设计

新增人群数据源接口，和人群定义接口平级：

- `GET /canvas/audience-data-sources`
- `GET /canvas/audience-data-sources/{id}`
- `POST /canvas/audience-data-sources`
- `PUT /canvas/audience-data-sources/{id}`
- `DELETE /canvas/audience-data-sources/{id}`

建议返回字段：
- `id`
- `name`
- `description`
- `url`
- `username`
- `driverClassName`
- `enabled`
- `referenceCount`
- `createdAt`
- `updatedAt`

接口行为要求：

1. 删除保护
- 如果某个数据源已被 JDBC 人群引用，删除请求返回业务错误，不执行删除。

2. 停用行为
- 停用后不能再被新建或编辑中的 JDBC 人群选择。
- 已引用该数据源的历史人群允许继续查看和执行计算。

3. 列表场景
- 默认列表返回全部数据源，前端自行按启用态区分展示。
- 提供足够字段支持人群页下拉选择和管理页列表展示。

4. 本期不做
- 不新增测试连接接口。

## 前端交互设计

### 页面组织

“人群数据源”属于人群模块内部配置，不进入系统全局设置。

建议入口：
- 人群列表页新增“数据源管理”入口。

新增页面：
- `/audiences/data-sources`
- `/audiences/data-sources/new`
- `/audiences/data-sources/{id}/edit`

如果现有路由更适合弹窗或抽屉实现，也可保持单页管理，但信息架构上仍归属于人群模块。

### 人群数据源管理页

列表字段建议：
- 数据源名称
- JDBC URL
- 驱动类
- 状态
- 引用数
- 更新时间
- 操作（编辑、停用/启用、删除）

表单字段：
- 名称
- 备注
- JDBC URL
- 用户名
- 密码
- Driver Class
- 启用状态

### 人群编辑页调整

当 `dataSourceType = 'JDBC'` 时：
- 展示“人群数据源”下拉选择
- 展示 `baseTable`
- 展示 `userIdColumn`
- 展示 `maxRows`
- 不再展示 `url`、`username`、`password`、`driverClassName`

当 `dataSourceType = 'TAGGER_API'` 时：
- 保持现有交互不变

交互要求：

1. 无可用数据源
- 如果当前没有启用中的 JDBC 数据源，人群页应明确提示先创建数据源。

2. 停用数据源回显
- 编辑已有 JDBC 人群时，如果其引用的数据源已停用，页面仍需正确回显。
- UI 需明确提示该数据源已停用，避免用户误解为数据丢失。

3. 兼容性
- 新建 JDBC 人群不再接受手工输入连接信息。
- 已迁移的人群应直接展示为“选中的数据源 + 表级配置”。

## 计算逻辑调整

`AudienceBatchComputeService` 的 JDBC 路径改为：

1. 从 `AudienceDefinition` 读取 `dataSourceType`、`dataSourceId`、`dataSourceConfig`
2. 若为 `JDBC`，根据 `dataSourceId` 加载 `AudienceDataSource`
3. 从 `dataSourceConfig` 中读取 `baseTable`、`userIdColumn`、`maxRows`
4. 组装 JDBC 连接和 SQL 查询参数
5. 执行现有 SQL 生成人群结果

异常规则：

1. `dataSourceType = 'JDBC'` 且 `dataSourceId` 为空
- 直接报配置错误

2. `dataSourceId` 对应记录不存在
- 直接报配置错误

3. 数据源已停用
- 已有引用的人群计算仍允许执行
- 前端仅限制新建和重新选择，不阻断存量计算

这样可以保证“停用”是面向配置入口的管控，而不是对运行中配置做硬切断。

## 数据迁移设计

采用一次性迁移，不长期维护旧新两套 JDBC 配置格式。

### Migration 步骤

1. 新增 `audience_data_source` 表
2. 给 `audience_definition` 增加 `data_source_id`
3. 扫描历史 `data_source_type = 'JDBC'` 的人群定义
4. 解析原始 `data_source_config`
5. 按连接级字段去重生成数据源记录：
   - `url`
   - `username`
   - `password`
   - `driverClassName`
6. 将人群定义回填到对应 `data_source_id`
7. 将原 `data_source_config` 收敛为仅包含：
   - `baseTable`
   - `userIdColumn`
   - `maxRows`

### 去重规则

使用以下字段完全相同视为同一个数据源：
- `url`
- `username`
- `password`
- `driverClassName`

`name` 可按 migration 规则自动生成，例如：
- `JDBC 数据源 1`
- `JDBC 数据源 2`

后续由用户按需要重命名。

### 历史数据兼容策略

本次选择“迁移后收敛到单一新模型”，而不是长期兼容双格式。

原因：
- 计算逻辑更简单
- 前端不需要维护复杂的新旧回显分支
- 减少未来继续背负历史格式的风险

## 验证与测试

### 后端

1. Migration 测试
- 旧 JDBC 人群能正确生成并绑定 `audience_data_source`
- 同连接配置的人群能正确复用同一个数据源
- `TAGGER_API` 人群不受影响

2. Service 测试
- JDBC 计算能正确合并“外层连接配置 + 人群级查询配置”
- `dataSourceId` 缺失时正确失败
- 数据源不存在时正确失败

3. Controller 测试
- 数据源列表、创建、更新、删除保护行为正确
- 被引用数据源无法删除
- 停用数据源后新建路径不可选

### 前端

1. 人群编辑页
- 选择 `JDBC` 后仅展示数据源选择和表级配置
- 没有可用数据源时能看到明确提示
- 编辑引用停用数据源的人群时能正确回显

2. 数据源管理页
- 可创建、编辑、启停数据源
- 删除被引用数据源时展示失败反馈

## 风险与约束

1. 密码仍为业务表字段存储，本次不扩展安全治理能力。
2. migration 会直接改写历史 JDBC 人群的 `data_source_config`，需要保证脚本幂等和可验证。
3. 如果未来需要非 JDBC 外部数据源，`audience_data_source` 可能还要进一步抽象；本次先只面向 JDBC，避免过度设计。

## 实施结论

本次将“人群数据源”建模为人群模块中的独立一级配置对象：

1. 连接级 JDBC 参数统一外提到 `audience_data_source`
2. 人群定义仅保留数据源引用和表级查询参数
3. 人群编辑页用“选择数据源”替代重复输入连接配置
4. 通过一次性 migration 将历史 JDBC 人群平滑迁移到新模型

该方案能消除重复配置，明确模型边界，并为后续的人群数据源管理能力留出清晰扩展点。
