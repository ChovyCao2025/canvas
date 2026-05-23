# Node Config Sidebar iOS Refinement Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refine the upgraded node config sidebar into the approved `B` direction with a stronger mist-blue header, category-colored header pills, and heavier Apple-style controls, while keeping all existing config behavior unchanged.

**Architecture:** Keep the current inspector structure and data flow intact, but tighten the visual system in `InspectorCards.tsx` and the field-control chrome in `ConfigPanel`. Add a small pure styling helper for shared control tokens so Select/Input styling stays consistent without touching save logic, visibility logic, or schema-driven rendering behavior. Keep header pill colors derived from the existing node category color map so the right-side inspector stays aligned with the left-side library and canvas node language.

**Tech Stack:** React 18, TypeScript, Ant Design 5, Vitest, Vite

---

## File Structure

- Create: `frontend/src/components/config-panel/controlChrome.ts`
- Create: `frontend/src/components/config-panel/controlChrome.test.ts`
- Modify: `frontend/src/components/config-panel/InspectorCards.tsx`
- Modify: `frontend/src/components/config-panel/index.tsx`

## Task 1: Define shared iOS-style control chrome tokens

**Files:**
- Create: `frontend/src/components/config-panel/controlChrome.ts`
- Create: `frontend/src/components/config-panel/controlChrome.test.ts`

- [ ] **Step 1: Write the failing tests for control chrome tokens**

```ts
import { describe, expect, it } from 'vitest'
import { getControlChrome, getControlLabelStyle } from './controlChrome'

describe('controlChrome', () => {
  it('returns the heavier iOS-like select/input shell', () => {
    const chrome = getControlChrome()

    expect(chrome.height).toBe(52)
    expect(chrome.borderRadius).toBe(18)
    expect(chrome.border).toContain('#d8e3f2')
    expect(String(chrome.background)).toContain('linear-gradient')
  })

  it('returns a subdued field label style', () => {
    const labelStyle = getControlLabelStyle()

    expect(labelStyle.fontSize).toBe(11)
    expect(labelStyle.color).toBe('#7b8798')
  })
})
```

- [ ] **Step 2: Run the targeted test to verify it fails**

Run:

```bash
cd frontend && npm test -- src/components/config-panel/controlChrome.test.ts
```

Expected: FAIL because `controlChrome.ts` does not exist yet.

- [ ] **Step 3: Add the shared control chrome helper**

```ts
import type { CSSProperties } from 'react'

export function getControlChrome(): CSSProperties {
  return {
    height: 52,
    borderRadius: 18,
    border: '1px solid #d8e3f2',
    background: 'linear-gradient(180deg,#ffffff 0%,#f6f8fb 100%)',
    boxShadow: 'inset 0 1px 0 rgba(255,255,255,.95), 0 5px 14px rgba(15,23,42,.04)',
  }
}

export function getControlLabelStyle(): CSSProperties {
  return {
    fontSize: 11,
    color: '#7b8798',
  }
}
```

- [ ] **Step 4: Re-run the targeted test**

Run:

```bash
cd frontend && npm test -- src/components/config-panel/controlChrome.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit the styling-token slice**

```bash
git add frontend/src/components/config-panel/controlChrome.ts frontend/src/components/config-panel/controlChrome.test.ts
git commit -m "feat: add config panel control chrome tokens"
```

## Task 2: Refine the inspector header to the approved mist-blue direction

**Files:**
- Modify: `frontend/src/components/config-panel/InspectorCards.tsx`

- [ ] **Step 1: Add a failing build check by updating the header API shape first**

Add the new mist-blue token usage and remove the now-rejected right-side badge block shape from the Tagger header variant in `InspectorCards.tsx`, then run:

```bash
cd frontend && npm run build
```

Expected: FAIL until the full header style refactor is finished.

- [ ] **Step 2: Implement the approved `B` header styling**

Update `NodeHeaderCard` so the `tagger` tone becomes:

- Mist-blue header shell instead of saturated blue
- No standalone `T` identifier block
- `已配置` stays as a small pill in the top-right
- `Tagger` badge, title, meta pills, and weak supporting copy remain

Use this target style direction:

```tsx
const TAGGER_STYLES = {
  shell: {
    background: '#e9f0f7',
    border: '1px solid #d9e4ef',
  },
  badge: {
    background: '#f6f9fc',
    color: '#345472',
    border: '1px solid #d4e1ee',
  },
  status: {
    background: '#f6f9fc',
    color: '#345472',
    border: '1px solid #d4e1ee',
  },
  title: '#17324d',
  description: '#6e8093',
}
```

Keep non-`tagger` headers restrained and functional.

Also add category-colored pill support for all header variants:

- `typeBadge` and `已配置` use the current node category color family
- `TAGGER` keeps the mist-blue shell but its pills still follow the node category color family
- `已配置` remains one visual step weaker than the main type badge

- [ ] **Step 3: Re-run the frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS.

- [ ] **Step 4: Commit the header refinement**

```bash
git add frontend/src/components/config-panel/InspectorCards.tsx
git commit -m "feat: refine config panel mist-blue header"
```

## Task 3: Apply heavy iOS-style chrome to form controls

**Files:**
- Modify: `frontend/src/components/config-panel/index.tsx`
- Test: `frontend/src/components/config-panel/controlChrome.test.ts`

- [ ] **Step 1: Extend the token test with a select-arrow-friendly control shape**

```ts
it('keeps enough horizontal padding for select affordances', () => {
  const chrome = getControlChrome()

  expect(chrome.height).toBe(52)
  expect(chrome.borderRadius).toBe(18)
})
```

- [ ] **Step 2: Run the targeted token test before wiring it into controls**

Run:

```bash
cd frontend && npm test -- src/components/config-panel/controlChrome.test.ts
```

Expected: PASS.

- [ ] **Step 3: Use the shared chrome in `ConfigPanel` controls without changing behavior**

In `index.tsx`, import the new helper:

```tsx
import { getControlChrome, getControlLabelStyle } from './controlChrome'
```

Apply it to:

- `Input`
- `Select`
- `InputNumber`
- the control labels rendered around them

Implementation constraints:

- Do not change `handleValuesChange`
- Do not change `evaluateVisible`
- Do not change `parseSchema`
- Do not change `renderControl` branching logic
- Do not change `displayValues.ts`
- Do not change `presentation.ts`

For example:

```tsx
const controlChrome = getControlChrome()

<Input style={controlChrome} />

<Select
  style={controlChrome}
  options={normalizeFieldOptions(field, options)}
  placeholder={`请选择${field.label}`}
/>
```

And for label wrappers:

```tsx
<div style={{ ...getControlLabelStyle(), margin: '0 6px 6px' }}>{field.label}</div>
```

Keep all current values, validation, and field names intact.

- [ ] **Step 4: Run focused verification**

Run:

```bash
cd frontend && npm test -- src/components/config-panel/controlChrome.test.ts src/components/config-panel/displayValues.test.ts src/components/config-panel/presentation.test.ts
cd frontend && npm run build
```

Expected: all PASS.

- [ ] **Step 5: Commit the control refinement**

```bash
git add frontend/src/components/config-panel/index.tsx frontend/src/components/config-panel/controlChrome.test.ts frontend/src/components/config-panel/controlChrome.ts
git commit -m "feat: apply ios-style chrome to config controls"
```

## Task 4: Verify visual refinement without behavior regression

**Files:**
- Modify: `frontend/src/components/config-panel/controlChrome.test.ts`
- Modify: `frontend/src/components/config-panel/presentation.test.ts`

- [ ] **Step 1: Add a regression test for non-audience Tagger badge labels staying human-readable**

```ts
it('prefers display labels for non-audience tagger mode badges', () => {
  const model = buildConfigPanelPresentation({
    nodeData: taggerNode(),
    formValues: { mode: 'realtime' },
    displayValues: { mode: '实时触发（监听 MQ 事件）' },
    fields: [],
    getNodeName: () => null,
  })

  expect(model.header.metaBadges).toEqual(['实时触发（监听 MQ 事件）'])
})
```

- [ ] **Step 2: Add a regression test for category-colored header pills**

```ts
it('uses category color accents for non-tagger header pills', () => {
  const accent = CATEGORY_SOLID['行为策略']

  expect(accent).toBe('#14b8a6')
})
```

- [ ] **Step 3: Add a token regression test for the mist-blue header palette**

```ts
it('keeps the approved iOS control shell restrained rather than saturated', () => {
  const chrome = getControlChrome()

  expect(String(chrome.border)).toContain('#d8e3f2')
  expect(String(chrome.background)).toContain('#f6f8fb')
})
```

- [ ] **Step 4: Run full frontend verification**

Run:

```bash
cd frontend && npm test
cd frontend && npm run build
```

Expected: all tests PASS and build PASS.

- [ ] **Step 5: Do manual visual verification in the running app**

Check these cases:

```text
1. TAGGER header uses mist-blue styling, with no right-side T block.
2. TAGGER / 已配置 pills use the same category color family, with 已配置 weaker.
3. Select/Input/InputNumber controls look thicker, rounder, and more unified.
4. DIRECT_CALL and non-TAGGER headers use their category color rather than neutral gray.
5. Existing editing, node switching, and readonly behavior are unchanged.
```

- [ ] **Step 6: Commit the regression-verification slice**

```bash
git add frontend/src/components/config-panel/controlChrome.test.ts frontend/src/components/config-panel/presentation.test.ts
git commit -m "test: verify ios refinement regression coverage"
```
