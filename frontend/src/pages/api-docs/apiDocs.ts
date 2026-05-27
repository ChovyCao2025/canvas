/**
 * 页面职责：API 文档共享类型、分类和过滤工具。
 *
 * 维护说明：页面数据来自后端 /v3/api-docs，前端只保留展示所需的稳定结构。
 */
export type ApiDocMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

/** API 文档鉴权方式枚举。 */
export type ApiDocAuth = 'none' | 'bearer'

/** 文档中的单个参数描述。 */
export interface ApiDocParam {
  /** 参数名。 */
  name: string

  /** 参数位置：路径、查询串或请求体。 */
  in: 'path' | 'query' | 'body'

  /** 是否必填。 */
  required?: boolean

  /** 参数说明。 */
  desc: string
}

/** 单个 API 文档条目，供文档页列表和详情面板渲染。 */
export interface ApiDocEndpoint {
  /** 前端生成的稳定 ID，用作锚点、复制和列表 key。 */
  id: string

  /** 接口标题。 */
  title: string

  /** HTTP 方法。 */
  method: ApiDocMethod

  /** 接口路径，路径参数用 {id} 这类形式标记。 */
  path: string

  /** 所属分类 key。 */
  category: string

  /** 一句话摘要。 */
  summary: string

  /** 鉴权方式。 */
  auth: ApiDocAuth

  /** 是否仅面向内部后台使用。 */
  internal?: boolean

  /** 参数列表。 */
  params?: ApiDocParam[]

  /** 请求示例。 */
  requestExample?: unknown

  /** 响应示例。 */
  responseExample?: unknown
}

/** API 文档分类定义。 */
export interface ApiDocCategory {
  /** 分类 key。 */
  key: string

  /** 分类标题。 */
  title: string

  /** 分类说明。 */
  description: string
}

/** 分类摘要，额外包含当前筛选结果下的接口数量。 */
export interface ApiDocCategorySummary extends ApiDocCategory {
  count: number
}

/** 文档页筛选条件。 */
export interface ApiDocFilter {
  /** 是否展示内部接口。 */
  showInternal: boolean

  /** 搜索关键词，匹配标题、摘要、路径、方法和分类。 */
  keyword?: string

  /** 分类 key。 */
  category?: string
}

/** 文档分类顺序和文案，页面侧按此顺序展示筛选导航。 */
export const API_DOC_CATEGORIES: ApiDocCategory[] = [
  { key: 'auth', title: '认证', description: '登录、登出和当前用户信息' },
  { key: 'external-trigger', title: '外部触发', description: '业务系统触发画布执行的主要入口' },
  { key: 'approval', title: '审批回调', description: '人工审批节点的通过和拒绝操作' },
  { key: 'canvas', title: '画布管理', description: '画布草稿、发布、版本和灰度管控' },
  { key: 'configuration', title: '配置管理', description: 'API、事件、MQ、标签、人群和实验配置' },
  { key: 'metadata', title: '元数据', description: '节点、上下文字段和下拉选项' },
  { key: 'observability', title: '运行观测', description: '执行记录、轨迹、统计和重放' },
  { key: 'operations', title: '运维与模板', description: '缓存、模板和发布审批工具' },
  { key: 'users', title: '用户管理', description: '后台用户管理接口' },
]

/** 根据方法和路径生成稳定文档 ID，避免人工维护重复 key。 */
export const endpointId = (method: ApiDocMethod, path: string) =>
  `${method.toLowerCase()}-${path.replace(/[{}]/g, '').replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '')}`

const categoriesByKey = new Map(API_DOC_CATEGORIES.map(category => [category.key, category]))

/** 按内部接口开关、分类和关键词过滤 API 文档条目。 */
export function filterApiDocEndpoints(endpoints: ApiDocEndpoint[], filter: ApiDocFilter): ApiDocEndpoint[] {
  const keyword = filter.keyword?.trim().toLowerCase()

  return endpoints.filter(endpoint => {
    if (!filter.showInternal && endpoint.internal) {
      return false
    }

    if (filter.category && endpoint.category !== filter.category) {
      return false
    }

    if (!keyword) {
      return true
    }

    const category = categoriesByKey.get(endpoint.category)
    return [
      endpoint.title,
      endpoint.path,
      endpoint.method,
      endpoint.summary,
      category?.title,
      category?.description,
    ].some(value => value?.toLowerCase().includes(keyword))
  })
}

/** 汇总每个可见分类下的接口数量。 */
export function getApiDocCategorySummaries(endpoints: ApiDocEndpoint[]): ApiDocCategorySummary[] {
  const counts = endpoints.reduce<Map<string, number>>((nextCounts, endpoint) => {
    nextCounts.set(endpoint.category, (nextCounts.get(endpoint.category) ?? 0) + 1)
    return nextCounts
  }, new Map())

  return API_DOC_CATEGORIES.map(category => ({
    ...category,
    count: counts.get(category.key) ?? 0,
  })).filter(summary => summary.count > 0)
}

/** 格式化示例 JSON，兼容 undefined 这类 JSON.stringify 返回 undefined 的值。 */
export function formatJsonExample(value: unknown): string {
  return JSON.stringify(value, null, 2) ?? String(value)
}
