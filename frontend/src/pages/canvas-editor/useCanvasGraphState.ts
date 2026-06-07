import { useMemo, useState } from 'react'
import {
  useEdgesState,
  useNodesState,
  type Edge,
  type Node,
} from '@xyflow/react'
import { useBranchPlaceholders } from '../../hooks/useBranchPlaceholders'
import type { CanvasNodeData } from '../../types/canvas'
import { buildBackendNodesFromFlowNodes, realCanvasNodes } from './graphSerialization'
import { deriveEdges } from './outletRouting'

export function deriveCanvasRoutedEdges(nodes: Node<CanvasNodeData>[]): Edge[] {
  return deriveEdges(buildBackendNodesFromFlowNodes(nodes))
}

export function deriveCanvasDisplayEdges(
  nodes: Node<CanvasNodeData>[],
  _edgeState: Edge[],
  placeholderEdges: Edge[],
): Edge[] {
  return [...deriveCanvasRoutedEdges(nodes), ...placeholderEdges]
}

/** Owns React Flow graph state and editor-only placeholder derivation. */
export function useCanvasGraphState() {
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edgeState, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const [draggingNodeId, setDraggingNodeId] = useState<string | null>(null)

  const realNodes = useMemo(() => realCanvasNodes(nodes), [nodes])
  const routedEdges = useMemo(() => deriveCanvasRoutedEdges(realNodes), [realNodes])
  const { nodes: phNodes, edges: phEdges } = useBranchPlaceholders(realNodes, routedEdges, draggingNodeId)
  const placeholders = phNodes
  const displayNodes = useMemo(() => [...realNodes, ...phNodes], [realNodes, phNodes])
  const displayEdges = useMemo(
    () => deriveCanvasDisplayEdges(realNodes, edgeState, phEdges),
    [realNodes, edgeState, phEdges],
  )

  return {
    nodes,
    setNodes,
    onNodesChange,
    edges: routedEdges,
    setEdges,
    onEdgesChange,
    draggingNodeId,
    setDraggingNodeId,
    realNodes,
    placeholders,
    displayNodes,
    displayEdges,
  }
}
