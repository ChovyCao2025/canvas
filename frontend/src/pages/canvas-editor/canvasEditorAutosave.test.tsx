/**
 * 测试职责：验证画布编辑器自动保存调度只在最后一次编辑 3 秒后触发。
 *
 * 维护说明：自动保存必须使用最新状态快照，调度层只负责 debounce 和 silent save。
 */
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  CANVAS_EDITOR_AUTOSAVE_DELAY_MS,
  clearCanvasEditorAutosave,
  scheduleCanvasEditorAutosave,
} from './canvasEditorAutosave'

describe('canvas editor autosave scheduling', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('fires silent save after edits settle for three seconds', () => {
    vi.useFakeTimers()
    const save = vi.fn()

    const timer = scheduleCanvasEditorAutosave(true, save)

    vi.advanceTimersByTime(CANVAS_EDITOR_AUTOSAVE_DELAY_MS - 1)
    expect(save).not.toHaveBeenCalled()

    vi.advanceTimersByTime(1)
    expect(save).toHaveBeenCalledWith(true)

    clearCanvasEditorAutosave(timer)
  })

  it('replaces a pending autosave when another edit arrives', () => {
    vi.useFakeTimers()
    const save = vi.fn()

    const first = scheduleCanvasEditorAutosave(true, save)
    vi.advanceTimersByTime(1000)
    const second = scheduleCanvasEditorAutosave(true, save, first)
    vi.advanceTimersByTime(CANVAS_EDITOR_AUTOSAVE_DELAY_MS - 1)
    expect(save).not.toHaveBeenCalled()

    vi.advanceTimersByTime(1)
    expect(save).toHaveBeenCalledTimes(1)

    clearCanvasEditorAutosave(second)
  })
})
