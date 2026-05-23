import type { Edge } from '@xyflow/react'
import { describe, expect, it } from 'vitest'
import { applyInsertIntoEdge, buildDetachedNode, buildPlaceholderEdge } from './insertNode'

describe('insertNode helpers', () => {
  const edge: Edge = {
    id: 'a->b',
    source: 'a',
    target: 'b',
    sourceHandle: 'default',
  }

  it('splits one edge into two connected edges', () => {
    const result = applyInsertIntoEdge(edge, 'GROOVY', 'new_node')

    expect(result.removeEdgeId).toBe('a->b')
    expect(result.newEdges).toEqual([
      { id: 'a->new_node', source: 'a', target: 'new_node', sourceHandle: 'default' },
      { id: 'new_node->b', source: 'new_node', target: 'b', sourceHandle: 'default' },
    ])
  })

  it('creates a detached node when dropped on blank canvas', () => {
    const node = buildDetachedNode('new_node', 'GROOVY', '其他', { x: 120, y: 80 })

    expect(node.position).toEqual({ x: 120, y: 80 })
    expect(node.data.nodeType).toBe('GROOVY')
    expect(node.data.bizConfig).toEqual({})
  })

  it('seeds branching node config and routes downstream through its initial branch', () => {
    const node = buildDetachedNode('new_node', 'AB_SPLIT', '逻辑分支', { x: 120, y: 80 })
    const result = applyInsertIntoEdge(edge, 'AB_SPLIT', 'new_node')

    expect(node.data.bizConfig).toEqual({
      groups: [{ groupKey: 'A', nextNodeId: undefined }],
    })
    expect(result.newEdges).toEqual([
      { id: 'a->new_node', source: 'a', target: 'new_node', sourceHandle: 'default' },
      { id: 'new_node->b', source: 'new_node', target: 'b', sourceHandle: 'group-A' },
    ])
  })

  it('creates an edge from placeholder source and handle', () => {
    expect(buildPlaceholderEdge('if_1', 'success', 'new_node')).toEqual({
      id: 'if_1->new_node::success',
      source: 'if_1',
      target: 'new_node',
      sourceHandle: 'success',
    })
  })
})
