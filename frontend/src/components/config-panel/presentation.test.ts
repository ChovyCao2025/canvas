/**
 * 测试职责：验证配置面板展示模型和 Inspector 卡片样式契约。
 *
 * 维护说明：TAGGER 节点的头部、摘要和分支展示是重点视觉路径，样式调整需同步这些断言。
 */
import { describe, expect, it } from 'vitest'
import type { ReactElement, ReactNode } from 'react'
import type { CanvasNodeData } from '../../types/canvas'
import { CATEGORY_SOLID } from '../canvas/constants'
import { NodeHeaderCard } from './InspectorCards'
import { buildConfigPanelPresentation } from './presentation'

/** 构造 TAGGER 节点默认样本，测试用例按需覆盖字段。 */
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

/** 测试用节点 ID 到展示名的映射，用于校验分支摘要文案。 */
const taggerRouteNames: Record<string, string> = {
  'api-node': '接口调用',
  'city-node': '是否高频消费城市用户',
}

/** 将 ReactNode 收窄为 ReactElement，方便读取组件 props 做样式断言。 */
function asElement(node: ReactNode): ReactElement {
  expect(node).toBeTruthy()
  return node as ReactElement
}

/** 提取 NodeHeaderCard 关键区域样式，避免测试直接依赖完整 JSX 结构。 */
function getHeaderStyles(props: Parameters<typeof NodeHeaderCard>[0]) {
  const card = asElement(NodeHeaderCard(props))
  const topRow = asElement(card.props.children[0])
  const leftColumn = asElement(topRow.props.children[0])
  const typeBadge = asElement(leftColumn.props.children[0])
  const statusPill = asElement(topRow.props.children[1])

  return {
    cardStyle: card.props.style as Record<string, string>,
    badgeStyle: typeBadge.props.style as Record<string, string>,
    statusStyle: statusPill.props.style as Record<string, string>,
  }
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

  it('keeps the tagger shell mist-blue while colorizing its badge and status pill by category', () => {
    const styles = getHeaderStyles({
      tone: 'tagger',
      typeBadge: 'Tagger',
      title: '是否高价值近30天活跃用户',
      metaBadges: [],
      description: '标签判断节点，根据圈选人群决定后续分支流向',
      statusLabel: '已配置',
      categoryColor: CATEGORY_SOLID['逻辑分支'],
    })

    expect(styles.cardStyle.background).toBe('#e9f0f7')
    expect(styles.badgeStyle.color).toBe(CATEGORY_SOLID['逻辑分支'])
    expect(styles.statusStyle.color).toBe(`${CATEGORY_SOLID['逻辑分支']}cc`)
    expect(styles.badgeStyle.background).toBe(`${CATEGORY_SOLID['逻辑分支']}14`)
    expect(styles.statusStyle.background).toBe(`${CATEGORY_SOLID['逻辑分支']}0d`)
    expect(styles.badgeStyle.border).toBe(`1px solid ${CATEGORY_SOLID['逻辑分支']}33`)
    expect(styles.statusStyle.border).toBe(`1px solid ${CATEGORY_SOLID['逻辑分支']}26`)
  })

  it('reuses the node category color for non-tagger badge and keeps the configured pill visually weaker', () => {
    const styles = getHeaderStyles({
      tone: 'default',
      typeBadge: 'API_CALL',
      title: '接口调用',
      metaBadges: [],
      statusLabel: '已配置',
      categoryColor: CATEGORY_SOLID['行为策略'],
    })

    expect(styles.badgeStyle.color).toBe(CATEGORY_SOLID['行为策略'])
    expect(styles.statusStyle.color).toBe(`${CATEGORY_SOLID['行为策略']}cc`)
    expect(styles.badgeStyle.background).toBe(`${CATEGORY_SOLID['行为策略']}14`)
    expect(styles.statusStyle.background).toBe(`${CATEGORY_SOLID['行为策略']}0d`)
    expect(styles.badgeStyle.border).toBe(`1px solid ${CATEGORY_SOLID['行为策略']}33`)
    expect(styles.statusStyle.border).toBe(`1px solid ${CATEGORY_SOLID['行为策略']}26`)
  })
})
