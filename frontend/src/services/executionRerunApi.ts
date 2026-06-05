/**
 * Service contract for test users and single-user execution reruns.
 */
import type { R } from '../types'
import http from './api'

export type JsonObject = Record<string, unknown>

export type RerunMode = 'DRY_RUN' | 'SKIP_SIDE_EFFECTS' | 'ADMIN_REPLAY'

export interface TestUserSet {
  id: number
  tenantId?: number
  name: string
  description?: string | null
  createdBy?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface TestUser {
  id: number
  tenantId?: number
  setId: number
  userId: string
  displayName?: string | null
  profileJson?: string | null
  inputParams?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface TestUserPreview {
  id: number
  userId: string
  displayName?: string | null
  profile: JsonObject
  inputParams: JsonObject
  context: JsonObject
}

export interface TestUserSetPayload {
  name: string
  description?: string
}

export interface TestUserPayload {
  userId: string
  displayName?: string
  profile?: JsonObject
  inputParams?: JsonObject
}

export interface RerunPayload {
  userId?: string
  testUserId?: number
  originalExecutionId?: string
  mode?: RerunMode
  reason: string
  inputParams?: JsonObject
  graphJson?: string
}

export interface RerunResult {
  auditId: number
  mode: RerunMode
  status: string
  result: JsonObject
}

export interface RerunAudit {
  id: number
  tenantId?: number
  canvasId: number
  userId: string
  testUserId?: number | null
  originalExecutionId?: string | null
  mode: RerunMode | string
  reason: string
  operator?: string | null
  status: string
  inputParams?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export function createExecutionRerunApi(client = http) {
  return {
    listSets: () => client.get<R<TestUserSet[]>, R<TestUserSet[]>>('/test-users/sets'),
    createSet: (payload: TestUserSetPayload) =>
      client.post<R<TestUserSet>, R<TestUserSet>>('/test-users/sets', payload),
    listUsers: (setId: number) =>
      client.get<R<TestUser[]>, R<TestUser[]>>(`/test-users/sets/${setId}/users`),
    createUser: (setId: number, payload: TestUserPayload) =>
      client.post<R<TestUser>, R<TestUser>>(`/test-users/sets/${setId}/users`, payload),
    userDetail: (id: number) =>
      client.get<R<TestUser>, R<TestUser>>(`/test-users/${id}`),
    previewUser: (id: number) =>
      client.get<R<TestUserPreview>, R<TestUserPreview>>(`/test-users/${id}/preview`),
    rerunCanvas: (canvasId: number, payload: RerunPayload) =>
      client.post<R<RerunResult>, R<RerunResult>>(`/execution-reruns/canvas/${canvasId}`, payload),
    audit: (id: number) =>
      client.get<R<RerunAudit>, R<RerunAudit>>(`/execution-reruns/${id}`),
    listAudits: (canvasId?: number) =>
      client.get<R<RerunAudit[]>, R<RerunAudit[]>>('/execution-reruns', {
        params: canvasId ? { canvasId } : undefined,
      }),
  }
}

export const executionRerunApi = createExecutionRerunApi()
