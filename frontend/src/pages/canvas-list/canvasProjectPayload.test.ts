import { describe, expect, it } from 'vitest'
import { buildCanvasCreatePayload } from './canvasProjectPayload'

describe('canvasProjectPayload', () => {
  it('keeps formal project assignment fields', () => {
    expect(buildCanvasCreatePayload({
      name: 'Welcome',
      projectId: 3,
      folderKey: 'new-user',
      folderName: 'New User',
    })).toEqual({
      name: 'Welcome',
      projectId: 3,
      folderKey: 'new-user',
      folderName: 'New User',
    })
  })

  it('drops blank optional fields', () => {
    expect(buildCanvasCreatePayload({
      name: 'Welcome',
      description: ' ',
      projectKey: '',
      projectName: ' ',
      folderKey: ' recall ',
    })).toEqual({
      name: 'Welcome',
      folderKey: 'recall',
    })
  })
})
