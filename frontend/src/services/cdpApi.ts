import http from './api'
import type { R } from '../types'

export interface CdpUserDetail {
  userId: string
  displayName: string
  phone?: string | null
  email?: string | null
  status: string
  propertiesJson?: string | null
  firstSeenAt?: string | null
  lastSeenAt?: string | null
}

export interface CdpUserTag {
  tagCode: string
  tagName: string
  tagValue?: string | null
  valueType?: string | null
  sourceType?: string | null
  status: string
  effectiveAt?: string | null
  expiresAt?: string | null
  updatedAt?: string | null
}

export interface CdpUserTagHistory {
  tagCode: string
  oldValue?: string | null
  newValue?: string | null
  operation: string
  sourceType?: string | null
  sourceRefId?: string | null
  reason?: string | null
  operator?: string | null
  operatedAt?: string | null
}

export interface CanvasUserRow {
  userId: string
  displayName: string
  executionCount: number
  successCount: number
  failedCount: number
  latestStatus: string
  firstEnteredAt?: string | null
  lastEnteredAt?: string | null
  tags: CdpUserTag[]
}

export interface TagWritePayload {
  tagCode: string
  tagValue?: string | null
  reason?: string | null
  expiresAt?: string | null
  sourceType?: string | null
  sourceRefId?: string | null
  operator?: string | null
  idempotencyKey?: string | null
}

export interface BatchTagPayload {
  operationType: 'BATCH_SET' | 'BATCH_REMOVE'
  tagCode: string
  tagValue?: string | null
  userIds: string[]
  reason?: string | null
  operator?: string | null
}

export const cdpApi = {
  listUsers: (keyword?: string) =>
    http.get<R<CanvasUserRow[]>, R<CanvasUserRow[]>>('/cdp/users', { params: keyword ? { keyword } : undefined }),

  getUser: (userId: string) =>
    http.get<R<CdpUserDetail>, R<CdpUserDetail>>(`/cdp/users/${encodeURIComponent(userId)}`),

  listUserTags: (userId: string) =>
    http.get<R<CdpUserTag[]>, R<CdpUserTag[]>>(`/cdp/users/${encodeURIComponent(userId)}/tags`),

  listUserTagHistory: (userId: string) =>
    http.get<R<CdpUserTagHistory[]>, R<CdpUserTagHistory[]>>(`/cdp/users/${encodeURIComponent(userId)}/tag-history`),

  addUserTag: (userId: string, body: TagWritePayload) =>
    http.post<R<void>, R<void>>(`/cdp/users/${encodeURIComponent(userId)}/tags`, body),

  removeUserTag: (userId: string, tagCode: string) =>
    http.delete<R<void>, R<void>>(`/cdp/users/${encodeURIComponent(userId)}/tags/${encodeURIComponent(tagCode)}`),

  createBatchTagOperation: (body: BatchTagPayload) =>
    http.post<R<any>, R<any>>('/cdp/tag-operations', body),

  getBatchTagOperation: (id: number) =>
    http.get<R<any>, R<any>>(`/cdp/tag-operations/${id}`),

  listCanvasUsers: (canvasId: number) =>
    http.get<R<CanvasUserRow[]>, R<CanvasUserRow[]>>(`/canvas/${canvasId}/users`),

  getCanvasUser: (canvasId: number, userId: string) =>
    http.get<R<CanvasUserRow>, R<CanvasUserRow>>(`/canvas/${canvasId}/users/${encodeURIComponent(userId)}`),

  listCanvasUserExecutions: (canvasId: number, userId: string) =>
    http.get<R<any[]>, R<any[]>>(`/canvas/${canvasId}/users/${encodeURIComponent(userId)}/executions`),
}
