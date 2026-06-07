import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { demoSandboxApi } from './demoSandboxApi'

describe('demoSandboxApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('calls sandbox lifecycle endpoints', async () => {
    const get = vi.spyOn(http, 'get').mockResolvedValue({ code: 0, message: 'success', data: [] })
    const post = vi.spyOn(http, 'post').mockResolvedValue({ code: 0, message: 'success', data: {} })
    const replyPayload = {
      canvasId: 10,
      versionId: 20,
      executionId: 'exec-1',
      userId: 'user-1',
      externalMessageId: 'reply-1',
      eventId: 'event-1',
      text: 'yes please',
      intent: 'PRODUCT_A',
      attributes: { buttonId: 'product-a' },
    }

    await demoSandboxApi.install({ tenantId: 8, demoName: 'Retail Demo', ttlDays: 14 })
    await demoSandboxApi.reset(8)
    await demoSandboxApi.reply(8, replyPayload)
    await demoSandboxApi.expired()

    expect(post).toHaveBeenCalledWith('/demo-sandboxes', {
      tenantId: 8,
      demoName: 'Retail Demo',
      ttlDays: 14,
    })
    expect(post).toHaveBeenCalledWith('/demo-sandboxes/8/reset')
    expect(post).toHaveBeenCalledWith('/demo-sandboxes/8/conversation-replies', replyPayload)
    expect(get).toHaveBeenCalledWith('/demo-sandboxes/expired')
  })
})
