import axios from 'axios'
import type {
  R, PageResult,
  Canvas, CanvasDetail, CanvasVersion,
  NodeTypeRegistry, ContextField, StubOption,
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

  update: (id: number, body: { name?: string; description?: string; graphJson?: string; editVersion?: number }) =>
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

  canary: (id: number, percent: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/canary?percent=${percent}`),

  promoteCanary: (id: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/promote-canary`),

  rollback: (id: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/rollback`),

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

export default http
