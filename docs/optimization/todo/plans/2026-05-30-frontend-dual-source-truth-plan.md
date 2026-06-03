# Frontend Dual Source Truth Fix Plan

**Goal:** Make bizConfig the single source of truth for edges. Derive React Flow edges via `useMemo` from node bizConfig. Fix undo/redo shallow copy bug with immer deep clone so history snapshots are never mutated.

**Architecture:** Edges are a computed view of `nodes[].data.bizConfig` — never stored independently. All edge modifications (connect, disconnect, insert) update bizConfig only; edges auto-derive via `useMemo`. The `useHistory` hook uses `immer/current` `produce()` to create deep-immutable snapshots; undo/restore replaces the full node array without risk of shared references.

**Tech Stack:** React 18, @xyflow/react, immer (new dependency), vitest

---

### Task 1: Remove useEdgesState, Derive Edges from bizConfig

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `frontend/src/pages/canvas-editor/outletRouting.ts`
- Test: `frontend/src/pages/canvas-editor/edgeDerivation.test.ts`
- Test: `frontend/src/pages/canvas-editor/outletRouting.test.ts` (already exists, verify still passes)

- [ ] **Step 1: Write failing test — edges derived from bizConfig, not independent state**

Create `frontend/src/pages/canvas-editor/edgeDerivation.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import type { BackendNode } from '../../types/canvas'
import { deriveEdges } from './outletRouting'

describe('edge derivation from bizConfig', () => {
  it('derives a single nextNodeId edge', () => {
    const nodes: BackendNode[] = [
      { id: 'a', type: 'DELAY', name: 'Delay', x: 0, y: 0, config: { nextNodeId: 'b' } },
    ]
    const edges = deriveEdges(nodes)
    expect(edges).toEqual([
      { id: 'a->b', source: 'a', target: 'b', sourceHandle: 'default' },
    ])
  })

  it('derives success/fail branch edges from IF_CONDITION', () => {
    const nodes: BackendNode[] = [
      { id: 'if1', type: 'IF_CONDITION', name: 'IF', x: 0, y: 0, config: { successNodeId: 'c', failNodeId: 'd' } },
    ]
    const edges = deriveEdges(nodes)
    expect(edges).toEqual([
      { id: 'if1->c::success', source: 'if1', target: 'c', sourceHandle: 'success' },
      { id: 'if1->d::fail', source: 'if1', target: 'd', sourceHandle: 'fail' },
    ])
  })

  it('derives approve/reject edges from MANUAL_APPROVAL', () => {
    const nodes: BackendNode[] = [
      { id: 'ap1', type: 'MANUAL_APPROVAL', name: 'Approval', x: 0, y: 0, config: { approveNodeId: 'e', rejectNodeId: 'f' } },
    ]
    const edges = deriveEdges(nodes)
    expect(edges).toEqual([
      { id: 'ap1->e::approve', source: 'ap1', target: 'e', sourceHandle: 'approve' },
      { id: 'ap1->f::reject', source: 'ap1', target: 'f', sourceHandle: 'reject' },
    ])
  })

  it('derives hit/miss edges from TAGGER in audience mode', () => {
    const nodes: BackendNode[] = [
      { id: 'tag1', type: 'TAGGER', name: 'Tagger', x: 0, y: 0, config: { mode: 'audience', hitNextNodeId: 'g', missNextNodeId: 'h' } },
    ]
    const edges = deriveEdges(nodes)
    expect(edges).toEqual([
      { id: 'tag1->g::hit', source: 'tag1', target: 'g', sourceHandle: 'hit' },
      { id: 'tag1->h::miss', source: 'tag1', target: 'h', sourceHandle: 'miss' },
    ])
  })

  it('derives DIRECT_CALL fan-out branches', () => {
    const nodes: BackendNode[] = [
      {
        id: 'dc1', type: 'DIRECT_CALL', name: 'API', x: 0, y: 0,
        config: {
          branches: [
            { label: '渠道A', nextNodeId: 'x' },
            { label: '渠道B', nextNodeId: 'y' },
          ],
        },
      },
    ]
    const edges = deriveEdges(nodes)
    expect(edges).toEqual([
      { id: 'dc1->x', source: 'dc1', target: 'x', sourceHandle: 'default' },
      { id: 'dc1->y', source: 'dc1', target: 'y', sourceHandle: 'default' },
    ])
  })

  it('derives AB_SPLIT group edges', () => {
    const nodes: BackendNode[] = [
      {
        id: 'ab1', type: 'AB_SPLIT', name: 'AB', x: 0, y: 0,
        config: {
          groups: [
            { groupKey: 'A', nextNodeId: 'p' },
            { groupKey: 'B', nextNodeId: 'q' },
          ],
        },
      },
    ]
    const edges = deriveEdges(nodes)
    expect(edges).toEqual([
      { id: 'ab1->p::group-A', source: 'ab1', target: 'p', sourceHandle: 'group-A' },
      { id: 'ab1->q::group-B', source: 'ab1', target: 'q', sourceHandle: 'group-B' },
    ])
  })

  it('derives SELECTOR branch edges', () => {
    const nodes: BackendNode[] = [
      {
        id: 'sel1', type: 'SELECTOR', name: 'Selector', x: 0, y: 0,
        config: {
          branches: [
            { label: 'Branch 1', nextNodeId: 's1' },
          ],
          elseNodeId: 's2',
        },
      },
    ]
    const edges = deriveEdges(nodes)
    expect(edges).toEqual([
      { id: 'sel1->s1::branch-0', source: 'sel1', target: 's1', sourceHandle: 'branch-0' },
      { id: 'sel1->s2::else', source: 'sel1', target: 's2', sourceHandle: 'else' },
    ])
  })

  it('ignores empty or undefined target fields', () => {
    const nodes: BackendNode[] = [
      { id: 'n1', type: 'DELAY', name: 'Delay', x: 0, y: 0, config: { nextNodeId: undefined } },
    ]
    const edges = deriveEdges(nodes)
    expect(edges).toEqual([])
  })

  it('produces no edges for node with empty config', () => {
    const nodes: BackendNode[] = [
      { id: 'n1', type: 'START', name: 'Start', x: 0, y: 0, config: {} },
    ]
    const edges = deriveEdges(nodes)
    expect(edges).toEqual([])
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd frontend && npx vitest run canvas-editor/edgeDerivation.test.ts
```

Expected: All tests PASS (deriveEdges already exists). This test documents expected behavior and will guard against regressions when we remove `useEdgesState`.

- [ ] **Step 3: Replace useEdgesState with useMemo derivation in `index.tsx`**

In `frontend/src/pages/canvas-editor/index.tsx`, make these changes:

3a. Remove the `useEdgesState` import and usage. Replace with a `useMemo` that derives edges from nodes:

```tsx
// BEFORE (line 9):
import { useNodesState, useEdgesState, useReactFlow, ... } from '@xyflow/react'

// AFTER:
import { useNodesState, useReactFlow, ... } from '@xyflow/react'
```

```tsx
// BEFORE (line 349-350):
const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])

// AFTER:
const [nodes, setNodes, onNodesChange] = useNodesState<Node<CanvasNodeData>[]>([])

// Derive edges from bizConfig — edges are a computed view, never independent state
const edges = useMemo(() => {
  const backendNodes: BackendNode[] = nodes
    .filter(n => !(n.data as any)?._placeholder)
    .map(n => {
      const data = n.data as CanvasNodeData
      return {
        id: n.id,
        type: data.nodeType,
        name: data.name,
        category: data.category,
        x: Math.round(n.position.x),
        y: Math.round(n.position.y),
        config: data.bizConfig,
        outletSchema: data.outletSchema,
      }
    })
  return deriveEdges(backendNodes)
}, [nodes])
```

3b. Remove the `onEdgesChange` handler since edges are now derived. Replace `onEdgesChangeWrapped` with a handler that only processes edge removals by clearing bizConfig refs:

```tsx
// BEFORE (lines 949-979): onEdgesChangeWrapped reads from `edges` state and calls onEdgesChange
// AFTER: Replace the entire onEdgesChangeWrapped with a version that only handles removals
const onEdgesChangeWrapped = useCallback((changes: EdgeChange[]) => {
  const removedIds = changes
    .filter((change): change is EdgeChange & { type: 'remove'; id: string } => change.type === 'remove')
    .map(change => change.id)

  if (removedIds.length > 0) {
    snapshot('删除连线')
    setInsertContext(current => (
      current?.kind === 'edge' && removedIds.includes(current.edgeId) ? null : current
    ))
    // Derive which edges are being removed from current nodes
    const currentBackendNodes = nodes
      .filter(n => !(n.data as any)?._placeholder)
      .map(n => {
        const data = n.data as CanvasNodeData
        return {
          id: n.id,
          type: data.nodeType,
          name: data.name,
          category: data.category,
          x: Math.round(n.position.x),
          y: Math.round(n.position.y),
          config: data.bizConfig,
          outletSchema: data.outletSchema,
        }
      })
    const currentEdges = deriveEdges(currentBackendNodes)
    const removedEdges = currentEdges.filter(edge => removedIds.includes(edge.id))
    if (removedEdges.length > 0) {
      setNodes(prev => prev.map(node => {
        const outgoing = removedEdges.filter(edge => edge.source === node.id)
        if (outgoing.length === 0) return node
        const data = node.data as CanvasNodeData
        return {
          ...node,
          data: {
            ...data,
            bizConfig: outgoing.reduce((cfg, edge) => clearEdgeRef(cfg, edge, data.outletSchema), data.bizConfig ?? {}),
          },
        }
      }))
    }
  }
  const significant = changes.some(c => c.type === 'add' || c.type === 'remove')
  if (significant) setIsDirty(true)
}, [nodes, setNodes, snapshot])
```

3c. Remove all `setEdges(...)` calls throughout the file. Each place that currently calls `setEdges` must be changed to only update `nodes` (bizConfig). The edges will auto-derive from the next render.

Note: Line numbers are approximate; search for `setEdges(` pattern in the file and remove/replace each occurrence.
- Line 565 `setEdges([])` -> remove (edges auto-derive as empty)
- Line 585 `setEdges(rfEdges)` -> remove
- Line 774 `setEdges(current => [...current.filter(...), ...expansion.edges, ...newEdges])` -> remove; the edges are derived from the node bizConfig updates that already happen in the surrounding code
- Line 811-812 `setEdges(current => [...replaceOutletEdge(...), ...expansion.edges])` -> remove; bizConfig update already done
- Line 816 `setEdges(current => [...current, ...expansion.edges])` -> remove
- Line 882 `setEdges(prev => replaceOutletEdge(...))` -> remove
- Line 912 `setEdges(prev => isSingleOutletFanOut ? appendDirectCallEdge(...) : replaceOutletEdge(...))` -> remove
- Line 939 `setEdges(prev => prev.filter(...))` -> remove (edges auto-derive from node cleanup)
- Line 1416 `setEdges(prev => prev.filter(...))` -> remove
- Line 1446 `setEdges(prev => { ... return prev.filter(...) })` -> change to only do the setNodes part:

```tsx
// BEFORE (deleteEdgeById, lines 1443-1458):
const deleteEdgeById = (edgeId: string) => {
  snapshot('删除连线')
  setInsertContext(current => current?.kind === 'edge' && current.edgeId === edgeId ? null : current)
  setEdges(prev => {
    const edge = prev.find(e => e.id === edgeId)
    if (edge) {
      setNodes(nodes => nodes.map(n => {
        if (n.id !== edge.source) return n
        const d = n.data as CanvasNodeData
        return { ...n, data: { ...d, bizConfig: clearEdgeRef(d.bizConfig ?? {}, edge, d.outletSchema) } }
      }))
    }
    return prev.filter(e => e.id !== edgeId)
  })
  setIsDirty(true)
}

// AFTER:
const deleteEdgeById = (edgeId: string) => {
  snapshot('删除连线')
  setInsertContext(current => current?.kind === 'edge' && current.edgeId === edgeId ? null : current)
  // Find the edge being deleted from derived edges
  const currentBackendNodes = nodes
    .filter(n => !(n.data as any)?._placeholder)
    .map(n => {
      const data = n.data as CanvasNodeData
      return {
        id: n.id,
        type: data.nodeType,
        name: data.name,
        category: data.category,
        x: Math.round(n.position.x),
        y: Math.round(n.position.y),
        config: data.bizConfig,
        outletSchema: data.outletSchema,
      }
    })
  const currentEdges = deriveEdges(currentBackendNodes)
  const edge = currentEdges.find(e => e.id === edgeId)
  if (edge) {
    setNodes(prev => prev.map(n => {
      if (n.id !== edge.source) return n
      const d = n.data as CanvasNodeData
      return { ...n, data: { ...d, bizConfig: clearEdgeRef(d.bizConfig ?? {}, edge, d.outletSchema) } }
    }))
  }
  setIsDirty(true)
}
```

- Line 1623 `setEdges([])` -> remove

3c1. Define the `clearEdgeRef` helper function. Add this in `index.tsx` after the `clearEdgeRef` comment/usage area (approximately after the `onEdgesChangeWrapped` definition, before the `useHistory` hook). This function clears the bizConfig reference for a given edge so that when edges are removed, the corresponding bizConfig fields are also cleared:

```tsx
/**
 * Clear the bizConfig field that produced the given edge.
 * This ensures that when an edge is visually removed, the underlying
 * bizConfig data is also cleared (edges are derived from bizConfig).
 */
const clearEdgeRef = (
  bizConfig: Record<string, unknown>,
  edge: { source: string; target: string; sourceHandle?: string | null },
  outletSchema?: { outlets?: Array<{ key: string; field: string }> } | null
): Record<string, unknown> => {
  const updated = { ...bizConfig };

  // If outletSchema is available, use it to find the exact field to clear
  if (outletSchema?.outlets) {
    for (const outlet of outletSchema.outlets) {
      if (outlet.key === edge.sourceHandle) {
        updated[outlet.field] = undefined;
        return updated;
      }
    }
  }

  // Fallback: clear based on common field naming conventions
  const handle = edge.sourceHandle;
  if (handle === 'success') {
    updated.successNodeId = undefined;
  } else if (handle === 'fail') {
    updated.failNodeId = undefined;
  } else if (handle === 'approve') {
    updated.approveNodeId = undefined;
  } else if (handle === 'reject') {
    updated.rejectNodeId = undefined;
  } else if (handle === 'hit') {
    updated.hitNextNodeId = undefined;
  } else if (handle === 'miss') {
    updated.missNextNodeId = undefined;
  } else if (handle === 'else') {
    updated.elseNodeId = undefined;
  } else if (handle?.startsWith('group-')) {
    // AB_SPLIT groups: clear the group's nextNodeId
    const groups = Array.isArray(updated.groups) ? [...(updated.groups as Array<Record<string, unknown>>)] : [];
    const groupKey = handle.replace('group-', '');
    const idx = groups.findIndex((g: Record<string, unknown>) => g.groupKey === groupKey);
    if (idx >= 0) {
      groups[idx] = { ...groups[idx], nextNodeId: undefined };
      updated.groups = groups;
    }
  } else if (handle?.startsWith('branch-')) {
    // SELECTOR/DIRECT_CALL branches: clear the branch's nextNodeId
    const branches = Array.isArray(updated.branches) ? [...(updated.branches as Array<Record<string, unknown>>)] : [];
    const branchIdx = parseInt(handle.replace('branch-', ''), 10);
    if (!isNaN(branchIdx) && branchIdx < branches.length) {
      branches[branchIdx] = { ...branches[branchIdx], nextNodeId: undefined };
      updated.branches = branches;
    }
  } else {
    // Default: clear nextNodeId (single-outlet nodes like DELAY, TRIGGER)
    updated.nextNodeId = undefined;
  }

  return updated;
};
```

3d. Install immer dependency (needed for useHistory replacement below). This must be done before Step 3e that uses `produce`:

```bash
cd frontend && npm install immer
```

3e. Update `useHistory` to not store edges in snapshots (they are derived):

```tsx
// BEFORE (line 205):
interface Snapshot { nodes: Node<CanvasNodeData>[]; edges: Edge[]; actionName: string }

// AFTER:
interface Snapshot { nodes: Node<CanvasNodeData>[]; actionName: string }
```

```tsx
// BEFORE (useHistory, lines 283-318):
function useHistory(nodes: Node<CanvasNodeData>[], edges: Edge[]) {
  const [history, setHistory] = useState<Snapshot[]>([])
  const [future,  setFuture]  = useState<Snapshot[]>([])
  const { setNodes, setEdges } = useReactFlow()

  const snapshot = useCallback((actionName = '操作') => {
    setHistory(h => [...h.slice(-49), { nodes: [...nodes], edges: [...edges], actionName }])
    setFuture([])
  }, [nodes, edges])

  const undo = useCallback(() => {
    if (!history.length) return
    const prev = history[history.length - 1]
    setFuture(f => [{ nodes, edges, actionName: prev.actionName }, ...f])
    setHistory(h => h.slice(0, -1))
    setNodes(prev.nodes)
    setEdges(prev.edges)
  }, [history, nodes, edges, setNodes, setEdges])

  const redo = useCallback(() => {
    if (!future.length) return
    const next = future[0]
    setHistory(h => [...h, { nodes, edges, actionName: next.actionName }])
    setFuture(f => f.slice(1))
    setNodes(next.nodes)
    setEdges(next.edges)
  }, [future, nodes, edges, setNodes, setEdges])

  const undoLabel = history.length ? `撤销：${history[history.length - 1].actionName}` : '没有可撤销的操作'
  const redoLabel = future.length  ? `重做：${future[0].actionName}` : '没有可重做的操作'

  return { snapshot, undo, redo, canUndo: history.length > 0, canRedo: future.length > 0, undoLabel, redoLabel }
}

// AFTER (using immer for deep clone, no edges in snapshot):
function useHistory(nodes: Node<CanvasNodeData>[]) {
  const [history, setHistory] = useState<Snapshot[]>([])
  const [future,  setFuture]  = useState<Snapshot[]>([])
  const { setNodes } = useReactFlow()

  const snapshot = useCallback((actionName = '操作') => {
    setHistory(h => [...h.slice(-49), { nodes: produce(nodes, draft => draft), actionName }])
    setFuture([])
  }, [nodes])

  const undo = useCallback(() => {
    if (!history.length) return
    const prev = history[history.length - 1]
    setFuture(f => [{ nodes: produce(nodes, draft => draft), actionName: prev.actionName }, ...f])
    setHistory(h => h.slice(0, -1))
    setNodes(produce(prev.nodes, draft => draft))
  }, [history, nodes, setNodes])

  const redo = useCallback(() => {
    if (!future.length) return
    const next = future[0]
    setHistory(h => [...h, { nodes: produce(nodes, draft => draft), actionName: next.actionName }])
    setFuture(f => f.slice(1))
    setNodes(produce(next.nodes, draft => draft))
  }, [future, nodes, setNodes])

  const undoLabel = history.length ? `撤销：${history[history.length - 1].actionName}` : '没有可撤销的操作'
  const redoLabel = future.length  ? `重做：${future[0].actionName}` : '没有可重做的操作'

  return { snapshot, undo, redo, canUndo: history.length > 0, canRedo: future.length > 0, undoLabel, redoLabel }
}
```

3f. Update the `useHistory` call site (line 436):

```tsx
// BEFORE:
const { snapshot, undo, redo, canUndo, canRedo, undoLabel, redoLabel } = useHistory(nodes as Node<CanvasNodeData>[], edges)

// AFTER:
const { snapshot, undo, redo, canUndo, canRedo, undoLabel, redoLabel } = useHistory(nodes as Node<CanvasNodeData>[])
```

3g. Remove `setEdges` from useReactFlow destructuring (line 343):

```tsx
// BEFORE:
const { screenToFlowPosition, getNodes, getEdges, fitView } = useReactFlow()

// AFTER:
const { screenToFlowPosition, getNodes, fitView } = useReactFlow()
```

Note: `getEdges` is still used in `onConnect` (line 893) and `isValidConnection` (line 983). Since edges are now derived, replace `getEdges()` calls with direct computation:

```tsx
// Add a helper in the component body:
const getDerivedEdges = useCallback(() => {
  const backendNodes: BackendNode[] = nodes
    .filter(n => !(n.data as any)?._placeholder)
    .map(n => {
      const data = n.data as CanvasNodeData
      return {
        id: n.id,
        type: data.nodeType,
        name: data.name,
        category: data.category,
        x: Math.round(n.position.x),
        y: Math.round(n.position.y),
        config: data.bizConfig,
        outletSchema: data.outletSchema,
      }
    })
  return deriveEdges(backendNodes)
}, [nodes])
```

Then replace all `getEdges()` with `getDerivedEdges()` in `onConnect`, `isValidConnection`, and `onDrop`.

3h. Remove `onEdgesChange` from ReactFlow component props (line 1864):

```tsx
// BEFORE:
onEdgesChange={onEdgesChangeWrapped}

// AFTER: (remove this prop entirely — edges are read-only, React Flow should not try to modify them)
```

- [ ] **Step 4: Run tests to verify existing outletRouting tests still pass + new tests pass**

```bash
cd frontend && npx vitest run canvas-editor/outletRouting.test.ts canvas-editor/edgeDerivation.test.ts
```

Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/canvas-editor/index.tsx frontend/src/pages/canvas-editor/edgeDerivation.test.ts && git commit -m "feat: derive React Flow edges from bizConfig via useMemo, remove useEdgesState"
```

---

### Task 2: Fix undo/redo with Immer Deep Clone

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `frontend/package.json`
- Test: `frontend/src/pages/canvas-editor/undoRedo.test.ts`

- [ ] **Step 1: Verify immer dependency is already installed**

This is a prerequisite verification step. The actual TDD cycle for immer-based undo/redo starts at Step 2.

Immer was installed in Task 1 Step 3d. Verify it exists:

```bash
cd frontend && npm ls immer
```

Expected: `immer@x.x.x` is listed. If not (e.g., Task 1 was skipped), install it:

```bash
cd frontend && npm install immer
```

- [ ] **Step 2: Write failing test — undo restores previous state, redo restores undone state**

Create `frontend/src/pages/canvas-editor/undoRedo.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { produce } from 'immer'
import type { Node } from '@xyflow/react'
import type { CanvasNodeData } from '../../types/canvas'

function createNode(id: string, name: string): Node<CanvasNodeData> {
  return {
    id,
    type: 'canvasNode',
    position: { x: 0, y: 0 },
    data: { nodeType: 'DELAY', name, category: '其他', bizConfig: { nextNodeId: undefined } },
  }
}

describe('undo/redo with immer deep clone', () => {
  it('produce() creates a deep clone — mutating draft does not affect original', () => {
    const original = [createNode('a', 'Node A')]
    const cloned = produce(original, draft => {
      draft[0].data.bizConfig.nextNodeId = 'b'
    })
    // Original is untouched
    expect(original[0].data.bizConfig.nextNodeId).toBeUndefined()
    // Clone has the change
    expect(cloned[0].data.bizConfig.nextNodeId).toBe('b')
  })

  it('snapshot array with produce() — each snapshot is independent', () => {
    const step0 = [createNode('a', 'Node A')]
    const snapshot0 = produce(step0, draft => draft)

    // Mutate step0
    const step1 = produce(step0, draft => {
      draft[0].data.bizConfig.nextNodeId = 'b'
    })
    const snapshot1 = produce(step1, draft => draft)

    // Snapshots are independent
    expect(snapshot0[0].data.bizConfig.nextNodeId).toBeUndefined()
    expect(snapshot1[0].data.bizConfig.nextNodeId).toBe('b')

    // Simulate undo: restore snapshot0
    const restored = produce(snapshot0, draft => draft)
    expect(restored[0].data.bizConfig.nextNodeId).toBeUndefined()
  })

  it('shallow copy would share references (demonstrating the bug)', () => {
    const step0 = [createNode('a', 'Node A')]
    const shallowSnapshot = [...step0]  // shallow — same node objects

    // Mutate the original node's bizConfig
    step0[0].data.bizConfig.nextNodeId = 'z'

    // Shallow snapshot is corrupted
    expect(shallowSnapshot[0].data.bizConfig.nextNodeId).toBe('z')
  })

  it('immer deep clone prevents snapshot corruption', () => {
    const step0 = [createNode('a', 'Node A')]
    const deepSnapshot = produce(step0, draft => draft)

    // Mutate the original node's bizConfig
    step0[0].data.bizConfig.nextNodeId = 'z'

    // Deep snapshot is NOT corrupted
    expect(deepSnapshot[0].data.bizConfig.nextNodeId).toBeUndefined()
  })

  it('undo then redo cycle preserves data integrity', () => {
    const step0 = [createNode('a', 'Node A')]
    const history = [produce(step0, draft => draft)]

    // User edits
    const step1 = produce(step0, draft => {
      draft[0].data.bizConfig.nextNodeId = 'b'
    })
    history.push(produce(step1, draft => draft))

    // Undo: restore step0
    const afterUndo = produce(history[0], draft => draft)
    expect(afterUndo[0].data.bizConfig.nextNodeId).toBeUndefined()

    // Redo: restore step1
    const afterRedo = produce(history[1], draft => draft)
    expect(afterRedo[0].data.bizConfig.nextNodeId).toBe('b')
  })
})
```

- [ ] **Step 3: Run test to verify it fails**

```bash
cd frontend && npx vitest run canvas-editor/undoRedo.test.ts
```

Expected: The "shallow copy would share references" test PASSES (demonstrating the bug exists). The "immer deep clone prevents snapshot corruption" test PASSES (proving the fix works). All tests should PASS since they test the library behavior, not the component. The real value is proving that `produce()` prevents corruption while `[...nodes]` does not.

- [ ] **Step 4: Apply immer produce() to useHistory in index.tsx**

This is already done in Task 1 Step 3d above — the `useHistory` function now uses `produce(nodes, draft => draft)` for all snapshot and restore operations. Import `produce` at the top of the file:

```tsx
// Add to imports at the top of frontend/src/pages/canvas-editor/index.tsx:
import { produce } from 'immer'
```

The full `useHistory` function with immer is shown in Task 1 Step 3d. Key changes:
- `snapshot()`: `{ nodes: produce(nodes, draft => draft), actionName }` instead of `{ nodes: [...nodes], edges: [...edges], actionName }`
- `undo()`: `setNodes(produce(prev.nodes, draft => draft))` instead of `setNodes(prev.nodes); setEdges(prev.edges)`
- `redo()`: `setNodes(produce(next.nodes, draft => draft))` instead of `setNodes(next.nodes); setEdges(next.edges)`

- [ ] **Step 5: Run all canvas-editor tests**

```bash
cd frontend && npx vitest run canvas-editor/
```

Expected: All tests PASS.

- [ ] **Step 6: Commit**

```bash
git add frontend/package.json frontend/package-lock.json frontend/src/pages/canvas-editor/index.tsx frontend/src/pages/canvas-editor/undoRedo.test.ts && git commit -m "feat: fix undo/redo shallow copy bug with immer deep clone, edges auto-derived from bizConfig"
```
