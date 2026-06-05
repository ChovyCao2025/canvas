import type { Node } from '@xyflow/react'
import { describe, expect, it } from 'vitest'
import type { CanvasNodeData } from '../../types/canvas'
import {
  buildBackendNodesFromFlowNodes,
  buildSaveGraphJson,
  isPlaceholderFlowNode,
  realCanvasNodes,
  sameSaveSnapshot,
} from './graphSerialization'

describe('graph serialization', () => {
  it('filters editor-only placeholder nodes before persistence', () => {
    const real = node('api', 'API_CALL', { x: 10.4, y: 20.6 })
    const placeholder: Node = {
      id: 'placeholder',
      position: { x: 0, y: 0 },
      data: { _placeholder: true },
    }

    expect(isPlaceholderFlowNode(placeholder)).toBe(true)
    expect(realCanvasNodes([real, placeholder])).toEqual([real])
    expect(buildBackendNodesFromFlowNodes([real, placeholder])).toEqual([{
      id: 'api',
      type: 'API_CALL',
      name: 'API_CALL',
      category: '测试',
      x: 10,
      y: 21,
      config: { endpoint: '/orders' },
      outletSchema: undefined,
    }])
  })

  it('builds backend graphJson from real nodes only', () => {
    expect(buildSaveGraphJson([node('start', 'START', { x: 1, y: 2 })]))
      .toBe('{"nodes":[{"id":"start","type":"START","name":"START","category":"测试","x":1,"y":2,"config":{"endpoint":"/orders"}}]}')
  })

  it('compares graph, name, settings, and description', () => {
    const base = {
      graphJson: '{"nodes":[]}',
      canvasName: '欢迎旅程',
      canvasSettings: { triggerType: 'REALTIME' as const },
      description: 'desc',
    }

    expect(sameSaveSnapshot(base, { ...base })).toBe(true)
    expect(sameSaveSnapshot(base, { ...base, canvasName: '改名' })).toBe(false)
  })
})

function node(id: string, nodeType: string, position: { x: number; y: number }): Node<CanvasNodeData> {
  return {
    id,
    type: 'canvasNode',
    position,
    data: {
      nodeType,
      name: nodeType,
      category: '测试',
      bizConfig: { endpoint: '/orders' },
    },
  }
}
