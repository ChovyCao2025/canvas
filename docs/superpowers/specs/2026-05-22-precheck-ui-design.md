# TriggerPreCheck 限制配置 UI 设计（优化点 #4）

## 背景

`TriggerPreCheckService.check()` 执行 6 项限制检查，对应 `Canvas` 实体上的字段：
`validStart` / `validEnd` / `maxTotalExecutions` / `perUserDailyLimit` / `perUserTotalLimit` / `cooldownSeconds`。

这 6 个字段已存在于实体和详情接口响应中，但：
- `CanvasUpdateReq` 不接收这些字段 → 无法保存
- `updateDraft()` 不写入这些字段
- 前端 `Canvas` TS 类型缺少这些字段
- 画布编辑页的设置弹窗没有对应表单项

## 解决方案

### 后端

**CanvasUpdateReq** 新增字段：

```java
private LocalDateTime validStart;
private LocalDateTime validEnd;
private Integer maxTotalExecutions;
private Integer perUserDailyLimit;
private Integer perUserTotalLimit;
private Integer cooldownSeconds;
```

**CanvasService.updateDraft()** 补写入逻辑（null 表示不限制，前端显式清空时传 null 可覆盖）：

```java
canvas.setValidStart(req.getValidStart());
canvas.setValidEnd(req.getValidEnd());
canvas.setMaxTotalExecutions(req.getMaxTotalExecutions());
canvas.setPerUserDailyLimit(req.getPerUserDailyLimit());
canvas.setPerUserTotalLimit(req.getPerUserTotalLimit());
canvas.setCooldownSeconds(req.getCooldownSeconds());
```

### 前端

**TS 类型**：`Canvas` interface 补充 6 个字段：

```typescript
validStart?: string
validEnd?: string
maxTotalExecutions?: number
perUserDailyLimit?: number
perUserTotalLimit?: number
cooldownSeconds?: number
```

**设置弹窗**：Modal 标题改为"画布设置"，在触发方式配置下方新增"执行限制"分组，包含：

| 字段 | 表单控件 | 说明 |
|------|---------|------|
| 有效期 | DatePicker.RangePicker | 可为空（不限制） |
| 全局最大执行次数 | InputNumber，min=1 | 可为空（不限制） |
| 用户每日触发上限 | InputNumber，min=1 | 可为空（不限制） |
| 用户总触发上限 | InputNumber，min=1 | 可为空（不限制） |
| 冷却期（秒） | InputNumber，min=0 | 可为空（不限制） |

所有字段为空 = 不限制（对应后端 null）。

**saveSettings** 补充传入这 6 个字段。

## 不在范围内

- TriggerPreCheckService 业务逻辑（已正确实现）
- 限制配置的可视化统计（如剩余配额）
