import { describe, expect, it } from 'vitest'
import { buildCanvasListParams, projectFolderLabel } from './canvasProjectFilters'

describe('canvas project filters', () => {
  it('drops blank project and folder filters', () => {
    expect(buildCanvasListParams({
      page: 2,
      projectKey: ' ',
      folderKey: '',
    })).toEqual({ page: 2, size: 20 })
  })

  it('keeps selected project and folder keys', () => {
    expect(buildCanvasListParams({
      page: 1,
      projectKey: 'growth',
      folderKey: 'new-user',
    })).toEqual({ page: 1, size: 20, projectKey: 'growth', folderKey: 'new-user' })
  })

  it('formats project and folder names for table display', () => {
    expect(projectFolderLabel({
      projectKey: 'growth',
      projectName: '增长',
      folderKey: 'new-user',
      folderName: '新客',
    })).toBe('增长 / 新客')
  })
})
