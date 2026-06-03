# Frontend Zustand Store Refactor Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans.

**Goal:** Extract EditorInner state into Zustand store with fine-grained subscriptions. Move Dagre layout to Web Worker. Lazy-load Modal/Drawer.

**Architecture:** canvasEditorStore with independent selectors. Components subscribe only to needed slices. Dagre layout runs off main thread via Web Worker.

**Tech Stack:** Zustand, @dagrejs/dagre, Web Worker API, immer

---

### Task 1: Create Zustand Store

**Files:**
- Create: `frontend/src/pages/canvas-editor/canvasEditorStore.ts`
- Test: `frontend/src/pages/canvas-editor/__tests__/canvasEditorStore.test.ts`

- [ ] **Step 1: Write failing test**

```ts
import { useCanvasEditorStore } from '../canvasEditorStore'

describe('canvasEditorStore', () => {
  beforeEach(() => {
    useCanvasEditorStore.setState({
      selectedNodeId: null,
      nodes: [],
      isDirty: false,
      undoStack: [],
      redoStack: [],
    });
  });

  test('setSelectedNodeId updates state', () => {
    const { setSelectedNodeId, selectedNodeId } = useCanvasEditorStore.getState();
    setSelectedNodeId('node-1');
    expect(useCanvasEditorStore.getState().selectedNodeId).toBe('node-1');
  });

  test('updateNodeBizConfig modifies node in place', () => {
    useCanvasEditorStore.setState({
      nodes: [{ id: 'node-1', data: { bizConfig: { yesNodeId: null } } } as any],
    });
    useCanvasEditorStore.getState().updateNodeBizConfig('node-1', { yesNodeId: 'node-2' });
    const node = useCanvasEditorStore.getState().nodes.find(n => n.id === 'node-1');
    expect(node!.data.bizConfig.yesNodeId).toBe('node-2');
  });

  test('undo/redo cycles correctly', () => {
    // Push initial state as a snapshot
    useCanvasEditorStore.getState().pushUndo({
      selectedNodeId: null,
      nodes: [],
      isDirty: false,
    });
    useCanvasEditorStore.getState().setSelectedNodeId('node-1');
    // Push dirty state as a snapshot
    useCanvasEditorStore.getState().pushUndo({
      selectedNodeId: 'node-1',
      nodes: [],
      isDirty: true,
    });
    useCanvasEditorStore.getState().undo();
    expect(useCanvasEditorStore.getState().selectedNodeId).toBeNull();
    useCanvasEditorStore.getState().redo();
    expect(useCanvasEditorStore.getState().selectedNodeId).toBe('node-1');
  });
});
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx vitest run pages/canvas-editor/__tests__/canvasEditorStore.test.ts`
Expected: FAIL.

- [ ] **Step 3: Define type interfaces and implement canvasEditorStore**

```ts
import { create } from 'zustand'
import { immer } from 'zustand/middleware/immer'

// ── Type definitions ──────────────────────────────────────────

/** Business configuration attached to a canvas node. */
interface BizConfig {
  yesNodeId?: string | null
  noNodeId?: string | null
  [key: string]: unknown
}

/** Minimal node type matching @xyflow/react's Node structure. */
interface CanvasNode {
  id: string
  type?: string
  position?: { x: number; y: number }
  data: {
    bizConfig: BizConfig
    [key: string]: unknown
  }
}

/**
 * Snapshot of the editor state for undo/redo.
 * Only stores the fields that change via user actions.
 */
interface CanvasSnapshot {
  selectedNodeId: string | null
  nodes: CanvasNode[]
  isDirty: boolean
}

// ── Store interface ───────────────────────────────────────────

interface CanvasEditorState {
  selectedNodeId: string | null
  nodes: CanvasNode[]
  isDirty: boolean
  undoStack: CanvasSnapshot[]
  redoStack: CanvasSnapshot[]
  setSelectedNodeId: (id: string | null) => void
  setNodes: (nodes: CanvasNode[]) => void
  updateNodeBizConfig: (nodeId: string, config: Partial<BizConfig>) => void
  pushUndo: (snapshot: CanvasSnapshot) => void
  undo: () => void
  redo: () => void
}

// ── Snapshot helpers ──────────────────────────────────────────

/**
 * Capture the current state as a CanvasSnapshot.
 * This reads from the immer draft (a plain JS object during the producer).
 */
function currentSnapshot(state: CanvasEditorState): CanvasSnapshot {
  return {
    selectedNodeId: state.selectedNodeId,
    nodes: [...state.nodes],
    isDirty: state.isDirty,
  }
}

/**
 * Apply a CanvasSnapshot to the current state, restoring selectedNodeId
 * and isDirty. Nodes are restored by reference (caller should ensure
 * the snapshot's node array is the desired state).
 */
function applySnapshot(state: CanvasEditorState, snapshot: CanvasSnapshot): void {
  state.selectedNodeId = snapshot.selectedNodeId
  state.nodes = snapshot.nodes
  state.isDirty = snapshot.isDirty
}

// ── Store implementation ──────────────────────────────────────

export const useCanvasEditorStore = create<CanvasEditorState>()(
  immer((set, get) => ({
    selectedNodeId: null,
    nodes: [],
    isDirty: false,
    undoStack: [],
    redoStack: [],

    setSelectedNodeId: (id) => set({ selectedNodeId: id }),
    setNodes: (nodes) => set({ nodes }),

    updateNodeBizConfig: (nodeId, config) =>
      set((state) => {
        const node = state.nodes.find((n) => n.id === nodeId);
        if (node) Object.assign(node.data.bizConfig, config);
      }),

    pushUndo: (snapshot) =>
      set((state) => {
        state.undoStack.push(snapshot);
        state.redoStack = [];
      }),

    undo: () =>
      set((state) => {
        if (state.undoStack.length === 0) return;
        const snapshot = state.undoStack.pop()!;
        state.redoStack.push(currentSnapshot(state));
        applySnapshot(state, snapshot);
      }),

    redo: () =>
      set((state) => {
        if (state.redoStack.length === 0) return;
        const snapshot = state.redoStack.pop()!;
        state.undoStack.push(currentSnapshot(state));
        applySnapshot(state, snapshot);
      }),
  }))
);

// Re-export types for consumers
export type { CanvasNode, BizConfig, CanvasSnapshot }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npx vitest run pages/canvas-editor/__tests__/canvasEditorStore.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/canvas-editor/canvasEditorStore.ts
git add frontend/src/pages/canvas-editor/__tests__/canvasEditorStore.test.ts
git commit -m "feat: create Zustand store for canvas editor with fine-grained subscriptions"
```

---

### Task 2: Move Dagre Layout to Web Worker

**Files:**
- Create: `frontend/src/pages/canvas-editor/layoutWorker.ts`
- Test: `frontend/src/pages/canvas-editor/__tests__/layoutWorker.test.ts`

- [ ] **Step 1: Write failing test**

> **Note on Web Worker testing:** Vitest does not natively support Web Worker syntax
> (`new Worker(new URL(...))`). The worker logic must be tested by importing and calling
> the handler function directly. Add the following to `vitest.config.ts` (or the existing
> `vite.config.ts` test section) to ensure worker files are transpiled:
>
> ```ts
> test: {
>   environment: 'node',
>   // Worker files need to be processed by the same Vite pipeline
>   include: ['src/**/*.{test,spec}.{ts,tsx}'],
> }
> ```

```ts
import { computeLayout } from '../layoutWorker'

describe('layoutWorker', () => {
  test('processes layout request and returns positioned nodes', () => {
    const nodes = [
      { id: 'a', width: 200, height: 80 },
      { id: 'b', width: 200, height: 80 },
    ]
    const edges = [{ source: 'a', target: 'b' }]

    const result = computeLayout(nodes, edges)

    expect(result).toHaveLength(2)
    expect(result[0].position).toBeDefined()
    expect(result[0].position.x).toBeGreaterThan(0)
    expect(result[1].position).toBeDefined()
  })

  test('handles empty edges gracefully', () => {
    const nodes = [{ id: 'a', width: 200, height: 80 }]
    const edges: { source: string; target: string }[] = []

    const result = computeLayout(nodes, edges)
    expect(result).toHaveLength(1)
  })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd frontend && npx vitest run pages/canvas-editor/__tests__/layoutWorker.test.ts`
Expected: FAIL.

- [ ] **Step 3: Create layout logic (importable function + Web Worker wrapper)**

```ts
// layoutWorker.ts
import dagre from '@dagrejs/dagre'

interface LayoutNode { id: string; width?: number; height?: number; position?: { x: number; y: number } }
interface LayoutEdge { source: string; target: string }

/**
 * Compute dagre layout. Exported as a regular function for testability.
 * The Web Worker wrapper below calls this function.
 */
export function computeLayout(nodes: LayoutNode[], edges: LayoutEdge[]): LayoutNode[] {
  const g = new dagre.graphlib.Graph()
  g.setDefaultEdgeLabel(() => ({}))
  g.setGraph({ rankdir: 'TB', nodesep: 50, ranksep: 80 })

  for (const node of nodes) {
    g.setNode(node.id, { width: node.width || 200, height: node.height || 80 })
  }
  for (const edge of edges) {
    g.setEdge(edge.source, edge.target)
  }

  dagre.layout(g)

  return nodes.map((n) => {
    const pos = g.node(n.id)
    return { ...n, position: { x: pos.x, y: pos.y } }
  })
}

// ── Web Worker entry point ────────────────────────────────────
// This self.onmessage handler is only executed in a Worker context.
// In Node.js test environment, it's a no-op since `self` is the global.

// TypeScript type guard for WorkerGlobalScope — avoids TS2304 reference error
// and ensures the onmessage assignment only runs in an actual Worker context.
const isWorkerScope: boolean =
  typeof globalThis !== 'undefined' &&
  'WorkerGlobalScope' in globalThis &&
  self instanceof (globalThis as any).WorkerGlobalScope

if (isWorkerScope) {
  const workerSelf = self as unknown as Worker
  workerSelf.onmessage = (e: MessageEvent<{ nodes: LayoutNode[]; edges: LayoutEdge[] }>) => {
    const result = computeLayout(e.data.nodes, e.data.edges)
    workerSelf.postMessage(result)
  }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd frontend && npx vitest run pages/canvas-editor/__tests__/layoutWorker.test.ts`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add frontend/src/pages/canvas-editor/layoutWorker.ts
git add frontend/src/pages/canvas-editor/__tests__/layoutWorker.test.ts
git commit -m "feat: create Dagre layout module with computeLayout function and Web Worker wrapper"
```
