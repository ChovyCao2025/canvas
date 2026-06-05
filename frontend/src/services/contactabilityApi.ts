import http from './api'
import type { R } from '../types'

export interface ContactabilityCheck {
  checkKey: string
  allowed: boolean
  reasonCode?: string | null
  reasonMessage?: string | null
}

export interface ContactabilityReport {
  userId: string
  channel: string
  allowed: boolean
  checks: ContactabilityCheck[]
}

export interface ContactabilityExplainParams {
  userId: string
  channel: string
  requireExplicitConsent?: boolean
  quietStart?: string
  quietEnd?: string
  quietTimezone?: string
  canvasId?: number
  nodeId?: string
  frequencyScope?: string
  frequencyMax?: number
  frequencyWindowSeconds?: number
}

type ContactabilityHttpClient = Pick<typeof http, 'get'>

function compactParams(params: ContactabilityExplainParams) {
  return Object.fromEntries(
    Object.entries(params).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  )
}

export function createContactabilityApi(client: ContactabilityHttpClient = http) {
  return {
    explain: (params: ContactabilityExplainParams) =>
      client.get<R<ContactabilityReport>, R<ContactabilityReport>>('/canvas/contactability/explain', {
        params: compactParams(params),
      }),
  }
}

export const contactabilityApi = createContactabilityApi()
