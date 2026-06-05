/**
 * 测试职责：验证画布连线命中半径大于 React Flow 默认值。
 *
 * 维护说明：节点 handle 尺寸调整时，应重新评估该半径是否仍适合拖线交互。
 */
import { describe, expect, it } from 'vitest'
import type { Edge, Node } from '@xyflow/react'
import type { CanvasNodeData } from '../../types/canvas'
import { CANVAS_CONNECTION_RADIUS, canCreateCanvasConnection } from './connectionInteraction'
import {
  CANVAS_CONNECTION_RADIUS as ADAPTER_CONNECTION_RADIUS,
  canCreateCanvasConnection as canCreateCanvasConnectionViaAdapter,
} from './reactFlowAdapter'

describe('canvas connection interaction', () => {
  it('uses a larger connection radius than the React Flow default', () => {
    expect(CANVAS_CONNECTION_RADIUS).toBeGreaterThan(20)
  })

  it('allows START to keep fan-out connections from the default outlet', () => {
    const nodes: Node<CanvasNodeData>[] = [
      node('start', 'START'),
      node('trigger_a', 'EVENT_TRIGGER'),
      node('trigger_b', 'MQ_TRIGGER'),
    ]
    const edges: Edge[] = [
      { id: 'start->trigger_a', source: 'start', target: 'trigger_a', sourceHandle: 'default' },
    ]

    expect(canCreateCanvasConnection({
      source: 'start',
      sourceHandle: 'default',
      target: 'trigger_b',
      targetHandle: 'input',
    }, nodes, edges)).toBe(true)
  })

  it('allows DIRECT_CALL to keep fan-out connections from the default outlet', () => {
    const nodes: Node<CanvasNodeData>[] = [
      node('direct', 'DIRECT_CALL'),
      node('api_a', 'API_CALL'),
      node('api_b', 'API_CALL'),
    ]
    const edges: Edge[] = [
      { id: 'direct->api_a', source: 'direct', target: 'api_a', sourceHandle: 'default' },
    ]

    expect(canCreateCanvasConnection({
      source: 'direct',
      sourceHandle: 'default',
      target: 'api_b',
      targetHandle: 'input',
    }, nodes, edges)).toBe(true)
  })

  it('exposes connection policy through the React Flow adapter boundary', () => {
    const nodes: Node<CanvasNodeData>[] = [
      node('start', 'START'),
      node('trigger', 'EVENT_TRIGGER'),
    ]

    expect(ADAPTER_CONNECTION_RADIUS).toBe(CANVAS_CONNECTION_RADIUS)
    expect(canCreateCanvasConnectionViaAdapter({
      source: 'start',
      sourceHandle: 'default',
      target: 'trigger',
      targetHandle: 'input',
    }, nodes, [])).toBe(true)
  })
})

function node(id: string, nodeType: string): Node<CanvasNodeData> {
  return {
    id,
    type: 'canvasNode',
    position: { x: 0, y: 0 },
    data: {
      nodeType,
      name: nodeType,
      category: '其他',
      bizConfig: {},
    },
  }
}
