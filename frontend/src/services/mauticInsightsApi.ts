import http from './api'
import type { R } from '../types'

export interface AudienceMembershipReport {
  audienceId: number
  audienceName?: string | null
  userId: string
  status: string
  statStatus?: string | null
  estimatedSize?: number | null
  latestRunStatus?: string | null
  evidence: string[]
}

export interface JourneyStep {
  nodeId?: string | null
  nodeName?: string | null
  nodeType?: string | null
  status: number
  statusLabel: string
  reason: string
  errorMessage?: string | null
  durationMs?: number | null
}

export interface JourneyPathReport {
  executionId: string
  steps: JourneyStep[]
  successCount: number
  failedCount: number
  skippedCount: number
}

export interface ChannelCandidate {
  channel: string
  state: string
  reason: string
}

export interface ChannelPreferenceReport {
  userId: string
  recommendedChannel?: string | null
  fallbackChannel?: string | null
  channels: ChannelCandidate[]
}

export interface SuppressionRecord {
  id: number
  channel: string
  reason?: string | null
  state: string
  createdAt?: string | null
  expiresAt?: string | null
}

export interface SuppressionTimeline {
  userId: string
  records: SuppressionRecord[]
}

export interface HealthCheck {
  checkKey: string
  passed: boolean
  message: string
}

export interface PublishHealthReport {
  canvasId: number
  canvasName?: string | null
  score: number
  checks: HealthCheck[]
}

export interface FrequencyTemplate {
  templateKey: string
  scope: string
  maxCount: number
  windowSeconds: number
  description: string
}

export interface AudienceMembershipParams {
  audienceId: number
  userId: string
}

export interface ChannelPreferenceParams {
  userId: string
  preferredChannel?: string
}

type MauticInsightsHttpClient = Pick<typeof http, 'get'>

function compactParams(params: object) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
}

export function createMauticInsightsApi(client: MauticInsightsHttpClient = http) {
  return {
    explainAudienceMembership: (params: AudienceMembershipParams) =>
      client.get<R<AudienceMembershipReport>, R<AudienceMembershipReport>>(
        '/canvas/mautic-insights/audience-membership',
        { params: compactParams(params) },
      ),
    explainJourneyPath: (executionId: string) =>
      client.get<R<JourneyPathReport>, R<JourneyPathReport>>('/canvas/mautic-insights/journey-path', {
        params: { executionId },
      }),
    resolveChannelPreference: (params: ChannelPreferenceParams) =>
      client.get<R<ChannelPreferenceReport>, R<ChannelPreferenceReport>>(
        '/canvas/mautic-insights/channel-preference',
        { params: compactParams(params) },
      ),
    suppressionTimeline: (userId: string) =>
      client.get<R<SuppressionTimeline>, R<SuppressionTimeline>>('/canvas/mautic-insights/suppression-timeline', {
        params: { userId },
      }),
    publishHealth: (canvasId: number) =>
      client.get<R<PublishHealthReport>, R<PublishHealthReport>>('/canvas/mautic-insights/publish-health', {
        params: { canvasId },
      }),
    frequencyTemplates: () =>
      client.get<R<FrequencyTemplate[]>, R<FrequencyTemplate[]>>('/canvas/mautic-insights/frequency-templates'),
  }
}

export const mauticInsightsApi = createMauticInsightsApi()
