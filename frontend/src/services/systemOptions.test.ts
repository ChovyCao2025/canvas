import { afterEach, describe, expect, it, vi } from 'vitest'

import http from './api'
import { systemOptionsApi } from './systemOptions'
import type { SystemOption } from '../types'

describe('systemOptionsApi', () => {
  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('normalizes current admin list responses into a page result', async () => {
    const option: SystemOption = {
      id: 1,
      tenantId: 7,
      category: 'http_method',
      optionKey: 'GET',
      label: 'GET',
      description: 'GET method',
      sortOrder: 1,
      enabled: 1,
      systemBuiltin: 1,
      updatedAt: '2026-06-16T00:00:00',
    }
    vi.spyOn(http, 'get').mockResolvedValue({ code: 0, message: 'success', data: [option] })

    const response = await systemOptionsApi.adminList({ category: 'http_method' })

    expect(http.get).toHaveBeenCalledWith('/admin/system-options', { params: { category: 'http_method' } })
    expect(response.data).toEqual({ total: 1, list: [option] })
  })
})
