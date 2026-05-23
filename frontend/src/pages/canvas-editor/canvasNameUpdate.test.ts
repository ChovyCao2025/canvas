import { describe, expect, it } from 'vitest'
import { buildCanvasNameUpdate, shouldShowCanvasNameActions } from './canvasNameUpdate'

describe('buildCanvasNameUpdate', () => {
  it('trims the canvas name before saving', () => {
    expect(buildCanvasNameUpdate('  新名称  ', 'old')).toEqual({ name: '新名称' })
  })

  it('returns unchanged when the trimmed name matches the saved name', () => {
    expect(buildCanvasNameUpdate('  旧名称  ', '旧名称')).toEqual({ unchanged: true })
  })

  it('returns an error for blank names', () => {
    expect(buildCanvasNameUpdate('   ', '旧名称')).toEqual({ error: '画布名称不能为空' })
  })

  it('shows save/cancel actions only when the visible name changed', () => {
    expect(shouldShowCanvasNameActions('  旧名称  ', '旧名称')).toBe(false)
    expect(shouldShowCanvasNameActions('新名称', '旧名称')).toBe(true)
  })
})
