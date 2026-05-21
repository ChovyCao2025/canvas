# TriggerPreCheck 限制配置 UI Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让运营人员能在画布编辑页的设置弹窗中配置 TriggerPreCheck 的 6 个执行限制字段，配置后保存时写入数据库并生效。

**Architecture:** 后端 CanvasUpdateReq + updateDraft() 补字段 → 前端 TS 类型 + 设置弹窗补表单项。

**Tech Stack:** Java 17, Spring WebFlux, React 18, Ant Design (DatePicker, InputNumber)

---

## File Map

| Action | File |
|--------|------|
| Modify | `backend/.../dto/CanvasUpdateReq.java` |
| Modify | `backend/.../domain/canvas/CanvasService.java` |
| Modify | `frontend/src/types/index.ts` |
| Modify | `frontend/src/pages/canvas-editor/index.tsx` |

路径前缀：`backend/canvas-engine/src/main/java/org/chovy/canvas/`

---

## Task 1：后端 — CanvasUpdateReq 补字段

**Files:**
- Modify: `dto/CanvasUpdateReq.java`

- [ ] **Step 1：在 CanvasUpdateReq 末尾添加 6 个字段**

在现有 `cronExpression` 字段之后追加：

```java
import java.time.LocalDateTime;

// 执行限制（对应 TriggerPreCheckService 的 6 项检查）
private LocalDateTime validStart;
private LocalDateTime validEnd;
private Integer maxTotalExecutions;
private Integer perUserDailyLimit;
private Integer perUserTotalLimit;
private Integer cooldownSeconds;
```

- [ ] **Step 2：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/dto/CanvasUpdateReq.java
git commit -m "feat: add execution limit fields to CanvasUpdateReq"
```

---

## Task 2：后端 — updateDraft() 写入限制字段

**Files:**
- Modify: `domain/canvas/CanvasService.java`

- [ ] **Step 1：在 updateDraft() 中补写 6 个字段（约 L102 之后）**

在 `if (req.getTriggerType() != null) canvas.setTriggerType(...)` 之后，`canvasMapper.updateById(canvas)` 之前，追加：

```java
// 执行限制（null = 不限制，允许显式清空）
canvas.setValidStart(req.getValidStart());
canvas.setValidEnd(req.getValidEnd());
canvas.setMaxTotalExecutions(req.getMaxTotalExecutions());
canvas.setPerUserDailyLimit(req.getPerUserDailyLimit());
canvas.setPerUserTotalLimit(req.getPerUserTotalLimit());
canvas.setCooldownSeconds(req.getCooldownSeconds());
```

- [ ] **Step 2：编译验证**

```bash
cd backend && mvn compile -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3：运行全量测试，确认无回归**

```bash
cd backend && mvn test -pl canvas-engine -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4：Commit**

```bash
git add backend/canvas-engine/src/main/java/org/chovy/canvas/domain/canvas/CanvasService.java
git commit -m "feat: write execution limit fields in updateDraft()"
```

---

## Task 3：前端 — TS 类型补字段

**Files:**
- Modify: `frontend/src/types/index.ts`

- [ ] **Step 1：在 Canvas interface 中追加 6 个可选字段**

在已有的 `editVersion?: number` 之后追加：

```typescript
validStart?: string
validEnd?: string
maxTotalExecutions?: number
perUserDailyLimit?: number
perUserTotalLimit?: number
cooldownSeconds?: number
```

- [ ] **Step 2：验证 TypeScript 无报错**

```bash
cd frontend && npx tsc --noEmit
```

Expected: 无输出

- [ ] **Step 3：Commit**

```bash
git add frontend/src/types/index.ts
git commit -m "feat: add execution limit fields to Canvas TypeScript interface"
```

---

## Task 4：前端 — 设置弹窗补表单项

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1：补充 import**

在顶部 antd import 行中加入 `DatePicker` 和 `InputNumber`（若未引入）：

```typescript
import {
  Button, Table, Tag, Space, Modal, Form, Input,
  message, Typography, Tooltip, Dropdown, DatePicker, InputNumber, Divider,
} from 'antd'
```

同时引入 dayjs（Ant Design DatePicker 依赖）：

```typescript
import dayjs from 'dayjs'
```

- [ ] **Step 2：修改 openSettings 函数，加载限制字段初始值**

将 `openSettings` 改为：

```typescript
const openSettings = () => {
  const c = detail.canvas
  settingsForm.setFieldsValue({
    triggerType:       c.triggerType    ?? 'REALTIME',
    cronExpression:    c.cronExpression ?? '',
    validRange: c.validStart && c.validEnd
      ? [dayjs(c.validStart), dayjs(c.validEnd)]
      : undefined,
    maxTotalExecutions: c.maxTotalExecutions ?? undefined,
    perUserDailyLimit:  c.perUserDailyLimit  ?? undefined,
    perUserTotalLimit:  c.perUserTotalLimit   ?? undefined,
    cooldownSeconds:    c.cooldownSeconds     ?? undefined,
  })
  setSettingsOpen(true)
}
```

- [ ] **Step 3：修改 saveSettings 函数，传入限制字段**

将 `saveSettings` 改为：

```typescript
const saveSettings = async () => {
  const vals = settingsForm.getFieldsValue()
  const [validStart, validEnd] = vals.validRange
    ? [vals.validRange[0].toISOString(), vals.validRange[1].toISOString()]
    : [undefined, undefined]
  await canvasApi.update(canvasId, {
    triggerType:       vals.triggerType,
    cronExpression:    vals.triggerType === 'SCHEDULED' ? vals.cronExpression : undefined,
    validStart,
    validEnd,
    maxTotalExecutions: vals.maxTotalExecutions ?? null,
    perUserDailyLimit:  vals.perUserDailyLimit  ?? null,
    perUserTotalLimit:  vals.perUserTotalLimit   ?? null,
    cooldownSeconds:    vals.cooldownSeconds     ?? null,
  })
  message.success('设置已保存')
  setSettingsOpen(false)
}
```

- [ ] **Step 4：在设置 Modal 的 Form 中追加执行限制表单项**

将 Modal 标题改为 `"画布设置"`，并在现有 triggerType 表单项之后追加：

```tsx
<Divider style={{ margin: '16px 0 12px' }}>执行限制（留空表示不限制）</Divider>

<Form.Item label="有效期" name="validRange">
  <DatePicker.RangePicker
    showTime
    format="YYYY-MM-DD HH:mm"
    style={{ width: '100%' }}
    placeholder={['开始时间', '结束时间']}
  />
</Form.Item>

<Form.Item label="全局最大执行次数" name="maxTotalExecutions">
  <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
</Form.Item>

<Form.Item label="用户每日触发上限" name="perUserDailyLimit">
  <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
</Form.Item>

<Form.Item label="用户总触发上限" name="perUserTotalLimit">
  <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
</Form.Item>

<Form.Item label="冷却期（秒）" name="cooldownSeconds">
  <InputNumber min={0} style={{ width: '100%' }} placeholder="不限制" />
</Form.Item>
```

- [ ] **Step 5：验证 TypeScript 无报错**

```bash
cd frontend && npx tsc --noEmit
```

Expected: 无输出

- [ ] **Step 6：手动测试**

启动 dev server → 打开任意画布 → 点击顶栏设置按钮 → 弹窗标题为"画布设置" → 填写有效期、每日上限、冷却期 → 点击"保存" → 再次打开设置弹窗，确认填写的值已回填。

同时验证：原触发方式（REALTIME/SCHEDULED）和 cron 表达式配置正常工作。

- [ ] **Step 7：Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "feat: add execution limit fields to canvas settings modal"
```
