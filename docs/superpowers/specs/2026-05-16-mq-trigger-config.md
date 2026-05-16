# MQ_TRIGGER 配置化 — 设计规格

**日期：** 2026-05-16  
**范围：** optimization_list_v3.md → 优化点 10  
**分组：** Group D（动态配置中心）

---

## 问题

`MQ_TRIGGER` 节点的"消息主题"下拉框由 `MetaService.getMqTopics()` 提供，内容是 Java 代码中的 3 条静态数据：

```java
return List.of(
  new StubOption("flight_order_status_change", "机票订单状态变化"),
  new StubOption("hotel_order_status_change",  "酒店订单状态变化"),
  new StubOption("train_order_status_change",  "火车票订单状态变化")
);
```

用户无法自行添加 MQ 消息类型，每次变更都需要修改代码重新部署，违反了"用户可以自我定义配置"的目标。

---

## 现有基础设施（可直接复用）

V19 已为 `SEND_MQ` 节点建立了完整的消息定义体系：

| 资源 | 说明 |
|------|------|
| `mq_message_definition` 表 | 存储消息编码、名称、Topic、参数 Schema |
| `GET /meta/mq-definitions` | 返回全量消息定义列表（MetaController:78） |
| `mq-config` 管理页 | 前端已有完整的 CRUD 管理界面 |

**MQ_TRIGGER 使用相同的消息定义库**，逻辑上 SEND_MQ 是"发送这类消息"，MQ_TRIGGER 是"监听这类消息"，一张表即可支持双向。

---

## 方案

### 后端变更

#### 1. Flyway V29：更新 MQ_TRIGGER config_schema

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
    "key":      "validateResult",
    "label":    "开启消息校验",
    "type":     "toggle"
  },
  {
    "key":      "validateRules",
    "label":    "校验规则",
    "type":     "condition-rule-list",
    "visible":  "validateResult==true"
  }
]'
WHERE type_key = 'MQ_TRIGGER';
```

变更说明：
- `topicKey` → `messageCodeKey`（与 SEND_MQ 及 mq_message_definition 保持一致）
- `dataSource` 从 `/meta/mq-topics` 改为 `/meta/mq-definitions`

#### 2. `MqTriggerHandler.java` — 改用 DB 数据解析 Topic

现有 Handler 读取 `bizConfig.topicKey` 作为 MQ topic 订阅/路由键。改造后：

```java
// 改前：直接使用 topicKey 作为 topic
String topic = config.get("topicKey");

// 改后：用 messageCodeKey 从 mq_message_definition 查 topic
String messageCode = config.get("messageCodeKey");
String topic = mqMessageDefinitionMapper
  .selectOne(Wrappers.<MqMessageDefinition>lambdaQuery()
    .eq(MqMessageDefinition::getMessageCode, messageCode)
    .eq(MqMessageDefinition::getEnabled, 1))
  .getTopic();
```

**向后兼容：** 若 `messageCodeKey` 为空但 `topicKey` 有值（旧画布），则 fallback 使用 `topicKey`。

#### 3. `/meta/mq-topics` 端点处理

`MetaController.getMqTopics()` 保留但标记为 `@Deprecated`，不再向 node_type_registry 暴露。旧画布的 config_schema 仍引用 `/meta/mq-topics` 的节点会继续工作（该端点保留返回原 3 条静态数据）。

---

### 前端变更

**无需改动 ConfigPanel**：`/meta/mq-definitions` 返回与 `/meta/mq-topics` 相同结构的 `{ key, label }` 列表，现有 `select` + `dataSource` 渲染路径完全复用。

**`mq-config` 管理页**：已存在，无需改动。用户在该页添加新的消息类型后，MQ_TRIGGER 节点下拉框立即可见新条目。

**页面导航说明补充**：在 `mq-config` 页顶部或说明区加一句提示：

> "在此配置好 MQ 消息类型后，可在画布中的「MQ消息触发」节点和「发送MQ」节点直接选用。"

---

### 存量画布迁移

MQ_TRIGGER 节点的 `bizConfig` 中 `topicKey` 字段需要迁移为 `messageCodeKey`，同时确认 `mq_message_definition` 中存在对应的 `topic` 记录。

迁移逻辑（由后端工具方法执行，不写入 Flyway）：

```
旧：bizConfig.topicKey = "flight_order_status_change"
新：bizConfig.messageCodeKey = "flight_order_status_change"
   （要求 mq_message_definition 中存在 message_code = "flight_order_status_change" 的记录）
```

**前置条件：** 在迁移前，将原 3 条静态 topic 插入 `mq_message_definition` 表（可在 V29 中完成）：

```sql
INSERT INTO mq_message_definition (name, message_code, topic, request_schema, enabled, created_by, created_at, updated_at)
VALUES
  ('机票订单状态变化', 'flight_order_status_change', 'flight_order_status_change', '[]', 1, 'system', NOW(), NOW()),
  ('酒店订单状态变化', 'hotel_order_status_change',  'hotel_order_status_change',  '[]', 1, 'system', NOW(), NOW()),
  ('火车票订单状态变化','train_order_status_change',  'train_order_status_change',  '[]', 1, 'system', NOW(), NOW())
ON DUPLICATE KEY UPDATE updated_at = NOW();
```

---

## Flyway 编号汇总（本 spec 补充）

| 版本 | 内容 |
|------|------|
| V24 | E4：Canvas 表加 trigger_type / cron_expression 字段 |
| V25 | F1：node_type_registry 中 TAGGER 节点 category 改为行为策略 |
| V26 | A1：新增 TAGGER 类型，旧 TAGGER_OFFLINE/REALTIME enabled=0 |
| V27 | A2：新增 BEHAVIOR_TRIGGER 类型，旧 BEHAVIOR_IN_APP/DIRECT_CALL enabled=0 |
| V28 | C1：修复 SEND_MQ config_schema，api-input-params 补 apiKeyField + defsSource |
| V29 | D1：更新 MQ_TRIGGER config_schema，迁移原 3 条静态 topic 到 mq_message_definition |

---

## 实现顺序

| 步骤 | 内容 | 工作量 |
|------|------|--------|
| 1 | V29：更新 MQ_TRIGGER schema + 插入原 3 条静态 topic 到 mq_message_definition | 20min |
| 2 | MqTriggerHandler：改用 messageCodeKey，fallback 兼容 topicKey | 1h |
| 3 | mq-config 页顶部加使用说明文案 | 15min |
| 4 | 存量画布迁移工具方法 | 30min |

**总工作量：** ~2h
