/* @vitest-environment jsdom */
import { describe, expect, it, vi } from 'vitest'
import {
  applyBeforeUnloadGuard,
  bindBeforeUnloadGuard,
  shouldBlockEditorUnload,
  shouldWarnBeforeUnload,
} from './unsavedChangeGuard'

describe('unsavedChangeGuard', () => {
  it('blocks unload only for dirty writable editors', () => {
    expect(shouldWarnBeforeUnload({ isDirty: true, readonly: false })).toBe(true)
    expect(shouldWarnBeforeUnload({ isDirty: false, readonly: false })).toBe(false)
    expect(shouldWarnBeforeUnload({ isDirty: true, readonly: true })).toBe(false)
    expect(shouldBlockEditorUnload(true, false)).toBe(true)
    expect(shouldBlockEditorUnload(false, false)).toBe(false)
    expect(shouldBlockEditorUnload(true, true)).toBe(false)
  })

  it('marks beforeunload events when blocking', () => {
    const event = { preventDefault: () => undefined, returnValue: undefined as string | undefined } as BeforeUnloadEvent

    expect(applyBeforeUnloadGuard(event, true, false)).toBe(true)
    expect(event.returnValue).toBe('')
  })

  it('registers and removes beforeunload listener', () => {
    const add = vi.spyOn(window, 'addEventListener')
    const remove = vi.spyOn(window, 'removeEventListener')

    const cleanup = bindBeforeUnloadGuard(() => true)
    cleanup()

    expect(add).toHaveBeenCalledWith('beforeunload', expect.any(Function))
    expect(remove).toHaveBeenCalledWith('beforeunload', expect.any(Function))
  })
})
