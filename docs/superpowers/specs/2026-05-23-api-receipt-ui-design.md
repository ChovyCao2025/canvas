# API 回执设置与预览布局设计

## 背景

API 配置页已经支持请求参数定义、环境信息开关和请求 JSON 示例。参考阿里云 Webhook 管理里的配置方式，下一步需要让用户在同一个配置弹窗里配置回执设置，并把消息格式预览做得更靠近实际配置流程。

## 目标

- API 配置弹窗改为左侧配置、右侧预览的布局。
- 新增回执设置开关。
- 开启回执后可配置数据回收周期和多组状态映射。
- 右侧预览包含“请求 Body”和“回执上报”两个视图。
- API 定义持久化回执设置，方便后续接入回执接收接口。

## 非目标

- 不实现回执接收接口。
- 不实现签名鉴权。
- 不实现上线后字段锁定，只先把配置结构和 UI 形态补齐。

## 数据模型

在 `api_definition` 增加字段：

- `receipt_enabled`: 是否开启回执，`0/1`。
- `receipt_expire_minutes`: 回执数据回收周期，单位分钟，默认 `1440`。
- `receipt_statuses`: 回执状态映射 JSON 数组，形如 `[{"code":"200","label":"成功"}]`。

前端表单使用：

- `receiptEnabled: boolean`
- `receiptExpireMinutes: number`
- `receiptStatuses: Array<{ code: string; label: string }>`

提交 API 定义时转换成后端字段格式。

## UI 设计

API 配置弹窗宽度扩大为双栏：

- 左栏：基础信息、请求参数、环境信息、回执设置。
- 右栏：固定预览区域，使用 Tabs 展示请求 Body 和回执上报 JSON。

回执设置区：

- 开关关闭时只展示“回执设置”开关。
- 开关开启时展示：
  - 数据回收周期，使用数字输入，单位分钟。
  - 状态映射表，字段为“状态 code”和“状态值”。
  - 添加状态按钮。

API 列表新增“回执”列，展示“开启/关闭”。

## 预览行为

请求 Body 预览沿用现有逻辑。

回执上报预览：

- 回执关闭时展示空数组 `[]`。
- 回执开启时展示数组结构，包含 `msg_id`、`status`、`cst_id`、`send_time`、`callback_params`。
- `status` 使用第一条状态映射的 code；没有状态映射时使用 `"200"`。
- `callback_params` 使用与请求环境信息相同的示例字段，强调接入方需要原样回传。

## 验证

前端：

- 预览 helper 测试覆盖回执关闭和开启。
- 预览 helper 测试覆盖 API 定义提交时回执字段转换。
- `npm test -- src/pages/api-config/requestPreview.test.ts`
- `npm run build`

后端：

- Controller 单元测试覆盖创建 API 时回执字段默认值。
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -Dtest=ApiDefinitionControllerTest test`
- `JAVA_HOME=$(/usr/libexec/java_home -v 21) mvn -pl canvas-engine -DskipTests compile`
