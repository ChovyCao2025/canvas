import type { Edge, Node } from '@xyflow/react'
import { produce } from 'immer'
import { createStore } from 'zustand/vanilla'
import { subscribeWithSelector } from 'zustand/middleware'
import type { CanvasNodeData } from '../../types/canvas'
import { cloneEditorSnapshot, type EditorSnapshot } from './editorSnapshot'

/** 编辑器保存状态，覆盖自动保存、重试、冲突等 UI 展示状态。 */
export type SaveStatus = 'idle' | 'saving' | 'retrying' | 'saved' | 'failed' | 'conflict'

/** 保存冲突信息，用于提示本地版本和服务端版本差异。 */
export interface EditorConflict {
  /** 服务端最新编辑版本。 */
  serverVersion?: number

  /** 当前本地编辑版本。 */
  localVersion?: number

  /** 冲突提示文案。 */
  message?: string

  /** 发生冲突时的本地保存载荷。 */
  payload?: unknown
}

/** 撤销/重做按钮展示所需的派生状态。 */
export interface EditorHistoryLabels {
  /** 是否存在可撤销快照。 */
  canUndo: boolean

  /** 是否存在可重做快照。 */
  canRedo: boolean

  /** 撤销按钮 tooltip 文案。 */
  undoLabel: string

  /** 重做按钮 tooltip 文案。 */
  redoLabel: string
}

/** 画布编辑器的集中状态，保存图、选中项、保存状态和历史栈。 */
export interface EditorState {
  /** 当前真实画布节点。 */
  nodes: Node<CanvasNodeData>[]

  /** 当前真实画布边。 */
  edges: Edge[]

  /** 当前选中节点 ID。 */
  selectedNodeId: string | null

  /** 是否存在未保存变更。 */
  dirty: boolean

  /** 当前保存状态。 */
  saveStatus: SaveStatus

  /** 当前保存尝试次数。 */
  saveAttempt: number

  /** 编辑器弹窗开关表。 */
  modals: Record<string, boolean>

  /** 当前保存冲突信息。 */
  conflict?: EditorConflict

  /** 撤销历史栈。 */
  history: EditorSnapshot[]

  /** 重做历史栈。 */
  future: EditorSnapshot[]

  /** 设置当前选中节点。 */
  setSelectedNodeId: (id: string | null) => void

  /** 打开或关闭指定弹窗。 */
  setModalOpen: (name: string, open: boolean) => void

  /** 标记存在未保存变更。 */
  markDirty: () => void

  /** 标记当前内容已保存。 */
  markClean: () => void

  /** 更新保存状态和尝试次数。 */
  setSaveStatus: (status: SaveStatus, attempt?: number) => void

  /** 设置或清除保存冲突。 */
  setConflict: (conflict?: EditorConflict) => void

  /** 同步 React Flow 中的最新图到 store。 */
  syncGraph: (nodes: Node<CanvasNodeData>[], edges: Edge[]) => void

  /** 记录一个可撤销快照，不改变当前图。 */
  snapshot: (nodes: Node<CanvasNodeData>[], edges: Edge[], actionName?: string) => void

  /** 替换当前图并把旧图压入历史栈。 */
  replaceGraph: (nodes: Node<CanvasNodeData>[], edges: Edge[], actionName: string) => void

  /** 撤销到上一个快照。 */
  undo: () => EditorSnapshot | undefined

  /** 重做到下一个快照。 */
  redo: () => EditorSnapshot | undefined

  /** 清空撤销/重做历史。 */
  clearHistory: () => void
}

/** 克隆图快照，避免历史栈持有 React Flow 可变对象引用。 */
function cloneGraph(
  nodes: Node<CanvasNodeData>[],
  edges: Edge[],
  actionName = '操作',
): EditorSnapshot {
  return cloneEditorSnapshot({ nodes, edges, actionName })
}

/** 将快照内容写回 zustand draft。 */
function setGraph(draft: EditorState, snapshot: EditorSnapshot) {
  draft.nodes = snapshot.nodes
  draft.edges = snapshot.edges
}

/** 从历史栈生成撤销/重做按钮状态和说明文案。 */
export function selectEditorHistoryLabels(state: Pick<EditorState, 'history' | 'future'>): EditorHistoryLabels {
  return {
    canUndo: state.history.length > 0,
    canRedo: state.future.length > 0,
    undoLabel: state.history.length ? `撤销：${state.history[state.history.length - 1].actionName}` : '没有可撤销的操作',
    redoLabel: state.future.length ? `重做：${state.future[0].actionName}` : '没有可重做的操作',
  }
}

/** 创建画布编辑器 store，封装保存状态、弹窗状态和撤销/重做历史。 */
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
        // React Flow 状态是源数据，这里仅同步副本供工具栏和历史逻辑读取。
        setGraph(draft, cloneGraph(nodes, edges))
      })),
      snapshot: (nodes, edges, actionName = '操作') => set(produce<EditorState>(draft => {
        draft.history.push(cloneGraph(nodes, edges, actionName))
        // 限制历史长度，避免长时间编辑导致内存持续增长。
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
          // 新操作会截断 redo 分支。
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
          // 当前图进入 future，便于 redo 回来。
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
          // 重做前的当前图重新进入 history，保持撤销链完整。
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
