/**
 * 服务职责：统一后端 API 客户端和核心业务 API 聚合层。
 *
 * 维护说明：axios 拦截器在这里注入 token、解包 R<T> 响应，并在 401 时清理登录态。
 */
import axios from 'axios'
import type {
  R, PageResult,
  Canvas, CanvasDetail, CanvasVersion,
  NodeTypeRegistry, ContextField, StubOption, AbExperimentGroup,
  IdentityType, TagValueDefinition, TagImportBatch, TagImportError,
  TagImportResult, TagImportRow, TagImportSource,
} from '../types'
import type { HomeOverview } from '../pages/home/homeOverview'

/**
 * 统一 HTTP 客户端。
 * 约定：后端统一返回 R<T> 包装，调用方直接拿到解包后的对象。
 */
const http = axios.create({ baseURL: '/' })

// 请求拦截：自动带 JWT token
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('canvas_token')
  // 仅在 token 存在时注入 Authorization，避免污染匿名请求
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// 响应拦截：解包 R<T>；401 跳转登录
http.interceptors.response.use(
  // 后端返回结构统一为 { code, message, data }
  // 这里直接返回 res.data，业务侧不需要每次再写一次 .data
  (res) => res.data,
  (err) => {
    if (err.response?.status === 401) {
      // token 失效后强制回到登录页，避免页面在无权限状态下继续操作
      localStorage.removeItem('canvas_token')
      localStorage.removeItem('canvas_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  },
)

// ── 认证 ─────────────────────────────────────────────────────

export type UserRole = 'ADMIN' | 'SUPER_ADMIN' | 'TENANT_ADMIN' | 'OPERATOR'

export interface LoginResp {
  /** JWT 令牌。 */
  token: string

  /** 用户 ID。 */
  userId: number

  /** 租户 ID；legacy ADMIN rollout 期间可能为空。 */
  tenantId: number | null

  /** 用户名。 */
  username: string

  /** 展示名。 */
  displayName: string

  /** 角色。 */
  role: UserRole
}

/** 后台系统用户列表项。 */
export interface SysUser {
  /** 用户 ID。 */
  id: number

  /** 租户 ID。 */
  tenantId: number | null

  /** 用户名。 */
  username: string

  /** 展示名。 */
  displayName: string

  /** 角色。 */
  role: UserRole

  /** 启用状态：1 启用，0 禁用。 */
  enabled: 0 | 1
}

/** 管理员创建用户请求体。 */
interface AdminCreateUserReq {
  /** 登录用户名。 */
  username: string

  /** 初始密码。 */
  password: string

  /** 展示名称。 */
  displayName: string

  /** 用户角色。 */
  role: string

  /** 所属租户。 */
  tenantId: number
}

/** 管理员更新用户请求体。 */
interface AdminUpdateUserReq {
  /** 展示名称。 */
  displayName?: string

  /** 重置密码。 */
  password?: string

  /** 调整角色。 */
  role?: string
}

/** 登录、登出和当前用户接口集合。 */
export const authApi = {
  login: (username: string, password: string) =>
    http.post<R<LoginResp>, R<LoginResp>>('/auth/login', { username, password }),
  logout: () => http.post<R<void>, R<void>>('/auth/logout'),
  me: () => http.get<R<LoginResp>, R<LoginResp>>('/auth/me'),
}

/** 管理员用户管理接口集合。 */
export const adminApi = {
  listUsers: () => http.get<R<SysUser[]>, R<SysUser[]>>('/admin/users'),
  createUser: (body: AdminCreateUserReq) =>
    http.post<R<SysUser>, R<SysUser>>('/admin/users', body),
  updateUser: (id: number, body: AdminUpdateUserReq) =>
    http.put<R<void>, R<void>>(`/admin/users/${id}`, body),
  disableUser: (id: number) =>
    http.put<R<void>, R<void>>(`/admin/users/${id}/disable`),
}

export interface Tenant {
  id: number
  tenantKey: string
  name: string
  status: 'ACTIVE' | 'DISABLED' | string
  planCode?: string
  quotaJson?: string
  remark?: string
  createdBy?: string
  createdAt?: string
  updatedBy?: string
  updatedAt?: string
}

export interface TenantUsage {
  tenantId: number
  canvasCount: number
  publishedCanvasCount: number
  executionCount: number
  failedExecutionCount: number
  dlqCount: number
}

interface TenantCreateReq {
  name: string
  tenantKey: string
  planCode?: string
  quotaJson?: string
}

export const tenantApi = {
  list: () => http.get<R<Tenant[]>, R<Tenant[]>>('/admin/tenants'),
  create: (body: TenantCreateReq) =>
    http.post<R<Tenant>, R<Tenant>>('/admin/tenants', body),
  disable: (id: number) =>
    http.put<R<void>, R<void>>(`/admin/tenants/${id}/disable`),
  activate: (id: number) =>
    http.put<R<void>, R<void>>(`/admin/tenants/${id}/activate`),
  usage: (id: number) =>
    http.get<R<TenantUsage>, R<TenantUsage>>(`/admin/tenants/${id}/usage`),
}

// ── 画布管理 ─────────────────────────────────────────────────

/** 创建画布请求体。 */
interface CanvasCreateReq {
  /** 画布名称。 */
  name: string

  /** 画布描述。 */
  description?: string

  /** 初始化图结构。 */
  graphJson?: string
}

/** 更新画布请求体（草稿保存 + 设置更新共用）。 */
interface CanvasUpdateReq {
  /** 画布名称。 */
  name?: string

  /** 画布描述。 */
  description?: string

  /** 图结构 JSON。 */
  graphJson?: string

  /** 乐观锁版本。 */
  editVersion?: number

  /** 触发类型。 */
  triggerType?: string

  /** cron 表达式（非定时时传 null）。 */
  cronExpression?: string | null

  /** 生效开始时间。 */
  validStart?: string | null

  /** 生效结束时间。 */
  validEnd?: string | null

  /** 总执行次数上限。 */
  maxTotalExecutions?: number | null

  /** 用户每日上限。 */
  perUserDailyLimit?: number | null

  /** 用户总上限。 */
  perUserTotalLimit?: number | null

  /** 冷却秒数。 */
  cooldownSeconds?: number | null
}

/** 画布列表查询参数。 */
interface CanvasListQuery {
  /** 页码。 */
  page?: number

  /** 每页数量。 */
  size?: number

  /** 状态筛选。 */
  status?: number

  /** 名称模糊查询。 */
  name?: string
}

/** 画布草稿、发布、版本、灰度和执行调试接口集合。 */
export const canvasApi = {
  create: (body: CanvasCreateReq) =>
    http.post<R<Canvas>, R<Canvas>>('/canvas', body),

  get: (id: number) =>
    http.get<R<CanvasDetail>, R<CanvasDetail>>(`/canvas/${id}`),

  update: (id: number, body: CanvasUpdateReq) =>
    http.put<R<void>, R<void>>(`/canvas/${id}`, body),

  list: (params: CanvasListQuery) =>
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

  // dry-run 允许前端带 graphJson，便于“未保存草稿”场景下做即时调试
  dryRun: (id: number, userId: string, payload: Record<string, unknown>, graphJson?: string) =>
    http.post<R<Record<string, unknown>>, R<Record<string, unknown>>>(
      `/canvas/execute/dry-run/${id}`,
      { userId, inputParams: payload, graphJson },
    ),
}

/** 首页运营概览接口。 */
export const homeApi = {
  overview: (days = 7) =>
    http.get<R<HomeOverview>, R<HomeOverview>>('/canvas/home/overview', { params: { days } }),
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
    /** 画布中事件触发节点引用的 eventCode 列表。 */
    eventCodes?: string[]

    /** 画布中 API 节点引用的 apiKey 列表。 */
    apiKeys?: string[]

    /** API 输出前缀列表。 */
    outputPrefixes?: string[]
  }) =>
    http.get<R<ContextField[]>, R<ContextField[]>>('/meta/canvas-context-fields', {
      params,
      // axios 默认数组格式是 key[]=v，后端 Spring Controller 这里按重复 key 接收
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
  getAbExperimentGroups: (key: string) =>
    http.get<R<StubOption[]>, R<StubOption[]>>(`/meta/ab-experiments/${key}/groups`),
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
  groups: (id: number, includeDisabled = false) =>
    http.get<R<AbExperimentGroup[]>, R<AbExperimentGroup[]>>(`/canvas/ab-experiments/${id}/groups`, {
      params: { includeDisabled },
    }),
  createGroup: (id: number, body: Partial<AbExperimentGroup>) =>
    http.post<R<AbExperimentGroup>, R<AbExperimentGroup>>(`/canvas/ab-experiments/${id}/groups`, body),
  updateGroup: (id: number, groupId: number, body: Partial<AbExperimentGroup>) =>
    http.put<R<void>, R<void>>(`/canvas/ab-experiments/${id}/groups/${groupId}`, body),
  deleteGroup: (id: number, groupId: number) =>
    http.delete<R<void>, R<void>>(`/canvas/ab-experiments/${id}/groups/${groupId}`),
}

/** 标签定义管理接口集合。 */
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

/** 身份类型管理接口集合。 */
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

/** 标签值管理接口集合。 */
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

/** 标签导入批次、来源和错误明细接口集合。 */
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
