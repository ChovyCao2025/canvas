import { describe, expect, it, vi } from 'vitest'
import { createContactabilityApi } from './contactabilityApi'

describe('contactabilityApi', () => {
  it('requests contactability explanation with encoded user and channel params', async () => {
    const get = vi.fn().mockResolvedValue({ data: { allowed: true, checks: [] } })
    const api = createContactabilityApi({ get } as any)

    await api.explain({ userId: 'user+1@example.com', channel: 'sms' })

    expect(get).toHaveBeenCalledWith('/canvas/contactability/explain', {
      params: {
        userId: 'user+1@example.com',
        channel: 'sms',
      },
    })
  })
})
