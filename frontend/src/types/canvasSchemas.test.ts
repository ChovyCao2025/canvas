import { describe, expect, it } from 'vitest'
import {
  canvasGraphSchema,
  nodeTypeRegistrySchema,
  parseCanvasGraph,
  parseCanvasGraphJson,
  riskDecisionConfigSchema,
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

  describe('RISK_DECISION config schema', () => {
    const validRiskDecisionConfig = {
      sceneKey: 'MARKETING_BENEFIT_ISSUE',
      subjectMapping: { userId: '$.profile.userId' },
      eventMapping: { amount: '$.event.amount' },
      contextMapping: { caller: 'CANVAS_NODE' },
      actionRoutes: {
        ALLOW: 'node_allow',
        REVIEW: 'node_review',
        VERIFY: 'node_verify',
        BLOCK: 'node_block',
        LIMIT: 'node_limit',
        DELAY: 'node_delay',
      },
      failPolicy: 'FAIL_REVIEW',
      timeoutMs: 50,
      includeTrace: false,
    }

    it('requires sceneKey', () => {
      expect(() => riskDecisionConfigSchema.parse({
        ...validRiskDecisionConfig,
        sceneKey: '',
      })).toThrow()
    })

    it('requires at least one subject mapping', () => {
      expect(() => riskDecisionConfigSchema.parse({
        ...validRiskDecisionConfig,
        subjectMapping: {},
      })).toThrow()
    })

    it('requires eventMapping', () => {
      expect(() => riskDecisionConfigSchema.parse({
        ...validRiskDecisionConfig,
        eventMapping: {},
      })).toThrow()
    })

    it('requires actionRoutes.ALLOW', () => {
      const { ALLOW: _allow, ...routesWithoutAllow } = validRiskDecisionConfig.actionRoutes

      expect(() => riskDecisionConfigSchema.parse({
        ...validRiskDecisionConfig,
        actionRoutes: routesWithoutAllow,
      })).toThrow()
    })

    it('accepts REVIEW, VERIFY, BLOCK, LIMIT, and DELAY routes', () => {
      const parsed = riskDecisionConfigSchema.parse(validRiskDecisionConfig)

      expect(parsed.actionRoutes.REVIEW).toBe('node_review')
      expect(parsed.actionRoutes.VERIFY).toBe('node_verify')
      expect(parsed.actionRoutes.BLOCK).toBe('node_block')
      expect(parsed.actionRoutes.LIMIT).toBe('node_limit')
      expect(parsed.actionRoutes.DELAY).toBe('node_delay')
    })

    it('rejects timeoutMs below 10', () => {
      expect(() => riskDecisionConfigSchema.parse({
        ...validRiskDecisionConfig,
        timeoutMs: 9,
      })).toThrow()
    })

    it('rejects timeoutMs above 500', () => {
      expect(() => riskDecisionConfigSchema.parse({
        ...validRiskDecisionConfig,
        timeoutMs: 501,
      })).toThrow()
    })

    it('accepts FAIL_OPEN, FAIL_REVIEW, and FAIL_CLOSED', () => {
      expect(riskDecisionConfigSchema.parse({
        ...validRiskDecisionConfig,
        failPolicy: 'FAIL_OPEN',
      }).failPolicy).toBe('FAIL_OPEN')
      expect(riskDecisionConfigSchema.parse({
        ...validRiskDecisionConfig,
        failPolicy: 'FAIL_REVIEW',
      }).failPolicy).toBe('FAIL_REVIEW')
      expect(riskDecisionConfigSchema.parse({
        ...validRiskDecisionConfig,
        failPolicy: 'FAIL_CLOSED',
      }).failPolicy).toBe('FAIL_CLOSED')
    })
  })
})
