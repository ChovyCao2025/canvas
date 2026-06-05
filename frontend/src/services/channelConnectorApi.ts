/**
 * Service contract for channel connector operator endpoints.
 */
import type { R } from '../types'
import http from './api'

export interface ConnectorRow {
  id: number
  connectorKey: string
  channel: string
  provider: string
  mode: string
  healthStatus: string
  healthMessage?: string | null
}

export interface LimitRow {
  channel: string
  provider: string
  operation: string
  perSecondLimit: number
  dailyLimit?: number | null
  failClosed: boolean
  updatedAt?: string | null
}

export interface FallbackPolicyPayload {
  channel: string
  provider: string
  fallbackChannel: string
  fallbackProvider: string
}

export interface ValidationResult {
  valid: boolean
  message: string
}

export interface FallbackDecisionRow {
  originalChannel: string
  originalProvider?: string | null
  finalChannel?: string | null
  finalProvider?: string | null
  decisionReason: string
  createdAt: string
}

export interface DedupeRecordRow {
  dedupeGroup: string
  contentHash: string
  channel: string
  userId: string
  expiresAt: string
}

export function createChannelConnectorApi(client = http) {
  return {
    list: () => client.get<R<ConnectorRow[]>, R<ConnectorRow[]>>('/channels/connectors'),
    limits: () => client.get<R<LimitRow[]>, R<LimitRow[]>>('/channels/connectors/limits'),
    updateMode: (id: number, mode: string, reason = '') =>
      client.post<R<void>, R<void>>(`/channels/connectors/${id}/mode`, { mode, reason }),
    testHealth: (id: number) =>
      client.post<R<{ status: string; message?: string | null }>, R<{ status: string; message?: string | null }>>(
        `/channels/connectors/${id}/health-test`,
      ),
    validateFallback: (payload: FallbackPolicyPayload) =>
      client.post<R<ValidationResult>, R<ValidationResult>>('/channels/connectors/fallback/validate', payload),
    decisions: () =>
      client.get<R<FallbackDecisionRow[]>, R<FallbackDecisionRow[]>>('/channels/connectors/fallback/decisions'),
    dedupeRecords: () =>
      client.get<R<DedupeRecordRow[]>, R<DedupeRecordRow[]>>('/channels/connectors/dedupe-records'),
  }
}

export const channelConnectorApi = createChannelConnectorApi()
