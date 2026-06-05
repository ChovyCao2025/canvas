import { describe, expect, it, vi } from 'vitest'
import { createExecutionRerunApi } from './executionRerunApi'

describe('executionRerunApi', () => {
  it('calls test user set and user endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createExecutionRerunApi(http as any)

    await api.listSets()
    await api.createSet({ name: 'VIP seeds', description: 'demo' })
    await api.listUsers(11)
    await api.createUser(11, {
      userId: 'user-1',
      displayName: 'Alice',
      profile: { tier: 'gold' },
      inputParams: { amount: 150 },
    })
    await api.previewUser(22)

    expect(http.get).toHaveBeenCalledWith('/test-users/sets')
    expect(http.post).toHaveBeenCalledWith('/test-users/sets', { name: 'VIP seeds', description: 'demo' })
    expect(http.get).toHaveBeenCalledWith('/test-users/sets/11/users')
    expect(http.post).toHaveBeenCalledWith('/test-users/sets/11/users', {
      userId: 'user-1',
      displayName: 'Alice',
      profile: { tier: 'gold' },
      inputParams: { amount: 150 },
    })
    expect(http.get).toHaveBeenCalledWith('/test-users/22/preview')
  })

  it('calls rerun and audit endpoints', async () => {
    const http = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createExecutionRerunApi(http as any)

    await api.rerunCanvas(10, {
      userId: 'user-1',
      mode: 'SKIP_SIDE_EFFECTS',
      reason: 'rerun without downstream side effects',
      inputParams: { amount: 150 },
    })
    await api.audit(44)
    await api.listAudits(10)
    await api.listAudits()

    expect(http.post).toHaveBeenCalledWith('/execution-reruns/canvas/10', {
      userId: 'user-1',
      mode: 'SKIP_SIDE_EFFECTS',
      reason: 'rerun without downstream side effects',
      inputParams: { amount: 150 },
    })
    expect(http.get).toHaveBeenCalledWith('/execution-reruns/44')
    expect(http.get).toHaveBeenCalledWith('/execution-reruns', { params: { canvasId: 10 } })
    expect(http.get).toHaveBeenCalledWith('/execution-reruns', { params: undefined })
  })
})
