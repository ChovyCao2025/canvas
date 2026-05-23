# Node Library Interaction Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebuild the canvas node library for product/operations users with full-list browsing, search/filter/common nodes, category-colored item styling, help affordances, and position-first insertion via edge `+` entry points and branch placeholders.

**Architecture:** Keep the current React Flow editor and metadata API, but move decision-heavy logic into small pure helpers that can be tested independently. The UI work stays in `NodePanel`, `HoverEdge`, `BranchPlaceholderNode`, and `canvas-editor`, with helper modules handling filtering, common-node ranking, and insertion graph rewrites so the large editor file does not absorb more ad hoc logic.

**Tech Stack:** React 18, TypeScript, Ant Design, `@xyflow/react`, Vitest, Vite

---

## File Structure

### Create

- `frontend/src/components/node-panel/nodeLibrary.ts`
  - Pure helper for category list, search/filter, common-node selection, and description fallback.
- `frontend/src/components/node-panel/nodeLibrary.test.ts`
  - Unit tests for the helper above.
- `frontend/src/components/node-panel/NodeLibraryItem.tsx`
  - Focused item renderer for the new compact node row with category color strip and circular help trigger.
- `frontend/src/pages/canvas-editor/insertNode.ts`
  - Pure helper for insertion context resolution and edge rewrite logic.
- `frontend/src/pages/canvas-editor/insertNode.test.ts`
  - Unit tests for insertion behaviors: edge split, empty-canvas create, branch-placeholder attach.

### Modify

- `frontend/src/components/node-panel/index.tsx`
  - Replace collapse list with full-list browser UI.
- `frontend/src/components/canvas/HoverEdge.tsx`
  - Add center `+` affordance without regressing delete-edge behavior.
- `frontend/src/components/canvas/BranchPlaceholderNode.tsx`
  - Tighten visuals and align category color strip / drop affordance language with the spec.
- `frontend/src/pages/canvas-editor/index.tsx`
  - Track insertion context, handle edge `+` workflow, interpret placeholder/blank drops, and call pure insertion helpers.
- `frontend/src/context/CanvasActionsContext.tsx`
  - Extend actions if `HoverEdge` needs an editor callback for “insert on this edge”.

### Verify

- `frontend/src/components/canvas/branchHandles.test.ts`
  - Re-run to ensure placeholder and branch semantics still hold.
- `frontend/package.json`
  - Use existing `vitest run` / `npm test` / `npm run build`; no script changes expected.

---

### Task 1: Extract Node Library View-Model Logic

**Files:**
- Create: `frontend/src/components/node-panel/nodeLibrary.ts`
- Create: `frontend/src/components/node-panel/nodeLibrary.test.ts`
- Verify: `frontend/package.json`

- [ ] **Step 1: Write the failing tests for category/filter/common-node behavior**

```ts
import { describe, expect, it } from 'vitest'
import type { NodeTypeRegistry } from '../../types'
import {
  DEFAULT_COMMON_NODE_TYPES,
  buildCategoryOptions,
  buildNodeLibraryView,
  getNodeSummary,
} from './nodeLibrary'

const nodes: NodeTypeRegistry[] = [
  {
    typeKey: 'API_CALL',
    typeName: '接口调用',
    category: '其他',
    configSchema: '[]',
    outputSchema: '[]',
    isTrigger: 0,
    isTerminal: 0,
    description: '调用外部服务并写回结果',
    enabled: 1,
  },
  {
    typeKey: 'GROOVY',
    typeName: 'Groovy脚本',
    category: '其他',
    configSchema: '[]',
    outputSchema: '[]',
    isTrigger: 0,
    isTerminal: 0,
    description: '',
    enabled: 1,
  },
  {
    typeKey: 'IF_CONDITION',
    typeName: 'IF判断',
    category: '逻辑分支',
    configSchema: '[]',
    outputSchema: '[]',
    isTrigger: 0,
    isTerminal: 0,
    description: '根据条件决定后续分支',
    enabled: 1,
  },
]

describe('nodeLibrary helpers', () => {
  it('always includes 全部 category first', () => {
    expect(buildCategoryOptions(nodes)).toEqual(['全部', '其他', '逻辑分支'])
  })

  it('filters by category and keyword together', () => {
    const view = buildNodeLibraryView(nodes, {
      activeCategory: '其他',
      keyword: 'groovy',
      commonTypeKeys: DEFAULT_COMMON_NODE_TYPES,
    })

    expect(view.filteredNodes.map(node => node.typeKey)).toEqual(['GROOVY'])
  })

  it('returns category-aware common nodes before full list', () => {
    const view = buildNodeLibraryView(nodes, {
      activeCategory: '其他',
      keyword: '',
      commonTypeKeys: ['API_CALL', 'IF_CONDITION'],
    })

    expect(view.commonNodes.map(node => node.typeKey)).toEqual(['API_CALL'])
    expect(view.filteredNodes.map(node => node.typeKey)).toEqual(['API_CALL', 'GROOVY'])
  })

  it('falls back to generic summary when description is empty', () => {
    expect(getNodeSummary(nodes[1])).toBe('处理复杂逻辑或字段加工')
  })
})
```

- [ ] **Step 2: Run the test to confirm it fails**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- src/components/node-panel/nodeLibrary.test.ts
```

Expected: FAIL with module resolution error for `./nodeLibrary` or missing exported functions.

- [ ] **Step 3: Implement the helper module with deterministic outputs**

```ts
import type { NodeTypeRegistry } from '../../types'

export const DEFAULT_COMMON_NODE_TYPES = [
  'API_CALL',
  'DELAY',
  'MANUAL_APPROVAL',
  'SEND_MQ',
]

const SUMMARY_FALLBACK: Record<string, string> = {
  API_CALL: '请求外部服务并拿回结果',
  DELAY: '等待一段时间后继续执行',
  SEND_MQ: '发送一条业务消息给下游系统',
  GROOVY: '处理复杂逻辑或字段加工',
  MANUAL_APPROVAL: '等待人工确认后继续流程',
  CANVAS_TRIGGER: '复用已有流程能力',
  SUB_FLOW_REF: '引用已有子流程片段',
}

export function buildCategoryOptions(nodes: NodeTypeRegistry[]) {
  const categories = Array.from(new Set(nodes.map(node => node.category)))
  return ['全部', ...categories]
}

export function getNodeSummary(node: NodeTypeRegistry) {
  const summary = (node.description ?? '').trim()
  return summary || SUMMARY_FALLBACK[node.typeKey] || node.typeName
}

export function buildNodeLibraryView(
  nodes: NodeTypeRegistry[],
  options: {
    activeCategory: string
    keyword: string
    commonTypeKeys: string[]
  },
) {
  const keyword = options.keyword.trim().toLowerCase()
  const filteredNodes = nodes.filter((node) => {
    if (options.activeCategory !== '全部' && node.category !== options.activeCategory) return false
    if (!keyword) return true
    const haystack = `${node.typeName} ${getNodeSummary(node)}`.toLowerCase()
    return haystack.includes(keyword)
  })

  const commonSet = new Set(options.commonTypeKeys)
  const commonNodes = filteredNodes.filter(node => commonSet.has(node.typeKey))

  return { commonNodes, filteredNodes }
}
```

- [ ] **Step 4: Run the helper tests until they pass**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- src/components/node-panel/nodeLibrary.test.ts
```

Expected: PASS with 4 passing tests.

- [ ] **Step 5: Commit the helper extraction**

```bash
cd /Users/photonpay/project/canvas
git add frontend/src/components/node-panel/nodeLibrary.ts frontend/src/components/node-panel/nodeLibrary.test.ts
git commit -m "test: cover node library filtering helpers"
```

---

### Task 2: Rebuild the Node Panel UI

**Files:**
- Create: `frontend/src/components/node-panel/NodeLibraryItem.tsx`
- Modify: `frontend/src/components/node-panel/index.tsx`
- Reuse: `frontend/src/components/canvas/constants.ts`
- Test: `frontend/src/components/node-panel/nodeLibrary.test.ts`

- [ ] **Step 1: Write a failing component-level expectation around the new item API**

Add this test case to `frontend/src/components/node-panel/nodeLibrary.test.ts` first so the new item props contract is anchored in data:

```ts
it('keeps category color ownership outside the helper layer', () => {
  const view = buildNodeLibraryView(nodes, {
    activeCategory: '全部',
    keyword: '',
    commonTypeKeys: DEFAULT_COMMON_NODE_TYPES,
  })

  expect(view.filteredNodes[0].category).toBe('其他')
})
```

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- src/components/node-panel/nodeLibrary.test.ts
```

Expected: PASS. This anchors that styling decisions stay in the component layer before editing UI files.

- [ ] **Step 2: Implement the compact node item component**

```tsx
import { Popover } from 'antd'
import type { NodeTypeRegistry } from '../../types'
import { CATEGORY_SOLID } from '../canvas/constants'
import { getNodeSummary } from './nodeLibrary'

export default function NodeLibraryItem({
  node,
  draggable = true,
  onDragStart,
}: {
  node: NodeTypeRegistry
  draggable?: boolean
  onDragStart: (node: NodeTypeRegistry) => void
}) {
  const color = CATEGORY_SOLID[node.category] ?? '#722ed1'

  return (
    <div
      draggable={draggable}
      onDragStart={(event) => onDragStart(node)}
      style={{
        display: 'flex',
        gap: 10,
        padding: '12px 14px',
        border: '1px solid #e6e8ee',
        borderRadius: 10,
        background: '#fff',
        cursor: draggable ? 'grab' : 'pointer',
      }}
    >
      <div style={{ width: 8, borderRadius: 999, background: color, alignSelf: 'stretch' }} />
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
          <span style={{ fontSize: 14, fontWeight: 600, color: '#111827' }}>{node.typeName}</span>
          <Popover
            trigger="click"
            content={<div style={{ maxWidth: 240 }}>{node.description || getNodeSummary(node)}</div>}
          >
            <button
              type="button"
              className="nopan nodrag"
              style={{
                width: 18,
                height: 18,
                borderRadius: '50%',
                border: '1px solid #c7cddd',
                background: '#fff',
                color: '#6b7280',
                fontSize: 11,
              }}
            >
              ?
            </button>
          </Popover>
        </div>
        <div style={{ marginTop: 4, fontSize: 12, color: '#6b7280' }}>{getNodeSummary(node)}</div>
      </div>
    </div>
  )
}
```

- [ ] **Step 3: Replace the collapse UI in `NodePanel` with full-list browser controls**

Key implementation shape for `frontend/src/components/node-panel/index.tsx`:

```tsx
const [keyword, setKeyword] = useState('')
const [activeCategory, setActiveCategory] = useState('全部')

const categories = useMemo(() => buildCategoryOptions(nodeTypes), [nodeTypes])
const view = useMemo(() => buildNodeLibraryView(nodeTypes, {
  activeCategory,
  keyword,
  commonTypeKeys: DEFAULT_COMMON_NODE_TYPES,
}), [nodeTypes, activeCategory, keyword])

return (
  <div style={{ height: '100%', overflow: 'auto', padding: 12, background: '#f7f8fa' }}>
    <div style={{ fontSize: 16, fontWeight: 600, marginBottom: 10 }}>添加节点</div>
    <Input
      value={keyword}
      onChange={(event) => setKeyword(event.target.value)}
      placeholder="搜索节点名称 / 功能"
      allowClear
    />
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8, marginTop: 10 }}>
      {categories.map((category) => (
        <Tag.CheckableTag
          key={category}
          checked={activeCategory === category}
          onChange={() => setActiveCategory(category)}
        >
          {category}
        </Tag.CheckableTag>
      ))}
    </div>

    {!!view.commonNodes.length && (
      <>
        <div style={{ margin: '14px 0 8px', fontSize: 12, color: '#6b7280', fontWeight: 600 }}>常用节点</div>
        <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
          {view.commonNodes.map((node) => (
            <Tag key={node.typeKey} style={{ padding: '6px 10px', borderRadius: 999 }}>
              {node.typeName}
            </Tag>
          ))}
        </div>
      </>
    )}

    <div style={{ margin: '14px 0 8px', fontSize: 12, color: '#6b7280', fontWeight: 600 }}>全部节点</div>
    <div style={{ display: 'grid', gap: 10 }}>
      {view.filteredNodes.map((node) => (
        <NodeLibraryItem key={node.typeKey} node={node} onDragStart={handleDragStart} />
      ))}
    </div>
  </div>
)
```

Keep the existing drag payload keys:

```ts
event.dataTransfer.setData('application/canvas-node-type', node.typeKey)
event.dataTransfer.setData('application/canvas-node-category', node.category)
```

- [ ] **Step 4: Run tests and a production build**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- src/components/node-panel/nodeLibrary.test.ts
npm run build
```

Expected:
- Vitest: PASS
- Vite build: PASS

- [ ] **Step 5: Commit the node panel redesign**

```bash
cd /Users/photonpay/project/canvas
git add frontend/src/components/node-panel/index.tsx frontend/src/components/node-panel/NodeLibraryItem.tsx
git commit -m "feat: redesign node library panel"
```

---

### Task 3: Extract Insertion Graph Rewrite Helpers

**Files:**
- Create: `frontend/src/pages/canvas-editor/insertNode.ts`
- Create: `frontend/src/pages/canvas-editor/insertNode.test.ts`
- Reference: `frontend/src/types/canvas.ts`

- [ ] **Step 1: Write failing tests for edge split, placeholder attach, and blank drop**

```ts
import { describe, expect, it } from 'vitest'
import type { Edge, Node } from '@xyflow/react'
import { applyInsertIntoEdge, buildDetachedNode, buildPlaceholderEdge } from './insertNode'

describe('insertNode helpers', () => {
  const edge: Edge = {
    id: 'a->b',
    source: 'a',
    target: 'b',
    sourceHandle: 'default',
  }

  it('splits one edge into two connected edges', () => {
    const result = applyInsertIntoEdge(edge, 'new_node')

    expect(result.removeEdgeId).toBe('a->b')
    expect(result.newEdges).toEqual([
      { id: 'a->new_node', source: 'a', target: 'new_node', sourceHandle: 'default' },
      { id: 'new_node->b', source: 'new_node', target: 'b', sourceHandle: 'default' },
    ])
  })

  it('creates a detached node when dropped on blank canvas', () => {
    const node = buildDetachedNode('new_node', 'GROOVY', '其他', { x: 120, y: 80 })
    expect(node.position).toEqual({ x: 120, y: 80 })
    expect(node.data.nodeType).toBe('GROOVY')
  })

  it('creates an edge from placeholder source and handle', () => {
    expect(buildPlaceholderEdge('if_1', 'success', 'new_node')).toEqual({
      id: 'if_1->new_node::success',
      source: 'if_1',
      target: 'new_node',
      sourceHandle: 'success',
    })
  })
})
```

- [ ] **Step 2: Run the tests to confirm the helper does not exist yet**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- src/pages/canvas-editor/insertNode.test.ts
```

Expected: FAIL with missing module / missing exports.

- [ ] **Step 3: Implement pure insertion helpers with editor-compatible IDs**

```ts
import type { Edge, Node, XYPosition } from '@xyflow/react'
import { DEFAULT_NAMES, type CanvasNodeData } from '../../components/canvas/constants'

export function applyInsertIntoEdge(edge: Edge, nodeId: string) {
  const sourceHandle = edge.sourceHandle ?? 'default'
  return {
    removeEdgeId: edge.id,
    newEdges: [
      { id: `${edge.source}->${nodeId}`, source: edge.source, target: nodeId, sourceHandle },
      { id: `${nodeId}->${edge.target}`, source: nodeId, target: edge.target, sourceHandle: 'default' },
    ] satisfies Edge[],
  }
}

export function buildPlaceholderEdge(sourceId: string, handleId: string, nodeId: string): Edge {
  return {
    id: `${sourceId}->${nodeId}::${handleId}`,
    source: sourceId,
    target: nodeId,
    sourceHandle: handleId,
  }
}

export function buildDetachedNode(
  nodeId: string,
  nodeType: string,
  category: string,
  position: XYPosition,
): Node<CanvasNodeData> {
  return {
    id: nodeId,
    type: 'canvasNode',
    position,
    data: {
      nodeType,
      category,
      name: DEFAULT_NAMES[nodeType] ?? nodeType,
      bizConfig: {},
    },
  }
}
```

- [ ] **Step 4: Run the insertion helper tests**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- src/pages/canvas-editor/insertNode.test.ts
```

Expected: PASS with 3 passing tests.

- [ ] **Step 5: Commit the insertion helper module**

```bash
cd /Users/photonpay/project/canvas
git add frontend/src/pages/canvas-editor/insertNode.ts frontend/src/pages/canvas-editor/insertNode.test.ts
git commit -m "test: cover canvas node insertion helpers"
```

---

### Task 4: Wire Edge `+`, Placeholder Drops, and Blank Drops into the Editor

**Files:**
- Modify: `frontend/src/components/canvas/HoverEdge.tsx`
- Modify: `frontend/src/components/canvas/BranchPlaceholderNode.tsx`
- Modify: `frontend/src/context/CanvasActionsContext.tsx`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Verify: `frontend/src/components/canvas/branchHandles.test.ts`
- Test: `frontend/src/pages/canvas-editor/insertNode.test.ts`

- [ ] **Step 1: Extend the canvas actions context for edge insertion selection**

Update `frontend/src/context/CanvasActionsContext.tsx` to expose an edge insertion callback:

```ts
export interface CanvasActions {
  deleteEdge: (edgeId: string) => void
  startInsertOnEdge: (edgeId: string) => void
}

export const CanvasActionsContext = createContext<CanvasActions>({
  deleteEdge: () => {},
  startInsertOnEdge: () => {},
})
```

Expected follow-up in the editor provider:

```tsx
<CanvasActionsContext.Provider value={{
  deleteEdge: handleDeleteEdge,
  startInsertOnEdge: (edgeId) => setInsertContext({ kind: 'edge', edgeId }),
}}>
```

- [ ] **Step 2: Add the edge-center `+` affordance in `HoverEdge`**

Add a second label renderer block near the edge midpoint while preserving delete behavior:

```tsx
const { deleteEdge, startInsertOnEdge } = useCanvasActions()

{highlighted && (
  <button
    type="button"
    className="nopan nodrag"
    style={{
      position: 'absolute',
      transform: `translate(-50%,-50%) translate(${labelX}px,${labelY}px)`,
      width: 26,
      height: 26,
      borderRadius: '50%',
      border: '1px solid #8b5cf6',
      background: '#fff',
      color: '#8b5cf6',
      fontSize: 18,
      lineHeight: 1,
      pointerEvents: 'all',
    }}
    onMouseDown={(event) => event.stopPropagation()}
    onClick={(event) => {
      event.stopPropagation()
      startInsertOnEdge(id)
    }}
  >
    +
  </button>
)}
```

Keep delete action rendered with an offset, for example `labelY + 28`, so the two controls do not overlap.

- [ ] **Step 3: Tighten placeholder visuals and preserve branch semantics**

Update `frontend/src/components/canvas/BranchPlaceholderNode.tsx` so the visual language matches the spec:

```tsx
<div
  style={{
    width: PLACEHOLDER_W,
    height: PLACEHOLDER_H,
    border: `1px dashed ${d.color}`,
    borderRadius: 10,
    background: '#fafbfc',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexDirection: 'column',
  }}
>
  <span style={{ fontSize: 12, fontWeight: 600, color: d.color }}>{d.label}</span>
  <span style={{ fontSize: 11, color: '#94a3b8', marginTop: 4 }}>拖到这里</span>
</div>
```

Do not change placeholder data shape; `sourceId`, `handleId`, and `label` remain the contract.

- [ ] **Step 4: Wire insertion context and drop handling inside the editor**

In `frontend/src/pages/canvas-editor/index.tsx`, introduce a narrow insertion state:

```ts
type InsertContext =
  | { kind: 'edge'; edgeId: string }
  | { kind: 'placeholder'; sourceId: string; handleId: string }
  | { kind: 'blank' }
  | null

const [insertContext, setInsertContext] = useState<InsertContext>(null)
```

Use the helper module in the existing drop path:

```ts
const handleCreateNode = (nodeType: string, category: string, position: XYPosition) => {
  const nodeId = `${nodeType}_${Date.now()}`
  const newNode = buildDetachedNode(nodeId, nodeType, category, position)

  if (insertContext?.kind === 'edge') {
    const edge = getEdges().find(item => item.id === insertContext.edgeId)
    if (edge) {
      const { removeEdgeId, newEdges } = applyInsertIntoEdge(edge, nodeId)
      setEdges((current) => [...current.filter(item => item.id !== removeEdgeId), ...newEdges])
    }
  } else if (insertContext?.kind === 'placeholder') {
    setEdges((current) => [...current, buildPlaceholderEdge(insertContext.sourceId, insertContext.handleId, nodeId)])
  }

  setNodes((current) => [...current, newNode])
  setInsertContext(null)
}
```

Blank drop behavior is the same helper call without edge mutations.

For placeholder detection, keep using the existing placeholder node data generated by `useBranchPlaceholders`; only read its `sourceId` and `handleId` when drop target is a placeholder node.

- [ ] **Step 5: Run focused tests and a full build, then commit**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test -- src/pages/canvas-editor/insertNode.test.ts src/components/canvas/branchHandles.test.ts
npm run build
```

Expected:
- Vitest: PASS for insertion helper and branch handle tests
- Vite build: PASS

Commit:

```bash
cd /Users/photonpay/project/canvas
git add frontend/src/components/canvas/HoverEdge.tsx frontend/src/components/canvas/BranchPlaceholderNode.tsx frontend/src/context/CanvasActionsContext.tsx frontend/src/pages/canvas-editor/index.tsx
git commit -m "feat: add context-aware canvas node insertion"
```

---

### Task 5: End-to-End Verification and Cleanup

**Files:**
- Verify: `frontend/src/components/node-panel/index.tsx`
- Verify: `frontend/src/components/canvas/HoverEdge.tsx`
- Verify: `frontend/src/pages/canvas-editor/index.tsx`
- Verify: `frontend/src/components/canvas/BranchPlaceholderNode.tsx`

- [ ] **Step 1: Run the full frontend test suite**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm test
```

Expected: PASS for all Vitest suites.

- [ ] **Step 2: Run a production build**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm run build
```

Expected: PASS with emitted assets under `frontend/dist`.

- [ ] **Step 3: Manual verification checklist in the browser**

Run:

```bash
cd /Users/photonpay/project/canvas/frontend
npm run dev
```

Verify manually:

```text
1. 节点库默认直接展示全部节点，不再使用折叠组
2. 分类 chips 可筛选节点；搜索可与分类叠加
3. 节点条目左侧色条与所属分类颜色一致
4. 问号为小圆圈帮助入口，点击后可看详细说明
5. 普通连线 hover/选中后出现 + 按钮
6. 通过连线 + 选择节点后，原连线被拆成两段并自动接入新节点
7. 分支占位点拖入后自动接到对应分支
8. 空白区拖入仅创建节点，不自动接线
```

- [ ] **Step 4: Commit the final verification pass**

```bash
cd /Users/photonpay/project/canvas
git add frontend
git commit -m "chore: verify node library interaction redesign"
```

---

## Self-Review

### Spec coverage

- 默认全量展示、搜索、分类 chips、常用节点：Task 1-2
- 分类色条与圆形问号帮助入口：Task 2
- 节点库负责选类型、画布负责定落点：Task 2-4
- 连线 `+` 插入与自动改线：Task 3-4
- 分支占位点拖入自动接线：Task 3-4
- 空白区拖入只创建不接线：Task 3-4
- 回归验证：Task 4-5

### Placeholder scan

- No `TBD` / `TODO`
- Every code-changing step includes concrete snippets or exact commands
- Test commands and commit commands are explicit

### Type consistency

- Shared helper names are consistent across tasks:
  - `buildNodeLibraryView`
  - `getNodeSummary`
  - `applyInsertIntoEdge`
  - `buildDetachedNode`
  - `buildPlaceholderEdge`
  - `startInsertOnEdge`

