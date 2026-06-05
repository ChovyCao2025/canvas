/**
 * 测试职责：验证右侧配置面板的必要展示模型契约。
 *
 * 维护说明：这里只覆盖会影响全节点兼容和配置可读性的纯逻辑，不做脆弱的 JSX 结构快照。
 */
import { describe, expect, it } from 'vitest'
import type { CanvasNodeData } from '../../types/canvas'
import { buildConfigPanelPresentation, resolveConnectorWarning, resolveContextValueListFieldKey } from './presentation'

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

const routeNames: Record<string, string> = {
  'api-node': '接口调用',
  'city-node': '是否高频消费城市用户',
  'coupon-node': '发送优惠券',
}

describe('buildConfigPanelPresentation', () => {
  it('uses the schema field key for context-value-list bindings', () => {
    expect(resolveContextValueListFieldKey('data')).toBe('data')
    expect(resolveContextValueListFieldKey('bizData')).toBe('bizData')
  })

  it('returns connector warnings for non-real send message channels', () => {
    expect(resolveConnectorWarning({
      nodeType: 'SEND_MESSAGE',
      bizConfig: { channel: 'SMS', provider: 'ALIYUN' },
      connectorMode: 'DISABLED',
    })).toBe('SMS/ALIYUN is disabled')

    expect(resolveConnectorWarning({
      nodeType: 'SEND_MESSAGE',
      bizConfig: { channel: 'SMS', provider: 'ALIYUN' },
      connectorMode: 'REAL',
    })).toBeNull()
  })

  it('groups visible schema fields by editing intent', () => {
    const model = buildConfigPanelPresentation({
      nodeData: {
        nodeType: 'API_CALL',
        name: '接口调用',
        category: '数据操作',
        bizConfig: {},
      },
      formValues: {},
      displayValues: {},
      fields: [
        { key: 'apiKey', label: '接口', type: 'select' },
        { key: 'rules', label: '条件', type: 'condition-rule-list' },
        { key: 'eventFilters', label: '事件过滤', type: 'condition-builder' },
        { key: 'inputParams', label: '请求参数', type: 'api-input-params' },
        { key: 'jsonPathMappings', label: '字段映射', type: 'json-path-mapping-list' },
        { key: 'eventAttrs', label: '事件属性', type: 'event-attr-preview' },
      ],
      getNodeName: () => null,
    })

    expect(model.fieldGroups).toEqual([
      { key: 'basic', title: '基础配置', fields: [{ key: 'apiKey', label: '接口', type: 'select' }] },
      { key: 'rules', title: '条件规则', summary: '2 项', fields: [
        { key: 'rules', label: '条件', type: 'condition-rule-list' },
        { key: 'eventFilters', label: '事件过滤', type: 'condition-builder' },
      ] },
      { key: 'mapping', title: '参数映射', summary: '2 项', fields: [
        { key: 'inputParams', label: '请求参数', type: 'api-input-params' },
        { key: 'jsonPathMappings', label: '字段映射', type: 'json-path-mapping-list' },
      ] },
      { key: 'preview', title: '预览信息', summary: '1 项', fields: [{ key: 'eventAttrs', label: '事件属性', type: 'event-attr-preview' }] },
    ])
  })

  it('groups user input form schema as mapping configuration', () => {
    const model = buildConfigPanelPresentation({
      nodeData: {
        nodeType: 'USER_INPUT',
        name: '收集用户信息',
        category: '等待与汇聚',
        bizConfig: {},
      },
      formValues: {},
      displayValues: {},
      fields: [
        { key: 'formSchema', label: '表单字段', type: 'user-input-field-list' },
        { key: 'maxWait', label: '最长等待', type: 'duration' },
      ],
      getNodeName: () => null,
    })

    expect(model.fieldGroups).toEqual([
      { key: 'basic', title: '基础配置', fields: [{ key: 'maxWait', label: '最长等待', type: 'duration' }] },
      { key: 'mapping', title: '参数映射', summary: '1 项', fields: [
        { key: 'formSchema', label: '表单字段', type: 'user-input-field-list' },
      ] },
    ])
  })

  it('builds generic outlet route summaries from node handles and bizConfig targets', () => {
    const model = buildConfigPanelPresentation({
      nodeData: {
        nodeType: 'IF_CONDITION',
        name: '高价值用户判断',
        category: '逻辑分支',
        bizConfig: {
          successNodeId: 'coupon-node',
          failNodeId: undefined,
        },
      },
      formValues: {},
      displayValues: {},
      fields: [],
      getNodeName: (id) => routeNames[id ?? ''] ?? null,
    })

    expect(model.header.metaBadges).toContain('2 出口')
    expect(model.branchRoutes).toEqual([
      { label: '条件成立', value: '发送优惠券', tone: 'success' },
      { label: '否则', value: '未连接', tone: 'danger' },
    ])
  })

  it('builds SPLIT route summaries from branch ids', () => {
    const model = buildConfigPanelPresentation({
      nodeData: {
        nodeType: 'SPLIT',
        name: '通用分流',
        category: '条件与分流',
        bizConfig: {
          branches: [
            { branchId: 'a', label: 'A组', weight: 30, nextNodeId: 'coupon-node' },
            { branchId: 'b', label: 'B组', weight: 70, nextNodeId: undefined },
          ],
        },
      },
      formValues: {},
      displayValues: {},
      fields: [],
      getNodeName: (id) => routeNames[id ?? ''] ?? null,
    })

    expect(model.branchRoutes).toEqual([
      { label: 'A组 30%', value: '发送优惠券', tone: 'success' },
      { label: 'B组 70%', value: '未连接', tone: 'success' },
    ])
  })

  it('keeps tagger mode badges and routes readable', () => {
    const model = buildConfigPanelPresentation({
      nodeData: taggerNode(),
      formValues: { mode: 'audience' },
      displayValues: { mode: '人群圈选' },
      fields: [
        { key: 'mode', label: '模式展示', type: 'select' },
        { key: 'audienceId', label: '圈选对象', type: 'select' },
      ],
      getNodeName: (id) => routeNames[id ?? ''] ?? null,
    })

    expect(model.header.tone).toBe('tagger')
    expect(model.header.typeBadge).toBe('TAGGER')
    expect(model.header.metaBadges).toEqual(['2 出口'])
    expect(model.summaryRows).toEqual([
      { label: '模式展示', value: '人群圈选' },
      { label: '圈选对象', value: 'audience_vip' },
    ])
    expect(model.branchRoutes).toEqual([
      { label: '命中', value: '接口调用', tone: 'success' },
      { label: '未命中', value: '是否高频消费城市用户', tone: 'danger' },
    ])
  })
})
