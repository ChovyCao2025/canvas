import { describe, expect, it } from 'vitest'
import type { Edge, Node } from '@xyflow/react'
import type { CanvasNodeData } from '../../types/canvas'
import { deriveCanvasDisplayEdges } from './useCanvasGraphState'

describe('useCanvasGraphState projection helpers', () => {
  it('derives display edges from node bizConfig and ignores stale edge state', () => {
    const nodes: Node<CanvasNodeData>[] = [
      node('node_a', { nextNodeId: undefined }),
      node('node_b', {}),
    ]
    const staleEdges: Edge[] = [
      { id: 'node_a->node_b', source: 'node_a', target: 'node_b', sourceHandle: 'default' },
    ]

    expect(deriveCanvasDisplayEdges(nodes, staleEdges, [])).toEqual([])
  })

  it('keeps editor-only placeholder edges after routed edges', () => {
    const nodes: Node<CanvasNodeData>[] = [
      node('node_a', { nextNodeId: 'node_b' }),
      node('node_b', {}),
    ]
    const placeholderEdges: Edge[] = [
      { id: 'node_b->placeholder::default', source: 'node_b', target: 'placeholder', sourceHandle: 'default' },
    ]

    expect(deriveCanvasDisplayEdges(nodes, [], placeholderEdges).map(edge => edge.id)).toEqual([
      'node_a->node_b',
      'node_b->placeholder::default',
    ])
  })
})

function node(id: string, bizConfig: CanvasNodeData['bizConfig']): Node<CanvasNodeData> {
  return {
    id,
    type: 'canvasNode',
    position: { x: 0, y: 0 },
    data: {
      nodeType: 'WAIT',
      name: id,
      category: '控制',
      bizConfig,
    },
  }
}
