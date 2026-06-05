/**
 * Service contract for churn prediction and smart timing operator endpoints.
 */
import type { R } from '../types'
import http from './api'

export type RiskBand = 'HIGH' | 'MEDIUM' | 'LOW' | string
export type PredictionRunStatus = 'RUNNING' | 'SUCCESS' | 'FAILED' | string

export interface PredictionRunView {
  id: number
  tenantId: number
  modelKey: string
  modelVersion: string
  runDate: string
  status: PredictionRunStatus
  processedCount: number
  skippedCount: number
  failedCount: number
  startedAt?: string | null
  finishedAt?: string | null
  errorMessage?: string | null
}

export interface RiskDistributionItem {
  band: RiskBand
  count: number
}

export interface TopRiskUser {
  userId: string
  churnProbability: number | string
  churnRiskBand: RiskBand
  bestSendHour?: number | null
  confidence?: number | string | null
}

export interface RecomputePredictionPayload {
  force?: boolean
  runDate?: string
  limit?: number
}

export function createAiPredictionApi(client = http) {
  return {
    latestRun: () =>
      client.get<R<PredictionRunView | null>, R<PredictionRunView | null>>('/ai/predictions/latest-run'),
    churnDistribution: () =>
      client.get<R<RiskDistributionItem[]>, R<RiskDistributionItem[]>>('/ai/predictions/churn-distribution'),
    topRiskUsers: (limit = 100) =>
      client.get<R<TopRiskUser[]>, R<TopRiskUser[]>>('/ai/predictions/top-risk-users', { params: { limit } }),
    recompute: (payload: RecomputePredictionPayload = {}) =>
      client.post<R<PredictionRunView>, R<PredictionRunView>>('/ai/predictions/recompute', payload),
  }
}

export const aiPredictionApi = createAiPredictionApi()
