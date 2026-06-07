import { describe, expect, it } from 'vitest'
import {
  canvasGraphSchema,
  nodeTypeRegistrySchema,
  parseCanvasGraph,
  parseCanvasGraphJson,
} from './canvasSchemas'

describe('canvasSchemas', () => {
  it('accepts a valid graph with node config and outlet schema', () => {
    const graph = parseCanvasGraph({
      nodes: [{
        id: 'start',
        type: 'START',
        name: '开始',
        category: '其他',
        x: 0,
        y: 0,
        config: {},
        outletSchema: JSON.stringify([{ id: 'default', label: '默认', targetField: 'nextNodeId' }]),
      }],
      edges: [],
    })

    expect(graph.nodes[0].id).toBe('start')
  })

  it('rejects invalid graph roots and nodes without id or type', () => {
    expect(() => parseCanvasGraph({})).toThrow('Invalid canvas graph')
    expect(() => parseCanvasGraph({ nodes: [{ id: 'n1' }], edges: [] })).toThrow('Invalid canvas graph')
  })

  it('rejects strict outlet target typos', () => {
    expect(() => canvasGraphSchema.parse({
      nodes: [{
        id: 'wait',
        type: 'WAIT',
        name: '等待',
        x: 0,
        y: 0,
        config: {},
        outletSchema: JSON.stringify([{ id: 'success', label: '成功', targetField: 'next_node_id' }]),
      }],
      edges: [],
    })).toThrow()
  })

  it('rejects invalid edge route shapes when edges are present', () => {
    expect(() => parseCanvasGraph({
      nodes: [{ id: 'a', type: 'START', name: '开始', x: 0, y: 0, config: {} }],
      edges: [{ source: 'a', target: 42 }],
    })).toThrow('Invalid canvas graph')
  })

  it('parses graph JSON with an empty graph fallback', () => {
    expect(parseCanvasGraphJson('').nodes).toEqual([])
    expect(() => parseCanvasGraphJson('{bad json')).toThrow('Invalid canvas graph JSON')
  })

  it('validates node registry outlet schemas', () => {
    expect(nodeTypeRegistrySchema.parse({
      typeKey: 'WAIT',
      typeName: '等待',
      category: '控制',
      configSchema: '[]',
      outputSchema: '{}',
      outletSchema: JSON.stringify([{ id: 'timeout', label: '超时', targetField: 'timeoutNodeId' }]),
      isTrigger: 0,
      isTerminal: 0,
      enabled: 1,
    }).typeKey).toBe('WAIT')
  })
})
