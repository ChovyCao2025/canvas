import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { platformWorkstreamApi } from './platformWorkstreamApi'

describe('platformWorkstreamApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('loads platform workstream statuses', async () => {
    const response = { code: 0, message: 'success', data: [] }
    const get = vi.spyOn(http, 'get').mockResolvedValue(response)

    await expect(platformWorkstreamApi.list()).resolves.toBe(response)

    expect(get).toHaveBeenCalledWith('/platform/workstreams')
  })
})
