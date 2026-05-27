/**
 * 测试职责：验证节点库分类排序、搜索过滤、常用节点和摘要兜底规则。
 *
 * 维护说明：节点库体验依赖稳定排序和搜索范围，新增分类排序规则时先补这里。
 */
import { describe, expect, it } from 'vitest'

import type { NodeTypeRegistry } from '../../types'
import {
  DEFAULT_COMMON_NODE_TYPES,
  buildCategoryOptions,
  buildNodeLibraryView,
  getNodeSummary,
} from './nodeLibrary'

/** 节点库测试样本，覆盖有描述、无描述、不同分类和触发节点。 */
const nodes: NodeTypeRegistry[] = [
  {
    typeKey: 'API_CALL',
    typeName: '接口调用',
    category: '其他',
    configSchema: '[]',
    outputSchema: '[]',
    isTrigger: 0,
    isTerminal: 0,
    description: '调用外部服务并写回结果',
    enabled: 1,
  },
  {
    typeKey: 'GROOVY',
    typeName: 'Groovy脚本',
    category: '其他',
    configSchema: '[]',
    outputSchema: '[]',
    isTrigger: 0,
    isTerminal: 0,
    description: '',
    enabled: 1,
  },
  {
    typeKey: 'IF_CONDITION',
    typeName: 'IF判断',
    category: '逻辑分支',
    configSchema: '[]',
    outputSchema: '[]',
    isTrigger: 0,
    isTerminal: 0,
    description: '根据条件决定后续分支',
    enabled: 1,
  },
  {
    typeKey: 'START',
    typeName: '开始',
    category: '流程控制',
    configSchema: '[]',
    outputSchema: '[]',
    isTrigger: 1,
    isTerminal: 0,
    description: '流程唯一入口',
    enabled: 1,
  },
]

describe('nodeLibrary helpers', () => {
  it('always includes 全部 first and keeps the preferred category order', () => {
    expect(buildCategoryOptions(nodes)).toEqual(['全部', '其他', '逻辑分支', '流程控制'])
  })

  it('filters by category and keyword together', () => {
    const view = buildNodeLibraryView(nodes, {
      activeCategory: '其他',
      keyword: 'groovy',
      commonTypeKeys: DEFAULT_COMMON_NODE_TYPES,
    })

    expect(view.filteredNodes.map(node => node.typeKey)).toEqual(['GROOVY'])
  })

  it('returns category-aware common nodes before full list', () => {
    const view = buildNodeLibraryView(nodes, {
      activeCategory: '其他',
      keyword: '',
      commonTypeKeys: ['API_CALL', 'IF_CONDITION'],
    })

    expect(view.commonNodes.map(node => node.typeKey)).toEqual(['API_CALL'])
    expect(view.filteredNodes.map(node => node.typeKey)).toEqual(['API_CALL', 'GROOVY'])
  })

  it('keeps category color ownership outside the helper layer', () => {
    const view = buildNodeLibraryView(nodes, {
      activeCategory: '全部',
      keyword: '',
      commonTypeKeys: DEFAULT_COMMON_NODE_TYPES,
    })

    expect(view.filteredNodes[0].category).toBe('其他')
  })

  it('falls back to generic summary when description is empty', () => {
    expect(getNodeSummary(nodes[1])).toBe('处理复杂逻辑或字段加工')
  })
})
