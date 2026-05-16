# Branch Node Placeholder System (Group B) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all hardcoded branch-node Handle rendering with a data-driven system, add dashed placeholder nodes for unconnected outputs, and enable drag-to-placeholder auto-connection for all 6 branching node types (IF_CONDITION, MANUAL_APPROVAL, SELECTOR, AB_SPLIT, PRIORITY, API_CALL).

**Architecture:** Three new files form the core: `branchHandles.ts` (pure data), `useBranchPlaceholders.ts` (derived state hook), `BranchPlaceholderNode.tsx` (render component). `CanvasNode.tsx` replaces its 3 hardcoded Handle blocks with a single dynamic renderer. The canvas editor merges placeholder nodes into `displayNodes` (separate from `nodes` in the undo stack) and handles drop/connect events.

**Tech Stack:** React 18, TypeScript, ReactFlow (@xyflow/react), Vite. No backend changes. Vitest is added in Task 1 for unit-testing the pure `getBranchHandles` function.

---

## File Map

| Action | File |
|--------|------|
| Create | `frontend/src/components/canvas/branchHandles.ts` |
| Create | `frontend/src/hooks/useBranchPlaceholders.ts` |
| Create | `frontend/src/components/canvas/BranchPlaceholderNode.tsx` |
| Modify | `frontend/src/components/canvas/CanvasNode.tsx` |
| Modify | `frontend/src/pages/canvas-editor/index.tsx` |
| Modify | `frontend/src/components/config-panel/index.tsx` |
| Create | `frontend/src/components/canvas/branchHandles.test.ts` |

---

### Task 1: Add Vitest + write `getBranchHandles` with tests

**Files:**
- Modify: `frontend/package.json`
- Create: `frontend/src/components/canvas/branchHandles.ts`
- Create: `frontend/src/components/canvas/branchHandles.test.ts`

- [ ] **Step 1: Install Vitest**

```bash
cd frontend
npm install -D vitest @vitest/ui
```

Add to `package.json` scripts:
```json
"test": "vitest run",
"test:watch": "vitest"
```

Add `test` block to `vite.config.ts`:
```ts
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'node',
  },
  server: { /* existing proxy config unchanged */ },
})
```

- [ ] **Step 2: Write failing tests first**

Create `frontend/src/components/canvas/branchHandles.test.ts`:

```ts
import { describe, it, expect } from 'vitest'
import { getBranchHandles } from './branchHandles'

describe('getBranchHandles', () => {
  it('returns empty array for non-branching node', () => {
    expect(getBranchHandles('DELAY', {})).toEqual([])
    expect(getBranchHandles('API_CALL', {})).toEqual([
      { id: 'success', label: '成功', color: '#52c41a' },
      { id: 'fail',    label: '失败', color: '#f5222d' },
    ])
  })

  it('IF_CONDITION returns fixed success+else handles', () => {
    const handles = getBranchHandles('IF_CONDITION', {})
    expect(handles).toHaveLength(2)
    expect(handles[0].id).toBe('success')
    expect(handles[1].id).toBe('else')
  })

  it('MANUAL_APPROVAL returns approve+reject', () => {
    const handles = getBranchHandles('MANUAL_APPROVAL', {})
    expect(handles.map(h => h.id)).toEqual(['approve', 'reject'])
  })

  it('SELECTOR handles = branches.length + 1 else', () => {
    const branches = [
      { label: '分支A' },
      { label: '分支B' },
      { label: '分支C' },
    ]
    const handles = getBranchHandles('SELECTOR', { branches })
    expect(handles).toHaveLength(4) // 3 branches + else
    expect(handles[0]).toEqual({ id: 'branch-0', label: '分支A', color: '#1677ff' })
    expect(handles[3]).toEqual({ id: 'else', label: '否则', color: '#8c8c8c' })
  })

  it('AB_SPLIT handles = groups.length, ids are group-KEY', () => {
    const groups = [
      { groupKey: 'A' },
      { groupKey: 'B' },
      { groupKey: 'C' },
    ]
    const handles = getBranchHandles('AB_SPLIT', { groups })
    expect(handles).toHaveLength(3)
    expect(handles[0].id).toBe('group-A')
    expect(handles[1].id).toBe('group-B')
    expect(handles[2].id).toBe('group-C')
  })

  it('PRIORITY handles = priorities.length + 1 default', () => {
    const priorities = [{ order: 1 }, { order: 2 }]
    const handles = getBranchHandles('PRIORITY', { priorities })
    expect(handles).toHaveLength(3) // 2 priorities + default
    expect(handles[0]).toEqual({ id: 'priority-0', label: '优先 1', color: '#eb2f96' })
    expect(handles[2]).toEqual({ id: 'default', label: '其余', color: '#8c8c8c' })
  })

  it('SELECTOR with empty branches returns only else', () => {
    const handles = getBranchHandles('SELECTOR', { branches: [] })
    expect(handles).toHaveLength(1)
    expect(handles[0].id).toBe('else')
  })

  it('AB_SPLIT with empty groups returns empty array', () => {
    expect(getBranchHandles('AB_SPLIT', { groups: [] })).toEqual([])
  })
})
```

- [ ] **Step 3: Run tests — expect failures (module not found)**

```bash
cd frontend && npm test 2>&1 | tail -20
```

Expected: `Cannot find module './branchHandles'`

- [ ] **Step 4: Create `branchHandles.ts`**

Create `frontend/src/components/canvas/branchHandles.ts`:

```ts
export type BranchHandle = {
  id:    string  // matches deriveEdges / patchBizConfig sourceHandle values
  label: string
  color: string
}

const GROUP_COLORS = ['#1677ff', '#52c41a', '#fa8c16', '#722ed1', '#eb2f96', '#13c2c2']

export function getBranchHandles(
  nodeType: string,
  bizConfig: Record<string, unknown>,
): BranchHandle[] {
  switch (nodeType) {
    case 'IF_CONDITION':
      return [
        { id: 'success', label: '条件成立', color: '#52c41a' },
        { id: 'else',    label: '否则',     color: '#8c8c8c' },
      ]

    case 'MANUAL_APPROVAL':
      return [
        { id: 'approve', label: '通过', color: '#52c41a' },
        { id: 'reject',  label: '拒绝', color: '#f5222d' },
      ]

    case 'API_CALL':
      return [
        { id: 'success', label: '成功', color: '#52c41a' },
        { id: 'fail',    label: '失败', color: '#f5222d' },
      ]

    case 'SELECTOR': {
      const branches = (bizConfig.branches as { label?: string }[]) ?? []
      const handles: BranchHandle[] = branches.map((b, i) => ({
        id:    `branch-${i}`,
        label: b.label ?? `分支 ${i + 1}`,
        color: '#1677ff',
      }))
      handles.push({ id: 'else', label: '否则', color: '#8c8c8c' })
      return handles
    }

    case 'AB_SPLIT': {
      const groups = (bizConfig.groups as { groupKey: string }[]) ?? []
      if (groups.length === 0) return []
      const bucketSize = Math.floor(100 / groups.length)
      return groups.map((g, i) => ({
        id:    `group-${g.groupKey}`,
        label: `${g.groupKey} ${i === groups.length - 1 ? 100 - bucketSize * i : bucketSize}%`,
        color: GROUP_COLORS[i % GROUP_COLORS.length],
      }))
    }

    case 'PRIORITY': {
      const priorities = (bizConfig.priorities as { order: number }[]) ?? []
      const handles: BranchHandle[] = priorities.map((p, i) => ({
        id:    `priority-${i}`,
        label: `优先 ${p.order ?? i + 1}`,
        color: '#eb2f96',
      }))
      if (priorities.length > 0) {
        handles.push({ id: 'default', label: '其余', color: '#8c8c8c' })
      }
      return handles
    }

    default:
      return []
  }
}
```

- [ ] **Step 5: Run tests — expect all PASS**

```bash
cd frontend && npm test 2>&1 | tail -20
```

Expected:
```
✓ branchHandles.test.ts (8 tests) X ms
Test Files  1 passed (1)
Tests       8 passed (8)
```

- [ ] **Step 6: Commit**

```bash
git add frontend/src/components/canvas/branchHandles.ts
git add frontend/src/components/canvas/branchHandles.test.ts
git add frontend/package.json frontend/vite.config.ts
git commit -m "feat: getBranchHandles utility with Vitest coverage"
```

---

### Task 2: Refactor `CanvasNode.tsx` — dynamic Handles

Replace the 3 hardcoded Handle blocks (`isIf`, `isApproval`, `isSelector`) with a single dynamic renderer using `getBranchHandles`.

**Files:**
- Modify: `frontend/src/components/canvas/CanvasNode.tsx`

- [ ] **Step 1: Add import**

At the top of `CanvasNode.tsx`, add:
```ts
import { getBranchHandles } from './branchHandles'
```

- [ ] **Step 2: Add `branchHandles` constant inside the component**

After existing constants (`bg`, `isTrigger`, etc.), add:
```tsx
const branchHandles = getBranchHandles(d.nodeType, d.bizConfig ?? {})
const isBranching   = branchHandles.length > 0
```

- [ ] **Step 3: Replace the three hardcoded Handle blocks**

Find and **delete** these three blocks (lines ~122–149 in the current file):
```tsx
{!isTerminal && !isIf && !isSelector && (
  <Handle type="source" position={Position.Bottom} id="default" ... />
)}
{isIf && (<> ... </>)}
{isApproval && (<> ... </>)}
{isSelector && (<> ... </>)}
```

Replace with:
```tsx
{!isTerminal && (
  isBranching ? (
    <div style={{ paddingBottom: 18 }}>
      {branchHandles.map((h, i) => {
        const pct = ((i + 1) / (branchHandles.length + 1)) * 100
        return (
          <Handle
            key={h.id}
            type="source"
            position={Position.Bottom}
            id={h.id}
            style={{
              left:       `${pct}%`,
              background: h.color,
              border:     '2px solid #fff',
              width:      10,
              height:     10,
            }}
          >
            <span style={{
              position:  'absolute',
              top:       12,
              left:      '50%',
              transform: 'translateX(-50%)',
              fontSize:  9,
              color:     h.color,
              whiteSpace: 'nowrap',
              pointerEvents: 'none',
            }}>
              {h.label}
            </span>
          </Handle>
        )
      })}
    </div>
  ) : (
    <Handle
      type="source"
      position={Position.Bottom}
      id="default"
      style={{ background: '#fff', border: '2px solid #bbb', width: 10, height: 10 }}
    />
  )
)}
```

- [ ] **Step 4: Remove now-unused local variables**

Delete these lines (they are no longer needed):
```tsx
const isIf       = d.nodeType === 'IF_CONDITION'
const isApproval = d.nodeType === 'MANUAL_APPROVAL'
const isSelector = d.nodeType === 'SELECTOR'
const branches   = (d.bizConfig?.branches as { label?: string }[] | undefined) ?? []
```

- [ ] **Step 5: Type check**

```bash
cd frontend && npx tsc --noEmit 2>&1 | head -20
```

Expected: 0 errors.

- [ ] **Step 6: Run existing unit tests**

```bash
npm test 2>&1 | tail -10
```

Expected: still passing.

- [ ] **Step 7: Manual test**

1. Open canvas editor
2. Drag out an IF_CONDITION node — should show green `条件成立` + grey `否则` handles at bottom
3. Drag out an AB_SPLIT node, configure 3 groups A/B/C — should show 3 colored handles labeled `A 33%` `B 33%` `C 34%`
4. Drag out a PRIORITY node, add 2 priorities — should show 2 pink + 1 grey `其余` handle
5. Drag out an API_CALL node — should show green `成功` + red `失败` handles
6. Confirm existing canvases still load correctly

- [ ] **Step 8: Commit**

```bash
git add frontend/src/components/canvas/CanvasNode.tsx
git commit -m "refactor: CanvasNode dynamic branch Handles via getBranchHandles (fixes AB_SPLIT/PRIORITY missing handles)"
```

---

### Task 3: `BranchPlaceholderNode` component

**Files:**
- Create: `frontend/src/components/canvas/BranchPlaceholderNode.tsx`

- [ ] **Step 1: Create the component**

```tsx
import { Handle, Position, type NodeProps } from '@xyflow/react'

export type PlaceholderData = {
  _placeholder: true
  sourceId:     string
  handleId:     string
  label:        string
  color:        string
}

export default function BranchPlaceholderNode({ data }: NodeProps) {
  const d = data as PlaceholderData
  return (
    <div
      style={{
        width:          150,
        height:         52,
        border:         `2px dashed ${d.color}`,
        borderRadius:   8,
        background:     `${d.color}14`,   // ~8% opacity tint
        display:        'flex',
        flexDirection:  'column',
        alignItems:     'center',
        justifyContent: 'center',
        cursor:         'default',
        userSelect:     'none',
        pointerEvents:  'all',
      }}
    >
      {/* Hidden target handle so ReactFlow can route edges into it */}
      <Handle
        type="target"
        position={Position.Top}
        id="input"
        style={{ opacity: 0, pointerEvents: 'none', width: 1, height: 1 }}
      />
      <span style={{ fontSize: 11, fontWeight: 600, color: d.color }}>{d.label}</span>
      <span style={{ fontSize: 10, color: '#8c8c8c', marginTop: 2 }}>拖节点到这里</span>
      <span style={{ fontSize: 15, color: d.color, lineHeight: 1, marginTop: 2 }}>＋</span>
    </div>
  )
}
```

- [ ] **Step 2: Type check**

```bash
npx tsc --noEmit 2>&1 | head -20
```

Expected: 0 errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/components/canvas/BranchPlaceholderNode.tsx
git commit -m "feat: BranchPlaceholderNode component for unconnected branch outputs"
```

---

### Task 4: `useBranchPlaceholders` hook

**Files:**
- Create: `frontend/src/hooks/useBranchPlaceholders.ts`

- [ ] **Step 1: Create the hook**

```ts
import { useMemo } from 'react'
import type { Node, Edge } from '@xyflow/react'
import type { CanvasNodeData }  from '../components/canvas/constants'
import type { PlaceholderData } from '../components/canvas/BranchPlaceholderNode'
import { getBranchHandles }    from '../components/canvas/branchHandles'
import { TERMINAL_TYPES }      from '../components/canvas/constants'

const PLACEHOLDER_W = 150
const PLACEHOLDER_H = 52
const V_GAP         = 80   // px gap between node bottom and placeholder top

export function useBranchPlaceholders(
  nodes: Node<CanvasNodeData>[],
  edges: Edge[],
): Node<PlaceholderData>[] {
  return useMemo(() => {
    // Build a set of already-connected source handles: "nodeId:handleId"
    const connected = new Set(
      edges
        .filter(e => e.source && e.sourceHandle)
        .map(e => `${e.source}:${e.sourceHandle}`),
    )

    const placeholders: Node<PlaceholderData>[] = []

    for (const node of nodes) {
      if ((node.data as unknown as PlaceholderData)?._placeholder) continue
      if (TERMINAL_TYPES.has(node.data.nodeType))                   continue

      const handles = getBranchHandles(node.data.nodeType, node.data.bizConfig ?? {})
      if (handles.length === 0) continue

      const nodeW = node.width  ?? 200
      const nodeH = node.height ?? 76

      handles.forEach((h, i) => {
        if (connected.has(`${node.id}:${h.id}`)) return

        // Mirror the horizontal position used in CanvasNode's dynamic Handle rendering
        const handlePct = (i + 1) / (handles.length + 1)
        const x = node.position.x + handlePct * nodeW - PLACEHOLDER_W / 2
        const y = node.position.y + nodeH + V_GAP

        placeholders.push({
          id:        `__ph_${node.id}_${h.id}`,
          type:      'branchPlaceholder',
          position:  { x, y },
          draggable: false,
          selectable: false,
          data: {
            _placeholder: true,
            sourceId: node.id,
            handleId: h.id,
            label:    h.label,
            color:    h.color,
          } as PlaceholderData,
        })
      })
    }

    return placeholders
  }, [nodes, edges])
}
```

- [ ] **Step 2: Type check**

```bash
npx tsc --noEmit 2>&1 | head -20
```

Expected: 0 errors.

- [ ] **Step 3: Commit**

```bash
git add frontend/src/hooks/useBranchPlaceholders.ts
git commit -m "feat: useBranchPlaceholders hook — derives placeholder nodes as pure computed state"
```

---

### Task 5: Wire placeholder system into canvas editor

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

- [ ] **Step 1: Import new types and hook**

At the top of `canvas-editor/index.tsx`, add:

```tsx
import BranchPlaceholderNode, { type PlaceholderData }
  from '../../components/canvas/BranchPlaceholderNode'
import { useBranchPlaceholders } from '../../hooks/useBranchPlaceholders'
```

- [ ] **Step 2: Register the new node type**

Find:
```ts
const nodeTypes = { canvasNode: CanvasNodeCmp }
```

Replace with:
```ts
const nodeTypes = {
  canvasNode:        CanvasNodeCmp,
  branchPlaceholder: BranchPlaceholderNode,
}
```

- [ ] **Step 3: Add `displayNodes` computation inside the component**

After the line `const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])`, add:

```tsx
// Separate real nodes from any stale placeholder residue
const realNodes = nodes.filter(n => !(n.data as any)?._placeholder) as Node<CanvasNodeData>[]

const placeholders = useBranchPlaceholders(realNodes, edges)

// displayNodes = realNodes + computed placeholders (never in undo stack)
const displayNodes = useMemo(
  () => [...realNodes, ...placeholders],
  [realNodes, placeholders],
)
```

- [ ] **Step 4: Pass `displayNodes` to `<ReactFlow>`**

Find `nodes={nodes}` in the `<ReactFlow>` JSX and replace with:
```tsx
nodes={displayNodes}
```

- [ ] **Step 5: Update `onDrop` to detect placeholder hits**

Find the existing `onDrop` callback. Add placeholder detection before creating the new node:

```tsx
const onDrop = useCallback((e: React.DragEvent) => {
  e.preventDefault()
  const nodeType = e.dataTransfer.getData('application/canvas-node-type')
  const category = e.dataTransfer.getData('application/canvas-node-category')
  if (!nodeType) return

  const dropPos = screenToFlowPosition({ x: e.clientX, y: e.clientY })

  // Check if dropped onto a placeholder
  const hitPlaceholder = placeholders.find(ph => {
    const { x, y } = ph.position
    return dropPos.x >= x && dropPos.x <= x + 150
        && dropPos.y >= y && dropPos.y <= y + 52
  })

  const newId   = crypto.randomUUID().replace(/-/g, '').slice(0, 12)
  const newPos  = hitPlaceholder?.position ?? dropPos
  const defaultBizConfig = /* copy the existing switch/case block from onDrop */ nodeType === 'AB_SPLIT'
    ? { groups: [{ groupKey: 'A', nextNodeId: undefined }] }
    : nodeType === 'SELECTOR'
    ? { branches: [{ label: '如果', nextNodeId: undefined }] }
    : nodeType === 'IF_CONDITION'
    ? { rules: [] }
    : nodeType === 'PRIORITY'
    ? { priorities: [{ order: 1, nextNodeId: undefined }] }
    : {}

  const newNode: Node = {
    id: newId, type: 'canvasNode', position: newPos,
    data: {
      nodeType, name: DEFAULT_NAMES[nodeType] ?? nodeType,
      category, bizConfig: defaultBizConfig,
    } as CanvasNodeData,
  }

  snapshot('拖入节点')
  setNodes(prev => [...prev.filter(n => !(n.data as any)?._placeholder), newNode])

  if (hitPlaceholder) {
    const ph = hitPlaceholder.data as PlaceholderData
    const newEdge: Edge = {
      id:           `${ph.sourceId}->${newId}`,
      source:       ph.sourceId,
      sourceHandle: ph.handleId,
      target:       newId,
      targetHandle: 'input',
    }
    // Also patch source node's bizConfig to record the connection
    setNodes(prev => prev.map(n => {
      if (n.id !== ph.sourceId) return n
      const d = n.data as CanvasNodeData
      return { ...n, data: { ...d, bizConfig: patchBizConfig(d.bizConfig, ph.handleId, newId) } }
    }))
    setEdges(prev => addEdge({
      ...newEdge,
      label: ph.label,
    }, prev))
    setSelectedNodeId(newId)
  } else {
    setSelectedNodeId(newNode.id)
  }
}, [placeholders, snapshot, screenToFlowPosition, setNodes, setEdges, patchBizConfig])
```

- [ ] **Step 6: Filter placeholders from save payload**

Find `handleSave` function. Where `nodes` is serialized to JSON, use `realNodes` instead:

```ts
const backendNodes = realNodes.map(n => { /* existing serialization */ })
```

And in the publish handler similarly.

- [ ] **Step 7: Type check**

```bash
npx tsc --noEmit 2>&1 | head -20
```

Expected: 0 errors.

- [ ] **Step 8: Manual test**

1. Open canvas editor
2. Drag an IF_CONDITION node onto canvas → two dashed placeholder boxes appear below (`条件成立` / `否则`)
3. Drag an AB_SPLIT node, configure groups A/B → two dashed boxes appear
4. Drag a new node from panel onto a placeholder box → node appears at that position and is auto-connected
5. After connecting, placeholder disappears
6. Save and reload → placeholder boxes are not saved, but edges are preserved
7. Draw a connection from any source Handle to a real target node → works as before, placeholder disappears

- [ ] **Step 9: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "feat: wire branch placeholder system into canvas editor (auto-connect on drop)"
```

---

### Task 6: Config panel — show successor node names

For branching nodes, each branch row in the config panel now shows the name of the connected successor node instead of "→ 从节点下方连接点拖线".

**Files:**
- Modify: `frontend/src/components/config-panel/index.tsx`

- [ ] **Step 1: Add `nodes` prop to ConfigPanel**

In the `Props` interface:
```ts
interface Props {
  nodeId:   string | null
  nodeData: CanvasNodeData | null
  onChange: (nodeId: string, patch: Partial<CanvasNodeData>) => void
  nodes:    Node<CanvasNodeData>[]   // ← ADD: for successor node lookup
  readonly?: boolean
}
```

Add it to the function signature: `export default function ConfigPanel({ nodeId, nodeData, onChange, nodes, readonly }: Props)`

- [ ] **Step 2: Create helper inside ConfigPanel**

```ts
const getNodeName = (id: string | undefined): string | null => {
  if (!id) return null
  return nodes.find(n => n.id === id)?.data.name ?? null
}
```

- [ ] **Step 3: Update `AbGroupList` to show successor names**

In `AbGroupList` (around line 509), find the group row JSX. Replace the current `→ 从节点下方对应连接点拖线` text with:

```tsx
{(() => {
  const name = getNodeName(g.nextNodeId)
  return name
    ? <Tag color="blue" style={{ fontSize: 11 }}>→ {name}</Tag>
    : <Tag color="warning" style={{ fontSize: 11 }}>⚠ 未连线</Tag>
})()}
```

Pass `getNodeName` down by calling it in the `AbGroupList` render (it reads from the outer scope via closure if defined inside `ConfigPanel`). Since `AbGroupList` is a separate function, pass `nodes` as a prop:

```tsx
function AbGroupList({ nodes }: { nodes: Node<CanvasNodeData>[] }) {
  const form = Form.useFormInstance()
  const getNodeName = (id: string | undefined) =>
    id ? nodes.find(n => n.id === id)?.data.name ?? null : null
  // ... rest unchanged
}
```

Update call site in the `renderControl` switch: `case 'ab-group-list': return <AbGroupList nodes={nodes} />`

- [ ] **Step 4: Update `MANUAL_APPROVAL` edge-hint fields**

In the `case 'edge-hint':` render block, show the connected node name if available:

```tsx
case 'edge-hint': {
  const connectedId = (nodeData?.bizConfig?.[field.key] as string | undefined)
  const connectedName = getNodeName(connectedId)
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      {connectedName
        ? <Tag color="blue">→ {connectedName}</Tag>
        : <Tag color="warning">⚠ 未连线</Tag>
      }
      <Text type="secondary" style={{ fontSize: 11 }}>{field.hint}</Text>
    </div>
  )
}
```

- [ ] **Step 5: Pass `nodes` from canvas editor to ConfigPanel**

In `canvas-editor/index.tsx`, find the `<ConfigPanel` JSX and add the prop:
```tsx
<ConfigPanel
  nodeId={selectedNodeId}
  nodeData={selectedNodeData}
  onChange={handleConfigChange}
  nodes={realNodes}
  readonly={readonly}
/>
```

- [ ] **Step 6: Type check + tests**

```bash
cd frontend && npx tsc --noEmit 2>&1 | head -20
npm test 2>&1 | tail -10
```

Expected: 0 type errors, all tests pass.

- [ ] **Step 7: Manual test**

1. Open an AB_SPLIT node config panel
2. Configure 3 groups (A/B/C)
3. Draw a line from group A handle to another node
4. Reopen the config panel → group A row shows `→ 节点名称`, groups B/C show `⚠ 未连线`
5. Open MANUAL_APPROVAL config — "通过"/"拒绝" rows show node names or warning tags

- [ ] **Step 8: Commit**

```bash
git add frontend/src/components/config-panel/index.tsx
git add frontend/src/pages/canvas-editor/index.tsx
git commit -m "feat: config panel shows successor node names for branching nodes"
```
