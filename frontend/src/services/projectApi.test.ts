import { describe, expect, it, vi } from 'vitest'
import { createProjectApi } from './api'

describe('projectApi', () => {
  it('calls project governance endpoints', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: [] }),
      post: vi.fn().mockResolvedValue({ data: {} }),
      put: vi.fn().mockResolvedValue({ data: {} }),
      delete: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createProjectApi(client as any)

    await api.list()
    await api.create({ projectKey: 'growth', projectName: 'Growth' })
    await api.setMember(3, 9, { username: 'alice', role: 'EDITOR' })

    expect(client.get).toHaveBeenCalledWith('/admin/projects')
    expect(client.post).toHaveBeenCalledWith('/admin/projects', { projectKey: 'growth', projectName: 'Growth' })
    expect(client.put).toHaveBeenCalledWith('/admin/projects/3/members/9', { username: 'alice', role: 'EDITOR' })
  })

  it('calls project details, canvases, stats, and member removal endpoints', async () => {
    const client = {
      get: vi.fn().mockResolvedValue({ data: {} }),
      post: vi.fn().mockResolvedValue({ data: {} }),
      put: vi.fn().mockResolvedValue({ data: {} }),
      delete: vi.fn().mockResolvedValue({ data: {} }),
    }
    const api = createProjectApi(client as any)

    await api.detail(3)
    await api.update(3, { projectName: 'Growth Plus' })
    await api.disable(3)
    await api.members(3)
    await api.removeMember(3, 9)
    await api.canvases(3, { page: 2, size: 10 })
    await api.stats(3)

    expect(client.get).toHaveBeenCalledWith('/admin/projects/3')
    expect(client.put).toHaveBeenCalledWith('/admin/projects/3', { projectName: 'Growth Plus' })
    expect(client.put).toHaveBeenCalledWith('/admin/projects/3/disable')
    expect(client.get).toHaveBeenCalledWith('/admin/projects/3/members')
    expect(client.delete).toHaveBeenCalledWith('/admin/projects/3/members/9')
    expect(client.get).toHaveBeenCalledWith('/admin/projects/3/canvases', { params: { page: 2, size: 10 } })
    expect(client.get).toHaveBeenCalledWith('/admin/projects/3/stats')
  })
})
