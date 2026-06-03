/**
 * 工具职责：画布编辑器自动保存 debounce 调度。
 *
 * 维护说明：这里不读取 React 状态，只负责 3 秒静默保存的计时器生命周期。
 */

/** 画布编辑器自动保存延迟。 */
export const CANVAS_EDITOR_AUTOSAVE_DELAY_MS = 3000

/** 清理一个待执行的自动保存计时器。 */
export function clearCanvasEditorAutosave(timer?: ReturnType<typeof setTimeout>) {
  if (timer) clearTimeout(timer)
}

/** 为脏编辑状态安排一次静默保存；再次调用会替换之前的待执行保存。 */
export function scheduleCanvasEditorAutosave(
  isDirty: boolean,
  save: (silent: boolean) => void,
  existingTimer?: ReturnType<typeof setTimeout>,
) {
  clearCanvasEditorAutosave(existingTimer)
  if (!isDirty) return undefined
  return setTimeout(() => save(true), CANVAS_EDITOR_AUTOSAVE_DELAY_MS)
}
