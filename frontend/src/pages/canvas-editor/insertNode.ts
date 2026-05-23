import type { Edge, Node, XYPosition } from '@xyflow/react'
import { DEFAULT_NAMES } from '../../components/canvas/constants'
import type { CanvasNodeData } from '../../types/canvas'

// Task 3 only covers splitting a linear/default edge; branch-specific routing is handled later in editor integration.
export function applyInsertIntoEdge(edge: Edge, nodeId: string) {
  const sourceHandle = edge.sourceHandle ?? 'default'

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
        sourceHandle: 'default',
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
      bizConfig: {},
    },
  }
}
