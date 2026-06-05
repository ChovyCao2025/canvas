import http from './api'
import type { R } from '../types'

export interface MarketingFormDefinition {
  id: number
  publicKey: string
  name: string
  description?: string | null
  status: string
  fieldSchemaJson: string
  submitActionJson?: string | null
  successMessage?: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

export interface PublicMarketingForm {
  publicKey: string
  name: string
  description?: string | null
  status: string
  fieldSchemaJson: string
  successMessage?: string | null
}

export interface MarketingFormSubmission {
  id: number
  formId: number
  publicKey: string
  userId?: string | null
  anonymousId?: string | null
  responseJson: string
  utmJson?: string | null
  consentChannel?: string | null
  consentStatus?: string | null
  idempotencyKey: string
  triggerEventCode?: string | null
  createdAt?: string | null
}

export interface MarketingFormPayload {
  publicKey?: string
  name: string
  description?: string
  fieldSchemaJson?: string
  submitActionJson?: string
  successMessage?: string
  active?: boolean
  createdBy?: string
}

export interface PublicSubmitPayload {
  response: Record<string, unknown>
  utm?: Record<string, unknown>
  anonymousId?: string
  idempotencyKey?: string
  consentChannel?: string
  consentStatus?: string
}

export interface PublicSubmitResult {
  submissionId?: number | null
  userId?: string | null
  successMessage?: string | null
  triggerEventCode?: string | null
  triggered: boolean
}

type MarketingFormsHttpClient = Pick<typeof http, 'get' | 'post' | 'put'>

function encodePath(value: string | number) {
  return encodeURIComponent(String(value))
}

function compactBody<T extends object>(body: T): Partial<T> {
  return Object.fromEntries(
    Object.entries(body).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  ) as Partial<T>
}

export function createMarketingFormsApi(client: MarketingFormsHttpClient = http) {
  return {
    list: () => client.get<R<MarketingFormDefinition[]>, R<MarketingFormDefinition[]>>('/canvas/marketing-forms'),
    get: (id: number) =>
      client.get<R<MarketingFormDefinition>, R<MarketingFormDefinition>>(`/canvas/marketing-forms/${encodePath(id)}`),
    create: (payload: MarketingFormPayload) =>
      client.post<R<MarketingFormDefinition>, R<MarketingFormDefinition>>(
        '/canvas/marketing-forms',
        compactBody(payload),
      ),
    update: (id: number, payload: MarketingFormPayload) =>
      client.put<R<MarketingFormDefinition>, R<MarketingFormDefinition>>(
        `/canvas/marketing-forms/${encodePath(id)}`,
        compactBody(payload),
      ),
    setStatus: (id: number, active: boolean) =>
      client.put<R<MarketingFormDefinition>, R<MarketingFormDefinition>>(
        `/canvas/marketing-forms/${encodePath(id)}/status`,
        { active },
      ),
    submissions: (formId?: number, limit = 50) => {
      const query = formId == null
        ? `?limit=${encodeURIComponent(String(limit))}`
        : `?formId=${encodePath(formId)}&limit=${encodeURIComponent(String(limit))}`
      return client.get<R<MarketingFormSubmission[]>, R<MarketingFormSubmission[]>>(
        `/canvas/marketing-forms/submissions${query}`,
      )
    },
    publicForm: (publicKey: string) =>
      client.get<R<PublicMarketingForm>, R<PublicMarketingForm>>(
        `/public/marketing-forms/${encodePath(publicKey)}`,
      ),
    publicSubmit: (publicKey: string, payload: PublicSubmitPayload) =>
      client.post<R<PublicSubmitResult>, R<PublicSubmitResult>>(
        `/public/marketing-forms/${encodePath(publicKey)}/submit`,
        compactBody(payload),
      ),
  }
}

export const marketingFormsApi = createMarketingFormsApi()
