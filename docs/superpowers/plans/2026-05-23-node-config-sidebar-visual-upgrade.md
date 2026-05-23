# Node Config Sidebar Visual Upgrade Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Upgrade the right-side node config sidebar to the approved inspector-style UI, including the emphasized `TAGGER` header and clearer branch result cards, without changing existing config behavior.

**Architecture:** Keep all schema-driven form behavior inside `ConfigPanel`, but move presentation decisions into a small pure helper so the visual rules are testable. Add focused view components for the header, section cards, summary rows, and branch route cards, then wire them into the existing form render flow without touching save, visibility, or field update logic.

**Tech Stack:** React 18, TypeScript, Ant Design 5, Vitest, Vite

---

## File Structure

- Create: `frontend/src/components/config-panel/presentation.ts`
- Create: `frontend/src/components/config-panel/presentation.test.ts`
- Create: `frontend/src/components/config-panel/InspectorCards.tsx`
- Modify: `frontend/src/components/config-panel/index.tsx`

## Task 1: Add a pure presentation model for the sidebar

**Files:**
- Create: `frontend/src/components/config-panel/presentation.ts`
- Create: `frontend/src/components/config-panel/presentation.test.ts`

- [ ] **Step 1: Write the failing presentation tests for `TAGGER` emphasis and branch fallbacks**

```ts
import { describe, expect, it } from 'vitest'
import type { CanvasNodeData } from '../../types/canvas'
import { buildConfigPanelPresentation } from './presentation'

const taggerNode = (overrides: Partial<CanvasNodeData> = {}): CanvasNodeData => ({
  nodeType: 'TAGGER',
  name: '是否高价值近30天活跃用户',
  category: '逻辑分支',
  bizConfig: {
    mode: 'audience',
    audienceId: 'audience_vip',
    hitNextNodeId: 'api-node',
    missNextNodeId: 'city-node',
  },
  ...overrides,
})

describe('buildConfigPanelPresentation', () => {
  it('builds the approved tagger header and summary rows', () => {
    const model = buildConfigPanelPresentation({
      nodeData: taggerNode(),
      formValues: {
        mode: '人群圈选',
        audienceId: '演示：高价值近30天活跃用户',
      },
      fields: [
        { key: 'mode', label: '标签模式', type: 'select' },
        { key: 'audienceId', label: '人群', type: 'select' },
      ],
      getNodeName: (id) => ({ 'api-node': '接口调用', 'city-node': '是否高频消费城市用户' }[id ?? ''] ?? null),
    })

    expect(model.header.tone).toBe('tagger')
    expect(model.header.typeBadge).toBe('Tagger')
    expect(model.header.title).toBe('是否高价值近30天活跃用户')
    expect(model.header.metaBadges).toEqual(['人群圈选', 'Audience Segment'])
    expect(model.header.description).toBe('标签判断节点，根据圈选人群决定后续分支流向')
    expect(model.summaryRows).toEqual([
      { label: '标签模式', value: '人群圈选' },
      { label: '人群', value: '演示：高价值近30天活跃用户' },
    ])
    expect(model.branchRoutes).toEqual([
      { label: '命中分支', value: '接口调用', tone: 'success' },
      { label: '未命中分支', value: '是否高频消费城市用户', tone: 'danger' },
    ])
  })

  it('falls back to 未连接 when a tagger branch target is missing', () => {
    const model = buildConfigPanelPresentation({
      nodeData: taggerNode({
        bizConfig: { mode: 'audience', hitNextNodeId: undefined, missNextNodeId: 'city-node' },
      }),
      formValues: {},
      fields: [],
      getNodeName: () => null,
    })

    expect(model.branchRoutes).toEqual([
      { label: '命中分支', value: '未连接', tone: 'success' },
      { label: '未命中分支', value: '未连接', tone: 'danger' },
    ])
  })

  it('keeps non-tagger nodes on the default inspector path', () => {
    const model = buildConfigPanelPresentation({
      nodeData: {
        nodeType: 'API_CALL',
        name: '接口调用',
        category: '行为策略',
        bizConfig: {},
      },
      formValues: {},
      fields: [],
      getNodeName: () => null,
    })

    expect(model.header.tone).toBe('default')
    expect(model.summaryRows).toEqual([])
    expect(model.branchRoutes).toEqual([])
  })
})
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run:

```bash
cd frontend && npm test -- src/components/config-panel/presentation.test.ts
```

Expected: FAIL because `presentation.ts` does not exist yet.

- [ ] **Step 3: Add the pure presentation helper**

```ts
import type { CanvasNodeData } from '../../types/canvas'

type SchemaField = { key: string; label: string; type: string }

export interface ConfigPanelPresentation {
  header: {
    tone: 'default' | 'tagger'
    typeBadge: string
    title: string
    metaBadges: string[]
    description?: string
    statusLabel: string
  }
  summaryRows: Array<{ label: string; value: string }>
  branchRoutes: Array<{ label: string; value: string; tone: 'success' | 'danger' }>
}

const TAGGER_SUMMARY_LABELS = new Set(['标签模式', '人群'])

function toDisplayValue(value: unknown): string {
  if (typeof value === 'string' && value.trim()) return value.trim()
  if (typeof value === 'number' || typeof value === 'boolean') return String(value)
  return '未设置'
}

function resolveTaggerMetaBadges(formValues: Record<string, unknown>): string[] {
  const mode = String(formValues.mode ?? '')
  if (mode === '人群圈选') return ['人群圈选', 'Audience Segment']
  return mode ? [mode] : []
}

export function buildConfigPanelPresentation(input: {
  nodeData: CanvasNodeData
  formValues: Record<string, unknown>
  fields: SchemaField[]
  getNodeName: (id: string | undefined) => string | null
}): ConfigPanelPresentation {
  const { nodeData, formValues, fields, getNodeName } = input
  const isTagger = nodeData.nodeType === 'TAGGER'

  const summaryRows = isTagger
    ? fields
        .filter((field) => TAGGER_SUMMARY_LABELS.has(field.label))
        .map((field) => ({ label: field.label, value: toDisplayValue(formValues[field.key]) }))
    : []

  const branchRoutes = isTagger
    ? [
        {
          label: '命中分支',
          value: getNodeName(nodeData.bizConfig.hitNextNodeId as string | undefined) ?? '未连接',
          tone: 'success' as const,
        },
        {
          label: '未命中分支',
          value: getNodeName(nodeData.bizConfig.missNextNodeId as string | undefined) ?? '未连接',
          tone: 'danger' as const,
        },
      ]
    : []

  return {
    header: {
      tone: isTagger ? 'tagger' : 'default',
      typeBadge: isTagger ? 'Tagger' : nodeData.nodeType,
      title: nodeData.name,
      metaBadges: isTagger ? resolveTaggerMetaBadges(formValues) : [],
      description: isTagger ? '标签判断节点，根据圈选人群决定后续分支流向' : nodeData.category,
      statusLabel: '已配置',
    },
    summaryRows,
    branchRoutes,
  }
}
```

- [ ] **Step 4: Re-run the targeted test**

Run:

```bash
cd frontend && npm test -- src/components/config-panel/presentation.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit the pure presentation slice**

```bash
git add frontend/src/components/config-panel/presentation.ts frontend/src/components/config-panel/presentation.test.ts
git commit -m "feat: add config panel presentation model"
```

## Task 2: Add focused inspector view components

**Files:**
- Create: `frontend/src/components/config-panel/InspectorCards.tsx`
- Modify: `frontend/src/components/config-panel/index.tsx`

- [ ] **Step 1: Wire the new component imports into `ConfigPanel` first and prove the build fails until the components exist**

```tsx
import {
  BranchRouteCard,
  ConfigSectionCard,
  FieldSummaryRow,
  NodeHeaderCard,
} from './InspectorCards'
import { buildConfigPanelPresentation } from './presentation'

const visibleFields = fields.filter((f) =>
  evaluateVisible(f.visible, formValues) &&
  evaluateVisible(f.showWhen, formValues)
)

const presentation = buildConfigPanelPresentation({
  nodeData,
  formValues,
  fields: visibleFields.map(({ key, label, type }) => ({ key, label, type })),
  getNodeName,
})
```

Run:

```bash
cd frontend && npm run build
```

Expected: FAIL with a module resolution error for `./InspectorCards`, proving the new layout path is actually in use.

- [ ] **Step 2: Add reusable header, section, summary, and branch card components**

```tsx
import type { ReactNode } from 'react'

const panelCardStyle: React.CSSProperties = {
  background: '#fff',
  border: '1px solid #e8eaef',
  borderRadius: 18,
  boxShadow: '0 10px 24px rgba(15,23,42,.06)',
}

export function NodeHeaderCard(props: {
  tone: 'default' | 'tagger'
  typeBadge: string
  title: string
  metaBadges: string[]
  description?: string
  statusLabel: string
}) {
  const isTagger = props.tone === 'tagger'
  return (
    <div style={{ ...panelCardStyle, padding: 18, marginBottom: 14 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12 }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: 8,
            padding: '6px 10px',
            borderRadius: 999,
            background: isTagger ? '#eff6ff' : '#f8fafc',
            border: `1px solid ${isTagger ? '#dbeafe' : '#eaecf0'}`,
            color: isTagger ? '#1d4ed8' : '#475467',
            fontSize: 10,
            fontWeight: 700,
            letterSpacing: '.08em',
            textTransform: 'uppercase',
          }}>
            {props.typeBadge}
          </div>
          <div style={{ fontSize: isTagger ? 18 : 16, fontWeight: 700, color: '#101828', marginTop: 10, lineHeight: 1.35 }}>
            {props.title}
          </div>
          {!!props.metaBadges.length && (
            <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap', marginTop: 10 }}>
              {props.metaBadges.map((badge) => (
                <span key={badge} style={{ padding: '4px 8px', borderRadius: 999, background: '#eef2ff', color: '#4338ca', fontSize: 11, fontWeight: 600 }}>
                  {badge}
                </span>
              ))}
            </div>
          )}
          {props.description && (
            <div style={{ fontSize: 11, color: isTagger ? '#98a2b3' : '#667085', marginTop: 8 }}>
              {props.description}
            </div>
          )}
        </div>
        <div style={{ padding: '6px 10px', borderRadius: 999, background: '#f8fafc', color: '#475467', fontSize: 11, border: '1px solid #eaecf0' }}>
          {props.statusLabel}
        </div>
      </div>
    </div>
  )
}

export function ConfigSectionCard({ title, children }: { title?: string; children: ReactNode }) {
  return (
    <div style={{ ...panelCardStyle, padding: 16, marginBottom: 14 }}>
      {title && <div style={{ fontSize: 12, fontWeight: 700, color: '#667085', marginBottom: 10 }}>{title}</div>}
      {children}
    </div>
  )
}

export function FieldSummaryRow({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ background: '#f8fafc', border: '1px solid #e7edf3', borderRadius: 14, padding: 12 }}>
      <div style={{ fontSize: 11, color: '#667085', marginBottom: 6 }}>{label}</div>
      <div style={{ fontSize: 14, fontWeight: 600, color: '#111827' }}>{value}</div>
    </div>
  )
}

export function BranchRouteCard({ label, value, tone }: { label: string; value: string; tone: 'success' | 'danger' }) {
  const palette = tone === 'success'
    ? { border: '#dbe7ff', background: '#f5f8ff', text: '#175cd3' }
    : { border: '#f0d7da', background: '#fff7f7', text: '#c01048' }
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', border: `1px solid ${palette.border}`, background: palette.background, borderRadius: 12, padding: '10px 12px', gap: 12 }}>
      <span style={{ color: '#344054', fontSize: 13 }}>{label}</span>
      <span style={{ color: palette.text, fontWeight: 600, fontSize: 13, textAlign: 'right' }}>{value}</span>
    </div>
  )
}
```

- [ ] **Step 3: Keep the old form logic intact while preserving the new `visibleFields` and `presentation` wiring**

Keep the existing `Form`, `handleValuesChange`, `renderControl`, and schema loading unchanged in this step.

- [ ] **Step 4: Re-run the frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 5: Commit the view component slice**

```bash
git add frontend/src/components/config-panel/InspectorCards.tsx frontend/src/components/config-panel/index.tsx
git commit -m "feat: add inspector card components for config panel"
```

## Task 3: Apply the approved inspector layout inside `ConfigPanel`

**Files:**
- Modify: `frontend/src/components/config-panel/index.tsx`
- Test: `frontend/src/components/config-panel/presentation.test.ts`

- [ ] **Step 1: Extend the presentation test with the approved weaker tagger description styling rule**

```ts
it('keeps the tagger supporting copy weak and non-interactive', () => {
  const model = buildConfigPanelPresentation({
    nodeData: taggerNode(),
    formValues: { mode: '人群圈选' },
    fields: [],
    getNodeName: () => null,
  })

  expect(model.header.description).toBe('标签判断节点，根据圈选人群决定后续分支流向')
  expect(model.header.metaBadges).toContain('Audience Segment')
})
```

- [ ] **Step 2: Run the targeted test and verify it still passes before the layout change**

Run:

```bash
cd frontend && npm test -- src/components/config-panel/presentation.test.ts
```

Expected: PASS.

- [ ] **Step 3: Replace the flat panel layout with the header card, summary card, and branch result card structure**

```tsx
return (
  <div style={{ padding: '14px 14px 8px', overflowY: 'auto', height: '100%', background: '#f6f7f9' }}>
    {loading && <Spin size="small" style={{ display: 'block', marginBottom: 8 }} />}

    <NodeHeaderCard {...presentation.header} />

    <Form form={form} layout="vertical" size="small" onValuesChange={handleValuesChange} disabled={readonly}>
      {presentation.summaryRows.length > 0 && (
        <ConfigSectionCard title="核心配置">
          <div style={{ display: 'grid', gap: 10 }}>
            {presentation.summaryRows.map((row) => (
              <FieldSummaryRow key={row.label} label={row.label} value={row.value} />
            ))}
          </div>
        </ConfigSectionCard>
      )}

      <ConfigSectionCard title="配置详情">
        <Form.Item name="name" label="节点名称" rules={[{ required: true }]}>
          <Input />
        </Form.Item>

        {visibleFields.map((field) => {
          if (field.type === 'api-input-params') {
            return <ApiCallInputParams key={field.key} label={field.label} apiKeyField={field.apiKeyField ?? 'apiKey'} defsSource={field.defsSource ?? '/meta/api-definitions'} />
          }
          if (field.type === 'event-attr-preview') {
            return (
              <div key={field.key} style={{ marginBottom: 16 }}>
                <div style={{ fontSize: 12, color: '#595959', marginBottom: 6 }}>{field.label}</div>
                <EventAttrPreview />
              </div>
            )
          }
          return (
            <Form.Item
              key={field.key}
              name={field.key}
              label={field.label}
              rules={field.required ? [{ required: true, message: `请填写${field.label}` }] : []}
            >
              {renderControl(field, options, ctxFields, form, getNodeName, nodeData)}
            </Form.Item>
          )
        })}
      </ConfigSectionCard>

      {presentation.branchRoutes.length > 0 && (
        <ConfigSectionCard title="分支结果">
          <div style={{ display: 'grid', gap: 10 }}>
            {presentation.branchRoutes.map((route) => (
              <BranchRouteCard key={route.label} {...route} />
            ))}
          </div>
        </ConfigSectionCard>
      )}
    </Form>
  </div>
)
```

In this step, do not touch:

- `handleValuesChange`
- `evaluateVisible`
- `parseSchema`
- `renderControl`
- the special handling for `api-input-params` and `event-attr-preview`

- [ ] **Step 4: Run the focused tests and the frontend build**

Run:

```bash
cd frontend && npm test -- src/components/config-panel/presentation.test.ts
cd frontend && npm run build
```

Expected: both commands PASS.

- [ ] **Step 5: Commit the integrated sidebar upgrade**

```bash
git add frontend/src/components/config-panel/index.tsx frontend/src/components/config-panel/InspectorCards.tsx frontend/src/components/config-panel/presentation.ts frontend/src/components/config-panel/presentation.test.ts
git commit -m "feat: upgrade node config sidebar visuals"
```

## Task 4: Verify zero regression on representative node types

**Files:**
- Modify: `frontend/src/components/config-panel/presentation.test.ts`

- [ ] **Step 1: Add a regression test for non-`TAGGER` headers staying simple**

```ts
it('does not invent branch cards or summary rows for API_CALL nodes', () => {
  const model = buildConfigPanelPresentation({
    nodeData: {
      nodeType: 'API_CALL',
      name: '接口调用',
      category: '行为策略',
      bizConfig: { apiKey: 'query_user_profile' },
    },
    formValues: { apiKey: 'query_user_profile' },
    fields: [{ key: 'apiKey', label: '接口标识', type: 'select' }],
    getNodeName: () => null,
  })

  expect(model.header.typeBadge).toBe('API_CALL')
  expect(model.header.metaBadges).toEqual([])
  expect(model.summaryRows).toEqual([])
  expect(model.branchRoutes).toEqual([])
})
```

- [ ] **Step 2: Run the targeted test suite**

Run:

```bash
cd frontend && npm test -- src/components/config-panel/presentation.test.ts
```

Expected: PASS.

- [ ] **Step 3: Do the manual zero-regression check in the browser**

Check these cases in the running app:

```text
1. TAGGER 节点：头部是 Tagger 胶囊 + 标题 + 双标签 + 弱说明。
2. TAGGER 节点：命中/未命中未连线时显示“未连接”。
3. API_CALL 节点：没有被错误地渲染成 TAGGER 样式。
4. 只读画布：控件仍不可编辑。
5. 切换节点后，表单值仍随当前节点更新。
```

- [ ] **Step 4: Run the full frontend test and build checks**

Run:

```bash
cd frontend && npm test
cd frontend && npm run build
```

Expected: all tests PASS and build PASS.

- [ ] **Step 5: Commit the regression verification adjustments**

```bash
git add frontend/src/components/config-panel/presentation.test.ts
git commit -m "test: verify config panel visual regression coverage"
```
