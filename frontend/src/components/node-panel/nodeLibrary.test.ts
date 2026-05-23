import { describe, expect, it } from 'vitest'

import type { NodeTypeRegistry } from '../../types'
import {
  DEFAULT_COMMON_NODE_TYPES,
  buildCategoryOptions,
  buildNodeLibraryView,
  getNodeSummary,
} from './nodeLibrary'

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
]

describe('nodeLibrary helpers', () => {
  it('always includes 全部 category first', () => {
    expect(buildCategoryOptions(nodes)).toEqual(['全部', '其他', '逻辑分支'])
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

  it('falls back to generic summary when description is empty', () => {
    expect(getNodeSummary(nodes[1])).toBe('处理复杂逻辑或字段加工')
  })
})
