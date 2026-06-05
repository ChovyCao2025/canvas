import { describe, expect, it, vi } from 'vitest'
import { createChannelConnectorApi } from './channelConnectorApi'

describe('channelConnectorApi', () => {
  it('calls connector list and fallback validation endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: { valid: true } }),
    }
    const api = createChannelConnectorApi(http as any)

    await api.list()
    await api.validateFallback({ channel: 'PUSH', provider: 'JPUSH', fallbackChannel: 'SMS', fallbackProvider: 'ALIYUN' })

    expect(http.get).toHaveBeenCalledWith('/channels/connectors')
    expect(http.post).toHaveBeenCalledWith('/channels/connectors/fallback/validate', {
      channel: 'PUSH',
      provider: 'JPUSH',
      fallbackChannel: 'SMS',
      fallbackProvider: 'ALIYUN',
    })
  })

  it('calls policy visibility endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createChannelConnectorApi(http as any)

    await api.limits()
    await api.decisions()
    await api.dedupeRecords()
    await api.testHealth(7)
    await api.updateMode(7, 'DISABLED', 'maintenance')

    expect(http.get).toHaveBeenCalledWith('/channels/connectors/limits')
    expect(http.get).toHaveBeenCalledWith('/channels/connectors/fallback/decisions')
    expect(http.get).toHaveBeenCalledWith('/channels/connectors/dedupe-records')
    expect(http.post).toHaveBeenCalledWith('/channels/connectors/7/health-test')
    expect(http.post).toHaveBeenCalledWith('/channels/connectors/7/mode', { mode: 'DISABLED', reason: 'maintenance' })
  })
})
