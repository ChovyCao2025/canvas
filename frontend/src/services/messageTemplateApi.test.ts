import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { messageTemplateApi } from './messageTemplateApi'

describe('messageTemplateApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('calls message template search, create, and preview endpoints', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ code: 0, message: 'success', data: [] })
    const post = vi.spyOn(http, 'post').mockResolvedValue({ code: 0, message: 'success', data: {} })

    await messageTemplateApi.search({ keyword: 'welcome', channel: 'SMS' })
    await messageTemplateApi.create({
      templateCode: 'welcome_sms',
      displayName: 'Welcome SMS',
      channel: 'SMS',
      body: 'Hi {{firstName}}',
    })
    await messageTemplateApi.preview('welcome_sms', { firstName: 'Alice' })

    expect(get).toHaveBeenCalledWith('/message-templates', {
      params: { keyword: 'welcome', channel: 'SMS' },
    })
    expect(post).toHaveBeenCalledWith('/message-templates', {
      templateCode: 'welcome_sms',
      displayName: 'Welcome SMS',
      channel: 'SMS',
      body: 'Hi {{firstName}}',
    })
    expect(post).toHaveBeenCalledWith('/message-templates/welcome_sms/preview', { firstName: 'Alice' })
  })
})
