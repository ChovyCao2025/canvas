import { describe, expect, it, vi } from 'vitest'
import { createAiPredictionApi } from './aiPredictionApi'

describe('aiPredictionApi', () => {
  it('calls AI prediction read endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createAiPredictionApi(http as any)

    await api.latestRun()
    await api.readiness()
    await api.churnDistribution()
    await api.topRiskUsers(25)

    expect(http.get).toHaveBeenCalledWith('/ai/predictions/latest-run')
    expect(http.get).toHaveBeenCalledWith('/ai/predictions/readiness')
    expect(http.get).toHaveBeenCalledWith('/ai/predictions/churn-distribution')
    expect(http.get).toHaveBeenCalledWith('/ai/predictions/top-risk-users', { params: { limit: 25 } })
  })

  it('calls recompute endpoint with force date and limit', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createAiPredictionApi(http as any)

    await api.recompute({ force: true, runDate: '2026-06-04', limit: 50 })

    expect(http.post).toHaveBeenCalledWith('/ai/predictions/recompute', {
      force: true,
      runDate: '2026-06-04',
      limit: 50,
    })
  })
})
