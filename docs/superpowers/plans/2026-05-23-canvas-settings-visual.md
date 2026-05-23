# Canvas Settings Visual Refresh Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refresh the canvas settings modal so trigger mode reads as the primary task, execution limits become a collapsible advanced section, and both sections expose light state summaries without changing backend behavior.

**Architecture:** Keep all data flow inside the existing `canvas-editor` page, but pull display-only logic into a small helper module so the advanced-section default state and summary text are testable. Rebuild the modal markup around two card sections and add a page-local stylesheet for the new layout, selected states, and collapsible container.

**Tech Stack:** React 18, TypeScript, Ant Design 5, Vite, Vitest

---

## File Map

- Create: `frontend/src/pages/canvas-editor/settingsPresentation.ts`
  - Pure helpers for execution-limit summary count, collapsed/expanded default state, and trigger summary text.
- Create: `frontend/src/pages/canvas-editor/settingsPresentation.test.ts`
  - Vitest coverage for the helper behavior.
- Create: `frontend/src/pages/canvas-editor/settingsPanel.css`
  - Page-local styles for section cards, option cards, summary pills, and advanced-section layout.
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
  - Import helper/style module, replace the existing stacked form UI with the new two-section modal, wire the collapsible advanced section, and keep save behavior intact.

### Task 1: Extract and Test Settings Presentation Logic

**Files:**
- Create: `frontend/src/pages/canvas-editor/settingsPresentation.ts`
- Test: `frontend/src/pages/canvas-editor/settingsPresentation.test.ts`

- [ ] **Step 1: Write the failing test**

```ts
import { describe, expect, it } from 'vitest'
import {
  countExecutionLimitFields,
  getExecutionLimitsSummary,
  getTriggerTypeSummary,
  shouldExpandExecutionLimits,
  type CanvasSettingsLike,
} from './settingsPresentation'

const base: CanvasSettingsLike = {
  triggerType: 'REALTIME',
  cronExpression: undefined,
  validStart: undefined,
  validEnd: undefined,
  maxTotalExecutions: undefined,
  perUserDailyLimit: undefined,
  perUserTotalLimit: undefined,
  cooldownSeconds: undefined,
}

describe('settingsPresentation', () => {
  it('returns realtime summary label', () => {
    expect(getTriggerTypeSummary('REALTIME')).toBe('当前为实时')
  })

  it('returns scheduled summary label', () => {
    expect(getTriggerTypeSummary('SCHEDULED')).toBe('当前为定时')
  })

  it('counts valid range as one configured item', () => {
    expect(countExecutionLimitFields({
      ...base,
      validStart: '2026-05-23T10:00:00',
      validEnd: '2026-05-24T10:00:00',
    })).toBe(1)
  })

  it('counts individual numeric limits independently', () => {
    expect(countExecutionLimitFields({
      ...base,
      maxTotalExecutions: 10,
      perUserDailyLimit: 2,
      perUserTotalLimit: 8,
      cooldownSeconds: 60,
    })).toBe(4)
  })

  it('summarizes empty execution limits', () => {
    expect(getExecutionLimitsSummary(base)).toBe('未设置限制')
  })

  it('summarizes configured execution limits by count', () => {
    expect(getExecutionLimitsSummary({
      ...base,
      perUserDailyLimit: 2,
      cooldownSeconds: 120,
    })).toBe('已配置 2 项')
  })

  it('keeps advanced section collapsed when nothing is configured', () => {
    expect(shouldExpandExecutionLimits(base)).toBe(false)
  })

  it('expands advanced section when any execution limit is configured', () => {
    expect(shouldExpandExecutionLimits({
      ...base,
      perUserTotalLimit: 3,
    })).toBe(true)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- settingsPresentation.test.ts
```

Expected: FAIL with a module resolution error for `./settingsPresentation`.

- [ ] **Step 3: Write the minimal implementation**

```ts
export interface CanvasSettingsLike {
  triggerType?: string
  cronExpression?: string
  validStart?: string
  validEnd?: string
  maxTotalExecutions?: number
  perUserDailyLimit?: number
  perUserTotalLimit?: number
  cooldownSeconds?: number
}

export function getTriggerTypeSummary(triggerType?: string): string {
  return triggerType === 'SCHEDULED' ? '当前为定时' : '当前为实时'
}

export function countExecutionLimitFields(settings: CanvasSettingsLike): number {
  let count = 0

  if (settings.validStart || settings.validEnd) count += 1
  if (settings.maxTotalExecutions != null) count += 1
  if (settings.perUserDailyLimit != null) count += 1
  if (settings.perUserTotalLimit != null) count += 1
  if (settings.cooldownSeconds != null) count += 1

  return count
}

export function shouldExpandExecutionLimits(settings: CanvasSettingsLike): boolean {
  return countExecutionLimitFields(settings) > 0
}

export function getExecutionLimitsSummary(settings: CanvasSettingsLike): string {
  const count = countExecutionLimitFields(settings)
  return count > 0 ? `已配置 ${count} 项` : '未设置限制'
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- settingsPresentation.test.ts
```

Expected: PASS with 8 passing assertions.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/canvas-editor/settingsPresentation.ts frontend/src/pages/canvas-editor/settingsPresentation.test.ts
git commit -m "test: cover canvas settings presentation helpers"
```

### Task 2: Rebuild the Modal Structure Around Two Section Cards

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Create: `frontend/src/pages/canvas-editor/settingsPanel.css`

- [ ] **Step 1: Write the failing test for helper-backed defaults**

Because this page does not currently have component-level UI tests, lock down the modal-specific default behavior by extending the helper test before touching JSX:

```ts
it('expands advanced section when valid start is set without end', () => {
  expect(shouldExpandExecutionLimits({
    ...base,
    validStart: '2026-05-23T10:00:00',
  })).toBe(true)
})
```

- [ ] **Step 2: Run test to verify it fails**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- settingsPresentation.test.ts
```

Expected: FAIL because the new test is not yet present in the file.

- [ ] **Step 3: Update the helper test and keep it passing**

Add the test above to `frontend/src/pages/canvas-editor/settingsPresentation.test.ts`, then re-run the same command until the suite passes. This gives the modal rebuild a guardrail for the “configured advanced section opens by default” rule.

- [ ] **Step 4: Replace the stacked modal markup**

In `frontend/src/pages/canvas-editor/index.tsx`, add the new imports:

```ts
import { CaretRightOutlined, DownOutlined } from '@ant-design/icons'
import './settingsPanel.css'
import {
  getExecutionLimitsSummary,
  getTriggerTypeSummary,
  shouldExpandExecutionLimits,
} from './settingsPresentation'
```

Then add local state near the existing settings form state:

```ts
  const [limitsExpanded, setLimitsExpanded] = useState(false)
```

Update `openSettings()` so it seeds both the form and the advanced-section state from persisted settings:

```ts
  const nextSettings = {
    triggerType: detail.canvas.triggerType ?? 'REALTIME',
    cronExpression: detail.canvas.cronExpression ?? undefined,
    validStart: detail.canvas.validStart ?? undefined,
    validEnd: detail.canvas.validEnd ?? undefined,
    maxTotalExecutions: detail.canvas.maxTotalExecutions ?? undefined,
    perUserDailyLimit: detail.canvas.perUserDailyLimit ?? undefined,
    perUserTotalLimit: detail.canvas.perUserTotalLimit ?? undefined,
    cooldownSeconds: detail.canvas.cooldownSeconds ?? undefined,
  }

  settingsForm.setFieldsValue({
    triggerType: nextSettings.triggerType,
    cronExpression: nextSettings.cronExpression ?? '',
    validRange: validStart || validEnd ? [validStart, validEnd] : undefined,
    maxTotalExecutions: nextSettings.maxTotalExecutions,
    perUserDailyLimit: nextSettings.perUserDailyLimit,
    perUserTotalLimit: nextSettings.perUserTotalLimit,
    cooldownSeconds: nextSettings.cooldownSeconds,
  })
  setLimitsExpanded(shouldExpandExecutionLimits(nextSettings))
  setSettingsOpen(true)
```

Finally replace the modal body with a two-card layout:

```tsx
      <Modal
        title="画布设置"
        open={settingsOpen}
        onOk={saveSettings}
        onCancel={() => setSettingsOpen(false)}
        okText="保存"
        cancelText="取消"
        width={560}
      >
        <Form form={settingsForm} layout="vertical" className="canvas-settings-form">
          <section className="settings-section-card">
            <div className="settings-section-header">
              <div>
                <div className="settings-section-title">触发方式</div>
                <div className="settings-section-help">决定画布是实时响应还是按计划批量执行</div>
              </div>
              <Tag className="settings-summary-tag" bordered={false}>
                {getTriggerTypeSummary(settingsForm.getFieldValue('triggerType') ?? canvasSettings.triggerType)}
              </Tag>
            </div>

            <Form.Item name="triggerType" initialValue="REALTIME" className="settings-trigger-group">
              <Radio.Group className="settings-trigger-options">
                <Radio.Button value="REALTIME" className="settings-trigger-option">
                  <span className="settings-trigger-option-title">实时触发</span>
                  <span className="settings-trigger-option-help">收到事件后立即执行，适合行为驱动场景</span>
                </Radio.Button>
                <Radio.Button value="SCHEDULED" className="settings-trigger-option">
                  <span className="settings-trigger-option-title">定时触发</span>
                  <span className="settings-trigger-option-help">按固定时间批量执行，适合周期性任务</span>
                </Radio.Button>
              </Radio.Group>
            </Form.Item>

            <Form.Item noStyle shouldUpdate={(prev, cur) => prev.triggerType !== cur.triggerType}>
              {({ getFieldValue }) =>
                getFieldValue('triggerType') === 'SCHEDULED' ? (
                  <div className="settings-inline-panel">
                    <div className="settings-inline-panel-title">执行时间</div>
                    <Form.Item
                      name="cronExpression"
                      rules={[{ required: true, message: '请配置触发时间' }]}
                      style={{ marginBottom: 0 }}
                    >
                      <CronBuilder onChange={cron => settingsForm.setFieldValue('cronExpression', cron)} />
                    </Form.Item>
                  </div>
                ) : null
              }
            </Form.Item>
          </section>

          <section className="settings-section-card settings-section-card--advanced">
            <button
              type="button"
              className="settings-advanced-toggle"
              onClick={() => setLimitsExpanded(prev => !prev)}
            >
              <div className="settings-section-header settings-section-header--compact">
                <div>
                  <div className="settings-section-title">执行限制</div>
                  <div className="settings-section-help">留空即不限制</div>
                </div>
                <div className="settings-advanced-actions">
                  <Tag className="settings-summary-tag" bordered={false}>
                    {getExecutionLimitsSummary(canvasSettings)}
                  </Tag>
                  {limitsExpanded ? <DownOutlined /> : <CaretRightOutlined />}
                </div>
              </div>
            </button>

            {limitsExpanded ? (
              <div className="settings-advanced-body">
                <div className="settings-subtle-tip">仅在需要控频、限流或限定活动窗口时填写</div>
                <Form.Item label="有效期" name="validRange">
                  <RangePicker
                    showTime
                    format="YYYY-MM-DD HH:mm"
                    placeholder={['开始时间', '结束时间']}
                    style={{ width: '100%' }}
                  />
                </Form.Item>
                <div className="settings-grid">
                  <Form.Item label="总执行次数上限" name="maxTotalExecutions">
                    <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
                  </Form.Item>
                  <Form.Item label="用户每日上限" name="perUserDailyLimit">
                    <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
                  </Form.Item>
                  <Form.Item label="用户总上限" name="perUserTotalLimit">
                    <InputNumber min={1} style={{ width: '100%' }} placeholder="不限制" />
                  </Form.Item>
                  <Form.Item label="冷却秒数" name="cooldownSeconds">
                    <InputNumber min={0} style={{ width: '100%' }} placeholder="不限制" />
                  </Form.Item>
                </div>
              </div>
            ) : null}
          </section>
        </Form>
      </Modal>
```

- [ ] **Step 5: Add the page-local stylesheet**

Create `frontend/src/pages/canvas-editor/settingsPanel.css`:

```css
.canvas-settings-form {
  margin-top: 16px;
  display: grid;
  gap: 14px;
}

.settings-section-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  padding: 16px;
}

.settings-section-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.settings-section-header--compact {
  margin-bottom: 0;
}

.settings-section-title {
  color: #111827;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.settings-section-help {
  margin-top: 4px;
  color: #6b7280;
  font-size: 12px;
  line-height: 20px;
}

.settings-summary-tag.ant-tag {
  margin-inline-end: 0;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  padding-inline: 8px;
  line-height: 22px;
}

.settings-trigger-options {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  width: 100%;
}

.settings-trigger-option.ant-radio-button-wrapper {
  height: auto;
  min-height: 88px;
  padding: 12px 14px;
  border-radius: 12px;
  border-inline-start-width: 1px;
  display: flex;
  flex-direction: column;
  gap: 6px;
  justify-content: flex-start;
}

.settings-trigger-option-title {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
}

.settings-trigger-option-help {
  color: #6b7280;
  font-size: 12px;
  line-height: 18px;
  white-space: normal;
}

.settings-trigger-option.ant-radio-button-wrapper-checked {
  background: #eff6ff;
  border-color: #60a5fa;
  box-shadow: inset 0 0 0 1px rgba(37, 99, 235, 0.16);
}

.settings-inline-panel {
  margin-top: 14px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #f8fbff;
  padding: 14px;
}

.settings-inline-panel-title {
  margin-bottom: 10px;
  color: #334155;
  font-size: 13px;
  font-weight: 600;
}

.settings-advanced-toggle {
  width: 100%;
  padding: 0;
  border: 0;
  background: transparent;
  text-align: left;
}

.settings-advanced-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  color: #64748b;
}

.settings-advanced-body {
  margin-top: 14px;
}

.settings-subtle-tip {
  margin-bottom: 12px;
  padding: 10px 12px;
  border: 1px dashed #cbd5e1;
  border-radius: 10px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
  line-height: 18px;
}

.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 12px;
}

.settings-grid .ant-form-item,
.settings-section-card .ant-form-item {
  margin-bottom: 14px;
}

.settings-section-card .ant-picker,
.settings-section-card .ant-input-number {
  min-height: 40px;
  border-radius: 10px;
}
```

- [ ] **Step 6: Run the targeted tests**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- settingsPresentation.test.ts
```

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx frontend/src/pages/canvas-editor/settingsPanel.css frontend/src/pages/canvas-editor/settingsPresentation.test.ts
git commit -m "feat: refresh canvas settings modal layout"
```

### Task 3: Sync Summaries With Form State and Verify the Modal End-to-End

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: Make the summary tags reflect unsaved edits**

Right now `canvasSettings` only updates after save, which would leave the summary tags stale while the modal is open. Use `Form.useWatch` so the header chips react immediately:

```ts
  const watchedTriggerType = Form.useWatch('triggerType', settingsForm)
  const watchedValidRange = Form.useWatch('validRange', settingsForm)
  const watchedMaxTotalExecutions = Form.useWatch('maxTotalExecutions', settingsForm)
  const watchedPerUserDailyLimit = Form.useWatch('perUserDailyLimit', settingsForm)
  const watchedPerUserTotalLimit = Form.useWatch('perUserTotalLimit', settingsForm)
  const watchedCooldownSeconds = Form.useWatch('cooldownSeconds', settingsForm)

  const liveSettings = useMemo(() => ({
    triggerType: watchedTriggerType ?? canvasSettings.triggerType,
    validStart: watchedValidRange?.[0]?.format('YYYY-MM-DDTHH:mm:ss'),
    validEnd: watchedValidRange?.[1]?.format('YYYY-MM-DDTHH:mm:ss'),
    maxTotalExecutions: watchedMaxTotalExecutions,
    perUserDailyLimit: watchedPerUserDailyLimit,
    perUserTotalLimit: watchedPerUserTotalLimit,
    cooldownSeconds: watchedCooldownSeconds,
  }), [
    watchedTriggerType,
    watchedValidRange,
    watchedMaxTotalExecutions,
    watchedPerUserDailyLimit,
    watchedPerUserTotalLimit,
    watchedCooldownSeconds,
    canvasSettings.triggerType,
  ])
```

Then use `liveSettings` in both summary tags:

```tsx
{getTriggerTypeSummary(liveSettings.triggerType)}
{getExecutionLimitsSummary(liveSettings)}
```

- [ ] **Step 2: Ensure collapse defaults reset on each modal open**

Keep `openSettings()` as the single place that seeds `limitsExpanded`. Do not persist the open/closed toggle across sessions. Verify that `saveSettings()` still updates `canvasSettings` with the saved values so the next open follows persisted data, not temporary UI state.

- [ ] **Step 3: Run the frontend test suite**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test
```

Expected: PASS for existing tests plus `settingsPresentation.test.ts`.

- [ ] **Step 4: Run a production build**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm run build
```

Expected: PASS with Vite build output and no TypeScript errors from the new modal code.

- [ ] **Step 5: Manual verification in the browser**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm run dev
```

Verify in the app:

1. Open any canvas editor page and click the settings button.
2. Confirm the modal width is visibly wider than before and shows two section cards.
3. Confirm “执行限制” is collapsed when no limits are stored.
4. Add `用户每日上限` or `冷却秒数` and confirm the summary chip changes to `已配置 1 项`.
5. Switch to `定时触发` and confirm the cron area appears inside the trigger card instead of below the whole form.
6. Save, reopen the modal, and confirm the advanced section auto-expands when limits are persisted.

- [ ] **Step 6: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "fix: sync canvas settings modal summaries"
```

## Self-Review

- Spec coverage check:
  - Two section cards: Task 2
  - Trigger-mode summary and card-style options: Task 2 and Task 3
  - Cron area visually subordinate to scheduled mode: Task 2
  - Execution limits collapsible by default: Task 1 and Task 2
  - Advanced summary count and default-open-on-configured behavior: Task 1 and Task 3
  - Local-only styling and no backend contract changes: Task 2
- Placeholder scan:
  - No `TODO`, `TBD`, or “implement later” markers remain.
  - Every code-changing step includes concrete snippets or exact commands.
- Type consistency:
  - `CanvasSettingsLike`, `getExecutionLimitsSummary`, `getTriggerTypeSummary`, and `shouldExpandExecutionLimits` are defined once in Task 1 and reused consistently in Tasks 2 and 3.
