/**
 * 组件职责：节点库视图模型工具，将后端节点类型列表整理为分类、摘要和过滤结果。
 *
 * 维护说明：这些纯函数由组件和测试共用，避免 UI 状态与排序规则混杂。
 */
import type { NodeTypeRegistry } from '../../types'

/** 默认展示在“常用节点”区域的节点类型，帮助新用户快速找到高频能力。 */
export const DEFAULT_COMMON_NODE_TYPES = [
  'API_CALL',
  'DELAY',
  'MANUAL_APPROVAL',
  'SEND_MQ',
]

/** “全部”分类的固定文案，过滤时作为特殊分类处理。 */
const ALL_CATEGORIES_LABEL = '全部'

/** 业务上更符合使用频率的分类顺序；未列入分类会按中文名称排在后面。 */
const CATEGORY_ORDER = [
  '其他',
  '逻辑分支',
  '流程控制',
  '行为策略',
  '用户触达',
  '权益发放',
]

/** 后端没有维护 description 时的兜底摘要，避免节点库出现空说明。 */
const SUMMARY_FALLBACK: Record<string, string> = {
  API_CALL: '请求外部服务并拿回结果',
  CANVAS_TRIGGER: '复用已有流程能力',
  DELAY: '等待一段时间后继续执行',
  GROOVY: '处理复杂逻辑或字段加工',
  MANUAL_APPROVAL: '等待人工确认后继续流程',
  SEND_MQ: '发送一条业务消息给下游系统',
  SUB_FLOW_REF: '引用已有子流程片段',
}

/** 构建分类筛选项，并把“全部”固定在第一项。 */
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

/** 读取节点摘要；优先使用后端 description，再回退到前端兜底文案和节点名。 */
export function getNodeSummary(node: NodeTypeRegistry) {
  const summary = (node.description ?? '').trim()
  return summary || SUMMARY_FALLBACK[node.typeKey] || node.typeName
}

/** 根据当前分类、关键词和常用节点配置计算节点库视图模型。 */
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
    // 搜索范围限定在名称和摘要，避免 typeKey 等技术字段干扰运营用户。
    return haystack.includes(normalizedKeyword)
  })

  const commonNodes = filteredNodes.filter(node => commonTypeKeys.has(node.typeKey))

  return {
    commonNodes,
    filteredNodes,
  }
}
