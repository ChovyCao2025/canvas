import { describe, expect, it, vi } from 'vitest'
import { createAiApi } from './aiApi'

describe('aiApi', () => {
  it('calls AI meta option endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
      put: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createAiApi(http as any)

    await api.listAiProvidersForMeta()
    await api.listAiTemplatesForMeta()
    await api.listAiModelsForMeta(11)
    await api.listAiModelsForMeta()

    expect(http.get).toHaveBeenCalledWith('/meta/ai-providers')
    expect(http.get).toHaveBeenCalledWith('/meta/ai-templates')
    expect(http.get).toHaveBeenCalledWith('/meta/ai-models', { params: { providerId: 11 } })
    expect(http.get).toHaveBeenCalledWith('/meta/ai-models', { params: undefined })
  })

  it('calls AI provider and prompt template admin endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
      put: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createAiApi(http as any)
    const providerPayload = {
      providerKey: 'openai',
      displayName: 'OpenAI',
      providerType: 'OPENAI_COMPATIBLE',
      endpoint: 'https://api.example.test/v1',
    }
    const templatePayload = {
      name: 'Message',
      category: 'text_generate',
      promptTemplate: 'Generate ${name}',
      outputSchema: { type: 'object' },
      defaultValues: { text: 'fallback' },
    }

    await api.listAiProviders()
    await api.saveAiProvider(providerPayload)
    await api.saveAiProvider(providerPayload, 7)
    await api.disableAiProvider(7)
    await api.listAiPromptTemplates()
    await api.saveAiPromptTemplate(templatePayload)
    await api.saveAiPromptTemplate(templatePayload, 9)
    await api.disableAiPromptTemplate(9)

    expect(http.get).toHaveBeenCalledWith('/ai/providers')
    expect(http.post).toHaveBeenCalledWith('/ai/providers', providerPayload)
    expect(http.put).toHaveBeenCalledWith('/ai/providers/7', providerPayload)
    expect(http.post).toHaveBeenCalledWith('/ai/providers/7/disable')
    expect(http.get).toHaveBeenCalledWith('/ai/prompt-templates')
    expect(http.post).toHaveBeenCalledWith('/ai/prompt-templates', templatePayload)
    expect(http.put).toHaveBeenCalledWith('/ai/prompt-templates/9', templatePayload)
    expect(http.post).toHaveBeenCalledWith('/ai/prompt-templates/9/disable')
  })
})
