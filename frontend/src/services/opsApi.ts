/**
 * API contract for production runtime status, ops audit events, and emergency controls.
 */
import type { R } from '../types'
import type { UserRole } from './api'
import http from './api'

export interface OpsRuntimeStatus {
  status: string
  role: UserRole
  tenantId?: number | null
  username: string
}

export interface OpsAuditEvent {
  id: string
  tenantId?: number | null
  action: string
  canvasId: number
  operator: string
  role: string
  reason: string
  createdAt: string
}

export interface OpsEmergencyActionReq {
  reason: string
  mode?: string
}

export interface OpsEmergencyActionResult {
  action: string
  canvasId: number
  auditId: string
}

export type OpsEmergencyAction = 'pause' | 'offline' | 'resume' | 'kill' | 'rollback'

export function createOpsApi(client = http) {
  return {
    runtimeStatus: () =>
      client.get<R<OpsRuntimeStatus>, R<OpsRuntimeStatus>>('/ops/runtime/status'),
    auditEvents: (limit = 50) =>
      client.get<R<OpsAuditEvent[]>, R<OpsAuditEvent[]>>('/ops/audit-events', { params: { limit } }),
    emergencyCanvasAction: (canvasId: number, action: OpsEmergencyAction, body: OpsEmergencyActionReq) =>
      client.post<R<OpsEmergencyActionResult>, R<OpsEmergencyActionResult>>(`/ops/canvas/${canvasId}/${action}`, body),
  }
}

export const opsApi = createOpsApi()
