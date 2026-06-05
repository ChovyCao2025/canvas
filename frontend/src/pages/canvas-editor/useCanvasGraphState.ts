import { useMemo, useState } from 'react'
import {
  useEdgesState,
  useNodesState,
  type Edge,
  type Node,
} from '@xyflow/react'
import { useBranchPlaceholders } from '../../hooks/useBranchPlaceholders'
import { realCanvasNodes } from './graphSerialization'

/** Owns React Flow graph state and editor-only placeholder derivation. */
export function useCanvasGraphState() {
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const [draggingNodeId, setDraggingNodeId] = useState<string | null>(null)

  const realNodes = realCanvasNodes(nodes)
  const { nodes: phNodes, edges: phEdges } = useBranchPlaceholders(realNodes, edges, draggingNodeId)
  const placeholders = phNodes
  const displayNodes = useMemo(() => [...realNodes, ...phNodes], [realNodes, phNodes])
  const displayEdges = useMemo(() => [...edges, ...phEdges], [edges, phEdges])

  return {
    nodes,
    setNodes,
    onNodesChange,
    edges,
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
