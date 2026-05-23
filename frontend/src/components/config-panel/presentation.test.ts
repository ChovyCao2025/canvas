import { describe, expect, it } from 'vitest'
import type { CanvasNodeData } from '../../types/canvas'
import { buildConfigPanelPresentation } from './presentation'

const taggerNode = (overrides: Partial<CanvasNodeData> = {}): CanvasNodeData => ({
  nodeType: 'TAGGER',
  name: '是否高价值近30天活跃用户',
  category: '逻辑分支',
  bizConfig: {
    mode: 'audience',
    audienceId: 'audience_vip',
    hitNextNodeId: 'api-node',
    missNextNodeId: 'city-node',
  },
  ...overrides,
})

const taggerRouteNames: Record<string, string> = {
  'api-node': '接口调用',
  'city-node': '是否高频消费城市用户',
}

describe('buildConfigPanelPresentation', () => {
  it('builds the approved tagger header and summary rows from raw values plus display lookups', () => {
    const input = {
      nodeData: taggerNode(),
      formValues: {
        mode: 'audience',
        audienceId: 101,
      },
      displayValues: {
        mode: '人群圈选',
        audienceId: '演示：高价值近30天活跃用户',
      },
      fields: [
        { key: 'mode', label: '模式展示', type: 'select' },
        { key: 'tagCodeKey', label: '标签', type: 'select' },
        { key: 'audienceId', label: '圈选对象', type: 'select' },
      ],
      getNodeName: (id: string | undefined) => taggerRouteNames[id ?? ''] ?? null,
    }
    const model = buildConfigPanelPresentation(input)

    expect(model.header.tone).toBe('tagger')
    expect(model.header.typeBadge).toBe('Tagger')
    expect(model.header.title).toBe('是否高价值近30天活跃用户')
    expect(model.header.metaBadges).toEqual(['人群圈选', 'Audience Segment'])
    expect(model.header.description).toBe('标签判断节点，根据圈选人群决定后续分支流向')
    expect(model.summaryRows).toEqual([
      { label: '模式展示', value: '人群圈选' },
      { label: '圈选对象', value: '演示：高价值近30天活跃用户' },
    ])
    expect(model.branchRoutes).toEqual([
      { label: '命中分支', value: '接口调用', tone: 'success' },
      { label: '未命中分支', value: '是否高频消费城市用户', tone: 'danger' },
    ])
  })

  it('falls back to 未连接 when a tagger branch target is missing', () => {
    const model = buildConfigPanelPresentation({
      nodeData: taggerNode({
        bizConfig: { mode: 'audience', hitNextNodeId: undefined, missNextNodeId: 'city-node' },
      }),
      formValues: { mode: 'audience', audienceId: '101' },
      displayValues: {},
      fields: [
        { key: 'mode', label: '标签模式', type: 'select' },
        { key: 'audienceId', label: '人群', type: 'select' },
      ],
      getNodeName: () => null,
    })

    expect(model.branchRoutes).toEqual([
      { label: '命中分支', value: '未连接', tone: 'success' },
      { label: '未命中分支', value: '未连接', tone: 'danger' },
    ])
    expect(model.summaryRows).toEqual([
      { label: '标签模式', value: 'audience' },
      { label: '人群', value: '101' },
    ])
  })

  it('keeps the tagger supporting copy weak and non-interactive', () => {
    const model = buildConfigPanelPresentation({
      nodeData: taggerNode(),
      formValues: { mode: 'audience' },
      displayValues: { mode: '人群圈选' },
      fields: [],
      getNodeName: () => null,
    })

    expect(model.header.description).toBe('标签判断节点，根据圈选人群决定后续分支流向')
    expect(model.header.metaBadges).toContain('Audience Segment')
  })

  it('prefers display labels for non-audience tagger mode badges', () => {
    const model = buildConfigPanelPresentation({
      nodeData: taggerNode(),
      formValues: { mode: 'realtime' },
      displayValues: { mode: '实时触发（监听 MQ 事件）' },
      fields: [],
      getNodeName: () => null,
    })

    expect(model.header.metaBadges).toEqual(['实时触发（监听 MQ 事件）'])
  })

  it('keeps non-tagger nodes on the default inspector path', () => {
    const model = buildConfigPanelPresentation({
      nodeData: {
        nodeType: 'API_CALL',
        name: '接口调用',
        category: '行为策略',
        bizConfig: {},
      },
      formValues: {},
      displayValues: {},
      fields: [],
      getNodeName: () => null,
    })

    expect(model.header.tone).toBe('default')
    expect(model.summaryRows).toEqual([])
    expect(model.branchRoutes).toEqual([])
  })
})
