import { describe, expect, it, vi } from 'vitest'
import { createCanvasBatchApi } from './canvasBatchApi'

describe('canvasBatchApi', () => {
  it('posts a batch operation with selected canvas ids', async () => {
    const http = {
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createCanvasBatchApi(http as any)

    await api.run('pause', {
      canvasIds: [1, 2],
      reason: 'maintenance window',
    })

    expect(http.post).toHaveBeenCalledWith('/canvas/batch/pause', {
      canvasIds: [1, 2],
      reason: 'maintenance window',
    })
  })

  it('posts clone replacements and filters using the backend contract', async () => {
    const http = {
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createCanvasBatchApi(http as any)

    await api.run('clone', {
      filters: { status: 0, name: 'growth', triggerType: 'REALTIME', limit: 50 },
      replacements: { name: 'EU Journey', market: 'EU' },
      reason: 'market rollout',
    })

    expect(http.post).toHaveBeenCalledWith('/canvas/batch/clone', {
      filters: { status: 0, name: 'growth', triggerType: 'REALTIME', limit: 50 },
      replacements: { name: 'EU Journey', market: 'EU' },
      reason: 'market rollout',
    })
  })
})
