import axios from 'axios'
import type {
  R, PageResult,
  Canvas, CanvasDetail, CanvasVersion,
  NodeTypeRegistry, ContextField, StubOption,
} from '../types'

const http = axios.create({ baseURL: '/' })

// 统一解包 R<T>
http.interceptors.response.use(
  (res) => res.data,
  (err) => Promise.reject(err),
)

// ── 画布管理 ─────────────────────────────────────────────────

export const canvasApi = {
  create: (body: { name: string; description?: string; graphJson?: string; createdBy?: string }) =>
    http.post<R<Canvas>, R<Canvas>>('/canvas', body),

  get: (id: number) =>
    http.get<R<CanvasDetail>, R<CanvasDetail>>(`/canvas/${id}`),

  update: (id: number, body: { name?: string; description?: string; graphJson?: string; updatedBy?: string }) =>
    http.put<R<void>, R<void>>(`/canvas/${id}`, body),

  list: (params: { page?: number; size?: number; status?: number; name?: string }) =>
    http.get<R<PageResult<Canvas>>, R<PageResult<Canvas>>>('/canvas/list', { params }),

  publish: (id: number) =>
    http.post<R<CanvasVersion>, R<CanvasVersion>>(`/canvas/${id}/publish`),

  offline: (id: number) =>
    http.post<R<void>, R<void>>(`/canvas/${id}/offline`),

  getVersions: (id: number) =>
    http.get<R<CanvasVersion[]>, R<CanvasVersion[]>>(`/canvas/${id}/versions`),
}

// ── 元数据 ───────────────────────────────────────────────────

export const metaApi = {
  getNodeTypes: () =>
    http.get<R<NodeTypeRegistry[]>, R<NodeTypeRegistry[]>>('/meta/node-types'),

  getContextFields: () =>
    http.get<R<ContextField[]>, R<ContextField[]>>('/meta/context-fields'),

  getMqTopics: () =>
    http.get<R<StubOption[]>, R<StubOption[]>>('/meta/mq-topics'),

  getCouponTypes: () =>
    http.get<R<StubOption[]>, R<StubOption[]>>('/meta/coupon-types'),

  getReachScenes: () =>
    http.get<R<StubOption[]>, R<StubOption[]>>('/meta/reach-scenes'),
}
