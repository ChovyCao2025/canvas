import type { Node } from '@xyflow/react'
import type { BackendNode, CanvasNodeData } from '../../types/canvas'
import type { CanvasSettingsLike } from './settingsPresentation'

export interface ComparableSaveSnapshot {
  graphJson: string
  canvasName: string
  canvasSettings: CanvasSettingsLike
  description?: string
}

interface PlaceholderLikeData {
  _placeholder?: unknown
}

/** Runtime guard for editor-only placeholder nodes that must never be persisted. */
export function isPlaceholderFlowNode(node: Node): boolean {
  return Boolean((node.data as PlaceholderLikeData | undefined)?._placeholder)
}

/** Keep only real canvas nodes and narrow their data model for graph operations. */
export function realCanvasNodes(nodes: Node[]): Node<CanvasNodeData>[] {
  return nodes.filter(node => !isPlaceholderFlowNode(node)) as Node<CanvasNodeData>[]
}

/** Convert React Flow nodes into backend graph_json nodes. */
export function buildBackendNodesFromFlowNodes(nodes: Node[]): BackendNode[] {
  return realCanvasNodes(nodes).map(node => {
    const data = node.data
    return {
      id: node.id,
      type: data.nodeType,
      name: data.name,
      category: data.category,
      x: Math.round(node.position.x),
      y: Math.round(node.position.y),
      config: data.bizConfig,
      outletSchema: data.outletSchema,
    }
  })
}

/** Build persisted graphJson; edge relationships are stored in node bizConfig. */
export function buildSaveGraphJson(nodes: Node[]): string {
  return JSON.stringify({ nodes: buildBackendNodesFromFlowNodes(nodes) })
}

/** Compare save snapshots to decide whether another save pass is needed. */
export function sameSaveSnapshot(a: ComparableSaveSnapshot, b: ComparableSaveSnapshot): boolean {
  return a.graphJson === b.graphJson
    && a.canvasName === b.canvasName
    && JSON.stringify(a.canvasSettings) === JSON.stringify(b.canvasSettings)
    && a.description === b.description
}
