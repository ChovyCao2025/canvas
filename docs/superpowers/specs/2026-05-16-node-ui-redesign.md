# 节点 UI 重设计 — 设计规格

**日期：** 2026-05-16  
**范围：** optimization_list_v3.md → 优化点 1, 3, 4  
**分组：** Group C（节点 UI 重设计）

---

## 现状核查

| 优化点 | 内容 | 现状 | 结论 |
|--------|------|------|------|
| 优化点 1 | TAGGER 标签配置化 | V18 已建 `tag_definition` 表，`tag-config` 管理页已实现，`dataSource` 已动态化 | **无需改动** |
| 优化点 3 | DELAY 节点 UI 重设计 | V17 已实现 `delay-input` 复合控件（数值输入 + 单位下拉 + 快捷预设按钮） | **无需改动** |
| 优化点 4 | SEND_MQ 动态配置 | V19 已建 `mq_message_definition` 表和 `mq-config` 管理页，config_schema 已更新；但 `api-input-params` 字段缺少必要参数 | **有 Bug，需修复** |

---

## SEND_MQ Bug 修复（优化点 4）

### 问题根因

V19 更新后的 SEND_MQ `config_schema`：

```json
[
  {"key":"messageCodeKey","label":"消息类型","type":"select","dataSource":"/meta/mq-definitions","required":true},
  {"key":"params","label":"消息参数","type":"api-input-params","required":false},
  {"key":"nextNodeId","label":"后继节点","type":"input","required":false}
]
```

`api-input-params` 控件（`ApiCallInputParams` 组件）依赖两个 schema 扩展字段：
- `apiKeyField`：监听哪个表单字段的值来确定当前选中的 API/消息 key（默认 `"apiKey"`）
- `defsSource`：从哪个接口加载参数定义（默认 `"/meta/api-definitions"`）

SEND_MQ 的 schema 未指定这两个字段，导致：
- 组件监听 `formValues.apiKey`（不存在）而非 `formValues.messageCodeKey`
- 从 `/meta/api-definitions`（API 定义）而非 `/meta/mq-definitions`（MQ 定义）加载参数
- **结果：用户选了消息类型后，参数列表永远为空**

### 修复方案

**Flyway V28：** 更新 SEND_MQ config_schema，在 `api-input-params` 字段补充两个扩展属性。

```sql
UPDATE node_type_registry
SET config_schema = '[
  {
    "key":        "messageCodeKey",
    "label":      "消息类型",
    "type":       "select",
    "dataSource": "/meta/mq-definitions",
    "required":   true
  },
  {
    "key":          "params",
    "label":        "消息参数",
    "type":         "api-input-params",
    "apiKeyField":  "messageCodeKey",
    "defsSource":   "/meta/mq-definitions",
    "required":     false
  }
]'
WHERE type_key = 'SEND_MQ';
```

> `nextNodeId` 字段从 schema 中移除（连线由画布 Handle 管理，不需要手填，与 Group B 占位框方案一致）。

**无需改动前端代码：** `ApiCallInputParams` 组件已支持 `apiKeyField` 和 `defsSource` 参数（`config-panel/index.tsx:639-640`），只需 schema 数据正确即可。

### 验证标准

1. 进入含 SEND_MQ 节点的画布，打开节点配置面板
2. 在"消息类型"下拉框选择任意已配置的 MQ 消息
3. "消息参数"区域应展示该消息在 `mq_message_definition.request_schema` 中定义的参数字段
4. 参数字段可填入动态表达式（复用 API_CALL 的 `api-input-params` 渲染逻辑）

---

## 优化点 1 — TAGGER 标签配置化（已完成，记录在册）

V18 实现内容：
- `tag_definition` 表：字段 `name`、`tag_code`、`tag_type(offline/realtime)`、`enabled`
- `/meta/tagger-tags?type=offline|realtime` 接口从该表读取
- `tag-config` 管理页：支持新增、编辑、禁用标签
- TAGGER_OFFLINE / TAGGER_REALTIME schema 中 `tagCodeKey` 字段的 `dataSource` 已配置为动态接口

**Group A TAGGER 合并节点的衔接：** 合并后的 TAGGER 节点使用 `dataSource: "/meta/tagger-tags"`（不带 type 参数），配合 mode 字段联动在前端拼接 `?type=realtime/offline`（详见 Group A spec）。无需新建接口，`MetaController` 不传 type 时返回全量标签即可。

---

## 优化点 3 — DELAY 节点 UI（已完成，记录在册）

V17 实现内容（`config-panel/index.tsx:694-741`）：
- `DelayInput` 复合控件：`InputNumber`（数值）+ `Select`（单位：秒/分钟/小时）横向排列
- 快捷预设按钮：30秒、5分、30分、1小时
- 存储格式：`{ duration: number, unit: 'SECOND'|'MINUTE'|'HOUR' }`

无需改动。

---

## 实现顺序

| 步骤 | 内容 | 工作量 |
|------|------|--------|
| 1 | Flyway V28：修复 SEND_MQ config_schema | 15min |

**总工作量：** ~15 分钟（纯 SQL 迁移）
