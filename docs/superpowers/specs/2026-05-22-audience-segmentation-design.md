# 标签人群圈选设计（优化点 #10）

## 背景

现有 `TagDefinition` 只是标签元数据，缺少：
1. 人群圈选规则（哪些用户属于这个标签所代表的人群）
2. 离线批量计算任务（计算人群 → 存 Roaring Bitmap）
3. 前端配置 UI（运营自助配置规则）

---

## 数据模型（新增两张表）

### audience_definition（人群定义）

```sql
CREATE TABLE audience_definition (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL COMMENT '人群名称',
    description     VARCHAR(500) COMMENT '描述',
    rule_json       TEXT NOT NULL COMMENT '圈选规则 JSON',
    engine_type     VARCHAR(20) NOT NULL DEFAULT 'AVIATOR'
                    COMMENT 'AVIATOR | QL',
    data_source_type VARCHAR(20) NOT NULL DEFAULT 'TAGGER_API'
                    COMMENT 'TAGGER_API | JDBC',
    data_source_config TEXT COMMENT '数据源配置 JSON',
    evaluation_strategy VARCHAR(20) NOT NULL DEFAULT 'OFFLINE_BATCH'
                    COMMENT 'ONLINE | OFFLINE_BATCH | HYBRID',
    cron_expression VARCHAR(100) COMMENT '定时计算 cron，null=手动触发',
    enabled         TINYINT NOT NULL DEFAULT 1,
    created_by      VARCHAR(100),
    created_at      DATETIME,
    updated_at      DATETIME
);
```

### audience_stat（人群计算结果元数据）

```sql
CREATE TABLE audience_stat (
    audience_id     BIGINT PRIMARY KEY,
    estimated_size  BIGINT COMMENT '人群规模',
    bitmap_size_kb  INT    COMMENT 'Bitmap 序列化大小（KB）',
    computed_at     DATETIME COMMENT '最后计算时间',
    status          VARCHAR(20) COMMENT 'COMPUTING | READY | FAILED',
    error_msg       VARCHAR(500)
);
```

**Redis Key：**
```
audience:bitmap:{audienceId}  →  Roaring Bitmap 序列化字节（STRING）
audience:bitmap:{audienceId}:meta  →  计算时间戳（用于判断是否需要刷新）
```

---

## 规则 JSON 格式

```json
{
  "logic": "AND",
  "conditions": [
    { "field": "last_purchase_days", "op": "<=",  "value": 30 },
    { "field": "city",               "op": "IN",  "value": ["Beijing", "Shanghai"] },
    { "field": "vip_level",          "op": ">=",  "value": 2 }
  ],
  "groups": [
    {
      "logic": "OR",
      "conditions": [
        { "field": "has_coupon",   "op": "=", "value": true },
        { "field": "order_count",  "op": ">", "value": 5 }
      ]
    }
  ]
}
```

支持嵌套 `groups`，`logic` 为 AND/OR，`conditions` 为叶节点。

---

## 规则引擎：AviatorScript + QLExpress 双引擎

```java
interface RuleEvaluator {
    boolean evaluate(String ruleJson, Map<String, Object> context);
}

@Component("AVIATOR")
class AviatorRuleEvaluator implements RuleEvaluator {
    // JSON 规则 → Aviator 表达式字符串 → AviatorEvaluator.execute()
    // 简单字段条件，性能极高
}

@Component("QL")
class QLExpressRuleEvaluator implements RuleEvaluator {
    // JSON 规则 → QLExpress 脚本 → ExpressRunner.execute()
    // 支持自定义操作符，如 "用户在活跃城市" 等业务函数
}

@Component
class RuleEvaluatorRouter {
    Map<String, RuleEvaluator> evaluators; // Spring 自动注入
    RuleEvaluator get(String engineType) { return evaluators.get(engineType); }
}
```

**规则转表达式示例（JSON → Aviator）：**
```java
// {"field":"last_purchase_days","op":"<=","value":30}
// → "last_purchase_days <= 30"

// {"field":"city","op":"IN","value":["Beijing","Shanghai"]}
// → "include(city_list, city)"  // city_list 注入为上下文变量
```

---

## 批量计算任务（AudienceBatchComputeJob）

### 触发方式
- **定时**：每个 `AudienceDefinition.cronExpression` 注册独立调度任务（复用 `CanvasSchedulerService` 模式）
- **手动**：管理员在 UI 点击"立即计算"，调用 `POST /canvas/audiences/{id}/compute`
- **变更触发**：规则保存时自动触发一次重新计算

### JDBC 数据源计算流程（直接 SQL）

```
1. 读取 audience_definition（ruleJson + dataSourceConfig）
2. JSON 规则 → SQL WHERE 子句（SqlWhereGenerator）
3. 执行: SELECT user_id FROM {base_table} WHERE {where_clause} LIMIT {max}
4. 每批 10000 个 userId → MurmurHash → 写入 RoaringBitmap
5. 序列化 Bitmap → redis.set("audience:bitmap:{id}", bytes)
6. 更新 audience_stat（size、computed_at、status=READY）
```

### Tagger API 数据源计算流程（分页扫描）

```
1. 读取种子标签（规则中的第一个 tagCode 作为起点）
2. 分页调 GET /offline/users?tagCode={seed}&page=N&size=1000
3. 每批用户：批量查所有 tagCode 的值（复用 TaggerOfflineHandler）
4. 规则引擎评估每个用户 → 命中则 bitmap.add(murmurHash(userId))
5. 序列化存 Redis + 更新 audience_stat
```

### 安全保护
- **规模预估**：正式计算前先采样 1%，估算总人群数。超过阈值（默认 5000 万）要求管理员二次确认
- **超时保护**：单次计算最长 2 小时，超时标记 FAILED
- **并发控制**：同一人群不允许并发计算（Redis SETNX 分布式锁）

### Roaring Bitmap 依赖
```xml
<dependency>
    <groupId>org.roaringbitmap</groupId>
    <artifactId>RoaringBitmap</artifactId>
    <version>1.0.0</version>
</dependency>
```

### UserId → Int 映射
```java
// MurmurHash3 取正整数（无映射表，零依赖）
static int toUid(String userId) {
    int h = Hashing.murmur3_32().hashString(userId, UTF_8).asInt();
    return h == Integer.MIN_VALUE ? 0 : Math.abs(h);
}
```

---

## TAGGER 节点集成

`TAGGER` 节点新增 `audience` 模式，config 格式：
```json
{
  "mode":           "audience",
  "audienceId":     101,
  "hitNextNodeId":  "send_coupon",
  "missNextNodeId": "end"
}
```

`TaggerHandler.executeAsync()` 处理：
1. 从 Redis 读 `audience:bitmap:{audienceId}` → 反序列化 Roaring Bitmap
2. `bitmap.contains(murmurHash(userId))` → true/false
3. 走对应分支

Bitmap 反序列化结果在 JVM 本地缓存（Caffeine，TTL 5 分钟），避免每次触发都反序列化。

---

## 前端 UI 设计

### 页面结构

```
/audiences                  ← 人群列表页
/audiences/new              ← 新建人群
/audiences/{id}/edit        ← 编辑人群
```

### 1. 人群列表页

| 列名 | 说明 |
|------|------|
| 人群名称 | 可搜索 |
| 规模 | 最后一次计算的用户数 |
| 状态 | COMPUTING / READY / FAILED |
| 最后计算 | 时间 |
| 操作 | 编辑 / 立即计算 / 删除 |

### 2. 人群编辑页

分四个 Section：

**① 基本信息**
- 人群名称（Input）
- 描述（TextArea）

**② 数据源配置**
- 数据源类型：`Tagger API`（默认） | `JDBC`
- Tagger API：种子标签 Code（下拉，从 TagDefinition 列表选）
- JDBC：基础查询表名 + 连接配置（DataSource 下拉，从预配置列表选）

**③ 圈选规则（可视化规则构建器）**

使用 `react-querybuilder` + Ant Design 主题：

```tsx
import QueryBuilder from 'react-querybuilder'
import { AntDActionElement, AntDValueEditor } from '@react-querybuilder/antd'

const fields = tagDefinitions.map(tag => ({
  name:  tag.tagCode,
  label: tag.name,
  valueEditorType: tag.tagType === 'number' ? 'number' : 'text',
}))

<QueryBuilder
  fields={fields}
  query={query}
  onQueryChange={setQuery}
  controlElements={{
    actionElement: AntDActionElement,
    valueEditor:   AntDValueEditor,
  }}
/>
```

渲染效果：
```
逻辑: [AND ▾]
  ├─ [近30天消费天数 ▾] [<= ▾] [30      ]  [✕]
  ├─ [城市         ▾] [IN ▾] [北京,上海]  [✕]
  └─ [VIP等级      ▾] [>= ▾] [2       ]  [✕]
  [+ 添加条件]  [+ 添加条件组]
```

**④ 计算配置**
- 计算策略：`离线批量`（默认） | `实时计算` | `混合`
- 规则引擎：`AviatorScript`（默认） | `QLExpress`
- 定时计算：开关 + Cron 表达式选择器（复用 CronBuilder 组件）

### 3. TAGGER 节点配置面板改动

在节点 Config Panel 的 `mode` 选择器里新增 `人群圈选` 选项，选中后：
- 显示"选择人群"下拉框（从 `/canvas/audiences` 拉 READY 状态的人群列表）
- 显示命中分支 / 未命中分支配置（复用现有分支连线逻辑）

---

## 后端 API 清单

| Method | Path | 说明 |
|--------|------|------|
| GET | `/canvas/audiences` | 人群列表（分页） |
| POST | `/canvas/audiences` | 创建人群 |
| PUT | `/canvas/audiences/{id}` | 更新人群定义 |
| DELETE | `/canvas/audiences/{id}` | 删除人群 |
| POST | `/canvas/audiences/{id}/compute` | 手动触发计算 |
| GET | `/canvas/audiences/{id}/stat` | 查询计算状态和规模 |
| GET | `/canvas/audiences/ready` | 拉 READY 状态人群列表（供 TAGGER 节点选择） |

---

## 不在范围内

- 人群包的导入/导出（CSV 导入 userId）
- 人群间集合运算 UI（AND/OR/NOT 组合多个人群）
- 实时数仓对接（Flink）
- A/B 测试人群分组（另一个功能点）
