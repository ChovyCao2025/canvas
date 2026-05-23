import type { Edge, Node, XYPosition } from '@xyflow/react'
import { DEFAULT_NAMES } from '../../components/canvas/constants'
import type { BizConfig, CanvasNodeData } from '../../types/canvas'

function buildInitialBizConfig(nodeType: string): BizConfig {
  switch (nodeType) {
    case 'SELECTOR':
      return { branches: [{ label: '如果', nextNodeId: undefined }] }
    case 'IF_CONDITION':
      return { rules: [] }
    case 'PRIORITY':
      return { priorities: [{ order: 1, nextNodeId: undefined }] }
    case 'AB_SPLIT':
      return { groups: [{ groupKey: 'A', nextNodeId: undefined }] }
    default:
      return {}
  }
}

function getInitialOutgoingHandle(nodeType: string): string {
  switch (nodeType) {
    case 'IF_CONDITION':
    case 'AGGREGATE':
    case 'THRESHOLD':
      return 'success'
    case 'MANUAL_APPROVAL':
      return 'approve'
    case 'SELECTOR':
      return 'branch-0'
    case 'PRIORITY':
      return 'priority-0'
    case 'AB_SPLIT':
      return 'group-A'
    default:
      return 'default'
  }
}

export function applyInsertIntoEdge(edge: Edge, nodeType: string, nodeId: string) {
  const sourceHandle = edge.sourceHandle ?? 'default'
  const outgoingHandle = getInitialOutgoingHandle(nodeType)

  return {
    removeEdgeId: edge.id,
    newEdges: [
      {
        id: `${edge.source}->${nodeId}`,
        source: edge.source,
        target: nodeId,
        sourceHandle,
      },
      {
        id: `${nodeId}->${edge.target}`,
        source: nodeId,
        target: edge.target,
        sourceHandle: outgoingHandle,
      },
    ] satisfies Edge[],
  }
}

export function buildPlaceholderEdge(sourceId: string, handleId: string, nodeId: string): Edge {
  return {
    id: `${sourceId}->${nodeId}::${handleId}`,
    source: sourceId,
    target: nodeId,
    sourceHandle: handleId,
  }
}

export function buildDetachedNode(
  nodeId: string,
  nodeType: string,
  category: string,
  position: XYPosition,
): Node<CanvasNodeData> {
  return {
    id: nodeId,
    type: 'canvasNode',
    position,
    data: {
      nodeType,
      category,
      name: DEFAULT_NAMES[nodeType] ?? nodeType,
      bizConfig: buildInitialBizConfig(nodeType),
    },
  }
}
