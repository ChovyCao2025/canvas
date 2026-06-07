import { describe, expect, it, vi } from 'vitest'
import { createCollaborationApi } from './collaborationApi'

describe('collaborationApi', () => {
  it('requests collaboration summary and editor preferences', async () => {
    const get = vi.fn().mockResolvedValue({ data: {} })
    const put = vi.fn().mockResolvedValue({ data: {} })
    const api = createCollaborationApi({ get, put } as any)

    await api.summary(42)
    await api.editorPreference()
    await api.updateEditorPreference({ theme: 'dark' })

    expect(get).toHaveBeenNthCalledWith(1, '/canvas/42/collaboration/summary')
    expect(get).toHaveBeenNthCalledWith(2, '/canvas/preferences/editor')
    expect(put).toHaveBeenCalledWith('/canvas/preferences/editor', { theme: 'dark' })
  })
})
