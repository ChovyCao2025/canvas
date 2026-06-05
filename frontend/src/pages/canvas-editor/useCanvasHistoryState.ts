import { useCallback, useState } from 'react'
import { useReactFlow, type Edge, type Node } from '@xyflow/react'
import type { CanvasNodeData } from '../../types/canvas'
import { cloneEditorSnapshot, type EditorSnapshot } from './editorSnapshot'

export interface CanvasHistoryState {
  snapshot: (actionName?: string) => void
  undo: () => void
  redo: () => void
  canUndo: boolean
  canRedo: boolean
  undoLabel: string
  redoLabel: string
}

export function getCanvasHistoryLabels(
  history: EditorSnapshot[],
  future: EditorSnapshot[],
): Pick<CanvasHistoryState, 'undoLabel' | 'redoLabel'> {
  return {
    undoLabel: history.length ? `撤销：${history[history.length - 1].actionName}` : '没有可撤销的操作',
    redoLabel: future.length ? `重做：${future[0].actionName}` : '没有可重做的操作',
  }
}

/** Canvas editor local undo/redo history. */
export function useCanvasHistoryState(
  nodes: Node<CanvasNodeData>[],
  edges: Edge[],
): CanvasHistoryState {
  const [history, setHistory] = useState<EditorSnapshot[]>([])
  const [future, setFuture] = useState<EditorSnapshot[]>([])
  const { setNodes, setEdges } = useReactFlow()

  const snapshot = useCallback((actionName = '操作') => {
    setHistory(items => [...items.slice(-49), cloneEditorSnapshot({ nodes, edges, actionName })])
    setFuture([])
  }, [nodes, edges])

  const undo = useCallback(() => {
    if (!history.length) return
    const prev = history[history.length - 1]
    setFuture(items => [cloneEditorSnapshot({ nodes, edges, actionName: prev.actionName }), ...items])
    setHistory(items => items.slice(0, -1))
    const restored = cloneEditorSnapshot(prev)
    setNodes(restored.nodes)
    setEdges(restored.edges)
  }, [history, nodes, edges, setNodes, setEdges])

  const redo = useCallback(() => {
    if (!future.length) return
    const next = future[0]
    setHistory(items => [...items, cloneEditorSnapshot({ nodes, edges, actionName: next.actionName })])
    setFuture(items => items.slice(1))
    const restored = cloneEditorSnapshot(next)
    setNodes(restored.nodes)
    setEdges(restored.edges)
  }, [future, nodes, edges, setNodes, setEdges])

  const { undoLabel, redoLabel } = getCanvasHistoryLabels(history, future)

  return {
    snapshot,
    undo,
    redo,
    canUndo: history.length > 0,
    canRedo: future.length > 0,
    undoLabel,
    redoLabel,
  }
}
