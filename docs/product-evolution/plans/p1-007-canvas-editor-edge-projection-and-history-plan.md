# Canvas Editor Edge Projection And History Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `bizConfig` the sole route source of truth for canvas editor edges and make undo/redo snapshots immutable.

**Architecture:** Keep `outletRouting.ts` as the pure routing boundary, add missing tests for stale edges and deep snapshot cloning, and shrink `canvas-editor/index.tsx` so React Flow edges are derived from nodes instead of maintained as separate business state.

**Tech Stack:** React 18, TypeScript, React Flow, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p1-007-canvas-editor-edge-projection-and-history.md`

## Current Code Facts

- `frontend/src/pages/canvas-editor/outletRouting.ts` already exports `deriveEdges`, `patchBizConfig`, `clearEdgeRef`, `mergeOutletEdge`, and `appendDirectCallBranch`.
- `frontend/src/pages/canvas-editor/index.tsx` still calls `useEdgesState([])` and stores shallow history snapshots.
- Existing tests: `outletRouting.test.ts`, `graphHydration.test.ts`, `insertNode.test.ts`, `connectionInteraction.test.ts`, `canvasEditorClipboard.test.tsx`.

## File Structure

- Modify: `frontend/src/pages/canvas-editor/outletRouting.ts`
- Modify: `frontend/src/pages/canvas-editor/outletRouting.test.ts`
- Create: `frontend/src/pages/canvas-editor/editorSnapshot.ts`
- Create: `frontend/src/pages/canvas-editor/editorSnapshot.test.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

### Task 1: Stale Edge Projection And Snapshot Tests

**Files:**
- Modify: `frontend/src/pages/canvas-editor/outletRouting.test.ts`
- Create: `frontend/src/pages/canvas-editor/editorSnapshot.test.ts`
- Create: `frontend/src/pages/canvas-editor/editorSnapshot.ts`

- [ ] **Step 1: Add stale edge projection test**

Append to `outletRouting.test.ts`:

```ts
it('does not derive stale edges after bizConfig target is cleared', () => {
  const before = deriveEdges([
    { id: 'node_a', type: 'WAIT', name: '等待', x: 0, y: 0, config: { nextNodeId: 'node_b' } },
  ])
  expect(before).toHaveLength(1)

  const after = deriveEdges([
    { id: 'node_a', type: 'WAIT', name: '等待', x: 0, y: 0, config: { nextNodeId: undefined } },
  ])
  expect(after).toEqual([])
})
```

- [ ] **Step 2: Add deep snapshot tests**

Create `frontend/src/pages/canvas-editor/editorSnapshot.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { cloneEditorSnapshot } from './editorSnapshot'

describe('editorSnapshot', () => {
  it('deep clones nodes, edges, and nested bizConfig', () => {
    const source = {
      nodes: [{
        id: 'n1',
        position: { x: 0, y: 0 },
        data: { nodeType: 'WAIT', name: '等待', category: '控制', bizConfig: { branches: [{ nextNodeId: 'n2' }] } },
      }],
      edges: [{ id: 'n1->n2', source: 'n1', target: 'n2' }],
      actionName: 'connect',
    }

    const cloned = cloneEditorSnapshot(source)
    source.nodes[0].data.bizConfig.branches[0].nextNodeId = 'mutated'

    expect(cloned.nodes[0].data.bizConfig.branches[0].nextNodeId).toBe('n2')
    expect(cloned.edges[0]).toEqual({ id: 'n1->n2', source: 'n1', target: 'n2' })
  })
})
```

- [ ] **Step 3: Run tests and confirm red state**

Run:

```bash
cd frontend && npm test -- outletRouting.test.ts editorSnapshot.test.ts
```

Expected: FAIL because `editorSnapshot.ts` does not exist.

- [ ] **Step 4: Implement snapshot helper**

Create `frontend/src/pages/canvas-editor/editorSnapshot.ts`:

```ts
import type { Edge, Node } from '@xyflow/react'
import type { CanvasNodeData } from '../../types/canvas'

export interface EditorSnapshot {
  nodes: Node<CanvasNodeData>[]
  edges: Edge[]
  actionName: string
}

export function cloneEditorSnapshot(snapshot: EditorSnapshot): EditorSnapshot {
  return {
    actionName: snapshot.actionName,
    nodes: snapshot.nodes.map(node => ({
      ...node,
      position: { ...node.position },
      data: structuredClone(node.data),
    })),
    edges: snapshot.edges.map(edge => ({ ...edge })),
  }
}
```

- [ ] **Step 5: Run projection and snapshot tests**

Run:

```bash
cd frontend && npm test -- outletRouting.test.ts editorSnapshot.test.ts
```

Expected: PASS.

### Task 2: Derive Display Edges From Nodes

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Test: `frontend/src/pages/canvas-editor/connectionInteraction.test.ts`
- Test: `frontend/src/pages/canvas-editor/graphHydration.test.ts`

- [ ] **Step 1: Replace independent business edge state**

In `canvas-editor/index.tsx`, keep transient React Flow edge changes only for UI gestures. Derive persisted display edges from current nodes:

```ts
const backendNodes = useMemo(() => nodes.map(n => ({
  id: n.id,
  type: (n.data as CanvasNodeData).nodeType,
  name: (n.data as CanvasNodeData).name,
  x: n.position.x,
  y: n.position.y,
  config: (n.data as CanvasNodeData).bizConfig ?? {},
  outletSchema: (n.data as CanvasNodeData).outletSchema,
})), [nodes])

const routedEdges = useMemo(() => deriveEdges(backendNodes), [backendNodes])
const displayEdges = useMemo(() => [...routedEdges, ...phEdges], [routedEdges, phEdges])
```

- [ ] **Step 2: Route connect/delete through bizConfig helpers**

Update connect and delete handlers so their durable action is node config mutation:

```ts
setNodes(current => current.map(node => {
  if (node.id !== connection.source) return node
  const data = node.data as CanvasNodeData
  return {
    ...node,
    data: {
      ...data,
      bizConfig: patchBizConfig(data.bizConfig ?? {}, connection.sourceHandle ?? 'default', connection.target!, data.outletSchema),
    },
  }
}))
```

For edge deletion, call `clearEdgeRef(data.bizConfig ?? {}, edge, data.outletSchema)` on the source node, then let `routedEdges` drop the display edge.

- [ ] **Step 3: Use immutable snapshot helper in history**

Replace shallow history pushes:

```ts
setHistory(h => [...h.slice(-49), cloneEditorSnapshot({ nodes, edges: routedEdges, actionName })])
```

Use cloned snapshots for undo/redo restoration.

- [ ] **Step 4: Run editor routing regression tests**

Run:

```bash
cd frontend && npm test -- graphHydration.test.ts outletRouting.test.ts connectionInteraction.test.ts canvasEditorClipboard.test.tsx
```

Expected: PASS.

### Task 3: Build Verification

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `docs/product-evolution/specs/p1-007-canvas-editor-edge-projection-and-history.md`
- Modify: `docs/product-evolution/plans/p1-007-canvas-editor-edge-projection-and-history-plan.md`

- [ ] **Step 1: Run broader editor tests**

Run:

```bash
cd frontend && npm test -- graphHydration.test.ts graphReloadKey.test.ts insertNode.test.ts outletRouting.test.ts connectionInteraction.test.ts settingsPresentation.test.ts localDraft.test.ts canvasEditorClipboard.test.tsx
```

Expected: PASS.

- [ ] **Step 2: Run frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS with TypeScript and Vite build success.

- [ ] **Step 3: Commit**

Run:

```bash
git add frontend/src/pages/canvas-editor docs/product-evolution/specs/p1-007-canvas-editor-edge-projection-and-history.md docs/product-evolution/plans/p1-007-canvas-editor-edge-projection-and-history-plan.md
git commit -m "refactor: derive canvas editor edges from node config"
```

Expected: commit contains only edge projection, snapshot, editor integration, and related docs.
