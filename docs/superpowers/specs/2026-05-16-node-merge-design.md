# 节点合并 — 设计规格

**日期：** 2026-05-16  
**范围：** optimization_list_v3.md → 优化点 6, 7  
**分组：** Group A（节点合并）

---

## 合并一：TAGGER_OFFLINE + TAGGER_REALTIME → TAGGER

### 问题

两个节点对用户来说语义相同（打标签），配置结构也几乎一样（都是选 tagCodeKey），唯一区别是数据源 URL 的 `?type=` 参数和执行时机。用户需要记住两个节点，学习成本高。

### 方案：新增 TAGGER 类型，旧类型软废弃

#### 后端变更

**Flyway V26（当前最新 V23；V24 留给 E4 trigger_type，V25 留给 F1 category）：** 新增 TAGGER 节点类型。

```sql
INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES (
  'TAGGER',
  'Tagger 标签',
  '行为策略',
  'org.chovy.canvas.engine.handlers.TaggerHandler',
  '[
    {
      "key":      "mode",
      "label":    "标签模式",
      "type":     "radio",
      "required": true,
      "options":  [
        {"label": "实时触发（监听 MQ 事件）", "value": "realtime"},
        {"label": "离线打标（流程内执行）",    "value": "offline"}
      ]
    },
    {
      "key":        "tagCodeKey",
      "label":      "标签",
      "type":       "select",
      "dataSource": "/meta/tagger-tags",
      "required":   true
    }
  ]',
  '[]',
  0, 0,
  '实时或离线方式对用户打 Tagger 标签，通过模式选项切换。',
  1
);

-- 旧类型隐藏（保留数据完整性，不删除）
UPDATE node_type_registry SET enabled = 0
WHERE type_key IN ('TAGGER_OFFLINE', 'TAGGER_REALTIME');
```

**后端接口 `/meta/tagger-tags` 改造：**
- 现有 `?type=offline` / `?type=realtime` 两个 URL 参数继续支持（兼容旧画布）
- 新增不带 type 参数时返回全量标签列表（TAGGER 节点使用），或在前端根据 mode 动态拼接参数

**TaggerHandler.java：** 新增统一 Handler，根据 `config.mode` 路由到原 offline/realtime 执行逻辑（内部委托）。

**存量画布迁移（Flyway 数据修复脚本，可选）：**
```sql
-- 将已有画布 JSON 中的 TAGGER_OFFLINE/TAGGER_REALTIME 节点迁移到 TAGGER 类型
-- 由于 graphJson 是 JSON 字符串，此步骤建议用 Java 工具类执行而非纯 SQL
-- 迁移规则：
--   nodeType: "TAGGER_OFFLINE"  → nodeType: "TAGGER", bizConfig.mode: "offline"
--   nodeType: "TAGGER_REALTIME" → nodeType: "TAGGER", bizConfig.mode: "realtime"
```
> 迁移脚本由后端开发者用 CanvasService 工具方法执行，不写入 Flyway（避免修改已发布画布的版本）。

#### 前端变更

**动态 is_trigger 行为（核心难点）：**

TAGGER_REALTIME 原本是触发器（无 target handle），TAGGER_OFFLINE 不是。合并后需根据 `mode` 动态控制：

- 在 `CanvasNode.tsx` 中：`isTrigger` 的判断从 `TRIGGER_TYPES.has(d.nodeType)` 扩展为：
  ```ts
  const isTrigger = TRIGGER_TYPES.has(d.nodeType)
    || (d.nodeType === 'TAGGER' && d.bizConfig?.mode === 'realtime')
  ```
- mode 为 `realtime` → 不渲染 target handle（无入边）
- mode 为 `offline` 或未选择 → 渲染 target handle（可接入边）

**ConfigPanel 配置面板：**

`TAGGER` 节点的 `config_schema` 中，`tagCodeKey` 字段的 `dataSource` 需要根据 `mode` 动态切换。当前 ConfigPanel 的 `loadDataSource` 是按 URL 缓存的，需要支持「字段 dataSource 依赖其他字段值」的联动：

```ts
// 在加载 tagCodeKey 的 select 选项时：
const mode = formValues.mode as string
const src  = mode ? `/meta/tagger-tags?type=${mode}` : '/meta/tagger-tags'
loadDataSource(src).then(...)
```

联动触发：`mode` 字段变化时，清空 `tagCodeKey` 的缓存并重新加载 options。

**node-panel 显示：** `TAGGER` 在节点面板显示为一个条目"Tagger 标签"，不再显示 TAGGER_OFFLINE/TAGGER_REALTIME（已 `enabled=0`）。

**TRIGGER_TYPES 常量更新：** `TAGGER_REALTIME` 从 `TRIGGER_TYPES` 集合移除（改为运行时动态判断）。

---

## 合并二：BEHAVIOR_IN_APP + DIRECT_CALL → BEHAVIOR_TRIGGER

### 问题与权衡

两个节点都是触发器（journey 入口），但触发机制完全不同：
- `BEHAVIOR_IN_APP`：监听用户端内行为事件（MQ 驱动，被动）
- `DIRECT_CALL`：业务方 HTTP 主动调用（API 驱动，主动）

**合并理由：** 对最终用户来说，两者都是"什么情况下触发这个旅程"的选项，放在一个节点里可以减少节点面板条目，并与 SCHEDULED_TRIGGER 迁移后的"触发方式"概念一致。

**复杂度说明：** 两者配置 UI 差异较大（事件选择 vs 参数定义），合并后配置面板的内容随 `triggerType` 字段完全切换。

### 方案：新增 BEHAVIOR_TRIGGER 类型，旧类型软废弃

#### 后端变更

**Flyway V27（同批）：** 新增合并节点类型。

```sql
INSERT INTO node_type_registry
  (type_key, type_name, category, handler_class,
   config_schema, output_schema, is_trigger, is_terminal, description, enabled)
VALUES (
  'BEHAVIOR_TRIGGER',
  '行为触发',
  '行为策略',
  'org.chovy.canvas.engine.handlers.BehaviorTriggerHandler',
  '[
    {
      "key":      "triggerType",
      "label":    "触发方式",
      "type":     "radio",
      "required": true,
      "options":  [
        {"label": "端内行为事件（监听 MQ）", "value": "inapp"},
        {"label": "业务直调（HTTP 推送）",    "value": "direct"}
      ]
    },
    {
      "key":        "eventCode",
      "label":      "触发事件",
      "type":       "select",
      "dataSource": "/meta/event-definitions",
      "required":   true,
      "showWhen":   "triggerType == inapp"
    },
    {
      "key":  "_attrHint",
      "label":"可用上下文变量",
      "type": "event-attr-preview",
      "showWhen": "triggerType == inapp"
    },
    {
      "key":  "inputParams",
      "label":"入参定义",
      "type": "param-define-list",
      "showWhen": "triggerType == direct"
    }
  ]',
  '[]',
  1, 0,
  '通过端内行为事件或业务 HTTP 直调触发旅程。',
  1
);

UPDATE node_type_registry SET enabled = 0
WHERE type_key IN ('BEHAVIOR_IN_APP', 'DIRECT_CALL');
```

**BehaviorTriggerHandler.java：** 根据 `config.triggerType` 分发到原 BehaviorInAppHandler / DirectCallHandler 执行逻辑。

**存量画布迁移：**
```
BEHAVIOR_IN_APP → BEHAVIOR_TRIGGER, bizConfig.triggerType = "inapp"
DIRECT_CALL     → BEHAVIOR_TRIGGER, bizConfig.triggerType = "direct"
```

#### 前端变更

**`showWhen` 条件渲染：** ConfigPanel 解析 schema 中的 `showWhen` 字段，根据当前 `formValues` 决定字段是否渲染：

```ts
function shouldShow(field: SchemaField, formValues: Record<string, unknown>): boolean {
  if (!field.showWhen) return true
  // 简单支持 "key == value" 格式
  const [key, val] = field.showWhen.split(' == ')
  return String(formValues[key]) === val
}
```

> `showWhen` 是对 config_schema 的能力扩展，后续其他节点若有条件显示需求可复用。

**node-panel 显示：** 显示为"行为触发"，替代 BEHAVIOR_IN_APP 和 DIRECT_CALL。

**TRIGGER_TYPES 常量更新：** `BEHAVIOR_IN_APP` 和 `DIRECT_CALL` 从常量中移除，加入 `BEHAVIOR_TRIGGER`。

---

## 共同约束

### 旧类型兼容性

- 旧类型（`TAGGER_OFFLINE`、`TAGGER_REALTIME`、`BEHAVIOR_IN_APP`、`DIRECT_CALL`）在 DB 中保持 `enabled=0` 而非删除
- 执行引擎 Handler 保留，旧画布继续正常运行
- ConfigPanel 对旧类型节点仍能渲染（使用旧 schema，因 schema 存在于 DB 中）

### DEFAULT_NAMES 常量更新（`constants.ts`）

```ts
TAGGER:            'Tagger 标签',
BEHAVIOR_TRIGGER:  '行为触发',
// 保留旧 key 兼容已有画布：
TAGGER_OFFLINE:    'Tagger离线标签',
TAGGER_REALTIME:   'Tagger实时标签',
BEHAVIOR_IN_APP:   '端内行为触发',
DIRECT_CALL:       '业务直调',
```

### getBranchHandles 无需改动

两个合并后的节点均为单输出（`default` handle），不进入分支 Handle 逻辑。

---

## 实现顺序

| 步骤 | 内容 | 依赖 |
|------|------|------|
| 1 | Flyway V26 + V27：新增 TAGGER、BEHAVIOR_TRIGGER，旧类型 enabled=0 | 无 |
| 2 | 新增 TaggerHandler、BehaviorTriggerHandler（委托旧 Handler） | Step 1 |
| 3 | ConfigPanel 支持 `showWhen` 条件渲染 + mode/triggerType 联动 dataSource | 无 |
| 4 | CanvasNode 动态 isTrigger 判断（TAGGER + mode=realtime） | 无 |
| 5 | constants.ts 更新 DEFAULT_NAMES，TRIGGER_TYPES 移除旧类型 | Step 4 |
| 6 | 存量画布迁移工具方法（Java 脚本，手动执行） | Step 1 |

**总工作量估计：** ~5h

---

## 不在本次范围内

- TAGGER 节点的标签自定义配置（optimization_list_v3 优化点 1）—— 属于 Group C 范畴
- 执行引擎对新类型的完整测试
