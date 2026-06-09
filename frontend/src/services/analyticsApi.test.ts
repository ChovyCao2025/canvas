import { describe, expect, it, vi } from 'vitest'
import { createAnalyticsApi } from './analyticsApi'

describe('analyticsApi', () => {
  it('calls bounded event analysis endpoints without tenant params', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
    }
    const api = createAnalyticsApi(http as any)

    await api.eventCounts({ startDate: '2026-06-01', endDate: '2026-06-03' })
    await api.eventTotal({ startDate: '2026-06-01', endDate: '2026-06-03', eventCode: 'OrderPaid' })

    expect(http.get).toHaveBeenCalledWith('/analytics/events/counts', {
      params: { startDate: '2026-06-01', endDate: '2026-06-03' },
    })
    expect(http.get).toHaveBeenCalledWith('/analytics/events/count', {
      params: { startDate: '2026-06-01', endDate: '2026-06-03', eventCode: 'OrderPaid' },
    })
  })

  it('encodes user and attribute path segments', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
    }
    const api = createAnalyticsApi(http as any)

    await api.userTimeline('user/1', { startDate: '2026-06-01', endDate: '2026-06-03', page: 2, size: 25 })
    await api.attributeDistribution('utm.source', { startDate: '2026-06-01', endDate: '2026-06-03' })

    expect(http.get).toHaveBeenCalledWith('/analytics/users/user%2F1/timeline', {
      params: { startDate: '2026-06-01', endDate: '2026-06-03', page: 2, size: 25 },
    })
    expect(http.get).toHaveBeenCalledWith('/analytics/events/attributes/utm.source/distribution', {
      params: { startDate: '2026-06-01', endDate: '2026-06-03' },
    })
  })
})
