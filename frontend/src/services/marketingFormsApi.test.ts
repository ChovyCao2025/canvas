import { describe, expect, it, vi } from 'vitest'
import { createMarketingFormsApi } from './marketingFormsApi'

describe('marketingFormsApi', () => {
  it('calls operator form endpoints with encoded path values', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
      put: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createMarketingFormsApi(client as any)

    await api.list()
    await api.get(7)
    await api.create({ publicKey: 'signup', name: 'Signup', active: true })
    await api.update(7, { name: 'Signup v2' })
    await api.setStatus(7, false)
    await api.submissions(7, 25)

    expect(client.get).toHaveBeenCalledWith('/canvas/marketing-forms')
    expect(client.get).toHaveBeenCalledWith('/canvas/marketing-forms/7')
    expect(client.post).toHaveBeenCalledWith('/canvas/marketing-forms', {
      publicKey: 'signup',
      name: 'Signup',
      active: true,
    })
    expect(client.put).toHaveBeenCalledWith('/canvas/marketing-forms/7', { name: 'Signup v2' })
    expect(client.put).toHaveBeenCalledWith('/canvas/marketing-forms/7/status', { active: false })
    expect(client.get).toHaveBeenCalledWith('/canvas/marketing-forms/submissions?formId=7&limit=25')
  })

  it('calls anonymous public form endpoints', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: {} }),
      post: vi.fn().mockResolvedValue({ data: {} }),
      put: vi.fn(),
    }
    const api = createMarketingFormsApi(client as any)

    await api.publicForm('signup form')
    await api.publicSubmit('signup form', {
      response: { email: 'lead@example.com' },
      anonymousId: 'anon-1',
      idempotencyKey: 'idem-1',
    })

    expect(client.get).toHaveBeenCalledWith('/public/marketing-forms/signup%20form')
    expect(client.post).toHaveBeenCalledWith('/public/marketing-forms/signup%20form/submit', {
      response: { email: 'lead@example.com' },
      anonymousId: 'anon-1',
      idempotencyKey: 'idem-1',
    })
  })
})
