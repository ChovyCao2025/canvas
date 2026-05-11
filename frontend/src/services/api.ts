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
  getTaggerTags: (type: 'realtime' | 'offline') =>
    http.get<R<StubOption[]>, R<StubOption[]>>(`/meta/tagger-tags?type=${type}`),
  getBizLines: () => http.get<R<StubOption[]>, R<StubOption[]>>('/meta/biz-lines'),
}

export default http
