import type { Canvas } from '../../types'

export interface CanvasProjectFilterState {
  page: number
  projectKey?: string
  folderKey?: string
}

export function buildCanvasListParams(state: CanvasProjectFilterState) {
  const params: Record<string, string | number> = { page: state.page, size: 20 }
  if (state.projectKey?.trim()) params.projectKey = state.projectKey.trim()
  if (state.folderKey?.trim()) params.folderKey = state.folderKey.trim()
  return params
}

export function projectFolderLabel(canvas: Pick<Canvas, 'projectKey' | 'projectName' | 'folderKey' | 'folderName'>) {
  const project = canvas.projectName || canvas.projectKey
  const folder = canvas.folderName || canvas.folderKey
  return [project, folder].filter(Boolean).join(' / ') || '-'
}
