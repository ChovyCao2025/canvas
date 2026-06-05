import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { pluginRegistryApi } from './pluginRegistryApi'

describe('pluginRegistryApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('requests plugin catalog from the registry endpoint', async () => {
    const response = { code: 0, message: 'success', data: {} }
    const get = vi.spyOn(http, 'get').mockResolvedValue(response)

    await expect(pluginRegistryApi.catalog()).resolves.toBe(response)

    expect(get).toHaveBeenCalledWith('/canvas/plugins')
  })

  it('sends enable state with current canvas version header', async () => {
    const response = { code: 0, message: 'success', data: undefined }
    const put = vi.spyOn(http, 'put').mockResolvedValue(response)

    await expect(pluginRegistryApi.setEnabled('csv-export', true, '1.2.0')).resolves.toBe(response)

    expect(put).toHaveBeenCalledWith(
      '/canvas/plugins/csv-export/enabled',
      { enabled: true },
      { headers: { 'X-Canvas-Version': '1.2.0' } },
    )
  })
})
