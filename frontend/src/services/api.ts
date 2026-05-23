import axios from 'axios'
import type {
  R, PageResult,
  Canvas, CanvasDetail, CanvasVersion,
  NodeTypeRegistry, ContextField, StubOption,
  IdentityType, TagValueDefinition, TagImportBatch, TagImportError,
  TagImportResult, TagImportRow, TagImportSource,
} from '../types'

const http = axios.create({ baseURL: '/' })

// 请求拦截：自动带 JWT token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('canvas_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：解包 R<T>；401 跳转登录
http.interceptors.response.use(
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('canvas_token')
      localStorage.removeItem('canvas_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

// ── 认证 ─────────────────────────────────────────────────────

export interface LoginResp {
  token: string
  userId: number
  username: string
  displayName: string
  role: 'ADMIN' | 'OPERATOR'
}

export interface SysUser {
  id: number
  username: string
  displayName: string
  role: 'ADMIN' | 'OPERATOR'
  enabled: 0 | 1
}

export const authApi = {
  login: (username: string, password: string) =>
    http.post<R<LoginResp>, R<LoginResp>>('/auth/login', { username, password }),
  logout: () => http.post<R<void>, R<void>>('/auth/logout'),
  me: () => http.get<R<LoginResp>, R<LoginResp>>('/auth/me'),
}

export const adminApi = {
  listUsers: () => http.get<R<SysUser[]>, R<SysUser[]>>('/admin/users'),
  createUser: (body: { username: string; password: string; displayName: string; role: string }) =>
    http.post<R<SysUser>, R<SysUser>>('/admin/users', body),
  updateUser: (id: number, body: { displayName?: string; password?: string; role?: string }) =>
    http.put<R<void>, R<void>>(`/admin/users/${id}`, body),
  disableUser: (id: number) =>
    http.put<R<void>, R<void>>(`/admin/users/${id}/disable`),
}

// ── 画布管理 ─────────────────────────────────────────────────

export const canvasApi = {
  create: (body: { name: string; description?: string; graphJson?: string }) =>
    http.post<R<Canvas>, R<Canvas>>('/canvas', body),

  get: (id: number) =>
    http.get<R<CanvasDetail>, R<CanvasDetail>>(`/canvas/${id}`),

  update: (id: number, body: {
    name?: string
    description?: string
    graphJson?: string
    editVersion?: number
    triggerType?: string
    cronExpression?: string | null
    validStart?: string | null
    validEnd?: string | null
    maxTotalExecutions?: number | null
    perUserDailyLimit?: number | null
    perUserTotalLimit?: number | null
    cooldownSeconds?: number | null
  }) =>
    http.put<R<void>, R<void>>(`/canvas/${id}`, body),

  list: (params: { page?: number; size?: number; status?: number; name?: string }) =>
    http.get<R<PageResult<Canvas>>, R<PageResult<Canvas>>>('/canvas/list', { params }),

  publish: (id: number) =>
    http.post<R<CanvasVersion>, R<CanvasVersion>>(`/canvas/${id}/publish`),

  offline: (id: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/offline`),

  getVersions: (id: number) =>
    http.get<R<CanvasVersion[]>, R<CanvasVersion[]>>(`/canvas/${id}/versions`),

  clone: (id: number) =>
    http.post<R<Canvas>, R<Canvas>>(`/canvas/${id}/clone`),

  kill: (id: number, mode = 'GRACEFUL') =>
    http.post<R<void>, R<void>>(`/canvas/${id}/kill?mode=${mode}`),

  archive: (id: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/archive`),

  canary: (id: number, percent: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/canary?percent=${percent}`),

  promoteCanary: (id: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/promote-canary`),

  rollbackCanary: (id: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/rollback-canary`),

  rollback: (id: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/rollback`),

  revert: (id: number, versionId: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/revert/${versionId}`),

  triggerDirect: (id: number, userId: string, payload: Record<string, unknown>) =>
    http.post<R<Record<string, unknown>>, R<Record<string, unknown>>>(
      `/canvas/execute/direct/${id}`,
      { userId, inputParams: payload },
    ),

  dryRun: (id: number, userId: string, payload: Record<string, unknown>, graphJson?: string) =>
    http.post<R<Record<string, unknown>>, R<Record<string, unknown>>>(
      `/canvas/execute/dry-run/${id}`,
      { userId, inputParams: payload, graphJson },
    ),
}

// ── 元数据 ───────────────────────────────────────────────────

export const metaApi = {
  getNodeTypes: () =>
    http.get<R<NodeTypeRegistry[]>, R<NodeTypeRegistry[]>>('/meta/node-types'),

  getNodeTypeSchema: (typeKey: string) =>
    http.get<R<NodeTypeRegistry>, R<NodeTypeRegistry>>(`/meta/node-types/${typeKey}/schema`),

  getContextFields: () =>
    http.get<R<ContextField[]>, R<ContextField[]>>('/meta/context-fields'),

  /** 根据画布中的 EVENT_TRIGGER / API_CALL 节点动态推导可用上下文字段 */
  getCanvasContextFields: (params: {
    eventCodes?: string[]
    apiKeys?: string[]
    outputPrefixes?: string[]
  }) =>
    http.get<R<ContextField[]>, R<ContextField[]>>('/meta/canvas-context-fields', {
      params,
      // axios 默认把数组序列化为 key[]=v，Spring 需要 key=v1&key=v2
      paramsSerializer: (p) => {
        const parts: string[] = []
        for (const [k, v] of Object.entries(p)) {
          if (Array.isArray(v)) v.forEach(s => parts.push(`${k}=${encodeURIComponent(s)}`))
          else if (v != null) parts.push(`${k}=${encodeURIComponent(v as string)}`)
        }
        return parts.join('&')
      },
    }),

  getMqTopics: () => http.get<R<StubOption[]>, R<StubOption[]>>('/meta/mq-topics'),
  getCouponTypes: () => http.get<R<StubOption[]>, R<StubOption[]>>('/meta/coupon-types'),
  getReachScenes: () => http.get<R<StubOption[]>, R<StubOption[]>>('/meta/reach-scenes'),
  getAbExperiments: () => http.get<R<StubOption[]>, R<StubOption[]>>('/meta/ab-experiments'),
  getApiDefinitions: () => http.get<R<StubOption[]>, R<StubOption[]>>('/meta/api-definitions'),
  getTaggerTags: (type: 'realtime' | 'offline') =>
    http.get<R<StubOption[]>, R<StubOption[]>>(`/meta/tagger-tags?type=${type}`),
  getBizLines: () => http.get<R<StubOption[]>, R<StubOption[]>>('/meta/biz-lines'),
}

// ── API 定义管理 ─────────────────────────────────────────────

export const apiDefinitionApi = {
  list: (params?: { page?: number; size?: number; enabled?: number }) =>
    http.get<R<PageResult<any>>, R<PageResult<any>>>('/canvas/api-definitions', { params }),
  create: (body: any) =>
    http.post<R<any>, R<any>>('/canvas/api-definitions', body),
  update: (id: number, body: any) =>
    http.put<R<void>, R<void>>(`/canvas/api-definitions/${id}`, body),
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/api-definitions/${id}`),
}

// ── AB 实验管理 ──────────────────────────────────────────────

export const abExperimentApi = {
  list: (params?: { page?: number; size?: number; enabled?: number }) =>
    http.get<R<PageResult<any>>, R<PageResult<any>>>('/canvas/ab-experiments', { params }),
  create: (body: any) =>
    http.post<R<any>, R<any>>('/canvas/ab-experiments', body),
  update: (id: number, body: any) =>
    http.put<R<void>, R<void>>(`/canvas/ab-experiments/${id}`, body),
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/ab-experiments/${id}`),
}

export const tagDefinitionApi = {
  list: (params?: { page?: number; size?: number; tagType?: string; enabled?: number }) =>
    http.get<R<PageResult<any>>, R<PageResult<any>>>('/canvas/tag-definitions', { params }),
  create: (body: any) =>
    http.post<R<any>, R<any>>('/canvas/tag-definitions', body),
  update: (id: number, body: any) =>
    http.put<R<void>, R<void>>(`/canvas/tag-definitions/${id}`, body),
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/tag-definitions/${id}`),
}

export const identityTypeApi = {
  list: (params?: { enabled?: number; allowImport?: number }) =>
    http.get<R<PageResult<IdentityType>>, R<PageResult<IdentityType>>>('/canvas/identity-types', { params }),
  create: (body: Partial<IdentityType>) =>
    http.post<R<IdentityType>, R<IdentityType>>('/canvas/identity-types', body),
  update: (id: number, body: Partial<IdentityType>) =>
    http.put<R<void>, R<void>>(`/canvas/identity-types/${id}`, body),
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/identity-types/${id}`),
}

export const tagValueApi = {
  list: (tagCode: string, params?: { enabled?: number }) =>
    http.get<R<TagValueDefinition[]>, R<TagValueDefinition[]>>(`/canvas/tag-definitions/${tagCode}/values`, { params }),
  create: (tagCode: string, body: Partial<TagValueDefinition>) =>
    http.post<R<TagValueDefinition>, R<TagValueDefinition>>(`/canvas/tag-definitions/${tagCode}/values`, body),
  update: (id: number, body: Partial<TagValueDefinition>) =>
    http.put<R<void>, R<void>>(`/canvas/tag-definitions/values/${id}`, body),
  delete: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/tag-definitions/values/${id}`),
}

export const tagImportApi = {
  apiPush: (rows: TagImportRow[]) =>
    http.post<R<TagImportResult>, R<TagImportResult>>('/canvas/tag-imports/api-push', { rows }),
  batches: () =>
    http.get<R<TagImportBatch[]>, R<TagImportBatch[]>>('/canvas/tag-imports/batches'),
  errors: (batchId: number) =>
    http.get<R<TagImportError[]>, R<TagImportError[]>>(`/canvas/tag-imports/batches/${batchId}/errors`),
  excelTemplateUrl: '/canvas/tag-imports/excel-template',
  uploadExcel: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.post<R<TagImportResult>, R<TagImportResult>>('/canvas/tag-imports/excel', form)
  },
  sources: () =>
    http.get<R<PageResult<TagImportSource>>, R<PageResult<TagImportSource>>>('/canvas/tag-import-sources'),
  createSource: (body: Partial<TagImportSource>) =>
    http.post<R<TagImportSource>, R<TagImportSource>>('/canvas/tag-import-sources', body),
  updateSource: (id: number, body: Partial<TagImportSource>) =>
    http.put<R<void>, R<void>>(`/canvas/tag-import-sources/${id}`, body),
  deleteSource: (id: number) =>
    http.delete<R<void>, R<void>>(`/canvas/tag-import-sources/${id}`),
  runSource: (id: number) =>
    http.post<R<TagImportResult>, R<TagImportResult>>(`/canvas/tag-import-sources/${id}/run`),
}

export default http
