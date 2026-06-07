import type { Edge, Node } from '@xyflow/react'
import { produce } from 'immer'
import { createStore } from 'zustand/vanilla'
import { subscribeWithSelector } from 'zustand/middleware'
import type { CanvasNodeData } from '../../types/canvas'
import { cloneEditorSnapshot, type EditorSnapshot } from './editorSnapshot'

export type SaveStatus = 'idle' | 'saving' | 'retrying' | 'saved' | 'failed' | 'conflict'

export interface EditorConflict {
  serverVersion?: number
  localVersion?: number
  message?: string
  payload?: unknown
}

export interface EditorHistoryLabels {
  canUndo: boolean
  canRedo: boolean
  undoLabel: string
  redoLabel: string
}

export interface EditorState {
  nodes: Node<CanvasNodeData>[]
  edges: Edge[]
  selectedNodeId: string | null
  dirty: boolean
  saveStatus: SaveStatus
  saveAttempt: number
  modals: Record<string, boolean>
  conflict?: EditorConflict
  history: EditorSnapshot[]
  future: EditorSnapshot[]
  setSelectedNodeId: (id: string | null) => void
  setModalOpen: (name: string, open: boolean) => void
  markDirty: () => void
  markClean: () => void
  setSaveStatus: (status: SaveStatus, attempt?: number) => void
  setConflict: (conflict?: EditorConflict) => void
  syncGraph: (nodes: Node<CanvasNodeData>[], edges: Edge[]) => void
  snapshot: (nodes: Node<CanvasNodeData>[], edges: Edge[], actionName?: string) => void
  replaceGraph: (nodes: Node<CanvasNodeData>[], edges: Edge[], actionName: string) => void
  undo: () => EditorSnapshot | undefined
  redo: () => EditorSnapshot | undefined
  clearHistory: () => void
}

function cloneGraph(
  nodes: Node<CanvasNodeData>[],
  edges: Edge[],
  actionName = '操作',
): EditorSnapshot {
  return cloneEditorSnapshot({ nodes, edges, actionName })
}

function setGraph(draft: EditorState, snapshot: EditorSnapshot) {
  draft.nodes = snapshot.nodes
  draft.edges = snapshot.edges
}

export function selectEditorHistoryLabels(state: Pick<EditorState, 'history' | 'future'>): EditorHistoryLabels {
  return {
    canUndo: state.history.length > 0,
    canRedo: state.future.length > 0,
    undoLabel: state.history.length ? `撤销：${state.history[state.history.length - 1].actionName}` : '没有可撤销的操作',
    redoLabel: state.future.length ? `重做：${state.future[0].actionName}` : '没有可重做的操作',
  }
}

export function createEditorStore(
  initialNodes: Node<CanvasNodeData>[] = [],
  initialEdges: Edge[] = [],
) {
  return createStore<EditorState>()(
    subscribeWithSelector((set, get) => ({
      nodes: cloneGraph(initialNodes, initialEdges).nodes,
      edges: cloneGraph(initialNodes, initialEdges).edges,
      selectedNodeId: null,
      dirty: false,
      saveStatus: 'idle',
      saveAttempt: 0,
      modals: {},
      history: [],
      future: [],
      setSelectedNodeId: id => set({ selectedNodeId: id }),
      setModalOpen: (name, open) => set(produce<EditorState>(draft => {
        draft.modals[name] = open
      })),
      markDirty: () => set({ dirty: true }),
      markClean: () => set({ dirty: false, saveStatus: 'saved', saveAttempt: 0, conflict: undefined }),
      setSaveStatus: (saveStatus, saveAttempt = 0) => set({ saveStatus, saveAttempt }),
      setConflict: conflict => set({
        conflict,
        saveStatus: conflict ? 'conflict' : get().saveStatus,
      }),
      syncGraph: (nodes, edges) => set(produce<EditorState>(draft => {
        setGraph(draft, cloneGraph(nodes, edges))
      })),
      snapshot: (nodes, edges, actionName = '操作') => set(produce<EditorState>(draft => {
        draft.history.push(cloneGraph(nodes, edges, actionName))
        draft.history = draft.history.slice(-50)
        draft.future = []
      })),
      replaceGraph: (nodes, edges, actionName) => {
        const state = get()
        const previous = cloneGraph(state.nodes, state.edges, actionName)
        const next = cloneGraph(nodes, edges, actionName)
        set(produce<EditorState>(draft => {
          draft.history.push(previous)
          draft.history = draft.history.slice(-50)
          setGraph(draft, next)
          draft.future = []
          draft.dirty = true
        }))
      },
      undo: () => {
        const state = get()
        const previous = state.history[state.history.length - 1]
        if (!previous) return undefined
        const restored = cloneEditorSnapshot(previous)
        const current = cloneGraph(state.nodes, state.edges, previous.actionName)
        set(produce<EditorState>(draft => {
          draft.history.pop()
          draft.future.unshift(current)
          setGraph(draft, cloneEditorSnapshot(previous))
          draft.dirty = true
        }))
        return restored
      },
      redo: () => {
        const state = get()
        const next = state.future[0]
        if (!next) return undefined
        const restored = cloneEditorSnapshot(next)
        const current = cloneGraph(state.nodes, state.edges, next.actionName)
        set(produce<EditorState>(draft => {
          draft.future.shift()
          draft.history.push(current)
          setGraph(draft, cloneEditorSnapshot(next))
          draft.dirty = true
        }))
        return restored
      },
      clearHistory: () => set({ history: [], future: [] }),
    })),
  )
}
