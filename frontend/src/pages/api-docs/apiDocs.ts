export type ApiDocMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'
export type ApiDocAuth = 'none' | 'bearer'

export interface ApiDocParam {
  name: string
  in: 'path' | 'query' | 'body'
  required?: boolean
  desc: string
}

export interface ApiDocEndpoint {
  id: string
  title: string
  method: ApiDocMethod
  path: string
  category: string
  summary: string
  auth: ApiDocAuth
  internal?: boolean
  params?: ApiDocParam[]
  requestExample?: unknown
  responseExample?: unknown
}

export interface ApiDocCategory {
  key: string
  title: string
  description: string
}

export interface ApiDocCategorySummary extends ApiDocCategory {
  count: number
}

export interface ApiDocFilter {
  showInternal: boolean
  keyword?: string
  category?: string
}

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

export const endpointId = (method: ApiDocMethod, path: string) =>
  `${method.toLowerCase()}-${path.replace(/[{}]/g, '').replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '')}`

const categoriesByKey = new Map(API_DOC_CATEGORIES.map(category => [category.key, category]))

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

export function formatJsonExample(value: unknown): string {
  return JSON.stringify(value, null, 2) ?? String(value)
}
