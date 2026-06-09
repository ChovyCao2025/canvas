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

/** 根据节点内出口配置重新推导真实业务边。 */
export function deriveCanvasRoutedEdges(nodes: Node<CanvasNodeData>[]): Edge[] {
  return deriveEdges(buildBackendNodesFromFlowNodes(nodes))
}

/** 合成画布展示边：真实业务边 + 编辑器分支占位辅助边。 */
export function deriveCanvasDisplayEdges(
  nodes: Node<CanvasNodeData>[],
  _edgeState: Edge[],
  placeholderEdges: Edge[],
): Edge[] {
  return [...deriveCanvasRoutedEdges(nodes), ...placeholderEdges]
}

/** 管理 React Flow 图状态，并派生仅编辑器展示用的分支占位节点和边。 */
export function useCanvasGraphState() {
  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edgeState, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const [draggingNodeId, setDraggingNodeId] = useState<string | null>(null)

  // 真实节点排除占位节点，所有序列化、连线推导和保存都以真实节点为准。
  const realNodes = useMemo(() => realCanvasNodes(nodes), [nodes])
  const routedEdges = useMemo(() => deriveCanvasRoutedEdges(realNodes), [realNodes])
  // 分支占位节点只用于拖拽吸附，不进入保存数据。
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
