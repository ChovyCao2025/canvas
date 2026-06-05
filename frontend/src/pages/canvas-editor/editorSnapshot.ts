import type { Edge, Node } from '@xyflow/react'
import type { CanvasNodeData } from '../../types/canvas'

export interface EditorSnapshot {
  nodes: Node<CanvasNodeData>[]
  edges: Edge[]
  actionName: string
}

function cloneData(data: CanvasNodeData): CanvasNodeData {
  if (typeof structuredClone === 'function') return structuredClone(data)
  return JSON.parse(JSON.stringify(data)) as CanvasNodeData
}

export function cloneEditorSnapshot(snapshot: EditorSnapshot): EditorSnapshot {
  return {
    actionName: snapshot.actionName,
    nodes: snapshot.nodes.map(node => ({
      ...node,
      position: { ...node.position },
      data: cloneData(node.data),
    })),
    edges: snapshot.edges.map(edge => ({ ...edge })),
  }
}
