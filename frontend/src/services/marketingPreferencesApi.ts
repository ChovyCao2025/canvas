import http from './api'
import type { R } from '../types'

export interface PreferenceSummary {
  totalChannels: number
  optInCount: number
  optOutCount: number
  activeSuppressionCount: number
  reachableChannelCount: number
}

export interface ConsentRow {
  channel: string
  consentStatus: string
  source?: string | null
  updatedAt?: string | null
}

export interface ChannelRow {
  channel: string
  address?: string | null
  enabled: boolean
  verified: boolean
  reachable: boolean
  metadata?: string | null
  updatedAt?: string | null
}

export interface SuppressionRow {
  id: number
  channel: string
  reason?: string | null
  active: boolean
  state: string
  expiresAt?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface PreferenceReport {
  userId: string
  consents: ConsentRow[]
  channels: ChannelRow[]
  suppressions: SuppressionRow[]
  summary: PreferenceSummary
}

export interface ConsentUpdatePayload {
  consentStatus: string
  source?: string
}

export interface ChannelUpdatePayload {
  address?: string
  enabled?: boolean
  verified?: boolean
  metadata?: string
}

export interface SuppressionCreatePayload {
  channel: string
  reason?: string
  active?: boolean
  expiresAt?: string
}

type MarketingPreferencesHttpClient = Pick<typeof http, 'get' | 'put' | 'post'>

function encodePath(value: string | number) {
  return encodeURIComponent(String(value))
}

function compactBody<T extends object>(body: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(body).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  ) as Partial<T>
}

export function createMarketingPreferencesApi(client: MarketingPreferencesHttpClient = http) {
  return {
    report: (userId: string) =>
      client.get<R<PreferenceReport>, R<PreferenceReport>>(
        `/canvas/marketing-preferences/users/${encodePath(userId)}`,
      ),
    updateConsent: (userId: string, channel: string, payload: ConsentUpdatePayload) =>
      client.put<R<ConsentRow>, R<ConsentRow>>(
        `/canvas/marketing-preferences/users/${encodePath(userId)}/consents/${encodePath(channel)}`,
        compactBody(payload),
      ),
    updateChannel: (userId: string, channel: string, payload: ChannelUpdatePayload) =>
      client.put<R<ChannelRow>, R<ChannelRow>>(
        `/canvas/marketing-preferences/users/${encodePath(userId)}/channels/${encodePath(channel)}`,
        compactBody(payload),
      ),
    addSuppression: (userId: string, payload: SuppressionCreatePayload) =>
      client.post<R<SuppressionRow>, R<SuppressionRow>>(
        `/canvas/marketing-preferences/users/${encodePath(userId)}/suppressions`,
        compactBody(payload),
      ),
    deactivateSuppression: (id: number) =>
      client.put<R<void>, R<void>>(`/canvas/marketing-preferences/suppressions/${encodePath(id)}/deactivate`),
  }
}

export const marketingPreferencesApi = createMarketingPreferencesApi()
