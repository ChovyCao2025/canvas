import { describe, expect, it, vi } from 'vitest'
import { createMarketingPreferencesApi } from './marketingPreferencesApi'

describe('marketingPreferencesApi', () => {
  it('calls preference center endpoints with encoded path params and compact bodies', async () => {
    const get = vi.fn().mockResolvedValue({ data: {} })
    const put = vi.fn().mockResolvedValue({ data: {} })
    const post = vi.fn().mockResolvedValue({ data: {} })
    const api = createMarketingPreferencesApi({ get, put, post } as any)

    await api.report('user+1@example.com')
    await api.updateConsent('user+1@example.com', 'email', { consentStatus: 'OPT_IN', source: '' })
    await api.updateChannel('user-1', 'sms', { address: '13800000000', enabled: true, verified: false, metadata: '' })
    await api.addSuppression('user-1', { channel: 'all', reason: 'complaint', active: true, expiresAt: '' })
    await api.deactivateSuppression(9)

    expect(get).toHaveBeenCalledWith('/canvas/marketing-preferences/users/user%2B1%40example.com')
    expect(put).toHaveBeenNthCalledWith(
      1,
      '/canvas/marketing-preferences/users/user%2B1%40example.com/consents/email',
      { consentStatus: 'OPT_IN' },
    )
    expect(put).toHaveBeenNthCalledWith(
      2,
      '/canvas/marketing-preferences/users/user-1/channels/sms',
      { address: '13800000000', enabled: true, verified: false },
    )
    expect(post).toHaveBeenCalledWith(
      '/canvas/marketing-preferences/users/user-1/suppressions',
      { channel: 'all', reason: 'complaint', active: true },
    )
    expect(put).toHaveBeenNthCalledWith(3, '/canvas/marketing-preferences/suppressions/9/deactivate')
  })
})
