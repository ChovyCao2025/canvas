import { describe, expect, it, vi } from 'vitest'
import { createMauticInsightsApi } from './mauticInsightsApi'

describe('mauticInsightsApi', () => {
  it('requests all Mautic-inspired insight endpoints with compact params', async () => {
    const get = vi.fn().mockResolvedValue({ data: {} })
    const api = createMauticInsightsApi({ get } as any)

    await api.explainAudienceMembership({ audienceId: 10, userId: 'user+1@example.com' })
    await api.explainJourneyPath('exec-1')
    await api.resolveChannelPreference({ userId: 'user-1', preferredChannel: '' })
    await api.suppressionTimeline('user-1')
    await api.publishHealth(9)
    await api.frequencyTemplates()

    expect(get).toHaveBeenNthCalledWith(1, '/canvas/mautic-insights/audience-membership', {
      params: { audienceId: 10, userId: 'user+1@example.com' },
    })
    expect(get).toHaveBeenNthCalledWith(2, '/canvas/mautic-insights/journey-path', {
      params: { executionId: 'exec-1' },
    })
    expect(get).toHaveBeenNthCalledWith(3, '/canvas/mautic-insights/channel-preference', {
      params: { userId: 'user-1' },
    })
    expect(get).toHaveBeenNthCalledWith(4, '/canvas/mautic-insights/suppression-timeline', {
      params: { userId: 'user-1' },
    })
    expect(get).toHaveBeenNthCalledWith(5, '/canvas/mautic-insights/publish-health', {
      params: { canvasId: 9 },
    })
    expect(get).toHaveBeenNthCalledWith(6, '/canvas/mautic-insights/frequency-templates')
  })
})
