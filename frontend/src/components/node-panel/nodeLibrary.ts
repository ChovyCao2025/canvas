import type { NodeTypeRegistry } from '../../types'

export const DEFAULT_COMMON_NODE_TYPES = [
  'API_CALL',
  'DELAY',
  'MANUAL_APPROVAL',
  'SEND_MQ',
]

const ALL_CATEGORIES_LABEL = '全部'
const CATEGORY_ORDER = [
  '其他',
  '逻辑分支',
  '流程控制',
  '行为策略',
  '用户触达',
  '权益发放',
]

const SUMMARY_FALLBACK: Record<string, string> = {
  API_CALL: '请求外部服务并拿回结果',
  CANVAS_TRIGGER: '复用已有流程能力',
  DELAY: '等待一段时间后继续执行',
  GROOVY: '处理复杂逻辑或字段加工',
  MANUAL_APPROVAL: '等待人工确认后继续流程',
  SEND_MQ: '发送一条业务消息给下游系统',
  SUB_FLOW_REF: '引用已有子流程片段',
}

export function buildCategoryOptions(nodes: NodeTypeRegistry[]) {
  const categories = Array.from(new Set(nodes.map(node => node.category))).sort((a, b) => {
    const aIdx = CATEGORY_ORDER.indexOf(a)
    const bIdx = CATEGORY_ORDER.indexOf(b)
    if (aIdx === -1 && bIdx === -1) return a.localeCompare(b, 'zh-CN')
    if (aIdx === -1) return 1
    if (bIdx === -1) return -1
    return aIdx - bIdx
  })
  return [ALL_CATEGORIES_LABEL, ...categories]
}

export function getNodeSummary(node: NodeTypeRegistry) {
  const summary = (node.description ?? '').trim()
  return summary || SUMMARY_FALLBACK[node.typeKey] || node.typeName
}

export function buildNodeLibraryView(
  nodes: NodeTypeRegistry[],
  options: {
    activeCategory: string
    keyword: string
    commonTypeKeys: string[]
  },
) {
  const normalizedKeyword = options.keyword.trim().toLowerCase()
  const commonTypeKeys = new Set(options.commonTypeKeys)

  const filteredNodes = nodes.filter(node => {
    if (
      options.activeCategory !== ALL_CATEGORIES_LABEL &&
      node.category !== options.activeCategory
    ) {
      return false
    }

    if (!normalizedKeyword) {
      return true
    }

    const haystack = `${node.typeName} ${getNodeSummary(node)}`.toLowerCase()
    return haystack.includes(normalizedKeyword)
  })

  const commonNodes = filteredNodes.filter(node => commonTypeKeys.has(node.typeKey))

  return {
    commonNodes,
    filteredNodes,
  }
}
