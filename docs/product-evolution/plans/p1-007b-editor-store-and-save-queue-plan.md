# Editor Store And Save Queue Implementation Plan

Status: Historical plan evidence records implementation and verification; commit and merge status was not verified in this docs-only audit.

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Move high-churn canvas editor state into a tested store and add a bounded save queue.

**Architecture:** Introduce a focused Zustand store for editor UI/graph state after P1-007 removes edge dual-state risk. Keep save retry logic in a pure `saveQueue.ts` state machine so queue behavior is tested before integrating with `canvas-editor/index.tsx`.

**Tech Stack:** React 18, TypeScript, Zustand, Immer, Vitest.

---

## Spec Reference

- `docs/product-evolution/specs/p1-007b-editor-store-and-save-queue.md`

## File Structure

- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Create: `frontend/src/pages/canvas-editor/editorStore.ts`
- Create: `frontend/src/pages/canvas-editor/editorStore.test.ts`
- Create: `frontend/src/pages/canvas-editor/saveQueue.ts`
- Create: `frontend/src/pages/canvas-editor/saveQueue.test.ts`
- Modify: `frontend/src/pages/canvas-editor/index.tsx`

### Task 1: Dependencies And Store Tests

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`
- Create: `frontend/src/pages/canvas-editor/editorStore.test.ts`
- Create: `frontend/src/pages/canvas-editor/editorStore.ts`

- [x] **Step 1: Add dependencies**

Run:

```bash
cd frontend && npm install zustand immer
```

Expected: `package.json` includes `zustand` and `immer`; `package-lock.json` is updated.

Actual: `zustand` and `immer` were added as direct dependencies.

- [x] **Step 2: Write editor store tests**

Create `frontend/src/pages/canvas-editor/editorStore.test.ts`:

```ts
import { describe, expect, it } from 'vitest'
import { createEditorStore } from './editorStore'

describe('editorStore', () => {
  it('tracks selected node, dirty state, and modal state', () => {
    const store = createEditorStore()

    store.getState().setSelectedNodeId('node-1')
    store.getState().setModalOpen('publish', true)
    store.getState().markDirty()

    expect(store.getState().selectedNodeId).toBe('node-1')
    expect(store.getState().modals.publish).toBe(true)
    expect(store.getState().dirty).toBe(true)
  })

  it('pushes immutable history and supports undo redo', () => {
    const store = createEditorStore()
    store.getState().replaceGraph([{ id: 'a', data: { bizConfig: { nextNodeId: 'b' } } } as any], 'load')
    store.getState().replaceGraph([{ id: 'a', data: { bizConfig: { nextNodeId: 'c' } } } as any], 'edit')

    const undo = store.getState().undo()
    expect(undo?.nodes[0].data.bizConfig.nextNodeId).toBe('b')
    undo!.nodes[0].data.bizConfig.nextNodeId = 'mutated'

    const redo = store.getState().redo()
    expect(redo?.nodes[0].data.bizConfig.nextNodeId).toBe('c')
  })

  it('updates save state without clearing conflict data accidentally', () => {
    const store = createEditorStore()

    store.getState().setConflict({ serverVersion: 3, localVersion: 2 })
    store.getState().setSaveStatus('retrying')

    expect(store.getState().saveStatus).toBe('retrying')
    expect(store.getState().conflict?.serverVersion).toBe(3)
  })
})
```

- [x] **Step 3: Run store tests and confirm red state**

Run:

```bash
cd frontend && npm test -- editorStore.test.ts
```

Expected: FAIL because `editorStore.ts` does not exist.

Actual: failed because `./editorStore` could not be resolved.

- [x] **Step 4: Implement editor store**

Create `frontend/src/pages/canvas-editor/editorStore.ts`:

```ts
import { createStore } from 'zustand/vanilla'
import { produce } from 'immer'
import type { Node } from '@xyflow/react'
import type { CanvasNodeData } from '../../types/canvas'
import { cloneEditorSnapshot, type EditorSnapshot } from './editorSnapshot'

export type SaveStatus = 'idle' | 'saving' | 'retrying' | 'saved' | 'failed' | 'conflict'

export interface EditorState {
  nodes: Node<CanvasNodeData>[]
  selectedNodeId?: string
  dirty: boolean
  saveStatus: SaveStatus
  modals: Record<string, boolean>
  conflict?: { serverVersion: number; localVersion: number }
  history: EditorSnapshot[]
  future: EditorSnapshot[]
  setSelectedNodeId: (id?: string) => void
  setModalOpen: (name: string, open: boolean) => void
  markDirty: () => void
  setSaveStatus: (status: SaveStatus) => void
  setConflict: (conflict?: EditorState['conflict']) => void
  replaceGraph: (nodes: Node<CanvasNodeData>[], actionName: string) => void
  undo: () => EditorSnapshot | undefined
  redo: () => EditorSnapshot | undefined
}

export function createEditorStore(initialNodes: Node<CanvasNodeData>[] = []) {
  return createStore<EditorState>((set, get) => ({
    nodes: initialNodes,
    dirty: false,
    saveStatus: 'idle',
    modals: {},
    history: [],
    future: [],
    setSelectedNodeId: id => set({ selectedNodeId: id }),
    setModalOpen: (name, open) => set(produce<EditorState>(draft => { draft.modals[name] = open })),
    markDirty: () => set({ dirty: true }),
    setSaveStatus: saveStatus => set({ saveStatus }),
    setConflict: conflict => set({ conflict, saveStatus: conflict ? 'conflict' : get().saveStatus }),
    replaceGraph: (nodes, actionName) => set(produce<EditorState>(draft => {
      draft.history.push(cloneEditorSnapshot({ nodes: draft.nodes, edges: [], actionName }))
      draft.nodes = nodes
      draft.future = []
      draft.dirty = true
    })),
    undo: () => {
      const state = get()
      const previous = state.history.at(-1)
      if (!previous) return undefined
      set(produce<EditorState>(draft => {
        draft.history.pop()
        draft.future.unshift(cloneEditorSnapshot({ nodes: draft.nodes, edges: [], actionName: previous.actionName }))
        draft.nodes = previous.nodes
        draft.dirty = true
      }))
      return cloneEditorSnapshot(previous)
    },
    redo: () => {
      const state = get()
      const next = state.future[0]
      if (!next) return undefined
      set(produce<EditorState>(draft => {
        draft.future.shift()
        draft.history.push(cloneEditorSnapshot({ nodes: draft.nodes, edges: [], actionName: next.actionName }))
        draft.nodes = next.nodes
        draft.dirty = true
      }))
      return cloneEditorSnapshot(next)
    },
  }))
}
```

- [x] **Step 5: Run store tests**

Run:

```bash
cd frontend && npm test -- editorStore.test.ts editorSnapshot.test.ts
```

Expected: PASS.

Actual: implemented `editorStore.ts` with Zustand, Immer, selector subscriptions, graph mirroring, save state, conflict state, modal state, and immutable undo/redo snapshots.

### Task 2: Save Queue State Machine

**Files:**
- Create: `frontend/src/pages/canvas-editor/saveQueue.test.ts`
- Create: `frontend/src/pages/canvas-editor/saveQueue.ts`

- [x] **Step 1: Write save queue tests**

Create `frontend/src/pages/canvas-editor/saveQueue.test.ts`:

```ts
import { describe, expect, it, vi } from 'vitest'
import { createSaveQueue } from './saveQueue'

describe('saveQueue', () => {
  it('coalesces rapid edits into the latest payload', async () => {
    const save = vi.fn().mockResolvedValue({ version: 2 })
    const queue = createSaveQueue({ save, maxAttempts: 3, baseDelayMs: 1 })

    const first = queue.enqueue({ version: 1, nodes: ['old'] })
    const second = queue.enqueue({ version: 1, nodes: ['latest'] })
    await Promise.all([first, second])

    expect(save).toHaveBeenCalledTimes(1)
    expect(save).toHaveBeenCalledWith({ version: 1, nodes: ['latest'] }, expect.any(AbortSignal))
  })

  it('retries transient errors then recovers', async () => {
    const save = vi.fn()
      .mockRejectedValueOnce({ retryable: true })
      .mockResolvedValueOnce({ version: 3 })
    const queue = createSaveQueue({ save, maxAttempts: 3, baseDelayMs: 1 })

    const result = await queue.enqueue({ version: 2 })

    expect(result.status).toBe('saved')
    expect(save).toHaveBeenCalledTimes(2)
  })

  it('returns conflict without clearing local payload', async () => {
    const save = vi.fn().mockRejectedValue({ status: 409, serverVersion: 5 })
    const queue = createSaveQueue({ save, maxAttempts: 3, baseDelayMs: 1 })

    const result = await queue.enqueue({ version: 4, nodes: ['local'] })

    expect(result).toEqual({ status: 'conflict', serverVersion: 5, payload: { version: 4, nodes: ['local'] } })
  })
})
```

- [x] **Step 2: Run save queue tests and confirm red state**

Run:

```bash
cd frontend && npm test -- saveQueue.test.ts
```

Expected: FAIL because `saveQueue.ts` does not exist.

Actual: failed because `./saveQueue` could not be resolved.

- [x] **Step 3: Implement save queue**

Create `frontend/src/pages/canvas-editor/saveQueue.ts`:

```ts
export interface SaveQueueOptions<TPayload> {
  save: (payload: TPayload, signal: AbortSignal) => Promise<unknown>
  maxAttempts: number
  baseDelayMs: number
}

export type SaveQueueResult<TPayload> =
  | { status: 'saved' }
  | { status: 'failed'; payload: TPayload }
  | { status: 'conflict'; serverVersion: number; payload: TPayload }

export function createSaveQueue<TPayload>(options: SaveQueueOptions<TPayload>) {
  let pending: TPayload | undefined
  let inFlight: Promise<SaveQueueResult<TPayload>> | undefined
  let controller: AbortController | undefined

  async function flush(): Promise<SaveQueueResult<TPayload>> {
    const payload = pending!
    pending = undefined
    controller = new AbortController()
    for (let attempt = 1; attempt <= options.maxAttempts; attempt += 1) {
      try {
        await options.save(payload, controller.signal)
        return { status: 'saved' }
      } catch (error: any) {
        if (error?.status === 409) {
          return { status: 'conflict', serverVersion: error.serverVersion, payload }
        }
        if (!error?.retryable || attempt === options.maxAttempts) {
          return { status: 'failed', payload }
        }
        await new Promise(resolve => setTimeout(resolve, options.baseDelayMs * 2 ** (attempt - 1)))
      }
    }
    return { status: 'failed', payload }
  }

  return {
    enqueue(payload: TPayload) {
      pending = payload
      if (!inFlight) {
        inFlight = Promise.resolve()
          .then(() => flush())
          .finally(() => { inFlight = undefined })
      }
      return inFlight
    },
    abort() {
      controller?.abort()
    },
  }
}
```

- [x] **Step 4: Run save queue tests**

Run:

```bash
cd frontend && npm test -- saveQueue.test.ts
```

Expected: PASS.

Actual: `saveQueue.ts` covers latest-payload coalescing, active-save draining, bounded retry, exponential backoff, transient recovery, conflict preservation, retry exhaustion, state reporting, and abort support.

### Task 3: Editor Integration And Verification

**Files:**
- Modify: `frontend/src/pages/canvas-editor/index.tsx`
- Modify: `docs/product-evolution/specs/p1-007b-editor-store-and-save-queue.md`
- Modify: `docs/product-evolution/plans/p1-007b-editor-store-and-save-queue-plan.md`

- [x] **Step 1: Integrate store and queue into editor**

Move selection, dirty state, modal state, save status, conflict state, and undo/redo calls from `canvas-editor/index.tsx` into `useStore(createEditorStore(...))`. Keep node creation, connect, edit config, undo, redo, save, dry-run, and publish behavior unchanged.

Actual: `canvas-editor/index.tsx` now uses `useStore(createEditorStore(...))` for selected node, dirty state, settings/message-preview modal state, save state, conflict state, and undo/redo labels/restoration. React Flow graph rendering remains in `useCanvasGraphState` and is mirrored into the store.

- [x] **Step 2: Wire save queue to existing save command**

Wrap the current save API call:

```ts
const saveQueue = useMemo(() => createSaveQueue({
  save: (payload, signal) => canvasApi.saveGraph(canvasId, payload, { signal }),
  maxAttempts: 3,
  baseDelayMs: 300,
}), [canvasId])
```

Map `saved`, `failed`, and `conflict` results into store save state and visible editor banners.

Actual: the existing save command is wrapped by `createSaveQueue({ maxAttempts: 3, baseDelayMs: 300 })`; queue states drive save status and visible retry/conflict/failure alerts. Failed and conflicted saves preserve local drafts.

- [x] **Step 3: Run focused tests**

Run:

```bash
cd frontend && npm test -- editorStore.test.ts saveQueue.test.ts localDraft.test.ts canvasNameUpdate.test.ts graphHydration.test.ts connectionInteraction.test.ts
```

Expected: PASS.

Actual: `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run test -- editorStore.test.ts saveQueue.test.ts localDraft.test.ts canvasNameUpdate.test.ts graphHydration.test.ts connectionInteraction.test.ts useCanvasHistoryState.test.ts useCanvasGraphState.test.ts` passed: 8 files, 29 tests.

- [x] **Step 4: Run frontend build**

Run:

```bash
cd frontend && npm run build
```

Expected: PASS.

Actual: `cd frontend && PATH="/opt/homebrew/bin:$PATH" npm run build` passed.

Commit boundary: no commit was created in this docs-only audit; commit and merge status remains unverified.

Run:

```bash
git add frontend/package.json frontend/package-lock.json frontend/src/pages/canvas-editor docs/product-evolution/specs/p1-007b-editor-store-and-save-queue.md docs/product-evolution/plans/p1-007b-editor-store-and-save-queue-plan.md
git commit -m "refactor: add canvas editor store and save queue"
```

Expected: commit contains only editor store, save queue, integration, package metadata, tests, and related docs.

Actual: skipped in this session because the user requested implementation without committing.
