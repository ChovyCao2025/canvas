export type CanvasNameUpdate =
  | { name: string }
  | { unchanged: true }
  | { error: string }

export function buildCanvasNameUpdate(inputName: string, savedName: string): CanvasNameUpdate {
  const name = inputName.trim()
  if (!name) return { error: '画布名称不能为空' }
  if (name === savedName) return { unchanged: true }
  return { name }
}

export function shouldShowCanvasNameActions(inputName: string, savedName: string): boolean {
  return inputName.trim() !== savedName
}
