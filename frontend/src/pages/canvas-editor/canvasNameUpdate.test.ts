/**
 * 测试职责：验证画布名称编辑的 trim、空值校验和编辑态 UI 辅助规则。
 *
 * 维护说明：标题栏交互变化时，先确认这些纯函数契约是否仍成立。
 */
import { describe, expect, it } from 'vitest'
import { buildCanvasNameUpdate, getCanvasNameStatusGap, shouldShowCanvasNameActions } from './canvasNameUpdate'

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

  it('shows save/cancel actions as soon as title editing starts', () => {
    expect(shouldShowCanvasNameActions(true)).toBe(true)
    expect(shouldShowCanvasNameActions(false)).toBe(false)
  })

  it('moves the status tag away only while title editing', () => {
    expect(getCanvasNameStatusGap(true)).toBe(16)
    expect(getCanvasNameStatusGap(false)).toBe(0)
  })
})
