# Node Config Panel Compact Inspector Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Convert only the right-side node configuration panel to the approved compact inspector style while preserving schema-driven editing behavior.

**Architecture:** Add presentation helpers that classify schema fields into compact groups and derive outlet route summaries. Reuse the existing `ConfigPanel` data flow, but render fields through grouped right-panel cards with smaller control chrome.

**Tech Stack:** React 18, TypeScript, Ant Design 5, Vitest.

---

## File Structure

- Modify `frontend/src/components/config-panel/presentation.ts`: add field grouping and generic outlet route presentation helpers.
- Modify `frontend/src/components/config-panel/InspectorCards.tsx`: replace large iOS cards with compact 8px inspector cards and route rows.
- Modify `frontend/src/components/config-panel/controlChrome.ts`: shrink main and inline control dimensions plus Ant Design overrides.
- Modify `frontend/src/components/config-panel/index.tsx`: render fields by presentation groups and keep existing form data flow.
- Modify tests under `frontend/src/components/config-panel/*.test.ts`: update visual contract tests and add grouping/route tests.

Do not modify left node library, center canvas editor, three-column layout, or `CanvasNode`.

### Task 1: Presentation Model

**Files:**
- Modify: `frontend/src/components/config-panel/presentation.ts`
- Test: `frontend/src/components/config-panel/presentation.test.ts`

- [ ] **Step 1: Write failing tests for grouping and outlet summaries**

Add tests that expect `buildConfigPanelPresentation` to expose grouped fields and non-TAGGER outlet routes:

```ts
expect(model.fieldGroups.map(group => group.title)).toEqual(['基础配置', '条件规则', '参数映射', '预览信息'])
expect(model.branchRoutes).toEqual([
  { label: '条件成立', value: '发送优惠券', tone: 'success' },
  { label: '否则', value: '未连接', tone: 'danger' },
])
expect(model.header.metaBadges).toContain('2 出口')
```

- [ ] **Step 2: Run failing test**

Run: `cd frontend && npm test -- src/components/config-panel/presentation.test.ts`

Expected: FAIL because `fieldGroups` and generic outlet summaries do not exist.

- [ ] **Step 3: Implement presentation helpers**

Add types and helpers:

```ts
export type ConfigPanelFieldGroupKey = 'basic' | 'rules' | 'mapping' | 'preview' | 'advanced'

export interface ConfigPanelFieldGroup {
  key: ConfigPanelFieldGroupKey
  title: string
  summary?: string
  fields: ConfigPanelPresentationField[]
}

const GROUP_TITLES = {
  basic: '基础配置',
  rules: '条件规则',
  mapping: '参数映射',
  preview: '预览信息',
  advanced: '高级配置',
} satisfies Record<ConfigPanelFieldGroupKey, string>
```

Classify field types:

```ts
function resolveGroupKey(type: string): ConfigPanelFieldGroupKey {
  if (['condition-rule-list', 'branch-list', 'priority-list', 'ab-group-list', 'cron', 'delay-input'].includes(type)) return 'rules'
  if (['context-value-list', 'param-define-list', 'key-value', 'api-input-params'].includes(type)) return 'mapping'
  if (['event-attr-preview', 'edge-hint'].includes(type)) return 'preview'
  return 'basic'
}
```

Use `getOutletHandles` and `getOutletTargetField` from `../canvas/outletSchema` to build branch routes for all nodes.

- [ ] **Step 4: Run presentation tests**

Run: `cd frontend && npm test -- src/components/config-panel/presentation.test.ts`

Expected: PASS.

### Task 2: Compact Visual Contracts

**Files:**
- Modify: `frontend/src/components/config-panel/controlChrome.ts`
- Modify: `frontend/src/components/config-panel/InspectorCards.tsx`
- Test: `frontend/src/components/config-panel/controlChrome.test.ts`
- Test: `frontend/src/components/config-panel/presentation.test.ts`

- [ ] **Step 1: Write failing compact style tests**

Update control chrome expectations:

```ts
expect(getControlChrome().height).toBe(40)
expect(getControlChrome().borderRadius).toBe(8)
expect(getInlineControlChrome().height).toBe(32)
```

Update header style tests to expect compact cards:

```ts
expect(styles.cardStyle.borderRadius).toBe(8)
expect(styles.cardStyle.boxShadow).toBe('none')
```

- [ ] **Step 2: Run failing tests**

Run: `cd frontend && npm test -- src/components/config-panel/controlChrome.test.ts src/components/config-panel/presentation.test.ts`

Expected: FAIL because current style is large iOS chrome.

- [ ] **Step 3: Implement compact styles**

Set main controls to `40px`, inline controls to `32px`, card radius to `8px`, and dropdown option height to about `32px`. Keep focus visible with a light blue outline.

- [ ] **Step 4: Run style tests**

Run: `cd frontend && npm test -- src/components/config-panel/controlChrome.test.ts src/components/config-panel/presentation.test.ts`

Expected: PASS.

### Task 3: Grouped Rendering

**Files:**
- Modify: `frontend/src/components/config-panel/index.tsx`
- Test: `frontend/src/components/config-panel/conditionRules.test.ts`
- Test: `frontend/src/components/config-panel/formValues.test.ts`

- [ ] **Step 1: Write or update tests guarding helper behavior**

Keep existing helper tests passing:

```ts
expect(getConditionRuleListFieldKey('rules')).toBe('rules')
expect(buildNodeConfigFormSyncPlan(previousValues, nextNode).values.name).toBe(nextNode.name)
```

- [ ] **Step 2: Render form by `presentation.fieldGroups`**

Move `name` into the `基础配置` section, and render each visible schema field inside its resolved group. Preserve the existing special cases:

```tsx
{presentation.fieldGroups.map(group => (
  <ConfigSectionCard key={group.key} title={group.title} summary={group.summary}>
    {group.fields.map(field => renderField(field))}
  </ConfigSectionCard>
))}
```

Keep `handleValuesChange`, `applyFormPatch`, `renderControl`, `evaluateVisible`, and `Form.Item` names unchanged.

- [ ] **Step 3: Run focused tests**

Run: `cd frontend && npm test -- src/components/config-panel`

Expected: PASS.

### Task 4: Final Verification

**Files:**
- Verify only, no source changes expected.

- [ ] **Step 1: Typecheck and build**

Run: `cd frontend && npm run build`

Expected: PASS.

- [ ] **Step 2: Confirm scope**

Run: `git diff --stat`

Expected: source changes limited to `frontend/src/components/config-panel/*` plus docs/plans. No changes to `frontend/src/components/node-panel/*`, `frontend/src/pages/canvas-editor/index.tsx`, or `frontend/src/components/canvas/CanvasNode.tsx`.

- [ ] **Step 3: Commit implementation**

```bash
git add frontend/src/components/config-panel docs/superpowers/plans/2026-05-27-node-config-panel-compact-inspector.md
git commit -m "feat: compact node config inspector"
```

## Self-Review

- Spec coverage: covers compact right panel styling, all-node grouping, outlet route summaries, unchanged data flow, and right-panel-only scope.
- Placeholder scan: no TBD/TODO placeholders.
- Type consistency: all new presentation fields hang from `ConfigPanelPresentation`; rendering consumes `fieldGroups` with existing `SchemaField` keys.
