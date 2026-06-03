/**
 * 组件职责：节点库视图模型工具，将后端节点类型列表整理为分类、摘要和过滤结果。
 *
 * 维护说明：这些纯函数由组件和测试共用，避免 UI 状态与排序规则混杂。
 */
import type { NodeTypeRegistry } from '../../types'

/** 默认展示在“常用节点”区域的节点类型，帮助新用户快速找到高频能力。 */
export const DEFAULT_COMMON_NODE_TYPES = [
  'DIRECT_CALL',
  'IF_CONDITION',
  'SPLIT',
  'SEND_MESSAGE',
  'API_CALL',
]

/** “全部”分类的固定文案，过滤时作为特殊分类处理。 */
const ALL_CATEGORIES_LABEL = '全部'

/** 业务上更符合使用频率的分类顺序；未列入分类会按中文名称排在后面。 */
const CATEGORY_ORDER = [
  '基础控制',
  '入口触发',
  '条件与分流',
  '等待与汇聚',
  '动作执行',
  '消息触达',
  '数据与权益',
  '流程复用',
]

/** 后端没有维护 description 时的兜底摘要，避免节点库出现空说明。 */
const SUMMARY_FALLBACK: Record<string, string> = {
  DIRECT_CALL: '外部系统同步调用并进入旅程',
  EVENT_TRIGGER: '业务事件上报后进入旅程',
  MQ_TRIGGER: '消费业务消息后进入旅程',
  SCHEDULED_TRIGGER: '按时间计划触发旅程',
  IF_CONDITION: '按规则判断后选择路径',
  SPLIT: '按比例或随机策略分流',
  WAIT: '等待时间或事件后继续',
  HUB: '等待多条路径汇合后继续',
  AGGREGATE: '等待全量上游后汇总判断',
  THRESHOLD: '达到阈值后提前触发',
  API_CALL: '请求外部服务并拿回结果',
  SEND_MQ: '发送一条业务消息给下游系统',
  GROOVY: '处理复杂逻辑或字段加工',
  SEND_MESSAGE: '通过配置渠道向用户发送消息',
  TAGGER: '读取或判断用户标签/人群',
  COMMIT_ACTION: '提交权益或关键副作用动作',
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
