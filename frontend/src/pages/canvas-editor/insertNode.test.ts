/**
 * 测试职责：验证拖拽插入节点时的边拆分、空白节点和占位边构造规则。
 *
 * 维护说明：画布拖拽/插入交互变化时，应保持默认边插入与分支插入语义分离。
 */
import type { Edge } from '@xyflow/react'
import { describe, expect, it } from 'vitest'
import { applyInsertIntoEdge, buildDetachedNode, buildNodeExpansion, buildPlaceholderEdge } from './insertNode'

describe('insertNode helpers', () => {
  const edge: Edge = {
    id: 'a->b',
    source: 'a',
    target: 'b',
    sourceHandle: 'default',
  }

  it('splits one edge into two connected edges', () => {
    const result = applyInsertIntoEdge(edge, 'new_node')

    expect(result.removeEdgeId).toBe('a->b')
    expect(result.newEdges).toEqual([
      { id: 'a->new_node', source: 'a', target: 'new_node', sourceHandle: 'default', targetHandle: 'input' },
      { id: 'new_node->b', source: 'new_node', target: 'b', sourceHandle: 'default' },
    ])
  })

  it('treats an undefined sourceHandle as default', () => {
    const result = applyInsertIntoEdge({
      id: 'a->b',
      source: 'a',
      target: 'b',
    }, 'new_node')

    expect(result.newEdges).toEqual([
      { id: 'a->new_node', source: 'a', target: 'new_node', sourceHandle: 'default', targetHandle: 'input' },
      { id: 'new_node->b', source: 'new_node', target: 'b', sourceHandle: 'default' },
    ])
  })

  it('preserves the original targetHandle on the downstream replacement edge', () => {
    const result = applyInsertIntoEdge({
      id: 'a->b',
      source: 'a',
      target: 'b',
      sourceHandle: 'default',
      targetHandle: 'input',
    }, 'new_node')

    expect(result.newEdges).toEqual([
      { id: 'a->new_node', source: 'a', target: 'new_node', sourceHandle: 'default', targetHandle: 'input' },
      { id: 'new_node->b', source: 'new_node', target: 'b', sourceHandle: 'default', targetHandle: 'input' },
    ])
  })

  it('rejects non-default source handles', () => {
    expect(() => applyInsertIntoEdge({
      id: 'if_1->b',
      source: 'if_1',
      target: 'b',
      sourceHandle: 'success',
    }, 'new_node')).toThrow('applyInsertIntoEdge only supports default sourceHandle edges')
  })

  it('creates a detached node when dropped on blank canvas', () => {
    const node = buildDetachedNode('new_node', 'GROOVY', '其他', { x: 120, y: 80 })

    expect(node.position).toEqual({ x: 120, y: 80 })
    expect(node.data.nodeType).toBe('GROOVY')
    expect(node.data.bizConfig).toEqual({})
  })

  it('preserves preset display name and conversation wait config on expansion', () => {
    const expansion = buildNodeExpansion({
      nodeId: 'conversation_wait',
      nodeType: 'WAIT',
      category: '等待与汇聚',
      position: { x: 120, y: 80 },
      displayName: '等待会话回复',
      bizConfig: {
        waitType: 'UNTIL_EVENT',
        eventCode: 'CONVERSATION_REPLY',
      },
    })

    expect(expansion.nodes[0].data.name).toBe('等待会话回复')
    expect(expansion.nodes[0].data.bizConfig).toEqual({
      waitType: 'UNTIL_EVENT',
      eventCode: 'CONVERSATION_REPLY',
    })
  })

  it('creates an edge from placeholder source and handle', () => {
    expect(buildPlaceholderEdge('if_1', 'success', 'new_node')).toEqual({
      id: 'if_1->new_node::success',
      source: 'if_1',
      target: 'new_node',
      sourceHandle: 'success',
      targetHandle: 'input',
    })
  })
})
