import http from './api'
import type { R, StubOption } from '../types'
import type { AxiosRequestConfig } from 'axios'

export interface AiModelView {
  id: number
  tenantId: number | null
  providerId: number | null
  modelKey: string
  displayName: string
  capability: string
  contextWindow: number
  enabled: boolean
}

export interface AiProviderView {
  id: number
  tenantId: number | null
  providerKey: string
  displayName: string
  providerType: string
  endpoint: string
  maskedApiKey?: string | null
  enabled: boolean
  models: AiModelView[]
}

export interface AiProviderSaveRequest {
  providerKey: string
  displayName: string
  providerType: string
  endpoint: string
  apiKey?: string
  enabled?: boolean
  models?: Array<{
    modelKey: string
    displayName?: string
    capability?: string
    contextWindow?: number
    enabled?: boolean
  }>
}

export interface AiPromptTemplateSummary {
  id: number
  tenantId: number | null
  name: string
  category: string
  enabled: boolean
}

export interface AiPromptTemplateDetail extends AiPromptTemplateSummary {
  promptTemplate: string
  outputSchema: Record<string, unknown>
  defaultValues: Record<string, unknown>
}

export interface AiPromptTemplateSaveRequest {
  name: string
  category: string
  promptTemplate: string
  outputSchema: Record<string, unknown>
  defaultValues: Record<string, unknown>
  enabled?: boolean
}

type HttpLike = {
  get: <T = unknown, Rv = T>(url: string, config?: AxiosRequestConfig) => Promise<Rv>
  post: <T = unknown, Rv = T>(url: string, data?: unknown, config?: AxiosRequestConfig) => Promise<Rv>
  put: <T = unknown, Rv = T>(url: string, data?: unknown, config?: AxiosRequestConfig) => Promise<Rv>
}

export function createAiApi(client: HttpLike) {
  return {
    listAiProvidersForMeta: () =>
      client.get<R<StubOption[]>, R<StubOption[]>>('/meta/ai-providers'),

    listAiTemplatesForMeta: () =>
      client.get<R<StubOption[]>, R<StubOption[]>>('/meta/ai-templates'),

    listAiModelsForMeta: (providerId?: number | string | null) =>
      client.get<R<StubOption[]>, R<StubOption[]>>('/meta/ai-models', {
        params: providerId == null || providerId === '' ? undefined : { providerId },
      }),

    listAiProviders: () =>
      client.get<R<AiProviderView[]>, R<AiProviderView[]>>('/ai/providers'),

    saveAiProvider: (payload: AiProviderSaveRequest, id?: number) =>
      id == null
        ? client.post<R<AiProviderView>, R<AiProviderView>>('/ai/providers', payload)
        : client.put<R<AiProviderView>, R<AiProviderView>>(`/ai/providers/${id}`, payload),

    disableAiProvider: (id: number) =>
      client.post<R<void>, R<void>>(`/ai/providers/${id}/disable`),

    listAiPromptTemplates: () =>
      client.get<R<AiPromptTemplateSummary[]>, R<AiPromptTemplateSummary[]>>('/ai/prompt-templates'),

    saveAiPromptTemplate: (payload: AiPromptTemplateSaveRequest) =>
      client.post<R<AiPromptTemplateDetail>, R<AiPromptTemplateDetail>>('/ai/prompt-templates', payload),

    disableAiPromptTemplate: (id: number) =>
      client.post<R<void>, R<void>>(`/ai/prompt-templates/${id}/disable`),
  }
}

export const aiApi = createAiApi(http)
