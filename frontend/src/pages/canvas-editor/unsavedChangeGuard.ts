interface GuardState {
  isDirty: boolean
  readonly: boolean
}

export function shouldWarnBeforeUnload(state: GuardState): boolean {
  return state.isDirty && !state.readonly
}

export function shouldBlockEditorUnload(isDirty: boolean, readonly: boolean): boolean {
  return shouldWarnBeforeUnload({ isDirty, readonly })
}

export function applyBeforeUnloadGuard(event: BeforeUnloadEvent, isDirty: boolean, readonly: boolean): boolean {
  if (!shouldBlockEditorUnload(isDirty, readonly)) return false
  event.preventDefault()
  event.returnValue = ''
  return true
}

export function bindBeforeUnloadGuard(shouldWarn: () => boolean): () => void {
  const handler = (event: BeforeUnloadEvent) => {
    if (!shouldWarn()) return
    event.preventDefault()
    event.returnValue = ''
  }
  window.addEventListener('beforeunload', handler)
  return () => window.removeEventListener('beforeunload', handler)
}
